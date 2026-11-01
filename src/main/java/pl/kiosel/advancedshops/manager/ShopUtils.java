package pl.kiosel.advancedshops.manager;

import org.bukkit.ChatColor;

import java.math.BigDecimal;

public class ShopUtils {

	private ShopUtils() {
	}

	public static double parsePrice(String input) {
		String clean = ChatColor.stripColor(input);
		if (clean == null) throw new IllegalArgumentException("Price cannot be null");
		if (clean.equals("-") || clean.equals("0")) return -1;
		BigDecimal value = new BigDecimal(clean.replace(',', '.'));
		if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 2 || value.compareTo(BigDecimal.valueOf(1_000_000_000L)) > 0)
			throw new IllegalArgumentException("Invalid price");
		return value.doubleValue();
	}

	public static void validatePrices(double buy, double sell) {
		if (!validPrice(buy) || !validPrice(sell) || (buy == -1 && sell == -1))
			throw new IllegalArgumentException("Enter a positive price");
		if (buy > 0 && sell > buy)
			throw new IllegalArgumentException("The purchase price cannot exceed the selling price");
	}

	private static boolean validPrice(double value) {
		return value == -1 || (Double.isFinite(value) && value > 0 && value <= Double.MAX_VALUE);
	}
}
