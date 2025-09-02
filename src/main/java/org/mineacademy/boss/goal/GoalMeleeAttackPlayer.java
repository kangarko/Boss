package org.mineacademy.boss.goal;

import java.util.EnumSet;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

// Separate goal for melee attack behavior
public class GoalMeleeAttackPlayer<T extends Mob> implements Goal<T> {

	private final T mob;
	private final double attackRange;
	private final double speedModifier;
	private int attackCooldown;

	public GoalMeleeAttackPlayer(T mob) {
		this.mob = mob;
		this.attackRange = 2.0; // Standard melee range
		this.speedModifier = 1.0;
		this.attackCooldown = 0;
	}

	@Override
	public boolean shouldActivate() {
		final LivingEntity target = this.mob.getTarget();

		return target != null && target instanceof Player && !target.isDead();
	}

	@Override
	public boolean shouldStayActive() {
		final LivingEntity target = this.mob.getTarget();
		return target != null && target.isValid() && !target.isDead() && this.mob.getWorld().equals(target.getWorld()) && this.mob.getLocation().distance(target.getLocation()) < 20.0;
	}

	@Override
	public void start() {
		this.mob.getPathfinder().moveTo(this.mob.getTarget(), this.speedModifier);
	}

	@Override
	public void stop() {
		this.mob.getTarget();
		this.mob.getPathfinder().stopPathfinding();
	}

	@Override
	public void tick() {
		final LivingEntity target = this.mob.getTarget();

		if (target == null)
			return;

		final double distance = this.mob.getLocation().distance(target.getLocation());

		// Move towards target if too far
		if (distance > this.attackRange)
			this.mob.getPathfinder().moveTo(target, this.speedModifier);

		// Attack if in range and cooldown is ready
		if (distance <= this.attackRange && this.attackCooldown <= 0) {
			this.mob.attack(target);

			this.attackCooldown = 20; // 1 second cooldown (20 ticks)
		}

		// Decrement cooldown
		if (this.attackCooldown > 0)
			this.attackCooldown--;

		// Look at target
		this.mob.lookAt(target);
	}

	@Override
	public GoalKey<T> getKey() {
		return (GoalKey<T>) GoalKey.of(Mob.class, new NamespacedKey("Boss", "melee_attack"));
	}

	@Override
	public EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
	}
}