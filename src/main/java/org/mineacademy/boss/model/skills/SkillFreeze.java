package org.mineacademy.boss.model.skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.BossSkillRestore;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillFreeze extends AbstractTargetSkill {

	private static List<UUID> freezedPlayers = new ArrayList<>();

	public static List<UUID> getFreezedPlayers() {
		return Collections.unmodifiableList(freezedPlayers);
	}

	/**
	 * How long shall the player be frozen in ticks?
	 */
	private int durationTicks;

	private String durationRaw;

	/**
	 * Create cobweb effect?
	 */
	private boolean cobweb;

	@Override
	public String getName() {
		return "Freeze Player";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("20 seconds", "40 seconds");
	}

	@Override
	public boolean execute(Player target, SpawnedBoss spawned) {
		if (target.hasMetadata("BossFreeze"))
			return true;

		freezedPlayers.add(target.getUniqueId());
		final boolean wasFlying = target.isFlying();
		final float flySpeed = target.getFlySpeed(), walkSpeed = target.getWalkSpeed();

		target.setFlying(false);
		target.setFlySpeed(0);
		target.setWalkSpeed(0);

		setMetadata(target, "BossFreeze", "");
		setMetadata(target, "BossFreeze_Flying", wasFlying);
		setMetadata(target, "BossFreeze_FlySpeed", flySpeed);
		setMetadata(target, "BossFreeze_WalkSpeed", walkSpeed);

		final Block block = target.getLocation().getBlock();
		final boolean changedBlock = cobweb && block.getType() == Material.AIR;

		if (changedBlock)
			block.setType(CompMaterial.COBWEB.getMaterial());

		scheduleFreezeTask(target);
		scheduleSkillRestore(durationTicks, target);

		Common.runLater(durationTicks, () -> {
			if (changedBlock)
				block.setType(CompMaterial.AIR.getMaterial());
		});

		sendSkillMessage(target, spawned);
		return true;
	}

	@Override
	protected BossSkillRestore getSkillRestore() {
		return player -> {
			final boolean flying = getMetadata(player, "BossFreeze_Flying", false);
			final float flySpeed = getMetadata(player, "BossFreeze_FlySpeed", 1F);
			final float walkSpeed = getMetadata(player, "BossFreeze_WalkSpeed", 1F);

			player.setFlying(flying);
			player.setFlySpeed(flySpeed);
			player.setWalkSpeed(walkSpeed);

			player.removeMetadata("BossFreeze", SimplePlugin.getInstance());
			freezedPlayers.remove(player.getUniqueId());
		};
	}

	private void scheduleFreezeTask(Player player) {
		final Location loc = player.getLocation();

		new BukkitRunnable() {

			@Override
			public void run() {
				if (!player.hasMetadata("BossFreeze") || !player.isOnline()) {
					cancel();

					return;
				}

				final Location curr = player.getLocation();

				if (!Valid.locationEquals(curr, loc)) {
					final Location adjusted = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), curr.getYaw(), curr.getPitch());

					player.teleport(adjusted);
				}
			}
		}.runTaskTimer(SimplePlugin.getInstance(), 0, 1);
	}

	@EventHandler
	public void onFlight(PlayerToggleFlightEvent e) {
		if (e.getPlayer().hasMetadata("BossFreeze"))
			e.getPlayer().setFlying(false);
	}

	@EventHandler
	public void onSprint(PlayerToggleSprintEvent e) {
		if (e.getPlayer().hasMetadata("BossFreeze"))
			e.getPlayer().setSprinting(false);
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"The Boss has put you into a meditation state. Do not move.",
				"You are now stuck! The {boss} is coming at you!",
				"You are playing too fast! Slow down please!"
		};
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.ICE,
				"Freeze Player",
				"",
				"Freeze the player",
				"completely.")
				.build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		durationRaw = map.getOrDefault("Duration", "3 seconds").toString();
		durationTicks = (int) TimeUtil.toTicks(durationRaw);
		cobweb = (boolean) map.getOrDefault("Cobweb", true);
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();

		map.put("Duration", durationRaw);
		map.put("Cobweb", cobweb);

		return map;
	}

	@Override
	public String[] getDefaultHeader() {
		return new String[] {
				"  Duration - How long should the player be frozen?",
				"  Cobweb - Should we spawn a cobweb at player's location? (Temporarily.)"
		};
	}
}
