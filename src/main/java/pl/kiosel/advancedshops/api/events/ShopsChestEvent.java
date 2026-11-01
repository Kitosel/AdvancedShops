package pl.kiosel.advancedshops.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import pl.kiosel.advancedshops.data.shop.Shop;

public class ShopEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	@Getter
	private final Shop shop;
	@Getter
	private final Player player;

	public ShopEvent(Shop shop, Player player) {
		this.shop = shop;
		this.player = player;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
