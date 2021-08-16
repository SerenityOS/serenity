/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.swing.plaf.basic;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.beans.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.*;
import javax.swing.text.Position;
import javax.swing.plaf.basic.DragRecognitionSupport.BeforeDrag;
import sun.awt.AWTAccessor;
import sun.swing.SwingUtilities2;

import sun.swing.DefaultLookup;
import sun.swing.UIAction;

/**
 * The basic L&amp;F for a hierarchical data structure.
 *
 * @author Scott Violet
 * @author Shannon Hickey (drag and drop)
 */

public class BasicTreeUI extends TreeUI
{
    private static final StringBuilder BASELINE_COMPONENT_KEY =
        new StringBuilder("Tree.baselineComponent");

    // Old actions forward to an instance of this.
    private static final Actions SHARED_ACTION = new Actions();

    /**
     * The collapsed icon.
     */
    protected transient Icon        collapsedIcon;
    /**
     * The expanded icon.
     */
    protected transient Icon        expandedIcon;

    /**
      * Color used to draw hash marks.  If <code>null</code> no hash marks
      * will be drawn.
      */
    private Color hashColor;

    /** Distance between left margin and where vertical dashes will be
      * drawn. */
    protected int               leftChildIndent;
    /** Distance to add to leftChildIndent to determine where cell
      * contents will be drawn. */
    protected int               rightChildIndent;
    /** Total distance that will be indented.  The sum of leftChildIndent
      * and rightChildIndent. */
    protected int               totalChildIndent;

    /** Minimum preferred size. */
    protected Dimension         preferredMinSize;

    /** Index of the row that was last selected. */
    protected int               lastSelectedRow;

    /** Component that we're going to be drawing into. */
    protected JTree             tree;

    /** Renderer that is being used to do the actual cell drawing. */
    protected transient TreeCellRenderer   currentCellRenderer;

    /** Set to true if the renderer that is currently in the tree was
     * created by this instance. */
    protected boolean           createdRenderer;

    /** Editor for the tree. */
    protected transient TreeCellEditor     cellEditor;

    /** Set to true if editor that is currently in the tree was
     * created by this instance. */
    protected boolean           createdCellEditor;

    /** Set to false when editing and shouldSelectCell() returns true meaning
      * the node should be selected before editing, used in completeEditing. */
    protected boolean           stopEditingInCompleteEditing;

    /** Used to paint the TreeCellRenderer. */
    protected CellRendererPane  rendererPane;

    /** Size needed to completely display all the nodes. */
    protected Dimension         preferredSize;

    /** Is the preferredSize valid? */
    protected boolean           validCachedPreferredSize;

    /** Object responsible for handling sizing and expanded issues. */
    // WARNING: Be careful with the bounds held by treeState. They are
    // always in terms of left-to-right. They get mapped to right-to-left
    // by the various methods of this class.
    protected AbstractLayoutCache  treeState;


    /** Used for minimizing the drawing of vertical lines. */
    protected Hashtable<TreePath,Boolean> drawingCache;

    /** True if doing optimizations for a largeModel. Subclasses that
     * don't support this may wish to override createLayoutCache to not
     * return a FixedHeightLayoutCache instance. */
    protected boolean           largeModel;

    /** Reponsible for telling the TreeState the size needed for a node. */
    protected AbstractLayoutCache.NodeDimensions     nodeDimensions;

    /** Used to determine what to display. */
    protected TreeModel         treeModel;

    /** Model maintaining the selection. */
    protected TreeSelectionModel treeSelectionModel;

    /** How much the depth should be offset to properly calculate
     * x locations. This is based on whether or not the root is visible,
     * and if the root handles are visible. */
    protected int               depthOffset;

    // Following 4 ivars are only valid when editing.

    /** When editing, this will be the Component that is doing the actual
      * editing. */
    protected Component         editingComponent;

    /** Path that is being edited. */
    protected TreePath          editingPath;

    /** Row that is being edited. Should only be referenced if
     * editingComponent is not null. */
    protected int               editingRow;

    /** Set to true if the editor has a different size than the renderer. */
    protected boolean           editorHasDifferentSize;

    /** Row correspondin to lead path. */
    private int                 leadRow;
    /** If true, the property change event for LEAD_SELECTION_PATH_PROPERTY,
     * or ANCHOR_SELECTION_PATH_PROPERTY will not generate a repaint. */
    private boolean             ignoreLAChange;

    /** Indicates the orientation. */
    private boolean             leftToRight;

    // Cached listeners
    private PropertyChangeListener propertyChangeListener;
    private PropertyChangeListener selectionModelPropertyChangeListener;
    private MouseListener mouseListener;
    private FocusListener focusListener;
    private KeyListener keyListener;
    /** Used for large models, listens for moved/resized events and
     * updates the validCachedPreferredSize bit accordingly. */
    private ComponentListener   componentListener;
    /** Listens for CellEditor events. */
    private CellEditorListener  cellEditorListener;
    /** Updates the display when the selection changes. */
    private TreeSelectionListener treeSelectionListener;
    /** Is responsible for updating the display based on model events. */
    private TreeModelListener treeModelListener;
    /** Updates the treestate as the nodes expand. */
    private TreeExpansionListener treeExpansionListener;

    /** UI property indicating whether to paint lines */
    private boolean paintLines = true;

    /** UI property for painting dashed lines */
    private boolean lineTypeDashed;

    /**
     * The time factor to treate the series of typed alphanumeric key
     * as prefix for first letter navigation.
     */
    private long timeFactor = 1000L;

    private Handler handler;

    /**
     * A temporary variable for communication between startEditingOnRelease
     * and startEditing.
     */
    private MouseEvent releaseEvent;

