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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.xml.bind.JAXBException;

import mobac.exceptions.MapSourceInitializationException;
import mobac.externaltools.ExternalToolDef;
import mobac.externaltools.ExternalToolsLoader;
import mobac.gui.actions.AddGpxTrackAreaPolygonMap;
import mobac.gui.actions.AddGpxTrackPolygonMap;
import mobac.gui.actions.AddMapLayer;
import mobac.gui.actions.AtlasConvert;
import mobac.gui.actions.AtlasCreate;
import mobac.gui.actions.AtlasNew;
import mobac.gui.actions.BookmarkAdd;
import mobac.gui.actions.BookmarkManage;
import mobac.gui.actions.DebugSetLogLevel;
import mobac.gui.actions.DebugShowLogFile;
import mobac.gui.actions.DebugShowMapSourceNames;
import mobac.gui.actions.DebugShowMapTileGrid;
import mobac.gui.actions.DebugShowReport;
import mobac.gui.actions.HelpLicenses;
import mobac.gui.actions.PanelShowHide;
import mobac.gui.actions.RefreshCustomMapsources;
import mobac.gui.actions.SelectionModeCircle;
import mobac.gui.actions.SelectionModePolygon;
import mobac.gui.actions.SelectionModeRectangle;
import mobac.gui.actions.SettingsButtonListener;
import mobac.gui.actions.ShowAboutDialog;
import mobac.gui.actions.ShowHelpAction;
import mobac.gui.actions.ShowReadme;
import mobac.gui.atlastree.JAtlasTree;
import mobac.gui.components.FilledLayeredPane;
import mobac.gui.components.JAtlasNameField;
import mobac.gui.components.JBookmarkMenuItem;
import mobac.gui.components.JCollapsiblePanel;
import mobac.gui.components.JMapSourceTree;
import mobac.gui.components.JMenuItem2;
import mobac.gui.components.JZoomCheckBox;
import mobac.gui.listeners.AtlasModelListener;
import mobac.gui.mapview.GridZoom;
import mobac.gui.mapview.JMapViewer;
import mobac.gui.mapview.PreviewMap;
import mobac.gui.mapview.WgsGrid.WgsDensity;
import mobac.gui.mapview.controller.JMapController;
import mobac.gui.mapview.controller.PolygonCircleSelectionMapController;
import mobac.gui.mapview.controller.PolygonSelectionMapController;
import mobac.gui.mapview.controller.RectangleSelectionMapController;
import mobac.gui.mapview.interfaces.MapEventListener;
import mobac.gui.panels.JCoordinatesPanel;
import mobac.gui.panels.JGpxPanel;
import mobac.gui.panels.JMapSourcesPanel;
import mobac.gui.panels.JProfilesPanel;
import mobac.gui.panels.JTileImageParametersPanel;
import mobac.gui.panels.JTileStoreCoveragePanel;
import mobac.mapsources.MapSourcesManager;
import mobac.program.ProgramInfo;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.InitializableMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.model.Bookmark;
import mobac.program.model.MapSelection;
import mobac.program.model.MercatorPixelCoordinate;
import mobac.program.model.Profile;
import mobac.program.model.SelectedZoomLevels;
import mobac.program.model.Settings;
import mobac.program.model.SettingsWgsGrid;
import mobac.program.model.TileImageParameters;
import mobac.utilities.GBC;
import mobac.utilities.GUIExceptionHandler;
import mobac.utilities.I18nUtils;
import mobac.utilities.Utilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class MainGUI extends JFrame implements MapEventListener {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(MainGUI.class);

	private static Color labelBackgroundColor = new Color(0, 0, 0, 127);
	private static Color checkboxBackgroundColor = new Color(0, 0, 0, 40);
	private static Color labelForegroundColor = Color.WHITE;

	// At this moment, smaller value than 254 will occur in cut right edge of the left panel
	public static final int LEFT_PANEL_MIN_SIZE = 254;
	private static final int LEFT_PANEL_MAX_SIZE = LEFT_PANEL_MIN_SIZE + 250;
	private static final int LEFT_PANEL_SIZE_STEP = 25;

	private static final int LEFT_PANEL_MARGIN = 2;

	public static int leftPanelWidth;

	private static MainGUI mainGUI = null;
	public static final ArrayList<Image> MOBAC_ICONS = new ArrayList<Image>(3);

	static {
		MOBAC_ICONS.add(Utilities.loadResourceImageIcon("mobac48.png").getImage());
		MOBAC_ICONS.add(Utilities.loadResourceImageIcon("mobac32.png").getImage());
		MOBAC_ICONS.add(Utilities.loadResourceImageIcon("mobac16.png").getImage());
	}

	protected JMenuBar menuBar;
	protected JMenu externalToolsMenu = null;

	private JMenu bookmarkMenu = null;

	public final PreviewMap previewMap = new PreviewMap();
	public final JAtlasTree jAtlasTree = new JAtlasTree(previewMap);

	private JCheckBox wgsGridCheckBox;
	private JComboBox wgsGridCombo;

	private JLabel zoomLevelText;
	private JComboBox gridZoomCombo;
	private JSlider zoomSlider;
	private JMapSourceTree mapSourceTree;
	private JAtlasNameField atlasNameTextField;
	private JButton createAtlasButton;
	private JPanel zoomLevelPanel;
	private JZoomCheckBox[] cbZoom = new JZoomCheckBox[0];
	private JLabel amountOfTilesLabel;
	private JMapSourcesPanel mapSourcePanel;

	private AtlasCreate atlasCreateAction = new AtlasCreate(jAtlasTree);

	private JCoordinatesPanel coordinatesPanel;
	private JProfilesPanel profilesPanel;
	public JTileImageParametersPanel tileImageParametersPanel;
	private JTileStoreCoveragePanel tileStoreCoveragePanel;
	public JGpxPanel gpxPanel;

	private JPanel mapControlPanel = new JPanel(new BorderLayout());
	private JPanel leftPanel = new JPanel(new GridBagLayout());
	private JPanel leftPanelContent = null;
	private JPanel rightPanel = new JPanel(new GridBagLayout());

	public JMenu logLevelMenu;
	private JMenuItem smRectangle;
	private JMenuItem smPolygon;
	private JMenuItem smCircle;

	private MercatorPixelCoordinate mapSelectionMax = null;
	private MercatorPixelCoordinate mapSelectionMin = null;

	public static void createMainGui() {
		if (mainGUI != null)
			return;

		mainGUI = new MainGUI();
		mainGUI.setVisible(true);
		log.trace("MainGUI now visible");
	}

	public static MainGUI getMainGUI() {
		return mainGUI;
	}

	// MP: get custom font
	static Font sCustomFont = null;

	private JMenuItem leftPanelShrink;
	private JMenuItem leftPanelExpand;

	public static Font customFont() {
		if (sCustomFont == null) {
			// force to use Chinese font
			sCustomFont = new Font("宋体", 9, 13);
		}
		return sCustomFont;
	}

	// MP: update all UI components' default font to custom font
	public static void setDefaultFontOfAllUIComponents(Font defaultFont) {
		if (defaultFont != null) {
			// register custom font to application，system font will return false
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(defaultFont);

			// update all UI's font settings
			javax.swing.plaf.FontUIResource fontRes = new javax.swing.plaf.FontUIResource(defaultFont);
			Enumeration<Object> keys = UIManager.getDefaults().keys();
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				Object value = UIManager.get(key);
				if (value instanceof javax.swing.plaf.FontUIResource) {
					UIManager.put(key, fontRes);
				}
			}
		}
	}

	private MainGUI() {
		super();
		mainGUI = this;
		setIconImages(MOBAC_ICONS);

		GUIExceptionHandler.registerForCurrentThread();
		setTitle(ProgramInfo.getCompleteTitle());

		log.trace("Creating main dialog - " + getTitle());
		setResizable(true);
		Dimension dScreen = Toolkit.getDefaultToolkit().getScreenSize();
		setMinimumSize(new Dimension(Math.min(800, dScreen.width), Math.min(590, dScreen.height)));
		setSize(getMinimumSize());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new WindowDestroyer());
		addComponentListener(new MainWindowListener());

		previewMap.addMapEventListener(this);

		createControls();
		calculateNrOfTilesToDownload();
		setLayout(new BorderLayout());
		add(leftPanel, BorderLayout.WEST);
		add(rightPanel, BorderLayout.EAST);
		JLayeredPane layeredPane = new FilledLayeredPane();
		layeredPane.add(previewMap, Integer.valueOf(0));
		layeredPane.add(mapControlPanel, Integer.valueOf(1));
		add(layeredPane, BorderLayout.CENTER);

		updateMapControlsPanel();
		updateLeftPanel();
		updateRightPanel();
		updateZoomLevelCheckBoxes();
		calculateNrOfTilesToDownload();

		menuBar = new JMenuBar();
		prepareMenuBar();
		setJMenuBar(menuBar);

		loadSettings();
		profilesPanel.initialize();
		mapSourceChanged(previewMap.getMapSource());
		updateZoomLevelCheckBoxes();
		updateGridSizeCombo();
		tileImageParametersPanel.updateControlsState();
		zoomChanged(previewMap.getZoom());
		gridZoomChanged(previewMap.getGridZoom());
		previewMap.updateMapSelection();
		previewMap.grabFocus();
	}

	private void createControls() {

		// zoom slider
		zoomSlider = new JSlider(JMapViewer.MIN_ZOOM, previewMap.getMapSource().getMaxZoom());
		zoomSlider.setOrientation(JSlider.HORIZONTAL);
		zoomSlider.setMinimumSize(new Dimension(50, 10));
		zoomSlider.setSize(50, zoomSlider.getPreferredSize().height);
		zoomSlider.addChangeListener(new ZoomSliderListener());
		zoomSlider.setOpaque(false);

		// zoom level text
		zoomLevelText = new JLabel(" 00 ");
		zoomLevelText.setOpaque(true);
		zoomLevelText.setBackground(labelBackgroundColor);
		zoomLevelText.setForeground(labelForegroundColor);
		zoomLevelText.setToolTipText(I18nUtils.localizedStringForKey("map_ctrl_zoom_level_title_tips"));

		// grid zoom combo
		gridZoomCombo = new JComboBox();
		gridZoomCombo.setEditable(false);
		gridZoomCombo.addActionListener(new GridZoomComboListener());
		gridZoomCombo.setToolTipText(I18nUtils.localizedStringForKey("map_ctrl_zoom_grid_tips"));

		SettingsWgsGrid s = Settings.getInstance().wgsGrid;

		// WGS Grid label
		wgsGridCheckBox = new JCheckBox(I18nUtils.localizedStringForKey("map_ctrl_wgs_grid_title"), s.enabled);
		// wgsGridCheckBox.setOpaque(true);
		wgsGridCheckBox.setOpaque(true);
		wgsGridCheckBox.setBackground(checkboxBackgroundColor);
		wgsGridCheckBox.setForeground(labelForegroundColor);
		wgsGridCheckBox.setToolTipText(I18nUtils.localizedStringForKey("map_ctrl_wgs_grid_tips"));
		wgsGridCheckBox.setMargin(new Insets(0, 0, 0, 0));
		wgsGridCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean enabled = wgsGridCheckBox.isSelected();
				Settings.getInstance().wgsGrid.enabled = enabled;
				wgsGridCombo.setVisible(enabled);
				previewMap.repaint();
			}
		});

		// WGS Grid combo
		wgsGridCombo = new JComboBox(WgsDensity.values());
		wgsGridCombo.setMaximumRowCount(WgsDensity.values().length);
		wgsGridCombo.setVisible(s.enabled);
		wgsGridCombo.setSelectedItem(s.density);
		wgsGridCombo.setToolTipText(I18nUtils.localizedStringForKey("map_ctrl_wgs_grid_density_tips"));
		wgsGridCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WgsDensity d = (WgsDensity) wgsGridCombo.getSelectedItem();
				Settings.getInstance().wgsGrid.density = d;
				previewMap.repaint();
			}
		});

		// map source tree
		mapSourceTree = new JMapSourceTree(MapSourcesManager.getInstance().getEnabledOrderedMapSources());
		mapSourceTree.addTreeSelectionListener(new MapSourceTreeListener());
		mapSourceTree.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				boolean isLocationClickable = ((JMapSourceTree) e.getComponent()).isLocationClickable(e.getPoint());
				JTree jTree = (JTree) e.getComponent();
				// If a node is clickable, user will see a "hand" mouse cursor
				jTree.setCursor(isLocationClickable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor
						.getDefaultCursor());
			}
		});

		// atlas name text field
		atlasNameTextField = new JAtlasNameField();
		atlasNameTextField.setColumns(12);
		atlasNameTextField.setActionCommand("atlasNameTextField");
		atlasNameTextField.setToolTipText(I18nUtils.localizedStringForKey("lp_atlas_name_field_tips"));

		// main button
		createAtlasButton = new JButton(I18nUtils.localizedStringForKey("lp_mian_create_btn_title"));
		createAtlasButton.setIcon(new ImageIcon(MainGUI.class.getResource("/mobac/resources/images/atlas_create.png")));
		createAtlasButton.addActionListener(atlasCreateAction);
		createAtlasButton.setToolTipText(I18nUtils.localizedStringForKey("lp_main_create_btn_tips"));

		// zoom level check boxes
		zoomLevelPanel = new JPanel();
		zoomLevelPanel.setBorder(BorderFactory.createEmptyBorder());
		zoomLevelPanel.setOpaque(false);

		// amount of tiles to download
		amountOfTilesLabel = new JLabel();
		amountOfTilesLabel.setToolTipText(I18nUtils.localizedStringForKey("lp_zoom_total_tile_count_tips"));
		amountOfTilesLabel.setOpaque(true);
		amountOfTilesLabel.setBackground(labelBackgroundColor);
		amountOfTilesLabel.setForeground(labelForegroundColor);

		coordinatesPanel = new JCoordinatesPanel();
		tileImageParametersPanel = new JTileImageParametersPanel();
		profilesPanel = new JProfilesPanel(jAtlasTree);
		profilesPanel.getLoadButton().addActionListener(new LoadProfileListener());
		// THIS IS A HACK - 90 is fixed width difference for a tileStoreCoveragePanel, but I don't know how to obtain
		// its real value to avoid hardcoding
		int tileStoreCoveragePanelWidth = leftPanelWidth - 90;
		tileStoreCoveragePanel = new JTileStoreCoveragePanel(previewMap, tileStoreCoveragePanelWidth);
	}

	private void prepareMenuBar() {
		// Atlas menu
		JMenu atlasMenu = new JMenu(I18nUtils.localizedStringForKey("menu_atlas"));
		atlasMenu.setIcon(Utilities.loadResourceImageIcon("atlas.png"));
		atlasMenu.setMnemonic(KeyEvent.VK_A);

		JMenuItem newAtlas = new JMenuItem(I18nUtils.localizedStringForKey("menu_atlas_new"));
		newAtlas.setIcon(Utilities.loadResourceImageIcon("atlas_add.png"));
		newAtlas.setMnemonic(KeyEvent.VK_N);
		newAtlas.addActionListener(new AtlasNew());
		atlasMenu.add(newAtlas);

		JMenuItem convertAtlas = new JMenuItem(I18nUtils.localizedStringForKey("menu_atlas_convert_format"));
		convertAtlas.setIcon(Utilities.loadResourceImageIcon("atlas_convert.png"));
		convertAtlas.setMnemonic(KeyEvent.VK_V);
		convertAtlas.addActionListener(new AtlasConvert());
		atlasMenu.add(convertAtlas);
		atlasMenu.addSeparator();

		JMenuItem createAtlas = new JMenuItem(I18nUtils.localizedStringForKey("menu_atlas_create"));
		createAtlas.setIcon(Utilities.loadResourceImageIcon("atlas_create.png"));
		createAtlas.setMnemonic(KeyEvent.VK_C);
		createAtlas.addActionListener(atlasCreateAction);
		atlasMenu.add(createAtlas);

		// Selection menu - before, it was called "Maps menu"
		JMenu selectionMenu = new JMenu(I18nUtils.localizedStringForKey("menu_selection"));
		selectionMenu.setIcon(Utilities.loadResourceImageIcon("menu_icons/selections.png"));
		selectionMenu.setMnemonic(KeyEvent.VK_S);
		JMenu selectionModeMenu = new JMenu(I18nUtils.localizedStringForKey("menu_selection_selection"));
		selectionModeMenu.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_selection_mode.png"));
		selectionModeMenu.setMnemonic(KeyEvent.VK_M);
		selectionMenu.add(selectionModeMenu);

		smRectangle = new JCheckBoxMenuItem(I18nUtils.localizedStringForKey("menu_selection_selection_rect"));
		smRectangle.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_selection_mode.png"));
		smRectangle.addActionListener(new SelectionModeRectangle());
		smRectangle.setSelected(true);
		selectionModeMenu.add(smRectangle);

		smPolygon = new JCheckBoxMenuItem(I18nUtils.localizedStringForKey("menu_selection_selection_polygon"));
		smPolygon.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_selection_polygon.png"));
		smPolygon.addActionListener(new SelectionModePolygon());
		selectionModeMenu.add(smPolygon);

		smCircle = new JCheckBoxMenuItem(I18nUtils.localizedStringForKey("menu_selection_selection_circle"));
		smCircle.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_selection_circle.png"));
		smCircle.addActionListener(new SelectionModeCircle());
		selectionModeMenu.add(smCircle);

		ButtonGroup selectionModeGroup = new ButtonGroup();
		selectionModeGroup.add(smRectangle);
		selectionModeGroup.add(smPolygon);
		selectionModeGroup.add(smCircle);

		JMenuItem addSelection = new JMenuItem(I18nUtils.localizedStringForKey("menu_selection_selection_add"));
		addSelection.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_selection_add.png"));
		addSelection.addActionListener(AddMapLayer.INSTANCE);

		JSeparator selectionSeparator = new JSeparator();
		selectionMenu.add(selectionSeparator);
		addSelection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
		addSelection.setMnemonic(KeyEvent.VK_A);
		selectionMenu.add(addSelection);

		JMenuItem addGpxTrackSelection = new JMenuItem2(
				I18nUtils.localizedStringForKey("menu_selection_selection_add_around_gpx"), AddGpxTrackPolygonMap.class);
		addGpxTrackSelection.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_selection_add.png"));
		selectionMenu.add(addGpxTrackSelection);

		JMenuItem addGpxTrackAreaSelection = new JMenuItem2(
				I18nUtils.localizedStringForKey("menu_selection_selection_add_by_gpx"), AddGpxTrackAreaPolygonMap.class);
		addGpxTrackAreaSelection.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_selection_add.png"));
		selectionMenu.add(addGpxTrackAreaSelection);

		// Bookmarks menu
		bookmarkMenu = new JMenu(I18nUtils.localizedStringForKey("menu_bookmark"));
		bookmarkMenu.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_bookmarks.png"));
		bookmarkMenu.setMnemonic(KeyEvent.VK_B);
		JMenuItem addBookmark = new JMenuItem(I18nUtils.localizedStringForKey("menu_bookmark_save"));
		addBookmark.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_save_view.png"));
		addBookmark.setMnemonic(KeyEvent.VK_S);
		addBookmark.addActionListener(new BookmarkAdd(previewMap));
		bookmarkMenu.add(addBookmark);
		JMenuItem manageBookmarks = new JMenuItem2(I18nUtils.localizedStringForKey("menu_bookmark_manage"),
				BookmarkManage.class);
		manageBookmarks.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_manage_bookmarks.png"));
		manageBookmarks.setMnemonic(KeyEvent.VK_S);
		bookmarkMenu.add(addBookmark);
		bookmarkMenu.add(manageBookmarks);
		bookmarkMenu.addSeparator();

		menuBar.add(atlasMenu);
		menuBar.add(selectionMenu);
		menuBar.add(bookmarkMenu);

		loadExternalToolsMenu();

		menuBar.add(Box.createHorizontalGlue());

		// Debug menu
		JMenu debugMenu = new JMenu(I18nUtils.localizedStringForKey("menu_debug"));
		debugMenu.setIcon(Utilities.loadResourceImageIcon("icon_debug_ms.png"));
		JMenuItem mapGrid = new JCheckBoxMenuItem(I18nUtils.localizedStringForKey("menu_debug_show_hide_tile_border"),
				false);
		mapGrid.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_show_tile_borders.png"));
		mapGrid.addActionListener(new DebugShowMapTileGrid());
		debugMenu.add(mapGrid);
		debugMenu.addSeparator();

		debugMenu.setMnemonic(KeyEvent.VK_D);
		JMenuItem mapSourceNames = new JMenuItem2(I18nUtils.localizedStringForKey("menu_debug_show_all_map_source"),
				DebugShowMapSourceNames.class);
		mapSourceNames.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_show_mapsources.png"));
		mapSourceNames.setMnemonic(KeyEvent.VK_N);
		debugMenu.add(mapSourceNames);
		debugMenu.addSeparator();

		JMenuItem refreshCustomMapSources = new JMenuItem2(
				I18nUtils.localizedStringForKey("menu_debug_refresh_map_source"), RefreshCustomMapsources.class);
		refreshCustomMapSources.setIcon(Utilities.loadResourceImageIcon("refresh.png"));
		debugMenu.add(refreshCustomMapSources);
		debugMenu.addSeparator();
		JMenuItem showLog = new JMenuItem2(I18nUtils.localizedStringForKey("menu_debug_show_log_file"),
				DebugShowLogFile.class);
		showLog.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_show_logfile.png"));
		showLog.setMnemonic(KeyEvent.VK_S);
		debugMenu.add(showLog);

		logLevelMenu = new JMenu(I18nUtils.localizedStringForKey("menu_debug_log_level"));
		logLevelMenu.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_log_level.png"));
		logLevelMenu.setMnemonic(KeyEvent.VK_L);
		Level[] list = new Level[] { Level.TRACE, Level.DEBUG, Level.INFO, Level.ERROR, Level.FATAL, Level.OFF };
		ActionListener al = new DebugSetLogLevel();
		Level rootLogLevel = Logger.getRootLogger().getLevel();
		for (Level level : list) {
			String name = level.toString();
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(name, (rootLogLevel.toString().equals(name)));
			item.setName(name);
			item.addActionListener(al);
			logLevelMenu.add(item);
		}
		debugMenu.add(logLevelMenu);
		debugMenu.addSeparator();
		JMenuItem report = new JMenuItem2(I18nUtils.localizedStringForKey("menu_debug_system_report"),
				DebugShowReport.class);
		report.setIcon(Utilities.loadResourceImageIcon("menu_icons/menu_gen_sysreport.png"));
		report.setMnemonic(KeyEvent.VK_R);
		debugMenu.add(report);
		menuBar.add(debugMenu);

		// Tools menu
		JMenu toolsMenu = new JMenu(I18nUtils.localizedStringForKey("menu_tools"));
		toolsMenu
				.setIcon(new ImageIcon(MainGUI.class.getResource("/mobac/resources/images/menu_icons/menu_tools.png")));
		toolsMenu.setMnemonic(KeyEvent.VK_T);
		menuBar.add(toolsMenu);

		JMenuItem settingsMenuItem = new JMenuItem2(I18nUtils.localizedStringForKey("menu_tools_settings"),
				SettingsButtonListener.class);
		settingsMenuItem.setIcon(new ImageIcon(MainGUI.class
				.getResource("/mobac/resources/images/menu_icons/menu_settings.png")));
		toolsMenu.add(settingsMenuItem);
		JSeparator separator = new JSeparator();
		toolsMenu.add(separator);
		JMenuItem showRightPanel = new JMenuItem(I18nUtils.localizedStringForKey("menu_show_hide_gpx_panel"));
		showRightPanel.setIcon(new ImageIcon(MainGUI.class
				.getResource("/mobac/resources/images/menu_icons/menu_panels_right.png")));
		showRightPanel.addActionListener(new PanelShowHide(rightPanel));

		JMenu mnLeftPanel = new JMenu(I18nUtils.localizedStringForKey("menu_tools_leftpanel"));
		mnLeftPanel.setIcon(new ImageIcon(MainGUI.class
				.getResource("/mobac/resources/images/menu_icons/menu_panels_left.png")));
		toolsMenu.add(mnLeftPanel);

		JMenuItem showLeftPanel = new JMenuItem(I18nUtils.localizedStringForKey("menu_show_hide_left_panel"));
		mnLeftPanel.add(showLeftPanel);
		showLeftPanel.setIcon(new ImageIcon(MainGUI.class
				.getResource("/mobac/resources/images/menu_icons/menu_panels_left.png")));

		leftPanelExpand = new JMenuItem(I18nUtils.localizedStringForKey("menu_show_expand_left_panel"));
		mnLeftPanel.add(leftPanelExpand);
		leftPanelExpand.setIcon(new ImageIcon(MainGUI.class
				.getResource("/mobac/resources/images/menu_icons/left_panel_expand.png")));

		leftPanelShrink = new JMenuItem(I18nUtils.localizedStringForKey("menu_show_shrink_left_panel"));
		mnLeftPanel.add(leftPanelShrink);

		// Should the listeners below be transfered to the mobac.gui.actions ?

		leftPanelShrink.setIcon(new ImageIcon(MainGUI.class
				.getResource("/mobac/resources/images/menu_icons/left_panel_contract.png")));
		leftPanelShrink.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				resizeLeftPanel(-LEFT_PANEL_SIZE_STEP);
			}
		});
		leftPanelExpand.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				resizeLeftPanel(LEFT_PANEL_SIZE_STEP);
			}
		});
		ensureLeftPanelResizable();
		showLeftPanel.addActionListener(new PanelShowHide(leftPanel));
		toolsMenu.add(showRightPanel);

		// Help menu
		JMenu help = new JMenu(I18nUtils.localizedStringForKey("menu_help"));
		help.setIcon(new ImageIcon(MainGUI.class.getResource("/mobac/resources/images/menu_icons/menu_help.png")));
		JMenuItem readme = new JMenuItem(I18nUtils.localizedStringForKey("menu_help_readme"));
		readme.setIcon(new ImageIcon(MainGUI.class.getResource("/mobac/resources/images/menu_icons/menu_info.png")));
		JMenuItem howToMap = new JMenuItem(I18nUtils.localizedStringForKey("menu_help_how_to_preview"));
		howToMap.setIcon(new ImageIcon(MainGUI.class.getResource("/mobac/resources/images/menu_icons/menu_info.png")));
		JMenuItem licenses = new JMenuItem(I18nUtils.localizedStringForKey("menu_help_licenses"));
		licenses.setIcon(new ImageIcon(MainGUI.class
				.getResource("/mobac/resources/images/menu_icons/menu_licenses.png")));
		JMenuItem about = new JMenuItem(I18nUtils.localizedStringForKey("menu_help_about"));
		about.setIcon(new ImageIcon(MainGUI.class.getResource("/mobac/resources/images/mobac16.png")));
		readme.addActionListener(new ShowReadme());
		about.addActionListener(new ShowAboutDialog());
		howToMap.addActionListener(new ShowHelpAction());
		licenses.addActionListener(new HelpLicenses());
		help.add(readme);
		help.add(howToMap);
		help.addSeparator();
		help.add(licenses);
		help.addSeparator();
		help.add(about);

		menuBar.add(help);
	}

	public void loadExternalToolsMenu() {
		if (ExternalToolsLoader.load()) {
			if (externalToolsMenu == null) {
				externalToolsMenu = new JMenu(I18nUtils.localizedStringForKey("menu_external_tools"));
				externalToolsMenu.setIcon(new ImageIcon(MainGUI.class
						.getResource("/mobac/resources/images/menu_icons/menu_external_tools.png")));
				externalToolsMenu.setMnemonic(KeyEvent.VK_E);
				externalToolsMenu.addMenuListener(new MenuListener() {

					public void menuSelected(MenuEvent e) {
						loadExternalToolsMenu();
						log.debug("External Tools menu Loaded");
					}

					public void menuDeselected(MenuEvent e) {
					}

					public void menuCanceled(MenuEvent e) {
					}
				});
				menuBar.add(externalToolsMenu);
			}
			externalToolsMenu.removeAll();
			for (ExternalToolDef t : ExternalToolsLoader.tools) {
				JMenuItem externalToolMenuItem = new JMenuItem(t.name);
				externalToolMenuItem.addActionListener(t);
				externalToolMenuItem.setIcon(new ImageIcon(MainGUI.class
						.getResource("/mobac/resources/images/menu_icons/menu_external_tool_item.png")));
				externalToolsMenu.add(externalToolMenuItem);
			}
		}
	}

	private void leftPanelUpdateWidth() {
		leftPanel.setPreferredSize(new Dimension(leftPanelWidth + LEFT_PANEL_MARGIN, (int) leftPanel.getPreferredSize()
				.getHeight()));
	}

	private void ensureLeftPanelResizable() {
		leftPanelShrink.setEnabled(leftPanelWidth > LEFT_PANEL_MIN_SIZE);
		leftPanelExpand.setEnabled(leftPanelWidth < LEFT_PANEL_MAX_SIZE);
	}

	private void updateLeftPanel() {
		leftPanel.removeAll();

		coordinatesPanel.addButtonActionListener(new ApplySelectionButtonListener());

		mapSourcePanel = new JMapSourcesPanel(mapSourceTree);

		JCollapsiblePanel zoomLevelsPanel = new JCollapsiblePanel(I18nUtils.localizedStringForKey("lp_zoom_title"),
				new GridBagLayout());
		zoomLevelsPanel.addContent(zoomLevelPanel, GBC.eol().insets(2, 4, 2, 0));
		zoomLevelsPanel.addContent(amountOfTilesLabel, GBC.std().anchor(GBC.WEST).insets(0, 5, 0, 2));

		int leftPanelVerticalScrollWidth = 14;

		GBC gbc_std = GBC.std().insets(5, 2, 5, 3);
		GBC gbc_eol = GBC.eol().insets(LEFT_PANEL_MARGIN, 2, LEFT_PANEL_MARGIN, 3);

		JCollapsiblePanel atlasContentPanel = new JCollapsiblePanel(I18nUtils.localizedStringForKey("lp_atlas_title"),
				new GridBagLayout());
		JScrollPane treeScrollPane = new JScrollPane(jAtlasTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jAtlasTree.getTreeModel().addTreeModelListener(new AtlasModelListener(jAtlasTree, profilesPanel));

		treeScrollPane.setPreferredSize(new Dimension(100, 200));
		treeScrollPane.setAutoscrolls(true);
		atlasContentPanel.addContent(treeScrollPane, GBC.eol().fill().insets(0, 1, 0, 0));
		JButton clearAtlas = new JButton(I18nUtils.localizedStringForKey("lp_atlas_new_btn_title"));
		atlasContentPanel.addContent(clearAtlas, GBC.std());
		clearAtlas.addActionListener(new AtlasNew());
		JButton addLayers = new JButton(I18nUtils.localizedStringForKey("lp_atlas_add_selection_btn_title"));
		atlasContentPanel.addContent(addLayers, GBC.eol());
		addLayers.addActionListener(AddMapLayer.INSTANCE);
		atlasContentPanel.addContent(new JLabel(I18nUtils.localizedStringForKey("lp_atlas_name_label_title")), gbc_std);
		atlasContentPanel.addContent(atlasNameTextField, gbc_eol.fill(GBC.HORIZONTAL));

		leftPanelContent = new JPanel(new GridBagLayout());
		leftPanelContent.add(mapSourcePanel, gbc_eol);
		leftPanelContent.add(zoomLevelsPanel, gbc_eol);
		leftPanelContent.add(atlasContentPanel, gbc_eol);
		leftPanelContent.add(profilesPanel, gbc_eol);
		leftPanelContent.add(createAtlasButton, gbc_eol);
		leftPanelContent.add(tileImageParametersPanel, gbc_eol);
		leftPanelContent.add(tileStoreCoveragePanel, gbc_eol);
		leftPanelContent.add(coordinatesPanel, gbc_eol);
		leftPanelContent.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

		JScrollPane scrollPane = new JScrollPane(leftPanelContent);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(leftPanelVerticalScrollWidth, 0));
		leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
		leftPanelUpdateWidth();
		leftPanel.add(scrollPane, GBC.std().fill());
	}

	private void resizeLeftPanel(int pixels) {
		if ((pixels < 0 && leftPanelWidth <= LEFT_PANEL_MIN_SIZE)
				|| (pixels > 0 && leftPanelWidth >= LEFT_PANEL_MAX_SIZE)) {
			return;
		}

		leftPanelWidth += pixels;
		leftPanelUpdateWidth();
		ensureLeftPanelResizable();
		previewMap.revalidate();
	}

	private void updateRightPanel() {
		GBC gbc_eol = GBC.eol().insets(5, 2, 5, 2).fill();
		gpxPanel = new JGpxPanel(previewMap);
		rightPanel.add(gpxPanel, gbc_eol);
	}

	private JPanel updateMapControlsPanel() {
		mapControlPanel.removeAll();
		mapControlPanel.setOpaque(false);

		// zoom label
		JLabel zoomLabel = new JLabel(I18nUtils.localizedStringForKey("map_ctrl_zoom_level_title"));
		zoomLabel.setOpaque(true);
		zoomLabel.setBackground(labelBackgroundColor);
		zoomLabel.setForeground(labelForegroundColor);

		// top panel
		JPanel topControls = new JPanel(new GridBagLayout());
		topControls.setOpaque(false);
		topControls.add(zoomLabel, GBC.std().insets(5, 5, 0, 0));
		topControls.add(zoomSlider, GBC.std().insets(0, 5, 0, 0));
		topControls.add(zoomLevelText, GBC.std().insets(0, 5, 0, 0));
		topControls.add(gridZoomCombo, GBC.std().insets(10, 5, 0, 0));
		topControls.add(wgsGridCheckBox, GBC.std().insets(10, 5, 0, 0));
		topControls.add(wgsGridCombo, GBC.std().insets(5, 5, 0, 0));
		topControls.add(Box.createHorizontalGlue(), GBC.std().fillH());
		mapControlPanel.add(topControls, BorderLayout.NORTH);

		// bottom panel
		// JPanel bottomControls = new JPanel(new GridBagLayout());
		// bottomControls.setOpaque(false);
		// bottomControls.add(Box.createHorizontalGlue(),
		// GBC.std().fill(GBC.HORIZONTAL));
		// mapControlPanel.add(bottomControls, BorderLayout.SOUTH);

		return mapControlPanel;
	}

	public void updateMapSourcesList() {
		mapSourceTree.selectClickedMapSource();
		MapSource ms = mapSourceTree.getSelectedMapSource();
		mapSourceTree.initialize(MapSourcesManager.getInstance().getEnabledOrderedMapSources());
		if (!mapSourceTree.selectMapSource(ms)) {
			mapSourceTree.selectFirstMapSource();
		}
		MapSource ms2 = mapSourceTree.getSelectedMapSource();

		if (!ms.equals(ms2))
			previewMap.setMapSource(ms2);
	}

	public void updateBookmarksMenu() {
		LinkedList<JMenuItem> items = new LinkedList<JMenuItem>();
		for (int i = 0; i < bookmarkMenu.getMenuComponentCount(); i++) {
			JMenuItem item = bookmarkMenu.getItem(i);
			if (!(item instanceof JBookmarkMenuItem))
				items.add(item);
		}
		bookmarkMenu.removeAll();
		for (JMenuItem item : items) {
			if (item != null) {
				bookmarkMenu.add(item);
			} else {
				bookmarkMenu.addSeparator();
			}
		}
		for (Bookmark bookmark : Settings.getInstance().placeBookmarks) {
			JBookmarkMenuItem bookmarkMenuItem = new JBookmarkMenuItem(bookmark);
			bookmarkMenuItem.setIcon(new ImageIcon(MainGUI.class
					.getResource("/mobac/resources/images/menu_icons/menu_bookmark_item.png")));
			bookmarkMenu.add(bookmarkMenuItem);
		}
	}

	private void loadSettings() {
		if (Profile.DEFAULT.exists()) {
			try {
				jAtlasTree.load(Profile.DEFAULT);
			} catch (Exception e) {
				log.error("Failed to load atlas", e);
				GUIExceptionHandler.processException(e);
				new AtlasNew().actionPerformed(null);
			}
		} else
			new AtlasNew().actionPerformed(null);

		Settings settings = Settings.getInstance();
		atlasNameTextField.setText(settings.elementName);
		previewMap.settingsLoad();
		int nextZoom = 0;
		List<Integer> zoomList = settings.selectedZoomLevels;
		if (zoomList != null) {
			for (JZoomCheckBox currentZoomCb : cbZoom) {
				for (int i = nextZoom; i < zoomList.size(); i++) {
					int currentListZoom = zoomList.get(i);
					if (currentZoomCb.getZoomLevel() == currentListZoom) {
						currentZoomCb.setSelected(true);
						nextZoom = 1;
						break;
					}
				}
			}
		}
		coordinatesPanel.setNumberFormat(settings.coordinateNumberFormat);

		tileImageParametersPanel.loadSettings();
		tileImageParametersPanel.atlasFormatChanged(jAtlasTree.getAtlas().getOutputFormat());
		// mapSourceCombo
		// .setSelectedItem(MapSourcesManager.getSourceByName(settings.
		// mapviewMapSource));

		setSize(settings.mainWindow.size);
		Point windowLocation = settings.mainWindow.position;
		if (windowLocation.x == -1 && windowLocation.y == -1) {
			setLocationRelativeTo(null);
		} else {
			setLocation(windowLocation);
		}
		if (settings.mainWindow.maximized)
			setExtendedState(Frame.MAXIMIZED_BOTH);

		leftPanel.setVisible(settings.mainWindow.leftPanelVisible);
		leftPanelWidth = settings.mainWindow.leftPanelWidth;
		leftPanelUpdateWidth();
		ensureLeftPanelResizable();
		rightPanel.setVisible(settings.mainWindow.rightPanelVisible);

		if (leftPanelContent != null) {
			for (Component c : leftPanelContent.getComponents()) {
				if (c instanceof JCollapsiblePanel) {
					JCollapsiblePanel cp = (JCollapsiblePanel) c;
					String name = cp.getName();
					if (name != null && settings.mainWindow.collapsedPanels.contains(name))
						cp.setCollapsed(true);
				}
			}
		}

		updateBookmarksMenu();
	}

	private void saveSettings() {
		try {
			jAtlasTree.save(Profile.DEFAULT);

			Settings s = Settings.getInstance();
			previewMap.settingsSave();
			s.mapviewMapSource = previewMap.getMapSource().getName();
			s.selectedZoomLevels = new SelectedZoomLevels(cbZoom).getZoomLevelList();

			s.elementName = atlasNameTextField.getText();
			s.coordinateNumberFormat = coordinatesPanel.getNumberFormat();

			tileImageParametersPanel.saveSettings();
			boolean maximized = (getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
			s.mainWindow.maximized = maximized;
			if (!maximized) {
				s.mainWindow.size = getSize();
				s.mainWindow.position = getLocation();
			}
			s.mainWindow.collapsedPanels.clear();
			if (leftPanelContent != null) {
				for (Component c : leftPanelContent.getComponents()) {
					if (c instanceof JCollapsiblePanel) {
						JCollapsiblePanel cp = (JCollapsiblePanel) c;
						if (cp.isCollapsed())
							s.mainWindow.collapsedPanels.add(cp.getName());
					}
				}
			}
			s.mainWindow.leftPanelVisible = leftPanel.isVisible();
			s.mainWindow.leftPanelWidth = leftPanelWidth;
			s.mainWindow.rightPanelVisible = rightPanel.isVisible();
			checkAndSaveSettings();
		} catch (Exception e) {
			GUIExceptionHandler.showExceptionDialog(e);
			JOptionPane.showMessageDialog(null, I18nUtils.localizedStringForKey("msg_settings_write_error"),
					I18nUtils.localizedStringForKey("Error"), JOptionPane.ERROR_MESSAGE);
		}
	}

	public void checkAndSaveSettings() throws JAXBException {
		if (Settings.checkSettingsFileModified()) {
			int x = JOptionPane.showConfirmDialog(this,
					I18nUtils.localizedStringForKey("msg_setting_file_is_changed_by_other"),
					I18nUtils.localizedStringForKey("msg_setting_file_is_changed_by_other_title"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (x != JOptionPane.YES_OPTION)
				return;
		}
		Settings.save();

	}

	public JTileImageParametersPanel getParametersPanel() {
		return tileImageParametersPanel;
	}

	public String getUserText() {
		return atlasNameTextField.getText();
	}

	public void refreshPreviewMap() {
		previewMap.refreshMap();
	}

	private class ZoomSliderListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			previewMap.setZoom(zoomSlider.getValue());
		}
	}

	private class GridZoomComboListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!gridZoomCombo.isEnabled())
				return;
			GridZoom g = (GridZoom) gridZoomCombo.getSelectedItem();
			if (g == null)
				return;
			log.debug("Selected grid zoom combo box item has changed: " + g.getZoom());
			previewMap.setGridZoom(g.getZoom());
			repaint();
			previewMap.updateMapSelection();
		}
	}

	private void updateGridSizeCombo() {
		int maxZoom = previewMap.getMapSource().getMaxZoom();
		int minZoom = previewMap.getMapSource().getMinZoom();
		GridZoom lastGridZoom = (GridZoom) gridZoomCombo.getSelectedItem();
		gridZoomCombo.setEnabled(false);
		gridZoomCombo.removeAllItems();
		gridZoomCombo.setMaximumRowCount(maxZoom - minZoom + 2);
		gridZoomCombo.addItem(new GridZoom(-1) {

			@Override
			public String toString() {
				return I18nUtils.localizedStringForKey("map_ctrl_zoom_grid_disable");
			}

		});
		for (int i = maxZoom; i >= minZoom; i--) {
			gridZoomCombo.addItem(new GridZoom(i));
		}
		if (lastGridZoom != null)
			gridZoomCombo.setSelectedItem(lastGridZoom);
		gridZoomCombo.setEnabled(true);
	}

	private class ApplySelectionButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			setSelectionByEnteredCoordinates();
		}
	}

	private class MapSourceTreeListener implements TreeSelectionListener {

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			mapSourceTree.selectClickedMapSource();
			MapSource selectedMapSource = mapSourceTree.getSelectedMapSource();
			if (selectedMapSource == null) {
				boolean wasFirstValidSourceSelected = mapSourceTree.selectFirstMapSource();
				if (!wasFirstValidSourceSelected) {
					return;
				}
				selectedMapSource = mapSourceTree.getSelectedMapSource();
			}
			if (selectedMapSource instanceof InitializableMapSource) {
				// initialize the map source e.g. detect available zoom levels
				try {
					((InitializableMapSource) selectedMapSource).initialize();
				} catch (MapSourceInitializationException e1) {
					JOptionPane.showMessageDialog(
							null,
							I18nUtils.localizedStringForKey("msg_map_source_initialization_failed",
									selectedMapSource.getName(), e1.getLocalizedMessage()),
							I18nUtils.localizedStringForKey("Error"), JOptionPane.ERROR_MESSAGE);
				}
			}
			previewMap.setMapSource(selectedMapSource);
			zoomSlider.setMinimum(previewMap.getMapSource().getMinZoom());
			zoomSlider.setMaximum(previewMap.getMapSource().getMaxZoom());
			updateGridSizeCombo();
			updateZoomLevelCheckBoxes();
			calculateNrOfTilesToDownload();
		}
	}

	private class LoadProfileListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			Profile profile = profilesPanel.getSelectedProfile();
			profilesPanel.getDeleteButton().setEnabled(profile != null);
			if (profile == null)
				return;

			jAtlasTree.load(profile);
			previewMap.repaint();
			tileImageParametersPanel.atlasFormatChanged(jAtlasTree.getAtlas().getOutputFormat());
		}
	}

	private void updateZoomLevelCheckBoxes() {
		MapSource tileSource = previewMap.getMapSource();
		int zoomLevels = tileSource.getMaxZoom() - tileSource.getMinZoom() + 1;
		zoomLevels = Math.max(zoomLevels, 0);
		JCheckBox oldZoomLevelCheckBoxes[] = cbZoom;
		int oldMinZoom = 0;
		if (cbZoom.length > 0)
			oldMinZoom = cbZoom[0].getZoomLevel();
		cbZoom = new JZoomCheckBox[zoomLevels];
		zoomLevelPanel.removeAll();

		zoomLevelPanel.setLayout(new GridLayout(0, 10, 1, 2));
		ZoomLevelCheckBoxListener cbl = new ZoomLevelCheckBoxListener();

		for (int i = cbZoom.length - 1; i >= 0; i--) {
			int cbz = i + tileSource.getMinZoom();
			JZoomCheckBox cb = new JZoomCheckBox(cbz);
			cb.setPreferredSize(new Dimension(22, 11));
			cb.setMinimumSize(cb.getPreferredSize());
			cb.setOpaque(false);
			cb.setFocusable(false);
			cb.setName(Integer.toString(cbz));
			int oldCbIndex = cbz - oldMinZoom;
			if (oldCbIndex >= 0 && oldCbIndex < (oldZoomLevelCheckBoxes.length))
				cb.setSelected(oldZoomLevelCheckBoxes[oldCbIndex].isSelected());
			cb.addActionListener(cbl);
			// cb.setToolTipText("Select zoom level " + cbz + " for atlas");
			zoomLevelPanel.add(cb);
			cbZoom[i] = cb;

			JLabel l = new JLabel(Integer.toString(cbz));
			zoomLevelPanel.add(l);
		}
		amountOfTilesLabel.setOpaque(false);
		amountOfTilesLabel.setForeground(Color.black);
	}

	private class ZoomLevelCheckBoxListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			calculateNrOfTilesToDownload();
		}
	}

	public void selectionChanged(MercatorPixelCoordinate max, MercatorPixelCoordinate min) {
		mapSelectionMax = max;
		mapSelectionMin = min;
		coordinatesPanel.setSelection(max, min);
		calculateNrOfTilesToDownload();
	}

	public void zoomChanged(int zoomLevel) {
		zoomLevelText.setText(" " + zoomLevel + " ");
		zoomSlider.setValue(zoomLevel);
	}

	public void gridZoomChanged(int newGridZoomLevel) {
		gridZoomCombo.setSelectedItem(new GridZoom(newGridZoomLevel));
	}

	public MapSource getSelectedMapSource() {
		mapSourceTree.selectClickedMapSource();
		return mapSourceTree.getSelectedMapSource();
	}

	public SelectedZoomLevels getSelectedZoomLevels() {
		return new SelectedZoomLevels(cbZoom);
	}

	public void selectNextMapSource() {
		if (!mapSourceTree.selectNextMapSource()) {
			Toolkit.getDefaultToolkit().beep();
		} else {
			mapSourceChanged(mapSourceTree.getSelectedMapSource());
		}
	}

	public void selectPreviousMapSource() {
		if (!mapSourceTree.selectPreviousMapSource()) {
			Toolkit.getDefaultToolkit().beep();
		} else {
			mapSourceChanged(mapSourceTree.getSelectedMapSource());
		}
	}

	public void mapSourceChanged(MapSource newMapSource) {
		// TODO update selected area if new map source has different projectionCategory
		calculateNrOfTilesToDownload();
		// if (newMapSource != null && newMapSource.equals(mapSourceCombo.getSelectedItem()))
		// return;
		mapSourceTree.selectMapSource(newMapSource);
		mapSourcePanel.setMapSourceLabel(newMapSource);
	}

	public void mapSelectionControllerChanged(JMapController newMapController) {
		smPolygon.setSelected(false);
		smCircle.setSelected(false);
		smRectangle.setSelected(false);
		if (newMapController instanceof PolygonSelectionMapController)
			smPolygon.setSelected(true);
		else if (newMapController instanceof PolygonCircleSelectionMapController)
			smCircle.setSelected(true);
		else if (newMapController instanceof RectangleSelectionMapController)
			smRectangle.setSelected(true);
	}

	private void setSelectionByEnteredCoordinates() {
		coordinatesPanel.correctMinMax();
		MapSelection ms = coordinatesPanel.getMapSelection(previewMap.getMapSource());
		mapSelectionMax = ms.getBottomRightPixelCoordinate();
		mapSelectionMin = ms.getTopLeftPixelCoordinate();
		previewMap.setSelectionAndZoomTo(ms, false);
	}

	public MapSelection getMapSelectionCoordinates() {
		if (mapSelectionMax == null || mapSelectionMin == null)
			return null;
		return new MapSelection(previewMap.getMapSource(), mapSelectionMax, mapSelectionMin);
	}

	public TileImageParameters getSelectedTileImageParameters() {
		return tileImageParametersPanel.getSelectedTileImageParameters();
	}

	private void calculateNrOfTilesToDownload() {
		MapSelection ms = getMapSelectionCoordinates();
		String baseText;
		baseText = I18nUtils.localizedStringForKey("lp_zoom_total_tile_title");
		if (ms == null || !ms.isAreaSelected()) {
			amountOfTilesLabel.setText(String.format(baseText, "0"));
			amountOfTilesLabel.setToolTipText("");
		} else {
			try {
				SelectedZoomLevels sZL = new SelectedZoomLevels(cbZoom);

				int[] zoomLevels = sZL.getZoomLevels();

				long totalNrOfTiles = 0;

				StringBuilder hint = new StringBuilder(1024);
				hint.append(I18nUtils.localizedStringForKey("lp_zoom_total_tile_hint_head"));
				for (int i = 0; i < zoomLevels.length; i++) {
					int zoom = zoomLevels[i];
					long[] info = ms.calculateNrOfTilesEx(zoom);
					totalNrOfTiles += info[0];
					hint.append(String.format(I18nUtils.localizedStringForKey("lp_zoom_total_tile_hint_row"),
							zoomLevels[i], info[0], info[1], info[2]));
					// hint.append("<br>Level " + zoomLevels[i] + ": " + info[0] + " (" + info[1] + "*" + info[2] +
					// ")");
				}
				String hintText = "<html>" + hint.toString() + "</html>";
				amountOfTilesLabel.setText(String.format(baseText, Long.toString(totalNrOfTiles)));
				amountOfTilesLabel.setToolTipText(hintText);
			} catch (Exception e) {
				amountOfTilesLabel.setText(String.format(baseText, "?"));
				log.error("", e);
			}
		}
	}

	public AtlasInterface getAtlas() {
		return jAtlasTree.getAtlas();
	}

	private class WindowDestroyer extends WindowAdapter {

		@Override
		public void windowOpened(WindowEvent e) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					previewMap.setEnabled(true);
				}
			});
		}

		public void windowClosing(WindowEvent event) {
			saveSettings();
		}
	}

	/**
	 * Saves the window position and size when window is moved or resized. This is necessary because of the maximized
	 * state. If a window is maximized it is impossible to retrieve the window size & position of the non-maximized
	 * window - therefore we have to collect the information every time they change.
	 */
	private class MainWindowListener extends ComponentAdapter {
		public void componentResized(ComponentEvent event) {
			// log.debug(event.paramString());
			updateValues();
		}

		public void componentMoved(ComponentEvent event) {
			// log.debug(event.paramString());
			updateValues();
		}

		private void updateValues() {
			// only update old values while window is in NORMAL state
			// Note(Java bug): Sometimes getExtendedState() says the window is
			// not maximized but maximizing is already in progress and therefore
			// the window bounds are already changed.
			if ((getExtendedState() & MAXIMIZED_BOTH) != 0)
				return;
			Settings s = Settings.getInstance();
			s.mainWindow.size = getSize();
			s.mainWindow.position = getLocation();
		}
	}
}
