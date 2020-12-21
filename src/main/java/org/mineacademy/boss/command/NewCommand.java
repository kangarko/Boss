package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.entity.EntityType;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.model.BossManager;
import org.mineacademy.boss.util.BossUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.TabUtil;

public final class NewCommand extends AbstractBossSubcommand {

	public NewCommand() {
		super("new");

		setDescription("Create a new boss.");
		setUsage("<name> <type>");
		setMinArguments(2);
	}

	@Override
	protected void onCommand() {
		final String name = args[0];

		checkBoolean(name.length() > 2 && name.length() < 31, "Name must be between 3 and 30 characters long.");
		checkArgs(2, "Please specify the type of the Boss. Available: " + BossManager.getValidTypesFormatted());

		if (!mayCreate(getPlayer(), name))
			return;

		final String typeRaw = args[1];
		final EntityType type = ReflectionUtil.lookupEnumSilent(EntityType.class, typeRaw.toUpperCase());
		checkBoolean(type != null && BossManager.getValidTypes().contains(type), "&cInvalid Boss type '" + typeRaw + "'. Available: " + BossManager.getValidTypesFormatted());

		final Boss boss = getBosses().createBoss(type, name);

		tell("&7A new Boss " + boss.getName() + " has been &2created&7!");
	}

	public static boolean mayCreate(final Conversable player, final String input) {

		@Deprecated
		class $ {
			private void say(final Object obj, final String message) {
				if (obj instanceof Conversable)
					Common.tellLaterConversing(1, (Conversable) obj, message);
				else
					Common.tell((CommandSender) obj, message);
			}
		}

		final $ $ = new $();

		if (input.length() > 30) {
			$.say(player, "&cName may only contain 30 letters (you can specify a longer alias afterwards).");

			return false;
		}

		if (input.contains(" ")) {
			$.say(player, "&cName may not contains spaces. You can use them later in Boss' alias.");

			return false;
		}

		if (input.contains("&")) {
			$.say(player, "&cName may not contains colors. You can use them later in Boss' alias.");

			return false;
		}

		if (!BossUtil.isValidFileName(input)) {
			$.say(player, "Boss file name must only contain A-Z letters and numbers. You can set special characters, spaces and accents later in the menu.");

			return false;
		}

		if (BossPlugin.getBossManager().findBoss(input) != null) {
			$.say(player, "&cBoss named '" + input + "' already exists.");

			return false;
		}

		return true;
	}

	@Override
	public List<String> tabComplete() {
		if (args.length == 2)
			return TabUtil.complete(args[1], BossManager.getValidTypes().toArray());

		return new ArrayList<>();
	}
}