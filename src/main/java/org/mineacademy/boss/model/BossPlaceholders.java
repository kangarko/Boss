package org.mineacademy.boss.model;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mineacademy.boss.PlayerCache;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.spawn.SpawnRule;
import org.mineacademy.boss.spawn.SpawnRuleRespawn;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleExpansion;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Dynamically insert data variables for PlaceholderAPI
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BossPlaceholders extends SimpleExpansion {

	/**
	 * The singleton of this class
	 */
	@Getter
	private static final SimpleExpansion instance = new BossPlaceholders();

	/**
	 * @see org.mineacademy.fo.model.SimpleExpansion#onReplace(org.bukkit.command.CommandSender, java.lang.String)
	 */
	@Override
	protected String onReplace(FoundationPlayer audience, String params) {
		final Player player = audience != null && audience.isPlayer() ? audience.getPlayer() : null;
		final String bossName = args[0];

		if (args.length == 2) {
			final String secondArg = args[1];

			if ("alias".equals(secondArg) || "alias_plain".equals(secondArg)) {
				final Boss boss = Boss.findBoss(bossName);

				if (boss == null)
					return "Unknown Boss";

				if ("alias".equals(secondArg))
					return boss.getAlias();

				else
					return SimpleComponent.fromMiniAmpersand(boss.getAlias()).toPlain();
			}
		}

		// %boss_Zombie_top_kills_1% etc.
		if (args.length == 5) {
			final String secondArg = args[1];
			final String thirdArg = args[2];
			final String forthArg = args[3];
			final String fifthArg = args[4];

			if (secondArg.equals("top")) {
				int order;

				try {
					order = Integer.parseInt(forthArg);

				} catch (final NumberFormatException ex) {
					return "Invalid order number " + forthArg;
				}

				final Boss boss = Boss.findBoss(bossName);

				if (boss == null)
					return "Invalid boss " + bossName;

				final Map<Double, String> stats = boss.getTopStats(thirdArg);

				if (stats.size() >= order) {
					int index = 1;

					for (final Map.Entry<Double, String> entry : stats.entrySet()) {
						if (index == order) {
							final double key = entry.getKey();
							final String value = entry.getValue();

							if ("kills".equals(thirdArg))
								return fifthArg.equals("player") ? value : String.valueOf((int) key);

							else if ("damage".equals(thirdArg))
								return fifthArg.equals("player") ? value : MathUtil.formatTwoDigits(key);

							else
								return "Invalid stat " + thirdArg;
						}

						index++;
					}
				}

				if ("kills".equals(thirdArg))
					return fifthArg.equals("player") ? "" : "0";

				else if ("damage".equals(thirdArg))
					return fifthArg.equals("player") ? "" : "0";

				else
					return "Invalid stat " + thirdArg;
			}

		} else if (args.length == 3) {
			final String secondArg = args[1];
			final String thirdArg = args[2];

			if (thirdArg.equals("kills") || thirdArg.equals("damage")) {
				UUID targetUid;

				if (secondArg.equals("player"))
					targetUid = player != null ? player.getUniqueId() : null;

				else if (secondArg.contains("-"))
					try {
						targetUid = UUID.fromString(secondArg);

					} catch (final IllegalArgumentException ex) {
						return "Invalid UUID " + secondArg;
					}

				else {
					final Player target = Bukkit.getPlayer(secondArg);

					if (target == null || !target.isOnline())
						return "player " + secondArg + " is offline";

					targetUid = target.getUniqueId();
				}

				if (targetUid == null)
					return "Invalid target";

				final Boss boss = Boss.findBoss(bossName);

				if (boss == null)
					return "Invalid boss " + bossName;

				if ("kills".equals(thirdArg))
					return String.valueOf(boss.getKills(targetUid));

				else if ("damage".equals(thirdArg))
					return MathUtil.formatTwoDigits(boss.getTotalDamage(targetUid));
			}

			else if ("respawn".equals(secondArg)) {
				final SpawnRule rule = SpawnRule.findRule(thirdArg);

				if (rule == null)
					return "Invalid Spawn Rule '" + thirdArg + "'";

				final Boss boss = Boss.findBoss(bossName);

				if (boss == null)
					return "Invalid boss " + bossName;

				if (!(rule instanceof SpawnRuleRespawn))
					return "Spawn Rule '" + thirdArg + "' Is Not 'Respawn After Death' Type";

				if (!rule.getBosses().contains(boss.getName()))
					return "Boss '" + bossName + "' Is Not Assigned To Spawn Rule '" + thirdArg + "'";

				final long now = System.currentTimeMillis();
				final long lastDeathTime = boss.getLastDeathFromSpawnRule(rule);

				if (lastDeathTime != 0) {
					final long difference = now - lastDeathTime;
					final long spawnRuleDelay = rule.getDelay().getTimeMilliseconds();

					return difference < spawnRuleDelay ? TimeUtil.formatTimeShort(((spawnRuleDelay - difference) / 1000) + 1) : Settings.Spawning.RESPAWN_PLACEHOLDER_PAST_DUE;
				} else
					return Settings.Spawning.RESPAWN_PLACEHOLDER_PAST_DUE;
			}
		}

		if ("name".equals(params) || "alias".equals(params) || "health".equals(params) || params.startsWith("top_damage") || "location".equals(params) || "location_x".equals(params) || "location_y".equals(params) || "location_z".equals(params) || "location_world".equals(params)) {
			if (!Bukkit.isPrimaryThread()) {
				Common.warning("Called Boss placeholder {" + params + "} asynchronoously. It requires getting the closest entity, which must be done on the main thread. Do NOT report this, this is caused by you using the variable in a plugin which replaces them async such as CMI. Adjust your setup or contact their developers and ask how to replace variables on the main thread.");

				return "";
			}

			final SpawnedBoss closestBoss = player != null ? Boss.findClosestBoss(player.getLocation(), Settings.Variables.NEARBY_BOSS_RADIUS) : null;
			final boolean hasBoss = closestBoss != null;

			final Location location = hasBoss ? closestBoss.getEntity().getLocation() : null;
			final TreeMap<Double, Player> lastDamageMap = hasBoss ? closestBoss.getBoss().getRecentDamagerPlayer(closestBoss.getEntity(), false) : null;

			if (hasBoss && params.startsWith("top_damage")) {
                try {
					final String[] split = params.split("_");
                    final int index = split.length < 3 ? 1 : Integer.parseInt(split[2]);
                    final boolean damager = params.startsWith("top_damager");

					if(index > lastDamageMap.size())
						return damager ? Lang.legacy("placeholder-no-damage-player") : Lang.legacy("placeholder-no-damage-value");

                    final NumberFormat damageFormat = new DecimalFormat("#.##");

                    final Object entry = new ArrayList<>(damager ? lastDamageMap.values() : lastDamageMap.keySet()).get(index - 1);

                    return damager ? ((Player)entry).getName() : damageFormat.format(entry);
                } catch(IndexOutOfBoundsException | IllegalArgumentException exception) {
                    return "Invalid key " + params;
                }
			}

			//
			// Variables that do not require any sender
			//
			switch (params) {
				case "name":
					return hasBoss ? closestBoss.getName() : "";
				case "alias":
					return hasBoss ? closestBoss.getBoss().getAlias() : "";
				case "alias_plain":
					return hasBoss ? SimpleComponent.fromMiniAmpersand(closestBoss.getBoss().getAlias()).toPlain() : "";
				case "health":
					return hasBoss ? String.valueOf(Remain.getHealth(closestBoss.getEntity())) : "";
				case "location":
					return hasBoss ? SerializeUtil.serializeLocation(location) : "";
				case "location_x":
					return hasBoss ? String.valueOf(location.getBlockX()) : "";
				case "location_y":
					return hasBoss ? String.valueOf(location.getBlockY()) : "";
				case "location_z":
					return hasBoss ? String.valueOf(location.getBlockZ()) : "";
				case "location_world":
					return hasBoss ? String.valueOf(location.getWorld().getName()) : "";
			}
		}

		if ("has_region".contains(params) || "region_primary_x".contains(params) || "region_primary_y".contains(params) || "region_primary_z".contains(params) || "region_secondary_x".contains(params) || "region_secondary_y".contains(params) || "region_secondary_z".contains(params) || "region_world".contains(params) || "region_size".contains(params)) {
			final Region region = player != null ? PlayerCache.from(player).getCreatedRegion() : null;

			switch (params) {
				case "has_region":
					return Lang.legacy("placeholder-creating-region-" + (region != null && region.isWhole() ? "yes" : "no"));
				case "region_primary_x":
					return region == null || region.getPrimary() == null ? "" : String.valueOf(region.getPrimary().getBlockX());
				case "region_primary_y":
					return region == null || region.getPrimary() == null ? "" : String.valueOf(region.getPrimary().getBlockY());
				case "region_primary_z":
					return region == null || region.getPrimary() == null ? "" : String.valueOf(region.getPrimary().getBlockZ());
				case "region_secondary_x":
					return region == null || region.getSecondary() == null ? "" : String.valueOf(region.getSecondary().getBlockX());
				case "region_secondary_y":
					return region == null || region.getSecondary() == null ? "" : String.valueOf(region.getSecondary().getBlockY());
				case "region_secondary_z":
					return region == null || region.getSecondary() == null ? "" : String.valueOf(region.getSecondary().getBlockZ());
				case "region_world":
					return region == null ? "" : !region.isWhole() ? "region incomplete" : region.getWorld().getName();
				case "region_size":
					return region == null || !region.isWhole() ? "" : String.format("%,d", region.getBlocks().size());
			}
		}

		return null;
	}
}
