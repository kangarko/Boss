package org.mineacademy.boss.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.World;
import org.mineacademy.boss.model.BossRegionRemover;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.OfflineRegionScanner;
import org.mineacademy.fo.settings.Lang;

/**
 * The command to scan offline chunks on disk and remove Bosses from them.
 */
final class ScanCommand extends BossSubCommand {

	/*
	 * Has the console been sent a warning log before running this command,
	 */
	private boolean warned = false;

	/**
	 * Create new command.
	 */
	ScanCommand() {
		super("scan");

		this.setValidArguments(1, 2);
		this.setDescription(Lang.component("command-scan-description"));
		this.setUsage("<world> [boss1|boss2]");
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#getMultilineUsageMessage()
	 */
	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} minigames Blaze|Zombie - Remove all Blaze and Zombie Bosses from all chunks in minigames world.",
				"/{label} {sublabel} survival - DANGEROUS: Remove all Bosses from all chunks in survival world.",
		};
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {
		final World world = this.findWorld(this.args[0]);
		final List<String> bosses = this.args.length == 2 ? Arrays.asList(this.args[1].split("\\|")) : Arrays.asList("*");

		this.checkBoolean(!this.isPlayer(), Lang.component("command-requires-console"));
		this.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "Only Minecraft 1.7.10 and up is supported.");

		if (!this.warned) {
			this.warned = true;

			Common.log(
					Common.chatLine(),
					" WARNING ABOUT REGION MANIPULATION",
					Common.chatLine(),
					" ",
					"You are about to access and change your regions saved",
					"on the disk. *Every* such operation is potentially",
					"dangerous and requires special attention.",
					" ",
					"PLEASE MAKE SURE THAT,",
					"1) You have backed up your world file:",
					"  " + world.getWorldFolder().getAbsolutePath(),
					" ",
					"2) No other region-related operation is running:",
					"   such as WorldEdit, VoxelSniper or related.",
					" ",
					"All players will be kicked for safety. The operation may",
					"take *MINUTES TO HOURS* depending on your world size",
					"and hardware. For large servers, run this overnight.",
					" ",
					"Each file is saved immediatelly after processing, so",
					"should server stop, data loss *should* not occur.",
					" ",
					"** If Spigot will complain about server not responding, **",
					"** ignore this (we must run this on the main thread). **",
					" ",
					"I TAKE NO RESPONSIBILITY FOR YOUR BOSSES GETTING REMOVED",
					"DATA CORRUPTION, SPIT COFFEE, SPIGOT VS PAPERSPIGOT WAR ETC.",
					" ",
					Common.chatLine(),
					" > Run the command again to proceed...",
					" > Estimated duration (may get longer): " + TimeUtil.formatTimeGeneric(OfflineRegionScanner.getEstimatedWaitTimeSec(world)),
					Common.chatLine());

			return;
		}

		BossRegionRemover.launch(bosses, world);
	}

	@Override
	public List<String> tabComplete() {

		switch (this.args.length) {
			case 1:
				return this.completeLastWordWorldNames();
			case 2:
				return this.completeLastWordBossNames();
		}

		return NO_COMPLETE;
	}
}