package org.mineacademy.boss.command;

import org.mineacademy.fo.Common;

public final class ConvCommand extends AbstractBossSubcommand {

	public ConvCommand() {
		super("conversation|c");

		setDescription("Reply to server's conversation manually.");
		setUsage("<message ...>");
		setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		checkBoolean(getPlayer().isConversing(), "&cYou must be conversing with the server!");

		getPlayer().acceptConversationInput(Common.joinRange(0, args));
	}
}