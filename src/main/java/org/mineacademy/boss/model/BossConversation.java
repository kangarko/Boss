package org.mineacademy.boss.model;

import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossEggItem;
import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.boss.command.NewCommand;
import org.mineacademy.boss.menu.MenuBossIndividual;
import org.mineacademy.boss.menu.MenuBossIndividual.CommandType;
import org.mineacademy.boss.menu.MenuMain;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.util.AutoUpdateMap;
import org.mineacademy.boss.util.BossUpdateUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.conversation.SimpleConversation;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.RangedRandomValue;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AllArgsConstructor;
import lombok.Getter;

public final class BossConversation extends SimpleConversation {

	private final Prompt prompt;

	public BossConversation(Menu menuToReturnTo, Prompt prompt) {
		super(menuToReturnTo);

		this.prompt = prompt;
	}

	@Override
	protected Prompt getFirstPrompt() {
		return prompt;
	}

	public static class PromptName extends BossPrompt {

		private final EntityType mob;

		private PromptName(EntityType mob) {
			super(null);

			this.mob = mob;
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Please enter the Boss' name.";
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return NewCommand.mayCreate(context.getForWhom(), input);
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext c, String input) {
			final Boss boss = BossPlugin.getBossManager().createBoss(mob, input);

			if (boss != null)
				c.setSessionData("Boss", boss);

			return Prompt.END_OF_CONVERSATION;
		}

		@Override
		public void onConversationEnd(SimpleConversation conversation, ConversationAbandonedEvent event) {
			conversation.setMenuToReturnTo(null);

			final Player player = (Player) event.getContext().getForWhom();
			final Object boss = event.getContext().getSessionData("Boss");

			if (boss != null && boss instanceof Boss)
				new MenuBossIndividual((Boss) boss).displayTo(player);

			else {
				new MenuMain().displayTo(player);

				tell(player, "&cBoss creation has been abandoned.");
			}
		}

		public static void show(EntityType type, Player pl) {
			final Prompt namePrompt = new PromptName(type);

			new BossConversation(null, namePrompt).start(pl);
		}
	}

	public static final class PromptCustomName extends BossPrompt {

