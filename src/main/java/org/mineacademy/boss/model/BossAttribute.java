package org.mineacademy.boss.model;

import javax.annotation.Nullable;

import org.bukkit.entity.LivingEntity;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompAttribute;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents bukkit attribute
 */
@Getter
@RequiredArgsConstructor
public enum BossAttribute {

	ARMOR(
			CompAttribute.ARMOR,
			CompMaterial.IRON_CHESTPLATE,
			"Armor Bonus",
			"Armor bonus of an entity."),

	ARMOR_TOUGHNESS(
			CompAttribute.ARMOR_TOUGHNESS,
			CompMaterial.CHAINMAIL_CHESTPLATE,
			"Armor Toughness",
			"Armor durability bonus of an entity."),

	ATTACK_DAMAGE(
			CompAttribute.ATTACK_DAMAGE,
			CompMaterial.DIAMOND_SWORD,
			"Attack Damage",
			"Attack damage of an entity. This attribute is not found on passive mobs and golems."),

	ATTACK_KNOCKBACK(
			CompAttribute.ATTACK_KNOCKBACK,
			CompMaterial.STICK,
			"Attack Knockback",
			"Attack knockback of an entity."),

	ATTACK_SPEED(
			CompAttribute.ATTACK_SPEED,
			CompMaterial.BLAZE_POWDER,
			"Attack Speed",
			"Attack speed of an entity."),

	BURNING_TIME(
			CompAttribute.BURNING_TIME,
			CompMaterial.FLINT_AND_STEEL,
			"Burning Time",
			"How long an entity remains burning after ignition."),

	EXPLOSION_KNOCKBACK_RESISTANCE(
			CompAttribute.EXPLOSION_KNOCKBACK_RESISTANCE,
			CompMaterial.TNT,
			"Explosion Knockback Resistance",
			"Resistance to knockback from explosions."),

	FALL_DAMAGE_MULTIPLIER(
			CompAttribute.FALL_DAMAGE_MULTIPLIER,
			CompMaterial.FEATHER,
			"Fall Damage Multiplier",
			"The fall damage multiplier of an entity)"),

	FLYING_SPEED(
			CompAttribute.FLYING_SPEED,
			Remain.getMaterial("ELYTRA", CompMaterial.PISTON),
			"Flight Speed",
			"Flying speed of an entity."),

	FOLLOW_RANGE(
			CompAttribute.FOLLOW_RANGE,
			CompMaterial.BUCKET,
			"Follow range",
			"Range at which an Entity will follow others."),

	GRAVITY(
			CompAttribute.GRAVITY,
			CompMaterial.GRAVEL,
			"Gravity",
			"The gravity applied to an entity."),

	JUMP_STRENGTH(
			CompAttribute.JUMP_STRENGTH,
			CompMaterial.SLIME_BALL,
			"Jump Strength",
			"Strength with which an entity will jump."),

	KNOCKBACK_RESISTANCE(
			CompAttribute.KNOCKBACK_RESISTANCE,
			CompMaterial.COBWEB,
			"Knockback Resistance",
			"Resistance of an entity to knockback."),

	LUCK(
			CompAttribute.LUCK,
			CompMaterial.EMERALD,
			"Luck Bonus",
			"Luck bonus of an entity."),

	MAX_ABSORPTION(
			CompAttribute.MAX_ABSORPTION,
			CompMaterial.GOLDEN_APPLE,
			"Max Absorption",
			"Maximum absorption of an entity."),

	MOVEMENT_EFFICIENCY(
			CompAttribute.MOVEMENT_EFFICIENCY,
			CompMaterial.DIAMOND_BOOTS,
			"Movement Efficiency",
			"Movement speed through difficult terrain."),

	MOVEMENT_SPEED(
			CompAttribute.MOVEMENT_SPEED,
			CompMaterial.CHAINMAIL_BOOTS,
			"Movement Speed",
			"Movement speed of an entity."),

