package org.mineacademy.boss.command;

import java.util.List;

import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to remove Bosses.
 */
final class RemoveCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	RemoveCommand() {
		super("remove|rm");

		this.setValidArguments(1, 1);
		this.setDescription(Lang.component("command-remove-description"));
		this.setUsage("<boss>");
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final Boss boss = this.findBoss(this.args[0]);

		Boss.removeBoss(boss);
		this.tellSuccess(Lang.component("command-remove-success", "boss", boss.getName()));
	}

	@Override
	public List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWordBossNames() : NO_COMPLETE;
	}
}