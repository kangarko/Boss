package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.model.BossPlayer;
import org.mineacademy.boss.storage.SimpleRegionData;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidCommandArgException;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.visual.VisualizedRegion;

public final class RegionCommand extends AbstractBossSubcommand {

	public RegionCommand() {
		super("region|rg");

		setDescription("Manage Boss' regions.");
		setUsage("<params ...>");
		setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		final String param = args[0].toLowerCase();
		final String name = args.length > 1 ? args[1] : null;

		final SimpleRegionData data = SimpleRegionData.$();
		final BossPlayer cache = BossPlugin.getDataFor(getPlayer());

		//
		// Commands without a region.
		//
		if ("list".equals(param)) {
			tell("&8" + Common.chatLineSmooth(),
					" ",
					ChatUtil.center("&6&lBOSS Regions"),
					ChatUtil.center("&7" + data.getRegions().size() + " loaded"),
					"&8" + Common.chatLineSmooth());

			for (final Region region : data.getRegions()) {
				final String longestText = "&7Secondary: &2" + Common.shortLocation(region.getSecondary());

				SimpleComponent
						.of(" ")

						.append("&8[&4X&8]")
						.onHover("&7Click to remove permanently.")
						.onClickRunCmd("/" + getLabel() + " " + getSublabel() + " " + Param.REMOVE + " " + region.getName() + " -list")

						.append(" ")

						.append("&8[&2?&8]")
						.onHover("&7Click to visualize.")
						.onClickRunCmd("/" + getLabel() + " " + getSublabel() + " " + Param.VIEW + " " + region.getName() + " -list")

						.append(" ")

						.append("&8[&3>&8]")
						.onHover("&7Click to teleport to the center.")
						.onClickRunCmd("/" + getLabel() + " " + getSublabel() + " " + Param.TP + " " + region.getName() + " -list")

						.append(" ")

						.append("&7" + region.getName())
						.onHover(ChatUtil.center("&fRegion Information", longestText.length() * 2 + longestText.length() / 3),
								"&7Primary: &2" + Common.shortLocation(region.getPrimary()),
								longestText,
								"&7Size: &2" + region.getBlocks().size() + " blocks")

						.send(sender);
			}

			return;
		}

		final Param p = Param.find(param);

		if (p == null)
			throw new InvalidCommandArgException();

		checkBoolean(sender instanceof Player, "Only players may execute /{label} {0} {1}");

		//
		// Commands requiring region.
		//
		checkNotNull(name, "&cPlease specify the region name.");

		if (p == Param.ADD) {

			final Region oldRegion = data.getRegion(name);
			final VisualizedRegion createdRegion = cache.getCreatedRegion();

			checkBoolean(createdRegion != null && createdRegion.isWhole(), "&cBefore adding a region, select primary and secondary points first. To do that use the tool in '/{label} tools'.");
			checkBoolean(oldRegion == null, "&cRegion '" + (oldRegion == null ? "" : oldRegion.getName()) + "' already exists.");

			createdRegion.setName(name);

			data.addRegion(createdRegion);
			returnTell("&7Saved new region named '&2" + name + "&7'");
		}

		//
		// Commands requiring a valid region.
		//
		checkBoolean(data.hasRegion(name), "&cRegion '" + name + "' doesn't exists.");

		if (p == Param.REMOVE) {
			data.removeRegion(name);

			tellAndList("&7Removed region named '&2" + name + "&7'");
		} else if (p == Param.VIEW) {
			final VisualizedRegion region = data.getRegion(name);

			region.showParticles(getPlayer(), 5 * 20);
			tellAndList("&7Region '&2" + name + "&7 is being visualized for 5 seconds.'");
		} else if (p == Param.TP) {
			final Location center = getHighest(data.getRegion(name).getCenter().clone());
			getPlayer().teleport(center);

			tellAndList("&7Teleported to region '&2" + name + "&7'");
		} else
			throw new FoException("Unhandled param " + p);
	}

	private void tellAndList(final String message) {
		for (int i = 0; i < 12 - SimpleRegionData.$().getRegions().size() + 1 + 3; i++)
			tellNoPrefix("  ");

		if (args.length > 2 && "-list".equals(args[2]))
			getPlayer().performCommand(getLabel() + " " + getSublabel() + " " + Param.LIST);

		tell(message);
	}

	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"add <name> - Create a new region.",
				"remove <name> - Delete a region.",
				"view <name> - Visualize a region.",
				"tp <name> - Teleport to a region.",
				"list - Browse available regions."
		};
	}

	private Location getHighest(final Location clone) {
		while (clone.getY() < clone.getWorld().getMaxHeight() && clone.getBlock().getType() != Material.AIR && clone.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR)
			clone.add(0, 1, 0);

		return clone;
	}

	private enum Param {
		ADD("add", "create"),
		REMOVE("remove", "rem"),
		VIEW("view", "show", "visualize"),
		TP("tp", "find"),
		LIST("list");

		private final String label;
		private final String[] aliases;

		Param(final String label, final String... aliases) {
			this.label = label;
			this.aliases = aliases;
		}

		private static final Param find(String argument) {
			argument = argument.toLowerCase();

			for (final Param p : values()) {
				if (p.label.toLowerCase().equals(argument))
					return p;

				if (p.aliases != null && Arrays.asList(p.aliases).contains(argument))
					return p;
			}

			return null;
		}
	}

	@Override
	public List<String> tabComplete() {
		final List<String> tab = new ArrayList<>();

		if (args.length == 1) {
			final String param = args[0].toLowerCase();

			for (final Param p : Param.values())
				if (p.toString().toLowerCase().startsWith(param))
					tab.add(p.toString().toLowerCase());
		}

		if (args.length == 2) {
			final String param = args[1].toLowerCase();

			for (final Region region : SimpleRegionData.$().getRegions())
				if (region.getName().startsWith(param))
					tab.add(region.getName());
		}

		return tab;
	}
}