package pl.kiosel.advancedshops.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.advancedshops.manager.ShopProtectionManager;
import pl.kiosel.rosacore.listener.RosaListener;

import java.util.ArrayList;
import java.util.List;

public final class BlockListener extends RosaListener {

	private final AdvancedShops plugin;
	private final ShopProtectionManager protection;

	public BlockListener(AdvancedShops plugin) {
		super(plugin);
		this.plugin = plugin;
		this.protection = plugin.getProtectionManager();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBreak(BlockBreakEvent event) {
		if (protection.isShop(event.getBlock())) {
			event.setCancelled(true);
			getMessage().message(Lang.PROTECTED_BREAK).with(plugin.getCommandConfig().placeholders()).sendPrefixed(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent event) {
		if (protection.isShop(event.getBlock())) {
			event.setCancelled(true);
			return;
		}
		if (protection.expandsShop(event.getBlock())) {
			event.setCancelled(true);
			getMessage().message(Lang.PROTECTED_EXPAND).with(plugin.getCommandConfig().placeholders()).sendPrefixed(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onExplosion(EntityExplodeEvent event) {
		handleExplosion(event.blockList());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockExplosion(BlockExplodeEvent event) {
		handleExplosion(event.blockList());
	}

	private void handleExplosion(List<Block> blocks) {
		List<Block> shopBlocks = new ArrayList<>();
		for (Block block : blocks) {
			if (protection.isShop(block)) shopBlocks.add(block);
		}
		if (shopBlocks.isEmpty()) return;

		if (!plugin.getChestsFile().getBoolean("remove-when-explode", false)) {
			blocks.removeAll(shopBlocks);
			return;
		}

		List<ShopChest> affectedShops = new ArrayList<>();
		for (Block block : shopBlocks) {
			ShopChest shop = plugin.getShopManager().getShopAt(block);
			if (shop != null && !affectedShops.contains(shop)) affectedShops.add(shop);
		}
		if (!plugin.getShopManager().removeShopsOnExplosion(affectedShops)) {
			blocks.removeAll(shopBlocks);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onExtend(BlockPistonExtendEvent event) {
		if (protection.deniesPiston(event.getBlocks(), event.getDirection())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRetract(BlockPistonRetractEvent event) {
		if (protection.deniesPiston(event.getBlocks(), event.getDirection())) event.setCancelled(true);
	}
}
