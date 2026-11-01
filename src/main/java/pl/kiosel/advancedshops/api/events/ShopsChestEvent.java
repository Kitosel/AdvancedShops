package pl.kiosel.advancedshops.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import pl.kiosel.advancedshops.data.shop.ShopChest;

public class ShopsChestEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	@Getter
	private final ShopChest shopChest;
	@Getter
	private final Player player;

	public ShopsChestEvent(ShopChest shopChest, Player player) {
		this.shopChest = shopChest;
		this.player = player;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
