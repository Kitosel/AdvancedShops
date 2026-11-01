package pl.kiosel.advancedshops.commands.subcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.ShopSubCommand;
import pl.kiosel.advancedshops.config.CommandLang;
import pl.kiosel.advancedshops.config.Lang;

public class CancelCommand extends ShopSubCommand {

	public CancelCommand(AdvancedShops plugin) {
		super(plugin, CommandLang.CANCEL);
	}

	@Override
	public String getPermission() {
		return "advancedshops.use";
	}

	@Override
	public boolean isAvailable(CommandSender sender) {
		return plugin.getSelectionManager().hasSelection((Player) sender);
	}

	@Override
	protected void execute(Player player, String[] args) {
		if (args.length != 0) {
			sendUsage(player);
			return;
		}
		plugin.getSelectionManager().cancel(player);
		getMessage().sendPrefixed(player, Lang.COMMAND_CANCEL);
	}
}
