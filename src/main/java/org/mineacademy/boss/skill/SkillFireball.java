package org.mineacademy.boss.skill;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.mineacademy.boss.model.BossSkillTag;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillFireball extends AbstractTargetSkill {

	/**
	 * Power of the explosion
	 */
	private float power;

	/**
	 * Should Fireball destroy blocks around?
	 */
	private boolean destroyBlocks = false;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("20 seconds - 50 seconds");
	}

	@Override
	public boolean execute(Player target, LivingEntity entity) {
		final Fireball fireball = entity.launchProjectile(Fireball.class);
		final Vector vector = target.getLocation().subtract(entity.getLocation()).toVector().normalize();

		if (!Valid.isFinite(vector))
			return false;

		fireball.setVelocity(vector);

		BossSkillTag.EXPLOSION_POWER.set(fireball, this.power);
		BossSkillTag.IS_EXPLOSION_DESTROYING_BLOCKS.set(fireball, this.destroyBlocks);
		BossSkillTag.IS_CANCELLING_COMBUSTION.set(fireball, true);

		this.sendSkillMessage(target, entity);

		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-fireball-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.FIRE_CHARGE,
				"Fireball",
				"",
				"The Boss will throw",
				"a fireball on the player.")
				.make();
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.power = map.getFloat("Power", 1.8F);
		this.destroyBlocks = map.getBoolean("Destroy_Blocks", false);
	}

	@Override
	public SerializedMap writeSettings() {
		final SerializedMap map = new SerializedMap();

		map.put("Power", this.power);
		map.put("Destroy_Blocks", this.destroyBlocks);

		return map;
	}

	/**
	 * @see org.mineacademy.boss.skill.BossSkill#getMenu(org.mineacademy.fo.menu.Menu)
	 */
	@Override
	public Menu getMenu(Menu parent) {
		return new FireballSettingsMenu(parent);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private class FireballSettingsMenu extends Menu {

		@Position(9 * 1 + 3)
		private final Button powerButton;

		@Position(9 * 1 + 5)
		private final Button destroyBlocksButton;

		FireballSettingsMenu(Menu parent) {
			super(parent);

			this.setSize(9 * 3);
			this.setTitle("Firewall Skill Settings");

			this.powerButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.MAGMA_CREAM,
					"Power",
					"",
					"Current: &f" + MathUtil.formatTwoDigits(SkillFireball.this.power),
					"",
					"Click to edit",
					"explosion power."),
					player -> {
						new SimpleDecimalPrompt("Enter the explosion power (as a decimal number). Current: '" + MathUtil.formatTwoDigits(SkillFireball.this.power) + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									if (Valid.isDecimal(input) && Valid.isInRange(Float.parseFloat(input), 0.01, 100)) {
										SkillFireball.this.power = Float.parseFloat(input);
										SkillFireball.this.save();

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
					SkillFireball.this.destroyBlocks = !SkillFireball.this.destroyBlocks;
					SkillFireball.this.save();

					FireballSettingsMenu.this.restartMenu(SkillFireball.this.destroyBlocks ? "&2Blocks No Longer Destroyed" : "&4Blocks Are Now Destroyed");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							(SkillFireball.this.destroyBlocks ? CompMaterial.BEACON : CompMaterial.GLASS),
							"Destroy Blocks",
							"",
							"Status: " + (SkillFireball.this.destroyBlocks ? "&aenabled" : "&cdisabled"),
							"",
							"Toggle whether the explosion",
							"should damage nearby blocks.").make();
				}
			};
		}

		@Override
		public Menu newInstance() {
			return new FireballSettingsMenu(this.getParent());
		}
	}
}
