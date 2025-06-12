package org.mineacademy.boss.spawn;

import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.debug.Debugger;

import lombok.Getter;

/**
 * Rule to spawn mobs at a given time on certain blocks.
 */
@Getter
public class SpawnRuleRespawn extends SpawnRuleLocationData {

	/**
	 * Create new rule with the given name
	 *
	 * @param name
	 */
	public SpawnRuleRespawn(String name) {
		super(name, SpawnRuleType.RESPAWN_AFTER_DEATH);
	}

	/* ------------------------------------------------------------------------------- */
	/* Logic */
	/* ------------------------------------------------------------------------------- */

	@Override
	protected boolean checkLastExecuted() {
		return false;
	}

	@Override
	protected boolean canRun(Boss boss, SpawnData data) {

		for (final SpawnedBoss spawned : Boss.findBossesAlive()) {
			final String spawnRuleName = spawned.getSpawnRuleName();

			if (spawnRuleName != null && this.getName().equals(spawnRuleName)) {
				Debugger.debug("spawning-respawn", "Skip spawning " + boss.getName() + " because another boss is already spawned from rule " + this.getName());

				return false;
			}
		}

		final long now = System.currentTimeMillis();
		final long lastDeathTime = boss.getLastDeathFromSpawnRule(this);

		if (lastDeathTime != 0 && (now - lastDeathTime) < this.getDelay().getTimeMilliseconds()) {
			Debugger.debug("spawning-respawn", "Skip spawning " + boss.getName() + " because the last death was less than " + this.getDelay().getTimeSeconds() + " sec ago (" + (now - lastDeathTime) / 1000 + ")");

			return false;
		}

		return true;
	}
}
