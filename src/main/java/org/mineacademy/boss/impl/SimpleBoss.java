package org.mineacademy.boss.impl;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossAttributes;
import org.mineacademy.boss.api.BossDrop;
import org.mineacademy.boss.api.BossEggItem;
import org.mineacademy.boss.api.BossEquipment;
import org.mineacademy.boss.api.BossEquipmentSlot;
import org.mineacademy.boss.api.BossPotion;
import org.mineacademy.boss.api.BossSettings;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.boss.api.BossSpawnReason;
import org.mineacademy.boss.api.BossSpawning;
import org.mineacademy.boss.api.BossSpecificSetting;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.api.event.BossPostSpawnEvent;
import org.mineacademy.boss.api.event.BossPreSpawnEvent;
import org.mineacademy.boss.model.BossAttribute;
import org.mineacademy.boss.model.specific.SpecificKey;
import org.mineacademy.boss.util.AutoUpdateList;
import org.mineacademy.boss.util.AutoUpdateMap;
import org.mineacademy.boss.util.BossNBTUtil;
import org.mineacademy.boss.util.BossTaggingUtil;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.remain.CompAttribute;
import org.mineacademy.fo.remain.CompMonsterEgg;
import org.mineacademy.fo.remain.NmsEntity;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;

public final class SimpleBoss implements Boss {

	@Getter
	private final String name;
	private final SimpleSettings settings;

	@Getter
	private final SimpleDropsManager dropsManager;

	private SimpleBoss(final EntityType type, final String name) {
		this.name = name;
		this.settings = new SimpleSettings(type, name, this);
		this.dropsManager = new SimpleDropsManager(this);
	}

	// ---------------------------------------------------------------------------
	// Loading
	// ---------------------------------------------------------------------------

	public static SimpleBoss create(final EntityType type, final String name) {
		final SimpleBoss boss = new SimpleBoss(type, name);
		((SimpleSettings) boss.getSettings()).loadConfig();

		return boss;
	}

	public static SimpleBoss load(final String name) {
		final SimpleBoss boss = new SimpleBoss(null, name);
		((SimpleSettings) boss.getSettings()).loadConfig();

		return boss;
	}

	// ---------------------------------------------------------------------------
	// Spawning
	// ---------------------------------------------------------------------------

	@Deprecated
	public LivingEntity spawnDummy(final boolean transform) {
		final World w = Bukkit.getWorlds().get(0);

		final NmsEntity nms = new NmsEntity(new Location(w, 0, 0, 0), getType().getEntityClass());

		if (transform)
			transformToBoss((LivingEntity) nms.getBukkitEntity());

		return (LivingEntity) nms.getBukkitEntity();
	}

	@Deprecated
	public void reapplyProperties(final LivingEntity en) {
		//if (en.isInsideVehicle())
		//	en.getVehicle().remove();

		transformToBoss(en);
	}

	@Override
	public SpawnedBoss spawn(final Location loc, final BossSpawnReason reason) {
		LagCatcher.start("Spawn " + getName());
		try {
			final NmsEntity nms = new NmsEntity(loc, getType().getEntityClass());
			LivingEntity en = (LivingEntity) nms.getBukkitEntity();

			if (!Common.callEvent(new BossPreSpawnEvent(this, en, reason))) {
				en.remove();

				return null;
			}

			BossTaggingUtil.setTag(en, this);
			en = nms.addEntity(SpawnReason.CUSTOM);

			if (!en.isDead() && en.isValid()) {
				try {
					final LivingEntity enF = en;

					transformToBoss(enF);

					Common.callEvent(new BossPostSpawnEvent(this, en, reason));

				} catch (final Throwable t) {
					Common.error(t, "Error making a Boss!", "Entity: " + en, "Boss: " + getName(), "%error");
				}
			}

			return new SpawnedBoss(this, en);

		} finally {
			LagCatcher.end("Spawn " + getName());
		}
	}

