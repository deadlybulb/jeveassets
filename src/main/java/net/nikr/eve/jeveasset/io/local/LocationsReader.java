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

package net.nikr.eve.jeveasset.io.local;

import java.io.IOException;
import java.util.Map;
import net.nikr.eve.jeveasset.data.MyLocation;
import net.nikr.eve.jeveasset.data.Settings;
import net.nikr.eve.jeveasset.data.StaticData;
import net.nikr.eve.jeveasset.io.shared.AbstractXmlReader;
import net.nikr.eve.jeveasset.io.shared.AttributeGetters;
import net.nikr.eve.jeveasset.io.shared.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public final class LocationsReader extends AbstractXmlReader {

	private static final Logger LOG = LoggerFactory.getLogger(LocationsReader.class);

	private LocationsReader() { }

	public static void load() {
		LocationsReader reader = new LocationsReader();
		reader.read();
	}

	private void read() {
		try {
			Element element = getDocumentElement(Settings.getPathLocations(), false);
			parseLocations(element, StaticData.get().getLocations());
			LOG.info("Locations loaded");
		} catch (IOException ex) {
			LOG.error("Locations not loaded: " + ex.getMessage(), ex);
			staticDataFix();
		} catch (XmlException ex) {
			LOG.error("Locations not loaded: " + ex.getMessage(), ex);
			staticDataFix();
		}
	}

	private void parseLocations(final Element element, final Map<Long, MyLocation> locations) {
		NodeList nodes = element.getElementsByTagName("row");
		MyLocation location;
		for (int i = 0; i < nodes.getLength(); i++) {
			location = parseLocation(nodes.item(i));
			locations.put(location.getLocationID(), location);
		}
	}

	private MyLocation parseLocation(final Node node) {
		long stationID = AttributeGetters.getLong(node, "si");
		String station = AttributeGetters.getString(node, "s");
		long systemID = AttributeGetters.getLong(node, "syi");
		String system = AttributeGetters.getString(node, "sy");
		long regionID = AttributeGetters.getLong(node, "ri");
		String region = AttributeGetters.getString(node, "r");
		String security = AttributeGetters.getString(node, "se");
		return new MyLocation(stationID, station, systemID, system, regionID, region, security);
	}
}
