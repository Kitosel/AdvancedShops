package pl.kiosel.advancedshops.commands.subcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.ShopSubCommand;
import pl.kiosel.advancedshops.config.CommandLang;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.data.shop.ShopChest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ListCommand extends ShopSubCommand {

	public ListCommand(AdvancedShops plugin) {
		super(plugin, CommandLang.LIST);
	}

	@Override
	public String getPermission() {
		return "advancedshops.use";
	}

	@Override
	public boolean isAvailable(CommandSender sender) {
		return sender.hasPermission("advancedshops.admin") || getShopManager().playerHasShop((Player) sender);
	}

	@Override
	protected void execute(Player player, String[] args) {
		boolean admin = args.length > 0 && getArgs(args[0], CommandLang.ADMIN);
		if (admin && !player.hasPermission("advancedshops.admin")) {
			getMessage().sendPrefixed(player, Lang.COMMAND_PERMISSION);
			return;
		}
		int offset = admin ? 1 : 0;
		if (args.length > offset + 1) {
			sendUsage(player);
			return;
		}
		int page = 1;
		try {
			if (args.length > offset) page = Integer.parseInt(args[offset]);
			if (page < 1) throw new NumberFormatException();
		} catch (NumberFormatException error) {
			getMessage().sendPrefixed(player, Lang.LIST_INVALID_PAGE);
			sendUsage(player);
			return;
		}
		List<ShopChest> shopChests = admin ? getShopManager().findAdminShops()
				: getShopManager().findPlayerShops(player.getUniqueId());
		plugin.getShopMessages().sendList(player, shopChests, admin, page);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 1)
			return complete(args[0], sender.hasPermission("advancedshops.admin")
					? Arrays.asList(getCommand(CommandLang.ADMIN), "1") : Arrays.asList("1"));
		if (args.length == 2 && getArgs(args[0], CommandLang.ADMIN) && sender.hasPermission("advancedshops.admin"))
			return complete(args[1], Collections.singletonList("1"));
		return EMPTY;
	}
}
