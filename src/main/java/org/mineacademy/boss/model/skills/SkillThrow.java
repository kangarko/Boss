package org.mineacademy.boss.model.skills;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.util.BossUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillThrow extends BossSkill {

	private int radius;
	private float power;

	@Override
	public String getName() {
		return "Throw Player";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("30 seconds", "1 minute");
	}

	@Override
	public boolean execute(SpawnedBoss spawned) {
		final LivingEntity en = spawned.getEntity();

		final Location bossLoc = en.getLocation();
		boolean success = false;

		for (final Entity nearby : en.getNearbyEntities(radius, radius - 1, radius))
			if (nearby instanceof Player) {
				final Player target = (Player) nearby;

				if (!BossUtil.canBossTarget(target))
					continue;

				final Location loc = nearby.getLocation();
				Vector v = new Vector(loc.getX() - bossLoc.getX(), 0, loc.getZ() - bossLoc.getZ());

				v = v.normalize().setY(1.2);

				if (!Valid.isFinite(v))
					return false;

				try {
					nearby.setVelocity(v);
				} catch (final IllegalArgumentException ex) {
					continue;
				}

				sendSkillMessage(target, spawned);

				success = true;
			}

		return success;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"Embrace the fresh air in the skies!",
				"The {boss} has decided to &ckick you&7!",
				"Good bye, {player}!"
		};
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.PISTON,
				"Throw Player",
				"",
				"The player will be",
				"thrown up the skies")
				.build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		radius = Integer.parseInt(map.getOrDefault("Radius", 5).toString());
		power = Float.parseFloat(map.getOrDefault("Power", 1.2F).toString());
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();

		map.put("Radius", radius);
		map.put("Power", power);

		return map;
	}
}
