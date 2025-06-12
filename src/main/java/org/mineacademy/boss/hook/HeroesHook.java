package org.mineacademy.boss.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;

import lombok.Getter;
import lombok.Setter;

/**
 * The connector for Heroes plugin.
 */
public final class HeroesHook {

	/**
	 * Return true if the plugin is hooked.
	 */
	@Getter
	@Setter
	private static boolean enabled = false;

	/**
	 * Gves the on-death experience to the given player at the given location.
	 *
	 * @param location
	 * @param player
	 * @param amount
	 */
	public static void giveKillExp(final Location location, final Player player, final double amount) {
		if (enabled) {
			final Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);

			if (hero != null)
				hero.gainExp(amount, ExperienceType.KILLING, location);
		}
	}
}
