package pl.kiosel.advancedshops.data.shop;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.manager.ShopUtils;
import pl.kiosel.rosacore.gui.Gui;
import pl.kiosel.rosacore.gui.GuiClickEvent;
import pl.kiosel.rosacore.input.ChatInput;
import pl.kiosel.rosacore.material.ItemCreator;

import java.util.List;

public final class ShopEditGui extends Gui {

	private final AdvancedShops plugin;
	private final Player player;
	private ShopChest shopChest;

	public ShopEditGui(AdvancedShops plugin, Player player, ShopChest shopChest) {
		super(1);
		this.plugin = plugin;
		this.player = player;
		this.shopChest = shopChest;

		playSoundOnClick(true);
		setTitle(plugin.getMessages().text(Lang.EDIT_TITLE));

		buildButtons();
	}

	private void buildButtons() {
		setButton(1, button(shopChest.getItem(), "item"), event -> chooseItem());
		setButton(3, button(new ItemStack(Material.PAPER), "amount"), event -> changeAmount(event.getPlayer()));
		setButton(5, button(new ItemStack(Material.GOLD_INGOT), "price"), event -> choosePrice());
		setButton(7, button(new ItemStack(shopChest.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE), "enabled"),
				event -> {
					save(shopChest.getItem(), shopChest.getAmount(), shopChest.getBuyPrice(), shopChest.getSellPrice(), !shopChest.isEnabled());
				});
	}

	private void chooseItem() {
		Gui pick = new Gui(1, this) {
			@Override
			protected boolean onClickPlayerInventory(GuiClickEvent event) {
				if (event.getClickType() != ClickType.LEFT) return false;

				ItemStack item = event.getClickedItem();
				if (item != null && item.getType() != Material.AIR)
					save(item, shopChest.getAmount(), shopChest.getBuyPrice(), shopChest.getSellPrice(), shopChest.isEnabled());
				return true;
			}
		};
		pick.setTitle(plugin.getMessages().text(Lang.EDIT_ITEM_TITLE));
		pick.setItem(4, button(shopChest.getItem(), "item"));
		pick.show(plugin.getGuiManager(), player);
	}

	private void changeAmount(Player player) {
		plugin.getMessages().sendPrefixed(player, Lang.EDIT_AMOUNT_PROMPT);

		ChatInput.showPrompt(plugin, player, (event) -> {
			int amount;
			try {
				amount = Integer.parseInt(event.getMessage().trim());
			} catch (NumberFormatException error) {
				reopenSync(player);
				plugin.getMessages().sendPrefixed(player, Lang.INVALID_AMOUNT);
				return;
			}

			plugin.getServer().getScheduler().runTask(plugin, () -> {
				saveDirectly(shopChest.getItem(), amount, shopChest.getBuyPrice(), shopChest.getSellPrice(), shopChest.isEnabled());
			});
		}).onCancel(() -> {
			reopenSync(player);
		}).onClose(() -> {
			reopenSync(player);
		}).setTimeOutMessage(plugin.getMessages().message(Lang.EDIT_TIMEOUT)
		).setTimeout(30 * 20, true);
	}

	private void choosePrice() {
		Gui prices = new Gui(1) {
		};
		prices.setTitle(plugin.getMessages().text(Lang.EDIT_PRICE_TITLE));
		prices.setButton(2, button(new ItemStack(Material.EMERALD), "buy"), event -> {
			event.getGui().close();
			changePrice(true);
		});
		prices.setButton(6, button(new ItemStack(Material.GOLD_INGOT), "sell"), event -> {
			event.getGui().close();
			changePrice(false);
		});
		prices.show(plugin.getGuiManager(), player);
	}

	private void changePrice(boolean buying) {
		plugin.getMessages().sendPrefixed(player, Lang.EDIT_PRICE_PROMPT);
		ChatInput.showPrompt(plugin, player, (event) -> {
			double changed;
			try {
				changed = ShopUtils.parsePrice(event.getMessage().trim());
			} catch (IllegalArgumentException error) {
				reopenSync(player);
				plugin.getMessages().sendPrefixed(player, Lang.INVALID_PRICE);
				return;
			}

			double newBuy = buying ? changed : shopChest.getBuyPrice();
			double newSell = buying ? shopChest.getSellPrice() : changed;

			plugin.getServer().getScheduler().runTask(plugin, () -> {
				saveDirectly(shopChest.getItem(), shopChest.getAmount(), newBuy, newSell, shopChest.isEnabled());
			});
		}).onCancel(() -> reopenSync(player)
		).onClose(() -> reopenSync(player)
		).setTimeOutMessage(plugin.getMessages().message(Lang.EDIT_TIMEOUT)
		).setTimeout(30 * 20, true);
	}

	private void save(ItemStack item, int amount, double buy, double sell, boolean enabled) {
		Gui gui = plugin.getGuiManager().getOpenGui(player);
		if (gui == null) return;

		saveDirectly(item, amount, buy, sell, enabled);
	}

	private void saveDirectly(ItemStack item, int amount, double buy, double sell, boolean enabled) {
		plugin.getShopManager().update(player, shopChest, item, amount, buy, sell, enabled, updated -> {
			if (!player.isOnline()) return;

			this.shopChest = updated;
			buildButtons();

			reopenSync(player);
		});
	}

	private void reopenSync(Player player) {
		plugin.getServer().getScheduler().runTask(plugin, () -> this.show(plugin.getGuiManager(), player));
	}

	private ItemStack button(ItemStack icon, String key) {
		String name = plugin.getMessages().text("edit.buttons." + key + ".name", plugin.getShopMessages().placeholders(shopChest));
		List<String> lore = plugin.getMessages().message("edit.buttons." + key + ".lore")
				.with(plugin.getShopMessages().placeholders(shopChest)).legacyLines();

		return ItemCreator.of(icon).name(name).lore(lore).makeMenuItem();
	}
}
