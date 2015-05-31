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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import mobac.exceptions.AtlasTestException;
import mobac.exceptions.MapCreationException;
import mobac.program.ProgramInfo;
import mobac.program.annotations.AtlasCreatorName;
import mobac.program.annotations.SupportedParameters;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.model.TileImageParameters.Name;
import mobac.utilities.Utilities;

/**
 * 
 * Warning: This implementation is defect and incomplete!
 * 
 * https://sourceforge.net/p/mobac/feature-requests/263/
 * 
 * http://www.geopackage.org/spec/
 */
@AtlasCreatorName(value = "GeoPackage SQLite", type = "GeoPackage")
@SupportedParameters(names = { Name.format })
public class GeoPackage extends AbstractSQLite {

	private static final double PIXEL_WORLD_CONST = 156543.0339280409984;

	private static final String CREATE_TABLE_CONTENTS = "CREATE TABLE gpkg_contents "
			+ "(table_name TEXT NOT NULL PRIMARY KEY, data_type TEXT NOT NULL, identifier TEXT UNIQUE, "
			+ "description TEXT DEFAULT '', last_change DATETIME NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')), "
			+ "min_x DOUBLE, min_y DOUBLE, max_x DOUBLE, max_y DOUBLE, srs_id INTEGER, "
			+ "CONSTRAINT fk_gc_r_srs_id FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys(srs_id))";

	private static final String CREATE_TABLE_SPATIAL_REF_SYS = "CREATE TABLE gpkg_spatial_ref_sys "
			+ "(srs_name TEXT NOT NULL, srs_id INTEGER NOT NULL PRIMARY KEY, organization TEXT NOT NULL, "
			+ "organization_coordsys_id INTEGER NOT NULL, definition TEXT NOT NULL, description TEXT)";

	private static final String CREATE_TABLE_TILE_MATRIX = "CREATE TABLE gpkg_tile_matrix "
			+ "(table_name TEXT NOT NULL, zoom_level INTEGER NOT NULL, matrix_width INTEGER NOT NULL, "
			+ "matrix_height INTEGER NOT NULL, tile_width INTEGER NOT NULL, tile_height INTEGER NOT NULL, "
			+ "pixel_x_size DOUBLE NOT NULL, pixel_y_size DOUBLE NOT NULL, "
			+ "CONSTRAINT pk_ttm PRIMARY KEY (table_name, zoom_level), "
			+ "CONSTRAINT fk_tmm_table_name FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name))";

	private static final String CREATE_TABLE_TILE_MATRIX_SET = "CREATE TABLE gpkg_tile_matrix_set "
			+ "(table_name TEXT NOT NULL PRIMARY KEY, srs_id INTEGER NOT NULL, min_x DOUBLE NOT NULL, "
			+ "min_y DOUBLE NOT NULL, max_x DOUBLE NOT NULL, max_y DOUBLE NOT NULL, "
			+ "CONSTRAINT fk_gtms_table_name FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name), "
			+ "CONSTRAINT fk_gtms_srs FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys(srs_id))";

	private static final String CREATE_TABLE_TILES = "CREATE TABLE tiles "
			+ "(id INTEGER PRIMARY KEY AUTOINCREMENT, zoom_level INTEGER NOT NULL, "
			+ "tile_column INTEGER NOT NULL, tile_row INTEGER NOT NULL, tile_data BLOB NOT NULL, "
			+ "UNIQUE (zoom_level, tile_column, tile_row))";

	private static final String INSERT_TILE = "INSERT INTO tiles "
			+ "(zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)";

	private boolean atlasInitialized = false;

	double minLon, minLat, maxLat, maxLon;

	public GeoPackage() {
		super();
	}

	@Override
	public boolean testMapSource(MapSource mapSource) {
		return true;
	}

	@Override
	protected String getDatabaseFileName() {
		return atlas.getName() + ".gpkg";
	}

