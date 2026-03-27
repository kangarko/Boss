package org.mineacademy.boss.command;

import java.util.List;

import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to duplicate an existing Boss.
 */
final class DuplicateCommand extends BossSubCommand {

	DuplicateCommand() {
		super("duplicate|copy|clone");

		this.setValidArguments(2, 2);
		this.setDescription(Lang.component("command-duplicate-description"));
		this.setUsage("<sourceBoss> <newName>");
	}

	@Override
	protected void onCommand() {
		final Boss source = this.findBoss(this.args[0]);

		final String newName = this.args[1];

		Valid.checkBoolean(newName.matches("[a-zA-Z0-9]+"), "Name must only contain English letters and numbers (no spaces or special characters).");
		Valid.checkBoolean(newName.length() >= 3 && newName.length() <= 24, "Name must be between 3 and 24 characters long.");
		this.checkBoolean(!Boss.isBossLoaded(newName), Lang.component("command-duplicate-already-exists", "boss", newName));

		final Boss duplicate = Boss.duplicateBoss(source, newName);

		this.tellSuccess(Lang.component("command-duplicate-success", "source", source.getName(), "boss", duplicate.getName()));
	}

	@Override
	public List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWordBossNames() : NO_COMPLETE;
	}
}
