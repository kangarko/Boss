package org.mineacademy.boss.model.skills;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillFire extends AbstractTargetSkill {

	private int durationTicks;
	private String durationRaw;

	@Override
	public String getName() {
		return "Ignite Player";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("20 seconds", "50 seconds");
	}

	@Override
	public boolean execute(Player target, SpawnedBoss spawned) {
		target.setFireTicks(durationTicks);
		sendSkillMessage(target, spawned);

		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"You have been &cignited &7by the boss!",
				"The {boss} has &cignited you&7!",
				"It is getting warmer and warmer. Wonder why?"
		};
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.FLINT_AND_STEEL,
				"Ignite Player",
				"",
				"Set the player",
				"on fire.")
				.build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		durationRaw = map.getOrDefault("Duration", "5 seconds").toString();
		durationTicks = (int) TimeUtil.toTicks(durationRaw);
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();

		map.put("Duration", durationRaw);

		return map;
	}

	@Override
	public String[] getDefaultHeader() {
		return new String[] {
				"  Duration - How long should the player be on fire?",
		};
	}
}