	@Override
	public void startAtlasCreation(AtlasInterface atlas, File customAtlasDir) throws IOException, AtlasTestException,
			InterruptedException {
		super.startAtlasCreation(atlas, customAtlasDir);
		Utilities.mkDir(atlasDir);
		minLat = Double.MAX_VALUE;
		minLon = Double.MAX_VALUE;
		maxLat = Double.MIN_VALUE;
		maxLon = Double.MIN_VALUE;
	}

	@Override
	public void createMap() throws MapCreationException, InterruptedException {
		try {
			if (!atlasInitialized) {
				atlasInitialized = true;
				openConnection();
				initializeDB();
			}
			createTiles();
		} catch (SQLException e) {
			throw new MapCreationException("Error writing SQL database \"" + databaseFile + "\": " + e.getMessage(),
					map, e);
		}
	}

	private void initializeDB() throws SQLException {
		Statement stat = conn.createStatement();
		stat.executeUpdate(CREATE_TABLE_CONTENTS);
		stat.executeUpdate(CREATE_TABLE_SPATIAL_REF_SYS);
		stat.executeUpdate(CREATE_TABLE_TILE_MATRIX);
		stat.executeUpdate(CREATE_TABLE_TILE_MATRIX_SET);
		stat.executeUpdate(CREATE_TABLE_TILES);

		stat.executeUpdate("INSERT INTO gpkg_spatial_ref_sys VALUES(' ',-1,'NONE',-1,'undefined',NULL)");
		stat.executeUpdate("INSERT INTO gpkg_spatial_ref_sys VALUES(' ',0,'NONE',0,'undefined',NULL)");
		stat.executeUpdate("INSERT INTO gpkg_spatial_ref_sys VALUES('WGS 84 / World Mercator',3395,'epsg',3395,' "
				+ "PROJCS[\"WGS 84 / World Mercator\",GEOGCS[\"WGS 84\", DATUM[\"WGS_1984\","
				+ "SPHEROID[\"WGS 84\",6378137,298.257223563, AUTHORITY[\"EPSG\",\"7030\"]],"
				+ "AUTHORITY[\"EPSG\",\"6326\"]], PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]], "
				+ "UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]], "
				+ "AUTHORITY[\"EPSG\",\"4326\"]],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]], "
				+ "PROJECTION[\"Mercator_1SP\"],PARAMETER[\"central_meridian\",0], PARAMETER[\"scale_factor\",1],"
				+ "PARAMETER[\"false_easting\",0], PARAMETER[\"false_northing\",0], AUTHORITY[\"EPSG\",\"3395\"], "
				+ "AXIS[\"Easting\",EAST],AXIS[\"Northing\",NORTH]] ',NULL)");
		stat.executeUpdate("INSERT INTO gpkg_spatial_ref_sys VALUES('WGS 84 / Pseudo-Mercator',3857,'epsg',3857,' "
				+ "PROJCS[\"WGS 84 / Pseudo-Mercator\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\", "
				+ "SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],"
				+ "AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\", \"8901\"]],"
				+ "UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],"
				+ "AUTHORITY[\"EPSG\",\"9122\"]]AUTHORITY[\"EPSG\",\"4326\"]],"
				+ "PROJECTION[\"Mercator_1SP\"],PARAMETER[\"central_meridian\",0],"
				+ "PARAMETER[ \"scale_factor\",1],PARAMETER[\"false_easting\",0],"
				+ "PARAMETER[ \"false_northing\",0],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],"
				+ "AXIS[ \"X\",EAST],AXIS[\"Y\",NORTH] ',NULL)");
		stat.executeUpdate("INSERT INTO gpkg_spatial_ref_sys VALUES('WGS 84',4326,'epsg',4326,"
				+ "'GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137, 298.257223563,"
				+ "AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\", \"6326\"]],PRIMEM[\"Greenwich\",0,"
				+ "AUTHORITY[\"EPSG\",\"8901\"]],UNIT [\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],"
				+ "AUTHORITY[\"EPSG\",\"4326\"]]',NULL)");
		stat.executeUpdate("INSERT INTO gpkg_spatial_ref_sys VALUES('WGS 84 / Scaled World Mercator',9804,'epsg',9804,"
				+ "'PROJCS[\"unnamed\",GEOGCS[\"WGS 84\", DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563, "
				+ "AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]], PRIMEM[\"Greenwich\",0],"
				+ "UNIT[\"degree\",0.0174532925199433], AUTHORITY[\"EPSG\",\"4326\"]],PROJECTION[\"Mercator_1SP\"],"
				+ "PARAMETER[\"central_meridian\",0], PARAMETER[\"scale_factor\",0.803798909747978],"
				+ "PARAMETER[\"false_easting\",0], PARAMETER[\"false_northing\",0], "
				+ "UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]]] ',NULL)");

		stat.close();
	}

