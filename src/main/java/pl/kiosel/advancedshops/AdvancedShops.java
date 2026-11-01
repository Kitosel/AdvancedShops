package pl.kiosel.advancedshops;

import lombok.Getter;
import pl.kiosel.advancedshops.commands.CommandShop;
import pl.kiosel.advancedshops.config.*;
import pl.kiosel.advancedshops.listeners.*;
import pl.kiosel.advancedshops.manager.*;
import pl.kiosel.advancedshops.storage.ShopDataManager;
import pl.kiosel.advancedshops.storage.SqlShopStorage;
import pl.kiosel.advancedshops.storage.migrations._1_InitialMigration;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.compatibility.ZColor;
import pl.kiosel.rosacore.config.RosaConfig;
import pl.kiosel.rosacore.config.setting.RosaSettings;
import pl.kiosel.rosacore.database.DatabaseManager;
import pl.kiosel.rosacore.database.DatabaseSettings;
import pl.kiosel.rosacore.dependencies.metrics.bukkit.Metrics;
import pl.kiosel.rosacore.hook.economy.EconomyHook;

public final class AdvancedShops extends RosaPlugin {

	@Getter private CommandConfig commandConfig;
	@Getter private HologramConfig hologramConfig;
	private CommandShop shopCommand;

	@Getter private RosaConfig configFile;
	@Getter private RosaConfig databaseFile;
	@Getter private RosaConfig commandFile;
	@Getter private RosaConfig chestsFile;
	@Getter private RosaConfig signsFile;

	@Getter private ShopMessages shopMessages;
	@Getter private ShopDataManager dataManager;
	@Getter private ShopManager shopManager;
	@Getter private ShopSelectionManager selectionManager;
	@Getter private ShopTradeManager tradeManager;
	@Getter private ShopNotificationManager notificationManager;
	@Getter private ShopProtectionManager protectionManager;
	@Getter private ShopDisplayManager displayManager;

	@Getter private ShopSignManager shopSignManager;
	@Getter private DatabaseManager shopDatabase;

	@Getter private RosaSettings settings;

	@Override
	public void onPluginLoad() {
		this.configFile = loadConfig(ShopConfigFile.CONFIG.getPath());
		this.databaseFile = loadConfig(ShopConfigFile.DATABASE.getPath(), false);
		this.commandFile = loadConfig(ShopConfigFile.COMMAND.getPath());
		this.chestsFile = loadConfig(ShopConfigFile.CHEST.getPath());
		this.signsFile = loadConfig(ShopConfigFile.SIGNS.getPath());

		this.settings = loadSettings(this.configFile, Settings.SCHEMA);

		useNMS(false);
		setDev(false);
		setAsciColor(ZColor.WHITE);
	}

	@Override
	public void onPluginEnable() {
		getHookManager().start();
		setLocale(Settings.LANGUAGE.get(), "en_US");

		this.commandConfig = new CommandConfig(this);
		this.commandConfig.setConfig();

		this.shopMessages = new ShopMessages(this);
		this.hologramConfig = new HologramConfig(this);
		this.hologramConfig.setConfig();
		this.setEconomy();

		try {
			DatabaseSettings settings = DatabaseSettings.fromConfig(databaseFile.snapshot(), "", getDataFolder());
			this.shopDatabase = createDatabase(settings,
					getName().toLowerCase() + '_',
					new _1_InitialMigration(settings.getType()));
			dataManager = new ShopDataManager(this, new SqlShopStorage(this.shopDatabase));
			shopManager = new ShopManager(this, dataManager);
			shopManager.load();
		} catch (Exception error) {
			throw new IllegalStateException("Could not load shops. Check database.yml.", error);
		}

		this.shopSignManager = new ShopSignManager(this);
		this.notificationManager = new ShopNotificationManager(this);
		this.selectionManager = new ShopSelectionManager(this);
		this.tradeManager = new ShopTradeManager(this, dataManager);
		this.protectionManager = new ShopProtectionManager(shopManager);
		this.displayManager = new ShopDisplayManager(this, shopManager);

		this.registerRuntimeHandlers();
		this.displayManager.start();

		if (Settings.UPDATE.get()) {
			checkUpdates("advancedshops");
		}
		new Metrics(this, 34968);
	}

	@Override
	public void onPluginDisable() {
		if (this.selectionManager != null)
			this.selectionManager.clear();
		if (this.shopSignManager != null)
			runShutdownStep("restoring signs", shopSignManager::close);
		if (this.notificationManager != null)
			runShutdownStep("closing notification storage", notificationManager::close);
		if (this.displayManager != null)
			runShutdownStep("closing shop displays", displayManager::close);
		if (this.dataManager != null)
			runShutdownStep("finishing shop saves and closing storage", dataManager::close);
	}

	@Override
	public void onDataLoad() {
		this.setEconomy();
	}

	@Override
	public void onConfigReload() {
		this.settings.reload();
		this.hologramConfig.setConfig();

		setLocale(Settings.LANGUAGE.get(), "en_US");
		this.setEconomy();

		if (this.displayManager != null)
			this.displayManager.refresh();
		this.commandConfig.setConfig();

		if (this.shopCommand != null)
			this.shopCommand.reloadSubCommands();
	}

	private void registerRuntimeHandlers() {
		this.shopCommand = new CommandShop(this);

		registerCommand("advancedshops", this.shopCommand);
		registerListeners(
				new InteractListener(this),
				new InventoryListener(this),
				new BlockListener(this),
				new QuitListener(this),
				new JoinListener(this),
				new DisplayListener(this),
				new SignListener(this)
		);
	}

	public void setEconomy() {
		getHookManager().refresh();
		EconomyHook available = getEconomy();
		if (available == null) {
			getRosaLogger().warning("No economy provider is ready yet. Trading will become available when an economy provider registers.");
		} else if (!available.getName().equalsIgnoreCase(Settings.ECONOMY.get())) {
			getRosaLogger().warning("Economy '" + Settings.ECONOMY.get()
					+ "' is unavailable. Using " + available.getName() + ".");
		}
	}

	public EconomyHook getEconomy() {
		if (!getHookManager().getEconomy().setPreferredHook(Settings.ECONOMY.get()))
			getHookManager().getEconomy().clearPreferredHook();
		return getHookManager().getEconomy().getActiveHook().orElse(null);
	}
}
