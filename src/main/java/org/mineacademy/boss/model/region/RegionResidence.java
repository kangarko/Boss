package org.mineacademy.boss.model.region;

import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.remain.CompMaterial;

public final class RegionResidence implements Regionable {

	@Override
	public Collection<String> getAll() {
		return HookManager.getResidences();
	}

	@Override
	public List<String> getObject(Location loc) {
		return listOrNull(HookManager.getResidence(loc));
	}

	@Override
	public Material getButtonMaterial() {
		return CompMaterial.WOODEN_AXE.getMaterial();
	}

	@Override
	public String[] getButtonDescription() {
		return new String[] {
				"Residences created in",
				"the Residence plugin."
		};
	}
}
