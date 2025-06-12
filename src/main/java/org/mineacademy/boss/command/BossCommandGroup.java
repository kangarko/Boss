package org.mineacademy.boss.command;

import org.mineacademy.boss.model.Permissions;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.PermsSubCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;

/**
 * The main Boss command group executing its subcommands, such as /boss biome.
 */
@AutoRegister
public final class BossCommandGroup extends SimpleCommandGroup {

	@Override
	protected String getHeaderPrefix() {
		return "&4&l";
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommandGroup#registerSubcommands()
	 */
	@Override
	protected void registerSubcommands() {
		this.registerSubcommand(BossSubCommand.class);
		this.registerDefaultSubcommands();

		this.registerSubcommand(new PermsSubCommand(Permissions.class));
	}
}
