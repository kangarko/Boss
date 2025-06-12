package org.mineacademy.boss.hook;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.mineacademy.boss.model.Boss;

import net.citizensnpcs.api.ai.event.CancelReason;
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import net.citizensnpcs.api.npc.NPC;

public class BossTargetNearbyEntityGoal extends BehaviorGoalAdapter {

	private final boolean aggressive;
	private boolean finished;
	private final NPC npc;
	private final double radius;
	private CancelReason reason;
	private Entity target;
	private final Set<EntityType> targets;

	private BossTargetNearbyEntityGoal(final NPC npc, final Set<EntityType> targets, final boolean aggressive,
			final double radius) {
		this.npc = npc;
		this.targets = targets;
		this.aggressive = aggressive;
		this.radius = radius;
	}

	@Override
	public void reset() {
		this.npc.getNavigator().cancelNavigation();
		this.target = null;
		this.finished = false;
		this.reason = null;
	}

	@Override
	public BehaviorStatus run() {
		if (this.finished)
			return (this.reason == null) ? BehaviorStatus.SUCCESS : BehaviorStatus.FAILURE;
		return BehaviorStatus.RUNNING;
	}

	@Override
	public boolean shouldExecute() {

		if (this.targets.size() == 0 || !this.npc.isSpawned())
			return false;

		final Collection<Entity> nearby = this.npc.getEntity().getNearbyEntities(this.radius, this.radius, this.radius);
		this.target = null;

		// MineAcademy start - ignore bad targets
		nearby.removeIf(en -> !Boss.canTarget(en));
		// MineAcademy end

		for (final Entity entity : nearby)
			if (this.targets.contains(entity.getType())) {
				this.target = entity;

				break;
			}

		if (this.target != null) {
			this.npc.getNavigator().setTarget(this.target, this.aggressive);
			this.npc.getNavigator().getLocalParameters()
					.addSingleUseCallback(cancelReason -> {
						BossTargetNearbyEntityGoal.this.reason = cancelReason;
						BossTargetNearbyEntityGoal.this.finished = true;
					});
			return true;
		}
		return false;
	}

	public static Builder builder(final NPC npc) {
		return new Builder(npc);
	}

	public static class Builder {
		private boolean aggressive;
		private final NPC npc;
		private double radius;
		private Set<EntityType> targetTypes;

		public Builder(final NPC npc) {
			this.radius = 10.0;
			this.targetTypes = new HashSet<>();
			this.npc = npc;
		}

		public Builder aggressive(final boolean aggressive) {
			this.aggressive = aggressive;
			return this;
		}

		public BossTargetNearbyEntityGoal build() {
			return new BossTargetNearbyEntityGoal(this.npc, this.targetTypes, this.aggressive, this.radius);
		}

		public Builder radius(final double radius) {
			this.radius = radius;
			return this;
		}

		public Builder targets(final Set<EntityType> targetTypes) {
			this.targetTypes = targetTypes;
			return this;
		}
	}
}