package org.mineacademy.boss.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.BossDrop;
import org.mineacademy.boss.api.BossEquipment;
import org.mineacademy.boss.api.BossEquipmentSlot;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class SimpleEquipment implements BossEquipment {

	private final SimpleSettings settings;

	private final StrictMap<BossEquipmentSlot, BossDrop> equipment = new StrictMap<>();
	private boolean allowRandom;

	@Override
	public void setNoSave(final BossEquipmentSlot slot, final ItemStack item) {
		setNoSave(slot, item, 0F);
	}

	@Override
	public void setNoSave(final BossEquipmentSlot slot, ItemStack item, final float dropChance) {
		if (item != null && item.getType() == Material.AIR)
			item = null;

		equipment.override(slot, new BossDrop(item, dropChance));
	}

	@Override
	public BossDrop get(final BossEquipmentSlot slot) {
		return equipment.get(slot);
	}

	@Override
	public void setAllowRandomNoSave(final boolean flag) {
		this.allowRandom = flag;
	}

	@Override
	public boolean allowRandom() {
		return allowRandom;
	}

	@Override
	public Iterator<BossDrop> iterator() {
		return equipment.values().iterator();
	}

	@Override
	public Map<BossEquipmentSlot, BossDrop> getAllCopy() {
		return Collections.unmodifiableMap(equipment.getSource());
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		for (final Map.Entry<BossEquipmentSlot, BossDrop> e : equipment.entrySet())
			map.put(e.getKey().toString(), e.getValue().serialize());

		map.put("Allow_Random", allowRandom);

		return map;
	}

	public static SimpleEquipment deserialize(final SerializedMap map, final SimpleSettings settings) {
		final SimpleEquipment eq = new SimpleEquipment(settings);

		if (map != null)
			for (final Entry<String, Object> e : map.asMap().entrySet()) {

				if (e.getKey().equals("Allow_Random")) {
					eq.allowRandom = Boolean.parseBoolean(e.getValue().toString());

					continue;
				}

				final BossEquipmentSlot slot = ReflectionUtil.lookupEnum(BossEquipmentSlot.class, e.getKey());

				final Map<String, Object> drops = Common.getMapFromSection(e.getValue());

				if (drops != null) {
					final BossDrop drop = BossDrop.deserialize(SerializedMap.of(drops), settings);

					if (drop != null)
						eq.equipment.put(slot, drop);
				}
			}

		return eq;
	}
}
