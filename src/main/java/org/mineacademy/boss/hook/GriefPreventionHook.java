package org.mineacademy.boss.hook;

import org.bukkit.Location;

import lombok.Getter;
import lombok.Setter;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public final class GriefPreventionHook {

	@Getter
	@Setter
	private static boolean enabled = false;

	public static boolean isClaimed(Location location) {
		return enabled ? Impl.isClaimed(location) : false;
	}
}

class Impl {

	static boolean isClaimed(Location location) {
		return GriefPrevention.instance.dataStore.getClaimAt(location, false, null) != null;
	}
}