	private void transformToBoss(final LivingEntity en) {
		try {
			if (getType() == EntityType.ZOMBIE && ((Zombie) en).isVillager())
				((Zombie) en).setVillager(false);
		} catch (final Throwable t) {
			// Some MC versions throw casting error
		}

		{ // Name
			final String n = getSettings().getCustomName();

			if (n != null)
				if ("none".equalsIgnoreCase(n))
					Common.runLater(1, () -> en.setCustomNameVisible(false));
				else {
					en.setCustomNameVisible(true);
					en.setCustomName("default".equals(n) ? getName() : Common.colorize(n));
				}
		}

		{ // Potions
			for (final PotionEffect old : en.getActivePotionEffects())
				en.removePotionEffect(old.getType());

			for (final BossPotion p : getSettings().getPotions())
				en.addPotionEffect(p.getType().createEffect(Integer.MAX_VALUE, p.getLevel() - 1), true);
		}

		{ // Equipment
			final EntityEquipment eq = en.getEquipment();

			if (settings.getEquipment().allowRandom())
				for (final BossEquipmentSlot slot : BossEquipmentSlot.values())
					setEquipment(eq, slot);
			else
				Common.runLater(() -> {
					for (final BossEquipmentSlot slot : BossEquipmentSlot.values())
						setEquipment(eq, slot);
				});
		}

		{ // Attributes
			for (final BossAttribute attr : getAttributes().getConfigured())
				if (attr != null)
					CompAttribute.valueOf(attr.toString()).set(en, getAttributes().get(attr));
		}

		{ // Specific settings
			for (final BossSpecificSetting setting : getSettings().getSpecificSettings()) {
				final SpecificKey<?> key = setting.getFor(this);

				if (key.isEnabled())
					key.onSpawn(en);
			}
		}

		{ // Health
			if (getSettings().getHealth() > Remain.getMaxHealth())
				getSettings().setHealth((int) Remain.getMaxHealth());

			en.setMaxHealth(getSettings().getHealth());
			en.setHealth(getSettings().getHealth());
		}

		// Riding
		if (!en.isInsideVehicle() && settings.getRidingVanilla() != null) {

			final Entity rided = en.getWorld().spawnEntity(en.getLocation(), settings.getRidingVanilla());

			if (rided instanceof Tameable)
				((Tameable) rided).setTamed(true);

			if (rided instanceof Zombie)
				((Zombie) rided).setBaby(false);

			if (rided instanceof Ageable) {
				((Ageable) rided).setAdult();
				((Ageable) rided).setAgeLock(true);
			}

			// prevent mount despawn
			if (rided instanceof LivingEntity)
				((LivingEntity) rided).setRemoveWhenFarAway(false);

			rided.setPassenger(en);
		}

		if (!en.isInsideVehicle() && settings.getRidingBoss() != null && !settings.getRidingBoss().isEmpty()) {
			final Boss ridedBoss = BossPlugin.getBossManager().findBoss(settings.getRidingBoss());

			if (ridedBoss == null) {
				Common.log("&c" + getName() + " should ride a Boss '" + settings.getRidingBoss() + "' which is not installed, removing!");

				settings.setRidingBoss(null);
			} else {

				final SpawnedBoss ridedSpawnedBoss = ridedBoss.spawn(en.getLocation(), BossSpawnReason.RIDING);

				if (ridedSpawnedBoss == null)
					Common.log("&c" + getName() + " should ride a Boss named '" + settings.getRidingBoss() + "' which could not be spawned!");

				else {
					final LivingEntity ridden = ridedSpawnedBoss.getEntity();

					if (ridden instanceof Tameable)
						((Tameable) ridden).setTamed(true);

					if (ridden instanceof Zombie)
						((Zombie) ridden).setBaby(false);

					if (ridden instanceof Ageable) {
						((Ageable) ridden).setAdult();
						((Ageable) ridden).setAgeLock(true);
					}

					ridden.setPassenger(en);
					// prevent mount despawn
					ridden.setRemoveWhenFarAway(false);
				}
			}
		}

		if (en instanceof EnderDragon && MinecraftVersion.atLeast(V.v1_9))
			((EnderDragon) en).setPhase(EnderDragon.Phase.SEARCH_FOR_BREATH_ATTACK_TARGET);
	}

