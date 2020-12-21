package org.mineacademy.boss.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.mineacademy.boss.model.specific.BooleanSpecificKey;
import org.mineacademy.boss.model.specific.IntegerSpecificKey;
import org.mineacademy.boss.model.specific.SpecificKey;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.ItemCreator.ItemCreatorBuilder;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBTEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The specific settings.
 * <p>
 * Some of them are only functional/available for certain mobs.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum BossSpecificSetting {

	// ---------------------------------------------------------------------------------
	// Generic settings
	// ---------------------------------------------------------------------------------

	/**
	 * Display Boss name above his head?
	 * <p>
	 * value: boolean mobs: all
	 */
	DISPLAY_NAME("Display_Name", new BooleanSpecificKey() {

		@Override
		public void onSpawn(LivingEntity en) {
			en.setCustomNameVisible(get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.NAME_TAG,
					"Display Name",
					"",
					get() ? "&aName displayed." : "&7Name hidden.",
					"",
					"Display Boss name",
					"above his head?.");
		}

		@Override
		public Boolean getDefault() {
			return true;
		}
	}),

	/**
	 * Has the mob a glowing box around? (Minecraft 1.9+)
	 * <p>
	 * value: boolean mobs: all
	 */
	GLOWING("Glowing", new BooleanSpecificKey() {

		@Override
		public boolean isEnabled() {
			return CompProperty.GLOWING.isAvailable(Entity.class);
		}

		@Override
		public void onSpawn(LivingEntity en) {
			CompProperty.GLOWING.apply(en, get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.BEACON,
					"Glowing",
					"",
					get() ? "&aBoss is glowing." : "&7Boss isn't glowing (default).",
					"",
					"Glowing entities have a",
					"white box around them.");
		}
	}),

	/**
	 * Is this mob not taking damage?
	 * <p>
	 * value: boolean mobs: all
	 */
	INVULNERABLE("Invulnerable", new BooleanSpecificKey() {

		@Override
		public boolean isEnabled() {
			return CompProperty.INVULNERABLE.isAvailable(Entity.class);
		}

		@Override
		public void onSpawn(LivingEntity en) {
			CompProperty.INVULNERABLE.apply(en, get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.BLAZE_POWDER,
					"Invulnerable",
					"",
					get() ? "&aBoss is invulnerable." : "&7Boss is vulnerable (default).",
					"",
					"Invulnerable Bosses",
					"can only be damaged by",
					"players in creative mode.");
		}
	}),

	/**
	 * Is this mob lacking intelligence/navigation/pathfinding?
	 * <p>
	 * value: boolean mobs: all
	 */
	NO_AI("No_AI", new BooleanSpecificKey() {

		@Override
		public boolean isEnabled() {
			return CompProperty.AI.isAvailable(Entity.class) || MinecraftVersion.atLeast(V.v1_8);
		}

		@Override
		public void onSpawn(LivingEntity en) {
			if (CompProperty.AI.isAvailable(Entity.class))
				CompProperty.AI.apply(en, !get());

			else
				new NBTEntity(en).setInteger("NoAI", get() ? 1 : 0);
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.REDSTONE,
					"No AI",
					"",
					get() ? "&aBoss lacks AI." : "&7Boss has AI (default).",
					"",
					"Bosses lacking AI",
					"do not move and do",
					"not attack players.");
		}

	}),

	/**
	 * Is this mob floating in the air?
	 * <p>
	 * value: boolean mobs: all
	 */
	GRAVITY("Gravity", new BooleanSpecificKey() {

		@Override
		public boolean isEnabled() {
			return CompProperty.GRAVITY.isAvailable(Entity.class);
		}

		@Override
		public void onSpawn(LivingEntity en) {
			CompProperty.GRAVITY.apply(en, get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.FEATHER,
					"Gravity",
					"",
					get() ? "&7Boss has gravity (default)." : "&aBoss lacks gravity.",
					"",
					"Bosses lacking gravity do",
					"not fall to the ground",
					"nor follow players.");
		}

		@Override
		public Boolean getDefault() {
			return true;
		}
	}),

	/**
	 * Can this mob pickup items?
	 * <p>
	 * value: boolean mobs: all
	 */
	PICKUP_ITEMS("Can_Pickup_Items", new BooleanSpecificKey() {

		@Override
		public void onSpawn(LivingEntity en) {
			en.setCanPickupItems(get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.CHEST,
					"Can Pickup Items",
					"",
					get() ? "Boss can pickup items (default)." : "&aBoss cannot pickup items.",
					"",
					"Set if the Boss can",
					"pick-up items from",
					"the ground.");
		}

		@Override
		public Boolean getDefault() {
			return true;
		}

	}),

	/**
	 * Is this mob not making walk/.. noises?
	 * <p>
	 * value: boolean mobs: all
	 */
	SILENT("Silent", new BooleanSpecificKey() {

		@Override
		public boolean isEnabled() {
			return CompProperty.SILENT.isAvailable(Entity.class);
		}

		@Override
		public void onSpawn(LivingEntity en) {
			CompProperty.SILENT.apply(en, get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.NOTE_BLOCK,
					"Silent",
					"",
					get() ? "&aBoss is silent." : "&7Boss isn't silent (default).",
					"",
					"Silent Bosses do not make",
					"any sounds when hit or die.");
		}

	}),

	/**
	 * Toggle whether or not other mobs can target this Boss.
	 * <p>
	 * value: boolean mobs: all
	 */
	TARGETABLE("Targetable", new BooleanSpecificKey() {

		@Override
		public void onSpawn(LivingEntity en) {
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.IRON_SWORD,
					"Targettable",
					"",
					get() ? "&7Boss can be attacked by monsters." : "&cMonsters ignore this Boss.",
					"",
					"Set whether or not you want",
					"other monsters to be able to",
					"attack and kill your Boss.");
		}

		@Override
		public Boolean getDefault() {
			return true;
		}
	}),

	/**
	 * Toggle whether or not other mobs can target this Boss.
	 * <p>
	 * value: boolean mobs: all
	 */
	DESPAWN_WHEN_FAR_AWAY("Despawn_When_Far_Away", new BooleanSpecificKey() {

		@Override
		public void onSpawn(LivingEntity en) {
			en.setRemoveWhenFarAway(get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					get() ? CompMaterial.LAVA_BUCKET : CompMaterial.BUCKET,
					"Despawn when far away?",
					"",
					get() ? "&2Boss will be removed when far from players." : "&7Boss will never despawn.",
					"",
					"Set whether or not you want",
					"your Boss to despawn when",
					"far away from all players.");
		}

		@Override
		public Boolean getDefault() {
			return false;
		}
	}),

	// ---------------------------------------------------------------------------------
	// Specific settings
	// ---------------------------------------------------------------------------------

	/**
	 * If the Boss is a {@link Creature}, should it target players nearby
	 * automatically - for example aggressive spiders during the day?
	 */
	AUTO_TARGET("Auto_Target", new BooleanSpecificKey() {

		@Override
		protected Object getApplicable() {
			return Creature.class;
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(Remain.getMaterial("SHIELD", CompMaterial.IRON_SWORD),
					"&rAuto Target",
					"",
					get() ? "&aBoss automatically finds new targets," : "&7Boss only attacks players as",
					get() ? "&aaccording to your settings.yml." : "&7per vanilla Minecraft conditions.",
					"",
					"Should Boss automatically finds",
					"new targets around him? Please see",
					"Fight.Auto_Retarget in settings.yml",
					"for more information.");
		}

		@Override
		public Boolean getDefault() {
			return false;
		}
	}),

	/**
	 * Is this mob burning under sunlight?
	 * <p>
	 * value: boolean mobs: hostile
	 */
	HOSTILE_SUN_BURN("Burns_Under_Sunlight", new BooleanSpecificKey() {

		@Override
		protected Object getApplicable() {
			final List<Object> list = new ArrayList<>(Arrays.asList(Zombie.class, Skeleton.class));

			if (MinecraftVersion.atLeast(V.v1_13))
				list.add(Phantom.class);

			return list;
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.FIRE_CHARGE,
					"Burns Under Sunlight",
					"",
					get() ? "Burns under sun-light (default)." : "&aBoss does not burn.",
					"",
					"Set if the boss burns",
					"under the day-light.");
		}

		@Override
		public Boolean getDefault() {
			return false;
		}
	}),

	/**
	 * Is this mob a baby version of the main mob?
	 * <p>
	 * value: boolean mobs: ageable
	 */
	AGEABLE_BABY("Baby", new BooleanSpecificKey() {

		@Override
		protected Object getApplicable() {
			return Arrays.asList(Zombie.class, Ageable.class);
		}

		@Override
		public void onSpawn(LivingEntity en) {

			if (en instanceof Zombie)
				((Zombie) en).setBaby(get());

			else if (en instanceof Ageable) {
				final Ageable age = (Ageable) en;

				if (get())
					age.setBaby();
				else
					age.setAdult();

				age.setAgeLock(true);
			}
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.ZOMBIE_HEAD,
					"Baby",
					"",
					get() ? "&aBoss is a baby." : "Boss isn't a baby (default).",
					"",
					"Baby Bosses are smaller",
					"and run faster.");
		}
	}),

	/**
	 * Is this Creeper powered?
	 * <p>
	 * value: boolean mobs: creeper
	 */
	CREEPER_POWERED("Powered", new BooleanSpecificKey() {

		@Override
		protected Object getApplicable() {
			return Creeper.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
			((Creeper) en).setPowered(get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.NETHER_STAR,
					"Powered",
					"",
					get() ? "&aBoss is powered." : "&7Boss is not powered (default).",
					"",
					"Powered Creepers have",
					"a special effect around,",
					"and explode stronger.");
		}
	}),

	/**
	 * Will this slime spawn babies upon death?
	 * <p>
	 * value: boolean mobs: slime
	 */
	SLIME_BABIES("Spawn Babies", new BooleanSpecificKey() {

		@Override
		protected Object getApplicable() {
			return Slime.class;
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(CompMaterial.SLIME_SPAWN_EGG,
					"&rSpawn Babies?",
					"",
					get() ? "&aSlime splits into babies." : "&7Slime does not have babies.",
					"",
					"Should this slime spawn",
					"its babies (smaller slimes)",
					"upon death?");
		}

		@Override
		public Boolean getDefault() {
			return true;
		}
	}),

	/**
	 * The size of the slime.
	 * <p>
	 * value: integer mobs: slime
	 */
	SLIME_SIZE("Size", new IntegerSpecificKey() {

		@Override
		protected Object getApplicable() {
			return Slime.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
			((Slime) en).setSize(get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			final ItemCreatorBuilder is = ItemCreator.of(
					CompMaterial.SLIME_BALL,
					"Slime Size",
					"",
					"Adjust the slime size.");

			if (get() == 50)
				is.lores(Arrays.asList(
						"",
						"&dBeware: Reached the",
						"&dmaximum slime size."));
			else if (get() > 24)
				is.lores(Arrays.asList(
						"",
						"&4Warning: Extremely oversized",
						"&4slimes are bugged and have",
						"&4inproper hit-box."));
			else if (get() > 15)
				is.lores(Arrays.asList(
						"",
						"&cWarning: Slime is oversized.",
						"&cYour server may not handle",
						"&cit correctly."));
			else if (get() > 10)
				is.lores(Arrays.asList(
						"",
						"&cNB: &7Too big slimes do not",
						"&7behave properly and may",
						"&7despawn randomly."));
			return is;
		}
	}),

	/**
	 * The size of the phantom.
	 * <p>
	 * value: integer mobs: phantom
	 */
	PHANTOM_SIZE("Size", new IntegerSpecificKey() {

		@Override
		public boolean isEnabled() {
			try {
				Class.forName("org.bukkit.entity.Phantom");

				return true;

			} catch (final ReflectiveOperationException ex) {
				return false;
			}
		}

		@Override
		protected Object getApplicable() {
			return Phantom.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
			((Phantom) en).setSize(get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			final ItemCreatorBuilder is = ItemCreator.of(
					CompMaterial.ENDER_EYE,
					"Phantom Size",
					"",
					"Adjust the Phantom size.");
			return is;
		}
	}),

	/**
	 * Makes the rabbit turn evil.
	 * <p>
	 * value: boolean mobs: rabbit
	 */
	RABBIT_KILLER_BUNNY("Rabbit_Killer_Bunny", new BooleanSpecificKey() {

		@Override
		public boolean isEnabled() {
			try {
				Class.forName("org.bukkit.entity.Rabbit");

				return true;

			} catch (final ReflectiveOperationException ex) {
				return false;
			}
		}

		@Override
		protected Object getApplicable() {
			return Rabbit.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
			if (get())
				((Rabbit) en).setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.RABBIT_FOOT,
					"Killer Rabbit",
					"",
					get() ? "&cThis is a Killer Rabbit!" : "&7The rabbit is regular.",
					"",
					"Killer rabbits are scary",
					"and attack players and",
					"wolves with mighty power.");
		}

		@Override
		public Boolean getDefault() {
			return false;
		}
	}),

	/**
	 * Prevents the enderman from teleport.
	 * <p>
	 * value: boolean mobs: endermen
	 */
	ENDERMAN_TELEPORT("Enderman_Teleport", new BooleanSpecificKey() {

		@Override
		protected Object getApplicable() {
			return Enderman.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.ENDER_PEARL,
					"Enderman Teleport",
					"",
					get() ? "&fEnderman teleports normally." : "&cEnderman does not teleport.",
					"",
					"If false, it completely prevents",
					"the Enderman from teleporting.");
		}

		@Override
		public Boolean getDefault() {
			return true;
		}
	}),

	ENDERDRAGON_GRIEF("Enderdragon_Grief", new BooleanSpecificKey() {

		@Override
		protected Object getApplicable() {
			return EnderDragon.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.ENDER_EYE,
					"Enderdragon Grief",
					"",
					get() ? "&cEnderdragon destroys blocks." : "&aEnderdragon does not destroy blocks.",
					"",
					"If false, this dragon will",
					"not destroy any blocks",
					"- hopefully.");
		}

		@Override
		public Boolean getDefault() {
			return true;
		}
	}),

	/**
	 * Prevents the Iron Golem from attacking monsters.
	 * <p>
	 * value: boolean mobs: Iron Golem
	 */
	GOLEM_AGGRESSIVE("Golem_Attack_Monsters", new BooleanSpecificKey() {

		@Override
		protected Object getApplicable() {
			return IronGolem.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.ROSE_RED,
					"Iron Golem Attack",
					"",
					get() ? "&7The Golem attacks monsters." : "&cGolem ignores monsters.",
					"",
					"If false, your Iron Golem Boss",
					"will no longer attack monsters.");
		}

		@Override
		public Boolean getDefault() {
			return true;
		}
	}),

	/**
	 * Is this zombie boss a villager?
	 */
	@Deprecated // It is now possible to create Villager Zombies directly in later MC versions
	ZOMBIE_VILLAGER("Zombie_Villager", new BooleanSpecificKey() {

		@Override
		public boolean isEnabled() {
			try {
				EntityType.valueOf("ZOMBIE_VILLAGER");

				return false;

			} catch (final Throwable t) {
				return true;
			}
		}

		@Override
		protected Object getApplicable() {
			return Zombie.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
			if (en instanceof Zombie)
				((Zombie) en).setVillager(get());
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.ROTTEN_FLESH,
					"Zombie Villager",
					"",
					get() ? "&cBoss is a Zombie Villager." : "&7Boss is not a Zombie Villager.",
					"",
					"Should this Boss be a",
					"Zombie Villager?");
		}

		@Override
		public Boolean getDefault() {
			return false;
		}
	}),

	/**
	 * Is the skeleton wither?
	 * <p>
	 * value: boolean mobs: skeleton
	 */
	@Deprecated // It is now possible to create wither skeleton directly in later MC versions
	SKELETON_WITHER("Wither", new BooleanSpecificKey() {

		@Override
		public boolean isEnabled() {
			try {
				EntityType.valueOf("WITHER_SKELETON");

				return false;

			} catch (final Throwable t) {
				return true;
			}
		}

		@Override
		protected Object getApplicable() {
			return Skeleton.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
			try {
				if (en instanceof Skeleton && !isStray((Skeleton) en))
					((Skeleton) en).setSkeletonType(get() ? SkeletonType.WITHER : SkeletonType.NORMAL);
			} catch (final UnsupportedOperationException | NoSuchFieldError ex) {
			}
		}

		private boolean isStray(Skeleton s) {
			try {
				return s.getSkeletonType() == SkeletonType.valueOf("STRAY");

			} catch (final Throwable t) {
				return false;
			}
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.BONE,
					"Wither Skeleton",
					"",
					get() ? "&cBoss is a Wither Skeleton." : "&7Boss is not a Wither Skeleton.",
					"",
					"Should this Boss be a",
					"Wither Skeleton?");
		}

		@Override
		public Boolean getDefault() {
			return false;
		}
	}),

	/**
	 * Is the skeleton stray?
	 * <p>
	 * value: boolean mobs: skeleton
	 */
	@Deprecated // It is now possible to create stray skeleton directly in later MC versions
	SKELETON_STRAY("Stray", new BooleanSpecificKey() {

		@Override
		public boolean isEnabled() {
			try {
				EntityType.valueOf("STRAY");

				return false;

			} catch (final Throwable t) {
				return MinecraftVersion.newerThan(V.v1_8);
			}
		}

		@Override
		protected Object getApplicable() {
			return Skeleton.class;
		}

		@Override
		public void onSpawn(LivingEntity en) {
			try {
				if (((Skeleton) en).getSkeletonType() != SkeletonType.WITHER)
					((Skeleton) en).setSkeletonType(get() ? SkeletonType.valueOf("STRAY") : SkeletonType.NORMAL);
			} catch (final UnsupportedOperationException | NoSuchFieldError | IllegalArgumentException ex) {
			}
		}

		@Override
		protected ItemCreatorBuilder getIcon() {
			return ItemCreator.of(
					CompMaterial.COAL,
					"Stray Skeleton",
					"",
					get() ? "&cBoss is a Stray Skeleton." : "&7Boss is not a Stray Skeleton.",
					"",
					"Should this Boss be a",
					"Stray Skeleton?");
		}

		@Override
		public Boolean getDefault() {
			return false;
		}
	});

	@Getter
	private final String configKey;

	private final SpecificKey<?> key;

	public SpecificKey<?> getFor(Boss boss) {
		key.setData(this, boss);

		return key;
	}
}
