package org.mineacademy.boss.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.mineacademy.boss.model.skills.SkillFreeze;

import lombok.NonNull;

/**
 * Listener to normalize the player if he dies while having the freeze-effect
 * enabled (All metadata will be cleared on death)
 */
public final class FreezeListener implements Listener {

	@EventHandler
	public void onDeath(@NonNull final PlayerRespawnEvent event) {
		final Player player = event.getPlayer();

		if (player == null)
			return;

		if (!SkillFreeze.getFreezedPlayers().contains(player.getUniqueId()))
			return;

		player.setWalkSpeed(1F);
		player.setFlySpeed(1F);
		player.setFlying(false);
	}
}
