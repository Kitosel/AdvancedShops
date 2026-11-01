package pl.kiosel.advancedshops.data.shop;

import org.bukkit.Location;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.HologramConfig;
import pl.kiosel.advancedshops.data.BlockKey;
import pl.kiosel.advancedshops.manager.ShopDisplayManager;
import pl.kiosel.rosacore.hologram.RosaHologram;
import pl.kiosel.rosacore.version.Version;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ShopDisplay implements AutoCloseable {

	public static boolean supportsPerPlayerVisibility() {
		return PlayerEntityVisibility.isSupported();
	}

	private final AdvancedShops plugin;
	private final ShopChest shopChest;
	private final Location center;
	private final Map<UUID, Set<UUID>> hiddenEntities = new HashMap<>();
	private List<Entity> cachedDisplayEntities;
	private RosaHologram hologram;
	private Item model;

	public ShopDisplay(AdvancedShops plugin, ShopChest shopChest) {
		this.plugin = plugin;
		this.shopChest = shopChest;
		this.center = shopChest.getBlocks().get(0).getLocation().add(0.5, 0, 0.5);
		if (shopChest.getBlocks().size() == 2) {
			BlockKey a = shopChest.getBlocks().get(0), b = shopChest.getBlocks().get(1);
			this.center.add((b.getX() - a.getX()) / 2D, 0, (b.getZ() - a.getZ()) / 2D);
		}

	}

	private List<String> hologramLines() {
		return plugin.getShopMessages().getHologram(shopChest);
	}

	public void refresh() {
		HologramConfig config = plugin.getHologramConfig();
		List<String> lines = hologramLines();
		if (lines.isEmpty()) {
			if (hologram != null) {
				hologram.close();
				hologram = null;
				cachedDisplayEntities = null;
			}
		} else {
			double offset = config.getHologramHeight();
			if (Version.isServerVersionBelow(Version.V1_19_4)) offset += 1.0;
			Location location = center.clone().add(0, offset, 0);
			if (hologram == null) {
				hologram = new RosaHologram(plugin, location) {
					@Override
					protected void onUpdate() {
						setLines(hologramLines());
					}
				};
				hologram.setPlaceholderApiEnabled(false);
				hologram.setLineSpacing(config.getLineSpacing());
				hologram.show();
				cachedDisplayEntities = null;
			} else {
				if (!hologram.getLocation().equals(location)) {
					hologram.setLocation(location);
					cachedDisplayEntities = null;
				}
				if (hologram.getLineSpacing() != config.getLineSpacing()) {
					hologram.setLineSpacing(config.getLineSpacing());
					cachedDisplayEntities = null;
				}
				if (!hologram.getLines().equals(lines)) {
					hologram.setLines(lines);
					cachedDisplayEntities = null;
				}
			}
		}
		if (!config.isItemEnabled(shopChest)) {
			if (model != null) {
				model.remove();
				model = null;
				cachedDisplayEntities = null;
			}
			return;
		}
		Location location = center.clone().add(0, config.getItemHeight(), 0);
		if (model == null || !model.isValid() || model.isDead()) {
			ItemStack item = shopChest.getItem();
			item.setAmount(1);
			model = location.getWorld().dropItem(location, item);
			model.addScoreboardTag(ShopDisplayManager.MODEL_TAG);
			model.setGravity(false);
			model.setInvulnerable(true);
			model.setSilent(true);
			model.setPickupDelay(Integer.MAX_VALUE);
			try {
				Entity.class.getMethod("setPersistent", boolean.class).invoke(model, false);
			} catch (ReflectiveOperationException ignored) {
			}
			cachedDisplayEntities = null;
		}
		model.setVelocity(new Vector(0, 0, 0));
		model.setTicksLived(1);
		if (model.getLocation().distanceSquared(location) > 0.01) model.teleport(location);
	}

	public void refreshVisibility(Collection<? extends Player> viewers, Set<UUID> onlineViewers,
							  double viewDistanceSquared) {
		List<Entity> entities = displayEntities();
		if (entities.isEmpty()) {
			hiddenEntities.clear();
			return;
		}
		Set<UUID> entityIds = new HashSet<>();
		for (Entity entity : entities) entityIds.add(entity.getUniqueId());

		for (Player viewer : viewers) {
			boolean visible = viewDistanceSquared <= 0
					|| (viewer.getWorld().equals(center.getWorld())
					&& viewer.getLocation().distanceSquared(center) <= viewDistanceSquared);
			Set<UUID> hidden = hiddenEntities.get(viewer.getUniqueId());

			if (visible) {
				if (hidden == null) continue;
				for (Entity entity : entities) {
					if (hidden.contains(entity.getUniqueId()))
						PlayerEntityVisibility.setVisible(viewer, plugin, entity, true);
				}
				hiddenEntities.remove(viewer.getUniqueId());
				continue;
			}

			if (hidden == null) {
				hidden = new HashSet<>();
				hiddenEntities.put(viewer.getUniqueId(), hidden);
			} else {
				hidden.retainAll(entityIds);
			}
			for (Entity entity : entities) {
				if (!hidden.contains(entity.getUniqueId())) {
					PlayerEntityVisibility.setVisible(viewer, plugin, entity, false);
					hidden.add(entity.getUniqueId());
				}
			}
		}

		hiddenEntities.keySet().retainAll(onlineViewers);
	}

	private List<Entity> displayEntities() {
		if (cachedDisplayEntities != null) {
			boolean valid = true;
			for (Entity entity : cachedDisplayEntities) {
				if (!entity.isValid() || entity.isDead()) {
					valid = false;
					break;
				}
			}
			if (valid) return cachedDisplayEntities;
			cachedDisplayEntities = null;
		}

		List<Entity> entities = new ArrayList<>();
		if (model != null && model.isValid() && !model.isDead()) entities.add(model);
		if (hologram == null) {
			cachedDisplayEntities = entities;
			return cachedDisplayEntities;
		}
		if (!hologram.isSpawned()) return entities;

		Location location = hologram.getLocation();
		int chunkX = location.getBlockX() >> 4;
		int chunkZ = location.getBlockZ() >> 4;
		if (!location.getWorld().isChunkLoaded(chunkX, chunkZ)) return entities;

		Chunk chunk = location.getWorld().getChunkAt(chunkX, chunkZ);
		String hologramTag = "rosa_hologram_" + hologram.getId();
		for (Entity entity : chunk.getEntities()) {
			if (entity.getScoreboardTags().contains(hologramTag)) entities.add(entity);
		}
		cachedDisplayEntities = entities;
		return cachedDisplayEntities;
	}

	public void forgetViewer(UUID viewerId) {
		hiddenEntities.remove(viewerId);
	}

	@Override
	public void close() {
		if (hologram != null) hologram.close();
		if (model != null) model.remove();
		cachedDisplayEntities = null;
		hiddenEntities.clear();
	}
}
