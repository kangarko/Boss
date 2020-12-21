package org.mineacademy.boss.util;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;

@UtilityClass
@FieldDefaults(makeFinal = true)
public class Constants {

	@UtilityClass
	@FieldDefaults(makeFinal = true)
	public class NBT {
		public final static String TAG = "KaBoss";
		public final static String KEEP_INSIDE = "KaBoss_KeepInside";
	}

	@UtilityClass
	@FieldDefaults(makeFinal = true)
	public class Folder {
		public String BOSSES = "bosses/";
	}

	@UtilityClass
	@FieldDefaults(makeFinal = true)
	public class Header {
		public final static String[] MENU_HELP = new String[] {
				"&8" + Common.chatLine(),
				"&6&l  " + SimplePlugin.getNamed() + " &8" + SimplePlugin.getVersion(),
				" "
		};
		public String[] BOSS_FILE = new String[] {
				" ------------------------------------------------------------------------",
				" Welcome to the individual Boss settings file.",
				" ------------------------------------------------------------------------",
				" Here you can configure the same settings as in",
				" '/boss menu' for this Boss",
				" ",
				" It is recommended to use the GUI above to edit this file.",
				" We have documentation to each setting in the game.",
				" ------------------------------------------------------------------------",
				" "
		};
	}
}
