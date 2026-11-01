package pl.kiosel.advancedshops.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.config.Settings;
import pl.kiosel.advancedshops.data.notification.PendingEarnings;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.advancedshops.data.shop.TradeQuote;
import pl.kiosel.advancedshops.data.sign.ShopSign;
import pl.kiosel.advancedshops.storage.SqlNotificationStorage;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public final class ShopNotificationManager implements AutoCloseable {

	private final AdvancedShops plugin;
	private final SqlNotificationStorage storage;
	private final ConcurrentMap<UUID, Boolean> enabledPreferences = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, Long> preferenceRevisions = new ConcurrentHashMap<>();
	private final Set<UUID> pendingDeliveries = ConcurrentHashMap.newKeySet();
	private final AtomicLong preferenceRevision = new AtomicLong();
	private final ExecutorService databaseQueue = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "AdvancedShops-notifications");
		thread.setDaemon(true);
		return thread;
	});
	private volatile boolean closing;

	public ShopNotificationManager(AdvancedShops plugin) {
		this.plugin = plugin;
		this.storage = new SqlNotificationStorage(plugin.getShopDatabase());

		enabledPreferences.putAll(storage.loadPreferences());
	}

	public boolean isEnabled(UUID owner) {
		return owner != null && enabledPreferences.getOrDefault(owner, Settings.NOTIFICATIONS_DEFAULT.get());
	}

	public void toggle(Player owner, Boolean enabled) {
		UUID id = owner.getUniqueId();
		String ownerName = owner.getName();
		boolean requested = enabled == null ? !isEnabled(id) : enabled;
		Boolean previous = enabledPreferences.put(id, requested);
		long revision = preferenceRevision.incrementAndGet();
		preferenceRevisions.put(id, revision);

		if (!enqueue(() -> {
			try {
				storage.setPreference(id, ownerName, requested);
				runOnServer(() -> {
					if (isCurrentPreferenceChange(id, revision) && owner.isOnline()) {
						plugin.getMessages().sendPrefixed(owner, requested ? Lang.NOTIFICATIONS_ON : Lang.NOTIFICATIONS_OFF);
						if (requested) {
							if (!Settings.NOTIFICATIONS_ENABLED.get()) {
								plugin.getMessages().sendPrefixed(owner, Lang.NOTIFICATIONS_GLOBAL_OFF);
							} else {
								deliverPending(owner);
							}
						}
					}
				});
			} catch (RuntimeException error) {
				plugin.getRosaLogger().log(Level.WARNING, "Could not save notifications for " + id, error);
				runOnServer(() -> {
					if (!isCurrentPreferenceChange(id, revision)) return;
					if (previous == null) {
						enabledPreferences.remove(id);
					} else {
						enabledPreferences.put(id, previous);
					}
					if (owner.isOnline()) plugin.getMessages().sendPrefixed(owner, Lang.NOTIFICATIONS_SAVE_FAILED);
				});
			}
		})) {
			if (previous == null) enabledPreferences.remove(id);
			else enabledPreferences.put(id, previous);
			preferenceRevisions.remove(id, revision);
			plugin.getMessages().sendPrefixed(owner, Lang.NOTIFICATIONS_SAVE_FAILED);
		}
	}

	public void notifyOwnerChest(Player customer, ShopChest shopChest, boolean buying, TradeQuote quote) {
		UUID ownerId = shopChest.getOwner();
		if (shopChest.isAdminShop() || ownerId.equals(customer.getUniqueId())
				|| !Settings.NOTIFICATIONS_ENABLED.get()
				|| !plugin.getSettings().get(buying ? Settings.NOTIFICATIONS_BUY : Settings.NOTIFICATIONS_SELL)
				|| !isEnabled(ownerId)) return;

		Player owner = Bukkit.getPlayer(ownerId);
		if (owner == null || !owner.isOnline()) {
			if (buying) {
				String ownerName = shopChest.getOwnerName();
				String itemName = shopChest.getItemName();
				String material = shopChest.getItem().getType().name();
				enqueueOrLog(() -> storage.addShopSale(ownerId, ownerName, material,
						itemName, quote.getAmount(), quote.getPrice()), "shop sale summary for " + ownerId);
			}
			return;
		}

		plugin.getMessages().message(buying ? Lang.NOTIFICATION_BOUGHT : Lang.NOTIFICATION_SOLD)
				.with(plugin.getShopMessages().placeholders(shopChest))
				.with("customer", customer.getName())
				.with("amount", quote.getAmount())
				.with("price", plugin.getShopMessages().money(quote.getPrice()))
				.sendPrefixed(owner);
	}

	public void notifyOwnerSign(Player customer, ShopSign sign, UUID ownerId) {
		if (!Settings.NOTIFICATIONS_ENABLED.get() || !Settings.NOTIFICATIONS_SIGN.get() || !isEnabled(ownerId)) return;
		String ownerName = sign.getPlayer();
		Player owner = Bukkit.getPlayer(ownerId);
		if (owner == null || !owner.isOnline()) {
			enqueueOrLog(() -> storage.addSignEarning(ownerId, ownerName, sign.getPrice()),
					"pay-sign summary for " + ownerId);
			return;
		}

		plugin.getMessages().message(Lang.NOTIFICATION_SIGN)
				.with(plugin.getShopMessages().placeholders(sign))
				.with("customer", customer.getName())
				.with("player", customer.getName())
				.sendPrefixed(owner);
	}

	public void recordLogout(Player player) {
		UUID id = player.getUniqueId();
		String name = player.getName();
		enqueueOrLog(() -> storage.recordLogout(id, name), "last logout for " + id);
	}

	public void deliverPending(Player owner) {
		UUID id = owner.getUniqueId();
		if (!Settings.NOTIFICATIONS_ENABLED.get() || !isEnabled(id) || !pendingDeliveries.add(id)) return;
		if (!enqueue(() -> {
			final PendingEarnings pending;
			try {
				pending = storage.getPending(id);
			} catch (RuntimeException error) {
				plugin.getRosaLogger().log(Level.WARNING, "Could not load pending notifications for " + id, error);
				runOnServer(() -> pendingDeliveries.remove(id));
				return;
			}
			if (pending.isEmpty()) {
				runOnServer(() -> pendingDeliveries.remove(id));
				return;
			}
			runOnServer(() -> {
				try {
					if (!owner.isOnline() || !Settings.NOTIFICATIONS_ENABLED.get() || !isEnabled(id)) return;
					sendPending(owner, pending);
					enqueueOrLog(() -> storage.clearPending(id, pending), "clearing delivered notifications for " + id);
				} catch (RuntimeException error) {
					plugin.getRosaLogger().log(Level.WARNING, "Could not deliver pending notifications for " + id, error);
				} finally {
					pendingDeliveries.remove(id);
				}
			});
		})) {
			pendingDeliveries.remove(id);
		}
	}

	private void sendPending(Player owner, PendingEarnings pending) {
		plugin.getMessages().message(Lang.PENDING_HEADER).sendPrefixed(owner);
		int shown = 0;
		for (PendingEarnings.SoldItem item : pending.getSoldItems()) {
			if (shown++ == 10) break;
			plugin.getMessages().message(Lang.PENDING_ITEM)
					.with("amount", item.getAmount())
					.with("item", item.getItemName())
					.with("price", plugin.getShopMessages().money(item.getEarnings().doubleValue()))
					.sendPrefixed(owner);
		}
		int remaining = pending.getSoldItems().size() - Math.min(pending.getSoldItems().size(), 10);
		if (remaining > 0)
			plugin.getMessages().message(Lang.PENDING_MORE).with("count", remaining).sendPrefixed(owner);
		String shopTotal = plugin.getShopMessages().money(pending.getShopEarnings().doubleValue());
		String signTotal = plugin.getShopMessages().money(pending.getSignEarnings().doubleValue());
		String total = plugin.getShopMessages().money(pending.getTotalEarnings().doubleValue());

		plugin.getMessages().message(Lang.PENDING_SHOP_TOTAL).with("price", shopTotal).sendPrefixed(owner);
		plugin.getMessages().message(Lang.PENDING_SIGN_TOTAL).with("price", signTotal).sendPrefixed(owner);
		plugin.getMessages().message(Lang.PENDING_TOTAL).with("price", total).sendPrefixed(owner);
	}

	private boolean isCurrentPreferenceChange(UUID id, long revision) {
		return preferenceRevisions.getOrDefault(id, 0L) == revision;
	}

	private void enqueueOrLog(Runnable task, String operation) {
		if (!enqueue(() -> {
			try {
				task.run();
			} catch (RuntimeException error) {
				plugin.getRosaLogger().log(Level.WARNING, "Could not save " + operation, error);
			}
		})) plugin.getRosaLogger().warning("Could not queue " + operation + " because notification storage is closing");
	}

	private boolean enqueue(Runnable task) {
		if (closing) return false;
		try {
			databaseQueue.execute(task);
			return true;
		} catch (RejectedExecutionException error) {
			return false;
		}
	}

	private void runOnServer(Runnable task) {
		if (closing) return;
		try {
			plugin.getRosaScheduler().runGlobal(task);
		} catch (IllegalPluginAccessException ignored) {
		}
	}

	@Override
	public void close() {
		closing = true;
		databaseQueue.shutdown();
		try {
			if (!databaseQueue.awaitTermination(10, TimeUnit.SECONDS)) {
				databaseQueue.shutdownNow();
				plugin.getRosaLogger().warning("Notification storage queue did not finish before shutdown");
			}
		} catch (InterruptedException error) {
			databaseQueue.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
