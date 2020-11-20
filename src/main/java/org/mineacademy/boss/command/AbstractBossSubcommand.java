package org.mineacademy.boss.command;

import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.model.BossManager;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.plugin.SimplePlugin;

abstract class AbstractBossSubcommand extends SimpleSubCommand {

	protected AbstractBossSubcommand(String aliases) {
		super(SimplePlugin.getInstance().getMainCommand(), aliases);
	}

	protected final BossManager getBosses() {
		return BossPlugin.getBossManager();
	}
}