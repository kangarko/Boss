package org.mineacademy.boss.model.skills;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.mineacademy.boss.api.BossAPI;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillFireball extends AbstractTargetSkill {

	/**
	 * Power of the explosion
	 */
	private float power;

	/**
	 * Should Fireball destroy blocks around?
	 */
	private boolean destroyBlocks = false;

	@Override
	public String getName() {
		return "Shoot Fireball";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("20 seconds", "50 seconds");
	}

	@Override
	public boolean execute(Player target, SpawnedBoss spawned) {
		final Fireball ball = spawned.getEntity().launchProjectile(Fireball.class);
		final Vector v = target.getLocation().subtract(spawned.getEntity().getLocation()).toVector().normalize();

		if (!Valid.isFinite(v))
			return false;

		ball.setVelocity(v);
		ball.setMetadata("BossFireball", new FixedMetadataValue(SimplePlugin.getInstance(), ""));

		sendSkillMessage(target, spawned);
		return true;
	}

	@EventHandler
	public void onPrime(ExplosionPrimeEvent e) {
		if (e.getEntity().hasMetadata("BossFireball"))
			e.setRadius(power);
	}

	@EventHandler
	public void onExplode(EntityExplodeEvent e) {
		if (!destroyBlocks && e.getEntity().hasMetadata("BossFireball"))
			e.blockList().clear();
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent e) {
		if (BossAPI.isBoss(e.getEntity()) && e.getDamager() != null && e.getDamager().hasMetadata("BossFireball"))
			e.setCancelled(true);
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"You have been &cfireballed &7by the boss!",
				"The {boss} has &claunched a fireball &7at you!",
				"Hellfire! Do you like small or big fireballs?"
		};
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.FIRE_CHARGE,
				"Shoot Fireball",
				"",
				"The Boss will throw",
				"a fireball on the player.")
				.build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		power = Float.parseFloat(map.getOrDefault("Power", 1.8F).toString());
		destroyBlocks = (boolean) map.getOrDefault("Destroy_Blocks", false);
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();

		map.put("Power", power);
		map.put("Destroy_Blocks", destroyBlocks);

		return map;
	}

	@Override
	public String[] getDefaultHeader() {
		return new String[] {
				"  Power - The strength of the explosion caused by the fireball.",
				"  Destroy_Blocks - Should the fireball explosion also destroy blocks?"
		};
	}
}
