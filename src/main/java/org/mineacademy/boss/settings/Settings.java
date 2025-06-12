package org.mineacademy.boss.settings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.mineacademy.boss.model.BossCheatDisable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * The settings.yml file
 */
@SuppressWarnings("unused")
public final class Settings extends SimpleSettings {

	/**
	 * The Spawning section
	 */
	public static class Spawning {

		public static Boolean AIR_SPAWN;
		public static Integer AIR_SPAWN_MAX_DISTANCE;
		public static Set<SpawnReason> IGNORE_REPLACING_VANILLA_FROM;
		public static Boolean CANCEL_VANILLA_IF_REPLACE_FAILS;
		public static Integer LOCATION_SPAWN_NEARBY_PLAYER_RADIUS;
		public static Integer NEARBY_SPAWN_MIN_DISTANCE_FROM_PLAYER;
		public static Boolean COUNT_UNLOADED_BOSSES_IN_LIMITS;
		public static String RESPAWN_PLACEHOLDER_PAST_DUE;
		public static Boolean LIVE_UPDATES;

		/*
		 * Automatically called method when we load settings.yml to load values in this subclass
		 */
		private static void init() {
			setPathPrefix("Spawning");

			AIR_SPAWN = getBoolean("Air_Spawn");
			AIR_SPAWN_MAX_DISTANCE = getInteger("Air_Spawn_Max_Distance");
			IGNORE_REPLACING_VANILLA_FROM = loadSpawnReasons("Ignore_Replacing_Vanilla_From");
			CANCEL_VANILLA_IF_REPLACE_FAILS = getBoolean("Cancel_Vanilla_If_Replace_Fails");
			LOCATION_SPAWN_NEARBY_PLAYER_RADIUS = getInteger("Location_Spawn_Nearby_Player_Radius");
			NEARBY_SPAWN_MIN_DISTANCE_FROM_PLAYER = getInteger("Nearby_Spawn_Min_Distance_From_Player");
			COUNT_UNLOADED_BOSSES_IN_LIMITS = getBoolean("Count_Unloaded_Bosses_In_Limits");
			RESPAWN_PLACEHOLDER_PAST_DUE = getString("Respawn_Placeholder_Past_Due");
			LIVE_UPDATES = getBoolean("Live_Updates");

			Valid.checkBoolean(AIR_SPAWN_MAX_DISTANCE != 0 && AIR_SPAWN_MAX_DISTANCE != -1, "To disable air spawn, set Spawning.Air_Spawn to false, and NOT Spawning.Air_Spawn_Max_Distance to 0 or -1");

			if (COUNT_UNLOADED_BOSSES_IN_LIMITS && !ReflectionUtil.isClassAvailable("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent")) {
				COUNT_UNLOADED_BOSSES_IN_LIMITS = false;

				set("Count_Unloaded_Bosses_In_Limits", false);

				Common.warning("Counting unloaded Bosses requires a modern Paper software. Disabling...");
			}
		}

		/**
		 * The Integration sub-section
		 */
		public static class Integration {

			public static Boolean LANDS;

			private static void init() {
				setPathPrefix("Spawning.Integration");

				LANDS = getBoolean("Lands");
			}
		}
	}

	/**
	 * The Fighting section
	 */
	public static class Fighting {

		public static Double RETARGET_CHANCE;
		public static Set<BossCheatDisable> DISABLE_CHEATS;

		private static void init() {
			setPathPrefix("Fighting");

			RETARGET_CHANCE = getPercentage("Retarget_Chance");
			DISABLE_CHEATS = getSet("Disable_Cheats", BossCheatDisable.class);
		}

		/**
		 * The Health_Bar section
		 */
		public static class HealthBar {

			public static Boolean ENABLED;
			public static String FORMAT;
			public static String PREFIX;
			public static String SUFFIX;
			public static CompChatColor COLOR_REMAINING;
			public static CompChatColor COLOR_TOTAL;
			public static CompChatColor COLOR_DEAD;

			private static void init() {
				setPathPrefix("Fighting.Health_Bar");

				ENABLED = getBoolean("Enabled");
				FORMAT = getString("Format");
				PREFIX = getString("Prefix");
				SUFFIX = getString("Suffix");
				COLOR_REMAINING = get("Color.Remaining", CompChatColor.class);
				COLOR_TOTAL = get("Color.Total", CompChatColor.class);
				COLOR_DEAD = get("Color.Dead", CompChatColor.class);
			}
		}

		/**
		 * The Citizens_Retarget section
		 */
		public static class CitizensRetarget {

			public static Boolean ENABLED;
			public static SimpleTime DELAY;

			private static void init() {
				setPathPrefix("Fighting.Citizens_Retarget");

				ENABLED = getBoolean("Enabled");
				DELAY = getTime("Delay");
			}
		}

	}

	/**
	 * The Health section
	 */
	public static class Health {

		public static Boolean PREVENT_REGENERATION;

		private static void init() {
			setPathPrefix("Health");

			PREVENT_REGENERATION = getBoolean("Prevent_Regeneration");
		}
	}

	/**
	 * The Death section
	 */
	public static class Death {

		public static Boolean RUN_PVP_COMMANDS_AS_CONSOLE;

		private static void init() {
			setPathPrefix("Death");

			RUN_PVP_COMMANDS_AS_CONSOLE = getBoolean("Run_PvP_Commands_As_Console");
		}
	}

	/**
	 * The Variables section
	 */
	public static class Variables {

		public static Integer NEARBY_BOSS_RADIUS;

		private static void init() {
			setPathPrefix("Variables");

			NEARBY_BOSS_RADIUS = getInteger("Nearby_Boss_Radius");
		}
	}

	/**
	 * The Skills section
	 */
	public static class Skills {

		public static Integer TARGET_RANGE;

		private static void init() {
			setPathPrefix("Skills");

			TARGET_RANGE = getInteger("Target_Range");
		}
	}

	/**
	 * The Prevent_Vanilla_Mobs section
	 */
	public static final class PreventVanillaMobs {

		public static Boolean ENABLED;
		public static Set<SpawnReason> SPAWN_REASONS;
		public static IsInList<EntityType> ENTITY_TYPES;
		public static IsInList<String> WORLDS;

		private static void init() {
			setPathPrefix("Prevent_Vanilla_Mobs");

			ENABLED = getBoolean("Enabled");
			SPAWN_REASONS = loadSpawnReasons("Prevent_From");
			ENTITY_TYPES = getIsInList("Entities", EntityType.class);
			WORLDS = getIsInList("Worlds", String.class);
		}
	}

	public static Boolean SORT_BY_TYPE;

	/*
	 * Automatically called method when we load settings.yml to load values in this subclass
	 */
	private static void init() {
		setPathPrefix(null);

		SORT_BY_TYPE = getBoolean("Sort_By_Type");

		Common.log("Using timezone " + TIMEZONE + " for spawning rules.");
	}

	/*
	 * Helper method to load spawn reasons without erroring default values on legacy MC
	 */
	private static Set<SpawnReason> loadSpawnReasons(String path) {
		final Set<SpawnReason> loaded = new HashSet<>();
		final List<String> compatibleReasons = Arrays.asList("COMMAND", "CUSTOM", "SLIME_SPLIT");

		for (final String reasonName : getStringList(path))
			try {
				final SpawnReason reason = ReflectionUtil.lookupEnum(SpawnReason.class, reasonName);

				loaded.add(reason);

			} catch (final Throwable t) {

				// Do not complain about default values non existing on legacy MC
				if (!compatibleReasons.contains(reasonName))
					t.printStackTrace();
			}

		return loaded;
	}
}
