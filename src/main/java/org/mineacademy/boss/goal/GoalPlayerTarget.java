package org.mineacademy.boss.goal;

import java.util.EnumSet;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

public class GoalPlayerTarget<T extends Mob> implements Goal<T> {

	private final T mob;
	private final double targetRange;
	private Player targetPlayer;

	public GoalPlayerTarget(T mob) {
		this.mob = mob;
		this.targetRange = 16.0;
	}

	@Override
	public boolean shouldActivate() {
		this.targetPlayer = this.mob.getWorld().getPlayers().stream()
				.filter(player -> player.getLocation().distance(this.mob.getLocation()) <= this.targetRange)
				.min((p1, p2) -> Double.compare(
						p1.getLocation().distance(this.mob.getLocation()),
						p2.getLocation().distance(this.mob.getLocation())))
				.orElse(null);

		return this.targetPlayer != null && !this.targetPlayer.isDead();
	}

	@Override
	public void start() {
		this.mob.setTarget(this.targetPlayer);
		this.mob.getPathfinder().moveTo(this.targetPlayer);
	}

	@Override
	public void tick() {
		if (this.targetPlayer != null && !this.mob.getPathfinder().hasPath())
			this.mob.getPathfinder().moveTo(this.targetPlayer);
	}

	@Override
	public GoalKey<T> getKey() {
		return (GoalKey<T>) GoalKey.of(Mob.class,
				new org.bukkit.NamespacedKey("Boss", "player_target"));
	}

	@Override
	public EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.TARGET);
	}
}
