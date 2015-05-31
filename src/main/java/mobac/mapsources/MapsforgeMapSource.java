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
package mobac.mapsources;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlElement;

import mobac.exceptions.MapSourceInitializationException;
import mobac.exceptions.NotImplementedException;
import mobac.exceptions.UnrecoverableDownloadException;
import mobac.mapsources.mapspace.MercatorPower2MapSpace;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;

import org.apache.log4j.Logger;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.MultiMapDataStore;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

public class MapsforgeMapSource implements MapSource, FileBasedMapSource {

	private static final Logger LOG = Logger.getLogger(MapsforgeMapSource.class);

	private static final String name = "MapsforgeWorld";

	private MapSourceLoaderInfo loaderInfo = null;

	private MapSpace mapSpace = MercatorPower2MapSpace.INSTANCE_256;

	protected File mapFile;
	protected DatabaseRenderer renderer;
	protected XmlRenderTheme xmlRenderTheme;
	protected DisplayModel displayModel;
	protected MultiMapDataStore multiMapDataStore;
	protected RenderThemeFuture renderThemeFuture;
	protected XmlRenderThemeStyleMenu renderThemeStyleMenu;

	protected MapsForgeCache tileCache = new MapsForgeCache();

	@XmlElement(defaultValue = "false")
	protected boolean transparent = false;

	@XmlElement(defaultValue = "1.0")
	protected float textScale = 1.0f;

	public MapsforgeMapSource() {
		this("world.map");
	}

	public MapsforgeMapSource(String mapFileName) {
		mapFile = new File(mapFileName);
		displayModel = new DisplayModel();
		xmlRenderTheme = InternalRenderTheme.OSMARENDER;
	}

	@Override
	public void initialize() throws MapSourceInitializationException {
		GraphicFactory graphicFactory = AwtGraphicFactory.INSTANCE;
		if (!mapFile.exists())
			throw new MapSourceInitializationException("File does not exist: " + mapFile.getAbsolutePath());
		multiMapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_FIRST);
		multiMapDataStore.addMapDataStore(new MapFile(mapFile), true, true);

		renderer = new DatabaseRenderer(multiMapDataStore, graphicFactory, tileCache);
		renderThemeFuture = new RenderThemeFuture(graphicFactory, xmlRenderTheme, displayModel);
		// new Thread(renderThemeFuture).start();
		renderThemeFuture.run();
		// renderThemeFuture = new RenderThemeFuture(graphicFactory, xmlRenderTheme, displayModel);
		// renderThemeFuture.run();
	}

	protected void loadExternalRenderTheme(File xmlRenderThemeFile) throws FileNotFoundException {

		XmlRenderThemeMenuCallback callBack = new XmlRenderThemeMenuCallback() {

			@Override
			public Set<String> getCategories(XmlRenderThemeStyleMenu styleMenu) {
				renderThemeStyleMenu = styleMenu;
				String id = styleMenu.getDefaultValue();
				XmlRenderThemeStyleLayer baseLayer = styleMenu.getLayer(id);
				Set<String> result = baseLayer.getCategories();

				for (XmlRenderThemeStyleLayer overlay : baseLayer.getOverlays()) {
					LOG.trace("Overlay " + overlay.getId() + " enabled: " + overlay.isEnabled());
					if (overlay.isEnabled()) {
						result.addAll(overlay.getCategories());
					}
				}

				return result;
			}

		};
		this.xmlRenderTheme = new ExternalRenderTheme(xmlRenderThemeFile, callBack);
	}

	@Override
	public void reinitialize() {
	}

	public Color getBackgroundColor() {
		return Color.WHITE;
	}

	public MapSpace getMapSpace() {
		return mapSpace;
	}

	public int getMaxZoom() {
		return 19;
	}

	public int getMinZoom() {
		return 0;
	}

	public String getName() {
		return name;
	}

	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException,
			UnrecoverableDownloadException, InterruptedException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream(16000);
		try {
			BufferedImage image = getTileImage(zoom, x, y, loadMethod);
			if (image == null)
				return null;
			ImageIO.write(image, "png", buf);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return buf.toByteArray();
	}

	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException,
			UnrecoverableDownloadException, InterruptedException {
		if (mapFile == null || xmlRenderTheme == null)
			return null;
		if (loadMethod == LoadMethod.CACHE)
			return null;

		// ((MapSourceCallerThreadInfo)Thread.currentThread()).isMapPreviewThread()
		RendererJob job;
		Bitmap tileBitmap;
		Tile tile = new Tile(x, y, (byte) zoom, 256);
		job = new RendererJob(tile, multiMapDataStore, renderThemeFuture, displayModel, displayModel.getScaleFactor(),
				transparent, false);

		synchronized (this) {
			tileBitmap = renderer.executeJob(job);
		}
		if (tileBitmap == null)
			throw new IOException("Failed to render image");
		tileCache.put(job, null);
		return AwtGraphicFactory.getBitmap(tileBitmap);
	}

	public TileImageType getTileImageType() {
		return TileImageType.PNG;
	}

	public MapSourceLoaderInfo getLoaderInfo() {
		return loaderInfo;
	}

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
		this.loaderInfo = loaderInfo;
	}

	@Override
	public String toString() {
		return name;
	}

	private static class MapsForgeCache implements TileCache {

		HashSet<Integer> set = new HashSet<>(10000);

		@Override
		public void put(Job job, TileBitmap tile) {
			set.add(job.hashCode());
		}

		@Override
		public boolean containsKey(Job job) {
			return set.contains(job.hashCode());
		}

		@Override
		public void destroy() {
		}

		@Override
		public TileBitmap get(Job job) {
			throw new NotImplementedException();
		}

		@Override
		public int getCapacity() {
			throw new NotImplementedException();
		}

		@Override
		public int getCapacityFirstLevel() {
			throw new NotImplementedException();
		}

		@Override
		public TileBitmap getImmediately(Job job) {
			throw new NotImplementedException();
		}

		@Override
		public void setWorkingSet(Set<Job> jobs) {
			throw new NotImplementedException();
		}

		@Override
		public void purge() {
			set.clear();
		}

	}
}
