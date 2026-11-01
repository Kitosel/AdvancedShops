package pl.kiosel.advancedshops.manager;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.api.events.ShopsSignCreateEvent;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.data.BlockKey;
import pl.kiosel.advancedshops.data.sign.ShopSign;
import pl.kiosel.rosacore.compatibility.ZColor;
import pl.kiosel.rosacore.hook.economy.EconomyHook;
import pl.kiosel.rosacore.scheduler.RosaTask;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class ShopSignManager implements AutoCloseable {

	private final AdvancedShops plugin;
	private final Map<String, Pulse> active = new HashMap<>();
	private boolean paymentFault;

	public ShopSignManager(AdvancedShops plugin) {
		this.plugin = plugin;
	}

	private boolean isEnabled() {
		return plugin.getSignsFile().getBoolean("enabled");
	}

	public String getSignHeader() {
		String header = plugin.getSignsFile().getString("sign-lines.header", "[PAY]");
		return (header == null ? "[PAY]" : header).replaceAll(";.*", "");
	}

	private String key(Block block) {
		return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
	}

	public boolean contains(Block block) {
		if (active.containsKey(key(block))) return true;
		if (!block.getType().name().endsWith("SIGN")) return false;
		BlockState state = block.getState();

		String pay = getSignHeader();

		return state instanceof Sign && pay.equalsIgnoreCase(ChatColor.stripColor(((Sign) state).getLine(0)).trim());
	}

	public boolean isProtected(Block block) {
		if (contains(block)) return true;
		for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
			Block adjacent = block.getRelative(face);
			if (contains(adjacent) && supportFace(adjacent) == face.getOppositeFace()) return true;
		}
		return false;
	}

	public boolean isProtected(Block block, Player player) {
		if (active.containsKey(key(block))) return true;
		if (!contains(block)) return isProtected(block);
		if (!plugin.getSignsFile().getBoolean("only-owner-can-destroy")
				|| player.hasPermission("advancedshops.sign.admin"))
			return false;

		String recipient = ChatColor.stripColor(((Sign) block.getState()).getLine(1)).trim();
		return !recipient.equalsIgnoreCase(player.getName());
	}

	private BlockFace supportFace(Block block) {
		Pulse pulse = active.get(key(block));
		BlockData data = pulse == null ? block.getBlockData() : pulse.shopSign.getSign().getBlockData();
		return data instanceof Directional ? ((Directional) data).getFacing().getOppositeFace() : BlockFace.DOWN;
	}

	public String[] create(Player player, Block block, String nick, String inputPrice) {
		if (!isEnabled()) {
			message(player, Lang.PAY_DISABLED);
			return null;
		}
		if (!player.hasPermission("advancedshops.sign.create")) {
			message(player, Lang.NO_PERMISSION);
			return null;
		}
		if (!plugin.getHookManager().getWorldGuard().isBuildAllowed(player, block.getLocation())) {
			message(player, Lang.PAY_PROTECTED);
			return null;
		}
		double price;
		try {
			price = ShopUtils.parsePrice(inputPrice);
		} catch (IllegalArgumentException error) {
			message(player, Lang.PAY_INVALID_PRICE);
			return null;
		}
		try {
			button(block);
		} catch (IllegalArgumentException error) {
			message(player, Lang.PAY_INVALID_SIGN);
			return null;
		}
		OfflinePlayer recipient = resolveRecipient(nick.trim());
		if (recipient == null) {
			message(player, Lang.PAY_UNKNOWN_PLAYER);
			return null;
		}
		boolean createOthers = plugin.getSignsFile().getBoolean("allow-create-for-others");
		if (createOthers && !player.getUniqueId().equals(recipient.getUniqueId()) && !player.hasPermission("advancedshops.sign.admin")) {
			message(player, Lang.PAY_OWN_ONLY);
			return null;
		}

		String realPrice = BigDecimal.valueOf(price).stripTrailingZeros().toPlainString();

		String headerLine = getLine("header", "[PAY];GREEN");
		String playerLine = getLine("player", "%player%").replace("%player%", recipient.getName());
		String amountLine = getLine("price", "%amount%;YELLOW").replace("%amount%", realPrice);
		String hintLine = getLine("hint", "Right-click");

		String[] lines = new String[]{
				headerLine, playerLine, amountLine, hintLine
		};
		Sign signState = (Sign) block.getState();
		for (int line = 0; line < lines.length; line++) signState.setLine(line, lines[line]);
		ShopSign shopSign = new ShopSign(signState, recipient.getName(), BlockKey.of(block), price,
				Math.max(1, Math.min(1200, plugin.getSignsFile().getInt("pulse-ticks"))));
		ShopsSignCreateEvent createEvent = new ShopsSignCreateEvent(shopSign, player);
		plugin.getServer().getPluginManager().callEvent(createEvent);
		if (createEvent.isCancelled()) return null;

		message(player, Lang.PAY_CREATED);
		return lines;
	}

	private String getLine(String path, String fallback) {
		String line = plugin.getSignsFile().getString("sign-lines." + path, fallback);
		String finalLine = (line == null) ? fallback : line;

		String[] parts = finalLine.split(";", 2);
		String cleanText = parts[0];

		if (parts.length > 1) {
			String colorStr = parts[1];
			ZColor zColor = ZColor.get(colorStr, null);

			if (zColor != null) {
				return zColor.getChatColor() + cleanText;
			}
		}
		return cleanText;
	}

	private OfflinePlayer resolveRecipient(String name) {
		Player online = Bukkit.getPlayerExact(name);
		if (online != null) return online;

		for (OfflinePlayer player : Bukkit.getOfflinePlayers())
			if (player.getName() != null && player.getName().equalsIgnoreCase(name)) return player;
		return null;
	}

	public void remove(Player player, Block block) {
		if (active.containsKey(key(block))) {
			message(player, Lang.PAY_BUSY);
			return;
		}
		if (!contains(block) || !(block.getState() instanceof Sign)) return;
		if (isProtected(block, player)) {
			message(player, Lang.PAY_PROTECTED);
			return;
		}
		block.breakNaturally();
		message(player, Lang.PAY_REMOVED);
	}

	public void use(Player player, Block block) {
		if (!isEnabled()) {
			message(player, Lang.PAY_DISABLED);
			return;
		}
		if (!player.hasPermission("advancedshops.sign.use")) {
			message(player, Lang.NO_PERMISSION);
			return;
		}

		String id = key(block);
		if (active.containsKey(id)) {
			message(player, Lang.PAY_BUSY);
			return;
		}
		if (paymentFault) {
			message(player, Lang.TRADING_HALTED);
			return;
		}
		if (!contains(block) || !(block.getState() instanceof Sign)) return;

		Sign original = (Sign) block.getState();
		ShopSign shopSign;

		try {
			shopSign = new ShopSign(original, ChatColor.stripColor(original.getLine(1)).trim(),
					BlockKey.of(block),
					ShopUtils.parsePrice(original.getLine(2)),
					Math.max(1, Math.min(1200, plugin.getSignsFile().getInt("pulse-ticks"))));
		} catch (IllegalArgumentException error) {
			message(player, Lang.PAY_INVALID_PRICE);
			return;
		}
		OfflinePlayer recipient = resolveRecipient(shopSign.getPlayer());
		if (recipient == null) {
			message(player, Lang.PAY_UNKNOWN_PLAYER);
			return;
		}
		if (player.getUniqueId().equals(recipient.getUniqueId())
				&& plugin.getSignsFile().getBoolean("owner-can-pay-yourself", false)) {
			message(player, Lang.PAY_SELF);
			return;
		}
		EconomyHook economy = plugin.getEconomy();
		if (economy == null) {
			message(player, Lang.ECONOMY_UNAVAILABLE);
			return;
		}
		double price = shopSign.getPrice();
		if (economy.getName().equalsIgnoreCase("PlayerPoints") && price != Math.rint(price)) {
			message(player, Lang.INTEGER_PRICE);
			return;
		}
		Switch button;
		try {
			button = button(block);
		} catch (IllegalArgumentException error) {
			message(player, Lang.PAY_INVALID_SIGN);
			return;
		}
		Pulse pulse = new Pulse(shopSign);
		active.put(id, pulse);

		boolean paid = false;
		try {
			pulse.task = plugin.getRosaScheduler().runAtLater(block.getLocation(), () -> finish(id), shopSign.getDelay());
			if (!economy.hasBalance(player, price)) {
				message(player, Lang.NO_MONEY);
				return;
			}
			if (!economy.withdraw(player, price)) {
				message(player, Lang.PAY_FAILED);
				return;
			}
			if (!economy.deposit(recipient, price)) {
				if (!economy.deposit(player, price)) throw new IllegalStateException("Shop sign refund was refused");
				message(player, Lang.PAY_FAILED);
				return;
			}
			paid = true;
			block.setBlockData(button, true);

			if (block.getType() != Material.STONE_BUTTON || !((Switch) block.getBlockData()).isPowered())
				throw new IllegalStateException("Shop sign button did not activate");
		} catch (RuntimeException error) {
			paymentFault = true;
			plugin.getRosaLogger().log(Level.SEVERE, "Shop sign failure: block=" + id + " payer=" + player.getName()
					+ " recipient=" + recipient.getName() + " price=" + price
					+ " provider=" + economy.getName() + " paid=" + paid, error);

			finish(id);
			if (paid) notifySignOwner(player, shopSign, recipient.getUniqueId());
			message(player, paid ? Lang.PAY_SIGNAL_FAILED : Lang.PAY_FAILED);
			return;
		} finally {
			if (!paid) finish(id);
		}

		try {
			plugin.getMessages().sendPrefixed(player, Lang.PAY_PAID, "recipient", shopSign.getPlayer(),
					"price", plugin.getShopMessages().money(price));
		} catch (RuntimeException error) {
			plugin.getRosaLogger().log(Level.WARNING, "Could not send shop sign receipt", error);
		}
		notifySignOwner(player, shopSign, recipient.getUniqueId());
	}

	private void notifySignOwner(Player customer, ShopSign sign, java.util.UUID ownerId) {
		try {
			plugin.getNotificationManager().notifyOwnerSign(customer, sign, ownerId);
		} catch (RuntimeException error) {
			plugin.getRosaLogger().log(Level.WARNING, "Could not send shop sign notification", error);
		}
	}

	private Switch button(Block block) {
		BlockData data = block.getBlockData();
		if (!(block.getState() instanceof Sign) || data.getMaterial().name().contains("HANGING")
				|| (data instanceof Waterlogged && ((Waterlogged) data).isWaterlogged()))
			throw new IllegalArgumentException("Use a dry standing or wall sign");

		Switch button = (Switch) Bukkit.createBlockData(Material.STONE_BUTTON);
		BlockFace support;

		if (data instanceof Directional) {
			BlockFace facing = ((Directional) data).getFacing();
			button.setFace(Switch.Face.WALL);
			button.setFacing(facing);
			support = facing.getOppositeFace();
		} else if (data instanceof Rotatable) {
			button.setFace(Switch.Face.FLOOR);
			button.setFacing(BlockFace.NORTH);
			support = BlockFace.DOWN;
		} else {
			throw new IllegalArgumentException("Unsupported sign");
		}
		if (!block.getRelative(support).getType().isOccluding())
			throw new IllegalArgumentException("Shop signs need a full solid supporting block");

		button.setPowered(true);
		return button;
	}

	private void finish(String id) {
		Pulse pulse = active.get(id);
		if (pulse == null) return;
		if (pulse.task != null) pulse.task.cancel();

		try {
			Sign original = pulse.shopSign.getSign();
			if (original.getBlock().getType() == Material.STONE_BUTTON && !original.update(true, true))
				throw new IllegalStateException("Could not restore shop sign " + id);
			active.remove(id);
		} catch (RuntimeException error) {
			paymentFault = true;
			plugin.getRosaLogger().log(Level.SEVERE, "Could not restore shop sign " + id, error);
		}
	}

	public void unloadChunk(Chunk chunk) {
		for (Map.Entry<String, Pulse> entry : new ArrayList<>(active.entrySet())) {
			Sign sign = entry.getValue().shopSign.getSign();
			if (sign.getWorld().equals(chunk.getWorld()) && (sign.getX() >> 4) == chunk.getX() && (sign.getZ() >> 4) == chunk.getZ())
				finish(entry.getKey());
		}
	}

	private void message(Player player, Lang key) {
		plugin.getMessages().sendPrefixed(player, key);
	}

	@Override
	public void close() {
		for (String id : new ArrayList<>(active.keySet()))
			finish(id);
	}

	private static final class Pulse {
		private final ShopSign shopSign;
		private RosaTask task;

		private Pulse(ShopSign shopSign) {
			this.shopSign = shopSign;
		}
	}
}
