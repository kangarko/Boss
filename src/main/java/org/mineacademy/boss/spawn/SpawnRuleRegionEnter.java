package org.mineacademy.boss.spawn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Entity;
import org.mineacademy.boss.menu.util.SpawnRuleLocationsMenu;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossLocation;
import org.mineacademy.boss.model.BossRegionType;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonConversation;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.Getter;

/**
 * Represents a rule that triggers when Boss enters a region
 */
@Getter
public final class SpawnRuleRegionEnter extends SpawnRuleRegions {

	/**
	 * Where to spawn Bosses
	 */
	private Set<String> locations;

	/**
	 * The limit of Bosses that can spawn in a region. This rule stops when there is the
	 * given number of Bosses of the same type in a region.
	 */
	private Integer maxBossesInRegion;

	/**
	 * Create a new rule by name
	 *
	 * @param name
	 */
	public SpawnRuleRegionEnter(String name) {
		super(name, SpawnRuleType.REGION_ENTER);
	}

	/* ------------------------------------------------------------------------------- */
	/* Data */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRuleDateExact#onLoad()
	 */
	@Override
	protected void onLoad() {
		super.onLoad();

		this.locations = this.getSet("Locations", String.class);
		this.regionsEnabled = true; // override
		this.maxBossesInRegion = this.getInteger("Max_Bosses_In_Region");
	}

	@Override
	public void onSave() {
		this.set("Locations", this.locations);
		this.set("Regions_Enabled", true); // override
		this.set("Max_Bosses_In_Region", this.maxBossesInRegion);

		super.onSave();
	}

	/* ------------------------------------------------------------------------------- */
	/* Logic */
	/* ------------------------------------------------------------------------------- */

	/**
	 * We do not support region enter from other plugins.
	 */
	@Override
	protected boolean showRegionsFromOtherPlugins() {
		return false;
	}

	@Override
	public void onTick(SpawnData data) {

		if (!this.canRun())
			return;

		final DiskRegion enteredRegion = data.getRegion();
		final String enteredRegionName = enteredRegion.getFileName();
		final VisualizedRegion border = enteredRegion.getBorder();

		final Map<String, Double> bossRegions = this.getRegions(BossRegionType.BOSS);

		if (!bossRegions.containsKey(enteredRegionName)) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running because it requires entering regions " + bossRegions.keySet() + " not " + enteredRegionName);

			return;
		}

		if (this.maxBossesInRegion != null) {
			int otherBossesInRegion = 0;

			for (final Entity entityInRegion : border.getEntities()) {
				final SpawnedBoss bossInRegion = Boss.findBoss(entityInRegion);

				if (bossInRegion != null && this.getBosses().contains(bossInRegion.getBoss().getName()))
					otherBossesInRegion++;
			}

			if (otherBossesInRegion >= this.maxBossesInRegion) {
				Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running, reached maxBossesInRegion limit of " + this.maxBossesInRegion + " in region " + enteredRegionName);

				return;
			}
		}

		final List<String> toRemove = new ArrayList<>();

		for (final String locationName : this.locations) {

			final BossLocation bossLocation = BossLocation.findLocation(locationName);

			// Probably removed
			if (bossLocation == null) {
				Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running, unable to find location by name: " + locationName);

				toRemove.add(locationName);
				continue;
			}

			final Location location = bossLocation.getLocation().add(0.5, 1, 0.5);

			if (location != null && this.canRun(location)) {
				Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Entered " + enteredRegionName + " region -> spawned boss at " + locationName + " (" + SerializeUtil.serializeLocation(location) + ")");

				this.spawn(location, data);
			}
		}

		// Remove invalid locations
		if (!toRemove.isEmpty()) {
			this.locations.removeAll(toRemove);

			this.save();
		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Menu system */
	/* ------------------------------------------------------------------------------- */

	@Override
	public List<Button> getButtons(Menu parent) {
		final List<Button> buttons = super.getButtons(parent);

		buttons.add(SpawnRuleLocationsMenu.createButton(parent, this.locations, this::setLocations, false));

		buttons.add(new ButtonConversation(new MaxBossesInRegionPrompt(), ItemCreator.from(
				CompMaterial.GRASS_BLOCK,
				"Max Bosses In Region",
				"",
				"Current: &f" + (this.maxBossesInRegion == null ? "&ano limit" : this.maxBossesInRegion),
				"",
				"Set a limit of Bosses in",
				"region for this rule not",
				"to spawn more Bosses when",
				"the player enters.")));

		return buttons;
	}

	private class MaxBossesInRegionPrompt extends SimpleDecimalPrompt {

		MaxBossesInRegionPrompt() {
			super("Enter the amount of Bosses that, when in region, this spawn rule won't trigger. Enter -1 for no limit. "
					+ "Current: " + (SpawnRuleRegionEnter.this.maxBossesInRegion == null ? "no limit" : SpawnRuleRegionEnter.this.maxBossesInRegion));
		}

		@Override
		protected String getMenuAnimatedTitle() {
			return "&9Set Boss limit in region to " + (SpawnRuleRegionEnter.this.maxBossesInRegion == null ? "unlimited" : SpawnRuleRegionEnter.this.maxBossesInRegion);
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if ("-1".equals(input))
				return true;

			try {
				final int value = Integer.parseInt(input);

				return value > 0 && value < Integer.MAX_VALUE;
			} catch (final Throwable t) {
			}

			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Invalid limit: '" + invalidInput + "'. Enter a value like '15' or '-1' to reset";
		}

		@Override
		protected void onValidatedInput(ConversationContext context, double input) {
			SpawnRuleRegionEnter.this.setMaxBossesInRegion(input == -1 ? null : (int) input);
		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Setters */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Set locations
	 *
	 * @param locations the locations to set
	 */
	public void setLocations(Set<String> locations) {
		this.locations = locations;

		this.save();
	}

	/**
	 * Set the max bosses in region limit
	 *
	 * @param maxBossesInRegion the limit to set
	 */
	public void setMaxBossesInRegion(Integer maxBossesInRegion) {
		this.maxBossesInRegion = maxBossesInRegion;

		this.save();
	}
}
