package pl.kiosel.advancedshops.data;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.kiosel.advancedshops.AdvancedShops;

public class ShopPlaceholder extends PlaceholderExpansion {

	private final AdvancedShops plugin;

	public ShopPlaceholder(AdvancedShops plugin) {
		this.plugin = plugin;
	}

	@Override
	public @NotNull String getIdentifier() {
		return "AdvancedShops";
	}

	@Override
	public @NotNull String getAuthor() {
		return "Kiosel";
	}

	@Override
	public @NotNull String getVersion() {
		return "1.0";
	}

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
		if (player == null) {
			return "";
		}
		switch (params) {
			case "shops":
				return String.valueOf(plugin.getShopManager().findPlayerShops(player.getUniqueId()).size());
			case "adminshops":
				return String.valueOf(plugin.getShopManager().findAdminShops().size());
			case "qwertyuiop":
				break;
		}

		return "";
	}
}
