package org.mineacademy.boss.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.boss.api.BossRegionSpawning;
import org.mineacademy.boss.api.BossRegionType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.region.Region;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class SimpleRegionSpawning implements BossRegionSpawning {

	private final SimpleSpawning spawning;

	private final StrictMap<BossRegionType, StrictList<BossRegionSettings>> regions = new StrictMap<>();

	private boolean blacklist;

	private void update() {
		spawning.update();
	}

	/**
	 * @see org.mineacademy.boss.api.BossRegionSpawning#getFormatted()
	 */
	@Override
	public String getFormatted() {
		final Map<String, List<String>> formatted = new HashMap<>();

		for (final Map.Entry<BossRegionType, StrictList<BossRegionSettings>> entry : regions.entrySet())
			formatted.put(entry.getKey().name(), Common.convert(entry.getValue(), BossRegionSettings::getRegionName));

		return formatted.toString();
	}

	@Override
	public boolean isWithinAny(final Location loc) {
		for (final Entry<BossRegionType, StrictList<BossRegionSettings>> e : regions.entrySet()) {
			final BossRegionType rg = e.getKey();
			final StrictList<BossRegionSettings> stored = e.getValue();

			if (!stored.isEmpty() && rg.isWithin(loc, stored.getSource()))
				return true;
		}

		return false;
	}

	/**
	 * Get the lowest region's height in this area
	 *
	 * @param loc location to check
	 * @return region height, or -1 if none
	 */
	@Override
	public int getLowestRegionHeight(final Location loc) {

		for (final Map.Entry<BossRegionType, StrictList<BossRegionSettings>> settings : regions.entrySet()) {
			final int x = loc.getBlockX();
			final int z = loc.getBlockZ();

			for (final BossRegionSettings rgs : settings.getValue()) {
				final Region region = settings.getKey().getBoundingBox(rgs.getRegionName());

				if (region != null) {
					final int primX = region.getPrimary().getBlockX();
					final int primZ = region.getPrimary().getBlockZ();

					final int secX = region.getSecondary().getBlockX();
					final int secZ = region.getSecondary().getBlockZ();

					if (primX <= x && x <= secX && primZ <= z && z <= secZ)
						return region.getPrimary().getBlockY(); // "in" region
				}
			}
		}
		return -1;
	}

	@Override
	public boolean hasRegionSpawning() {
		for (final StrictList<BossRegionSettings> list : regions.values())
			if (!list.isEmpty())
				return true;

		return false;
	}

	@Override
	public boolean hasRegions(final BossRegionType type) {
		return regions.containsKey(type) && !regions.get(type).isEmpty();
	}

	@Override
	public boolean hasRegion(final BossRegionType type, final String regionName) {
		if (!hasRegions(type))
			return false;

		for (final BossRegionSettings rg : regions.get(type))
			if (rg.getRegionName().equalsIgnoreCase(regionName))
				return true;

		return false;
	}

	@Override
	public int getCount(final BossRegionType type) {
		return hasRegions(type) ? regions.get(type).size() : 0;
	}

	@Override
	public void remove(final BossRegionType type, final String regionName) {
		final StrictList<BossRegionSettings> list = regions.get(type);
		boolean updated = false;

		if (list != null)
			for (final BossRegionSettings rg : list)
				if (rg.getRegionName().equalsIgnoreCase(regionName)) {
					list.remove(rg);

					updated = true;
					break;
				}

		Valid.checkBoolean(updated, "Cannot remove non-existing " + type + " region '" + regionName + "'");
		update();
	}

	@Override
	public void add(final BossRegionType type, final String regionName) {
		Valid.checkBoolean(!hasRegion(type, regionName), "Cannot add already existing " + type + " region '" + regionName + "'");

		StrictList<BossRegionSettings> list = regions.get(type);

		if (list == null) {
			list = new StrictList<>();

			regions.put(type, list);
		}

		list.add(new SimpleRegionSettings(spawning, regionName));
		update();
	}

	@Override
	public BossRegionSettings findRegion(final BossRegionType type, final String name) {
		for (final BossRegionSettings rg : regions.get(type))
			if (rg.getRegionName().equalsIgnoreCase(name))
				return rg;

		return null;
	}

	@Override
	public void clear(final BossRegionType type) {
		if (hasRegions(type)) {
			regions.get(type).clear();

			update();
		}
	}

	@Override
	public void setBlacklist(final boolean blacklist) {
		this.blacklist = blacklist;

		update();
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		for (final Map.Entry<BossRegionType, StrictList<BossRegionSettings>> e : regions.entrySet())
			map.put(e.getKey().getPlugin(), e.getValue());

		map.put("Blacklist", blacklist);

		return map;
	}

	public static SimpleRegionSpawning deserialize(final SerializedMap map, final SimpleSpawning spawning) {
		final SimpleRegionSpawning c = new SimpleRegionSpawning(spawning);

		for (final BossRegionType reg : BossRegionType.values()) {
			StrictList<BossRegionSettings> list = new StrictList<>();

			if (map.containsKey(reg.getPlugin())) {
				final List<?> oldList = (List<?>) map.asMap().get(reg.getPlugin());

				if (!oldList.isEmpty())
					// Upgrade old
					if (oldList.get(0) instanceof String)
						list = convert(spawning, map.getStringList(reg.getPlugin()));
					else // Assume the rest is a HashMap
						for (final Object raw : oldList)
							list.add(SimpleRegionSettings.deserialize(SerializedMap.of(Common.getMapFromSection(raw)), spawning));
			}

			c.regions.put(reg, list);
		}

		c.blacklist = map.getBoolean("Blacklist", false);

		return c;
	}

	private static StrictList<BossRegionSettings> convert(final SimpleSpawning spawning, final List<String> regionNames) {
		final StrictList<BossRegionSettings> list = new StrictList<>();

		for (final String rg : regionNames)
			list.add(new SimpleRegionSettings(spawning, rg));

		return list;
	}
}