	private void setEquipment(final EntityEquipment eq, final BossEquipmentSlot slot) {
		if (MinecraftVersion.olderThan(V.v1_9) && slot == BossEquipmentSlot.OFF_HAND)
			return;

		final BossEquipment bossEquip = getEquipment();
		BossDrop drop = bossEquip != null ? bossEquip.get(slot) : null;

		if (drop == null)
			drop = new BossDrop((getType() == EntityType.SKELETON || getType().toString().equals("STRAY")) && slot == BossEquipmentSlot.HAND ? new ItemStack(Material.BOW) : new ItemStack(Material.AIR), 0);

		try {
			final Class<?> cl = EntityEquipment.class;
			final String method = WordUtils.capitalize(slot.getMethodName());

			cl.getMethod("set" + method, ItemStack.class).invoke(eq, drop.getItem());
			cl.getMethod("set" + method + "DropChance", float.class).invoke(eq, drop.getDropChance());

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex);
		}
	}

	@Override
	public ItemStack asEgg() {
		final BossEggItem egg = getSettings().getEggItem();
		final ItemStack pre = CompMonsterEgg.makeEgg(getType());

		// Enforce appropriate egg types when the default sheep spawn egg is given, assume it is intended
		if (!egg.getMaterial().toString().endsWith("_SPAWN_EGG"))
			pre.setType(egg.getMaterial().getMaterial());

		//final CompMaterial mat = MinecraftVersion.atLeast(V.v1_13) ? CompMaterial.fromMaterial(pre.getType()) : egg.getMaterial();

		final ItemStack item = ItemCreator
				.of(/*mat*/pre)
				.name(egg.getName().replace("{boss}", getName()))
				.lores(Replacer.replaceArray(egg.getLore(), "boss", getName()))
				.glow(egg.isGlowing())
				.hideTags(true)
				.build()
				.make();

		return BossNBTUtil.writeBossName(item, this);

	}

	// ---------------------------------------------------------------------------
	// Settings-related
	// ---------------------------------------------------------------------------

	@Override
	public String getSpawnPermission() {
		return ChatUtil.replaceDiacritic(name.toLowerCase().replace(" ", ""));
	}

	@Override
	public EntityType getType() {
		return settings.getType();
	}

	@Override
	public BossEquipment getEquipment() {
		return settings.getEquipment();
	}

	@Override
	public AutoUpdateMap<Integer, BossDrop> getDrops() {
		return settings.getDrops();
	}

	@Override
	public BossAttributes getAttributes() {
		return settings.getAttributes();
	}

	@Override
	public AutoUpdateList<BossSkill> getSkills() {
		return settings.getSkills();
	}

	@Override
	public AutoUpdateMap<String, Integer> getReinforcementsBoss() {
		return settings.getReinforcementsBoss();
	}

	@Override
	public AutoUpdateMap<String, Integer> getReinforcementsVanilla() {
		return settings.getReinforcementsVanilla();
	}

	@Override
	public AutoUpdateMap<String, Double> getSpawnCommands() {
		return settings.getSpawnCommands();
	}

	@Override
	public AutoUpdateMap<String, Double> getDeathCommands() {
		return settings.getDeathCommands();
	}

	@Override
	public AutoUpdateMap<String, Double> getDeathByPlayerCommands() {
		return settings.getDeathByPlayerCommands();
	}

	@Override
	public BossSettings getSettings() {
		return settings;
	}

	@Override
	public BossSpawning getSpawning() {
		return settings.getSpawning();
	}

	@Override
	public boolean hasLightningOnDeath() {
		return settings.isDeathLightning();
	}

	@Override
	public void setLightningOnDeath(final boolean flag) {
		settings.setDeathLightning(flag);
	}

	// ---------------------------------------------------------------------------
	// Utility
	// ---------------------------------------------------------------------------

	public void onDelete() {
		settings.delete();
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Boss ? ((Boss) obj).getName().equals(name) : false;
	}

	@Override
	public String toString() {
		return "Boss{" + getName() + " - " + getType() + "}";
	}

}