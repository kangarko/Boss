package org.mineacademy.boss.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.model.BossRegionScanner;
import org.mineacademy.boss.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.TabUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.OfflineRegionScanner;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

public final class ScannerCommand extends AbstractBossSubcommand {

	private boolean warned = false;

	public ScannerCommand() {
		super("scanner|scan");

		setDescription("Remove Bosses from unloaded chunks.");
		setUsage("<world> [bosses ...]");
		setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		checkBoolean(!(sender instanceof Player), "&cThis command requires execution from the console.");
		checkBoolean(MinecraftVersion.newerThan(V.v1_7), "Only MC 1.8.8 and up are supported (for safety). Please notify the developer to test out if the new MC version is safe to use.");

		final String worldRaw = args[0];
		final String[] bosses = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[] {};

		final World world = Bukkit.getWorld(worldRaw);
		checkNotNull(world, "World " + worldRaw + " does not exists. Available: " + StringUtils.join(getWorlds(), ", "));

		if (!warned) {
			warned = true;

			returnTell(
					Common.consoleLine(),
					" WARNING ABOUT REGION MANIPULATION",
					Common.consoleLine(),
					" ",
					"You are about to access and change your regions saved",
					"on the disk. *Every* such operation is potentially",
					"dangerous and requires special attention.",
					" ",
					"PLEASE MAKE SURE THAT,",
					"1) You have manually backed up your world file:",
					"  (if unsure, just clone the world folder to a different name)",
					"  " + world.getWorldFolder().getAbsolutePath(),
					" ",
					"2) No other region-related operation is running,",
					"   such as WorldEdit, VoxelSniper or related.",
					" ",
					"All players will be kicked for safety. The operation may",
					"take *MINUTES TO HOURS* depending on your world size",
					"and hardware.",
					" ",
					"Each file is saved immediatelly after processing, so",
					"if server stops forcefully, data loss *should* not occur.",
					" ",
					"** If Spigot will complain about server not responding **",
					"** you can safely ignore this (we must run this on the main thread). **",
					" ",
					SimplePlugin.getNamed().toUpperCase() + " TAKES NO RESPONSIBILITY FOR YOUR DATA",
					"AND PROVIDES THIS FUNCTION WITHOUT ANY WARRANTY.",
					" ",
					Common.consoleLine(),
					" > Run the command again to proceed...",
					" > Estimated duration: " + TimeUtil.formatTimeGeneric(OfflineRegionScanner.getEstimatedWaitTimeSec(world)),
					Common.consoleLine());
		}

		launch(Arrays.asList(bosses), world);
	}

	private static long now = 0;

	private void launch(final List<String> bossesToRemove, final World world) {
		Common.log("1/4 Kicking all players & enabling whitelist ...");

		for (final Player pl : Remain.getOnlinePlayers())
			pl.kickPlayer("Kicked due to server maintenance");

		Bukkit.setWhitelist(true);

		final BossRegionScanner cleaner = new BossRegionScanner();

		Common.log("2/4 Running region scan ...");
		now = System.currentTimeMillis();
		cleaner.launch(bossesToRemove, world);
	}

	public static void finish(final World w) {
		Common.log("3/4 Saving world ...");
		w.save();

		Common.log("4/4 Disabling whitelist ...");
		Bukkit.setWhitelist(false);

		Common.log(Common.consoleLine());
		Common.log("Operation finished in " + TimeUtil.formatTimeGeneric((int) (System.currentTimeMillis() - now) / 1000));
		Common.log(Common.consoleLine());

		now = 0;
	}

	@Override
	public List<String> tabComplete() {
		if (!PlayerUtil.hasPerm(sender, Permissions.Commands.SCANNER))
			return null;

		if (args.length == 1)
			return TabUtil.complete(args[0], getWorlds());

		else if (args.length > 1)
			return BossPlugin.getBossManager().getBossesAsList().getSource();

		return new ArrayList<>();
	}

	private List<String> getWorlds() {
		return Common.convert(Bukkit.getWorlds(), World::getName);
	}
}