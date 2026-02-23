package org.mineacademy.boss.menu.boss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossCommand;
import org.mineacademy.boss.skill.BossSkill;
import org.mineacademy.boss.skill.SkillCommands;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonConversation;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.ButtonRemove;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

/**
 * The menu with settings for Boss skills.
 */
final class SkillsMenu extends MenuPaged<BossSkill> {

	private final Boss boss;

	@Position(start = StartPosition.BOTTOM_RIGHT)
	private final Button createButton;

	SkillsMenu(Menu parent, Boss boss) {
		super(parent, boss.getSkills(), true);

		this.boss = boss;

		this.setTitle(Lang.legacy("menu-skills-title"));

		this.createButton = new ButtonMenu(new CreateSkillMenu(), CompMaterial.EMERALD,
				Lang.legacy("menu-skills-button-create"),
				Lang.legacy("menu-skills-button-create-lore").split("\n"));
	}

	@Override
	protected ItemStack convertToItemStack(BossSkill skill) {
		return skill.getIcon();
	}

	@Override
	protected void onPageClick(Player player, BossSkill skill, ClickType click) {
		new IndividualSkillMenu(skill).displayTo(player);
	}

	@Override
	protected String[] getInfo() {
		return Lang.legacy("menu-skills-info").split("\n");
	}

	@Override
	public Menu newInstance() {
		return new SkillsMenu(this.getParent(), this.boss);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	final class CreateSkillMenu extends MenuPaged<BossSkill> {

		protected CreateSkillMenu() {
			super(SkillsMenu.this, SkillsMenu.this.boss.getUnequippedSkills());

			this.setTitle(Lang.legacy("menu-skills-create-title"));
		}

		@Override
		protected ItemStack convertToItemStack(BossSkill skill) {
			return skill.getIcon();
		}

		@Override
		protected String[] getInfo() {
			return Lang.legacy("menu-skills-create-info").split("\n");
		}

		@Override
		protected void onPageClick(Player player, BossSkill skill, ClickType click) {
			if (SkillsMenu.this.boss.hasSkill(skill))
				this.animateTitle(Lang.legacy("menu-skills-create-already-has"));

			else {
				SkillsMenu.this.boss.addSkill(skill);

				final Menu menu = new IndividualSkillMenu(skill);

				menu.displayTo(player);
				Platform.runTask(1, () -> menu.animateTitle(Lang.legacy("menu-skills-create-success")));
			}
		}
	}

	final class IndividualSkillMenu extends Menu {

		private final BossSkill skill;

		private final Button settingsButton;
		private final Button delayButton;
		private final Button messagesButton;
		private final Button commandsButton;

		@Position(start = StartPosition.BOTTOM_CENTER, value = -1)
		private final Button stopMoreSkillsButton;

		@Position(start = StartPosition.BOTTOM_CENTER, value = +1)
		private final Button removeButton;

		IndividualSkillMenu(BossSkill skill) {
			super(SkillsMenu.this, true);

			this.skill = skill;

			this.setTitle(Lang.legacy("menu-skills-individual-title", "skill", skill.getName().replace("_", " ")));
			this.setSize(9 * 4);

			final boolean isSkillCommands = this.skill instanceof SkillCommands;

			this.settingsButton = isSkillCommands ? Button.makeEmpty() : new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final Menu skillMenu = skill.getMenu(IndividualSkillMenu.this);

					if (skillMenu != null)
						skillMenu.displayTo(player);
					else
						IndividualSkillMenu.this.animateTitle(Lang.legacy("menu-skills-individual-button-settings-no-menu"));
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							CompMaterial.IRON_HORSE_ARMOR,
							Lang.legacy("menu-skills-individual-button-settings"),
							Lang.legacy("menu-skills-individual-button-settings-lore").split("\n")).make();
				}
			};

			this.delayButton = new ButtonConversation(new DelayPrompt(),
					CompMaterial.CLOCK,
					Lang.legacy("menu-skills-individual-button-delay"),
					Lang.legacy("menu-skills-individual-button-delay-lore", "delay", skill.getDelay().toLine()).split("\n"));

			final String messagesDisplay = skill.getMessages().isEmpty() ? Lang.plain("part-none") : skill.getMessages().stream().map(el -> SimpleComponent.fromMiniAmpersand(el).toLegacySection()).collect(Collectors.joining("\n - ", "\n - ", ""));

			this.messagesButton = isSkillCommands ? Button.makeEmpty()
					: new ButtonConversation(new MessagesPrompt(),
							CompMaterial.PAPER,
							Lang.legacy("menu-skills-individual-button-messages"),
							Lang.legacy("menu-skills-individual-button-messages-lore", "messages", messagesDisplay).split("\n"));

