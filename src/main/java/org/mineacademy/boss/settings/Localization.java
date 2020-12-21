package org.mineacademy.boss.settings;

import org.mineacademy.fo.settings.SimpleLocalization;

@SuppressWarnings("unused")
public final class Localization extends SimpleLocalization {

	@Override
	protected int getConfigVersion() {
		return 1;
	}

	public static final class Commands {
		public static String BIOME;

		private static void init() {
			pathPrefix("Commands");

			BIOME = getString("Biome");
		}
	}

	public static class Spawning {
		public static String NO_PERMISSION, NOT_INSTALLED, MAIN_HAND, FAIL;
		public static String SUMMONED_LOCATION;

		private static void init() {
			pathPrefix("Spawning");

			NO_PERMISSION = getString("No_Permission");
			NOT_INSTALLED = getString("Not_Installed");
			MAIN_HAND = getString("Main_Hand");
			FAIL = getString("Fail");
			SUMMONED_LOCATION = getString("Summoned_Location");
		}
	}

	public static class Tools {
		public static String INSPECT_OPEN, INSPECT_INVALID;

		private static void init() {
			pathPrefix("Tools");

			INSPECT_OPEN = getString("Inspect_Open");
			INSPECT_INVALID = getString("Inspect_Invalid");
		}
	}

	public static class Invalid {
		public static String BOSS, WORLD;

		private static void init() {
			pathPrefix("Invalid");

			BOSS = getString("Boss");
			WORLD = getString("World");
		}
	}
}
