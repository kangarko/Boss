package org.mineacademy.boss.model;

import org.apache.commons.lang.WordUtils;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompAttribute;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BossAttribute {

	GENERIC_ARMOR(CompMaterial.IRON_CHESTPLATE, "Armor bonus of an Entity."),
	GENERIC_ARMOR_TOUGHNESS(CompMaterial.CHAINMAIL_CHESTPLATE, "Armor durability bonus of an Entity."),
	GENERIC_ATTACK_DAMAGE(CompMaterial.DIAMOND_SWORD, "Attack damage of an Entity."),
	GENERIC_ATTACK_SPEED(CompMaterial.DIAMOND_SWORD, "Attack speed of an Entity."),
	GENERIC_FLYING_SPEED(Remain.getMaterial("ELYTRA", CompMaterial.PISTON), "Flying speed of an Entity."),
	GENERIC_FOLLOW_RANGE(CompMaterial.BUCKET, "Range at which an Entity will follow others."),
	GENERIC_KNOCKBACK_RESISTANCE(CompMaterial.COBWEB, "Resistance of an Entity to knockback."),
	GENERIC_LUCK(CompMaterial.DIAMOND, "Luck bonus of an Entity."),
	//GENERIC_MAX_HEALTH(CompMaterial.POTION, "Maximum health of an Entity."),
	GENERIC_MOVEMENT_SPEED(CompMaterial.CHAINMAIL_BOOTS, "Movement speed of an Entity."),
	HORSE_JUMP_STRENGTH(CompMaterial.SADDLE, "Strength with which a horse will jump."),
	ZOMBIE_SPAWN_REINFORCEMENTS(CompMaterial.ZOMBIE_HEAD, "Chance of a zombie to spawn reinforcements.");

	private final CompMaterial icon;
	private final String description;

	public final boolean hasLegacy() {
		return CompAttribute.valueOf(name()).hasLegacy();
	}

	public final ItemStack getItem(Boss boss) {
		final ItemCreator.ItemCreatorBuilder builder = ItemCreator
				.of(icon)
				.hideTags(true)
				.name("&r" + ItemUtil.bountifyCapitalized(this).replaceFirst("Generic ", ""))
				.lore(" ");

		for (final String wrap : WordUtils.wrap(description, 28).split(System.lineSeparator()))
			builder.lore("&7" + wrap);

		builder.lore(" ");

		if (MinecraftVersion.olderThan(V.v1_9) && !hasLegacy())
			builder.lore("&4Unsupported on your MC version.");
		else
			builder.lore("&7Value: &f" + MathUtil.formatFiveDigits(boss.getAttributes().get(this)));

		return builder.build().make();
	}
}