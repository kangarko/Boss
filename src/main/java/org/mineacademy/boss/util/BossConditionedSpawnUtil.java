package org.mineacademy.boss.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossNativeConditions;
import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.boss.api.BossRegionSpawning;
import org.mineacademy.boss.api.BossRegionType;
import org.mineacademy.boss.api.BossSpawnReason;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.hook.GriefPreventionHook;
import org.mineacademy.boss.model.BossManager;
import org.mineacademy.boss.model.task.BossTimedSpawnTask;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.settings.Settings.Limits;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BossConditionedSpawnUtil {

	public boolean spawnTimed(final Location location) {

		if (GriefPreventionHook.isClaimed(location))
			return false;

		final List<Boss> bosses = getBossesInRandomOrder();

		for (final Boss boss : bosses) {
			if (boss == null || !Settings.TimedSpawning.BOSSES.contains(boss.getName()))
				continue;

			final double y = findHighestBlock(location.getWorld(), location.getBlockX(), location.getBlockZ(), 1, false, boss.getSpawning().getConditions().canSpawnUnderWater());

			location.setY(y);

			if (!CompMaterial.isAir(location.getBlock()))
				location.setY(y + 1);

			if (Settings.TimedSpawning.Performance.CONDITIONS) {
				final boolean spawned = spawnConditioned(boss, location, BossSpawnReason.TIMED);

				if (spawned)
					return true;

			} else {
				final SpawnedBoss spawned = boss.spawn(location, BossSpawnReason.TIMED);

				if (spawned != null && spawned.getEntity().isValid())
					return true;
			}

			if (!Settings.ITERATE_SPAWNING_TRIES)
				break;
		}

		return false;
	}

	public boolean convert(final EntityType bossType, final Location location, final BossSpawnReason reason) {
		final List<Boss> bosses = getBossesInRandomOrder();

		for (final Boss boss : bosses)
			if (boss.getType() == bossType) {

				if (!RandomUtil.chance(boss.getSettings().getConvertingChance()))
					return false;

				final boolean spawned = spawnConditioned(boss, location, reason);

				if (spawned)
					return true;

				if (!Settings.ITERATE_SPAWNING_TRIES)
					return false;
			}

		return false;
	}

	private List<Boss> getBossesInRandomOrder() {
		final BossManager bm = BossPlugin.getBossManager();
		final List<Boss> bosses = new ArrayList<>(bm.getBosses());

		Collections.shuffle(bosses);

		return bosses;
	}

	public boolean spawnWithLimits(final Boss boss, final Location loc, final BossSpawnReason reason) {
		return spawnWithLimits(boss, loc, reason, null);
	}

	public boolean spawnWithLimits(final Boss boss, final Location loc, final BossSpawnReason reason, @Nullable final Player player) {

		if (GriefPreventionHook.isClaimed(loc))
			return false;

		final boolean checkLimits = reason == BossSpawnReason.COMMAND && Settings.Limits.APPLY_CMD || (reason == BossSpawnReason.EGG || reason == BossSpawnReason.DISPENSE) && Settings.Limits.APPLY_EGG;

		// Don't check limits if not applied

		if (reason == BossSpawnReason.EGG && !Limits.APPLY_EGG) {
			final SpawnedBoss spawned = boss.spawn(loc, reason);

			return spawned != null && spawned.isAlive();
		}

		if (reason == BossSpawnReason.COMMAND && !Limits.APPLY_CMD) {
			final SpawnedBoss spawned = boss.spawn(loc, reason);

			return spawned != null && spawned.isAlive();
		}

		if (checkLimits && (!checkLimitsWorld(boss, loc) || !checkLimitsChunk(boss, loc)) || !checkRegions(boss, loc) || !checkLimitsRegion(boss, loc))
			return false;

		if (reason == BossSpawnReason.EGG && Settings.EggSpawning.CHECK_REGIONS && !checkRegions(boss, loc))
			if (player != null && !PlayerUtil.hasPerm(player, Permissions.Bypass.EGG_REGION)) {
				// deny
				Debugger.debug("spawning", "Can't spawn " + boss.getName() + " : checkRegion()");
				return false;
			}

		final SpawnedBoss spawned = boss.spawn(loc, reason);

		return spawned != null && spawned.isAlive();
	}

	private int findHighestBlock(final World w, final int x, final int z, final int startHeight, final boolean tall, final boolean underwater) {
		if (!Settings.TimedSpawning.Performance.SPAWN_UNDERGROUND || RandomUtil.nextBoolean())
			return w.getHighestBlockYAt(x, z);

		return findHighestBlock0(w, x, z, startHeight, tall, underwater);
	}

	private int findHighestBlock0(final World w, final int x, final int z, final int startHeight, final boolean tall, final boolean underwater) {
		final Material water = CompMaterial.WATER.getMaterial();

		for (int y = startHeight; y < w.getMaxHeight(); ++y) {
			final Block ground = w.getBlockAt(x, y, z);
			final Block aboveGround = ground.getRelative(BlockFace.UP);
			final Block twoAboveGround = aboveGround.getRelative(BlockFace.UP);

			if ((CompMaterial.isAir(ground) || ground.getType() == CompMaterial.SNOW.getMaterial() || underwater && ground.getType() == water) && (CompMaterial.isAir(aboveGround) || underwater && aboveGround.getType() == water) && (!tall || CompMaterial.isAir(twoAboveGround) || underwater && twoAboveGround.getType() == water))
				return y;
		}
		return -1;
	}

	public boolean spawnConditioned(final Boss boss, Location location, final BossSpawnReason reason) {
		Valid.checkNotNull(boss, "Boss to spawn is null!");

		Debugger.debug("spawning", "Attempt to spawn " + boss.getName() + " at " + Common.shortLocation(location) + " for " + reason);

		if (GriefPreventionHook.isClaimed(location))
			return false;

		if (reason == BossSpawnReason.TIMED && !Settings.TimedSpawning.Performance.REGIONS) {
			// Do not check if disabled

		} else {
			final boolean tall = boss.getType() == EntityType.ENDERMAN;

			// if this boss can only spawn in regions, bump the location's height to the region height
			final BossRegionSpawning regionSpawning = boss.getSpawning().getRegions();

			if (!regionSpawning.isBlacklist() && regionSpawning.hasRegionSpawning()) {
				final int lowestHeight = boss.getSpawning().getRegions().getLowestRegionHeight(location);

				if (lowestHeight != -1 && location.getY() < lowestHeight) {
					final int highestBlockY = findHighestBlock0(location.getWorld(), (int) location.getX(), (int) location.getZ(), lowestHeight, tall, boss.getSpawning().getConditions().canSpawnUnderWater());

					if (highestBlockY == -1)
						return false;

					location = location.clone();
					location.setY(highestBlockY);
				}

			} else if (tall)
				if (location.getY() < location.getWorld().getMaxHeight() - 2 && !CompMaterial.isAir(location.getBlock().getRelative(0, 2, 0))) {
					Debugger.debug("spawning", "Can't spawn: Boss is to large");
					return false;
				}
		}

		final boolean canTransform = canTransform(boss, location);

		if (canTransform) {
			Debugger.debug("spawning", "\t[OK] Boss " + boss.getName() + " Spawned.");

			final long now = System.currentTimeMillis();
			final SpawnedBoss spawned = boss.spawn(location, reason);
			BossTimedSpawnTask.logLag("spawning", System.currentTimeMillis() - now);

			return spawned != null && spawned.isAlive();
		}

		Debugger.debug("spawning", "Can't transform");
		return false;
	}

	private boolean canTransform(final Boss boss, final Location loc) {

		long now = System.currentTimeMillis();
		final boolean worldLimit = checkLimitsWorld(boss, loc);
		BossTimedSpawnTask.logLag("limit-world", System.currentTimeMillis() - now);
		if (!worldLimit) { // Global and Individual world limits from settings.yml
			Debugger.debug("spawning", "world-limit reached for " + boss.getName());
			return false;
		}

		now = System.currentTimeMillis();
		final boolean chunkLimit = checkLimitsChunk(boss, loc);
		BossTimedSpawnTask.logLag("limit-chunk", System.currentTimeMillis() - now);
		// Global and Individual chunk limits from settings.yml
		if (!chunkLimit) {
			Debugger.debug("spawning", "Chunk limit reached for " + boss.getName());
			return false;
		}

		{
			now = System.currentTimeMillis();
			final boolean blacklist = boss.getSpawning().getRegions().isBlacklist();
			BossTimedSpawnTask.logLag("blacklist", System.currentTimeMillis() - now);

			now = System.currentTimeMillis();
			final boolean regionLimit = checkLimitsRegion(boss, loc);
			BossTimedSpawnTask.logLag("limit-region", System.currentTimeMillis() - now);

			// Region limits as set per-boss in their menu
			if (!blacklist && !regionLimit) {
				Debugger.debug("spawning", "World is blacklisted for " + boss.getName());
				return false;
			}
		}

		now = System.currentTimeMillis();
		final boolean checkWorld = checkWorld(boss, loc.getWorld());
		BossTimedSpawnTask.logLag("world", System.currentTimeMillis() - now);

		// World spawn chances, set in the menu
		if (!checkWorld) {
			Debugger.debug("spawning", "World limits applied for " + boss.getName());
			return false;
		}

		now = System.currentTimeMillis();
		final boolean checkBiome = checkBiome(boss, loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockZ()));
		BossTimedSpawnTask.logLag("biome", System.currentTimeMillis() - now);
		// Biome chance, set in the menu
		if (!checkBiome) {
			Debugger.debug("spawning", "Biome ");
			return false;
		}

		now = System.currentTimeMillis();
		final boolean checkConditions = checkConditions(boss, loc);
		BossTimedSpawnTask.logLag("conditions", System.currentTimeMillis() - now);

		if (!checkConditions) {
			Debugger.debug("spawning", "Conditions not met");

			return false;
		}

		return true;
	}

	private boolean checkLimitsWorld(final Boss boss, final Location location) {
		if (!Settings.Limits.Individual.ENABLED && !Settings.Limits.Global.ENABLED)
			return true;

		int sameFound = 0;
		int total = 0;

		{
			for (final SpawnedBoss spawned : BossPlugin.getBossManager().findBosses(location.getWorld())) {
				if (Settings.Limits.Individual.ENABLED)
					if (spawned.getBoss().equals(boss))
						sameFound++;

				total++;
			}

			if (total >= Settings.Limits.Global.WORLD) {
				Debugger.debug("spawning", "\tGlobal world limit (" + Settings.Limits.Global.WORLD + ") failed.");

				return false;
			}

			if (sameFound >= Settings.Limits.Individual.WORLD) {
				Debugger.debug("spawning", "\tIndividual world limit failed.");

				return false;
			}
		}

		return true;
	}

	private boolean checkLimitsChunk(final Boss boss, final Location location) {
		if (!Settings.Limits.Individual.ENABLED && !Settings.Limits.Global.ENABLED)
			return true;

		int sameFound = 0;
		int total = 0;

		{
			final int radius = Settings.Limits.RADIUS_BLOCKS;

			if (radius > 0)
				for (final SpawnedBoss spawnedBoss : BossPlugin.getBossManager().findBosses(location, radius)) {
					if (spawnedBoss.getBoss().equals(boss))
						sameFound++;

					total++;

				}

			if (total >= Settings.Limits.Global.RADIUS_LIMIT) {
				Debugger.debug("spawning", "\tGlobal chunk limit (" + Settings.Limits.Global.RADIUS_LIMIT + ") failed.");

				return false;
			}

			if (sameFound >= Settings.Limits.Individual.RADIUS_LIMIT) {
				Debugger.debug("spawning", "\tIndividual chunk limit (" + Settings.Limits.Individual.RADIUS_LIMIT + ") failed.");

				return false;
			}
		}

		return true;
	}

	private boolean checkLimitsRegion(final Boss boss, final Location location) {
		for (final BossRegionType type : BossRegionType.values()) {
			final List<String> regions = type.findRegions(location);

			if (regions != null)
				for (final String name : regions) {
					final BossRegionSettings rg = boss.getSpawning().getRegions().findRegion(type, name);

					if (rg != null && rg.getLimit() != -1 && type.findBosses(location.getWorld(), name).size() >= rg.getLimit()) {
						Debugger.debug("spawning", "\tBosses in region " + name + " over limit " + rg.getLimit() + ".");

						return false;
					}
				}
		}

		return true;
	}

	private boolean checkWorld(final Boss boss, final World world) {
		final AutoUpdateMap<String, Integer> worlds = boss.getSpawning().getWorlds();
		final Integer sanca = worlds.get(world.getName());

		final boolean spawn = sanca != null ? RandomUtil.chance(sanca) : false;

		if (!spawn)
			Debugger.debug("spawning", "\tNot spawning. World " + world.getName() + " has " + (sanca == null ? "no chance of spawning boss. No spawn." : sanca + "% of spawning boss. Should spawn? " + spawn));

		return spawn;
	}

	private boolean checkBiome(final Boss boss, final Biome biome) {
		final AutoUpdateMap<Biome, Integer> biomes = boss.getSpawning().getBiomes();

		if (biomes.isEmpty())
			return true;

		final Integer sanca = biomes.get(biome);
		final boolean spawn = sanca != null ? RandomUtil.chance(sanca) : false;

		if (!spawn)
			Debugger.debug("spawning", "\tNot spawning. Biome " + biome + " has " + (sanca == null ? "no chance of spawning boss. No spawn." : sanca + "% of spawning boss. Should spawn? " + spawn));

		return spawn;
	}

	private boolean checkRegions(final Boss boss, final Location loc) {
		if (!boss.getSpawning().getRegions().hasRegionSpawning())
			return true;

		final boolean isWithinAny = boss.getSpawning().getRegions().isWithinAny(loc);
		final boolean blacklist = boss.getSpawning().getRegions().isBlacklist();

		if (blacklist && isWithinAny) {
			Debugger.debug("spawning", "\tNot spawning. Blacklist is set & Boss is within a blacklisted region.");

			return false;
		} else if (!blacklist && !isWithinAny) {
			Debugger.debug("spawning", "\tNot spawning. Whitelist is set & Boss is not within any whitelisted region.", "Regions: " + boss.getSpawning().getRegions());
			return false;
		}
		return true;
	}

	private boolean checkConditions(final Boss boss, final Location loc) {
		final World w = loc.getWorld();
		final BossNativeConditions con = boss.getSpawning().getConditions();

		if (!checkRegions(boss, loc))
			return false;

		if (!isWithin(con.getHeight(), loc.getBlockY())) {
			Debugger.debug("spawning", "\tHeight condition failed.");

			return false;
		}

		if (!isWithin(con.getLight(), loc.getBlock().getLightLevel())) {
			Debugger.debug("spawning", "\tLight condition failed.");

			return false;
		}

		if (!isWithin(con.getTime(), w.getTime())) {
			Debugger.debug("spawning", "\tTime condition failed.");

			return false;
		}

		if (con.isRainRequired() && !w.hasStorm()) {
			Debugger.debug("spawning", "\tStorm condition failed.");

			return false;
		}

		if (con.isThunderRequired() && !w.isThundering()) {
			Debugger.debug("spawning", "\tThunder condition failed.");

			return false;
		}

		return true;
	}

	private boolean isWithin(final RangedValue value, final Number what) {
		return value.isWithin(what);
	}
}
