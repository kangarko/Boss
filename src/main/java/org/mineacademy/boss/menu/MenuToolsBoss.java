package org.mineacademy.boss.menu;

import org.mineacademy.boss.tool.InspectorTool;
import org.mineacademy.boss.tool.RegionTool;
import org.mineacademy.boss.tool.SpawnerTool;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuTools;

import lombok.Getter;

public class MenuToolsBoss extends MenuTools {

	@Getter
	private static final MenuToolsBoss instance = new MenuToolsBoss();

	private MenuToolsBoss() {
	}

	public MenuToolsBoss(final Menu parent) {
		super(parent);
	}

	@Override
	protected Object[] compileTools() {
		return new Object[] {
				RegionTool.getInstance(),
				InspectorTool.getInstance(),
				SpawnerTool.getInstance()
		};
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"In this menu you'll find",
				"additional tools you can",
				"use to configure Boss",
				"or its parts."
		};
	}
}