package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSpawnReason;
import org.mineacademy.boss.settings.Localization;
import org.mineacademy.boss.util.BossConditionedSpawnUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.TabUtil;
import org.mineacademy.fo.remain.Remain;

public final class SpawnPlayerCommand extends AbstractBossSubcommand {

	public SpawnPlayerCommand() {
		super("spawnpl|summonp|spawnp|spawnplayer");

		setDescription("Spawn Bosses near players.");
		setUsage("<player> <boss, anotherBoss, .. OR random>");
		setMinArguments(2);
	}

	@Override
	protected void onCommand() {
		final Player player = findPlayer(args[0]);

		final Location location = player.getLocation();
		final String[] bosses = Arrays.copyOfRange(args, 1, args.length);

		boolean saveChunk = false;

		if (!location.getChunk().isLoaded()) {
			location.getChunk().load();

			saveChunk = true;
		}

		for (final String name : bosses) {
			final Boss boss = "random".equalsIgnoreCase(name) ? getBosses().getRandomBoss() : getBosses().findBoss(name);
			checkNotNull(boss, "Boss '" + name + "' does not exists. Available: " + ListCommand.listAvailable());

			if (BossConditionedSpawnUtil.spawnWithLimits(boss, location, BossSpawnReason.COMMAND))
				tell("Summoned " + Common.pluralEs(bosses.length, "Boss") + " at " + player.getName() + "'s location at " + Common.shortLocation(location) + ".");
			else
				Common.tellReplaced(sender, Localization.Spawning.FAIL, "boss", boss.getName());
		}

		if (saveChunk)
			location.getChunk().unload(true);
	}

	@Override
	public List<String> tabComplete() {
		final List<String> tab = new ArrayList<>();

		if (args.length == 1)
			return TabUtil.complete(args[0], Remain.getOnlinePlayers().toArray());

		if (args.length >= 2) {
			for (final String type : getBosses().getBossesAsList())
				if (type.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
					tab.add(type.toString());

			tab.add("random");
		}

		return tab;
	}
}