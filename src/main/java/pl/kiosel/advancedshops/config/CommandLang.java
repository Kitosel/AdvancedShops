package pl.kiosel.advancedshops.config;

import lombok.Getter;

public enum CommandLang {

	CREATE("create", "create"),
	REMOVE("remove", "remove"),
	EDIT("edit", "edit"),
	INFO("info", "info"),
	STATUS("status", "status"),
	SETTINGS("settings", "settings"),
	LIST("list", "list"),
	RELOAD("reload", "reload"),
	CANCEL("cancel", "cancel"),
	NOTIFICATION("notification", "notification"),

	ADMIN("admin", "admin"),

	ON("on", "on"),
	OFF("off", "off");

	@Getter private final String path;
	@Getter private final String def;

	CommandLang(String path, String def) {
		this.path = path;
		this.def = def;
	}
}
