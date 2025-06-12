package org.mineacademy.boss.hook;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.remain.Remain;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;

@TraitName("ma_healthtrait")
public final class MaxHealthTrait extends Trait {

	/**
	 * Stores last known current health since Citizens does not save this value
	 */
	@Persist("boss_health")
	double health = -1;

	public MaxHealthTrait() {
		super("ma_healthtrait");
	}

	/**
	 * Updates health
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		this.updateHealth(event);
	}

	/**
	 * Updates health
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockDamage(EntityDamageByBlockEvent event) {
		this.updateHealth(event);
	}

	/*
	 * Update last known health
	 */
	private void updateHealth(EntityDamageEvent event) {
		final NPCRegistry registry = CitizensAPI.getNPCRegistry();

		if (registry == null)
			return;

		final NPC npc = registry.getNPC(event.getEntity());

		if (npc != null && npc.equals(this.npc)) {
			final LivingEntity living = (LivingEntity) event.getEntity();
			final double health = Math.max(0, living.getHealth() - Remain.getFinalDamage(event));

			npc.getTraitNullable(MaxHealthTrait.class).health = health;
		}
	}

	/**
	 * Sets the health back instead of letting Citizens reset it to max if we saved it
	 */
	@Override
	public void onSpawn() {
		if (!this.npc.isSpawned())
			return;

		final LivingEntity entity = (LivingEntity) this.npc.getEntity();
		final SpawnedBoss spawnedBoss = Boss.findBoss(entity);

		if (spawnedBoss != null) {
			final Boss boss = spawnedBoss.getBoss();

			entity.setMaxHealth(boss.getMaxHealth());

			if (this.health != -1 && this.health <= boss.getMaxHealth())
				entity.setHealth(this.health);
		}
	}
}