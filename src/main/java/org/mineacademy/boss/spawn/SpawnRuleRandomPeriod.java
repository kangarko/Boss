package org.mineacademy.boss.spawn;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.mineacademy.boss.hook.GriefPreventionHook;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;

/**
 * A rule denoting spawning naturally each X
 */
@Getter
public final class SpawnRuleRandomPeriod extends SpawnRuleRandom {

	/**
	 * Radius to spawn around players
	 */
	private int blockRadius;

	/**
	 * Create new rule
	 *
	 * @param name
	 */
	public SpawnRuleRandomPeriod(String name) {
		super(name, SpawnRuleType.PERIOD);
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

		this.blockRadius = this.getInteger("Block_Radius", 30);
	}

	@Override
	public void onSave() {
		this.set("Block_Radius", this.blockRadius);

		super.onSave();
	}

	/* ------------------------------------------------------------------------------- */
	/* Spawning */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRuleRandom#canRun(org.bukkit.Location)
	 */
	@Override
	protected boolean canRun(Location location) {

		if (GriefPreventionHook.isEnabled() && GriefPreventionHook.getClaimOwner(location) != null)
			return false;

		return super.canRun(location);
	}

	/* ------------------------------------------------------------------------------- */
	/* Logic */
	/* ------------------------------------------------------------------------------- */

	@Override
	public void onTick(SpawnData data) {

		if (!this.canRun())
			return;

		for (final Player online : Remain.getOnlinePlayers()) {
			Location location = online.getLocation();

			// Randomize
			location = this.randomizeLocation(location);

			if (this.canRun(location))
				this.spawn(location, data);
		}
	}

	private Location randomizeLocation(Location location) {
		final int minDistanceFromPlayer = Settings.Spawning.NEARBY_SPAWN_MIN_DISTANCE_FROM_PLAYER;

		if (this.blockRadius < 1) {
			Common.warning("Zero block radius found in rule " + this.getFileName() + "! Set the 'Block_Radius' key to more than 0 and restart.");

			this.blockRadius = 1;
		}

		// Randomize XZ coords
		final int randomX = minDistanceFromPlayer + MathUtil.range(RandomUtil.nextInt(this.blockRadius), 0, this.blockRadius - minDistanceFromPlayer);
		final int randomZ = minDistanceFromPlayer + MathUtil.range(RandomUtil.nextInt(this.blockRadius), 0, this.blockRadius - minDistanceFromPlayer);

		location.add(randomX * (RandomUtil.nextBoolean() ? 1 : -1), 0, randomZ * (RandomUtil.nextBoolean() ? 1 : -1));

		// Find Y coordinate
		final boolean nether = location.getWorld().getEnvironment() == Environment.NETHER;
		final int y = nether ? BlockUtil.findHighestNetherAirBlock(location) : BlockUtil.findAirBlock(location, RandomUtil.nextBoolean(), Material::isSolid);

		location.setY(y);

		return location.add(0.5, 0, 0.5);
	}

	/* ------------------------------------------------------------------------------- */
	/* Menu system */
	/* ------------------------------------------------------------------------------- */

	@Override
	public List<Button> getButtons(Menu parent) {
		final List<Button> buttons = super.getButtons(parent);

		buttons.add(Button.makeIntegerPrompt(ItemCreator.from(
				CompMaterial.ICE,
				"Radius",
				"",
				"Current: &f" + this.blockRadius + " blocks",
				"",
				"Click to change how far",
				"from players Bosses will",
				"appear randomly."),
				"Enter a number of blocks between 5-80 for how far around players will Bosses spawn. Current: {current}.",
				"block radius",
				RangedValue.fromString("5 - 80"),
				this::getBlockRadius, this::setBlockRadius));

		return buttons;
	}

	/* ------------------------------------------------------------------------------- */
	/* Setters */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Set block radius
	 *
	 * @param blockRadius the blockRadius to set
	 */
	public void setBlockRadius(int blockRadius) {
		Valid.checkBoolean(blockRadius > 0, "Block radius must be more than 0, got: " + blockRadius);
		this.blockRadius = blockRadius;

		this.save();
	}

}
