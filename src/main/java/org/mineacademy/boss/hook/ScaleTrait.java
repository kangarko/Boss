package org.mineacademy.boss.hook;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.remain.CompAttribute;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;

@TraitName("ma_scaletrait")
public final class ScaleTrait extends Trait {

	/**
	 * Stores last known current health since Citizens does not save this value
	 */
	@Persist("boss_health")
	double scale = -1;

	public ScaleTrait() {
		super("ma_scaletrait");
	}

	/*
	 * Update last known health
	 */
	public static void updateScale(Entity entity, double scale) {
		if (scale < 0)
			scale = -1;

		final SpawnedBoss boss = Boss.findBoss(entity);

		if (boss != null) {
			final NPCRegistry registry = CitizensAPI.getNPCRegistry();
			final NPC npc = registry.getNPC(entity);

			if (npc != null)
				npc.getTraitNullable(ScaleTrait.class).scale = scale;
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
			if (this.scale > 0)
				CompAttribute.SCALE.set(entity, this.scale);
		}
	}
}