package pl.kiosel.advancedshops.config;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.data.BlockKey;
import pl.kiosel.advancedshops.data.shop.InventoryTransfer;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.advancedshops.data.shop.ShopContainers;
import pl.kiosel.advancedshops.data.sign.ShopSign;
import pl.kiosel.rosacore.message.MessageCatalog;
import pl.kiosel.rosacore.message.MessagePlaceholders;

import java.util.List;
import java.util.Locale;

public final class ShopMessages {

	private final AdvancedShops plugin;

	public ShopMessages(AdvancedShops plugin) {
		this.plugin = plugin;
	}

	public void sendList(CommandSender sender, List<ShopChest> shopChests, boolean admin, int page) {
		if (shopChests.isEmpty()) {
			getMessages().message(admin ? Lang.LIST_ADMIN_EMPTY : Lang.LIST_EMPTY)
					.with(plugin.getCommandConfig().placeholders()).sendPrefixed(sender);
			return;
		}

		int pageSize = 5;
		int pages = (shopChests.size() - 1) / pageSize + 1;
		if (page < 1 || page > pages) {
			getMessages().sendPrefixed(sender, Lang.LIST_PAGE_MISSING, "pages", pages);
			return;
		}

		getMessages().send(sender, Lang.LIST_HEADER,
				"title", getMessages().text(admin ? Lang.LIST_TITLE_ADMIN : Lang.LIST_TITLE_OWN),
				"total", shopChests.size(), "page", page, "pages", pages);

		int start = (page - 1) * pageSize;
		for (int index = start; index < Math.min(start + pageSize, shopChests.size()); index++)
			sendListEntry(sender, shopChests.get(index), index + 1);

		if (page < pages) {
			String cmd = plugin.getCommandConfig().commandLine(CommandLang.LIST) + " "
					+ (admin ? plugin.getCommandConfig().getCommand(CommandLang.ADMIN) + " " : "") + (page + 1);
			getMessages().send(sender, Lang.LIST_NEXT, "command", cmd);
		}
		getMessages().send(sender, Lang.LIST_FOOTER);
	}

	public MessagePlaceholders placeholders(ShopChest shopChest) {
		BlockKey block = shopChest.getBlocks().get(0);
		World world = Bukkit.getWorld(block.getWorld());
		String worldName = world == null ? block.getWorld().toString() : world.getName();
		return MessagePlaceholders.pairs(
				"id", shopChest.getId(), "creator", shopChest.getOwnerName(),
				"owner", shopChest.isAdminShop() ? getMessages().text(Lang.LABEL_ADMIN) : shopChest.getOwnerName(),
				"type", getMessages().text(shopChest.isAdminShop() ? Lang.LABEL_ADMIN : Lang.LABEL_PLAYER),
				"status", getMessages().text(shopChest.isEnabled() ? Lang.STATE_OPEN : Lang.STATE_CLOSED),
				"right-click", getMessages().text(Lang.HOLOGRAM_RIGHT_CLICK),
				"left-click", getMessages().text(Lang.HOLOGRAM_LEFT_CLICK),
				"amount", shopChest.getAmount(), "item", shopChest.getItemName(),
				"cost-creation", shopChest.getCostCharged(),
				"buy", price(shopChest.getBuyPrice()), "sell", price(shopChest.getSellPrice()),
				"world", worldName, "x", block.getX(), "y", block.getY(), "z", block.getZ(),
				"location", getLocation(worldName, block));
	}

	private String getLocation(String worldName, BlockKey block) {
		return getMessages().text(Lang.LOCATION, "world", worldName,
				"x", block.getX(), "y", block.getY(), "z", block.getZ());
	}

	public MessagePlaceholders placeholders(ShopSign shopSign) {
		BlockKey block = shopSign.getBlock();
		World world = Bukkit.getWorld(block.getWorld());
		String worldName = world == null ? block.getWorld().toString() : world.getName();
		return MessagePlaceholders.pairs(
				"owner", shopSign.getPlayer(),
				"price", price(shopSign.getPrice()),
				"delay", price(shopSign.getDelay()),
				"world", worldName, "x", block.getX(), "y", block.getY(), "z", block.getZ(),
				"location", getMessages().text(Lang.LOCATION, "world", worldName,
						"x", block.getX(), "y", block.getY(), "z", block.getZ()));
	}

	public void sendInfo(CommandSender sender, ShopChest shopChest) {
		String stock;
		if (shopChest.isAdminShop()) {
			stock = getMessages().text(Lang.LABEL_INFINITE);
		} else {
			Inventory inventory = ShopContainers.resolve(shopChest);
			stock = inventory == null ? getMessages().text(Lang.LABEL_UNAVAILABLE)
					: String.valueOf(InventoryTransfer.count(inventory.getContents(), shopChest.getItem()));
		}
		getMessages().message(Lang.INFO).with(placeholders(shopChest).with("stock", stock)).send(sender);
	}

	public void sendListEntry(CommandSender sender, ShopChest shopChest, int index) {
		getMessages().message(Lang.LIST_ENTRY).with(placeholders(shopChest).with("index", index)).send(sender);
	}

	public List<String> getHologram(ShopChest shopChest) {
		return plugin.getHologramConfig().getLines(shopChest);
	}

	public String money(double amount) {
		Locale locale = Locale.forLanguageTag(getMessages().getLocale().replace('_', '-'));
		return getMessages().text(Lang.MONEY, "amount", String.format(locale, "%.2f", amount),
				"currency", plugin.getSettings().get(Settings.CURRENCY));
	}

	private MessageCatalog getMessages() {
		return plugin.getMessages();
	}

	private String price(double value) {
		return value > 0 ? money(value) : getMessages().text(Lang.LABEL_DISABLED);
	}
}
