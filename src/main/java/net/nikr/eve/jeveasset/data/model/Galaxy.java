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
package net.nikr.eve.jeveasset.data.model;

import java.util.*;
import net.nikr.eve.jeveasset.data.Jump;
import net.nikr.eve.jeveasset.data.MyLocation;

public class Galaxy {

	private Set<Region> regions;

	public Galaxy(final Map<Long, MyLocation> locations, final List<Jump> jumps) {
		this.regions = new TreeSet<Region>();

		Collection<MyLocation> loc = locations.values();
		// Add the regions first...
		for (MyLocation l : loc) {
			String id = Long.toString(l.getLocationID());
			if (id.charAt(0) == '1') { // a region
				String name = l.getLocation();
				regions.add(new Region(id, name));
			}
		}
		// ...then systems...
		for (MyLocation l : loc) {
			String id = Long.toString(l.getLocationID());
			if (id.charAt(0) == '3') { // a solar system
				String name = l.getLocation();
				String region = Long.toString(l.getRegionID());
				String sec = l.getSecurity();
				addSystemToRegion(id, name, sec, region);
			}
		}
		//... and finally the stations.
		for (MyLocation l : loc) {
			String id = Long.toString(l.getLocationID());
			if (id.charAt(0) == '6') { // a station
				String name = l.getLocation();
				String region = Long.toString(l.getRegionID());
				String system = Long.toString(l.getSystemID());
				addStationToSystem(id, name, region, system);
			}
		}
		// Add the jump associations.
		Map<String, SolarSystem> systemMap = getAllSolarSystemsAsMap();
		for (Jump j : jumps) {
			SolarSystem from = systemMap.get(Long.toString(j.getFrom().getLocationID()));
			SolarSystem to = systemMap.get(Long.toString(j.getTo().getLocationID()));
			from.addJump(to);
		}
	}

	public final Set<Region> getRegions() {
		return Collections.unmodifiableSet(regions);
	}

	public final Map<String, SolarSystem> getAllSolarSystemsAsMap() {
		Map<String, SolarSystem> systemMap = new HashMap<String, SolarSystem>();
		for (Region r : regions) {
			systemMap.putAll(r.getSolarSystemsAsMap());
		}
		return Collections.unmodifiableMap(systemMap);
	}

	public int getRegionCount() {
		return regions.size();
	}

	public String toStringFull() {
		return "New Eden (" + getRegionCount() + " regions)";
	}

	@Override
	public String toString() {
		return "New Eden";
	}

	// <editor-fold defaultstate="collapsed" desc="Package level model construction methods">
	void addRegion(final String id, final String name) {
		regions.add(new Region(id, name));
	}

	final void addSystemToRegion(final String id, final String name, final String sec, final String regionId) {
		for (Region r : regions) {
			if (r.getId().equals(regionId)) {
				r.addSystem(id, name, sec);
			}
		}
	}

	final void addStationToSystem(final String id, final String name, final String regionId, final String systemId) {
		for (Region r : regions) {
			if (r.getId().equals(regionId)) {
				r.addStationToSystem(id, name, systemId);
			}
		}
	}
	// </editor-fold>
}
