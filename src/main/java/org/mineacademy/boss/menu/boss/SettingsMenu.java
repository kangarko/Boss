package org.mineacademy.boss.menu.boss;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.ticxo.modelengine.api.ModelEngineAPI;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.boss.custom.CustomSetting;
import org.mineacademy.boss.goal.GoalManagerCheck;
import org.mineacademy.boss.hook.ModelEngineHook;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossAttribute;
import org.mineacademy.boss.model.BossCitizensSettings;
import org.mineacademy.boss.model.BossCommandType;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuContainer;
import org.mineacademy.fo.menu.MenuContainerChances;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.MenuQuantitable;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.menu.model.MenuQuantity;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.CompEquipmentSlot;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMonsterEgg;
import org.mineacademy.fo.remain.CompPotionEffectType;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.Setter;

/**
 * The menu with main Boss settings.
 */
class SettingsMenu extends Menu {

	private final Boss boss;

	@Position(9 + 1)
	private final Button aliasButton;

	@Position(9 + 3)
	private final Button healthButton;

	@Position(9 + 5)
	private final Button potionsButton;

	@Position(9 + 7)
	private final Button equipmentButton;

	@Position(9 * 3 + 1)
	private final Button lightningButton;

	@Position(9 * 3 + 2)
	private final Button pathfindersButton;

	@Position(9 * 3 + 3)
	private final Button ridingButton;

	@Position(9 * 3 + 5)
	private final Button commandsButton;

	@Position(9 * 3 + 6)
	private final Button attributesButton;

	@Position(9 * 3 + 7)
	private final Button customSettingsButton;

	@Position(9 * 5 + 7)
	private final Button customModelsButton;

