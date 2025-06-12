package org.mineacademy.boss.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.model.Boss;

import lombok.NoArgsConstructor;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.flags.type.NaturalFlag;
import me.angeschossen.lands.api.land.Area;
import me.angeschossen.lands.api.land.LandWorld;

/**
 * A hook for the Lands plugin
 */
@NoArgsConstructor
public final class LandsHook {

	/**
	 * Whether the Lands plugin is enabled
	 */
	private static boolean enabled;

	/**
	 * Set whether the Lands plugin is enabled
	 *
	 * @param enabled
	 */
	public static void setEnabled(boolean enabled) {
		LandsHook.enabled = enabled;
	}

	/**
	 * Get all lands in the server or an empty list if not found
	 *
	 * @return
	 */
	public static List<String> getLands() {
		return enabled ? Integration.getLands() : new ArrayList<>();
	}

	/**
	 * Find the land at the given location or null if not found
	 *
	 * @param location
	 * @return
	 */
	public static String findLand(Location location) {
		return enabled ? Integration.findLand(location) : null;
	}

	/**
	 * Check if the given boss can spawn at the given location
	 *
	 * @param location
	 * @param boss
	 * @return
	 */
	public static boolean canSpawn(Location location, Boss boss) {
		return enabled ? Integration.canSpawn(location, boss) : true;
	}
}

/**
 * The actual implementation to prevent no class def error.
 */
final class Integration {

	/**
	 * The API connector
	 */
	private static LandsIntegration api;

	static List<String> getLands() {
		init();

		return api.getLands().stream().map(land -> land.getName()).collect(Collectors.toList());
	}

	static String findLand(Location location) {
		init();

		final Area area = api.getArea(location);

		return area != null ? area.getLand().getName() : null;
	}

	static boolean canSpawn(Location location, Boss boss) {
		init();

		final LandWorld world = api.getWorld(location.getWorld());

		if (world != null) {
			final EntityType type = boss.getType();
			NaturalFlag flag = null;

			if (type == EntityType.PHANTOM)
				flag = Flags.PHANTOM_SPAWN;

			else if (Animals.class.isAssignableFrom(type.getEntityClass()))
				flag = Flags.ANIMAL_SPAWN;

			else
				flag = Flags.MONSTER_SPAWN;

			return world.hasNaturalFlag(location, flag);
		}

		return true;
	}

	/*
	 * Injects this plugin into Lands API if not done previously
	 */
	private static void init() {
		if (api == null)
			api = LandsIntegration.of(BossPlugin.getInstance());
	}
}