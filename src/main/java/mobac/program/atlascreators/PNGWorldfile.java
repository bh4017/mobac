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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;

import mobac.exceptions.MapCreationException;
import mobac.program.annotations.AtlasCreatorName;
import mobac.utilities.Utilities;

/**
 * http://sourceforge.net/p/mobac/feature-requests/136/
 */
@AtlasCreatorName("PNG + Worldfile (PNG & PGW)")
public class PNGWorldfile extends Glopus {

	@Override
	public void createMap() throws MapCreationException, InterruptedException {
		try {
			Utilities.mkDir(layerDir);
		} catch (IOException e) {
			throw new MapCreationException(map, e);
		}
		createTiles();
		writeWorldFile();
		writeProjectionFile();
	}

	/**
	 * http://en.wikipedia.org/wiki/World_file
	 * 
	 * <pre>
	 * Format of Worldfile: 
	 * 			   0.000085830078125  (size of pixel in x direction)                              =(east-west)/image width
	 * 			   0.000000000000     (rotation term for row)
	 * 			   0.000000000000     (rotation term for column)
	 * 			   -0.00006612890625  (size of pixel in y direction)                              =-(north-south)/image height
	 * 			   -106.54541         (x coordinate of centre of upper left pixel in map units)   =west
	 * 			   39.622615          (y coordinate of centre of upper left pixel in map units)   =north
	 * </pre>
	 */
	private void writeWorldFile() throws MapCreationException {
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(new File(layerDir, mapName + ".pgw"));
			OutputStreamWriter mapWriter = new OutputStreamWriter(fout, TEXT_FILE_CHARSET);

			// MapSpace mapSpace = mapSource.getMapSpace();

			double worldSize = 2 * Math.PI * 6378137;
			double maxTiles = 1 << zoom;
			double pixelSize = worldSize / (maxTiles * tileSize);

			mapWriter.write(String.format(Locale.ENGLISH, "%.15f\n", pixelSize));
			mapWriter.write("0.0\n");
			mapWriter.write("0.0\n");
			mapWriter.write(String.format(Locale.ENGLISH, "%.15f\n", -pixelSize));

			double xMin1 = worldSize * (xMin / maxTiles - 0.5);
			double yMax1 = worldSize * (0.5 - yMin / maxTiles);

			mapWriter.write(String.format(Locale.ENGLISH, "%.7f\n", xMin1 + 0.5 * pixelSize));
			mapWriter.write(String.format(Locale.ENGLISH, "%.7f\n", yMax1 - 0.5 * pixelSize));

			mapWriter.flush();
			mapWriter.close();
		} catch (IOException e) {
			throw new MapCreationException(map, e);
		} finally {
			Utilities.closeStream(fout);
		}
	}

	private void writeProjectionFile() throws MapCreationException {
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(new File(layerDir, mapName + ".png.aux.xml"));
			OutputStreamWriter writer = new OutputStreamWriter(fout, TEXT_FILE_CHARSET);

			writer.write("<PAMDataset><SRS>PROJCS[&quot;WGS 84 / Pseudo-Mercator&quot;,GEOGCS[&quot;WGS 84&quot;,"
					+ "DATUM[&quot;WGS_1984&quot;,SPHEROID[&quot;WGS 84&quot;,6378137,298.257223563,"
					+ "AUTHORITY[&quot;EPSG&quot;,&quot;7030&quot;]],AUTHORITY[&quot;EPSG&quot;,&quot;6326&quot;]],"
					+ "PRIMEM[&quot;Greenwich&quot;,0],UNIT[&quot;degree&quot;,0.0174532925199433],"
					+ "AUTHORITY[&quot;EPSG&quot;,&quot;4326&quot;]],"
					+ "PROJECTION[&quot;Mercator_1SP&quot;],PARAMETER[&quot;central_meridian&quot;,0],"
					+ "PARAMETER[&quot;scale_factor&quot;,1],PARAMETER[&quot;false_easting&quot;,0],"
					+ "PARAMETER[&quot;false_northing&quot;,0],UNIT[&quot;metre&quot;,1,"
					+ "AUTHORITY[&quot;EPSG&quot;,&quot;9001&quot;]],"
					+ "EXTENSION[&quot;PROJ4&quot;,&quot;+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs&quot;],"
					+ "AUTHORITY[&quot;EPSG&quot;,&quot;3857&quot;]]</SRS></PAMDataset>");

			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new MapCreationException(map, e);
		} finally {
			Utilities.closeStream(fout);
		}
	}
}
