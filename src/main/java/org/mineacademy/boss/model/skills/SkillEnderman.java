package org.mineacademy.boss.model.skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.util.BossUtil;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillEnderman extends BossSkill {

	/**
	 * How many blocks away from the Boss to look for?
	 */
	private int radius;

	@Override
	public String getName() {
		return "Enderman";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("45 seconds", "2 minutes");
	}

	@Override
	public boolean execute(SpawnedBoss spawned) {
		final LivingEntity en = spawned.getEntity();
		final List<Player> found = new ArrayList<>();

		for (final Entity nearby : en.getNearbyEntities(radius, radius - 1, radius))
			if (nearby instanceof Player)
				found.add((Player) nearby);

		if (!found.isEmpty()) {
			Collections.shuffle(found);

			final Player target = found.get(0);

			if (BossUtil.canBossTarget(target)) {
				en.teleport(target.getLocation());
				sendSkillMessage(target, spawned);
			}
		}

		return false;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"I was lonely far from you so I wanted to say hey",
				"Say hey! {boss} has &cteleported itself to you &7!",
				"I thought we were friends, so here I come!"
		};
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.ENDER_PEARL,
				"Enderman",
				"",
				"Boss will teleport itself",
				"to a random player.")
				.build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		radius = (int) map.getOrDefault("Radius", 5);
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();

		map.put("Radius", radius);

		return map;
	}

	@Override
	public String[] getDefaultHeader() {
		return new String[] {
				"  Radius - How many blocks around should the Boss search for players",
				"           to teleport to them?",
		};
	}
}
