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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import mobac.exceptions.MapCreationException;
import mobac.program.annotations.AtlasCreatorName;
import mobac.program.atlascreators.tileprovider.TileProvider;
import mobac.program.interfaces.LayerInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSpace;
import mobac.program.interfaces.TileImageDataWriter;
import mobac.program.tiledatawriter.TileImageJpegDataWriter;
import mobac.utilities.Utilities;
import mobac.utilities.stream.ZipStoreOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

@AtlasCreatorName(value = "Google Earth Overlay (KMZ)", type = "GoogleEarthRasterOverlay")
public class GoogleEarthOverlay extends AbstractPlainImage {

	protected File mapDir;
	protected String cleanedMapName;

	protected File kmzFile = null;
	protected ZipStoreOutputStream kmzOutputStream = null;

	private Document kmlDoc = null;
	private Element groundOverlayRoot = null;

	@Override
	public void initLayerCreation(LayerInterface layer) throws IOException {
		super.initLayerCreation(layer);
		Utilities.mkDirs(atlasDir);
		kmzFile = new File(atlasDir, layer.getName() + ".kmz");
		kmzOutputStream = new ZipStoreOutputStream(kmzFile);
		kmzOutputStream.setMethod(ZipOutputStream.STORED);
		try {
			if (layer.getMapCount() <= 1)
				initKmlDoc(null);
			else
				initKmlDoc(layer.getName());
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void finishLayerCreation() throws IOException {
		try {
			writeKmlToZip();
		} catch (Exception e) {
			throw new IOException(e);
		}
		Utilities.closeStream(kmzOutputStream);
		kmzOutputStream = null;
		kmzFile = null;
		super.finishLayerCreation();
	}

	@Override
	public void abortAtlasCreation() throws IOException {
		Utilities.closeStream(kmzOutputStream);
		kmzOutputStream = null;
		kmzFile = null;
		super.abortAtlasCreation();
	}

	@Override
	public void initializeMap(MapInterface map, TileProvider mapTileProvider) {
		super.initializeMap(map, mapTileProvider);
		mapDir = new File(atlasDir, map.getLayer().getName());
		cleanedMapName = map.getName();
		cleanedMapName = cleanedMapName.replaceAll("[[^\\p{Alnum}-_]]+", "_");
		cleanedMapName = cleanedMapName.replaceAll("_{2,}", "_");
		if (cleanedMapName.endsWith("_"))
			cleanedMapName = cleanedMapName.substring(0, cleanedMapName.length() - 1);
		if (cleanedMapName.startsWith("_"))
			cleanedMapName = cleanedMapName.substring(1, cleanedMapName.length());
	}
	
	protected int getBufferedImageType() {
		return BufferedImage.TYPE_3BYTE_BGR;
	}

	protected void writeTileImage(BufferedImage tileImage) throws MapCreationException {
		TileImageDataWriter writer;
		if (parameters != null) {
			writer = parameters.getFormat().getDataWriter();
		} else
			writer = new TileImageJpegDataWriter(0.9);
		writer.initialize();
		try {
			int initialBufferSize = tileImage.getWidth() * tileImage.getHeight() / 4;
			ByteArrayOutputStream buf = new ByteArrayOutputStream(initialBufferSize);
			writer.processImage(tileImage, buf);
			String imageFileName = "files/" + cleanedMapName + "." + writer.getType();
			kmzOutputStream.writeStoredEntry(imageFileName, buf.toByteArray());
			addMapToKmz(imageFileName);
		} catch (Exception e) {
			throw new MapCreationException(map, e);
		}
	}

	protected void addMapToKmz(String imageFileName) throws ParserConfigurationException,
			TransformerFactoryConfigurationError, TransformerException, IOException {
		int startX = xMin * tileSize;
		int endX = (xMax + 1) * tileSize;
		int startY = yMin * tileSize;
		int endY = (yMax + 1) * tileSize;
		addKmlEntry(map.getName(), imageFileName, startX, startY, endX - startX, endY - startY);
	}

	private void initKmlDoc(String folderName) throws ParserConfigurationException {

		DocumentBuilder builder;
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		kmlDoc = builder.newDocument();

		Element kml = kmlDoc.createElementNS("http://www.opengis.net/kml/2.2", "kml");
		kmlDoc.appendChild(kml);
		groundOverlayRoot = kml;
		if (folderName != null) {
			groundOverlayRoot = kmlDoc.createElement("Folder");
			kml.appendChild(groundOverlayRoot);
			Element name = kmlDoc.createElement("name");
			name.setTextContent(folderName);
			Element open = kmlDoc.createElement("open");
			open.setTextContent("1");
			groundOverlayRoot.appendChild(name);
			groundOverlayRoot.appendChild(open);
		}

	}

	private void addKmlEntry(String imageName, String imageFileName, int startX, int startY, int width, int height) {
		Element go = kmlDoc.createElement("GroundOverlay");
		Element name = kmlDoc.createElement("name");
		Element ico = kmlDoc.createElement("Icon");
		Element href = kmlDoc.createElement("href");
		Element drawOrder = kmlDoc.createElement("drawOrder");
		Element latLonBox = kmlDoc.createElement("LatLonBox");
		Element north = kmlDoc.createElement("north");
		Element south = kmlDoc.createElement("south");
		Element east = kmlDoc.createElement("east");
		Element west = kmlDoc.createElement("west");
		Element rotation = kmlDoc.createElement("rotation");

		name.setTextContent(imageName);
		href.setTextContent(imageFileName);
		drawOrder.setTextContent("0");

		MapSpace mapSpace = mapSource.getMapSpace();
		NumberFormat df = Utilities.FORMAT_6_DEC_ENG;

		String longitudeMin = df.format(mapSpace.cXToLon(startX, zoom));
		String longitudeMax = df.format(mapSpace.cXToLon(startX + width, zoom));
		String latitudeMin = df.format(mapSpace.cYToLat(startY + height, zoom));
		String latitudeMax = df.format(mapSpace.cYToLat(startY, zoom));

		north.setTextContent(latitudeMax);
		south.setTextContent(latitudeMin);
		west.setTextContent(longitudeMin);
		east.setTextContent(longitudeMax);
		rotation.setTextContent("0.0");

		groundOverlayRoot.appendChild(go);
		go.appendChild(name);
		go.appendChild(ico);
		go.appendChild(latLonBox);
		ico.appendChild(href);
		ico.appendChild(drawOrder);

		latLonBox.appendChild(north);
		latLonBox.appendChild(south);
		latLonBox.appendChild(east);
		latLonBox.appendChild(west);
		latLonBox.appendChild(rotation);
	}

	private void writeKmlToZip() throws TransformerFactoryConfigurationError, TransformerException, IOException {
		Transformer serializer;
		serializer = TransformerFactory.newInstance().newTransformer();
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		ByteArrayOutputStream bos = new ByteArrayOutputStream(16000);
		serializer.transform(new DOMSource(kmlDoc), new StreamResult(bos));
		kmzOutputStream.writeStoredEntry("doc.kml", bos.toByteArray());
		kmlDoc = null;
		groundOverlayRoot = null;
	}

}
