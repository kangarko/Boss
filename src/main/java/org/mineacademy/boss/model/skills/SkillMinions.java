package org.mineacademy.boss.model.skills;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillMinions extends AbstractTargetSkill {

	private int radius;
	private int amount;

	@Override
	public String getName() {
		return "Spawn Minions";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("30 seconds", "1 minute");
	}

	@Override
	public boolean execute(Player target, SpawnedBoss spawned) {
		final LivingEntity en = spawned.getEntity();
		final Boss boss = spawned.getBoss();

		int extraTries = 5, count = 0;
		for (int i = 0; i < amount; ++i) {
			final Location loc = getRandomLocation(en.getLocation());

			if (loc == null) {
				if (--extraTries > 0)
					--i;

				continue;
			}

			final Entity minion = en.getWorld().spawnEntity(loc, boss.getType());
			++count;

			if (minion instanceof Ageable)
				((Ageable) minion).setBaby();
			else if (minion instanceof Zombie)
				((Zombie) minion).setBaby(true);

			if (minion instanceof Creature)
				((Creature) minion).setTarget(target);

			minion.setMetadata("BossMinion", new FixedMetadataValue(SimplePlugin.getInstance(), boss.getName()));
		}

		if (count != 0)
			sendSkillMessage(target, spawned);
		return count != 0;
	}

	private Location getRandomLocation(Location center) {
		final int tries = 5;

		for (int i = 0; i < tries; ++i) {
			final Location loc = RandomUtil.nextLocation(center, radius, false);
			//loc.setY(loc.getWorld().getHighestBlockYAt(loc));
			final org.bukkit.block.Block b = loc.add(0, 1, 0).getBlock();
			final int maxY = loc.getWorld().getMaxHeight();

			// find first nearby block
			int dy = 0;
			boolean last = b.getType() == Material.AIR;
			for (; loc.getY() + dy > 1; --dy)
				if (b.getRelative(0, dy, 0).getType() != Material.AIR) {
					if (last)
						break;
				} else
					last = true;
			// find available space
			last = false;
			for (int y = (int) loc.getY(); dy < 15 && y <= maxY; ++dy, ++y)
				if (b.getRelative(0, dy, 0).getType() == Material.AIR) {
					if (last)
						return loc.add(0, dy, 0);
					last = true;
				} else
					last = false;
		}

		return null;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] { "Watch out for {boss}'s new friends - Minions!", "{boss} has spawned reinforcements!", "Say hello to your new friends, {player}!" };
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(CompMaterial.SHEEP_SPAWN_EGG, getName(), "", "Boss will spawn reinforcements", "to help him chase the player!").build().make();
	}

	@Override
	public void readSettings(Map<String, Object> map) {
		radius = Integer.parseInt(map.getOrDefault("Radius", 5).toString());
		amount = Integer.parseInt(map.getOrDefault("Amount", 1).toString());
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();

		map.put("Radius", radius);
		map.put("Amount", amount);

		return map;
	}
}
