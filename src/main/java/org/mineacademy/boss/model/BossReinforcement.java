package org.mineacademy.boss.model;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ConfigSerializable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Represents Boss reinforcement.
 */
@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BossReinforcement implements ConfigSerializable {

	/**
	 * The Boss name, if null, we spawn vanilla entity
	 */
	@Nullable
	private final String bossName;

	/**
	 * The Boss entity type, if null, we spawn a boss
	 */
	@Nullable
	private final EntityType entityType;

	/**
	 * Amount of entities/bosses to spawn
	 */
	private final int amount;

	/**
	 * Chance of this rule to work
	 */
	private final double chance;

	/**
	 * Spawn this rule at the given location
	 *
	 * @param location
	 */
	public void spawn(Location location) {

		if (!RandomUtil.chanceD(this.chance))
			return;

		if (this.bossName != null) {
			final Boss boss = Boss.findBoss(this.bossName);

			if (boss != null)
				for (int i = 0; i < this.amount; i++)
					boss.spawn(location.clone(), BossSpawnReason.REINFORCEMENTS);
		}

		else if (this.entityType != null)
			for (int i = 0; i < this.amount; i++)
				location.getWorld().spawnEntity(location.clone(), this.entityType);

		else
			throw new FoException("Cannot spawn boss reinforcements when both entity type and boss name are null");
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIfExists("Boss_Name", this.bossName);
		map.putIfExists("Entity_Type", this.entityType);
		map.put("Amount", this.amount);
		map.put("Chance", this.chance);

		return map;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create new reinforcement from vanilla
	 *
	 * @param type
	 * @param amount
	 * @param chance
	 * @return
	 */
	public static BossReinforcement fromVanilla(EntityType type, int amount, double chance) {
		return new BossReinforcement(null, type, amount, chance);
	}

	/**
	 * Create new reinforcement from Boss
	 *
	 * @param boss
	 * @param amount
	 * @param chance
	 * @return
	 */
	public static BossReinforcement fromBoss(Boss boss, int amount, double chance) {
		return new BossReinforcement(boss.getName(), null, amount, chance);
	}

	/**
	 * Load disk reinforcement
	 *
	 * @param map
	 * @return
	 */
	public static BossReinforcement deserialize(SerializedMap map) {
		final String bossName = map.getString("Boss_Name");
		final EntityType entityType = map.get("Entity_Type", EntityType.class);
		final int amount = map.getInteger("Amount");
		final double chance = map.getDouble("Chance");

		return new BossReinforcement(bossName, entityType, amount, chance);
	}
}