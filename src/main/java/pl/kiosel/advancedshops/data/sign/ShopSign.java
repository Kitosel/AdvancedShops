package pl.kiosel.advancedshops.data.sign;

import lombok.Getter;
import org.bukkit.block.Sign;

import java.util.Objects;

public class PaySign {

	@Getter private final Sign sign;
	@Getter private final String player;
	@Getter private final double price;
	@Getter private final int delay;

	public PaySign(Sign sign, String player, double price, int delay) {
		this.sign = Objects.requireNonNull(sign);
		this.player = Objects.requireNonNull(player);
		this.price = price;
		this.delay = delay;
	}
}
