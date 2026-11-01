package pl.kiosel.advancedshops.manager;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.Lang;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class ShopSelectionManager {

	private final AdvancedShops plugin;
	private static final int TIMEOUT_SECONDS = 30;
	private final Map<UUID, Selection> selections = new HashMap<>();

	public ShopSelectionManager(AdvancedShops plugin) {
		this.plugin = plugin;
	}

	public void select(Player player, String permission, BiConsumer<Player, Block> action) {
		selections.put(player.getUniqueId(), new Selection(permission, action));
		plugin.getMessages().message(Lang.SELECT_CONTAINER)
				.with(plugin.getCommandConfig().placeholders())
				.with("seconds", TIMEOUT_SECONDS).sendPrefixed(player);
	}

	public boolean hasSelection(Player player) {
		Selection selection = selections.get(player.getUniqueId());
		if (selection != null && selection.expires < System.currentTimeMillis()) {
			cancel(player);
			return false;
		}
		return selection != null;
	}

	public void accept(Player player, Block block) {
		if (!hasSelection(player)) return;

		Selection selection = selections.remove(player.getUniqueId());
		if (!player.hasPermission(selection.permission)) {
			plugin.getMessages().sendPrefixed(player, Lang.NO_PERMISSION);
			return;
		}
		selection.action.accept(player, block);
	}

	public void cancel(Player player) {
		selections.remove(player.getUniqueId());
	}

	public void clear() {
		selections.clear();
	}

	private static final class Selection {

		private final String permission;
		private final BiConsumer<Player, Block> action;
		private final long expires = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000L;

		private Selection(String permission, BiConsumer<Player, Block> action) {
			this.permission = permission;
			this.action = action;
		}
	}
}
