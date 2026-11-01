package pl.kiosel.advancedshops.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.commands.subcommand.*;
import pl.kiosel.advancedshops.config.Lang;
import pl.kiosel.rosacore.command.RosaCommand;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.List;
import java.util.logging.Level;

public final class CommandShop extends RosaCommand {

	private final AdvancedShops plugin;

	public CommandShop(AdvancedShops plugin) {
		super(plugin, plugin.getCommandConfig().getCommandName(), plugin.getCommandConfig().getCommandAliases());
		this.plugin = plugin;
		reloadSubCommands();
	}

	public void reloadSubCommands() {
		setSubCommands(
				new CreateCommand(plugin),
				new ListCommand(plugin),
				new RemoveCommand(plugin),
				new InfoCommand(plugin),
				new EditCommand(plugin),
				new StatusCommand(plugin),
				new NotificationCommand(plugin),
				new CancelCommand(plugin),
				//new SettingsCommand(plugin),
				new ReloadCommand(plugin)
		);
	}

	@Override
	public boolean onExecute(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player)) {
			try {
				plugin.reloadRosaConfig();
				getMessage().sendPrefixed(sender, Lang.COMMAND_RELOAD);
			} catch (RuntimeException error) {
				plugin.getRosaLogger().log(Level.SEVERE, "Could not reload AdvancedShops configuration and messages", error);
				getMessage().sendPrefixed(sender, Lang.RELOAD_FAILED);
			}
			return true;
		}
		sendHelp(sender, label);
		return true;
	}

	@Override
	protected boolean onUnknownSubCommand(CommandSender sender, String label, String[] args) {
		getMessage().sendPrefixed(sender, Lang.COMMAND_UNKNOWN, "label", label);
		if (sender instanceof Player) sendHelp(sender, label);
		return true;
	}

	@Override
	public void sendHelp(CommandSender sender, String label) {
		getMessage().send(sender, Lang.HELP_HEADER);
		boolean any = false;
		for (RosaSubCommand command : getSubCommands()) {
			String permission = command.getPermission();
			if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) continue;
			if (command.isPlayerOnly() && !(sender instanceof Player)) continue;
			if (!command.isAvailable(sender) || !command.isVisible(sender)) continue;
			getMessage().send(sender, Lang.HELP_ENTRY, "usage", command.getUsage(), "description", command.getDescription());
			any = true;
		}
		if (!any) getMessage().send(sender, Lang.HELP_EMPTY);
		getMessage().send(sender, Lang.HELP_FOOTER);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String[] args) {
		return EMPTY;
	}
}
