package org.mineacademy.boss.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossSpawnReason;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

/**
 * The subcommand hosting all Boss subcommands.
 */
abstract class BossSubCommand extends SimpleSubCommand {

	/**
	 * Create a new subcommand.
	 *
	 * @param sublabel
	 */
	BossSubCommand(String sublabel) {
		super(sublabel);
	}

	/**
	 * Return the Boss by name or send error to player if invalid.
	 *
	 * @param name
	 * @return
	 */
	protected final Boss findBoss(String name) {
		final Boss boss = Boss.findBoss(name);
		this.checkNotNull(boss, Lang.component("command-invalid-boss", "boss", name, "available", Common.join(Boss.getBossesNames())));

		return boss;
	}

	/**
	 * Attempts to spawn bosses at the given location from the given command args index,
	 * splitting multiple names by |.
	 *
	 * @param spawnEggs
	 * @param location
	 * @param indexForBossNames
	 */
	protected final void spawnBosses(boolean spawnEggs, Location location, int indexForBossNames) {
		this.checkBoolean(!Boss.getBosses().isEmpty(), "No Bosses are installed. Use '/" + Settings.MAIN_COMMAND_ALIASES.get(0) + " new' to add one.");

		final List<String> bossNames = Arrays.asList(this.args[indexForBossNames].split("\\|"));

		boolean saveChunk = false;

		if (!location.getChunk().isLoaded()) {
			location.getChunk().load();

			saveChunk = true;
		}

		// Adjust to center
		location.add(.5, 0, .5);

		for (final String name : bossNames) {
			final Boss boss = "random".equals(name) ? RandomUtil.nextItem(Boss.getBosses()) : this.findBoss(name);

			if (spawnEggs)
				this.spawnBossEgg0(boss, location);
			else
				this.spawnBoss0(boss, location);
		}

		if (saveChunk)
			location.getChunk().unload(true);
	}

	/*
	 * Executor method to spawn Boss
	 */
	private void spawnBoss0(Boss boss, Location location) {
		final boolean success = boss.spawn(location, BossSpawnReason.COMMAND) != null;
		final String locationString = SerializeUtil.serializeLocation(location);

		if (success)
			this.tellSuccess(Lang.component("command-spawn-success", "boss", boss.getName(), "location", locationString)
					.onHoverLegacy("Click to copy coordinates " + locationString)
					.onClickSuggestCmd(location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()));
		else
			this.tellError(Lang.component("command-spawn-fail", "boss", boss.getName(), "location", locationString));
	}

	/*
	 * Executor method to spawn boss eggs
	 */
	private void spawnBossEgg0(Boss boss, Location location) {
		Remain.spawnItem(location, boss.getEgg(), null);

		this.tellSuccess(Lang.component("command-spawn-success-egg", "boss", boss.getName(), "location", SerializeUtil.serializeLocation(location)));
	}

	/**
	 * Tab complete last word with Bosses names.
	 *
	 * @return
	 */
	protected final List<String> completeLastWordBossNames() {
		return this.completeLastWord(Boss.getBossesNames(), "");
	}
}
