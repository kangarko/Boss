package org.mineacademy.boss.command;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.mineacademy.boss.settings.Localization;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.model.Replacer;

public final class BiomeCommand extends AbstractBossSubcommand {

	public BiomeCommand() {
		super("biome");

		setDescription("Find biome at your location.");
	}

	@Override
	protected void onCommand() {
		checkConsole();

		final Location loc = getPlayer().getLocation();
		final Biome biome = getPlayer().getWorld().getBiome(loc.getBlockX(), loc.getBlockZ());

		tell(Replacer.replaceArray(Localization.Commands.BIOME, "x", loc.getBlockX(), "z", loc.getBlockZ(), "biome", ItemUtil.bountifyCapitalized(biome)));
	}
}