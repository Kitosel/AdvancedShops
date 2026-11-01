package pl.kiosel.advancedshops.commands.subcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.ShopSubCommand;
import pl.kiosel.advancedshops.config.CommandLang;

import java.util.Arrays;
import java.util.List;

public final class StatusCommand extends ShopSubCommand {

	public StatusCommand(AdvancedShops plugin) {
		super(plugin, CommandLang.STATUS);
	}

	@Override
	public String getPermission() {
		return "advancedshops.status";
	}

	@Override
	protected void execute(Player player, String[] args) {
		if (args.length > 1 || (args.length == 1 && !getArgs(args[0], CommandLang.ON) && !getArgs(args[0], CommandLang.OFF))) {
			sendUsage(player);
			return;
		}
		Boolean enabled = args.length == 0 ? null : getArgs(args[0], CommandLang.ON);
		selectShop(player, (who, shop) -> getShopManager().status(who, shop, enabled));
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 1) {
			return complete(args[0], Arrays.asList(getCommand(CommandLang.ON), getCommand(CommandLang.OFF)));
		}
		return EMPTY;
	}
}
