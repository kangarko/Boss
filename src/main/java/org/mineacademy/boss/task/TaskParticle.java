package org.mineacademy.boss.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossParticleSettings;
import org.mineacademy.boss.model.ParticleShape;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.remain.CompParticle;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Task spawning particles around alive Bosses.
 *
 * Runs every 2 server ticks (100ms) for smooth animated shapes.
 * The interval setting controls how many runs to skip between spawns.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskParticle extends SimpleRunnable {

	@Getter
	private static final TaskParticle instance = new TaskParticle();

	private final Map<UUID, Long> nextRunTick = new HashMap<>();

	private final Map<UUID, Double> step = new HashMap<>();

	private long runCount;

	@Override
	public void run() {

		if (Remain.getOnlinePlayers().isEmpty())
			return;

		this.runCount++;

		final List<SpawnedBoss> alive = Boss.findBossesAlive();

		for (final SpawnedBoss spawned : alive)
			this.particle(spawned);

		if (this.runCount % 100 == 0) {
			final Set<UUID> aliveIds = alive.stream().map(s -> s.getEntity().getUniqueId()).collect(Collectors.toSet());

			this.nextRunTick.keySet().retainAll(aliveIds);
			this.step.keySet().retainAll(aliveIds);
		}
	}

	private void particle(SpawnedBoss spawnedBoss) {
		final BossParticleSettings settings = spawnedBoss.getBoss().getParticleSettings();

		if (!settings.isEnabled() || settings.getType() == null || settings.getType().isRemoved())
			return;

		if (settings.getType().getParticle() == null || settings.getType().getParticle().getDataType() != Void.class)
			return;

		final UUID uuid = spawnedBoss.getEntity().getUniqueId();
		final long nextTick = this.nextRunTick.getOrDefault(uuid, 0L);

		if (this.runCount < nextTick)
			return;

		this.nextRunTick.put(uuid, this.runCount + Math.max(1, settings.getIntervalTicks()));

		final LivingEntity entity = spawnedBoss.getEntity();
		final Location center = entity.getLocation().add(0, entity.getHeight() / 2, 0);

		if (settings.getForwardOffset() != 0) {
			final double yaw = Math.toRadians(center.getYaw());

			center.add(-Math.sin(yaw) * settings.getForwardOffset(), 0, Math.cos(yaw) * settings.getForwardOffset());
		}
		final int count = Math.min(settings.getCount(), 100);
		final double currentStep = this.step.getOrDefault(uuid, 0.0);
		final ParticleShape shape = settings.getShape();
		final double radius = settings.getRadius();
		final CompParticle type = settings.getType();

		switch (shape) {
			case AMBIENT:
				type.spawn(center,
						settings.getOffsetX(), settings.getOffsetY(), settings.getOffsetZ(),
						settings.getSpeed(), count, 0, null);
				break;

			case CIRCLE: {
				final double increment = 2 * Math.PI / count;

				for (int i = 0; i < count; i++) {
					final double angle = currentStep + i * increment;

					spawnAt(type, center, Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
				}

				this.step.put(uuid, (currentStep + 0.15) % (2 * Math.PI));
				break;
			}

			case HELIX: {
				final int strands = Math.max(1, settings.getStrands());
				final double height = settings.getHeight();

				for (int i = 0; i < count; i++) {
					final double ratio = (double) i / count;
					final double y = ratio * height - height / 2;

					for (int s = 0; s < strands; s++) {
						final double angle = currentStep + ratio * 4 * Math.PI + (2 * Math.PI * s / strands);

						spawnAt(type, center, Math.cos(angle) * radius, y, Math.sin(angle) * radius);
					}
				}

				this.step.put(uuid, (currentStep + 0.2) % (2 * Math.PI));
				break;
			}

			case VORTEX: {
				final int strands = Math.max(1, settings.getStrands());
				final double height = settings.getHeight();
				final double radials = Math.PI / 16;

				for (int i = 0; i < count; i++) {
					final double progress = (currentStep + i) % 80;

					for (int s = 0; s < strands; s++) {
						final double angle = progress * radials + (2 * Math.PI * s / strands);
						final double currentRadius = radius * (1 + progress * 0.02);
						final double y = progress * height / 80;

						spawnAt(type, center, Math.cos(angle) * currentRadius, y, Math.sin(angle) * currentRadius);
					}
				}

				this.step.put(uuid, (currentStep + count) % 80);
				break;
			}

			case SPHERE: {
				for (int i = 0; i < count; i++) {
					final double theta = RandomUtil.nextDouble() * 2 * Math.PI;
					final double phi = Math.acos(2 * RandomUtil.nextDouble() - 1);

					spawnAt(type, center,
							radius * Math.sin(phi) * Math.cos(theta),
							radius * Math.sin(phi) * Math.sin(theta),
							radius * Math.cos(phi));
				}

				break;
			}

			case TORNADO: {
				final double height = settings.getHeight();
				final double distance = 0.375;
				int budget = count;

				for (double y = 0; y < height && budget > 0; y += distance) {
					final double currentRadius = radius * (y / height);

					if (currentRadius < 0.05)
						continue;

					final int amount = Math.min(budget, (int) Math.max(4, currentRadius * 8));
					final double increment = 2 * Math.PI / amount;

					for (int i = 0; i < amount; i++) {
						final double angle = currentStep + i * increment;

						spawnAt(type, center, Math.cos(angle) * currentRadius, y, Math.sin(angle) * currentRadius);
					}

					budget -= amount;
				}

				this.step.put(uuid, (currentStep + 0.15) % (2 * Math.PI));
				break;
			}

			case WINGS: {
				final double yawRad = Math.toRadians(center.getYaw());
				final double sideX = Math.cos(yawRad);
				final double sideZ = Math.sin(yawRad);
				final double scale = radius / 3.0;
				final double flapScale = 0.85 + 0.15 * Math.sin(currentStep);

				for (int i = 0; i < count; i++) {
					final double t = (double) i / count * 2 * Math.PI;
					final double factor = Math.exp(Math.cos(t)) - 2 * Math.cos(4 * t) - Math.pow(Math.sin(t / 12.0), 5);
					final double localX = Math.sin(t) * factor * scale * flapScale;
					final double localY = Math.cos(t) * factor * scale;

					spawnAt(type, center, localX * sideX, localY, localX * sideZ);
				}

				this.step.put(uuid, (currentStep + 0.15) % (2 * Math.PI));
				break;
			}

			case CUBE: {
				final double half = radius;
				final double cosR = Math.cos(currentStep);
				final double sinR = Math.sin(currentStep);

				for (int i = 0; i < count; i++) {
					final int edge = RandomUtil.nextIntBetween(0, 11);
					final int axis = edge / 4;
					final int corner = edge % 4;

					final double fixed1 = (corner % 2 == 0) ? -half : half;
					final double fixed2 = (corner / 2 == 0) ? -half : half;
					final double vary = RandomUtil.nextDouble() * 2 * half - half;

					double x, y, z;

					if (axis == 0) {
						x = vary;
						y = fixed1;
						z = fixed2;
					} else if (axis == 1) {
						y = vary;
						x = fixed1;
						z = fixed2;
					} else {
						z = vary;
						x = fixed1;
						y = fixed2;
					}

					final double rx = x * cosR - z * sinR;
					final double rz = x * sinR + z * cosR;

					spawnAt(type, center, rx, y, rz);
				}

				this.step.put(uuid, (currentStep + 0.05) % (2 * Math.PI));
				break;
			}
		}
	}

	private static void spawnAt(CompParticle type, Location center, double x, double y, double z) {
		center.add(x, y, z);
		type.spawn(center);
		center.subtract(x, y, z);
	}
}
