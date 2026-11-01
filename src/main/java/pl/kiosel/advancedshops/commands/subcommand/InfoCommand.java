package pl.kiosel.advancedshops.commands.subcommand;

import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.ShopSubCommand;
import pl.kiosel.advancedshops.config.CommandLang;

public final class InfoCommand extends ShopSubCommand {

	public InfoCommand(AdvancedShops plugin) {
		super(plugin, CommandLang.INFO);
	}

	@Override
	public String getPermission() {
		return "advancedshops.use";
	}

	@Override
	protected void execute(Player player, String[] args) {
		if (args.length != 0) {
			sendUsage(player);
			return;
		}
		selectShop(player, (who, shop) -> plugin.getShopMessages().sendInfo(who, shop));
	}
}
