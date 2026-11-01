package pl.kiosel.advancedshops.commands.subcommand;

import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.ShopSubCommand;
import pl.kiosel.advancedshops.config.CommandLang;
import pl.kiosel.advancedshops.config.Lang;

import java.util.logging.Level;

public final class ReloadCommand extends ShopSubCommand {

	public ReloadCommand(AdvancedShops plugin) {
		super(plugin, CommandLang.RELOAD);
	}

	@Override
	public String getPermission() {
		return "advancedshops.reload";
	}

	@Override
	protected void execute(Player player, String[] args) {
		try {
			plugin.reloadRosaConfig();
			getMessage().sendPrefixed(player, Lang.COMMAND_RELOAD);
		} catch (RuntimeException error) {
			plugin.getRosaLogger().log(Level.SEVERE, "Could not reload AdvancedShops configuration and messages", error);
			getMessage().sendPrefixed(player, Lang.RELOAD_FAILED);
		}
	}
}
