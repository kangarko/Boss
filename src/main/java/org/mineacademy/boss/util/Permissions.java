package org.mineacademy.boss.util;

import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;

@UtilityClass
@FieldDefaults(makeFinal = true)
public class Permissions {

	@UtilityClass
	@FieldDefaults(makeFinal = true)
	public class Commands {
		public String BUTCHER = "boss.command.butcher";
		public String FIND = "boss.command.find";
		public String EGG = "boss.command.egg";
		public String LIST = "boss.command.list";
		public String MENU = "boss.command.menu";
		public String NEW = "boss.command.new";
		public String REGION = "boss.command.region";
		public String RELOAD = "boss.command.reload";
		public String REMOVE = "boss.command.remove";
		public String TOOLS = "boss.command.tools";
		public String CONVERSATION = "boss.command.conversation";
		public String BIOME = "boss.command.biome";
		public String SCANNER = "boss.command.scanner";
	}

	@UtilityClass
	@FieldDefaults(makeFinal = true)
	public class Use {
		public String SPAWNER = "boss.use.spawner";
		public String SPAWNER_EGG = "boss.use.spawneregg";
		public String SPAWN = "boss.spawn.{name}";
	}

	@UtilityClass
	@FieldDefaults(makeFinal = true)
	public class Bypass {
		public String EGG_REGION = "boss.bypass.eggregions";
	}

	public String AIRSPAWN = "boss.airspawn";
}
