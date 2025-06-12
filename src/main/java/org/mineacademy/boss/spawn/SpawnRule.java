package org.mineacademy.boss.spawn;

import java.lang.reflect.Constructor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossSpawnReason;
import org.mineacademy.boss.model.BossSpawnResult;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonConversation;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.ConfigItems;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a spawn rule concept class for Boss appearance.
 */
@Getter
public abstract class SpawnRule extends YamlConfig {

	/**
	 * The loaded spawning rules
	 */
	private static final ConfigItems<SpawnRule> loadedRules = ConfigItems.fromFolder("spawnrules", SpawnRule.class);

	/**
	 * The type of this spawn rule.
	 */
	private SpawnRuleType type;

	/**
	 * Is this spawning rule enabled?
	 */
	private boolean enabled;

	/**
	 * Which Bosses should this rule be applied to?
	 */
	private IsInList<String> bosses;

	/**
	 * The delay between each rule execution
	 */
	private SimpleTime delay;

	/**
	 * Which week days this rule works
	 */
	private IsInList<DayOfWeek> whichDays;

	/**
	 * Which year months in our galaxy this rule should work
	 */
	private IsInList<Month> whichMonths;

	/**
	 * What time in Minecraft must the server have for this rule to fire?
	 */
	private RangedValue minecraftTime;

	/**
	 * What light level to spawn Bosses in
	 */
	private RangedValue lightLevel;

	/**
	 * Must it rain for the rule to fire?
	 */
	private boolean rainingRequired;

	/**
	 * Must it thunder for the rule to fire?
	 */
	private boolean thunderRequired;

	/**
	 * What is the chance for this rule to fire? From 0.00 to 1.00.
	 */
	private double chance;

	/**
	 * Timestamp when the task was run last
	 */
	private long lastExecuted;

	/**
	 * Create new spawn rule
	 *
	 * @param name
	 * @param type
	 */
	protected SpawnRule(@NonNull String name, @Nullable SpawnRuleType type) {
		this.type = type;

		this.setHeader(" -------------------------------------------------------------------------------------------------\n" +
				" This is a spawn rule configuration file. We strongly recommend only editing it using our GUI.\n" +
				" MineAcademy.org is not responsible for data loss or thermonuclear war from mistakes in your edit.\n" +
				" -------------------------------------------------------------------------------------------------");

		this.loadAndExtract(NO_DEFAULT, "spawnrules/" + name + ".yml");
	}

	/* ------------------------------------------------------------------------------- */
	/* Data */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.org.mineacademy.fo.settings.YamlConfig#onLoad()
	 */
	@Override
	protected void onLoad() {

		// Type is not null when Spawn Rule is being created
		if (this.type == null)
			this.type = this.get("Type", SpawnRuleType.class);

		this.enabled = this.getBoolean("Enabled", true);
		this.bosses = this.getIsInList("Bosses", String.class);
		this.delay = this.getTime("Delay", SimpleTime.fromString("0"));
		this.whichDays = this.getIsInList("Days", DayOfWeek.class);
		this.whichMonths = this.getIsInList("Months", Month.class);
		this.minecraftTime = this.get("Minecraft_Time", RangedValue.class);
		this.lightLevel = this.get("Light_Level", RangedValue.class);
		this.rainingRequired = this.getBoolean("Rain", false);
		this.thunderRequired = this.getBoolean("Thunder", false);
		this.chance = this.getDouble("Chance", 1D);
		this.lastExecuted = this.getLong("Last_Executed", -1L);

		if (this.whichDays.getList().isEmpty())
			this.whichDays = IsInList.fromList(Arrays.asList(DayOfWeek.values()));

		if (this.whichMonths.getList().isEmpty())
			this.whichMonths = IsInList.fromList(Arrays.asList(Month.values()));
	}

	/**
	 * Called when we should save data to file
	 */
	@Override
	public void onSave() {
		this.set("Type", this.type);
		this.set("Enabled", this.enabled);
		this.set("Bosses", this.bosses);
		this.set("Delay", this.delay);
		this.set("Days", this.whichDays.getList().isEmpty() ? IsInList.fromStar() : this.whichDays);
		this.set("Months", this.whichMonths.getList().isEmpty() ? IsInList.fromStar() : this.whichMonths);
		this.set("Minecraft_Time", this.minecraftTime);
		this.set("Light_Level", this.lightLevel);
		this.set("Rain", this.rainingRequired);
		this.set("Thunder", this.thunderRequired);
		this.set("Chance", this.chance);
		this.set("Last_Executed", this.lastExecuted);
	}

