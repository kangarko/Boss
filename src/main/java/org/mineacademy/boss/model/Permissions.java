package org.mineacademy.boss.model;

import org.mineacademy.fo.command.PermsSubCommand;
import org.mineacademy.fo.command.annotation.Permission;
import org.mineacademy.fo.command.annotation.PermissionGroup;

/**
 * The common Boss plugin permissions, see {@link PermsSubCommand} and /boss perms
 */
public final class Permissions {

	/**
	 * Command permissions (not all need to be explicitly declared here)
	 */
	@PermissionGroup("Permissions related to Boss commands.")
	public static final class Command {

		@Permission("Open Boss menu by clicking with a Boss egg or use /boss menu command.")
		public static final String MENU = "boss.command.menu";
	}

	/**
	 * Use related permissions
	 */
	@PermissionGroup("Permissions related to features of the Boss plugin.")
	public static final class Use {

		@Permission("Use the Boss spawner egg to spawn a Boss. You also need an additional boss.spawn.<bossName> permission for each Boss.")
		public static final String SPAWNER_EGG = "boss.use.spawneregg";

		@Permission("Use the Boss Egg to click a Boss to find more information.")
		public static final String INSPECT = "boss.use.inspect";

		@Permission("Use the Boss Tamer Tool to edit owners of an entity.")
		public static final String TAMER = "boss.use.tamer";
	}

	/**
	 * Spawn related permissions
	 */
	@PermissionGroup("Permissions related to spawning Bosses.")
	public static final class Spawn {

		@Permission("Spawn the given Boss through a spawner egg.")
		public static final String BOSS = "boss.spawn.{boss}";

		@Permission("Right click air holding Boss Egg to spawn Bosses in distances.")
		public static final String AIR = "boss.airspawn";
	}

	/**
	 * Bypass perms
	 */
	@PermissionGroup("Permissions related to bypassing plugin limitations.")
	public static final class Bypass {

		@Permission("Use Boss Egg in protected claims such as GriefPrevention, Residence, Towny etc.")
		public static final String CLAIM = "boss.bypass.claim";
	}
}
