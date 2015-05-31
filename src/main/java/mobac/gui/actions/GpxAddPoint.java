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
package mobac.gui.actions;

import java.awt.event.ActionEvent;  
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import mobac.gui.MainGUI;
import mobac.gui.gpxtree.GpxEntry;
import mobac.gui.mapview.PreviewMap;
import mobac.gui.mapview.controller.GpxMapController;
import mobac.gui.panels.JGpxPanel;
import mobac.utilities.I18nUtils;



public class GpxAddPoint implements ActionListener {

	JGpxPanel panel;
	
	private GpxMapController mapController = null;

	public GpxAddPoint(JGpxPanel panel) {
		super();
		this.panel = panel;
	}

	public synchronized void actionPerformed(ActionEvent event) {
		GpxEntry entry = panel.getSelectedEntry();
		if (entry == null) {
			int answer = JOptionPane.showConfirmDialog(null,
					I18nUtils.localizedStringForKey("rp_gpx_msg_ask_create_new"),
					I18nUtils.localizedStringForKey("rp_gpx_msg_ask_create_new_title"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (answer != JOptionPane.YES_OPTION)
				return;
			entry = new GpxNew(panel).newGpx();
		}
		
		if (!entry.isWaypointParent()) {
			JOptionPane.showMessageDialog(null, 
					I18nUtils.localizedStringForKey("rp_gpx_msg_add_point_failed"), 
					I18nUtils.localizedStringForKey("Error"), 
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		PreviewMap map = MainGUI.getMainGUI().previewMap;
		map.getMapSelectionController().disable();
		if (mapController == null)
			mapController = new GpxMapController(map, panel, false);
		mapController.enable();
	}
}
