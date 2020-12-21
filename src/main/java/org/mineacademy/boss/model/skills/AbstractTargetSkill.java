package org.mineacademy.boss.model.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.util.BossUtil;
import org.mineacademy.fo.EntityUtil;

/**
 * This is a skill that is only applied for the target of the Boss.
 */
public abstract class AbstractTargetSkill extends BossSkill {

	@Override
	public final boolean execute(SpawnedBoss spawned) {
		final LivingEntity boss = spawned.getEntity();
		final Player target = EntityUtil.getTargetPlayer(boss);

		if (target != null && BossUtil.canBossTarget(target) && BossUtil.checkSameWorld(boss.getLocation(), target.getLocation()))
			return execute(target, spawned);

		return false;
	}

	protected abstract boolean execute(Player target, SpawnedBoss spawned);
}
