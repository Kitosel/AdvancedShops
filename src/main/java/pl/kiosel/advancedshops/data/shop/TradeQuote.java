package pl.kiosel.advancedshops.data.shop;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TradeQuote {

	@Getter private final int amount;
	@Getter private final double price;

	private TradeQuote(int amount, double price) {
		this.amount = amount;
		this.price = price;
	}

	public static TradeQuote of(ShopChest shopChest, boolean buying, boolean fullStack) {
		int amount = fullStack ? shopChest.getItem().getMaxStackSize() : shopChest.getAmount();
		double price = buying ? shopChest.getBuyPrice() : shopChest.getSellPrice();

		if (price > 0 && amount != shopChest.getAmount()) {
			price = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(amount))
					.divide(BigDecimal.valueOf(shopChest.getAmount()), 2, RoundingMode.DOWN).doubleValue();
		}
		return new TradeQuote(amount, price);
	}
}
