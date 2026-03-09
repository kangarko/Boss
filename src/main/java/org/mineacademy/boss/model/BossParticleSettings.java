package org.mineacademy.boss.model;

import javax.annotation.Nullable;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.remain.CompParticle;

import lombok.Getter;

@Getter
public final class BossParticleSettings implements ConfigSerializable {

	private final Boss boss;

	private boolean enabled;

	@Nullable
	private CompParticle type;

	private ParticleShape shape;

	private int count;

	private double offsetX;

	private double offsetY;

	private double offsetZ;

	private double speed;

	private int intervalTicks;

	private double radius;

	private double height;

	private int strands;

	private double forwardOffset;

	private BossParticleSettings(Boss boss) {
		this.boss = boss;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;

		this.boss.save();
	}

	public void setType(@Nullable CompParticle type) {
		this.type = type;

		this.boss.save();
	}

	public void setCount(int count) {
		this.count = count;

		this.boss.save();
	}

	public void setOffset(double offsetX, double offsetY, double offsetZ) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;

		this.boss.save();
	}

	public void setSpeed(double speed) {
		this.speed = speed;

		this.boss.save();
	}

	public void setIntervalTicks(int intervalTicks) {
		this.intervalTicks = intervalTicks;

		this.boss.save();
	}

	public void setShape(ParticleShape shape) {
		this.shape = shape;

		this.boss.save();
	}

	public void setRadius(double radius) {
		this.radius = radius;

		this.boss.save();
	}

	public void setHeight(double height) {
		this.height = height;

		this.boss.save();
	}

	public void setStrands(int strands) {
		this.strands = strands;

		this.boss.save();
	}

	public void setForwardOffset(double forwardOffset) {
		this.forwardOffset = forwardOffset;

		this.boss.save();
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.fromArray(
				"Enabled", this.enabled,
				"Type", this.type != null ? this.type.name() : null,
				"Shape", this.shape.name(),
				"Count", this.count,
				"Offset_X", this.offsetX,
				"Offset_Y", this.offsetY,
				"Offset_Z", this.offsetZ,
				"Speed", this.speed,
				"Interval_Ticks", this.intervalTicks,
				"Radius", this.radius,
				"Height", this.height,
				"Strands", this.strands,
				"Forward_Offset", this.forwardOffset);
	}

	public static BossParticleSettings deserialize(SerializedMap map, Boss boss) {
		final BossParticleSettings settings = new BossParticleSettings(boss);

		settings.enabled       = map.getBoolean("Enabled", false);
		settings.count         = map.getInteger("Count", 20);
		settings.offsetX       = map.getDouble("Offset_X", 0.3);
		settings.offsetY       = map.getDouble("Offset_Y", 0.5);
		settings.offsetZ       = map.getDouble("Offset_Z", 0.3);
		settings.speed         = map.getDouble("Speed", 0.01);
		settings.intervalTicks = map.getInteger("Interval_Ticks", 2);
		settings.radius        = map.getDouble("Radius", 1.0);
		settings.height        = map.getDouble("Height", 2.0);
		settings.strands        = map.getInteger("Strands", 3);
		settings.forwardOffset  = map.getDouble("Forward_Offset", 0.0);

		final String shapeName = map.getString("Shape");

		if (shapeName != null)
			try {
				settings.shape = ParticleShape.valueOf(shapeName);
			} catch (final IllegalArgumentException e) {
				CommonCore.warning("Unknown particle shape '" + shapeName + "' for Boss " + boss.getName() + ", defaulting to AMBIENT.");

				settings.shape = ParticleShape.AMBIENT;
			}
		else
			settings.shape = ParticleShape.AMBIENT;

		final String typeName = map.getString("Type");

		if (typeName != null)
			try {
				settings.type = CompParticle.fromName(typeName);
			} catch (final IllegalArgumentException e) {
				CommonCore.warning("Unknown particle type '" + typeName + "' for Boss " + boss.getName() + ", removing.");

				settings.type = null;
			}

		return settings;
	}
}
