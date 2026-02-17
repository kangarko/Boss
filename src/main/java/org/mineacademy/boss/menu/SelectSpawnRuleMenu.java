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

		this.setTitle(Lang.legacy("menu-spawn-rules-title"));

		this.createButton = new ButtonMenu(new ConditionSelectMenu(),
				CompMaterial.EMERALD,
				Lang.legacy("menu-spawn-rules-button-create"),
				Lang.legacy("menu-spawn-rules-button-create-lore").split("\n"));
	}

	@Override
	protected ItemStack convertToItemStack(SpawnRule item) {

		final List<String> warning = item.getBosses().getList().isEmpty()
				? Arrays.asList(Lang.legacy("menu-spawn-rules-item-warning").split("\n"))
				: new ArrayList<>();

		return ItemCreator.from(
				item.getType().getIconType(),
				item.getName(),
				Lang.legacy("menu-spawn-rules-item-lore", "type", item.getType().getDescription()).split("\n"))
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
		return Lang.legacy("menu-spawn-rules-info").split("\n");
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
			this.setTitle(Lang.legacy("menu-spawn-rules-condition-title"));

			this.locationPeriodButton = this.generateButton(SpawnRuleType.LOCATION_PERIOD,
					CompMaterial.SPAWNER,
					Lang.legacy("menu-spawn-rules-condition-location-period"),
					Lang.legacy("menu-spawn-rules-condition-location-period-lore").split("\n"));

			this.regionEnterButton = Settings.REGISTER_REGIONS ? this.generateButton(SpawnRuleType.REGION_ENTER,
					CompMaterial.ORANGE_DYE,
					Lang.legacy("menu-spawn-rules-condition-region-enter"),
					Lang.legacy("menu-spawn-rules-condition-region-enter-lore").split("\n"))
					: Button.makeDummy(
							CompMaterial.ORANGE_DYE,
							Lang.legacy("menu-spawn-rules-condition-region-enter"),
							Lang.legacy("menu-spawn-rules-condition-region-disabled-lore").split("\n"));

			this.respawnAfterDeathButton = this.generateButton(SpawnRuleType.RESPAWN_AFTER_DEATH,
					CompMaterial.BONE,
					Lang.legacy("menu-spawn-rules-condition-respawn"),
					Lang.legacy("menu-spawn-rules-condition-respawn-lore").split("\n"));

			this.periodButton = this.generateButton(SpawnRuleType.PERIOD,
					CompMaterial.PLAYER_HEAD,
					Lang.legacy("menu-spawn-rules-condition-period"),
					Lang.legacy("menu-spawn-rules-condition-period-lore").split("\n"));

			this.replaceVanillaButton = this.generateButton(SpawnRuleType.REPLACE_VANILLA,
					CompMaterial.OAK_SAPLING,
					Lang.legacy("menu-spawn-rules-condition-replace"),
					Lang.legacy("menu-spawn-rules-condition-replace-lore").split("\n"));
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
			return Lang.legacy("menu-spawn-rules-condition-info").split("\n");
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

			this.setTitle(Lang.legacy("menu-spawn-rules-individual-title", "rule", spawnRule.getName()));

			this.spawnRule = spawnRule;

			this.buttonsToAutoRegister = spawnRule.getButtons(this);

			final String spawnRuleName = spawnRule.getName();

			this.removeButton = new ButtonRemove(this, "spawn rule", spawnRuleName, () -> {
				if (SpawnRule.isRuleLoaded(spawnRuleName))
					SpawnRule.removeRule(spawnRule);
				else
					this.animateTitle(Lang.legacy("menu-spawn-rules-individual-rule-not-loaded"));
			});

			this.enabledButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {

					final boolean has = spawnRule.isEnabled();
					spawnRule.setEnabled(!has);

					final Menu newInstance = newInstance();
					newInstance.displayTo(player);

					Platform.runTask(1, () -> newInstance.restartMenu(!has ? Lang.legacy("menu-spawn-rules-individual-enabled-message") : Lang.legacy("menu-spawn-rules-individual-disabled-message")));
				}

				@Override
				public ItemStack getItem() {
					final boolean has = spawnRule.isEnabled();

					return ItemCreator.from(
							has ? CompMaterial.BEACON : CompMaterial.GLASS,
							Lang.legacy("menu-spawn-rules-individual-enabled"),
							Lang.legacy("menu-spawn-rules-individual-enabled-lore", "status", has ? Lang.legacy("part-enabled") : Lang.legacy("part-disabled")).split("\n"))
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
			return Lang.legacy("menu-spawn-rules-individual-info").split("\n");
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
