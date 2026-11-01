package pl.kiosel.advancedshops.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.rosacore.listener.RosaListener;

public final class QuitListener extends RosaListener {

	private final AdvancedShops plugin;

	public QuitListener(AdvancedShops plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		plugin.getSelectionManager().cancel(event.getPlayer());
		plugin.getTradeManager().forget(event.getPlayer());
		plugin.getNotificationManager().recordLogout(event.getPlayer());
	}
}
