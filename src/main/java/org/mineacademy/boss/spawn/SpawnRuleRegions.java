package org.mineacademy.boss.spawn;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.BossRegionType;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.MenuQuantitable;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuQuantity;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;
import lombok.Setter;

/**
 * Spawn rule to spawn in regions
 */
@Getter
abstract class SpawnRuleRegions extends SpawnRuleDateRange {

	/**
	 * Should we filter regions where Boss is spawning?
	 */
	protected boolean regionsEnabled;

	/**
	 * Are we filtering regions as whitelist, i.e. selected are allowed,
	 * or a blacklist, i.e. selected are disallowed?
	 */
	private boolean regionsWhitelist;

	/**
	 * What regions permits Boss to spawn?
	 */
	private Map<BossRegionType, Map<String, Double>> regionsWithChances;

	/**
	 * Create new spawn rule by name
	 *
	 * @param name
	 * @param type
	 */
	public SpawnRuleRegions(String name, SpawnRuleType type) {
		super(name, type);
	}

	/* ------------------------------------------------------------------------------- */
	/* Data */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRule#onLoad()
	 */
	@Override
	protected void onLoad() {
		super.onLoad();

		this.regionsWithChances = this.loadRegions();
		this.regionsEnabled = this.getBoolean("Regions_Enabled", false);
		this.regionsWhitelist = this.getBoolean("Regions_Whitelist", true);
	}

	/*
	 * A helper method to load regions
	 */
	private Map<BossRegionType, Map<String, Double>> loadRegions() {
		final Map<BossRegionType, Map<String, Double>> regions = new LinkedHashMap<>();

		for (final Map.Entry<String, Object> entry : this.getMap("Regions_2")) {
			final BossRegionType type = ReflectionUtil.lookupEnumSilent(BossRegionType.class, entry.getKey());

			if (type == null)
				Common.warning("Unknown region type: " + entry.getKey() + " in " + this.getName() + " spawn rule. This plugin version only supports: " + Common.join(BossRegionType.values()));

			else {
				final Map<String, Double> specificRegions = new LinkedHashMap<>();

				for (final Map.Entry<String, Object> regionEntry : SerializedMap.fromObject(entry.getValue()).entrySet())
					specificRegions.put(regionEntry.getKey(), Double.valueOf(regionEntry.getValue().toString()));

				regions.put(type, specificRegions);
			}
		}

		// Fill with defaults
		for (final BossRegionType type : BossRegionType.values())
			if (!regions.containsKey(type))
				regions.put(type, new LinkedHashMap<>());

		// Migrate old key
		if (this.isSet("Regions")) {
			final Map<String, Double> legacyRegions = this.getMap("Regions", String.class, Double.class);
			final Map<String, Double> existingBossRegions = regions.get(BossRegionType.BOSS);

			existingBossRegions.putAll(legacyRegions);
			this.save("Regions", null);
		}

		return regions;

	}

	@Override
	public void onSave() {
		this.set("Regions_2", this.regionsWithChances);
		this.set("Regions_Enabled", this.regionsEnabled);
		this.set("Regions_Whitelist", this.regionsWhitelist);

		super.onSave();
	}

	/* ------------------------------------------------------------------------------- */
	/* Logic */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRule#canRun(org.bukkit.Location)
	 */
	@Override
	protected boolean canRun(Location location) {

		if (this.isRegionsEnabled() && !this.getRegionsWithChances().isEmpty()) {

			boolean inAtLeastOneRegion = false;

			outer:
			for (final Entry<BossRegionType, Map<String, Double>> entry : this.getRegionsWithChances().entrySet()) {
				final BossRegionType type = entry.getKey();

				if (!this.showRegionsFromOtherPlugins() && type != BossRegionType.BOSS)
					continue;

				for (final Map.Entry<String, Double> regionEntry : entry.getValue().entrySet()) {
					final String regionName = regionEntry.getKey();
					final double chance = regionEntry.getValue();

					if (!RandomUtil.chanceD(chance))
						continue;

					if (type.isWithin(regionName, location)) {
						inAtLeastOneRegion = true;

						break outer;
					}
				}
			}

			if (!inAtLeastOneRegion && this.isRegionsWhitelist()) {
				Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to location not being in any region");

				return false;
			}

			if (inAtLeastOneRegion && !this.isRegionsWhitelist()) {
				Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to location being in a region (blacklist is enabled)");

				return false;
			}
		}

		return super.canRun(location);

	}

