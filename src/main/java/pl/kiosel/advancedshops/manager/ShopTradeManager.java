package pl.kiosel.advancedshops.manager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.advancedshops.data.shop.ShopContainers;
import pl.kiosel.advancedshops.data.shop.TradeQuote;
import pl.kiosel.advancedshops.storage.ShopDataManager;
import pl.kiosel.rosacore.hook.economy.EconomyHook;
import pl.kiosel.rosacore.message.MessageCatalog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ShopTradeManager {

	private final AdvancedShops plugin;
	private final ShopDataManager dataManager;
	private final Map<UUID, Long> lastTrade = new HashMap<>();
	private final TradeService trades = new TradeService();
	private boolean trading, economyFault;

	public ShopTradeManager(AdvancedShops plugin, ShopDataManager dataManager) {
		this.plugin = plugin;
		this.dataManager = dataManager;
	}

	public void trade(Player customer, ShopChest shopChest, boolean buying) {
		trade(customer, shopChest, buying, false);
	}

	public void trade(Player customer, ShopChest shopChest, boolean buying, boolean fullStack) {
		if (dataManager.isPending(shopChest.getId())) {
			getMessage().sendPrefixed(customer, Lang.TRADE_SAVE_BUSY);
			return;
		}

		if (!customer.hasPermission("advancedshops.use")) {
			getMessage().sendPrefixed(customer, Lang.NO_PERMISSION);
			return;
		}

		if (!shopChest.isAdminShop() && shopChest.getOwner().equals(customer.getUniqueId())) {
			return;
		}

		if (!shopChest.isEnabled()) {
			getMessage().sendPrefixed(customer, Lang.SHOP_CLOSED);
			return;
		}

		double price = buying ? shopChest.getBuyPrice() : shopChest.getSellPrice();
		if (price <= 0) {
			getMessage().sendPrefixed(customer, buying ? Lang.BUY_DISABLED : Lang.SELL_DISABLED);
			return;
		}

		TradeQuote quote = TradeQuote.of(shopChest, buying, fullStack);
		price = quote.getPrice();
		if (!Double.isFinite(price) || price <= 0) {
			getMessage().sendPrefixed(customer, Lang.TRADE_INVALID_TOTAL);
			return;
		}

		long now = System.currentTimeMillis();
		long delay = Math.max(0, plugin.getChestsFile().getLong("trade-cooldown-ms", 150));
		if (now - lastTrade.getOrDefault(customer.getUniqueId(), 0L) < delay || trading) return;

		lastTrade.put(customer.getUniqueId(), now);
		if (economyFault) {
			getMessage().sendPrefixed(customer, Lang.TRADING_HALTED);
			return;
		}

		Inventory storage = ShopContainers.resolve(shopChest);
		if (storage == null) {
			getMessage().sendPrefixed(customer, Lang.STORAGE_UNAVAILABLE);
			return;
		}

		EconomyHook economy = plugin.getEconomy();
		if (economy == null) {
			getMessage().sendPrefixed(customer, Lang.ECONOMY_UNAVAILABLE);
			return;
		}

		if (economy.getName().equalsIgnoreCase("PlayerPoints") && price != Math.rint(price)) {
			getMessage().sendPrefixed(customer, Lang.INTEGER_PRICE);
			return;
		}

		trading = true;
		completeTrade(customer, shopChest, economy, buying, quote);
	}

	private void completeTrade(Player customer, ShopChest shopChest, EconomyHook economy, boolean buying, TradeQuote quote) {
		double price = quote.getPrice();
		TradeService.Result result;
		try {
			Inventory storage = ShopContainers.resolve(shopChest);
			if (!customer.isOnline()
					|| !customer.hasPermission("advancedshops.use")
					|| !shopChest.isEnabled()
					|| dataManager.isPending(shopChest.getId()) || storage == null
					|| plugin.getShopManager().getShopAt(shopChest.getBlocks().get(0).resolve()) != shopChest) {
				if (customer.isOnline())
					getMessage().sendPrefixed(customer, Lang.TRADE_STALE);
				return;
			}
			result = trades.trade(shopChest, customer, storage, economy, buying, quote);
		} catch (RuntimeException error) {
			economyFault = true;
			plugin.getRosaLogger().log(Level.SEVERE, String.format(
					"Trading halted; reconcile shop=%s owner=%s customer=%s buying=%s price=%s provider=%s",
					shopChest.getId(), shopChest.getOwner(), customer.getUniqueId(), buying, price, economy.getName()
			), error);

			getMessage().sendPrefixed(customer, Lang.TRADE_FAILED);
			return;
		} finally {
			trading = false;
		}
		if (result == TradeService.Result.SUCCESS) {
			try {
				plugin.getNotificationManager().notifyOwnerChest(customer, shopChest, buying, quote);
			} catch (RuntimeException error) {
				plugin.getRosaLogger().log(Level.WARNING, "Could not notify shop owner " + shopChest.getOwner(), error);
			}
		}
		try {
			switch (result) {
				case SUCCESS:
					getMessage().sendPrefixed(customer, buying ? Lang.TRADE_BOUGHT : Lang.TRADE_SOLD,
							"amount", quote.getAmount(), "item", shopChest.getItemName(), "price", plugin.getShopMessages().money(price));
					plugin.getDebug().debug(String.format(
							"TRADE shop=%s owner=%s admin=%s customer=%s buying=%s amount=%s price=%s",
							shopChest.getId(), shopChest.getOwner(), shopChest.isAdminShop(), customer.getUniqueId(), buying, quote.getAmount(), price
					));
					break;
				case NO_STOCK:
					getMessage().sendPrefixed(customer, buying ? Lang.SHOP_NO_STOCK : Lang.PLAYER_NO_STOCK);
					break;
				case NO_SPACE:
					getMessage().sendPrefixed(customer, buying ? Lang.PLAYER_NO_SPACE : Lang.SHOP_NO_SPACE);
					break;
				case NO_MONEY:
					if (buying) {
						getMessage().sendPrefixed(customer, Lang.NO_MONEY);
					} else {
						getMessage().sendPrefixed(customer, Lang.OWNER_NO_MONEY);
					}
					break;
				default:
					getMessage().sendPrefixed(customer, Lang.DECLINED);
			}
		} catch (RuntimeException error) {
			plugin.getRosaLogger().log(Level.WARNING, "Could not display trade result for shop " + shopChest.getId(), error);
		}
	}

	public void forget(Player player) {
		lastTrade.remove(player.getUniqueId());
	}

	private MessageCatalog getMessage() {
		return plugin.getMessages();
	}
}
