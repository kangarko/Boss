package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.World;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to kill alive Bosses.
 */
final class ButcherCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	ButcherCommand() {
		super("butcher|b");

		this.setValidArguments(1, 2);
		this.setUsage("<radius/world/*> [boss]");
		this.setDescription(Lang.component("command-butcher-description"));
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#getMultilineUsageMessage()
	 */
	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} 10 Zombie - Removes all 'Zombie' bosses in 10 blocks around you.",
				"/{label} {sublabel} minigames Zombie - Removes all 'Zombie' bosses in world 'minigames'.",
				"/{label} {sublabel} * Zombie - Removes all 'Zombie' bosses from all world.",
				"/{label} {sublabel} * - Removes all bosses from all world.",
				"",
				"&cNote: This command only removes Bosses from loaded chunks. If you need to remove all Bosses, use '/{label} scan'."
		};
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final String param = this.args[0];
		final RadiusMode mode = "*".equals(param) ? RadiusMode.ALL_WORLDS : Valid.isInteger(param) ? RadiusMode.BLOCKS : RadiusMode.WORLD;

		// Boss can be optional, if not set, we remove all bosses
		Boss boss = null;

		if (this.args.length == 2)
			boss = this.findBoss(this.args[1]);

		if (mode == RadiusMode.BLOCKS) {
			this.checkBoolean(this.isPlayer(), Lang.component("command-butcher-no-player"));

			final int radius = this.findInt(0, 0, 10_000, Lang.component("command-invalid-radius", "min", "0", "max", "10,000"));
			final int count = Boss.killAliveInRange(this.getPlayer().getLocation(), radius, boss);

			this.tellSuccess(Lang.component("command-butcher-success-radius",
					"amount", Lang.numberFormat("case-boss", count),
					"radius", Lang.numberFormat("case-block", radius)));
		}

		else if (mode == RadiusMode.WORLD) {
			final World world = this.findWorld(param);
			final int count = Boss.killAliveInWorld(world, boss);

			this.tellSuccess(Lang.component("command-butcher-success-world",
					"amount", Lang.numberFormat("case-boss", count),
					"world", world.getName()));
		}

		else if (mode == RadiusMode.ALL_WORLDS) {
			final int count = Boss.killAliveBosses(boss);

			this.tellSuccess(Lang.component("command-butcher-success-all-words", "amount", Lang.numberFormat("case-boss", count)));
		}
	}

	@Override
	public List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWord(this.completeLastWordWorldNames(), "10", "*");
			case 2:
				return this.completeLastWordBossNames();
		}

		return NO_COMPLETE;
	}

	/*
	 * The radius mode for clarity.
	 */
	private enum RadiusMode {
		BLOCKS,
		WORLD,
		ALL_WORLDS
	}
}