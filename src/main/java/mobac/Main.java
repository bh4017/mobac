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
package mobac;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import mobac.gui.MainGUI;
import mobac.gui.SplashFrame;
import mobac.mapsources.DefaultMapSourcesManager;
import mobac.program.DirectoryManager;
import mobac.program.EnvironmentSetup;
import mobac.program.Logging;
import mobac.program.ProgramInfo;
import mobac.program.commandline.CommandLineEmpty;
import mobac.program.commandline.CreateAtlas;
import mobac.program.interfaces.CommandLineAction;
import mobac.program.model.Settings;
import mobac.program.tilestore.TileStore;
import mobac.utilities.GUIExceptionHandler;

/**
 * Java 6 version of the main starter class
 */
public class Main {

	protected CommandLineAction cmdAction = new CommandLineEmpty();

	public Main() {
		try {
			parseCommandLine();
			if (cmdAction.showSplashScreen())
				SplashFrame.showFrame();

			DirectoryManager.initialize();
			Logging.configureLogging();

			// MySocketImplFactory.install();
			ProgramInfo.initialize(); // Load revision info
			Logging.logSystemInfo();
			GUIExceptionHandler.installToolkitEventQueueProxy();
			// Logging.logSystemProperties();
			ImageIO.setUseCache(false);

			EnvironmentSetup.checkFileSetup();
			Settings.loadOrQuit();
			EnvironmentSetup.checkMemory();

			EnvironmentSetup.copyMapPacks();
			DefaultMapSourcesManager.initialize();
			EnvironmentSetup.createDefaultAtlases();
			TileStore.initialize();
			EnvironmentSetup.upgrade();
			cmdAction.runBeforeMainGUI();
			if (cmdAction.showMainGUI()) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						Logging.LOG.debug("Starting GUI");
						MainGUI.createMainGui();
						SplashFrame.hideFrame();
						cmdAction.runMainGUI();
					}
				});
			}
		} catch (Throwable t) {
			GUIExceptionHandler.processException(t);
			System.exit(1);
		}
	}

	protected void parseCommandLine() {
		String[] args = StartMOBAC.ARGS;
		if (args.length >= 2) {
			if ("create".equalsIgnoreCase(args[0])) {
				if (args.length > 2)
					cmdAction = new CreateAtlas(args[1], args[2]);
				else
					cmdAction = new CreateAtlas(args[1]);
				return;
			}
		}
	}

	/**
	 * Start MOBAC without Java Runtime version check
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			new Main();
		} catch (Throwable t) {
			GUIExceptionHandler.processException(t);
		}
	}
}
