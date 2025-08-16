package org.mineacademy.boss.command;

import org.bukkit.Location;
import org.bukkit.World;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to spawn Bosses.
 */
final class SpawnCommand extends BossEggAbstractCommand {

	/**
	 * Create new command.
	 */
	SpawnCommand() {
		super("spawn");

		this.setValidArguments(5, 5);
		this.setDescription(Lang.component("command-spawn-description"));
		this.setUsage("<world x y z> <boss1|boss2|random>");
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#getMultilineUsageMessage()
	 */
	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} ~ ~ ~ ~ Blaze - Spawns the Blaze boss near you.",
				"/{label} {sublabel} ~ ~ 15 ~ Blaze - Spawns the Blaze boss at same X/Z coordinates at you but at y=15.",
				"/{label} {sublabel} minigames 100 15 50 Blaze|Zombie - Spawns the Blaze and Zombie boss in minigames world at the xyz location.",
				"/{label} {sublabel} minigames 100 15 50 random - Spawns a random boss in minigames world at the xyz location.",
		};
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final World world = this.findWorld(this.args[0]);

		final double x = this.parseCoordinate(1);
		final double y = this.parseCoordinate(2);
		final double z = this.parseCoordinate(3);

		final Location location = new Location(world, x, y, z);

		this.spawnBosses(false, location, 4);
	}
}