package org.mineacademy.boss.settings;

import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleSettings;

@SuppressWarnings("unused")
public final class Settings extends SimpleSettings {

	/**
	 * @see org.mineacademy.fo.settings.YamlStaticConfig#saveComments()
	 */
	@Override
	protected boolean saveComments() {
		return true;
	}

	@Override
	protected int getConfigVersion() {
		return 6;
	}

	public static final class EggSpawning {

		public static Integer RADIUS;
		public static Boolean AIR_SPAWN, SPAWN_IF_CANCELLED, CHECK_REGIONS, FORCE_LATEST_EGG;

		private static void init() {
			if (VERSION < 2) {
				pathPrefix(null);

				set("Spawning.Lightning", null);
				move(getObject("Spawning"), "Spawning", "Egg_Spawning");
			}

			pathPrefix("Egg_Spawning");

			RADIUS = getInteger("Max_Distance");
			AIR_SPAWN = getBoolean("Air_Spawn");
			SPAWN_IF_CANCELLED = getBoolean("Spawn_If_Event_Cancelled");
			CHECK_REGIONS = getBoolean("Check_Regions");
			FORCE_LATEST_EGG = getBoolean("Enforce_Latest_Egg_Version");
		}

		public static final class Item {

			public static String NAME;
			public static CompMaterial MATERIAL;
			public static Boolean GLOW;
			public static List<String> LORE;

			private static void init() {
				pathPrefix("Egg_Spawning.Item");

				NAME = getString("Name");
				MATERIAL = CompMaterial.fromString(getString("Material"));
				GLOW = getBoolean("Glow");
				LORE = getStringList("Lore");
			}
		}
	}

	public static final class TimedSpawning {

		public static Boolean ENABLED;
		public static Boolean LIGHTNING;
		public static SimpleTime DELAY;
		public static IsInList<String> WORLDS;
		public static IsInList<String> BOSSES;

		private static void init() {
			pathPrefix("Timed_Spawning");

			if (VERSION < 3) {
				if (isSet("Chunk_Radius"))
					move("Chunk_Radius", "Timed_Spawning.Performance.Chunk_Radius");

				if (isSet("Check_Conditions"))
					move("Check_Conditions", "Timed_Spawning.Performance.Check_Conditions");
			}

			ENABLED = getBoolean("Enabled");
			LIGHTNING = getBoolean("Lightning");
			DELAY = getTime("Delay");
			WORLDS = new IsInList<>(getStringList("Worlds"));
			BOSSES = new IsInList<>(getStringList("Bosses"));

			if (!Debugger.isDebugModeEnabled()) {
				if (DELAY.getTimeTicks() < 20)
					throw new FoException("Timed_Spawning.Delay cannot be less than 20 ticks");

				if (DELAY.getTimeTicks() < 20 * 5)
					Common.logFramed(false,
							"&eNOTICE ON PERFORMANCE:",
							" ",
							"Timed Spawning delay is less",
							"than 5 seconds (100 ticks).",
							"Performance will be degraded.");

				else if (DELAY.getTimeTicks() < 20 * 15)
					Common.logFramed(false,
							"&eNOTICE ON PERFORMANCE:",
							" ",
							"Timed Spawning delay is less",
							"than 15 seconds (300 ticks).",
							"Performance may be affected.");
			}
		}

		public static final class Performance {

			public static Integer CHUNK_RADIUS;
			public static Boolean CONDITIONS, REGIONS;
			public static Boolean SPAWN_UNDERGROUND;

			private static void init() {
				pathPrefix("Timed_Spawning.Performance");

				CHUNK_RADIUS = getInteger("Chunk_Radius");
				CONDITIONS = getBoolean("Check_Conditions");
				REGIONS = getBoolean("Check_Regions");
				SPAWN_UNDERGROUND = getBoolean("Spawn_Underground");

				if (!Debugger.isDebugModeEnabled()) {
					if (CHUNK_RADIUS > 6)
						CHUNK_RADIUS = 6;

					if (CHUNK_RADIUS > 3)
						Common.logFramed(false,
								"&eNOTICE ON PERFORMANCE:",
								" ",
								"Timed Spawning chunk radius is",
								"over 3 (on " + CHUNK_RADIUS + "). Your server will",
								"likely get overloaded by this.");

					if (MinecraftVersion.newerThan(V.v1_13) && CHUNK_RADIUS > 1) {
						CHUNK_RADIUS = 1;

						set("Chunk_Radius", CHUNK_RADIUS);

						Common.logFramed(false,
								"&eNOTICE ON PERFORMANCE:",
								" ",
								"Timed Spawning chunk radius was greater than 1.",
								" ",
								"Minecraft 1.14+ has poorly implemented getting",
								"living entities (Bosses). To avoid lags, we reduced",
								"the radius to 1. Spawning won't be affected, but Bosses",
								"might spawn a bit closer to players.",
								" ",
								"Don't complain to us, this is not a Boss problem. See:",
								"https://github.com/PaperMC/Paper/issues/2125");
					}
				}
			}
		}
	}

	public static final class Converting {

		public static Boolean ENABLED;
		public static Boolean LIGHTNING;
		public static List<SpawnReason> IGNORED_CAUSES;

		private static void init() {
			pathPrefix("Converting");

			ENABLED = getBoolean("Enabled");
			LIGHTNING = getBoolean("Lightning");
			IGNORED_CAUSES = getList("Ignore_From", SpawnReason.class);
		}
	}

	public static final class PreventVanillaMobs {

		public static Boolean ENABLED;
		public static IsInList<SpawnReason> SPAWN_REASONS;
		public static IsInList<EntityType> ENTITY_TYPES;
		public static IsInList<String> WORLDS;

