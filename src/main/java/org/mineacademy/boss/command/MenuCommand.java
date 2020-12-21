package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.menu.MenuBossIndividual;
import org.mineacademy.boss.menu.MenuMain;
import org.mineacademy.fo.TabUtil;

public final class MenuCommand extends AbstractBossSubcommand {

	public MenuCommand() {
		super("menu|m");

		setDescription("Access the main menu.");
		setUsage("[boss]");
	}

	@Override
	protected void onCommand() {
		if (args.length == 1) {
			final String name = args[0];
			final Boss boss = getBosses().findBoss(name);
			checkNotNull(boss, "&cUnknown boss '" + name + "'. Available: " + ListCommand.listAvailable());

			new MenuBossIndividual(boss).displayTo(getPlayer());
		} else
			new MenuMain().displayTo(getPlayer());
	}

	@Override
	public List<String> tabComplete() {
		final List<String> tab = new ArrayList<>();

		if (args.length == 1)
			return TabUtil.complete(args[0], BossPlugin.getBossManager().getBossesAsList().getSource());

		return tab;
	}
}