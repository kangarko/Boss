package org.mineacademy.boss.api;

import org.mineacademy.fo.model.ConfigSerializable;

// This is made a new instance for each individual Boss
public interface BossRegionSettings extends ConfigSerializable {

	String getRegionName();

	// -1 = unlimited
	int getLimit();

	void setLimit(int limit);

	void setKeepInside(boolean keepInside);

	boolean getKeepInside();
}
