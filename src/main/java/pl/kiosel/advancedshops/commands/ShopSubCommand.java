package pl.kiosel.advancedshops.commands;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.config.CommandLang;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.advancedshops.manager.ShopManager;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.function.BiConsumer;

public abstract class ShopSubCommand extends RosaSubCommand {

	protected final AdvancedShops plugin;
	private final CommandLang commandLang;
	@Getter private final ShopManager shopManager;

	protected ShopSubCommand(AdvancedShops plugin, CommandLang commandLang) {
		super(plugin);
		this.plugin = plugin;
		this.commandLang = commandLang;
		this.shopManager = plugin.getShopManager();
	}

	@Override
	public String getName() {
		return getCommand(commandLang);
	}

	@Override
	public String getDescription() {
		return getMessage().text("command.descriptions." + commandLang.getDef());
	}

	@Override
	public String getUsage() {
		return getMessage().text("command.usages." + commandLang.getDef(), plugin.getCommandConfig().placeholders());
	}

	@Override
	protected void sendUsage(CommandSender sender) {
		getMessage().sendPrefixed(sender, Lang.COMMAND_USAGE, "usage", getUsage());
	}

	@Override
	public String getNoPermissionMessage() {
		return getMessage().message(Lang.COMMAND_PERMISSION).prefixedLegacy();
	}

	@Override
	public String getPlayerOnlyMessage() {
		return getMessage().message(Lang.PLAYER_ONLY).prefixedLegacy();
	}

	@Override
	public String getUnavailableMessage() {
		return getMessage().message(Lang.COMMAND_UNAVAILABLE).prefixedLegacy();
	}

	@Override
	public String getUnknownSubCommandMessage() {
		return getMessage().format(Lang.COMMAND_UNKNOWN, "label", plugin.getCommandConfig().getCommandName()).prefixedLegacy();
	}

	@Override
	public boolean isPlayerOnly() {
		return true;
	}

	@Override
	public final void run(CommandSender sender, String[] args) {
		execute((Player) sender, args);
	}

	protected abstract void execute(Player player, String[] args);

	protected void selectShop(Player player, BiConsumer<Player, ShopChest> action) {
		plugin.getSelectionManager().select(player, getPermission(), (who, block) -> {
			ShopChest shopChest = plugin.getShopManager().getShopAt(block);
			if (shopChest == null) {
				getMessage().sendPrefixed(who, Lang.SHOP_NOT_BLOCK);
			} else {
				action.accept(who, shopChest);
			}
		});
	}

	protected String getCommand(CommandLang lang) {
		return plugin.getCommandConfig().getCommand(lang);
	}

	protected boolean getArgs(String args, CommandLang lang) {
		return args.equalsIgnoreCase(getCommand(lang));
	}
}
