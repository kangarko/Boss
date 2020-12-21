package org.mineacademy.boss.model.skills;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillConfuse extends AbstractTargetSkill {

	@Override
	public String getName() {
		return "Confuse Player";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("10 seconds", "15 seconds");
	}

	@Override
	public boolean execute(Player target, SpawnedBoss spawned) {
		final Location loc = target.getLocation();

		loc.setYaw(loc.getYaw() + 45 + RandomUtil.nextInt(270));

		target.teleport(loc);
		target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 3, 0), true);

		sendSkillMessage(target, spawned);
		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"You have been &cconfused &7by the boss!",
				"OMG! What is happening?",
				"You were teleported to the wonderlands! Just kidding."
		};
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.GUNPOWDER,
				"Confuse Player",
				"",
				"Give player blindness",
				"and rotate him randomly.")
				.build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
	}

	@Override
	public Map<String, Object> writeSettings() {
		return new HashMap<>();
	}
}
