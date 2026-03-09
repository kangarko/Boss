package org.mineacademy.boss.menu.boss;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossParticleSettings;
import org.mineacademy.boss.model.ParticleShape;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompParticle;

final class ParticleMenu extends Menu {

	private final Boss boss;

	@Position(9 + 1)
	private final Button enabledButton;

	@Position(9 + 3)
	private final Button typeButton;

	@Position(9 + 5)
	private final Button shapeButton;

	@Position(9 + 7)
	private final Button settingsButton;

	ParticleMenu(Menu parent, Boss boss) {
		super(parent);

		this.boss = boss;

		this.setTitle("Particle Effects");
		this.setSize(9 * 4);

		final BossParticleSettings settings = boss.getParticleSettings();

		this.enabledButton = Button.makeBoolean(ItemCreator.from(
				CompMaterial.BLAZE_POWDER,
				"&eEnabled",
				"",
				"Status: " + (settings.isEnabled() ? "&aEnabled" : "&cDisabled"),
				"",
				"Toggle particle effects",
				"around this Boss."),
				settings::isEnabled, settings::setEnabled);

		this.typeButton = new ButtonMenu(new TypeMenu(),
				CompMaterial.FIREWORK_ROCKET,
				"&bParticle Type",
				"",
				"Current: &f" + (settings.getType() != null ? ChatUtil.capitalizeFully(settings.getType().name()) : "None"),
				"",
				"Pick the particle effect",
				"to display around Boss.");

		this.shapeButton = new ButtonMenu(new ShapeMenu(),
				CompMaterial.NAUTILUS_SHELL,
				"&5Shape Pattern",
				"",
				"Current: &f" + settings.getShape().getTitle(),
				"",
				settings.getShape().getDescription());

		final ParticleShape shape = settings.getShape();

		this.settingsButton = new ButtonMenu(new DetailsMenu(),
				CompMaterial.COMPARATOR,
				"&6Settings",
				"",
				"Count: &f" + settings.getCount(),
				"Speed: &f" + settings.getSpeed(),
				"Interval: &f" + settings.getIntervalTicks(),
				(shape != ParticleShape.AMBIENT ? "Radius: &f" + settings.getRadius() : null),
				(shape.hasHeight() ? "Height: &f" + settings.getHeight() : null),
				(settings.getForwardOffset() != 0 ? "Forward Offset: &f" + settings.getForwardOffset() : null),
				"",
				"Edit particle parameters.");
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Configure particle effects",
				"displayed around this",
				"Boss while alive."
		};
	}

	@Override
	public Menu newInstance() {
		return new ParticleMenu(this.getParent(), this.boss);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Type picker
	// ------------------------------------------------------------------------------------------------------------

	private class TypeMenu extends MenuPaged<CompParticle> {

		TypeMenu() {
			super(ParticleMenu.this, getAvailableParticles(), true);

			this.setTitle("Select Particle Type");
		}

		@Override
		protected ItemStack convertToItemStack(CompParticle particle) {
			final boolean selected = particle.equals(ParticleMenu.this.boss.getParticleSettings().getType());

			return ItemCreator.from(
					selected ? CompMaterial.LIME_DYE : CompMaterial.GRAY_DYE,
					(selected ? "&a" : "&7") + ChatUtil.capitalizeFully(particle.name()),
					"",
					selected ? "&aCurrently selected." : "&7Click to select.")
					.glow(selected)
					.make();
		}

		@Override
		protected void onPageClick(Player player, CompParticle particle, ClickType click) {
			ParticleMenu.this.boss.getParticleSettings().setType(particle);

			this.restartMenu("&2Selected " + ChatUtil.capitalizeFully(particle.name()));
		}

		@Override
		public Menu newInstance() {
			return new TypeMenu();
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Shape picker
	// ------------------------------------------------------------------------------------------------------------

	private class ShapeMenu extends MenuPaged<ParticleShape> {

		ShapeMenu() {
			super(ParticleMenu.this, Arrays.asList(ParticleShape.values()), true);

			this.setTitle("Select Particle Shape");
		}

		@Override
		protected ItemStack convertToItemStack(ParticleShape shape) {
			final boolean selected = shape == ParticleMenu.this.boss.getParticleSettings().getShape();

			return ItemCreator.from(
					selected ? CompMaterial.LIME_DYE : CompMaterial.GRAY_DYE,
					(selected ? "&a" : "&7") + shape.getTitle(),
					"",
					shape.getDescription(),
					"",
					selected ? "&aCurrently selected." : "&7Click to select.")
					.glow(selected)
					.make();
		}

		@Override
		protected void onPageClick(Player player, ParticleShape shape, ClickType click) {
			ParticleMenu.this.boss.getParticleSettings().setShape(shape);

			this.restartMenu("&2Selected " + shape.getTitle());
		}

		@Override
		public Menu newInstance() {
			return new ShapeMenu();
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Detail settings
	// ------------------------------------------------------------------------------------------------------------

	private class DetailsMenu extends Menu {

		@Position(9 + 1)
		private final Button countButton;

		@Position(9 + 3)
		private final Button speedButton;

		@Position(9 + 5)
		private final Button intervalButton;

		@Position(9 + 7)
		private final Button spreadOrRadiusButton;

		@Position(9 * 2 + 1)
		private final Button forwardOffsetButton;

		@Position(9 * 2 + 3)
		private final Button heightButton;

		@Position(9 * 2 + 5)
		private final Button strandsButton;

		DetailsMenu() {
			super(ParticleMenu.this);

			this.setTitle("Particle Settings");
			this.setSize(9 * 4);

			final BossParticleSettings settings = ParticleMenu.this.boss.getParticleSettings();
			final ParticleShape shape = settings.getShape();

			this.countButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.REDSTONE,
					"&cCount",
					"",
					"Current: &f" + settings.getCount(),
					"",
					"How many particles to spawn",
					"each interval. Max 100."),
					player -> {
						new SimpleDecimalPrompt("Enter particle count (1-100). Current: " + settings.getCount()) {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {
								return Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), 1, 100);
							}

							@Override
							protected void onValidatedInput(ConversationContext context, double input) {
								settings.setCount((int) input);
							}
						}.show(player);
					});

			this.speedButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.SUGAR,
					"&fSpeed",
					"",
					"Current: &f" + settings.getSpeed(),
					"",
					"Particle movement speed.",
					"Use 0 for static."),
					player -> {
						new SimpleDecimalPrompt("Enter particle speed (0.0-5.0). Current: " + settings.getSpeed()) {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {
								return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), 0, 5);
							}

							@Override
							protected void onValidatedInput(ConversationContext context, double input) {
								settings.setSpeed(input);
							}
						}.show(player);
					});

			this.intervalButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.CLOCK,
					"&eInterval",
					"",
					"Current: &f" + settings.getIntervalTicks(),
					"",
					"How often to spawn particles.",
					"Lower = more frequent.",
					"1 = every 100ms, 5 = every 500ms."),
					player -> {
						new SimpleDecimalPrompt("Enter interval (1-200). Current: " + settings.getIntervalTicks() + ". 1 = every 100ms, 10 = every second.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {
								return Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), 1, 200);
							}

							@Override
							protected void onValidatedInput(ConversationContext context, double input) {
								settings.setIntervalTicks((int) input);
							}
						}.show(player);
				});

			this.forwardOffsetButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.LEAD,
					"&6Forward Offset",
					"",
					"Current: &f" + settings.getForwardOffset(),
					"",
					"Shift particles forward or",
					"backward from center.",
					"Negative = behind Boss."),
					player -> {
						new SimpleDecimalPrompt("Enter forward offset (-5.0 to 5.0). Current: " + settings.getForwardOffset() + ". Negative = behind Boss.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {
								return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), -5, 5);
							}

							@Override
							protected void onValidatedInput(ConversationContext context, double input) {
								settings.setForwardOffset(input);
							}
						}.show(player);
					});

			if (shape == ParticleShape.AMBIENT) {
				this.spreadOrRadiusButton = Button.makeSimple(ItemCreator.from(
						CompMaterial.ARROW,
						"&9Spread",
						"",
						"X: &f" + settings.getOffsetX(),
						"Y: &f" + settings.getOffsetY(),
						"Z: &f" + settings.getOffsetZ(),
						"",
						"How far particles spread",
						"from the Boss center."),
						player -> {
							new SimpleDecimalPrompt("Enter spread radius for all axes (0.0-5.0). Current X: " + settings.getOffsetX()) {

								@Override
								protected boolean isInputValid(ConversationContext context, String input) {
									return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), 0, 5);
								}

								@Override
								protected void onValidatedInput(ConversationContext context, double input) {
									settings.setOffset(input, input, input);
								}
							}.show(player);
						});

				this.heightButton   = Button.makeEmpty();
				this.strandsButton  = Button.makeEmpty();
			} else {
				this.spreadOrRadiusButton = Button.makeSimple(ItemCreator.from(
						CompMaterial.ENDER_PEARL,
						"&3Radius",
						"",
						"Current: &f" + settings.getRadius(),
						"",
						"The radius of the",
						"particle shape."),
						player -> {
							new SimpleDecimalPrompt("Enter shape radius (0.1-10.0). Current: " + settings.getRadius()) {

								@Override
								protected boolean isInputValid(ConversationContext context, String input) {
									return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), 0.1, 10);
								}

								@Override
								protected void onValidatedInput(ConversationContext context, double input) {
									settings.setRadius(input);
								}
							}.show(player);
						});

				final boolean hasHeight = shape.hasHeight();
				final boolean hasStrands = shape.hasStrands();

				this.heightButton = hasHeight ? Button.makeSimple(ItemCreator.from(
						CompMaterial.LADDER,
						"&aHeight",
						"",
						"Current: &f" + settings.getHeight(),
						"",
						"Vertical height of",
						"the particle shape."),
						player -> {
							new SimpleDecimalPrompt("Enter shape height (0.5-10.0). Current: " + settings.getHeight()) {

								@Override
								protected boolean isInputValid(ConversationContext context, String input) {
									return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), 0.5, 10);
								}

								@Override
								protected void onValidatedInput(ConversationContext context, double input) {
									settings.setHeight(input);
								}
							}.show(player);
						}) : Button.makeEmpty();

				this.strandsButton = hasStrands ? Button.makeSimple(ItemCreator.from(
						CompMaterial.STRING,
						"&dStrands",
						"",
						"Current: &f" + settings.getStrands(),
						"",
						"Number of spiral arms",
						"in the shape."),
						player -> {
							new SimpleDecimalPrompt("Enter number of strands (1-8). Current: " + settings.getStrands()) {

								@Override
								protected boolean isInputValid(ConversationContext context, String input) {
									return Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), 1, 8);
								}

								@Override
								protected void onValidatedInput(ConversationContext context, double input) {
									settings.setStrands((int) input);
								}
							}.show(player);
						}) : Button.makeEmpty();
			}
		}

		@Override
		public Menu newInstance() {
			return new DetailsMenu();
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	private static List<CompParticle> getAvailableParticles() {
		return Arrays.stream(CompParticle.values())
				.filter(particle -> !particle.isRemoved() && particle.getParticle() != null && particle.getParticle().getDataType() == Void.class)
				.collect(Collectors.toList());
	}
}
