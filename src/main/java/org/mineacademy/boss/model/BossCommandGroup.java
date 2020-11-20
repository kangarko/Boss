package org.mineacademy.boss.model;

import org.mineacademy.boss.command.BiomeCommand;
import org.mineacademy.boss.command.ButcherCommand;
import org.mineacademy.boss.command.ConvCommand;
import org.mineacademy.boss.command.EggCommand;
import org.mineacademy.boss.command.FindCommand;
import org.mineacademy.boss.command.ListCommand;
import org.mineacademy.boss.command.MenuCommand;
import org.mineacademy.boss.command.NewCommand;
import org.mineacademy.boss.command.RegionCommand;
import org.mineacademy.boss.command.RemoveCommand;
import org.mineacademy.boss.command.ScannerCommand;
import org.mineacademy.boss.command.SpawnCommand;
import org.mineacademy.boss.command.SpawnPlayerCommand;
import org.mineacademy.boss.command.ToolsCommand;
import org.mineacademy.boss.command.UidCommand;
import org.mineacademy.fo.command.ReloadCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;

public final class BossCommandGroup extends SimpleCommandGroup {

	@Override
	protected void registerSubcommands() {
		registerSubcommand(new MenuCommand());
		registerSubcommand(new NewCommand());
		registerSubcommand(new FindCommand());
		registerSubcommand(new EggCommand());
		registerSubcommand(new SpawnCommand());
		registerSubcommand(new SpawnPlayerCommand());
		registerSubcommand(new ButcherCommand());
		registerSubcommand(new ScannerCommand());
		registerSubcommand(new RegionCommand());
		registerSubcommand(new ReloadCommand());
		registerSubcommand(new RemoveCommand());
		registerSubcommand(new ToolsCommand());
		registerSubcommand(new BiomeCommand());
		registerSubcommand(new ConvCommand());
		registerSubcommand(new ListCommand());
		registerSubcommand(new UidCommand());
	}
}