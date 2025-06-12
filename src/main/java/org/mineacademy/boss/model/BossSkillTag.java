package org.mineacademy.boss.model;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.platform.BukkitPlugin;

import lombok.RequiredArgsConstructor;

/**
 * SkillTag class represents a custom metadata tag for entities within the game.
 * It is used to assign and retrieve typed metadata values to/from entities.
 *
 * @param <T> The type of the value associated with this tag.
 */
@RequiredArgsConstructor
public final class BossSkillTag<T> {

	/**
	 * Stores a list of potions
	 */
	public static final BossSkillTag<List<PotionEffect>> POTIONS = constructTag("Potions");

	/**
	 * Stores the radius of an explosion
	 */
	public static final BossSkillTag<Float> EXPLOSION_POWER = constructTag("Explosion_Power");

	/**
	 * Flag if we should cancel the explosion from destroying blocks
	 */
	public static final BossSkillTag<Boolean> IS_EXPLOSION_DESTROYING_BLOCKS = constructTag("Is_Explosion_Destroying_Blocks");

	/**
	 * Flag if we should cancel the damage to the victim
	 */
	public static final BossSkillTag<Boolean> IS_CANCELLING_DAMAGE_TO_VICTIM = constructTag("Is_Cancelling_Damage");

	/**
	 * Flag if we should cancel the damage to non-players
	 */
	public static final BossSkillTag<Boolean> IS_CANCELLING_DAMAGE_TO_NON_PLAYERS = constructTag("Is_Cancelling_Damage_To_Non_Players");

	/**
	 * Flag if we should cancel the combustion
	 */
	public static final BossSkillTag<Boolean> IS_CANCELLING_COMBUSTION = constructTag("Is_Cancelling_Combustion");

	/**
	 * Flag if the entity was thrown by a boss
	 */
	public static final BossSkillTag<Boolean> IS_THROW_BY_BOSS = constructTag("Thrown_By_Bos");

	/**
	 * Unique key for this metadata tag
	 */
	private final String key;

	/**
	 * Apply a metadata value to an entity.
	 *
	 * @param entity The entity to which the metadata will be applied.
	 * @param value  The value to set for this metadata.
	 */
	public void set(Entity entity, T value) {
		entity.setMetadata(this.key, new FixedMetadataValue(BukkitPlugin.getInstance(), value));
	}

	/**
	 * Check if the entity has this metadata tag.
	 *
	 * @param entity The entity to check.
	 * @return true if the entity has the metadata tag, false otherwise.
	 */
	public boolean has(Entity entity) {
		return entity.hasMetadata(this.key);
	}

	/**
	 * Remove this metadata tag from the entity.
	 *
	 * @param entity The entity from which to remove the metadata.
	 */
	public void remove(Entity entity) {
		if (this.has(entity))
			entity.removeMetadata(this.key, BukkitPlugin.getInstance());
	}

	/**
	 * Retrieve the metadata value associated with this tag from the entity.
	 *
	 * @param entity The entity from which to retrieve the metadata value.
	 * @return The value of the metadata if present, otherwise null.
	 */
	public T get(Entity entity) {
		final MetadataValue value = entity.getMetadata(this.key).get(0);
		Valid.checkNotNull(value, "Metadata '" + this.key + "' not found on entity " + entity);

		return value == null ? null : (T) value.value();
	}

	/**
	 * Construct a new SkillTag with a given name.
	 *
	 * @param name The name of the SkillTag.
	 * @param <T>  The type of the value associated with the SkillTag.
	 * @return A new instance of SkillTag.
	 */
	private static <T> BossSkillTag<T> constructTag(String name) {
		return new BossSkillTag<>("Boss_Skill_" + name);
	}
}
