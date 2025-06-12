package org.mineacademy.boss.skill;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.mineacademy.boss.menu.util.SkillPotionMenu;
import org.mineacademy.boss.model.BossSkillTag;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillArrow extends AbstractTargetSkill {

	/**
	 * List of effects
	 */
	private List<PotionEffect> potions = new ArrayList<>();

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("2 seconds - 5 seconds");
	}

	@Override
	public boolean execute(Player target, LivingEntity entity) {
		final Arrow arrow = entity.launchProjectile(Arrow.class);
		final Vector vector = target.getLocation().subtract(entity.getLocation()).toVector().normalize();

		if (!Valid.isFinite(vector))
			return false;

		arrow.setVelocity(vector.multiply(2).add(new Vector(0, 0.1, 0)));

		BossSkillTag.POTIONS.set(arrow, this.potions);

		this.sendSkillMessage(target, entity);

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

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-arrow-default-message").split("\n");
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

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				Remain.getMaterial("TIPPED_ARROW", CompMaterial.ARROW),
				"Arrow",
				"",
				"Shoot an arrow with",
				"potions (see the",
				"skill settings).")
				.make();
	}
}
