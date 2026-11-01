package pl.kiosel.advancedshops.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.api.events.ShopsSignRemoveEvent;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.data.BlockKey;
import pl.kiosel.advancedshops.data.sign.ShopSign;
import pl.kiosel.advancedshops.manager.ShopSignManager;
import pl.kiosel.advancedshops.manager.ShopUtils;
import pl.kiosel.rosacore.listener.RosaListener;

public final class SignListener extends RosaListener {

	private final AdvancedShops plugin;
	private final ShopSignManager signs;

	public SignListener(AdvancedShops plugin) {
		super(plugin);
		this.plugin = plugin;
		this.signs = plugin.getShopSignManager();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEdit(SignChangeEvent event) {
		if (signs.contains(event.getBlock())) {
			event.setCancelled(true);
			getMessage().sendPrefixed(event.getPlayer(), Lang.PAY_PROTECTED);
			return;
		}
		String header = ChatColor.stripColor(event.getLine(0));
		String pay = plugin.getShopSignManager().getSignHeader();
		if (header == null || !header.trim().equalsIgnoreCase(pay)) return;

		event.setCancelled(true);
		try {
			Object side = event.getClass().getMethod("getSide").invoke(event);
			if (!side.toString().equals("FRONT")) {
				getMessage().sendPrefixed(event.getPlayer(), Lang.PAY_FRONT_ONLY);
				return;
			}
		} catch (NoSuchMethodException ignored) {
		} catch (ReflectiveOperationException error) {
			throw new IllegalStateException("Could not determine sign side", error);
		}
		String[] lines = signs.create(event.getPlayer(), event.getBlock(), event.getLine(1), event.getLine(2));
		if (lines != null) {
			for (int i = 0; i < 4; i++) event.setLine(i, lines[i]);
			event.setCancelled(false);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block block = event.getClickedBlock();
		if (block == null || !signs.contains(block)) return;
		boolean denied = event.useInteractedBlock() == Event.Result.DENY;

		event.setCancelled(true);
		if (denied || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		signs.use(event.getPlayer(), block);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBreak(BlockBreakEvent event) {
		if (!signs.isProtected(event.getBlock())) return;
		event.setCancelled(true);

		if (signs.contains(event.getBlock())) {
			Block block = event.getBlock();
			Sign sign = (Sign) block.getState();
			double price;
			try {
				price = ShopUtils.parsePrice(sign.getLine(2));
			} catch (IllegalArgumentException malformedPrice) {
				price = 0;
			}
			ShopSign shopSign = new ShopSign(sign, ChatColor.stripColor(sign.getLine(1)).trim(),
					BlockKey.of(block), price,
					Math.max(1, Math.min(1200, plugin.getSignsFile().getInt("pulse-ticks"))));

			ShopsSignRemoveEvent removeEvent = new ShopsSignRemoveEvent(shopSign, event.getPlayer());
			plugin.getServer().getPluginManager().callEvent(removeEvent);
			if (removeEvent.isCancelled()) {
				event.setCancelled(true);
				return;
			}
			boolean ownerDestroy = plugin.getSignsFile().getBoolean("only-owner-can-destroy");
			if (!shopSign.getPlayer().equals(event.getPlayer().getName()) && !ownerDestroy) {
				getMessage().sendPrefixed(event.getPlayer(), Lang.PAY_OWN_DESTROY);
				event.setCancelled(true);
				return;
			}

			signs.remove(event.getPlayer(), event.getBlock());
		} else {
			getMessage().sendPrefixed(event.getPlayer(), Lang.PAY_PROTECTED);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent event) {
		if (signs.isProtected(event.getBlock())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onExplosion(EntityExplodeEvent event) {
		event.blockList().removeIf(signs::isProtected);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onExplosion(BlockExplodeEvent event) {
		event.blockList().removeIf(signs::isProtected);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onExtend(BlockPistonExtendEvent event) {
		if (event.getBlocks().stream().anyMatch(block -> signs.isProtected(block) || signs.isProtected(block.getRelative(event.getDirection()))))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRetract(BlockPistonRetractEvent event) {
		if (event.getBlocks().stream().anyMatch(block -> signs.isProtected(block) || signs.isProtected(block.getRelative(event.getDirection()))))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFlow(BlockFromToEvent event) {
		if (signs.isProtected(event.getToBlock())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBurn(BlockBurnEvent event) {
		if (signs.isProtected(event.getBlock())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFade(BlockFadeEvent event) {
		if (signs.isProtected(event.getBlock())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityChange(EntityChangeBlockEvent event) {
		if (signs.isProtected(event.getBlock())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPhysics(BlockPhysicsEvent event) {
		if (signs.contains(event.getBlock())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkUnload(ChunkUnloadEvent event) {
		signs.unloadChunk(event.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		for (Chunk chunk : event.getWorld().getLoadedChunks())
			signs.unloadChunk(chunk);
	}
}
