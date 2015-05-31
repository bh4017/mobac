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
package mobac.program;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import mobac.exceptions.AtlasTestException;
import mobac.exceptions.MapDownloadSkippedException;
import mobac.gui.AtlasProgress;
import mobac.gui.AtlasProgress.AtlasCreationController;
import mobac.program.atlascreators.AtlasCreator;
import mobac.program.atlascreators.tileprovider.DownloadedTileProvider;
import mobac.program.atlascreators.tileprovider.FilteredMapSourceProvider;
import mobac.program.atlascreators.tileprovider.TileProvider;
import mobac.program.download.DownloadJobProducerThread;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.DownloadJobListener;
import mobac.program.interfaces.DownloadableElement;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.LayerInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSource.LoadMethod;
import mobac.program.interfaces.MapSourceCallerThreadInfo;
import mobac.program.model.AtlasOutputFormat;
import mobac.program.model.Settings;
import mobac.program.tilestore.TileStore;
import mobac.utilities.GUIExceptionHandler;
import mobac.utilities.I18nUtils;
import mobac.utilities.Utilities;
import mobac.utilities.tar.TarIndex;
import mobac.utilities.tar.TarIndexedArchive;

import org.apache.log4j.Logger;

public class AtlasThread extends Thread implements DownloadJobListener, AtlasCreationController, MapSourceCallerThreadInfo {

	private static final Logger log = Logger.getLogger(AtlasThread.class);
	private static int threadNum = 0;

	private File customAtlasDir = null;
	private boolean quitMobacAfterAtlasCreation = false;

	private DownloadJobProducerThread djp = null;
	private JobDispatcher downloadJobDispatcher;
	private AtlasProgress ap; // The GUI showing the progress

	private AtlasInterface atlas;
	private AtlasCreator atlasCreator = null;
	private PauseResumeHandler pauseResumeHandler;

	private int activeDownloads = 0;
	private int jobsCompleted = 0;
	private int jobsRetryError = 0;
	private int jobsPermanentError = 0;
	private int maxDownloadRetries = 1;

	public AtlasThread(AtlasInterface atlas) throws AtlasTestException {
		this(atlas, atlas.getOutputFormat().createAtlasCreatorInstance());
	}

	public AtlasThread(AtlasInterface atlas, AtlasCreator atlasCreator) throws AtlasTestException {
		super("AtlasThread " + getNextThreadNum());
		ap = new AtlasProgress(this);
		this.atlas = atlas;
		this.atlasCreator = atlasCreator;
		testAtlas();
		TileStore.getInstance().closeAll();
		maxDownloadRetries = Settings.getInstance().downloadRetryCount;
		pauseResumeHandler = new PauseResumeHandler();
	}

	private void testAtlas() throws AtlasTestException {
		try {
			for (LayerInterface layer : atlas) {
				for (MapInterface map : layer) {
					MapSource mapSource = map.getMapSource();
					if (!atlasCreator.testMapSource(mapSource))
						throw new AtlasTestException("The selected atlas output format \"" + atlas.getOutputFormat()
								+ "\" does not support the map source \"" + map.getMapSource() + "\"");
				}
			}
		} catch (AtlasTestException e) {
			throw e;
		} catch (Exception e) {
			throw new AtlasTestException(e);
		}
	}

	private static synchronized int getNextThreadNum() {
		threadNum++;
		return threadNum;
	}

