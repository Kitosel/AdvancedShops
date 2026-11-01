package pl.kiosel.advancedshops.config;

import lombok.Getter;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.rosacore.config.RosaConfig;
import pl.kiosel.rosacore.message.MessagePlaceholders;

import java.util.*;

public class CommandConfig {

	private final AdvancedShops plugin;

	@Getter private String commandName;
	@Getter private List<String> commandAliases;

	private Map<String, String> command = Collections.emptyMap();

	public CommandConfig(AdvancedShops plugin) {
		this.plugin = plugin;
	}

	public void setConfig() {
		RosaConfig config = plugin.getConfigFile();
		String name = normalize(config.getString("command.name", "advancedshop"));
		List<String> configuredAliases = config.contains("command.aliases")
				? config.getStringList("command.aliases") : Arrays.asList("shop", "shops");

		Set<String> aliases = new LinkedHashSet<>();
		for (String alias : configuredAliases) {
			String normalized = normalize(alias);
			if (!normalized.equals(name)) aliases.add(normalized);
		}

		Map<String, String> refreshed = new HashMap<>();
		Set<String> subcommands = new HashSet<>();
		for (CommandLang lang : CommandLang.values()) {
			String value = config.getString("command-language." + lang.getPath(), lang.getDef());

			if (lang == CommandLang.INFO && config.isLocallySet("command-language.information"))
				value = config.getString("command-language.information");
			String normalized = normalize(value);
			refreshed.put(lang.getPath(), normalized);
			if (lang != CommandLang.ADMIN && lang != CommandLang.ON && lang != CommandLang.OFF
					&& !subcommands.add(normalized))
				throw new IllegalArgumentException("Duplicate subcommand in command.yml: " + normalized);
		}

		if (refreshed.get(CommandLang.ON.getPath()).equals(refreshed.get(CommandLang.OFF.getPath())))
			throw new IllegalArgumentException("The on/off arguments in command.yml must be different");
		if (refreshed.get(CommandLang.ADMIN.getPath()).matches("[0-9]+"))
			throw new IllegalArgumentException("The admin argument in command.yml cannot be a page number");

		if (commandName == null) {
			commandName = name;
			commandAliases = Collections.unmodifiableList(new ArrayList<>(aliases));
		} else if (!commandName.equals(name) || !commandAliases.equals(new ArrayList<>(aliases))) {
			plugin.getRosaLogger().warning("Root command name and aliases changed in command.yml; restart to apply them.");
		}
		command = Collections.unmodifiableMap(refreshed);
	}

	public String getCommand(CommandLang lang) {
		String cmd = command.getOrDefault(lang.getPath(), lang.getDef());
		if (cmd == null || cmd.trim().isEmpty() || cmd.contains(" ")) {
			cmd = lang.getDef();
		}
		return cmd;
	}

	public MessagePlaceholders placeholders() {
		MessagePlaceholders values = MessagePlaceholders.pairs("root", commandName);
		for (CommandLang lang : CommandLang.values())
			values = values.with(lang.getDef(), getCommand(lang));
		return values;
	}

	public String commandLine(CommandLang lang) {
		return "/" + commandName + " " + getCommand(lang);
	}

	private String normalize(String value) {
		if (value == null || !value.trim().matches("[\\p{L}\\p{N}_-]+"))
			throw new IllegalArgumentException("Invalid command name in command.yml: " + value);
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
