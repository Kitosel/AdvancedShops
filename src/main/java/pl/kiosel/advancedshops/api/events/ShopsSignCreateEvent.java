package pl.kiosel.advancedshops.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import pl.kiosel.advancedshops.data.sign.ShopSign;

public class ShopsSignEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	@Getter
	private final ShopSign shopSign;
	@Getter
	private final Player player;

	public ShopsSignEvent(ShopSign shopSign, Player player) {
		this.shopSign = shopSign;
		this.player = player;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
