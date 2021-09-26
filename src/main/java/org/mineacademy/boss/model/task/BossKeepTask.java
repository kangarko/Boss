package org.mineacademy.boss.model.task;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.boss.api.BossRegionType;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.util.BossTaggingUtil;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMaterial;

/**
 * A task that keeps Bosses in their region.
 */
public final class BossKeepTask implements Runnable {

	@Override
	public void run() {
		LagCatcher.start("Boss Keep Task");

		for (final World w : Bukkit.getWorlds())
			bossScan:
			for (final SpawnedBoss boss : BossPlugin.getBossManager().findBosses(w)) {
				final String keepInside = BossTaggingUtil.getTagKeepInside(boss.getEntity());

				if (keepInside == null)
					continue bossScan;

				final BossRegionSettings settings = boss.getBoss().getSpawning().getRegions().findRegion(BossRegionType.BOSS, keepInside);

				if (settings == null || !settings.getKeepInside())
					continue bossScan;

				final Region region = BossRegionType.BOSS.getBoundingBox(keepInside);

				if (region == null)
					continue bossScan;

				final Location loc = boss.getEntity().getLocation();

				// Boss escaped.. !
				if (!region.isWithin(loc)) {
					Location closestLoc = null;
					double closest = Double.MAX_VALUE;

					// Find closes location in the border that has enough space
					locationScan:
					for (final Location regionWall : BlockUtil.getBoundingBox(region.getPrimary(), region.getSecondary())) {
						if (regionWall.getBlock().getType() != Material.AIR || regionWall.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR)
							continue locationScan;

						final double distance = regionWall.distance(loc);

						if (distance < closest) {
							closestLoc = regionWall;

							closest = distance;
						}
					}

					// Pull the Boss one block beyond the border
					if (closestLoc != null) {
						closestLoc = moveCloserToRegion(closestLoc, loc);

						if (Settings.RegionKeep.PORT_TO_CENTER) {
							Location safeCenter = region.getCenter().clone();

							while (!CompMaterial.isAir(safeCenter.getBlock()))
								safeCenter = safeCenter.add(0, 1, 0);

							boss.getEntity().teleport(safeCenter);
						} else
							boss.getEntity().teleport(closestLoc);
					}

					// Find if the entity targets a player that is outside of the region and de-target it
					final Player target = EntityUtil.getTargetPlayer(boss.getEntity());

					if (target != null && !region.isWithin(target.getLocation()))
						((Creature) boss.getEntity()).setTarget(null);
				}
			}

		LagCatcher.end("Boss Keep Task");
	}

	private Location moveCloserToRegion(final Location loc, final Location region) {
		if (region.getBlockX() < loc.getBlockX())
			loc.add(1.5, 0, 0);

		if (region.getBlockZ() < loc.getBlockZ())
			loc.add(0, 0, 1.5);

		if (region.getBlockX() > loc.getBlockX())
			loc.add(-2.5, 0, 0);

		if (region.getBlockZ() > loc.getBlockZ())
			loc.add(0, 0, -1.5);

		if (loc.getBlock().getType() != Material.AIR)
			loc.add(0, 1, 0);

		return loc;
	}
}