	OXYGEN_BONUS(
			CompAttribute.OXYGEN_BONUS,
			CompMaterial.WATER_BUCKET,
			"Oxygen Bonus",
			"Oxygen use underwater."),

	SAFE_FALL_DISTANCE(
			CompAttribute.SAFE_FALL_DISTANCE,
			CompMaterial.FEATHER,
			"Safe Fall Distance",
			"The distance which an entity can fall without damage."),

	SCALE(
			CompAttribute.SCALE,
			CompMaterial.SLIME_BLOCK,
			"Scale",
			"The relative scale of an entity."),

	STEP_HEIGHT(
			CompAttribute.STEP_HEIGHT,
			CompMaterial.LADDER,
			"Step Height",
			"The height which an entity can walk over."),

	TEMPT_RANGE(
			CompAttribute.TEMPT_RANGE,
			CompMaterial.CARROT,
			"Tempt Range",
			"Range at which mobs will be tempted by items."),

	WATER_MOVEMENT_EFFICIENCY(
			CompAttribute.WATER_MOVEMENT_EFFICIENCY,
			CompMaterial.WATER_BUCKET,
			"Water Movement Efficiency",
			"Movement speed through water."),

	ZOMBIE_SPAWN_REINFORCEMENTS(
			CompAttribute.SPAWN_REINFORCEMENTS,
			CompMaterial.ZOMBIE_HEAD,
			"Zombie Spawn Reinforcements",
			"Chance of a zombie spawning reinforcements.") {

		@Override
		public boolean isAvailable(LivingEntity entity) {
			return super.isAvailable(entity) && entity.getType() == CompEntityType.ZOMBIE;
		}
	},

	/**
	 * Custom attribute
	 */
	DAMAGE_MULTIPLIER(null, CompMaterial.GHAST_TEAR, "Damage Multiplier", "Damage multiplier of the Boss (example: 0.5 to reduce by half, 2.0 to double).") {

		@Override
		public Double getDefaultValue(LivingEntity entity) {
			return 1.0;
		}

		@Override
		public boolean isAvailable(LivingEntity entity) {
			return true;
		}

		@Override
		public void apply(LivingEntity entity, double value) {
			// Handled in a custom way
		}
	};

	/**
	 * The bukkit attribute
	 */
	@Nullable
	private final CompAttribute attribute;

	/**
	 * The menu icon
	 */
	private final CompMaterial icon;

	/**
	 * The menu title
	 */
	private final String title;

	/**
	 * The menu description
	 */
	private final String description;

	/**
	 * Get default value for this entity
	 *
	 * @param entity
	 * @return
	 */
	@Nullable
	public Double getDefaultValue(LivingEntity entity) {
		Valid.checkNotNull(this.attribute, "Getting default attribute " + this + " of " + entity + " not implemented!");

		return this.attribute.get(entity);
	}

	/**
	 * Return if attribute is available
	 *
	 * @param entity
	 * @return
	 */
	public boolean isAvailable(LivingEntity entity) {
		if (MinecraftVersion.atLeast(V.v1_9) || this.attribute.getNmsName() != null)
			try {
				return this.attribute.canApply(entity);

			} catch (NoClassDefFoundError | IllegalArgumentException ex) {
				return false;
			}

		return false;
	}

	/**
	 * Apply attribute for the given entity
	 *
	 * @param entity
	 * @param value
	 */
	public void apply(LivingEntity entity, double value) {
		Valid.checkNotNull(this.attribute, "Applying attribute " + this + " to " + entity + " not implemented!");

		this.attribute.set(entity, value);
	}

	/**
	 * Attempts to find a boss attribute from the given key
	 *
	 * @param key
	 * @return
	 */
	@Nullable
	public static BossAttribute fromKey(String key) {
		key = key.toUpperCase().replace(" ", "_");

		for (final BossAttribute attribute : values())
			if (attribute.name().equals(key) || (attribute.attribute != null && attribute.attribute.toString().equals(key)))
				return attribute;

		throw new IllegalArgumentException("No such BossAttribute " + key + ". Available: " + values());
	}
}