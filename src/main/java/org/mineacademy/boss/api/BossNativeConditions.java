package org.mineacademy.boss.api;

import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.RangedValue;

/**
 * Native spawning conditions
 */
public interface BossNativeConditions extends ConfigSerializable {

	/**
	 * Get the required time frame
	 *
	 * @return the required time frame
	 */
	RangedValue getTime();

	/**
	 * Set the required time.
	 *
	 * @param time the time
	 */
	void setTime(RangedValue time);

	/**
	 * Get the required y-height in the world
	 *
	 * @return the required y-height coordinate
	 */
	RangedValue getHeight();

	/**
	 * Set the required height.
	 *
	 * @param height the height
	 */
	void setHeight(RangedValue height);

	/**
	 * Get the required light level (0-15)
	 *
	 * @return the required light level
	 */
	RangedValue getLight();

	/**
	 * Set the required light level.
	 *
	 * @param lightLevel the light level
	 */
	void setLight(RangedValue lightLevel);

	/**
	 * Get if the Boss requires rain.
	 *
	 * @return whether rain is required
	 */
	boolean isRainRequired();

	/**
	 * Set the flag.
	 *
	 * @param flag the flag
	 */
	void setRainRequired(boolean flag);

	/**
	 * Get if the Boss requires thunderstorm.
	 *
	 * @return whether thunderstorm is required
	 */
	boolean isThunderRequired();

	/**
	 * Set the flag.
	 *
	 * @param flag the flag
	 */
	void setThunderRequired(boolean flag);

	/**
	 * Return true if the Boss is spawning on the upper-most
	 * block on the ground, false if we can spawn him under water
	 */
	boolean canSpawnUnderWater();

	/**
	 * Set true if the Boss is spawning on the upper-most
	 * block on the ground, false if we can spawn him under water
	 *
	 * @param spawnUnderWater
	 */
	void setSpawnUnderWater(boolean spawnUnderWater);
}
