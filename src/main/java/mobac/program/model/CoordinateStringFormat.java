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
package mobac.program.model;

import java.text.NumberFormat;

import mobac.utilities.I18nUtils;
import mobac.utilities.Utilities;
import mobac.utilities.geo.CoordinateDm2Format;
import mobac.utilities.geo.CoordinateDms2Format;
import mobac.utilities.geo.CoordinateTileFormat;

public enum CoordinateStringFormat {

	DEG_ENG(Utilities.FORMAT_6_DEC_ENG), //
	DEG_LOCAL(Utilities.FORMAT_6_DEC), //
	DEG_MIN_ENG(new CoordinateDm2Format(Utilities.DFS_ENG)), //
	DEG_MIN_LOCAL(new CoordinateDm2Format(Utilities.DFS_LOCAL)), //
	DEG_MIN_SEC_ENG(new CoordinateDms2Format(Utilities.DFS_ENG)), //
	DEG_MIN_SEC_LOCAL(new CoordinateDms2Format(Utilities.DFS_LOCAL)), //
	TILE_X_Y_Z(new CoordinateTileFormat(false), new CoordinateTileFormat(true));

	/*
	 * formatButton.addDropDownItem(new JNumberFormatMenuItem()); formatButton.addDropDownItem(new
	 * JNumberFormatMenuItem("Deg Min Sec,2 (local)",
	 */
	//private final String displayName;
	private NumberFormat numberFormatLatitude;
	private NumberFormat numberFormatLongitude;

	private CoordinateStringFormat(NumberFormat numberFormat) {
		//this.displayName = displayName;
		this.numberFormatLatitude = numberFormat;
		this.numberFormatLongitude = numberFormat;
	}

	private CoordinateStringFormat(NumberFormat numberFormatLatitude,
			NumberFormat numberFormatLongitude) {
		//this.displayName = displayName;
		this.numberFormatLatitude = numberFormatLatitude;
		this.numberFormatLongitude = numberFormatLongitude;
	}

//	public String getDisplayName() {
//		return this.toString();
//	}

	public NumberFormat getNumberFormatLatitude() {
		return numberFormatLatitude;
	}

	public NumberFormat getNumberFormatLongitude() {
		return numberFormatLongitude;
	}

	@Override
	public String toString() {
		//return displayName;
		switch(this)
		{
			case DEG_ENG:return I18nUtils.localizedStringForKey("lp_coords_fmt_degree_eng");
			case DEG_LOCAL:return I18nUtils.localizedStringForKey("lp_coords_fmt_degree_local");
			case DEG_MIN_ENG:return I18nUtils.localizedStringForKey("lp_coords_fmt_degree_min_eng");
			case DEG_MIN_LOCAL:return I18nUtils.localizedStringForKey("lp_coords_fmt_degree_min_local");
			case DEG_MIN_SEC_ENG:return I18nUtils.localizedStringForKey("lp_coords_fmt_degree_min_sec_eng");
			case DEG_MIN_SEC_LOCAL:return I18nUtils.localizedStringForKey("lp_coords_fmt_degree_min_sec_local");
			case TILE_X_Y_Z:return I18nUtils.localizedStringForKey("lp_coords_fmt_tile");
		}
		return I18nUtils.localizedStringForKey("Undefined");
	}

}
