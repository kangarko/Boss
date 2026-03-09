package org.mineacademy.boss.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParticleShape {

	AMBIENT("Ambient Cloud", "Particles randomly spread around the Boss."),
	CIRCLE("Circle", "Particles orbit in a flat circle around the Boss."),
	HELIX("Helix", "Particles spiral upward around the Boss."),
	VORTEX("Vortex", "An expanding rising spiral around the Boss."),
	SPHERE("Sphere", "Particles cover a sphere around the Boss."),
	TORNADO("Tornado", "A funnel of particles widening upward."),
	WINGS("Wings", "Butterfly-shaped wings around the Boss."),
	CUBE("Cube", "A rotating wireframe cube around the Boss.");

	private final String title;
	private final String description;

	public boolean hasHeight() {
		return this == HELIX || this == VORTEX || this == TORNADO;
	}

	public boolean hasStrands() {
		return this == HELIX || this == VORTEX;
	}
}
