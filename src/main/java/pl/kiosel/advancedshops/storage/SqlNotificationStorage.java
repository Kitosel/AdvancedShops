package pl.kiosel.advancedshops.storage;

import pl.kiosel.advancedshops.data.notification.PendingEarnings;
import pl.kiosel.rosacore.database.DatabaseManager;
import pl.kiosel.rosacore.database.DatabaseSession;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SqlNotificationStorage {

	private final DatabaseManager database;
	private final String table;

	public SqlNotificationStorage(DatabaseManager database) {
		this.database = Objects.requireNonNull(database, "database");
		this.table = database.tableName("players");
	}

	public Map<UUID, Boolean> loadPreferences() {
		Map<UUID, Boolean> result = new LinkedHashMap<>();
		database.query("SELECT `id`, `notifications_enabled` FROM " + table
				+ " WHERE `notifications_enabled` IS NOT NULL", row -> {
			UUID id = UUID.fromString(row.getString("id"));
			Boolean enabled = row.getBooleanObject("notifications_enabled");
			if (enabled != null)
				result.put(id, enabled);
			return null;
		});
		return result;
	}

	public void setPreference(UUID id, String name, boolean enabled) {
		database.transaction(connection -> {
			DatabaseSession session = database.session(connection);
			ensurePlayer(session, id, name);
			session.executeUpdate("UPDATE " + table + " SET `notifications_enabled` = ? WHERE `id` = ?",
					enabled, id.toString());
		});
	}

	public void recordLogout(UUID id, String name) {
		database.transaction(connection -> {
			DatabaseSession session = database.session(connection);
			ensurePlayer(session, id, name);
			session.executeUpdate("UPDATE " + table + " SET `last_logout` = ? WHERE `id` = ?",
					Instant.now().toString(), id.toString());
		});
	}

	public void addShopSale(UUID owner, String name, String material, String itemName, int amount, double earnings) {
		updateSummary(owner, name, summary -> summary.addShopSale(material, itemName, amount, earnings));
	}

	public void addSignEarning(UUID owner, String name, double earnings) {
		updateSummary(owner, name, summary -> summary.addSignEarning(earnings));
	}

	public PendingEarnings getPending(UUID id) {
		return database.queryFirst("SELECT `pending_summary` FROM " + table + " WHERE `id` = ?",
						row -> PendingEarnings.decode(row.getString("pending_summary")), id.toString())
				.orElseGet(PendingEarnings::new);
	}

	public void clearPending(UUID id, PendingEarnings delivered) {
		database.executeUpdate("UPDATE " + table + " SET `pending_summary` = NULL WHERE `id` = ? AND `pending_summary` = ?",
				id.toString(), delivered.encode());
	}

	private void updateSummary(UUID owner, String name, SummaryChange change) {
		database.transaction(connection -> {
			DatabaseSession session = database.session(connection);
			ensurePlayer(session, owner, name);

			String encoded = session.queryFirst("SELECT `pending_summary` FROM " + table +
					" WHERE `id` = ? FOR UPDATE", row -> row.getString("pending_summary"), owner.toString()).orElse(null);
			PendingEarnings summary = PendingEarnings.decode(encoded);
			change.apply(summary);
			session.executeUpdate("UPDATE " + table + " SET `pending_summary` = ? WHERE `id` = ?",
					summary.encode(), owner.toString());
		});
	}

	private void ensurePlayer(DatabaseSession session, UUID id, String name) {
		if (findPreference(session, id, true) == null) {
			session.executeUpdate("INSERT INTO " + table +
							" (`id`, `name`, `last_logout`, `notifications_enabled`, `pending_summary`) VALUES (?, ?, '', NULL, NULL)",
					id.toString(), safeName(name, id));
		} else if (name != null && !name.trim().isEmpty()) {
			session.executeUpdate("UPDATE " + table + " SET `name` = ? WHERE `id` = ?", name, id.toString());
		}
	}

	private StoredPreference findPreference(DatabaseSession session, UUID id, boolean lock) {
		String sql = "SELECT `notifications_enabled` FROM " + table + " WHERE `id` = ?" + (lock ? " FOR UPDATE" : "");
		return session.queryFirst(sql, row -> new StoredPreference(row.getBooleanObject("notifications_enabled")), id.toString())
				.orElse(null);
	}

	private static String safeName(String name, UUID id) {
		return name == null || name.trim().isEmpty() ? id.toString() : name;
	}

	@FunctionalInterface
	private interface SummaryChange {
		void apply(PendingEarnings summary);
	}

	private static final class StoredPreference {
		private final Boolean enabled;

		private StoredPreference(Boolean enabled) {
			this.enabled = enabled;
		}
	}

}
