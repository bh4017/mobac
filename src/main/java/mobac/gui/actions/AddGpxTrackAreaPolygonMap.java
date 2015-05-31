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

import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JOptionPane;

import mobac.data.gpx.gpx11.TrkType;
import mobac.data.gpx.gpx11.TrksegType;
import mobac.data.gpx.interfaces.GpxPoint;
import mobac.exceptions.InvalidNameException;
import mobac.gui.MainGUI;
import mobac.gui.atlastree.JAtlasTree;
import mobac.gui.gpxtree.GpxEntry;
import mobac.gui.gpxtree.GpxRootEntry;
import mobac.gui.gpxtree.TrkEntry;
import mobac.gui.gpxtree.TrksegEntry;
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
import mobac.utilities.I18nUtils;

public class AddGpxTrackAreaPolygonMap implements ActionListener {

	public static final AddGpxTrackAreaPolygonMap INSTANCE = new AddGpxTrackAreaPolygonMap();

	public void actionPerformed(ActionEvent event) {
		MainGUI mg = MainGUI.getMainGUI();
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
		MapSource mapSource = mg.getSelectedMapSource();
		SelectedZoomLevels sZL = mg.getSelectedZoomLevels();
		int[] zoomLevels = sZL.getZoomLevels();
		if (zoomLevels.length == 0) {
			JOptionPane.showMessageDialog(mg, I18nUtils.localizedStringForKey("msg_no_zoom_level_selected"));
			return;
		}
		List<? extends GpxPoint> points = trk.getTrkpt();
		EastNorthCoordinate[] trackPoints = new EastNorthCoordinate[points.size()];
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

		TileImageParameters customTileParameters = mg.getSelectedTileImageParameters();

		int[] xPoints = new int[trackPoints.length];
		int[] yPoints = new int[trackPoints.length];
		for (int i = 0; i < trackPoints.length; i++) {
			EastNorthCoordinate coord = trackPoints[i];
			xPoints[i] = mapSpace.cLonToX(coord.lon, maxZoom);
			yPoints[i] = mapSpace.cLatToY(coord.lat, maxZoom);
		}

		Polygon p = new Polygon(xPoints, yPoints, xPoints.length);
		MapPolygon maxZoomMap = new MapPolygon(null, "Dummy", mapSource, maxZoom, p, customTileParameters);

		int width = maxZoomMap.getMaxTileCoordinate().x - maxZoomMap.getMinTileCoordinate().x;
		int height = maxZoomMap.getMaxTileCoordinate().y - maxZoomMap.getMinTileCoordinate().y;
		if (Math.max(width, height) > Settings.getInstance().maxMapSize) {
			String msg = I18nUtils.localizedStringForKey("msg_add_gpx_polygon_maxsize");
			int result = JOptionPane.showConfirmDialog(mg, msg,
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
