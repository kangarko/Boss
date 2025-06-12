package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.entity.Player;
import org.mineacademy.fo.menu.model.ItemCreator;

/**
 * The command to get player skulls so you can place them at Boss heads.
 */
final class SkullCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	SkullCommand() {
		super("skull");

		this.setValidArguments(2, 3);
		this.setDescription("Get a skull of a player. You can place it at a Boss head.");
		this.setUsage("<name/url/uuid/base64> <value> [player]");
	}

	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"&c&lTIP: &r&cSee <click:open_url:'https://mcheads.ru/en/alphabet/vfro'>https://mcheads.ru/en/alphabet/vfro</click> for an example",
				"&cskull with information how to get its data for",
				"&cdifferent server versions.",
				"",
				"&c&clTIP 2: &r&cRun this command from console to paste long base64 strings."
		};
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {

		final String param = this.args[0].toLowerCase();
		final String value = this.args[1];
		final Player target = this.findPlayerOrSelf(2);

		final ItemCreator skull = ItemCreator.fromPlayerSkull();

		if (param.equals("name"))
			skull.skullOwner(value);

		else if (param.equals("url")) {
			this.checkBoolean(value.startsWith("http://") || value.startsWith("https://"), "Invalid URL: " + value);

			skull.skullUrl(value);

		} else if (param.equals("base64"))
			skull.skullBase64(value);

		else if (param.equals("uuid"))
			skull.skullUid(this.findUUID(1));

		else
			this.returnInvalidArgs(param);

		try {
			skull.give(target);

		} catch (final StringIndexOutOfBoundsException ex) {
			this.returnTell("&cInvalid " + param + " value: " + ex.getMessage());
		}

		this.tellSuccess(target.getName() + " got skull from " + param + " '" + value + "'");
	}

	@Override
	public List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWord("uuid", "name", "url", "base64");
			case 2: {
				final String param = this.args[0].toLowerCase();

				if (param.equals("name"))
					return this.completeLastWordPlayerNames();

				else if (param.equals("url"))
					return this.completeLastWord("http://", "https://");

				else if (param.equals("uuid"))
					return this.completeLastWord(this.isPlayer() ? this.getPlayer().getUniqueId().toString() : "");

				else if (param.equals("base64"))
					return this.completeLastWord("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDY5NzZhOGFiNzFjNDY3OTU2MDFhNzFhNjY4OTQ0YjFlODU1NzI0YjlmZjMxYzc0MzE3NmY2YTRhNTViMmViNiJ9fX0");
			}

			case 3:
				return this.completeLastWordPlayerNames();
		}

		return NO_COMPLETE;
	}
}