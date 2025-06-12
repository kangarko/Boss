package org.mineacademy.boss.skill;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.mineacademy.boss.menu.util.SkillPotionMenu;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillPotion extends AbstractTargetSkill {

	/**
	 * List of effects
	 */
	private List<PotionEffect> potions = new ArrayList<>();

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("30 seconds - 1 minute");
	}

	@Override
	public boolean execute(Player target, LivingEntity entity) {
		for (final PotionEffect effect : this.potions)
			target.addPotionEffect(effect, true);

		if (!this.potions.isEmpty())
			this.sendSkillMessage(target, entity);

		return true;
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.POTION,
				"Potion",
				"",
				"Let the Boss give",
				"the player special",
				"potion effects!")
				.make();
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-potions-default-message").split("\n");
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.potions = map.getList("Potions", PotionEffect.class);
	}

	@Override
	public SerializedMap writeSettings() {
		return SerializedMap.fromArray(
				"Potions", this.potions);
	}

	@Override
	public Menu getMenu(Menu parent) {
		return new SkillPotionMenu(parent, this, this.potions);
	}
}
