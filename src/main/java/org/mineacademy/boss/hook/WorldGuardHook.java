package org.mineacademy.boss.hook;

import org.bukkit.Location;
import org.mineacademy.fo.Common;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import lombok.Getter;

public final class WorldGuardHook {

	/*
	 * Private flag since MC 1.16+ is needed
	 */
	private static boolean loaded = false;

	/**
	 * Hook into WorldGuard
	 */
	public static void init() {
		BossTargetFlag.register();

		loaded = true;
	}

	/**
	 * Return if Boss can target entity at the location
	 *
	 * @param location
	 * @return
	 */
	public static boolean canTarget(Location location) {
		if (!loaded)
			return true;

		return BossTargetFlag.canTarget(location);
	}
}

class BossTargetFlag extends BooleanFlag {

	@Getter
	static final BossTargetFlag instance = new BossTargetFlag();

	BossTargetFlag() {
		super("boss-target");
	}

	static void register() {
		WorldGuard.getInstance().getFlagRegistry().register(instance);
	}

	static boolean canTarget(Location location) {
		try {
			final RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(location.getWorld()));

			if (manager != null) {
				final ApplicableRegionSet regions = manager.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));

				for (final ProtectedRegion region : regions) {
					final Boolean flagValue = region.getFlag(instance);

					if (flagValue != null && flagValue == false)
						return false;
				}
			}

		} catch (final NoClassDefFoundError t) {
			Common.logTimed(60 * 60, "WorldGuard incompatible with Boss, got " + t + ". This message only shows once per hour.");
		}

		return true;
	}
}
