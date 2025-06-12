package org.mineacademy.boss.skill;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillConfuse extends AbstractTargetSkill {

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("10 seconds - 15 seconds");
	}

	@Override
	public boolean execute(Player target, LivingEntity bossEntity) {
		final Location loc = target.getLocation();

		loc.setYaw(loc.getYaw() + 45 + RandomUtil.nextInt(270));

		target.teleport(loc);
		target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 3, 0), true);

		this.sendSkillMessage(target, bossEntity);
		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-confuse-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.GUNPOWDER,
				"Confuse",
				"",
				"Give player blindness",
				"and rotate him randomly.")
				.make();
	}
}
