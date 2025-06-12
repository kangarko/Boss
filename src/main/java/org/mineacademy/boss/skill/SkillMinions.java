package org.mineacademy.boss.skill;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossSpawnReason;
import org.mineacademy.boss.model.BossSpawnResult;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillMinions extends AbstractTargetSkill {

	private Set<String> bossNames;
	private int radius;
	private int amount;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("30 seconds - 1 minute");
	}

	@Override
	public boolean execute(Player target, LivingEntity entity) {
		int extraTries = 0, count = 0;

		bossLookup:
		for (final String bossName : this.bossNames)
			for (int i = 0; i < this.amount; ++i) {
				final Location loc = this.getRandomLocation(entity.getLocation());

				if (loc == null) {
					if (extraTries++ < 15)
						--i;

					continue;
				}

				final Boss minionBoss = Boss.findBoss(bossName);

				// Probably uninstalled
				if (minionBoss == null)
					continue bossLookup;

				final Tuple<BossSpawnResult, SpawnedBoss> tuple = minionBoss.spawn(loc, BossSpawnReason.REINFORCEMENTS);

				if (tuple.getKey().isSuccess() && tuple.getValue().getEntity() instanceof Creature)
					((Creature) tuple.getValue().getEntity()).setTarget(target);

				++count;
			}

		if (count > 0) {
			this.sendSkillMessage(target, entity);

			return true;
		}

		return false;
	}

	private Location getRandomLocation(Location center) {
		final int tries = 5;

		for (int i = 0; i < tries; ++i) {
			final Location loc = Common.getRandomLocation(center, this.radius, false);
			final org.bukkit.block.Block block = loc.add(0, 1, 0).getBlock();
			final int maxY = loc.getWorld().getMaxHeight() - 1;

			// find first nearby block
			int dy = 0;
			boolean last = CompMaterial.isAir(block);

			for (; loc.getY() + dy > 1; --dy)
				if (!CompMaterial.isAir(block.getRelative(0, dy, 0))) {
					if (last)
						break;
				} else
					last = true;

			// find available space
			last = false;

			for (int y = (int) loc.getY(); dy < 15 && y <= maxY; ++dy, ++y)
				if (CompMaterial.isAir(block.getRelative(0, dy, 0))) {
					if (last)
						return loc.add(0, dy, 0);

					last = true;
				} else
					last = false;
		}

		return null;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-minions-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.SHEEP_SPAWN_EGG,
				"Minions",
				"",
				"Boss will spawn reinforcements",
				"to help him chase the player!")
				.make();
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.bossNames = map.getSet("Boss_Names", String.class);
		this.radius = map.getInteger("Radius", 5);
		this.amount = map.getInteger("Amount", 1);
	}

	@Override
	public SerializedMap writeSettings() {
		final SerializedMap map = new SerializedMap();

		map.put("Boss_Names", this.bossNames);
		map.put("Radius", this.radius);
		map.put("Amount", this.amount);

		return map;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return new SkillSettingsMenu(parent);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private class SkillSettingsMenu extends Menu {

		@Position(start = StartPosition.CENTER, value = -2)
		private final Button amountButton;

		@Position(start = StartPosition.CENTER)
		private final Button selectBossesButton;

		@Position(start = StartPosition.CENTER, value = +2)
		private final Button radiusButton;

		SkillSettingsMenu(Menu parent) {
			super(parent);

			this.setSize(9 * 3);
			this.setTitle("Minions Skill Settings");

			this.amountButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.INK_SAC,
					"Amount",
					"",
					"Current: &f" + SkillMinions.this.amount,
					"",
					"How many baby mobs that",
					"look like Boss to spawn",
					"around player? These are",
					"not Bosses, only have the",
					"same entity type."),

					player -> {
						new SimpleDecimalPrompt("Enter how many minions to spawn from each Boss. Current: '" + SkillMinions.this.amount + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final int newRadius = Integer.parseInt(input);

									if (newRadius > 0 && newRadius <= 20) {
										SkillMinions.this.amount = newRadius;
										SkillMinions.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid amount, enter a whole number between 1-20";
							}

						}.show(player);
					});

			this.selectBossesButton = new ButtonMenu(new BossSelectionMenu(), CompMaterial.SHEEP_SPAWN_EGG,
					"Select Bosses",
					"",
					"Select which Bosses will",
					"spawn as minions.");

			this.radiusButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.ENDER_PEARL,
					"Radius",
					"",
					"Current: &f" + SkillMinions.this.radius,
					"",
					"How many blocks around",
					"Boss looks for players?"),

					player -> {
						new SimpleDecimalPrompt("Enter how many blocks around the Boss will look for players to spawn minions for. Current: '" + SkillMinions.this.radius + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final int newRadius = Integer.parseInt(input);

									if (newRadius > 0 && newRadius <= 50) {
										SkillMinions.this.radius = newRadius;
										SkillMinions.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid radius, enter a whole number between 1-50";
							}

						}.show(player);

					});
		}

		@Override
		public Menu newInstance() {
			return new SkillSettingsMenu(this.getParent());
		}

		class BossSelectionMenu extends MenuPaged<Boss> {

			public BossSelectionMenu() {
				super(SkillSettingsMenu.this, Boss.getBosses());

				this.setTitle("Select Minion Bosses");
			}

			@Override
			protected String[] getInfo() {
				return new String[] {
						"Select which other Bosses",
						"will spawn as reinforcement",
						"for the minion skill.",
						"",
						"We recommend creating a completely",
						"new Boss just for minions and",
						"setting its size to 'Baby' if",
						"possible, for best experience :)"
				};
			}

			@Override
			protected ItemStack convertToItemStack(Boss item) {
				final boolean has = bossNames.contains(item.getName());

				return (item.getEggMaterial() == null ? (item.getType() == CompEntityType.PLAYER ? ItemCreator.fromMaterial(CompMaterial.PLAYER_HEAD) : ItemCreator.fromMonsterEgg(item.getType())) : ItemCreator.fromMaterial(item.getEggMaterial()))
						.name(item.getName())
						.lore(
								"",
								has ? "&aSelected. Click to unselect." : "Click to select.")
						.glow(has)
						.make();
			}

			@Override
			protected void onPageClick(Player player, Boss boss, ClickType click) {
				final String name = boss.getName();
				final boolean has = bossNames.contains(name);

				if (has)
					bossNames.remove(name);
				else
					bossNames.add(name);

				restartMenu(has ? "&4Minion unselected" : "&2Minion selected!");
			}
		}

	}
}
