package org.mineacademy.boss.spawn;

import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the different types of spawning rules
 */
@RequiredArgsConstructor
public enum SpawnRuleType {

	/**
	 * Timed spawning at the given location.
	 */
	LOCATION_PERIOD(SpawnRuleLocationPeriod.class, CompMaterial.CLOCK, "Spawn in selected locations"),

	/**
	 * Spawn the next boss after the previous is killed
	 */
	RESPAWN_AFTER_DEATH(SpawnRuleRespawn.class, CompMaterial.BONE, "Spawn the next boss after the previous is killed"),

	/**
	 * Spawning when a region is entered by player.
	 */
	REGION_ENTER(SpawnRuleRegionEnter.class, CompMaterial.GLASS, "Spawn when players enter a region"),

	/**
	 * Timed spawning around bosses.
	 */
	PERIOD(SpawnRuleRandomPeriod.class, CompMaterial.COMPASS, "Randomly spawn around players"),

	/**
	 * Replacing vanilla entities
	 */
	REPLACE_VANILLA(SpawnRuleRandomVanilla.class, CompMaterial.PIG_SPAWN_EGG, "Replace vanilla mobs when they spawn");

	/**
	 * The class holding a spawn rule with its settings.
	 */
	@Getter
	private final Class<? extends SpawnRule> spawnRuleClass;

	/**
	 * The icon type
	 */
	@Getter
	private final CompMaterial iconType;

	/**
	 * The desc
	 */
	@Getter
	private final String description;
}
