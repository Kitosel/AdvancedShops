package pl.kiosel.advancedshops.config;

import pl.kiosel.rosacore.config.setting.SettingKey;
import pl.kiosel.rosacore.config.setting.SettingsSchema;

public final class Settings {

	private Settings() {
	}

	public static final SettingKey<String> LANGUAGE = SettingKey.string("settings.language", "en_US").build();
	public static final SettingKey<String> ECONOMY = SettingKey.string("settings.economy", "Vault").build();
	public static final SettingKey<String> CURRENCY = SettingKey.string("settings.currency", "$").build();
	public static final SettingKey<Boolean> UPDATE = SettingKey.booleanValue("settings.update-check", true).build();

	public static final SettingKey<Boolean> NOTIFICATIONS_ENABLED = SettingKey.booleanValue("notifications.enabled", true).build();
	public static final SettingKey<Boolean> NOTIFICATIONS_DEFAULT = SettingKey.booleanValue("notifications.default-enabled", true).build();
	public static final SettingKey<Boolean> NOTIFICATIONS_BUY = SettingKey.booleanValue("notifications.customer-buys", true).build();
	public static final SettingKey<Boolean> NOTIFICATIONS_SELL = SettingKey.booleanValue("notifications.customer-sells", true).build();
	public static final SettingKey<Boolean> NOTIFICATIONS_SIGN = SettingKey.booleanValue("notifications.customer-sign", true).build();

	public static final SettingsSchema SCHEMA = SettingsSchema.builder()
			.addAll(
					//basics settings idk
					LANGUAGE, ECONOMY, CURRENCY, UPDATE,
					//notifications
					NOTIFICATIONS_ENABLED, NOTIFICATIONS_DEFAULT,
					NOTIFICATIONS_BUY, NOTIFICATIONS_SELL, NOTIFICATIONS_SIGN
			).build();
}
