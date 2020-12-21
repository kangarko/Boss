package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.model.BossManager;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.model.SimpleComponent;

public class FindCommand extends AbstractBossSubcommand {

	public FindCommand() {
		super("find|f");

		setDescription("Find spawned Bosses.");
		setUsage("<boss/*> [world]");
		setMinArguments(1);

		//registerPlaceholder(new ReturnedPlaceholder(1, "boss"));
	}

	@Override
	protected final void onCommand() {
		final BossManager manager = getBosses();
		final StrictList<SpawnedBoss> all = new StrictList<>();

		final String bossName = args[0].toLowerCase();
		final String worldName = args.length > 1 ? args[1] : "*";

		{ // Add worlds
			for (final World world : Bukkit.getWorlds())
				if ("*".equals(worldName) || worldName.equalsIgnoreCase(world.getName()))
					all.addAll(manager.findBosses(world));
		}

		// Filter bosses
		if (!"*".equals(bossName)) {
			checkBoolean(getBosses().getBossesAsList().contains(bossName), "&cThe boss named {boss} does not exist.");

			for (final Iterator<SpawnedBoss> it = all.iterator(); it.hasNext();) {
				final SpawnedBoss sb = it.next();

				if (!bossName.equalsIgnoreCase(sb.getBoss().getName()))
					it.remove();
			}
		}

		tell("&8" + Common.chatLineSmooth(),
				" ",
				ChatUtil.center("&6&lBosses Live"),
				ChatUtil.center("&7" + all.size() + " tracked"),
				" ",
				"&8" + Common.chatLineSmooth());

		for (final SpawnedBoss spawned : all) {
			final LivingEntity entity = spawned.getEntity();
			final Boss boss = spawned.getBoss();
			final String uid = entity.getUniqueId().toString();

			final String nameMessage = "&7Name: &2" + boss.getName();

			SimpleComponent
					.of(" ")
					.append("&8[&4X&8]")
					.onHover("&7Click to kill silently.")
					.onClickRunCmd("/" + getLabel() + " uid " + uid + " kill")

					.append(" ")

					.append("&8[&3>&8]")
					.onHover("&7Click to teleport to.")
					.onClickRunCmd("/" + getLabel() + " uid " + uid + " tp")

					.append(" ")

					.append(boss.getName() + " &8[&7" + ItemUtil.bountifyCapitalized(boss.getType()) + "&8] [&7" + Common.shortLocation(entity.getLocation()) + "&8]")

					.onHover(ChatUtil.center("&fBoss Information", nameMessage.length() * 2 + nameMessage.length() / 3),
							nameMessage,
							"&7Type: &2" + boss.getType(),
							"&7Health: &2" + (int) entity.getHealth() + " / " + (int) entity.getMaxHealth(),
							"&7Location: &2" + Common.shortLocation(entity.getLocation()))
					.send(sender);
		}
	}

	@Override
	public final List<String> tabComplete() {
		final List<String> tab = new ArrayList<>();

		if (args.length == 1) {
			final String param = args[0].toLowerCase();

			for (final String name : getBosses().getBossesAsList())
				if (name.toLowerCase().startsWith(param))
					tab.add(name);

			tab.add("*");
		}

		if (args.length == 2) {
			final String param = args[1].toLowerCase();

			for (final World w : Bukkit.getWorlds())
				if (w.getName().toLowerCase().startsWith(param))
					tab.add(w.getName());
		}

		return tab;
	}
}