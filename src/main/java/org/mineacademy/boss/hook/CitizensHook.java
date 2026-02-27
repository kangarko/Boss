package org.mineacademy.boss.hook;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossCitizensSettings;
import org.mineacademy.boss.model.BossSpawnReason;
import org.mineacademy.boss.model.BossSpawnResult;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.ExpiringMap;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.CompEquipmentSlot;
import org.mineacademy.fo.remain.Remain;

import com.google.gson.JsonObject;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.BehaviorController;
import net.citizensnpcs.api.ai.EntityTarget;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.goals.WanderGoal;
import net.citizensnpcs.api.npc.MetadataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.MountTrait;
import net.citizensnpcs.trait.SkinTrait;

/**
 * Connector to Citizens for integration.
 */
public final class CitizensHook {

	/*
	 * Skin cache
	 */
	private static final Map<String, JsonObject> cache = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();

	/**
	 * Register our custom traits
	 */
	public static void registerTraits() {
		if (MinecraftVersion.atLeast(V.v1_8))
			try {
				net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(MaxHealthTrait.class));
				net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(ScaleTrait.class));

			} catch (final Throwable t) {
				t.printStackTrace();
			}
	}

	/**
	 * Spawns the given Boss at the location with help of Citizens
	 *
	 * @param boss
	 * @param location
	 * @return
	 */
	public static Entity spawn(Boss boss, Location location) {
		final String alias = boss.getAlias();
		final NPCRegistry registry = findRegistry();

		if (registry == null)
			return null;

		final NPC npc = registry.createNPC(boss.getType(), alias.length() > 16 ? alias.substring(0, 16) : alias);

		// Spawn
		if (!npc.spawn(location)) {
			npc.destroy();

			return null;
		}

		// Apply attributes
		update(boss, npc);

		// Set initial health to max and save
		final LivingEntity living = (LivingEntity) npc.getEntity();
		living.setMaxHealth(boss.getMaxHealth());
		living.setHealth(boss.getMaxHealth());

		final MaxHealthTrait trait = npc.getOrAddTrait(MaxHealthTrait.class);

		// Mount
		if (!boss.getRidingEntitiesBoss().isEmpty()) {
			UUID mountId = null;

			for (final String mountBossName : boss.getRidingEntitiesBoss()) {
				final Boss mountBoss = Boss.findBoss(mountBossName);

				if (mountBoss != null && mountBoss.getCitizensSettings().isEnabled()) {
					final Tuple<BossSpawnResult, SpawnedBoss> tuple = mountBoss.spawn(living.getLocation(), BossSpawnReason.RIDING);

					if (tuple.getKey() == BossSpawnResult.SUCCESS) {
						mountId = tuple.getValue().getEntity().getUniqueId();

						break;
					}
				}
			}

			if (mountId != null) {
				final MountTrait mount = npc.getOrAddTrait(MountTrait.class);

				mount.setMountedOn(mountId);
			}
		}

		if (trait != null)
			trait.health = living.getHealth();

		return npc.getEntity();
	}

	/**
	 * Updates Boss behavior for the spawned Boss entity
	 *
	 * @param boss
	 * @param entity
	 */
	public static void update(Boss boss, Entity entity) {
		final NPC npc = findRegistry().getNPC(entity);

		if (npc == null)
			return;

		if (!npc.isSpawned())
			npc.destroy();

		else
			update(boss, npc);
	}

	/*
	 * Updates Boss behavior for the spawned Boss entity
	 */
	private static void update(Boss boss, NPC npc) {
		final LivingEntity living = (LivingEntity) npc.getEntity();
		Valid.checkNotNull(living, "Unable to retrieve living entity from NPC " + npc);

		final BossCitizensSettings citizens = boss.getCitizensSettings();
		final MetadataStore data = npc.data();

		npc.setProtected(false);

		npc.getOrAddTrait(MaxHealthTrait.class);

		final Equipment equipment = npc.getOrAddTrait(Equipment.class);

		if (living.getType() == CompEntityType.ENDERMAN) {
			final ItemStack item = boss.getEquipmentItem(CompEquipmentSlot.HAND);

			try {
				equipment.set(Equipment.EquipmentSlot.HAND, item);
			} catch (final NullPointerException ex) {
				// Citizens bug
			}

		} else
			for (final CompEquipmentSlot slot : CompEquipmentSlot.values()) {
				final ItemStack item = boss.getEquipmentItem(slot);

				try {
					equipment.set(Equipment.EquipmentSlot.valueOf(slot.getBukkitName()), item);
				} catch (final NullPointerException ex) {
					// Citizens bug
				}
			}

		data.setPersistent("BossName", boss.getName());

		if (citizens.getDeathSound() != null)
			data.setPersistent(NPC.Metadata.DEATH_SOUND, citizens.getDeathSound());

		if (citizens.getHurtSound() != null)
			data.setPersistent(NPC.Metadata.HURT_SOUND, citizens.getHurtSound());

		if (citizens.getAmbientSound() != null)
			data.setPersistent(NPC.Metadata.AMBIENT_SOUND, citizens.getAmbientSound());

		data.setPersistent(NPC.Metadata.DROPS_ITEMS, true);
		data.setPersistent(NPC.Metadata.COLLIDABLE, true);
		data.setPersistent(NPC.Metadata.DAMAGE_OTHERS, true);

		final BehaviorController behaviorController = npc.getDefaultBehaviorController();

		behaviorController.clear();

		if (citizens.isTargetGoalEnabled())
			behaviorController.addBehavior(BossTargetNearbyEntityGoal.builder(npc)
					.aggressive(citizens.isTargetGoalAggressive())
					.radius(citizens.getTargetGoalRadius())
					.targets(citizens.getTargetGoalEntities())
					.build());

		if (citizens.isWanderGoalEnabled())
			try {
				behaviorController.addBehavior(WanderGoal.builder(npc).xrange(citizens.getWanderGoalRadius()).yrange(citizens.getWanderGoalRadius()).delay(0).build());
			} catch (final NoSuchMethodError ex) {
			}

		if (!citizens.isTargetGoalEnabled() && !citizens.isWanderGoalEnabled())
			npc.setUseMinecraftAI(true);

		final Navigator navigator = npc.getNavigator();

		if (boss.getType() == CompEntityType.PLAYER) {
			navigator.getLocalParameters().speedModifier(1F);
			navigator.getLocalParameters().speed(1F);
			navigator.getLocalParameters().baseSpeed((float) boss.getCitizensSettings().getSpeed());
		}

		navigator.getLocalParameters().distanceMargin(0.5).pathDistanceMargin(0.5);

		// Remove teleporting to players when far away
		npc.getNavigator().getDefaultParameters().stuckAction(null);

		// Set skin
		setSkinUrl(boss, npc);

		// Set scale and finalize
		npc.getOrAddTrait(ScaleTrait.class);
	}

	/*
	 * Helper to set Skin URL
	 */
	private static void setSkinUrl(Boss boss, NPC npc) {
		final String skin = boss.getCitizensSettings().getSkinOrAlias();
		final SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);

		if ((skin.startsWith("http://") || skin.startsWith("https://"))) {
			final JsonObject output = cache.get(skin);

			if (output != null)
				setSkin0(npc, trait, output);
			else
				Platform.runTaskAsync(() -> {
					final StringBuilder response = new StringBuilder();

					try {
						final URL url = new URL("https://api.mineskin.org/generate/url");
						final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

						connection.setRequestMethod("POST");
						connection.setDoOutput(true);
						connection.setRequestProperty("Accept", "application/json");
						connection.setRequestProperty("Authorization", "Bearer 4cae49319e3490934229cd135970f57c828adbb1fe902e1bf29d8924649f6871");
						connection.setRequestProperty("Content-Type", "application/json");

						try (OutputStream os = connection.getOutputStream()) {
							final byte[] input = SerializedMap.fromArray("url", skin).toJson().getBytes(StandardCharsets.UTF_8);

							os.write(input);
						}

						if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
							try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
								String inputLine;

								while ((inputLine = in.readLine()) != null)
									response.append(inputLine);

								final JsonObject jsonResponse = Common.GSON.fromJson(response.toString(), JsonObject.class);

								cache.put(skin, jsonResponse);
								Platform.runTask(() -> setSkin0(npc, trait, jsonResponse));
							}

						} else
							Common.logTimed(60, "Warning: Error fetching skin for NPC " + npc.getName() + ", got response code: " + connection.getResponseCode() + " from URL:" + skin);

						connection.disconnect();

					} catch (final Throwable t) {
						Common.error(t, "Error applying NPC's " + npc.getName() + " skin!",
								"Url: " + skin,
								"Response: " + response,
								"Error: {error}");
					}
				});

		} else
			trait.setSkinName(skin, true);
	}

	/*
	 * Helper method to set the persistent skin trait
	 */
	private static void setSkin0(NPC npc, SkinTrait trait, JsonObject output) {
		final JsonObject data = output.getAsJsonObject("data");
		final String uuid = data.get("uuid").getAsString();

		final JsonObject texture = data.getAsJsonObject("texture");
		final String textureEncoded = texture.get("value").getAsString();
		final String signature = texture.get("signature").getAsString();

		trait.setSkinPersistent(uuid, signature, textureEncoded);
	}

	/**
	 * Retargets the closest entity for the given boss
	 *
	 * @param spawnedBoss the boss
	 */
	public static void retarget(SpawnedBoss spawnedBoss) {
		final Entity entity = spawnedBoss.getEntity();
		final Boss boss = spawnedBoss.getBoss();

		final DiskRegion spawnRegion = boss.isKeptInSpawnRegion() ? spawnedBoss.getSpawnRegion() : null;

		final NPCRegistry registry = findRegistry();

		if (registry == null)
			return;

		final NPC npc = registry.getNPC(entity);
		final BossCitizensSettings citizens = boss.getCitizensSettings();

		if (npc != null && citizens.isTargetGoalEnabled()) {

			final List<Entity> potentialTargets = new ArrayList<>();
			final EntityTarget oldTarget = npc.getNavigator().getEntityTarget();

			for (final Entity nearby : Remain.getNearbyEntities(entity.getLocation(), citizens.getTargetGoalRadius())) {

				// Ignore same type entities
				if (entity.getType() == nearby.getType())
					continue;

				// Do not target self
				if (entity.getUniqueId().equals(nearby.getUniqueId()))
					continue;

				// Only retarget to someone else
				if (oldTarget != null && oldTarget.getTarget().getUniqueId().equals(nearby.getUniqueId()))
					continue;

				// Ignore entities out of reach
				if (spawnRegion != null && !spawnRegion.isWithin(nearby.getLocation()))
					continue;

				if (!Boss.canTarget(nearby))
					continue;

				final SpawnedBoss nearbyBoss = Boss.findBoss(nearby);

				if (nearbyBoss != null && nearbyBoss.getBoss().equals(boss))
					continue;

				if (citizens.getTargetGoalEntities().contains(nearby.getType()))
					potentialTargets.add(nearby);
			}

			if (!potentialTargets.isEmpty()) {
				Collections.shuffle(potentialTargets);

				final Location bossLocation = entity.getLocation();
				Entity closestTarget = potentialTargets.get(0);

				for (final Entity potentialTarget : potentialTargets)
					if (potentialTarget.getLocation().distance(bossLocation) < closestTarget.getLocation().distance(bossLocation))
						closestTarget = potentialTarget;

				if (npc.isSpawned())
					npc.getNavigator().setTarget(closestTarget, citizens.isTargetGoalAggressive());

			} else if (oldTarget != null && !Boss.canTarget(oldTarget.getTarget())) {
				npc.getNavigator().cancelNavigation();
			}
		}

		// Remove teleporting to players when far away
		if (npc != null && npc.isSpawned())
			npc.getNavigator().getDefaultParameters().stuckAction(null);
	}

	/**
	 * Sends the given Boss to the given location using Citizens pathfinding.
	 *
	 * @param boss
	 * @param target
	 * @return true if the NPC was found and navigation started
	 */
	public static boolean sendToLocation(SpawnedBoss boss, Location target) {
		final NPCRegistry registry = findRegistry();

		if (registry == null)
			return false;

		final NPC npc = registry.getNPC(boss.getEntity());

		if (npc == null)
			return false;

		npc.getNavigator().setTarget(target);

		return true;
	}

	/**
	 * Attempts to find a boss from the given NPC.
	 *
	 * @param entity
	 * @return
	 */
	public static SpawnedBoss findNPC(Entity entity) {
		final NPCRegistry registry = findRegistry();

		if (registry == null)
			return null;

		final NPC npc = registry.getNPC(entity);

		if (npc != null) {
			final String bossName = npc.data().get("BossName");

			if (bossName != null) {
				final Boss boss = Boss.findBoss(bossName);

				if (boss != null)
					return new SpawnedBoss(boss, (LivingEntity) entity);
			}
		}

		return null;
	}

	/*
	 * Find the NPC registry and return null if not found by catching the exception
	 */
	private static NPCRegistry findRegistry() {
		try {
			return CitizensAPI.getNPCRegistry();

		} catch (final IllegalStateException ex) {
			return null;
		}
	}
}
