package org.mineacademy.boss.api;

/**
 * Why the {@link Boss} is spawned?
 */
public enum BossSpawnReason {

	/**
	 * From a Boss monster egg.
	 */
	EGG,

	/**
	 * A Boss that was dispensed from an egg.
	 */
	DISPENSE,

	/**
	 * As another Boss when the main Boss dies.
	 */
	REINFORCEMENTS,

	/**
	 * A creature spawned naturally was spontaneously transformed into Boss.
	 */
	CONVERTED,

	/**
	 * Timed spawning via the natural spawning in settings.yml
	 */
	TIMED,

	/**
	 * The boss has been summoned via a /boss command.
	 */
	COMMAND,

	/**
	 * When a boss slime spits out their babies!
	 */
	SLIME_SPLIT,

	/**
	 * The Boss is spawned to ride another Boss
	 */
	RIDING,

	/**
	 * The boss summoned naturally from a Boss Spawner
	 */
	SPAWNER,

	/**
	 * Unknown reason.
	 */
	CUSTOM
}
