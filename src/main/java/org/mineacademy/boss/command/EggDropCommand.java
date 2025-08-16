package org.mineacademy.boss.command;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * The command to spawn Bosses.
 */
final class EggDropCommand extends BossEggAbstractCommand {

	/**
	 * Create new command.
	 */
	EggDropCommand() {
		super("eggdrop");

		this.setValidArguments(5, 5);
		this.setDescription("Drops an egg of Boss(es) at a location.");
		this.setUsage("<world x y z> <boss1|boss2|random>");
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#getMultilineUsageMessage()
	 */
	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} ~ ~ ~ ~ Blaze - Drops the Blaze boss egg near you.",
				"/{label} {sublabel} ~ ~ 15 ~ Blaze - Drops the Blaze boss egg at same X/Z coordinates at you but at y=15.",
				"/{label} {sublabel} minigames 100 15 50 Blaze|Zombie - Drops the Blaze and Zombie boss eggs in minigames world at the xyz location.",
				"/{label} {sublabel} minigames 100 15 50 random - Drops a random boss egg in minigames world at the xyz location.",
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

		this.spawnBosses(true, location, 4);
	}
}