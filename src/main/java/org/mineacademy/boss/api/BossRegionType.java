package org.mineacademy.boss.api;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.model.region.RegionBoss;
import org.mineacademy.boss.model.region.RegionFactions;
import org.mineacademy.boss.model.region.RegionResidence;
import org.mineacademy.boss.model.region.RegionTowny;
import org.mineacademy.boss.model.region.RegionWorldGuard;
import org.mineacademy.boss.model.region.Regionable;
import org.mineacademy.fo.region.Region;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Regions from other plugins
 */
@RequiredArgsConstructor
public enum BossRegionType {

	/**
	 * Our native region accessible with /boss region command.
	 */
	BOSS("Boss", new RegionBoss()),

	/**
	 * Residence plugin.
	 */
	RESIDENCE("Residence", new RegionResidence()),

	/**
	 * Factions UUID, MCore Factions, Factions 1.6 and LegacyFactions
	 */
	FACTIONS("Factions", new RegionFactions()),

	/**
	 * Towny plugin.
	 */
	TOWNY("Towny", new RegionTowny()),

	/**
	 * WorldGuard plugin.
	 */
	WORLD_GUARD("WorldGuard", new RegionWorldGuard());

	/**
	 * The plugin name
	 */
	@Getter
	private final String plugin;

	/**
	 * The region implementation
	 */
	private final Regionable regions;

	/**
	 * Locale-sensitive comparator of the regions.
	 */
	private final Collator collator = Collator.getInstance();

	/**
	 * Get all known regions
	 *
	 * @return all known regions
	 */
	@NonNull
	public final Collection<String> getRegions() {
		final List<String> sorted = new ArrayList<>(regions.getAll());

		java.util.Collections.sort(sorted, (o1, o2) -> collator.compare(o1.toLowerCase(), o2.toLowerCase()));

		return sorted;
	}

	/**
	 * Get if the location is within a list
	 *
	 * @param loc             the location
	 * @param evaluateAgainst the list of regions
	 * @return true if the location is in the list
	 */
	public final boolean isWithin(@NonNull Location loc, @NonNull List<BossRegionSettings> evaluateAgainst) {
		return regions.isWithin(loc, evaluateAgainst);
	}

	public final List<String> findRegions(@NonNull Location loc) {
		return regions.getObject(loc);
	}

	public final Region getBoundingBox(String region) {
		return regions.getBoundingBox(region);
	}

	public final Collection<SpawnedBoss> findBosses(World w, String region) {
		final Collection<SpawnedBoss> found = new ArrayList<>();

		for (final SpawnedBoss spawned : BossPlugin.getBossManager().findBosses(w)) {
			final LivingEntity entity = spawned.getEntity();

			if (entity.isValid() && !entity.isDead()) {
				final List<String> regions = findRegions(entity.getLocation());

				if (regions != null && regions.contains(region))
					found.add(spawned);
			}
		}

		return found;
	}

	/**
	 * Get if the plugin is enabled.
	 *
	 * @return if the plugin owning the regions is enabled
	 */
	public final boolean isPluginEnabled() {
		return regions.isPluginEnabled(plugin);
	}

	/**
	 * Get the button material for this region in menu
	 *
	 * @return the button material
	 */
	@NonNull
	public final Material getButtonMaterial() {
		return regions.getButtonMaterial();
	}

	/**
	 * Get the button description for this region in menu
	 *
	 * @return the button description
	 */
	@NonNull
	public final String[] getButtonDescription() {
		return regions.getButtonDescription();
	}
}
