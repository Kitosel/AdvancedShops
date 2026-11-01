package pl.kiosel.advancedshops.commands.subcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.ShopSubCommand;
import pl.kiosel.advancedshops.config.CommandLang;

public final class RemoveCommand extends ShopSubCommand {

	public RemoveCommand(AdvancedShops plugin) {
		super(plugin, CommandLang.REMOVE);
	}

	@Override
	public String getPermission() {
		return "advancedshops.remove";
	}

	@Override
	public boolean isAvailable(CommandSender sender) {
		return sender.hasPermission("advancedshops.admin") || getShopManager().playerHasShop((Player) sender);
	}

	@Override
	protected void execute(Player player, String[] args) {
		if (args.length != 0) {
			sendUsage(player);
			return;
		}
		selectShop(player, (who, shop) -> plugin.getShopManager().remove(who, shop));
	}
}
