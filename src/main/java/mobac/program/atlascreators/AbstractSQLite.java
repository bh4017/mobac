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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import mobac.exceptions.AtlasTestException;
import mobac.exceptions.MapCreationException;
import mobac.program.atlascreators.tileprovider.ConvertedRawTileProvider;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.RequiresSQLite;
import mobac.program.model.TileImageParameters;
import mobac.utilities.jdbc.SQLiteLoader;

public abstract class AbstractSQLite extends AtlasCreator implements RequiresSQLite {

	private static final int MAX_BATCH_SIZE = 1000;

	protected File databaseFile;

	protected Connection conn = null;
	protected PreparedStatement prepStmt;

	public AbstractSQLite() {
		super();
		SQLiteLoader.loadSQLiteOrShowError();
	}

	protected void openConnection() throws SQLException {
		if (conn == null || conn.isClosed()) {
			String url = "jdbc:sqlite:/" + databaseFile.getAbsolutePath();
			conn = DriverManager.getConnection(url);
		}
	}

	@Override
	protected void testAtlas() throws AtlasTestException {
		super.testAtlas();
		try {
			SQLiteLoader.loadSQLite();
		} catch (SQLException e) {
			throw new AtlasTestException(SQLiteLoader.getMsgSqliteMissing());
		}
	}

	@Override
	public void startAtlasCreation(AtlasInterface atlas, File customAtlasDir) throws IOException, AtlasTestException,
			InterruptedException {
		super.startAtlasCreation(atlas, customAtlasDir);
		databaseFile = new File(atlasDir, getDatabaseFileName());
		log.debug("SQLite Database file: " + databaseFile);
	}

	@Override
	public void abortAtlasCreation() throws IOException {
		SQLiteLoader.closeConnection(conn);
		conn = null;
		super.abortAtlasCreation();
	}

	@Override
	public void finishAtlasCreation() throws IOException, InterruptedException {
		SQLiteLoader.closeConnection(conn);
		conn = null;
		super.finishAtlasCreation();
	}

	protected void createTiles() throws InterruptedException, MapCreationException {
		int maxMapProgress = 2 * (xMax - xMin + 1) * (yMax - yMin + 1);
		atlasProgress.initMapCreation(maxMapProgress);
		TileImageParameters param = map.getParameters();
		if (param != null)
			mapDlTileProvider = new ConvertedRawTileProvider(mapDlTileProvider, param.getFormat());
		try {
			conn.setAutoCommit(false);
			int batchTileCount = 0;
			int tilesWritten = 0;
			Runtime r = Runtime.getRuntime();
			long heapMaxSize = r.maxMemory();
			prepStmt = conn.prepareStatement(getTileInsertSQL());
			for (int x = xMin; x <= xMax; x++) {
				for (int y = yMin; y <= yMax; y++) {
					checkUserAbort();
					atlasProgress.incMapCreationProgress();
					try {
						byte[] sourceTileData = mapDlTileProvider.getTileData(x, y);
						if (sourceTileData != null) {
							writeTile(x, y, zoom, sourceTileData);
							tilesWritten++;
							long heapAvailable = heapMaxSize - r.totalMemory() + r.freeMemory();

							batchTileCount++;
							if ((heapAvailable < HEAP_MIN) || (batchTileCount >= MAX_BATCH_SIZE)) {
								log.trace("Executing batch containing " + batchTileCount + " tiles");
								prepStmt.executeBatch();
								prepStmt.clearBatch();
								System.gc();
								conn.commit();
								atlasProgress.incMapCreationProgress(batchTileCount);
								batchTileCount = 0;
							}
						}
					} catch (IOException e) {
						throw new MapCreationException(map, e);
					}
				}
			}
			prepStmt.executeBatch();
			prepStmt.clearBatch();
			prepStmt.close();
			prepStmt = null;
			System.gc();
			if (tilesWritten > 0)
				updateTileMetaInfo();
			log.trace("Final commit containing " + batchTileCount + " tiles");
			conn.commit();
			atlasProgress.setMapCreationProgress(maxMapProgress);
		} catch (SQLException e) {
			throw new MapCreationException(map, e);
		}
	}

	protected abstract String getTileInsertSQL();

	protected abstract void writeTile(int x, int y, int z, byte[] tileData) throws SQLException, IOException;

	protected abstract String getDatabaseFileName();

	protected abstract void updateTileMetaInfo() throws SQLException;
}
