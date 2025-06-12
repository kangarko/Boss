package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.entity.Player;
import org.mineacademy.boss.menu.MainMenu;
import org.mineacademy.boss.menu.SelectBossMenu;
import org.mineacademy.boss.menu.boss.BossMenu;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to open our main GUI.
 */
final class MenuCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	MenuCommand() {
		super("menu|m");

		this.setValidArguments(0, 2);
		this.setUsage("[boss] [player]");
		this.setDescription(Lang.component("command-menu-description"));
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {

		// If not set, we open the main menu
		final Boss boss = this.args.length > 0 ? this.findBoss(this.args[0]) : null;
		final Player player = this.args.length > 1 ? this.findPlayer(this.args[1]) : this.isPlayer() ? this.getPlayer() : null;

		if (player == null)
			this.checkConsole();

		if (boss == null)
			MainMenu.showTo(player);

		else
			BossMenu.showTo(SelectBossMenu.create(), player, boss);

		if (!player.getName().equals(this.audience.getName()))
			this.tellSuccess(Lang.component("command-menu-success", "player", player));
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWordBossNames();
			case 2:
				return this.completeLastWordPlayerNames();
		}

		return NO_COMPLETE;
	}
}
