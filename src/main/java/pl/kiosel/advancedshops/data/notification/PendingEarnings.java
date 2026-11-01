package pl.kiosel.advancedshops.data.notification;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PendingEarnings {

	@Getter private BigDecimal shopEarnings = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
	@Getter private BigDecimal signEarnings = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
	private final Map<String, SoldItem> soldItems = new LinkedHashMap<>();

	public void addShopSale(String material, String itemName, int amount, double earnings) {
		if (material == null || material.isEmpty() || itemName == null || itemName.isEmpty() || amount < 1)
			throw new IllegalArgumentException("Invalid pending shop sale");
		BigDecimal value = money(earnings);
		String key = material + '\u0000' + itemName;
		SoldItem previous = soldItems.get(key);
		if (previous == null) {
			soldItems.put(key, new SoldItem(material, itemName, amount, value));
		} else {
			soldItems.put(key, new SoldItem(material, itemName, Math.addExact(previous.amount, amount), previous.earnings.add(value)));
		}
		shopEarnings = shopEarnings.add(value);
	}

	public void addSignEarning(double earnings) {
		signEarnings = signEarnings.add(money(earnings));
	}

	public BigDecimal getTotalEarnings() {
		return shopEarnings.add(signEarnings);
	}

	public List<SoldItem> getSoldItems() {
		List<SoldItem> result = new ArrayList<>(soldItems.values());
		result.sort(Comparator.comparing(SoldItem::getItemName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(SoldItem::getMaterial));
		return Collections.unmodifiableList(result);
	}

	public boolean isEmpty() {
		return soldItems.isEmpty() && signEarnings.signum() == 0;
	}

	public String encode() {
		StringBuilder output = new StringBuilder("v1\nshop:").append(shopEarnings.toPlainString())
				.append("\nsign:").append(signEarnings.toPlainString());
		for (SoldItem item : soldItems.values()) {
			output.append("\nitem:").append(encodePart(item.material)).append(':')
					.append(encodePart(item.itemName)).append(':').append(item.amount).append(':')
					.append(item.earnings.toPlainString());
		}
		return output.toString();
	}

	public static PendingEarnings decode(String encoded) {
		PendingEarnings result = new PendingEarnings();
		if (encoded == null || encoded.trim().isEmpty()) return result;

		String[] lines = encoded.split("\\n");
		if (lines.length < 3 || !"v1".equals(lines[0]) || !lines[1].startsWith("shop:") || !lines[2].startsWith("sign:"))
			throw new IllegalArgumentException("Unsupported pending earnings format");

		result.shopEarnings = money(new BigDecimal(lines[1].substring("shop:".length())));
		result.signEarnings = money(new BigDecimal(lines[2].substring("sign:".length())));

		for (int index = 3; index < lines.length; index++) {
			String line = lines[index];
			if (line.isEmpty()) continue;

			String[] fields = line.split(":", -1);
			if (fields.length != 5 || !"item".equals(fields[0]))
				throw new IllegalArgumentException("Invalid pending item entry");

			String material = decodePart(fields[1]);
			String itemName = decodePart(fields[2]);
			int amount = Integer.parseInt(fields[3]);
			BigDecimal earnings = money(new BigDecimal(fields[4]));

			if (amount < 1 || material.isEmpty() || itemName.isEmpty() || earnings.signum() < 0)
				throw new IllegalArgumentException("Invalid pending item values");

			String key = material + '\u0000' + itemName;
			SoldItem previous = result.soldItems.get(key);
			if (previous == null) {
				result.soldItems.put(key, new SoldItem(material, itemName, amount, earnings));
			} else {
				result.soldItems.put(key, new SoldItem(material, itemName, Math.addExact(previous.amount, amount), previous.earnings.add(earnings)));
			}
		}
		if (result.shopEarnings.signum() < 0 || result.signEarnings.signum() < 0)
			throw new IllegalArgumentException("Pending earnings cannot be negative");
		return result;
	}

	private static BigDecimal money(double amount) {
		if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid earnings");
		return money(BigDecimal.valueOf(amount));
	}

	private static BigDecimal money(BigDecimal amount) {
		if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("Invalid earnings");
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private static String encodePart(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private static String decodePart(String value) {
		return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
	}

	@Getter
	public static final class SoldItem {
		private final String material;
		private final String itemName;
		private final int amount;
		private final BigDecimal earnings;

		private SoldItem(String material, String itemName, int amount, BigDecimal earnings) {
			this.material = material;
			this.itemName = itemName;
			this.amount = amount;
			this.earnings = earnings;
		}

	}
}
