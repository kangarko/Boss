package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.remain.Remain;

public final class EggCommand extends AbstractBossSubcommand {

	public EggCommand() {
		super("egg|e");

		setDescription("Get a Boss' spawner egg.");
		setUsage("<name> [player] [amount]");
		setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		final String name = args[0];
		final Boss boss = getBosses().findBoss(name);

		checkNotNull(boss, "Boss named '" + name + "' doesn't exist. Available: " + ListCommand.listAvailable());

		Player player = sender instanceof Player ? getPlayer() : null;

		if (args.length == 2) {
			final String other = args[1];

			player = Bukkit.getPlayer(other);
			checkBoolean(player != null && player.isOnline(), "&cPlayer " + other + " is not online!");

			if (!player.equals(getPlayer()))
				Common.tell(player, "&7You have obtained spawn egg for the &f" + boss.getName() + " &7boss.");
		} else
			checkNotNull(player, "When running from the console, please specify the player as the last argument!");

		final int amount = args.length > 2 ? findNumber(2, "Please enter a valid amount number.") : 1;
		final ItemStack egg = boss.asEgg();

		egg.setAmount(amount);

		// Add items to players inventory and drop leftovers
		final Map<Integer, ItemStack> leftovers = PlayerUtil.addItems(player.getInventory(), egg);
		final World world = player.getWorld();
		final Location loc = player.getLocation();

		for (final ItemStack leftover : leftovers.values())
			world.dropItem(loc, leftover);

		tell("&7" + (player.equals(getPlayer()) ? "You have" : player.getName() + " has") + " obtained spawn egg for the &f" + boss.getName() + " &7boss." + (leftovers.isEmpty() ? "" : " Your inventory was full so some items were dropped on the floor."));
	}

	@Override
	public List<String> tabComplete() {
		final List<String> tab = new ArrayList<>();

		if (args.length == 1) {
			for (final String boss : getBosses().getBossesAsList())
				if (boss.toLowerCase().startsWith(args[0].toLowerCase()))
					tab.add(boss);
		} else if (args.length == 2)
			for (final Player player : Remain.getOnlinePlayers())
				if (player.getName().toLowerCase().startsWith(args[1].toLowerCase()))
					tab.add(player.getName());

		return tab;
	}
}