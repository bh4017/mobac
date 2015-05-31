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
/**
 * 
 */
package mobac.mapsources.mappacks.openstreetmap;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import mobac.exceptions.TileException;
import mobac.mapsources.AbstractMultiLayerMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.model.TileImageType;

public class Hikebikemap extends AbstractMultiLayerMapSource {

	private static final String[] SERVERS = { "a", "b", "c" };
	private static int SERVER_NUM = 0;
	private static final Color BACKGROUND = new Color(180, 180, 180);

	public Hikebikemap() {
		super("OpenStreetMap Hikebikemap.de", TileImageType.PNG);
		mapSources = new MapSource[] { new HikebikemapBase(), new HikebikemapRelief() };
		initializeValues();
	}

	/**
	 * http://hikebikemap.de/
	 */
	public static class HikebikemapBase extends AbstractOsmMapSource {

		public HikebikemapBase() {
			super("HikebikemapTiles");
			maxZoom = 17;
			tileUpdate = TileUpdate.IfNoneMatch;
		}

		@Override
		public String toString() {
			return "OpenStreetMap Hikebikemap Map";
		}

		public String getTileUrl(int zoom, int tilex, int tiley) {
			String server = SERVERS[SERVER_NUM];
			SERVER_NUM = (SERVER_NUM + 1) % SERVERS.length;
			return String.format("http://%s.tiles.wmflabs.org/hikebike/%d/%d/%d.png", server, zoom, tilex, tiley);
		}

	}

	/**
	 * Hill shades / relief
	 * 
	 * http://hikebikemap.de/
	 */
	public static class HikebikemapRelief extends AbstractOsmMapSource {

		public HikebikemapRelief() {
			super("HikebikemapRelief");
			maxZoom = 17;
			tileUpdate = TileUpdate.IfNoneMatch;
		}

		public String getTileUrl(int zoom, int tilex, int tiley) {
			String server = SERVERS[SERVER_NUM];
			SERVER_NUM = (SERVER_NUM + 1) % SERVERS.length;
			return String.format("http://%s.tiles.wmflabs.org/hillshading/%d/%d/%d.png", server, zoom, tilex, tiley);
		}

		@Override
		public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException,
				TileException, InterruptedException {
			if (zoom > 16)
				return null;
			return super.getTileImage(zoom, x, y, loadMethod);
		}

	}

	@Override
	public Color getBackgroundColor() {
		return BACKGROUND;
	}

}