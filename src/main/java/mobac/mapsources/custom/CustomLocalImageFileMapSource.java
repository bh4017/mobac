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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import mobac.exceptions.TileException;
import mobac.gui.mapview.PreviewMap;
import mobac.mapsources.mapspace.MapSpaceFactory;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.jaxb.ColorAdapter;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;
import mobac.utilities.I18nUtils;

import org.apache.log4j.Logger;

@XmlRootElement(name = "localImageFile")
public class CustomLocalImageFileMapSource implements FileBasedMapSource {

	private static final Logger log = Logger.getLogger(CustomLocalImageFileMapSource.class);

	private MapSourceLoaderInfo loaderInfo = null;

	@XmlElement(required = true, nillable = false)
	private double boxNorth = 90.0;

	@XmlElement(required = true, nillable = false)
	private double boxSouth = -90.0;

	@XmlElement(required = true, nillable = false)
	private double boxEast = 180.0;

	@XmlElement(required = true, nillable = false)
	private double boxWest = -180.0;

	private MapSpace mapSpace = MapSpaceFactory.getInstance(256, true);

	private boolean initialized = false;

	BufferedImage fullImage = null;

	private TileImageType tileImageType = null;

	@XmlElement(nillable = false, defaultValue = "CustomImage")
	private String name = "Custom";

	@XmlElement(nillable = false, defaultValue = "0")
	private int minZoom = PreviewMap.MIN_ZOOM;

	@XmlElement(nillable = false, defaultValue = "20")
	private int maxZoom = PreviewMap.MAX_ZOOM;

	@XmlElement(required = true)
	private File imageFile = null;

	@XmlElement(nillable = false, defaultValue = "false")
	private boolean retinaDisplay = false;

	// @XmlElement()
	// private CustomMapSourceType sourceType = CustomMapSourceType.DIR_ZOOM_X_Y;

	@XmlElement(defaultValue = "false")
	private boolean invertYCoordinate = false;

	@XmlElement(defaultValue = "#00000000")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	private Color backgroundColor = Color.BLACK;

	public CustomLocalImageFileMapSource() {
		super();
	}

	public synchronized void initialize() {
		if (initialized)
			return;
		reinitialize();
	}

