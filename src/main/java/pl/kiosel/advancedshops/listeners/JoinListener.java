package pl.kiosel.advancedshops.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.config.Settings;
import pl.kiosel.rosacore.compatibility.ZSound;
import pl.kiosel.rosacore.dependencies.adventure.adventure.text.Component;
import pl.kiosel.rosacore.dependencies.adventure.adventure.text.event.ClickEvent;
import pl.kiosel.rosacore.dependencies.adventure.adventure.text.event.HoverEvent;
import pl.kiosel.rosacore.listener.RosaListener;

public final class JoinListener extends RosaListener {

	private final AdvancedShops plugin;

	public JoinListener(AdvancedShops plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		plugin.getNotificationManager().deliverPending(player);

		if (Settings.UPDATE.get()) {
			if (!player.isOp() || !player.hasPermission("advancedshops.admin")) return;

			plugin.getRosaScheduler().runForEntityLater(player, () -> {
				String newVersion = plugin.getNewPluginVersion();
				if (newVersion == null || plugin.getDescription().getVersion().equals(newVersion)) return;

				ZSound.ENTITY_PLAYER_LEVELUP.play(player, 0.4f, 2f);

				String download = getMessage().text(Lang.DOWNLOAD);
				String update = getMessage().message(Lang.UPDATE).with("version", newVersion).with("download", download).prefixedLegacy();
				String url = "https://modrinth.com/plugin/advancedshops";

				Component clickEvent = Component.text(update)
						.hoverEvent(HoverEvent.showText(Component.text(download)))
						.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, url));

				plugin.getMessenger().sendComponent(player, clickEvent);
			}, () -> {
			}, 20 * 2);
		}
	}
}
