package org.mineacademy.boss.api;

import org.bukkit.block.Biome;
import org.mineacademy.boss.util.AutoUpdateMap;
import org.mineacademy.fo.model.ConfigSerializable;

/**
 * Configure the natural appearance of the Boss.
 */
public interface BossSpawning extends ConfigSerializable {

	/**
	 * Return a map of all worlds in which this Boss
	 * is being spawned, along with chances from 0 to 100%
	 * that when a new entity of the same type is spawned
	 * naturally, it will be transformed to a Boss.
	 * <p>
	 * Note: Before entities become Boss, they must pass
	 * all the other criteria in this class.
	 * <p>
	 * YOU CAN MODIFY THIS MAP DIRECTLY
	 *
	 * @return the map of the worlds with spawn chances
	 */
	AutoUpdateMap<String, Integer> getWorlds();

	/**
	 * Return a map of all biomes in which this Boss
	 * is being spawned, along with chances from 0 to 100%.
	 * <p>
	 * Note: Before entities become Boss, they must pass
	 * all the other criteria in this class.
	 * <p>
	 * YOU CAN MODIFY THIS MAP DIRECTLY
	 *
	 * @return the map of the biomes with spawn chances
	 */
	AutoUpdateMap<Biome, Integer> getBiomes();

	/**
	 * Spawning conditions by regions.
	 * <p>
	 * NOTICE: If any of these is NOT EMPTY, a so called "region spawning"
	 * is enabled, meaning the Boss will only spawn there!
	 */
	BossRegionSpawning getRegions();

	/**
	 * Get the default conditions
	 *
	 * @return the default conditions
	 */
	BossNativeConditions getConditions();
}
