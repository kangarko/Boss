package org.mineacademy.boss.command;

import org.apache.commons.lang.StringUtils;
import org.mineacademy.boss.BossPlugin;

public final class ListCommand extends AbstractBossSubcommand {

	public ListCommand() {
		super("list|l");

		setDescription("List created bosses.");
	}

	@Override
	protected void onCommand() {
		tell("Available (" + getBosses().getBossesAsList().size() + "): " + listAvailable());
	}

	static String listAvailable() {
		return StringUtils.join(BossPlugin.getBossManager().getBossesAsList().getSource(), "&8, &7");
	}
}