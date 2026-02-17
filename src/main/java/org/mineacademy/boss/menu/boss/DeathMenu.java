package org.mineacademy.boss.menu.boss;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossReinforcement;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuContainerChances;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonConversation;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.ButtonRemove;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMonsterEgg;
import org.mineacademy.fo.settings.Lang;

/**
 * The menu with settings on Boss death.
 */
final class DeathMenu extends Menu {

	private final Boss boss;

	@Position(9 + 2)
	private final Button dropsButton;

	@Position(9 + 4)
	private final Button droppedExpButton;

	@Position(9 + 6)
	private final Button reinforcementsButton;

	DeathMenu(Menu parent, Boss boss) {
		super(parent, true);

		this.boss = boss;

		this.setTitle(Lang.legacy("menu-death-title"));
		this.setSize(9 * 3);

		this.dropsButton = new ButtonMenu(new DropsMenu(),
				CompMaterial.STICK,
				Lang.legacy("menu-death-button-drops"),
				Lang.legacy("menu-death-button-drops-lore").split("\n"));

		this.droppedExpButton = new ButtonConversation(new DroppedExpPrompt(), ItemCreator.from(
				CompMaterial.EXPERIENCE_BOTTLE,
				Lang.legacy("menu-death-button-exp"),
				Lang.legacy("menu-death-button-exp-lore", "exp", boss.getDroppedExpAsString()).split("\n")));

		this.reinforcementsButton = new ButtonMenu(new ReinforcementsMenu(),
				CompMaterial.MAGMA_CREAM,
				Lang.legacy("menu-death-button-reinforcements"),
				Lang.legacy("menu-death-button-reinforcements-lore").split("\n"));
	}

	private class DroppedExpPrompt extends SimpleStringPrompt {

		DroppedExpPrompt() {
			super("Enter dropped experience after Boss death. Current: " + DeathMenu.this.boss.getDroppedExpAsString() + ". Type 0 to disable dropped exp or 'default' to use Minecraft default.");
		}

		@Override
		protected String getMenuAnimatedTitle() {
			return "&9Dropped exp set to " + DeathMenu.this.boss.getDroppedExpAsString();
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if ("-1".equals(input) || "default".equals(input))
				return true;

			try {
				final RangedValue value = RangedValue.fromString(input);

				return value.getMinLong() >= 0 && value.getMaxLong() <= 10_000 && value.getMinLong() <= value.getMaxLong();
			} catch (final Throwable t) {
			}

			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Invalid input: '" + invalidInput + "'. Enter a value such as '80' or a range like '0 - 100' up to 10,000.";
		}

		@Override
		protected void onValidatedInput(ConversationContext context, String input) {
			if ("-1".equals(input) || "default".equals(input))
				DeathMenu.this.boss.setDroppedExp(null);

			else {
				final RangedValue value = RangedValue.fromString(input);

				DeathMenu.this.boss.setDroppedExp(value);
			}
		}
	}

	@Override
	public Menu newInstance() {
		return new DeathMenu(this.getParent(), this.boss);
	}

