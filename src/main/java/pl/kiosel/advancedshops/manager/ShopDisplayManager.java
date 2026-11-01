package pl.kiosel.advancedshops.manager;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.data.BlockKey;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.advancedshops.data.shop.ShopContainers;
import pl.kiosel.advancedshops.data.shop.ShopDisplay;
import pl.kiosel.rosacore.scheduler.RosaTask;

import java.util.*;

public final class ShopDisplayManager implements AutoCloseable {

	public static final String MODEL_TAG = "advancedshops_model";
	private final AdvancedShops plugin;
	private final ShopManager shops;
	private final Map<UUID, ShopDisplay> displays = new HashMap<>();
	private RosaTask refreshTask;

	public ShopDisplayManager(AdvancedShops plugin, ShopManager shops) {
		this.plugin = plugin;
		this.shops = shops;
	}

	public void start() {
		if (refreshTask != null) return;
		if (!ShopDisplay.supportsPerPlayerVisibility())
			plugin.getRosaLogger().warning("This server API does not support per-player entity visibility; display-view-distance will have no effect.");
		cleanLoadedModels();
		refreshTask = plugin.getRosaScheduler().runGlobalTimer(this::refresh, 1L, 20L);
	}

	public void refresh() {
		Collection<? extends Player> viewers = new ArrayList<>(Bukkit.getOnlinePlayers());
		Set<UUID> onlineViewers = new HashSet<>();

		for (Player player : viewers)
			onlineViewers.add(player.getUniqueId());

		double viewDistance = plugin.getHologramConfig().getDisplayViewDistance();
		double viewDistanceSquared = viewDistance * viewDistance;

		for (ShopChest shopChest : shops.allShops()) {
			if (ShopContainers.resolve(shopChest) == null) {
				remove(shopChest);
				continue;
			}
			ShopDisplay display = displays.get(shopChest.getId());
			if (display == null) {
				display = new ShopDisplay(plugin, shopChest);
				displays.put(shopChest.getId(), display);
			}
			display.refresh();
			display.refreshVisibility(viewers, onlineViewers, viewDistanceSquared);
		}
	}

	public void forgetViewer(UUID viewerId) {
		for (ShopDisplay display : displays.values()) display.forgetViewer(viewerId);
	}

	public void remove(ShopChest shopChest) {
		ShopDisplay display = displays.remove(shopChest.getId());
		if (display != null) display.close();
	}

	public void removeChunk(Chunk chunk) {
		for (ShopChest shopChest : shops.allShops()) {
			for (BlockKey block : shopChest.getBlocks()) {
				if (block.getWorld().equals(chunk.getWorld().getUID())
						&& (block.getX() >> 4) == chunk.getX() && (block.getZ() >> 4) == chunk.getZ()) {
					remove(shopChest);
					break;
				}
			}
		}
	}

	public void removeWorld(World world) {
		for (ShopChest shopChest : shops.allShops()) {
			if (shopChest.getBlocks().get(0).getWorld().equals(world.getUID()))
				remove(shopChest);
		}
	}

	public void cleanLoadedModels() {
		for (World world : Bukkit.getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks())
				clean(chunk);
		}
	}

	public void clean(Chunk chunk) {
		for (Entity entity : chunk.getEntities()) {
			if (isModel(entity))
				entity.remove();
		}
	}

	public boolean isModel(Entity entity) {
		return entity.getScoreboardTags().contains(MODEL_TAG);
	}

	@Override
	public void close() {
		if (refreshTask != null) {
			refreshTask.cancel();
			refreshTask = null;
		}
		displays.values().forEach(ShopDisplay::close);
		displays.clear();
	}
}
