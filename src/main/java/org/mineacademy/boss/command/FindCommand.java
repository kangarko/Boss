package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to find Bosses in worlds.
 */
final class FindCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	FindCommand() {
		super("find|f");

		this.setValidArguments(0, 2);
		this.setDescription(Lang.component("command-find-description"));
		this.setUsage("[boss/*] [world]");
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final List<SpawnedBoss> toList = new ArrayList<>();

		final String bossName = this.args.length > 0 ? this.args[0].toLowerCase() : "*";
		final String worldName = this.args.length > 1 ? this.args[1] : "*";

		if (!"*".equals(worldName))
			this.findWorld(worldName);

		// Add worlds
		for (final World world : Bukkit.getWorlds())
			if ("*".equals(worldName) || worldName.equalsIgnoreCase(world.getName()))
				toList.addAll(Boss.findBossesAliveIn(world));

		// Filter bosses
		if (!"*".equals(bossName)) {
			this.checkBoolean(Boss.isBossLoaded(bossName), Lang.component("command-invalid-boss", "boss", bossName, "available", Boss.getBossesNames()));

			for (final Iterator<SpawnedBoss> it = toList.iterator(); it.hasNext();) {
				final SpawnedBoss boss = it.next();

				if (!bossName.equalsIgnoreCase(boss.getBoss().getName()))
					it.remove();
			}
		}

		final List<SimpleComponent> lines = new ArrayList<>();

		for (final SpawnedBoss spawnedBoss : toList) {
			final LivingEntity entity = spawnedBoss.getEntity();
			final Boss boss = spawnedBoss.getBoss();
			final String bossUniqueId = entity.getUniqueId().toString();

			lines.add(SimpleComponent
					.fromPlain(" ")
					.appendMiniAmpersand("&8[&4X&8]")
					.onHoverLegacy("&7Click to kill silently.")
					.onClickRunCmd("/" + this.getLabel() + " uid kill " + bossUniqueId)

					.appendPlain(" ")

					.appendMiniAmpersand("&8[&3>&8]")
					.onHoverLegacy("&7Click to teleport to.")
					.onClickRunCmd("/" + this.getLabel() + " uid tp " + bossUniqueId)

					.appendPlain(" ")

					.appendMiniAmpersand("&f" + boss.getName() + " &8[&7" + SerializeUtil.serializeLocation(entity.getLocation()) + "&8]"
							+ " &8[&7\u2764 " + Remain.getHealth(entity) + "&8/&7" + Remain.getMaxHealth(entity) + "&8]")

					.onHoverLegacy("&4Boss Information",
							"&7Name: &f" + boss.getName(),
							"&7Entity type: &f" + boss.getTypeFormatted(),
							"&7Health: &f" + Remain.getHealth(entity) + "&8/&f" + Remain.getMaxHealth(entity),
							"&7Location: &f" + SerializeUtil.serializeLocation(entity.getLocation())));
		}

		this.checkBoolean(!lines.isEmpty(), Lang.component("command-invalid-bosses"));

		new ChatPaginator()
				.setFoundationHeader("Found " + toList.size() + " Bosses (In Loaded Chunks)")
				.setPages(lines)
				.send(this.audience);
	}

	@Override
	public List<String> tabComplete() {

		switch (this.args.length) {
			case 1:
				return this.completeLastWordBossNames();
			case 2:
				return this.completeLastWordWorldNames();
		}

		return NO_COMPLETE;
	}
}