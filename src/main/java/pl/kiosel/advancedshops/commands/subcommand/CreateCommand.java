package pl.kiosel.advancedshops.commands.subcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.ShopSubCommand;
import pl.kiosel.advancedshops.config.CommandLang;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.manager.ShopUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CreateCommand extends ShopSubCommand {

	public CreateCommand(AdvancedShops plugin) {
		super(plugin, CommandLang.CREATE);
	}

	@Override
	public String getPermission() {
		return "advancedshops.create";
	}

	@Override
	protected void execute(Player player, String[] args) {
		if (args.length != 3 && (args.length != 4 || !getArgs(args[3], CommandLang.ADMIN))) {
			sendUsage(player);
			return;
		}

		boolean adminShop = args.length == 4;
		if (adminShop && !player.hasPermission("advancedshops.adminshop")) {
			getMessage().sendPrefixed(player, Lang.COMMAND_PERMISSION);
			return;
		}

		int amount;
		try {
			amount = Integer.parseInt(args[0]);
			if (amount < 1 || amount > 2304) throw new NumberFormatException();
		} catch (NumberFormatException error) {
			getMessage().sendPrefixed(player, Lang.INVALID_AMOUNT);
			return;
		}

		try {
			double buy = ShopUtils.parsePrice(args[1]), sell = ShopUtils.parsePrice(args[2]);
			ShopUtils.validatePrices(buy, sell);
			ItemStack item = player.getInventory().getItemInMainHand().clone();

			if (item.getType().name().equals("AIR")) {
				getMessage().sendPrefixed(player, Lang.HOLD_ITEM);
				return;
			}
			plugin.getSelectionManager().select(player, adminShop ? "advancedshops.adminshop" : getPermission(),
					(who, block) -> getShopManager().create(who, block, item, amount, buy, sell, adminShop));
		} catch (IllegalArgumentException error) {
			getMessage().sendPrefixed(player, Lang.INVALID_PRICE);
			sendUsage(player);
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 1) {
			return complete(args[0], Arrays.asList("1", "16", "64"));
		}
		if (args.length <= 3) {
			return Arrays.asList("-", "10", "100");
		}
		if (args.length == 4 && sender.hasPermission("advancedshops.adminshop")) {
			return complete(args[3], Collections.singletonList(getCommand(CommandLang.ADMIN)));
		}
		return EMPTY;
	}
}
