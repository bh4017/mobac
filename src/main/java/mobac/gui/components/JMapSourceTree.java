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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import mobac.mapsources.AbstractMultiLayerMapSource;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.HttpMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.utilities.I18nUtils;
import mobac.utilities.Utilities;

import org.apache.commons.lang3.StringEscapeUtils;

//import org.apache.log4j.Logger;

/**
 * @author Maksym "elmuSSo" Kondej
 * 
 *         This class holds methods needed for managing a MapSources loaded into a JTree structure
 */
public class JMapSourceTree extends JTree {

	/**
	 * ComparableTreeNode is a DefaultMutableTreeNode with a mechanism of comparison with other nodes. This is used when
	 * putting a node into a tree in an alphabetic order and placing folder nodes above other types of nodes.
	 */
	class ComparableTreeNode extends DefaultMutableTreeNode implements Comparable<DefaultMutableTreeNode> {
		private static final long serialVersionUID = 1L;

		public ComparableTreeNode(MapSource mapSource) {
			super(mapSource);
		}

		public ComparableTreeNode(String string) {
			super(string);
		}

		@Override
		public int compareTo(DefaultMutableTreeNode treeNode) {
			Class<? extends Object> thisObjectClass = this.getUserObject().getClass();
			Class<? extends Object> comparedObjectClass = treeNode.getUserObject().getClass();

			// This rule will always put "folders" above MapSources.
			if (thisObjectClass.equals(folderClass) != comparedObjectClass.equals(folderClass)) {
				return thisObjectClass.equals(folderClass) ? -1 : 1;
			}

			return this.toString().compareToIgnoreCase(treeNode.toString());
		}
	}

	/**
	 * CustomIconRenderer was created to manage icons within a tree
	 */
	static class CustomIconRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 1L;

		private ImageIcon multiLayerIcon, fileBasedIcon, httpIcon, debugIcon, folderOpenedIcon, folderClosedIcon;

