package org.mineacademy.boss.menu.boss;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossCommand;
import org.mineacademy.boss.model.BossCommandType;
import org.mineacademy.boss.skill.BossSkill;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonConversation;
import org.mineacademy.fo.menu.button.ButtonRemove;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

/**
 * Represents a menu where players can add/remove commands executed in various Boss
 * behaviors i.e. on death, in skills etc.
 */
public final class CommandsMenu extends MenuPaged<BossCommand> {

	private final BossCommandType type;
	private final Boss boss;
	private final Supplier<List<BossCommand>> getter;
	private final Consumer<String> setter;
	private final Consumer<BossCommand> remover;

	@Position(start = StartPosition.BOTTOM_RIGHT)
	private final Button createButton;

	private CommandsMenu(Menu parent, BossCommandType type, Boss boss, Supplier<List<BossCommand>> getter, Consumer<String> setter, Consumer<BossCommand> remover) {
		super(parent, getter.get(), true);

		this.type = type;
		this.boss = boss;
		this.getter = getter;
		this.setter = setter;
		this.remover = remover;

		this.setTitle(Lang.legacy("menu-commands-title", "type", ChatUtil.capitalize(type.getMenuLabel())));

		this.createButton = new ButtonConversation(new SimpleStringPrompt("Enter the command without /. For help, see <click:open_url:'https://docs.mineacademy.org/boss/boss-commands'>https://docs.mineacademy.org/boss/boss-commands</click>", setter::accept),
				ItemCreator.from(
						CompMaterial.EMERALD,
						Lang.legacy("menu-commands-button-create"),
						Lang.legacy("menu-commands-button-create-lore", "type", type.getMenuLabel()).split("\n")));
	}

	@Override
	protected ItemStack convertToItemStack(BossCommand item) {
		final String healthTrigger = item.getType() == BossCommandType.HEALTH_TRIGGER
				? Lang.legacy("menu-commands-health-trigger", "value", Common.getOrDefault(item.getHealthTrigger(), Lang.legacy("menu-commands-health-trigger-unset"))) + "\n"
				: "";

		return ItemCreator.from(CompMaterial.BOOK,
				Lang.legacy("menu-commands-item-name"),
				Lang.legacy("menu-commands-item-lore",
						"chance", MathUtil.formatTwoDigits(item.getChance() * 100),
						"run_as", item.isConsole() ? Lang.legacy("menu-commands-run-as-console") : Lang.legacy("menu-commands-run-as-player"),
						"health_trigger", healthTrigger,
						"command", item.getCommandFormatted()).split("\n"))
				.glow(true)
				.make();
	}

	@Override
	protected void onPageClick(Player player, BossCommand item, ClickType click) {
		new IndividualCommandMenu(item).displayTo(player);
	}

