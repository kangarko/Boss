package org.mineacademy.boss;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.mineacademy.boss.hook.CitizensHook;
import org.mineacademy.boss.hook.GriefPreventionHook;
import org.mineacademy.boss.hook.HeroesHook;
import org.mineacademy.boss.hook.LandsHook;
import org.mineacademy.boss.hook.WorldGuardHook;
import org.mineacademy.boss.listener.ChunkListener;
import org.mineacademy.boss.listener.ThirdPartiesListener;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossLocation;
import org.mineacademy.boss.model.BossPlaceholders;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.skill.BossSkill;
import org.mineacademy.boss.skill.SkillArrow;
import org.mineacademy.boss.skill.SkillBomb;
import org.mineacademy.boss.skill.SkillCommands;
import org.mineacademy.boss.skill.SkillCommandsNearby;
import org.mineacademy.boss.skill.SkillCommandsTarget;
import org.mineacademy.boss.skill.SkillConfuse;
import org.mineacademy.boss.skill.SkillDisarm;
import org.mineacademy.boss.skill.SkillEnderman;
import org.mineacademy.boss.skill.SkillFireball;
import org.mineacademy.boss.skill.SkillFreeze;
import org.mineacademy.boss.skill.SkillIgnite;
import org.mineacademy.boss.skill.SkillLightning;
import org.mineacademy.boss.skill.SkillMinions;
import org.mineacademy.boss.skill.SkillPotion;
import org.mineacademy.boss.skill.SkillStealLife;
import org.mineacademy.boss.skill.SkillTeleport;
import org.mineacademy.boss.skill.SkillThrow;
import org.mineacademy.boss.spawn.SpawnRule;
import org.mineacademy.boss.task.TaskBehavior;
import org.mineacademy.boss.task.TaskDistarget;
import org.mineacademy.boss.task.TaskFrozenPlayers;
import org.mineacademy.boss.task.TaskRegionEnter;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.HealthBarUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.BukkitPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.visual.VisualizedRegion;

/**
 * The main class for Boss, recoded August 2024
 */
public final class BossPlugin extends BukkitPlugin {

	/**
	 * @see org.mineacademy.fo.platform.BukkitPlugin#getStartupLogo()
	 */
	@Override
	public String[] getStartupLogo() {
		return new String[] {
				"&c ___   ___   __   __  ",
				"&c| |_) / / \\ ( (` ( (` ",
				"&4|_|_) \\_\\_/ _)_) _)_) ",
				"",
		};
	}

	/**
	 * @see org.mineacademy.fo.platform.BukkitPlugin#onPluginLoad()
	 */
	@Override
	protected void onPluginLoad() {
		final String platform = Platform.getPlatformName();

		if (platform.contains("Luminol") || platform.contains("Folia"))
			throw new IllegalStateException("You cannot run Boss plugin on " + platform + ". Please use Paper, Spigot or Purpur.");

		// Register skills
		BossSkill.registerSkill("Arrow", SkillArrow.class);
		BossSkill.registerSkill("Bomb", SkillBomb.class);
		BossSkill.registerSkill("Confuse", SkillConfuse.class);
		BossSkill.registerSkill("Disarm", SkillDisarm.class);
		BossSkill.registerSkill("Enderman", SkillEnderman.class);
		BossSkill.registerSkill("Ignite", SkillIgnite.class);
		BossSkill.registerSkill("Fireball", SkillFireball.class);
		BossSkill.registerSkill("Freeze", SkillFreeze.class);
		BossSkill.registerSkill("Lightning", SkillLightning.class);
		BossSkill.registerSkill("Minions", SkillMinions.class);
		BossSkill.registerSkill("Potions", SkillPotion.class);
		BossSkill.registerSkill("StealLife", SkillStealLife.class);
		BossSkill.registerSkill("Teleport", SkillTeleport.class);
		BossSkill.registerSkill("Throw", SkillThrow.class);
		BossSkill.registerSkill("Commands", SkillCommands.class);
		BossSkill.registerSkill("Commands_Nearby", SkillCommandsNearby.class);
		BossSkill.registerSkill("Commands_Target", SkillCommandsTarget.class);

		// Set how to get the region for tools
		DiskRegion.setCreatedPlayerRegionGetter(player -> PlayerCache.from(player).getCreatedRegion());
		DiskRegion.setCreatedPlayerRegionResetter(player -> PlayerCache.from(player).setCreatedRegion(new VisualizedRegion()));

		if (MinecraftVersion.atLeast(V.v1_16) && Platform.isPluginInstalled("WorldGuard"))
			WorldGuardHook.init();
	}

