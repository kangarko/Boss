package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.boss.api.Boss;
import org.mineacademy.fo.Common;

public final class RemoveCommand extends AbstractBossSubcommand {

	public RemoveCommand() {
		super("remove|rm|r");

		setDescription("Delete a Boss permanently.");
		setUsage("<boss>");
		setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		final String name = Common.joinRange(0, args);

		final Boss boss = getBosses().findBoss(name);
		checkNotNull(boss, "&cBoss '" + name + "' doesn't exist. Available (" + getBosses().getBossesAsList().size() + "): " + ListCommand.listAvailable());

		getBosses().removeBoss(name);
		tell("&7The Boss '" + boss.getName() + "' has been &cremoved&7!");
	}

	@Override
	public List<String> tabComplete() {
		final List<String> tab = new ArrayList<>();

		if (args.length == 1)
			for (final String boss : getBosses().getBossesAsList())
				if (boss.toLowerCase().startsWith(args[0].toLowerCase()))
					tab.add(boss);

		return tab;
	}
}