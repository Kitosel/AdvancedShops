package pl.kiosel.advancedshops.storage;

import pl.kiosel.advancedshops.data.shop.ShopChest;

import java.util.Collection;
import java.util.List;

public interface ShopStorage extends AutoCloseable {

	List<ShopChest> load() throws Exception;

	PreparedSave prepareSave(Collection<ShopChest> shopChests) throws Exception;

	@Override
	default void close() {
	}

	@FunctionalInterface
	interface PreparedSave {
		void write() throws Exception;
	}
}
