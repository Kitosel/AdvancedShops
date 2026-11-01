package pl.kiosel.advancedshops.listeners;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.rosacore.listener.RosaListener;

import java.util.Locale;

public final class InteractListener extends RosaListener {

	private final AdvancedShops plugin;

	public InteractListener(AdvancedShops plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock() == null) return;
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		if (event.useInteractedBlock() == Event.Result.DENY) return;

		Player player = event.getPlayer();
		Block block = event.getClickedBlock();

		if (player.getGameMode() == GameMode.SPECTATOR) return;

		if (event.getHand() != EquipmentSlot.HAND) {
			ShopChest shopChest = plugin.getShopManager().getShopAt(block);
			if (shopChest != null && !plugin.getProtectionManager().canAccessStorage(player, shopChest))
				event.setCancelled(true);
			return;
		}

		String configuredClick = plugin.getChestsFile().getString("buy-with-click", "RIGHT");
		ShopChest.BuyClick configClick;
		try {
			configClick = ShopChest.BuyClick.valueOf(
					(configuredClick == null ? "RIGHT" : configuredClick).toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException invalidConfig) {
			// Keep chest interaction usable when this optional config value is mistyped.
			configClick = ShopChest.BuyClick.RIGHT;
		}

		boolean click;
		if (configClick.equals(ShopChest.BuyClick.LEFT)) {
			click = event.getAction() == Action.LEFT_CLICK_BLOCK;
		} else {
			click = event.getAction() == Action.RIGHT_CLICK_BLOCK;
		}

		if (click && plugin.getSelectionManager().hasSelection(player)) {
			event.setCancelled(true);
			plugin.getSelectionManager().accept(player, block);
			return;
		}

		ShopChest shopChest = plugin.getShopManager().getShopAt(block);
		if (shopChest == null) return;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK
				&& plugin.getProtectionManager().canAccessStorage(player, shopChest)) return;

		event.setCancelled(true);
		plugin.getTradeManager().trade(player, shopChest, click, player.isSneaking());
	}
}
