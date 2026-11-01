package pl.kiosel.advancedshops.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import pl.kiosel.advancedshops.data.shop.Shop;

public class ShopEditEvent extends ShopEvent implements Cancellable {

	private boolean cancelled;

	public ShopEditEvent(Shop shop, Player player) {
		super(shop, player);
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
