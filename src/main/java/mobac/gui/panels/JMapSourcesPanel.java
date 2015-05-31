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
package mobac.gui.panels;

import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import mobac.gui.components.JCollapsiblePanel;
import mobac.gui.components.JMapSourceTree;
import mobac.program.interfaces.MapSource;
import mobac.utilities.GBC;
import mobac.utilities.I18nUtils;

public class JMapSourcesPanel extends JCollapsiblePanel {

	private static final long serialVersionUID = 1L;
	protected JLabel mapSourceLabel;

	// initialMapSourceLabel can't be empty to let the mapSourceLabel initialize with a proper height
	protected static final String initialMapSourceLabel = " ";

	protected final String plainTitle;

	public JMapSourcesPanel(JMapSourceTree mapSourceTree) {
		super(I18nUtils.localizedStringForKey("lp_map_source_title"), new GridBagLayout());
		plainTitle = getTitle();

		JScrollPane mapSourceTreeScrollPane = new JScrollPane(mapSourceTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		mapSourceTreeScrollPane.setPreferredSize(new Dimension(100, 200));
		mapSourceTreeScrollPane.setAutoscrolls(true);
		addContent(mapSourceTreeScrollPane, GBC.eol().fill().insets(0, 1, 0, 0));
	}

	@Override
	protected void fillTitlePanel() {
		super.fillTitlePanel();
		mapSourceLabel = new JLabel(initialMapSourceLabel);
		mapSourceLabel.addMouseListener(collapsingMouseListener);
		titlePanel.add(mapSourceLabel, GBC.std());
		titlePanel.revalidate();
	}

	public void setMapSourceLabel(MapSource mapSource) {
		String mapSourceString = mapSource.toString();
		if (mapSourceString == null) {
			mapSourceString = "";
		}

		mapSourceLabel.setText(mapSourceString);
		mapSourceLabel.setToolTipText(JMapSourceTree.generateMapSourceTooltip(mapSource));
		mapSourceLabel.setVisible(!mapSourceString.isEmpty());
	}
}
