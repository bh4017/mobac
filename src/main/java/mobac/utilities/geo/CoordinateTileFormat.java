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
package mobac.utilities.geo;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import mobac.gui.MainGUI;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;

import org.apache.log4j.Logger;

public class CoordinateTileFormat extends NumberFormat {

	protected static Logger log = Logger.getLogger(CoordinateTileFormat.class);

	private final boolean isLongitude;

	public CoordinateTileFormat(boolean isLongitude) {
		this.isLongitude = isLongitude;
	}

	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
		MainGUI gui = MainGUI.getMainGUI();
		MapSource mapSource = gui.getSelectedMapSource();
		if (mapSource == null)
			return toAppendTo;
		MapSpace mapSpace = mapSource.getMapSpace();
		int zoom = gui.previewMap.getZoom();
		int tileNum = 0;
		if (isLongitude)
			tileNum = mapSpace.cLonToX(number, zoom);
		else
			tileNum = mapSpace.cLatToY(number, zoom);
		toAppendTo.append(String.format("%d / z%d ", tileNum / mapSpace.getTileSize(), zoom));
		return toAppendTo;
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Number parse(String source, ParsePosition parsePosition) {
		MainGUI gui = MainGUI.getMainGUI();
		MapSpace mapSpace = gui.getSelectedMapSource().getMapSpace();
		try {
			String[] tokens = source.trim().split("/");
			int zoom = 0;
			int tileNum = 0;
			if (tokens.length == 2) {
				String s = tokens[1].trim();
				if (s.startsWith("z"))
					s = s.substring(1);
				zoom = Integer.parseInt(s);
			} else {
				zoom = gui.previewMap.getZoom();
			}
			if (tokens.length > 0) {
				String s = tokens[0];
				s = s.trim();
				if ((s.indexOf('.') < 0) && (s.indexOf(',') < 0)) {
					tileNum = Integer.parseInt(s);
					tileNum *= mapSpace.getTileSize();
				} else {
					double num = Double.parseDouble(s);
					tileNum = (int) (num * mapSpace.getTileSize());
				}
			}
			parsePosition.setIndex(source.length());
			if (isLongitude)
				return mapSpace.cXToLon(tileNum, zoom);
			return mapSpace.cYToLat(tileNum, zoom);
		} catch (Exception e) {
			parsePosition.setErrorIndex(0);
			log.error("e");
			return null;
		}
	}
}
