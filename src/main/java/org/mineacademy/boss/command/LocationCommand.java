package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.mineacademy.boss.model.BossLocation;
import org.mineacademy.boss.tool.LocationTool;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.settings.Lang;

import lombok.RequiredArgsConstructor;

/**
 * The command to manage Boss locations system.
 */
final class LocationCommand extends BossSubCommand {

	public LocationCommand() {
		super("location|loc");

		this.setDescription("Create and manage locations.");
		this.setUsage("<params ...>");
		this.setMinArguments(1);
	}

	@Override
	protected SimpleComponent getMultilineUsage() {
		return Param.generateUsages(this);
	}

	@Override
	protected void onCommand() {
		final LocationTool tool = LocationTool.getInstance();

		final String locationName = this.args.length > 1 ? this.args[1] : null;
		final BossLocation location = locationName != null ? BossLocation.findLocation(locationName) : null;
		final Param param = Param.find(this.args[0]);

		this.checkNoSuchType(param, "param", "{0}", Param.values());

		//
		// Commands without a location.
		//
		if (param == Param.LIST) {
			final Collection<BossLocation> locations = BossLocation.getLocations();

			if (locations.isEmpty())
				this.returnTell("There are no location created yet.");

			final List<SimpleComponent> components = new ArrayList<>();

			for (final BossLocation otherLocation : BossLocation.getLocations()) {

				final String longestText = "&7Position: &2" + SerializeUtil.serializeLocation(otherLocation.getLocation());

				components.add(SimpleComponent
						.fromPlain(" ")

						.appendMiniAmpersand("&8[&4X&8]")
						.onHoverLegacy("Click to remove permanently.")
						.onClickRunCmd("/" + this.getLabel() + " " + this.getSublabel() + " " + Param.REMOVE + " " + otherLocation.getFileName() + " -list")

						.appendPlain(" ")

						.appendMiniAmpersand("&8[&2?&8]")
						.onHoverLegacy("Click to visualize.")
						.onClickRunCmd("/" + this.getLabel() + " " + this.getSublabel() + " " + Param.VIEW + " " + otherLocation.getFileName() + " -list")

						.appendPlain(" ")

						.appendMiniAmpersand("&8[&3>&8]")
						.onHoverLegacy("Click to teleport to.")
						.onClickRunCmd("/" + this.getLabel() + " " + this.getSublabel() + " " + Param.TELEPORT + " " + otherLocation.getFileName() + " -list")

						.appendPlain(" ")

						.appendMiniAmpersand("&7" + otherLocation.getFileName())
						.onHoverLegacy(ChatUtil.center("&fLocation Information", longestText.length() * 2 + longestText.length() / 3),
								longestText));
			}

			new ChatPaginator()
					.setFoundationHeader("Listing " + ChatUtil.capitalize(Lang.numberFormat("case-location", locations.size())))
					.setPages(components)
					.send(this.audience);

			return;
		}

		else if (param == Param.NEW) {
			this.checkConsole();

			tool.giveIfHasnt(this.getPlayer());

			this.tellInfo("To create a location, click a block using the tool which was given to you and type the location name to chat.");
			return;

		} else if (param == Param.TOOL) {
			this.checkConsole();

			tool.giveIfHasnt(this.getPlayer());
			this.tellSuccess("You were given a location tool. Click a block to set.");

			return;

		} else if (param == Param.POINT) {
			this.checkConsole();
			tool.simulateClick(this.getPlayer(), true, this.getPlayer().getLocation());

			return;
		}

		// Require location name for all params below, except view, but when it is provided, check it
		if (param != Param.VIEW || (param == Param.VIEW && locationName != null)) {
			this.checkNotNull(locationName, "Please specify the location name.");
			this.checkNotNull(location, "Location '" + locationName + "' doesn't exists.");
		}

		if (param == Param.REMOVE) {
			BossLocation.removeLocation(location);

			this.tellSuccess("Removed location '&2" + locationName + "&7'.");
		}

		else if (param == Param.VIEW) {
			this.checkConsole();

			if (location != null) {
				location.visualize(this.getPlayer());

				this.tellAndList(location, "Location '&2" + locationName + "&7' now visualized for 10 seconds.");
			}

			else {
				final Location playerLocation = this.getPlayer().getLocation();
				int count = 0;

				for (final BossLocation otherLocation : BossLocation.getLocations())
					if (otherLocation.getLocation().getWorld().equals(playerLocation.getWorld()) && otherLocation.getLocation().distance(playerLocation) < 100) {
						otherLocation.visualize(this.getPlayer());

						count++;
					}

				this.tellSuccess("Visualized " + Lang.numberFormat("case-location", count) + " nearby for 10 seconds.");
			}

		}

		else if (param == Param.TELEPORT) {
			this.getPlayer().teleport(location.getLocation().add(0.5, 1.0, 0.5));

			this.tellAndList(location, "Teleported to location '&2" + locationName + "&7'");
		}

		else
			throw new FoException("Unhandled param " + param);
	}

	/*
	 * Util method to show the given location using the given message.
	 *
	 * We automatically will invoke /{label} location list before showing this message.
	 */
	private void tellAndList(BossLocation location, final String message) {
		if (this.isPlayer() && this.args.length > 2 && "-list".equals(this.args[2]))
			this.getPlayer().performCommand(this.getLabel() + " " + this.getSublabel() + " " + Param.LIST);

		Messenger.info(this.audience, SimpleComponent.fromMiniAmpersand(message));
	}

	@Override
	public List<String> tabComplete() {
		final Param param = this.args.length > 0 ? Param.find(this.args[0]) : null;

		switch (this.args.length) {
			case 1:
				return this.completeLastWord(Param.values());

			case 2:
				return param == Param.LIST || param == Param.NEW ? NO_COMPLETE : this.completeLastWord(BossLocation.getLocationsNames());
		}

		return NO_COMPLETE;
	}

	@RequiredArgsConstructor
	private enum Param {
		NEW("new", "n", "<name>", "Create a new location."),
		TOOL("tool", "t", "", "Get the location creation tool."),
		REMOVE("rem", "rm", "<name>", "Delete a location."),
		VIEW("view", "v", "[name]", "Visualize location if it's less than 100 blocks from you."),
		TELEPORT("tp", null, "<name>", "Teleport to a location's center."),
		LIST("list", "l", "", "Browse available locations."),
		POINT("point", "p", "", "Set your feet to a location.");

		private final String label;
		private final String alias;
		private final String usage;
		private final String description;

		@Nullable
		private static Param find(String argument) {
			argument = argument.toLowerCase();

			for (final Param param : values()) {
				if (param.label.toLowerCase().equals(argument))
					return param;

				if (param.alias != null && param.alias.toLowerCase().equals(argument))
					return param;
			}

			return null;
		}

		public static SimpleComponent generateUsages(LocationCommand command) {
			final Param[] params = Param.values();
			final List<SimpleComponent> usages = new ArrayList<>();

			for (int i = 0; i < params.length; i++) {
				final Param param = params[i];

				// Format usage. Replace [] with &2 and <> with &6
				final String usage = param.usage;
				final String suggestable = "/" + command.getLabel() + " " + command.getSublabel() + " " + param.label;

				usages.add(SimpleComponent
						.fromMiniAmpersand(" " + suggestable + (!usage.isEmpty() ? " " + usage : "") + " - " + param.description)
						.onHoverLegacy("Click to copy.")
						.onClickSuggestCmd(suggestable));
			}

			return SimpleComponent.join(usages);
		}

		@Override
		public String toString() {
			return this.label;
		}
	}
}