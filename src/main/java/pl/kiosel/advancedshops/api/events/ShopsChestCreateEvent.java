package pl.kiosel.advancedshops.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import pl.kiosel.advancedshops.data.shop.ShopChest;

public class ShopsChestCreateEvent extends ShopsChestEvent implements Cancellable {

	private boolean cancelled;

	public ShopsChestCreateEvent(ShopChest shopChest, Player player) {
		super(shopChest, player);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}
}
