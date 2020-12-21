package org.mineacademy.boss;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.mineacademy.boss.api.BossSkillRegistry;
import org.mineacademy.boss.hook.BossHeroesHook;
import org.mineacademy.boss.hook.GriefPreventionHook;
import org.mineacademy.boss.hook.SilkSpawnersHook;
import org.mineacademy.boss.hook.StackMobListener;
import org.mineacademy.boss.listener.BossSpawnerListener;
import org.mineacademy.boss.listener.EntityListener;
import org.mineacademy.boss.listener.FreezeListener;
import org.mineacademy.boss.listener.PlayerListener;
import org.mineacademy.boss.listener.SpawningListener;
import org.mineacademy.boss.menu.MenuToolsBoss;
import org.mineacademy.boss.model.BossCommandGroup;
import org.mineacademy.boss.model.BossManager;
import org.mineacademy.boss.model.BossPlayer;
import org.mineacademy.boss.model.task.BossKeepTask;
import org.mineacademy.boss.model.task.BossRetargetTask;
import org.mineacademy.boss.model.task.BossSkillTask;
import org.mineacademy.boss.model.task.BossTimedSpawnTask;
import org.mineacademy.boss.settings.Localization;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.storage.SimplePlayerData;
import org.mineacademy.boss.storage.SimpleSpawnerData;
import org.mineacademy.boss.storage.SimpleTagData;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.model.SpigotUpdater;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlStaticConfig;

/**
* The main class for the Boss plugin.
*/
public final class BossPlugin extends SimplePlugin {

	/**
	 * Store temporary players' data until BOSS restarts.
	 */
	private static final StrictMap<String, BossPlayer> playerCache = new StrictMap<>();

	/**
	 * Class responsible for loading, saving, and creating Bosses.
	 */
	private final BossManager bossManager = new BossManager();

	/**
	 * The main /boss command, capable of handling subcommands such as /boss region.
	 */
	private final BossCommandGroup bossCommand = new BossCommandGroup();

	@Override
	protected String[] getStartupLogo() {
		return new String[] {
				"&e______                   ",
				"&e(____  \\                 ",
				"&e ____)  ) ___   ___  ___ ",
				"&e|  __  ( / _ \\ /___)/___)",
				"&e| |__)  ) |_| |___ |___ |",
				"&6|______/ \\___/(___/(___/ ",
				" "
		};
	}

	@Override
	protected void onPluginStart() {
		MenuToolsBoss.getInstance();

		if (!Remain.hasScoreboardTags())
			SimpleTagData.$();

		SimplePlayerData.$();

		registerEvents(new PlayerListener());
		registerEvents(new EntityListener());
		registerEvents(new SpawningListener());
		registerEvents(new BossSpawnerListener());
		registerEvents(new FreezeListener());

		if (Common.doesPluginExist("Heroes"))
			BossHeroesHook.setEnabled(true);

		if (Common.doesPluginExist("GriefPrevention"))
			GriefPreventionHook.setEnabled(true);

		Common.ADD_TELL_PREFIX = true;

		// Delay after start because we check the world, so Multiverse must load it
		// first
		Common.runLater(SimpleSpawnerData::$);

		Common.runLater(10, () -> SimpleTagData.$().clear());

		Common.log(
				" ",
				"Tutorial:",
				"&6https://github.com/kangarko/Boss/wiki",
				" ",
				"Get help:",
				"&6https://github.com/kangarko/Boss/issues",
				"&8" + Common.consoleLineSmooth());

		if (Settings.TimedSpawning.ENABLED && (Common.doesPluginExistSilently("Top") || Common.doesPluginExistSilently("TopLite") || Common.doesPluginExistSilently("MassiveLag")))
			Common.runLaterAsync(10, () -> Common.logFramed(false,
					"[Boss] Notice for Anti-Lag Plugins",
					" ",
					"Boss is a greatly optimized plugin, however timed",
					"spawning must use the main thread for safety.",
					"Depending on your settings, this may be shown",
					"in your Top / MassiveLag. You can ignore this."));
	}

	@Override
	protected void onPluginReload() {
		SimpleSpawnerData.$().save();
	}

	/**
	 * Start the parts of the plugin that support /boss reload or /reload function.
	 */
	@Override
	protected void onReloadablesStart() {
		BossSkillRegistry.registerDefaults();

		Common.runLater(() -> bossManager.loadBosses());

		Common.runTimer(10, 20, new BossSkillTask());
		Common.runTimer(10, Settings.Fight.Target.DELAY.getTimeTicks(), new BossRetargetTask());

		if (Settings.TimedSpawning.ENABLED)
			Common.runTimer(Settings.TimedSpawning.DELAY.getTimeTicks(), new BossTimedSpawnTask());

		if (Settings.RegionKeep.ENABLED)
			Common.runTimer(Settings.RegionKeep.PERIOD.getTimeTicks(), new BossKeepTask());

		if (Common.doesPluginExist("SilkSpawners"))
			registerEvents(new SilkSpawnersHook());

		registerEventsIf(new StackMobListener(), Common.doesPluginExist("StackMob"));
	}

	public static BossPlayer getDataFor(final Player player) {
		Valid.checkNotNull(player, "Player = null");

		BossPlayer cache = playerCache.get(player.getName());

		if (cache == null) {
			cache = new BossPlayer();

			playerCache.put(player.getName(), cache);

		}

		return cache;
	}

	public static BossManager getBossManager() {
		return ((BossPlugin) SimplePlugin.getInstance()).bossManager;
	}

	@Override
	public List<Class<? extends YamlStaticConfig>> getSettings() {
		return Arrays.asList(Settings.class, Localization.class);
	}

	@Override
	public int getFoundedYear() {
		return 2017; // 17.07
	}

	@Override
	public SimpleCommandGroup getMainCommand() {
		return bossCommand;
	}

	@Override
	public SpigotUpdater getUpdateCheck() {
		return new SpigotUpdater(46497);
	}

	@Override
	public V getMinimumVersion() {
		return V.v1_8;
	}
}
