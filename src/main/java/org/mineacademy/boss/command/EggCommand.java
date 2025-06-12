package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to get Boss spawn egg.
 */
final class EggCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	EggCommand() {
		super("egg|e");

		this.setValidArguments(1, 3);
		this.setDescription(Lang.component("command-egg-description"));
		this.setUsage("<boss> [player] [amount]");
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final String name = this.args[0];
		final Boss boss = this.findBoss(name);

		// Find the target player
		Player targetPlayer;

		if (this.args.length == 2) {
			targetPlayer = this.findPlayer(this.args[1]);

			if (!targetPlayer.equals(this.getPlayer()))
				Messenger.success(targetPlayer, Lang.component("command-egg-success", "player", targetPlayer.getName(), "boss", boss.getName()));

		} else {
			this.checkBoolean(this.isPlayer(), Lang.component("command-egg-no-player"));

			targetPlayer = this.getPlayer();
		}

		// Find amount
		final int amount = this.args.length > 2 ? this.findNumber(2, Lang.component("command-invalid-amount")) : 1;

		// Add items to players inventory and drop leftovers
		final ItemStack eggItem = boss.getEgg(amount);
		final boolean droppedOnFloor = !PlayerUtil.addItemsOrDrop(targetPlayer, eggItem);

		this.tellSuccess(Lang.component("command-egg-success",
				"player", targetPlayer.getName(),
				"boss", boss.getName()).append(droppedOnFloor ? SimpleComponent.fromPlain(" ").append(Lang.component("player-full-inventory")) : SimpleComponent.empty()));
	}

	@Override
	public List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWordBossNames();
			case 2:
				return this.completeLastWordPlayerNames();
			case 3:
				return this.completeLastWord(1);
		}

		return NO_COMPLETE;
	}
}