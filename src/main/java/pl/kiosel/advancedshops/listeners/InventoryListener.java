package pl.kiosel.advancedshops.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.manager.ShopProtectionManager;
import pl.kiosel.rosacore.listener.RosaListener;

public final class InventoryListener extends RosaListener {

	private final ShopProtectionManager protection;

	public InventoryListener(AdvancedShops plugin) {
		super(plugin);
		this.protection = plugin.getProtectionManager();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onOpen(InventoryOpenEvent event) {
		if (protection.deniesAccess(event.getPlayer(), event.getInventory())) {
			event.setCancelled(true);
			getMessage().sendPrefixed(event.getPlayer(), Lang.PROTECTED_ACCESS);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onClick(InventoryClickEvent event) {
		if (protection.deniesAccess(event.getWhoClicked(), event.getInventory()))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDrag(InventoryDragEvent event) {
		if (protection.deniesAccess(event.getWhoClicked(), event.getInventory()))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onMove(InventoryMoveItemEvent event) {
		if (protection.deniesTransfer(event.getSource(), event.getDestination()))
			event.setCancelled(true);
	}
}
