package org.mineacademy.boss.menu;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossDrop;
import org.mineacademy.boss.api.BossEggItem;
import org.mineacademy.boss.api.BossEquipment;
import org.mineacademy.boss.api.BossEquipmentSlot;
import org.mineacademy.boss.api.BossNativeConditions;
import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.boss.api.BossRegionSpawning;
import org.mineacademy.boss.api.BossRegionType;
import org.mineacademy.boss.api.BossSettings;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.boss.api.BossSkillRegistry;
import org.mineacademy.boss.api.BossSpecificSetting;
import org.mineacademy.boss.impl.SimpleSettings;
import org.mineacademy.boss.model.BossAttribute;
import org.mineacademy.boss.model.BossConversation;
import org.mineacademy.boss.model.BossConversation.BossPrompt;
import org.mineacademy.boss.model.BossConversation.PromptAttribute;
import org.mineacademy.boss.model.BossConversation.PromptCustomName;
import org.mineacademy.boss.model.BossConversation.PromptDroppedExp;
import org.mineacademy.boss.model.BossConversation.PromptHealth;
import org.mineacademy.boss.model.BossConversation.PromptHeight;
import org.mineacademy.boss.model.BossConversation.PromptLight;
import org.mineacademy.boss.model.BossConversation.PromptTime;
import org.mineacademy.boss.model.BossManager;
import org.mineacademy.boss.model.specific.SpecificKey;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.util.AutoUpdateList;
import org.mineacademy.boss.util.AutoUpdateMap;
import org.mineacademy.boss.util.BossUpdateUtil;
import org.mineacademy.boss.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPagged;
import org.mineacademy.fo.menu.MenuQuantitable;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.ButtonRemove;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.menu.model.MenuQuantity;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.CompMonsterEgg;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public final class MenuBossIndividual extends Menu {

	private final boolean addReturn;
	private final Boss boss;

	private final Button settingButton;
	private final Button skillsButton;
	private final Button deathButton;
	private final Button spawningButton;

	private final Button eggButton;
	private final Button butcherButton;
	private final Button removeButton;

	public MenuBossIndividual(@NonNull final String name, final boolean addReturn) {
		this(BossPlugin.getBossManager().findBoss(name), addReturn);
	}

	public MenuBossIndividual(@NonNull final Boss boss) {
		this(boss, true);
	}

	public MenuBossIndividual(@NonNull final Boss boss, final boolean addReturn) {
		super(new MenuBossContainer());

		setSize(9 * 4);
		setTitle("Boss " + boss.getName());

		this.addReturn = addReturn;
		this.boss = boss;

		settingButton = new ButtonMenu(new MenuSettings(),
				CompMaterial.CHEST,
				"&6Settings",
				"",
				"&7Edit how this Boss behaves",
				"&7or looks like, when spawned.");

		deathButton = new ButtonMenu(new MenuDeath(),
				CompMaterial.BONE,
				"Death",
				"",
				"Configure what happens",
				"when this Boss dies.");

		skillsButton = new ButtonMenu(new MenuSkills(),
				CompMaterial.ENDER_EYE,
				"&bSkills",
				"",
				"Give your Boss special",
				"functions or abilities.");

		spawningButton = new ButtonMenu(new MenuSpawning(),
				CompMaterial.OAK_SAPLING,
				"&aSpawning",
				"",
				"Configure natural spawning",
				"of this Boss on your server.");

		eggButton = new Button() {

			@Override
			public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
				pl.closeInventory();
				pl.getInventory().addItem(getBoss().asEgg());

				Common.tell(pl, "&7The Boss egg is now &2available &7in your inventory.");
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator.of(
						CompMaterial.SKELETON_SPAWN_EGG,
						"&2Get Spawner Egg",
						"",
						"Receive a Boss egg you",
						"can use to spawn the Boss.",
						"",
						"&cNB: &7Give players the permission",
						"below to use this spawner egg:",
						"&c&o" + Permissions.Use.SPAWN.replace("{plugin_name}", SimplePlugin.getNamed().toLowerCase()).replace("{name}", boss.getSpawnPermission())).hideTags(true)
						.build().make();
			}
		};

		butcherButton = new Button() {

			@Override
			public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
				pl.performCommand(SimplePlugin.getInstance().getMainCommand().getLabel() + " butcher * " + getBoss().getName());
				animateTitle("&4Bosses has been removed!");
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator.of(CompMaterial.RED_DYE,
						"&cKill Bosses",
						"",
						"Remove this Boss",
						"from all worlds.")
						.build().make();
			}
		};

		removeButton = new ButtonRemove(this, "Boss", boss.getName(), name -> {
			final BossManager manager = BossPlugin.getBossManager();
			manager.removeBoss(name);

			final Menu nextMenu = manager.getBossesAsList().isEmpty() ? new MenuMain() : new MenuBossContainer();
			nextMenu.displayTo(getViewer());
		});
	}

	@Override
	public Menu newInstance() {
		return new MenuBossIndividual(boss, addReturn);
	}

	@Override
	public ItemStack getItemAt(final int slot) {
		if (slot == 9 + 1)
			return settingButton.getItem();

		if (slot == 9 + 3)
			return skillsButton.getItem();

		if (slot == 9 + 5)
			return deathButton.getItem();

		if (slot == 9 + 7)
			return spawningButton.getItem();

		if (slot == getSize() - 6)
			return eggButton.getItem();

		if (slot == getSize() - 5)
			return butcherButton.getItem();

		if (slot == getSize() - 4)
			return removeButton.getItem();

		return null;
	}

	@Override
	protected boolean addReturnButton() {
		return addReturn;
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"This is the main Boss' menu.",
				"You can set up how the Boss",
				"looks like or behaves here.",
				"",
				"Your Bosses on your worlds",
				"will update automatically."
		};
	}

	public Boss getBoss() {
		return boss;
	}

	private ItemStack getFillerItem() {
		return ItemCreator.of(CompMaterial.LIGHT_GRAY_STAINED_GLASS_PANE).name(" ").build().make();
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Sub-Menus
	// ----------------------------------------------------------------------------------------------------------------

	private final class MenuSettings extends Menu {

		private final Button customNameButton;
		private final Button healthButton;
		private final Button droppedExpButton;
		private final Button potionsButton;
		private final Button soundsButton;
		private final Button ridingButton;
		private final Button equipmentButton;
		private final Button attributesButton;
		private final Button specificButton;
		private final Button commandsButton;

		private final Button reloadButton;

		protected MenuSettings() {
			super(MenuBossIndividual.this);

			setSize(9 * 6);
			setTitle("Settings");

			customNameButton = makeButton(PromptCustomName.class,
					ItemCreator.of(
							CompMaterial.PAPER,
							"&fName",
							"",
							"Edit the name above",
							"the Boss' head.",
							"",
							"Current: &f" + formatCustomName(),
							"",
							"&c[!] &7This does not affect",
							"the file name: &f" + boss.getName() + ".yml"));

			healthButton = makeButton(PromptHealth.class,
					ItemCreator.of(
							Remain.getMaterial("BEETROOT", CompMaterial.REDSTONE),
							"&cHealth",
							"",
							"Edit Boss' health.",
							"",
							"Current: &c" + getBoss().getSettings().getHealth() + " HP"));

			droppedExpButton = makeButton(PromptDroppedExp.class,
					ItemCreator.of(
							CompMaterial.EXPERIENCE_BOTTLE,
							"&aDropped Experience",
							"",
							"Set a custom dropped",
							"exp on Boss' death.",
							"",
							"Current: &a" + Common.getOrDefault(getBoss().getSettings().getDroppedExp(), "(default)")));

			potionsButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					new MenuPotions().displayTo(pl);
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(
							CompMaterial.POTION,
							"&dPotion Effects",
							"",
							"Add custom potion",
							"effects to the Boss.")
							.hideTags(true)
							.build().make();
				}
			};

			soundsButton = Button.makeDummy(ItemCreator.of(CompMaterial.NOTE_BLOCK,
					"Sounds",
					"",
					"Changing Boss sounds",
					"has temporarily been",
					"disabled as it requires",
					"packet interception which",
					"reportedly caused crashes.",
					"We are working on a fix."));

			/*soundsButton = new Button() {
			
				@Override
				public void onClickedInMenu(Player pl, Menu menu, ClickType click) {
					if (!HookManager.isProtocolLibLoaded()) {
						animateTitle("&4Install ProtocolLib first!");
			
						return;
					}
			
					if (!Settings.Setup.PROTOCOLLIB) {
						animateTitle("&4Enable Setup.Hook_ProtocolLib!");
			
						return;
					}
			
					new MenuSounds(MenuSettings.this).displayTo(pl);
				}
			
				@Override
				public ItemStack getItem() {
					return ItemCreator.of(
							CompMaterial.NOTE_BLOCK,
							"Sounds",
							"",
							"Change sounds that",
							"this Boss makes.",
							HookManager.isProtocolLibLoaded() ? "&3Uses ProtocolLib." : "&cRequires ProtocolLib.").build().make();
				}
			
			};*/

			equipmentButton = new ButtonMenu(new MenuEquipment(MenuSettings.this, EditMode.ITEMS),
					CompMaterial.LEATHER_CHESTPLATE,
					"&6Equipment",
					"",
					"Edit Boss' armor and",
					"the hand items.");

			attributesButton = new ButtonMenu(new MenuAttributes(MenuSettings.this),
					CompMaterial.ANVIL,
					"Attributes",
					"",
					"Edit attributes like speed",
					"follow range, damage, ...");

			specificButton = new ButtonMenu(new MenuSpecific(MenuSettings.this),
					Remain.getMaterial("ARMOR_STAND", CompMaterial.REDSTONE_ORE),
					"Specific Settings",
					"",
					"Settings only available",
					"for the " + ItemUtil.bountifyCapitalized(getBoss().getType()).toLowerCase() + " bosses.");

			ridingButton = new ButtonMenu(new MenuRiding(MenuSettings.this),
					CompMaterial.CARROT_ON_A_STICK,
					"&eRiding",
					"",
					"Select a mob this",
					"Boss should ride.");

			commandsButton = new ButtonMenu(new MenuCommandsOverview(MenuSettings.this),
					CompMaterial.COMMAND_BLOCK,
					"Commands",
					"",
					"Select what commands should",
					"be executed when this Boss",
					"dies or is spawned.");

			//

			reloadButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					((SimpleSettings) getBoss().getSettings()).reload();

					final MenuSettings newMenu = (MenuSettings) newInstance();

					newMenu.displayTo(pl);
					newMenu.animateTitle("&2Settings were reloaded!");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(
							CompMaterial.LEVER,
							"Reload settings",
							"",
							"Reload Boss's settings",
							"directly from its file",
							"in bosses/ folder.")
							.build().make();
				}
			};
		}

		private String formatCustomName() {
			final String name = getBoss().getSettings().getCustomName();

			return name == null ? "&7(hidden)" : name;
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == 9 + 1)
				return customNameButton.getItem();

			if (slot == 9 + 3)
				return healthButton.getItem();

			if (slot == 9 + 5)
				return droppedExpButton.getItem();

			if (slot == 9 + 7)
				return equipmentButton.getItem();

			if (slot == 9 * 3 + 1)
				return potionsButton.getItem();

			if (slot == 9 * 3 + 2)
				return soundsButton.getItem();

			if (slot == 9 * 3 + 3)
				return ridingButton.getItem();

			if (slot == 9 * 3 + 5)
				return commandsButton.getItem();

			if (slot == 9 * 3 + 6)
				return attributesButton.getItem();

			if (slot == 9 * 3 + 7)
				return specificButton.getItem();

			if (slot == getSize() - 5)
				return reloadButton.getItem();

			return null;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Edit various behavioural or",
					"visual settings for this Boss."
			};
		}

		@Override
		public Menu newInstance() {
			return new MenuSettings();
		}

		private MenuButtonConvo makeButton(final Class<? extends BossPrompt> prompt, final ItemCreator.ItemCreatorBuilder item) {
			return new MenuButtonConvo(boss, this, prompt, item);
		}
	}

	private final class MenuSpecific extends Menu {

		private final List<Button> buttons = new ArrayList<>();

		private final Button resetButton;

		protected MenuSpecific(final Menu parent) {
			super(parent);

			setTitle("Specific Settings");
			setSize(9 * 3);

			resetButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					for (final BossSpecificSetting setting : BossSpecificSetting.values()) {
						final SpecificKey<?> key = setting.getFor(boss);

						if (key.matches(getBoss().getType()))
							getBoss().getSettings().setSpecificSetting(setting, key.getDefault());
					}

					BossUpdateUtil.updateAll();
					restartMenu("&2Settings were reset!");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(
							CompMaterial.FLINT,
							"Reset",
							"",
							"Set all settings to",
							"their default values.").build().make();
				}
			};

			reregisterButtons();
		}

		private void reregisterButtons() {
			for (final BossSpecificSetting setting : BossSpecificSetting.values()) {
				final SpecificKey<?> key = setting.getFor(boss);

				if (key.matches(getBoss().getType()))
					buttons.add(key.getButton());
			}
		}

		@Override
		public void onButtonClick(final Player pl, final int slot, final InventoryAction action, final ClickType click, final Button button) {
			super.onButtonClick(pl, slot, action, click, button);

			BossUpdateUtil.updateAll();
		}

		@Override
		protected List<Button> getButtonsToAutoRegister() {
			return buttons;
		}

		@Override
		public ItemStack getItemAt(final int slot) {
			if (slot < buttons.size())
				return buttons.get(slot).getItem();

			if (slot == getSize() - 5)
				return resetButton.getItem();

			return null;
		}

		@Override
		public Menu newInstance() {
			return new MenuSpecific(getParent());
		}

		@Override
		protected String[] getInfo() {
			final List<String> i = Common.toList(
					"Configure specific settings for",
					"this Boss type (" + ItemUtil.bountifyCapitalized(getBoss().getType()).toLowerCase() + ").");

			if (MinecraftVersion.olderThan(V.v1_12)) {
				i.add(" ");
				i.add("&cNotice&7: Some features are not");
				i.add("available in your Minecraft version.");
			}

			return i.toArray(new String[i.size()]);
		}

	}

	private final class MenuEquipment extends MenuEditChances {

		private final StrictMap<Integer, BossEquipmentSlot> slots = new StrictMap<>();

		private final Button randomButton;

		private MenuEquipment(final Menu parent, final EditMode mode) {
			super(parent, mode, 9 * 4);

			setTitle("Equipment");
			fillSlots();

			randomButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					final boolean has = boss.getEquipment().allowRandom();

					boss.getEquipment().setAllowRandomNoSave(!has);
					((SimpleSettings) boss.getSettings()).saveEquipment_();

					restartMenu(has ? "&4Random equipment disallowed." : "&2Random equipment allowed.");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = boss.getEquipment().allowRandom();

					return ItemCreator.of(
							has ? CompMaterial.BEACON : CompMaterial.GLASS,
							has ? "&aRandom Equipment Allowed" : "&fRandom Equipment Prevented",
							"",
							"Click to toggle.",
							"",
							"If random equipment is allowed",
							"mobs can wear vanilla equipment",
							"given randomly if you leave",
							"a slot empty.").build().make();
				}
			};
		}

		private void fillSlots() {
			int offset = 0;

			for (final BossEquipmentSlot slot : BossEquipmentSlot.values()) {

				if (slot == BossEquipmentSlot.OFF_HAND && MinecraftVersion.olderThan(V.v1_9)) {
					offset++;
					continue;
				}

				slots.put(9 + (offset = slot == BossEquipmentSlot.HELMET ? offset + 2 : offset + 1), slot);
			}
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (mode == EditMode.ITEMS && slots.contains(slot + 9))
				return getHelpFor(slots.get(slot + 9));

			if (slots.contains(slot))
				return getEquipment(slots.get(slot));

			if (slot == getSize() - 3)
				return randomButton.getItem();

			return Common.getOrDefault(super.getItemAt(slot), ItemCreator.of(CompMaterial.BROWN_STAINED_GLASS_PANE).name(" ").build().make());
		}

		private ItemStack getEquipment(final BossEquipmentSlot slot) {
			return paintChanceItem(getBoss().getEquipment().get(slot));
		}

		private ItemStack getHelpFor(final BossEquipmentSlot slot) {
			return ItemCreator.of(
					CompMaterial.BLACK_STAINED_GLASS_PANE,
					"" + ItemUtil.bountifyCapitalized(slot) + " Slot",
					"",
					"Place &6" + ItemUtil.bountifyCapitalized(slot).toLowerCase() + (slot.toString().contains("HAND") ? " item" : "") + " &7below.")
					.color(CompColor.BLACK)
					.build().make();
		}

		@Override
		public void onMenuClose(final Player pl, final Inventory inv) {
			if (mode == EditMode.ITEMS)
				for (final Entry<Integer, BossEquipmentSlot> e : slots.entrySet()) {
					final ItemStack item = pl.getOpenInventory().getTopInventory().getItem(e.getKey());

					if (item != null && item.getType() == CompMaterial.PAPER.getMaterial() && item.getItemMeta().hasLore())
						continue;

					final BossEquipmentSlot equipmentSlot = e.getValue();
					final BossDrop oldDrop = getBoss().getEquipment().get(equipmentSlot);

					getBoss().getEquipment().setNoSave(equipmentSlot, item, item != null && oldDrop != null ? oldDrop.getDropChance() : 0F);
				}

			((SimpleSettings) getBoss().getSettings()).saveEquipment_();

			BossUpdateUtil.updateAll();
		}

		@Override
		public void onMenuClick(final Player pl, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack clicked, final boolean cancelled) {

			if (mode == EditMode.DROP_CHANCES && slots.contains(slot)) {
				final BossEquipment eq = getBoss().getEquipment();
				final BossEquipmentSlot equipSlot = slots.get(slot);

				final int chance = (int) MathUtil.ceiling(eq.get(equipSlot).getDropChance() * 100);
				final int newChance = MathUtil.range(chance + getNextQuantity(click), 0, 100);

				eq.setNoSave(equipSlot, eq.get(equipSlot).getItem(), newChance / 100F);

				pl.getOpenInventory().getTopInventory().setItem(slot, getItemAt(slot));
			}

			//((SimpleSettings) getBoss().getSettings()).saveEquipment();
		}

		@Override
		public boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor) {
			if (mode == EditMode.DROP_CHANCES)
				return false;

			if (location == MenuClickLocation.MENU && slots.contains(slot)) {
				final String type = cursor.getType().toString();
				final BossEquipmentSlot eq = slots.get(slot);

				final boolean passed = type.equals("AIR") || eq == BossEquipmentSlot.HELMET ||
						eq == BossEquipmentSlot.CHESTPLATE && (type.contains("CHESTPLATE") || type.contains("ELYTRA")) ||
						eq == BossEquipmentSlot.LEGGINGS && type.contains("LEGGINGS") ||
						eq == BossEquipmentSlot.BOOTS && type.contains("BOOTS") ||
						eq == BossEquipmentSlot.HAND || eq == BossEquipmentSlot.OFF_HAND;

				if (!passed) {
					if (clicked != null)
						clicked.setAmount(0);

					animateTitle("&4Only insert " + ItemUtil.bountifyCapitalized(eq).toLowerCase() + " here!");
				}

				return passed;
			}

			return location == MenuClickLocation.PLAYER_INVENTORY;
		}
	}

	private final class MenuDeath extends Menu {

		private final Button dropsButton;
		private final Button reinforcementsButton;
		private final Button lightningButton;

		protected MenuDeath() {
			super(MenuBossIndividual.this);

			setSize(9 * 4);
			setTitle("Death");

			dropsButton = new ButtonMenu(new MenuDrops(MenuDeath.this, EditMode.ITEMS),
					CompMaterial.STICK,
					"Drops",
					"",
					"Specify items dropped",
					"when the Boss dies.");

			reinforcementsButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					new MenuReinforcements(MenuDeath.this).displayTo(pl);
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(CompMaterial.SKELETON_SPAWN_EGG,
							"Reinforcements",
							"",
							"Specify what Bosses should",
							"be spawned when this Boss dies.").build().make();
				}
			};

			lightningButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {

					final boolean has = getBoss().hasLightningOnDeath();

					getBoss().setLightningOnDeath(!has);
					restartMenu(has ? "&4Lightning was disabled." : "&2Enabled lightning on death!");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = getBoss().hasLightningOnDeath();

					return ItemCreator.of(CompMaterial.ENDER_PEARL,
							"Lightning on Death",
							"",
							has ? "&bBoss strikes lightning on death." : "&cLightning is disabled.",
							"",
							"Click to toggle.")
							.glow(has)
							.build().make();
				}
			};
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == 9 + 2)
				return dropsButton.getItem();

			if (slot == 9 + 4)
				return reinforcementsButton.getItem();

			if (slot == 9 + 6)
				return lightningButton.getItem();

			return null;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure what happens",
					"when this Boss dies."
			};
		}
	}

	private final class MenuDrops extends MenuEditChances {

		private final Button dropToInventoryButton;
		private final Button vanillaDropsButton;
		private final Button singleDropButton;

		private MenuDrops(final Menu parent, final EditMode mode) {
			super(parent, mode, 9 * 6);

			setTitle("Drops");

			vanillaDropsButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					final boolean has = getBoss().getSettings().hasNaturalDrops();

					getBoss().getSettings().setNaturalDrops(!has);
					restartMenu(has ? "&4Vanilla drops disabled." : "&2Enabled vanilla drops.");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = getBoss().getSettings().hasNaturalDrops();

					return ItemCreator.of(CompMaterial.FEATHER,
							"Vanilla Drops",
							"",
							has ? "&aBoss drops their vanilla drops" : "&7Boss only drops the items",
							has ? "&alike bones, food or picked items." : "&7specified in this menu.",
							"",
							"Click to toggle.")
							.glow(has)
							.build().make();
				}
			};

			dropToInventoryButton = new ButtonMenu(new MenuInventoryDrop(this),
					ItemCreator.of(
							CompMaterial.DROPPER,
							"Inventory Drops",
							"",
							"Instead of dropping the loot on",
							"the floor, give it directly to",
							"the inventory of last players",
							"who damaged the Boss the most."));

			singleDropButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					final boolean has = getBoss().getSettings().hasSingleDrops();

					getBoss().getSettings().setSingleDrops(!has);
					restartMenu(has ? "&4Single drops disabled." : "&2Enabled single drops.");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = getBoss().getSettings().hasSingleDrops();

					return ItemCreator.of(has ? CompMaterial.BLAZE_ROD : CompMaterial.STICK,
							"Single Drops",
							"",
							has ? "&aBoss only drops one item from" : "&7Boss drops all items above",
							has ? "&aall items you put above." : "&7that trigger their drop chance.",
							"",
							"Click to toggle.")
							.build().make();
				}
			};
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == getSize() - 3)
				return vanillaDropsButton.getItem();

			if (slot == getSize() - 4)
				return singleDropButton.getItem();

			if (slot == getSize() - 7 && getMode() != EditMode.DROP_CHANCES)
				return dropToInventoryButton.getItem();

			if (slot < getSize() - 9) {
				final BossDrop drop = getBoss().getDrops().get(slot);

				if (drop != null)
					return paintChanceItem(drop);
			}

			return super.getItemAt(slot);
		}

		@Override
		public void onMenuClose(final Player pl, final Inventory inv) {
			if (mode == EditMode.ITEMS)
				for (final Entry<Integer, ItemStack> e : getItemsExceptBottomBar(inv).entrySet()) {
					final AutoUpdateMap<Integer, BossDrop> drops = getBoss().getDrops();

					final int slot = e.getKey();
					final ItemStack item = e.getValue();

					if (item == null) {
						if (drops.contains(slot))
							drops.removeAndUpdate(slot);

						continue;
					}

					final float dropChance = drops.contains(slot) ? drops.get(slot).getDropChance() : 0.5F;
					final BossDrop drop = new BossDrop(item, dropChance);

					drops.overrideAndUpdate(slot, drop);
				}
		}

		private Map<Integer, ItemStack> getItemsExceptBottomBar(final Inventory inv) {
			final Map<Integer, ItemStack> items = new HashMap<>();

			for (int i = 0; i < getSize() - 9; i++) {
				final ItemStack item = inv.getItem(i);

				items.put(i, item != null && !CompMaterial.isAir(item.getType()) ? item : null);
			}

			return items;
		}

		@Override
		public void onMenuClick(final Player pl, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack clicked, final boolean cancelled) {

			if (mode == EditMode.DROP_CHANCES) {
				final BossDrop drop = getBoss().getDrops().get(slot);

				if (drop == null)
					return;

				final int chance = (int) MathUtil.ceiling(Math.round(drop.getDropChance() * 100));
				final int newChance = MathUtil.range(chance + getNextQuantity(click), 0, 100);

				drop.setDropChance(newChance / 100F);
				pl.getOpenInventory().getTopInventory().setItem(slot, getItemAt(slot));
			}
		}

		@Override
		public Menu newInstance() {
			return new MenuDrops(getParent(), mode);
		}
	}

	private final class MenuInventoryDrop extends Menu {

		private final Button enabledButton;
		private final Button playerLimitButton;
		private final Button timeLimitButton;

		protected MenuInventoryDrop(final Menu parent) {
			super(parent);

			setSize(9 * 4);
			setTitle("Inventory Drops");

			enabledButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					final boolean has = getBoss().getSettings().hasInventoryDrops();

					getBoss().getSettings().setInventoryDrops(!has);
					restartMenu(has ? "&4Inventory drops disabled." : "&2Enabled direct inventory drops.");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = getBoss().getSettings().hasInventoryDrops();

					return ItemCreator.of(CompMaterial.ENDER_EYE,
							"Inventory Drops",
							"",
							has ? "&aBoss drops go directly" : "&7Boss drops items normally",
							has ? "&ainto the player's inventory." : "&7on the floor (default Minecraft).",
							"",
							"Click to toggle.")
							.glow(has)
							.build().make();
				}
			};

			playerLimitButton = new MenuButtonConvo(boss, this, BossConversation.PromptInvDropPlayerLimit.class, ItemCreator.of(
					CompMaterial.PLAYER_HEAD,
					"Player Limit",
					"",
					"Current: " + boss.getSettings().getInventoryDropsPlayerLimit(),
					"",
					"How many last players who",
					"damaged the Boss should",
					"get the drop reward?",
					"Default is 5 players."));

			timeLimitButton = new MenuButtonConvo(boss, this, BossConversation.PromptInvDropTimeLimit.class, ItemCreator.of(
					CompMaterial.CLOCK,
					"Time Limit",
					"",
					"Currently: " + Common.plural(boss.getSettings().getInventoryDropsTimeLimit(), "second"),
					"",
					"How many seconds should we",
					"keep the player who damaged",
					"the Boss in the reward list?",
					"Default is 10 seconds."));
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == 9 + 2)
				return enabledButton.getItem();

			if (slot == 9 + 4)
				return playerLimitButton.getItem();

			if (slot == 9 + 6)
				return timeLimitButton.getItem();

			return null;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure giving Boss drops",
					"directly to the inventories",
					"of last damaging players."
			};
		}

		@Override
		public Menu newInstance() {
			return new MenuInventoryDrop(getParent());
		}
	}

	private final class MenuAttributes extends Menu {

		private final Button damageMultiplierButton;

		private final StrictMap<Integer, BossAttribute> attributes = new StrictMap<>();

		private MenuAttributes(final Menu parent) {
			super(parent);

			setSize(9 * 3);
			setTitle("Attributes");

			fillSlots();

			this.damageMultiplierButton = new MenuButtonConvo(this, new BossConversation.PromptDamageModifier(boss),
					ItemCreator.of(CompMaterial.IRON_SWORD,
							"Damage Multiplier",
							"",
							"Current: " + boss.getSettings().getDamageMultiplier(),
							"",
							"Enter the number we multiply",
							"the damage of the Boss when",
							"it attacks something.",
							"",
							"Example: 0.5 to reduce his",
							"damage by 50%, or 1.7 to",
							"increase it to 170%"));
		}

		private void fillSlots() {
			int offset = 0;

			for (final BossAttribute attr : getBoss().getAttributes().getVanilla())
				if (!Arrays.asList(BossAttribute.GENERIC_ATTACK_DAMAGE).contains(attr))
					attributes.put(offset++, attr);
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == getSize() - 5)
				return damageMultiplierButton.getItem();

			return attributes.contains(slot) ? getAttribute(attributes.get(slot)) : null;
		}

		@Override
		public void onMenuClick(final Player pl, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack clicked, final boolean cancelled) {
			if (attributes.contains(slot)) {
				final BossAttribute attr = attributes.get(slot);

				if (MinecraftVersion.olderThan(V.v1_9) && !attr.hasLegacy())
					animateTitle("&4Unsupported on MC 1.8.9!");
				else
					PromptAttribute.show(attr, getBoss(), this, pl);
			}
		}

		protected ItemStack getAttribute(final BossAttribute attr) {
			return attr.getItem(getBoss());
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Here you edit Boss' attributes",
					"that are common for all Bosses.",
					"",
					"&2Click &7an attribute to edit it."
			};
		}

		@Override
		public Menu newInstance() {
			return new MenuAttributes(getParent());
		}
	}

	private final class MenuPotions extends Menu {

		private final StrictList<PotionEffectType> potionList = new StrictList<>();

		protected MenuPotions() {
			super(new MenuSettings());

			fillPotions();

			setSize(9 * 2 + 9 * (potionList.size() / 9));
			setTitle("Potions");
		}

		private void fillPotions() {
			for (final PotionEffectType type : PotionEffectType.values())
				if (type != null && !Arrays.asList("GLOWING", "INCREASE_DAMAGE", "WEAKNESS").contains(type.toString()))
					potionList.add(type);
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot < potionList.size()) {
				final PotionEffectType type = potionList.get(slot);
				final int lvl = boss.getSettings().getLevelOf(type);
				final String name = (lvl > 0 ? "&f" : "&7") + ItemUtil.bountify(type);
				final boolean longName = name.length() > 15;

				final ItemStack it = ItemCreator.of(
						CompMaterial.POTION,
						"&r" + name + " " + lvl,
						"",
						(longName ? "  " : "") + " &8(Mouse click)",
						(longName ? " " : "") + " &7&l< &4-1    &2+1 &7&l>")
						.amount(lvl > 1 ? lvl : 1)
						.hideTags(true).build().make();

				Remain.setPotion(it, type, lvl);
				return CompMetadata.setMetadata(it, "potion", type.getName());
			}

			if (slot > getSize() - 9)
				return getFillerItem();

			return null;
		}

		@Override
		public void onMenuClick(final Player pl, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack clicked, final boolean cancelled) {
			final ItemStack is = getItemAt(slot);
			if (is == null)
				return;

			final String potionName = CompMetadata.getMetadata(is, "potion");
			final PotionEffectType type = potionName != null ? PotionEffectType.getByName(potionName) : null;

			if (type == null)
				return;

			final int level = getBoss().getSettings().getLevelOf(type);
			final int newLevel = MathUtil.range(level + (click == ClickType.LEFT ? -1 : 1), 0, 50);

			getBoss().getSettings().setPotion(type, newLevel);
			pl.getOpenInventory().getTopInventory().setItem(slot, getItemAt(slot));

			BossUpdateUtil.updateAll();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Here you edit what potions",
					"this Boss receives on spawn.",
					"",
					"Simply &2click the potions",
					"to edit their levels."
			};
		}
	}

	private final class MenuReinforcements extends MenuChooseVanillaOrBoss {

		protected MenuReinforcements(final Menu parent) {
			super(parent);

			setTitle("Death Reinforcements");
		}

		@Override
		protected Button getBossButton() {
			return new ButtonMenu(new MenuReinforcementsBoss(this), ItemCreator.of(
					CompMaterial.ENDERMAN_SPAWN_EGG,
					"Boss Reinforcements",
					"",
					"Select other Bosses",
					"that will be spawned.")
					.build().make());
		}

		@Override
		protected Button getVanillaButton() {
			return new ButtonMenu(new MenuReinforcementsVanilla(this), ItemCreator.of(
					CompMaterial.SPAWNER,
					"Vanilla Reinforcements",
					"",
					"Select vanilla mobs",
					"that will be spawned."));
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"When this Boss dies, you can",
					"set up additional monsters",
					"that will be spawned."
			};
		}
	}

	private abstract static class MenuChooseVanillaOrBoss extends Menu {

		private final Button bossMenu;
		private final Button vanillaMenu;

		protected MenuChooseVanillaOrBoss(final Menu parent) {
			super(parent);

			bossMenu = getBossButton();
			vanillaMenu = getVanillaButton();
		}

		protected abstract Button getBossButton();

		protected abstract Button getVanillaButton();

		protected ItemStack getAdditionaItemAt(final int slot) {
			return null;
		}

		@Override
		public final ItemStack getItemAt(final int slot) {
			if (slot == 9 + 2)
				return bossMenu.getItem();

			if (slot == 9 + 4)
				return vanillaMenu.getItem();

			return getAdditionaItemAt(slot);
		}
	}

	private final class MenuReinforcementsVanilla extends MenuReinforcementsBase {

		protected MenuReinforcementsVanilla(final Menu parent) {
			super(boss.getReinforcementsVanilla(), parent, fillBossesVanilla());

			setTitle("Vanilla Reinforcements");
			setSize(9 * 6);
		}

		@Override
		protected ItemStack getEgg(final String name) {
			return CompMonsterEgg.makeEgg(EntityType.valueOf(name));
		}

		@Override
		protected String formatName(final String name) {
			return ItemUtil.bountifyCapitalized(name);
		}
	}

	protected Iterable<String> fillBossesVanilla() {
		return Common.convert(BossManager.getValidTypes(), EntityType::toString).stream().filter(s -> !s.contains("PARROT")).collect(Collectors.toList());
	}

	private final class MenuReinforcementsBoss extends MenuReinforcementsBase {

		protected MenuReinforcementsBoss(final Menu parent) {
			super(boss.getReinforcementsBoss(), parent, fillBossesBoss());

			setTitle("Boss Reinforcements");
		}

		@Override
		protected ItemStack getEgg(final String name) {
			final Boss boss = BossPlugin.getBossManager().findBoss(name);

			return boss != null ? boss.asEgg() : null;
		}
	}

	private Iterable<String> fillBossesBoss() {
		return BossPlugin.getBossManager().getBossesAsList();
	}

	private abstract class MenuReinforcementsBase extends MenuPagged<String /* Boss name */> implements MenuQuantitable {

		private final AutoUpdateMap<String, Integer> map;

		@Getter
		@Setter
		private MenuQuantity quantity = MenuQuantity.ONE;

		private final Button quantityButton;

		protected MenuReinforcementsBase(final AutoUpdateMap<String, Integer> map, final Menu parent, final Iterable<String> bosses) {
			super(9 * 5, parent, bosses);

			this.map = map;

			quantityButton = getEditQuantityButton(this);
		}

		protected abstract ItemStack getEgg(String name);

		protected String formatName(final String name) {
			return name;
		}

		@Override
		protected ItemStack convertToItemStack(final String name) {
			final boolean longName = name.length() > 15;

			final int currentQuantity = map.getOrDefault(name, 0);

			final ItemStack it = ItemCreator
					.of(getEgg(name))
					.name("&r" + formatName(name))
					.lores(Arrays.asList(
							"",
							"&7Amount: &f" + currentQuantity,
							"",
							(longName ? "  " : "") + " &8(Mouse click)",
							(longName ? " " : "") + " &7&l< &4-{q}    &2+{q} &7&l>".replace("{q}", quantity.getAmount() + "")))
					.glow(currentQuantity > 0)
					.build().make();

			if (currentQuantity == 0)
				for (final Enchantment en : it.getEnchantments().keySet())
					it.removeEnchantment(en);

			it.setAmount(currentQuantity > 0 ? currentQuantity : 1);

			return it;
		}

		@Override
		public final ItemStack getItemAt(final int slot) {
			if (slot == getSize() - 5)
				return quantityButton.getItem();

			return super.getItemAt(slot);
		}

		@Override
		protected final void onPageClick(final Player pl, final String name, final ClickType click) {
			final int quantity = map.getOrDefault(name, 0);
			final int newQuantity = MathUtil.range(quantity + getNextQuantity(click), 0, 30);

			if (map.contains(name) && newQuantity == 0)
				map.removeAndUpdate(name);
			else
				map.overrideAndUpdate(name, newQuantity);

			restartMenu();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Here you edit what other monsters",
					"or bosses should be spawned when",
					"this Boss dies."
			};
		}
	}

	private final class MenuSkills extends MenuPagged<BossSkill> {

		protected MenuSkills() {
			super(9 * 2, MenuBossIndividual.this, BossSkillRegistry.getRegistered());

			setTitle("Skills");
		}

		@Override
		protected ItemStack convertToItemStack(final BossSkill skill) {
			final boolean has = boss.getSkills().contains(skill);

			final ItemStack item = skill.getIcon();
			final ItemMeta meta = item.getItemMeta();

			final List<String> lore = meta.getLore() == null ? new ArrayList<>() : meta.getLore();

			lore.addAll(Arrays.asList(
					"",
					Common.colorize(has ? "&aSkill is activated." : "&cSkill is disabled.")));

			return ItemCreator.of(item).clearLores().lores(lore).glow(has).build().make();
		}

		@Override
		protected void onPageClick(final Player pl, final BossSkill s, final ClickType click) {
			final AutoUpdateList<BossSkill> skills = boss.getSkills();
			final boolean has = skills.contains(s);

			if (has)
				skills.removeAndUpdate(s);
			else
				skills.addAndUpdate(s);

			BossUpdateUtil.updateAll();

			restartMenu(has ? "&4The skill has been removed!" : "&2The skill has been activated!");
		}

		@Override
		public Menu newInstance() {
			return new MenuSpecific(getParent());
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure specific",
					"settings for this Boss."
			};
		}
	}

	private final class MenuSpawning extends Menu {

		private final Button worldsButton;
		private final Button regionsButton;
		private final Button biomesButton;

		private final Button conditionsButton;
		private final Button convertingChanceButton;
		private final Button eggButton;

		public MenuSpawning() {
			super(MenuBossIndividual.this);

			setSize(9 * 6);
			setTitle("Spawning");

			worldsButton = new ButtonMenu(new MenuWorlds(MenuSpawning.this),
					CompMaterial.GRASS_BLOCK,
					"Worlds",
					"",
					"Select worlds in which",
					"the Boss spawns.");

			final boolean regionSpawning = getBoss().getSpawning().getRegions().hasRegionSpawning();

			regionsButton = new ButtonMenu(new MenuRegions(MenuSpawning.this), ItemCreator.of(regionSpawning ? CompMaterial.BEACON : CompMaterial.GLASS,
					"&fRegions",
					"",
					regionSpawning ? "&2Region Spawning is enabled." : "&7Region Spawning is disabled.",
					"",
					"Specify in what regions",
					"Boss may spawn in.")
					.glow(regionSpawning)
					.build().make());

			biomesButton = new ButtonMenu(new MenuBiomes(MenuSpawning.this), ItemCreator.of(
					CompMaterial.SPRUCE_SAPLING,
					"&2Biomes",
					"",
					"Specify in what biomes",
					"the boss should appear."));

			conditionsButton = new ButtonMenu(new MenuSpawnConditions(MenuSpawning.this),
					CompMaterial.HOPPER,
					"Native Conditions",
					"",
					"Specify when the Boss",
					"spawns, for example what",
					"time, sea or light level.");

			convertingChanceButton = new MenuButtonConvo(this, new BossConversation.PromptConvertingChance(boss),
					ItemCreator.of(CompMaterial.CLOCK,
							"Converting Chance",
							"",
							"Current: " + boss.getSettings().getConvertingChance() + "%",
							"",
							"Click to edit the converting",
							"chance. This will only work if",
							"Converting.Enabled is on true",
							"in settings.yml"));

			eggButton = new ButtonMenu(new MenuEggItem(this), ItemCreator.of(CompMaterial.ENDERMAN_SPAWN_EGG,
					"Customize Egg Item",
					"",
					"Click here to edit",
					"the outlook of this",
					"Boss' spawner egg.").build().make());
		}

		@Override
		public ItemStack getItemAt(final int slot) {
			if (Arrays.asList(0, 9, 18, 27, 36, 8, 17, 26, 35, 44).contains(slot))
				if (hasSpawning() && !Settings.TimedSpawning.ENABLED)
					return ItemCreator.of(CompMaterial.RED_STAINED_GLASS_PANE,
							"&cWarning: Your Boss will not spawn!",
							"",
							"You have enabled region or world",
							"spawning but Timed_Spawning.Enabled",
							"in settings.yml is on false!",
							"",
							"Set it to true for your Boss to appear.").build().make();

				else if (hasRegionSpawningButNotWorlds())
					return ItemCreator.of(CompMaterial.RED_STAINED_GLASS_PANE,
							"&cWarning: Your Boss will not spawn!",
							"",
							"You have enabled region spawning but",
							"all of your worlds have 0% spawn rates.",
							"",
							"If you want your Boss to spawn increase",
							"spawning rates there first!").build().make();

				else if (!Settings.TimedSpawning.WORLDS.isEntireList())
					return ItemCreator.of(CompMaterial.ORANGE_STAINED_GLASS_PANE,
							"&cWarning: &7Your Timed_Spawning.World",
							"in settings.yml makes your boss",
							"only spawn in the following worlds:",
							"",
							Common.join(Settings.TimedSpawning.WORLDS.getList())).build().make();

			if (slot == 9 + 1)
				return worldsButton.getItem();

			if (slot == 9 + 3)
				return regionsButton.getItem();

			if (slot == 9 + 5)
				return biomesButton.getItem();

			if (slot == 9 + 7)
				return conditionsButton.getItem();

			if (slot == 9 * 3 + 3)
				return eggButton.getItem();

			if (slot == 9 * 3 + 5)
				return convertingChanceButton.getItem();

			return null;
		}

		private boolean hasSpawning() {
			return boss.getSpawning().getRegions().hasRegionSpawning() || hasWorldSpawning();
		}

		private boolean hasRegionSpawningButNotWorlds() {
			return boss.getSpawning().getRegions().hasRegionSpawning() && !hasWorldSpawning();
		}

		private boolean hasWorldSpawning() {
			for (final int spawnRate : boss.getSpawning().getWorlds().values())
				if (spawnRate > 0)
					return true;

			return false;
		}

		@Override
		public Menu newInstance() {
			return new MenuSpawning();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure how your Boss naturally",
					"appears in your worlds or regions.",
					"",
					"&6TIP: &7To understand how spawning",
					"works, set Debug to [spawning] in",
					"settings.yml and see your console."
			};
		}
	}

	private final class MenuWorlds extends Menu implements MenuQuantitable {

		private final StrictList<String> list = new StrictList<>();

		@Setter
		@Getter
		private MenuQuantity quantity = MenuQuantity.ONE;

		private final Button quantityButton;

		protected MenuWorlds(final Menu parent) {
			super(parent);

			fill();

			setSize(18 + 9 * (list.size() / 9));
			setTitle("Spawning in Worlds");

			quantityButton = getEditQuantityButton(this);
		}

		private void fill() {
			for (final World world : Bukkit.getWorlds())
				list.add(world.getName());
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == getSize() - 5)
				return quantityButton.getItem();

			if (slot < list.size()) {
				final World world = Bukkit.getWorld(list.get(slot));
				final boolean longName = world.getName().length() > 12;
				final int env = world.getEnvironment().getId();

				return ItemCreator.of(env == -1 ? CompMaterial.NETHERRACK : env == 1 ? CompMaterial.END_STONE : CompMaterial.GRASS_BLOCK,
						"&r" + WordUtils.capitalize(world.getName()),
						"",
						"&7Type: &" + (env == -1 ? "c" : env == 1 ? "b" : "f") + ItemUtil.bountifyCapitalized(world.getEnvironment()),
						"&7Chance: &f" + boss.getSpawning().getWorlds().getOrDefault(world.getName(), 0) + "%",
						"",
						(longName ? "  " : "") + " &8(Mouse click)",
						(longName ? " " : "") + " &7&l< &4-{q}%    &2+{q}% &7&l>".replace("{q}", quantity.getAmount() + ""))
						.build().make();
			}

			return null;
		}

		@Override
		public void onMenuClick(final Player pl, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack clicked, final boolean cancelled) {
			final World w = slot < list.size() ? Bukkit.getWorld(list.get(slot)) : null;

			if (w == null)
				return;

			final AutoUpdateMap<String, Integer> copy = getBoss().getSpawning().getWorlds();

			final int percent = copy.getOrDefault(w.getName(), 0);
			final int newPercent = MathUtil.range(percent + getNextQuantity(click), 0, 100);

			if (copy.contains(w.getName()) && newPercent == 0)
				copy.removeAndUpdate(w.getName());

			else
				copy.overrideAndUpdate(w.getName(), newPercent);

			pl.getOpenInventory().getTopInventory().setItem(slot, getItemAt(slot));
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Specify in which worlds",
					"the Boss should appear.",
					"",
					"&2Click the icons &7to edit how",
					"likely it is for a monster",
					"of the same type to be",
					"converted into a Boss",
					"in a world."
			};
		}
	}

	private final class MenuBiomes extends MenuPagged<Biome> implements MenuQuantitable {

		@Setter
		@Getter
		private MenuQuantity quantity = MenuQuantity.ONE;

		private final Button quantityButton;
		private final Button resetButton;

		protected MenuBiomes(final Menu parent) {
			super(9 * 5, parent, Arrays.asList(Biome.values()));

			setTitle("Spawning in Biomes");

			quantityButton = getEditQuantityButton(this);
			resetButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					boss.getSpawning().getBiomes().clearAndUpdate();

					restartMenu("&4Biomes were reset!");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(CompMaterial.FLINT,
							"Clear Biomes",
							"",
							"Click to &4remove all configured",
							"biomes and &2allow this Boss to",
							"&2spawn everywhere&7.").build().make();
				}
			};
		}

		@Override
		protected ItemStack convertToItemStack(final Biome biome) {
			final boolean longName = biome.toString().length() > 12;
			final CompMaterial icon = getIcon(biome);
			final int chance = boss.getSpawning().getBiomes().getOrDefault(biome, 0);

			return ItemCreator.of(
					icon,
					ItemUtil.bountifyCapitalized(biome),
					"",
					"&7Chance: &f" + chance + "%",
					"",
					(longName ? "  " : "") + " &8(Mouse click)",
					(longName ? " " : "") + " &7&l< &4-{q}%    &2+{q}% &7&l>".replace("{q}", quantity.getAmount() + ""))
					.glow(chance > 0)
					.build().make();
		}

		@Override
		protected void onPageClick(final Player pl, final Biome b, final ClickType click) {
			final AutoUpdateMap<Biome, Integer> copy = getBoss().getSpawning().getBiomes();

			final int percent = copy.getOrDefault(b, 0);
			final int newPercent = MathUtil.range(percent + getNextQuantity(click), 0, 100);

			if (newPercent == 0) {
				if (copy.contains(b))
					copy.removeAndUpdate(b);
			} else
				copy.overrideAndUpdate(b, newPercent);

			restartMenu("&1Spawn chances were updated!");
		}

		@Override
		public ItemStack getItemAt(final int slot) {
			if (slot == getSize() - 5)
				return quantityButton.getItem();

			if (slot == getSize() - 2)
				return resetButton.getItem();

			return super.getItemAt(slot);
		}

		private CompMaterial getIcon(final Biome b) {
			final String t = b.toString();

			if (t.contains("BEACH"))
				return CompMaterial.SANDSTONE;

			if (t.contains("BIRCH"))
				return CompMaterial.SPRUCE_SAPLING;

			if (t.contains("DESERT"))
				return CompMaterial.SAND;

			if (t.contains("FOREST"))
				return CompMaterial.OAK_SAPLING;

			if (t.contains("FROZEN") || t.contains("ICE"))
				return CompMaterial.ICE;

			if (t.contains("SNOW"))
				return CompMaterial.SNOW_BLOCK;

			if (t.contains("JUNGLE"))
				return CompMaterial.JUNGLE_SAPLING;

			if (t.contains("MESA"))
				return CompMaterial.RED_SAND;

			if (t.contains("MUSHROOM"))
				return CompMaterial.BROWN_MUSHROOM_BLOCK;

			if (t.contains("OCEAN") || t.contains("RIVER"))
				return CompMaterial.WATER_BUCKET;

			if (t.contains("SAVANNA"))
				return CompMaterial.ACACIA_SAPLING;

			if (t.contains("SKY") || t.contains("VOID"))
				return CompMaterial.GRAY_WOOL;

			if (t.contains("TAIGA"))
				return CompMaterial.DARK_OAK_SAPLING;

			if (t.contains("SWAMP"))
				return CompMaterial.LILY_PAD;

			if (t.contains("PLAINS") || t.contains("MOUNTAIN"))
				return CompMaterial.FERN;

			if (t.contains("HILLS"))
				return CompMaterial.STONE;

			if (t.equals("HELL") || t.equals("NETHER"))
				return CompMaterial.NETHERRACK;

			if (t.equals("THE_END") || t.equals("END_BARRENS"))
				return CompMaterial.END_STONE_BRICKS;

			if (t.contains("LANDS"))
				return CompMaterial.GRASS_BLOCK;

			if (t.equals("STONE_SHORE"))
				return CompMaterial.STONE;

			Common.log("Unresolved biome in Boss: " + t);
			return CompMaterial.WHITE_STAINED_GLASS_PANE;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Specify biomes where the Boss",
					"should appear.",
					"",
					"&2Click the icons &7to configure",
					"spawning/converting chances.",
					"",
					"&6NB: &7By selecting any biome",
					"&cBoss will then ONLY appear",
					"&cin those biomes.",
					"",
					"&6NB2: &7You also &cmust increase the",
					"spawning chances in Worlds menu."
			};
		}
	}

	private final class MenuSpawnConditions extends Menu {

		private final Button timeButton;
		private final Button lightButton;
		private final Button heightButton;
		private final Button rainButton;
		private final Button thunderButton;
		private final Button underWaterButton;

		protected MenuSpawnConditions(final Menu parent) {
			super(parent);

			setSize(9 * 4);
			setTitle("Native Conditions");

			final BossNativeConditions c = getBoss().getSpawning().getConditions();

			timeButton = makeButton(PromptTime.class, ItemCreator.of(CompMaterial.CLOCK,
					"&aTime",
					"",
					"Current: &f" + c.getTime(),
					"",
					"Specify when during the",
					"day or night the Boss",
					"shall spawn."));

			lightButton = makeButton(PromptLight.class, ItemCreator.of(CompMaterial.TORCH,
					"&eLight Level",
					"",
					"Current: &f" + c.getLight(),
					"",
					"Set how dark the",
					"environment in which",
					"the Boss spawns",
					"should be.",
					"",
					"0 - absolute dark",
					"15 - maximum light"));

			heightButton = makeButton(PromptHeight.class, ItemCreator.of(CompMaterial.LADDER,
					"&6Height",
					"",
					"Current: &f" + c.getHeight(),
					"",
					"Specify in which y-height",
					"the Boss should spawn.",
					"",
					"0 - Bedrock bottom",
					"256 - default max limit"));

			rainButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					final boolean flag = c.isRainRequired();

					c.setRainRequired(!flag);

					restartMenu("&0" + (flag ? "&4Boss no longer requires rain." : "&2Boss now requires rain."));
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(CompMaterial.WATER_BUCKET,
							"&bRequires Rain",
							"",
							c.isRainRequired() ? "&3Condition enabled." : "Condition disabled.",
							"",
							"Should the Boss only",
							"spawn in rain?",
							"Click to change.").glow(c.isRainRequired()).build().make();
				}
			};

			thunderButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					final boolean flag = c.isThunderRequired();

					c.setThunderRequired(!flag);

					restartMenu("&0" + (flag ? "&4Thunder condition was disabled." : "&2Boss now requires thunder."));
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(CompMaterial.ENDER_EYE,
							"&9Requires Thunder",
							"",
							c.isThunderRequired() ? "&3Condition enabled." : "Condition disabled.",
							"",
							"Should the Boss only",
							"spawn in a thunderstorm?",
							"Click to change.").glow(c.isThunderRequired()).build().make();
				}
			};

			underWaterButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					final boolean has = has();

					boss.getSpawning().getConditions().setSpawnUnderWater(!has);

					restartMenu(has ? "&4Disabled underwater spawn" : "&2Enabled underwater spawn");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = has();

					return ItemCreator.of(has ? CompMaterial.WATER_BUCKET : CompMaterial.BUCKET,
							has ? "&bSpawns Under Water" : "Does Not Spawn Under Water",
							"",
							"Click to toggle.",
							"",
							has ? "The Boss spawns on ground and" : "The Boss spawns on the highest",
							has ? "also under water, but not in caves." : "point at a given location.")
							.glow(has)
							.build().make();
				}

				private boolean has() {
					return boss.getSpawning().getConditions().canSpawnUnderWater();
				}
			};
		}

		@Override
		public ItemStack getItemAt(final int slot) {
			if (slot == 9 + 1)
				return timeButton.getItem();

			if (slot == 9 + 2)
				return lightButton.getItem();

			if (slot == 9 + 3)
				return heightButton.getItem();

			if (slot == 9 + 5)
				return underWaterButton.getItem();
			if (slot == 9 + 6)
				return rainButton.getItem();
			if (slot == 9 + 7)
				return thunderButton.getItem();

			return null;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Set various conditions for",
					"Boss' natural spawning.",
					"",
					"&2Click the icons &7to edit",
					"the specific conditions."
			};
		}

		@Override
		public Menu newInstance() {
			return new MenuSpawnConditions(getParent());
		}

		private MenuButtonConvo makeButton(final Class<? extends BossPrompt> prompt, final ItemCreator.ItemCreatorBuilder item) {
			return new MenuButtonConvo(boss, MenuSpawnConditions.this, prompt, item);
		}
	}

	private final class MenuRegions extends MenuPagged<BossRegionType> {

		private final Button blacklistButton;

		protected MenuRegions(final Menu parent) {
			super(9 * 2, parent, Arrays.asList(BossRegionType.values()), true);

			setTitle("Spawning in Regions");

			blacklistButton = new Button() {

				private boolean is() {
					return boss.getSpawning().getRegions().isBlacklist();
				}

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					boss.getSpawning().getRegions().setBlacklist(!is());

					restartMenu(is() ? "&4Blacklist mode disabled." : "&2Blacklist mode enabled.");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(
							is() ? CompMaterial.GREEN_DYE : CompMaterial.INK_SAC,
							"Blacklist",
							"",
							"Status: " + (is() ? "&2Enabled" : "&6Disabled"),
							"",
							"&2When blacklist is enabled&7,",
							"the Boss spawns everywhere",
							"else but NOT in selected regions.",
							"",
							"&6Disabled blacklist &7means that,",
							"the Boss spawns ONLY in selected",
							"regions with their specific settings.")
							.build().make();
				}
			};
		}

		@Override
		protected ItemStack convertToItemStack(final BossRegionType p) {
			final int enabled = getBoss().getSpawning().getRegions().getCount(p);

			return ItemCreator.of(
					CompMaterial.fromMaterial(p.getButtonMaterial()),
					p.getPlugin() + " Regions",
					"")
					.lores(Arrays.asList(p.getButtonDescription()))
					.lore("")
					.lore(p.isPluginEnabled() ? (enabled > 0 ? "&2" : "&7") + "Selected " + enabled + "/" + p.getRegions().size() : "&4Plugin not detected.")
					.amount(enabled > 0 ? enabled : 1)
					.build().make();
		}

		@Override
		public ItemStack getItemAt(final int slot) {
			if (slot == getSize() - 4)
				return blacklistButton.getItem();

			if (slot == getSize() - 6) {
				final boolean hasRegionSpawning = getBoss().getSpawning().getRegions().hasRegionSpawning();
				final List<String> offLore = Arrays.asList(
						"This Boss &6spawns everywhere",
						"on all the worlds specified",
						"in the Worlds menu, &6when",
						"&6all other conditions are met&7.");

				final List<String> onLore = Arrays.asList(
						"This Boss only &6spawns in the",
						"&6regions above when all the",
						"&6other conditions are met&7.");

				return ItemCreator.of(hasRegionSpawning ? CompMaterial.BEACON : CompMaterial.GLASS,
						hasRegionSpawning ? "&aRegion Spawning is Enabled" : "&fRegion Spawning is Disabled",
						"")
						.lores(hasRegionSpawning ? onLore : offLore)
						.glow(hasRegionSpawning)
						.build().make();
			}

			return super.getItemAt(slot);
		}

		@Override
		protected void onPageClick(final Player pl, final BossRegionType region, final ClickType click) {
			if (region.isPluginEnabled())
				new MenuRegion(MenuRegions.this, region).displayTo(pl);

			else
				animateTitle("&4The plugin " + region.getPlugin() + " is missing!");
		}

		@Override
		protected boolean updateButtonOnClick() {
			return false;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure specific regions",
					"in which the Boss spawns.",
					"",
					"&6NB: &7By selecting any regions, you",
					"enable so called Region Spawning.",
					"&cBoss will then ONLY appear",
					"&cin those regions you select.",
					"",
					"&6NB2: &7You also &cmust increase",
					"the spawning chances in Worlds menu."
			};
		}
	}

	private final class MenuRegion extends MenuPagged<String> {

		private final BossRegionType regionType;

		private Button regionInfoButton;
		private Button allButton;
		private Button resetButton;

		protected MenuRegion(final Menu parent, final BossRegionType region) {
			super(9 * 2, parent, region.getRegions());

			regionType = region;

			setTitle("Select Regions");
			setFields();
		}

		private void setFields() {
			final boolean on = getRegions().hasRegions(regionType);

			regionInfoButton = Button.makeDummy(ItemCreator.of(
					on ? CompMaterial.BEACON : CompMaterial.GLASS,
					on ? "&aRegions are enabled" : "&fRegions are disabled",
					"",
					on ? "Boss only spawns in the" : "Boss is not affected",
					on ? "regions marked above" : "by any of the regions",
					on ? "and in other menues." : "above."));

			allButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					for (String rg : regionType.getRegions()) {
						rg = Common.stripColors(rg);

						if (!getRegions().hasRegion(regionType, rg))
							getRegions().add(regionType, rg);
					}

					restartMenu("&2All regions were set!");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(CompMaterial.SLIME_BALL,
							"Add All Regions",
							"",
							"Click to &2all every",
							"region to the Boss.").build().make();
				}
			};

			resetButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					getRegions().clear(regionType);

					restartMenu("&4Regions were reset!");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(CompMaterial.FLINT,
							"Clear Regions",
							"",
							"Click to &4remove all",
							"assigned regions",
							"from this Boss.").build().make();
				}
			};
		}

		private BossRegionSpawning getRegions() {
			return getBoss().getSpawning().getRegions();
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == getSize() - 8)
				return allButton.getItem();

			if (slot == getSize() - 5)
				return regionInfoButton.getItem();

			if (slot == getSize() - 2)
				return resetButton.getItem();

			return super.getItemAt(slot);
		}

		@Override
		protected ItemStack convertToItemStack(final String regionName) {
			final boolean on = getRegions().hasRegion(regionType, regionName);

			return ItemCreator.of(CompMaterial.GRASS,
					WordUtils.capitalize(regionName),
					"",
					on ? "&2Spawning is enabled." : "Spawning is disabled.",
					on ? "&2Left click to open settings." : "",
					on ? "&7Right click to disable." : "Left click to enable.").glow(on).build().make();
		}

		@Override
		protected void onPageClick(final Player pl, final String rgName, final ClickType click) {
			final String regionName = Common.stripColors(rgName);

			final boolean has = getRegions().hasRegion(regionType, regionName);

			if (click == ClickType.LEFT) {
				if (has)
					Common.runLater(() -> new SpecificRegionMenu(regionType, getRegions().findRegion(regionType, regionName), this).displayTo(pl));
				else {
					getRegions().add(regionType, regionName);
					restartMenu("&2Region added!");
				}

			} else if (has) {
				getRegions().remove(regionType, regionName);
				restartMenu("&4Region removed!");
			}
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Specify regions in which",
					"the Boss should appear.",
					"",
					"&2Click the icons &7to enable",
					"or disable individual regions.",
					"",
					"&cNB: &7If any region is enabled,",
					"Boss spawns *only there* and",
					"not elsewhere on your server!"
			};
		}
	}

	private final class SpecificRegionMenu extends Menu {

		private final BossRegionType type;
		private final BossRegionSettings region;

		private final MenuButtonConvo limitButton;
		private final Button keepButton;

		private SpecificRegionMenu(final BossRegionType type, final BossRegionSettings settings, final Menu parent) {
			super(parent);

			setTitle("Region " + settings.getRegionName());
			setSize(9 * 3);

			this.type = type;
			region = settings;

			limitButton = new MenuButtonConvo(this, new BossConversation.PromptRegionLimit(boss, region),
					ItemCreator.of(CompMaterial.SHEEP_SPAWN_EGG,
							"Limit",
							"",
							"Current: " + (region.getLimit() == -1 ? "&ounlimited" : region.getLimit()),
							"",
							"Click to set how many",
							"bosses can appear in",
							"this region at once.")
							.amount(MathUtil.range(region.getLimit(), 1, Short.MAX_VALUE)));

			keepButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {

					if (type != BossRegionType.BOSS) {
						animateTitle("Only Boss' regions support this feature.");

						return;
					}

					final boolean was = region.getKeepInside();

					region.setKeepInside(!was);
					restartMenu(was ? "&4Boss can now escape." : "&2Boss is now kept inside.");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(region.getKeepInside() ? CompMaterial.BEACON : CompMaterial.GLASS,
							"Keep Inside",
							"",
							"Enabled: " + (region.getKeepInside() ? "&2yes" : "&cno"),
							"",
							"Should we automatically teleport",
							"your Boss back one he reaches",
							"beyond the border of this region?",
							"",
							"Only works for newly spawned Bosses",
							"because we mark Bosses spawned in this",
							"region with invisible tag and can so",
							"keep them inside.")
							.build().make();
				}
			};
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == 9 + 3)
				return keepButton.getItem();

			if (slot == 9 + 5)
				return limitButton.getItem();

			return null;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"This menu controls",
					"individual settings for",
					"this boss spawning in",
					"this region."
			};
		}

		@Override
		public Menu newInstance() {
			return new SpecificRegionMenu(type, region, getParent());
		}
	}

	private final class MenuRiding extends MenuChooseVanillaOrBoss {

		final Button killRidingButton;

		protected MenuRiding(final Menu parent) {
			super(parent);

			setTitle("Riding");

			this.killRidingButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					final boolean has = getBoss().getSettings().isRemovingRidingOnDeath();

					getBoss().getSettings().setRemoveRidingOnDeath(!has);
					restartMenu(has ? "&4Riding doesn't die with Boss." : "&2Riding entity dies with Boss.");
				}

				@Override
				public ItemStack getItem() {
					final boolean has = getBoss().getSettings().isRemovingRidingOnDeath();

					return ItemCreator.of(CompMaterial.BONE,
							"Kill Riding on Death",
							"",
							has ? "&aRiding entity dies together" : "&7Riding entity continues to live after",
							has ? "&awhen the Boss dies." : "&7Boss' death and must be killed manually.",
							"",
							"Click to toggle.")
							.glow(has)
							.build().make();
				}
			};
		}

		@Override
		protected ItemStack getAdditionaItemAt(final int slot) {

			if (slot == 9 + 6)
				return killRidingButton.getItem();

			return null;
		}

		@Override
		protected Button getBossButton() {
			return new ButtonMenu(new MenuRidingBoss(this, getBoss()), ItemCreator.of(
					CompMaterial.ENDERMAN_SPAWN_EGG,
					"Boss Reinforcements",
					"",
					"Select other Bosses",
					"that this Boss will ride.")
					.build().make());
		}

		@Override
		protected Button getVanillaButton() {
			return new ButtonMenu(new MenuRidingVanilla(this), ItemCreator.of(
					CompMaterial.SPAWNER,
					"Vanilla Riding",
					"",
					"Select vanilla mobs",
					"that this Boss will ride."));
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select what Bosses or monsters",
					"this Boss will ride.",
					"",
					"&cNB:&7 Due to Bukkit limitations,",
					"a Boss can only ride one entity (",
					"you can bypass that if you set the",
					"Boss to ride another Boss, that will",
					"ride one another entity as well, etc.)",
					"",
					"Also, riding entities cannot be",
					"updated in real-time and &cany changes",
					"&cwill only be applied on new bosses&7."
			};
		}
	}

	private static StrictList<EntityType> compileRiding() {
		final StrictList<EntityType> list = new StrictList<>();

		for (final EntityType type : EntityType.values())
			if (type.getEntityClass() != null && Creature.class.isAssignableFrom(type.getEntityClass()))
				list.add(type);

		return list;
	}

	private final class MenuRidingVanilla extends MenuPagged<EntityType> {

		protected MenuRidingVanilla(final Menu parent) {
			super(9 * 5, parent, compileRiding());

			setTitle("Riding");
		}

		@Override
		protected ItemStack convertToItemStack(final EntityType type) {
			final EntityType riding = boss.getSettings().getRidingVanilla();
			final boolean is = riding != null && riding == type;

			return ItemCreator.of(CompMaterial.makeMonsterEgg(type),
					ItemUtil.bountifyCapitalized(type),
					"",
					"Click to " + (is ? "&cun" : "&2") + "select",
					"this entity for riding.")
					.glow(is)
					.build().make();
		}

		@Override
		protected void onPageClick(final Player pl, final EntityType type, final ClickType click) {
			final BossSettings settings = boss.getSettings();
			final EntityType riding = settings.getRidingVanilla();

			if (riding != null && riding == type) {
				animateTitle("&4Riding has been disabled!");
				settings.setRidingVanilla(null);
				BossUpdateUtil.updateAll();

				return;
			}

			settings.setRidingVanilla(type);

			BossUpdateUtil.updateAll();

			restartMenu("&2Boss now rides " + Common.article(ItemUtil.bountifyCapitalized(type)));
		}

		@Override
		public Menu newInstance() {
			return new MenuRidingVanilla(getParent());
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select what entity",
					"should the Boss sit on."
			};
		}
	}

	private final class MenuRidingBoss extends MenuPagged<Boss> {

		protected MenuRidingBoss(final Menu parent, final Boss myBoss) {
			super(9 * 2, parent, BossPlugin.getBossManager().getBosses().stream().filter(otherBoss -> !otherBoss.equals(myBoss)).collect(Collectors.toList()));

			setTitle("Riding");
		}

		@Override
		protected ItemStack convertToItemStack(final Boss boss) {
			final String riding = getBoss().getSettings().getRidingBoss();
			final boolean is = riding != null && riding.equals(boss.getName());

			return ItemCreator.of(CompMaterial.makeMonsterEgg(boss.getType()),
					boss.getName(),
					"",
					"Click to " + (is ? "&cun" : "&2") + "select",
					"this Boss for riding.")
					.glow(is)
					.build().make();
		}

		@Override
		protected void onPageClick(final Player pl, final Boss ridingBoss, final ClickType click) {
			final BossSettings settings = boss.getSettings();
			final String currentRidingBoss = settings.getRidingBoss();

			if (currentRidingBoss != null && currentRidingBoss.equals(ridingBoss.getName())) {
				animateTitle("&4Riding has been disabled!");
				settings.setRidingBoss(null);
				BossUpdateUtil.updateAll();

				return;
			}

			settings.setRidingBoss(ridingBoss.getName());

			BossUpdateUtil.updateAll();
			restartMenu("&2Boss now rides " + ridingBoss.getName());
		}

		@Override
		public Menu newInstance() {
			return new MenuRidingVanilla(getParent());
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select what entity",
					"should the Boss sit on."
			};
		}
	}

	private final class MenuEggItem extends Menu {

		private final BossEggItem egg;

		private final Button materialButton;
		private final Button dataButton;
		private final Button glowButton;
		private final Button nameButton;
		private final Button loreButton;
		private final Button previewButton;
		private final Button noSpawnPermissionButton;

		private MenuEggItem(final Menu parent) {
			super(parent);

			egg = boss.getSettings().getEggItem();

			setTitle("Egg Item");
			setSize(9 * 4);

			materialButton = new MenuButtonConvo(this, new BossConversation.PromptEggMaterial(boss),
					ItemCreator.of(CompMaterial.SHEEP_SPAWN_EGG,
							"Material",
							"",
							"Current: " + ItemUtil.bountifyCapitalized(egg.getMaterial()),
							"",
							"Click to define the material",
							"of the spawner egg."));

			dataButton = MinecraftVersion.atLeast(V.v1_13) ? Button.makeEmpty()
					: new MenuButtonConvo(this, new BossConversation.PromptEggData(boss),
							ItemCreator.of(CompMaterial.LAVA_BUCKET,
									"Data value",
									"",
									"Current: " + egg.getMaterial().getData(),
									"",
									"Click to set the data",
									"value of the egg material."));

			{ // lore
				final List<String> lore = new ArrayList<>();

				lore.add("");
				lore.add("Current: {");
				{
					final List<String> oldlore = new ArrayList<>(egg.getLore());

					if (!oldlore.isEmpty() && oldlore.get(0).replace(" ", "").isEmpty())
						oldlore.remove(0);

					for (int i = 0; i < oldlore.size(); i++)
						oldlore.set(i, "  " + oldlore.get(i));

					lore.addAll(oldlore);
				}
				lore.add("}");
				lore.add("");
				lore.add("Click to edit the lore.");

				loreButton = new MenuButtonConvo(this, new BossConversation.PromptEggLore(boss),
						ItemCreator.of(CompMaterial.WRITABLE_BOOK).name("Lore").lores(lore));

			}

			nameButton = new MenuButtonConvo(this, new BossConversation.PromptEggName(boss),
					ItemCreator.of(CompMaterial.FEATHER,
							"Name",
							"",
							"Current: " + egg.getName(),
							"",
							"Click to edit the name."));

			glowButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					final boolean was = egg.isGlowing();
					egg.setGlowing(!was);

					restartMenu(was ? "&4Egg is no longer glowing." : "&2Egg now shines glowing bright.");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(CompMaterial.DIAMOND,
							"Glowing",
							"",
							"Enabled: " + (egg.isGlowing() ? "&2yes" : "&cno"),
							"",
							"Should the egg be glowing?",
							"Not all materials support this.")
							.glow(egg.isGlowing())
							.build().make();
				}
			};

			previewButton = new Button() {

				@Override
				public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
				}

				@Override
				public ItemStack getItem() {
					return boss.asEgg();
				}
			};

			noSpawnPermissionButton = new MenuButtonConvo(this, new BossConversation.NoSpawnPermMessage(boss), ItemCreator.of(
					CompMaterial.BOOK,
					"No Spawn Permission",
					"",
					"Click to edit the message",
					"sent to player attempting",
					"to use this egg without",
					"permission.",
					"",
					"PS: See your Boss Egg in the",
					"menu 2 pages before for the",
					"permission itself."));

		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == 9 + 1)
				return nameButton.getItem();

			if (slot == 9 + 2)
				return loreButton.getItem();

			if (slot == 9 + 3)
				return glowButton.getItem();

			if (slot == 9 + 4)
				return materialButton.getItem();

			if (slot == 9 + 5)
				return dataButton.getItem();

			if (slot == 9 + 6)
				return noSpawnPermissionButton.getItem();

			if (slot == getSize() - 5)
				return previewButton.getItem();

			return null;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Change the outlook of this",
					"Boss' spawner egg item. By",
					"default, the egg has properties",
					"set in your settings.yml.",
					"",
					"&2See to the right for a preview.",
					"",
					"&cAny changes here will not",
					"&caffect existing eggs."
			};
		}

		@Override
		public Menu newInstance() {
			return new MenuEggItem(getParent());
		}
	}

	private final class MenuCommandsOverview extends Menu {

		private final Button spawnCommandsButton;
		private final Button deathCommandsButton;
		private final Button deathByPlayerCommandsButton;

		protected MenuCommandsOverview(final Menu parent) {
			super(parent);

			setSize(9 * 4);
			setTitle("Commands");

			spawnCommandsButton = new ButtonMenu(new MenuCommands(this, CommandType.SPAWN),
					CompMaterial.SPAWNER,
					"Commands on Spawn",
					"",
					"Commands that are run",
					"when the Boss spawns.");

			deathCommandsButton = new ButtonMenu(new MenuCommands(this, CommandType.DEATH),
					CompMaterial.BONE,
					"Commands on Death",
					"",
					"Commands that are run",
					"when the Boss dies.");

			deathByPlayerCommandsButton = !getBoss().getSettings().hasInventoryDrops()
					? Button.makeDummy(ItemCreator.of(CompMaterial.CHEST,
							"Commands on Death by Player",
							"",
							"&cTo run commands for players",
							"&cthat killed this Boss, enable",
							"&cinventory drops in Death->Drops."))

					: new ButtonMenu(new MenuCommands(this, CommandType.DEATH_BY_PLAYER),
							CompMaterial.CHEST,
							"Commands on Death by Player",
							"",
							"Commands that are run",
							"when the Boss was killed",
							"by player(s). All players",
							"registered in direct inv",
							"drops will get this command",
							"executed with their permissions.");
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == 9 + 2)
				return spawnCommandsButton.getItem();

			if (slot == 9 + 4)
				return deathCommandsButton.getItem();

			if (slot == 9 + 6)
				return deathByPlayerCommandsButton.getItem();

			return null;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Configure what commands",
					"should be run upon various",
					"happenings to your Boss."
			};
		}
	}

	public enum CommandType {
		SPAWN,
		DEATH,
		DEATH_BY_PLAYER;
	}

	private final class MenuCommands extends MenuPagged<Entry<String, Double>> {

		private final CommandType commandType;

		private final Button addButton;

		protected MenuCommands(final Menu parent, final CommandType type) {
			super(9 * 2, parent, (type == CommandType.SPAWN ? getBoss().getSpawnCommands() : type == CommandType.DEATH ? getBoss().getDeathCommands() : getBoss().getDeathByPlayerCommands()).entrySet());

			this.commandType = type;

			setTitle((type == CommandType.DEATH ? "Death" : type == CommandType.DEATH_BY_PLAYER ? "Death by Players" : "Spawn") + " Commands");

			this.addButton = new MenuButtonConvo(this, new BossConversation.PromptCommand(getBoss(), type), ItemCreator.of(
					CompMaterial.EMERALD,
					"&aAdd new",
					"",
					"Click to add a",
					"new command for",
					"your Boss."));
		}

		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == getSize() - 5)
				return addButton.getItem();

			return super.getItemAt(slot);
		}

		@Override
		protected ItemStack convertToItemStack(final Entry<String, Double> e) {
			final String command = e.getKey();
			// display up to one decimal place, if this has thousanths-chance set
			String chance = String.format("%.1f", e.getValue() * 100);
			if (chance.endsWith(".0"))
				chance = String.format("%d", (int) (e.getValue() * 100));

			final List<String> lore = new ArrayList<>();

			{
				lore.add("");
				lore.add("Chance to run: " + chance + "%");
				lore.add("Runs the command: ");

				boolean first = true;
				for (String commandLine : command.split("(?<=\\G.{40})")) {
					if (first) {
						final boolean magicCommand = commandLine.startsWith("say") || commandLine.startsWith("broadcast") || commandLine.startsWith("tell");

						if (magicCommand) {
							final String[] split = commandLine.split(" ");

							commandLine = " &d" + split[0] + "&7 " + Common.joinRange(1, split);
						} else
							commandLine = " /" + commandLine;

					}

					lore.add((first ? " -" : " ") + "&7" + commandLine);
					first = false;
				}

				lore.add("");
				lore.add("&2&l< &8Left click to edit run chance");
				lore.add("&2&l> &8Right click to delete");

			}

			return ItemCreator.of(CompMaterial.BOOK)
					.name("Command")
					.lores(lore)
					.glow(true)
					.build().make();
		}

		@Override
		protected void onPageClick(final Player pl, final Entry<String, Double> e, final ClickType click) {
			final String command = e.getKey();

			if (click == ClickType.RIGHT) {
				animateTitle("&4Command has been removed!");

				final AutoUpdateMap<String, Double> commands = commandType == CommandType.DEATH ? boss.getDeathCommands() : commandType == CommandType.DEATH_BY_PLAYER ? boss.getDeathByPlayerCommands() : boss.getSpawnCommands();
				commands.removeAndUpdate(command);

				newInstance().displayTo(pl);

			} else
				BossConversation.PromptCommandChance.show(this, getBoss(), command, commandType, pl);
		}

		@Override
		protected boolean updateButtonOnClick() {
			return false;
		}

		@Override
		public Menu newInstance() {
			return new MenuCommands(getParent(), commandType);
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Commands that your Boss",
					"executes on his " + (commandType == CommandType.DEATH ? "death" : commandType == CommandType.SPAWN ? "spawn" : "death by players") + ".",
					commandType == CommandType.DEATH_BY_PLAYER ? "&6Requires direct inventory drops." : ""
			};
		}
	}

	// ----------------------------------------------------------------------------------------------
	// Helper classes
	// ----------------------------------------------------------------------------------------------

	private abstract class MenuEditChances extends Menu implements MenuQuantitable {

		@Getter
		protected final EditMode mode;

		@Setter
		@Getter
		private MenuQuantity quantity = MenuQuantity.ONE;

		private final Button modeButton;
		private final Button quantityButton;

		private MenuEditChances(final EditMode mode, final int size) {
			this(MenuBossIndividual.this, mode, size);
		}

		private MenuEditChances(final Menu parent, final EditMode mode, final int size) {
			super(parent);

			this.mode = mode;
			setSize(size);

			modeButton = new Button() {

				@Override
				public final void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
					pl.closeInventory();

					getNextMenu().displayTo(pl);
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator
							.of(mode == EditMode.ITEMS ? CompMaterial.CHEST : CompMaterial.GOLD_NUGGET,
									"Editing " + ItemUtil.bountify(mode),
									"",
									"&7Click to edit " + ItemUtil.bountify(mode.next()))
							.build().make();
				}
			};

			quantityButton = mode == EditMode.DROP_CHANCES ? getEditQuantityButton(this) : Button.makeEmpty();
		}

		@Override
		public ItemStack getItemAt(final int slot) {
			if (slot == getSize() - 5)
				return modeButton.getItem();

			if (slot == getSize() - 7 && !quantityButton.equals(Button.makeEmpty()))
				return quantityButton.getItem();

			if (slot > getSize() - 10)
				return getFillerItem();

			return null;
		}

		protected final ItemStack paintChanceItem(final BossDrop drop) {
			return drop != null && drop.getItem() != null ? mode == EditMode.DROP_CHANCES ? ItemCreator
					.of(drop.getItem())
					.lores(Arrays.asList(
							"",
							"&7Drop chance: &6" + Math.round(100 * Double.valueOf(drop.getDropChance())) + "%",
							"",
							"   &8(Mouse click)",
							"  &7&l< &4-{q}%    &2+{q}% &7&l>".replace("{q}", quantity.getAmount() + "")))
					.hideTags(true)
					.build().make()
					: drop.getItem() : null;
		}

		@Override
		protected final String[] getInfo() {
			return mode == EditMode.ITEMS ? new String[] {
					"Here you edit the items",
					"for this Boss.",
					"",
					"Simply &2drag and drop &7items",
					"from your inventory here."
			}
					: new String[] {
							"Here you edit how likely it",
							"is that each item will drop,",
							"when the Boss dies.",
							"",
							"&2Right or left click &7on items",
							"to adjust the drop chances."
					};
		}

		@Override
		public boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor) {
			if (mode == EditMode.DROP_CHANCES)
				return false;

			return slot < getSize() - 9 || location == MenuClickLocation.PLAYER_INVENTORY;
		}

		private Menu getNextMenu() {
			try {
				final Constructor<? extends MenuEditChances> c = getClass().getDeclaredConstructor(MenuBossIndividual.class, Menu.class, EditMode.class);
				c.setAccessible(true);

				return c.newInstance(MenuBossIndividual.this, getParent(), mode.next());
			} catch (final ReflectiveOperationException ex) {
				Common.throwError(ex, "Error generating next menu " + this);
			}

			return null;
		}
	}

	public static final class MenuButtonConvo extends Button {

		private final BossConversation convo;
		@Getter
		private final ItemStack item;

		public MenuButtonConvo(final Menu parent, final BossPrompt prompt, final ItemCreator.ItemCreatorBuilder item) {
			convo = new BossConversation(parent, prompt);
			this.item = item.hideTags(true).build().make();
		}

		public MenuButtonConvo(final Boss boss, final Menu parent, final Class<? extends BossPrompt> prompt, final ItemCreator.ItemCreatorBuilder item) {
			try {
				convo = new BossConversation(parent, prompt.getDeclaredConstructor(Boss.class).newInstance(boss));
			} catch (final ReflectiveOperationException ex) {
				throw new FoException(ex, "Error starting conversation with prompt " + prompt);
			}

			this.item = item.hideTags(true).build().make();
		}

		@Override
		public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
			convo.start(pl);
		}
	}

	enum EditMode {
		ITEMS,

		DROP_CHANCES;

		public EditMode next() {
			final int next = ordinal() + 1;
			final EditMode[] values = EditMode.values();

			return next >= values.length ? values[0] : values[next];
		}
	}
}