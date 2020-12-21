package org.mineacademy.boss.model.region;

import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.remain.CompMaterial;

public final class RegionTowny implements Regionable {

	@Override
	public Collection<String> getAll() {
		return HookManager.getTowns();
	}

	@Override
	public List<String> getObject(Location loc) {
		return listOrNull(HookManager.getTown(loc));
	}

	@Override
	public Material getButtonMaterial() {
		return CompMaterial.BRICK.getMaterial();
	}

	@Override
	public String[] getButtonDescription() {
		return new String[] {
				"Towns from the",
				"Towny plugin."
		};
	}
}
