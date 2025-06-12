package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.World;
import org.mineacademy.boss.listener.ChunkListener;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Common;

/**
 * The command to debug unloaded Bosses.
 */
final class CountUnloadedCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	CountUnloadedCommand() {
		super("countunloaded");

		this.setValidArguments(1, 2);
		this.setUsage("<save/load> OR <world> <Boss/all>");
		this.setDescription("A command to count unloaded Bosses.");
	}

	@Override
	protected boolean showInHelp() {
		return false;
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final String param = this.args[0].toLowerCase();

		if ("save".equals(param)) {
			ChunkListener.saveToFile();

			this.tellSuccess("Saved unloaded Bosses to file.");
			return;

		} else if ("load".equals(param)) {
			ChunkListener.loadFromFile();

			this.tellSuccess("Loaded unloaded Bosses from file.");
			return;
		} else
			this.checkArgs(2, this.getUsage());

		final World world = this.findWorld(param);
		final String bossType = this.args[1].toLowerCase();

		if ("all".equals(bossType)) {
			this.tellInfo("Counting all Bosses in world " + world.getName());

			this.tell("&7In loaded chunks: &f" + Boss.findBossesAliveIn(world).size());
			this.tell("&7In unloaded chunks: &f" + ChunkListener.countAllBosses(world));

		} else {
			final Boss boss = this.findBoss(this.args[1]);

			this.tellInfo("Counting " + boss.getName() + " Bosses in world " + world.getName());

			this.tell("&7In loaded chunks: &f" + Boss.findBossesAliveIn(world, boss).size());
			this.tell("&7In unloaded chunks: &f" + ChunkListener.countUnloadedBosses(world, boss));
		}
	}

	@Override
	protected List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWord("save", "load", Common.getWorldNames());

			case 2:
				return this.completeLastWord(Boss.getBossesNames(), "all");
		}

		return NO_COMPLETE;
	}
}