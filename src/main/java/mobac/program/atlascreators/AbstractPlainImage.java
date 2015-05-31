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
package mobac.program.atlascreators;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import mobac.exceptions.AtlasTestException;
import mobac.exceptions.MapCreationException;
import mobac.mapsources.mapspace.MercatorPower2MapSpace;
import mobac.program.interfaces.LayerInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.interfaces.MapSpace.ProjectionCategory;
import mobac.utilities.Utilities;

public abstract class AbstractPlainImage extends AtlasCreator {

	@Override
	public boolean testMapSource(MapSource mapSource) {
		MapSpace mapSpace = mapSource.getMapSpace();
		return (mapSpace instanceof MercatorPower2MapSpace && ProjectionCategory.SPHERE.equals(mapSpace
				.getProjectionCategory()));
	}

	@Override
	protected void testAtlas() throws AtlasTestException {
		Runtime r = Runtime.getRuntime();
		long heapMaxSize = r.maxMemory();
		int maxMapSize = (int) (Math.sqrt(heapMaxSize / 3d) * 0.8); // reduce maximum by 20%
		maxMapSize = (maxMapSize / 100) * 100; // round by 100;
		for (LayerInterface layer : atlas) {
			for (MapInterface map : layer) {
				int w = map.getMaxTileCoordinate().x - map.getMinTileCoordinate().x;
				int h = map.getMaxTileCoordinate().y - map.getMinTileCoordinate().y;
				if (w > maxMapSize || h > maxMapSize)
					throw new AtlasTestException("Map size too large for memory (is: " + Math.max(w, h) + " max:  "
							+ maxMapSize + ")", map);
			}
		}
	}

	@Override
	public void createMap() throws MapCreationException, InterruptedException {
		try {
			createImage();
		} catch (InterruptedException e) {
			throw e;
		} catch (MapCreationException e) {
			throw e;
		} catch (Exception e) {
			throw new MapCreationException(map, e);
		}
	}

	/**
	 * @return maximum image height and width. In case an image is larger it will be scaled to fit.
	 */
	protected int getMaxImageSize() {
		return Integer.MAX_VALUE;
	}

	protected int getBufferedImageType() {
		return BufferedImage.TYPE_4BYTE_ABGR;
	}

	protected void createImage() throws InterruptedException, MapCreationException {

		atlasProgress.initMapCreation((xMax - xMin + 1) * (yMax - yMin + 1));
		ImageIO.setUseCache(false);

		int mapWidth = (xMax - xMin + 1) * tileSize;
		int mapHeight = (yMax - yMin + 1) * tileSize;

		int maxImageSize = getMaxImageSize();
		int imageWidth = Math.min(maxImageSize, mapWidth);
		int imageHeight = Math.min(maxImageSize, mapHeight);

		int len = Math.max(mapWidth, mapHeight);
		double scaleFactor = 1.0;
		boolean scaleImage = (len > maxImageSize);
		if (scaleImage) {
			scaleFactor = (double) getMaxImageSize() / (double) len;
			if (mapWidth != mapHeight) {
				// Map is not rectangle -> adapt height or width
				if (mapWidth > mapHeight)
					imageHeight = (int) (scaleFactor * mapHeight);
				else
					imageWidth = (int) (scaleFactor * mapWidth);
			}
		}
		if (imageHeight < 0 || imageWidth < 0)
			throw new MapCreationException("Invalid map size: (width/height: " + imageWidth + "/" + imageHeight + ")",
					map);
		long imageSize = 3l * ((long) imageWidth) * ((long) imageHeight);
		if (imageSize > Integer.MAX_VALUE)
			throw new MapCreationException("Map image too large: (width/height: " + imageWidth + "/" + imageHeight
					+ ") - reduce the map size and try again", map);
		BufferedImage tileImage = Utilities.safeCreateBufferedImage(imageWidth, imageHeight,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D graphics = tileImage.createGraphics();
		try {
			if (scaleImage) {
				graphics.setTransform(AffineTransform.getScaleInstance(scaleFactor, scaleFactor));
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			}
			int lineY = 0;
			for (int y = yMin; y <= yMax; y++) {
				int lineX = 0;
				for (int x = xMin; x <= xMax; x++) {
					checkUserAbort();
					atlasProgress.incMapCreationProgress();
					try {
						byte[] sourceTileData = mapDlTileProvider.getTileData(x, y);
						if (sourceTileData != null) {
							BufferedImage tile = ImageIO.read(new ByteArrayInputStream(sourceTileData));
							graphics.drawImage(tile, lineX, lineY, tileSize, tileSize, Color.WHITE, null);
						}
					} catch (IOException e) {
						log.error("", e);
					}
					lineX += tileSize;
				}
				lineY += tileSize;
			}
		} finally {
			graphics.dispose();
		}
		writeTileImage(tileImage);
	}

	protected abstract void writeTileImage(BufferedImage tileImage) throws MapCreationException;

}
