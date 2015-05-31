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
package mobac.program.model;

import java.io.OutputStream;
import java.util.ArrayList;

import javax.swing.JComboBox;

import mobac.gui.MainGUI;
import mobac.program.interfaces.TileImageDataWriter;
import mobac.program.tiledatawriter.TileImageJpegDataWriter;
import mobac.program.tiledatawriter.TileImagePng4DataWriter;
import mobac.program.tiledatawriter.TileImagePng8DataWriter;
import mobac.program.tiledatawriter.TileImagePngDataWriter;
import mobac.utilities.I18nUtils;

/**
 * Defines all available image formats selectable in the {@link JComboBox} in the {@link MainGUI}. Each element of this
 * enumeration contains one instance of an {@link TileImageDataWriter} instance that can perform one or more image
 * operations (e.g. color reduction) and then saves the image to an {@link OutputStream}.
 * 
 * @see TileImageDataWriter
 * @see TileImagePngDataWriter
 * @see TileImagePng4DataWriter
 * @see TileImagePng8DataWriter
 * @see TileImageJpegDataWriter
 */
public enum TileImageFormat {

	// PNG("PNG", new TileImagePngDataWriter()), //
	// PNG8Bit(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_png_8bit"), new TileImagePng8DataWriter()), //
	// PNG4Bit(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_png_4bit"), new TileImagePng4DataWriter()), //
	// JPEG100(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_jpg_q100"), new TileImageJpegDataWriter(1.00)), //
	// JPEG99(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_jpg_q99"), new TileImageJpegDataWriter(0.99)), //
	// JPEG95(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_jpg_q100"), new TileImageJpegDataWriter(0.95)), //
	// JPEG90(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_jpg_q90"), new TileImageJpegDataWriter(0.90)), //
	// JPEG85(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_jpg_q85"), new TileImageJpegDataWriter(0.85)), //
	// JPEG80(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_jpg_q80"), new TileImageJpegDataWriter(0.80)), //
	// JPEG70(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_jpg_q70"), new TileImageJpegDataWriter(0.70)), //
	// JPEG60(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_jpg_q60"), new TileImageJpegDataWriter(0.60)), //
	// JPEG50(MainGUI.localizedStringForKey("lp_tile_param_image_fmt_jpg_q50"), new TileImageJpegDataWriter(0.50)); //

	PNG(new TileImagePngDataWriter(), "lp_tile_param_image_fmt_png"), //
	PNG8Bit(new TileImagePng8DataWriter(), "lp_tile_param_image_fmt_png_8bit"), //
	PNG4Bit(new TileImagePng4DataWriter(), "lp_tile_param_image_fmt_png_4bit"), //
	JPEG100(new TileImageJpegDataWriter(1.00), "lp_tile_param_image_fmt_jpg_q100"), //
	JPEG99(new TileImageJpegDataWriter(0.99), "lp_tile_param_image_fmt_jpg_q99"), //
	JPEG95(new TileImageJpegDataWriter(0.95), "lp_tile_param_image_fmt_jpg_q95"), //
	JPEG90(new TileImageJpegDataWriter(0.90), "lp_tile_param_image_fmt_jpg_q90"), //
	JPEG85(new TileImageJpegDataWriter(0.85), "lp_tile_param_image_fmt_jpg_q85"), //
	JPEG80(new TileImageJpegDataWriter(0.80), "lp_tile_param_image_fmt_jpg_q80"), //
	JPEG70(new TileImageJpegDataWriter(0.70), "lp_tile_param_image_fmt_jpg_q70"), //
	JPEG60(new TileImageJpegDataWriter(0.60), "lp_tile_param_image_fmt_jpg_q60"), //
	JPEG50(new TileImageJpegDataWriter(0.50), "lp_tile_param_image_fmt_jpg_q50"); //

	// private final String description;

	private final TileImageDataWriter dataWriter;

	private final String translationKey;

	private TileImageFormat(TileImageDataWriter dataWriter, String translationKey) {
		// this.description = description;
		this.dataWriter = dataWriter;
		this.translationKey = translationKey;
	}

	@Override
	public String toString() {
		return I18nUtils.localizedStringForKey(translationKey);
	}

	public TileImageDataWriter getDataWriter() {
		return dataWriter;
	}

	public TileImageType getType() {
		return dataWriter.getType();
	}

	/**
	 * File extension
	 * 
	 * @return
	 */
	public String getFileExt() {
		return dataWriter.getType().getFileExt();
	}

	public static TileImageFormat[] getPngFormats() {
		return getFormats(TileImageType.PNG);
	}

	public static TileImageFormat[] getJpgFormats() {
		return getFormats(TileImageType.JPG);
	}

	protected static TileImageFormat[] getFormats(TileImageType tileImageType) {
		ArrayList<TileImageFormat> list = new ArrayList<TileImageFormat>();
		for (TileImageFormat format : values()) {
			if (tileImageType.equals(format.getType()))
				list.add(format);
		}
		TileImageFormat[] result = new TileImageFormat[0];
		result = list.toArray(result);
		return result;
	}

}
