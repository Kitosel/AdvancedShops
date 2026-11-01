package pl.kiosel.advancedshops.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.manager.ShopDisplayManager;
import pl.kiosel.rosacore.listener.RosaListener;

public final class DisplayListener extends RosaListener {

	private final ShopDisplayManager displays;

	public DisplayListener(AdvancedShops plugin) {
		super(plugin);
		this.displays = plugin.getDisplayManager();
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		displays.clean(event.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkUnload(ChunkUnloadEvent event) {
		displays.removeChunk(event.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		displays.removeWorld(event.getWorld());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		displays.forgetViewer(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPickup(EntityPickupItemEvent event) {
		if (displays.isModel(event.getItem())) event.setCancelled(true);
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onLegacyPickup(PlayerPickupItemEvent event) {
		if (displays.isModel(event.getItem())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onHopper(InventoryPickupItemEvent event) {
		if (displays.isModel(event.getItem())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDespawn(ItemDespawnEvent event) {
		if (displays.isModel(event.getEntity())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMerge(ItemMergeEvent event) {
		if (displays.isModel(event.getEntity()) || displays.isModel(event.getTarget())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDamage(EntityDamageEvent event) {
		if (displays.isModel(event.getEntity())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPortal(EntityPortalEvent event) {
		if (displays.isModel(event.getEntity())) event.setCancelled(true);
	}
}
