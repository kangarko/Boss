package org.mineacademy.boss.impl;

import java.util.List;

import org.mineacademy.boss.api.BossEggItem;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public final class SimpleEggItem implements BossEggItem {

	@Getter(value = AccessLevel.PRIVATE)
	private final SimpleSettings settings;

	private CompMaterial material;
	private String name;
	private List<String> lore;
	private boolean glowing;

	public SimpleEggItem(final SimpleSettings settings) {
		this.settings = settings;
	}

	public static SimpleEggItem getDefault() {
		final SimpleEggItem s = new SimpleEggItem(null);

		s.material = Settings.EggSpawning.Item.MATERIAL;
		s.glowing = Settings.EggSpawning.Item.GLOW;
		s.name = Settings.EggSpawning.Item.NAME;
		s.lore = Settings.EggSpawning.Item.LORE;

		return s;
	}

	public static SimpleEggItem deserialize(final SerializedMap map, final SimpleSettings settings) {
		if (map.isEmpty())
			return getDefault();

		final SimpleEggItem s = new SimpleEggItem(settings);

		s.material = map.getMaterial("Material");
		s.name = map.getString("Name");
		s.lore = map.getStringList("Lore");
		s.glowing = map.getBoolean("Glow");

		return s;
	}

	@Override
	public void setMaterial(final CompMaterial mat) {
		material = mat;
		update();
	}

	@Override
	public void setName(final String name) {
		this.name = name;
		update();
	}

	@Override
	public void setLore(final List<String> lore) {
		this.lore = lore;
		update();
	}

	@Override
	public void setGlowing(final boolean glow) {
		glowing = glow;
		update();
	}

	public void update() {
		settings.updateEggItem(this);
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.put("Material", material.name());
		map.put("Glow", glowing);
		map.put("Name", name);
		map.put("Lore", lore);

		return map;
	}
}
