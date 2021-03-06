/*
 * Copyright 2009-2016 Contributors (see credits.txt)
 *
 * This file is part of jEveAssets.
 *
 * jEveAssets is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * jEveAssets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jEveAssets; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package net.nikr.eve.jeveasset.io.eveapi;

import net.nikr.eve.jeveasset.data.MyLocation;
import net.nikr.eve.jeveasset.data.StaticData;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author nkr
 */
public class AssetsGetterTest {
	
	@Test
	public void testFlatListExclusions() {
		assertEquals(StaticData.get().getItemFlags().get(7).getFlagName(), "Skill");
		assertEquals(StaticData.get().getItemFlags().get(89).getFlagName(), "Implant");
		assertEquals(StaticData.get().getItemFlags().get(61).getFlagName(), "Skill In Training");
		long max = 0;
		for (MyLocation location : StaticData.get().getLocations().values()) {
			max = Math.max(max, location.getLocationID());
		}
		assertEquals("MyLocation.isCitadel() and ApiIdConverter.getLocation(final long locationID, final MyAsset parentAsset) needs to be updated", 61001139, max);
	}
}
