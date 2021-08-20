package org.mineacademy.boss.menu;

import java.util.stream.Collectors;

import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.model.BossConversation;
import org.mineacademy.boss.storage.SimpleSpawnerData;
import org.mineacademy.boss.tool.SpawnerTool;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPagged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;

public final class MenuSpawner extends Menu {

	private final CreatureSpawner spawner;

	private final Button bossesButton;
	private final Button delayButton;
	private final Button rangeButton;
	private final Button limitsButton;

	private final Button oldVersionButton;

	public MenuSpawner(CreatureSpawner spawner) {
		super(null);

		this.spawner = spawner;

		setSize(9 * 3);
		setTitle("Spawner Menu");

		bossesButton = new ButtonMenu(new MenuBosses(),
				ItemCreator.of(
						CompMaterial.ZOMBIE_SPAWN_EGG,
						"Select boss",
						"",
						"Choose which Boss will",
						"spawn from this spawner.")
						.glow(true));

		delayButton = MinecraftVersion.olderThan(V.v1_12) ? Button.makeEmpty()
				: new MenuBossIndividual.MenuButtonConvo(this, new BossConversation.PromptSpawnDelay(getCurrentBoss(), spawner), ItemCreator.of(
						CompMaterial.CLOCK,
						"Spawning period",
						"",
						"&fCurrent: &7" + spawner.getMinSpawnDelay() + " ticks",
						"",
						"Click to set the spawn period",
						"in ticks (20 ticks = 1 second).",
						"The default is 4 ticks."));

		rangeButton = MinecraftVersion.olderThan(V.v1_12) ? Button.makeEmpty()
				: new MenuBossIndividual.MenuButtonConvo(this, new BossConversation.PromptSpawnRange(getCurrentBoss(), spawner), ItemCreator.of(
						CompMaterial.TORCH,
						"Spawning range",
						"",
						"&fCurrent: &7" + spawner.getSpawnRange() + " blocks",
						"",
						"Click to set the spawn range",
						"in blocks (it is 2 blocks high,",
						"centered on spawner's y- coordinate)"));

		limitsButton = MinecraftVersion.olderThan(V.v1_12) ? Button.makeEmpty()
				: new MenuBossIndividual.MenuButtonConvo(this, new BossConversation.PromptSpawnLimit(getCurrentBoss(), spawner), ItemCreator.of(
						CompMaterial.SPAWNER,
						"Spawning limit",
						"",
						"&fCurrent: &7" + Common.pluralEs(spawner.getMaxNearbyEntities(), "Boss"),
						"",
						"Click to set how many similar",
						"entities can be within the",
						"spawner's spawning range"));

		oldVersionButton = Button.makeDummy(ItemCreator.of(CompMaterial.BUCKET,
				"Outdated Minecraft!",
				"",
				"Unfortunately many spawner",
				"options such as range or",
				"delay require Minecraft 1.12",
				"or newer to be available."));
	}

	@Override
	public ItemStack getItemAt(int slot) {

		if (slot == 9 + 1)
			return bossesButton.getItem();

		if (MinecraftVersion.atLeast(V.v1_12)) {
			if (slot == 9 + 3)
				return delayButton.getItem();

			if (slot == 9 + 5)
				return rangeButton.getItem();

			if (slot == 9 + 7)
				return limitsButton.getItem();

		} else if (slot == 9 + 7)
			return oldVersionButton.getItem();

		return null;
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Configure this Boss spawner",
				"to spawn a custom boss at",
				"different times you need to."
		};
	}

	@Override
	public Menu newInstance() {
		return new MenuSpawner(spawner);
	}

	@Override
	protected boolean addReturnButton() {
		return false;
	}

	private Boss getCurrentBoss() {
		Valid.checkBoolean(spawner.hasMetadata(SpawnerTool.METADATA), "Boss spawner at " + Common.shortLocation(spawner.getLocation()) + " lacks Boss metadata!");

		final String bossName = spawner.getMetadata(SpawnerTool.METADATA).get(0).asString();

		if (SpawnerTool.METADATA_UNSPECIFIED.equals(bossName))
			return null;

		final Boss boss = BossPlugin.getBossManager().findBoss(bossName);
		Valid.checkNotNull(boss, "Boss spawner at " + Common.shortLocation(spawner.getLocation()) + " spawns a non-existing Boss '" + bossName + "'!");

		return boss;
	}

	public class MenuBosses extends MenuPagged<Boss> {

		protected MenuBosses() {
			super(9 * 3, MenuSpawner.this, BossPlugin.getBossManager().getBosses().stream().filter(boss -> boss.getType() != EntityType.ENDER_DRAGON).collect(Collectors.toList()));

			setTitle("Select Boss to spawn");
		}

		@Override
		protected ItemStack convertToItemStack(Boss boss) {
			final Boss curr = getCurrentBoss();
			final boolean isCurr = curr != null && boss.getName().equals(curr.getName());

			return ItemCreator
					.of(boss.asEgg())
					.name("Boss " + boss.getName())
					.lore("")
					.lore(isCurr ? "Boss selected, click to remove." : "Click to select this Boss.")
					.glow(isCurr)
					.build().make();
		}

		@Override
		protected void onPageClick(Player pl, Boss boss, ClickType click) {
			final boolean removed = boss.equals(getCurrentBoss());

			spawner.setMetadata(SpawnerTool.METADATA, new FixedMetadataValue(SimplePlugin.getInstance(), removed ? SpawnerTool.METADATA_UNSPECIFIED : boss.getName()));
			spawner.setSpawnedType(removed ? SpawnerTool.getUnspecifiedType() : boss.getType());

			spawner.update(true);

			SimpleSpawnerData.$().addSpawner(spawner.getLocation(), removed ? SpawnerTool.METADATA_UNSPECIFIED : boss.getName());
			restartMenu(!removed ? "&2Boss has been selected!" : "&4Boss removed, spawning paused!");
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select which one Boss should",
					"spawn from this spawner"
			};
		}
	}
}
