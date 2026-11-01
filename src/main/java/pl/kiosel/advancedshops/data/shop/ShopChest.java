package pl.kiosel.advancedshops.data.shop;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.advancedshops.data.BlockKey;
import pl.kiosel.advancedshops.manager.ShopUtils;

import java.util.*;

public final class ShopChest {
	@Getter
	private final UUID id;
	@Getter
	private final UUID owner;
	@Getter
	private final String ownerName;
	@Getter
	private final List<BlockKey> blocks;
	private final ItemStack item;
	@Getter
	private final int amount;
	@Getter
	private final double buyPrice, sellPrice, costCharged;
	@Getter
	private final boolean adminShop;
	@Setter
	@Getter
	private boolean enabled;

	public ShopChest(UUID id, UUID owner, String ownerName, List<BlockKey> blocks, ItemStack item,
					 int amount, double buyPrice, double sellPrice, double costCharged, boolean enabled) {
		this(id, owner, ownerName, blocks, item, amount, buyPrice, sellPrice, costCharged, enabled, false);
	}

	public ShopChest(UUID id, UUID owner, String ownerName, List<BlockKey> blocks, ItemStack item,
					 int amount, double buyPrice, double sellPrice, double costCharged, boolean enabled, boolean adminShop) {
		if (blocks == null || blocks.isEmpty() || blocks.size() > 2
				|| new HashSet<>(blocks).size() != blocks.size())
			throw new IllegalArgumentException("Invalid shop blocks");
		if (item == null || item.getType().name().equals("AIR") || amount < 1 || amount > 2304)
			throw new IllegalArgumentException("Invalid shop item or amount");
		ShopUtils.validatePrices(buyPrice, sellPrice);
		this.id = Objects.requireNonNull(id);
		this.owner = Objects.requireNonNull(owner);
		this.ownerName = Objects.requireNonNull(ownerName);
		this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
		this.item = item.clone();
		this.item.setAmount(1);
		this.amount = amount;
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.costCharged = costCharged;
		this.enabled = enabled;
		this.adminShop = adminShop;
	}

	public ItemStack getItem() {
		return item.clone();
	}

	public String getItemName() {
		return item.hasItemMeta() && item.getItemMeta().hasDisplayName()
				? item.getItemMeta().getDisplayName() : item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
	}

	public enum BuyClick {
		RIGHT,
		LEFT
	}
}
