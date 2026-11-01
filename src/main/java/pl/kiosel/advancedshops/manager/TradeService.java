package pl.kiosel.advancedshops.manager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pl.kiosel.advancedshops.data.shop.InventoryTransfer;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.advancedshops.data.shop.TradeQuote;
import pl.kiosel.rosacore.hook.economy.EconomyHook;

public final class TradeService {

	public enum Result {
		SUCCESS, NO_STOCK, NO_SPACE, NO_MONEY, PAYMENT_FAILED
	}

	public Result trade(ShopChest shopChest, Player customer, Inventory storage, EconomyHook economy, boolean buying) {
		return trade(shopChest, customer, storage, economy, buying, TradeQuote.of(shopChest, buying, false));
	}

	public Result trade(ShopChest shopChest, Player customer, Inventory storage, EconomyHook economy, boolean buying, TradeQuote quote) {
		if (!Double.isFinite(quote.getPrice()) || quote.getPrice() <= 0 || quote.getAmount() <= 0)
			throw new IllegalArgumentException("Invalid trade quote");

		if (shopChest.isAdminShop()) return tradeAdmin(shopChest, customer, economy, buying, quote);

		ItemStack item = shopChest.getItem();
		Inventory source = buying ? storage : customer.getInventory();
		Inventory destination = buying ? customer.getInventory() : storage;

		ItemStack[] sourceBefore = contents(source), destinationBefore = contents(destination);
		ItemStack[] sourceAfter = InventoryTransfer.copy(sourceBefore), destinationAfter = InventoryTransfer.copy(destinationBefore);

		if (!InventoryTransfer.remove(sourceAfter, item, quote.getAmount()))
			return Result.NO_STOCK;
		if (!InventoryTransfer.add(destinationAfter, item, quote.getAmount(), destination.getMaxStackSize()))
			return Result.NO_SPACE;

		OfflinePlayer owner = Bukkit.getOfflinePlayer(shopChest.getOwner());
		OfflinePlayer payer = buying ? customer : owner, payee = buying ? owner : customer;
		double price = quote.getPrice();

		if (!Double.isFinite(price) || price <= 0 || payer.getUniqueId().equals(payee.getUniqueId()))
			throw new IllegalArgumentException("Invalid trade");

		if (!economy.hasBalance(payer, price)) return Result.NO_MONEY;
		if (!economy.withdraw(payer, price)) return Result.PAYMENT_FAILED;

		try {
			setContents(source, sourceAfter);
			setContents(destination, destinationAfter);
		} catch (RuntimeException failure) {
			try {
				setContents(source, sourceBefore);
			} catch (RuntimeException error) {
				failure.addSuppressed(error);
			}
			try {
				setContents(destination, destinationBefore);
			} catch (RuntimeException error) {
				failure.addSuppressed(error);
			}
			try {
				refund(economy, payer, price);
			} catch (RuntimeException error) {
				failure.addSuppressed(error);
			}
			throw failure;
		}

		if (economy.deposit(payee, price)) return Result.SUCCESS;

		RuntimeException rollbackFailure = null;
		try {
			setContents(source, sourceBefore);
		} catch (RuntimeException error) {
			rollbackFailure = error;
		}
		try {
			setContents(destination, destinationBefore);
		} catch (RuntimeException error) {
			if (rollbackFailure == null) rollbackFailure = error;
			else rollbackFailure.addSuppressed(error);
		}
		try {
			refund(economy, payer, price);
		} catch (RuntimeException error) {
			if (rollbackFailure == null) rollbackFailure = error;
			else rollbackFailure.addSuppressed(error);
		}
		if (rollbackFailure != null) throw rollbackFailure;
		return Result.PAYMENT_FAILED;
	}

	private Result tradeAdmin(ShopChest shopChest, Player customer, EconomyHook economy, boolean buying, TradeQuote quote) {
		double price = quote.getPrice();

		if (!Double.isFinite(price) || price <= 0) throw new IllegalArgumentException("Invalid trade");
		PlayerInventory inventory = customer.getInventory();
		ItemStack[] before = contents(inventory), after = InventoryTransfer.copy(before);

		if (buying) {
			if (!InventoryTransfer.add(after, shopChest.getItem(), quote.getAmount(), inventory.getMaxStackSize()))
				return Result.NO_SPACE;
			if (!economy.hasBalance(customer, price))
				return Result.NO_MONEY;
			if (!economy.withdraw(customer, price))
				return Result.PAYMENT_FAILED;
		} else if (!InventoryTransfer.remove(after, shopChest.getItem(), quote.getAmount())) {
			return Result.NO_STOCK;
		}
		try {
			setContents(inventory, after);
		} catch (RuntimeException failure) {
			try {
				setContents(inventory, before);
			} catch (RuntimeException error) {
				failure.addSuppressed(error);
			}
			if (buying) {
				try {
					refund(economy, customer, price);
				} catch (RuntimeException error) {
					failure.addSuppressed(error);
				}
			}
			throw failure;
		}
		if (buying) return Result.SUCCESS;
		if (economy.deposit(customer, price)) return Result.SUCCESS;

		setContents(inventory, before);
		return Result.PAYMENT_FAILED;
	}

	private void refund(EconomyHook economy, OfflinePlayer payer, double price) {
		if (!economy.deposit(payer, price))
			throw new IllegalStateException("Economy refused refund to " + payer.getUniqueId() + ": " + price);
	}

	private ItemStack[] contents(Inventory inventory) {
		return InventoryTransfer.copy(inventory instanceof PlayerInventory
				? inventory.getStorageContents() : inventory.getContents());
	}

	private void setContents(Inventory inventory, ItemStack[] contents) {
		if (inventory instanceof PlayerInventory) {
			inventory.setStorageContents(contents);
		} else {
			inventory.setContents(contents);
		}
	}
}
