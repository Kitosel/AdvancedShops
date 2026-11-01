package pl.kiosel.advancedshops.manager;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.advancedshops.data.shop.ShopContainers;

import java.util.List;

public final class ShopProtectionManager {

	private final ShopManager shops;

	public ShopProtectionManager(ShopManager shops) {
		this.shops = shops;
	}

	public boolean isShop(Block block) {
		return shops.getShopAt(block) != null;
	}

	public boolean canAccessStorage(HumanEntity player, ShopChest shopChest) {
		return player instanceof Player && !shopChest.isAdminShop() && shopChest.getOwner().equals(player.getUniqueId());
	}

	public boolean deniesAccess(HumanEntity player, Inventory inventory) {
		ShopChest shopChest = shops.getShopIn(inventory);
		return shopChest != null && !canAccessStorage(player, shopChest);
	}

	public boolean deniesTransfer(Inventory source, Inventory destination) {
		return shops.getShopIn(source) != null || shops.getShopIn(destination) != null;
	}

	public boolean expandsShop(Block block) {
		if (!ShopContainers.isSupported(block)) return false;
		for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
			Block neighbor = block.getRelative(face);
			if (block.getType() == neighbor.getType() && isShop(neighbor)) return true;
		}
		return false;
	}

	public boolean deniesPiston(List<Block> blocks, BlockFace direction) {
		return blocks.stream().anyMatch(block -> isShop(block) || isShop(block.getRelative(direction)));
	}
}
