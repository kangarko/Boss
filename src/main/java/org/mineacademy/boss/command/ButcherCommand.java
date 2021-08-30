package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.exception.CommandException;

public final class ButcherCommand extends AbstractBossSubcommand {

	public ButcherCommand() {
		super("butcher|b");

		setDescription("Kill spawned Bosses");
		setUsage("<radius/world/*> [boss]");
		setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		final String param = args[0];
		int radius = -1;

		try {
			radius = Integer.parseInt(param);
			checkBoolean(radius > 0 && radius <= 100_000, "Radius must be between 0 and 100 000.");

		} catch (final NumberFormatException ex) {
		}

		Boss type = null;

		if (args.length == 2) {
			type = getBosses().findBoss(args[1]);
			checkBoolean(type != null, "&cInvalid Boss '" + args[1] + "'. Available: &7" + ListCommand.listAvailable());
		}

		int removed = 0;

		if (radius != -1) {
			checkBoolean(sender instanceof Player, "&4Only players can specify the butcher radius from their location.");
			removed = cleanup(type, getPlayer().getNearbyEntities(radius, radius, radius));

			tell("&7Removed &2" + removed + " Bosses &7within " + radius + " blocks.");
		} else if ("*".equals(param))
			for (final World w : Bukkit.getWorlds())
				cleanupWorld(type, w.getName());

		else
			cleanupWorld(type, param);
	}

	private void cleanupWorld(Boss match, String param) throws CommandException {
		final World world = Bukkit.getWorld(param);
		checkNotNull(world, "World '" + param + "' doesn't exist. Available (" + Common.getWorldNames().size() + "): " + Common.join(Common.getWorldNames()));

		final int removed = cleanup(match, world.getLivingEntities());

		tell("&7Removed &2" + removed + " Bosses &7in " + world.getName() + (world.getName().equals("world") ? "" : " world") + ".");
	}

	private int cleanup(Boss match, List<? extends Entity> entities) {
		int removed = 0;

		for (final Entity en : entities) {
			final Boss toRemove = getBosses().findBoss(en);

			if (toRemove != null) {
				if (match != null && !toRemove.equals(match))
					continue;

				if (en.getPassenger() != null)
					en.getPassenger().remove();

				if (en.getVehicle() != null)
					en.getVehicle().remove();

				en.remove();

				removed++;
			}
		}

		return removed;
	}

	@Override
	public List<String> tabComplete() {
		final List<String> tab = new ArrayList<>();

		if (args.length == 1)
			for (final World world : Bukkit.getWorlds())
				if (world.getName().toString().toLowerCase().startsWith(args[0].toLowerCase()))
					tab.add(world.getName());

		if (args.length == 2)
			for (final String type : getBosses().getBossesAsList())
				if (type.toLowerCase().startsWith(args[1].toLowerCase()))
					tab.add(type.toString());

		return tab;
	}
}