	public void reinitialize() {
		try {
			if (!imageFile.isFile()) {
				JOptionPane.showMessageDialog(null, String.format(
						I18nUtils.localizedStringForKey("msg_environment_invalid_source_folder"), name,
						imageFile.toString()), I18nUtils
						.localizedStringForKey("msg_environment_invalid_source_folder_title"),
						JOptionPane.ERROR_MESSAGE);
				initialized = true;
				return;
			}
			String[] parts = imageFile.getName().split("\\.");
			if (parts.length >= 2) {
				tileImageType = TileImageType.getTileImageType(parts[parts.length - 1]);
			} else {
				tileImageType = TileImageType.PNG;
			}

			boxWest = Math.min(boxEast, boxWest);
			boxEast = Math.max(boxEast, boxWest);
			boxSouth = Math.min(boxNorth, boxSouth);
			boxNorth = Math.max(boxNorth, boxSouth);

		} finally {
			initialized = true;
		}
	}

	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException,
			InterruptedException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream(16000);
		BufferedImage image = getTileImage(zoom, x, y, loadMethod);
		if (image == null)
			return null;
		ImageIO.write(image, tileImageType.getFileExt(), buf);
		return buf.toByteArray();
	}

	// integer nearest to zero
	private int absFloor(double value) {
		return value > 0 ? (int) Math.floor(value) : (int) Math.ceil(value);
	}

	// integer farest from zero
	private int absCeil(double value) {
		return value > 0 ? (int) Math.ceil(value) : (int) Math.floor(value);
	}

	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException,
			InterruptedException {
		if (!initialized)
			initialize();

		if (log.isTraceEnabled())
			log.trace(String.format("Loading tile z=%d x=%d y=%d", zoom, x, y));

		BufferedImage image = null;
		Graphics2D g2 = null;

		try {
			if (fullImage == null) {
				fullImage = ImageIO.read(imageFile);
			}
			int imageWidth = fullImage.getWidth();
			int imageHeight = fullImage.getHeight();
			int tileSize = mapSpace.getTileSize();
			double tileWest = mapSpace.cXToLon(x * tileSize, zoom);
			double tileNorth = mapSpace.cYToLat(y * tileSize, zoom);
			double tileEast = mapSpace.cXToLon((x + 1) * tileSize, zoom);
			double tileSouth = mapSpace.cYToLat((y + 1) * tileSize, zoom);
			double tileWidth = tileEast - tileWest;
			double tileHeight = tileNorth - tileSouth;

			// intersects coordinate region
			double intersectWest = Math.max(tileWest, boxWest);
			double intersectEast = Math.min(tileEast, boxEast);
			double intersectNorth = Math.min(tileNorth, boxNorth);
			double intersectSouth = Math.max(tileSouth, boxSouth);
			double intersectWidth = intersectEast - intersectWest;
			double intersectHeight = intersectNorth - intersectSouth;

			// intersects
			if (intersectWidth > 0 && intersectHeight > 0) {
				int graphContextSize = tileSize * (retinaDisplay ? 2 : 1);
				image = new BufferedImage(graphContextSize, graphContextSize, BufferedImage.TYPE_4BYTE_ABGR);
				g2 = image.createGraphics();
				g2.setColor(getBackgroundColor());
				g2.fillRect(0, 0, graphContextSize, graphContextSize);

				double boxWidth = (boxEast - boxWest);
				double boxHeight = (boxNorth - boxSouth);

				// crop parameters
				double cropWScale = (boxWidth <= 0) ? 0 : (imageWidth / boxWidth);
				double cropHScale = (boxHeight <= 0) ? 0 : (imageHeight / boxHeight);
				int cropW = absCeil(intersectWidth * cropWScale);
				int cropH = absCeil(intersectHeight * cropHScale);
				int cropX = absFloor((intersectWest - boxWest) * cropWScale);
				// int cropY = imageHeight - absCeil((boxNorth - intersectNorth) * cropHScale) - cropH;
				int cropY = absFloor((boxNorth - intersectNorth) * cropHScale);
				// skip when no valid crop
				if (cropX < imageWidth && cropY < imageHeight && (cropX + cropW) > 0 && (cropY + cropH) > 0) {
					// draw rect
					double drawrectWScale = (tileWidth <= 0) ? 0 : (graphContextSize / tileWidth);
					double drawrectHScale = (tileHeight <= 0) ? 0 : (graphContextSize / tileHeight);
					int drawrectW = absCeil(intersectWidth * drawrectWScale);
					int drawrectH = absCeil(intersectHeight * drawrectHScale);
					int drawrectX = absFloor((intersectWest - tileWest) * drawrectWScale);
					// int drawrectY = tileSize - absCeil((tileNorth - intersectNorth) * drawrectHScale) - drawrectH;
					int drawrectY = absFloor((tileNorth - intersectNorth) * drawrectHScale);

					// skip when draw rectangle totally draw out of image
					if (drawrectX < graphContextSize && drawrectY < graphContextSize && (drawrectX + drawrectW) > 1
							&& (drawrectY + drawrectH) > 1) {
						g2.drawImage(fullImage, drawrectX, drawrectY, drawrectX + drawrectW, drawrectY + drawrectH,
								cropX, cropY, cropX + cropW, cropY + cropH, null);
					}
				}

			}
		} catch (FileNotFoundException e) {
			log.debug("Map image file not found: " + imageFile.getAbsolutePath());
		} finally {
			if (g2 != null) {
				g2.dispose();
			}
		}
		return image;
	}

	public TileImageType getTileImageType() {
		return tileImageType;
	}

	public int getMaxZoom() {
		return maxZoom;
	}

	public int getMinZoom() {
		return minZoom;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public MapSpace getMapSpace() {
		return mapSpace;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	@XmlTransient
	public MapSourceLoaderInfo getLoaderInfo() {
		return loaderInfo;
	}

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
		this.loaderInfo = loaderInfo;
	}

}
