package pl.kiosel.advancedshops.data.shop;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import pl.kiosel.advancedshops.data.BlockKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class ShopContainers {

	private ShopContainers() {
	}

	public static boolean isSupported(Block block) {
		if (block == null) return false;
		String type = block.getType().name();
		return type.equals("CHEST") || type.equals("TRAPPED_CHEST") || type.equals("BARREL");
	}

	public static Inventory inventory(Block block) {
		if (!isSupported(block)) return null;
		BlockState state = block.getState();
		if (state instanceof Chest) return ((Chest) state).getInventory();
		return state instanceof InventoryHolder ? ((InventoryHolder) state).getInventory() : null;
	}

	public static List<BlockKey> keys(Inventory inventory) {
		List<BlockKey> keys = new ArrayList<>();
		if (inventory != null)
			addHolder(keys, inventory.getHolder());
		return keys;
	}

	private static void addHolder(List<BlockKey> keys, InventoryHolder holder) {
		if (holder instanceof DoubleChest) {
			addHolder(keys, ((DoubleChest) holder).getLeftSide());
			addHolder(keys, ((DoubleChest) holder).getRightSide());
		} else if (holder instanceof BlockState) {
			keys.add(BlockKey.of(((BlockState) holder).getBlock()));
		}
	}

	public static Inventory resolve(ShopChest shopChest) {
		for (BlockKey key : shopChest.getBlocks())
			if (key.resolve() == null) return null;

		Inventory inventory = inventory(shopChest.getBlocks().get(0).resolve());
		return inventory != null && new HashSet<>(keys(inventory)).equals(new HashSet<>(shopChest.getBlocks()))
				? inventory : null;
	}
}
