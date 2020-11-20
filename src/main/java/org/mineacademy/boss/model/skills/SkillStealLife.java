package org.mineacademy.boss.model.skills;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompParticle;
import org.mineacademy.fo.remain.CompSound;

public final class SkillStealLife extends AbstractTargetSkill {

	private int percent;

	@Override
	public String getName() {
		return "Steal Life";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("2 minute", "4 minutes");
	}

	@Override
	public boolean execute(Player target, SpawnedBoss spawned) {
		final double take = target.getHealth() / 100 * percent;
		final double rest = target.getHealth() - take;

		{ // Player
			target.setHealth(MathUtil.range(rest, 0, target.getHealth()));

			CompSound.HURT_FLESH.play(target, 1F, 1);
			CompParticle.CRIT_MAGIC.spawn(target.getEyeLocation());
		}

		{ // Boss
			final LivingEntity boss = spawned.getEntity();
			final double bossHealh = spawned.getEntity().getHealth();

			boss.setHealth(MathUtil.range(bossHealh + take, bossHealh, boss.getMaxHealth()));

			CompParticle.SPELL_WITCH.spawn(boss.getEyeLocation().clone().add(boss.getVelocity().normalize()));
			CompParticle.SPELL_WITCH.spawn(boss.getEyeLocation().clone().add(0, 0.5, 0));

		}

		sendSkillMessage(target, spawned);
		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"You have been &cstolen &7your health to the Boss!",
				"Your lives have been &cdecreased&7 by the {boss}!",
				"The {boss} has &cdrained &7your health!"
		};
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.FERMENTED_SPIDER_EYE,
				"Steal Life",
				"",
				"Boss will take a portion of",
				"player's health to his own!")
				.build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		percent = (int) map.getOrDefault("Percent", 20);
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();

		map.put("Percent", percent);

		return map;
	}

	@Override
	public String[] getDefaultHeader() {
		return new String[] {
				"  Percent - How big portion of player's health should the",
				"            Boss take from player and give to himself?",
				"            Set the value from 0 to 100 (in percent)"
		};
	}
}
