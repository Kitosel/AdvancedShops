package pl.kiosel.advancedshops.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import pl.kiosel.advancedshops.data.sign.ShopSign;

public class ShopsSignRemoveEvent extends ShopsSignEvent implements Cancellable {

	private boolean cancelled;

	public ShopsSignRemoveEvent(ShopSign shopSign, Player player) {
		super(shopSign, player);
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
