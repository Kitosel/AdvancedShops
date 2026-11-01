package pl.kiosel.advancedshops.commands.subcommand;

import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.ShopSubCommand;
import pl.kiosel.advancedshops.config.CommandLang;

public final class SettingsCommand extends ShopSubCommand {

	public SettingsCommand(AdvancedShops plugin) {
		super(plugin, CommandLang.SETTINGS);
	}

	@Override
	public String getPermission() {
		return "advancedshops.settings";
	}

	@Override
	protected void execute(Player player, String[] args) {
		plugin.openConfigEditor(player);
	}
}