	/* ------------------------------------------------------------------------------- */
	/* Ticking */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return if we should check delay in the canRun method
	 *
	 * @return
	 */
	protected boolean checkLastExecuted() {
		return true;
	}

	/**
	 * Perform this rule's logic.
	 */
	protected abstract void onTick(SpawnData data);

	/**
	 * Return if we can run this run rule now
	 *
	 * @return
	 */
	protected boolean canRun() {

		// Respawn rules are ticked in a runnable task
		if (this.checkLastExecuted())
			if (this.lastExecuted != -1 && System.currentTimeMillis() - this.lastExecuted + 1 < this.delay.getTimeSeconds() * 1000) {
				Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to last executed " + (System.currentTimeMillis() - this.lastExecuted) / 1000 + "s ago and delay is " + this.delay.getTimeSeconds() + "s");

				return false;
			}

		if (!RandomUtil.chanceD(this.getChance())) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to chance did not pass: " + this.getChance() * 100 + "%");

			return false;
		}

		final LocalDate date = TimeUtil.getCurrentDate();

		if (!this.getWhichDays().contains(date.getDayOfWeek())) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to real-life week day (" + date.getDayOfWeek() + ") out of range: " + this.getWhichDays());

			return false;
		}

		if (!this.getWhichMonths().contains(date.getMonth())) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to real-life month (" + date.getMonth() + ") out of range: " + this.getWhichMonths());

			return false;
		}

		return true;
	}

	/**
	 * Return if the rule can be run at the given location
	 *
	 * @param location
	 * @return
	 */
	protected boolean canRun(Location location) {
		final World world = location.getWorld();

		if (!this.enabled) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running since it is disabled");

			return false;
		}

		if (!location.getChunk().isLoaded()) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to chunk being unloaded");

			return false;
		}

		if (this.getMinecraftTime() != null && !this.getMinecraftTime().isInRangeLong(world.getTime())) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to world " + world.getName() + " not in Minecraft time " + this.getMinecraftTime() + " but in " + world.getTime());

			return false;
		}

		if (this.getLightLevel() != null && !this.getLightLevel().isInRangeLong(location.getBlock().getLightLevel())) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to not in light levels " + this.getLightLevel() + " but in " + location.getBlock().getLightLevel());

			return false;
		}

		if (this.isRainingRequired() && !world.hasStorm()) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to storm required but no storm");

			return false;
		}

		if (this.isThunderRequired() && !world.isThundering()) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to thunder required but no thunder");

			return false;
		}

		return true;
	}

	/**
	 * Return if this spawn rule can run for the given boss
	 *
	 * @param boss
	 * @param data
	 * @return
	 */
	protected boolean canRun(Boss boss, SpawnData data) {
		return true;
	}

	/**
	 * Attempts spawning at the given location
	 *
	 * @param location
	 * @param data
	 */
	public final void spawn(Location location, SpawnData data) {
		final EntityType matchingType = data.getMatchingType();
		boolean success = false;

		for (final Boss boss : Boss.getBosses()) {
			if (matchingType != null && matchingType != boss.getType())
				continue;

			if (this.getBosses().contains(boss.getName()) && this.canRun(boss, data)) {
				location.setPitch(RandomUtil.nextIntBetween(-90, 90));
				location.setYaw(RandomUtil.nextIntBetween(0, 360));

				final Tuple<BossSpawnResult, SpawnedBoss> spawned = boss.spawn(location, BossSpawnReason.SPAWN_RULE, this);

				if (spawned.getKey() == BossSpawnResult.SUCCESS) {
					data.getBosses().add(spawned.getValue());

					Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Spawned Boss " + boss.getName() + " at " + SerializeUtil.serializeLocation(spawned.getValue().getSpawnLocation()));

					success = true;
				}
			}
		}

		if (success)
			this.lastExecuted = System.currentTimeMillis();
	}

	/* ------------------------------------------------------------------------------- */
	/* Menu system */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Compile a list of buttons to activate
	 *
	 * @param parent
	 * @return
	 */
	public List<Button> getButtons(Menu parent) {
		final List<Button> buttons = new ArrayList<>();

		buttons.add(new ButtonMenu(new EditBossesMenu(parent), ItemCreator.from(
				CompMaterial.CHEST,
				"&6Select Bosses",
				"",
				"Current: &f" + (this.bosses.getList().isEmpty() ? "&cPlease choose!" : Common.join(this.bosses)),
				"",
				"Choose which Bosses",
				"will this spawn rule",
				"apply to.")
				.glow(this.bosses.getList().isEmpty())));

		buttons.add(Button.makeSimple(ItemCreator.from(
				CompMaterial.GOLD_INGOT,
				"&dDelay",
				"",
				"Current: &f" + (this.delay.getTimeSeconds() == 0 ? "&6no delay" : this.delay),
				"",
				"Choose how much to wait",
				(this instanceof SpawnRuleRespawn ? "before Boss respawns after death" : "before running the rule"),
				"again. This works together",
				"with other options.",
				"",
				"If no delay is set and this",
				"rule is periodical, we default",
				"to &c1 second&7."),
				player -> {
					new SimpleStringPrompt("Enter how long to wait between running this task again. Current: '" + this.getDelay() + "'.") {

						@Override
						protected boolean isInputValid(ConversationContext context, String input) {

							try {
								final SimpleTime newValue = SimpleTime.fromString(input);

								if (newValue.getTimeTicks() >= 0) {
									SpawnRule.this.setDelay(newValue);

									return true;
								}

							} catch (final Throwable t) {
								// see getFailedValiationText
							}

							return false;
						}

						@Override
						protected String getFailedValidationText(ConversationContext context, String invalidInput) {
							return "Invalid time, enter a human readable format such as '3 seconds', or 0 for no delay.";
						}

					}.show(player);
				}));

		buttons.add(new ButtonMenu(new SelectDaysMonthsMenu(parent, "on", this.whichDays, DayOfWeek.values(), edited -> this.setWhichDays((IsInList<DayOfWeek>) edited)), ItemCreator.from(
				CompMaterial.ENDER_PEARL,
				"Select Days",
				"",
				"Current: &f" + (this.whichDays.isEntireList() ? "Every day" : "\n &8- &f" + Common.join(this.whichDays, "\n &8- &f", val -> ChatUtil.capitalize(val.toString().toLowerCase()))),
				"",
				"Choose which days",
				"of the real-life week",
				"this rule will apply.")));

		buttons.add(new ButtonMenu(new SelectDaysMonthsMenu(parent, "in", this.whichMonths, Month.values(), edited -> this.setWhichMonths((IsInList<Month>) edited)), ItemCreator.from(
				CompMaterial.ENDER_EYE,
				"&eSelect Months",
				"",
				"Current: &f" + (this.whichMonths.isEntireList() ? "Every month" : "\n &8- &f" + Common.join(this.whichMonths, "\n &8- &f", val -> ChatUtil.capitalize(val.toString().toLowerCase()))),
				"",
				"Choose which months",
				"of the real-life year",
				"this rule will apply.")));

		buttons.add(new ButtonConversation(new MinecraftTimePrompt(), ItemCreator.from(
				CompMaterial.CLOCK,
				"Minecraft Time",
				"",
				"Current: &f" + (this.minecraftTime == null ? "&aany game time" : this.minecraftTime),
				"",
				"Choose what time",
				"period in Minecraft",
				"this rule works within.")));

		buttons.add(new ButtonConversation(new LightLevelPrompt(), ItemCreator.from(
				CompMaterial.TORCH,
				"Light Level",
				"",
				"Current: &f" + (this.lightLevel == null ? "&aall light levels" : this.lightLevel),
				"",
				"Choose what light",
				"levels to spawn",
				"Bosses within.")));

		buttons.add(new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				final boolean has = SpawnRule.this.rainingRequired;
				SpawnRule.this.setRainingRequired(!has);

				menu.restartMenu(has ? "&4Boss no longer requires rain" : "&2Boss will only spawn on rain");
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator.from(
						CompMaterial.BUCKET,
						"&bRequires Rain",
						"",
						"Status: " + (SpawnRule.this.rainingRequired ? "&cRequired" : "&aNot required"),
						"",
						"Is this spawn rule only",
						"active when it's raining?")
						.glow(SpawnRule.this.rainingRequired)
						.make();
			}
		});

		buttons.add(new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				final boolean has = SpawnRule.this.thunderRequired;
				SpawnRule.this.setThunderRequired(!has);

				menu.restartMenu(has ? "&4Boss no longer requires thunder" : "&2Boss only spawns during thunder");
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator.from(
						CompMaterial.WATER_BUCKET,
						"&bRequires Thunder",
						"",
						"Status: " + (SpawnRule.this.thunderRequired ? "&cRequired" : "&aNot required"),
						"",
						"Is this spawn rule only",
						"active when it's thundering?")
						.glow(SpawnRule.this.thunderRequired)
						.make();
			}
		});

		buttons.add(new ButtonConversation(new ChancePrompt(), ItemCreator.from(
				CompMaterial.GOLD_INGOT,
				"&6Run Chance",
				"",
				"Status: &f" + MathUtil.formatTwoDigits(this.chance * 100) + "%",
				"",
				"The chance from 0-100",
				"that this rule will run",
				"each time it is checked.")));

		return buttons;
	}

	/**
	 * The menu for the Select Bosses button.
	 */
	private class EditBossesMenu extends MenuPaged<Boss> {

		EditBossesMenu(Menu parent) {
			super(parent, Boss.getBosses(), true);

			this.setTitle("Select Bosses Rule Applies To");
		}

		@Override
		protected ItemStack convertToItemStack(Boss boss) {
			final boolean has = SpawnRule.this.bosses.contains(boss.getName());

			return ItemCreator
					.fromItemStack(boss.getEgg())
					.clearLore()
					.lore(
							"",
							"Status: " + (has ? "&aEnabled" : "&cDisabled"),
							"",
							"Click to toggle if this",
							"rule applies for the Boss.")
					.glow(has)
					.make();
		}

		@Override
		protected void onPageClick(Player player, Boss boss, ClickType click) {
			final String bossName = boss.getName();
			final boolean has = SpawnRule.this.bosses.contains(bossName);
			final Set<String> list = SpawnRule.this.bosses.getList();

			if (has)
				list.remove(bossName);
			else
				list.add(bossName);

			SpawnRule.this.setBosses(IsInList.fromList(list));
			this.restartMenu((has ? "&4" : "&2") + bossName + " has been " + (has ? "excluded" : "included") + "!");
		}
	}

	@SuppressWarnings("rawtypes")
	private class SelectDaysMonthsMenu extends MenuPaged<Enum<?>> {

		private final String preposition;
		private final IsInList existing;
		private final Consumer<IsInList<? extends Enum<?>>> successAction;

		SelectDaysMonthsMenu(Menu parent, String preposition, IsInList<? extends Enum<?>> existing, Enum<?>[] values, Consumer<IsInList<? extends Enum<?>>> successAction) {
			super(parent, Arrays.asList(values), true);

			this.preposition = preposition;
			this.existing = existing;
			this.successAction = successAction;

			this.setTitle("Select Date/Time For Rule");
		}

		@Override
		protected ItemStack convertToItemStack(Enum<?> item) {
			final String title = ChatUtil.capitalize(item.toString().toLowerCase());

			return ItemCreator.from(
					CompMaterial.STICK,
					title,
					"",
					"Status: " + (this.existing.contains(item) ? "&aActive" : "&cInactive"),
					"",
					"Click to toggle",
					"spawning " + this.preposition + " " + title)
					.glow(this.existing.contains(item))
					.make();
		}

		@Override
		protected void onPageClick(Player player, Enum<?> item, ClickType click) {
			final String title = ChatUtil.capitalize(item.toString().toLowerCase());

			final boolean has = this.existing.contains(item);
			IsInList list = this.existing;

			if (has)
				list.getList().remove(item);

			else
				list.getList().add(item);

			if (list.getList().isEmpty())
				list = IsInList.fromStar();

			this.successAction.accept(list);

			this.restartMenu(has ? "&4Disabled spawn " + this.preposition + " " + title : "&2Enabled spawn " + this.preposition + " " + title);
		}
	}

	private class MinecraftTimePrompt extends SimpleStringPrompt {

		MinecraftTimePrompt() {
			super("Enter Minecraft time from 0 to 24000 to restrict when Boss spawns (in ticks). Example: '0 - 24000'. "
					+ "A range '0 - 12000' means daylight, or '12000 - 24000' will make Boss spawn at night. "
					+ "Current: " + (SpawnRule.this.minecraftTime == null ? "any game time" : SpawnRule.this.minecraftTime) + ". Enter -1 to reset.");
		}

		@Override
		protected String getMenuAnimatedTitle() {
			return "&9Set spawn time to " + (SpawnRule.this.minecraftTime == null ? "any game time" : SpawnRule.this.minecraftTime);
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if ("-1".equals(input))
				return true;

			try {
				final RangedValue value = RangedValue.fromString(input);

				return value.getMinLong() >= 0 && value.getMaxLong() <= 24000 && value.getMinLong() <= value.getMaxLong();
			} catch (final Throwable t) {
				t.printStackTrace();
			}

			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Invalid time: '" + invalidInput + "'. Enter a ranged value such as '0 - 12000' (ticks) etc.";
		}

		@Override
		protected void onValidatedInput(ConversationContext context, String input) {
			if ("-1".equals(input))
				SpawnRule.this.setMinecraftTime(null);

			else {
				final RangedValue value = RangedValue.fromString(input);

				SpawnRule.this.setMinecraftTime(value);
			}
		}
	}

	private class LightLevelPrompt extends SimpleStringPrompt {

		LightLevelPrompt() {
			super("Enter light level from 0 to 15 to restrict how much light Boss needs to spawns. "
					+ "See https://minecraft.fandom.com/wiki/Light for more info. "
					+ "Current: " + (SpawnRule.this.lightLevel == null ? "all light levels" : SpawnRule.this.lightLevel) + ". Enter -1 to reset.");
		}

		@Override
		protected String getMenuAnimatedTitle() {
			return "&9Set light level to " + (SpawnRule.this.lightLevel == null ? "all" : SpawnRule.this.lightLevel);
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if ("-1".equals(input))
				return true;

			try {
				final RangedValue value = RangedValue.fromString(input);

				return value.getMinLong() >= 0 && value.getMaxLong() <= 15 && value.getMinLong() <= value.getMaxLong();
			} catch (final Throwable t) {
			}

			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Invalid light level: '" + invalidInput + "'. Enter a value like '7' or a range: '0 - 15' etc.";
		}

		@Override
		protected void onValidatedInput(ConversationContext context, String input) {
			if ("-1".equals(input))
				SpawnRule.this.setLightLevel(null);

			else {
				final RangedValue value = RangedValue.fromString(input);

				SpawnRule.this.setLightLevel(value);
			}
		}
	}

	private class ChancePrompt extends SimpleStringPrompt {

		ChancePrompt() {
			super("Enter how likely this rule will execute when it is scheduled, from 0.00% to 100%. Current: " + MathUtil.formatTwoDigits(SpawnRule.this.chance * 100) + "%");
		}

		@Override
		protected String getMenuAnimatedTitle() {
			return "&9Set chance to " + MathUtil.formatTwoDigits(SpawnRule.this.chance * 100);
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), 0, 100);
		}

		@Override
		protected void onValidatedInput(ConversationContext context, String input) {
			SpawnRule.this.setChance(Double.parseDouble(input) / 100);
		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Setters */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Toggle state of this rule
	 *
	 * @param enabled
	 */
	public final void setEnabled(boolean enabled) {
		this.enabled = enabled;

		this.save();
	}

	/**
	 * Set Bosses for this rule
	 *
	 * @param bosses
	 */
	public final void setBosses(IsInList<String> bosses) {
		this.bosses = bosses;

		this.save();
	}

	/**
	 * Set the delay for running the task
	 *
	 * @param delay the delay to set
	 */
	public void setDelay(SimpleTime delay) {
		this.delay = delay;

		this.save();
	}

	/**
	 * Set which days this rule works
	 *
	 * @param whichDays the whichDays to set
	 */
	public final void setWhichDays(IsInList<DayOfWeek> whichDays) {
		this.whichDays = whichDays;

		this.save();
	}

	/**
	 * Set which months this rule works
	 *
	 * @param whichMonths the whichMonths to set
	 */
	public final void setWhichMonths(IsInList<Month> whichMonths) {
		this.whichMonths = whichMonths;

		this.save();
	}

	/**
	 * @param minecraftTime the minecraftTime to set
	 */
	public final void setMinecraftTime(RangedValue minecraftTime) {
		this.minecraftTime = minecraftTime;

		this.save();
	}

	/**
	 *
	 * @param lightLevel
	 */
	public void setLightLevel(RangedValue lightLevel) {
		this.lightLevel = lightLevel;

		this.save();
	}

	/**
	 * @param rainingRequired the rainingRequired to set
	 */
	public final void setRainingRequired(boolean rainingRequired) {
		this.rainingRequired = rainingRequired;

		this.save();
	}

	/**
	 * @param thunderRequired the thunderRequired to set
	 */
	public final void setThunderRequired(boolean thunderRequired) {
		this.thunderRequired = thunderRequired;

		this.save();
	}

	/**
	 * Set chance for this rule
	 *
	 * @param chance the chance to set
	 */
	public final void setChance(double chance) {
		this.chance = chance;

		this.save();
	}

	public String getName() {
		return this.getFileName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Ticks all rules. This will return the list of Bosses it spawned
	 *
	 * @param data
	 * @param types what rule types should we tick
	 * @return
	 */
	public static SpawnData tick(SpawnData data, @NonNull SpawnRuleType... types) {
		final Set<SpawnRuleType> typesSet = Common.newSet(types);

		for (final SpawnRule rule : getRules())
			if (typesSet.contains(rule.getType()))
				rule.onTick(data);

		return data;
	}

	/**
	 * @see org.mineacademy.fo.settings.ConfigItems#loadItems()
	 */
	public static final void loadRules() {

		loadedRules.loadItems(file -> {
			final String name = FileUtil.getFileName(file);

			// We must preload files manually and read their Type to instantiate the right class
			final YamlConfig yaml = YamlConfig.fromFile(file);
			final String typeName = yaml.getString("Type");

			if (typeName == null)
				Common.warning("Invalid spawn rule: " + file + ", lacking 'Type' key to determine spawn rule type for the boss.");
			else {
				final SpawnRuleType type = SpawnRuleType.valueOf(typeName);

				createRule(name, type);
			}
		});
	}

	/**
	 * @see org.mineacademy.fo.settings.ConfigItems#loadOrCreateItem(java.lang.String)
	 *
	 * @param name
	 * @param type
	 *
	 * @return
	 */
	public static final SpawnRule createRule(@NonNull String name, SpawnRuleType type) {
		return loadedRules.loadOrCreateItem(name, () -> {
			final Constructor<?> constructor = ReflectionUtil.getConstructor(type.getSpawnRuleClass(), String.class);
			final SpawnRule rule = (SpawnRule) ReflectionUtil.instantiate(constructor, name);

			// Save after constructor chain
			rule.save();

			return rule;
		});
	}

	/**
	 * @param item
	 *
	 */
	public static final void removeRule(@NonNull SpawnRule item) {
		loadedRules.removeItem(item);
	}

	/**
	 * @param name
	 * @return
	 * @see org.mineacademy.fo.settings.ConfigItems#isItemLoaded(java.lang.String)
	 */
	public static final boolean isRuleLoaded(String name) {
		return loadedRules.isItemLoaded(name);
	}

	/**
	 * @param name
	 * @return
	 */
	public static final SpawnRule findRule(@NonNull String name) {
		return loadedRules.findItem(name);
	}

	/**
	 * @return
	 * @see org.mineacademy.fo.settings.ConfigItems#getItems()
	 */
	public static final Collection<SpawnRule> getRules() {
		return loadedRules.getItems();
	}

	/**
	 * @return
	 * @see org.mineacademy.fo.settings.ConfigItems#getItemNames()
	 */
	public static final List<String> getRulesNames() {
		return loadedRules.getItemNames();
	}
}
