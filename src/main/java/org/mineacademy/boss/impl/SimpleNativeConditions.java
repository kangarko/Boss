package org.mineacademy.boss.impl;

import org.mineacademy.boss.api.BossNativeConditions;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.RangedValue;

import lombok.Getter;

@Getter
public final class SimpleNativeConditions implements BossNativeConditions {

	private final SimpleSpawning spawning;

	private RangedValue time;
	private RangedValue light;
	private RangedValue height;

	private boolean rainRequired;
	private boolean thunderRequired;
	private boolean spawnUnderWater;

	private SimpleNativeConditions(final SimpleSpawning spawning) {
		this.spawning = spawning;
	}

	@Override
	public void setTime(final RangedValue time) {
		this.time = time == null ? new RangedValue(0, 24000) : time;

		update();
	}

	@Override
	public void setLight(final RangedValue light) {
		this.light = light == null ? new RangedValue(0, 15) : light;

		update();
	}

	@Override
	public void setHeight(final RangedValue height) {
		this.height = height == null ? new RangedValue(0, 256) : height;

		update();
	}

	@Override
	public void setRainRequired(final boolean flag) {
		this.rainRequired = flag;

		update();
	}

	@Override
	public void setThunderRequired(final boolean flag) {
		this.thunderRequired = flag;

		update();
	}

	@Override
	public boolean canSpawnUnderWater() {
		return spawnUnderWater;
	}

	@Override
	public void setSpawnUnderWater(final boolean spawnUnderWater) {
		this.spawnUnderWater = spawnUnderWater;

		update();
	}

	private void update() {
		spawning.update();
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.put("Time", time.toString());
		map.put("Light", light.toString());
		map.put("Height", height.toString());
		map.put("Rain", rainRequired);
		map.put("Thunder", thunderRequired);
		map.put("Spawn_Under_Water", spawnUnderWater);

		return map;
	}

	public static SimpleNativeConditions deserialize(final SerializedMap map, final SimpleSpawning spawning) {
		final SimpleNativeConditions cond = new SimpleNativeConditions(spawning);

		cond.time = getOrDef(map, "Time", new RangedValue(0, 24000));
		cond.light = getOrDef(map, "Light", new RangedValue(0, 15));
		cond.height = getOrDef(map, "Height", new RangedValue(0, 256));

		if (map != null) {
			cond.rainRequired = map.getBoolean("Rain", false);
			cond.thunderRequired = map.getBoolean("Thunder", false);
			cond.spawnUnderWater = map.getBoolean("Spawn_Under_Water", false);
		}

		return cond;
	}

	private static RangedValue getOrDef(final SerializedMap map, final String path, final RangedValue def) {
		return map != null && map.containsKey(path) ? RangedValue.parse(map.getString(path)) : def;
	}
}
