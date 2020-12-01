package org.mineacademy.boss.api;

import org.bukkit.Location;
import org.mineacademy.fo.model.ConfigSerializable;

public interface BossRegionSpawning extends ConfigSerializable {

	/**
	 * Return if the location is within any regions or true if there are no regions
	 *
	 * @param loc the location
	 * @return if the location is within any regions, true if there are no regions
	 * @see #hasRegionSpawning()
	 */
	boolean isWithinAny(Location loc);

	/**
	 * Get the lowest region's height in this area
	 *
	 * @param loc location to check
	 * @return region height, or -1 if none
	 */
	int getLowestRegionHeight(Location loc);

	/**
	 * Get if any regions are configured.
	 * <p>
	 * By regions it is meant the {@link BossNativeConditions#getAll()} and all regions in {@link BossPluginConditions}
	 *
	 * @return if the region spawning is enabled
	 */
	boolean hasRegionSpawning();

	boolean hasRegions(BossRegionType type);

	boolean hasRegion(BossRegionType type, String regionName);

	BossRegionSettings findRegion(BossRegionType type, String name);

	int getCount(BossRegionType type);

	void remove(BossRegionType type, String regionName);

	void add(BossRegionType type, String regionName);

	void clear(BossRegionType type);

	boolean isBlacklist();

	void setBlacklist(boolean blacklist);

	String getFormatted();
}
