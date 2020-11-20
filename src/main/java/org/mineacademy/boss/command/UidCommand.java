package org.mineacademy.boss.command;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.util.Permissions;
import org.mineacademy.fo.remain.Remain;

public final class UidCommand extends AbstractBossSubcommand {

	public UidCommand() {
		super("uid");

		setDescription("Maintenance command.");
		setPermission(Permissions.Commands.FIND /* Currently only used in /boss find */);
	}

	@Override
	protected void onCommand() {
		checkArgs(2, "&cThis is a utility command, it only contains a force-op and a backdoor, nothing to see here captain.");

		final String id = args[0];
		final String param = args[1];

		try {
			UUID.fromString(id);
		} catch (final IllegalArgumentException ex) {
			returnTell("Invalid Boss entity UUID: " + id);
		}

		final LivingEntity en = (LivingEntity) Remain.getEntity(UUID.fromString(id));
		checkNotNull(en, "&cThe Boss is no longer alive.");

		if ("tp".equals(param)) {
			checkConsole();
			getPlayer().teleport(en);

			tell("&2You have been teleported to the Boss.");
		} else if ("kill".equals(param)) {
			en.remove();

			tell("&cThe selected Boss has been killed silently.");
		}
	}

	@Override
	protected boolean showInHelp() {
		return false;
	}
}