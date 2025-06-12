package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.entity.EntityType;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.prompt.CreateBossPrompt;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to create a new Boss.
 */
final class NewCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	NewCommand() {
		super("new/create/add");

		this.setValidArguments(1, 1);
		this.setDescription(Lang.component("command-new-description"));
		this.setUsage("<entityType>");
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final EntityType entityType = CompEntityType.fromName(this.args[0]);
		this.checkBoolean(entityType != null && Boss.getValidEntities().contains(entityType), Lang.component("command-invalid-boss-type", "boss", this.args[0], "available", Common.join(Boss.getValidEntities())));
		this.checkBoolean(!this.getPlayer().isConversing(), "You already have an open server conversation. Type 'quit' to exit that first.");

		CreateBossPrompt.showTo(this.getPlayer(), entityType);
	}

	@Override
	public List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWord(Boss.getValidEntities()) : NO_COMPLETE;
	}
}