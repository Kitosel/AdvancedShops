package pl.kiosel.advancedshops.storage.migrations;

import pl.kiosel.rosacore.database.DatabaseMigration;
import pl.kiosel.rosacore.database.DatabaseType;

import java.sql.Connection;
import java.sql.Statement;

public final class _1_InitialMigration extends DatabaseMigration {

	private final DatabaseType type;

	public _1_InitialMigration(DatabaseType type) {
		super(1);
		this.type = type;
	}

	@Override
	public void migrate(Connection connection, String prefix) throws Exception {
		try (Statement statement = connection.createStatement()) {
			//shops table
			statement.execute("CREATE TABLE IF NOT EXISTS " + prefix + "shops (" +
					"`id` VARCHAR(36) NOT NULL, " +
					"`owner_id` VARCHAR(36) NOT NULL, " +
					"`owner_name` VARCHAR(255) NOT NULL, " +
					"`world` VARCHAR(36) NOT NULL, " +
					"`location` TEXT NOT NULL, " +
					"`item` LONGTEXT NOT NULL, " +
					"`amount` INT NOT NULL, " +
					"`buy` DOUBLE NOT NULL, " +
					"`sell` DOUBLE NOT NULL, " +
					"`cost_charged` DOUBLE NOT NULL, " +
					"`enabled` BOOLEAN NOT NULL, " +
					"`admin` BOOLEAN NOT NULL, " +
					"PRIMARY KEY (`id`))" + (type.isRemote() ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" : ""));

			//players table
			statement.execute("CREATE TABLE IF NOT EXISTS " + prefix + "players (" +
					"`id` VARCHAR(36) NOT NULL, " +
					"`name` VARCHAR(36) NOT NULL, " +
					"`last_logout` VARCHAR(255) NOT NULL, " +
					"`notifications_enabled` BOOLEAN NULL, " +
					"`pending_summary` TEXT NULL, " +
					"PRIMARY KEY (`id`))" + (type.isRemote() ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" : ""));
		}
	}
}
