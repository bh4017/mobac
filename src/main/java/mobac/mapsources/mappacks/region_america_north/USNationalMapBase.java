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
import mobac.program.model.TileImageType;

/**
 * http://viewer.nationalmap.gov/example/services.html
 */
public class USNationalMapBase extends AbstractHttpMapSource {

	public USNationalMapBase() {
		super("USGS National Map Base", 0, 15, TileImageType.JPG, TileUpdate.ETag);
	}

	public String getTileUrl(int zoom, int x, int y) {
		return "http://basemap.nationalmap.gov/ArcGIS/rest/services/USGSTopo/MapServer/tile/" + zoom + "/" + y + "/"
				+ x;
	}

	// http://basemap.nationalmap.gov/ArcGIS/rest/services/USGSTopo/MapServer/tile/6/23/11
	// http://basemap.nationalmap.gov/ArcGIS/rest/services/USGSImageryOnly/MapServer/tile/4/7/3
	// http://basemap.nationalmap.gov/ArcGIS/rest/services/TNM_Vector_Fills_Small/MapServer/tile/4/6/1
	// http://basemap.nationalmap.gov/ArcGIS/rest/services/TNM_Vector_Small/MapServer/tile/4/6/2
	// http://raster1.nationalmap.gov/ArcGIS/rest/services/TNM_Small_Scale_Shaded_Relief/MapServer/tile/4/7/6

}