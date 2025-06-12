package org.mineacademy.boss.spawn;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.MenuQuantitable;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonConversation;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuQuantity;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.remain.CompBiome;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;
import lombok.Setter;

/**
 * Spawn rule to spawn at date ranges.
 */
@Getter
abstract class SpawnRuleRandom extends SpawnRuleRegions {

	/**
	 * The height where Boss should appear?
	 */
	private RangedValue height;

	/**
	 * What worlds should the Boss appear at?
	 */
	private Map<String, Double> worldsWithChances;

	/**
	 * What biomes permit Boss to spawn?
	 */
	private Map<CompBiome, Double> biomesWithChances;

	/**
	 * Create new spawn rule by name
	 *
	 * @param name
	 * @param type
	 */
	public SpawnRuleRandom(String name, SpawnRuleType type) {
		super(name, type);
	}

	/* ------------------------------------------------------------------------------- */
	/* Data */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRule#onLoad()
	 */
	@Override
	protected void onLoad() {
		super.onLoad();

		this.height = this.get("Height", RangedValue.class);
		this.worldsWithChances = this.getMap("Worlds", String.class, Double.class);
		this.biomesWithChances = this.getMap("Biomes", CompBiome.class, Double.class);

		if (this.worldsWithChances.isEmpty())
			for (final World world : Bukkit.getWorlds())
				this.worldsWithChances.put(world.getName(), 1.0D);

		if (this.biomesWithChances.isEmpty())
			for (final CompBiome biome : CompBiome.getAvailable())
				this.biomesWithChances.put(biome, 1.0D);
	}

	@Override
	public void onSave() {
		this.set("Height", this.height);
		this.set("Worlds", this.worldsWithChances);
		this.set("Biomes", this.biomesWithChances);

		super.onSave();
	}

	/* ------------------------------------------------------------------------------- */
	/* Spawning */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRule#canRun(org.bukkit.Location)
	 */
	@Override
	protected boolean canRun(Location location) {
		final double height = location.getY();
		final String worldName = location.getWorld().getName();
		final CompBiome biome = CompBiome.fromBlock(location.getBlock());

		if (this.getHeight() != null && !this.getHeight().isInRangeDouble(height)) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to location height (" + height + ") out of limit: " + this.getHeight());

			return false;
		}

		if (!RandomUtil.chanceD(this.worldsWithChances.getOrDefault(worldName, 0D))) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to world chance (" + (this.worldsWithChances.getOrDefault(worldName, 0D) * 100) + ") did not pass");

