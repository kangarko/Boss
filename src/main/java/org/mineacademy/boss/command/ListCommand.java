package org.mineacademy.boss.command;

import java.util.List;

import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to list installed Bosses.
 */
final class ListCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	ListCommand() {
		super("list|l");

		this.setMaxArguments(0);
		this.setDescription(Lang.component("command-find-description"));
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final List<String> bosses = Boss.getBossesNames();
		SimpleComponent component = Lang.component("command-list-start", "amount", bosses.size());

		for (int i = 0; i < bosses.size(); i++) {
			final String bossName = bosses.get(i);

			component = component
					.appendMiniAmpersand(bossName + (i + 1 == bosses.size() ? "" : "&8, &7"))
					.onHover(Lang.component("command-list-hover"))
					.onClickRunCmd("/" + this.getLabel() + " menu " + bossName);
		}

		tell(component);
	}
}