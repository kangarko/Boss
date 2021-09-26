package org.mineacademy.boss.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.impl.SimpleBoss;
import org.mineacademy.boss.model.region.RegionBoss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.util.BossNBTUtil;
import org.mineacademy.boss.util.BossTaggingUtil;
import org.mineacademy.boss.util.Constants;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

public final class BossManager {

	/**
	 * Holds all loaded bosses.
	 */
	private final StrictList<Boss> bosses = new StrictList<>();

	public void loadBosses() {
		bosses.clear();

		final File[] files = FileUtil.getFiles(Constants.Folder.BOSSES, "yml");
		Common.log("[Boss] Loading " + Common.plural(files.length, "boss"));

		for (final File f : files)
			try {
				final String name = f.getName().replace(".yml", "");
				final SimpleBoss boss = SimpleBoss.load(name);

				bosses.add(boss);

			} catch (final Throwable t) {
				Common.throwError(t, "Error loading boss from " + f.getName());
			}

		Common.log(" ");
	}

	// --------------------------------------------------------------------
	// Main
	// --------------------------------------------------------------------

	public Boss createBoss(EntityType type, String name) {
		Valid.checkNotNull(name, "Name cannot be null!");
		Valid.checkBoolean(findBoss(name) == null, "Boss " + name + " already exists!");

		final Boss boss = SimpleBoss.create(type, name);
		bosses.add(boss);

		return boss;
	}

	public void removeBoss(String name) {
		Valid.checkNotNull(name, "Name cannot be null!");

		final Boss boss = findBoss(name);
		Valid.checkNotNull(boss, "Boss " + name + " does not exist!");

		bosses.remove(boss);
		((SimpleBoss) boss).onDelete();
	}

	// --------------------------------------------------------------------
	// Finding bosses
	// --------------------------------------------------------------------

	public Boss findBoss(String name) {
		Valid.checkNotNull(name, "Name cannot be null!");

		for (final Boss boss : bosses)
			if (boss.getName().equalsIgnoreCase(name))
				return boss;

		return null;
	}

	public Boss findBoss(ItemStack egg) {
		if (!CompMaterial.isMonsterEgg(egg.getType()))
			return null;

		final String name = BossNBTUtil.readBossName(egg);
		return name != null ? findBoss(name) : null;
	}

	public Boss findBoss(Entity entity) {
		Valid.checkNotNull(entity, "Cannot find boss from null entity!");

		final String bossName = BossTaggingUtil.getTag(entity);

		return bossName != null ? findBoss(bossName) : null;
	}

	public SpawnedBoss findBoss(Location loc) {
		final StrictList<SpawnedBoss> found = findBosses(loc, 0.001);

		return !found.isEmpty() ? found.get(0) : null;
	}

	public StrictList<SpawnedBoss> findBosses(Location loc, double radius) {
		Valid.checkNotNull(loc.getWorld(), "Cannot find boss from location not having world!");

		final StrictList<SpawnedBoss> found = new StrictList<>();

		for (final Entity en : Remain.getNearbyEntities(loc, radius)) {
			final Boss boss = findBoss(en);

			if (boss != null)
				found.add(new SpawnedBoss(boss, (LivingEntity) en));
		}

		return found;
	}

	public Collection<SpawnedBoss> findBosses(World world) {
		final Set<SpawnedBoss> bosses = new HashSet<>();

		try {
			for (final LivingEntity entity : world.getLivingEntities()) {
				if (!(entity instanceof Player)) {
					final Boss boss = findBoss(entity);

					if (boss != null)
						bosses.add(new SpawnedBoss(boss, entity));
				}
			}
		} catch (final ConcurrentModificationException ex) {
			Common.log("Failed to find Bosses in world " + world.getName() + ". Got " + ex);
		}

		return bosses;
	}

	public Set<SpawnedBoss> findBosses(Chunk chunk) {
		final Set<SpawnedBoss> bosses = new HashSet<>();

		for (final Entity en : chunk.getEntities())
			if (en != null && en instanceof LivingEntity) {
				final Boss boss = findBoss(en);

				if (boss != null)
					bosses.add(new SpawnedBoss(boss, (LivingEntity) en));
			}

		return bosses;
	}

	public List<Boss> getBosses() {
		final List<Boss> bosses = new ArrayList<>();

		bosses.addAll(this.bosses.getSource());

		if (Settings.Setup.SORT_ALPHABETICALLY)
			Collections.sort(bosses, (f, s) -> f.getName().compareTo(s.getName()));

		return Collections.unmodifiableList(bosses);
	}

	public StrictList<String> getBossesAsList() {
		return Common.convertStrict(getBosses(), Boss::getName);
	}

	public Boss getRandomBoss() {
		return bosses.get(RandomUtil.nextInt(bosses.size()));
	}

	public static StrictList<EntityType> getValidTypes() {
		final StrictList<EntityType> types = new StrictList<>();

		for (final EntityType entity : EntityType.values())
			if (entity.getEntityClass() != null && Creature.class.isAssignableFrom(entity.getEntityClass()))
				types.add(entity);

		for (final EntityType add : Arrays.asList(EntityType.ENDER_DRAGON, EntityType.GHAST, EntityType.WOLF, EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.SQUID, EntityType.BAT, EntityType.WITHER))
			if (!types.contains(add))
				types.add(add);

		if (MinecraftVersion.atLeast(V.v1_13))
			for (final EntityType entity : EntityType.values())
				if (entity.getEntityClass() != null && Flying.class.isAssignableFrom(entity.getEntityClass()))
					if (!types.contains(entity))
						types.add(entity);

		return types;
	}

	public static String getValidTypesFormatted() {
		final TreeSet<EntityType> list = new TreeSet<>((f, s) -> f.toString().compareTo(s.toString()));

		list.addAll(getValidTypes().getSource());

		return Common.join(list, "&7, &7", new Common.Stringer<EntityType>() {

			private boolean color = false;

			@Override
			public String toString(EntityType type) {
				return ((color = !color) ? "&7" : "&f") + type.toString().toLowerCase();
			}
		});
	}

	public static long getNextSpawnFor(Boss boss, RegionBoss region) {
		return 0;
	}

}