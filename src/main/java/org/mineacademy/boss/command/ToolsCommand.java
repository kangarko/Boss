package org.mineacademy.boss.command;

import org.mineacademy.boss.menu.MenuToolsBoss;

public final class ToolsCommand extends AbstractBossSubcommand {

	public ToolsCommand() {
		super("tools|t");

		setDescription("Open the tools menu");
	}

	@Override
	protected void onCommand() {
		checkConsole();

		MenuToolsBoss.getInstance().displayTo(getPlayer());
	}
}