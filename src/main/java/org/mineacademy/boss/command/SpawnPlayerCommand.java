package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

final class SpawnPlayerCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	SpawnPlayerCommand() {
		super("spawnpl");

		this.setValidArguments(2, 2);
		this.setDescription(Lang.component("command-spawn-player-description"));
		this.setUsage("<player> <boss1|boss2|random>");
	}

	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} kangarko Blaze - Spawns the Blaze boss near kangarko.",
				"/{label} {sublabel} kangarko Blaze|random Spawns Blaze and a random boss near kangarko."
		};
	}

	@Override
	protected void onCommand() {
		final Player target = this.findPlayer(this.args[0]);
		final Location location = target.getLocation();
		final Vector vector = location.getDirection().multiply(1);

		location.subtract(vector.normalize().multiply(2));

		Block block = location.getBlock();

		while (!CompMaterial.isAir(block)) {
			location.add(0, 1, 0);

			block = location.getBlock();
		}

		this.spawnBosses(false, location, 1);
	}

	@Override
	public List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWordPlayerNames();
			case 2:
				return this.completeLastWord(Boss.getBossesNames(), "random");
		}

		return NO_COMPLETE;
	}
}