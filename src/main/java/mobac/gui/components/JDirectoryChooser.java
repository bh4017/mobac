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
package mobac.gui.components;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import mobac.utilities.I18nUtils;

public class JDirectoryChooser extends JFileChooser {

	private static final long serialVersionUID = -1954689476383812988L;

	public JDirectoryChooser() {
		super();
		setDialogType(CUSTOM_DIALOG);
		setDialogTitle(I18nUtils.localizedStringForKey("dlg_select_dir_title"));
		//setApproveButtonText("Select Directory");
		setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		setAcceptAllFileFilterUsed(false);
		addChoosableFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}

			@Override
			public String getDescription() {
				return I18nUtils.localizedStringForKey("dlg_select_dir_description");
			}
		});
	}

	@Override
	public void approveSelection() {
		if (!this.getFileFilter().accept(this.getSelectedFile()))
			return;
		super.approveSelection();
	}

}
