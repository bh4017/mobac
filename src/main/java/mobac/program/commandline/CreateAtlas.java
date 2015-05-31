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
package mobac.program.commandline;

import java.io.File;

import javax.xml.bind.JAXBException;

import mobac.program.AtlasThread;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.CommandLineAction;
import mobac.program.model.Profile;
import mobac.utilities.GUIExceptionHandler;

public class CreateAtlas implements CommandLineAction {

	private final String profileName;
	private final File outputDir;

	public CreateAtlas(String profileName) {
		this(profileName, null);
	}

	public CreateAtlas(String profileName, String outputDirectory) {
		super();
		this.profileName = profileName;
		if (outputDirectory != null) {
			File dir = new File(outputDirectory);
			if (dir.isDirectory() || dir.exists()) {
				System.err.println("Error: Atlas output directory \"" + outputDirectory + "\" already exists.");
				System.exit(1);
			}
			outputDir = dir;
		} else
			outputDir = null;
	}

	@Override
	public void runBeforeMainGUI() {
		try {
			Profile p = new Profile(profileName);
			if (!p.exists()) {
				System.err.println("Profile \"" + profileName + "\" could not be loaded:");
				System.err.println("File \"" + p.getFile().getAbsolutePath() + "\" does not exist.");
				System.exit(1);
			}
			AtlasInterface atlas = null;
			try {
				atlas = p.load();
			} catch (JAXBException e) {
				System.err.println("Error loading profile \"" + profileName + "\".");
				e.printStackTrace();
				System.exit(1);
			}
			AtlasThread atlasThread = new AtlasThread(atlas);
			if (outputDir != null)
				atlasThread.setCustomAtlasDir(outputDir);
			atlasThread.setQuitMobacAfterAtlasCreation(true);
			atlasThread.start();
		} catch (Exception e) {
			GUIExceptionHandler.processException(e);
		}
	}

	@Override
	public void runMainGUI() {
	}

	@Override
	public boolean showSplashScreen() {
		return false;
	}

	@Override
	public boolean showMainGUI() {
		return false;
	}

}