	@Override
	public Menu newInstance() {
		return new CommandsMenu(this.getParent(), this.type, this.boss, this.getter, this.setter, this.remover);
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Commands that your Boss",
				"executes on " + (this.type == BossCommandType.SKILL ? "this" : "his") + " " + this.type.getMenuLabel() + ".",
				"",
				"See this link for help and examples:",
				"https://docs.mineacademy.org/boss/boss-commands"
		};
	}

	private class IndividualCommandMenu extends Menu {

		private final BossCommand command;

		private final Button commandButton;
		private final Button chanceButton;
		private final Button isConsoleButton;
		private final Button ignoreIfDeadButton;
		private final Button healthTriggerButton;

		@Position(start = StartPosition.BOTTOM_CENTER)
		private final Button removeButton;

		IndividualCommandMenu(BossCommand command) {
			super(CommandsMenu.this, true);

			this.command = command;

			this.setTitle(Lang.legacy("menu-commands-individual-title"));
			this.setSize(9 * 4);

			final boolean isSkill = command.getType() == BossCommandType.SKILL;

			this.commandButton = Button.makeStringPrompt(ItemCreator.from(CompMaterial.PAPER,
					"Command",
					"",
					"Current: " + command.getCommandFormatted(),
					"",
					"Click to edit."),
					"Enter the command without /. For help, see <click:open_url:'https://docs.mineacademy.org/boss/boss-commands'>https://docs.mineacademy.org/boss/boss-commands</click>. Current: '&f<click:suggest_command:'"
							+ command.getCommand() + "'><hover:show_text:'Click to edit.'>" + command.getCommand() + "</hover></click>&7'",
					"Command updated!", command::setCommand);

			this.chanceButton = Button.makeDecimalPrompt(ItemCreator.from(
					CompMaterial.GOLD_INGOT,
					"Run Chance",
					"",
					"Current: &f{current}%",
					"",
					"Click to edit."),
					"Enter the chance to run this command from 0.00% to 100%. Current: {current}%.",
					"Chance updated!",
					RangedValue.fromString("0-100"),
					() -> MathUtil.formatTwoDigits(command.getChance() * 100),
					newValue -> command.setChance(newValue / 100D));

			this.isConsoleButton = Button.makeBoolean(ItemCreator.from(
					CompMaterial.COMMAND_BLOCK,
					"Run As Console?",
					"",
					"Status: " + (command.isConsole() ? "&aEnabled" : "&cDisabled"),
					"",

					(isSkill ? "If enabled, we execute the" : "Click to toggle. If disabled,"),
					(isSkill ? "command as the console. If" : "we run the command as the killer"),
					(isSkill ? "disabled, we run it as the" : "player, if he killed the Boss."),
					(isSkill ? "target player (only if there)" : "If there is no killer, the"),
					(isSkill ? "is one." : "command will not run.")),
					command::isConsole, command::setConsole);

			this.ignoreIfDeadButton = command.getType() == BossCommandType.HEALTH_TRIGGER ? Button.makeBoolean(ItemCreator.from(
					CompMaterial.BONE,
					"Ignore If Died",
					"",
					"Status: " + (command.isIgnoreIfDead() ? "&aIgnored" : "Not Ignored"),
					"",
					"Click to toggle. If enabled,",
					"this command will not run if",
					"the Boss dies from the damage."),
					command::isIgnoreIfDead, command::setIgnoreIfDead) : Button.makeEmpty();

			this.healthTriggerButton = command.getType() == BossCommandType.HEALTH_TRIGGER ? Button.makeDecimalPrompt(ItemCreator.from(
					Remain.getMaterial("BEETROOT", CompMaterial.REDSTONE),
					"Health Trigger",
					"",
					"Current: &f{current}",
					"Boss Max Health: &f" + CommandsMenu.this.boss.getMaxHealth(),
					"",
					"Click to edit the threshold health",
					"that will trigger this command.",
					"",
					"Example: If you have multiple commands",
					"i.e. one at 15HP and another at 10HP,",
					"and Boss' health goes from 20HP to 8HP",
					"only the second command will run."),
					"Enter at what health of the Boss will this command trigger from 1 to " + CommandsMenu.this.boss.getMaxHealth() + ". Current: {current} HP.",
					"Trigger",
					RangedValue.between(1, CommandsMenu.this.boss.getMaxHealth()),
					() -> Common.getOrDefault(command.getHealthTrigger(), "unset"), command::setHealthTrigger) : Button.makeEmpty();

			this.removeButton = new ButtonRemove(this, "command", "", () -> {
				CommandsMenu.this.remover.accept(command);

				CommandsMenu.this.newInstance().displayTo(this.getViewer());
			});
		}

		@Override
		public ItemStack getItemAt(int slot) {

			if (this.command.getType() == BossCommandType.HEALTH_TRIGGER) {
				if (slot == 9 + 1)
					return this.commandButton.getItem();

				if (slot == 9 + 3)
					return this.chanceButton.getItem();

				if (slot == 9 + 4)
					return this.isConsoleButton.getItem();

				if (slot == 9 + 5)
					return this.ignoreIfDeadButton.getItem();

				if (slot == 9 + 7)
					return this.healthTriggerButton.getItem();
			}

			else {
				if (slot == 9 + 2)
					return this.commandButton.getItem();

				if (slot == 9 + 4)
					return this.chanceButton.getItem();

				if (slot == 9 + 6)
					return this.isConsoleButton.getItem();
			}

			return NO_ITEM;
		}

		@Override
		protected String[] getInfo() {
			return Lang.legacy("menu-commands-individual-info").split("\n");
		}

		@Override
		public Menu newInstance() {
			return new IndividualCommandMenu(this.command);
		}
	}

	public static CommandsMenu from(Menu parent, BossCommandType type, Boss boss) {
		Valid.checkBoolean(type != BossCommandType.SKILL, "Cannot use this initializer for Boss skill commands!");

		return new CommandsMenu(parent, type, boss, () -> boss.getCommands(type), command -> boss.addCommand(type, command), boss::removeCommand);
	}

	public static CommandsMenu fromSkill(Menu parent, BossSkill skill) {
		return new CommandsMenu(parent, BossCommandType.SKILL, skill.getBoss(), skill::getCommands, skill::addCommand, skill::removeCommand);
	}
}