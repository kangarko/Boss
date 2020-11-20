package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSpawnReason;
import org.mineacademy.boss.settings.Localization;
import org.mineacademy.boss.util.BossConditionedSpawnUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.Replacer;

public final class SpawnCommand extends AbstractBossSubcommand {

	public SpawnCommand() {
		super("spawn|summon");

		setDescription("Spawn Bosses on your world.");
		setUsage("<world x y z> <boss, anotherBoss, .. OR random>");
		setMinArguments(5);
	}

	@Override
	protected void onCommand() {
		final double x = getInteger(1);
		final double y = getInteger(2);
		final double z = getInteger(3);

		final Location loc = new Location(getWorld(args[0]), x, y, z);
		final String[] bosses = Arrays.copyOfRange(args, 4, args.length);

		boolean saveChunk = false;

		if (!loc.getChunk().isLoaded()) {
			loc.getChunk().load();

			saveChunk = true;
		}

		for (final String name : bosses) {
			final Boss b = "random".equalsIgnoreCase(name) ? getBosses().getRandomBoss() : getBosses().findBoss(name);
			checkNotNull(b, Localization.Invalid.BOSS.replace("{boss}", name).replace("{available}", ListCommand.listAvailable()));

			if (BossConditionedSpawnUtil.spawnWithLimits(b, loc, BossSpawnReason.COMMAND))
				Common.tellReplaced(sender, Localization.Spawning.SUMMONED_LOCATION, "amount", bosses.length, "boss", Common.pluralEs(bosses.length, "Boss"), "location", Common.shortLocation(loc));
			else
				Common.tellReplaced(sender, Localization.Spawning.FAIL, "boss", b.getName());
		}

		if (saveChunk)
			loc.getChunk().unload(true);
	}

	private World getWorld(final String name) throws CommandException {
		final World w = Bukkit.getWorld(name);
		checkNotNull(w, Replacer.of(Localization.Invalid.WORLD).find("world", "available").replace(name, StringUtils.join(Bukkit.getWorlds(), ", ")).getReplacedMessageJoined());

		return w;
	}

	private double getInteger(final int index) throws CommandException {
		final String raw = args[index];

		try {
			if ("~".equals(raw)) {
				checkBoolean(sender instanceof Player, "Only living players can use ~ for their location!");

				switch (index) {
					case 1:
						return getPlayer().getLocation().getBlockX();
					case 2:
						return getPlayer().getLocation().getBlockY();
					case 3:
						return getPlayer().getLocation().getBlockZ();

					default:
						throw new FoException("out of index: " + index);
				}
			}

			return Double.parseDouble(raw);

		} catch (final NumberFormatException ex) {
			returnTell("Location must be a number (or ~ for your location)! Got: " + raw);
		}

		return -1;
	}

	@Override
	public List<String> tabComplete() {
		final List<String> tab = new ArrayList<>();

		if (args.length == 1)
			for (final World w : Bukkit.getWorlds())
				if (w.getName().toString().toLowerCase().startsWith(args[0].toLowerCase()))
					tab.add(w.getName());

		// 3 4 5 -> loc

		if (args.length > 1 && args.length < 5)
			tab.add("~");

		if (args.length >= 5) {
			for (final String type : getBosses().getBossesAsList())
				if (type.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
					tab.add(type.toString());

			tab.add("random");
		}

		return tab;
	}
}