package org.mineacademy.boss.model.skills;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.util.SkillPotionUtils;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillPotions extends AbstractTargetSkill {

	/**
	 * List of effects
	 */
	private final StrictList<PotionEffect> potions = new StrictList<>();

	@Override
	public String getName() {
		return "Potions";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("30 seconds", "1 minute");
	}

	@Override
	public boolean execute(Player target, SpawnedBoss spawned) {
		for (final PotionEffect effect : potions)
			target.addPotionEffect(effect, true);

		sendSkillMessage(target, spawned);
		return true;
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.POTION,
				"Potion",
				"",
				"Let the Boss give",
				"the player special",
				"potion effects!")
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
				"Watch out! The {boss} has &cgiven you a special potion effect&7!",
				"The {boss} has &cpoisoned you&7!",
				"You are now given special potion effects!"
		};
	}
}
