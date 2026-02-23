package org.mineacademy.boss.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.mineacademy.boss.tool.BossTamerTool;
import org.mineacademy.boss.tool.EntityInfoTool;
import org.mineacademy.boss.tool.LocationTool;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuTools;
import org.mineacademy.fo.menu.tool.RegionTool;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * Menu for players to get Boss tools.
 */
public final class ToolsMenu extends MenuTools {

	protected ToolsMenu(Menu parent) {
		super(parent);
	}

	@Override
	protected String[] getInfo() {
		return Lang.legacy("menu-tools-info").split("\n");
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuTools#compileTools()
	 */
	@Override
	protected Object[] compileTools() {
		final List<Object> tools = new ArrayList<>();

		if (SimpleSettings.REGISTER_REGIONS)
			tools.add(RegionTool.class);

		tools.add(LocationTool.class);
		tools.add(EntityInfoTool.class);
		tools.add(BossTamerTool.class);

		return tools.toArray();
	}

	/**
	 *
	 */
	@Override
	public Menu newInstance() {
		return new ToolsMenu(this.getParent());
	}

	/**
	 * Create a new Tools menu and show it to the player.
	 *
	 * @param player
	 */
	public static void showTo(Player player) {
		new ToolsMenu(null).displayTo(player);
	}
}