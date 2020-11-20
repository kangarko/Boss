package org.mineacademy.boss.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.exception.InvalidWorldException;
import org.mineacademy.fo.settings.YamlSectionConfig;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.Getter;

@Getter
public final class SimpleRegionData extends YamlSectionConfig {

	private final static SimpleRegionData instance = new SimpleRegionData();

	public static SimpleRegionData $() {
		return instance;
	}

	// Name, Region
	private final StrictList<VisualizedRegion> regions = new StrictList<>();

	public SimpleRegionData() {
		super("Regions");

		loadConfiguration(NO_DEFAULT, FoConstants.File.DATA);
	}

	@Override
	protected void onLoadFinish() {
		regions.clear();

		if (!isSet("Converted")) {
			convert("Stored", Map.class, Map.class, old -> {
				final Map<String, Object> newMap = new HashMap<>();
				final Map<String, Object> region = (Map<String, Object>) old.get("Region");

				// No need to convert
				if (region == null)
					return old;

				newMap.put("Primary", region.get("Primary"));
				newMap.put("Secondary", region.get("Secondary"));
				newMap.put("Name", old.get("Name"));

				return newMap;
			});

			setNoSave("Converted", true);
		}

		final List<?> stored = getList("Stored");

		if (stored != null)
			for (final Object map : stored)
				try {
					final VisualizedRegion region = VisualizedRegion.deserialize(SerializedMap.of(map));

					regions.add(region);
				} catch (final Throwable ex) {
					Throwable t = ex;

					while (t.getCause() != null)
						t = t.getCause();

					if (t instanceof InvalidWorldException)
						Common.log("Skipping Boss region with invalid world. Region data: " + map);
					else
						t.printStackTrace();
				}
	}

	public boolean hasRegion(String name) {
		return getRegion(name) != null;
	}

	public VisualizedRegion getRegion(String name) {
		for (final VisualizedRegion region : regions)
			if (region.getName() != null && region.getName().equalsIgnoreCase(name))
				return region;

		return null;
	}

	public List<VisualizedRegion> getRegions(Location loc) {
		final List<VisualizedRegion> found = new ArrayList<>();

		for (final VisualizedRegion region : regions)
			if (region.isWithin(loc))
				found.add(region);

		return found;
	}

	public void addRegion(VisualizedRegion region) {
		regions.add(region);

		update();
	}

	public void removeRegion(String name) {
		final VisualizedRegion rg = getRegion(name);
		Valid.checkNotNull(rg, "Region " + name + " does not exist");

		regions.remove(rg);
		update();
	}

	private void update() {
		save("Stored", regions);

		onLoadFinish();
	}
}
