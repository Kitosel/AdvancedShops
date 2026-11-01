package pl.kiosel.advancedshops.data.sign;

import lombok.Getter;
import org.bukkit.block.Sign;
import pl.kiosel.advancedshops.data.BlockKey;

import java.util.Objects;

public class ShopSign {

	@Getter private final Sign sign;
	@Getter private final String player;
	@Getter private final double price;
	@Getter private final int delay;
	@Getter private final BlockKey block;

	public ShopSign(Sign sign, String player, BlockKey key, double price, int delay) {
		this.sign = Objects.requireNonNull(sign);
		this.player = Objects.requireNonNull(player);
		this.block = Objects.requireNonNull(key);
		this.price = price;
		this.delay = delay;
	}
}
