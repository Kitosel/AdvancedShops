package pl.kiosel.advancedshops.manager;

import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.api.events.ShopsChestCreateEvent;
import pl.kiosel.advancedshops.api.events.ShopsChestEditEvent;
import pl.kiosel.advancedshops.api.events.ShopsChestRemoveEvent;
import pl.kiosel.advancedshops.api.events.ShopsChestStatusChangeEvent;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.data.BlockKey;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.advancedshops.data.shop.ShopContainers;
import pl.kiosel.advancedshops.data.shop.ShopEditGui;
import pl.kiosel.advancedshops.storage.ShopDataManager;
import pl.kiosel.rosacore.hook.economy.EconomyHook;
import pl.kiosel.rosacore.message.MessageCatalog;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class ShopManager {

	private final AdvancedShops plugin;
	private final ShopDataManager dataManager;
	private final Map<UUID, ShopChest> shops = new LinkedHashMap<>();
	private final Map<BlockKey, ShopChest> byBlock = new HashMap<>();

	public ShopManager(AdvancedShops plugin, ShopDataManager dataManager) {
		this.plugin = plugin;
		this.dataManager = dataManager;
	}

	public void load() throws Exception {
		for (ShopChest shopChest : dataManager.load()) {
			shops.put(shopChest.getId(), shopChest);

			for (BlockKey key : shopChest.getBlocks())
				byBlock.put(key, shopChest);
		}
	}

	public Collection<ShopChest> allShops() {
		return Collections.unmodifiableCollection(shops.values());
	}

	public List<ShopChest> findPlayerShops(UUID playerId) {
		return shops.values().stream().filter(shop -> !shop.isAdminShop() && shop.getOwner().equals(playerId))
				.sorted(Comparator.comparing(ShopChest::getId)).collect(Collectors.toList());
	}

	public List<ShopChest> findAdminShops() {
		return shops.values().stream().filter(ShopChest::isAdminShop)
				.sorted(Comparator.comparing(ShopChest::getId)).collect(Collectors.toList());
	}

	public boolean playerHasShop(Player player) {
		for (ShopChest shopChest : allShops()) {
			if (!shopChest.isAdminShop() && shopChest.getOwner().equals(player.getUniqueId())) {
				return true;
			}
		}
		return false;
	}

	public boolean adminHasShop() {
		for (ShopChest shopChest : findAdminShops()) {
			if (shopChest != null && shopChest.isAdminShop()) {
				return true;
			}
		}
		return false;
	}

	public ShopChest getShopAt(Block block) {
		return block == null ? null : byBlock.get(BlockKey.of(block));
	}

	public ShopChest getShopIn(Inventory inventory) {
		for (BlockKey key : ShopContainers.keys(inventory)) {
			ShopChest shopChest = byBlock.get(key);
			if (shopChest != null)
				return shopChest;
		}
		return null;
	}

	public boolean manages(Player player, ShopChest shopChest) {
		return player.hasPermission("advancedshops.admin")
				|| (!shopChest.isAdminShop() && shopChest.getOwner().equals(player.getUniqueId()));
	}

	public void create(Player player, Block block, ItemStack item, int amount, double buy, double sell) {
		create(player, block, item, amount, buy, sell, false);
	}

	public void create(Player player, Block block, ItemStack item, int amount, double buy, double sell, boolean adminShop) {
		if (adminShop && !player.hasPermission("advancedshops.adminshop")) {
			getMessage().sendPrefixed(player, Lang.CANT_CREATE_ADMIN);
			return;
		}
		if (busy(player)) return;

		boolean takeMoney = plugin.getChestsFile().getBoolean("take-money-for-creating-shop");
		double costUser = plugin.getChestsFile().getDouble("cost-user");
		double costAdmin = plugin.getChestsFile().getDouble("cost-admin");
		double cost = adminShop ? costAdmin : costUser;
		boolean isZero = cost == 0;

		if (takeMoney && (!Double.isFinite(cost) || cost < 0)) {
			getMessage().sendPrefixed(player, Lang.CREATE_COST_INVALID);
			plugin.getRosaLogger().severe("Invalid shop creation cost configured for " + (adminShop ? "admin" : "user") + " shops: " + cost);
			return;
		}

		EconomyHook economy = plugin.getEconomy();
		if (economy == null && !isZero) {
			getMessage().sendPrefixed(player, Lang.ECONOMY_UNAVAILABLE);
			return;
		}

		Inventory inventory = ShopContainers.inventory(block);
		if (inventory == null) {
			getMessage().sendPrefixed(player, Lang.INVALID_CONTAINER);
			return;
		}

		List<BlockKey> keys = ShopContainers.keys(inventory);
		if (keys.isEmpty() || keys.stream().anyMatch(byBlock::containsKey)) {
			getMessage().sendPrefixed(player, Lang.ALREADY_SHOP);
			return;
		}
		for (BlockKey key : keys) {
			if (!plugin.getHookManager().getWorldGuard().isBuildAllowed(player, key.getLocation())) {
				getMessage().sendPrefixed(player, Lang.CANT_CREATE);
				return;
			}
		}

		int limit = plugin.getChestsFile().getInt("max-per-player");
		if (!player.hasPermission("advancedshops.admin") && limit > 0
				&& shops.values().stream().filter(s -> !s.isAdminShop() && s.getOwner().equals(player.getUniqueId())).count() >= limit) {
			plugin.getMessages().sendPrefixed(player, Lang.LIMIT, "limit", limit);
			return;
		}
		ShopChest shopChest = new ShopChest(
				UUID.randomUUID(), player.getUniqueId(), player.getName(), keys,
				item, amount, buy, sell, takeMoney ? cost : 0, true, adminShop);

		ShopsChestCreateEvent event = new ShopsChestCreateEvent(shopChest, player);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		List<ShopChest> candidate = new ArrayList<>(shops.values());
		candidate.add(shopChest);

		for (BlockKey key : keys)
			byBlock.put(key, shopChest);

		new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);

		boolean costCharged = false;
		if (economy != null && !isZero && takeMoney) {
			try {
				costCharged = economy.withdraw(player, cost);
			} catch (RuntimeException error) {
				keys.forEach(byBlock::remove);
				plugin.getRosaLogger().log(Level.SEVERE,
						"Shop creation payment failed: player=" + player.getUniqueId() + " cost=" + cost
								+ " provider=" + economy.getName(), error);
				getMessage().sendPrefixed(player, Lang.CREATE_PAYMENT_FAILED);
				return;
			}
			if (!costCharged) {
				keys.forEach(byBlock::remove);
				getMessage().sendPrefixed(player, Lang.CREATE_PAYMENT_FAILED);
				return;
			}
		}
		boolean finalCostCharged = costCharged;
		dataManager.save(candidate, player, shopChest, () -> {
			shops.put(shopChest.getId(), shopChest);
			plugin.getDisplayManager().refresh();

			if (finalCostCharged)
				getMessage().message(Lang.WITHDRAW)
						.with(plugin.getShopMessages().placeholders(shopChest))
						.with("price", cost).sendPrefixed(player);
			getMessage().sendPrefixed(player, adminShop ? Lang.ADMIN_CREATED : Lang.SHOP_CREATED);
		}, () -> {
			keys.forEach(byBlock::remove);
			if (!finalCostCharged) return;
			try {
				if (!economy.deposit(player, cost)) {
					plugin.getRosaLogger().severe("Could not refund shop creation cost after save failure: player="
							+ player.getUniqueId() + " shop=" + shopChest.getId() + " cost=" + cost
							+ " provider=" + economy.getName());
					if (player.isOnline()) getMessage().sendPrefixed(player, Lang.CREATE_REFUND_FAILED);
				}
			} catch (RuntimeException error) {
				plugin.getRosaLogger().log(Level.SEVERE,
						"Could not refund shop creation cost after save failure: player=" + player.getUniqueId()
								+ " shop=" + shopChest.getId() + " cost=" + cost + " provider=" + economy.getName(), error);
				if (player.isOnline()) getMessage().sendPrefixed(player, Lang.CREATE_REFUND_FAILED);
			}
		});
	}

	public void remove(Player player, ShopChest shopChest) {
		if (busy(player)) return;

		if (!manages(player, shopChest)) {
			getMessage().sendPrefixed(player, Lang.NOT_YOUR_SHOP);
			return;
		}

		ShopsChestRemoveEvent event = new ShopsChestRemoveEvent(shopChest, player);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		List<ShopChest> candidate = new ArrayList<>(shops.values());
		candidate.remove(shopChest);

		boolean refund = plugin.getChestsFile().getBoolean("refund-shop-creation");
		EconomyHook economy = plugin.getEconomy();
		if (economy != null && refund && shopChest.getCostCharged() > 0) {
			if (economy.deposit(player, shopChest.getCostCharged())) {
				getMessage().message(Lang.CREATE_REFUND)
						.with(plugin.getShopMessages().placeholders(shopChest))
						.with("cost", shopChest.getCostCharged()).sendPrefixed(player);
			} else {
				plugin.getRosaLogger().severe("Could not refund shop creation cost after save failure: player="
						+ player.getUniqueId() + " shop=" + shopChest.getId() + " cost=" + shopChest.getCostCharged()
						+ " provider=" + economy.getName());
				if (player.isOnline()) getMessage().sendPrefixed(player, Lang.CREATE_REFUND_FAILED);
			}
		}

		dataManager.save(candidate, player, shopChest, () -> {
			shops.remove(shopChest.getId());
			shopChest.getBlocks().forEach(byBlock::remove);

			plugin.getDisplayManager().remove(shopChest);
			getMessage().sendPrefixed(player, Lang.SHOP_REMOVED);
		}, () -> plugin.getRosaLogger().warning("Failed to remove shop: " + shopChest.getId() + ", " + shopChest.getOwnerName()));
	}

	public boolean removeShopsOnExplosion(Collection<ShopChest> affectedShops) {
		if (affectedShops.isEmpty()) return true;

		Set<ShopChest> existing = new LinkedHashSet<>();
		for (ShopChest shopChest : affectedShops) {
			if (shops.get(shopChest.getId()) == shopChest) existing.add(shopChest);
		}
		if (existing.isEmpty()) return true;

		List<ShopChest> candidate = new ArrayList<>(shops.values());
		candidate.removeAll(existing);

		Set<UUID> ids = new HashSet<>();
		for (ShopChest shopChest : existing)
			ids.add(shopChest.getId());
		if (!dataManager.saveSystem(candidate, ids))
			return false;

		for (ShopChest shopChest : existing) {
			shops.remove(shopChest.getId());
			shopChest.getBlocks().forEach(byBlock::remove);
			plugin.getDisplayManager().remove(shopChest);
		}
		return true;
	}

	public void status(Player player, ShopChest shopChest, Boolean enabled) {
		if (busy(player)) return;

		if (!manages(player, shopChest)) {
			getMessage().sendPrefixed(player, Lang.NOT_YOUR_SHOP);
			return;
		}
		boolean after = enabled == null ? !shopChest.isEnabled() : enabled;
		ShopsChestStatusChangeEvent event = new ShopsChestStatusChangeEvent(shopChest, player, after);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) return;

		ShopChest updated = new ShopChest(shopChest.getId(), shopChest.getOwner(), shopChest.getOwnerName(), shopChest.getBlocks(),
				shopChest.getItem(), shopChest.getAmount(), shopChest.getBuyPrice(), shopChest.getSellPrice(), shopChest.getCostCharged(),
				after, shopChest.isAdminShop());

		List<ShopChest> candidate = new ArrayList<>(shops.values());
		candidate.remove(shopChest);
		candidate.add(updated);

		dataManager.save(candidate, player, shopChest, () -> {
			shopChest.setEnabled(after);
			plugin.getDisplayManager().refresh();
			getMessage().sendPrefixed(player, after ? Lang.SHOP_ENABLED : Lang.SHOP_DISABLED);
		}, () -> {
		});
	}

	public void edit(Player player, ShopChest shopChest) {
		if (canEdit(player, shopChest))
			new ShopEditGui(plugin, player, shopChest).show(plugin.getGuiManager(), player);
	}

	public boolean canEdit(Player player, ShopChest shopChest) {
		if (!player.hasPermission("advancedshops.edit")) {
			getMessage().sendPrefixed(player, Lang.NO_PERMISSION);
			return false;
		}
		if (!manages(player, shopChest)) {
			getMessage().sendPrefixed(player, Lang.NOT_YOUR_SHOP);
			return false;
		}
		if (shops.get(shopChest.getId()) != shopChest) {
			getMessage().sendPrefixed(player, Lang.EDIT_STALE);
			return false;
		}

		ShopsChestEditEvent event = new ShopsChestEditEvent(shopChest, player);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return false;
		}
		return !busy(player);
	}

	public void update(Player player, ShopChest shopChest, ItemStack item, int amount, double buy, double sell,
					   boolean enabled, Consumer<ShopChest> saved) {
		if (!canEdit(player, shopChest)) return;
		if (enabled != shopChest.isEnabled()) {
			ShopsChestStatusChangeEvent statusEvent = new ShopsChestStatusChangeEvent(shopChest, player, enabled);
			plugin.getServer().getPluginManager().callEvent(statusEvent);
			if (statusEvent.isCancelled()) return;
		}

		if (item == null || item.getType().name().equals("AIR")) {
			getMessage().sendPrefixed(player, Lang.HOLD_ITEM);
			return;
		}
		if (amount < 1 || amount > 2304) {
			getMessage().sendPrefixed(player, Lang.INVALID_AMOUNT);
			return;
		}
		try {
			ShopUtils.validatePrices(buy, sell);
			if (buy > 0) ShopUtils.parsePrice(Double.toString(buy));
			if (sell > 0) ShopUtils.parsePrice(Double.toString(sell));
		} catch (IllegalArgumentException error) {
			getMessage().sendPrefixed(player, Lang.INVALID_PRICE);
			return;
		}
		ShopChest updated = new ShopChest(shopChest.getId(), shopChest.getOwner(), shopChest.getOwnerName(), shopChest.getBlocks(),
				item, amount, buy, sell, shopChest.getCostCharged(), enabled, shopChest.isAdminShop());

		List<ShopChest> candidate = new ArrayList<>(shops.values());
		candidate.remove(shopChest);
		candidate.add(updated);

		dataManager.save(candidate, player, shopChest, () -> {
			shops.put(updated.getId(), updated);
			updated.getBlocks().forEach(key -> byBlock.put(key, updated));

			plugin.getDisplayManager().remove(shopChest);
			plugin.getDisplayManager().refresh();

			getMessage().sendPrefixed(player, Lang.EDIT_SAVED);
			saved.accept(updated);
		}, () -> {
		});
	}

	private boolean busy(Player player) {
		if (!dataManager.isBusy()) return false;

		getMessage().sendPrefixed(player, Lang.SAVE_BUSY);
		return true;
	}

	private MessageCatalog getMessage() {
		return plugin.getMessages();
	}
}
