package pl.kiosel.advancedshops.storage;

import pl.kiosel.advancedshops.data.BlockKey;
import pl.kiosel.advancedshops.data.shop.ShopChest;
import pl.kiosel.rosacore.database.DatabaseManager;
import pl.kiosel.rosacore.database.DatabaseSession;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public final class SqlShopStorage implements ShopStorage {

	private static final String COLUMNS = "`id`, `owner_id`, `owner_name`, `world`, `location`, `item`, `amount`, `buy`, `sell`, `cost_charged`, `enabled`, `admin`";
	private final DatabaseManager database;
	private final String table;
	private volatile Map<String, ShopRecord> snapshot;

	public SqlShopStorage(DatabaseManager database) {
		this.database = Objects.requireNonNull(database);
		table = database.tableName("shops");

		try (Connection connection = database.getConnection();
			 PreparedStatement statement = connection.prepareStatement("SELECT " + COLUMNS + " FROM " + table + " WHERE 1 = 0");
			 ResultSet rows = statement.executeQuery()) {
			if (rows.getMetaData().getColumnType(8) != Types.DOUBLE || rows.getMetaData().getColumnType(9) != Types.DOUBLE)
				throw new SQLException("Shop prices must use DOUBLE columns");
		} catch (SQLException error) {
			throw new IllegalStateException("Shop table does not match _1_InitialMigration. " +
					"Use a new database or table prefix in database.yml for this development schema. Existing data was not modified.", error);
		}
	}

	@Override
	public synchronized List<ShopChest> load() throws Exception {
		List<ShopRecord> rows = database.query("SELECT " + COLUMNS + " FROM " + table + " ORDER BY `id`", ShopRecord::read);
		List<ShopChest> shopChests = new ArrayList<>();
		Map<String, ShopRecord> loaded = new TreeMap<>();

		for (ShopRecord row : rows) {
			shopChests.add(row.toShop());
			if (loaded.put(row.getId(), row) != null) throw new IOException("Duplicate shop id: " + row.getId());
		}
		validate(shopChests);
		snapshot = Collections.unmodifiableMap(loaded);
		return shopChests;
	}

	@Override
	public synchronized PreparedSave prepareSave(Collection<ShopChest> shopChests) throws Exception {
		if (snapshot == null) throw new IllegalStateException("Load shops before saving");
		validate(shopChests);
		Map<String, ShopRecord> desired = new TreeMap<>();
		for (ShopChest shopChest : shopChests) {
			ShopRecord row = ShopRecord.from(shopChest);
			desired.put(row.getId(), row);
		}
		Map<String, ShopRecord> expected = snapshot;
		Map<String, ShopRecord> after = Collections.unmodifiableMap(desired);
		return () -> write(expected, after);
	}

	private synchronized void write(Map<String, ShopRecord> expected, Map<String, ShopRecord> after) throws Exception {
		if (snapshot != expected) throw new IOException("Shop snapshot changed after preparing the save");
		database.transaction(connection -> {
			DatabaseSession session = database.session(connection);
			for (ShopRecord previous : expected.values()) {
				ShopRecord next = after.get(previous.getId());
				if (previous.equals(next)) continue;
				ShopRecord actual = session.queryFirst("SELECT " + COLUMNS + " FROM " + table
						+ " WHERE `id` = ? FOR UPDATE", ShopRecord::read, previous.getId()).orElse(null);
				if (!previous.equals(actual))
					throw new IOException("Shop changed externally: " + previous.getId() + ". Reload before saving.");
				if (next == null) {
					session.executeUpdate("DELETE FROM " + table + " WHERE `id` = ?", previous.getId());
				} else {
					session.executeUpdate("UPDATE " + table
									+ " SET `owner_id`=?, `owner_name`=?, `world`=?, `location`=?, `item`=?, "
									+ "`amount`=?, `buy`=?, `sell`=?, `cost_charged`=?, `enabled`=?, `admin`=? WHERE `id`=?",
							next.updateValues());
				}
			}
			for (ShopRecord next : after.values()) {
				if (!expected.containsKey(next.getId()))
					session.executeUpdate("INSERT INTO " + table + " (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", next.values());
			}
		});
		snapshot = after;
	}

	private void validate(Collection<ShopChest> shopChests) throws IOException {
		Set<UUID> ids = new HashSet<>();
		Set<BlockKey> occupied = new HashSet<>();

		for (ShopChest shopChest : shopChests) {
			if (!ids.add(shopChest.getId())) throw new IOException("Duplicate shop UUID");

			for (BlockKey key : shopChest.getBlocks()) {
				if (!occupied.add(key)) throw new IOException("Overlapping shop blocks");
			}

			if (shopChest.getBlocks().size() == 2) {
				BlockKey a = shopChest.getBlocks().get(0), b = shopChest.getBlocks().get(1);

				if (!a.getWorld().equals(b.getWorld()) || a.getY() != b.getY()
						|| Math.abs((long) a.getX() - b.getX()) + Math.abs((long) a.getZ() - b.getZ()) != 1)
					throw new IOException("Invalid double chest");
			}
		}
	}
}
