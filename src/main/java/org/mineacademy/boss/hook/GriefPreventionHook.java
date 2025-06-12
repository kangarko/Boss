package org.mineacademy.boss.hook;

import org.bukkit.Location;

import lombok.Getter;
import lombok.Setter;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

/**
 * Connector to GriefPrevention.
 */
public final class GriefPreventionHook {

	/**
	 * Return true if the plugin is hooked.
	 */
	@Getter
	@Setter
	private static boolean enabled = false;

	/**
	 * Return the claim owner at the given location.
	 *
	 * @param location
	 * @return
	 */
	public static String getClaimOwner(Location location) {
		if (enabled) {
			final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);

			if (claim != null)
				return claim.getOwnerName();
		}

		return null;
	}
}