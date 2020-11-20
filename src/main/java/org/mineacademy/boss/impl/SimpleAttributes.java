package org.mineacademy.boss.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.WordUtils;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossAttributes;
import org.mineacademy.boss.model.BossAttribute;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.MissingEnumException;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.remain.CompAttribute;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SimpleAttributes implements BossAttributes {

	@NonNull
	private final Boss boss;

	private final StrictMap<BossAttribute, Double> attributes = new StrictMap<>();

	@Override
	public double get(final BossAttribute attr) {
		return attributes.getOrDefault(attr, getDefaultBase(attr));
	}

	@Override
	public double getDefaultBase(final BossAttribute attr) {
		final LivingEntity entity = ((SimpleBoss) boss).spawnDummy(false);
		final Double attribute = CompAttribute.valueOf(attr.toString()).get(entity);

		entity.remove();
		return attribute != null ? attribute : 0;
	}

	@Override
	public void set(final BossAttribute attr, final double value) {
		attributes.override(attr, value);

		((SimpleSettings) boss.getSettings()).updateAttributes(this);
	}

	@Override
	public Set<BossAttribute> getConfigured() {
		return Collections.unmodifiableSet(attributes.keySet());
	}

	@Override
	public Set<BossAttribute> getVanilla() {
		final Set<BossAttribute> attributes = new HashSet<>();
		final LivingEntity en = ((SimpleBoss) boss).spawnDummy(false);

		for (final BossAttribute attr : BossAttribute.values())
			if (CompAttribute.valueOf(attr.toString()).get(en) != null)
				attributes.add(attr);

		return attributes;
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		for (final Entry<BossAttribute, Double> e : attributes.entrySet())
			map.put(WordUtils.capitalizeFully(e.getKey().toString().replace("GENERIC_", "").toLowerCase().replace("_", " ")), e.getValue());

		return map;
	}

	public static SimpleAttributes deserialize(final SerializedMap map, final Boss boss) {
		final SimpleAttributes attributes = new SimpleAttributes(boss);

		if (map != null)
			for (final Entry<String, Object> e : map.asMap().entrySet()) {
				final String name = e.getKey();
				final Double value = Double.parseDouble(e.getValue().toString());
				BossAttribute attr = null;

				try {
					attr = ReflectionUtil.lookupEnum(BossAttribute.class, name);
				} catch (final MissingEnumException ex) {
				}

				if (attr == null && !name.startsWith("GENERIC_") && !name.startsWith("Max Health"))
					attr = ReflectionUtil.lookupEnum(BossAttribute.class, "GENERIC_" + name);

				if (attr != null)
					attributes.attributes.put(attr, value);
			}

		return attributes;
	}
}