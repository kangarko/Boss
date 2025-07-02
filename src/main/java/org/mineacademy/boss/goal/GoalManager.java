package org.mineacademy.boss.goal;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;

import com.destroystokyo.paper.entity.ai.GoalType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A utility class for managing pathfinders in Paper API.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GoalManager {

	/**
	 * If the Goal API is available.
	 */
	private static final boolean available;

	static {
		boolean isAvailable = false;

		try {
			Class.forName("com.destroystokyo.paper.entity.ai.Goal");

			Bukkit.class.getMethod("getMobGoals");
			isAvailable = true;

		} catch (final ClassNotFoundException | NoSuchMethodException e) {
			// Goal API is not available
		}

		available = isAvailable;
	}

	/**
	 * Checks if the Goal API is available.
	 *
	 * @return true if the Goal API is available, false otherwise
	 */
	public static boolean isAvailable() {
		return available;
	}

	/**
	 * Makes the given mob aggressive towards players, allowing it to attack them.
	 *
	 * @param mob the mob to make aggressive
	 */
	public static void makeAggressive(Mob mob) {
		if (!available)
			return;

		// Check and register attack damage attribute if missing
		if (mob.getAttribute(Attribute.ATTACK_DAMAGE) == null) {
			mob.registerAttribute(Attribute.ATTACK_DAMAGE);

			mob.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(3.0);
		}

		Bukkit.getMobGoals().removeAllGoals(mob, GoalType.TARGET);

		Bukkit.getMobGoals().addGoal(mob, 0, new GoalPlayerTarget<>(mob));
		Bukkit.getMobGoals().addGoal(mob, 1, new GoalMeleeAttackPlayer<>(mob));

		// Ensure AI remains active
		mob.setAware(true);
	}
}