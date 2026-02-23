package org.mineacademy.boss.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.prompt.CreateSpawnRulePrompt;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.spawn.SpawnRule;
import org.mineacademy.boss.spawn.SpawnRuleType;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.ButtonRemove;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * The menu where players can select a spawning rule or create one.
 */
public final class SelectSpawnRuleMenu extends MenuPaged<SpawnRule> {

	private final Boss bossFor;

	@Position(start = StartPosition.BOTTOM_RIGHT)
	private final Button createButton;

	protected SelectSpawnRuleMenu(Menu parent) {
		this(parent, null);
	}

	public SelectSpawnRuleMenu(Menu parent, Boss bossFor) {
		super(parent, SpawnRule
				.getRules()
				.stream()
				.filter(rule -> bossFor == null || rule.getBosses().contains(bossFor.getName()))
				.sorted(Comparator.comparing(SpawnRule::getName))
				.collect(Collectors.toList()), true);

		this.bossFor = bossFor;

		this.setTitle("Create Or Edit Spawn Rules");

		this.createButton = new ButtonMenu(new ConditionSelectMenu(),
				CompMaterial.EMERALD,
				"&aCreate New",
				"",
				"Click to add a new",
				"way for your Boss",
				"to spawn naturally.");
	}

	@Override
	protected ItemStack convertToItemStack(SpawnRule item) {

		final List<String> warning = item.getBosses().getList().isEmpty()
				? Arrays.asList("", "&cWarning: This rule has no", "&cBosses it applies for.", "&cOpen and configure it.")
				: new ArrayList<>();

		return ItemCreator.from(
				item.getType().getIconType(),
				item.getName(),
				"",
				"Type: &f" + item.getType().getDescription(),
				"",
				"Click to enter and set",
				"up conditions when",
				"Bosses will appear.")
				.lore(warning)
				.make();
	}

	@Override
	protected void onPageClick(Player player, SpawnRule item, ClickType click) {
		new IndividualSpawningMenu(this, item).displayTo(player);
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return new String[] {
				"Spawns rules make it easy",
				"to spawn Boss automatically",
				"on a certain day, or randomly",
				"around players. You can create",
				"or edit spawn rules here."
		};
	}

	@Override
	public Menu newInstance() {
		return new SelectSpawnRuleMenu(this.getParent(), this.bossFor);
	}

	private class ConditionSelectMenu extends Menu {

		@Position(0)
		private final Button locationPeriodButton;

		@Position(1)
		private final Button regionEnterButton;

		@Position(2)
		private final Button respawnAfterDeathButton;

		@Position(3)
		private final Button periodButton;

		@Position(4)
		private final Button replaceVanillaButton;

		ConditionSelectMenu() {
			super(SelectSpawnRuleMenu.this);

			this.setSize(9 * 2);
			this.setTitle("Select How To Spawn Bosses");

			this.locationPeriodButton = this.generateButton(SpawnRuleType.LOCATION_PERIOD,
					CompMaterial.SPAWNER,
					"On A Block At A Given Time",
					"",
					"Spawn Boss at a block",
					"you select every day",
					"at 17:00, each Friday",
					"at noon or every 30 mins.");

			this.regionEnterButton = Settings.REGISTER_REGIONS ? this.generateButton(SpawnRuleType.REGION_ENTER,
					CompMaterial.ORANGE_DYE,
					"On Entering A Region",
					"",
					"Spawn Boss every time on",
					"your custom location when",
					"players enter a region.",
					"(Requires Boss Region and",
					"Boss Location, see /boss tools)")
					: Button.makeDummy(
							CompMaterial.ORANGE_DYE,
							"On Entering A Region",
							"",
							"&cThis feature is disabled.",
							"&cEnable Register_Regions in",
							"&csettings.yml first.");

			this.respawnAfterDeathButton = this.generateButton(SpawnRuleType.RESPAWN_AFTER_DEATH,
					CompMaterial.BONE,
					"Respawn After Death",
					"",
					"Spawn the next Boss after",
					"a given delay after the",
					"previous Boss dies.",
					"",
					"This rule only spawns 1 Boss.",
					"We mark the spawned Boss with",
					"invisible metadata and wait the",
					"given delay to respawn it.",
					"",
					"This rule is unaffected by other",
					"rules.");

			this.periodButton = this.generateButton(SpawnRuleType.PERIOD,
					CompMaterial.PLAYER_HEAD,
					"Randomly Around Players",
					"",
					"Spawn Boss around players",
					"like vanilla mobs. Your",
					"vanilla mobs won't be",
					"affected.");

			this.replaceVanillaButton = this.generateButton(SpawnRuleType.REPLACE_VANILLA,
					CompMaterial.OAK_SAPLING,
					"Replacing Vanilla Mobs",
					"",
					"Replace vanilla mobs",
					"with Bosses, with a",
					"configurable chance",
					"and limits.");
		}

