package pl.kiosel.advancedshops.data.shop;

import org.bukkit.inventory.ItemStack;

public final class InventoryTransfer {

	private InventoryTransfer() {
	}

	public static ItemStack[] copy(ItemStack[] contents) {
		ItemStack[] result = new ItemStack[contents.length];
		for (int i = 0; i < contents.length; i++)
			result[i] = contents[i] == null ? null : contents[i].clone();
		return result;
	}

	public static int count(ItemStack[] contents, ItemStack item) {
		int count = 0;
		for (ItemStack stack : contents) if (matches(stack, item)) count += stack.getAmount();
		return count;
	}

	public static boolean remove(ItemStack[] contents, ItemStack item, int amount) {
		if (amount <= 0 || count(contents, item) < amount) return false;

		for (int i = 0; i < contents.length && amount > 0; i++) {
			ItemStack stack = contents[i];
			if (!matches(stack, item)) continue;
			int take = Math.min(amount, stack.getAmount());
			amount -= take;
			if (take == stack.getAmount()) contents[i] = null;
			else stack.setAmount(stack.getAmount() - take);
		}
		return true;
	}

	public static boolean add(ItemStack[] contents, ItemStack item, int amount, int inventoryLimit) {
		if (amount <= 0) return false;

		int limit = Math.min(item.getMaxStackSize(), inventoryLimit);
		if (limit <= 0) return false;

		long space = 0;
		for (ItemStack stack : contents)
			space += empty(stack) ? limit : matches(stack, item) ? Math.max(0, limit - stack.getAmount()) : 0;

		if (space < amount) return false;

		for (ItemStack stack : contents) {
			if (!matches(stack, item)) continue;
			int put = Math.min(amount, Math.max(0, limit - stack.getAmount()));

			stack.setAmount(stack.getAmount() + put);
			amount -= put;
		}
		for (int i = 0; i < contents.length && amount > 0; i++) {
			if (!empty(contents[i])) continue;

			contents[i] = item.clone();
			int put = Math.min(amount, limit);

			contents[i].setAmount(put);
			amount -= put;
		}
		return true;
	}

	private static boolean matches(ItemStack stack, ItemStack item) {
		return !empty(stack) && stack.isSimilar(item);
	}

	private static boolean empty(ItemStack stack) {
		return stack == null || stack.getType().name().equals("AIR") || stack.getAmount() <= 0;
	}
}