	SettingsMenu(Menu parent, Boss boss) {
		super(parent);

		this.boss = boss;
		this.setSize(9 * 6);
		this.setTitle(boss.getName() + " Settings");

		Valid.checkNotNull(boss.getCitizensSettings(), "Unable to load Boss settings. Check for earlier errors. If you used PlugMan or reloaded, try without it. Otherwise, report to the developer.");
		final boolean isNpc = boss.getCitizensSettings().isEnabled();

		this.aliasButton = Button.makeSimple(ItemCreator.from(
				CompMaterial.PAPER,
				"Alias",
				"",
				"Current: &f" + ("hidden".equals(boss.getAlias()) ? "&dHidden" : boss.getAlias()),
				"",
				"Edit the name above",
				"Boss' head, and in",
				"messages or commands.",
				"",
				"&c[!] &7This does not affect",
				"the file name: &f" + boss.getFile().getName()), player -> {
			new SimpleStringPrompt("Enter the Boss alias (for NPCs this is limited to 16 letters including colors). Use & color colors or minimessage tags (put hex colors in <>) Current: '" + boss.getAlias() + "&7' Type 'hidden' to hide, or 'default' to reset to '" + boss.getName() + "'.") {

				@Override
				protected boolean isInputValid(ConversationContext context, String input) {
					return Valid.isInRange(input.length(), 3, 256);
				}

				@Override
				protected void onValidatedInput(ConversationContext context, String input) {
					boss.setAlias(input);
				}
			}.show(player);
		});

		this.healthButton = Button.makeSimple(ItemCreator.from(
				Remain.getMaterial("BEETROOT", CompMaterial.REDSTONE),
				"&cHealth",
				"",
				"Current: &c" + boss.getMaxHealth() + " HP",
				"",
				"Click to edit Boss'",
				"spawn health."), player -> {
			new SimpleDecimalPrompt("Please enter the Boss max health. Current: " + boss.getMaxHealth() + " HP. Maximum (editable in spigot.yml): "
									+ Remain.getMaxHealth() + ". Or type '0' to reset back to the vanilla HP.") {

				@Override
				protected boolean isInputValid(ConversationContext context, String input) {
					return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), 0, Remain.getMaxHealth());
				}

				@Override
				protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {

					if (Valid.isDecimal(invalidInput)) {
						final double health = Double.parseDouble(invalidInput);

						if (health < 0)
							return "Health cannot be negative!";

						else if (health > Remain.getMaxHealth())
							return "Health can't be over " + Remain.getMaxHealth() + ". Please increase 'settings.attribute.maxHealth' in spigot.yml file first. "
								   + "Keep in mind the server might not keep up with too high values because mobs were never designed to have them.";
					}

					return super.getFailedValidationText(context, invalidInput);
				}

				@Override
				protected void onValidatedInput(ConversationContext context, double input) {
					boss.setMaxHealth(input == 0 ? boss.getDefaultHealth() : input);
				}
			}.show(player);
		});

		this.potionsButton = new ButtonMenu(new PotionsMenu(),
				CompMaterial.POTION,
				"&dPotion Effects",
				"",
				"Add custom potion",
				"effects to the Boss.");

		this.equipmentButton = boss.canHaveEquipment() ? new ButtonMenu(new EquipmentMenu(),
				CompMaterial.LEATHER_CHESTPLATE,
				"&6Equipment",
				"",
				"Edit Boss' hand item",
				"and armor items.")
				: Button.makeDummy(CompMaterial.LEATHER_CHESTPLATE,
				"Equipment",
				"",
				"&cThis Boss type does",
				"&cnot support equipment.");

		this.lightningButton = new ButtonMenu(new LightningMenu(),
				CompMaterial.ENDER_PEARL,
				"&3Lightning",
				"",
				"Select when Boss",
				"strikes lightning.");

		this.pathfindersButton = new ButtonMenu(new PathfindersMenu(),
				CompMaterial.ITEM_FRAME,
				"&8Pathfinders",
				"",
				"Edit how Boss",
				"should behave and what",
				"pathfinders it should use.");

		this.ridingButton = new ButtonMenu(new RidingMenu(),
				CompMaterial.CARROT_ON_A_STICK,
				"&eRiding",
				"",
				"Select mobs this",
				"Boss should ride.",
				(isNpc ? "" : null),
				(isNpc ? "&cWarning: Citizens Boss can only" : null),
				(isNpc ? "&chave one riding which must also" : null),
				(isNpc ? "&cbe a Boss hooked with Citizens." : null));

		this.commandsButton = new ButtonMenu(new CommandsOverviewMenu(),
				CompMaterial.COMMAND_BLOCK,
				"&dCommands",
				"",
				"Select what commands should",
				"be executed when this Boss",
				"dies or is spawned.");

		this.attributesButton = new ButtonMenu(new AttributesMenu(),
				CompMaterial.ANVIL,
				"&7Attributes",
				"",
				"Edit attributes like speed",
				"follow range, damage, ...");

		this.customSettingsButton = new ButtonMenu(new CustomSettingsMenu(),
				Remain.getMaterial("JIGSAW", CompMaterial.REDSTONE_BLOCK),
				"&4Custom Settings",
				"",
				"Edit custom settings only",
				"applicable for this Boss.");

		this.customModelsButton = ModelEngineHook.isAvailable() ? new ButtonMenu(new CustomModelMenu(),
				CompMaterial.ARMOR_STAND,
				"&bCustom Models",
				"",
				"Enable and edit",
				"custom models.",
				"",
				"&cWarning: &7This feature is",
				"&7under heavy development.")
				: Button.makeDummy(CompMaterial.ARMOR_STAND,
				"&bCustom Models",
				"",
				"Enable and edit",
				"custom models.",
				"",
				"&cError: &7ModelEngine required!");

	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Edit the main Boss properties",
				"in this menu such as health etc."
		};
	}

	@Override
	public Menu newInstance() {
		return new SettingsMenu(this.getParent(), this.boss);
	}

	private Button generateBooleanButton(Menu menu, String type, Supplier<Boolean> isEnabled, Consumer<Boolean> setEnabled, String... lore) {
		return new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				final boolean has = isEnabled.get();

				setEnabled.accept(!has);
				menu.restartMenu(!has ? "&2Enabled " + type : "&4Disabled " + type);
			}

			@Override
			public ItemStack getItem() {
				final boolean has = isEnabled.get();

				return ItemCreator.from(
								has ? CompMaterial.BEACON : CompMaterial.GLASS,
								"Enable " + type + "?",
								"",
								"Status: " + (has ? "&aEnabled" : "&cDisabled"),
								"")
						.lore(lore)
						.glow(has)
						.make();
			}
		};
	}

	private SimplePrompt generateSoundPrompt(String type, Supplier<String> getDeathSound, Consumer<String> setDeathSound) {
		return new SimpleStringPrompt("Enter Boss " + type + " sound, type 'silent' for no sound, or type 'default' to reset. Current: " + Common.getOrDefault(getDeathSound.get(), "default") + ".") {

			@Override
			protected boolean isInputValid(ConversationContext context, String input) {
				final CompSound sound = CompSound.fromName(input);

				return "default".equals(input) || (sound != null && sound.getSound() != null);
			}

			@Override
			protected String getFailedValidationText(ConversationContext context, String invalidInput) {
				return "Unrecognized sound '" + invalidInput + "', please click the Wiki button in the menu for help. Type 'exit' to go back to menu.";
			}

			@Override
			protected String getMenuAnimatedTitle() {
				return "&9Updated " + type + " sound effect!";
			}

			@Override
			protected void onValidatedInput(ConversationContext context, String input) {
				setDeathSound.accept("default".equals(input) ? null : input);
			}
		};
	}

	private SimplePrompt generateRadiusPrompt(String type, Supplier<Integer> getRadius, Consumer<Integer> setRadius) {
		return new SimpleDecimalPrompt("Enter radius for target " + type + ", or type '-1' to reset. Current: " + getRadius.get() + ".") {

			@Override
			protected boolean isInputValid(ConversationContext context, String input) {
				return Valid.isInteger(input) && (Valid.isInRange(Integer.parseInt(input), 4, 40) || Integer.parseInt(input) == -1);
			}

			@Override
			protected String getFailedValidationText(ConversationContext context, String invalidInput) {
				return "Invalid radius '" + invalidInput + "', enter a number from 4-40";
			}

			@Override
			protected String getMenuAnimatedTitle() {
				return "&9Set " + type + " radius to " + getRadius.get();
			}

			@Override
			protected void onValidatedInput(ConversationContext context, double input) {
				setRadius.accept(input == -1 ? 24 : (int) input);
			}
		};
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private final class EquipmentMenu extends MenuContainerChances {

		private final ItemStack FILLER_ITEM = ItemCreator.from(CompMaterial.GRAY_STAINED_GLASS_PANE, " ").make();
		private final Map<Integer, CompEquipmentSlot> equipmentBySlot = new LinkedHashMap<>();

		private final Button randomEquipmentButton;
		private final boolean isHorse;

		EquipmentMenu() {
			super(SettingsMenu.this);

			this.setTitle("Boss Equipment");

			this.isHorse = boss.getType() == CompEntityType.HORSE;

			this.randomEquipmentButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final boolean hasRandom = SettingsMenu.this.boss.isEmptyEquipmentRandomlyEquipped();
					SettingsMenu.this.boss.setEmptyEquipmentRandomlyEquipped(!hasRandom);

					EquipmentMenu.this.restartMenu((hasRandom ? "&4Disabled" : "&2Enabled") + " random equipment");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							SettingsMenu.this.boss.isEmptyEquipmentRandomlyEquipped() ? CompMaterial.BEACON : CompMaterial.GLASS,
							"Random Equipment",
							"",
							"Status: " + (SettingsMenu.this.boss.isEmptyEquipmentRandomlyEquipped() ? "&aEnabled" : "&cDisabled"),
							"",
							"Let Minecraft give random",
							"equipment to empty slots?",
							"",
							"Click to toggle.").make();
				}
			};

			// Fill equipment slots

			if (!isHorse) {
				this.equipmentBySlot.put(1 + 9, CompEquipmentSlot.HAND);
				if (MinecraftVersion.atLeast(V.v1_9) && boss.getType() != CompEntityType.ENDERMAN)
					this.equipmentBySlot.put(2 + 9, CompEquipmentSlot.OFF_HAND);
				this.equipmentBySlot.put(4 + 9, CompEquipmentSlot.HEAD);
			}

			this.equipmentBySlot.put(5 + 9, CompEquipmentSlot.CHEST);

			if (!isHorse) {
				this.equipmentBySlot.put(6 + 9, CompEquipmentSlot.LEGS);
				this.equipmentBySlot.put(7 + 9, CompEquipmentSlot.FEET);
			}
		}

		@Override
		public boolean allowDecimalQuantities() {
			return true;
		}

		@Override
		public ItemStack getDropAt(int slot) {

			final CompEquipmentSlot toolTip = this.equipmentBySlot.get(slot + 9);
			final CompEquipmentSlot equipment = this.equipmentBySlot.get(slot);

			if (toolTip != null)
				return this.generateInfoIcon(toolTip);

			if (equipment != null)
				return SettingsMenu.this.boss.getEquipmentItem(equipment);

			if (slot == this.getSize() - 5)
				return this.randomEquipmentButton.getItem();

			return this.FILLER_ITEM;
		}

		private ItemStack generateInfoIcon(CompEquipmentSlot slot) {
			return ItemCreator.from(CompMaterial.BROWN_STAINED_GLASS_PANE, "&7Place &6" + slot.toString().toLowerCase().replace("_", " ") + " item &7below.").make();
		}

		@Override
		protected boolean canEditItem(int slot) {
			return this.equipmentBySlot.containsKey(slot);
		}

		@Override
		protected double getDropChance(int slot) {
			final CompEquipmentSlot equipmentSlot = this.equipmentBySlot.get(slot);

			return equipmentSlot != null ? SettingsMenu.this.boss.getEquipmentDropChance(equipmentSlot) : 1.0;
		}

		@Override
		protected boolean canEditItem(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor, InventoryAction action) {

			if (slot == 10 && boss.getType() == CompEntityType.ENDERMAN && !cursor.getType().isBlock()) {
				this.animateTitle("&4Enderman cannot carry items");

				return false;
			}

			if (slot == 10 || slot == 11 && MinecraftVersion.atLeast(V.v1_9) || slot == 13)
				return true;

			if (cursor == null && clicked != null)
				cursor = clicked;

			if (slot == 14) {

				if (this.isHorse) {
					if (!CompMaterial.isAir(cursor.getType()) && !cursor.getType().toString().contains("HORSE_ARMOR")) {
						this.animateTitle("&4Item must be horse armor!");

						return false;
					}
				} else {
					if (!CompMaterial.isAir(cursor.getType()) && !cursor.getType().toString().contains("CHESTPLATE")) {
						this.animateTitle("&4Item must be chestplate!");

						return false;
					}
				}

				return true;
			}

			if (slot == 15) {
				if (!CompMaterial.isAir(cursor.getType()) && !cursor.getType().toString().contains("LEGGINGS")) {
					this.animateTitle("&4Item must be leggings!");

					return false;
				}

				return true;
			}

			if (slot == 16) {
				if (!CompMaterial.isAir(cursor.getType()) && !cursor.getType().toString().contains("BOOTS")) {
					this.animateTitle("&4Item must be boots!");

					return false;
				}

				return true;
			}

			return false;
		}

		@Override
		protected void onMenuClose(Map<Integer, Tuple<ItemStack, Double>> items) {

			for (final Map.Entry<Integer, CompEquipmentSlot> entry : this.equipmentBySlot.entrySet()) {
				final int slot = entry.getKey();
				final CompEquipmentSlot equipment = entry.getValue();

				Tuple<ItemStack, Double> tuple = items.get(slot);

				if (tuple != null && (tuple.getKey() == null || CompMaterial.isAir(tuple.getKey().getType())))
					tuple = null;

				SettingsMenu.this.boss.setEquipmentNoSave(equipment, tuple);
			}

			SettingsMenu.this.boss.save();
		}

		/**
		 * @see org.mineacademy.fo.menu.MenuContainerChances#getInfo()
		 */
		@Override
		protected String[] getInfo() {
			return new String[] {
					"Drag and drop items from your",
					"inventory into the available",
					"slots in this container for",
					"Boss to drop them on death."
			};
		}
	}

	private class PotionsMenu extends MenuPaged<PotionEffectType> implements MenuQuantitable {

		@Setter
		@Getter
		private MenuQuantity quantity = MenuQuantity.ONE;

		PotionsMenu() {
			super(SettingsMenu.this, CompPotionEffectType.getPotions());

			this.setTitle("Boss Potions");
		}

		@Override
		protected ItemStack convertToItemStack(PotionEffectType type) {
			final int level = SettingsMenu.this.boss.getPotionEffectLevel(type);

			final ItemStack item = ItemCreator.fromPotion(type, CompPotionEffectType.getLoreName(type)).glow(level > 0).hideTags(true).make();

			if (MinecraftVersion.olderThan(V.v1_13) && level == 0)
				item.setType(CompMaterial.GLASS_BOTTLE.getMaterial());

			return this.addLevelToItem(item, level);
		}

		@Override
		protected void onPageClick(Player player, PotionEffectType type, ClickType click) {
			final double level = SettingsMenu.this.boss.getPotionEffectLevel(type);
			final int newLevel = (int) MathUtil.range(level + this.getNextQuantityPercent(click), 0.D, 50.D);

			SettingsMenu.this.boss.setPotionEffectNoSave(type, newLevel);
		}

		@Override
		protected void onMenuClose(Player player, Inventory inventory) {
			SettingsMenu.this.boss.save();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select what potions should",
					"be applied to Boss at spawning.",
					"Boss will always have those",
					"effects."
			};
		}
	}

	private class RidingMenu extends Menu {

		@Position(9 * 1 + 2)
		private final Button ridingBossButton;

		@Position(9 * 1 + 4)
		private final Button ridingVanillaButton;

		@Position(9 * 1 + 6)
		private final Button removeOnDeathButton;

		RidingMenu() {
			super(SettingsMenu.this);

			this.setSize(9 * 3);
			this.setTitle("Entities This Boss Rides");

			final boolean isNpc = boss.getCitizensSettings().isEnabled();

			this.ridingBossButton = new ButtonMenu(new RidingBossMenu(),
					CompMaterial.ENDERMAN_SPAWN_EGG,
					"Bosses To Ride",
					"",
					"Select other Bosses",
					"this Boss will ride.",
					"",
					"&cNotice:",
					"Due to Bukkit limits,",
					"either use this or the",
					"other option, not both.");

			this.ridingVanillaButton = isNpc ? Button.makeDummy(CompMaterial.SPAWNER,
					"Mobs To Ride",
					"",
					"&cThis feature is disabled",
					"&cbecause Citizens mobs",
					"&cdo not support it.")
					: new ButtonMenu(new RidingVanillaMenu(),
					CompMaterial.SPAWNER,
					"Mobs To Ride",
					"",
					"Select vanilla mobs",
					"this Boss will ride.",
					"",
					"&cNotice:",
					"Due to Bukkit limits,",
					"either use this or the",
					"other option, not both.");

			this.removeOnDeathButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final boolean has = SettingsMenu.this.boss.getRemoveRidingOnDeath();

					SettingsMenu.this.boss.setRemoveRidingOnDeath(!has);
					RidingMenu.this.restartMenu(has ? "&4Riding no longer dies with Boss." : "&2Riding now dies with Boss.");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = SettingsMenu.this.boss.getRemoveRidingOnDeath();

					return ItemCreator
							.from(CompMaterial.BONE,
									"Kill Riding On Death",
									"",
									"Status: " + (has ? "&2Enabled" : "&7Disabled"),
									"",
									has ? "We remove riding entities" : "Riding entities will continue",
									has ? "when the Boss dies." : "to live after Boss' death.",
									"",
									"Click to toggle.")
							.glow(has)
							.make();
				}
			};
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure which entities this",
					"Boss rides.",
					"",
					"&cWarning: &7You can only select",
					"either vanilla or Boss entities",
					"selecting both causes a bug and",
					"they will dismount on spawning."
			};
		}

		private class RidingVanillaMenu extends MenuContainer {

			protected RidingVanillaMenu() {
				super(RidingMenu.this);

				this.setTitle("Drop Mob Eggs Here");
			}

			@Override
			protected boolean canEditItem(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor, InventoryAction action) {
				final ItemStack item = clicked != null && !CompMaterial.isAir(clicked) ? clicked : cursor;

				if (item == null || CompMaterial.isAir(item.getType()))
					return true;

				if (item.getAmount() > 1) {
					this.animateTitle("&4Amount must be 1!");

					return false;
				}

				if (Boss.findBoss(item) != null) {
					this.animateTitle("&4Item must be a vanilla egg!");

					return false;
				}

				if (!CompMaterial.isMonsterEgg(item.getType())) {
					this.animateTitle("&4Item must be a monster egg!");

					return false;
				}

				return super.canEditItem(location, slot, clicked, cursor, action);
			}

			@Override
			protected ItemStack getDropAt(int slot) {
				final List<EntityType> riding = SettingsMenu.this.boss.getRidingEntitiesVanilla();

				return slot < riding.size() ? ItemCreator.fromMonsterEgg(riding.get(slot)).make() : NO_ITEM;
			}

			@Override
			protected ItemStack onItemClick(int slot, ClickType clickType, ItemStack item) {
				return item;
			}

			@Override
			protected void onMenuClose(Map<Integer, ItemStack> items) {
				final List<EntityType> types = new ArrayList<>();

				for (final ItemStack item : items.values())
					if (item != null && CompMaterial.isMonsterEgg(item.getType())) {
						final EntityType type = CompMonsterEgg.lookupEntity(item);

						types.add(type);
					}

				SettingsMenu.this.boss.setRidingVanilla(types);
			}

			@Override
			protected String[] getInfo() {
				return new String[] {
						"Place monster eggs from your inventory",
						"to this container in the order in which",
						"your Boss will ride them.",
						"",
						"Note: Old MC versions only support riding",
						"one Boss - we pick the first one in",
						"the upper left corner.",
				};
			}
		}

		private class RidingBossMenu extends MenuContainer {

			protected RidingBossMenu() {
				super(RidingMenu.this);

				this.setTitle("Drop Boss Eggs Here");
			}

			@Override
			protected boolean canEditItem(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor, InventoryAction action) {
				final ItemStack item = clicked != null && !CompMaterial.isAir(clicked) ? clicked : cursor;

				if (item == null || CompMaterial.isAir(item.getType()))
					return true;

				if (item.getAmount() > 1) {
					this.animateTitle("&4Amount must be 1!");

					return false;
				}

				if (Boss.findBoss(item) == null) {
					this.animateTitle("&4Menu only accepts Boss eggs!");

					return false;
				}

				return super.canEditItem(location, slot, clicked, cursor, action);
			}

			@Override
			protected ItemStack getDropAt(int slot) {
				final List<String> riding = SettingsMenu.this.boss.getRidingEntitiesBoss();
				Valid.checkNotNull(riding, "Riding cannot be null");

				if (slot < riding.size()) {
					final Boss ridingBoss = Boss.findBoss(riding.get(slot));

					if (ridingBoss != null)
						return ridingBoss.getEgg();
				}

				return Menu.NO_ITEM;
			}

			@Override
			protected ItemStack onItemClick(int slot, ClickType clickType, ItemStack item) {
				return item;
			}

			@Override
			protected void onMenuClose(Map<Integer, ItemStack> items) {
				final List<String> bossNames = new ArrayList<>();

				for (final ItemStack item : items.values()) {
					if (item == null || CompMaterial.isAir(item))
						continue;

					final Boss foundBoss = Boss.findBoss(item);

					if (foundBoss != null)
						bossNames.add(foundBoss.getName());
				}

				SettingsMenu.this.boss.setRidingBoss(bossNames);
			}

			@Override
			protected String[] getInfo() {
				return new String[] {
						"Place Boss eggs from your inventory",
						"to this container in the order in",
						"which your Boss will ride them.",
						"",
						"Note: Old MC versions only support riding",
						"one Boss - we pick the first one in",
						"the upper left corner.",
				};
			}

		}
	}

	private class CommandsOverviewMenu extends Menu {

		@Position(9 + 2)
		private final Button spawnCommandsButton;

		@Position(9 + 4)
		private final Button deathCommandsButton;

		@Position(9 + 6)
		private final Button lifeDecreaseCommandsButton;

		@Position(start = StartPosition.BOTTOM_RIGHT)
		private final Button stopAfterFirstButton;

		protected CommandsOverviewMenu() {
			super(SettingsMenu.this);

			this.setTitle("Commands");
			this.setSize(9 * 4);

			this.spawnCommandsButton = new ButtonMenu(CommandsMenu.from(this, BossCommandType.SPAWN, boss),
					CompMaterial.SPAWNER,
					"&3Commands On Spawn",
					"",
					"Commands that are run",
					"when the Boss spawns.");

			this.deathCommandsButton = new ButtonMenu(CommandsMenu.from(this, BossCommandType.DEATH, boss),
					CompMaterial.BONE,
					"&6Commands On Death",
					"",
					"Commands that are run",
					"when the Boss dies.");

			this.lifeDecreaseCommandsButton = new ButtonMenu(CommandsMenu.from(this, BossCommandType.HEALTH_TRIGGER, boss),
					Remain.getMaterial("BEETROOT", CompMaterial.REDSTONE),
					"&cCommands On Life Decrease",
					"",
					"Commands run when Boss'",
					"health reaches below",
					"a given threshold.");

			this.stopAfterFirstButton = Button.makeBoolean(ItemCreator.from(
							CompMaterial.GREEN_DYE,
							"&2One Command Mode",
							"",
							"Status: {status}",
							"",
							"If enabled, we stop running",
							"more commands after the first",
							"successful one. Useful to have",
							"two different commands, one for",
							"killer and one when there is none."),
					SettingsMenu.this.boss::isCommandsStoppedAfterFirst, SettingsMenu.this.boss::setCommandsStoppedAfterFirst);
		}

		@Override
		public Menu newInstance() {
			return new CommandsOverviewMenu();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure what commands",
					"should run upon various",
					"happenings to your Boss."
			};
		}
	}

	private class PathfindersMenu extends Menu {

		@Position(9 * 1 + 3)
		private final Button citizensButton;

		@Position(9 * 1 + 5)
		private final Button nativeButton;

		private PathfindersMenu() {
			super(SettingsMenu.this);

			this.setTitle("Pathfinders");

			this.citizensButton = new Button() {

				final boolean has = HookManager.isCitizensLoaded();

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					if(this.has)
						new CitizensMenu().displayTo(player);
					else
						SettingsMenu.this.animateTitle("&4Install Citizens plugin!");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							CompMaterial.WITHER_SKELETON_SKULL,
							"Citizens",
							"",
							"Edit behavior, sounds",
							"and other advanced",
							"settings via Citizens.",
							"",
							this.has ? "&cWarning: &7This will only apply" : "&cError: &7These settings",
							this.has ? "&7to newly spawned Bosses." : "require Citizens plugin.").make();
				}
			};

			this.nativeButton = new Button() {

				final boolean has = GoalManagerCheck.isAvailable();

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					if(this.has)
						new NativeGoalsMenu().displayTo(player);
					else
						SettingsMenu.this.animateTitle("&4Use paper 1.15.2 or newer!");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							CompMaterial.SKELETON_SKULL,
							"Native",
							"",
							"Edit behavior using",
							"Paper native goals.",
							"",
							this.has ? "&cWarning: &7This feature is" : "&cError: &7These settings",
							this.has ? "&7under heavy development." : "&7require Paper 1.15 or newer.").make();
				}
			};
		}

		@Override
		public Menu newInstance() {
			return new PathfindersMenu();
		}

		@Override
		protected String[] getInfo() {
			return new String[]{
					"Configure how your Boss",
					"should behave and what",
					"pathfinders it should use.",
			};
		}

		private class NativeGoalsMenu extends Menu {

			@Position(9 * 1 + 4)
			private final Button nativeAttackGoalToggleButton;

			NativeGoalsMenu() {
				super(PathfindersMenu.this);

				this.setTitle("Native Goals");

				this.nativeAttackGoalToggleButton = new Button() {

					@Override
					public void onClickedInMenu(Player player, Menu menu, ClickType click) {
						final boolean enabled = SettingsMenu.this.boss.isNativeAttackGoalEnabled();
						SettingsMenu.this.boss.setNativeAttackGoalEnabled(!enabled);
						NativeGoalsMenu.this.restartMenu((!enabled ? "§aEnabled" : "§cDisabled") + " native attack goal");
					}

					@Override
					public ItemStack getItem() {
						return ItemCreator.from(
								CompMaterial.STONE_SWORD,
								"Attack Goal",
								"",
								"Status: " + (SettingsMenu.this.boss.isNativeAttackGoalEnabled() ? "§aEnabled" : "§cDisabled"),
								"",
								"When enabled, this Boss will use",
								"the native Paper attack goal.").make();
					}
				};
			}

			@Override
			public Menu newInstance() {
				return new NativeGoalsMenu();
			}

			@Override
			protected String[] getInfo() {
				return new String[]{
						"Configure native goals",
						"for your Boss.",
						"",
						"&cWarning: &7This feature is",
						"under heavy development.",
				};
			}
		}

		private class CitizensMenu extends Menu {

			private final BossCitizensSettings citizens;

			@Position(9 * 1 + 1)
			private final Button enabledButton;

			@Position(9 * 1 + 2)
			private final Button speedButton;

			@Position(9 * 1 + 3)
			private final Button skinButton;

			@Position(9 * 1 + 5)
			private final Button soundsButton;

			@Position(9 * 1 + 7)
			private final Button goalsButton;

			CitizensMenu() {
				super(PathfindersMenu.this);

				this.citizens = SettingsMenu.this.boss.getCitizensSettings();

				this.setTitle("Citizens Integration");

				final boolean isHuman = SettingsMenu.this.boss.getType() == CompEntityType.PLAYER;

				// Duplicate this, either can the user enable citizens or native
				this.enabledButton = new Button() {

					@Override
					public void onClickedInMenu(Player player, Menu menu, ClickType click) {
						if (isHuman) {
							CitizensMenu.this.animateTitle("&4Can't disable for human NPCs!");

							return;
						}

						final boolean has = CitizensMenu.this.citizens.isEnabled();

						CitizensMenu.this.citizens.setEnabled(!has);

						final Menu newInstance = CitizensMenu.this.newInstance();
						newInstance.displayTo(player);

						Platform.runTask(1, () -> newInstance.restartMenu(!has ? "&2Enabled Citizens hook :)" : "&4Disabled Citizens hook :("));
					}

					@Override
					public ItemStack getItem() {
						final boolean has = CitizensMenu.this.citizens.isEnabled();

						return ItemCreator.from(
										isHuman || has ? CompMaterial.BEACON : CompMaterial.GLASS,
										"Use Citizens?",
										"",
										"Status: " + (has || isHuman ? "&aEnabled" : "&cDisabled"),
										"",
										isHuman ? "&oCan't disable for" : "Enable Citizens integration?",
										isHuman ? "&ohuman NPCs." : "&cWarning: &7Options here",
										isHuman ? null : "will only apply to new Bosses.")
								.glow(isHuman || has)
								.make();
					}
				};

				// Make a new menu inside this menu for Citizens Settings and move this there
				this.speedButton = Button.makeSimple(ItemCreator.from(
								CompMaterial.STRING,
								"Speed",
								"",
								"Current: " + CitizensMenu.this.citizens.getSpeed(),
								"",
								"How fast should this NPC move?",
								"Defaults to " + boss.getDefaultBaseSpeed() + "."),
						player -> new SimpleDecimalPrompt("Please enter the Boss speed. Current: " + CitizensMenu.this.citizens.getSpeed() +
														  ". Or type '0' to reset back to default.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {
								return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), 0, 100);
							}

							@Override
							protected void onValidatedInput(ConversationContext context, double input) {
								CitizensMenu.this.citizens.setSpeed(input);

								boss.setMaxHealth(input == 0 ? boss.getDefaultBaseSpeed() : input);
							}
						}.show(player));

				final String skinUrl = SettingsMenu.this.boss.getCitizensSettings().getSkinOrAlias().replace("https://", "").replace("www.", "");

				// Also move this to Citizens Settings menu
				this.skinButton = Button.makeSimple(ItemCreator.from(
						CompMaterial.PAPER,
						"Skin",
						"",
						"Current: &f" + (skinUrl.length() > 40 ? skinUrl.substring(0, 20) + "..." + skinUrl.substring(skinUrl.length() - 20) : skinUrl),
						"",
						"Edit the skin of the Boss.",
						"Your skin can be independent",
						"from your Boss alias or name.",
						"",
						"If not set, we use skin from",
						"your Boss' alias."), player -> {
					new SimpleStringPrompt("Please enter the Boss skin (username or URL ending with a .png skin - check console for errors on Boss spawn to see if it was valid)."
										   + " Current: '" + Common.getOrDefault(SettingsMenu.this.boss.getCitizensSettings().getSkin(), "none")
										   + "&7' Type 'default' to use skin from Boss' alias '" + SettingsMenu.this.boss.getAlias() + "&7'.") {

						@Override
						protected boolean isInputValid(ConversationContext context, String input) {
							return Valid.isInRange(input.length(), 3, 256);
						}

						@Override
						protected void onValidatedInput(ConversationContext context, String input) {
							SettingsMenu.this.boss.getCitizensSettings().setSkin(input);
						}
					}.show(player);
				});

				// Also move this to Citizens Settings menu
				this.soundsButton = new ButtonMenu(new SoundsMenu(),
						CompMaterial.NOTE_BLOCK,
						"&6Sounds",
						"",
						"Give the Boss custom",
						"death, idle or damage",
						"sounds!");

				// Duplicate this and put to each Citizens and Native menus
				this.goalsButton = new ButtonMenu(new GoalsMenu(),
						CompMaterial.PURPLE_DYE,
						"Pathfinders & Goals",
						"",
						"Assign custom behavior",
						"to your Boss here.");
			}

			@Override
			public Menu newInstance() {
				return new CitizensMenu();
			}

			@Override
			protected String[] getInfo() {
				return new String[] {
						"Make our plugin use Citizens",
						"for spawning Bosses. &6This",
						"&6will only apply for newly",
						"&6spawned Bosses.",
						"",
						"&6TIP: &7To change Boss skin, use",
						"&6/npc skin &7command from Citizens.",
						"Visit their wiki page for help:",
						"wiki.citizensnpcs.co/Commands"
				};
			}

			private class SoundsMenu extends Menu {

				@Position(9 * 1 + 2)
				private final Button deathSoundButton;

				@Position(9 * 1 + 4)
				private final Button hurtSoundButton;

				@Position(9 * 1 + 6)
				private final Button ambientSoundButton;

				@Position(start = StartPosition.BOTTOM_CENTER)
				private final Button wikiLinkSoundButton;

				SoundsMenu() {
					super(CitizensMenu.this);

					this.setSize(9 * 4);
					this.setTitle("Boss Sounds");

					this.deathSoundButton = Button.makeSimple(ItemCreator.from(
									CompMaterial.BONE,
									"Death Sound",
									"",
									"Current: &7" + Common.getOrDefault(CitizensMenu.this.citizens.getDeathSound(), "default"),
									"",
									"Click to change",
									"Boss death sound."),
							player -> SettingsMenu.this.generateSoundPrompt("death", CitizensMenu.this.citizens::getDeathSound, CitizensMenu.this.citizens::setDeathSound).show(player));

					this.hurtSoundButton = Button.makeSimple(ItemCreator.from(
									CompMaterial.RED_DYE,
									"Hurt Sound",
									"",
									"Current: &7" + Common.getOrDefault(CitizensMenu.this.citizens.getHurtSound(), "default"),
									"",
									"Click to change",
									"sound when damaged."),
							player -> SettingsMenu.this.generateSoundPrompt("hurt", CitizensMenu.this.citizens::getHurtSound, CitizensMenu.this.citizens::setHurtSound).show(player));

					this.ambientSoundButton = Button.makeSimple(ItemCreator.from(
									CompMaterial.FEATHER,
									"Ambient Sound",
									"",
									"Current: &7" + Common.getOrDefault(CitizensMenu.this.citizens.getAmbientSound(), "default"),
									"",
									"Click to change",
									"sound when idle."),
							player -> SettingsMenu.this.generateSoundPrompt("ambient", CitizensMenu.this.citizens::getAmbientSound, CitizensMenu.this.citizens::setAmbientSound).show(player));

					this.wikiLinkSoundButton = Button.makeSimple(ItemCreator.from(
							CompMaterial.PAPER,
							"Sound Names Help",
							"",
							"Sound names are different",
							"internally. Click this",
							"icon to get a list."), player -> {
						player.closeInventory();

						Messenger.info(player, "See <click:open_url:'https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html'><u>hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html</u></click>"
											   + " for valid sound names. For legacy Minecraft versions, we translate those sounds that exist automatically.");
					});
				}

				@Override
				protected String[] getInfo() {
					return new String[] {
							"Remap what sounds this Boss",
							"emits, using NMS item names.",
							"Click the Paper icon for help."
					};
				}

				@Override
				public Menu newInstance() {
					return new SoundsMenu();
				}
			}

			private class GoalsMenu extends Menu {

				@Position(9 * 1 + 1)
				private final Button targetEnabledButton;

				@Position(9 * 1 + 3)
				private final Button targetAggressiveButton;

				@Position(9 * 1 + 5)
				private final Button targetRadiusButton;

				@Position(9 * 1 + 7)
				private final Button targetEntitiesButton;

				@Position(9 * 3 + 3)
				private final Button wanderEnabledButton;

				@Position(9 * 3 + 5)
				private final Button wanderRadiusButton;

				GoalsMenu() {
					super(CitizensMenu.this);

					this.setSize(9 * 6);
					this.setTitle("Pathfinder & AI");

					this.targetEnabledButton = SettingsMenu.this.generateBooleanButton(this,
							"Target Goal",
							CitizensMenu.this.citizens::isTargetGoalEnabled, CitizensMenu.this.citizens::setTargetGoalEnabled,
							"Make this Boss target",
							"and follow another",
							"entities nearby?");

					this.targetAggressiveButton = SettingsMenu.this.generateBooleanButton(this,
							"Kill Target",
							CitizensMenu.this.citizens::isTargetGoalAggressive, CitizensMenu.this.citizens::setTargetGoalAggressive,
							"Make this Boss attack",
							"and kill its target?");

					this.targetRadiusButton = Button.makeSimple(ItemCreator.from(
							CompMaterial.LEAD,
							"Target Radius",
							"",
							"Current: " + CitizensMenu.this.citizens.getTargetGoalRadius() + " blocks",
							"",
							"How far should Boss find",
							"and follow entities? High",
							"values impact performance."), player -> {
						SettingsMenu.this.generateRadiusPrompt("target", CitizensMenu.this.citizens::getTargetGoalRadius, CitizensMenu.this.citizens::setTargetGoalRadius).show(player);
					});

					this.targetEntitiesButton = new ButtonMenu(new MenuTargetEntites(),
							CompMaterial.SKELETON_SPAWN_EGG,
							"Targets",
							"",
							"Click to select what",
							"mobs this Boss targets.");

					// Basically you can disable AI for native
					this.wanderEnabledButton = SettingsMenu.this.generateBooleanButton(this,
							"Wander Goal",
							CitizensMenu.this.citizens::isWanderGoalEnabled, CitizensMenu.this.citizens::setWanderGoalEnabled,
							"Make this Boss wander",
							"around in an area?");

					this.wanderRadiusButton = Button.makeSimple(ItemCreator.from(
							CompMaterial.LEAD,
							"Wander Radius",
							"",
							"Current: " + CitizensMenu.this.citizens.getWanderGoalRadius() + " blocks",
							"",
							"How far should Boss walk",
							"around his spawn point? High",
							"values impact performance."), player -> {
						SettingsMenu.this.generateRadiusPrompt("wander", CitizensMenu.this.citizens::getWanderGoalRadius, CitizensMenu.this.citizens::setWanderGoalRadius).show(player);
					});
				}

				@Override
				public Menu newInstance() {
					return new GoalsMenu();
				}

				@Override
				protected String[] getInfo() {
					return new String[] {
							"Configure custom pathfinder",
							"and navigation for this Boss."
					};
				}

				private class MenuTargetEntites extends MenuPaged<EntityType> {

					MenuTargetEntites() {
						super(GoalsMenu.this, CompEntityType.getAvailable()
								.stream()
								.filter(type -> type == CompEntityType.PLAYER || (type.isAlive() && type.isSpawnable()))
								.sorted(Comparator.comparing(EntityType::name))
								.collect(Collectors.toList()), true);

						this.setTitle("Select Target Entities");
					}

					@Override
					protected ItemStack convertToItemStack(EntityType type) {
						final boolean has = CitizensMenu.this.citizens.getTargetGoalEntities().contains(type);

						return ItemCreator.fromItemStack((type == CompEntityType.PLAYER ? ItemCreator.fromMaterial(CompMaterial.PLAYER_HEAD) : ItemCreator.fromMonsterEgg(type)).make())
								.name("Target " + ChatUtil.capitalizeFully(type))
								.lore("")
								.lore("Status: " + (has ? "&aTargetting" : "&cIgnoring"))
								.lore("")
								.lore("Click to toggle Boss")
								.lore("following this entity.")
								.glow(has)
								.make();
					}

					@Override
					protected void onPageClick(Player player, EntityType type, ClickType click) {
						final Set<EntityType> targets = CitizensMenu.this.citizens.getTargetGoalEntities();
						final boolean has = targets.contains(type);

						if (has)
							targets.remove(type);
						else
							targets.add(type);

						CitizensMenu.this.citizens.setTargetGoalEntities(targets);

						this.restartMenu(has ? "&4Target disabled!" : "&2Now targets " + ChatUtil.capitalizeFully(type) + "!");
					}

					@Override
					protected String[] getInfo() {
						return new String[] {
								"Pick what entities this Boss",
								"will follow, or even attack if",
								"configured as aggressive."
						};
					}
				}
			}
		}
	}

	private class LightningMenu extends Menu {

		@Position(9 * 1 + 3)
		private final Button spawnLightningButton;

		@Position(9 * 1 + 5)
		private final Button deathLightningButton;

		private LightningMenu() {
			super(SettingsMenu.this);

			this.setTitle("Strike Lightning");

			this.spawnLightningButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final boolean has = SettingsMenu.this.boss.hasLightningOnSpawn();

					SettingsMenu.this.boss.setLightningOnSpawn(!has);
					LightningMenu.this.restartMenu((has ? "&4Disabled" : "&2Enabled") + " lightning on spawn");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = SettingsMenu.this.boss.hasLightningOnSpawn();

					return ItemCreator.from(
									CompMaterial.ENDER_PEARL,
									"Lightning On Spawn",
									"",
									"Status: " + (has ? "&aEnabled" : "&cDisabled"),
									"",
									"Click to toggle if Boss",
									"strikes a lightning on spawn.")
							.glow(has)
							.make();
				}
			};

			this.deathLightningButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final boolean has = SettingsMenu.this.boss.hasLightningOnDeath();

					SettingsMenu.this.boss.setLightningOnDeath(!has);
					LightningMenu.this.restartMenu((has ? "&4Disabled" : "&2Enabled") + " lightning on death");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = SettingsMenu.this.boss.hasLightningOnDeath();

					return ItemCreator.from(
									CompMaterial.BONE,
									"Lightning On Death",
									"",
									"Status: " + (has ? "&aEnabled" : "&cDisabled"),
									"",
									"Click to toggle if Boss",
									"strikes a lightning on death.")
							.glow(has)
							.make();
				}
			};
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select at what events we",
					"should strike lightning at",
					"Boss location. This lightning",
					"doesn't cause fire or damage."
			};
		}
	}

	private class AttributesMenu extends MenuPaged<BossAttribute> {

		private AttributesMenu() {
			super(SettingsMenu.this, SettingsMenu.this.boss.getDefaultAttributes());

			this.setTitle("Attributes");
		}

		@Override
		protected ItemStack convertToItemStack(BossAttribute attribute) {
			return ItemCreator.from(
					attribute.getIcon(),
					attribute.getTitle(),
					"",
					"Current: " + SettingsMenu.this.boss.getAttribute(attribute),
					"",
					attribute.getDescription()).make();
		}

		@Override
		protected void onPageClick(Player player, BossAttribute attribute, ClickType click) {
			new SimpleStringPrompt(
					"Enter the value for this attribute, or type 'default' to reset to " + SettingsMenu.this.boss.getDefaultAttribute(attribute),
					value -> {
						SettingsMenu.this.boss.setAttribute(attribute, "default".equals(value) ? null : Double.parseDouble(value));
					}) {

				@Override
				protected boolean isInputValid(ConversationContext context, String input) {
					return "default".equals(input) || Valid.isDecimal(input);
				}

			}.show(player);
		}

		@Override
		protected String[] getInfo() {
			final boolean isNpc = SettingsMenu.this.boss.getType() == CompEntityType.PLAYER;
			final List<String> lore = Common.newList("Select special attributes",
					"that will apply to the Boss.");

			if (isNpc) {
				lore.add(" ");
				lore.add("&cPlayer NPCs from Citizens do NOT");
				lore.add("&chave all attributes available.");
			}

			if (MinecraftVersion.olderThan(V.v1_9)) {
				lore.add(" ");
				lore.add("&cWarning: &7Many attributes are not");
				lore.add("&7supported on MC 1.8 and below.");
			}

			return Common.toArray(lore);
		}

		@Override
		public Menu newInstance() {
			return new AttributesMenu();
		}
	}

	private class CustomSettingsMenu extends MenuPaged<CustomSetting<?>> {

		private CustomSettingsMenu() {
			super(SettingsMenu.this, SettingsMenu.this.boss.getApplicableCustomSettings());

			this.setTitle("Custom Settings");
		}

		@Override
		protected ItemStack convertToItemStack(CustomSetting<?> customSetting) {
			return customSetting.toMenuItem();
		}

		@Override
		protected void onPageClick(Player player, CustomSetting<?> customSetting, ClickType clickType) {
			customSetting.onMenuClick(SettingsMenu.this.boss, this, player, clickType);
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select custom settings only",
					"applicable for this Boss.",
					"You can create new settings",
					"using our developer API, see",
					"&6github.com/kangarko/boss/Wiki",
					"for help."
			};
		}

		@Override
		public Menu newInstance() {
			return new CustomSettingsMenu();
		}
	}

	private class CustomModelMenu extends Menu {

		@Position(9 * 1 + 3)
		private final Button enableCustomModelButton;

		@Position(9 * 1 + 5)
		private final Button selectCustomModelsButton;

		public CustomModelMenu() {
			super(SettingsMenu.this);

			this.setTitle("Custom Models");

			this.enableCustomModelButton = new Button() {
				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final boolean enabled = SettingsMenu.this.boss.isUseCustomModel();
					SettingsMenu.this.boss.setUseCustomModel(!enabled);
					SettingsMenu.CustomModelMenu.this.restartMenu(!enabled ? "&2Enabled Custom Models" : "&4Disabled Custom Models");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							CompMaterial.BEACON,
							"Use Custom Model",
							"",
							"Status: " + (SettingsMenu.this.boss.isUseCustomModel() ? "§aEnabled" : "§cDisabled"),
							"",
							"When enabled, we will use ModelEngine",
							"to render a custom model in your Boss.",
							"",
							"Click to toggle").make();
				}
			};

			this.selectCustomModelsButton = new ButtonMenu(new SelectCustomModelsMenu(),
					CompMaterial.ARMOR_STAND,
					"Select Custom Models",
					"",
					"If you select more than 1 model,",
					"the plugin will choose a random",
					"one every time a Boss spawns or",
					"updates.",
					"",
					"Click to change the",
					"custom model to use.");
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure custom model for this Boss",
					"with ModelEngine.",
					"",
					"&cWarning: &7This feature is",
					"&7under heavy development."
			};
		}

		@Override
		public Menu newInstance() {
			return new CustomModelMenu();
		}

		private class SelectCustomModelsMenu extends MenuPaged<String> {

			public SelectCustomModelsMenu() {
				super(CustomModelMenu.this, new ArrayList<>(ModelEngineAPI.getAPI().getModelRegistry().getKeys()));
			}

			@Override
			public Menu newInstance() {
				return new SelectCustomModelsMenu();
			}

			@Override
			protected String[] getInfo() {
				return new String[]{
						"If you select more than 1 model,",
						"the plugin will choose a random",
						"one every time a Boss spawns or",
						"updates."
				};
			}

			@Override
			protected ItemStack convertToItemStack(String item) {
				final boolean selected = SettingsMenu.this.boss.getCustomModels().contains(item);
				return ItemCreator.from(
						CompMaterial.ARMOR_STAND,
						item + (selected ? " &7&o(Selected)" : ""),
						"",
						selected ? "Click to remove" : "Click to select"
				).glow(selected).make();
			}

			@Override
			protected void onPageClick(Player player, String item, ClickType click) {
				final Boss boss = SettingsMenu.this.boss;

				if (boss.getCustomModels().contains(item))
					boss.removeCustomModel(item);
				else
					boss.addCustomModel(item);

				this.restartMenu((boss.getCustomModels().contains(item) ? "§2Selected " : "§cRemoved ") + item);
			}
		}
	}
}