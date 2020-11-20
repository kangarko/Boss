package org.mineacademy.boss.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossDropsManager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
public class SimpleDropsManager implements BossDropsManager {

	/**
	 * The boss associated with this drops manager
	 */
	private final Boss boss;

	/**
	 * The history of this boss being attacked
	 */
	private final List<BossDamage> damageLogs = new ArrayList<>();

	@Override
	public void registerDamage(Player attacker, double damage) {
		final BossDamage current = find(attacker);

		if (current != null)
			current.addDamage(damage);
		else
			damageLogs.add(new BossDamage(attacker, damage));
	}

	private BossDamage find(Player attacker) {
		for (final BossDamage log : damageLogs)
			if (log.getAttacker().equals(attacker.getName()))
				return log;

		return null;
	}

	@Override
	public List<Player> getPlayersToReward() {
		final List<Player> playersToReward = new ArrayList<>();
		List<BossDamage> damages = new ArrayList<>();

		// Fill with damage logs in the time limit
		damages.addAll(getDamageInTime0());

		// Sort by the biggest damage
		Collections.sort(damages, (first, second) -> Long.compare(first.getLastAttackTime(), second.getLastAttackTime()));

		// Filter out the last X players by limit
		damages = filterDamageInLimit0(damages);

		// Convert to Bukkit players
		for (final BossDamage logInLimit : damages) {
			final Player player = Bukkit.getPlayer(logInLimit.getAttacker());

			if (player != null && player.isOnline())
				playersToReward.add(player);
		}

		return playersToReward;
	}

	private List<BossDamage> getDamageInTime0() {
		final List<BossDamage> logsInTime = new ArrayList<>();

		for (final BossDamage log : damageLogs)
			if (System.currentTimeMillis() - log.getLastAttackTime() < getTimeLimit() * 1000)
				logsInTime.add(log);

		return logsInTime;
	}

	private List<BossDamage> filterDamageInLimit0(List<BossDamage> logs) {
		final LinkedList<BossDamage> queue = new LinkedList<>(logs);
		final List<BossDamage> copy = new ArrayList<>();

		for (int i = 0; i < getPlayerLimit(); i++) {
			if (queue.isEmpty())
				break;

			copy.add(queue.removeLast());
		}

		return copy;
	}

	@Override
	public void clearAll() {
		damageLogs.clear();
	}

	@Override
	public int getPlayerLimit() {
		return boss.getSettings().getInventoryDropsPlayerLimit();
	}

	@Override
	public int getTimeLimit() {
		return boss.getSettings().getInventoryDropsTimeLimit();
	}
}

/**
 * Stores an individual history log when a Boss is damaged by a player at a certain time
 */
@ToString
@Getter
class BossDamage {

	/**
	 * The attacker's name
	 */
	private final String attacker;

	/**
	 * The time of last attack
	 */
	private long lastAttackTime;

	/**
	 * The total damage, can be add up
	 */
	private final double totalDamage;

	protected BossDamage(Player player, double damage) {
		this.attacker = player.getName();
		this.totalDamage = damage;
		this.lastAttackTime = System.currentTimeMillis();
	}

	/**
	 * Updates the last damage time and adds up the new damage
	 *
	 * @param damage the new damage
	 */
	protected void addDamage(double damage) {
		lastAttackTime = System.currentTimeMillis();
		damage += damage;
	}
}