		public CustomIconRenderer() {
			multiLayerIcon = new ImageIcon(Utilities.getResourceImageUrl("icon_multilayer_ms.png"));
			fileBasedIcon = new ImageIcon(Utilities.getResourceImageUrl("icon_filebased_ms.png"));
			httpIcon = new ImageIcon(Utilities.getResourceImageUrl("icon_http_ms.png"));
			debugIcon = new ImageIcon(Utilities.getResourceImageUrl("icon_debug_ms.png"));
			folderOpenedIcon = new ImageIcon(Utilities.getResourceImageUrl("icon_folder_opened.png"));
			folderClosedIcon = new ImageIcon(Utilities.getResourceImageUrl("icon_folder_closed.png"));
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

			setTextSelectionColor(getTextNonSelectionColor());
			setBorderSelectionColor(null);

			if (selected) {
				// Special style for selected node
				this.setFont(getFont().deriveFont(Font.BOLD));
				setBackgroundSelectionColor(mapSourceInTreeHighlightColor);
			} else {
				this.setFont(getFont().deriveFont(Font.PLAIN));
				setBackgroundSelectionColor(null);
			}

			// Adding additional left margin of icon
			setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

			// Setting icons for opened/closed folder
			setOpenIcon(folderOpenedIcon);
			setClosedIcon(folderClosedIcon);

			Object treeNodeObject = ((DefaultMutableTreeNode) value).getUserObject();

			// Giving a appropriate icon to each category of MapSource (taken from node's userObject)
			if (treeNodeObject instanceof AbstractMultiLayerMapSource) {
				setIcon(multiLayerIcon);
			} else if (treeNodeObject instanceof FileBasedMapSource) {
				setIcon(fileBasedIcon);
			} else if (treeNodeObject instanceof HttpMapSource) {
				setIcon(httpIcon);
			} else if (treeNodeObject instanceof MapSource) {
				setIcon(debugIcon);
			}
			return this;
		}
	}

	// private final Logger log = Logger.getLogger(JMapSourceTree.class);
	private static final long serialVersionUID = 1L;

	// Specifying a class which will determine if a node is a folder
	static final Class<String> folderClass = String.class;

	private static Color mapSourceInTreeHighlightColor = new Color(230, 245, 255);

	private Vector<MapSource> mapSources;
	private MapSource selectedMapSource, previouslySelectedMapSource;

	private ComparableTreeNode rootNode = new ComparableTreeNode("Maps sources root");
	private DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

	public JMapSourceTree(Vector<MapSource> enabledOrderedMapSources) {
		super();
		// Setting a cell renderer which will provide proper icons behavior
		setCellRenderer(new CustomIconRenderer());
		initialize(enabledOrderedMapSources);
		setRootVisible(false);
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		setExpandsSelectedPaths(true);
		setToggleClickCount(1);
		setToolTipText(I18nUtils.localizedStringForKey("lp_map_source_tree_tips"));
	}

	/**
	 * This method takes a list of all valid and enabled MapSources, saves them into internal list of MapSources, and
	 * then generates a tree model basing on them
	 * 
	 * @param enabledOrderedMapSources
	 *            - list of all MapSources that must be put into a tree.
	 */
	public void initialize(Vector<MapSource> enabledOrderedMapSources) {
		mapSources = enabledOrderedMapSources;
		resetTree();
		generateTreeModel();
		super.setModel(treeModel);
	}

	/**
	 * This method will take a MapSource and will analyzed it's folder path to check if there is no need to put it into
	 * any folders/subfolders structure.
	 * 
	 * @param mapSource
	 *            - this mapSource's path will be analyzed
	 */
	private void addChildBasedOnFolderPath(MapSource mapSource) {
		MapSourceLoaderInfo loaderInfo = mapSource.getLoaderInfo();
		String[] folderPath = null;
		if (loaderInfo != null) {
			folderPath = loaderInfo.getRelativePath();
		}
		ComparableTreeNode parent = rootNode;
		if (folderPath != null) {
			for (String folderPathElementName : folderPath) {

				ComparableTreeNode folderPathElement = null;
				ComparableTreeNode childFound = getChildByUserObject(parent, folderPathElementName);

				if (childFound != null) {
					folderPathElement = childFound;
				} else {
					folderPathElement = new ComparableTreeNode(folderPathElementName);
					insertInRightOrder(parent, folderPathElement);
				}
				parent = folderPathElement;
			}
		}
		ComparableTreeNode newLeaf = new ComparableTreeNode(mapSource);
		insertInRightOrder(parent, newLeaf);
	}

	/**
	 * This method clears a tree and its model to make sure it is ready for (re)initialization
	 */
	private void resetTree() {
		this.setModel(null);
		rootNode.removeAllChildren();
	}

	/**
	 * This method inserts a node into a tree in an order based on comparator from ComparableTreeNode
	 * 
	 * @param parentNode
	 *            - a parent node to which a new node will be attached
	 * @param insertedNode
	 *            - a new node
	 */
	private void insertInRightOrder(ComparableTreeNode parentNode, ComparableTreeNode insertedNode) {
		int parentChildCount = treeModel.getChildCount(parentNode);
		int insertIndex = 0;

		if (parentChildCount != 0) {
			insertIndex = parentChildCount;
			for (int i = parentChildCount - 1; i >= 0; i--) {
				ComparableTreeNode child = (ComparableTreeNode) treeModel.getChild(parentNode, i);

				if (insertedNode.compareTo(child) <= 0) {
					insertIndex = i;
				} else {
					break;
				}
			}
		}
		treeModel.insertNodeInto(insertedNode, parentNode, insertIndex);
	}

	/**
	 * This method searches for a parentNode's child relying on its name
	 * 
	 * @param parentNode
	 *            - its children will be iterated during search
	 * @param nodeName
	 *            - name of a sought node
	 * @return found child, or null if child was not found
	 */
	private static ComparableTreeNode getChildByUserObject(ComparableTreeNode parentNode, String nodeName) {
		@SuppressWarnings("unchecked")
		Enumeration<ComparableTreeNode> childsOfParent = parentNode.breadthFirstEnumeration();
		while (childsOfParent.hasMoreElements()) {
			ComparableTreeNode child = childsOfParent.nextElement();
			if (child.getUserObject().equals(nodeName)) {
				return child;
			}
		}
		return null;
	}

	/**
	 * Method is iterating over all MapSources and adding them to a tree's model in an appropriate place
	 */
	private void generateTreeModel() {
		for (MapSource mapSource : this.mapSources) {
			addChildBasedOnFolderPath(mapSource);
		}
	}

	/**
	 * This method searches for an index of a MapSource with a requested name
	 * 
	 * @param mapSourceName
	 *            - name of a sought MapSource
	 * @return index of found MapSource or -1 in case nothing was found
	 */
	private int getMapSourceIndexByName(String mapSourceName) {
		for (int i = 0; i < mapSources.size(); i++) {
			MapSource mapSource = mapSources.get(i);
			String mapSourceAsString = mapSource.toString();
			if (mapSourceAsString.equals(mapSourceName)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * This method searches for a MapSource object with a requested name
	 * 
	 * @param mapSourceName
	 *            - name of a sought MapSource
	 * @return found MapSource object or null otherwise
	 */
	private MapSource getMapSourceByName(String mapSourceName) {
		int mapSourceIndex = getMapSourceIndexByName(mapSourceName);

		if (mapSourceIndex == -1) {
			return null;
		}
		return this.mapSources.get(mapSourceIndex);
	}

	/**
	 * This method searches for a TreePath of a requested MapSouce
	 * 
	 * @param mapSource
	 *            - analyzed mapSource object
	 * @return TreePath of a specified MapSource or null otherwise
	 */
	private TreePath findTreePathOfMapSource(MapSource mapSource) {
		@SuppressWarnings("unchecked")
		Enumeration<ComparableTreeNode> rootDescendants = rootNode.depthFirstEnumeration();
		while (rootDescendants.hasMoreElements()) {
			ComparableTreeNode descendantNode = rootDescendants.nextElement();

			if (descendantNode.getUserObject().equals(mapSource)) {
				return new TreePath(descendantNode.getPath());
			}
		}
		return null;
	}

	/**
	 * This method is a main gateway for selecting a mapSource, it should be used by other functions. Firstly it
	 * searches for a requested MapSource and if it will find it, it will mark it internally as selected and then it
	 * will be passed to be selected in a graphical tree.
	 * 
	 * @param mapSourceToSelect
	 *            - MapSource that will be internally marked as selected
	 * @return a true if MapSource was found, false otherwise
	 */
	public boolean selectMapSource(MapSource mapSourceToSelect) {
		int foundMapSourceIndex = mapSources.indexOf(mapSourceToSelect);
		if (foundMapSourceIndex != -1) {
			previouslySelectedMapSource = selectedMapSource;
			// Marking internally as selected MapSource
			selectedMapSource = mapSourceToSelect;
			chooseAndShowTreeNodeInTree(mapSourceToSelect);
			return true;
		}
		return false;
	}

	/**
	 * This method is selecting a requested MapSource and focusing on it IN A TREE. MapSource is not being marked
	 * internally as selected in this method.
	 * 
	 * @param mapSourceToSelect
	 *            - MapSource that will be selected
	 */
	private void chooseAndShowTreeNodeInTree(MapSource mapSourceToSelect) {
		TreePath pathFound = findTreePathOfMapSource(mapSourceToSelect);
		if (pathFound != null) {
			// Expand all folders and subfolders to show a chosen node
			expandPath(pathFound.getParentPath());
			// Choose the node
			setSelectionPath(pathFound);
			// Scroll a JTree viewport to the chosen node
			scrollPathToVisibleVerticalOnly(pathFound);
			// Signaling to refresh a node, after its font/color was changed
			((DefaultTreeModel) this.getModel()).nodeChanged((TreeNode) pathFound.getLastPathComponent());
		}
	}

	/**
	 * This method will vertically scroll to the requested treePath, but without touching a horizontal scroll-bar
	 * 
	 * @param treePath
	 *            - TreePath to be vertically scrolled to
	 */
	private void scrollPathToVisibleVerticalOnly(TreePath treePath) {
		if (treePath != null) {
			makeVisible(treePath);

			Rectangle pathBounds = getPathBounds(treePath);
			if (pathBounds != null) {
				pathBounds.x = 0;
				scrollRectToVisible(pathBounds);
			}
		}
	}

	/**
	 * This method should be called after clicking on any node in a tree. It gets a clicked/chosen node from a tree and
	 * pass it to the method that will mark it as internally selected.
	 * 
	 * @return true if clicked MapSource was successfully selected internally, false otherwise
	 */
	public boolean selectClickedMapSource() {
		if (getSelectionPath() != null) {
			String selectedTreeElement = getSelectionPath().getLastPathComponent().toString();
			MapSource foundMapSource = getMapSourceByName(selectedTreeElement);
			if (foundMapSource == null) {
				// React if a non-MapSource node was clicked
				setSelectionPath(findTreePathOfMapSource(previouslySelectedMapSource));
			} else {
				// A mapSource type was clicked
				return selectMapSource(foundMapSource);
			}
		}
		return false;
	}

	/**
	 * @return internally selected MapSource
	 */
	public MapSource getSelectedMapSource() {
		return selectedMapSource;
	}

	/**
	 * Selects a next MapSource from internal list of MapSources
	 */
	public boolean selectNextMapSource() {
		if (mapSources.lastElement().equals(selectedMapSource)) {
			return false;
		} else {
			int indexOfSelectedMapSource = mapSources.indexOf(selectedMapSource);
			return selectMapSource(mapSources.get(indexOfSelectedMapSource + 1));
		}
	}

	/**
	 * Selects a previous MapSource from internal list of MapSources
	 */
	public boolean selectPreviousMapSource() {
		if (mapSources.firstElement().equals(selectedMapSource)) {
			return false;
		} else {
			int indexOfSelectedMapSource = mapSources.indexOf(selectedMapSource);
			return selectMapSource(mapSources.get(indexOfSelectedMapSource - 1));
		}
	}

	/**
	 * Selects a first MapSource from internal list of MapSources. Because internal list is not in the same order as
	 * nodes in tree, user gets a random (from his point of view) MapSource. Perhaps, this method should ask user what
	 * MapSource he want to pick, or maybe automatically select a closest sibling of the previously selected MapSource?
	 */
	public boolean selectFirstMapSource() {
		return selectMapSource(mapSources.get(0));
	}

	/**
	 * @return a size of internal list of MapSources
	 */
	public int getMapSourcesCount() {
		return mapSources.size();
	}

	/**
	 * Check if the mouse cursor was over a clickable node
	 * 
	 * @param eventPoint
	 *            - a mouse position coordinates
	 * @return true if a node was clickable, false otherwise
	 */
	public boolean isLocationClickable(Point eventPoint) {
		int x = (int) eventPoint.getX();
		int y = (int) eventPoint.getY();
		TreePath treePathForXY = getPathForLocation(x, y);

		// If a node is an ancestor of a currently selected node - it can't be closed, so it is unclickable.
		if (treePathForXY != null && treePathForXY.isDescendant(findTreePathOfMapSource(selectedMapSource))) {
			return false;
		}

		boolean isInside = false;
		if (treePathForXY != null) {
			Rectangle pathBounds = this.getPathBounds(treePathForXY);
			isInside = pathBounds.contains(eventPoint);
		}
		return isInside;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		if (getRowForLocation(event.getX(), event.getY()) == -1)
			return "";
		TreePath curPath = getPathForLocation(event.getX(), event.getY());
		Object lastPathComponent = curPath.getLastPathComponent();
		if (lastPathComponent == null)
			return null;

		Object userObject = ((ComparableTreeNode) lastPathComponent).getUserObject();
		if (userObject.getClass().equals(folderClass)) {
			return null;
		}
		return generateMapSourceTooltip((MapSource) userObject);
	}

	/**
	 * Static method used for dynamic generation of a tooltip with information about a mapSource
	 * 
	 * @param mapSource
	 *            - of which information will be put into a tooltip
	 * @return generated tooltip string
	 */
	public static String generateMapSourceTooltip(MapSource mapSource) {
		boolean multiLayer = (mapSource instanceof AbstractMultiLayerMapSource);
		boolean fileBased = (mapSource instanceof FileBasedMapSource);

		// Getting a localized string for an input to tooltip
		String locName = I18nUtils.localizedStringForKey("lp_map_source_tooltip_layer_name");
		String locInternalName = I18nUtils.localizedStringForKey("lp_map_source_tooltip_inernal_name");
		String locType = I18nUtils.localizedStringForKey("lp_map_source_tooltip_type");
		String locLoadedFrom = I18nUtils.localizedStringForKey("lp_map_source_tooltip_loaded_from");
		String locFileName = I18nUtils.localizedStringForKey("lp_map_source_tooltip_file_name");

		String locMultiLayer = I18nUtils.localizedStringForKey("lp_map_source_layer_multi");
		String locSingleLayer = I18nUtils.localizedStringForKey("lp_map_source_layer_single");
		String locFileBased = I18nUtils.localizedStringForKey("lp_map_source_layer_file_based");
		String locWebBased = I18nUtils.localizedStringForKey("lp_map_source_layer_web_based");

		// Getting a values for some attributes
		String name = StringEscapeUtils.escapeHtml4(mapSource.toString());
		String nameInternal = StringEscapeUtils.escapeHtml4(mapSource.getName());
		String type1 = multiLayer ? locMultiLayer : locSingleLayer;
		String type2 = fileBased ? locFileBased : locWebBased;

		String toolTipString = locName + ": <b>%s</b><br>" + locInternalName + ": %s<br>" + locType + ": %s (%s)";
		toolTipString = String.format(toolTipString, name, nameInternal, type1, type2);

		MapSourceLoaderInfo info = mapSource.getLoaderInfo();
		if (info != null) {
			toolTipString += "<br>" + locLoadedFrom + ": " + info.getLoaderType().displayName;

			File f = info.getSourceFile();
			if (f != null) {
				toolTipString += "<br>" + locFileName + ": <tt>" + StringEscapeUtils.escapeHtml4(f.getName()) + "</tt>";
			}
		}

		return "<html>" + toolTipString + "</html>";
	}
}