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
package mobac.gui.panels;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.TreeSet;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import mobac.gui.components.JCollapsiblePanel;
import mobac.gui.components.JTileSizeCombo;
import mobac.program.annotations.SupportedParameters;
import mobac.program.atlascreators.AtlasCreator;
import mobac.program.model.AtlasOutputFormat;
import mobac.program.model.Settings;
import mobac.program.model.TileImageFormat;
import mobac.program.model.TileImageParameters;
import mobac.program.model.TileImageParameters.Name;
import mobac.program.tiledatawriter.TileImageJpegDataWriter;
import mobac.utilities.GBC;
import mobac.utilities.I18nUtils;
import mobac.utilities.Utilities;

public class JTileImageParametersPanel extends JCollapsiblePanel {

	private static final long serialVersionUID = 1L;
	private static boolean JPEG_TESTED = false;

	private JCheckBox enableCustomTileProcessingCheckButton;
	private JLabel tileSizeWidthLabel;
	private JLabel tileSizeHeightLabel;
	private JLabel tileImageFormatLabel;
	private JTileSizeCombo tileSizeWidth;
	private JTileSizeCombo tileSizeHeight;
	private JComboBox<TileImageFormat> tileImageFormat;

	private boolean widthEnabled = true;
	private boolean heightEnabled = true;
	private boolean formatPngEnabled = true;
	private boolean formatJpgEnabled = true;

	public JTileImageParametersPanel() {
		super(I18nUtils.localizedStringForKey("lp_tile_param_title"), new GridBagLayout());
		setName("TileImageParameters");

		enableCustomTileProcessingCheckButton = new JCheckBox(
				I18nUtils.localizedStringForKey("lp_tile_param_recreate_checkbox_title"));
		enableCustomTileProcessingCheckButton.addActionListener(new EnableCustomTileSizeCheckButtonListener());
		enableCustomTileProcessingCheckButton.setToolTipText(I18nUtils
				.localizedStringForKey("lp_tile_param_recreate_checkbox_tips"));

		tileSizeWidthLabel = new JLabel(I18nUtils.localizedStringForKey("lp_tile_param_width_title"));
		tileSizeWidth = new JTileSizeCombo();
		tileSizeWidth.setToolTipText(I18nUtils.localizedStringForKey("lp_tile_param_width_tips"));

		tileSizeHeightLabel = new JLabel(I18nUtils.localizedStringForKey("lp_tile_param_height_title"));
		tileSizeHeight = new JTileSizeCombo();
		tileSizeHeight.setToolTipText(I18nUtils.localizedStringForKey("lp_tile_param_height_tips"));

		tileImageFormatLabel = new JLabel(I18nUtils.localizedStringForKey("lp_tile_param_image_fmt_title"));
		tileImageFormat = new JComboBox<TileImageFormat>(new TileFormatComboModel(TileImageFormat.values()));
		tileImageFormat.setMaximumRowCount(tileImageFormat.getItemCount());
		tileImageFormat.addActionListener(new TileImageFormatListener());

		GBC gbc_std = GBC.std().insets(5, 2, 5, 3);
		GBC gbc_eol = GBC.eol().insets(5, 2, 5, 3);

		JPanel tileSizePanel = new JPanel(new GridBagLayout());
		tileSizePanel.add(tileSizeWidthLabel, gbc_std);
		tileSizePanel.add(tileSizeWidth, gbc_std);
		tileSizePanel.add(tileSizeHeightLabel, gbc_std);
		tileSizePanel.add(tileSizeHeight, gbc_eol);
		JPanel tileColorDepthPanel = new JPanel();
		tileColorDepthPanel.add(tileImageFormatLabel);
		tileColorDepthPanel.add(tileImageFormat);
		contentContainer.add(enableCustomTileProcessingCheckButton, gbc_eol);
		contentContainer.add(tileSizePanel, GBC.eol());
		contentContainer.add(tileColorDepthPanel, GBC.eol());
	}

	public void loadSettings() {
		Settings settings = Settings.getInstance();
		enableCustomTileProcessingCheckButton.setSelected(settings.isCustomTileSize());
		updateControlsState();
		tileImageFormat.setSelectedItem(settings.getTileImageFormat());
		tileSizeHeight.setValue(settings.getTileSize().height);
		tileSizeWidth.setValue(settings.getTileSize().width);
	}

	public void saveSettings() {
		Settings settings = Settings.getInstance();
		settings.setCustomTileSize(enableCustomTileProcessingCheckButton.isSelected());
		Dimension tileSize = new Dimension(tileSizeWidth.getValue(), tileSizeHeight.getValue());
		settings.setTileSize(tileSize);
		settings.setTileImageFormat((TileImageFormat) tileImageFormat.getSelectedItem());
	}

	public TileImageParameters getSelectedTileImageParameters() {
		TileImageParameters customTileParameters = null;
		boolean customTileSize = enableCustomTileProcessingCheckButton.isSelected();
		if (customTileSize) {
			int width = tileSizeWidth.getValue();
			int height = tileSizeHeight.getValue();
			TileImageFormat format = (mobac.program.model.TileImageFormat) tileImageFormat.getSelectedItem();
			customTileParameters = new TileImageParameters(width, height, format);
		}
		return customTileParameters;
	}

