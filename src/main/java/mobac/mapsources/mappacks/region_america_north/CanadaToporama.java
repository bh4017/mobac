/*******************************************************************************
 * Copyright (c) MOBAC developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.mapsources.mappacks.region_america_north;

import mobac.mapsources.AbstractHttpMapSource;
import mobac.program.interfaces.HttpMapSource;
import mobac.program.model.TileImageType;

/**
 * 
 * http://mobac.sourceforge.net/forum/viewtopic.php?f=1&t=1&e=1&view=unread#p1972
 * http://sourceforge.net/p/mobac/feature-requests/197/
 * http://geogratis.cgdi.gc.ca/geogratis/en/service/toporama.html
 * 
 */
public class CanadaToporama extends AbstractHttpMapSource {

	private static String BASE_URL = "http://wms.ess-ws.nrcan.gc.ca/wms/toporama_en?"
			+ "service=wms&request=GetMap&version=1.1.1&format=image/png&"
			+ "srs=epsg:4326&layers=WMS-Toporama&width=256&height=256&bbox=";

	public CanadaToporama() {
		super("CanadaToporama", 2, 18, TileImageType.PNG, HttpMapSource.TileUpdate.None);
	}

	private double tile2lon(int x, int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

	private double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}

	@Override
	public String getTileUrl(int zoom, int x, int y) {

		double north = tile2lat(y, zoom);
		double south = tile2lat(y + 1, zoom);
		double west = tile2lon(x, zoom);
		double east = tile2lon(x + 1, zoom);

		return BASE_URL + west + "," + south + "," + east + "," + north;

	}

	@Override
	public String toString() {
		return "Canada Toporama";
	}

}
