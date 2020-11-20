package org.mineacademy.boss.model.region;

import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.mineacademy.fo.model.HookManager;

public final class RegionFactions implements Regionable {

	@Override
	public Collection<String> getAll() {
		return HookManager.getFactions();
	}

	@Override
	public List<String> getObject(Location loc) {
		return listOrNull(HookManager.getFaction(loc));
	}

	@Override
	public Material getButtonMaterial() {
		return Material.DIAMOND_SWORD;
	}

	@Override
	public boolean isPluginEnabled(String name) {
		return HookManager.isFactionsLoaded();
	}

	@Override
	public String[] getButtonDescription() {
		return new String[] {
				"Factions from the Factions",
				"plugin installed on your",
				"server."
		};
	}
}
