package pl.kiosel.advancedshops.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.advancedshops.AdvancedShops;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.rosacore.config.RosaConfig;
import pl.kiosel.rosacore.message.MessagePlaceholders;
import pl.kiosel.rosacore.message.RosaMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class HologramConfig {

	private final AdvancedShops plugin;
	private boolean hologramEnabled, itemEnabled;
	@Getter private double hologramHeight, itemHeight, lineSpacing, displayViewDistance;

	private Predicate<ShopChest> hologramRequirements = shop -> true;
	private Predicate<ShopChest> itemRequirements = shop -> true;
	private List<Line> lines = Collections.emptyList();

	public HologramConfig(AdvancedShops plugin) {
		this.plugin = plugin;
	}

	public void setConfig() {
		RosaConfig config = plugin.getChestsFile();

		boolean nextHologramEnabled = config.getBoolean("hologram.enabled", true);
		boolean nextItemEnabled = config.getBoolean("item.enabled", true);

		double nextHologramHeight = number(config.get("hologram.height"), 2.6, "hologram.height", -16, 16);
		double nextItemHeight = number(config.get("item.height"), 1.0, "item.height", -16, 16);
		double nextLineSpacing = number(config.get("hologram.line-spacing"), 0.27, "hologram.line-spacing", 0.01, 4);
		double nextDisplayViewDistance = number(config.get("display-view-distance"), 32, "display-view-distance", 0, 512);

		Predicate<ShopChest> nextHologramRequirements = requirements(config.get("hologram.requirements"), "hologram.requirements");
		Predicate<ShopChest> nextItemRequirements = requirements(config.get("item.requirements"), "item.requirements");

		Object rawLines = config.get("hologram.lines");
		if (!(rawLines instanceof List)) throw invalid("hologram.lines", "expected a list");

		List<Line> nextLines = new ArrayList<>();
		int index = 0;
		for (Object rawLine : (List<?>) rawLines) {
			String path = "hologram.lines[" + index++ + "]";
			if (!(rawLine instanceof Map)) throw invalid(path, "expected text or message with optional requirements");

			Map<?, ?> line = (Map<?, ?>) rawLine;
			Object text = line.get("text"), message = line.get("message");
			if ((text == null) == (message == null))
				throw invalid(path, "set exactly one of text or message");

			Object value = text == null ? message : text;
			if (!(value instanceof String) || (message != null && ((String) value).trim().isEmpty()))
				throw invalid(path, "text/message must be a string; message cannot be empty");

			boolean enabled = bool(line.get("enabled"), true, path + ".enabled");

			Predicate<ShopChest> condition = requirements(line.get("requirements"), path + ".requirements");
			if (enabled)
				nextLines.add(new Line((String) value, message != null, condition));
		}

		hologramEnabled = nextHologramEnabled;
		itemEnabled = nextItemEnabled;
		hologramHeight = nextHologramHeight;
		itemHeight = nextItemHeight;
		lineSpacing = nextLineSpacing;
		displayViewDistance = nextDisplayViewDistance;
		hologramRequirements = nextHologramRequirements;
		itemRequirements = nextItemRequirements;
		lines = Collections.unmodifiableList(nextLines);
	}

	public boolean isItemEnabled(ShopChest shopChest) {
		return itemEnabled && itemRequirements.test(shopChest);
	}

	public List<String> getLines(ShopChest shopChest) {
		if (!hologramEnabled || !hologramRequirements.test(shopChest)) return Collections.emptyList();
		MessagePlaceholders values = plugin.getShopMessages().placeholders(shopChest);

		List<String> result = new ArrayList<>();
		for (Line line : lines) {
			if (!line.requirements.test(shopChest)) continue;
			RosaMessage message = line.message ? plugin.getMessages().message(line.value) : plugin.getMessages().literal(line.value);
			result.addAll(message.with(values).legacyLines());
		}
		return result;
	}

	private Predicate<ShopChest> requirements(Object raw, String path) {
		if (raw == null) return shop -> true;
		if (raw instanceof ConfigurationSection)
			raw = ((ConfigurationSection) raw).getValues(false);
		if (!(raw instanceof Map)) throw invalid(path, "expected a map of conditions to true/false");

		Predicate<ShopChest> result = shop -> true;
		for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
			String name = String.valueOf(entry.getKey());
			boolean expected = bool(entry.getValue(), true, path + "." + name);

			Predicate<ShopChest> condition;
			switch (name) {
				case "admin-shop":
					condition = ShopChest::isAdminShop;
					break;
				case "enabled":
					condition = ShopChest::isEnabled;
					break;
				case "buy-enabled":
					condition = shop -> shop.getBuyPrice() > 0;
					break;
				case "sell-enabled":
					condition = shop -> shop.getSellPrice() > 0;
					break;
				default:
					throw invalid(path + "." + name, "unknown requirement");
			}
			result = result.and(expected ? condition : condition.negate());
		}
		return result;
	}

	private boolean bool(Object value, boolean fallback, String path) {
		if (value == null) return fallback;
		if (!(value instanceof Boolean)) throw invalid(path, "expected true or false");
		return (Boolean) value;
	}

	private double number(Object value, double fallback, String path, double min, double max) {
		if (value == null) return fallback;
		if (!(value instanceof Number)) throw invalid(path, "expected a number");
		double number = ((Number) value).doubleValue();
		if (!Double.isFinite(number) || number < min || number > max)
			throw invalid(path, "expected a finite number between " + min + " and " + max);
		return number;
	}

	private IllegalArgumentException invalid(String path, String message) {
		return new IllegalArgumentException("chests.yml: " + path + ": " + message);
	}

	private static final class Line {
		private final String value;
		private final boolean message;
		private final Predicate<ShopChest> requirements;

		private Line(String value, boolean message, Predicate<ShopChest> requirements) {
			this.value = value;
			this.message = message;
			this.requirements = requirements;
		}
	}
}
