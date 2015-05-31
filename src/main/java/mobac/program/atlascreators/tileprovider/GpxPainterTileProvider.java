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
package mobac.program.atlascreators.tileprovider;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mobac.data.gpx.gpx11.Gpx;
import mobac.data.gpx.gpx11.TrkType;
import mobac.data.gpx.gpx11.TrksegType;
import mobac.data.gpx.gpx11.WptType;
import mobac.data.gpx.interfaces.GpxPoint;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.model.TileImageFormat;

/**
 * 
 * Incomplete!
 * 
 * TODO: Fully implement this class so that the content (points, tracks, ...) can be painted on each tile. If the
 * implementation is complete the {@link GpxPainterTileProvider} can be chained into the tile provider chain after the
 * {@link DownloadedTileProvider} (see AtlasThread line ~348).
 * 
 * Problem: texts and lines that span multiple tiles.
 * 
 */
public class GpxPainterTileProvider extends ConvertedRawTileProvider {

	private final MapSpace mapSpace;
	private final int zoom;

	private List<Point> points = new ArrayList<Point>();
	private List<Line> lines = new ArrayList<Line>();

	public GpxPainterTileProvider(MapSourceProvider tileProvider, TileImageFormat tileImageFormat, Gpx gpx) {
		super(tileProvider, tileImageFormat);
		zoom = tileProvider.getZoom();
		MapSource mapSource = tileProvider.getMapSource();
		mapSpace = mapSource.getMapSpace();

		// TODO Prepare GPX points
		for (TrkType trk : gpx.getTrk()) {
			for (TrksegType trkSeg : trk.getTrkseg()) {
				List<WptType> trackPoints = trkSeg.getTrkpt();

				if (trackPoints.size() < 2)
					continue;
				Point last = convert(trackPoints.get(0));
				points.add(last);
				for (int i = 1; i < trackPoints.size(); i++) {
					Point current = convert(trackPoints.get(i));
					points.add(current);
					lines.add(new Line(last, current));
					last = current;
				}
			}
		}
	}

	private Point convert(GpxPoint gpxPoint) {
		int x = mapSpace.cLonToX(gpxPoint.getLon().doubleValue(), zoom);
		int y = mapSpace.cLatToY(gpxPoint.getLat().doubleValue(), zoom);
		return new Point(x, y);
	}

	@Override
	public BufferedImage getTileImage(int x, int y) throws IOException {
		BufferedImage image = super.getTileImage(x, y);

		// Calculate tile bounds:
		final int tileSize = mapSpace.getTileSize();
		int xMin = tileSize * x;
		int yMin = tileSize * y;
		int xMax = xMin + tileSize - 1;
		int yMax = yMin + tileSize - 1;

		Graphics2D g = (Graphics2D) image.getGraphics();
		try {
			for (Point p : points) {
				if (p.x < xMin || p.x > xMax || p.y < yMin || p.y > yMax)
					continue; // Point is outside of tile
				int px = p.x - xMin;
				int py = p.y - yMin;
				g.drawOval(px, py, 5, 5);
			}
			// TODO paint lines
		} finally {
			g.dispose();
		}

		return image;
	}

	public static class Line {
		public final Point start;
		public final Point end;

		public Line(Point start, Point end) {
			super();
			this.start = start;
			this.end = end;
		}

	}
}
