package org.mineacademy.boss.impl;

import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.Getter;

@Getter
public final class SimpleRegionSettings implements BossRegionSettings {

	private final SimpleSpawning spawning;

	private final String regionName;
	private int limit = -1;
	private boolean keepInside;

	public SimpleRegionSettings(final SimpleSpawning spawning, final String name) {
		this.spawning = spawning;
		this.regionName = name;
	}

	@Override
	public void setLimit(final int limit) {
		this.limit = limit;

		update();
	}

	@Override
	public void setKeepInside(final boolean keepInside) {
		this.keepInside = keepInside;

		update();
	}

	@Override
	public boolean getKeepInside() {
		return keepInside;
	}

	private void update() {
		spawning.update();
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.put("Name", regionName);
		map.put("Limit", limit);
		map.put("Keep_Inside", keepInside);

		return map;
	}

	@Override
	public String toString() {
		return "Region{" + regionName + ", limit=" + limit + ", keepInside=" + keepInside + "}";
	}

	public static SimpleRegionSettings deserialize(final SerializedMap map, final SimpleSpawning spawning) {
		final SimpleRegionSettings c = new SimpleRegionSettings(spawning, map.getString("Name"));

		c.limit = map.getInteger("Limit", -1);
		c.keepInside = map.getBoolean("Keep_Inside", false);

		return c;
	}
}