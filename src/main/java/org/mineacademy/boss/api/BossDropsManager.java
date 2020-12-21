package org.mineacademy.boss.api;

import java.util.List;

import org.bukkit.entity.Player;

public interface BossDropsManager {

	void registerDamage(Player attacker, double damage);

	List<Player> getPlayersToReward();

	void clearAll();

	int getPlayerLimit();

	int getTimeLimit();
}
