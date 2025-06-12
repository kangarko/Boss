package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.Location;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.settings.Lang;

public abstract class BossEggAbstractCommand extends BossSubCommand {

	BossEggAbstractCommand(String sublabel) {
		super(sublabel);
	}

	/*
	 * Parse coordinate from the given index, either X, Y, or Z for indexes 1, 2 and 3 respectivelly.
	 */
	protected final int parseCoordinate(final int index) throws CommandException {
		final String raw = this.args[index];

		if ("~".equals(raw)) {
			this.checkBoolean(this.isPlayer(), Lang.component("command-spawn-cannot-autodetect-location"));
			final Location location = this.getPlayer().getLocation();

			switch (index) {
				case 1:
					return location.getBlockX();
				case 2:
					return location.getBlockY();
				case 3:
					return location.getBlockZ();
			}
		}

		return this.findNumber(index, Lang.component("command-spawn-invalid-position", "position", raw));
	}

	@Override
	public final List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWordWorldNames();
			case 2:
			case 3:
			case 4:
				return this.isPlayer() ? this.completeLastWord("~") : NO_COMPLETE;
			case 5:
				return this.completeLastWord(Boss.getBossesNames(), "random");
		}

		return NO_COMPLETE;
	}
}