			final String commandsDisplay = skill.getCommands().isEmpty() ? Lang.plain("part-none") : skill.getCommands().stream().map(BossCommand::getCommand).collect(Collectors.joining("\n - ", "\n - ", ""));

			this.commandsButton = new ButtonMenu(CommandsMenu.fromSkill(this, skill),
					ItemCreator.from(CompMaterial.COMMAND_BLOCK,
							Lang.legacy("menu-skills-individual-button-commands"),
							Lang.legacy("menu-skills-individual-button-commands-lore", "commands", commandsDisplay).split("\n")));

			this.stopMoreSkillsButton = Button.makeBoolean(ItemCreator.from(
					(skill.isStopMoreSkills() ? CompMaterial.RED_DYE : CompMaterial.GREEN_DYE),
					Lang.legacy("menu-skills-individual-button-stop"),
					Lang.legacy("menu-skills-individual-button-stop-lore").split("\n")),
					skill::isStopMoreSkills, skill::setStopMoreSkills);

			this.removeButton = new ButtonRemove(IndividualSkillMenu.this, "skill", skill.getName(), () -> {
				SkillsMenu.this.boss.removeSkill(skill);

				SkillsMenu.this.newInstance().displayTo(this.getViewer());
			});
		}

		@Override
		public ItemStack getItemAt(int slot) {
			final boolean isSkillCommands = this.skill instanceof SkillCommands;

			if (isSkillCommands) {
				if (slot == 9 * 1 + 3)
					return this.delayButton.getItem();

				if (slot == 9 * 1 + 5)
					return this.commandsButton.getItem();

			} else {
				if (slot == 9 * 1 + 1)
					return this.settingsButton.getItem();

				if (slot == 9 * 1 + 3)
					return this.delayButton.getItem();

				if (slot == 9 * 1 + 5)
					return this.messagesButton.getItem();

				if (slot == 9 * 1 + 7)
					return this.commandsButton.getItem();
			}

			return NO_ITEM;
		}

		@Override
		protected String[] getInfo() {
			return Lang.legacy("menu-skills-individual-info").split("\n");
		}

		@Override
		public Menu newInstance() {
			return new IndividualSkillMenu(this.skill);
		}

		final class DelayPrompt extends SimpleStringPrompt {

			@Override
			protected String getPrompt(ConversationContext ctx) {
				return "Enter how often should this skill execute, when Boss is around players. This can either be one value, such as '2 minutes',"
						+ " or a range '30 seconds - 3 minutes' to pick the delay randomly. Current: '" + IndividualSkillMenu.this.skill.getDelay().toLine() + "'.";
			}

			@Override
			protected boolean isInputValid(ConversationContext context, String input) {
				try {
					final RangedSimpleTime delay = RangedSimpleTime.fromString(input);

					context.setSessionData("Delay", delay);
					return true;

				} catch (final Throwable t) {
					return false;
				}
			}

			@Override
			protected String getFailedValidationText(ConversationContext context, String invalidInput) {
				return "Invalid delay format! Syntax: '<time>', such as '23 ticks' or '<from> - <to>', such as '1 minute - 20 minutes'.";
			}

			@Override
			protected void onValidatedInput(ConversationContext context, String input) {
				final RangedSimpleTime delay = (RangedSimpleTime) context.getSessionData("Delay");

				IndividualSkillMenu.this.skill.setDelay(delay);
			}
		}

		final class MessagesPrompt extends SimpleStringPrompt {

			@Override
			protected String getPrompt(ConversationContext ctx) {
				final String joined = String.join("&7|", IndividualSkillMenu.this.skill.getMessages());

				return "Enter message(s) to show when the skill is executed. Separate multiple by '|' and we'll randomly pick one. Type 'none' to hide. Current: &f"
						+ "<click:suggest_command:'" + joined + "'><hover:show_text:'Click to edit.'>" + joined + "</hover></click>";
			}

			@Override
			protected boolean isInputValid(ConversationContext context, String input) {
				return input.length() > 2;
			}

			@Override
			protected String getFailedValidationText(ConversationContext context, String invalidInput) {
				return "Message must be at least 3 letters long, or type 'none' to hide.";
			}

			@Override
			protected void onValidatedInput(ConversationContext context, String input) {
				IndividualSkillMenu.this.skill.setMessages("none".equals(input) ? new ArrayList<>() : Arrays.asList(input.split("\\|")));
			}
		}
	}
}
