package pl.kiosel.advancedshops.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import pl.kiosel.advancedshops.data.shop.Shop;

public class ShopStatusChangeEvent extends ShopEvent implements Cancellable {

	private boolean cancelled;
	@Getter private final boolean enabled;

	public ShopStatusChangeEvent(Shop shop, Player player, boolean enabled) {
		super(shop, player);
		this.enabled = enabled;
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