	@Override
	protected String[] getInfo() {
		return Lang.legacy("menu-death-info").split("\n");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private class DropsMenu extends Menu {

		@Position(9 * 1 + 2)
		private final Button generalDropsButton;

		@Position(9 * 1 + 4)
		private final Button playerDropsButton;

		@Position(9 * 1 + 6)
		private final Button vanillaDropsButton;

		DropsMenu() {
			super(DeathMenu.this);

			this.setTitle(Lang.legacy("menu-death-drops-title"));

			this.generalDropsButton = new ButtonMenu(new GeneralDropsMenu(),
					CompMaterial.ZOMBIE_HEAD,
					Lang.legacy("menu-death-drops-button-general"),
					Lang.legacy("menu-death-drops-button-general-lore").split("\n"));

			this.playerDropsButton = new ButtonMenu(new PlayerDropsMenu(),
					CompMaterial.PLAYER_HEAD,
					Lang.legacy("menu-death-drops-button-player"),
					Lang.legacy("menu-death-drops-button-player-lore").split("\n"));

			this.vanillaDropsButton = Button.makeBoolean(ItemCreator.from(
					CompMaterial.YELLOW_DYE,
					Lang.legacy("menu-death-drops-button-vanilla"),
					Lang.legacy("menu-death-drops-button-vanilla-lore").split("\n")),
					DeathMenu.this.boss::hasVanillaDrops, DeathMenu.this.boss::setVanillaDrops);
		}

		/**
		 * @see org.mineacademy.fo.menu.Menu#getInfo()
		 */
		@Override
		protected String[] getInfo() {
			return Lang.legacy("menu-death-drops-info").split("\n");
		}

		@Override
		public Menu newInstance() {
			return new DropsMenu();
		}

		private class GeneralDropsMenu extends MenuContainerChances {

			GeneralDropsMenu() {
				super(DropsMenu.this, true);

				this.setSize(9 * 6);
				this.setTitle(Lang.legacy("menu-death-drops-general-title"));
			}

			@Override
			public boolean allowDecimalQuantities() {
				return true;
			}

			@Override
			protected ItemStack getDropAt(int slot) {
				return DeathMenu.this.boss.getGeneralDropAt(slot);
			}

			@Override
			protected double getDropChance(int slot) {
				return Common.getOrDefault(DeathMenu.this.boss.getGeneralDropChanceAt(slot), 1D);
			}

			@Override
			protected boolean canEditItem(int slot) {
				return true;
			}

			@Override
			protected void onMenuClose(Map<Integer, Tuple<ItemStack, Double>> slotItems) {
				DeathMenu.this.boss.setGeneralDrops(slotItems.values());
			}

			@Override
			protected int getChangeModeButtonPosition() {
				return this.getSize() - 2;
			}

			@Override
			protected String[] getInfo() {
				return Lang.legacy("menu-death-drops-general-info").split("\n");
			}

			@Override
			public GeneralDropsMenu newInstance() {
				return this;
			}
		}

		private class PlayerDropsMenu extends MenuPaged<Integer> {

			@Position(start = StartPosition.BOTTOM_RIGHT, value = -1)
			private final Button thresholdButton;

			@Position(start = StartPosition.BOTTOM_RIGHT)
			private final Button createButton;

			PlayerDropsMenu() {
				super(DropsMenu.this, DeathMenu.this.boss.getPlayerDropsOrders(), true);

				this.setTitle("Player Boss Drops");

				final int nextOrder = DeathMenu.this.boss.getPlayerDropsOrders().size() + 1;

				this.thresholdButton = Button.makeSimple(ItemCreator.from(
						CompMaterial.CLOCK,
						"Last Damage Time Threshold",
						"",
						"Current: &f" + DeathMenu.this.boss.getPlayerDropsTimeThreshold().getRaw(),
						"",
						"When rewarding players who",
						"did the most damage to Boss,",
						"in what time period we should",
						"look for their last damage?",
						"Example: in the last 2 minutes"),
						player -> {
							new SimpleStringPrompt("Enter the time limit we apply when looking for players who damaged Boss the last. (Keep in mind the damager cache is reset upon reloading or server restart). Current: '"
									+ DeathMenu.this.boss.getPlayerDropsTimeThreshold().getRaw() + "'.") {

								@Override
								protected boolean isInputValid(ConversationContext context, String input) {

									try {
										final SimpleTime newValue = SimpleTime.fromString(input);

										if (newValue.getTimeTicks() > 0) {
											DeathMenu.this.boss.setPlayerDropsTimeThreshold(newValue);

											return true;
										}

									} catch (final Throwable t) {
										// see getFailedValiationText
									}

									return false;
								}

								@Override
								protected String getFailedValidationText(ConversationContext context, String invalidInput) {
									return "Invalid time, enter a human readable format such as '3 seconds' between 1 tick and 10 minutes.";
								}

							}.show(player);
						});

				this.createButton = new ButtonMenu(new IndividualPlayerDropMenu(nextOrder), CompMaterial.EMERALD,
						"&aCreate New",
						"",
						"Click to create",
						"new drops for player",
						"who did the #" + nextOrder,
						"most damage to Boss.");
			}

			@Override
			protected ItemStack convertToItemStack(Integer order) {
				return ItemCreator
						.from(CompMaterial.WHITE_STAINED_GLASS,
								"Player #" + order,
								"",
								"Click to edit drops",
								"for this player.")
						.color(CompColor.values()[order % CompColor.values().length])
						.make();
			}

			@Override
			protected void onPageClick(Player player, Integer order, ClickType click) {
				new IndividualPlayerDropMenu(order).displayTo(player);
			}

			@Override
			protected String[] getInfo() {
				return new String[] {
						"You can give players who did the",
						"most damage different items, in",
						"the other in which they did the",
						"most damage."
				};
			}

			@Override
			public Menu newInstance() {
				return new PlayerDropsMenu();
			}

			private class IndividualPlayerDropMenu extends MenuContainerChances {

				private final int order;

				@Position(start = StartPosition.BOTTOM_RIGHT, value = -2)
				private final Button removeButton;

				@Position(start = StartPosition.BOTTOM_RIGHT, value = -1)
				private final Button commandsButton;

				IndividualPlayerDropMenu(int order_) {
					super(PlayerDropsMenu.this, true);

					final int order = order_ - 1;
					this.order = order;

					this.setTitle("#" + (order + 1) + (order == 0 ? " Top" : "") + " Damager Drops");

					this.removeButton = new ButtonRemove(this, "player drop", "#" + (order + 1), () -> {
						DeathMenu.this.boss.removePlayerDrops(order);

						PlayerDropsMenu.this.newInstance().displayTo(this.getViewer());
					});

					this.commandsButton = new Button() {

						@Override
						public void onClickedInMenu(Player player, Menu menu, ClickType click) {

							if (click == ClickType.LEFT) {
								if (!DeathMenu.this.boss.getPlayerDropsCommands(order).isEmpty()) {
									DeathMenu.this.boss.removePlayerDropsCommand(order);

									IndividualPlayerDropMenu.this.restartMenu("&4Command has been removed!");

								} else
									IndividualPlayerDropMenu.this.animateTitle("&4No command to remove!");

							} else
								new SimpleStringPrompt("Enter the command to run as console without /, separate | for multiple commands. Use the {order}, {killer}, {damage} and Boss variables here.",
										newCommand -> DeathMenu.this.boss.setPlayerDropsCommand(order, newCommand)).show(player);
						}

						@Override
						public ItemStack getItem() {
							String command = String.join("\n", DeathMenu.this.boss.getPlayerDropsCommands(order));

							if (command == null || command.isEmpty())
								command = null;

							else
								command = String.join("\n   &f", "/" + command);

							return ItemCreator.from(CompMaterial.BOOK,
									"Command",
									"",
									command == null ? " &ounset" : " - &f" + command,
									"",
									"&4&l< &7Left click to delete",
									"&9&l> &7Right click to edit")
									.glow(command != null)
									.make();
						}
					};

				}

				private List<Map.Entry<ItemStack, Double>> getItems(int order) {
					final Map<ItemStack, Double> orderDrops = DeathMenu.this.boss.getPlayerDrops(this.order);

					return orderDrops == null ? new ArrayList<>() : new ArrayList<>(orderDrops.entrySet());
				}

				@Override
				public boolean allowDecimalQuantities() {
					return true;
				}

				@Override
				protected ItemStack getDropAt(int slot) {
					final List<Map.Entry<ItemStack, Double>> items = this.getItems(this.order);

					return slot < items.size() ? items.get(slot).getKey() : null;
				}

				@Override
				protected double getDropChance(int slot) {
					final List<Map.Entry<ItemStack, Double>> items = this.getItems(this.order);

					return slot < items.size() ? items.get(slot).getValue() : 1.0D;
				}

				@Override
				protected boolean canEditItem(int slot) {
					return true;
				}

				@Override
				protected void onMenuClose(Map<Integer, Tuple<ItemStack, Double>> slotItems) {
					final Map<ItemStack, Double> items = new LinkedHashMap<>();

					for (final Tuple<ItemStack, Double> tuple : slotItems.values())
						if (tuple != null)
							items.put(tuple.getKey(), tuple.getValue());

					DeathMenu.this.boss.setPlayerDrops(this.order, items);
				}

				@Override
				protected int getChangeModeButtonPosition() {
					return this.getSize() - 2;
				}

				@Override
				public Menu newInstance() {
					return new IndividualPlayerDropMenu(this.order + 1);
				}

				@Override
				protected String[] getInfo() {
					return new String[] {
							"Drag items into this container",
							"and set their drop chance. Boss",
							"will drop these items on death."
					};
				}
			}
		}
	}

	private class ReinforcementsMenu extends MenuContainerChances {

		ReinforcementsMenu() {
			super(DeathMenu.this);

			this.setTitle("Drop Reinforcements Here");
		}

		@Override
		public boolean allowDecimalQuantities() {
			return true;
		}

		@Override
		protected ItemStack getDropAt(int slot) {
			final List<BossReinforcement> reinforcements = DeathMenu.this.boss.getReinforcements();

			if (slot < reinforcements.size()) {
				final BossReinforcement reinforcement = reinforcements.get(slot);
				final Boss boss = reinforcement.getBossName() != null ? Boss.findBoss(reinforcement.getBossName()) : null;

				// Since removed Boss
				if (reinforcement.getBossName() != null && boss == null)
					return NO_ITEM;

				if (boss != null)
					return ItemCreator
							.fromItemStack(boss.getEgg(reinforcement.getAmount()))
							.name("Spawn Boss " + boss.getName())
							.clearLore()
							.lore("")
							.lore("Spawn Boss reinforcement.")
							.lore(this.getMode() == EditMode.ITEM ? "Chance: " + MathUtil.formatTwoDigits(reinforcement.getChance() * 100) + "%" : null)
							.make();

				else
					return ItemCreator
							.fromMonsterEgg(reinforcement.getEntityType())
							.name("Spawn " + ChatUtil.capitalizeFully(reinforcement.getEntityType()))
							.clearLore()
							.lore("")
							.lore("Spawn vanilla reinforcement.")
							.lore(this.getMode() == EditMode.ITEM ? "Chance: " + MathUtil.formatTwoDigits(reinforcement.getChance() * 100) + "%" : null)
							.amount(reinforcement.getAmount())
							.make();
			}

			return NO_ITEM;
		}

		@Override
		protected double getDropChance(int slot) {
			final List<BossReinforcement> reinforcements = DeathMenu.this.boss.getReinforcements();

			return slot < reinforcements.size() ? reinforcements.get(slot).getChance() : 1.0D;
		}

		@Override
		protected int getChangeModeButtonPosition() {
			return this.getSize() - 2;
		}

		@Override
		protected boolean canEditItem(int slot) {
			return true;
		}

		@Override
		protected boolean canEditItem(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor, InventoryAction action) {
			final ItemStack item = clicked != null && !CompMaterial.isAir(clicked) ? clicked : cursor;

			if (Boss.findBoss(item) == null) {
				this.animateTitle("&4Only place Boss eggs here!");

				return false;
			}

			return true;
		}

		@Override
		protected void onMenuClose(Map<Integer, Tuple<ItemStack, Double>> items) {
			final List<BossReinforcement> reinforcements = new ArrayList<>();

			for (final Tuple<ItemStack, Double> tuple : items.values()) {

				if (tuple == null)
					continue;

				final ItemStack item = tuple.getKey();
				final double chance = tuple.getValue();

				if (item == null || CompMaterial.isAir(item))
					continue;

				final Boss boss = Boss.findBoss(item);

				if (boss != null)
					reinforcements.add(BossReinforcement.fromBoss(boss, item.getAmount(), chance));

				else if (CompMaterial.isMonsterEgg(item.getType()))
					reinforcements.add(BossReinforcement.fromVanilla(CompMonsterEgg.lookupEntity(item), item.getAmount(), chance));
			}

			boss.setReinforcements(reinforcements);
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Place vanilla or Boss spawn eggs",
					"to this menu to spawn them when",
					"this Boss dies."
			};
		}

		@Override
		public ReinforcementsMenu newInstance() {
			return this;
		}
	}
}
