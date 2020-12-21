package org.mineacademy.boss.listener;

import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.boss.api.BossSkillRegistry;
import org.mineacademy.boss.storage.SimplePlayerData;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.debug.Debugger;

public final class PlayerListener implements Listener {

	@EventHandler
	public void onJoin(final PlayerJoinEvent e) {
		final Player player = e.getPlayer();
		final SimplePlayerData data = SimplePlayerData.$();
		final Set<String> skillsToRestore = data.getSkillsToRestore(player);

		if (skillsToRestore != null) {
			for (final String skillName : skillsToRestore) {
				final BossSkill skill = BossSkillRegistry.getByName(skillName);

				if (skill != null) {
					Debugger.debug("skills", "Removing effect from skill " + skillName + " from player " + player.getName());

					// Delay so other plugins can handle stuff correctly
					Common.runLater(5, () -> skill.removeSkillEffect(player));

				} else
					Debugger.debug("skills", "Could not remove skill effect from " + player.getName() + ", non existing skill '" + skill + "'");
			}

			data.removeSkills(player);
		}
	}
}
