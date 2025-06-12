package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.entity.Player;
import org.mineacademy.boss.menu.ToolsMenu;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to open Tools menu.
 */
final class ToolsCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	ToolsCommand() {
		super("tools|t");

		this.setValidArguments(0, 1);
		this.setUsage("[player]");
		this.setDescription(Lang.component("command-tools-description"));
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		this.checkBoolean(Settings.REGISTER_TOOLS, Lang.component("command-requires-tools"));

		Player target = this.args.length > 0 ? this.findPlayer(this.args[0]) : null;

		if (target == null) {
			this.checkConsole();

			target = this.getPlayer();
		}

		ToolsMenu.showTo(target);

		if (!target.getName().equals(this.audience.getName()))
			this.tellInfo(Lang.component("command-tools-success", "player", target));
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWordPlayerNames() : NO_COMPLETE;
	}

}