			return false;
		}

		// Ignore custom biomes for now
		if (biome != null)
			if (!RandomUtil.chanceD(this.biomesWithChances.getOrDefault(biome, 0D))) {
				Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to biome chance (" + (this.biomesWithChances.getOrDefault(biome, 0D) * 100) + "% in " + biome + ") did not pass");

				return false;
			}

		return super.canRun(location);
	}

	/* ------------------------------------------------------------------------------- */
	/* Menu system */
	/* ------------------------------------------------------------------------------- */

	@Override
	public List<Button> getButtons(Menu parent) {
		final List<Button> buttons = super.getButtons(parent);

		buttons.add(new ButtonConversation(new HeightPrompt(),
				CompMaterial.LADDER,
				"Height",
				"",
				"Current: &f" + (this.height == null ? "&aany" : this.height.toString()),
				"",
				"Click to set the world height",
				"Bosses can spawn at or between."));

		buttons.add(new ButtonMenu(new WorldsMenu(parent),
				CompMaterial.GRASS_BLOCK,
				"Worlds",
				"",
				"Click to choose worlds",
				"where Bosses will spawn."));

		buttons.add(new ButtonMenu(new BiomesMenu(parent),
				CompMaterial.LILY_PAD,
				"Biomes",
				"",
				"Click to choose biomes",
				"where Bosses will spawn."));

		return buttons;
	}

	private class HeightPrompt extends SimpleStringPrompt {

		HeightPrompt() {
			super("Enter the world height, such as 50, or a range such 40-100 as that means between y=40 and y=100, or type -1 to spawn at any height. Current: " + (SpawnRuleRandom.this.height == null ? "any" : SpawnRuleRandom.this.height.toLine()));
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if ("-1".equals(input))
				return true;

			try {
				final RangedValue val = RangedValue.fromString(input);

				return val.getMinLong() >= (MinecraftVersion.atLeast(V.v1_17) ? -127 : 0) && val.getMaxLong() <= 255 && val.getMinLong() <= val.getMaxLong();

			} catch (final Throwable t) {
			}

			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Invalid height: '" + invalidInput + "' Type either a number between 0-255 to spawn Bosses only at the given heigh,"
					+ " or a range like '40-100' to spawn between y=40 and y=100, or type -1 for any height.";
		}

		@Override
		protected String getMenuAnimatedTitle() {
			return "Set height to " + (SpawnRuleRandom.this.height == null ? "any" : SpawnRuleRandom.this.height);
		}

		@Override
		protected void onValidatedInput(ConversationContext context, String input) {
			SpawnRuleRandom.this.setHeight("-1".equals(input) ? null : RangedValue.fromString(input));
		}
	}

	private class WorldsMenu extends MenuPaged<World> implements MenuQuantitable {

		@Getter
		@Setter
		private MenuQuantity quantity = MenuQuantity.ONE;

		WorldsMenu(Menu parent) {
			super(parent, Bukkit.getWorlds(), true);
		}

		@Override
		protected ItemStack convertToItemStack(World world) {
			final Environment env = world.getEnvironment();
			final CompMaterial material = env == Environment.NETHER ? CompMaterial.NETHER_BRICK : env == Environment.THE_END ? CompMaterial.END_STONE : CompMaterial.GRASS_BLOCK;
			final String worldName = world.getName();

			return this.addLevelToItem(ItemCreator.from(
					material,
					(worldName.equals("world") ? "Main" : ChatUtil.capitalizeFully(worldName)) + " World")
					.make(),
					MathUtil.formatTwoDigits(SpawnRuleRandom.this.worldsWithChances.getOrDefault(worldName, 0D) * 100) + "%");
		}

		@Override
		public String getLevelLoreLabel() {
			return "Chance";
		}

		@Override
		public boolean allowDecimalQuantities() {
			return true;
		}

		@Override
		protected void onPageClick(Player player, World world, ClickType click) {
			final String worldName = world.getName();
			final double oldChance = SpawnRuleRandom.this.worldsWithChances.getOrDefault(worldName, 0D);
			final double newChance = MathUtil.range(oldChance + this.getNextQuantityDouble(click), 0, 1D);

			SpawnRuleRandom.this.worldsWithChances.put(worldName, newChance);
			SpawnRuleRandom.this.save();

			this.restartMenu("&9Spawn chance set to " + MathUtil.formatTwoDigits(newChance * 100) + "%!");
		}
	}

	private class BiomesMenu extends MenuPaged<CompBiome> implements MenuQuantitable {

		@Getter
		@Setter
		private MenuQuantity quantity = MenuQuantity.ONE;

		@Position(start = StartPosition.BOTTOM_LEFT)
		private final Button disableAllButton;

		@Position(start = StartPosition.BOTTOM_LEFT, value = +1)
		private final Button enableAllButton;

		BiomesMenu(Menu parent) {
			super(parent, CompBiome.getAvailable(), true);

			this.disableAllButton = Button.makeSimple(CompMaterial.LAVA_BUCKET,
					"Disable All",
					"Click to set chances to\n"
							+ "0% in all biomes.",
					player -> {
						for (final CompBiome biome : CompBiome.getAvailable())
							SpawnRuleRandom.this.biomesWithChances.put(biome, 0D);

						SpawnRuleRandom.this.save();

						BiomesMenu.this.restartMenu("&4All biomes disabled!");
					});

			this.enableAllButton = Button.makeSimple(CompMaterial.WATER_BUCKET,
					"Enable All",
					"Click to set chances to\n"
							+ "0% in all biomes.",
					player -> {
						for (final CompBiome biome : CompBiome.getAvailable())
							SpawnRuleRandom.this.biomesWithChances.put(biome, 1D);

						SpawnRuleRandom.this.save();

						BiomesMenu.this.restartMenu("&4All biomes enabled!");
					});
		}

		@Override
		public Menu newInstance() {
			return new BiomesMenu(this.getParent());
		}

		@Override
		protected ItemStack convertToItemStack(CompBiome biome) {
			return this.addLevelToItem(ItemCreator.from(
					this.resolveBiomeIcon(biome),
					ChatUtil.capitalizeFully(biome.toString()))
					.glow(SpawnRuleRandom.this.biomesWithChances.getOrDefault(biome, 0D) < 1D)
					.make(),
					MathUtil.formatTwoDigits(SpawnRuleRandom.this.biomesWithChances.getOrDefault(biome, 0D) * 100) + "%");
		}

		private CompMaterial resolveBiomeIcon(final CompBiome biome) {
			final String biomeName = biome.toString();

			if (biomeName.contains("BEACH"))
				return CompMaterial.SANDSTONE;

			if (biomeName.contains("BIRCH"))
				return CompMaterial.SPRUCE_SAPLING;

			if (biomeName.contains("DESERT"))
				return CompMaterial.SAND;

			if (biomeName.contains("FOREST"))
				return CompMaterial.OAK_SAPLING;

			if (biomeName.contains("FROZEN") || biomeName.contains("ICE"))
				return CompMaterial.ICE;

			if (biomeName.contains("SNOW"))
				return CompMaterial.SNOW_BLOCK;

			if (biomeName.contains("JUNGLE"))
				return CompMaterial.JUNGLE_SAPLING;

			if (biomeName.contains("MESA"))
				return CompMaterial.RED_SAND;

			if (biomeName.contains("MUSHROOM"))
				return CompMaterial.BROWN_MUSHROOM_BLOCK;

			if (biomeName.contains("OCEAN") || biomeName.contains("RIVER"))
				return CompMaterial.WATER_BUCKET;

			if (biomeName.contains("SAVANNA"))
				return CompMaterial.ACACIA_SAPLING;

			if (biomeName.contains("SKY") || biomeName.contains("VOID"))
				return CompMaterial.GRAY_WOOL;

			if (biomeName.contains("TAIGA"))
				return CompMaterial.DARK_OAK_SAPLING;

			if (biomeName.contains("SWAMP"))
				return CompMaterial.LILY_PAD;

			if (biomeName.contains("PLAINS") || biomeName.contains("MOUNTAIN"))
				return CompMaterial.FERN;

			if (biomeName.contains("HILLS"))
				return CompMaterial.STONE;

			if (biomeName.equals("HELL") || biomeName.equals("NETHER"))
				return CompMaterial.NETHERRACK;

			if (biomeName.equals("THE_END") || biomeName.equals("END_BARRENS"))
				return CompMaterial.END_STONE_BRICKS;

			if (biomeName.contains("LANDS"))
				return CompMaterial.GRASS_BLOCK;

			if (biomeName.equals("STONE_SHORE"))
				return CompMaterial.STONE;

			return CompMaterial.STONE;
		}

		@Override
		public String getLevelLoreLabel() {
			return "Chance";
		}

		@Override
		public boolean allowDecimalQuantities() {
			return true;
		}

		@Override
		protected void onPageClick(Player player, CompBiome biome, ClickType click) {
			final double oldChance = SpawnRuleRandom.this.biomesWithChances.getOrDefault(biome, 0D);
			final double newChance = MathUtil.range(oldChance + this.getNextQuantityDouble(click), 0, 1D);

			SpawnRuleRandom.this.biomesWithChances.put(biome, newChance);
			SpawnRuleRandom.this.save();

			this.restartMenu("&9Spawn chance set to " + MathUtil.formatTwoDigits(newChance * 100) + "%!");
		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Settings */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @param height the height to set
	 */
	public final void setHeight(RangedValue height) {
		this.height = height;

		this.save();
	}

	/**
	 * @param worldsWithChances the worldsWithChances to set
	 */
	public final void setWorldsWithChances(Map<String, Double> worldsWithChances) {
		this.worldsWithChances = worldsWithChances;

		this.save();
	}

	/**
	 * @param biomesWithChances the biomesWithChances to set
	 */
	public final void setBiomesWithChances(Map<CompBiome, Double> biomesWithChances) {
		this.biomesWithChances = biomesWithChances;

		this.save();
	}
}
