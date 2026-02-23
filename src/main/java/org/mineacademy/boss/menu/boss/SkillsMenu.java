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

		this.setTitle("Create Or Edit Skills");

		this.createButton = new ButtonMenu(new CreateSkillMenu(), CompMaterial.EMERALD,
				"&aCreate New",
				"",
				"Click to create",
				"a new skill.");
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
		return new String[] {
				"Create or edit what skills",
				"will be applied to this Boss",
				"randomly when around players."
		};
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

			this.setTitle("Select Skill To Create");
		}

		@Override
		protected ItemStack convertToItemStack(BossSkill skill) {
			return skill.getIcon();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select what skill you want",
					"to add to your Boss. You can",
					"ask a developer to create",
					"new skills via our API, see",
					"&6github.com/kangarko/boss/Wiki",
					"for tutorial and examples."
			};
		}

		@Override
		protected void onPageClick(Player player, BossSkill skill, ClickType click) {
			if (SkillsMenu.this.boss.hasSkill(skill))
				this.animateTitle("&4Boss Already Has This Skill!");

			else {
				SkillsMenu.this.boss.addSkill(skill);

				final Menu menu = new IndividualSkillMenu(skill);

				menu.displayTo(player);
				Platform.runTask(1, () -> menu.animateTitle("&2Skill Created!"));
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

			this.setTitle("Skill " + skill.getName().replace("_", " "));
			this.setSize(9 * 4);

			final boolean isSkillCommands = this.skill instanceof SkillCommands;

			this.settingsButton = isSkillCommands ? Button.makeEmpty() : new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					final Menu skillMenu = skill.getMenu(IndividualSkillMenu.this);

					if (skillMenu != null)
						skillMenu.displayTo(player);
					else
						IndividualSkillMenu.this.animateTitle("&4Skill Has No Custom Menu!");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							CompMaterial.IRON_HORSE_ARMOR,
							"Skill Settings",
							"",
							"Edit settings specific",
							"for this skill.").make();
				}
			};

			this.delayButton = new ButtonConversation(new DelayPrompt(),
					CompMaterial.CLOCK,
					"Delay",
					"",
					"Current: &f" + skill.getDelay().toLine(),
					"",
					"Click to edit how often",
					"to run this skill when",
					"is Boss around players.");

			final String messagesDisplay = skill.getMessages().isEmpty() ? Lang.plain("part-none") : skill.getMessages().stream().map(el -> SimpleComponent.fromMiniAmpersand(el).toLegacySection()).collect(Collectors.joining("\n - ", "\n - ", ""));

			this.messagesButton = isSkillCommands ? Button.makeEmpty()
					: new ButtonConversation(new MessagesPrompt(),
							CompMaterial.PAPER,
							"Messages",
							"",
							"Current: &f" + messagesDisplay,
							"",
							"Edit the message for the",
							"player targeted by Boss",
							"when running this skill.");

			final String commandsDisplay = skill.getCommands().isEmpty() ? Lang.plain("part-none") : skill.getCommands().stream().map(BossCommand::getCommand).collect(Collectors.joining("\n - ", "\n - ", ""));

			this.commandsButton = new ButtonMenu(CommandsMenu.fromSkill(this, skill),
					ItemCreator.from(CompMaterial.COMMAND_BLOCK,
							"Commands",
							"",
							"Current: " + commandsDisplay));

			this.stopMoreSkillsButton = Button.makeBoolean(ItemCreator.from(
					(skill.isStopMoreSkills() ? CompMaterial.RED_DYE : CompMaterial.GREEN_DYE),
					"Stop More Skills?",
					"",
					"Status: " + (skill.isStopMoreSkills() ? "&aEnabled" : "&cDisabled"),
					"",
					"If enabled, we stop running",
					"more skills after this was",
					"successfully executed.",
					"",
					"Note: We pick the order of",
					"skills at random each time."),
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
			return new String[] {
					"Configure different options",
					"for this skill."
			};
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
