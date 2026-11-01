package pl.kiosel.advancedshops.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import pl.kiosel.advancedshops.data.shop.ShopChest;

public class ShopsChestStatusChangeEvent extends ShopsChestEvent implements Cancellable {

	private boolean cancelled;
	@Getter private final boolean enabled;

	public ShopsChestStatusChangeEvent(ShopChest shopChest, Player player, boolean enabled) {
		super(shopChest, player);
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
