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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import mobac.gui.components.JCollapsiblePanel;
import mobac.gui.mapview.PreviewMap;
import mobac.gui.mapview.controller.JMapController;
import mobac.gui.mapview.interfaces.MapEventListener;
import mobac.gui.mapview.layer.TileStoreCoverageLayer;
import mobac.mapsources.AbstractMultiLayerMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.model.MercatorPixelCoordinate;
import mobac.utilities.GBC;
import mobac.utilities.I18nUtils;

public class JTileStoreCoveragePanel extends JCollapsiblePanel implements MapEventListener, ActionListener {

	//MP-add
	private static final long serialVersionUID = 1L;
	
	private JButton showCoverage;
	private JButton hideCoverage;
	private JComboBox layerSelector;
	private JComboBox zoomCombo;
	private PreviewMap mapViewer;

	public JTileStoreCoveragePanel(PreviewMap mapViewer, int layerSelectorWidth) {
		super(I18nUtils.localizedStringForKey("lp_tile_store_title"));
		contentContainer.setLayout(new GridBagLayout());
		this.mapViewer = mapViewer;

		showCoverage = new JButton(I18nUtils.localizedStringForKey("lp_tile_store_show_coverage_btn_title"));
		showCoverage.addActionListener(this);
		showCoverage.setToolTipText(I18nUtils.localizedStringForKey("lp_tile_store_show_coverage_btn_tips"));
		hideCoverage = new JButton(I18nUtils.localizedStringForKey("lp_tile_store_hide_coverage_btn_title"));
		hideCoverage.addActionListener(this);
		hideCoverage.setEnabled(false);
		zoomCombo = new JComboBox();
		zoomCombo.setToolTipText(I18nUtils.localizedStringForKey("lp_tile_store_zoom_combo_tips"));
		titlePanel.setToolTipText(I18nUtils.localizedStringForKey("lp_tile_store_title_tips"));
		layerSelector = new JComboBox();
		layerSelector.setPreferredSize(new Dimension(layerSelectorWidth, layerSelector.getPreferredSize().height));

		GBC gbc_eol = GBC.eol().insets(2, 2, 2, 2);
		GBC gbc_std = GBC.std().insets(2, 2, 2, 2);

		contentContainer.add(new JLabel(I18nUtils.localizedStringForKey("lp_tile_store_zoom_title")), gbc_std);
		contentContainer.add(zoomCombo, gbc_eol);
		contentContainer.add(new JLabel(I18nUtils.localizedStringForKey("lp_tile_store_layer_title")), gbc_std);
		contentContainer.add(layerSelector, gbc_eol);
		contentContainer.add(showCoverage, gbc_eol.fillH());
		contentContainer.add(hideCoverage, gbc_eol.fillH());
		mapSourceChanged(mapViewer.getMapSource());
		mapViewer.addMapEventListener(this);
	}

	public void actionPerformed(ActionEvent e) {
		if (hideCoverage.equals(e.getSource())) {
			TileStoreCoverageLayer.removeCacheCoverageLayers();
			mapViewer.repaint();
			hideCoverage.setEnabled(false);
			return;
		}
		Integer zoom = (Integer) zoomCombo.getSelectedItem();
		if (zoom == null)
			return;
		TileStoreCoverageLayer.removeCacheCoverageLayers();
		mapViewer.repaint();
		TileStoreCoverageLayer tscl = new TileStoreCoverageLayer(mapViewer,
				(MapSource) layerSelector.getSelectedItem(), zoom);
		mapViewer.mapLayers.add(tscl);
		hideCoverage.setEnabled(true);
	}

	public void gridZoomChanged(int newGridZoomLevel) {
	}

	public void mapSourceChanged(MapSource newMapSource) {
		TileStoreCoverageLayer.removeCacheCoverageLayers();
		hideCoverage.setEnabled(false);
		Integer selZoom = (Integer) zoomCombo.getSelectedItem();
		if (selZoom == null)
			selZoom = new Integer(8);
		int zoomLevels = Math.max(0, newMapSource.getMaxZoom() - newMapSource.getMinZoom() + 1);
		Integer[] items = new Integer[zoomLevels];
		int zoom = newMapSource.getMinZoom();
		for (int i = 0; i < items.length; i++) {
			items[i] = new Integer(zoom++);
		}
		zoomCombo.setModel(new DefaultComboBoxModel(items));
		zoomCombo.setMaximumRowCount(10);
		zoomCombo.setSelectedItem(selZoom);
		MapSource[] layers;
		if (newMapSource instanceof AbstractMultiLayerMapSource) {
			layers = ((AbstractMultiLayerMapSource) newMapSource).getLayerMapSources();
			layerSelector.setEnabled(true);
		} else {
			layers = new MapSource[] { newMapSource };
			layerSelector.setEnabled(false);
		}
		layerSelector.setModel(new DefaultComboBoxModel(layers));
		layerSelector.setSelectedIndex(0);
	}

	public void selectNextMapSource() {
	}

	public void selectPreviousMapSource() {
	}

	public void selectionChanged(MercatorPixelCoordinate max, MercatorPixelCoordinate min) {
	}

	public void zoomChanged(int newZoomLevel) {
	}

	public void mapSelectionControllerChanged(JMapController newMapController) {
	}

}
