package pl.kiosel.advancedshops.storage;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pl.kiosel.advancedshops.data.BlockKey;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.rosacore.database.DatabaseRow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public final class ShopRecord {

	@Getter private final String id;
	private final String ownerId, ownerName, world, location, item;
	private final int amount;
	private final double buy, sell, costCharged;
	private final boolean enabled, admin;

	private ShopRecord(String id, String ownerId, String ownerName, String world, String location,
					   String item, int amount, double buy, double sell, double costCharged, boolean enabled, boolean admin) {
		this.id = Objects.requireNonNull(id);
		this.ownerId = Objects.requireNonNull(ownerId);
		this.ownerName = Objects.requireNonNull(ownerName);
		this.world = Objects.requireNonNull(world);
		this.location = Objects.requireNonNull(location);
		this.item = Objects.requireNonNull(item);
		this.amount = amount;
		this.buy = buy;
		this.sell = sell;
		this.costCharged = costCharged;
		this.enabled = enabled;
		this.admin = admin;
	}

	public static ShopRecord from(ShopChest shopChest) {
		List<String> blocks = new ArrayList<>();
		for (BlockKey block : shopChest.getBlocks())
			blocks.add(block.getX() + "," + block.getY() + "," + block.getZ());

		YamlConfiguration serializedItem = new YamlConfiguration();
		serializedItem.set("item", shopChest.getItem());
		return new ShopRecord(shopChest.getId().toString(), shopChest.getOwner().toString(), shopChest.getOwnerName(),
				shopChest.getBlocks().get(0).getWorld().toString(), String.join(";", blocks),
				serializedItem.saveToString(), shopChest.getAmount(), shopChest.getBuyPrice(), shopChest.getSellPrice(),
				shopChest.getCostCharged(), shopChest.isEnabled(), shopChest.isAdminShop());
	}

	public static ShopRecord read(DatabaseRow row) throws SQLException {
		return new ShopRecord(row.getString("id"), row.getString("owner_id"), row.getString("owner_name"),
				row.getString("world"), row.getString("location"), row.getString("item"), row.getInt("amount"),
				row.getDouble("buy"), row.getDouble("sell"), row.getDouble("cost_charged"),
				row.getBoolean("enabled"), row.getBoolean("admin"));
	}

	public ShopChest toShop() throws Exception {
		UUID worldId = UUID.fromString(world);
		List<BlockKey> blocks = getBlocks(worldId);
		YamlConfiguration serializedItem = new YamlConfiguration();
		serializedItem.loadFromString(item);

		ItemStack stack = serializedItem.getItemStack("item");
		if (stack == null)
			throw new IOException("Missing shop item: " + id);
		return new ShopChest(UUID.fromString(id), UUID.fromString(ownerId), ownerName, blocks, stack, amount, buy, sell, costCharged, enabled, admin);
	}

	private @NotNull List<BlockKey> getBlocks(UUID worldId) throws IOException {
		String[] positions = location.split(";", -1);
		if (positions.length < 1 || positions.length > 2)
			throw new IOException("Invalid shop location: " + id);

		List<BlockKey> blocks = new ArrayList<>();
		for (String position : positions) {
			String[] coordinates = position.split(",", -1);

			if (coordinates.length != 3)
				throw new IOException("Invalid shop coordinates: " + id);

			blocks.add(new BlockKey(worldId, Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2])));
		}
		return blocks;
	}

	public Object[] values() {
		return new Object[]{id, ownerId, ownerName, world, location, item, amount, buy, sell, costCharged, enabled, admin};
	}

	public Object[] updateValues() {
		return new Object[]{ownerId, ownerName, world, location, item, amount, buy, sell, costCharged, enabled, admin, id};
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ShopRecord && Arrays.equals(values(), ((ShopRecord) other).values());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values());
	}
}
