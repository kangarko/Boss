package org.mineacademy.boss.menu.boss;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.menu.SelectSpawnRuleMenu;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossLocation;
import org.mineacademy.boss.model.BossSpawnReason;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.RequiredArgsConstructor;

/**
 * The menu with limits for Boss spawning.
 */
final class SpawningMenu extends Menu {

	private final Boss boss;

	@Position(9 * 1 + 1)
	private final Button spawnRulesButton;

	@Position(9 * 1 + 3)
	private final Button worldLimitButton;

	@Position(9 * 1 + 5)
	private final Button nearbyBossesButton;

	@Position(9 * 1 + 7)
	private final Button applyToReasonsButton;

	@Position(start = StartPosition.BOTTOM_CENTER, value = -1)
	private final Button regionKeepingButton;

	@Position(start = StartPosition.BOTTOM_CENTER, value = 1)
	private final Button spawnEggButton;

	SpawningMenu(Menu parent, Boss boss) {
		super(parent);

		this.boss = boss;

		this.setSize(9 * 4);
		this.setTitle("Spawning");

		this.spawnRulesButton = new ButtonMenu(new SelectSpawnRuleMenu(this, boss), CompMaterial.REDSTONE,
				"&cSpawn Rules",
				"",
				"Click to manage spawn",
				"rules for this Boss.");

		this.worldLimitButton = new ButtonMenu(new EditWorldLimitMenu(),
				CompMaterial.GRASS_BLOCK,
				"World Limits",
				"",
				"Click to limit how",
				"many " + boss.getName() + " Bosses",
				"can appear in worlds.");

		this.nearbyBossesButton = new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				new EditRadiusPrompt(click == ClickType.LEFT).show(player);
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator.from(
						CompMaterial.FISHING_ROD,
						"Radius",
						"",
						"Current: Max &3" + boss.getNearbyBossesLimit() + " &7other",
						"&7Bosses &4" + MathUtil.formatTwoDigits(boss.getNearbyBossesRadiusForLimit()) + " &7blocks near",
						"&7the spawned Boss.",
						"",
						"&3&l< &7Left click to edit other.",
						"Boss limit.",
						"",
						"&4&l> &7Right click to edit block",
						"radius to scan for Bosses.").make();
			}
		};

		this.applyToReasonsButton = new ButtonMenu(new ReasonsSelectionMenu(),
				CompMaterial.PURPLE_DYE,
				"Where Limits Are Applied",
				"",
				"You can turn on/off applying",
				"these limits for certain",
				"ways the Boss is spawned,",
				"such as through Boss egg.");

		this.regionKeepingButton = new ButtonMenu(new RegionKeepingMenu(),
				CompMaterial.SLIME_BLOCK,
				"Region Keeping",
				"",
				"Enable forcing Boss to stay",
				"within the Boss Region he",
				"spawned in, if it exists.");

		this.spawnEggButton = new ButtonMenu(new SpawnEggMenu(),
				CompMaterial.ENDERMAN_SPAWN_EGG,
				"Spawn Egg Appearance",
				"",
				"Customize spawn egg material,",
				"title or its lore.");
	}

	private class EditWorldLimitMenu extends MenuPaged<World> {

		EditWorldLimitMenu() {
			super(SpawningMenu.this, Bukkit.getWorlds(), true);

			this.setTitle("World Spawning Limits");
		}

		@Override
		protected ItemStack convertToItemStack(World world) {
			final Environment env = world.getEnvironment();
			final CompMaterial material = env == Environment.NETHER ? CompMaterial.NETHER_BRICK : env == Environment.THE_END ? CompMaterial.END_STONE : CompMaterial.GRASS_BLOCK;
			final String worldName = world.getName();

			return ItemCreator.from(
					material,
					(worldName.equals("world") ? "Main" : ChatUtil.capitalizeFully(worldName)) + " World",
					"",
					"Limit: &f" + (SpawningMenu.this.boss.getWorldLimit(worldName) == -1 ? "Unlimited" : SpawningMenu.this.boss.getWorldLimit(worldName)),
					"",
					"Click to edit maximum",
					"amount of Bosses that",
					"exist on this world.").make();
		}

		@Override
		protected void onPageClick(Player player, World item, ClickType click) {
			final String current = SpawningMenu.this.boss.getWorldLimit(item.getName()) == -1 ? "unlimited" : String.valueOf(SpawningMenu.this.boss.getWorldLimit(item.getName()));

			new SimpleDecimalPrompt("Enter how many " + SpawningMenu.this.boss.getName() + " Bosses can appear on " + item.getName() + " world. Currently: " + current + ". Type -1 for no limit.") {

				@Override
				protected boolean isInputValid(ConversationContext context, String input) {
					return Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), -1, 10_000);
				}

				@Override
				protected String getFailedValidationText(ConversationContext context, String invalidInput) {
					return "Invalid input: '" + invalidInput + "'. Enter a whole number from 0-10,000 or -1 for no limit.";
				}

				@Override
				protected void onValidatedInput(ConversationContext context, double input) {
					SpawningMenu.this.boss.setWorldLimit(item.getName(), (int) input);
				}
			}.show(player);
		}

		/**
		 * @see org.mineacademy.fo.menu.Menu#getInfo()
		 */
		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure how many Bosses",
					"can appear in worlds. We only",
					"count alive Bosses in loaded",
					"chunks for this limit."
			};
		}

		@Override
		public Menu newInstance() {
			return new EditWorldLimitMenu();
		}
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return new String[] {
				"Configure global limits for all",
				"Bosses of this kind to prevent",
				"flooding your server. Also see",
				"the Spawning section in settings.yml."
		};
	}

	@Override
	public Menu newInstance() {
		return new SpawningMenu(this.getParent(), this.boss);
	}

	@RequiredArgsConstructor
	private class EditRadiusPrompt extends SimpleDecimalPrompt {

		private final boolean leftClick;

		@Override
		protected String getPrompt(ConversationContext ctx) {
			return "Enter the " + (this.leftClick ? "other Boss limit in radius" : "block radius to scan for other Bosses")
					+ ". Current: '" + (this.leftClick ? SpawningMenu.this.boss.getNearbyBossesLimit() : MathUtil.formatTwoDigits(SpawningMenu.this.boss.getNearbyBossesRadiusForLimit())) + "'." + (this.leftClick ? " Enter 0 to disable." : "");
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if (this.leftClick)
				return Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), 0, 100);

			else
				return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), 1, 200);
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Invalid input '" + invalidInput + "'! Please enter a " + (this.leftClick ? "whole" : "decimal") + " number between " + (this.leftClick ? "0-100" : "1-200") + ".";
		}

		@Override
		protected void onValidatedInput(ConversationContext context, double input) {
			if (this.leftClick)
				SpawningMenu.this.boss.setNearbyBossesLimit(new Tuple<>((int) input, SpawningMenu.this.boss.getNearbyBossesRadiusForLimit()));

			else
				SpawningMenu.this.boss.setNearbyBossesLimit(new Tuple<>(SpawningMenu.this.boss.getNearbyBossesLimit(), input));
		}
	}

	private class ReasonsSelectionMenu extends MenuPaged<BossSpawnReason> {

		ReasonsSelectionMenu() {
			super(SpawningMenu.this, Arrays.asList(BossSpawnReason.values()), true);

			this.setTitle("Select Where To Apply Limits");
		}

		@Override
		protected ItemStack convertToItemStack(BossSpawnReason reason) {
			final boolean has = SpawningMenu.this.boss.areLimitsAppliedTo(reason);

			return ItemCreator.from(
					reason.getIcon(),
					reason.getTitle(),
					"",
					"Status: " + (has ? "&aLimited" : "&cNo limits"),
					"",
					"Toggle if spawn limits",
					"should apply when Boss",
					"spawns by this cause.")
					.glow(has)
					.make();
		}

		@Override
		protected void onPageClick(Player player, BossSpawnReason reason, ClickType click) {
			final boolean has = SpawningMenu.this.boss.areLimitsAppliedTo(reason);

			if (has)
				SpawningMenu.this.boss.removeLimitReason(reason);
			else
				SpawningMenu.this.boss.addLimitReason(reason);

			this.restartMenu(has ? "&4Limits no longer applied here" : "&6" + reason.getTitle() + " is now limited!");
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select what spawn causes should",
					"be filtered by limits."
			};
		}
	}

	private class RegionKeepingMenu extends Menu {

		@Position(value = 9 * 1 + 3)
		private final Button enabledButton;

		@Position(value = 9 * 1 + 5)
		private final Button returnLocationButton;

		RegionKeepingMenu() {
			super(SpawningMenu.this);

			this.enabledButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final boolean has = SpawningMenu.this.boss.isKeptInSpawnRegion();
					SpawningMenu.this.boss.setKeptInSpawnRegion(!has);

					final Menu newInstance = RegionKeepingMenu.this.newInstance();
					newInstance.displayTo(player);

					Platform.runTask(1, () -> newInstance.restartMenu(!has ? "&2Enabled region keeping." : "&4Disabled region keeping."));
				}

				@Override
				public ItemStack getItem() {
					final boolean has = SpawningMenu.this.boss.isKeptInSpawnRegion();

					return ItemCreator.from(
							(has ? CompMaterial.BEACON : CompMaterial.GLASS),
							"Region Keeping",
							"",
							"Status: " + (has ? "&aEnabled" : "&cDisabled"),
							"",
							"If a Boss spawns inside a Boss",
							"Region, restrict his movement",
							"to that regions?")
							.glow(has)
							.make();
				}
			};

			this.returnLocationButton = new ButtonMenu(new RegionKeepingLocationsMenu(this),
					CompMaterial.GRASS_BLOCK,
					"Return Location",
					"",
					"Current: " + (SpawningMenu.this.boss.getEscapeReturnLocation() != null ? "&a" + SpawningMenu.this.boss.getEscapeReturnLocation() : "&7Spawn location."),
					"",
					"Select location to return Boss to when",
					"he escapes his spawn region. If none",
					"is selected, we return Boss to his",
					"spawn origin location.",
					"",
					"When Citizens are installed, Boss will",
					"walk to the location. Otherwise he will",
					"be teleported.");
		}

		@Override
		public Menu newInstance() {
			return new RegionKeepingMenu();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"We automatically find the Boss Region",
					"where your Boss spawned and can force",
					"to Boss to stay within its box.",
					"",
					"NB: This only works properly if there's",
					"only 1 Boss Region where Boss spawned,",
					"if multiple regions intersect a location,",
					"we return the first we find."
			};
		}

		private class RegionKeepingLocationsMenu extends MenuPaged<BossLocation> {

			RegionKeepingLocationsMenu(Menu parent) {
				super(parent, BossLocation.getLocations(), true);

				this.setTitle("Select Location To Return");
			}

			@Override
			protected ItemStack convertToItemStack(BossLocation location) {

				final CompMaterial material = CompMaterial.GRASS_BLOCK;
				final boolean selected = location.getName().equals(SpawningMenu.this.boss.getEscapeReturnLocation());

				return ItemCreator.from(
						material,
						location.getName(),
						"",
						(selected ? "&aActive. &7Click to unselect." : "&7Inactive. Click to select."),
						"",
						"Click to edit maximum",
						"amount of Bosses that",
						"exist on this world.")
						.glow(selected).make();
			}

			@Override
			protected void onPageClick(Player player, BossLocation location, ClickType click) {
				final boolean selected = location.getName().equals(SpawningMenu.this.boss.getEscapeReturnLocation());

				if (!selected) {
					SpawningMenu.this.boss.setEscapeReturnLocation(location);

					this.restartMenu("&2Location selected!");
				}

				else {
					SpawningMenu.this.boss.setEscapeReturnLocation(null);

					this.restartMenu("&4Location unselected!");
				}
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#getInfo()
			 */
			@Override
			protected String[] getInfo() {
				return new String[] {
						"Select the location where we return",
						"your Boss (either teleporting or, if",
						"Citizens is installed, walking), when",
						"he escapes his spawn region."
				};
			}

			@Override
			public Menu newInstance() {
				return new RegionKeepingLocationsMenu(this.getParent());
			}
		}
	}

	private class SpawnEggMenu extends Menu {

		@Position(value = 9 * 1 + 2)
		private final Button materialButton;

		@Position(value = 9 * 1 + 4)
		private final Button titleButton;

		@Position(value = 9 * 1 + 6)
		private final Button loreButton;

		@Position(start = StartPosition.BOTTOM_CENTER)
		private final Button previewButton;

		SpawnEggMenu() {
			super(SpawningMenu.this);

			this.setSize(9 * 4);

			this.materialButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.ENDERMAN_SPAWN_EGG,
					"Egg Material",
					"",
					"Current: &f" + ChatUtil.capitalizeFully(Common.getOrDefault(SpawningMenu.this.boss.getEggMaterial(), SpawningMenu.this.boss.getEggDefaultMaterial())),
					"",
					"Edit the material of",
					"the Boss spawn egg."),
					player -> {
						new SimpleStringPrompt("Please enter a material for the boss egg from https://mineacademy.org/material. Current: '" + (SpawningMenu.this.boss.getEggMaterial() == null ? "default"
								: ChatUtil.capitalizeFully(SpawningMenu.this.boss.getEggMaterial())) + "&7' Type 'default' to reset to '" + ChatUtil.capitalizeFully(SpawningMenu.this.boss.getEggDefaultMaterial()) + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								if (input.equalsIgnoreCase("default"))
									return true;

								try {
									CompMaterial.valueOf(input.toUpperCase());

									return true;
								} catch (final Throwable t) {
									return false;
								}
							}

							@Override
							protected void onValidatedInput(ConversationContext context, String input) {
								SpawningMenu.this.boss.setEggMaterial("default".equalsIgnoreCase(input) ? null : CompMaterial.valueOf(input.toUpperCase()));
							}
						}.show(player);
					});

			this.titleButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.PAPER,
					"Egg Title",
					"",
					"Current: &f" + (SpawningMenu.this.boss.getEggTitle() == null ? SpawningMenu.this.boss.getEggDefaultTitle() : SpawningMenu.this.boss.getEggTitle()),
					"",
					"Edit the title of",
					"the Boss spawn egg."),
					player -> {
						new SimpleStringPrompt("Please enter the Boss spawn egg name. Current: '" + (SpawningMenu.this.boss.getEggTitle() == null ? "default" : SpawningMenu.this.boss.getEggTitle()) + "&7' Type 'default' to reset to '" + SpawningMenu.this.boss.getEggDefaultTitle() + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {
								return Valid.isInRange(input.length(), 3, 256);
							}

							@Override
							protected void onValidatedInput(ConversationContext context, String input) {
								SpawningMenu.this.boss.setEggTitle("default".equalsIgnoreCase(input) ? null : input);
							}
						}.show(player);
					});

			this.loreButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.BOOK,
					"Egg Lore",
					"",
					"Current: &f",
					String.join("\n", SpawningMenu.this.boss.getEggLore() == null || SpawningMenu.this.boss.getEggLore().isEmpty() ? SpawningMenu.this.boss.getEggDefaultLore() : SpawningMenu.this.boss.getEggLore()),
					"",
					"Edit the lore of",
					"the Boss spawn egg."),
					player -> {
						new SimpleStringPrompt("Please enter the Boss spawn egg lore, separate multiple lines by |. Current: '" + (SpawningMenu.this.boss.getEggLore() == null || SpawningMenu.this.boss.getEggLore().isEmpty()
								? "default"
								: SpawningMenu.this.boss.getEggLore()) + "&7' Type 'default' to reset to '" + SpawningMenu.this.boss.getEggDefaultLore() + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {
								return Valid.isInRange(input.length(), 3, 256);
							}

							@Override
							protected void onValidatedInput(ConversationContext context, String input) {
								SpawningMenu.this.boss.setEggLore("default".equalsIgnoreCase(input) ? null : Arrays.asList(input.split("\\|")));
							}
						}.show(player);
					});

			this.previewButton = Button.makeSimple(ItemCreator.fromItemStack(SpawningMenu.this.boss.getEgg()), player -> {
				player.getInventory().addItem(SpawningMenu.this.boss.getEgg());

				this.animateTitle("Egg added to inventory!");
			});
		}

		@Override
		public Menu newInstance() {
			return new SpawnEggMenu();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Customize the appearance of",
					"the egg with which you can",
					"spawn " + SpawningMenu.this.boss.getName() + " easily."
			};
		}
	}

}
