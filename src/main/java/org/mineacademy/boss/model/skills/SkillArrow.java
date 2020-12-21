package org.mineacademy.boss.model.skills;

import java.util.Map;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.util.SkillPotionUtils;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

public final class SkillArrow extends AbstractTargetSkill {

	/**
	 * List of effects
	 */
	private final StrictList<PotionEffect> potions = new StrictList<>();

	@Override
	public String getName() {
		return "Shoot Arrow";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("2 seconds", "5 seconds");
	}

	@Override
	public boolean execute(Player target, SpawnedBoss spawned) {
		final Arrow arrow = spawned.getEntity().launchProjectile(Arrow.class);
		final Vector v = target.getLocation().subtract(spawned.getEntity().getLocation()).toVector().normalize();

		if (!Valid.isFinite(v))
			return false;

		//arrow.setVelocity(v);
		arrow.setVelocity(v.multiply(2).add(new Vector(0, 0.1, 0)));
		arrow.setMetadata("BossArrow", new FixedMetadataValue(SimplePlugin.getInstance(), ""));

		sendSkillMessage(target, spawned);
		return true;
	}

	@Override
	public boolean isCompatible() {
		try {
			ProjectileHitEvent.class.getMethod("getHitEntity");
			return true;

		} catch (final ReflectiveOperationException ex) {
			return false;
		}
	}

	@EventHandler
	public void onHit(EntityDamageByEntityEvent e) {
		final Entity damager = e.getDamager();
		final Entity victim = e.getEntity();

		if (damager != null && damager instanceof Arrow && victim != null && victim instanceof Player)
			if (damager.hasMetadata("BossArrow")) {
				final Player pl = (Player) victim;

				for (final PotionEffect ef : potions)
					pl.addPotionEffect(ef, true);
			}
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				Remain.getMaterial("TIPPED_ARROW", CompMaterial.ARROW),
				"Shoot Arrow",
				"",
				"Shoot an arrow with",
				"potions (see the",
				"skill settings).")
				.build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		potions.clear();
		potions.addAll(SkillPotionUtils.readSettings(map));
	}

	@Override
	public Map<String, Object> writeSettings() {
		return SkillPotionUtils.writeSettings(potions);
	}

	@Override
	public String[] getDefaultHeader() {
		return SkillPotionUtils.getDefaultHeader();
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"Watch out! The {boss} has &cshoot an arrow &7at ya!",
				"The {boss} has &claunched a projectile &7at you!",
				"You are now under attack!"
		};
	}
}
