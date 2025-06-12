package org.mineacademy.boss.listener;

import java.util.List;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffect;
import org.mineacademy.boss.model.BossSkillTag;
import org.mineacademy.boss.task.TaskFrozenPlayers;
import org.mineacademy.fo.annotation.AutoRegister;

/**
 * Class responsible for listening to events when skills are used
 */
@AutoRegister
public final class SkillListener implements Listener {

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		final Entity damager = event.getDamager();
		final Entity victim = event.getEntity();

		if (damager instanceof Arrow && victim instanceof LivingEntity)
			if (BossSkillTag.POTIONS.has(damager)) {
				final List<PotionEffect> potions = BossSkillTag.POTIONS.get(damager);

				for (final PotionEffect potion : potions)
					((LivingEntity) victim).addPotionEffect(potion, true);
			}

		if (BossSkillTag.IS_CANCELLING_DAMAGE_TO_VICTIM.has(damager))
			event.setCancelled(true);

		else if (BossSkillTag.IS_CANCELLING_DAMAGE_TO_NON_PLAYERS.has(damager) && !(victim instanceof Player))
			event.setCancelled(true);
	}

	@EventHandler
	public void onCombust(EntityCombustByEntityEvent event) {
		final Entity combuster = event.getCombuster();

		if (BossSkillTag.IS_CANCELLING_COMBUSTION.has(combuster))
			event.setCancelled(true);
	}

	@EventHandler
	public void onPrime(ExplosionPrimeEvent event) {
		final Entity entity = event.getEntity();

		if (BossSkillTag.EXPLOSION_POWER.has(entity))
			event.setRadius(BossSkillTag.EXPLOSION_POWER.get(entity));
	}

	@EventHandler
	public void onExplode(EntityExplodeEvent event) {
		final Entity entity = event.getEntity();

		if (BossSkillTag.IS_EXPLOSION_DESTROYING_BLOCKS.has(entity)) {
			final boolean destroy = BossSkillTag.IS_EXPLOSION_DESTROYING_BLOCKS.get(entity);

			if (!destroy)
				event.blockList().clear();
		}

	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		TaskFrozenPlayers.getInstance().unfreezeIfFrozen(event.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		TaskFrozenPlayers.getInstance().unfreezeIfFrozen(event.getPlayer());
	}

	@EventHandler
	public void onFlight(PlayerToggleFlightEvent event) {
		if (TaskFrozenPlayers.getInstance().isFrozen(event.getPlayer()))
			event.getPlayer().setFlying(false);
	}

	@EventHandler
	public void onSprint(PlayerToggleSprintEvent event) {
		if (TaskFrozenPlayers.getInstance().isFrozen(event.getPlayer()))
			event.getPlayer().setSprinting(false);
	}
}