	@Override
	protected void createTiles() throws InterruptedException, MapCreationException {
		super.createTiles();
		try {
			PreparedStatement prepStmt = conn.prepareStatement("INSERT INTO gpkg_tile_matrix "
					+ "VALUES('tiles',?,?,?,256,256,?,?)");
			prepStmt.setInt(1, map.getZoom());
			prepStmt.setInt(2, xMax - xMin + 1); // matrix_width
			prepStmt.setInt(3, yMax - yMin + 1); // matrix_height
			double pixel_size = PIXEL_WORLD_CONST / Math.pow(2.0, map.getZoom());
			prepStmt.setDouble(4, pixel_size); // pixel_x_size
			prepStmt.setDouble(5, pixel_size); // pixel_y_size
			prepStmt.executeUpdate();
			prepStmt.close();
			conn.commit();
		} catch (SQLException e) {
			throw new MapCreationException(map, e);
		}

	}

	private double lonToMetersX(double lon) {
		return lon * Math.PI * 6378137.0 / 180.0;
	}

	private double latToMetersY(double lat) {
		double meters_y = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
		meters_y = meters_y * Math.PI * 6378137.0 / 180.0;
		return meters_y;
	}

	@Override
	protected void updateTileMetaInfo() throws SQLException {
		minLon = Math.min(minLon, map.getMinLon());
		minLat = Math.min(minLat, map.getMinLat());
		maxLat = Math.max(maxLat, map.getMaxLon());
		maxLon = Math.max(maxLon, map.getMaxLat());

		double min_y = latToMetersY(minLat);
		double max_y = latToMetersY(maxLat);
		double min_x = lonToMetersX(minLon);
		double max_x = lonToMetersX(maxLon);

		Statement stat = conn.createStatement();
		stat.execute("DELETE FROM gpkg_contents;");
		stat.execute("DELETE FROM gpkg_tile_matrix_set;");
		stat.close();

		PreparedStatement prepStmt;
		prepStmt = conn.prepareStatement("INSERT INTO gpkg_contents "
				+ "(rowid, table_name, data_type, identifier, description, min_x, min_y, max_x, max_y,srs_id) "
				+ "VALUES (1,'tiles','tiles','Raster Tiles',?,?,?,?,?,3857)");

		prepStmt.setString(1, "created by " + ProgramInfo.getCompleteTitle());
		prepStmt.setDouble(2, min_x); // min_x
		prepStmt.setDouble(3, min_y); // min_y
		prepStmt.setDouble(4, max_x); // max_x
		prepStmt.setDouble(5, max_y); // max_y
		prepStmt.executeUpdate();
		prepStmt.close();

		prepStmt = conn.prepareStatement("INSERT INTO gpkg_tile_matrix_set VALUES('tiles',3857,?,?,?,?)");
		prepStmt.setDouble(1, min_x); // min_x
		prepStmt.setDouble(2, min_y); // min_y
		prepStmt.setDouble(3, max_x); // max_x
		prepStmt.setDouble(4, max_y); // max_y
		prepStmt.executeUpdate();
		prepStmt.close();
		conn.commit();
	}

	@Override
	protected String getTileInsertSQL() {
		return INSERT_TILE;
	}

	@Override
	protected void writeTile(int x, int y, int z, byte[] tileData) throws SQLException, IOException {
		prepStmt.setInt(1, z);
		prepStmt.setInt(2, x - xMin);
		prepStmt.setInt(3, y - yMin);
		prepStmt.setBytes(4, tileData);
		prepStmt.addBatch();
	}

}
