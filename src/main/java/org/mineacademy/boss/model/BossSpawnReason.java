package org.mineacademy.boss.model;

import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents why the Boss has been spawned
 */
@RequiredArgsConstructor
public enum BossSpawnReason {

	/**
	 * From egg
	 */
	EGG("From Boss Egg", CompMaterial.EGG),

	/**
	 * From /boss spawn
	 */
	COMMAND("From /Boss Spawn", CompMaterial.COMMAND_BLOCK),

	/**
	 * As riding Boss entity
	 */
	RIDING("Boss Riding Another Boss", CompMaterial.SADDLE),

	/**
	 * Death reinforcements
	 */
	REINFORCEMENTS("Boss Death Reinforcements", CompMaterial.SKELETON_SPAWN_EGG),

	/**
	 * When Boss slime split into smaller Boss slimes
	 */
	SLIME_SPLIT("Slime Boss Splitting", CompMaterial.SLIME_BALL),

	/**
	 * From spawn rule
	 */
	SPAWN_RULE("From Spawn Rule", CompMaterial.ENDER_EYE),

	/**
	 * From dispenser
	 */
	DISPENSE("From Dispenser", CompMaterial.DISPENSER),

	/**
	 * From API or unknown
	 */
	CUSTOM("From API (Other Plugins)", CompMaterial.CLAY_BALL);

	/**
	 * Get menu title
	 */
	@Getter
	private final String title;

	/**
	 * Get menu icon
	 */
	@Getter
	private final CompMaterial icon;
}