	public void atlasFormatChanged(AtlasOutputFormat newAtlasOutputFormat) {
		Class<? extends AtlasCreator> atlasCreatorClass = newAtlasOutputFormat.getMapCreatorClass();
		SupportedParameters params = atlasCreatorClass.getAnnotation(SupportedParameters.class);
		if (params != null) {
			TreeSet<TileImageParameters.Name> paramNames = new TreeSet<TileImageParameters.Name>(Arrays.asList(params
					.names()));
			if (paramNames.contains(Name.format)) {
				formatPngEnabled = true;
				formatJpgEnabled = true;
			} else {
				formatPngEnabled = paramNames.contains(Name.format_png);
				formatJpgEnabled = paramNames.contains(Name.format_jpg);
			}
			widthEnabled = paramNames.contains(Name.width);
			heightEnabled = paramNames.contains(Name.height);
			enableCustomTileProcessingCheckButton.setEnabled(true);
		} else {
			formatPngEnabled = false;
			formatJpgEnabled = false;
			widthEnabled = false;
			heightEnabled = false;
			enableCustomTileProcessingCheckButton.setEnabled(false);
		}
		updateControlsState();
	}

	public void updateControlsState() {
		boolean b = false;
		if (enableCustomTileProcessingCheckButton.isEnabled())
			b = enableCustomTileProcessingCheckButton.isSelected();
		tileSizeWidth.setEnabled(b && widthEnabled);
		tileSizeWidthLabel.setEnabled(b && widthEnabled);
		tileSizeHeightLabel.setEnabled(b && heightEnabled);
		tileSizeHeight.setEnabled(b && heightEnabled);
		boolean formatEnabled = formatJpgEnabled || formatPngEnabled;
		tileImageFormatLabel.setEnabled(b && formatEnabled);
		tileImageFormat.setEnabled(b && formatEnabled);
		if (formatPngEnabled && !formatJpgEnabled)
			updateFormatComboModel(TileImageFormat.getPngFormats());
		else if (!formatPngEnabled && formatJpgEnabled)
			updateFormatComboModel(TileImageFormat.getJpgFormats());
		else
			updateFormatComboModel(TileImageFormat.values());
	}

	private void updateFormatComboModel(TileImageFormat[] values) {
		TileFormatComboModel model = (TileFormatComboModel) tileImageFormat.getModel();
		model.changeValues(values);
	}

	public String getValidationErrorMessages() {
		String errorText = "";
		if (!enableCustomTileProcessingCheckButton.isSelected())
			return errorText;
		if (!tileSizeHeight.isValueValid())
			errorText += String.format(I18nUtils.localizedStringForKey("lp_tile_param_msg_valid_height"),
					JTileSizeCombo.MIN, JTileSizeCombo.MAX);

		if (!tileSizeWidth.isValueValid())
			errorText += String.format(I18nUtils.localizedStringForKey("lp_tile_param_msg_valid_width"),
					JTileSizeCombo.MIN, JTileSizeCombo.MAX);
		return errorText;
	}

	private class EnableCustomTileSizeCheckButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			updateControlsState();
		}
	}

	private class TileImageFormatListener implements ActionListener {

		public void actionPerformed(ActionEvent event) {
			if (!tileImageFormat.isEnabled())
				return;
			TileImageFormat tif = (TileImageFormat) tileImageFormat.getSelectedItem();
			if (tif == null)
				return;
			if (!JPEG_TESTED && (tif.getDataWriter() instanceof TileImageJpegDataWriter)) {
				if (!TileImageJpegDataWriter.performOpenJDKJpegTest())
					JOptionPane.showMessageDialog(null, "<html>The JPEG image format is not supported by OpenJDK.<br>"
							+ "Please select a different tile format.</html>", "Image format not available on OpenJDK",
							JOptionPane.ERROR_MESSAGE);
				JPEG_TESTED = true;
			} else if (tif == TileImageFormat.PNG4Bit || tif == TileImageFormat.PNG8Bit) {
				if (Utilities.testJaiColorQuantizerAvailable())
					return;
				JOptionPane.showMessageDialog(null,
						"<html>This image format is requires additional libraries to be installed:<br>"
								+ "<b>Java Advanced Image library</b> (jai_core.jar & jai_codec.jar)<br>"
								+ "For more details please see the file <b>README.HTM</b> "
								+ "in section <b>Requirements</b>.</html>",
						"Image format not available - libraries missing", JOptionPane.ERROR_MESSAGE);
				tileImageFormat.setSelectedIndex(0);
			}
		}
	}

	private class TileFormatComboModel extends AbstractListModel<TileImageFormat> implements
			ComboBoxModel<TileImageFormat> {

		TileImageFormat[] values;
		Object selectedObject = null;

		public TileFormatComboModel(TileImageFormat[] values) {
			super();
			this.values = values;
			if (values.length > 0)
				selectedObject = values[0];
		}

		public void changeValues(TileImageFormat[] values) {
			this.values = values;
			boolean found = false;
			for (TileImageFormat format : values) {
				if (format.equals(selectedObject)) {
					found = true;
					break;
				}
			}
			if (!found)
				selectedObject = values[0];
			fireContentsChanged(this, -1, -1);
		}

		public int getSize() {
			return values.length;
		}

		public TileImageFormat getElementAt(int index) {
			return values[index];
		}

		@Override
		public void setSelectedItem(Object anItem) {
			if ((selectedObject != null && !selectedObject.equals(anItem)) || selectedObject == null && anItem != null) {
				selectedObject = anItem;
				fireContentsChanged(this, -1, -1);
			}
		}

		@Override
		public Object getSelectedItem() {
			return selectedObject;
		}

	}

}
