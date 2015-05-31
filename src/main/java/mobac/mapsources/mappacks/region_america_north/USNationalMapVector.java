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
/**
 * 
 */
package mobac.mapsources.mappacks.region_america_north;

import java.awt.Color;

import mobac.mapsources.AbstractMultiLayerMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.model.TileImageType;

public class USNationalMapVector extends AbstractMultiLayerMapSource {

	public USNationalMapVector() {
		super("USGS National Map Vector", TileImageType.PNG);
		mapSources = new MapSource[] { new USNationalMapVFS(), new USNationalMapVS() };
		initializeValues();
	}

	@Override
	public Color getBackgroundColor() {
		return Color.WHITE;
	}

}