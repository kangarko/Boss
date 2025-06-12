package org.mineacademy.boss.command;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to show what biome you are in.
 */
final class BiomeCommand extends BossSubCommand {

	/**
	 * Create a new instance
	 */
	BiomeCommand() {
		super("biome");

		this.setValidArguments(0, 1);
		this.setUsage("[player]");
		this.setDescription(Lang.component("command-biome-description"));
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final Player target = this.findPlayerOrSelf(0);
		final Location loc = target.getLocation();
		final Biome biome = Remain.getBiome(loc);

		this.tell(Lang.component("command-biome-success",
				"x", loc.getBlockX(),
				"y", loc.getBlockY(),
				"z", loc.getBlockZ(),
				"biome", ChatUtil.capitalizeFully(ReflectionUtil.getEnumName(biome))));
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWordPlayerNames() : NO_COMPLETE;
	}
}