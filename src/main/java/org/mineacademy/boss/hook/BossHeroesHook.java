package org.mineacademy.boss.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;

import lombok.Getter;
import lombok.Setter;

public final class BossHeroesHook {

	@Getter
	@Setter
	private static boolean enabled = false;

	public static void gainKillExp(final Location loc, final Player player, final double amount) {
		if (!enabled)
			return;

		final Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);

		if (hero != null)
			hero.gainExp(amount, ExperienceType.KILLING, loc);
	}
}