	public void run() {
		GUIExceptionHandler.registerForCurrentThread();
		log.info("Starting creation of " + atlas.getOutputFormat() + " atlas \"" + atlas.getName() + "\"");
		if (customAtlasDir != null)
			log.debug("Target directory: " + customAtlasDir);
		ap.setDownloadControlerListener(this);
		try {
			createAtlas();
			log.info("Altas creation finished");
			if (quitMobacAfterAtlasCreation)
				System.exit(0);
		} catch (OutOfMemoryError e) {
			System.gc();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					String message = I18nUtils.localizedStringForKey("msg_out_of_memory_head");
					int maxMem = Utilities.getJavaMaxHeapMB();
					if (maxMem > 0)
						message += String.format(I18nUtils.localizedStringForKey("msg_out_of_memory_detail"), maxMem);
					JOptionPane.showMessageDialog(null, message,
							I18nUtils.localizedStringForKey("msg_out_of_memory_title"), JOptionPane.ERROR_MESSAGE);
					ap.closeWindow();
				}
			});
			log.error("Out of memory: ", e);
		} catch (InterruptedException e) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, I18nUtils.localizedStringForKey("msg_atlas_download_abort"),
							I18nUtils.localizedStringForKey("Information"), JOptionPane.INFORMATION_MESSAGE);
					ap.closeWindow();
				}
			});
			log.info("Altas creation was interrupted by user");
		} catch (Exception e) {
			log.error("Altas creation aborted because of an error: ", e);
			GUIExceptionHandler.showExceptionDialog(e);
		}
		System.gc();
		if (quitMobacAfterAtlasCreation) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			System.exit(1);
		}
	}

	/**
	 * Create atlas: For each map download the tiles and perform atlas/map creation
	 */
	protected void createAtlas() throws InterruptedException, IOException {

		long totalNrOfOnlineTiles = atlas.calculateTilesToDownload();

		for (LayerInterface l : atlas) {
			for (MapInterface m : l) {
				// Offline map sources are not relevant for the maximum tile limit.
				if (m.getMapSource() instanceof FileBasedMapSource)
					totalNrOfOnlineTiles -= m.calculateTilesToDownload();
			}
		}

		if (totalNrOfOnlineTiles > 500000) {
			// NumberFormat f = DecimalFormat.getInstance();
			JOptionPane.showMessageDialog(null, String.format(
					I18nUtils.localizedStringForKey("msg_too_many_tiles_msg"), 500000, totalNrOfOnlineTiles), I18nUtils
					.localizedStringForKey("msg_too_many_tiles_title"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			atlasCreator.startAtlasCreation(atlas, customAtlasDir);
		} catch (AtlasTestException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Atlas format restriction violated",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		ap.initAtlas(atlas);
		ap.setVisible(true);

		Settings s = Settings.getInstance();

		downloadJobDispatcher = new JobDispatcher(this, s.downloadThreadCount, pauseResumeHandler, ap);
		try {
			for (LayerInterface layer : atlas) {
				atlasCreator.initLayerCreation(layer);
				for (MapInterface map : layer) {
					try {
						while (!createMap(map))
							;
					} catch (InterruptedException e) {
						throw e; // User has aborted
					} catch (MapDownloadSkippedException e) {
						// Do nothing and continue with next map
					} catch (Exception e) {
						log.error("", e);
						String[] options = { I18nUtils.localizedStringForKey("Continue"),
								I18nUtils.localizedStringForKey("Abort"),
								I18nUtils.localizedStringForKey("dlg_download_show_error_report") };
						int a = JOptionPane.showOptionDialog(null,
								I18nUtils.localizedStringForKey("dlg_download_erro_head") + e.getMessage() + "\n["
										+ e.getClass().getSimpleName() + "]\n\n",
								I18nUtils.localizedStringForKey("Error"), 0, JOptionPane.ERROR_MESSAGE, null, options,
								options[0]);
						switch (a) {
						case 2:
							GUIExceptionHandler.processException(e);
						case 1:
							throw new InterruptedException();
						}
					}
				}
				atlasCreator.finishLayerCreation();
			}
		} catch (InterruptedException e) {
			atlasCreator.abortAtlasCreation();
			throw e;
		} catch (Error e) {
			atlasCreator.abortAtlasCreation();
			throw e;
		} finally {
			// In case of an abort: Stop create new download jobs
			if (djp != null)
				djp.cancel();
			downloadJobDispatcher.terminateAllWorkerThreads();
			if (!atlasCreator.isAborted())
				atlasCreator.finishAtlasCreation();
			ap.atlasCreationFinished();
		}

	}

	/**
	 * 
	 * @param map
	 * @return true if map creation process was finished and false if something went wrong and the user decided to retry
	 *         map download
	 * @throws Exception
	 */
	public boolean createMap(MapInterface map) throws Exception {
		TarIndex tileIndex = null;
		TarIndexedArchive tileArchive = null;

		jobsCompleted = 0;
		jobsRetryError = 0;
		jobsPermanentError = 0;

		ap.initMapDownload(map);
		if (currentThread().isInterrupted())
			throw new InterruptedException();

		// Prepare the tile store directory
		// ts.prepareTileStore(map.getMapSource());

		/***
		 * In this section of code below, tiles for Atlas is being downloaded and saved in the temporary layer tar file
		 * in the system temp directory.
		 **/
		int zoom = map.getZoom();

		final int tileCount = (int) map.calculateTilesToDownload();

		ap.setZoomLevel(zoom);
		try {
			tileArchive = null;
			TileProvider mapTileProvider;
			if (!(map.getMapSource() instanceof FileBasedMapSource)) {
				// For online maps we download the tiles first and then start creating the map if
				// we are sure we got all tiles
				if (!AtlasOutputFormat.TILESTORE.equals(atlas.getOutputFormat())) {
					String tempSuffix = "MOBAC_" + atlas.getName() + "_" + zoom + "_";
					File tileArchiveFile = File.createTempFile(tempSuffix, ".tar", DirectoryManager.tempDir);
					// If something goes wrong the temp file only persists until the VM exits
					tileArchiveFile.deleteOnExit();
					log.debug("Writing downloaded tiles to " + tileArchiveFile.getPath());
					tileArchive = new TarIndexedArchive(tileArchiveFile, tileCount);
				} else
					log.debug("Downloading to tile store only");

				djp = new DownloadJobProducerThread(this, downloadJobDispatcher, tileArchive, (DownloadableElement) map);

				boolean failedMessageAnswered = false;

				while (djp.isAlive() || (downloadJobDispatcher.getWaitingJobCount() > 0)
						|| downloadJobDispatcher.isAtLeastOneWorkerActive()) {
					Thread.sleep(500);
					if (!failedMessageAnswered && (jobsRetryError > 50) && !ap.ignoreDownloadErrors()) {
						pauseResumeHandler.pause();
						String[] answers = new String[] { I18nUtils.localizedStringForKey("Continue"),
								I18nUtils.localizedStringForKey("Retry"), I18nUtils.localizedStringForKey("Skip"),
								I18nUtils.localizedStringForKey("Abort") };
						int answer = JOptionPane.showOptionDialog(ap,
								I18nUtils.localizedStringForKey("dlg_download_errors_todo_msg"),
								I18nUtils.localizedStringForKey("dlg_download_errors_todo"), 0,
								JOptionPane.QUESTION_MESSAGE, null, answers, answers[0]);
						failedMessageAnswered = true;
						switch (answer) {
						case 0: // Continue
							pauseResumeHandler.resume();
							break;
						case 1: // Retry
							djp.cancel();
							djp = null;
							downloadJobDispatcher.cancelOutstandingJobs();
							return false;
						case 2: // Skip
							downloadJobDispatcher.cancelOutstandingJobs();
							throw new MapDownloadSkippedException();
						default: // Abort or close dialog
							downloadJobDispatcher.cancelOutstandingJobs();
							downloadJobDispatcher.terminateAllWorkerThreads();
							throw new InterruptedException();
						}
					}
				}
				djp = null;
				log.debug("All download jobs has been completed!");
				if (tileArchive != null) {
					tileArchive.writeEndofArchive();
					tileArchive.close();
					tileIndex = tileArchive.getTarIndex();
					if (tileIndex.size() < tileCount && !ap.ignoreDownloadErrors()) {
						int missing = tileCount - tileIndex.size();
						log.debug("Expected tile count: " + tileCount + " downloaded tile count: " + tileIndex.size()
								+ " missing: " + missing);
						int answer = JOptionPane.showConfirmDialog(ap, String.format(
								I18nUtils.localizedStringForKey("dlg_download_errors_missing_tile_msg"), missing),
								I18nUtils.localizedStringForKey("dlg_download_errors_missing_tile"),
								JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
						if (answer != JOptionPane.YES_OPTION)
							throw new InterruptedException();
					}
				}
				downloadJobDispatcher.cancelOutstandingJobs();
				log.debug("Starting to create atlas from downloaded tiles");
				mapTileProvider = new DownloadedTileProvider(tileIndex, map);
			} else {
				// We don't need to download anything. Everything is already stored locally therefore we can just use it
				mapTileProvider = new FilteredMapSourceProvider(map, LoadMethod.DEFAULT);
			}
			atlasCreator.initializeMap(map, mapTileProvider);
			atlasCreator.createMap();
		} catch (Error e) {
			log.error("Error in createMap: " + e.getMessage(), e);
			throw e;
		} finally {
			if (tileIndex != null)
				tileIndex.closeAndDelete();
			else if (tileArchive != null)
				tileArchive.delete();
		}
		return true;
	}

	public void pauseResumeAtlasCreation() {
		if (pauseResumeHandler.isPaused()) {
			log.debug("Atlas creation resumed");
			pauseResumeHandler.resume();
		} else {
			log.debug("Atlas creation paused");
			pauseResumeHandler.pause();
		}
	}

	public boolean isPaused() {
		return pauseResumeHandler.isPaused();
	}

	public PauseResumeHandler getPauseResumeHandler() {
		return pauseResumeHandler;
	}

	/**
	 * Stop listener from {@link AtlasProgress}
	 */
	public void abortAtlasCreation() {
		try {
			DownloadJobProducerThread djp_ = djp;
			if (djp_ != null)
				djp_.cancel();
			if (downloadJobDispatcher != null)
				downloadJobDispatcher.terminateAllWorkerThreads();
			pauseResumeHandler.resume();
			this.interrupt();
		} catch (Exception e) {
			log.error("Exception thrown in stopDownload()" + e.getMessage());
		}
	}

	public int getActiveDownloads() {
		return activeDownloads;
	}

	public synchronized void jobStarted() {
		activeDownloads++;
	}

	public void jobFinishedSuccessfully(int bytesDownloaded) {
		synchronized (this) {
			ap.incMapDownloadProgress();
			activeDownloads--;
			jobsCompleted++;
		}
		ap.updateGUI();
	}

	public void jobFinishedWithError(boolean retry) {
		synchronized (this) {
			activeDownloads--;
			if (retry)
				jobsRetryError++;
			else {
				jobsPermanentError++;
				ap.incMapDownloadProgress();
			}
		}
		if (!ap.ignoreDownloadErrors())
			Toolkit.getDefaultToolkit().beep();
		ap.setErrorCounter(jobsRetryError, jobsPermanentError);
		ap.updateGUI();
	}

	public int getMaxDownloadRetries() {
		return maxDownloadRetries;
	}

	public AtlasProgress getAtlasProgress() {
		return ap;
	}

	public File getCustomAtlasDir() {
		return customAtlasDir;
	}

	public void setCustomAtlasDir(File customAtlasDir) {
		this.customAtlasDir = customAtlasDir;
	}

	public void setQuitMobacAfterAtlasCreation(boolean quitMobacAfterAtlasCreation) {
		this.quitMobacAfterAtlasCreation = quitMobacAfterAtlasCreation;
	}

	@Override
	public boolean isMapPreviewThread() {
		return false;
	}

}
