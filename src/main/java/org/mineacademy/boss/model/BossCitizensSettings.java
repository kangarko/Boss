package org.mineacademy.boss.model;

import java.util.Set;

import org.bukkit.entity.EntityType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.remain.CompEntityType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents custom Citizens integration
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BossCitizensSettings implements ConfigSerializable {

	/**
	 * The Boss this is for.
	 */
	private final Boss boss;

	/**
	 * Is Citizens integration enabled?
	 */
	private boolean enabled;

	/**
	 * A custom skin, independent from a Boss' name.
	 */
	private String skin;

	/**
	 * The entity death sound to remap.
	 */
	private String deathSound;

	/**
	 * The entity hurt sound to remap.
	 */
	private String hurtSound;

	/**
	 * The entity idle sound to remap.
	 */
	private String ambientSound;

	/**
	 * The movement speed
	 */
	private double speed;

	/**
	 * Should we inject custom target pathfinder?
	 */
	private boolean targetGoalEnabled;

	/**
	 * Kill the target?
	 */
	private boolean targetGoalAggressive;

	/**
	 * Radius to find targets.
	 */
	private int targetGoalRadius;

	/**
	 * What entities to target.
	 */
	private Set<EntityType> targetGoalEntities;

	/**
	 * Should we inject custom wandering pathfinder?
	 */
	private boolean wanderGoalEnabled;

	/**
	 * Radius to wander around spawn location.
	 */
	private int wanderGoalRadius;

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;

		this.boss.save();
	}

	/**
	 *
	 * @param skinOrURL
	 */
	public void setSkin(String skinOrURL) {
		this.skin = skinOrURL;

		this.boss.save();
	}

	/**
	 * Returns skin or alias or name of Boss whatever is non-null first
	 * @return
	 */
	public String getSkinOrAlias() {
		return Common.getOrDefault(this.skin, this.boss.getAlias());
	}

	/**
	 * @param deathSound the deathSound to set
	 */
	public void setDeathSound(String deathSound) {
		this.deathSound = deathSound;

		this.boss.save();
	}

	/**
	 * @param hurtSound the hurtSound to set
	 */
	public void setHurtSound(String hurtSound) {
		this.hurtSound = hurtSound;

		this.boss.save();
	}

	/**
	 * @param ambientSound the ambientSound to set
	 */
	public void setAmbientSound(String ambientSound) {
		this.ambientSound = ambientSound;

		this.boss.save();
	}

	/**
	 * @param targetGoalEnabled the targetGoalEnabled to set
	 */
	public void setTargetGoalEnabled(boolean targetGoalEnabled) {
		this.targetGoalEnabled = targetGoalEnabled;

		this.boss.save();
	}

	/**
	 * @param speed
	 */
	public void setSpeed(double speed) {
		this.speed = speed;

		this.boss.save();
	}

	/**
	 * @param targetGoalAggressive the targetGoalAggressive to set
	 */
	public void setTargetGoalAggressive(boolean targetGoalAggressive) {
		this.targetGoalAggressive = targetGoalAggressive;

		this.boss.save();
	}

	/**
	 * @param targetGoalRadius the targetGoalRadius to set
	 */
	public void setTargetGoalRadius(int targetGoalRadius) {
		this.targetGoalRadius = targetGoalRadius;

		this.boss.save();
	}

	/**
	 * @param targetGoalEntities the targetGoalEntities to set
	 */
	public void setTargetGoalEntities(Set<EntityType> targetGoalEntities) {
		this.targetGoalEntities = targetGoalEntities;

		this.boss.save();
	}

	/**
	 * @param wanderGoalEnabled the wanderGoalEnabled to set
	 */
	public void setWanderGoalEnabled(boolean wanderGoalEnabled) {
		this.wanderGoalEnabled = wanderGoalEnabled;

		this.boss.save();
	}

	/**
	 * @param wanderGoalRadius the wanderGoalRadius to set
	 */
	public void setWanderGoalRadius(int wanderGoalRadius) {
		this.wanderGoalRadius = wanderGoalRadius;

		this.boss.save();
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.fromArray(
				"Enabled", this.enabled,
				"Speed", this.speed,
				"Skin", this.skin,
				"Sound_Death", this.deathSound,
				"Sound_Hurt", this.hurtSound,
				"Sound_Ambient", this.ambientSound,
				"Goal_Target_Enabled", this.targetGoalEnabled,
				"Goal_Target_Aggressive", this.targetGoalAggressive,
				"Goal_Target_Radius", this.targetGoalRadius,
				"Goal_Target_Entities", this.targetGoalEntities,
				"Goal_Wander_Enabled", this.wanderGoalEnabled,
				"Goal_Wander_Radius", this.wanderGoalRadius);
	}

	/**
	 * Create new settings from the map.
	 *
	 * @param map
	 * @param boss
	 * @return
	 */
	public static BossCitizensSettings deserialize(SerializedMap map, Boss boss) {
		final BossCitizensSettings settings = new BossCitizensSettings(boss);

		settings.enabled = boss.getType() == CompEntityType.PLAYER ? true : map.getBoolean("Enabled", false);
		settings.speed = map.getDouble("Speed", (double) boss.getDefaultBaseSpeed());
		settings.skin = map.getString("Skin");
		settings.deathSound = map.getString("Sound_Death");
		settings.hurtSound = map.getString("Sound_Hurt");
		settings.ambientSound = map.getString("Sound_Ambient");
		settings.targetGoalEnabled = map.getBoolean("Goal_Target_Enabled", false);
		settings.targetGoalAggressive = map.getBoolean("Goal_Target_Aggressive", false);
		settings.targetGoalRadius = map.getInteger("Goal_Target_Radius", 24);
		settings.targetGoalEntities = map.getSet("Goal_Target_Entities", EntityType.class);
		settings.wanderGoalEnabled = map.getBoolean("Goal_Wander_Enabled", false);
		settings.wanderGoalRadius = map.getInteger("Goal_Wander_Radius", 18);

		return settings;
	}
}
