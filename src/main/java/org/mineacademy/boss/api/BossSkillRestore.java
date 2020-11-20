package org.mineacademy.boss.api;

import org.bukkit.entity.Player;

/**
 * Interface used when taking away skill effects from players.
 * This is fired either after the skill duration ended,
 * or when the player logs back in.
 */
public interface BossSkillRestore {

	/**
	 * Removes the skill effect from player (i.e. restore fly speed etc.)
	 *
	 * @param player the online player
	 */
	void removeSkillEffect(Player player);
}
