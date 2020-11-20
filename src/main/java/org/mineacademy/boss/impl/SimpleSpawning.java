package org.mineacademy.boss.impl;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.block.Biome;
import org.mineacademy.boss.api.BossNativeConditions;
import org.mineacademy.boss.api.BossRegionSpawning;
import org.mineacademy.boss.api.BossSpawning;
import org.mineacademy.boss.util.AutoUpdateMap;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.Getter;

@Getter
public final class SimpleSpawning implements BossSpawning {

	private final SimpleSettings settings;

	private final AutoUpdateMap<String, Integer> worlds;
	private final AutoUpdateMap<Biome, Integer> biomes;
	private BossNativeConditions conditions;
	private BossRegionSpawning regions;

	public SimpleSpawning(SimpleSettings settings) {
		this.settings = settings;

		this.worlds = new AutoUpdateMap<>(this::update);
		this.biomes = new AutoUpdateMap<>(this::update);
	}

	public void update() {
		settings.updateSpawning(this);
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.put("Worlds", convert(worlds));
		map.put("Biomes", convert(biomes));
		map.put("Conditions", conditions);
		map.put("Regions", regions);

		return map;
	}

	private Map<String, String> convert(AutoUpdateMap<?, Integer> toConvert) {
		final HashMap<String, String> copy = new HashMap<>();

		for (final Map.Entry<?, Integer> e : toConvert.entrySet())
			if (e.getValue() != 0)
				copy.put(e.getKey().toString(), e.getValue() + "%");

		return copy;
	}

	public static SimpleSpawning deserialize(SerializedMap map, SimpleSettings settings) {
		final SimpleSpawning s = new SimpleSpawning(settings);

		if (map.containsKey("Worlds"))
			for (final Map.Entry<String, Object> e : map.getMap("Worlds").entrySet()) {
				final int percentage = Integer.parseInt(e.getValue().toString().replace("%", ""));

				s.worlds.getSource().put(e.getKey(), percentage);
			}

		if (map.containsKey("Biomes"))
			for (final Map.Entry<String, Object> e : map.getMap("Biomes").entrySet()) {
				final int percentage = Integer.parseInt(e.getValue().toString().replace("%", ""));

				final Biome b = ReflectionUtil.lookupEnumSilent(Biome.class, e.getKey());

				if (b != null)
					s.biomes.getSource().put(b, percentage);
				else
					Common.log("Boss does not support your custom biome '" + e.getKey() + "', skipping..");
			}

		s.regions = SimpleRegionSpawning.deserialize(map.containsKey("Regions") ? map.getMap("Regions") : new SerializedMap(), s);
		s.conditions = SimpleNativeConditions.deserialize(map.containsKey("Conditions") ? map.getMap("Conditions") : new SerializedMap(), s);

		return s;
	}
}
