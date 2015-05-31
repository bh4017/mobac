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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mobac.data.gpx.gpx11.TrkType;
import mobac.data.gpx.gpx11.TrksegType;
import mobac.data.gpx.interfaces.GpxPoint;
import mobac.exceptions.InvalidNameException;
import mobac.gui.MainGUI;
import mobac.gui.atlastree.JAtlasTree;
import mobac.gui.components.JDistanceSlider;
import mobac.gui.gpxtree.GpxEntry;
import mobac.gui.gpxtree.GpxRootEntry;
import mobac.gui.gpxtree.TrkEntry;
import mobac.gui.gpxtree.TrksegEntry;
import mobac.gui.mapview.layer.MapAreaHighlightingLayer;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.model.EastNorthCoordinate;
import mobac.program.model.Layer;
import mobac.program.model.MapPolygon;
import mobac.program.model.SelectedZoomLevels;
import mobac.program.model.Settings;
import mobac.program.model.TileImageParameters;
import mobac.program.model.UnitSystem;
import mobac.utilities.I18nUtils;

public class AddGpxTrackPolygonMap implements ActionListener {

	public static final AddGpxTrackPolygonMap INSTANCE = new AddGpxTrackPolygonMap();

	private MapAreaHighlightingLayer msl = null;

	public void actionPerformed(ActionEvent event) {
		final MainGUI mg = MainGUI.getMainGUI();
		GpxEntry entry = mg.gpxPanel.getSelectedEntry();

		if (entry == null)
			return;

		TrksegType trk = null;
		TrkType t = null;
		if (entry instanceof TrksegEntry) {
			trk = ((TrksegEntry) entry).getTrkSeg();
		} else if (entry instanceof GpxRootEntry) {
			GpxRootEntry re = (GpxRootEntry) entry;
			List<TrkType> tlist = re.getLayer().getGpx().getTrk();
			if (tlist.size() > 1) {
				JOptionPane
						.showMessageDialog(mg, I18nUtils.localizedStringForKey("msg_add_gpx_polygon_too_many_track"));
				return;
			} else if (tlist.size() == 1)
				t = tlist.get(0);
		}
		if (entry instanceof TrkEntry)
			t = ((TrkEntry) entry).getTrk();
		if (t != null) {
			if (t.getTrkseg().size() > 1) {
				JOptionPane.showMessageDialog(mg,
						I18nUtils.localizedStringForKey("msg_add_gpx_polygon_too_many_segment"));
				return;
			} else if (t.getTrkseg().size() == 1)
				trk = t.getTrkseg().get(0);
		}
		if (trk == null) {
			JOptionPane.showMessageDialog(mg, I18nUtils.localizedStringForKey("msg_add_gpx_polygon_no_select"),
					I18nUtils.localizedStringForKey("Error"), JOptionPane.ERROR_MESSAGE);
			return;
		}

		JAtlasTree jAtlasTree = mg.jAtlasTree;
		final String mapNameFmt = "%s %02d";
		AtlasInterface atlasInterface = jAtlasTree.getAtlas();
		String name = mg.getUserText();
		final MapSource mapSource = mg.getSelectedMapSource();
		SelectedZoomLevels sZL = mg.getSelectedZoomLevels();
		int[] zoomLevels = sZL.getZoomLevels();
		if (zoomLevels.length == 0) {
			JOptionPane.showMessageDialog(mg, I18nUtils.localizedStringForKey("msg_no_zoom_level_selected"));
			return;
		}
		List<? extends GpxPoint> points = trk.getTrkpt();
		final EastNorthCoordinate[] trackPoints = new EastNorthCoordinate[points.size()];
		EastNorthCoordinate minCoordinate = new EastNorthCoordinate(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		EastNorthCoordinate maxCoordinate = new EastNorthCoordinate(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
		for (int i = 0; i < trackPoints.length; i++) {
			GpxPoint gpxPoint = points.get(i);
			EastNorthCoordinate c = new EastNorthCoordinate(gpxPoint.getLat().doubleValue(), gpxPoint.getLon()
					.doubleValue());
			minCoordinate.lat = Math.min(minCoordinate.lat, c.lat);
			minCoordinate.lon = Math.min(minCoordinate.lon, c.lon);
			maxCoordinate.lat = Math.max(maxCoordinate.lat, c.lat);
			maxCoordinate.lon = Math.max(maxCoordinate.lon, c.lon);
			trackPoints[i] = c;
		}

		final int maxZoom = zoomLevels[zoomLevels.length - 1];
		final MapSpace mapSpace = mapSource.getMapSpace();
		Point p1 = maxCoordinate.toTileCoordinate(mapSpace, maxZoom);
		Point p2 = minCoordinate.toTileCoordinate(mapSpace, maxZoom);

		final int centerY = p1.y + ((p1.y - p2.y) / 2);

		final UnitSystem unitSystem = Settings.getInstance().getUnitSystem();

		final TileImageParameters customTileParameters = mg.getSelectedTileImageParameters();

		JPanel panel = new JPanel(new BorderLayout());
		panel.setPreferredSize(new Dimension(300, 100));
		final JLabel label = new JLabel("");
		final JDistanceSlider slider = new JDistanceSlider(mapSource.getMapSpace(), maxZoom, centerY, unitSystem, 5,
				500);
		ChangeListener cl = new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				double d = mapSpace.horizontalDistance(maxZoom, centerY, slider.getValue());
				d *= unitSystem.earthRadius * unitSystem.unitFactor;
				String unitName = unitSystem.unitSmall;
				if (d > unitSystem.unitFactor) {
					d /= unitSystem.unitFactor;
					unitName = unitSystem.unitLarge;
				}
				label.setText(String.format(I18nUtils.localizedStringForKey("dlg_gpx_track_select_distance"),
						((int) d), unitName));
			}
		};
		final JButton previewButton = new JButton(I18nUtils.localizedStringForKey("dlg_gpx_track_select_preview"));
		previewButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int distance = slider.getValue();
				MapPolygon maxZoomMap = MapPolygon.createTrackEnclosure(null, "Dummy", mapSource, maxZoom, trackPoints,
						distance, customTileParameters);
				if (msl != null)
					msl.setObject(maxZoomMap);
				msl = new MapAreaHighlightingLayer(maxZoomMap);
				mg.previewMap.repaint();
			}
		});

		cl.stateChanged(null);
		slider.addChangeListener(cl);
		panel.add(label, BorderLayout.NORTH);
		panel.add(slider, BorderLayout.CENTER);
		panel.add(previewButton, BorderLayout.SOUTH);

		int result = JOptionPane.showConfirmDialog(mg, panel,
				I18nUtils.localizedStringForKey("dlg_gpx_track_select_title"), JOptionPane.OK_CANCEL_OPTION);

		if (msl != null) {
			mg.previewMap.mapLayers.remove(msl);
			msl.setObject(null);
		}

		if (result != JOptionPane.OK_OPTION)
			return;

		int distance = slider.getValue();
		MapPolygon maxZoomMap = MapPolygon.createTrackEnclosure(null, "Dummy", mapSource, maxZoom, trackPoints,
				distance, customTileParameters);

		String layerName = name;
		int c = 1;
		Layer layer = null;
		boolean success = false;
		do {
			try {
				layer = new Layer(atlasInterface, layerName);
				success = true;
			} catch (InvalidNameException e) {
				layerName = name + "_" + Integer.toString(c++);
			}
		} while (!success);

		int width = maxZoomMap.getMaxTileCoordinate().x - maxZoomMap.getMinTileCoordinate().x;
		int height = maxZoomMap.getMaxTileCoordinate().y - maxZoomMap.getMinTileCoordinate().y;
		if (Math.max(width, height) > Settings.getInstance().maxMapSize) {
			String msg = I18nUtils.localizedStringForKey("msg_add_gpx_polygon_maxsize");
			result = JOptionPane.showConfirmDialog(mg, msg,
					I18nUtils.localizedStringForKey("msg_add_gpx_polygon_maxsize_title"), JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (result != JOptionPane.YES_OPTION)
				return;
		}

		for (int zoom : zoomLevels) {
			String mapName = String.format(mapNameFmt, new Object[] { layerName, zoom });
			MapInterface map = MapPolygon.createFromMapPolygon(layer, mapName, zoom, maxZoomMap);
			layer.addMap(map);
		}
		atlasInterface.addLayer(layer);
		mg.jAtlasTree.getTreeModel().notifyNodeInsert(layer);

	}
}
