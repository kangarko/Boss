package org.mineacademy.boss.model.skills;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.BossAPI;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.util.BossUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillLightning extends BossSkill {

	private int radius;

	@Override
	public String getName() {
		return "Strike Lightning";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("30 seconds", "1 minute");
	}

	@Override
	public boolean execute(SpawnedBoss spawned) {
		int count = 0;
		boolean success = false;

		for (final Entity nearby : spawned.getEntity().getNearbyEntities(radius, radius - 1, radius))
			if (nearby instanceof Player) {
				if (!BossUtil.canBossTarget((Player) nearby))
					continue;

				strike(spawned, (Player) nearby, 10 * (count++ / 10));

				success = true;
			}

		return success;
	}

	private void strike(final SpawnedBoss boss, final Player player, int delayTicks) {
		Common.runLater(delayTicks, () -> {
			final LightningStrike bolt = player.getWorld().spawn(player.getLocation(), LightningStrike.class);
			bolt.setMetadata("BossBolt", new FixedMetadataValue(SimplePlugin.getInstance(), ""));

			sendSkillMessage(player, boss);
		});
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"You have been &cstrucked by a lightning bolt&7!",
				"The {boss} has &csend a lightning &7at you!",
				"You are very attractive so a lightning bolt connected to you!"
		};
	}

	@EventHandler
	public void onEntityBurn(EntityCombustByEntityEvent e) {
		final Entity combuster = e.getCombuster();
		final boolean valid = combuster != null && combuster instanceof LightningStrike && combuster.hasMetadata("BossBolt");

		if (!valid)
			return;

		if (BossPlugin.getBossManager().findBoss(e.getEntity()) != null)
			e.setCancelled(true);
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent e) {
		if (BossAPI.isBoss(e.getEntity()) && e.getDamager() != null && e.getDamager().hasMetadata("BossBolt"))
			e.setCancelled(true);
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.ENDER_EYE,
				"Strike Lightning",
				"",
				"The player will be",
				"striked by lightning.")
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
				"           to strike them?"
		};
	}
}
