package org.mineacademy.boss.hook;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.plugin.SimplePlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;

public final class BossProtocolLibHook {

	public static void init() {
		HookManager.addPacketListener(new PacketAdapter(SimplePlugin.getInstance(), PacketType.Play.Server.NAMED_SOUND_EFFECT) {

			@Override
			public void onPacketSending(final PacketEvent e) {
				final PacketContainer packet = e.getPacket();

				final StructureModifier<Integer> ints = packet.getIntegers();
				final Sound sound = packet.getSoundEffects().read(0);

				final Location location = new Location(e.getPlayer().getWorld(), ints.read(0) / 8.0, ints.read(1) / 8.0, ints.read(2) / 8.0);

				// Packet is sent async, but getLivingEntities lookup cannot be performed async
				final SpawnedBoss boss = BossPlugin.getBossManager().findBoss(location);

				if (boss != null) {
					final SimpleSound remappedSound = boss.getBoss().getSettings().getRemappedSounds().get(sound);

					if (remappedSound != null) {
						packet.getSoundEffects().write(0, remappedSound.getSound());

						packet.getFloat().write(0, remappedSound.getVolume());
						packet.getFloat().write(1, remappedSound.getPitch());
					}

					if (boss.getBoss().getSettings().isDebuggingSounds())
						for (final Entity nearby : boss.getEntity().getNearbyEntities(15, 15, 15))
							if (nearby instanceof Player)
								Common.tell((Player) nearby, "&7" + boss.getBoss().getName() + " makes &6" + sound + (remappedSound != null ? " &7that is transformed to &6" + remappedSound : "") + "&7.");
				}
			}
		});
	}
}
