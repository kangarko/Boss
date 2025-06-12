package org.mineacademy.boss.skill;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.EntityUtil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This is a skill that is only applied for the target of the Boss.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractTargetSkill extends BossSkill {

	/**
	 * @see org.mineacademy.boss.skill.BossSkill#execute(org.bukkit.entity.LivingEntity)
	 */
	@Override
	public final boolean execute(LivingEntity boss) {
		final Entity targetEntity = EntityUtil.getTarget(boss);

		if (!(targetEntity instanceof Player))
			return false;

		if (!targetEntity.getLocation().getWorld().equals(boss.getWorld()))
			return false;

		final Player target = (Player) targetEntity;

		if (!Boss.canTarget(target))
			return false;

		if (target.getLocation().distance(boss.getLocation()) > Settings.Skills.TARGET_RANGE)
			return false;

		final boolean result = this.execute(target, boss);

		if (result)
			this.executeSkillCommands(target, boss);

		return result;
	}

	/**
	 * Execute this skill for the player that the Boss is targetting.
	 *
	 * @param target
	 * @param boss
	 * @return
	 */
	protected abstract boolean execute(Player target, LivingEntity boss);
}
