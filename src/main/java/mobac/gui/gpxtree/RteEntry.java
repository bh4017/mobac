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
package mobac.gui.gpxtree;

import mobac.data.gpx.gpx11.RteType;
import mobac.gui.mapview.layer.GpxLayer;
import mobac.utilities.I18nUtils;

public class RteEntry extends GpxEntry {
	private RteType rte;

	public RteEntry(RteType rte, GpxLayer layer) {
		this.setRte(rte);
		this.setLayer(layer);
		this.setWaypointParent(true);
	}

	public String toString() {
		String name = "";
		try {
			name = getRte().getName();
		} catch (NullPointerException e) {
			// no name set
		}
		if (name != null && !name.equals("")) {
			return name;
		} else {
			return I18nUtils.localizedStringForKey("rp_gpx_unname_route_name");
		}
	}

	private void setRte(RteType rte) {
		this.rte = rte;
	}

	public RteType getRte() {
		return rte;
	}
}