		private static void init() {
			pathPrefix("Timed_Spawning");

			if (VERSION < 6 && isSet("Prevent_Vanilla_Mobs"))
				move("Prevent_Vanilla_Mobs", "Prevent_Vanilla_Mobs");

			pathPrefix("Prevent_Vanilla_Mobs");

			ENABLED = getBoolean("Enabled");
			SPAWN_REASONS = new IsInList<>(getCompatibleEnumList("Prevent_From", SpawnReason.class));
			ENTITY_TYPES = new IsInList<>(getCompatibleEnumList("Entities", EntityType.class));
			WORLDS = new IsInList<>(getStringList("Worlds"));

			if (VERSION < 4 && ENABLED) {
				ENABLED = false;
				set("Prevent_Vanilla_Mobs.Enabled", false);

				Common.runLaterAsync(0, () -> {
					Common.logFramed(false,
							"Notice from the Boss plugin:",
							" ",
							"You had Prevent_Vanilla_Mobs option enabled",
							"that prevented natural mob and animal spawn.",
							"We disabled it as it may be enabled undesirably.",
							"If you are using it, please set it back again",
							"and it won't be turned off. Best, kangarko");
				});
			}
		}
	}

	public static final class Limits {

		public static Boolean APPLY_CMD, APPLY_EGG, APPLY_REINFORCEMENTS, APPLY_SPAWNERS;
		public static Integer RADIUS_BLOCKS;

		private static void init() {
			pathPrefix("Limits");

			APPLY_CMD = getBoolean("Apply_For_Commands");
			APPLY_EGG = getBoolean("Apply_For_Eggs");
			APPLY_REINFORCEMENTS = getBoolean("Apply_For_Reinforcements");
			APPLY_SPAWNERS = getBoolean("Apply_For_Spawners");
			RADIUS_BLOCKS = getInteger("Radius_Blocks");
		}

		public static final class Global {

			public static Boolean ENABLED;
			public static Integer WORLD;
			public static Integer RADIUS_LIMIT;

			private static void init() {
				pathPrefix("Limits.Global");

				if (isSet("Chunk"))
					set("Chunk", null);

				ENABLED = getBoolean("Enabled");
				WORLD = getInteger("World");
				RADIUS_LIMIT = getInteger("Radius_Limit");
			}
		}

		public static final class Individual {

			public static Boolean ENABLED;
			public static Integer WORLD;
			public static Integer RADIUS_LIMIT;

			private static void init() {
				pathPrefix("Limits.Individual");

				if (isSet("Chunk"))
					set("Chunk", null);

				ENABLED = getBoolean("Enabled");
				WORLD = getInteger("World");
				RADIUS_LIMIT = getInteger("Radius_Limit");
			}
		}
	}

	public static final class RegionKeep {

		public static Boolean PORT_TO_CENTER;
		public static Boolean ENABLED;
		public static SimpleTime PERIOD;

		private static void init() {
			pathPrefix("Region_Keeping");

			ENABLED = getBoolean("Enabled");
			PORT_TO_CENTER = getBoolean("Port_To_Center");
			PERIOD = getTime("Period");

			if (ENABLED && MinecraftVersion.olderThan(V.v1_10)) {
				Common.log("&6Warning: Region keeping requires Minecraft version 1.10 or greater, disabling..");

				ENABLED = false;
			}
		}
	}

	public static final class Fight {

		public static Boolean HEALTH_BAR;

		private static void init() {
			pathPrefix("Fight");

			HEALTH_BAR = getBoolean("Health_Bar");
		}

		public static final class Target {

			public static Boolean OVERRIDE_ON_ATTACK;
			public static Boolean CREATURES;
			public static Boolean PLAYERS;
			public static SimpleTime DELAY;
			public static Integer RADIUS;

			private static void init() {

				// Remove old section
				if (isSetAbsolute("Fight.Auto_Retarget")) {
					pathPrefix(null);

					set("Fight.Auto_Retarget", null);
				}

				pathPrefix("Fight.Auto_Target");

				if (isSet("Override_Target"))
					set("Override_Target", null);

				OVERRIDE_ON_ATTACK = getBoolean("Override_On_Attack");
				CREATURES = getBoolean("Creatures");
				PLAYERS = getBoolean("Players");
				DELAY = getTime("Delay");
				RADIUS = getInteger("Radius");
			}
		}
	}

	public static final class Death {

		public static Boolean RUN_PLAYER_COMMANDS_AS_PLAYER;

		private static void init() {
			pathPrefix("Death");

			RUN_PLAYER_COMMANDS_AS_PLAYER = getBoolean("Run_Player_Commands_As_Player");
		}
	}

	public static final class Setup {
		public static Boolean VISUALIZE;
		public static Boolean PROTOCOLLIB;
		public static Boolean SORT_ALPHABETICALLY;

		private static void init() {
			pathPrefix("Setup");

			if (VERSION < 5 && isSet("Enable_Tools"))
				set("Enable_Tools", null);

			VISUALIZE = getBoolean("Visualize_Regions");
			PROTOCOLLIB = getBoolean("Hook_ProtocolLib");
			SORT_ALPHABETICALLY = getBoolean("Sort_Bosses_In_Menu_Alphabetically");
		}
	}

	public static Boolean ITERATE_SPAWNING_TRIES;

	private static void init() {
		pathPrefix(null);

		ITERATE_SPAWNING_TRIES = getBoolean("Iterate_Spawning_Tries");

		if (Debugger.isDebugged("spawning"))
			Common.log("&cWarning: We have detected that you are debugging 'spawning'. This will show up in your timings because we save each debug log to a file.");
	}
}
