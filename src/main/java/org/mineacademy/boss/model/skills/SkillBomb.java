package org.mineacademy.boss.model.skills;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillBomb extends AbstractTargetSkill {

	/**
	 * How many ticks before the bomb goes off.
	 */
	private int fuseTicks;

	private String fuseTicksRaw;

	/**
	 * How high above the sky to spawn?
	 */
	private int height;

	/**
	 * The strength of the explosion.
	 */
	private float power;

	/**
	 * Should TNT destroy blocks around?
	 */
	private boolean destroyBlocks = false;

	@Override
	public String getName() {
		return "Bomb";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("30 seconds", "1 minute");
	}

	@Override
	public boolean execute(Player target, SpawnedBoss spawned) {
		final Block block = findHighestBlock(target);

		spawnTnt(block.getLocation());
		sendSkillMessage(target, spawned);

		return true;
	}

	private Block findHighestBlock(Player player) {
		Block block = player.getWorld().getBlockAt(player.getLocation());

		for (int i = 0; i < height; i++) {
			if (block.getType() != Material.AIR)
				break;

			block = block.getRelative(BlockFace.UP);
		}

		return block;
	}

	private void spawnTnt(Location loc) {
		final TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);

		tnt.setFuseTicks(fuseTicks);
		tnt.setMetadata("BossTNT", new FixedMetadataValue(SimplePlugin.getInstance(), ""));
	}

	@EventHandler
	public void onPrime(ExplosionPrimeEvent e) {
		if (e.getEntity().hasMetadata("BossTNT"))
			e.setRadius(power);
	}

	@EventHandler
	public void onExplode(EntityExplodeEvent e) {
		if (!destroyBlocks && e.getEntity().hasMetadata("BossTNT"))
			e.blockList().clear();
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.TNT,
				"Obsidian Bomb",
				"",
				"Spawn a TnT bomb",
				"above the player.")
				.build().make();
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"You have been &cbombed &7by the boss!",
				"The {boss} has &cspawned a TnT &7at you!",
				"Please do not move, there's a gift for ya!"
		};
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		fuseTicksRaw = map.getOrDefault("Fuse_Time", "3 seconds").toString();
		fuseTicks = (int) TimeUtil.toTicks(fuseTicksRaw);

		height = (int) map.getOrDefault("Spawn_Height", 7);
		power = Float.parseFloat(map.getOrDefault("Power", 4F).toString());
		destroyBlocks = (boolean) map.getOrDefault("Destroy_Blocks", false);
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();

		map.put("Fuse_Time", fuseTicksRaw);
		map.put("Spawn_Height", height);
		map.put("Power", power);
		map.put("Destroy_Blocks", destroyBlocks);

		return map;
	}

	@Override
	public String[] getDefaultHeader() {
		return new String[] {
				"  Fuse_Time - How long should it take before the TnT explodes?",
				"  Spawn_Height - How many blocks above the player to spawn the TnT?",
				"  Power - The power of the explosion. Must be a whole number: 4.0",
				"  Destroy_Blocks - Should the TnT not only damage players, but also destroy blocks?"
		};
	}
}