	/* ------------------------------------------------------------------------------- */
	/* Menu system */
	/* ------------------------------------------------------------------------------- */
	@Override
	public List<Button> getButtons(Menu parent) {
		final List<Button> buttons = super.getButtons(parent);

		if (Settings.REGISTER_REGIONS)
			buttons.add(new ButtonMenu(this.showRegionsFromOtherPlugins() ? new SelectRegionTypesMenu(parent) : new SelectRegionsMenu(BossRegionType.BOSS, parent),
					CompMaterial.YELLOW_DYE,
					"Regions",
					"",
					"Click to choose regions",
					"where Bosses will spawn."));
		else
			buttons.add(Button.makeDummy(CompMaterial.BARRIER, "Regions disabled", "&cRegions are disabled in settings!"));

		return buttons;
	}

	/**
	 * The menu where players can select a region or create one.
	 */
	private class SelectRegionTypesMenu extends MenuPaged<BossRegionType> {

		@Position(start = StartPosition.BOTTOM_RIGHT, value = -1)
		private final Button regionsEnabledButton;

		@Position(start = StartPosition.BOTTOM_RIGHT)
		private final Button regionsWhitelistButton;

		protected SelectRegionTypesMenu(Menu parent) {
			super(parent, Arrays.asList(BossRegionType.values()), true);

			this.setTitle("Select Region Type");

			this.regionsEnabledButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final boolean has = SpawnRuleRegions.this.regionsEnabled;

					SpawnRuleRegions.this.setRegionsEnabled(!has);
					SelectRegionTypesMenu.this.restartMenu(has ? "&4Regions feature disabled!" : "&2Regions feature enabled!");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							SpawnRuleRegions.this.regionsEnabled ? CompMaterial.BEACON : CompMaterial.GLASS,
							"Regions Enabled?",
							"",
							"Status: " + (SpawnRuleRegions.this.regionsEnabled ? "&aEnabled" : "&cDisabled"),
							"",
							"Should we prevent or allow",
							"Boss spawning only in selected.",
							"regions? If you enable this,",
							"you must select at least 1",
							"region or no Bosses will spawn.")
							.glow(SpawnRuleRegions.this.regionsEnabled).make();
				}
			};

			this.regionsWhitelistButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final boolean has = SpawnRuleRegions.this.regionsWhitelist;

					SpawnRuleRegions.this.setRegionsWhitelist(!has);
					SelectRegionTypesMenu.this.restartMenu(has ? "&4Bosses won't spawn in selected" : "&2Bosses only spawn in selected");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							SpawnRuleRegions.this.regionsWhitelist ? CompMaterial.WATER_BUCKET : CompMaterial.LAVA_BUCKET,
							"Region Whitelist",
							"",
							"Status: " + (SpawnRuleRegions.this.regionsWhitelist ? "&aWhitelist" : "&cBlacklist"),
							"",
							"In whitelist, Bosses only",
							"spawn in selected regions.",
							"In blacklist, they only spawn",
							"outside. Click to toggle.").make();
				}
			};
		}

		@Override
		protected ItemStack convertToItemStack(BossRegionType type) {
			return ItemCreator
					.fromMaterial(type.getIcon())
					.name(type.getPlugin() + " Regions")
					.lore("")
					.lore(type.getDescription())
					.make();
		}

		@Override
		public Menu newInstance() {
			return new SelectRegionTypesMenu(getParent());
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select regions from",
					"different plugins.",
			};
		}

		@Override
		protected void onPageClick(Player player, BossRegionType type, ClickType click) {

			if (!SpawnRuleRegions.this.regionsEnabled) {
				this.animateTitle("&eEnable regions first!");

				return;
			}

			if (type.isEnabled())
				new SelectRegionsMenu(type, this).displayTo(player);
			else
				this.animateTitle("&4Install " + type.getPlugin() + " first!");
		}
	}

	private class SelectRegionsMenu extends MenuPaged<String> implements MenuQuantitable {

		private final BossRegionType type;

		@Getter
		@Setter
		private MenuQuantity quantity = MenuQuantity.ONE;

		SelectRegionsMenu(BossRegionType type, Menu parent) {
			super(parent, type.getRegionNames(), true);

			this.type = type;
			this.setTitle("Select " + type.getPlugin() + " Regions");
		}

		@Override
		protected ItemStack convertToItemStack(String regionName) {
			return this.addLevelToItem(ItemCreator.from(
					CompMaterial.LAPIS_LAZULI,
					regionName)
					.make(),
					MathUtil.formatTwoDigits(getRegions(this.type).getOrDefault(regionName, 0D) * 100) + "%");
		}

		@Override
		public String getLevelLoreLabel() {
			return "Chance";
		}

		@Override
		public boolean allowDecimalQuantities() {
			return true;
		}

		@Override
		protected void onPageClick(Player player, String regionName, ClickType click) {

			if (!SpawnRuleRegions.this.regionsEnabled) {
				this.animateTitle("&4Enable regions in previous menu first!");
				Common.tell(player, "&cRegions are disabled, enable them in the region type selection menu by clicking the Glass icon!");

				return;
			}

			final Map<String, Double> regions = SpawnRuleRegions.this.regionsWithChances.getOrDefault(this.type, new LinkedHashMap<>());

			final double oldChance = regions.getOrDefault(regionName, 0D);
			final double newChance = MathUtil.range(oldChance + this.getNextQuantityDouble(click), 0, 1D);

			regions.put(regionName, newChance);

			SpawnRuleRegions.this.regionsWithChances.put(this.type, regions);
			SpawnRuleRegions.this.save();

			this.restartMenu("&9Spawn chance set to " + MathUtil.formatTwoDigits(newChance * 100) + "%!");
		}

		@Override
		protected String[] getInfo() {
			return this.type != BossRegionType.BOSS ? super.getInfo()
					: new String[] {
							"Select regions you created",
							"in the Boss plugin. You can",
							"create new regions using the",
							"tool in '&6/boss tools&7' menu.",
							"",
							"&6Tip: &7To save time, you can stop",
							"the server and write regions",
							"manually into your region yml",
							"file in the regions/ folder."
					};
		}
	}

	/*
	 * Internal flag
	 */
	protected boolean showRegionsFromOtherPlugins() {
		return true;
	}

	/* ------------------------------------------------------------------------------- */
	/* Settings */
	/* ------------------------------------------------------------------------------- */

	/**
	 *
	 * @param type
	 * @return
	 */
	public final Map<String, Double> getRegions(BossRegionType type) {
		return Collections.unmodifiableMap(this.regionsWithChances.getOrDefault(type, new LinkedHashMap<>()));
	}

	/**
	 * @param type
	 * @param regionsWithChances the regionsWithChances to set
	 */
	public final void setRegionsWithChances(BossRegionType type, Map<String, Double> regionsWithChances) {
		this.regionsWithChances.put(type, regionsWithChances);

		this.save();
	}

	/**
	 * @param regionsEnabled the regionsEnabled to set
	 */
	public void setRegionsEnabled(boolean regionsEnabled) {
		this.regionsEnabled = regionsEnabled;

		this.save();
	}

	/**
	 * @param regionsWhitelist the regionsWhitelist to set
	 */
	public void setRegionsWhitelist(boolean regionsWhitelist) {
		this.regionsWhitelist = regionsWhitelist;

		this.save();
	}
}
