package pl.kiosel.advancedshops.storage;

import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.data.shop.ShopChest;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public final class ShopDataManager implements AutoCloseable {

	private final AdvancedShops plugin;
	private final ShopStorage store;
	private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "AdvancedShops-storage");
		thread.setDaemon(true);
		return thread;
	});
	private final Set<UUID> pendingShops = ConcurrentHashMap.newKeySet();
	private boolean saving;
	private volatile boolean closing;

	public ShopDataManager(AdvancedShops plugin, ShopStorage store) {
		this.plugin = plugin;
		this.store = store;
	}

	public List<ShopChest> load() throws Exception {
		return store.load();
	}

	public boolean isBusy() {
		return saving || closing;
	}

	public boolean isPending(UUID shopId) {
		return pendingShops.contains(shopId);
	}

	public void save(Collection<ShopChest> candidate, Player player, ShopChest shopChest, Runnable success, Runnable failure) {
		final ShopStorage.PreparedSave prepared;
		try {
			prepared = store.prepareSave(candidate);
		} catch (Exception error) {
			saveFailed(player, failure, error);
			return;
		}
		saving = true;
		pendingShops.add(shopChest.getId());
		try {
			writer.execute(() -> {
				Exception problem = null;
				try {
					prepared.write();
				} catch (Exception error) {
					problem = error;
					plugin.getRosaLogger().log(Level.SEVERE, "Could not save shops", error);
				}
				final Exception result = problem;
				if (closing) return;
				try {
					plugin.getRosaScheduler().runGlobal(() -> {
						if (closing) return;
						saving = false;
						pendingShops.remove(shopChest.getId());
						if (result == null) {
							success.run();
						} else {
							failure.run();
							plugin.getMessages().sendPrefixed(player, Lang.SAVE_FAILED);
						}
					});
				} catch (IllegalPluginAccessException ignored) {
				}
			});
		} catch (RejectedExecutionException error) {
			saving = false;
			pendingShops.remove(shopChest.getId());
			saveFailed(player, failure, error);
		}
	}

	public boolean saveSystem(Collection<ShopChest> candidate, Collection<UUID> affectedShops) {
		if (isBusy()) return false;

		final ShopStorage.PreparedSave prepared;
		try {
			prepared = store.prepareSave(candidate);
		} catch (Exception error) {
			plugin.getRosaLogger().log(Level.SEVERE, "Could not prepare shop removal after explosion", error);
			return false;
		}

		Set<UUID> affectedIds = new HashSet<>(affectedShops);
		saving = true;
		pendingShops.addAll(affectedIds);
		try {
			prepared.write();
			return true;
		} catch (Exception error) {
			plugin.getRosaLogger().log(Level.SEVERE, "Could not remove shop data after explosion", error);
			return false;
		} finally {
			saving = false;
			pendingShops.removeAll(affectedIds);
		}
	}

	private void saveFailed(Player player, Runnable failure, Exception error) {
		failure.run();
		plugin.getRosaLogger().log(Level.SEVERE, "Could not prepare shop save", error);
		plugin.getMessages().sendPrefixed(player, Lang.SAVE_FAILED);
	}

	@Override
	public void close() {
		closing = true;
		writer.shutdown();
		try {
			if (!writer.awaitTermination(15, TimeUnit.SECONDS)) {
				writer.shutdownNow();
				plugin.getRosaLogger().severe("Storage writer did not finish before shutdown; verify storage before restarting");
			}
			store.close();
		} catch (InterruptedException error) {
			writer.shutdownNow();
			Thread.currentThread().interrupt();
		} catch (Exception error) {
			throw new IllegalStateException("Could not close shop storage", error);
		}
	}
}