		public PromptCustomName(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			final String curr = getBoss().getSettings().getCustomName();

			return "Enter the name displayed above the Boss. Type 'default' to reset, 'none' to hide. Current name: &f" + (curr == null ? getBoss().getName() : curr);
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return input.length() < 51;
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			getBoss().getSettings().setCustomName("none".equals(input) ? null : "default".equals(input) ? getBoss().getName() : input);

			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptHealth extends BossNumericPrompt {

		public PromptHealth(Boss boss) {
			super(boss, false);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Please enter the Boss' health (0 to " + Remain.getMaxHealth() + ").";
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return super.isInputValid(context, input) && Integer.parseInt(input) <= Remain.getMaxHealth();
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "The health must be a whole number from 0 to " + Remain.getMaxHealth() + "!";
		}

		@Override
		protected void acceptValidatedInputInt(ConversationContext context, int input) {
			getBoss().getSettings().setHealth(input);
		}

		public static void show(MenuBossIndividual menu, Player pl) {
			new BossConversation(menu, new PromptHealth(menu.getBoss())).start(pl);
		}
	}

	public static final class PromptDroppedExp extends BossPrompt {

		public PromptDroppedExp(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter how much exp should this Boss drop on death.\n" +
					"a) 1 - 10 will drop a random exp from 1 to 10\n" +
					"b) 20 will always drop 20 experience points";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "Malformed syntax: " + invalidInput);

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			try {
				RangedRandomValue.parse(input);

				return true;
			} catch (final Throwable ex) {
				return false;
			}
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			getBoss().getSettings().setDroppedExp(RangedRandomValue.parse(input));

			tellLater(1, context.getForWhom(), "&7The dropped exp has been &2set &7to " + input + "!");

			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptAttribute extends BossPrompt {

		private final BossAttribute attribute;

		public PromptAttribute(BossAttribute attribute, Boss boss) {
			super(boss);

			this.attribute = attribute;
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter the value of the '" + ItemUtil.bountifyCapitalized(attribute) + "' attribute, or type 'default' to remove.";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "The attribute must be a number of zero or greater!");

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return "default".equals(input.toLowerCase()) || NumberUtils.isNumber(input) && Double.parseDouble(input) >= 0;
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			final double value = "default".equals(input.toLowerCase()) ? getBoss().getAttributes().getDefaultBase(attribute) : Double.parseDouble(input);
			getBoss().getAttributes().set(attribute, value);

			tellLater(1, context.getForWhom(), "&7The attribute has been &2set &7to " + MathUtil.formatFiveDigits(value) + "!");

			return Prompt.END_OF_CONVERSATION;
		}

		public static void show(BossAttribute attribute, Boss boss, Menu menu, Player pl) {
			new BossConversation(menu, new PromptAttribute(attribute, boss)).start(pl);
		}
	}

	public static final class PromptTime extends BossPrompt {

		private final StrictMap<String, RangedValue> wrapped = new StrictMap<>();

		public PromptTime(Boss boss) {
			super(boss);

			fillKnown();
		}

		private void fillKnown() {
			wrapped.put("dawn", new RangedValue(0, 1000));
			wrapped.put("day", new RangedValue(1000, 12000));
			wrapped.put("dusk", new RangedValue(12000, 13000));
			wrapped.put("night", new RangedValue(13000, 24000));
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter when during the day the Boss spawns. Example:\n" +
					" a) Possible values: dawn, day, dusk, night\n" +
					" b) or enter a custom range:\n" +
					"   &8* &70 - 24000 (always spawns)\n" +
					"   &8* &70 - 6000 (spawn till mid-day)\n" +
					"   &8* &7<min> - <max> (spawns in the range)";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "Malformed syntax: " + invalidInput);

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if (wrapped.contains(input.toLowerCase()))
				return true;

			try {
				final RangedValue value = RangedValue.parse(input);

				return value.getMin().intValue() >= 0 && value.getMax().intValue() <= 24000;

			} catch (final Throwable ex) {
				return false;
			}
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			getBoss().getSpawning().getConditions().setTime(wrapped.contains(input.toLowerCase()) ? wrapped.get(input) : RangedValue.parse(input));

			tellLater(1, context.getForWhom(), "&7The spawning time has been &2set &7to " + input + "!");
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptLight extends BossPrompt {

		private final StrictMap<String, RangedValue> wrapped = new StrictMap<>();

		public PromptLight(Boss boss) {
			super(boss);

			fillKnown();
		}

		private void fillKnown() {
			wrapped.put("dark", new RangedValue(0, 1000));
			wrapped.put("bright", new RangedValue(1000, 12000));
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter the light level required for the Boss to spawn.\n" +
					" a) Possible values: dark, bright\n" +
					" b) or enter a custom range:\n" +
					"   &8* &70 - 15 (always spawns)\n" +
					"   &8* &78 - 15 (only spawns spawn in bright environments)\n" +
					"   &8* &7<min> - <max> (spawns in the range)";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "Malformed syntax: " + invalidInput);

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if (wrapped.contains(input.toLowerCase()))
				return true;

			try {
				final RangedValue value = RangedValue.parse(input);

				return value.getMin().intValue() >= 0 && value.getMax().intValue() <= 15;

			} catch (final Throwable ex) {
				return false;
			}
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			getBoss().getSpawning().getConditions().setLight(wrapped.contains(input.toLowerCase()) ? wrapped.get(input) : RangedValue.parse(input));

			tellLater(1, context.getForWhom(), "&7The spawning light has been &2set &7to " + input + "!");
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptHeight extends BossPrompt {

		public PromptHeight(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter the y-heigh required for the Boss to spawn.\n" +
					" b) Enter a custom range:\n" +
					"   &8* &70 - 256 (always spawns)\n" +
					"   &8* &70 - 64 (spawns mostly underground)\n" +
					"   &8* &764 - 256 (spawns above sea level)\n" +
					"   &8* &7<min> - <max> (spawns in the range)";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "Malformed syntax: " + invalidInput);

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			try {
				final RangedValue value = RangedValue.parse(input);

				return value.getMin().intValue() >= 0 && value.getMax().intValue() <= 256;

			} catch (final Throwable ex) {
				return false;
			}
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			getBoss().getSpawning().getConditions().setHeight(RangedValue.parse(input));

			tellLater(1, context.getForWhom(), "&7The spawning height has been &2set &7to " + input + "!");
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptRegionLimit extends BossPrompt {

		private final BossRegionSettings region;

		public PromptRegionLimit(Boss boss, BossRegionSettings region) {
			super(boss);

			this.region = region;
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter the maximum amount of Bosses that can appear naturally in region " + region.getRegionName() + ", or -1 for unlimited.\n" +
					" Currently: " + region.getLimit() + "\n";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "Must be a non-zero number or -1, but got: " + invalidInput);

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			try {
				return Integer.parseInt(input) >= -1;

			} catch (final Throwable ex) {
				return false;
			}
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			region.setLimit(Integer.parseInt(input));

			tellLater(1, context.getForWhom(), "&7Boss limit for " + region.getRegionName() + " was &2set &7to " + input + "!");
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptSoundFrom extends BossPrompt {

		public PromptSoundFrom(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "&8(&a1&8) &fEnter the sound you want to remap. Available: \n" +
					"&f" + (MinecraftVersion.newerThan(V.v1_8) ? "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html" : "https://www.spigotmc.org/wiki/cc-sounds-list/") + "\n";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "&cSound not found: " + invalidInput);

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return ReflectionUtil.lookupEnumSilent(Sound.class, input) != null;
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			final Sound sound = ReflectionUtil.lookupEnumSilent(Sound.class, input);

			if (getBoss().getSettings().getRemappedSounds().contains(sound)) {
				tellLater(1, context.getForWhom(), "&cThis sound is already remapped!");

				return this;
			}

			context.setSessionData("from", sound);
			return new PromptSoundTo(getBoss());
		}
	}

	public static final class PromptSoundTo extends BossPrompt {

		public PromptSoundTo(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "&8(&a2&8) &fEnter the new sound.\n" +
					"&fFormat: &7<sound (see below> <volume> <pitch>\n" +
					"&fExample: &7ENTITY_ENDERMEN_DEATH 1F 0.5F\n" +
					"&fAvailable: &7https://www.spigotmc.org/wiki/cc-sounds-list/\n";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "&cWrong syntax or sound not found: " + invalidInput);

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			try {
				new SimpleSound(input);
				return true;

			} catch (final Throwable t) {

				return false;
			}
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			final Sound from = (Sound) context.getSessionData("from");
			final SimpleSound to = new SimpleSound(input);

			getBoss().getSettings().getRemappedSounds().putAndUpdate(from, to);

			tellLater(1, context.getForWhom(), "&7Remapping &6" + ItemUtil.bountifyCapitalized(from) + " &7to " + ItemUtil.bountifyCapitalized(to.getSound()));
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptEggMaterial extends BossPrompt {

		public PromptEggMaterial(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter the material of the spawner egg or type \"default\" to reset. If you enter \"monster_egg\", it will be changed dynamically.\n" +
					" Currently: " + getBoss().getSettings().getEggItem().getMaterial() + "\n";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "&cInvalid material '" + invalidInput + "', see https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html for available.");

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			try {
				return input.equals("default") || Material.valueOf(input) != null;

			} catch (final Throwable ex) {
				return false;
			}
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			final CompMaterial mat = input.equals("default") ? CompMaterial.SHEEP_SPAWN_EGG : CompMaterial.fromStringStrict(input);

			getBoss().getSettings().getEggItem().setMaterial(mat);

			tellLater(1, context.getForWhom(), "&7Changed egg material to &6" + ItemUtil.bountifyCapitalized(mat) + "&7.");
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptEggData extends BossNumericPrompt {

		public PromptEggData(Boss boss) {
			super(boss, false);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter the dava value of the spawner egg material or type \"default\" to reset.\n" +
					" Currently: " + getBoss().getSettings().getEggItem().getMaterial().getData() + "\n";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "&cInvalid data value '" + invalidInput + "', enter whole numbers only. For available values see https://www.minecraftinfo.com/idlist.htm");

			return null;
		}

		@Override
		protected void acceptValidatedInputInt(ConversationContext context, int data) {
			final BossEggItem egg = getBoss().getSettings().getEggItem();

			final CompMaterial mat = CompMaterial.fromLegacy(egg.getMaterial().toString(), data);
			egg.setMaterial(mat);

			tellLater(1, context.getForWhom(), "&7Changed egg data value to &6" + data + "&7.");
		}
	}

	public static final class PromptEggName extends BossPrompt {

		public PromptEggName(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter title of the spawner egg, or type 'default' to use the one in settings.yml" +
					" Currently: " + getBoss().getSettings().getEggItem().getName() + "\n";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return true;
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			final boolean def = input.equals("default");
			final String name = def ? Settings.EggSpawning.Item.NAME : input;

			getBoss().getSettings().getEggItem().setName("&r" + name);

			tellLater(1, context.getForWhom(), "&7" + (def ? "Reset" : "Changed") + " egg name to &6" + name + "&7.");
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptEggLore extends BossPrompt {

		public PromptEggLore(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter lore of the spawner egg, divided by |, or type 'default' to use the one in settings.yml" +
					" Currently: " + getBoss().getSettings().getEggItem().getLore() + "\n";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return true;
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			final boolean def = input.equals("default");
			final List<String> lore = def ? Settings.EggSpawning.Item.LORE : java.util.Arrays.asList(input.split("\\|"));

			getBoss().getSettings().getEggItem().setLore(lore);

			tellLater(1, context.getForWhom(), "&7" + (def ? "Reset" : "Changed") + " lore to &6" + lore + "&7.");
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptInvDropPlayerLimit extends BossPrompt {

		public PromptInvDropPlayerLimit(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter how many players who did the last damage to the Boss should get his drops. Current: " + Common.plural(getBoss().getSettings().getInventoryDropsTimeLimit(), "second") + ".";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "The value must be a non-zero number up to 300.");

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			try {
				final int limit = Integer.parseInt(input);

				return limit > 0 && limit <= 300;

			} catch (final Throwable ex) {
				return false;
			}
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			final int limit = Integer.parseInt(input);

			getBoss().getSettings().setInventoryDropsPlayerLimit(limit);
			tellLater(1, context.getForWhom(), "&7The inventory drop player limit is now " + Common.plural(limit, "player") + "!");
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptInvDropTimeLimit extends BossPrompt {

		public PromptInvDropTimeLimit(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter how many seconds should we register the player from his last damage in the reward list. Current: " + Common.plural(getBoss().getSettings().getInventoryDropsTimeLimit(), "second") + ".";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "The value must be a non-zero number up to 1800.");

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			try {
				final int limit = Integer.parseInt(input);

				return limit > 0 && limit <= 1800;

			} catch (final Throwable ex) {
				return false;
			}
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			final int limit = Integer.parseInt(input);

			getBoss().getSettings().setInventoryDropsTimeLimit(limit);
			tellLater(1, context.getForWhom(), "&7The inventory drop time limit is now " + Common.plural(limit, "second") + "!");
			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptCommand extends BossPrompt {

		private final CommandType commandType;

		public PromptCommand(Boss boss, MenuBossIndividual.CommandType type) {
			super(boss);

			this.commandType = type;
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter a command (without /) that will be run automatically when this Boss " + (commandType == CommandType.DEATH ? "dies" : "spawns") + ", or 'exit' to stop." +
					"\n&81) &7You can use variables &f" + (commandType == CommandType.DEATH_BY_PLAYER ? "{player}" : "{killer} &7(if applicable)") + "&7, &f{location} &7or &f{boss}" +
					"\n&82) &7To send a message to the killer, just type: &ftell <message>" +
					"\n&83) &7To broadcast a message, just type: &fbroadcast <message>";
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return true;
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			final AutoUpdateMap<String, Double> commands = commandType == CommandType.DEATH ? getBoss().getDeathCommands() : commandType == CommandType.DEATH_BY_PLAYER ? getBoss().getDeathByPlayerCommands() : getBoss().getSpawnCommands();

			commands.putAndUpdate(input, 1.0);
			tellLater(1, context.getForWhom(), "&2Successfully added a new command for your Boss!");

			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptCommandChance extends BossNumericPrompt {

		final String command;
		final CommandType commandType;

		public PromptCommandChance(Boss boss, String command, CommandType commandType) {
			super(boss, true);

			this.command = command;
			this.commandType = commandType;
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Please enter how likely this command will run (from 0 to 100)?";
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if (NumberUtils.isNumber(input))
				try {
					final double number = Double.parseDouble(input);

					return number >= 0 && number <= 100;
				} catch (final NumberFormatException ex) {
				}
			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Percentage must be between 0 and 100!";
		}

		@Override
		protected void acceptValidatedInputDouble(ConversationContext context, double input) {
			final double chance = input / 100;

			(commandType == CommandType.DEATH ? getBoss().getDeathCommands() : commandType == CommandType.DEATH_BY_PLAYER ? getBoss().getDeathByPlayerCommands() : getBoss().getSpawnCommands())
					.overrideAndUpdate(command, chance);
		}

		public static void show(Menu menu, Boss boss, String command, CommandType commandType, Player pl) {
			new BossConversation(menu, new PromptCommandChance(boss, command, commandType)).start(pl);
		}
	}

	public static final class NoSpawnPermMessage extends BossPrompt {

		public NoSpawnPermMessage(Boss boss) {
			super(boss);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			String message = getBoss().getSettings().getNoSpawnPermissionMessage();

			if (message == null || "".equals(message))
				message = SimpleLocalization.NO_PERMISSION;

			return "&cEnter the message for players attempting to use Boss egg without permission, or type 'default' to reset. \n&7Current: &f" + message;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return true;
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {
			getBoss().getSettings().setNoSpawnPermissionMessage("default".equalsIgnoreCase(input) ? null : input);
			tellLater(1, context.getForWhom(), "&2Set a new no spawn permission message to: &f" + input);

			return Prompt.END_OF_CONVERSATION;
		}
	}

	public static final class PromptSpawnDelay extends BossPrompt {

		private final CreatureSpawner spawner;

		public PromptSpawnDelay(Boss boss, CreatureSpawner spawner) {
			super(boss);

			this.spawner = spawner;
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "How long in ticks, seconds or minutes the spawner will wait between attempting the next spawn? Type 'default' to reset back to 20 seconds.";
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if ("default".equals(input))
				return true;

			final String[] split = input.split(" ");

			if (split.length != 2)
				return false;

			if (!split[1].startsWith("tick") && !split[1].startsWith("second") && !split[1].startsWith("minutes"))
				return false;

			int delay;

			try {
				delay = Integer.parseInt(split[0]);

			} catch (final NumberFormatException ex) {
				return false;
			}

			return delay > 0 && delay < Integer.MAX_VALUE;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Invalid format! Valid examples: '5 seconds', '40 ticks' or '60 minutes'";
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, String input) {

			int delay;

			if ("default".equals(input))
				delay = 20 * 20;

			else {
				final String[] split = input.split(" ");
				final int rawDelay = Integer.parseInt(split[0]);
				final boolean seconds = split[1].startsWith("second");
				final boolean minutes = split[1].startsWith("minute");

				delay = rawDelay * (seconds ? 20 : minutes ? 60 * 20 : 1);
			}

			spawner.setDelay(delay);

			if (delay < spawner.getMaxSpawnDelay()) {
				spawner.setMinSpawnDelay(delay);
				spawner.setMaxSpawnDelay(delay);
			}

			spawner.setMaxSpawnDelay(delay);
			spawner.setMinSpawnDelay(delay);

			spawner.update(true);

			tellLater(1, context.getForWhom(), "&2Set a new spawner delay to &f" + delay + " ticks");

			return Prompt.END_OF_CONVERSATION;
		}

		public static void show(Menu menu, Boss boss, String command, CommandType commandType, Player pl) {
			new BossConversation(menu, new PromptCommandChance(boss, command, commandType)).start(pl);
		}
	}

	public static final class PromptConvertingChance extends BossNumericPrompt {

		public PromptConvertingChance(Boss boss) {
			super(boss, false);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter the chance from 0 to 100% (without %) a vanilla mob should convert to Boss at? The default is 100.";
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if (NumberUtils.isNumber(input))
				try {
					final double number = Double.parseDouble(input);

					return number >= 0 && number <= 100;
				} catch (final NumberFormatException ex) {
				}

			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Range must be from 1 to 100 (without %)!";
		}

		@Override
		protected void acceptValidatedInputInt(ConversationContext context, int input) {
			getBoss().getSettings().setConvertingChance(input);
		}

		public static void show(Menu menu, Boss boss, String command, CommandType commandType, Player pl) {
			new BossConversation(menu, new PromptCommandChance(boss, command, commandType)).start(pl);
		}
	}

	public static final class PromptSpawnRange extends BossNumericPrompt {
		private final CreatureSpawner spawner;

		public PromptSpawnRange(Boss boss, CreatureSpawner spawner) {
			super(boss, false);

			this.spawner = spawner;
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter how many far (in blocks) from the spawner we should spawn the Boss? Type '-1' to reset back to 4 blocks.";
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if (NumberUtils.isNumber(input))
				try {
					final double number = Double.parseDouble(input);

					return number == -1 || number > 0 && number <= 100;
				} catch (final NumberFormatException ex) {
				}

			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Range must be either -1 or 1 till 100 (blocks)!";
		}

		@Override
		protected void acceptValidatedInputInt(ConversationContext context, int input) {
			final int range = input == -1 ? 4 : input;

			spawner.setSpawnRange(range);
			spawner.update(true);
		}

		public static void show(Menu menu, Boss boss, String command, CommandType commandType, Player pl) {
			new BossConversation(menu, new PromptCommandChance(boss, command, commandType)).start(pl);
		}
	}

	public static final class PromptSpawnLimit extends BossNumericPrompt {
		private final CreatureSpawner spawner;

		public PromptSpawnLimit(Boss boss, CreatureSpawner spawner) {
			super(boss, false);

			this.spawner = spawner;
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "Enter how many entities of the same type can be within this spawner's spawning range before the spawner stops spawning more? "
					+ "&cPlease note entities will move outside of your spawn range naturally, "
					+ "so the spawner will gradually spawn more as they move. &7Type '-1' to reset back to 4 Bosses.";
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if (NumberUtils.isNumber(input))
				try {
					final double number = Double.parseDouble(input);

					return number == -1 || number > 0 && number <= 100;
				} catch (final NumberFormatException ex) {
				}

			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Range must be either -1 or 1 till 100 (entities)!";
		}

		@Override
		protected void acceptValidatedInputInt(ConversationContext context, int input) {
			final int range = input == -1 ? 4 : input;

			spawner.setMaxNearbyEntities(range);
			spawner.update(true);
		}

		public static void show(Menu menu, Boss boss, String command, CommandType commandType, Player pl) {
			new BossConversation(menu, new PromptCommandChance(boss, command, commandType)).start(pl);
		}
	}

	public static final class PromptDamageModifier extends BossNumericPrompt {

		public PromptDamageModifier(Boss boss) {
			super(boss, true);
		}

		@Override
		public String getPrompt(ConversationContext c) {
			return "&6Enter the damage multiplier for this boss. Default is &71.0&6. You can also type lower values such as &70.5 &6to make this Boss deal 50% less damage.";
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "&cRange must be a whole number such as 1.1 (110% damage) or 0.5 (50% of his original damage)!";
		}

		@Override
		protected void acceptValidatedInputDouble(ConversationContext context, double input) {
			getBoss().getSettings().setDamageMultiplier(input);

			tell(context, "&6Set the damage multiplier to: " + input);
		}
	}

	// -------------------------------------------------------------------------------------------------

	public static abstract class BossNumericPrompt extends BossPrompt {

		final boolean allowDecimal;

		public BossNumericPrompt(Boss boss, boolean allowDecimal) {
			super(boss);
			this.allowDecimal = allowDecimal;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			tellLater(1, context.getForWhom(), "The input must be a whole number!");

			return null;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			return NumberUtils.isNumber(input) && (allowDecimal ? Double.parseDouble(input) > 0 : !input.contains(".") && Integer.parseInt(input) > 0);
		}

		@Override
		protected final Prompt acceptValidatedInput(ConversationContext context, String input) {
			if (allowDecimal)
				acceptValidatedInputDouble(context, Double.parseDouble(input));
			else
				acceptValidatedInputInt(context, Integer.parseInt(input));

			return Prompt.END_OF_CONVERSATION;
		}

		protected void acceptValidatedInputInt(ConversationContext context, int input) {
		}

		protected void acceptValidatedInputDouble(ConversationContext context, double input) {
			acceptValidatedInputInt(context, (int) input);
		}
	}

	@AllArgsConstructor
	public static abstract class BossPrompt extends SimplePrompt {

		@Getter
		private final Boss boss;

		@Override
		public void onConversationEnd(SimpleConversation conversation, ConversationAbandonedEvent event) {
			if (!event.gracefulExit() && getBoss() != null)
				tell(event.getContext().getForWhom(), "&cEditing " + boss.getName() + "&c has been abandoned.");

			else
				BossUpdateUtil.updateAll();
		}
	}
}
