package org.mineacademy.boss.model.skills;

import java.util.HashMap;
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

public final class SkillTeleport extends BossSkill {

	/**
	 * How many blocks away from the Boss to look for?
	 */
	private int radius;

	/**
	 * The maximum amount of players to teleport at once.
	 */
	private int players;

	@Override
	public String getName() {
		return "Teleport Player";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("45 seconds", "2 minutes");
	}

	@Override
	public boolean execute(SpawnedBoss spawned) {
		final LivingEntity en = spawned.getEntity();

		boolean success = false;
		int teleported = 0;

		for (final Entity nearby : en.getNearbyEntities(radius, radius - 1, radius))
			if (nearby instanceof Player) {
				if (!BossUtil.canBossTarget((Player) nearby))
					continue;

				if (teleported > players)
					break;

				nearby.teleport(en.getLocation());

				sendSkillMessage((Player) nearby, spawned);
				success = true;
				teleported++;
			}

		return success;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"You are now facing the {boss} himself!",
				"Welcome! {boss} has &cteleported you &7closer!",
				"Don't run away from me! Fight like a man!"
		};
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.ENDER_EYE,
				"Teleport Player",
				"",
				"Boss will teleport",
				"distant player to self.")
				.build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		radius = (int) map.getOrDefault("Radius", 5);
		players = (int) map.getOrDefault("Max_Players_At_Once", 2);
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();

		map.put("Radius", radius);
		map.put("Max_Players_At_Once", players);

		return map;
	}

	@Override
	public String[] getDefaultHeader() {
		return new String[] {
				"  Radius - How many blocks around should the Boss search for players",
				"           to teleport them?",
				"  Max_Players_At_Once - The amount of players to teleport at once"
		};
	}
}
