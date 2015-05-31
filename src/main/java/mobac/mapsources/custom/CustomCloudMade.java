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
package mobac.mapsources.custom;

import javax.swing.JOptionPane;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import mobac.mapsources.mappacks.openstreetmap.CloudMade;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.WrappedMapSource;
import mobac.utilities.I18nUtils;

/**
 * Requires the OpenStreetMap
 * 
 * 
 */
@XmlRootElement(name = "cloudMade")
public class CustomCloudMade implements WrappedMapSource {

	private static boolean ERROR = false;

	public static Class<CloudMade> CLOUD_MADE_CLASS = null;

	@XmlElement
	public String styleID;

	@XmlElement
	public String displayName;

	public MapSource getMapSource() {
		if (!ERROR) {
			JOptionPane.showMessageDialog(null, "CloudMade map sources are no longer supported",
					I18nUtils.localizedStringForKey("Error"), JOptionPane.ERROR_MESSAGE);
			ERROR = true;
		}
		return null;
	}

}
