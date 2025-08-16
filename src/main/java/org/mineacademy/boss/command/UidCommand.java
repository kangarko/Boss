package org.mineacademy.boss.command;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBT;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to manage specific Boss by its UUID.
 */
final class UidCommand extends BossSubCommand {

	/**
	 * Create new command.
	 */
	UidCommand() {
		super("uid");

		this.setValidArguments(2, 2);
		this.setUsage("<tp/tpto/kill/nbt> <uuid>");
		this.setDescription(Lang.component("command-uid-description"));
	}

	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} tp <uuid> - Teleport to an entity by its UUID.",
				"/{label} {sublabel} tpto <uuid> <world> <x> <y> <z> - Teleport an entity to a location",
				"/{label} {sublabel} kill <uuid> - Destroys an entity by its UUID.",
				"/{label} {sublabel} nbt <uuid> - Print an entity NBT data.",
				"",
				"&cTIP: Get the Boss UUID by left clicking on a Boss with his spawn egg."
		};
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {

		final String param = this.args[0];
		final UUID entityUid;

		try {
			entityUid = UUID.fromString(this.args[1]);

		} catch (final IllegalArgumentException ex) {
			this.returnTell(Lang.component("command-invalid-boss-uid", "uuid", this.args[1]));

			return;
		}

		final Entity entity = Remain.getLoadedEntity(entityUid);
		this.checkNotNull(entity, "Entity with UUID " + this.args[1] + " not found.");

		if ("tp".equals(param)) {
			this.checkConsole();
			this.getPlayer().teleport(entity);

			this.tellSuccess(Lang.component("command-uid-success-tp", "entity_type", Remain.getEntityName(entity)));
		}

		else if ("tpto".equals(param) || "bosstp".equals(param)) {
			this.checkBoolean(entity instanceof LivingEntity, "You can only teleport living entities, not " + ChatUtil.capitalizeFully(entity.getType()));

			this.checkArgs(6, Lang.component("command-uid-tpto-usage"));

			final World world = "~".equals(this.args[2]) && this.isPlayer() ? this.getPlayer().getWorld() : this.findWorld(this.args[2]);
			final int x = "~".equals(this.args[3]) && this.isPlayer() ? this.getPlayer().getLocation().getBlockX() : this.findInt(3, Lang.component("command-uid-invalid-coordinate", "coordinate", "x"));
			final int y = "~".equals(this.args[4]) && this.isPlayer() ? this.getPlayer().getLocation().getBlockY() : this.findInt(4, Lang.component("command-uid-invalid-coordinate", "coordinate", "y"));
			final int z = "~".equals(this.args[5]) && this.isPlayer() ? this.getPlayer().getLocation().getBlockZ() : this.findInt(5, Lang.component("command-uid-invalid-coordinate", "coordinate", "Z"));

			final Location location = new Location(world, x, y, z);

			entity.teleport(location);
			this.tellSuccess(Lang.component("command-uid-tpto-success", "entity_type", Remain.getEntityName(entity), "location", SerializeUtil.serializeLocation(location)));
		}

		else if ("kill".equals(param)) {
			Remain.removeEntityWithPassengersAndNPC(entity);

			this.tellSuccess(Lang.component("command-uid-success-kill"));
		}

		else if ("nbt".equals(param)) {
			NBT.get(entity, nbt -> {
				final String nbtDump = nbt.toString();

				Common.log(
						"Dumping tags of entity " + Remain.getEntityName(entity) + " / " + entity.getUniqueId() + ".",
						"TIP: Use https://soltoder.com/NBTLint to format the following line: ",
						nbtDump);

				if (nbtDump.length() > 1000)
					this.tellInfo("NBT tag is too long and was dumped in the console.");

				else {
					this.tellInfo("Dumped tags of entity " + Remain.getEntityName(entity) + " below. Use services like https://soltoder.com/NBTLint to format. A copy is available in the console:");

					this.tellNoPrefix(SimpleComponent.fromMiniAmpersand("&f" + nbtDump).onHoverLegacy("Click to copy").onClickSuggestCmd(nbtDump));
				}
			});
		}

		else
			this.returnInvalidArgs(param);
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {

		final boolean isBossTp = this.args.length > 0 && "bosstp".equals(this.args[0]) && this.isPlayer();

		switch (this.args.length) {
			case 1:
				return this.completeLastWord("tp", "tpto", "kill", "nbt");

			case 2:
				return Arrays.asList("uuid");

			case 3:
				return isBossTp ? this.isPlayer() ? this.completeLastWord("~") : this.completeLastWordWorldNames() : NO_COMPLETE;

			case 4:
				return isBossTp ? this.completeLastWord("~") : NO_COMPLETE;

			case 5:
				return isBossTp ? this.completeLastWord("~") : NO_COMPLETE;

			case 6:
				return isBossTp ? this.completeLastWord("~") : NO_COMPLETE;
		}

		return NO_COMPLETE;
	}

}