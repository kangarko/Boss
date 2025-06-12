package org.mineacademy.boss.spawn;

import org.bukkit.Location;
import org.mineacademy.fo.Valid;

/**
 * Spawn rule to replace vanilla mobs
 */
public final class SpawnRuleRandomVanilla extends SpawnRuleRandom {

	/**
	 * Create new rule.
	 *
	 * @param name
	 */
	public SpawnRuleRandomVanilla(String name) {
		super(name, SpawnRuleType.REPLACE_VANILLA);
	}

	/* ------------------------------------------------------------------------------- */
	/* Logic */
	/* ------------------------------------------------------------------------------- */

	@Override
	public void onTick(SpawnData data) {
		final Location location = data.getLocation();
		Valid.checkNotNull(location);

		if (this.canRun() && this.canRun(location))
			this.spawn(location, data);
	}
}
