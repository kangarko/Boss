package org.mineacademy.boss.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.remain.Remain;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BossEggSpawnUtil {

	public Location getSpawnLocation(final Player playerWhoSpawns, final int range) {
		final Block target = Remain.getTargetBlock(playerWhoSpawns, range);

		return target != null && (target.getType() != Material.AIR || Settings.EggSpawning.AIR_SPAWN) ? moveToPlayerIfNecessary(target, playerWhoSpawns, range) : null;
	}

	public Location moveToPlayerIfNecessary(final Block block, final Player playerWhoSpawns, final int range) {
		if (block.getRelative(BlockFace.UP).getType().isSolid()) {
			final Location solidLocation = getFirstSolidNearLocation(playerWhoSpawns, range);

			if (solidLocation != null)
				return solidLocation;
		}

		return block.getLocation().clone().add(0.5, 0, 0.5);
	}

	// -----------------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------------

	private Location getFirstSolidNearLocation(final Player player, final int range) {
		final Block block = iterateFirstSolidBlock(player, range);

		return block != null ? moveCloserToPlayer(block.getLocation(), player.getLocation()) : null;
	}

	private Block iterateFirstSolidBlock(final Player player, final int range) {
		final BlockIterator iterator = new BlockIterator(player, range);

		while (iterator.hasNext()) {
			final Block block = iterator.next();

			if (iterator.hasNext() && block.getType().isSolid())
				return block;
		}

		return null;
	}

	private Location moveCloserToPlayer(final Location loc, final Location playerLoc) {
		if (playerLoc.getBlockX() < loc.getBlockX())
			loc.add(-1.5, 0, 0);

		if (playerLoc.getBlockZ() < loc.getBlockZ())
			loc.add(0, 0, -1.5);

		if (playerLoc.getBlockX() > loc.getBlockX())
			loc.add(1.5, 0, 0);

		if (playerLoc.getBlockZ() > loc.getBlockZ())
			loc.add(0, 0, 1.5);

		return loc;
	}
}