		protected Button generateButton(SpawnRuleType type, CompMaterial material, String name, String... lore) {
			return Button.makeSimple(ItemCreator.from(material, name, lore), player -> {
				CreateSpawnRulePrompt.showTo(player, type, bossFor);
			});
		}

		@Override
		public Menu newInstance() {
			return new ConditionSelectMenu();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select how we will spawn",
					"your Boss across worlds."
			};
		}
	}

	private static class IndividualSpawningMenu extends Menu {

		private final SpawnRule spawnRule;

		@Getter(value = AccessLevel.PROTECTED)
		private final List<Button> buttonsToAutoRegister;

		@Position(start = StartPosition.BOTTOM_CENTER, value = -1)
		private final Button removeButton;

		@Position(start = StartPosition.BOTTOM_CENTER, value = 1)
		private final Button enabledButton;

		IndividualSpawningMenu(Menu parent, SpawnRule spawnRule) {
			super(parent);

			this.setTitle(spawnRule.getName() + " Settings");

			this.spawnRule = spawnRule;

			this.buttonsToAutoRegister = spawnRule.getButtons(this);

			final String spawnRuleName = spawnRule.getName();

			this.removeButton = new ButtonRemove(this, "spawn rule", spawnRuleName, () -> {
				if (SpawnRule.isRuleLoaded(spawnRuleName))
					SpawnRule.removeRule(spawnRule);
				else
					this.animateTitle("&4Rule not loaded!");
			});

			this.enabledButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {

					final boolean has = spawnRule.isEnabled();
					spawnRule.setEnabled(!has);

					final Menu newInstance = newInstance();
					newInstance.displayTo(player);

					Platform.runTask(1, () -> newInstance.restartMenu(!has ? "&2Enabled this spawn rule" : "&4Disabled spawn rule :("));
				}

				@Override
				public ItemStack getItem() {
					final boolean has = spawnRule.isEnabled();

				return ItemCreator.from(
						has ? CompMaterial.BEACON : CompMaterial.GLASS,
						"Enabled?",
						"",
						"Status: " + (has ? Lang.legacy("part-enabled") : Lang.legacy("part-disabled")),
						"",
						"Click to toggle this spawn",
						"rule from functioning.")
							.glow(has)
							.make();
				}
			};
		}

		@Override
		public ItemStack getItemAt(int slot) {
			return slot < this.buttonsToAutoRegister.size() ? this.buttonsToAutoRegister.get(slot).getItem() : NO_ITEM;
		}

		/**
			 * @see org.mineacademy.fo.menu.Menu#getInfo()
			 */
		@Override
		protected String[] getInfo() {
			return new String[] {
					"Edit the conditions under which",
					"this spawn rules will function.",
					"",
					"&cImportant: &7Please also see the",
					"Spawning Limits for each boss that",
					"this rule affects because we will",
					"take it into account.",
					"",
					"&6Tip: &7Set Debug key to [spawning] in",
					"in settings.yml and reload to see",
					"console logs why this rule failed."
			};
		}

		@Override
		public Menu newInstance() {
			return new IndividualSpawningMenu(this.getParent(), this.spawnRule);
		}
	}

	/**
	 * Create spawning menu with all rules
	 *
	 * @param rule
	 * @return
	 */
	public static Menu createSpawnRuleMenu(SpawnRule rule) {
		return new IndividualSpawningMenu(new SelectSpawnRuleMenu(MainMenu.create()), rule);
	}
}
