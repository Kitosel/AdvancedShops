package pl.kiosel.advancedshops.config;

import lombok.Getter;

public enum ShopConfigFile {

	CONFIG("config.yml"),
	DATABASE("database.yml"),
	COMMAND("command.yml"),
	CHEST("chests.yml"),
	SIGNS("signs.yml");

	@Getter
	private final String path;

	ShopConfigFile(String path) {
		this.path = path;
	}

}