    /**
     * Constructs a new instance of {@code BasicTreeUI}.
     *
     * @param x a component
     * @return a new instance of {@code BasicTreeUI}
     */
    public static ComponentUI createUI(JComponent x) {
        return new BasicTreeUI();
    }


    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.SELECT_PREVIOUS));
        map.put(new Actions(Actions.SELECT_PREVIOUS_CHANGE_LEAD));
        map.put(new Actions(Actions.SELECT_PREVIOUS_EXTEND_SELECTION));

        map.put(new Actions(Actions.SELECT_NEXT));
        map.put(new Actions(Actions.SELECT_NEXT_CHANGE_LEAD));
        map.put(new Actions(Actions.SELECT_NEXT_EXTEND_SELECTION));

        map.put(new Actions(Actions.SELECT_CHILD));
        map.put(new Actions(Actions.SELECT_CHILD_CHANGE_LEAD));

        map.put(new Actions(Actions.SELECT_PARENT));
        map.put(new Actions(Actions.SELECT_PARENT_CHANGE_LEAD));

        map.put(new Actions(Actions.SCROLL_UP_CHANGE_SELECTION));
        map.put(new Actions(Actions.SCROLL_UP_CHANGE_LEAD));
        map.put(new Actions(Actions.SCROLL_UP_EXTEND_SELECTION));

        map.put(new Actions(Actions.SCROLL_DOWN_CHANGE_SELECTION));
        map.put(new Actions(Actions.SCROLL_DOWN_EXTEND_SELECTION));
        map.put(new Actions(Actions.SCROLL_DOWN_CHANGE_LEAD));

        map.put(new Actions(Actions.SELECT_FIRST));
        map.put(new Actions(Actions.SELECT_FIRST_CHANGE_LEAD));
        map.put(new Actions(Actions.SELECT_FIRST_EXTEND_SELECTION));

        map.put(new Actions(Actions.SELECT_LAST));
        map.put(new Actions(Actions.SELECT_LAST_CHANGE_LEAD));
        map.put(new Actions(Actions.SELECT_LAST_EXTEND_SELECTION));

        map.put(new Actions(Actions.TOGGLE));

        map.put(new Actions(Actions.CANCEL_EDITING));

        map.put(new Actions(Actions.START_EDITING));

        map.put(new Actions(Actions.SELECT_ALL));

        map.put(new Actions(Actions.CLEAR_SELECTION));

        map.put(new Actions(Actions.SCROLL_LEFT));
        map.put(new Actions(Actions.SCROLL_RIGHT));

        map.put(new Actions(Actions.SCROLL_LEFT_EXTEND_SELECTION));
        map.put(new Actions(Actions.SCROLL_RIGHT_EXTEND_SELECTION));

        map.put(new Actions(Actions.SCROLL_RIGHT_CHANGE_LEAD));
        map.put(new Actions(Actions.SCROLL_LEFT_CHANGE_LEAD));

        map.put(new Actions(Actions.EXPAND));
        map.put(new Actions(Actions.COLLAPSE));
        map.put(new Actions(Actions.MOVE_SELECTION_TO_PARENT));

        map.put(new Actions(Actions.ADD_TO_SELECTION));
        map.put(new Actions(Actions.TOGGLE_AND_ANCHOR));
        map.put(new Actions(Actions.EXTEND_TO));
        map.put(new Actions(Actions.MOVE_SELECTION_TO));

        map.put(TransferHandler.getCutAction());
        map.put(TransferHandler.getCopyAction());
        map.put(TransferHandler.getPasteAction());
    }

    /**
     * Constructs a new instance of {@code BasicTreeUI}.
     */
    public BasicTreeUI() {
        super();
    }

    /**
     * Returns the hash color.
     *
     * @return the hash color
     */
    protected Color getHashColor() {
        return hashColor;
    }

    /**
     * Sets the hash color.
     *
     * @param color the hash color
     */
    protected void setHashColor(Color color) {
        hashColor = color;
    }

    /**
     * Sets the left child indent.
     *
     * @param newAmount the left child indent
     */
    public void setLeftChildIndent(int newAmount) {
        leftChildIndent = newAmount;
        totalChildIndent = leftChildIndent + rightChildIndent;
        if(treeState != null)
            treeState.invalidateSizes();
        updateSize();
    }

    /**
     * Returns the left child indent.
     *
     * @return the left child indent
     */
    public int getLeftChildIndent() {
        return leftChildIndent;
    }

    /**
     * Sets the right child indent.
     *
     * @param newAmount the right child indent
     */
    public void setRightChildIndent(int newAmount) {
        rightChildIndent = newAmount;
        totalChildIndent = leftChildIndent + rightChildIndent;
        if(treeState != null)
            treeState.invalidateSizes();
        updateSize();
    }

    /**
     * Returns the right child indent.
     *
     * @return the right child indent
     */
    public int getRightChildIndent() {
        return rightChildIndent;
    }

    /**
     * Sets the expanded icon.
     *
     * @param newG the expanded icon
     */
    public void setExpandedIcon(Icon newG) {
        expandedIcon = newG;
    }

    /**
     * Returns the expanded icon.
     *
     * @return the expanded icon
     */
    public Icon getExpandedIcon() {
        return expandedIcon;
    }

    /**
     * Sets the collapsed icon.
     *
     * @param newG the collapsed icon
     */
    public void setCollapsedIcon(Icon newG) {
        collapsedIcon = newG;
    }

    /**
     * Returns the collapsed icon.
     *
     * @return the collapsed icon
     */
    public Icon getCollapsedIcon() {
        return collapsedIcon;
    }

    //
    // Methods for configuring the behavior of the tree. None of them
    // push the value to the JTree instance. You should really only
    // call these methods on the JTree.
    //

    /**
     * Sets the {@code largeModel}.
     *
     * Called when the {@code largeModel} property is changed in the drawn tree
     * component.
     *
     * @param largeModel the new value of the {@code largeModel} property
     */
    protected void setLargeModel(boolean largeModel) {
        if(getRowHeight() < 1)
            largeModel = false;
        if(this.largeModel != largeModel) {
            completeEditing();
            this.largeModel = largeModel;
            treeState = createLayoutCache();
            configureLayoutCache();
            updateLayoutCacheExpandedNodesIfNecessary();
            updateSize();
        }
    }

    /**
     * Returns {@code true} if large model is set.
     *
     * @return {@code true} if large model is set
     */
    protected boolean isLargeModel() {
        return largeModel;
    }

    /**
     * Sets the row height, this is forwarded to the treeState.
     *
     * Called when the {@code rowHeight} property is changed in
     * the drawn tree component.
     *
     * @param rowHeight the new value of the {@code rowHeight} property
     */
    protected void setRowHeight(int rowHeight) {
        completeEditing();
        if(treeState != null) {
            setLargeModel(tree.isLargeModel());
            treeState.setRowHeight(rowHeight);
            updateSize();
        }
    }

    /**
     * Returns the height of each row in the drawn tree component. If the
     * returned value is less than or equal to 0 the height for each row is
     * determined by the renderer.
     *
     * @return the height of each row, in pixels
     */
    protected int getRowHeight() {
        return (tree == null) ? -1 : tree.getRowHeight();
    }

    /**
     * Sets the {@code TreeCellRenderer} to {@code tcr}. This invokes
     * {@code updateRenderer}.
     *
     * Called when the {@code cellRenderer} property is changed in
     * the drawn tree component.
     *
     * @param tcr the new value of the {@code cellRenderer} property
     */
    protected void setCellRenderer(TreeCellRenderer tcr) {
        completeEditing();
        updateRenderer();
        if(treeState != null) {
            treeState.invalidateSizes();
            updateSize();
        }
    }

    /**
     * Returns the current instance of the {@link TreeCellRenderer} that is
     * rendering each cell.
     *
     * @return the {@link TreeCellRenderer} instance
     */
    protected TreeCellRenderer getCellRenderer() {
        return currentCellRenderer;
    }

    /**
     * Sets the {@code TreeModel}.
     *
     * @param model the new value
     */
    protected void setModel(TreeModel model) {
        completeEditing();
        if(treeModel != null && treeModelListener != null)
            treeModel.removeTreeModelListener(treeModelListener);
        treeModel = model;
        if(treeModel != null) {
            if(treeModelListener != null)
                treeModel.addTreeModelListener(treeModelListener);
        }
        if(treeState != null) {
            treeState.setModel(model);
            updateLayoutCacheExpandedNodesIfNecessary();
            updateSize();
        }
    }

    /**
     * Returns the tree model.
     *
     * @return the tree model
     */
    protected TreeModel getModel() {
        return treeModel;
    }

    /**
     * Sets the root to being visible.
     *
     * Called when the {@code rootVisible} property is changed in the drawn tree
     * component.
     *
     * @param newValue the new value of the {@code rootVisible} property
     */
    protected void setRootVisible(boolean newValue) {
        completeEditing();
        updateDepthOffset();
        if(treeState != null) {
            treeState.setRootVisible(newValue);
            treeState.invalidateSizes();
            updateSize();
        }
    }

    /**
     * Returns whether the root node of the drawn tree component should be displayed.
     *
     * @return {@code true} if the root node of the tree is displayed
     */
    protected boolean isRootVisible() {
        return (tree != null) ? tree.isRootVisible() : false;
    }

    /**
     * Determines whether the node handles are to be displayed.
     *
     * Called when the {@code showsRootHandles} property is changed in the drawn
     * tree component.
     *
     * @param newValue the new value of the {@code showsRootHandles} property
     */
    protected void setShowsRootHandles(boolean newValue) {
        completeEditing();
        updateDepthOffset();
        if(treeState != null) {
            treeState.invalidateSizes();
            updateSize();
        }
    }

    /**
     * Returns {@code true} if the root handles are to be displayed.
     *
     * @return {@code true} if the root handles are to be displayed
     */
    protected boolean getShowsRootHandles() {
        return (tree != null) ? tree.getShowsRootHandles() : false;
    }

    /**
     * Sets the cell editor.
     *
     * Called when the {@code cellEditor} property is changed in the drawn tree
     * component.
     *
     * @param editor the new value of the {@code cellEditor} property
     */
    protected void setCellEditor(TreeCellEditor editor) {
        updateCellEditor();
    }

    /**
     * Returns the editor used to edit entries in the drawn tree component, or
     * {@code null} if the tree cannot be edited.
     *
     * @return the {@link TreeCellEditor} instance, or {@code null}
     */
    protected TreeCellEditor getCellEditor() {
        return (tree != null) ? tree.getCellEditor() : null;
    }

    /**
     * Configures the receiver to allow, or not allow, editing.
     *
     * Called when the {@code editable} property is changed in the drawn tree
     * component.
     *
     * @param newValue the new value of the {@code editable} property
     */
    protected void setEditable(boolean newValue) {
        updateCellEditor();
    }

    /**
     * Returns whether the drawn tree component should be enabled for editing.
     *
     * @return {@code true} if the tree is editable
     */
    protected boolean isEditable() {
        return (tree != null) ? tree.isEditable() : false;
    }

    /**
     * Resets the selection model. The appropriate listener are installed
     * on the model.
     *
     * Called when the {@code selectionModel} property is changed in the drawn tree
     * component.
     *
     * @param newLSM the new value of the {@code selectionModel} property
     */
    protected void setSelectionModel(TreeSelectionModel newLSM) {
        completeEditing();
        if(selectionModelPropertyChangeListener != null &&
           treeSelectionModel != null)
            treeSelectionModel.removePropertyChangeListener
                              (selectionModelPropertyChangeListener);
        if(treeSelectionListener != null && treeSelectionModel != null)
            treeSelectionModel.removeTreeSelectionListener
                               (treeSelectionListener);
        treeSelectionModel = newLSM;
        if(treeSelectionModel != null) {
            if(selectionModelPropertyChangeListener != null)
                treeSelectionModel.addPropertyChangeListener
                              (selectionModelPropertyChangeListener);
            if(treeSelectionListener != null)
                treeSelectionModel.addTreeSelectionListener
                                   (treeSelectionListener);
            if(treeState != null)
                treeState.setSelectionModel(treeSelectionModel);
        }
        else if(treeState != null)
            treeState.setSelectionModel(null);
        if(tree != null)
            tree.repaint();
    }

    /**
     * Returns the current instance of the {@link TreeSelectionModel} which is
     * the model for selections.
     *
     * @return the {@link TreeSelectionModel} instance
     */
    protected TreeSelectionModel getSelectionModel() {
        return treeSelectionModel;
    }

    //
    // TreeUI methods
    //

    /**
      * Returns the Rectangle enclosing the label portion that the
      * last item in path will be drawn into.  Will return null if
      * any component in path is currently valid.
      */
    public Rectangle getPathBounds(JTree tree, TreePath path) {
        if(tree != null && treeState != null) {
            return getPathBounds(path, tree.getInsets(), new Rectangle());
        }
        return null;
    }

    private Rectangle getPathBounds(TreePath path, Insets insets,
                                    Rectangle bounds) {
        bounds = treeState.getBounds(path, bounds);
        if (bounds != null) {
            if (leftToRight) {
                bounds.x += insets.left;
            } else {
                bounds.x = tree.getWidth() - (bounds.x + bounds.width) -
                        insets.right;
            }
            bounds.y += insets.top;
        }
        return bounds;
    }

    /**
      * Returns the path for passed in row.  If row is not visible
      * null is returned.
      */
    public TreePath getPathForRow(JTree tree, int row) {
        return (treeState != null) ? treeState.getPathForRow(row) : null;
    }

    /**
      * Returns the row that the last item identified in path is visible
      * at.  Will return -1 if any of the elements in path are not
      * currently visible.
      */
    public int getRowForPath(JTree tree, TreePath path) {
        return (treeState != null) ? treeState.getRowForPath(path) : -1;
    }

    /**
      * Returns the number of rows that are being displayed.
      */
    public int getRowCount(JTree tree) {
        return (treeState != null) ? treeState.getRowCount() : 0;
    }

    /**
      * Returns the path to the node that is closest to x,y.  If
      * there is nothing currently visible this will return null, otherwise
      * it'll always return a valid path.  If you need to test if the
      * returned object is exactly at x, y you should get the bounds for
      * the returned path and test x, y against that.
      */
    public TreePath getClosestPathForLocation(JTree tree, int x, int y) {
        if(tree != null && treeState != null) {
            // TreeState doesn't care about the x location, hence it isn't
            // adjusted
            y -= tree.getInsets().top;
            return treeState.getPathClosestTo(x, y);
        }
        return null;
    }

    /**
      * Returns true if the tree is being edited.  The item that is being
      * edited can be returned by getEditingPath().
      */
    public boolean isEditing(JTree tree) {
        return (editingComponent != null);
    }

    /**
      * Stops the current editing session.  This has no effect if the
      * tree isn't being edited.  Returns true if the editor allows the
      * editing session to stop.
      */
    public boolean stopEditing(JTree tree) {
        if(editingComponent != null && cellEditor.stopCellEditing()) {
            completeEditing(false, false, true);
            return true;
        }
        return false;
    }

    /**
      * Cancels the current editing session.
      */
    public void cancelEditing(JTree tree) {
        if(editingComponent != null) {
            completeEditing(false, true, false);
        }
    }

    /**
      * Selects the last item in path and tries to edit it.  Editing will
      * fail if the CellEditor won't allow it for the selected item.
      */
    public void startEditingAtPath(JTree tree, TreePath path) {
        tree.scrollPathToVisible(path);
        if(path != null && tree.isVisible(path))
            startEditing(path, null);
    }

    /**
     * Returns the path to the element that is being edited.
     */
    public TreePath getEditingPath(JTree tree) {
        return editingPath;
    }

    //
    // Install methods
    //

    public void installUI(JComponent c) {
        if ( c == null ) {
            throw new NullPointerException( "null component passed to BasicTreeUI.installUI()" );
        }

        tree = (JTree)c;

        prepareForUIInstall();

        // Boilerplate install block
        installDefaults();
        installKeyboardActions();
        installComponents();
        installListeners();

        completeUIInstall();
    }

    /**
     * Invoked after the {@code tree} instance variable has been
     * set, but before any defaults/listeners have been installed.
     */
    protected void prepareForUIInstall() {
        drawingCache = new Hashtable<TreePath,Boolean>(7);

        // Data member initializations
        leftToRight = BasicGraphicsUtils.isLeftToRight(tree);
        stopEditingInCompleteEditing = true;
        lastSelectedRow = -1;
        leadRow = -1;
        preferredSize = new Dimension();

        largeModel = tree.isLargeModel();
        if(getRowHeight() <= 0)
            largeModel = false;
        setModel(tree.getModel());
    }

    /**
     * Invoked from installUI after all the defaults/listeners have been
     * installed.
     */
    protected void completeUIInstall() {
        // Custom install code

        this.setShowsRootHandles(tree.getShowsRootHandles());

        updateRenderer();

        updateDepthOffset();

        setSelectionModel(tree.getSelectionModel());

        // Create, if necessary, the TreeState instance.
        treeState = createLayoutCache();
        configureLayoutCache();

        updateSize();
    }

    /**
     * Installs default properties.
     */
    protected void installDefaults() {
        if(tree.getBackground() == null ||
           tree.getBackground() instanceof UIResource) {
            tree.setBackground(UIManager.getColor("Tree.background"));
        }
        if(getHashColor() == null || getHashColor() instanceof UIResource) {
            setHashColor(UIManager.getColor("Tree.hash"));
        }
        if (tree.getFont() == null || tree.getFont() instanceof UIResource)
            tree.setFont( UIManager.getFont("Tree.font") );
        // JTree's original row height is 16.  To correctly display the
        // contents on Linux we should have set it to 18, Windows 19 and
        // Solaris 20.  As these values vary so much it's too hard to
        // be backward compatable and try to update the row height, we're
        // therefor NOT going to adjust the row height based on font.  If the
        // developer changes the font, it's there responsibility to update
        // the row height.

        setExpandedIcon( (Icon)UIManager.get( "Tree.expandedIcon" ) );
        setCollapsedIcon( (Icon)UIManager.get( "Tree.collapsedIcon" ) );

        setLeftChildIndent(((Integer)UIManager.get("Tree.leftChildIndent")).
                           intValue());
        setRightChildIndent(((Integer)UIManager.get("Tree.rightChildIndent")).
                           intValue());

        LookAndFeel.installProperty(tree, "rowHeight",
                                    UIManager.get("Tree.rowHeight"));

        largeModel = (tree.isLargeModel() && tree.getRowHeight() > 0);

        Object scrollsOnExpand = UIManager.get("Tree.scrollsOnExpand");
        if (scrollsOnExpand != null) {
            LookAndFeel.installProperty(tree, "scrollsOnExpand", scrollsOnExpand);
        }

        paintLines = UIManager.getBoolean("Tree.paintLines");
        lineTypeDashed = UIManager.getBoolean("Tree.lineTypeDashed");

        Long l = (Long)UIManager.get("Tree.timeFactor");
        timeFactor = (l!=null) ? l.longValue() : 1000L;

        Object showsRootHandles = UIManager.get("Tree.showsRootHandles");
        if (showsRootHandles != null) {
            LookAndFeel.installProperty(tree,
                    JTree.SHOWS_ROOT_HANDLES_PROPERTY, showsRootHandles);
        }
    }

    /**
     * Registers listeners.
     */
    protected void installListeners() {
        if ( (propertyChangeListener = createPropertyChangeListener())
             != null ) {
            tree.addPropertyChangeListener(propertyChangeListener);
        }
        if ( (mouseListener = createMouseListener()) != null ) {
            tree.addMouseListener(mouseListener);
            if (mouseListener instanceof MouseMotionListener) {
                tree.addMouseMotionListener((MouseMotionListener)mouseListener);
            }
        }
        if ((focusListener = createFocusListener()) != null ) {
            tree.addFocusListener(focusListener);
        }
        if ((keyListener = createKeyListener()) != null) {
            tree.addKeyListener(keyListener);
        }
        if((treeExpansionListener = createTreeExpansionListener()) != null) {
            tree.addTreeExpansionListener(treeExpansionListener);
        }
        if((treeModelListener = createTreeModelListener()) != null &&
           treeModel != null) {
            treeModel.addTreeModelListener(treeModelListener);
        }
        if((selectionModelPropertyChangeListener =
            createSelectionModelPropertyChangeListener()) != null &&
           treeSelectionModel != null) {
            treeSelectionModel.addPropertyChangeListener
                (selectionModelPropertyChangeListener);
        }
        if((treeSelectionListener = createTreeSelectionListener()) != null &&
           treeSelectionModel != null) {
            treeSelectionModel.addTreeSelectionListener(treeSelectionListener);
        }

        TransferHandler th = tree.getTransferHandler();
        if (th == null || th instanceof UIResource) {
            tree.setTransferHandler(defaultTransferHandler);
            // default TransferHandler doesn't support drop
            // so we don't want drop handling
            if (tree.getDropTarget() instanceof UIResource) {
                tree.setDropTarget(null);
            }
        }

        LookAndFeel.installProperty(tree, "opaque", Boolean.TRUE);
    }

    /**
     * Registers keyboard actions.
     */
    protected void installKeyboardActions() {
        InputMap km = getInputMap(JComponent.
                                  WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        SwingUtilities.replaceUIInputMap(tree, JComponent.
                                         WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                                         km);
        km = getInputMap(JComponent.WHEN_FOCUSED);
        SwingUtilities.replaceUIInputMap(tree, JComponent.WHEN_FOCUSED, km);

        LazyActionMap.installLazyActionMap(tree, BasicTreeUI.class,
                                           "Tree.actionMap");
    }

    InputMap getInputMap(int condition) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            return (InputMap)DefaultLookup.get(tree, this,
                                               "Tree.ancestorInputMap");
        }
        else if (condition == JComponent.WHEN_FOCUSED) {
            InputMap keyMap = (InputMap)DefaultLookup.get(tree, this,
                                                      "Tree.focusInputMap");
            InputMap rtlKeyMap;

            if (tree.getComponentOrientation().isLeftToRight() ||
                  ((rtlKeyMap = (InputMap)DefaultLookup.get(tree, this,
                  "Tree.focusInputMap.RightToLeft")) == null)) {
                return keyMap;
            } else {
                rtlKeyMap.setParent(keyMap);
                return rtlKeyMap;
            }
        }
        return null;
    }

    /**
     * Intalls the subcomponents of the tree, which is the renderer pane.
     */
    protected void installComponents() {
        if ((rendererPane = createCellRendererPane()) != null) {
            tree.add( rendererPane );
        }
    }

    //
    // Create methods.
    //

    /**
     * Creates an instance of {@code NodeDimensions} that is able to determine
     * the size of a given node in the tree.
     *
     * @return an instance of {@code NodeDimensions}
     */
    protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
        return new NodeDimensionsHandler();
    }

    /**
     * Creates a listener that is responsible that updates the UI based on
     * how the tree changes.
     *
     * @return an instance of the {@code PropertyChangeListener}
     */
    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    /**
     * Creates the listener responsible for updating the selection based on
     * mouse events.
     *
     * @return an instance of the {@code MouseListener}
     */
    protected MouseListener createMouseListener() {
        return getHandler();
    }

    /**
     * Creates a listener that is responsible for updating the display
     * when focus is lost/gained.
     *
     * @return an instance of the {@code FocusListener}
     */
    protected FocusListener createFocusListener() {
        return getHandler();
    }

    /**
     * Creates the listener responsible for getting key events from
     * the tree.
     *
     * @return an instance of the {@code KeyListener}
     */
    protected KeyListener createKeyListener() {
        return getHandler();
    }

    /**
     * Creates the listener responsible for getting property change
     * events from the selection model.
     *
     * @return an instance of the {@code PropertyChangeListener}
     */
    protected PropertyChangeListener createSelectionModelPropertyChangeListener() {
        return getHandler();
    }

    /**
     * Creates the listener that updates the display based on selection change
     * methods.
     *
     * @return an instance of the {@code TreeSelectionListener}
     */
    protected TreeSelectionListener createTreeSelectionListener() {
        return getHandler();
    }

    /**
     * Creates a listener to handle events from the current editor.
     *
     * @return an instance of the {@code CellEditorListener}
     */
    protected CellEditorListener createCellEditorListener() {
        return getHandler();
    }

    /**
     * Creates and returns a new ComponentHandler. This is used for
     * the large model to mark the validCachedPreferredSize as invalid
     * when the component moves.
     *
     * @return an instance of the {@code ComponentListener}
     */
    protected ComponentListener createComponentListener() {
        return new ComponentHandler();
    }

    /**
     * Creates and returns the object responsible for updating the treestate
     * when nodes expanded state changes.
     *
     * @return an instance of the {@code TreeExpansionListener}
     */
    protected TreeExpansionListener createTreeExpansionListener() {
        return getHandler();
    }

    /**
     * Creates the object responsible for managing what is expanded, as
     * well as the size of nodes.
     *
     * @return the object responsible for managing what is expanded
     */
    protected AbstractLayoutCache createLayoutCache() {
        if(isLargeModel() && getRowHeight() > 0) {
            return new FixedHeightLayoutCache();
        }
        return new VariableHeightLayoutCache();
    }

    /**
     * Returns the renderer pane that renderer components are placed in.
     *
     * @return an instance of the {@code CellRendererPane}
     */
    protected CellRendererPane createCellRendererPane() {
        return new CellRendererPane();
    }

    /**
     * Creates a default cell editor.
     *
     * @return a default cell editor
     */
    protected TreeCellEditor createDefaultCellEditor() {
        if(currentCellRenderer != null &&
           (currentCellRenderer instanceof DefaultTreeCellRenderer)) {
            DefaultTreeCellEditor editor = new DefaultTreeCellEditor
                        (tree, (DefaultTreeCellRenderer)currentCellRenderer);

            return editor;
        }
        return new DefaultTreeCellEditor(tree, null);
    }

    /**
     * Returns the default cell renderer that is used to do the
     * stamping of each node.
     *
     * @return an instance of {@code TreeCellRenderer}
     */
    protected TreeCellRenderer createDefaultCellRenderer() {
        return new DefaultTreeCellRenderer();
    }

    /**
     * Returns a listener that can update the tree when the model changes.
     *
     * @return an instance of the {@code TreeModelListener}.
     */
    protected TreeModelListener createTreeModelListener() {
        return getHandler();
    }

    //
    // Uninstall methods
    //

    public void uninstallUI(JComponent c) {
        completeEditing();

        prepareForUIUninstall();

        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        uninstallComponents();

        completeUIUninstall();
    }

    /**
     * Invoked before unstallation of UI.
     */
    protected void prepareForUIUninstall() {
    }

    /**
     * Uninstalls UI.
     */
    protected void completeUIUninstall() {
        if(createdRenderer) {
            tree.setCellRenderer(null);
        }
        if(createdCellEditor) {
            tree.setCellEditor(null);
        }
        cellEditor = null;
        currentCellRenderer = null;
        rendererPane = null;
        componentListener = null;
        propertyChangeListener = null;
        mouseListener = null;
        focusListener = null;
        keyListener = null;
        setSelectionModel(null);
        treeState = null;
        drawingCache = null;
        selectionModelPropertyChangeListener = null;
        tree = null;
        treeModel = null;
        treeSelectionModel = null;
        treeSelectionListener = null;
        treeExpansionListener = null;
    }

    /**
     * Uninstalls default properties.
     */
    protected void uninstallDefaults() {
        if (tree.getTransferHandler() instanceof UIResource) {
            tree.setTransferHandler(null);
        }
    }

    /**
     * Unregisters listeners.
     */
    protected void uninstallListeners() {
        if(componentListener != null) {
            tree.removeComponentListener(componentListener);
        }
        if (propertyChangeListener != null) {
            tree.removePropertyChangeListener(propertyChangeListener);
        }
        if (mouseListener != null) {
            tree.removeMouseListener(mouseListener);
            if (mouseListener instanceof MouseMotionListener) {
                tree.removeMouseMotionListener((MouseMotionListener)mouseListener);
            }
        }
        if (focusListener != null) {
            tree.removeFocusListener(focusListener);
        }
        if (keyListener != null) {
            tree.removeKeyListener(keyListener);
        }
        if(treeExpansionListener != null) {
            tree.removeTreeExpansionListener(treeExpansionListener);
        }
        if(treeModel != null && treeModelListener != null) {
            treeModel.removeTreeModelListener(treeModelListener);
        }
        if(selectionModelPropertyChangeListener != null &&
           treeSelectionModel != null) {
            treeSelectionModel.removePropertyChangeListener
                (selectionModelPropertyChangeListener);
        }
        if(treeSelectionListener != null && treeSelectionModel != null) {
            treeSelectionModel.removeTreeSelectionListener
                               (treeSelectionListener);
        }
        handler = null;
    }

    /**
     * Unregisters keyboard actions.
     */
    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIActionMap(tree, null);
        SwingUtilities.replaceUIInputMap(tree, JComponent.
                                         WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                                         null);
        SwingUtilities.replaceUIInputMap(tree, JComponent.WHEN_FOCUSED, null);
    }

    /**
     * Uninstalls the renderer pane.
     */
    protected void uninstallComponents() {
        if(rendererPane != null) {
            tree.remove(rendererPane);
        }
    }

    /**
     * Recomputes the right margin, and invalidates any tree states
     */
    private void redoTheLayout() {
        if (treeState != null) {
            treeState.invalidateSizes();
        }
    }

    /**
     * Returns the baseline.
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        UIDefaults lafDefaults = UIManager.getLookAndFeelDefaults();
        Component renderer = (Component)lafDefaults.get(
                BASELINE_COMPONENT_KEY);
        if (renderer == null) {
            TreeCellRenderer tcr = createDefaultCellRenderer();
            renderer = tcr.getTreeCellRendererComponent(
                    tree, "a", false, false, false, -1, false);
            lafDefaults.put(BASELINE_COMPONENT_KEY, renderer);
        }
        int rowHeight = tree.getRowHeight();
        int baseline;
        if (rowHeight > 0) {
            baseline = renderer.getBaseline(Integer.MAX_VALUE, rowHeight);
        }
        else {
            Dimension pref = renderer.getPreferredSize();
            baseline = renderer.getBaseline(pref.width, pref.height);
        }
        return baseline + tree.getInsets().top;
    }

    /**
     * Returns an enum indicating how the baseline of the component
     * changes as the size changes.
     *
     * @throws NullPointerException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(
            JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    //
    // Painting routines.
    //

    public void paint(Graphics g, JComponent c) {
        if (tree != c) {
            throw new InternalError("incorrect component");
        }

        // Should never happen if installed for a UI
        if(treeState == null) {
            return;
        }

        Rectangle        paintBounds = g.getClipBounds();
        Insets           insets = tree.getInsets();
        TreePath         initialPath = getClosestPathForLocation
                                       (tree, 0, paintBounds.y);
        Enumeration<?>   paintingEnumerator = treeState.getVisiblePathsFrom
                                              (initialPath);
        int              row = treeState.getRowForPath(initialPath);
        int              endY = paintBounds.y + paintBounds.height;

        drawingCache.clear();

        if(initialPath != null && paintingEnumerator != null) {
            TreePath   parentPath = initialPath;

            // Draw the lines, knobs, and rows

            // Find each parent and have them draw a line to their last child
            parentPath = parentPath.getParentPath();
            while(parentPath != null) {
                paintVerticalPartOfLeg(g, paintBounds, insets, parentPath);
                drawingCache.put(parentPath, Boolean.TRUE);
                parentPath = parentPath.getParentPath();
            }

            boolean         done = false;
            // Information for the node being rendered.
            boolean         isExpanded;
            boolean         hasBeenExpanded;
            boolean         isLeaf;
            Rectangle       boundsBuffer = new Rectangle();
            Rectangle       bounds;
            TreePath        path;
            boolean         rootVisible = isRootVisible();

            while(!done && paintingEnumerator.hasMoreElements()) {
                path = (TreePath)paintingEnumerator.nextElement();
                if(path != null) {
                    isLeaf = treeModel.isLeaf(path.getLastPathComponent());
                    if(isLeaf)
                        isExpanded = hasBeenExpanded = false;
                    else {
                        isExpanded = treeState.getExpandedState(path);
                        hasBeenExpanded = tree.hasBeenExpanded(path);
                    }
                    bounds = getPathBounds(path, insets, boundsBuffer);
                    if(bounds == null)
                        // This will only happen if the model changes out
                        // from under us (usually in another thread).
                        // Swing isn't multithreaded, but I'll put this
                        // check in anyway.
                        return;
                    // See if the vertical line to the parent has been drawn.
                    parentPath = path.getParentPath();
                    if(parentPath != null) {
                        if(drawingCache.get(parentPath) == null) {
                            paintVerticalPartOfLeg(g, paintBounds,
                                                   insets, parentPath);
                            drawingCache.put(parentPath, Boolean.TRUE);
                        }
                        paintHorizontalPartOfLeg(g, paintBounds, insets,
                                                 bounds, path, row,
                                                 isExpanded,
                                                 hasBeenExpanded, isLeaf);
                    }
                    else if(rootVisible && row == 0) {
                        paintHorizontalPartOfLeg(g, paintBounds, insets,
                                                 bounds, path, row,
                                                 isExpanded,
                                                 hasBeenExpanded, isLeaf);
                    }
                    if(shouldPaintExpandControl(path, row, isExpanded,
                                                hasBeenExpanded, isLeaf)) {
                        paintExpandControl(g, paintBounds, insets, bounds,
                                           path, row, isExpanded,
                                           hasBeenExpanded, isLeaf);
                    }
                    paintRow(g, paintBounds, insets, bounds, path,
                                 row, isExpanded, hasBeenExpanded, isLeaf);
                    if((bounds.y + bounds.height) >= endY)
                        done = true;
                }
                else {
                    done = true;
                }
                row++;
            }
        }

        paintDropLine(g);

        // Empty out the renderer pane, allowing renderers to be gc'ed.
        rendererPane.removeAll();

        drawingCache.clear();
    }

    /**
     * Tells if a {@code DropLocation} should be indicated by a line between
     * nodes. This is meant for {@code javax.swing.DropMode.INSERT} and
     * {@code javax.swing.DropMode.ON_OR_INSERT} drop modes.
     *
     * @param loc a {@code DropLocation}
     * @return {@code true} if the drop location should be shown as a line
     * @since 1.7
     */
    protected boolean isDropLine(JTree.DropLocation loc) {
        return loc != null && loc.getPath() != null && loc.getChildIndex() != -1;
    }

    /**
     * Paints the drop line.
     *
     * @param g {@code Graphics} object to draw on
     * @since 1.7
     */
    protected void paintDropLine(Graphics g) {
        JTree.DropLocation loc = tree.getDropLocation();
        if (!isDropLine(loc)) {
            return;
        }

        Color c = UIManager.getColor("Tree.dropLineColor");
        if (c != null) {
            g.setColor(c);
            Rectangle rect = getDropLineRect(loc);
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
    }

    /**
     * Returns a unbounding box for the drop line.
     *
     * @param loc a {@code DropLocation}
     * @return bounding box for the drop line
     * @since 1.7
     */
    protected Rectangle getDropLineRect(JTree.DropLocation loc) {
        Rectangle rect;
        TreePath path = loc.getPath();
        int index = loc.getChildIndex();
        boolean ltr = leftToRight;

        Insets insets = tree.getInsets();

        if (tree.getRowCount() == 0) {
            rect = new Rectangle(insets.left,
                                 insets.top,
                                 tree.getWidth() - insets.left - insets.right,
                                 0);
        } else {
            TreeModel model = getModel();
            Object root = model.getRoot();

            if (path.getLastPathComponent() == root
                    && index >= model.getChildCount(root)) {

                rect = tree.getRowBounds(tree.getRowCount() - 1);
                rect.y = rect.y + rect.height;
                Rectangle xRect;

                if (!tree.isRootVisible()) {
                    xRect = tree.getRowBounds(0);
                } else if (model.getChildCount(root) == 0){
                    xRect = tree.getRowBounds(0);
                    xRect.x += totalChildIndent;
                    xRect.width -= totalChildIndent + totalChildIndent;
                } else {
                    TreePath lastChildPath = path.pathByAddingChild(
                        model.getChild(root, model.getChildCount(root) - 1));
                    xRect = tree.getPathBounds(lastChildPath);
                }

                rect.x = xRect.x;
                rect.width = xRect.width;
            } else {
                if (index >= model.getChildCount(path.getLastPathComponent())) {
                    rect = tree.getPathBounds(path.pathByAddingChild(
                            model.getChild(path.getLastPathComponent(),
                                    index - 1)));
                    rect.y = rect.y + rect.height;
                } else {
                    rect = tree.getPathBounds(path.pathByAddingChild(
                            model.getChild(path.getLastPathComponent(),
                                    index)));
                }
            }
        }

        if (rect.y != 0) {
            rect.y--;
        }

        if (!ltr) {
            rect.x = rect.x + rect.width - 100;
        }

        rect.width = 100;
        rect.height = 2;

        return rect;
    }

    /**
     * Paints the horizontal part of the leg. The receiver should
     * NOT modify {@code clipBounds}, or {@code insets}.<p>
     * NOTE: {@code parentRow} can be -1 if the root is not visible.
     *
     * @param g a graphics context
     * @param clipBounds a clipped rectangle
     * @param insets insets
     * @param bounds a bounding rectangle
     * @param path a tree path
     * @param row a row
     * @param isExpanded {@code true} if the path is expanded
     * @param hasBeenExpanded {@code true} if the path has been expanded
     * @param isLeaf {@code true} if the path is leaf
     */
    protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds,
                                            Insets insets, Rectangle bounds,
                                            TreePath path, int row,
                                            boolean isExpanded,
                                            boolean hasBeenExpanded, boolean
                                            isLeaf) {
        if (!paintLines) {
            return;
        }

        // Don't paint the legs for the root'ish node if the
        int depth = path.getPathCount() - 1;
        if((depth == 0 || (depth == 1 && !isRootVisible())) &&
           !getShowsRootHandles()) {
            return;
        }

        int clipLeft = clipBounds.x;
        int clipRight = clipBounds.x + clipBounds.width;
        int clipTop = clipBounds.y;
        int clipBottom = clipBounds.y + clipBounds.height;
        int lineY = bounds.y + bounds.height / 2;

        if (leftToRight) {
            int leftX = bounds.x - getRightChildIndent();
            int nodeX = bounds.x - getHorizontalLegBuffer();

            if(lineY >= clipTop
                    && lineY < clipBottom
                    && nodeX >= clipLeft
                    && leftX < clipRight
                    && leftX < nodeX) {

                g.setColor(getHashColor());
                paintHorizontalLine(g, tree, lineY, leftX, nodeX - 1);
            }
        } else {
            int nodeX = bounds.x + bounds.width + getHorizontalLegBuffer();
            int rightX = bounds.x + bounds.width + getRightChildIndent();

            if(lineY >= clipTop
                    && lineY < clipBottom
                    && rightX >= clipLeft
                    && nodeX < clipRight
                    && nodeX < rightX) {

                g.setColor(getHashColor());
                paintHorizontalLine(g, tree, lineY, nodeX, rightX - 1);
            }
        }
    }

    /**
     * Paints the vertical part of the leg. The receiver should
     * NOT modify {@code clipBounds}, {@code insets}.
     *
     * @param g a graphics context
     * @param clipBounds a clipped rectangle
     * @param insets insets
     * @param path a tree path
     */
    protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds,
                                          Insets insets, TreePath path) {
        if (!paintLines) {
            return;
        }

        int depth = path.getPathCount() - 1;
        if (depth == 0 && !getShowsRootHandles() && !isRootVisible()) {
            return;
        }
        int lineX = getRowX(-1, depth + 1);
        if (leftToRight) {
            lineX = lineX - getRightChildIndent() + insets.left;
        }
        else {
            lineX = tree.getWidth() - lineX - insets.right +
                    getRightChildIndent() - 1;
        }
        int clipLeft = clipBounds.x;
        int clipRight = clipBounds.x + (clipBounds.width - 1);

        if (lineX >= clipLeft && lineX <= clipRight) {
            int clipTop = clipBounds.y;
            int clipBottom = clipBounds.y + clipBounds.height;
            Rectangle parentBounds = getPathBounds(tree, path);
            Rectangle lastChildBounds = getPathBounds(tree,
                                                     getLastChildPath(path));

            if(lastChildBounds == null)
                // This shouldn't happen, but if the model is modified
                // in another thread it is possible for this to happen.
                // Swing isn't multithreaded, but I'll add this check in
                // anyway.
                return;

            int       top;

            if(parentBounds == null) {
                top = Math.max(insets.top + getVerticalLegBuffer(),
                               clipTop);
            }
            else
                top = Math.max(parentBounds.y + parentBounds.height +
                               getVerticalLegBuffer(), clipTop);
            if(depth == 0 && !isRootVisible()) {
                TreeModel      model = getModel();

                if(model != null) {
                    Object        root = model.getRoot();

                    if(model.getChildCount(root) > 0) {
                        parentBounds = getPathBounds(tree, path.
                                  pathByAddingChild(model.getChild(root, 0)));
                        if(parentBounds != null)
                            top = Math.max(insets.top + getVerticalLegBuffer(),
                                           parentBounds.y +
                                           parentBounds.height / 2);
                    }
                }
            }

            int bottom = Math.min(lastChildBounds.y +
                                  (lastChildBounds.height / 2), clipBottom);

            if (top <= bottom) {
                g.setColor(getHashColor());
                paintVerticalLine(g, tree, lineX, top, bottom);
            }
        }
    }

    /**
     * Paints the expand (toggle) part of a row. The receiver should
     * NOT modify {@code clipBounds}, or {@code insets}.
     *
     * @param g a graphics context
     * @param clipBounds a clipped rectangle
     * @param insets insets
     * @param bounds a bounding rectangle
     * @param path a tree path
     * @param row a row
     * @param isExpanded {@code true} if the path is expanded
     * @param hasBeenExpanded {@code true} if the path has been expanded
     * @param isLeaf {@code true} if the row is leaf
     */
    protected void paintExpandControl(Graphics g,
                                      Rectangle clipBounds, Insets insets,
                                      Rectangle bounds, TreePath path,
                                      int row, boolean isExpanded,
                                      boolean hasBeenExpanded,
                                      boolean isLeaf) {
        Object       value = path.getLastPathComponent();

        // Draw icons if not a leaf and either hasn't been loaded,
        // or the model child count is > 0.
        if (!isLeaf && (!hasBeenExpanded ||
                        treeModel.getChildCount(value) > 0)) {
            int middleXOfKnob;
            if (leftToRight) {
                middleXOfKnob = bounds.x - getRightChildIndent() + 1;
            } else {
                middleXOfKnob = bounds.x + bounds.width + getRightChildIndent() - 1;
            }
            int middleYOfKnob = bounds.y + (bounds.height / 2);

            if (isExpanded) {
                Icon expandedIcon = getExpandedIcon();
                if(expandedIcon != null)
                  drawCentered(tree, g, expandedIcon, middleXOfKnob,
                               middleYOfKnob );
            }
            else {
                Icon collapsedIcon = getCollapsedIcon();
                if(collapsedIcon != null)
                  drawCentered(tree, g, collapsedIcon, middleXOfKnob,
                               middleYOfKnob);
            }
        }
    }

    /**
     * Paints the renderer part of a row. The receiver should
     * NOT modify {@code clipBounds}, or {@code insets}.
     *
     * @param g a graphics context
     * @param clipBounds a clipped rectangle
     * @param insets insets
     * @param bounds a bounding rectangle
     * @param path a tree path
     * @param row a row
     * @param isExpanded {@code true} if the path is expanded
     * @param hasBeenExpanded {@code true} if the path has been expanded
     * @param isLeaf {@code true} if the path is leaf
     */
    protected void paintRow(Graphics g, Rectangle clipBounds,
                            Insets insets, Rectangle bounds, TreePath path,
                            int row, boolean isExpanded,
                            boolean hasBeenExpanded, boolean isLeaf) {
        // Don't paint the renderer if editing this row.
        if(editingComponent != null && editingRow == row)
            return;

        int leadIndex;

        if(tree.hasFocus()) {
            leadIndex = getLeadSelectionRow();
        }
        else
            leadIndex = -1;

        Component component;

        component = currentCellRenderer.getTreeCellRendererComponent
                      (tree, path.getLastPathComponent(),
                       tree.isRowSelected(row), isExpanded, isLeaf, row,
                       (leadIndex == row));

        rendererPane.paintComponent(g, component, tree, bounds.x, bounds.y,
                                    bounds.width, bounds.height, true);
    }

    /**
     * Returns {@code true} if the expand (toggle) control should be drawn for
     * the specified row.
     *
     * @param path a tree path
     * @param row a row
     * @param isExpanded {@code true} if the path is expanded
     * @param hasBeenExpanded {@code true} if the path has been expanded
     * @param isLeaf {@code true} if the row is leaf
     * @return {@code true} if the expand (toggle) control should be drawn
     *         for the specified row
     */
    protected boolean shouldPaintExpandControl(TreePath path, int row,
                                               boolean isExpanded,
                                               boolean hasBeenExpanded,
                                               boolean isLeaf) {
        if(isLeaf)
            return false;

        int              depth = path.getPathCount() - 1;

        if((depth == 0 || (depth == 1 && !isRootVisible())) &&
           !getShowsRootHandles())
            return false;
        return true;
    }

    /**
     * Paints a vertical line.
     *
     * @param g a graphics context
     * @param c a component
     * @param x an X coordinate
     * @param top an Y1 coordinate
     * @param bottom an Y2 coordinate
     */
    protected void paintVerticalLine(Graphics g, JComponent c, int x, int top,
                                    int bottom) {
        if (lineTypeDashed) {
            drawDashedVerticalLine(g, x, top, bottom);
        } else {
            g.drawLine(x, top, x, bottom);
        }
    }

    /**
     * Paints a horizontal line.
     *
     * @param g a graphics context
     * @param c a component
     * @param y an Y coordinate
     * @param left an X1 coordinate
     * @param right an X2 coordinate
     */
    protected void paintHorizontalLine(Graphics g, JComponent c, int y,
                                      int left, int right) {
        if (lineTypeDashed) {
            drawDashedHorizontalLine(g, y, left, right);
        } else {
            g.drawLine(left, y, right, y);
        }
    }

    /**
     * The vertical element of legs between nodes starts at the bottom of the
     * parent node by default.  This method makes the leg start below that.
     *
     * @return the vertical leg buffer
     */
    protected int getVerticalLegBuffer() {
        return 0;
    }

    /**
     * The horizontal element of legs between nodes starts at the
     * right of the left-hand side of the child node by default.  This
     * method makes the leg end before that.
     *
     * @return the horizontal leg buffer
     */
    protected int getHorizontalLegBuffer() {
        return 0;
    }

    private int findCenteredX(int x, int iconWidth) {
        return leftToRight
               ? x - (int)Math.ceil(iconWidth / 2.0)
               : x - (int)Math.floor(iconWidth / 2.0);
    }

    //
    // Generic painting methods
    //

    /**
     * Draws the {@code icon} centered at (x,y).
     *
     * @param c a component
     * @param graphics a graphics context
     * @param icon an icon
     * @param x an X coordinate
     * @param y an Y coordinate
     */
    protected void drawCentered(Component c, Graphics graphics, Icon icon,
                                int x, int y) {
        icon.paintIcon(c, graphics,
                      findCenteredX(x, icon.getIconWidth()),
                      y - icon.getIconHeight() / 2);
    }

    /**
     * Draws a horizontal dashed line. It is assumed {@code x1} &lt;= {@code x2}.
     * If {@code x1} is greater than {@code x2}, the method draws nothing.
     *
     * @param g an instance of {@code Graphics}
     * @param y an Y coordinate
     * @param x1 an X1 coordinate
     * @param x2 an X2 coordinate
     */
    protected void drawDashedHorizontalLine(Graphics g, int y, int x1, int x2) {
        // Drawing only even coordinates helps join line segments so they
        // appear as one line.  This can be defeated by translating the
        // Graphics by an odd amount.
        drawDashedLine(g, y, x1, x2, false);
    }

    /**
     * Draws a vertical dashed line. It is assumed {@code y1} &lt;= {@code y2}.
     * If {@code y1} is greater than {@code y2}, the method draws nothing.
     *
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y1 an Y1 coordinate
     * @param y2 an Y2 coordinate
     */
    protected void drawDashedVerticalLine(Graphics g, int x, int y1, int y2) {
        // Drawing only even coordinates helps join line segments so they
        // appear as one line.  This can be defeated by translating the
        // Graphics by an odd amount.
        drawDashedLine(g, x, y1, y2, true);
    }

    private void drawDashedLine(Graphics g, int v, int v1, int v2, boolean isVertical) {
        if (v1 >= v2) {
            return;
        }
        v1 += (v1 % 2);
        Graphics2D g2d = (Graphics2D) g;
        Stroke oldStroke = g2d.getStroke();

        BasicStroke dashedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 0, new float[]{1}, 0);
        g2d.setStroke(dashedStroke);
        if (isVertical) {
            g2d.drawLine(v, v1, v, v2);
        } else {
            g2d.drawLine(v1, v, v2, v);
        }

        g2d.setStroke(oldStroke);
    }
    //
    // Various local methods
    //

    /**
     * Returns the location, along the x-axis, to render a particular row
     * at. The return value does not include any Insets specified on the JTree.
     * This does not check for the validity of the row or depth, it is assumed
     * to be correct and will not throw an Exception if the row or depth
     * doesn't match that of the tree.
     *
     * @param row Row to return x location for
     * @param depth Depth of the row
     * @return amount to indent the given row.
     * @since 1.5
     */
    protected int getRowX(int row, int depth) {
        return totalChildIndent * (depth + depthOffset);
    }

    /**
     * Makes all the nodes that are expanded in JTree expanded in LayoutCache.
     * This invokes updateExpandedDescendants with the root path.
     */
    protected void updateLayoutCacheExpandedNodes() {
        if(treeModel != null && treeModel.getRoot() != null)
            updateExpandedDescendants(new TreePath(treeModel.getRoot()));
    }

    private void updateLayoutCacheExpandedNodesIfNecessary() {
        if (treeModel != null && treeModel.getRoot() != null) {
            TreePath rootPath = new TreePath(treeModel.getRoot());
            if (tree.isExpanded(rootPath)) {
                updateLayoutCacheExpandedNodes();
            } else {
                treeState.setExpandedState(rootPath, false);
            }
        }
    }

    /**
     * Updates the expanded state of all the descendants of {@code path}
     * by getting the expanded descendants from the tree and forwarding
     * to the tree state.
     *
     * @param path a tree path
     */
    protected void updateExpandedDescendants(TreePath path) {
        completeEditing();
        if(treeState != null) {
            treeState.setExpandedState(path, true);

            Enumeration<?> descendants = tree.getExpandedDescendants(path);

            if(descendants != null) {
                while(descendants.hasMoreElements()) {
                    path = (TreePath)descendants.nextElement();
                    treeState.setExpandedState(path, true);
                }
            }
            updateLeadSelectionRow();
            updateSize();
        }
    }

    /**
     * Returns a path to the last child of {@code parent}.
     *
     * @param parent a tree path
     * @return a path to the last child of {@code parent}
     */
    protected TreePath getLastChildPath(TreePath parent) {
        if(treeModel != null) {
            int         childCount = treeModel.getChildCount
                (parent.getLastPathComponent());

            if(childCount > 0)
                return parent.pathByAddingChild(treeModel.getChild
                           (parent.getLastPathComponent(), childCount - 1));
        }
        return null;
    }

    /**
     * Updates how much each depth should be offset by.
     */
    protected void updateDepthOffset() {
        if(isRootVisible()) {
            if(getShowsRootHandles())
                depthOffset = 1;
            else
                depthOffset = 0;
        }
        else if(!getShowsRootHandles())
            depthOffset = -1;
        else
            depthOffset = 0;
    }

    /**
      * Updates the cellEditor based on the editability of the JTree that
      * we're contained in.  If the tree is editable but doesn't have a
      * cellEditor, a basic one will be used.
      */
    protected void updateCellEditor() {
        TreeCellEditor        newEditor;

        completeEditing();
        if(tree == null)
            newEditor = null;
        else {
            if(tree.isEditable()) {
                newEditor = tree.getCellEditor();
                if(newEditor == null) {
                    newEditor = createDefaultCellEditor();
                    if(newEditor != null) {
                        tree.setCellEditor(newEditor);
                        createdCellEditor = true;
                    }
                }
            }
            else
                newEditor = null;
        }
        if(newEditor != cellEditor) {
            if(cellEditor != null && cellEditorListener != null)
                cellEditor.removeCellEditorListener(cellEditorListener);
            cellEditor = newEditor;
            if(cellEditorListener == null)
                cellEditorListener = createCellEditorListener();
            if(newEditor != null && cellEditorListener != null)
                newEditor.addCellEditorListener(cellEditorListener);
            createdCellEditor = false;
        }
    }

    /**
      * Messaged from the tree we're in when the renderer has changed.
      */
    protected void updateRenderer() {
        if(tree != null) {
            TreeCellRenderer      newCellRenderer;

            newCellRenderer = tree.getCellRenderer();
            if(newCellRenderer == null) {
                tree.setCellRenderer(createDefaultCellRenderer());
                createdRenderer = true;
            }
            else {
                createdRenderer = false;
                currentCellRenderer = newCellRenderer;
                if(createdCellEditor) {
                    tree.setCellEditor(null);
                }
            }
        }
        else {
            createdRenderer = false;
            currentCellRenderer = null;
        }
        updateCellEditor();
    }

    /**
     * Resets the TreeState instance based on the tree we're providing the
     * look and feel for.
     */
    protected void configureLayoutCache() {
        if(treeState != null && tree != null) {
            if(nodeDimensions == null)
                nodeDimensions = createNodeDimensions();
            treeState.setNodeDimensions(nodeDimensions);
            treeState.setRootVisible(tree.isRootVisible());
            treeState.setRowHeight(tree.getRowHeight());
            treeState.setSelectionModel(getSelectionModel());
            // Only do this if necessary, may loss state if call with
            // same model as it currently has.
            if(treeState.getModel() != tree.getModel())
                treeState.setModel(tree.getModel());
            updateLayoutCacheExpandedNodesIfNecessary();
            // Create a listener to update preferred size when bounds
            // changes, if necessary.
            if(isLargeModel()) {
                if(componentListener == null) {
                    componentListener = createComponentListener();
                    if(componentListener != null)
                        tree.addComponentListener(componentListener);
                }
            }
            else if(componentListener != null) {
                tree.removeComponentListener(componentListener);
                componentListener = null;
            }
        }
        else if(componentListener != null) {
            tree.removeComponentListener(componentListener);
            componentListener = null;
        }
    }

    /**
     * Marks the cached size as being invalid, and messages the
     * tree with <code>treeDidChange</code>.
     */
    protected void updateSize() {
        validCachedPreferredSize = false;
        tree.treeDidChange();
    }

    private void updateSize0() {
        validCachedPreferredSize = false;
        tree.revalidate();
    }

    /**
     * Updates the <code>preferredSize</code> instance variable,
     * which is returned from <code>getPreferredSize()</code>.<p>
     * For left to right orientations, the size is determined from the
     * current AbstractLayoutCache. For RTL orientations, the preferred size
     * becomes the width minus the minimum x position.
     */
    protected void updateCachedPreferredSize() {
        if(treeState != null) {
            Insets               i = tree.getInsets();

            if(isLargeModel()) {
                Rectangle            visRect = tree.getVisibleRect();

                if (visRect.x == 0 && visRect.y == 0 &&
                        visRect.width == 0 && visRect.height == 0 &&
                        tree.getVisibleRowCount() > 0) {
                    // The tree doesn't have a valid bounds yet. Calculate
                    // based on visible row count.
                    visRect.width = 1;
                    visRect.height = tree.getRowHeight() *
                            tree.getVisibleRowCount();
                } else {
                    visRect.x -= i.left;
                    visRect.y -= i.top;
                }
                // we should consider a non-visible area above
                Component component = SwingUtilities.getUnwrappedParent(tree);
                if (component instanceof JViewport) {
                    component = component.getParent();
                    if (component instanceof JScrollPane) {
                        JScrollPane pane = (JScrollPane) component;
                        JScrollBar bar = pane.getHorizontalScrollBar();
                        if ((bar != null) && bar.isVisible()) {
                            int height = bar.getHeight();
                            visRect.y -= height;
                            visRect.height += height;
                        }
                    }
                }
                preferredSize.width = treeState.getPreferredWidth(visRect);
            }
            else {
                preferredSize.width = treeState.getPreferredWidth(null);
            }
            preferredSize.height = treeState.getPreferredHeight();
            preferredSize.width += i.left + i.right;
            preferredSize.height += i.top + i.bottom;
        }
        validCachedPreferredSize = true;
    }

    /**
     * Messaged from the {@code VisibleTreeNode} after it has been expanded.
     *
     * @param path a tree path
     */
    protected void pathWasExpanded(TreePath path) {
        if(tree != null) {
            tree.fireTreeExpanded(path);
        }
    }

    /**
     * Messaged from the {@code VisibleTreeNode} after it has collapsed.
     *
     * @param path a tree path
     */
    protected void pathWasCollapsed(TreePath path) {
        if(tree != null) {
            tree.fireTreeCollapsed(path);
        }
    }

    /**
     * Ensures that the rows identified by {@code beginRow} through
     * {@code endRow} are visible.
     *
     * @param beginRow the begin row
     * @param endRow the end row
     */
    protected void ensureRowsAreVisible(int beginRow, int endRow) {
        if(tree != null && beginRow >= 0 && endRow < getRowCount(tree)) {
            boolean scrollVert = DefaultLookup.getBoolean(tree, this,
                              "Tree.scrollsHorizontallyAndVertically", false);
            if(beginRow == endRow) {
                Rectangle     scrollBounds = getPathBounds(tree, getPathForRow
                                                           (tree, beginRow));

                if(scrollBounds != null) {
                    if (!scrollVert) {
                        scrollBounds.x = tree.getVisibleRect().x;
                        scrollBounds.width = 1;
                    }
                    tree.scrollRectToVisible(scrollBounds);
                }
            }
            else {
                Rectangle   beginRect = getPathBounds(tree, getPathForRow
                                                      (tree, beginRow));
                if (beginRect != null) {
                    Rectangle   visRect = tree.getVisibleRect();
                    Rectangle   testRect = beginRect;
                    int         beginY = beginRect.y;
                    int         maxY = beginY + visRect.height;

                    for(int counter = beginRow + 1; counter <= endRow; counter++) {
                            testRect = getPathBounds(tree,
                                    getPathForRow(tree, counter));
                        if (testRect == null) {
                            return;
                        }
                        if((testRect.y + testRect.height) > maxY)
                                counter = endRow;
                            }
                        tree.scrollRectToVisible(new Rectangle(visRect.x, beginY, 1,
                                                      testRect.y + testRect.height-
                                                      beginY));
                }
            }
        }
    }

    /**
     * Sets the preferred minimum size.
     *
     * @param newSize the new preferred size
     */
    public void setPreferredMinSize(Dimension newSize) {
        preferredMinSize = newSize;
    }

    /**
     * Returns the minimum preferred size.
     *
     * @return the minimum preferred size
     */
    public Dimension getPreferredMinSize() {
        if(preferredMinSize == null)
            return null;
        return new Dimension(preferredMinSize);
    }

    /**
     * Returns the preferred size to properly display the tree,
     * this is a cover method for {@code getPreferredSize(c, true)}.
     *
     * @param c a component
     * @return the preferred size to represent the tree in the component
     */
    public Dimension getPreferredSize(JComponent c) {
        return getPreferredSize(c, true);
    }

    /**
     * Returns the preferred size to represent the tree in
     * <I>c</I>.  If <I>checkConsistency</I> is {@code true}
     * <b>checkConsistency</b> is messaged first.
     *
     * @param c a component
     * @param checkConsistency if {@code true} consistency is checked
     * @return the preferred size to represent the tree in the component
     */
    public Dimension getPreferredSize(JComponent c,
                                      boolean checkConsistency) {
        Dimension       pSize = this.getPreferredMinSize();

        if(!validCachedPreferredSize)
            updateCachedPreferredSize();
        if(tree != null) {
            if(pSize != null)
                return new Dimension(Math.max(pSize.width,
                                              preferredSize.width),
                              Math.max(pSize.height, preferredSize.height));
            return new Dimension(preferredSize.width, preferredSize.height);
        }
        else if(pSize != null)
            return pSize;
        else
            return new Dimension(0, 0);
    }

    /**
      * Returns the minimum size for this component.  Which will be
      * the min preferred size or 0, 0.
      */
    public Dimension getMinimumSize(JComponent c) {
        if(this.getPreferredMinSize() != null)
            return this.getPreferredMinSize();
        return new Dimension(0, 0);
    }

    /**
      * Returns the maximum size for this component, which will be the
      * preferred size if the instance is currently in a JTree, or 0, 0.
      */
    public Dimension getMaximumSize(JComponent c) {
        if(tree != null)
            return getPreferredSize(tree);
        if(this.getPreferredMinSize() != null)
            return this.getPreferredMinSize();
        return new Dimension(0, 0);
    }


    /**
     * Messages to stop the editing session. If the UI the receiver
     * is providing the look and feel for returns true from
     * <code>getInvokesStopCellEditing</code>, stopCellEditing will
     * invoked on the current editor. Then completeEditing will
     * be messaged with false, true, false to cancel any lingering
     * editing.
     */
    protected void completeEditing() {
        /* If should invoke stopCellEditing, try that */
        if(tree.getInvokesStopCellEditing() &&
           stopEditingInCompleteEditing && editingComponent != null) {
            cellEditor.stopCellEditing();
        }
        /* Invoke cancelCellEditing, this will do nothing if stopCellEditing
           was successful. */
        completeEditing(false, true, false);
    }

    /**
     * Stops the editing session. If {@code messageStop} is {@code true} the editor
     * is messaged with {@code stopEditing}, if {@code messageCancel}
     * is {@code true} the editor is messaged with {@code cancelEditing}.
     * If {@code messageTree} is {@code true} the {@code treeModel} is messaged
     * with {@code valueForPathChanged}.
     *
     * @param messageStop message to stop editing
     * @param messageCancel message to cancel editing
     * @param messageTree message to tree
     */
    @SuppressWarnings("deprecation")
    protected void completeEditing(boolean messageStop,
                                   boolean messageCancel,
                                   boolean messageTree) {
        if(stopEditingInCompleteEditing && editingComponent != null) {
            Component             oldComponent = editingComponent;
            TreePath              oldPath = editingPath;
            TreeCellEditor        oldEditor = cellEditor;
            Object                newValue = oldEditor.getCellEditorValue();
            Rectangle             editingBounds = getPathBounds(tree,
                                                                editingPath);
            boolean               requestFocus = (tree != null &&
                                   (tree.hasFocus() || SwingUtilities.
                                    findFocusOwner(editingComponent) != null));

            editingComponent = null;
            editingPath = null;
            if(messageStop)
                oldEditor.stopCellEditing();
            else if(messageCancel)
                oldEditor.cancelCellEditing();
            tree.remove(oldComponent);
            if(editorHasDifferentSize) {
                treeState.invalidatePathBounds(oldPath);
                updateSize();
            }
            else if (editingBounds != null) {
                editingBounds.x = 0;
                editingBounds.width = tree.getSize().width;
                tree.repaint(editingBounds);
            }
            if(requestFocus)
                tree.requestFocus();
            if(messageTree)
                treeModel.valueForPathChanged(oldPath, newValue);
        }
    }

    // cover method for startEditing that allows us to pass extra
    // information into that method via a class variable
    private boolean startEditingOnRelease(TreePath path,
                                          MouseEvent event,
                                          MouseEvent releaseEvent) {
        this.releaseEvent = releaseEvent;
        try {
            return startEditing(path, event);
        } finally {
            this.releaseEvent = null;
        }
    }

    /**
     * Will start editing for node if there is a {@code cellEditor} and
     * {@code shouldSelectCell} returns {@code true}.<p>
     * This assumes that path is valid and visible.
     *
     * @param path a tree path
     * @param event a mouse event
     * @return {@code true} if the editing is successful
     */
    protected boolean startEditing(TreePath path, MouseEvent event) {
        if (isEditing(tree) && tree.getInvokesStopCellEditing() &&
                               !stopEditing(tree)) {
            return false;
        }
        completeEditing();
        if(cellEditor != null && tree.isPathEditable(path)) {
            int           row = getRowForPath(tree, path);

            if(cellEditor.isCellEditable(event)) {
                editingComponent = cellEditor.getTreeCellEditorComponent
                      (tree, path.getLastPathComponent(),
                       tree.isPathSelected(path), tree.isExpanded(path),
                       treeModel.isLeaf(path.getLastPathComponent()), row);
                Rectangle           nodeBounds = getPathBounds(tree, path);
                if (nodeBounds == null) {
                    return false;
                }

                editingRow = row;

                Dimension editorSize = editingComponent.getPreferredSize();

                // Only allow odd heights if explicitly set.
                if(editorSize.height != nodeBounds.height &&
                   getRowHeight() > 0)
                    editorSize.height = getRowHeight();

                if(editorSize.width != nodeBounds.width ||
                   editorSize.height != nodeBounds.height) {
                    // Editor wants different width or height, invalidate
                    // treeState and relayout.
                    editorHasDifferentSize = true;
                    treeState.invalidatePathBounds(path);
                    updateSize();
                    // To make sure x/y are updated correctly, fetch
                    // the bounds again.
                    nodeBounds = getPathBounds(tree, path);
                    if (nodeBounds == null) {
                        return false;
                    }
                }
                else
                    editorHasDifferentSize = false;
                tree.add(editingComponent);
                editingComponent.setBounds(nodeBounds.x, nodeBounds.y,
                                           nodeBounds.width,
                                           nodeBounds.height);
                editingPath = path;
                AWTAccessor.getComponentAccessor().revalidateSynchronously(editingComponent);
                editingComponent.repaint();
                if(cellEditor.shouldSelectCell(event)) {
                    stopEditingInCompleteEditing = false;
                    tree.setSelectionRow(row);
                    stopEditingInCompleteEditing = true;
                }

                Component focusedComponent = SwingUtilities2.
                                 compositeRequestFocus(editingComponent);
                boolean selectAll = true;

                if(event != null) {
                    /* Find the component that will get forwarded all the
                       mouse events until mouseReleased. */
                    Point          componentPoint = SwingUtilities.convertPoint
                        (tree, new Point(event.getX(), event.getY()),
                         editingComponent);

                    /* Create an instance of BasicTreeMouseListener to handle
                       passing the mouse/motion events to the necessary
                       component. */
                    // We really want similar behavior to getMouseEventTarget,
                    // but it is package private.
                    Component activeComponent = SwingUtilities.
                                    getDeepestComponentAt(editingComponent,
                                       componentPoint.x, componentPoint.y);
                    if (activeComponent != null) {
                        MouseInputHandler handler =
                            new MouseInputHandler(tree, activeComponent,
                                                  event, focusedComponent);

                        if (releaseEvent != null) {
                            handler.mouseReleased(releaseEvent);
                        }

                        selectAll = false;
                    }
                }
                if (selectAll && focusedComponent instanceof JTextField) {
                    ((JTextField)focusedComponent).selectAll();
                }
                return true;
            }
            else
                editingComponent = null;
        }
        return false;
    }

    //
    // Following are primarily for handling mouse events.
    //

    /**
     * If the {@code mouseX} and {@code mouseY} are in the
     * expand/collapse region of the {@code row}, this will toggle
     * the row.
     *
     * @param path a tree path
     * @param mouseX an X coordinate
     * @param mouseY an Y coordinate
     */
    protected void checkForClickInExpandControl(TreePath path,
                                                int mouseX, int mouseY) {
      if (isLocationInExpandControl(path, mouseX, mouseY)) {
          handleExpandControlClick(path, mouseX, mouseY);
        }
    }

    /**
     * Returns {@code true} if {@code mouseX} and {@code mouseY} fall
     * in the area of row that is used to expand/collapse the node and
     * the node at {@code row} does not represent a leaf.
     *
     * @param path a tree path
     * @param mouseX an X coordinate
     * @param mouseY an Y coordinate
     * @return {@code true} if the mouse cursor fall in the area of row that
     *         is used to expand/collapse the node and the node is not a leaf.
     */
    protected boolean isLocationInExpandControl(TreePath path,
                                                int mouseX, int mouseY) {
        if(path != null && !treeModel.isLeaf(path.getLastPathComponent())){
            int                     boxWidth;
            Insets                  i = tree.getInsets();

            if(getExpandedIcon() != null)
                boxWidth = getExpandedIcon().getIconWidth();
            else
                boxWidth = 8;

            int boxLeftX = getRowX(tree.getRowForPath(path),
                                   path.getPathCount() - 1);

            if (leftToRight) {
                boxLeftX = boxLeftX + i.left - getRightChildIndent() + 1;
            } else {
                boxLeftX = tree.getWidth() - boxLeftX - i.right + getRightChildIndent() - 1;
            }

            boxLeftX = findCenteredX(boxLeftX, boxWidth);

            return (mouseX >= boxLeftX && mouseX < (boxLeftX + boxWidth));
        }
        return false;
    }

    /**
     * Messaged when the user clicks the particular row, this invokes
     * {@code toggleExpandState}.
     *
     * @param path a tree path
     * @param mouseX an X coordinate
     * @param mouseY an Y coordinate
     */
    protected void handleExpandControlClick(TreePath path, int mouseX,
                                            int mouseY) {
        toggleExpandState(path);
    }

    /**
     * Expands path if it is not expanded, or collapses row if it is expanded.
     * If expanding a path and {@code JTree} scrolls on expand,
     * {@code ensureRowsAreVisible} is invoked to scroll as many of the children
     * to visible as possible (tries to scroll to last visible descendant of path).
     *
     * @param path a tree path
     */
    protected void toggleExpandState(TreePath path) {
        if(!tree.isExpanded(path)) {
            int       row = getRowForPath(tree, path);

            tree.expandPath(path);
            updateSize();
            if(row != -1) {
                if(tree.getScrollsOnExpand())
                    ensureRowsAreVisible(row, row + treeState.
                                         getVisibleChildCount(path));
                else
                    ensureRowsAreVisible(row, row);
            }
        }
        else {
            tree.collapsePath(path);
            updateSize();
        }
    }

    /**
     * Returning {@code true} signifies a mouse event on the node should toggle
     * the selection of only the row under mouse.
     *
     * @param event a mouse event
     * @return {@code true} if a mouse event on the node should toggle the selection
     */
    protected boolean isToggleSelectionEvent(MouseEvent event) {
        return (SwingUtilities.isLeftMouseButton(event) &&
                BasicGraphicsUtils.isMenuShortcutKeyDown(event));
    }

    /**
     * Returning {@code true} signifies a mouse event on the node should select
     * from the anchor point.
     *
     * @param event a mouse event
     * @return {@code true} if a mouse event on the node should select
     *         from the anchor point
     */
    protected boolean isMultiSelectEvent(MouseEvent event) {
        return (SwingUtilities.isLeftMouseButton(event) &&
                event.isShiftDown());
    }

    /**
     * Returning {@code true} indicates the row under the mouse should be toggled
     * based on the event. This is invoked after {@code checkForClickInExpandControl},
     * implying the location is not in the expand (toggle) control.
     *
     * @param event a mouse event
     * @return {@code true} if the row under the mouse should be toggled
     */
    protected boolean isToggleEvent(MouseEvent event) {
        if(!SwingUtilities.isLeftMouseButton(event)) {
            return false;
        }
        int           clickCount = tree.getToggleClickCount();

        if(clickCount <= 0) {
            return false;
        }
        return ((event.getClickCount() % clickCount) == 0);
    }

    /**
     * Messaged to update the selection based on a {@code MouseEvent} over a
     * particular row. If the event is a toggle selection event, the
     * row is either selected, or deselected. If the event identifies
     * a multi selection event, the selection is updated from the
     * anchor point. Otherwise the row is selected, and if the event
     * specified a toggle event the row is expanded/collapsed.
     *
     * @param path the selected path
     * @param event the mouse event
     */
    protected void selectPathForEvent(TreePath path, MouseEvent event) {
        /* Adjust from the anchor point. */
        if(isMultiSelectEvent(event)) {
            TreePath    anchor = getAnchorSelectionPath();
            int         anchorRow = (anchor == null) ? -1 :
                                    getRowForPath(tree, anchor);

            if(anchorRow == -1 || tree.getSelectionModel().
                      getSelectionMode() == TreeSelectionModel.
                      SINGLE_TREE_SELECTION) {
                tree.setSelectionPath(path);
            }
            else {
                int          row = getRowForPath(tree, path);
                TreePath     lastAnchorPath = anchor;

                if (isToggleSelectionEvent(event)) {
                    if (tree.isRowSelected(anchorRow)) {
                        tree.addSelectionInterval(anchorRow, row);
                    } else {
                        tree.removeSelectionInterval(anchorRow, row);
                        tree.addSelectionInterval(row, row);
                    }
                } else if(row < anchorRow) {
                    tree.setSelectionInterval(row, anchorRow);
                } else {
                    tree.setSelectionInterval(anchorRow, row);
                }
                lastSelectedRow = row;
                setAnchorSelectionPath(lastAnchorPath);
                setLeadSelectionPath(path);
            }
        }

        // Should this event toggle the selection of this row?
        /* Control toggles just this node. */
        else if(isToggleSelectionEvent(event)) {
            if(tree.isPathSelected(path))
                tree.removeSelectionPath(path);
            else
                tree.addSelectionPath(path);
            lastSelectedRow = getRowForPath(tree, path);
            setAnchorSelectionPath(path);
            setLeadSelectionPath(path);
        }

        /* Otherwise set the selection to just this interval. */
        else if(SwingUtilities.isLeftMouseButton(event)) {
            tree.setSelectionPath(path);
            if(isToggleEvent(event)) {
                toggleExpandState(path);
            }
        }
    }

    /**
     * Returns {@code true} if the node at {@code row} is a leaf.
     *
     * @param row a row
     * @return {@code true} if the node at {@code row} is a leaf
     */
    protected boolean isLeaf(int row) {
        TreePath          path = getPathForRow(tree, row);

        if(path != null)
            return treeModel.isLeaf(path.getLastPathComponent());
        // Have to return something here...
        return true;
    }

    //
    // The following selection methods (lead/anchor) are covers for the
    // methods in JTree.
    //
    private void setAnchorSelectionPath(TreePath newPath) {
        ignoreLAChange = true;
        try {
            tree.setAnchorSelectionPath(newPath);
        } finally{
            ignoreLAChange = false;
        }
    }

    private TreePath getAnchorSelectionPath() {
        return tree.getAnchorSelectionPath();
    }

    private void setLeadSelectionPath(TreePath newPath) {
        setLeadSelectionPath(newPath, false);
    }

    private void setLeadSelectionPath(TreePath newPath, boolean repaint) {
        Rectangle       bounds = repaint ?
                            getPathBounds(tree, getLeadSelectionPath()) : null;

        ignoreLAChange = true;
        try {
            tree.setLeadSelectionPath(newPath);
        } finally {
            ignoreLAChange = false;
        }
        leadRow = getRowForPath(tree, newPath);

        if (repaint) {
            if (bounds != null) {
                tree.repaint(getRepaintPathBounds(bounds));
            }
            bounds = getPathBounds(tree, newPath);
            if (bounds != null) {
                tree.repaint(getRepaintPathBounds(bounds));
            }
        }
    }

    private Rectangle getRepaintPathBounds(Rectangle bounds) {
        if (UIManager.getBoolean("Tree.repaintWholeRow")) {
           bounds.x = 0;
           bounds.width = tree.getWidth();
        }
        return bounds;
    }

    private TreePath getLeadSelectionPath() {
        return tree.getLeadSelectionPath();
    }

    /**
     * Updates the lead row of the selection.
     * @since 1.7
     */
    protected void updateLeadSelectionRow() {
        leadRow = getRowForPath(tree, getLeadSelectionPath());
    }

    /**
     * Returns the lead row of the selection.
     *
     * @return selection lead row
     * @since 1.7
     */
    protected int getLeadSelectionRow() {
        return leadRow;
    }

    /**
     * Extends the selection from the anchor to make <code>newLead</code>
     * the lead of the selection. This does not scroll.
     */
    private void extendSelection(TreePath newLead) {
        TreePath           aPath = getAnchorSelectionPath();
        int                aRow = (aPath == null) ? -1 :
                                  getRowForPath(tree, aPath);
        int                newIndex = getRowForPath(tree, newLead);

        if(aRow == -1) {
            tree.setSelectionRow(newIndex);
        }
        else {
            if(aRow < newIndex) {
                tree.setSelectionInterval(aRow, newIndex);
            }
            else {
                tree.setSelectionInterval(newIndex, aRow);
            }
            setAnchorSelectionPath(aPath);
            setLeadSelectionPath(newLead);
        }
    }

    /**
     * Invokes <code>repaint</code> on the JTree for the passed in TreePath,
     * <code>path</code>.
     */
    private void repaintPath(TreePath path) {
        if (path != null) {
            Rectangle bounds = getPathBounds(tree, path);
            if (bounds != null) {
                tree.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }
    }

    /**
     * Updates the TreeState in response to nodes expanding/collapsing.
     */
    public class TreeExpansionHandler implements TreeExpansionListener {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code TreeExpansionHandler}.
         */
        public TreeExpansionHandler() {}

        /**
         * Called whenever an item in the tree has been expanded.
         */
        public void treeExpanded(TreeExpansionEvent event) {
            getHandler().treeExpanded(event);
        }

        /**
         * Called whenever an item in the tree has been collapsed.
         */
        public void treeCollapsed(TreeExpansionEvent event) {
            getHandler().treeCollapsed(event);
        }
    } // BasicTreeUI.TreeExpansionHandler


    /**
     * Updates the preferred size when scrolling (if necessary).
     */
    public class ComponentHandler extends ComponentAdapter implements
                 ActionListener {
        /** Timer used when inside a scrollpane and the scrollbar is
         * adjusting. */
        protected Timer                timer;
        /** ScrollBar that is being adjusted. */
        protected JScrollBar           scrollBar;

        /**
         * Constructs a {@code ComponentHandler}.
         */
        public ComponentHandler() {}

        public void componentMoved(ComponentEvent e) {
            if(timer == null) {
                JScrollPane   scrollPane = getScrollPane();

                if(scrollPane == null)
                    updateSize();
                else {
                    scrollBar = scrollPane.getVerticalScrollBar();
                    if(scrollBar == null ||
                        !scrollBar.getValueIsAdjusting()) {
                        // Try the horizontal scrollbar.
                        if((scrollBar = scrollPane.getHorizontalScrollBar())
                            != null && scrollBar.getValueIsAdjusting())
                            startTimer();
                        else
                            updateSize();
                    }
                    else
                        startTimer();
                }
            }
        }

        /**
         * Creates, if necessary, and starts a Timer to check if need to
         * resize the bounds.
         */
        protected void startTimer() {
            if(timer == null) {
                timer = new Timer(200, this);
                timer.setRepeats(true);
            }
            timer.start();
        }

        /**
         * Returns the {@code JScrollPane} housing the {@code JTree},
         * or null if one isn't found.
         *
         * @return the {@code JScrollPane} housing the {@code JTree}
         */
        protected JScrollPane getScrollPane() {
            Component       c = tree.getParent();

            while(c != null && !(c instanceof JScrollPane))
                c = c.getParent();
            if(c instanceof JScrollPane)
                return (JScrollPane)c;
            return null;
        }

        /**
         * Public as a result of Timer. If the scrollBar is null, or
         * not adjusting, this stops the timer and updates the sizing.
         */
        public void actionPerformed(ActionEvent ae) {
            if(scrollBar == null || !scrollBar.getValueIsAdjusting()) {
                if(timer != null)
                    timer.stop();
                updateSize();
                timer = null;
                scrollBar = null;
            }
        }
    } // End of BasicTreeUI.ComponentHandler


    /**
     * Forwards all TreeModel events to the TreeState.
     */
    public class TreeModelHandler implements TreeModelListener {

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code TreeModelHandler}.
         */
        public TreeModelHandler() {}

        public void treeNodesChanged(TreeModelEvent e) {
            getHandler().treeNodesChanged(e);
        }

        public void treeNodesInserted(TreeModelEvent e) {
            getHandler().treeNodesInserted(e);
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            getHandler().treeNodesRemoved(e);
        }

        public void treeStructureChanged(TreeModelEvent e) {
            getHandler().treeStructureChanged(e);
        }
    } // End of BasicTreeUI.TreeModelHandler


    /**
     * Listens for changes in the selection model and updates the display
     * accordingly.
     */
    public class TreeSelectionHandler implements TreeSelectionListener {

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code TreeSelectionHandler}.
         */
        public TreeSelectionHandler() {}

        /**
         * Messaged when the selection changes in the tree we're displaying
         * for.  Stops editing, messages super and displays the changed paths.
         */
        public void valueChanged(TreeSelectionEvent event) {
            getHandler().valueChanged(event);
        }
    }// End of BasicTreeUI.TreeSelectionHandler


    /**
     * Listener responsible for getting cell editing events and updating
     * the tree accordingly.
     */
    public class CellEditorHandler implements CellEditorListener {

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code CellEditorHandler}.
         */
        public CellEditorHandler() {}

        /** Messaged when editing has stopped in the tree. */
        public void editingStopped(ChangeEvent e) {
            getHandler().editingStopped(e);
        }

        /** Messaged when editing has been canceled in the tree. */
        public void editingCanceled(ChangeEvent e) {
            getHandler().editingCanceled(e);
        }
    } // BasicTreeUI.CellEditorHandler


    /**
     * This is used to get multiple key down events to appropriately generate
     * events.
     */
    public class KeyHandler extends KeyAdapter {

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        // Also note these fields aren't use anymore, nor does Handler have
        // the old functionality. This behavior worked around an old bug
        // in JComponent that has long since been fixed.

        /** Key code that is being generated for. */
        protected Action              repeatKeyAction;

        /** Set to true while keyPressed is active. */
        protected boolean            isKeyDown;

        /**
         * Constructs a {@code KeyHandler}.
         */
        public KeyHandler() {}

        /**
         * Invoked when a key has been typed.
         *
         * Moves the keyboard focus to the first element
         * whose first letter matches the alphanumeric key
         * pressed by the user. Subsequent same key presses
         * move the keyboard focus to the next object that
         * starts with the same letter.
         */
        public void keyTyped(KeyEvent e) {
            getHandler().keyTyped(e);
        }

        public void keyPressed(KeyEvent e) {
            getHandler().keyPressed(e);
        }

        public void keyReleased(KeyEvent e) {
            getHandler().keyReleased(e);
        }
    } // End of BasicTreeUI.KeyHandler


    /**
     * Repaints the lead selection row when focus is lost/gained.
     */
    public class FocusHandler implements FocusListener {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code FocusHandler}.
         */
        public FocusHandler() {}

        /**
         * Invoked when focus is activated on the tree we're in, redraws the
         * lead row.
         */
        public void focusGained(FocusEvent e) {
            getHandler().focusGained(e);
        }

        /**
         * Invoked when focus is activated on the tree we're in, redraws the
         * lead row.
         */
        public void focusLost(FocusEvent e) {
            getHandler().focusLost(e);
        }
    } // End of class BasicTreeUI.FocusHandler


    /**
     * Class responsible for getting size of node, method is forwarded
     * to BasicTreeUI method. X location does not include insets, that is
     * handled in getPathBounds.
     */
    // This returns locations that don't include any Insets.
    public class NodeDimensionsHandler extends
                 AbstractLayoutCache.NodeDimensions {
        /**
         * Constructs a {@code NodeDimensionsHandler}.
         */
        public NodeDimensionsHandler() {}

        /**
         * Responsible for getting the size of a particular node.
         */
        public Rectangle getNodeDimensions(Object value, int row,
                                           int depth, boolean expanded,
                                           Rectangle size) {
            // Return size of editing component, if editing and asking
            // for editing row.
            if(editingComponent != null && editingRow == row) {
                Dimension        prefSize = editingComponent.
                                              getPreferredSize();
                int              rh = getRowHeight();

                if(rh > 0 && rh != prefSize.height)
                    prefSize.height = rh;
                if(size != null) {
                    size.x = getRowX(row, depth);
                    size.width = prefSize.width;
                    size.height = prefSize.height;
                }
                else {
                    size = new Rectangle(getRowX(row, depth), 0,
                                         prefSize.width, prefSize.height);
                }
                return size;
            }
            // Not editing, use renderer.
            if(currentCellRenderer != null) {
                Component          aComponent;

                aComponent = currentCellRenderer.getTreeCellRendererComponent
                    (tree, value, tree.isRowSelected(row),
                     expanded, treeModel.isLeaf(value), row,
                     false);
                if(tree != null) {
                    // Only ever removed when UI changes, this is OK!
                    rendererPane.add(aComponent);
                    aComponent.validate();
                }
                Dimension        prefSize = aComponent.getPreferredSize();

                if(size != null) {
                    size.x = getRowX(row, depth);
                    size.width = prefSize.width;
                    size.height = prefSize.height;
                }
                else {
                    size = new Rectangle(getRowX(row, depth), 0,
                                         prefSize.width, prefSize.height);
                }
                return size;
            }
            return null;
        }

        /**
         * Returns amount to indent the given row.
         *
         * @param row a row
         * @param depth a depth
         * @return amount to indent the given row
         */
        protected int getRowX(int row, int depth) {
            return BasicTreeUI.this.getRowX(row, depth);
        }

    } // End of class BasicTreeUI.NodeDimensionsHandler


    /**
     * TreeMouseListener is responsible for updating the selection
     * based on mouse events.
     */
    public class MouseHandler extends MouseAdapter implements MouseMotionListener
 {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code MouseHandler}.
         */
        public MouseHandler() {}

        /**
         * Invoked when a mouse button has been pressed on a component.
         */
        public void mousePressed(MouseEvent e) {
            getHandler().mousePressed(e);
        }

        public void mouseDragged(MouseEvent e) {
            getHandler().mouseDragged(e);
        }

        /**
         * Invoked when the mouse button has been moved on a component
         * (with no buttons no down).
         * @since 1.4
         */
        public void mouseMoved(MouseEvent e) {
            getHandler().mouseMoved(e);
        }

        public void mouseReleased(MouseEvent e) {
            getHandler().mouseReleased(e);
        }
    } // End of BasicTreeUI.MouseHandler


    /**
     * PropertyChangeListener for the tree. Updates the appropriate
     * variable, or TreeState, based on what changes.
     */
    public class PropertyChangeHandler implements
                       PropertyChangeListener {

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code PropertyChangeHandler}.
         */
        public PropertyChangeHandler() {}

        public void propertyChange(PropertyChangeEvent event) {
            getHandler().propertyChange(event);
        }
    } // End of BasicTreeUI.PropertyChangeHandler


    /**
     * Listener on the TreeSelectionModel, resets the row selection if
     * any of the properties of the model change.
     */
    public class SelectionModelPropertyChangeHandler implements
                      PropertyChangeListener {

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code SelectionModelPropertyChangeHandler}.
         */
        public SelectionModelPropertyChangeHandler() {}

        public void propertyChange(PropertyChangeEvent event) {
            getHandler().propertyChange(event);
        }
    } // End of BasicTreeUI.SelectionModelPropertyChangeHandler


    /**
     * <code>TreeTraverseAction</code> is the action used for left/right keys.
     * Will toggle the expandedness of a node, as well as potentially
     * incrementing the selection.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public class TreeTraverseAction extends AbstractAction {
        /** Determines direction to traverse, 1 means expand, -1 means
          * collapse. */
        protected int direction;
        /** True if the selection is reset, false means only the lead path
         * changes. */
        private boolean changeSelection;

        /**
         * Constructs a new instance of {@code TreeTraverseAction}.
         *
         * @param direction the direction
         * @param name the name of action
         */
        public TreeTraverseAction(int direction, String name) {
            this(direction, name, true);
        }

        private TreeTraverseAction(int direction, String name,
                                   boolean changeSelection) {
            this.direction = direction;
            this.changeSelection = changeSelection;
        }

        public void actionPerformed(ActionEvent e) {
            if (tree != null) {
                SHARED_ACTION.traverse(tree, BasicTreeUI.this, direction,
                                       changeSelection);
            }
        }

        public boolean isEnabled() { return (tree != null &&
                                             tree.isEnabled()); }
    } // BasicTreeUI.TreeTraverseAction


    /** TreePageAction handles page up and page down events.
      */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public class TreePageAction extends AbstractAction {
        /** Specifies the direction to adjust the selection by. */
        protected int         direction;
        /** True indicates should set selection from anchor path. */
        private boolean       addToSelection;
        private boolean       changeSelection;

        /**
         * Constructs a new instance of {@code TreePageAction}.
         *
         * @param direction the direction
         * @param name the name of action
         */
        public TreePageAction(int direction, String name) {
            this(direction, name, false, true);
        }

        private TreePageAction(int direction, String name,
                               boolean addToSelection,
                               boolean changeSelection) {
            this.direction = direction;
            this.addToSelection = addToSelection;
            this.changeSelection = changeSelection;
        }

        public void actionPerformed(ActionEvent e) {
            if (tree != null) {
                SHARED_ACTION.page(tree, BasicTreeUI.this, direction,
                                   addToSelection, changeSelection);
            }
        }

        public boolean isEnabled() { return (tree != null &&
                                             tree.isEnabled()); }

    } // BasicTreeUI.TreePageAction


    /** TreeIncrementAction is used to handle up/down actions.  Selection
      * is moved up or down based on direction.
      */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public class TreeIncrementAction extends AbstractAction  {
        /** Specifies the direction to adjust the selection by. */
        protected int         direction;
        /** If true the new item is added to the selection, if false the
         * selection is reset. */
        private boolean       addToSelection;
        private boolean       changeSelection;

        /**
         * Constructs a new instance of {@code TreeIncrementAction}.
         *
         * @param direction the direction
         * @param name the name of action
         */
        public TreeIncrementAction(int direction, String name) {
            this(direction, name, false, true);
        }

        private TreeIncrementAction(int direction, String name,
                                   boolean addToSelection,
                                    boolean changeSelection) {
            this.direction = direction;
            this.addToSelection = addToSelection;
            this.changeSelection = changeSelection;
        }

        public void actionPerformed(ActionEvent e) {
            if (tree != null) {
                SHARED_ACTION.increment(tree, BasicTreeUI.this, direction,
                                        addToSelection, changeSelection);
            }
        }

        public boolean isEnabled() { return (tree != null &&
                                             tree.isEnabled()); }

    } // End of class BasicTreeUI.TreeIncrementAction

    /**
      * TreeHomeAction is used to handle end/home actions.
      * Scrolls either the first or last cell to be visible based on
      * direction.
      */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public class TreeHomeAction extends AbstractAction {
        /**
         * The direction.
         */
        protected int            direction;
        /** Set to true if append to selection. */
        private boolean          addToSelection;
        private boolean          changeSelection;

        /**
         * Constructs a new instance of {@code TreeHomeAction}.
         *
         * @param direction the direction
         * @param name the name of action
         */
        public TreeHomeAction(int direction, String name) {
            this(direction, name, false, true);
        }

        private TreeHomeAction(int direction, String name,
                               boolean addToSelection,
                               boolean changeSelection) {
            this.direction = direction;
            this.changeSelection = changeSelection;
            this.addToSelection = addToSelection;
        }

        public void actionPerformed(ActionEvent e) {
            if (tree != null) {
                SHARED_ACTION.home(tree, BasicTreeUI.this, direction,
                                   addToSelection, changeSelection);
            }
        }

        public boolean isEnabled() { return (tree != null &&
                                             tree.isEnabled()); }

    } // End of class BasicTreeUI.TreeHomeAction


    /**
      * For the first selected row expandedness will be toggled.
      */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public class TreeToggleAction extends AbstractAction {
        /**
         * Constructs a new instance of {@code TreeToggleAction}.
         *
         * @param name the name of action
         */
        public TreeToggleAction(String name) {
        }

        public void actionPerformed(ActionEvent e) {
            if(tree != null) {
                SHARED_ACTION.toggle(tree, BasicTreeUI.this);
            }
        }

        public boolean isEnabled() { return (tree != null &&
                                             tree.isEnabled()); }

    } // End of class BasicTreeUI.TreeToggleAction


    /**
     * ActionListener that invokes cancelEditing when action performed.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public class TreeCancelEditingAction extends AbstractAction {
        /**
         * Constructs a new instance of {@code TreeCancelEditingAction}.
         *
         * @param name the name of action
         */
        public TreeCancelEditingAction(String name) {
        }

        public void actionPerformed(ActionEvent e) {
            if(tree != null) {
                SHARED_ACTION.cancelEditing(tree, BasicTreeUI.this);
            }
        }

        public boolean isEnabled() { return (tree != null &&
                                             tree.isEnabled() &&
                                             isEditing(tree)); }
    } // End of class BasicTreeUI.TreeCancelEditingAction


    /**
      * MouseInputHandler handles passing all mouse events,
      * including mouse motion events, until the mouse is released to
      * the destination it is constructed with. It is assumed all the
      * events are currently target at source.
      */
    public class MouseInputHandler implements
                     MouseInputListener
    {
        /** Source that events are coming from. */
        protected Component        source;
        /** Destination that receives all events. */
        protected Component        destination;
        private Component          focusComponent;
        private boolean            dispatchedEvent;

        /**
         * Constructs a new instance of {@code MouseInputHandler}.
         *
         * @param source a source component
         * @param destination a destination component
         * @param event a mouse event
         */
        public MouseInputHandler(Component source, Component destination,
                                      MouseEvent event){
            this(source, destination, event, null);
        }

        MouseInputHandler(Component source, Component destination,
                          MouseEvent event, Component focusComponent) {
            this.source = source;
            this.destination = destination;
            this.source.addMouseListener(this);
            this.source.addMouseMotionListener(this);

            SwingUtilities2.setSkipClickCount(destination,
                                              event.getClickCount() - 1);

            /* Dispatch the editing event! */
            destination.dispatchEvent(SwingUtilities.convertMouseEvent
                                          (source, event, destination));
            this.focusComponent = focusComponent;
        }

        public void mouseClicked(MouseEvent e) {
            if(destination != null) {
                dispatchedEvent = true;
                destination.dispatchEvent(SwingUtilities.convertMouseEvent
                                          (source, e, destination));
            }
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            if(destination != null)
                destination.dispatchEvent(SwingUtilities.convertMouseEvent
                                          (source, e, destination));
            removeFromSource();
        }

        public void mouseEntered(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                removeFromSource();
            }
        }

        public void mouseExited(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                removeFromSource();
            }
        }

        public void mouseDragged(MouseEvent e) {
            if(destination != null) {
                dispatchedEvent = true;
                destination.dispatchEvent(SwingUtilities.convertMouseEvent
                                          (source, e, destination));
            }
        }

        public void mouseMoved(MouseEvent e) {
            removeFromSource();
        }

        /**
         * Removes an event from the source.
         */
        protected void removeFromSource() {
            if(source != null) {
                source.removeMouseListener(this);
                source.removeMouseMotionListener(this);
                if (focusComponent != null &&
                      focusComponent == destination && !dispatchedEvent &&
                      (focusComponent instanceof JTextField)) {
                    ((JTextField)focusComponent).selectAll();
                }
            }
            source = destination = null;
        }

    } // End of class BasicTreeUI.MouseInputHandler

    private static final TransferHandler defaultTransferHandler = new TreeTransferHandler();

    @SuppressWarnings("serial") // JDK-implementation class
    static class TreeTransferHandler extends TransferHandler implements UIResource, Comparator<TreePath> {

        private JTree tree;

        /**
         * Create a Transferable to use as the source for a data transfer.
         *
         * @param c  The component holding the data to be transfered.  This
         *  argument is provided to enable sharing of TransferHandlers by
         *  multiple components.
         * @return  The representation of the data to be transfered.
         *
         */
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof JTree) {
                tree = (JTree) c;
                TreePath[] paths = tree.getSelectionPaths();

                if (paths == null || paths.length == 0) {
                    return null;
                }

                StringBuilder plainStr = new StringBuilder();
                StringBuilder htmlStr = new StringBuilder();

                htmlStr.append("<html>\n<body>\n<ul>\n");

                TreeModel model = tree.getModel();
                TreePath lastPath = null;
                TreePath[] displayPaths = getDisplayOrderPaths(paths);

                for (TreePath path : displayPaths) {
                    Object node = path.getLastPathComponent();
                    boolean leaf = model.isLeaf(node);
                    String label = getDisplayString(path, true, leaf);

                    plainStr.append(label).append('\n');
                    htmlStr.append("  <li>").append(label).append('\n');
                }

                // remove the last newline
                plainStr.deleteCharAt(plainStr.length() - 1);
                htmlStr.append("</ul>\n</body>\n</html>");

                tree = null;

                return new BasicTransferable(plainStr.toString(), htmlStr.toString());
            }

            return null;
        }

        public int compare(TreePath o1, TreePath o2) {
            int row1 = tree.getRowForPath(o1);
            int row2 = tree.getRowForPath(o2);
            return row1 - row2;
        }

        String getDisplayString(TreePath path, boolean selected, boolean leaf) {
            int row = tree.getRowForPath(path);
            boolean hasFocus = tree.getLeadSelectionRow() == row;
            Object node = path.getLastPathComponent();
            return tree.convertValueToText(node, selected, tree.isExpanded(row),
                                           leaf, row, hasFocus);
        }

        /**
         * Selection paths are in selection order.  The conversion to
         * HTML requires display order.  This method resorts the paths
         * to be in the display order.
         */
        TreePath[] getDisplayOrderPaths(TreePath[] paths) {
            // sort the paths to display order rather than selection order
            ArrayList<TreePath> selOrder = new ArrayList<TreePath>();
            for (TreePath path : paths) {
                selOrder.add(path);
            }
            Collections.sort(selOrder, this);
            int n = selOrder.size();
            TreePath[] displayPaths = new TreePath[n];
            for (int i = 0; i < n; i++) {
                displayPaths[i] = selOrder.get(i);
            }
            return displayPaths;
        }

        public int getSourceActions(JComponent c) {
            return COPY;
        }

    }


    private class Handler implements CellEditorListener, FocusListener,
                  KeyListener, MouseListener, MouseMotionListener,
                  PropertyChangeListener, TreeExpansionListener,
                  TreeModelListener, TreeSelectionListener,
                  BeforeDrag {
        //
        // KeyListener
        //
        private String prefix = "";
        private String typedString = "";
        private long lastTime = 0L;

        /**
         * Invoked when a key has been typed.
         *
         * Moves the keyboard focus to the first element whose prefix matches the
         * sequence of alphanumeric keys pressed by the user with delay less
         * than value of <code>timeFactor</code> property (or 1000 milliseconds
         * if it is not defined). Subsequent same key presses move the keyboard
         * focus to the next object that starts with the same letter until another
         * key is pressed, then it is treated as the prefix with appropriate number
         * of the same letters followed by first typed another letter.
         */
        public void keyTyped(KeyEvent e) {
            // handle first letter navigation
            if(tree != null && tree.getRowCount()>0 && tree.hasFocus() &&
               tree.isEnabled()) {
                if (e.isAltDown() || BasicGraphicsUtils.isMenuShortcutKeyDown(e) ||
                    isNavigationKey(e)) {
                    return;
                }
                boolean startingFromSelection = true;

                char c = e.getKeyChar();

                long time = e.getWhen();
                int startingRow = tree.getLeadSelectionRow();
                if (time - lastTime < timeFactor) {
                    typedString += c;
                    if((prefix.length() == 1) && (c == prefix.charAt(0))) {
                        // Subsequent same key presses move the keyboard focus to the next
                        // object that starts with the same letter.
                        startingRow++;
                    } else {
                        prefix = typedString;
                    }
                } else {
                    startingRow++;
                    typedString = "" + c;
                    prefix = typedString;
                }
                lastTime = time;

                if (startingRow < 0 || startingRow >= tree.getRowCount()) {
                    startingFromSelection = false;
                    startingRow = 0;
                }
                TreePath path = tree.getNextMatch(prefix, startingRow,
                                                  Position.Bias.Forward);
                if (path != null) {
                    tree.setSelectionPath(path);
                    int row = getRowForPath(tree, path);
                    ensureRowsAreVisible(row, row);
                } else if (startingFromSelection) {
                    path = tree.getNextMatch(prefix, 0,
                                             Position.Bias.Forward);
                    if (path != null) {
                        tree.setSelectionPath(path);
                        int row = getRowForPath(tree, path);
                        ensureRowsAreVisible(row, row);
                    }
                }
            }
        }

        /**
         * Invoked when a key has been pressed.
         *
         * Checks to see if the key event is a navigation key to prevent
         * dispatching these keys for the first letter navigation.
         */
        public void keyPressed(KeyEvent e) {
            if (tree != null && isNavigationKey(e)) {
                prefix = "";
                typedString = "";
                lastTime = 0L;
            }
        }

        public void keyReleased(KeyEvent e) {
        }

        /**
         * Returns whether or not the supplied key event maps to a key that is used for
         * navigation.  This is used for optimizing key input by only passing non-
         * navigation keys to the first letter navigation mechanism.
         */
        private boolean isNavigationKey(KeyEvent event) {
            InputMap inputMap = tree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            KeyStroke key = KeyStroke.getKeyStrokeForEvent(event);

            return inputMap != null && inputMap.get(key) != null;
        }


        //
        // PropertyChangeListener
        //
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getSource() == treeSelectionModel) {
                treeSelectionModel.resetRowSelection();
            }
            else if(event.getSource() == tree) {
                String              changeName = event.getPropertyName();

                if (changeName == JTree.LEAD_SELECTION_PATH_PROPERTY) {
                    if (!ignoreLAChange) {
                        updateLeadSelectionRow();
                        repaintPath((TreePath)event.getOldValue());
                        repaintPath((TreePath)event.getNewValue());
                    }
                }
                else if (changeName == JTree.ANCHOR_SELECTION_PATH_PROPERTY) {
                    if (!ignoreLAChange) {
                        repaintPath((TreePath)event.getOldValue());
                        repaintPath((TreePath)event.getNewValue());
                    }
                }
                if(changeName == JTree.CELL_RENDERER_PROPERTY) {
                    setCellRenderer((TreeCellRenderer)event.getNewValue());
                    redoTheLayout();
                }
                else if(changeName == JTree.TREE_MODEL_PROPERTY) {
                    setModel((TreeModel)event.getNewValue());
                }
                else if(changeName == JTree.ROOT_VISIBLE_PROPERTY) {
                    setRootVisible(((Boolean)event.getNewValue()).
                                   booleanValue());
                }
                else if(changeName == JTree.SHOWS_ROOT_HANDLES_PROPERTY) {
                    setShowsRootHandles(((Boolean)event.getNewValue()).
                                        booleanValue());
                }
                else if(changeName == JTree.ROW_HEIGHT_PROPERTY) {
                    setRowHeight(((Integer)event.getNewValue()).
                                 intValue());
                }
                else if(changeName == JTree.CELL_EDITOR_PROPERTY) {
                    setCellEditor((TreeCellEditor)event.getNewValue());
                }
                else if(changeName == JTree.EDITABLE_PROPERTY) {
                    setEditable(((Boolean)event.getNewValue()).booleanValue());
                }
                else if(changeName == JTree.LARGE_MODEL_PROPERTY) {
                    setLargeModel(tree.isLargeModel());
                }
                else if(changeName == JTree.SELECTION_MODEL_PROPERTY) {
                    setSelectionModel(tree.getSelectionModel());
                }
                else if(changeName == "font"
                        || SwingUtilities2.isScaleChanged(event)) {
                    completeEditing();
                    if(treeState != null)
                        treeState.invalidateSizes();
                    updateSize();
                }
                else if (changeName == "componentOrientation") {
                    if (tree != null) {
                        leftToRight = BasicGraphicsUtils.isLeftToRight(tree);
                        redoTheLayout();
                        tree.treeDidChange();

                        InputMap km = getInputMap(JComponent.WHEN_FOCUSED);
                        SwingUtilities.replaceUIInputMap(tree,
                                                JComponent.WHEN_FOCUSED, km);
                    }
                } else if ("dropLocation" == changeName) {
                    JTree.DropLocation oldValue = (JTree.DropLocation)event.getOldValue();
                    repaintDropLocation(oldValue);
                    repaintDropLocation(tree.getDropLocation());
                }
            }
        }

        private void repaintDropLocation(JTree.DropLocation loc) {
            if (loc == null) {
                return;
            }

            Rectangle r;

            if (isDropLine(loc)) {
                r = getDropLineRect(loc);
            } else {
                r = tree.getPathBounds(loc.getPath());
            }

            if (r != null) {
                tree.repaint(r);
            }
        }

        //
        // MouseListener
        //

        // Whether or not the mouse press (which is being considered as part
        // of a drag sequence) also caused the selection change to be fully
        // processed.
        private boolean dragPressDidSelection;

        // Set to true when a drag gesture has been fully recognized and DnD
        // begins. Use this to ignore further mouse events which could be
        // delivered if DnD is cancelled (via ESCAPE for example)
        private boolean dragStarted;

        // The path over which the press occurred and the press event itself
        private TreePath pressedPath;
        private MouseEvent pressedEvent;

        // Used to detect whether the press event causes a selection change.
        // If it does, we won't try to start editing on the release.
        private boolean valueChangedOnPress;

        private boolean isActualPath(TreePath path, int x, int y) {
            if (path == null) {
                return false;
            }

            Rectangle bounds = getPathBounds(tree, path);
            if (bounds == null || y > (bounds.y + bounds.height)) {
                return false;
            }

            return (x >= bounds.x) && (x <= (bounds.x + bounds.width));
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        /**
         * Invoked when a mouse button has been pressed on a component.
         */
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities2.shouldIgnore(e, tree)) {
                return;
            }

            // if we can't stop any ongoing editing, do nothing
            if (isEditing(tree) && tree.getInvokesStopCellEditing()
                                && !stopEditing(tree)) {
                return;
            }

            completeEditing();

            pressedPath = getClosestPathForLocation(tree, e.getX(), e.getY());

            if (tree.getDragEnabled()) {
                mousePressedDND(e);
            } else {
                SwingUtilities2.adjustFocus(tree);
                handleSelection(e);
            }
        }

        private void mousePressedDND(MouseEvent e) {
            pressedEvent = e;
            boolean grabFocus = true;
            dragStarted = false;
            valueChangedOnPress = false;

            // if we have a valid path and this is a drag initiating event
            if (isActualPath(pressedPath, e.getX(), e.getY()) &&
                    DragRecognitionSupport.mousePressed(e)) {

                dragPressDidSelection = false;

                if (BasicGraphicsUtils.isMenuShortcutKeyDown(e)) {
                    // do nothing for control - will be handled on release
                    // or when drag starts
                    return;
                } else if (!e.isShiftDown() && tree.isPathSelected(pressedPath)) {
                    // clicking on something that's already selected
                    // and need to make it the lead now
                    setAnchorSelectionPath(pressedPath);
                    setLeadSelectionPath(pressedPath, true);
                    return;
                }

                dragPressDidSelection = true;

                // could be a drag initiating event - don't grab focus
                grabFocus = false;
            }

            if (grabFocus) {
                SwingUtilities2.adjustFocus(tree);
            }

            handleSelection(e);
        }

        void handleSelection(MouseEvent e) {
            if(pressedPath != null) {
                Rectangle bounds = getPathBounds(tree, pressedPath);

                if (bounds == null || e.getY() >= (bounds.y + bounds.height)) {
                    return;
                }

                // Preferably checkForClickInExpandControl could take
                // the Event to do this it self!
                if(SwingUtilities.isLeftMouseButton(e)) {
                    checkForClickInExpandControl(pressedPath, e.getX(), e.getY());
                }

                int x = e.getX();

                // Perhaps they clicked the cell itself. If so,
                // select it.
                if (x >= bounds.x && x < (bounds.x + bounds.width)) {
                    if (tree.getDragEnabled() || !startEditing(pressedPath, e)) {
                        selectPathForEvent(pressedPath, e);
                    }
                }
            }
        }

        public void dragStarting(MouseEvent me) {
            dragStarted = true;

            if (BasicGraphicsUtils.isMenuShortcutKeyDown(me)) {
                tree.addSelectionPath(pressedPath);
                setAnchorSelectionPath(pressedPath);
                setLeadSelectionPath(pressedPath, true);
            }

            pressedEvent = null;
            pressedPath = null;
        }

        public void mouseDragged(MouseEvent e) {
            if (SwingUtilities2.shouldIgnore(e, tree)) {
                return;
            }

            if (tree.getDragEnabled()) {
                DragRecognitionSupport.mouseDragged(e, this);
            }
        }

        /**
         * Invoked when the mouse button has been moved on a component
         * (with no buttons no down).
         */
        public void mouseMoved(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities2.shouldIgnore(e, tree)) {
                return;
            }

            if (tree.getDragEnabled()) {
                mouseReleasedDND(e);
            }

            pressedEvent = null;
            pressedPath = null;
        }

        private void mouseReleasedDND(MouseEvent e) {
            MouseEvent me = DragRecognitionSupport.mouseReleased(e);
            if (me != null) {
                SwingUtilities2.adjustFocus(tree);
                if (!dragPressDidSelection) {
                    handleSelection(me);
                }
            }

            if (!dragStarted) {

                // Note: We don't give the tree a chance to start editing if the
                // mouse press caused a selection change. Otherwise the default
                // tree cell editor will start editing on EVERY press and
                // release. If it turns out that this affects some editors, we
                // can always parameterize this with a client property. ex:
                //
                // if (pressedPath != null &&
                //         (Boolean.TRUE == tree.getClientProperty("Tree.DnD.canEditOnValueChange") ||
                //          !valueChangedOnPress) && ...
                if (pressedPath != null && !valueChangedOnPress &&
                        isActualPath(pressedPath, pressedEvent.getX(), pressedEvent.getY())) {

                    startEditingOnRelease(pressedPath, pressedEvent, e);
                }
            }
        }

        //
        // FocusListener
        //
        public void focusGained(FocusEvent e) {
            if(tree != null) {
                Rectangle                 pBounds;

                pBounds = getPathBounds(tree, tree.getLeadSelectionPath());
                if(pBounds != null)
                    tree.repaint(getRepaintPathBounds(pBounds));
                pBounds = getPathBounds(tree, getLeadSelectionPath());
                if(pBounds != null)
                    tree.repaint(getRepaintPathBounds(pBounds));
            }
        }

        public void focusLost(FocusEvent e) {
            focusGained(e);
        }

        //
        // CellEditorListener
        //
        public void editingStopped(ChangeEvent e) {
            completeEditing(false, false, true);
        }

        /** Messaged when editing has been canceled in the tree. */
        public void editingCanceled(ChangeEvent e) {
            completeEditing(false, false, false);
        }


        //
        // TreeSelectionListener
        //
        public void valueChanged(TreeSelectionEvent event) {
            valueChangedOnPress = true;

            // Stop editing
            completeEditing();
            // Make sure all the paths are visible, if necessary.
            // PENDING: This should be tweaked when isAdjusting is added
            if(tree.getExpandsSelectedPaths() && treeSelectionModel != null) {
                TreePath[]           paths = treeSelectionModel
                                         .getSelectionPaths();

                if(paths != null) {
                    for(int counter = paths.length - 1; counter >= 0;
                        counter--) {
                        TreePath path = paths[counter].getParentPath();
                        boolean expand = true;

                        while (path != null) {
                            // Indicates this path isn't valid anymore,
                            // we shouldn't attempt to expand it then.
                            if (treeModel.isLeaf(path.getLastPathComponent())){
                                expand = false;
                                path = null;
                            }
                            else {
                                path = path.getParentPath();
                            }
                        }
                        if (expand) {
                            tree.makeVisible(paths[counter]);
                        }
                    }
                }
            }

            TreePath oldLead = getLeadSelectionPath();
            lastSelectedRow = tree.getMinSelectionRow();
            TreePath lead = tree.getSelectionModel().getLeadSelectionPath();
            setAnchorSelectionPath(lead);
            setLeadSelectionPath(lead);

            TreePath[]       changedPaths = event.getPaths();
            Rectangle        nodeBounds;
            Rectangle        visRect = tree.getVisibleRect();
            boolean          paintPaths = true;
            int              nWidth = tree.getWidth();

            if(changedPaths != null) {
                int              counter, maxCounter = changedPaths.length;

                if(maxCounter > 4) {
                    tree.repaint();
                    paintPaths = false;
                }
                else {
                    for (counter = 0; counter < maxCounter; counter++) {
                        nodeBounds = getPathBounds(tree,
                                                   changedPaths[counter]);
                        if(nodeBounds != null &&
                           visRect.intersects(nodeBounds))
                            tree.repaint(0, nodeBounds.y, nWidth,
                                         nodeBounds.height);
                    }
                }
            }
            if(paintPaths) {
                nodeBounds = getPathBounds(tree, oldLead);
                if(nodeBounds != null && visRect.intersects(nodeBounds))
                    tree.repaint(0, nodeBounds.y, nWidth, nodeBounds.height);
                nodeBounds = getPathBounds(tree, lead);
                if(nodeBounds != null && visRect.intersects(nodeBounds))
                    tree.repaint(0, nodeBounds.y, nWidth, nodeBounds.height);
            }
        }


        //
        // TreeExpansionListener
        //
        public void treeExpanded(TreeExpansionEvent event) {
            if(event != null && tree != null) {
                TreePath      path = event.getPath();

                updateExpandedDescendants(path);
            }
        }

        public void treeCollapsed(TreeExpansionEvent event) {
            if(event != null && tree != null) {
                TreePath        path = event.getPath();

                completeEditing();
                if(path != null && tree.isVisible(path)) {
                    treeState.setExpandedState(path, false);
                    updateLeadSelectionRow();
                    updateSize();
                }
            }
        }

        //
        // TreeModelListener
        //
        public void treeNodesChanged(TreeModelEvent e) {
            if(treeState != null && e != null) {
                TreePath parentPath = SwingUtilities2.getTreePath(e, getModel());
                int[] indices = e.getChildIndices();
                if (indices == null || indices.length == 0) {
                    // The root has changed
                    treeState.treeNodesChanged(e);
                    updateSize();
                }
                else if (treeState.isExpanded(parentPath)) {
                    // Changed nodes are visible
                    // Find the minimum index, we only need paint from there
                    // down.
                    int minIndex = indices[0];
                    for (int i = indices.length - 1; i > 0; i--) {
                        minIndex = Math.min(indices[i], minIndex);
                    }
                    Object minChild = treeModel.getChild(
                            parentPath.getLastPathComponent(), minIndex);
                    TreePath minPath = parentPath.pathByAddingChild(minChild);
                    Rectangle minBounds = getPathBounds(tree, minPath);

                    // Forward to the treestate
                    treeState.treeNodesChanged(e);

                    // Mark preferred size as bogus.
                    updateSize0();

                    // And repaint
                    Rectangle newMinBounds = getPathBounds(tree, minPath);
                    if (minBounds == null || newMinBounds == null) {
                        return;
                    }

                    if (indices.length == 1 &&
                            newMinBounds.height == minBounds.height) {
                        tree.repaint(0, minBounds.y, tree.getWidth(),
                                     minBounds.height);
                    }
                    else {
                        tree.repaint(0, minBounds.y, tree.getWidth(),
                                     tree.getHeight() - minBounds.y);
                    }
                }
                else {
                    // Nodes that changed aren't visible.  No need to paint
                    treeState.treeNodesChanged(e);
                }
            }
        }

        public void treeNodesInserted(TreeModelEvent e) {
            if(treeState != null && e != null) {
                treeState.treeNodesInserted(e);

                updateLeadSelectionRow();

                TreePath       path = SwingUtilities2.getTreePath(e, getModel());

                if(treeState.isExpanded(path)) {
                    updateSize();
                }
                else {
                    // PENDING(sky): Need a method in TreeModelEvent
                    // that can return the count, getChildIndices allocs
                    // a new array!
                    int[]      indices = e.getChildIndices();
                    int        childCount = treeModel.getChildCount
                                            (path.getLastPathComponent());

                    if(indices != null && (childCount - indices.length) == 0)
                        updateSize();
                }
            }
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            if(treeState != null && e != null) {
                treeState.treeNodesRemoved(e);

                updateLeadSelectionRow();

                TreePath       path = SwingUtilities2.getTreePath(e, getModel());

                if(treeState.isExpanded(path) ||
                   treeModel.getChildCount(path.getLastPathComponent()) == 0)
                    updateSize();
            }
        }

        public void treeStructureChanged(TreeModelEvent e) {
            if(treeState != null && e != null) {
                treeState.treeStructureChanged(e);

                updateLeadSelectionRow();

                TreePath       pPath = SwingUtilities2.getTreePath(e, getModel());

                if (pPath != null) {
                    pPath = pPath.getParentPath();
                }
                if(pPath == null || treeState.isExpanded(pPath))
                    updateSize();
            }
        }
    }



    private static class Actions extends UIAction {
        private static final String SELECT_PREVIOUS = "selectPrevious";
        private static final String SELECT_PREVIOUS_CHANGE_LEAD =
                             "selectPreviousChangeLead";
        private static final String SELECT_PREVIOUS_EXTEND_SELECTION =
                             "selectPreviousExtendSelection";
        private static final String SELECT_NEXT = "selectNext";
        private static final String SELECT_NEXT_CHANGE_LEAD =
                                    "selectNextChangeLead";
        private static final String SELECT_NEXT_EXTEND_SELECTION =
                                    "selectNextExtendSelection";
        private static final String SELECT_CHILD = "selectChild";
        private static final String SELECT_CHILD_CHANGE_LEAD =
                                    "selectChildChangeLead";
        private static final String SELECT_PARENT = "selectParent";
        private static final String SELECT_PARENT_CHANGE_LEAD =
                                    "selectParentChangeLead";
        private static final String SCROLL_UP_CHANGE_SELECTION =
                                    "scrollUpChangeSelection";
        private static final String SCROLL_UP_CHANGE_LEAD =
                                    "scrollUpChangeLead";
        private static final String SCROLL_UP_EXTEND_SELECTION =
                                    "scrollUpExtendSelection";
        private static final String SCROLL_DOWN_CHANGE_SELECTION =
                                    "scrollDownChangeSelection";
        private static final String SCROLL_DOWN_EXTEND_SELECTION =
                                    "scrollDownExtendSelection";
        private static final String SCROLL_DOWN_CHANGE_LEAD =
                                    "scrollDownChangeLead";
        private static final String SELECT_FIRST = "selectFirst";
        private static final String SELECT_FIRST_CHANGE_LEAD =
                                    "selectFirstChangeLead";
        private static final String SELECT_FIRST_EXTEND_SELECTION =
                                    "selectFirstExtendSelection";
        private static final String SELECT_LAST = "selectLast";
        private static final String SELECT_LAST_CHANGE_LEAD =
                                    "selectLastChangeLead";
        private static final String SELECT_LAST_EXTEND_SELECTION =
                                    "selectLastExtendSelection";
        private static final String TOGGLE = "toggle";
        private static final String CANCEL_EDITING = "cancel";
        private static final String START_EDITING = "startEditing";
        private static final String SELECT_ALL = "selectAll";
        private static final String CLEAR_SELECTION = "clearSelection";
        private static final String SCROLL_LEFT = "scrollLeft";
        private static final String SCROLL_RIGHT = "scrollRight";
        private static final String SCROLL_LEFT_EXTEND_SELECTION =
                                    "scrollLeftExtendSelection";
        private static final String SCROLL_RIGHT_EXTEND_SELECTION =
                                    "scrollRightExtendSelection";
        private static final String SCROLL_RIGHT_CHANGE_LEAD =
                                    "scrollRightChangeLead";
        private static final String SCROLL_LEFT_CHANGE_LEAD =
                                    "scrollLeftChangeLead";
        private static final String EXPAND = "expand";
        private static final String COLLAPSE = "collapse";
        private static final String MOVE_SELECTION_TO_PARENT =
                                    "moveSelectionToParent";

        // add the lead item to the selection without changing lead or anchor
        private static final String ADD_TO_SELECTION = "addToSelection";

        // toggle the selected state of the lead item and move the anchor to it
        private static final String TOGGLE_AND_ANCHOR = "toggleAndAnchor";

        // extend the selection to the lead item
        private static final String EXTEND_TO = "extendTo";

        // move the anchor to the lead and ensure only that item is selected
        private static final String MOVE_SELECTION_TO = "moveSelectionTo";

        Actions() {
            super(null);
        }

        Actions(String key) {
            super(key);
        }

        @Override
        public boolean accept(Object o) {
            if (o instanceof JTree) {
                if (getName() == CANCEL_EDITING) {
                    return ((JTree)o).isEditing();
                }
            }
            return true;
        }

        public void actionPerformed(ActionEvent e) {
            JTree tree = (JTree)e.getSource();
            BasicTreeUI ui = (BasicTreeUI)BasicLookAndFeel.getUIOfType(
                             tree.getUI(), BasicTreeUI.class);
            if (ui == null) {
                return;
            }
            String key = getName();
            if (key == SELECT_PREVIOUS) {
                increment(tree, ui, -1, false, true);
            }
            else if (key == SELECT_PREVIOUS_CHANGE_LEAD) {
                increment(tree, ui, -1, false, false);
            }
            else if (key == SELECT_PREVIOUS_EXTEND_SELECTION) {
                increment(tree, ui, -1, true, true);
            }
            else if (key == SELECT_NEXT) {
                increment(tree, ui, 1, false, true);
            }
            else if (key == SELECT_NEXT_CHANGE_LEAD) {
                increment(tree, ui, 1, false, false);
            }
            else if (key == SELECT_NEXT_EXTEND_SELECTION) {
                increment(tree, ui, 1, true, true);
            }
            else if (key == SELECT_CHILD) {
                traverse(tree, ui, 1, true);
            }
            else if (key == SELECT_CHILD_CHANGE_LEAD) {
                traverse(tree, ui, 1, false);
            }
            else if (key == SELECT_PARENT) {
                traverse(tree, ui, -1, true);
            }
            else if (key == SELECT_PARENT_CHANGE_LEAD) {
                traverse(tree, ui, -1, false);
            }
            else if (key == SCROLL_UP_CHANGE_SELECTION) {
                page(tree, ui, -1, false, true);
            }
            else if (key == SCROLL_UP_CHANGE_LEAD) {
                page(tree, ui, -1, false, false);
            }
            else if (key == SCROLL_UP_EXTEND_SELECTION) {
                page(tree, ui, -1, true, true);
            }
            else if (key == SCROLL_DOWN_CHANGE_SELECTION) {
                page(tree, ui, 1, false, true);
            }
            else if (key == SCROLL_DOWN_EXTEND_SELECTION) {
                page(tree, ui, 1, true, true);
            }
            else if (key == SCROLL_DOWN_CHANGE_LEAD) {
                page(tree, ui, 1, false, false);
            }
            else if (key == SELECT_FIRST) {
                home(tree, ui, -1, false, true);
            }
            else if (key == SELECT_FIRST_CHANGE_LEAD) {
                home(tree, ui, -1, false, false);
            }
            else if (key == SELECT_FIRST_EXTEND_SELECTION) {
                home(tree, ui, -1, true, true);
            }
            else if (key == SELECT_LAST) {
                home(tree, ui, 1, false, true);
            }
            else if (key == SELECT_LAST_CHANGE_LEAD) {
                home(tree, ui, 1, false, false);
            }
            else if (key == SELECT_LAST_EXTEND_SELECTION) {
                home(tree, ui, 1, true, true);
            }
            else if (key == TOGGLE) {
                toggle(tree, ui);
            }
            else if (key == CANCEL_EDITING) {
                cancelEditing(tree, ui);
            }
            else if (key == START_EDITING) {
                startEditing(tree, ui);
            }
            else if (key == SELECT_ALL) {
                selectAll(tree, ui, true);
            }
            else if (key == CLEAR_SELECTION) {
                selectAll(tree, ui, false);
            }
            else if (key == ADD_TO_SELECTION) {
                if (ui.getRowCount(tree) > 0) {
                    int lead = ui.getLeadSelectionRow();
                    if (!tree.isRowSelected(lead)) {
                        TreePath aPath = ui.getAnchorSelectionPath();
                        tree.addSelectionRow(lead);
                        ui.setAnchorSelectionPath(aPath);
                    }
                }
            }
            else if (key == TOGGLE_AND_ANCHOR) {
                if (ui.getRowCount(tree) > 0) {
                    int lead = ui.getLeadSelectionRow();
                    TreePath lPath = ui.getLeadSelectionPath();
                    if (!tree.isRowSelected(lead)) {
                        tree.addSelectionRow(lead);
                    } else {
                        tree.removeSelectionRow(lead);
                        ui.setLeadSelectionPath(lPath);
                    }
                    ui.setAnchorSelectionPath(lPath);
                }
            }
            else if (key == EXTEND_TO) {
                extendSelection(tree, ui);
            }
            else if (key == MOVE_SELECTION_TO) {
                if (ui.getRowCount(tree) > 0) {
                    int lead = ui.getLeadSelectionRow();
                    tree.setSelectionInterval(lead, lead);
                }
            }
            else if (key == SCROLL_LEFT) {
                scroll(tree, ui, SwingConstants.HORIZONTAL, -10);
            }
            else if (key == SCROLL_RIGHT) {
                scroll(tree, ui, SwingConstants.HORIZONTAL, 10);
            }
            else if (key == SCROLL_LEFT_EXTEND_SELECTION) {
                scrollChangeSelection(tree, ui, -1, true, true);
            }
            else if (key == SCROLL_RIGHT_EXTEND_SELECTION) {
                scrollChangeSelection(tree, ui, 1, true, true);
            }
            else if (key == SCROLL_RIGHT_CHANGE_LEAD) {
                scrollChangeSelection(tree, ui, 1, false, false);
            }
            else if (key == SCROLL_LEFT_CHANGE_LEAD) {
                scrollChangeSelection(tree, ui, -1, false, false);
            }
            else if (key == EXPAND) {
                expand(tree, ui);
            }
            else if (key == COLLAPSE) {
                collapse(tree, ui);
            }
            else if (key == MOVE_SELECTION_TO_PARENT) {
                moveSelectionToParent(tree, ui);
            }
        }

        private void scrollChangeSelection(JTree tree, BasicTreeUI ui,
                           int direction, boolean addToSelection,
                           boolean changeSelection) {
            int           rowCount;

            if((rowCount = ui.getRowCount(tree)) > 0 &&
                ui.treeSelectionModel != null) {
                TreePath          newPath;
                Rectangle         visRect = tree.getVisibleRect();

                if (direction == -1) {
                    newPath = ui.getClosestPathForLocation(tree, visRect.x,
                                                        visRect.y);
                    visRect.x = Math.max(0, visRect.x - visRect.width);
                }
                else {
                    visRect.x = Math.min(Math.max(0, tree.getWidth() -
                                   visRect.width), visRect.x + visRect.width);
                    newPath = ui.getClosestPathForLocation(tree, visRect.x,
                                                 visRect.y + visRect.height);
                }
                // Scroll
                tree.scrollRectToVisible(visRect);
                // select
                if (addToSelection) {
                    ui.extendSelection(newPath);
                }
                else if(changeSelection) {
                    tree.setSelectionPath(newPath);
                }
                else {
                    ui.setLeadSelectionPath(newPath, true);
                }
            }
        }

        private void scroll(JTree component, BasicTreeUI ui, int direction,
                            int amount) {
            Rectangle visRect = component.getVisibleRect();
            Dimension size = component.getSize();
            if (direction == SwingConstants.HORIZONTAL) {
                visRect.x += amount;
                visRect.x = Math.max(0, visRect.x);
                visRect.x = Math.min(Math.max(0, size.width - visRect.width),
                                     visRect.x);
            }
            else {
                visRect.y += amount;
                visRect.y = Math.max(0, visRect.y);
                visRect.y = Math.min(Math.max(0, size.width - visRect.height),
                                     visRect.y);
            }
            component.scrollRectToVisible(visRect);
        }

        private void extendSelection(JTree tree, BasicTreeUI ui) {
            if (ui.getRowCount(tree) > 0) {
                int       lead = ui.getLeadSelectionRow();

                if (lead != -1) {
                    TreePath      leadP = ui.getLeadSelectionPath();
                    TreePath      aPath = ui.getAnchorSelectionPath();
                    int           aRow = ui.getRowForPath(tree, aPath);

                    if(aRow == -1)
                        aRow = 0;
                    tree.setSelectionInterval(aRow, lead);
                    ui.setLeadSelectionPath(leadP);
                    ui.setAnchorSelectionPath(aPath);
                }
            }
        }

        private void selectAll(JTree tree, BasicTreeUI ui, boolean selectAll) {
            int                   rowCount = ui.getRowCount(tree);

            if(rowCount > 0) {
                if(selectAll) {
                    if (tree.getSelectionModel().getSelectionMode() ==
                            TreeSelectionModel.SINGLE_TREE_SELECTION) {

                        int lead = ui.getLeadSelectionRow();
                        if (lead != -1) {
                            tree.setSelectionRow(lead);
                        } else if (tree.getMinSelectionRow() == -1) {
                            tree.setSelectionRow(0);
                            ui.ensureRowsAreVisible(0, 0);
                        }
                        return;
                    }

                    TreePath      lastPath = ui.getLeadSelectionPath();
                    TreePath      aPath = ui.getAnchorSelectionPath();

                    if(lastPath != null && !tree.isVisible(lastPath)) {
                        lastPath = null;
                    }
                    tree.setSelectionInterval(0, rowCount - 1);
                    if(lastPath != null) {
                        ui.setLeadSelectionPath(lastPath);
                    }
                    if(aPath != null && tree.isVisible(aPath)) {
                        ui.setAnchorSelectionPath(aPath);
                    }
                }
                else {
                    TreePath      lastPath = ui.getLeadSelectionPath();
                    TreePath      aPath = ui.getAnchorSelectionPath();

                    tree.clearSelection();
                    ui.setAnchorSelectionPath(aPath);
                    ui.setLeadSelectionPath(lastPath);
                }
            }
        }

        private void startEditing(JTree tree, BasicTreeUI ui) {
            TreePath   lead = ui.getLeadSelectionPath();
            int        editRow = (lead != null) ?
                                     ui.getRowForPath(tree, lead) : -1;

            if(editRow != -1) {
                tree.startEditingAtPath(lead);
            }
        }

        private void cancelEditing(JTree tree, BasicTreeUI ui) {
            tree.cancelEditing();
        }

        private void toggle(JTree tree, BasicTreeUI ui) {
            int            selRow = ui.getLeadSelectionRow();

            if(selRow != -1 && !ui.isLeaf(selRow)) {
                TreePath aPath = ui.getAnchorSelectionPath();
                TreePath lPath = ui.getLeadSelectionPath();

                ui.toggleExpandState(ui.getPathForRow(tree, selRow));
                ui.setAnchorSelectionPath(aPath);
                ui.setLeadSelectionPath(lPath);
            }
        }

        private void expand(JTree tree, BasicTreeUI ui) {
            int selRow = ui.getLeadSelectionRow();
            tree.expandRow(selRow);
        }

        private void collapse(JTree tree, BasicTreeUI ui) {
            int selRow = ui.getLeadSelectionRow();
            tree.collapseRow(selRow);
        }

        private void increment(JTree tree, BasicTreeUI ui, int direction,
                               boolean addToSelection,
                               boolean changeSelection) {

            // disable moving of lead unless in discontiguous mode
            if (!addToSelection && !changeSelection &&
                    tree.getSelectionModel().getSelectionMode() !=
                        TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION) {
                changeSelection = true;
            }

            int              rowCount;

            if(ui.treeSelectionModel != null &&
                  (rowCount = tree.getRowCount()) > 0) {
                int                  selIndex = ui.getLeadSelectionRow();
                int                  newIndex;

                if(selIndex == -1) {
                    if(direction == 1)
                        newIndex = 0;
                    else
                        newIndex = rowCount - 1;
                }
                else
                    /* Aparently people don't like wrapping;( */
                    newIndex = Math.min(rowCount - 1, Math.max
                                        (0, (selIndex + direction)));
                if(addToSelection && ui.treeSelectionModel.
                        getSelectionMode() != TreeSelectionModel.
                        SINGLE_TREE_SELECTION) {
                    ui.extendSelection(tree.getPathForRow(newIndex));
                }
                else if(changeSelection) {
                    tree.setSelectionInterval(newIndex, newIndex);
                }
                else {
                    ui.setLeadSelectionPath(tree.getPathForRow(newIndex),true);
                }
                ui.ensureRowsAreVisible(newIndex, newIndex);
                ui.lastSelectedRow = newIndex;
            }
        }

        private void traverse(JTree tree, BasicTreeUI ui, int direction,
                              boolean changeSelection) {

            // disable moving of lead unless in discontiguous mode
            if (!changeSelection &&
                    tree.getSelectionModel().getSelectionMode() !=
                        TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION) {
                changeSelection = true;
            }

            int                rowCount;

            if((rowCount = tree.getRowCount()) > 0) {
                int               minSelIndex = ui.getLeadSelectionRow();
                int               newIndex;

                if(minSelIndex == -1)
                    newIndex = 0;
                else {
                    /* Try and expand the node, otherwise go to next
                       node. */
                    if(direction == 1) {
                        TreePath minSelPath = ui.getPathForRow(tree, minSelIndex);
                        int childCount = tree.getModel().
                            getChildCount(minSelPath.getLastPathComponent());
                        newIndex = -1;
                        if (!ui.isLeaf(minSelIndex)) {
                            if (!tree.isExpanded(minSelIndex)) {
                                ui.toggleExpandState(minSelPath);
                            }
                            else if (childCount > 0) {
                                newIndex = Math.min(minSelIndex + 1, rowCount - 1);
                            }
                        }
                    }
                    /* Try to collapse node. */
                    else {
                        if(!ui.isLeaf(minSelIndex) &&
                           tree.isExpanded(minSelIndex)) {
                            ui.toggleExpandState(ui.getPathForRow
                                              (tree, minSelIndex));
                            newIndex = -1;
                        }
                        else {
                            TreePath         path = ui.getPathForRow(tree,
                                                                  minSelIndex);

                            if(path != null && path.getPathCount() > 1) {
                                newIndex = ui.getRowForPath(tree, path.
                                                         getParentPath());
                            }
                            else
                                newIndex = -1;
                        }
                    }
                }
                if(newIndex != -1) {
                    if(changeSelection) {
                        tree.setSelectionInterval(newIndex, newIndex);
                    }
                    else {
                        ui.setLeadSelectionPath(ui.getPathForRow(
                                                    tree, newIndex), true);
                    }
                    ui.ensureRowsAreVisible(newIndex, newIndex);
                }
            }
        }

        private void moveSelectionToParent(JTree tree, BasicTreeUI ui) {
            int selRow = ui.getLeadSelectionRow();
            TreePath path = ui.getPathForRow(tree, selRow);
            if (path != null && path.getPathCount() > 1) {
                int  newIndex = ui.getRowForPath(tree, path.getParentPath());
                if (newIndex != -1) {
                    tree.setSelectionInterval(newIndex, newIndex);
                    ui.ensureRowsAreVisible(newIndex, newIndex);
                }
            }
        }

        private void page(JTree tree, BasicTreeUI ui, int direction,
                          boolean addToSelection, boolean changeSelection) {

            // disable moving of lead unless in discontiguous mode
            if (!addToSelection && !changeSelection &&
                    tree.getSelectionModel().getSelectionMode() !=
                        TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION) {
                changeSelection = true;
            }

            int           rowCount;

            if((rowCount = ui.getRowCount(tree)) > 0 &&
                           ui.treeSelectionModel != null) {
                Dimension         maxSize = tree.getSize();
                TreePath          lead = ui.getLeadSelectionPath();
                TreePath          newPath;
                Rectangle         visRect = tree.getVisibleRect();

                if(direction == -1) {
                    // up.
                    newPath = ui.getClosestPathForLocation(tree, visRect.x,
                                                         visRect.y);
                    if(newPath.equals(lead)) {
                        visRect.y = Math.max(0, visRect.y - visRect.height);
                        newPath = tree.getClosestPathForLocation(visRect.x,
                                                                 visRect.y);
                    }
                }
                else {
                    // down
                    visRect.y = Math.min(maxSize.height, visRect.y +
                                         visRect.height - 1);
                    newPath = tree.getClosestPathForLocation(visRect.x,
                                                             visRect.y);
                    if(newPath.equals(lead)) {
                        visRect.y = Math.min(maxSize.height, visRect.y +
                                             visRect.height - 1);
                        newPath = tree.getClosestPathForLocation(visRect.x,
                                                                 visRect.y);
                    }
                }
                Rectangle            newRect = ui.getPathBounds(tree, newPath);
                if (newRect != null) {
                    newRect.x = visRect.x;
                    newRect.width = visRect.width;
                    if(direction == -1) {
                        newRect.height = visRect.height;
                    }
                    else {
                        newRect.y -= (visRect.height - newRect.height);
                        newRect.height = visRect.height;
                    }

                    if(addToSelection) {
                        ui.extendSelection(newPath);
                    }
                    else if(changeSelection) {
                        tree.setSelectionPath(newPath);
                    }
                    else {
                        ui.setLeadSelectionPath(newPath, true);
                    }
                    tree.scrollRectToVisible(newRect);
                }
            }
        }

        private void home(JTree tree, final BasicTreeUI ui, int direction,
                          boolean addToSelection, boolean changeSelection) {

            // disable moving of lead unless in discontiguous mode
            if (!addToSelection && !changeSelection &&
                    tree.getSelectionModel().getSelectionMode() !=
                        TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION) {
                changeSelection = true;
            }

            final int rowCount = ui.getRowCount(tree);

            if (rowCount > 0) {
                if(direction == -1) {
                    ui.ensureRowsAreVisible(0, 0);
                    if (addToSelection) {
                        TreePath        aPath = ui.getAnchorSelectionPath();
                        int             aRow = (aPath == null) ? -1 :
                                        ui.getRowForPath(tree, aPath);

                        if (aRow == -1) {
                            tree.setSelectionInterval(0, 0);
                        }
                        else {
                            tree.setSelectionInterval(0, aRow);
                            ui.setAnchorSelectionPath(aPath);
                            ui.setLeadSelectionPath(ui.getPathForRow(tree, 0));
                        }
                    }
                    else if(changeSelection) {
                        tree.setSelectionInterval(0, 0);
                    }
                    else {
                        ui.setLeadSelectionPath(ui.getPathForRow(tree, 0),
                                                true);
                    }
                }
                else {
                    ui.ensureRowsAreVisible(rowCount - 1, rowCount - 1);
                    if (addToSelection) {
                        TreePath        aPath = ui.getAnchorSelectionPath();
                        int             aRow = (aPath == null) ? -1 :
                                        ui.getRowForPath(tree, aPath);

                        if (aRow == -1) {
                            tree.setSelectionInterval(rowCount - 1,
                                                      rowCount -1);
                        }
                        else {
                            tree.setSelectionInterval(aRow, rowCount - 1);
                            ui.setAnchorSelectionPath(aPath);
                            ui.setLeadSelectionPath(ui.getPathForRow(tree,
                                                               rowCount -1));
                        }
                    }
                    else if(changeSelection) {
                        tree.setSelectionInterval(rowCount - 1, rowCount - 1);
                    }
                    else {
                        ui.setLeadSelectionPath(ui.getPathForRow(tree,
                                                          rowCount - 1), true);
                    }
                    if (ui.isLargeModel()){
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                ui.ensureRowsAreVisible(rowCount - 1, rowCount - 1);
                            }
                        });
                    }
                }
            }
        }
    }
} // End of class BasicTreeUI
