package org.mineacademy.boss.model;

import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.settings.Lang;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the result of the spawning, see {@link Boss#spawn(org.bukkit.Location, BossSpawnReason)}
 */
@NoArgsConstructor
@AllArgsConstructor
public enum BossSpawnResult {

	SUCCESS(true) {
		@Override
		public SimpleComponent getFailReason() {
			throw new FoException("Boss was spawned successfully, cannot get fail reason");
		}
	},

	FAIL_LIMIT {
		@Override
		public SimpleComponent getFailReason() {
			return Lang.component("egg-spawn-fail-limit");
		}
	},

	FAIL_API_CANCELLED {
		@Override
		public SimpleComponent getFailReason() {
			return Lang.component("egg-spawn-fail-api");
		}
	},

	FAIL_CANCELLED {
		@Override
		public SimpleComponent getFailReason() {
			return Lang.component("egg-spawn-fail-cancelled");
		}
	};

	/**
	 * Was spawning of Boss successful?
	 */
	@Getter
	private boolean success = false;

	/**
	 * Get fail reason if spawning failed
	 * @return
	 */
	public abstract SimpleComponent getFailReason();
}
