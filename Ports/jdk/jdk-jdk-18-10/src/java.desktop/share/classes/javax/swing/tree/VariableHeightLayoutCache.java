/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.tree;

import javax.swing.event.TreeModelEvent;
import java.awt.Rectangle;
import java.beans.BeanProperty;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Vector;

import sun.swing.SwingUtilities2;

/**
 * NOTE: This will become more open in a future release.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Rob Davis
 * @author Ray Ryan
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Same-version serialization only
public class VariableHeightLayoutCache extends AbstractLayoutCache {
    /**
     * The array of nodes that are currently visible, in the order they
     * are displayed.
     */
    private Vector<Object> visibleNodes;

    /**
     * This is set to true if one of the entries has an invalid size.
     */
    private boolean           updateNodeSizes;

    /**
     * The root node of the internal cache of nodes that have been shown.
     * If the treeModel is vending a network rather than a true tree,
     * there may be one cached node for each path to a modeled node.
     */
    private TreeStateNode     root;

    /**
     * Used in getting sizes for nodes to avoid creating a new Rectangle
     * every time a size is needed.
     */
    private Rectangle         boundsBuffer;

    /**
     * Maps from <code>TreePath</code> to a <code>TreeStateNode</code>.
     */
    private Hashtable<TreePath, TreeStateNode> treePathMapping;

    /**
     * A stack of stacks.
     */
    private Stack<Stack<TreePath>> tempStacks;


    /**
     * Constructs a {@code VariableHeightLayoutCache}.
     */
    public VariableHeightLayoutCache() {
        super();
        tempStacks = new Stack<Stack<TreePath>>();
        visibleNodes = new Vector<Object>();
        boundsBuffer = new Rectangle();
        treePathMapping = new Hashtable<TreePath, TreeStateNode>();
    }

    /**
     * Sets the <code>TreeModel</code> that will provide the data.
     *
     * @param newModel the <code>TreeModel</code> that is to provide the data
     */
    @BeanProperty(description
            = "The TreeModel that will provide the data.")
    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
        rebuild(false);
    }

    /**
     * Determines whether or not the root node from
     * the <code>TreeModel</code> is visible.
     *
     * @param rootVisible true if the root node of the tree is to be displayed
     * @see #rootVisible
     */
    @BeanProperty(description
            = "Whether or not the root node from the TreeModel is visible.")
    public void setRootVisible(boolean rootVisible) {
        if(isRootVisible() != rootVisible && root != null) {
            if(rootVisible) {
                root.updatePreferredSize(0);
                visibleNodes.insertElementAt(root, 0);
            }
            else if(visibleNodes.size() > 0) {
                visibleNodes.removeElementAt(0);
                if(treeSelectionModel != null)
                    treeSelectionModel.removeSelectionPath
                        (root.getTreePath());
            }
            if(treeSelectionModel != null)
                treeSelectionModel.resetRowSelection();
            if(getRowCount() > 0)
                getNode(0).setYOrigin(0);
            updateYLocationsFrom(0);
            visibleNodesChanged();
        }
        super.setRootVisible(rootVisible);
    }

    /**
     * Sets the height of each cell.  If the specified value
     * is less than or equal to zero the current cell renderer is
     * queried for each row's height.
     *
     * @param rowHeight the height of each cell, in pixels
     */
    @BeanProperty(description
            = "The height of each cell.")
    public void setRowHeight(int rowHeight) {
        if(rowHeight != getRowHeight()) {
            super.setRowHeight(rowHeight);
            invalidateSizes();
            this.visibleNodesChanged();
        }
    }

    /**
     * Sets the renderer that is responsible for drawing nodes in the tree.
     * @param nd the renderer
     */
    public void setNodeDimensions(NodeDimensions nd) {
        super.setNodeDimensions(nd);
        invalidateSizes();
        visibleNodesChanged();
    }

    /**
     * Marks the path <code>path</code> expanded state to
     * <code>isExpanded</code>.
     * @param path the <code>TreePath</code> of interest
     * @param isExpanded true if the path should be expanded, otherwise false
     */
    public void setExpandedState(TreePath path, boolean isExpanded) {
        if(path != null) {
            if(isExpanded)
                ensurePathIsExpanded(path, true);
            else {
                TreeStateNode        node = getNodeForPath(path, false, true);

                if(node != null) {
                    node.makeVisible();
                    node.collapse();
                }
            }
        }
    }

    /**
     * Returns true if the path is expanded, and visible.
     * @return true if the path is expanded and visible, otherwise false
     */
    public boolean getExpandedState(TreePath path) {
        TreeStateNode       node = getNodeForPath(path, true, false);

        return (node != null) ? (node.isVisible() && node.isExpanded()) :
                                 false;
    }

    /**
      * Returns the <code>Rectangle</code> enclosing the label portion
      * into which the item identified by <code>path</code> will be drawn.
      *
      * @param path  the path to be drawn
      * @param placeIn the bounds of the enclosing rectangle
      * @return the bounds of the enclosing rectangle or <code>null</code>
      *    if the node could not be ascertained
      */
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        TreeStateNode       node = getNodeForPath(path, true, false);

        if(node != null) {
            if(updateNodeSizes)
                updateNodeSizes(false);
            return node.getNodeBounds(placeIn);
        }
        return null;
    }

    /**
      * Returns the path for <code>row</code>.  If <code>row</code>
      * is not visible, <code>null</code> is returned.
      *
      * @param row the location of interest
      * @return the path for <code>row</code>, or <code>null</code>
      * if <code>row</code> is not visible
      */
    public TreePath getPathForRow(int row) {
        if(row >= 0 && row < getRowCount()) {
            return getNode(row).getTreePath();
        }
        return null;
    }

    /**
      * Returns the row where the last item identified in path is visible.
      * Will return -1 if any of the elements in path are not
      * currently visible.
      *
      * @param path the <code>TreePath</code> of interest
      * @return the row where the last item in path is visible
      */
    public int getRowForPath(TreePath path) {
        if(path == null)
            return -1;

        TreeStateNode    visNode = getNodeForPath(path, true, false);

        if(visNode != null)
            return visNode.getRow();
        return -1;
    }

    /**
     * Returns the number of visible rows.
     * @return the number of visible rows
     */
    public int getRowCount() {
        return visibleNodes.size();
    }

    /**
     * Instructs the <code>LayoutCache</code> that the bounds for
     * <code>path</code> are invalid, and need to be updated.
     *
     * @param path the <code>TreePath</code> which is now invalid
     */
    public void invalidatePathBounds(TreePath path) {
        TreeStateNode       node = getNodeForPath(path, true, false);

        if(node != null) {
            node.markSizeInvalid();
            if(node.isVisible())
                updateYLocationsFrom(node.getRow());
        }
    }

    /**
     * Returns the preferred height.
     * @return the preferred height
     */
    public int getPreferredHeight() {
        // Get the height
        int           rowCount = getRowCount();

        if(rowCount > 0) {
            TreeStateNode  node = getNode(rowCount - 1);

            return node.getYOrigin() + node.getPreferredHeight();
        }
        return 0;
    }

    /**
     * Returns the preferred width and height for the region in
     * <code>visibleRegion</code>.
     *
     * @param bounds  the region being queried
     */
    public int getPreferredWidth(Rectangle bounds) {
        if(updateNodeSizes)
            updateNodeSizes(false);

        return getMaxNodeWidth();
    }

    /**
      * Returns the path to the node that is closest to x,y.  If
      * there is nothing currently visible this will return <code>null</code>,
      * otherwise it will always return a valid path.
      * If you need to test if the
      * returned object is exactly at x, y you should get the bounds for
      * the returned path and test x, y against that.
      *
      * @param x  the x-coordinate
      * @param y  the y-coordinate
      * @return the path to the node that is closest to x, y
      */
    public TreePath getPathClosestTo(int x, int y) {
        if(getRowCount() == 0)
            return null;

        if(updateNodeSizes)
            updateNodeSizes(false);

        int                row = getRowContainingYLocation(y);

        return getNode(row).getTreePath();
    }

    /**
     * Returns an <code>Enumerator</code> that increments over the visible paths
     * starting at the passed in location. The ordering of the enumeration
     * is based on how the paths are displayed.
     *
     * @param path the location in the <code>TreePath</code> to start
     * @return an <code>Enumerator</code> that increments over the visible
     *     paths
     */
    public Enumeration<TreePath> getVisiblePathsFrom(TreePath path) {
        TreeStateNode       node = getNodeForPath(path, true, false);

        if(node != null) {
            return new VisibleTreeStateNodeEnumeration(node);
        }
        return null;
    }

    /**
     * Returns the number of visible children for <code>path</code>.
     * @return the number of visible children for <code>path</code>
     */
    public int getVisibleChildCount(TreePath path) {
        TreeStateNode         node = getNodeForPath(path, true, false);

        return (node != null) ? node.getVisibleChildCount() : 0;
    }

    /**
     * Informs the <code>TreeState</code> that it needs to recalculate
     * all the sizes it is referencing.
     */
    public void invalidateSizes() {
        if(root != null)
            root.deepMarkSizeInvalid();
        if(!isFixedRowHeight() && visibleNodes.size() > 0) {
            updateNodeSizes(true);
        }
    }

    /**
      * Returns true if the value identified by <code>path</code> is
      * currently expanded.
      * @return true if the value identified by <code>path</code> is
      *    currently expanded
      */
    public boolean isExpanded(TreePath path) {
        if(path != null) {
            TreeStateNode     lastNode = getNodeForPath(path, true, false);

            return (lastNode != null && lastNode.isExpanded());
        }
        return false;
    }

    //
    // TreeModelListener methods
    //

    /**
     * Invoked after a node (or a set of siblings) has changed in some
     * way. The node(s) have not changed locations in the tree or
     * altered their children arrays, but other attributes have
     * changed and may affect presentation. Example: the name of a
     * file has changed, but it is in the same location in the file
     * system.
     *
     * <p><code>e.path</code> returns the path the parent of the
     * changed node(s).
     *
     * <p><code>e.childIndices</code> returns the index(es) of the
     * changed node(s).
     *
     * @param e the <code>TreeModelEvent</code> of interest
     */
    public void treeNodesChanged(TreeModelEvent e) {
        if(e != null) {
            int[]             changedIndexs  = e.getChildIndices();
            TreeStateNode     changedNode = getNodeForPath(
                    SwingUtilities2.getTreePath(e, getModel()), false, false);

            if(changedNode != null) {
                Object            changedValue = changedNode.getValue();

                /* Update the size of the changed node, as well as all the
                   child indexs that are passed in. */
                changedNode.updatePreferredSize();
                if(changedNode.hasBeenExpanded() && changedIndexs != null) {
                    for(int index : changedIndexs) {
                        TreeStateNode changedChildNode = (TreeStateNode)changedNode
                                    .getChildAt(index);
                        /* Reset the user object. */
                        changedChildNode.setUserObject
                                    (treeModel.getChild(changedValue, index));
                        changedChildNode.updatePreferredSize();
                    }
                }
                else if (changedNode == root) {
                    // Null indicies for root indicates it changed.
                    changedNode.updatePreferredSize();
                }
                if(!isFixedRowHeight()) {
                    int          aRow = changedNode.getRow();

                    if(aRow != -1)
                        this.updateYLocationsFrom(aRow);
                }
                this.visibleNodesChanged();
            }
        }
    }


    /**
     * Invoked after nodes have been inserted into the tree.
     *
     * <p><code>e.path</code> returns the parent of the new nodes.
     * <p><code>e.childIndices</code> returns the indices of the new nodes in
     * ascending order.
     *
     * @param e the <code>TreeModelEvent</code> of interest
     */
    public void treeNodesInserted(TreeModelEvent e) {
        if(e != null) {
            int[]             changedIndexs = e.getChildIndices();
            TreeStateNode     changedParentNode = getNodeForPath(
                    SwingUtilities2.getTreePath(e, getModel()), false, false);
            /* Only need to update the children if the node has been
               expanded once. */
            // PENDING(scott): make sure childIndexs is sorted!
            if(changedParentNode != null && changedIndexs != null &&
               changedIndexs.length > 0) {
                if(changedParentNode.hasBeenExpanded()) {
                    boolean   makeVisible =((changedParentNode == root &&
                                            !rootVisible) ||
                                            (changedParentNode.getRow() != -1 &&
                                               changedParentNode.isExpanded()));
                    int  oldChildCount = changedParentNode.getChildCount();

                    for(int index : changedIndexs)
                    {
                        this.createNodeAt(changedParentNode, index);
                    }

                    if(oldChildCount == 0) {
                        // Update the size of the parent.
                        changedParentNode.updatePreferredSize();
                    }
                    if(treeSelectionModel != null)
                        treeSelectionModel.resetRowSelection();
                    /* Update the y origins from the index of the parent
                       to the end of the visible rows. */
                    if(!isFixedRowHeight() && (makeVisible ||
                                               (oldChildCount == 0 &&
                                        changedParentNode.isVisible()))) {
                        if(changedParentNode == root)
                            this.updateYLocationsFrom(0);
                        else
                            this.updateYLocationsFrom(changedParentNode.
                                                      getRow());
                        this.visibleNodesChanged();
                    }
                    else if(makeVisible)
                        this.visibleNodesChanged();
                }
                else if(treeModel.getChildCount(changedParentNode.getValue())
                        - changedIndexs.length == 0) {
                    changedParentNode.updatePreferredSize();
                    if(!isFixedRowHeight() && changedParentNode.isVisible())
                        updateYLocationsFrom(changedParentNode.getRow());
                }
            }
        }
    }

    /**
     * Invoked after nodes have been removed from the tree.  Note that
     * if a subtree is removed from the tree, this method may only be
     * invoked once for the root of the removed subtree, not once for
     * each individual set of siblings removed.
     *
     * <p><code>e.path</code> returns the former parent of the deleted nodes.
     *
     * <p><code>e.childIndices</code> returns the indices the nodes had
     * before they were deleted in ascending order.
     *
     * @param e the <code>TreeModelEvent</code> of interest
     */
    public void treeNodesRemoved(TreeModelEvent e) {
        if(e != null) {
            int[]             changedIndexs;
            TreeStateNode     changedParentNode;

            changedIndexs = e.getChildIndices();
            changedParentNode = getNodeForPath(SwingUtilities2.getTreePath(e, getModel()), false, false);
            // PENDING(scott): make sure that changedIndexs are sorted in
            // ascending order.
            if(changedParentNode != null && changedIndexs != null &&
               changedIndexs.length > 0) {
                if(changedParentNode.hasBeenExpanded()) {
                    boolean            makeInvisible;
                    int                counter;
                    int                removedRow;
                    TreeStateNode      removedNode;

                    makeInvisible = ((changedParentNode == root &&
                                      !rootVisible) ||
                                     (changedParentNode.getRow() != -1 &&
                                      changedParentNode.isExpanded()));
                    for(counter = changedIndexs.length - 1;counter >= 0;
                        counter--) {
                        removedNode = (TreeStateNode)changedParentNode.
                                getChildAt(changedIndexs[counter]);
                        if(removedNode.isExpanded()) {
                            removedNode.collapse(false);
                        }

                        /* Let the selection model now. */
                        if(makeInvisible) {
                            removedRow = removedNode.getRow();
                            if(removedRow != -1) {
                                visibleNodes.removeElementAt(removedRow);
                            }
                        }
                        changedParentNode.remove(changedIndexs[counter]);
                    }
                    if(changedParentNode.getChildCount() == 0) {
                        // Update the size of the parent.
                        changedParentNode.updatePreferredSize();
                        if (changedParentNode.isExpanded() &&
                                   changedParentNode.isLeaf()) {
                            // Node has become a leaf, collapse it.
                            changedParentNode.collapse(false);
                        }
                    }
                    if(treeSelectionModel != null)
                        treeSelectionModel.resetRowSelection();
                    /* Update the y origins from the index of the parent
                       to the end of the visible rows. */
                    if(!isFixedRowHeight() && (makeInvisible ||
                               (changedParentNode.getChildCount() == 0 &&
                                changedParentNode.isVisible()))) {
                        if(changedParentNode == root) {
                            /* It is possible for first row to have been
                               removed if the root isn't visible, in which
                               case ylocations will be off! */
                            if(getRowCount() > 0)
                                getNode(0).setYOrigin(0);
                            updateYLocationsFrom(0);
                        }
                        else
                            updateYLocationsFrom(changedParentNode.getRow());
                        this.visibleNodesChanged();
                    }
                    else if(makeInvisible)
                        this.visibleNodesChanged();
                }
                else if(treeModel.getChildCount(changedParentNode.getValue())
                        == 0) {
                    changedParentNode.updatePreferredSize();
                    if(!isFixedRowHeight() && changedParentNode.isVisible())
                        this.updateYLocationsFrom(changedParentNode.getRow());
                }
            }
        }
    }

    /**
     * Invoked after the tree has drastically changed structure from a
     * given node down.  If the path returned by <code>e.getPath</code>
     * is of length one and the first element does not identify the
     * current root node the first element should become the new root
     * of the tree.
     *
     * <p><code>e.path</code> holds the path to the node.
     * <p><code>e.childIndices</code> returns <code>null</code>.
     *
     * @param e the <code>TreeModelEvent</code> of interest
     */
    public void treeStructureChanged(TreeModelEvent e) {
        if(e != null)
        {
            TreePath          changedPath = SwingUtilities2.getTreePath(e, getModel());
            TreeStateNode     changedNode;

            changedNode = getNodeForPath(changedPath, false, false);

            // Check if root has changed, either to a null root, or
            // to an entirely new root.
            if(changedNode == root ||
               (changedNode == null &&
                ((changedPath == null && treeModel != null &&
                  treeModel.getRoot() == null) ||
                 (changedPath != null && changedPath.getPathCount() == 1)))) {
                rebuild(true);
            }
            else if(changedNode != null) {
                int                              nodeIndex;
                TreeStateNode                    newNode, parent;
                boolean                          wasExpanded, wasVisible;
                int                              newIndex;

                wasExpanded = changedNode.isExpanded();
                wasVisible = (changedNode.getRow() != -1);
                /* Remove the current node and recreate a new one. */
                parent = (TreeStateNode)changedNode.getParent();
                nodeIndex = parent.getIndex(changedNode);
                if(wasVisible && wasExpanded) {
                    changedNode.collapse(false);
                }
                if(wasVisible)
                    visibleNodes.removeElement(changedNode);
                changedNode.removeFromParent();
                createNodeAt(parent, nodeIndex);
                newNode = (TreeStateNode)parent.getChildAt(nodeIndex);
                if(wasVisible && wasExpanded)
                    newNode.expand(false);
                newIndex = newNode.getRow();
                if(!isFixedRowHeight() && wasVisible) {
                    if(newIndex == 0)
                        updateYLocationsFrom(newIndex);
                    else
                        updateYLocationsFrom(newIndex - 1);
                    this.visibleNodesChanged();
                }
                else if(wasVisible)
                    this.visibleNodesChanged();
            }
        }
    }


    //
    // Local methods
    //

    private void visibleNodesChanged() {
    }

    /**
     * Adds a mapping for node.
     */
    private void addMapping(TreeStateNode node) {
        treePathMapping.put(node.getTreePath(), node);
    }

    /**
     * Removes the mapping for a previously added node.
     */
    private void removeMapping(TreeStateNode node) {
        treePathMapping.remove(node.getTreePath());
    }

    /**
     * Returns the node previously added for <code>path</code>. This may
     * return null, if you to create a node use getNodeForPath.
     */
    private TreeStateNode getMapping(TreePath path) {
        return treePathMapping.get(path);
    }

    /**
     * Retursn the bounds for row, <code>row</code> by reference in
     * <code>placeIn</code>. If <code>placeIn</code> is null a new
     * Rectangle will be created and returned.
     */
    private Rectangle getBounds(int row, Rectangle placeIn) {
        if(updateNodeSizes)
            updateNodeSizes(false);

        if(row >= 0 && row < getRowCount()) {
            return getNode(row).getNodeBounds(placeIn);
        }
        return null;
    }

    /**
     * Completely rebuild the tree, all expanded state, and node caches are
     * removed. All nodes are collapsed, except the root.
     */
    private void rebuild(boolean clearSelection) {
        Object rootObject;

        treePathMapping.clear();
        if(treeModel != null && (rootObject = treeModel.getRoot()) != null) {
            root = createNodeForValue(rootObject);
            root.path = new TreePath(rootObject);
            addMapping(root);
            root.updatePreferredSize(0);
            visibleNodes.removeAllElements();
            if (isRootVisible())
                visibleNodes.addElement(root);
            if(!root.isExpanded())
                root.expand();
            else {
                Enumeration<?> cursor = root.children();
                while(cursor.hasMoreElements()) {
                    visibleNodes.addElement(cursor.nextElement());
                }
                if(!isFixedRowHeight())
                    updateYLocationsFrom(0);
            }
        }
        else {
            visibleNodes.removeAllElements();
            root = null;
        }
        if(clearSelection && treeSelectionModel != null) {
            treeSelectionModel.clearSelection();
        }
        this.visibleNodesChanged();
    }

    /**
      * Creates a new node to represent the node at <I>childIndex</I> in
      * <I>parent</I>s children.  This should be called if the node doesn't
      * already exist and <I>parent</I> has been expanded at least once.
      * The newly created node will be made visible if <I>parent</I> is
      * currently expanded.  This does not update the position of any
      * cells, nor update the selection if it needs to be.  If succesful
      * in creating the new TreeStateNode, it is returned, otherwise
      * null is returned.
      */
    private TreeStateNode createNodeAt(TreeStateNode parent,
                                         int childIndex) {
        boolean                isParentRoot;
        Object                 newValue;
        TreeStateNode          newChildNode;

        newValue = treeModel.getChild(parent.getValue(), childIndex);
        newChildNode = createNodeForValue(newValue);
        parent.insert(newChildNode, childIndex);
        newChildNode.updatePreferredSize(-1);
        isParentRoot = (parent == root);
        if(newChildNode != null && parent.isExpanded() &&
           (parent.getRow() != -1 || isParentRoot)) {
            int                 newRow;

            /* Find the new row to insert this newly visible node at. */
            if(childIndex == 0) {
                if(isParentRoot && !isRootVisible())
                    newRow = 0;
                else
                    newRow = parent.getRow() + 1;
            }
            else if(childIndex == parent.getChildCount())
                newRow = parent.getLastVisibleNode().getRow() + 1;
            else {
                TreeStateNode          previousNode;

                previousNode = (TreeStateNode)parent.
                    getChildAt(childIndex - 1);
                newRow = previousNode.getLastVisibleNode().getRow() + 1;
            }
            visibleNodes.insertElementAt(newChildNode, newRow);
        }
        return newChildNode;
    }

    /**
      * Returns the TreeStateNode identified by path.  This mirrors
      * the behavior of getNodeForPath, but tries to take advantage of
      * path if it is an instance of AbstractTreePath.
      */
    private TreeStateNode getNodeForPath(TreePath path,
                                           boolean onlyIfVisible,
                                           boolean shouldCreate) {
        if(path != null) {
            TreeStateNode      node;

            node = getMapping(path);
            if(node != null) {
                if(onlyIfVisible && !node.isVisible())
                    return null;
                return node;
            }

            // Check all the parent paths, until a match is found.
            Stack<TreePath> paths;

            if(tempStacks.size() == 0) {
                paths = new Stack<TreePath>();
            }
            else {
                paths = tempStacks.pop();
            }

            try {
                paths.push(path);
                path = path.getParentPath();
                node = null;
                while(path != null) {
                    node = getMapping(path);
                    if(node != null) {
                        // Found a match, create entries for all paths in
                        // paths.
                        while(node != null && paths.size() > 0) {
                            path = paths.pop();
                            node.getLoadedChildren(shouldCreate);

                            int            childIndex = treeModel.
                                      getIndexOfChild(node.getUserObject(),
                                                  path.getLastPathComponent());

                            if(childIndex == -1 ||
                               childIndex >= node.getChildCount() ||
                               (onlyIfVisible && !node.isVisible())) {
                                node = null;
                            }
                            else
                                node = (TreeStateNode)node.getChildAt
                                               (childIndex);
                        }
                        return node;
                    }
                    paths.push(path);
                    path = path.getParentPath();
                }
            }
            finally {
                paths.removeAllElements();
                tempStacks.push(paths);
            }
            // If we get here it means they share a different root!
            // We could throw an exception...
        }
        return null;
    }

    /**
      * Updates the y locations of all of the visible nodes after
      * location.
      */
    private void updateYLocationsFrom(int location) {
        if(location >= 0 && location < getRowCount()) {
            int                    counter, maxCounter, newYOrigin;
            TreeStateNode          aNode;

            aNode = getNode(location);
            newYOrigin = aNode.getYOrigin() + aNode.getPreferredHeight();
            for(counter = location + 1, maxCounter = visibleNodes.size();
                counter < maxCounter;counter++) {
                aNode = (TreeStateNode)visibleNodes.
                    elementAt(counter);
                aNode.setYOrigin(newYOrigin);
                newYOrigin += aNode.getPreferredHeight();
            }
        }
    }

    /**
      * Resets the y origin of all the visible nodes as well as messaging
      * all the visible nodes to updatePreferredSize().  You should not
      * normally have to call this.  Expanding and contracting the nodes
      * automaticly adjusts the locations.
      * updateAll determines if updatePreferredSize() is call on all nodes
      * or just those that don't have a valid size.
      */
    private void updateNodeSizes(boolean updateAll) {
        int                      aY, counter, maxCounter;
        TreeStateNode            node;

        updateNodeSizes = false;
        for(aY = counter = 0, maxCounter = visibleNodes.size();
            counter < maxCounter; counter++) {
            node = (TreeStateNode)visibleNodes.elementAt(counter);
            node.setYOrigin(aY);
            if(updateAll || !node.hasValidSize())
                node.updatePreferredSize(counter);
            aY += node.getPreferredHeight();
        }
    }

    /**
      * Returns the index of the row containing location.  If there
      * are no rows, -1 is returned.  If location is beyond the last
      * row index, the last row index is returned.
      */
    private int getRowContainingYLocation(int location) {
        final int rows = getRowCount();

        if(rows <= 0)
            return -1;
        if(isFixedRowHeight()) {
            return Math.max(0, Math.min(rows - 1,
                                        location / getRowHeight()));
        }

        int max = rows, min = 0, mid = 0;

        while(min < max) {
            mid = (max - min) / 2 + min;
            TreeStateNode node = (TreeStateNode)visibleNodes.elementAt(mid);
            int minY = node.getYOrigin();
            int maxY = minY + node.getPreferredHeight();
            if(location < minY) {
                max = mid - 1;
            }
            else if(location >= maxY) {
                min = mid + 1;
            }
            else
                break;
        }
        if(min == max) {
            mid = min;
            if(mid >= rows)
                mid = rows - 1;
        }
        return mid;
    }

    /**
     * Ensures that all the path components in path are expanded, accept
     * for the last component which will only be expanded if expandLast
     * is true.
     * Returns true if succesful in finding the path.
     */
    private void ensurePathIsExpanded(TreePath aPath, boolean expandLast) {
        if(aPath != null) {
            // Make sure the last entry isn't a leaf.
            if(treeModel.isLeaf(aPath.getLastPathComponent())) {
                aPath = aPath.getParentPath();
                expandLast = true;
            }
            if(aPath != null) {
                TreeStateNode     lastNode = getNodeForPath(aPath, false,
                                                            true);

                if(lastNode != null) {
                    lastNode.makeVisible();
                    if(expandLast)
                        lastNode.expand();
                }
            }
        }
    }

    /**
     * Returns the AbstractTreeUI.VisibleNode displayed at the given row
     */
    private TreeStateNode getNode(int row) {
        return (TreeStateNode)visibleNodes.elementAt(row);
    }

    /**
      * Returns the maximum node width.
      */
    private int getMaxNodeWidth() {
        int                     maxWidth = 0;
        int                     nodeWidth;
        int                     counter;
        TreeStateNode           node;

        for(counter = getRowCount() - 1;counter >= 0;counter--) {
            node = this.getNode(counter);
            nodeWidth = node.getPreferredWidth() + node.getXOrigin();
            if(nodeWidth > maxWidth)
                maxWidth = nodeWidth;
        }

        return maxWidth;
    }
    /**
      * Responsible for creating a TreeStateNode that will be used
      * to track display information about value.
      */
    private TreeStateNode createNodeForValue(Object value) {
        return new TreeStateNode(value);
    }


    /**
     * TreeStateNode is used to keep track of each of
     * the nodes that have been expanded. This will also cache the preferred
     * size of the value it represents.
     */
    private class TreeStateNode extends DefaultMutableTreeNode {
        /** Preferred size needed to draw the user object. */
        protected int             preferredWidth;
        protected int             preferredHeight;

        /** X location that the user object will be drawn at. */
        protected int             xOrigin;

        /** Y location that the user object will be drawn at. */
        protected int             yOrigin;

        /** Is this node currently expanded? */
        protected boolean         expanded;

        /** Has this node been expanded at least once? */
        protected boolean         hasBeenExpanded;

        /** Path of this node. */
        protected TreePath        path;


        public TreeStateNode(Object value) {
            super(value);
        }

        //
        // Overriden DefaultMutableTreeNode methods
        //

        /**
         * Messaged when this node is added somewhere, resets the path
         * and adds a mapping from path to this node.
         */
        public void setParent(MutableTreeNode parent) {
            super.setParent(parent);
            if(parent != null) {
                path = ((TreeStateNode)parent).getTreePath().
                                       pathByAddingChild(getUserObject());
                addMapping(this);
            }
        }

        /**
         * Messaged when this node is removed from its parent, this messages
         * <code>removedFromMapping</code> to remove all the children.
         */
        public void remove(int childIndex) {
            TreeStateNode     node = (TreeStateNode)getChildAt(childIndex);

            node.removeFromMapping();
            super.remove(childIndex);
        }

        /**
         * Messaged to set the user object. This resets the path.
         */
        public void setUserObject(Object o) {
            super.setUserObject(o);
            if(path != null) {
                TreeStateNode      parent = (TreeStateNode)getParent();

                if(parent != null)
                    resetChildrenPaths(parent.getTreePath());
                else
                    resetChildrenPaths(null);
            }
        }

        /**
         * Returns the children of the receiver.
         * If the receiver is not currently expanded, this will return an
         * empty enumeration.
         */
        @Override
        public Enumeration<TreeNode> children() {
            if (!this.isExpanded()) {
                return DefaultMutableTreeNode.EMPTY_ENUMERATION;
            } else {
                return super.children();
            }
        }

        /**
         * Returns true if the receiver is a leaf.
         */
        public boolean isLeaf() {
            return getModel().isLeaf(this.getValue());
        }

        //
        // VariableHeightLayoutCache
        //

        /**
         * Returns the location and size of this node.
         */
        public Rectangle getNodeBounds(Rectangle placeIn) {
            if(placeIn == null)
                placeIn = new Rectangle(getXOrigin(), getYOrigin(),
                                        getPreferredWidth(),
                                        getPreferredHeight());
            else {
                placeIn.x = getXOrigin();
                placeIn.y = getYOrigin();
                placeIn.width = getPreferredWidth();
                placeIn.height = getPreferredHeight();
            }
            return placeIn;
        }

        /**
         * @return x location to draw node at.
         */
        public int getXOrigin() {
            if(!hasValidSize())
                updatePreferredSize(getRow());
            return xOrigin;
        }

        /**
         * Returns the y origin the user object will be drawn at.
         */
        public int getYOrigin() {
            if(isFixedRowHeight()) {
                int      aRow = getRow();

                if(aRow == -1)
                    return -1;
                return getRowHeight() * aRow;
            }
            return yOrigin;
        }

        /**
         * Returns the preferred height of the receiver.
         */
        public int getPreferredHeight() {
            if(isFixedRowHeight())
                return getRowHeight();
            else if(!hasValidSize())
                updatePreferredSize(getRow());
            return preferredHeight;
        }

        /**
         * Returns the preferred width of the receiver.
         */
        public int getPreferredWidth() {
            if(!hasValidSize())
                updatePreferredSize(getRow());
            return preferredWidth;
        }

        /**
         * Returns true if this node has a valid size.
         */
        public boolean hasValidSize() {
            return (preferredHeight != 0);
        }

        /**
         * Returns the row of the receiver.
         */
        public int getRow() {
            return visibleNodes.indexOf(this);
        }

        /**
         * Returns true if this node has been expanded at least once.
         */
        public boolean hasBeenExpanded() {
            return hasBeenExpanded;
        }

        /**
         * Returns true if the receiver has been expanded.
         */
        public boolean isExpanded() {
            return expanded;
        }

        /**
         * Returns the last visible node that is a child of this
         * instance.
         */
        public TreeStateNode getLastVisibleNode() {
            TreeStateNode                node = this;

            while(node.isExpanded() && node.getChildCount() > 0)
                node = (TreeStateNode)node.getLastChild();
            return node;
        }

        /**
         * Returns true if the receiver is currently visible.
         */
        public boolean isVisible() {
            if(this == root)
                return true;

            TreeStateNode        parent = (TreeStateNode)getParent();

            return (parent != null && parent.isExpanded() &&
                    parent.isVisible());
        }

        /**
         * Returns the number of children this will have. If the children
         * have not yet been loaded, this messages the model.
         */
        public int getModelChildCount() {
            if(hasBeenExpanded)
                return super.getChildCount();
            return getModel().getChildCount(getValue());
        }

        /**
         * Returns the number of visible children, that is the number of
         * children that are expanded, or leafs.
         */
        public int getVisibleChildCount() {
            int               childCount = 0;

            if(isExpanded()) {
                int         maxCounter = getChildCount();

                childCount += maxCounter;
                for(int counter = 0; counter < maxCounter; counter++)
                    childCount += ((TreeStateNode)getChildAt(counter)).
                                    getVisibleChildCount();
            }
            return childCount;
        }

        /**
         * Toggles the receiver between expanded and collapsed.
         */
        public void toggleExpanded() {
            if (isExpanded()) {
                collapse();
            } else {
                expand();
            }
        }

        /**
         * Makes the receiver visible, but invoking
         * <code>expandParentAndReceiver</code> on the superclass.
         */
        public void makeVisible() {
            TreeStateNode       parent = (TreeStateNode)getParent();

            if(parent != null)
                parent.expandParentAndReceiver();
        }

        /**
         * Expands the receiver.
         */
        public void expand() {
            expand(true);
        }

        /**
         * Collapses the receiver.
         */
        public void collapse() {
            collapse(true);
        }

        /**
         * Returns the value the receiver is representing. This is a cover
         * for getUserObject.
         */
        public Object getValue() {
            return getUserObject();
        }

        /**
         * Returns a TreePath instance for this node.
         */
        public TreePath getTreePath() {
            return path;
        }

        //
        // Local methods
        //

        /**
         * Recreates the receivers path, and all its children's paths.
         */
        protected void resetChildrenPaths(TreePath parentPath) {
            removeMapping(this);
            if(parentPath == null)
                path = new TreePath(getUserObject());
            else
                path = parentPath.pathByAddingChild(getUserObject());
            addMapping(this);
            for(int counter = getChildCount() - 1; counter >= 0; counter--)
                ((TreeStateNode)getChildAt(counter)).resetChildrenPaths(path);
        }

        /**
         * Sets y origin the user object will be drawn at to
         * <I>newYOrigin</I>.
         */
        protected void setYOrigin(int newYOrigin) {
            yOrigin = newYOrigin;
        }

        /**
         * Shifts the y origin by <code>offset</code>.
         */
        protected void shiftYOriginBy(int offset) {
            yOrigin += offset;
        }

        /**
         * Updates the receivers preferredSize by invoking
         * <code>updatePreferredSize</code> with an argument of -1.
         */
        protected void updatePreferredSize() {
            updatePreferredSize(getRow());
        }

        /**
         * Updates the preferred size by asking the current renderer
         * for the Dimension needed to draw the user object this
         * instance represents.
         */
        protected void updatePreferredSize(int index) {
            Rectangle       bounds = getNodeDimensions(this.getUserObject(),
                                                       index, getLevel(),
                                                       isExpanded(),
                                                       boundsBuffer);

            if(bounds == null || bounds.height == 0) {
                xOrigin = 0;
                preferredWidth = preferredHeight = 0;
                updateNodeSizes = true;
            } else {
                xOrigin = bounds.x;
                preferredWidth = bounds.width;
                if(isFixedRowHeight())
                    preferredHeight = getRowHeight();
                else
                    preferredHeight = bounds.height;
            }
        }

        /**
         * Marks the receivers size as invalid. Next time the size, location
         * is asked for it will be obtained.
         */
        protected void markSizeInvalid() {
            preferredHeight = 0;
        }

        /**
         * Marks the receivers size, and all its descendants sizes, as invalid.
         */
        protected void deepMarkSizeInvalid() {
            markSizeInvalid();
            for(int counter = getChildCount() - 1; counter >= 0; counter--)
                ((TreeStateNode)getChildAt(counter)).deepMarkSizeInvalid();
        }

        /**
         * Returns the children of the receiver. If the children haven't
         * been loaded from the model and
         * <code>createIfNeeded</code> is true, the children are first
         * loaded.
         */
        protected Enumeration<TreeNode> getLoadedChildren(boolean createIfNeeded) {
            if(!createIfNeeded || hasBeenExpanded)
                return super.children();

            TreeStateNode   newNode;
            Object          realNode = getValue();
            TreeModel       treeModel = getModel();
            int             count = treeModel.getChildCount(realNode);

            hasBeenExpanded = true;

            int    childRow = getRow();

            if(childRow == -1) {
                for (int i = 0; i < count; i++) {
                    newNode = createNodeForValue
                        (treeModel.getChild(realNode, i));
                    this.add(newNode);
                    newNode.updatePreferredSize(-1);
                }
            }
            else {
                childRow++;
                for (int i = 0; i < count; i++) {
                    newNode = createNodeForValue
                        (treeModel.getChild(realNode, i));
                    this.add(newNode);
                    newNode.updatePreferredSize(childRow++);
                }
            }
            return super.children();
        }

        /**
         * Messaged from expand and collapse. This is meant for subclassers
         * that may wish to do something interesting with this.
         */
        protected void didAdjustTree() {
        }

        /**
         * Invokes <code>expandParentAndReceiver</code> on the parent,
         * and expands the receiver.
         */
        protected void expandParentAndReceiver() {
            TreeStateNode       parent = (TreeStateNode)getParent();

            if(parent != null)
                parent.expandParentAndReceiver();
            expand();
        }

        /**
         * Expands this node in the tree.  This will load the children
         * from the treeModel if this node has not previously been
         * expanded.  If <I>adjustTree</I> is true the tree and selection
         * are updated accordingly.
         */
        protected void expand(boolean adjustTree) {
            if (!isExpanded() && !isLeaf()) {
                boolean         isFixed = isFixedRowHeight();
                int             startHeight = getPreferredHeight();
                int             originalRow = getRow();

                expanded = true;
                updatePreferredSize(originalRow);

                if (!hasBeenExpanded) {
                    TreeStateNode  newNode;
                    Object         realNode = getValue();
                    TreeModel      treeModel = getModel();
                    int            count = treeModel.getChildCount(realNode);
                    int offset = originalRow == -1 ? -1 : originalRow + 1;
                    hasBeenExpanded = true;

                    for (int i = 0; i < count; i++) {
                        newNode = createNodeForValue(treeModel.getChild
                                                        (realNode, i));
                        this.add(newNode);
                        newNode.updatePreferredSize(offset);
                    }
                }

                int i = originalRow;
                Enumeration<TreeNode> cursor = preorderEnumeration();
                cursor.nextElement(); // don't add me, I'm already in

                int newYOrigin = isFixed || (this == root && !isRootVisible()) ?
                                    0 : getYOrigin() + this.getPreferredHeight();

                TreeStateNode   aNode;
                if(!isFixed) {
                    while (cursor.hasMoreElements()) {
                        aNode = (TreeStateNode) cursor.nextElement();
                        if(!updateNodeSizes && !aNode.hasValidSize())
                            aNode.updatePreferredSize(i + 1);
                        aNode.setYOrigin(newYOrigin);
                        newYOrigin += aNode.getPreferredHeight();
                        visibleNodes.insertElementAt(aNode, ++i);
                    }
                }
                else {
                    while (cursor.hasMoreElements()) {
                        aNode = (TreeStateNode) cursor.nextElement();
                        visibleNodes.insertElementAt(aNode, ++i);
                    }
                }

                if(adjustTree && (originalRow != i ||
                                  getPreferredHeight() != startHeight)) {
                    // Adjust the Y origin of any nodes following this row.
                    if(!isFixed && ++i < getRowCount()) {
                        int              counter;
                        int              heightDiff = newYOrigin -
                            (getYOrigin() + getPreferredHeight()) +
                            (getPreferredHeight() - startHeight);

                        for(counter = visibleNodes.size() - 1;counter >= i;
                            counter--)
                            ((TreeStateNode)visibleNodes.elementAt(counter)).
                                shiftYOriginBy(heightDiff);
                    }
                    didAdjustTree();
                    visibleNodesChanged();
                }

                // Update the rows in the selection
                if(treeSelectionModel != null) {
                    treeSelectionModel.resetRowSelection();
                }
            }
        }

        /**
         * Collapses this node in the tree.  If <I>adjustTree</I> is
         * true the tree and selection are updated accordingly.
         */
        protected void collapse(boolean adjustTree) {
            if (isExpanded()) {
                Enumeration<TreeNode> cursor = preorderEnumeration();
                cursor.nextElement(); // don't remove me, I'm still visible
                int rowsDeleted = 0;
                boolean isFixed = isFixedRowHeight();
                int lastYEnd;
                if(isFixed)
                    lastYEnd = 0;
                else
                    lastYEnd = getPreferredHeight() + getYOrigin();
                int startHeight = getPreferredHeight();
                int startYEnd = lastYEnd;
                int myRow = getRow();

                if(!isFixed) {
                    while(cursor.hasMoreElements()) {
                        TreeStateNode node = (TreeStateNode)cursor.
                            nextElement();
                        if (node.isVisible()) {
                            rowsDeleted++;
                            //visibleNodes.removeElement(node);
                            lastYEnd = node.getYOrigin() +
                                node.getPreferredHeight();
                        }
                    }
                }
                else {
                    while(cursor.hasMoreElements()) {
                        TreeStateNode node = (TreeStateNode)cursor.
                            nextElement();
                        if (node.isVisible()) {
                            rowsDeleted++;
                            //visibleNodes.removeElement(node);
                        }
                    }
                }

                // Clean up the visible nodes.
                for (int counter = rowsDeleted + myRow; counter > myRow;
                     counter--) {
                    visibleNodes.removeElementAt(counter);
                }

                expanded = false;

                if(myRow == -1)
                    markSizeInvalid();
                else if (adjustTree)
                    updatePreferredSize(myRow);

                if(myRow != -1 && adjustTree &&
                   (rowsDeleted > 0 || startHeight != getPreferredHeight())) {
                    // Adjust the Y origin of any rows following this one.
                    startYEnd += (getPreferredHeight() - startHeight);
                    if(!isFixed && (myRow + 1) < getRowCount() &&
                       startYEnd != lastYEnd) {
                        int                 counter, maxCounter, shiftAmount;

                        shiftAmount = startYEnd - lastYEnd;
                        for(counter = myRow + 1, maxCounter =
                                visibleNodes.size();
                            counter < maxCounter;counter++)
                            ((TreeStateNode)visibleNodes.elementAt(counter))
                                .shiftYOriginBy(shiftAmount);
                    }
                    didAdjustTree();
                    visibleNodesChanged();
                }
                if(treeSelectionModel != null && rowsDeleted > 0 &&
                   myRow != -1) {
                    treeSelectionModel.resetRowSelection();
                }
            }
        }

        /**
         * Removes the receiver, and all its children, from the mapping
         * table.
         */
        protected void removeFromMapping() {
            if(path != null) {
                removeMapping(this);
                for(int counter = getChildCount() - 1; counter >= 0; counter--)
                    ((TreeStateNode)getChildAt(counter)).removeFromMapping();
            }
        }
    } // End of VariableHeightLayoutCache.TreeStateNode


    /**
     * An enumerator to iterate through visible nodes.
     */
    private class VisibleTreeStateNodeEnumeration implements
                     Enumeration<TreePath> {
        /** Parent thats children are being enumerated. */
        protected TreeStateNode       parent;
        /** Index of next child. An index of -1 signifies parent should be
         * visibled next. */
        protected int                 nextIndex;
        /** Number of children in parent. */
        protected int                 childCount;

        protected VisibleTreeStateNodeEnumeration(TreeStateNode node) {
            this(node, -1);
        }

        protected VisibleTreeStateNodeEnumeration(TreeStateNode parent,
                                                  int startIndex) {
            this.parent = parent;
            this.nextIndex = startIndex;
            this.childCount = this.parent.getChildCount();
        }

        /**
         * @return true if more visible nodes.
         */
        public boolean hasMoreElements() {
            return (parent != null);
        }

        /**
         * @return next visible TreePath.
         */
        public TreePath nextElement() {
            if(!hasMoreElements())
                throw new NoSuchElementException("No more visible paths");

            TreePath                retObject;

            if(nextIndex == -1) {
                retObject = parent.getTreePath();
            }
            else {
                TreeStateNode   node = (TreeStateNode)parent.
                                        getChildAt(nextIndex);

                retObject = node.getTreePath();
            }
            updateNextObject();
            return retObject;
        }

        /**
         * Determines the next object by invoking <code>updateNextIndex</code>
         * and if not succesful <code>findNextValidParent</code>.
         */
        protected void updateNextObject() {
            if(!updateNextIndex()) {
                findNextValidParent();
            }
        }

        /**
         * Finds the next valid parent, this should be called when nextIndex
         * is beyond the number of children of the current parent.
         */
        protected boolean findNextValidParent() {
            if(parent == root) {
                // mark as invalid!
                parent = null;
                return false;
            }
            while(parent != null) {
                TreeStateNode      newParent = (TreeStateNode)parent.
                                                  getParent();

                if(newParent != null) {
                    nextIndex = newParent.getIndex(parent);
                    parent = newParent;
                    childCount = parent.getChildCount();
                    if(updateNextIndex())
                        return true;
                }
                else
                    parent = null;
            }
            return false;
        }

        /**
         * Updates <code>nextIndex</code> returning false if it is beyond
         * the number of children of parent.
         */
        protected boolean updateNextIndex() {
            // nextIndex == -1 identifies receiver, make sure is expanded
            // before descend.
            if((nextIndex == -1 && !parent.isExpanded()) ||
                childCount == 0 || // Check that it can have kids
                ++nextIndex >= childCount) // Make sure next index not beyond
                                             // child count.
                return false;

            TreeStateNode       child = (TreeStateNode)parent.
                                        getChildAt(nextIndex);

            if(child != null && child.isExpanded()) {
                parent = child;
                nextIndex = -1;
                childCount = child.getChildCount();
            }
            return true;
        }
    } // VariableHeightLayoutCache.VisibleTreeStateNodeEnumeration
}
