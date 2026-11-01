package pl.kiosel.advancedshops.commands.subcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.ShopSubCommand;
import pl.kiosel.advancedshops.config.CommandLang;
import pl.kiosel.advancedshops.config.Settings;

import java.util.Arrays;
import java.util.List;

public class NotificationCommand extends ShopSubCommand {

	public NotificationCommand(AdvancedShops plugin) {
		super(plugin, CommandLang.NOTIFICATION);
	}

	@Override
	public String getPermission() {
		return "advancedshops.notifications";
	}

	@Override
	public boolean isAvailable(CommandSender sender) {
		return plugin.getSettings().get(Settings.NOTIFICATIONS_ENABLED)
				&& getShopManager().playerHasShop((Player) sender);
	}

	@Override
	protected void execute(Player player, String[] args) {
		if (args.length > 1 || (args.length == 1 && !getArgs(args[0], CommandLang.ON) && !getArgs(args[0], CommandLang.OFF))) {
			sendUsage(player);
			return;
		}
		plugin.getNotificationManager().toggle(player, args.length == 0 ? null : getArgs(args[0], CommandLang.ON));
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 1)
			return complete(args[0], Arrays.asList(getCommand(CommandLang.ON), getCommand(CommandLang.OFF)));
		return EMPTY;
	}
}
