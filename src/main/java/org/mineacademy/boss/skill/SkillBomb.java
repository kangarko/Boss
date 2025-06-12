package org.mineacademy.boss.skill;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.BossSkillTag;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillBomb extends AbstractTargetSkill {

	/**
	 * How much to wait before setting off the bomb?
	 */
	private SimpleTime fuseTime;

	/**
	 * How high above the sky to spawn?
	 */
	private int height;

	/**
	 * The strength of the explosion.
	 */
	private float power;

	/**
	 * Should TNT destroy blocks around?
	 */
	private boolean destroyBlocks = false;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("30 seconds - 1 minute");
	}

	@Override
	public boolean execute(Player target, LivingEntity entity) {
		final Block block = this.findHighestBlock(target);

		this.spawnTnt(block.getLocation());
		this.sendSkillMessage(target, entity);

		return true;
	}

	private Block findHighestBlock(Player player) {
		Block block = player.getWorld().getBlockAt(player.getLocation());

		for (int i = 0; i < this.height; i++) {
			if (block.getType() != Material.AIR)
				break;

			block = block.getRelative(BlockFace.UP);
		}

		return block;
	}

	private void spawnTnt(Location loc) {
		final TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);

		BossSkillTag.EXPLOSION_POWER.set(tnt, this.power);
		BossSkillTag.IS_EXPLOSION_DESTROYING_BLOCKS.set(tnt, this.destroyBlocks);
		BossSkillTag.IS_CANCELLING_COMBUSTION.set(tnt, true);

		tnt.setFuseTicks(this.fuseTime.getTimeTicks());
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.TNT,
				"Bomb",
				"",
				"Spawn a TnT bomb",
				"above the player.")
				.make();
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-bomb-default-message").split("\n");
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.fuseTime = map.containsKey("Fuse_Time") ? map.get("Fuse_Time", SimpleTime.class) : SimpleTime.fromSeconds(3);
		this.height = map.getInteger("Spawn_Height", 7);
		this.power = map.getFloat("Power", 4F);
		this.destroyBlocks = map.getBoolean("Destroy_Blocks", false);
	}

	@Override
	public SerializedMap writeSettings() {
		final SerializedMap map = new SerializedMap();

		map.put("Fuse_Time", this.fuseTime);
		map.put("Spawn_Height", this.height);
		map.put("Power", this.power);
		map.put("Destroy_Blocks", this.destroyBlocks);

		return map;
	}

	/**
	 * @see org.mineacademy.boss.skill.BossSkill#getMenu(org.mineacademy.fo.menu.Menu)
	 */
	@Override
	public Menu getMenu(Menu parent) {
		return new BombSettingsMenu(parent);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private class BombSettingsMenu extends Menu {

		@Position(9 * 1 + 2)
		private final Button fuseButton;

		@Position(9 * 1 + 3)
		private final Button heightButton;

		@Position(9 * 1 + 5)
		private final Button powerButton;

		@Position(9 * 1 + 6)
		private final Button destroyBlocksButton;

		BombSettingsMenu(Menu parent) {
			super(parent);

			this.setSize(9 * 3);
			this.setTitle("Bomb Skill Settings");

			this.fuseButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.CLOCK,
					"Fuse Time",
					"",
					"Current: &f" + SkillBomb.this.fuseTime.getRaw(),
					"",
					"Click to adjust how long",
					"to wait before blowing off",
					"the bomb."),
					player -> {
						new SimpleStringPrompt("Enter the time to wait before setting the bomb off. Current: '" + SkillBomb.this.fuseTime.getRaw() + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final SimpleTime newValue = SimpleTime.fromString(input);

									if (newValue.getTimeTicks() > 0 && newValue.getTimeSeconds() <= 120) {
										SkillBomb.this.fuseTime = newValue;
										SkillBomb.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid time, enter a human readable format such as '3 seconds' between 1 tick and 2 minutes.";
							}

						}.show(player);
					});

			this.heightButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.LADDER,
					"Blocks Height",
					"",
					"Current: &f" + SkillBomb.this.height,
					"",
					"Click to edit how many",
					"blocks above the player",
					"will the bomb spawn."),
					player -> {
						new SimpleDecimalPrompt("Enter how many blocks above the player will the bomb appear. Current: '" + SkillBomb.this.height + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									if (Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), 1, 255)) {
										SkillBomb.this.height = Integer.parseInt(input);
										SkillBomb.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid height, enter a whole number from 0-255 such as '5' for five blocks.";
							}

						}.show(player);
					});

			this.powerButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.MAGMA_CREAM,
					"Power",
					"",
					"Current: &f" + MathUtil.formatTwoDigits(SkillBomb.this.power),
					"",
					"Click to edit",
					"explosion power."),
					player -> {
						new SimpleDecimalPrompt("Enter the explosion power (as a decimal number). Current: '" + MathUtil.formatTwoDigits(SkillBomb.this.power) + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									if (Valid.isDecimal(input) && Valid.isInRange(Float.parseFloat(input), 0.01, 100)) {
										SkillBomb.this.power = Float.parseFloat(input);
										SkillBomb.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid explosion power, enter a decimal number between 0.01 and 100 (high values may oh-shit the server).";
							}
						}.show(player);
					});

			this.destroyBlocksButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					SkillBomb.this.destroyBlocks = !SkillBomb.this.destroyBlocks;
					SkillBomb.this.save();

					BombSettingsMenu.this.restartMenu(SkillBomb.this.destroyBlocks ? "&2Blocks No Longer Destroyed" : "&4Blocks Are Now Destroyed");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							(SkillBomb.this.destroyBlocks ? CompMaterial.BEACON : CompMaterial.GLASS),
							"Destroy Blocks",
							"",
							"Status: " + (SkillBomb.this.destroyBlocks ? "&aenabled" : "&cdisabled"),
							"",
							"Toggle whether the explosion",
							"should damage nearby blocks.").make();
				}
			};
		}

		@Override
		public Menu newInstance() {
			return new BombSettingsMenu(this.getParent());
		}
	}
}