	/**
	 * @see org.mineacademy.fo.platform.BukkitPlugin#onPluginStart()
	 */
	@Override
	protected void onPluginStart() {

		// Third party integrations
		if (Platform.isPluginInstalled("Heroes"))
			HeroesHook.setEnabled(true);

		if (Platform.isPluginInstalled("GriefPrevention"))
			GriefPreventionHook.setEnabled(true);

		if (Platform.isPluginInstalled("Lands"))
			LandsHook.setEnabled(true);

		if (HookManager.isCitizensLoaded())
			CitizensHook.registerTraits();

		// Register events
		ThirdPartiesListener.registerEvents();

		ChunkListener.loadFromFile();

		this.registerEvents(new ChunkListener());

		for (final World world : Bukkit.getWorlds())
			if (world.getName().contains("."))
				Common.warning("Boss does not support worlds with dots in their name, please change the . to _ in world: " + world.getName());

		// Load data
		Platform.runTask(() -> {
			Boss.loadBosses();

			this.loadData();
		});

		// Run timer tasks
		Platform.runTaskTimer(5, TaskFrozenPlayers.getInstance());
		Platform.runTaskTimer(20, TaskBehavior.getInstance());
		Platform.runTaskTimer(20 * 2, new TaskDistarget());

		if (Settings.REGISTER_REGIONS)
			Platform.runTaskTimer(20, new TaskRegionEnter());

		// Save delay and other values once per 5 minutes not to trash disk data
		Platform.runTaskTimer(20 * 60 * 5, () -> {
			for (final SpawnRule rule : SpawnRule.getRules())
				rule.save();
		});

		// Register variables
		Variables.addExpansion(BossPlaceholders.getInstance());

		// Localize health bar
		HealthBarUtil.setPrefix(Settings.Fighting.HealthBar.PREFIX);
		HealthBarUtil.setSuffix(Settings.Fighting.HealthBar.SUFFIX);
		HealthBarUtil.setRemainingColor(Settings.Fighting.HealthBar.COLOR_REMAINING);
		HealthBarUtil.setTotalColor(Settings.Fighting.HealthBar.COLOR_TOTAL);
		HealthBarUtil.setDeadColor(Settings.Fighting.HealthBar.COLOR_DEAD);

		if (HookManager.isWorldGuardLoaded())
			Common.log("WorldGuard detected. You can set the 'boss-target' flag to false to stop Bosses automatically targeting players inside regions.");

		Common.log(
				"",
				"&fTutorial:",
				"&6https://docs.mineacademy.org/boss",
				"",
				"&fGet help:",
				"&6https://github.com/kangarko/boss/issues",
				"");
	}

	/**
	 * @see org.mineacademy.fo.platform.BukkitPlugin#onPluginPreReload()
	 */
	@Override
	protected void onPluginPreReload() {
		for (final Boss boss : Boss.getBosses())
			boss.save();
	}

	@Override
	protected void onPluginReload() {
		this.loadData();
	}

	private void loadData() {
		BossLocation.loadLocations();
		SpawnRule.loadRules();
	}

	/**
	 * @see org.mineacademy.fo.platform.BukkitPlugin#onPluginStop()
	 */
	@Override
	protected void onPluginStop() {
		for (final Boss boss : Boss.getBosses())
			boss.save();

		TaskFrozenPlayers.getInstance().unfreezeAll();

		ChunkListener.saveToFile();
	}

	/**
	 * @see org.mineacademy.fo.platform.BukkitPlugin#getSentryDsn()
	 */
	@Override
	public String getSentryDsn() {
		return "https://9a2bf9f9e4308ddeea77e79b269e2206@o4508048573661184.ingest.us.sentry.io/4508052202913792";
	}

	/**
	 * @see org.mineacademy.fo.platform.BukkitPlugin#getFoundedYear()
	 */
	@Override
	public int getFoundedYear() {
		return 2017;
	}

	@Override
	public int getBuiltByBitId() {
		return 21619;
	}

	@Override
	public String getBuiltByBitSharedToken() {
		return "UYl2VzxD9xaAiDVCnLTxTCDjD4vDuPr7";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get the instance of Boss
	 *
	 * @return
	 */
	public static BossPlugin getInstance() {
		return (BossPlugin) BukkitPlugin.getInstance();
	}
}
