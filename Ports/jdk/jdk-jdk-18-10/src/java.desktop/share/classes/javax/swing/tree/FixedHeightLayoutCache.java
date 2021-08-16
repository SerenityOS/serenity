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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Stack;

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
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Same-version serialization only
public class FixedHeightLayoutCache extends AbstractLayoutCache {
    /** Root node. */
    private FHTreeStateNode    root;

    /** Number of rows currently visible. */
    private int                rowCount;

    /**
     * Used in getting sizes for nodes to avoid creating a new Rectangle
     * every time a size is needed.
     */
    private Rectangle          boundsBuffer;

    /**
     * Maps from TreePath to a FHTreeStateNode.
     */
    private Hashtable<TreePath, FHTreeStateNode> treePathMapping;

    /**
     * Used for getting path/row information.
     */
    private SearchInfo         info;

    private Stack<Stack<TreePath>> tempStacks;

    /**
     * Constructs a {@code FixedHeightLayoutCache}.
     */
    public FixedHeightLayoutCache() {
        super();
        tempStacks = new Stack<Stack<TreePath>>();
        boundsBuffer = new Rectangle();
        treePathMapping = new Hashtable<TreePath, FHTreeStateNode>();
        info = new SearchInfo();
        setRowHeight(1);
    }

    /**
     * Sets the TreeModel that will provide the data.
     *
     * @param newModel the TreeModel that is to provide the data
     */
    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
        rebuild(false);
    }

    /**
     * Determines whether or not the root node from
     * the TreeModel is visible.
     *
     * @param rootVisible true if the root node of the tree is to be displayed
     * @see #rootVisible
     */
    public void setRootVisible(boolean rootVisible) {
        if(isRootVisible() != rootVisible) {
            super.setRootVisible(rootVisible);
            if(root != null) {
                if(rootVisible) {
                    rowCount++;
                    root.adjustRowBy(1);
                }
                else {
                    rowCount--;
                    root.adjustRowBy(-1);
                }
                visibleNodesChanged();
            }
        }
    }

    /**
     * Sets the height of each cell. If rowHeight is less than or equal to
     * 0 this will throw an IllegalArgumentException.
     *
     * @param rowHeight the height of each cell, in pixels
     */
    public void setRowHeight(int rowHeight) {
        if(rowHeight <= 0)
            throw new IllegalArgumentException("FixedHeightLayoutCache only supports row heights greater than 0");
        if(getRowHeight() != rowHeight) {
            super.setRowHeight(rowHeight);
            visibleNodesChanged();
        }
    }

    /**
     * Returns the number of visible rows.
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * Does nothing, FixedHeightLayoutCache doesn't cache width, and that
     * is all that could change.
     */
    public void invalidatePathBounds(TreePath path) {
    }


    /**
     * Informs the TreeState that it needs to recalculate all the sizes
     * it is referencing.
     */
    public void invalidateSizes() {
        // Nothing to do here, rowHeight still same, which is all
        // this is interested in, visible region may have changed though.
        visibleNodesChanged();
    }

    /**
      * Returns true if the value identified by row is currently expanded.
      */
    public boolean isExpanded(TreePath path) {
        if(path != null) {
            FHTreeStateNode     lastNode = getNodeForPath(path, true, false);

            return (lastNode != null && lastNode.isExpanded());
        }
        return false;
    }

    /**
     * Returns a rectangle giving the bounds needed to draw path.
     *
     * @param path     a TreePath specifying a node
     * @param placeIn  a Rectangle object giving the available space
     * @return a Rectangle object specifying the space to be used
     */
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        if(path == null)
            return null;

        FHTreeStateNode      node = getNodeForPath(path, true, false);

        if(node != null)
            return getBounds(node, -1, placeIn);

        // node hasn't been created yet.
        TreePath       parentPath = path.getParentPath();

        node = getNodeForPath(parentPath, true, false);
        if (node != null && node.isExpanded()) {
            int              childIndex = treeModel.getIndexOfChild
                                 (parentPath.getLastPathComponent(),
                                  path.getLastPathComponent());

            if(childIndex != -1)
                return getBounds(node, childIndex, placeIn);
        }
        return null;
    }

    /**
      * Returns the path for passed in row.  If row is not visible
      * null is returned.
      */
    public TreePath getPathForRow(int row) {
        if(row >= 0 && row < getRowCount()) {
            if(root.getPathForRow(row, getRowCount(), info)) {
                return info.getPath();
            }
        }
        return null;
    }

    /**
      * Returns the row that the last item identified in path is visible
      * at.  Will return -1 if any of the elements in path are not
      * currently visible.
      */
    public int getRowForPath(TreePath path) {
        if(path == null || root == null)
            return -1;

        FHTreeStateNode         node = getNodeForPath(path, true, false);

        if(node != null)
            return node.getRow();

        TreePath       parentPath = path.getParentPath();

        node = getNodeForPath(parentPath, true, false);
        if(node != null && node.isExpanded()) {
            return node.getRowToModelIndex(treeModel.getIndexOfChild
                                           (parentPath.getLastPathComponent(),
                                            path.getLastPathComponent()));
        }
        return -1;
    }

    /**
      * Returns the path to the node that is closest to x,y.  If
      * there is nothing currently visible this will return null, otherwise
      * it'll always return a valid path.  If you need to test if the
      * returned object is exactly at x, y you should get the bounds for
      * the returned path and test x, y against that.
      */
    public TreePath getPathClosestTo(int x, int y) {
        if(getRowCount() == 0)
            return null;

        int                row = getRowContainingYLocation(y);

        return getPathForRow(row);
    }

    /**
     * Returns the number of visible children for row.
     */
    public int getVisibleChildCount(TreePath path) {
        FHTreeStateNode         node = getNodeForPath(path, true, false);

        if(node == null)
            return 0;
        return node.getTotalChildCount();
    }

    /**
     * Returns an Enumerator that increments over the visible paths
     * starting at the passed in location. The ordering of the enumeration
     * is based on how the paths are displayed.
     */
    public Enumeration<TreePath> getVisiblePathsFrom(TreePath path) {
        if(path == null)
            return null;

        FHTreeStateNode         node = getNodeForPath(path, true, false);

        if(node != null) {
            return new VisibleFHTreeStateNodeEnumeration(node);
        }
        TreePath            parentPath = path.getParentPath();

        node = getNodeForPath(parentPath, true, false);
        if(node != null && node.isExpanded()) {
            return new VisibleFHTreeStateNodeEnumeration(node,
                  treeModel.getIndexOfChild(parentPath.getLastPathComponent(),
                                            path.getLastPathComponent()));
        }
        return null;
    }

    /**
     * Marks the path <code>path</code> expanded state to
     * <code>isExpanded</code>.
     */
    public void setExpandedState(TreePath path, boolean isExpanded) {
        if(isExpanded)
            ensurePathIsExpanded(path, true);
        else if(path != null) {
            TreePath              parentPath = path.getParentPath();

            // YECK! Make the parent expanded.
            if(parentPath != null) {
                FHTreeStateNode     parentNode = getNodeForPath(parentPath,
                                                                false, true);
                if(parentNode != null)
                    parentNode.makeVisible();
            }
            // And collapse the child.
            FHTreeStateNode         childNode = getNodeForPath(path, true,
                                                               false);

            if(childNode != null)
                childNode.collapse(true);
        }
    }

    /**
     * Returns true if the path is expanded, and visible.
     */
    public boolean getExpandedState(TreePath path) {
        FHTreeStateNode       node = getNodeForPath(path, true, false);

        return (node != null) ? (node.isVisible() && node.isExpanded()) :
                                 false;
    }

    //
    // TreeModelListener methods
    //

    /**
     * <p>Invoked after a node (or a set of siblings) has changed in some
     * way. The node(s) have not changed locations in the tree or
     * altered their children arrays, but other attributes have
     * changed and may affect presentation. Example: the name of a
     * file has changed, but it is in the same location in the file
     * system.</p>
     *
     * <p>e.path() returns the path the parent of the changed node(s).</p>
     *
     * <p>e.childIndices() returns the index(es) of the changed node(s).</p>
     */
    public void treeNodesChanged(TreeModelEvent e) {
        if(e != null) {
            int[]               changedIndexs;
            FHTreeStateNode     changedParent = getNodeForPath
                                  (SwingUtilities2.getTreePath(e, getModel()), false, false);
            int                 maxCounter;

            changedIndexs = e.getChildIndices();
            /* Only need to update the children if the node has been
               expanded once. */
            // PENDING(scott): make sure childIndexs is sorted!
            if (changedParent != null) {
                if (changedIndexs != null &&
                    (maxCounter = changedIndexs.length) > 0) {
                    Object       parentValue = changedParent.getUserObject();

                    for(int counter = 0; counter < maxCounter; counter++) {
                        FHTreeStateNode    child = changedParent.
                                 getChildAtModelIndex(changedIndexs[counter]);

                        if(child != null) {
                            child.setUserObject(treeModel.getChild(parentValue,
                                                     changedIndexs[counter]));
                        }
                    }
                    if(changedParent.isVisible() && changedParent.isExpanded())
                        visibleNodesChanged();
                }
                // Null for root indicates it changed.
                else if (changedParent == root && changedParent.isVisible() &&
                         changedParent.isExpanded()) {
                    visibleNodesChanged();
                }
            }
        }
    }

    /**
     * <p>Invoked after nodes have been inserted into the tree.</p>
     *
     * <p>e.path() returns the parent of the new nodes
     * <p>e.childIndices() returns the indices of the new nodes in
     * ascending order.
     */
    public void treeNodesInserted(TreeModelEvent e) {
        if(e != null) {
            int[]               changedIndexs;
            FHTreeStateNode     changedParent = getNodeForPath
                                  (SwingUtilities2.getTreePath(e, getModel()), false, false);
            int                 maxCounter;

            changedIndexs = e.getChildIndices();
            /* Only need to update the children if the node has been
               expanded once. */
            // PENDING(scott): make sure childIndexs is sorted!
            if(changedParent != null && changedIndexs != null &&
               (maxCounter = changedIndexs.length) > 0) {
                boolean          isVisible =
                    (changedParent.isVisible() &&
                     changedParent.isExpanded());

                for(int counter = 0; counter < maxCounter; counter++) {
                    changedParent.childInsertedAtModelIndex
                        (changedIndexs[counter], isVisible);
                }
                if(isVisible && treeSelectionModel != null)
                    treeSelectionModel.resetRowSelection();
                if(changedParent.isVisible())
                    this.visibleNodesChanged();
            }
        }
    }

    /**
     * <p>Invoked after nodes have been removed from the tree.  Note that
     * if a subtree is removed from the tree, this method may only be
     * invoked once for the root of the removed subtree, not once for
     * each individual set of siblings removed.</p>
     *
     * <p>e.path() returns the former parent of the deleted nodes.</p>
     *
     * <p>e.childIndices() returns the indices the nodes had before they were deleted in ascending order.</p>
     */
    public void treeNodesRemoved(TreeModelEvent e) {
        if(e != null) {
            int[]                changedIndexs;
            int                  maxCounter;
            TreePath             parentPath = SwingUtilities2.getTreePath(e, getModel());
            FHTreeStateNode      changedParentNode = getNodeForPath
                                       (parentPath, false, false);

            changedIndexs = e.getChildIndices();
            // PENDING(scott): make sure that changedIndexs are sorted in
            // ascending order.
            if(changedParentNode != null && changedIndexs != null &&
               (maxCounter = changedIndexs.length) > 0) {
                Object[]           children = e.getChildren();
                boolean            isVisible =
                    (changedParentNode.isVisible() &&
                     changedParentNode.isExpanded());

                for(int counter = maxCounter - 1; counter >= 0; counter--) {
                    changedParentNode.removeChildAtModelIndex
                                     (changedIndexs[counter], isVisible);
                }
                if(isVisible) {
                    if(treeSelectionModel != null)
                        treeSelectionModel.resetRowSelection();
                    if (treeModel.getChildCount(changedParentNode.
                                                getUserObject()) == 0 &&
                                  changedParentNode.isLeaf()) {
                        // Node has become a leaf, collapse it.
                        changedParentNode.collapse(false);
                    }
                    visibleNodesChanged();
                }
                else if(changedParentNode.isVisible())
                    visibleNodesChanged();
            }
        }
    }

    /**
     * <p>Invoked after the tree has drastically changed structure from a
     * given node down.  If the path returned by e.getPath() is of length
     * one and the first element does not identify the current root node
     * the first element should become the new root of the tree.
     *
     * <p>e.path() holds the path to the node.</p>
     * <p>e.childIndices() returns null.</p>
     */
    public void treeStructureChanged(TreeModelEvent e) {
        if(e != null) {
            TreePath          changedPath = SwingUtilities2.getTreePath(e, getModel());
            FHTreeStateNode   changedNode = getNodeForPath
                                                (changedPath, false, false);

            // Check if root has changed, either to a null root, or
            // to an entirely new root.
            if (changedNode == root ||
                (changedNode == null &&
                 ((changedPath == null && treeModel != null &&
                   treeModel.getRoot() == null) ||
                  (changedPath != null && changedPath.getPathCount() <= 1)))) {
                rebuild(true);
            }
            else if(changedNode != null) {
                boolean             wasExpanded, wasVisible;
                FHTreeStateNode     parent = (FHTreeStateNode)
                                              changedNode.getParent();

                wasExpanded = changedNode.isExpanded();
                wasVisible = changedNode.isVisible();

                int index = parent.getIndex(changedNode);
                changedNode.collapse(false);
                parent.remove(index);

                if(wasVisible && wasExpanded) {
                    int row = changedNode.getRow();
                    parent.resetChildrenRowsFrom(row, index,
                                                 changedNode.getChildIndex());
                    changedNode = getNodeForPath(changedPath, false, true);
                    changedNode.expand();
                }
                if(treeSelectionModel != null && wasVisible && wasExpanded)
                    treeSelectionModel.resetRowSelection();
                if(wasVisible)
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
     * Returns the bounds for the given node. If <code>childIndex</code>
     * is -1, the bounds of <code>parent</code> are returned, otherwise
     * the bounds of the node at <code>childIndex</code> are returned.
     */
    private Rectangle getBounds(FHTreeStateNode parent, int childIndex,
                                  Rectangle placeIn) {
        boolean              expanded;
        int                  level;
        int                  row;
        Object               value;

        if(childIndex == -1) {
            // Getting bounds for parent
            row = parent.getRow();
            value = parent.getUserObject();
            expanded = parent.isExpanded();
            level = parent.getLevel();
        }
        else {
            row = parent.getRowToModelIndex(childIndex);
            value = treeModel.getChild(parent.getUserObject(), childIndex);
            expanded = false;
            level = parent.getLevel() + 1;
        }

        Rectangle      bounds = getNodeDimensions(value, row, level,
                                                  expanded, boundsBuffer);
        // No node dimensions, bail.
        if(bounds == null)
            return null;

        if(placeIn == null)
            placeIn = new Rectangle();

        placeIn.x = bounds.x;
        placeIn.height = getRowHeight();
        placeIn.y = row * placeIn.height;
        placeIn.width = bounds.width;
        return placeIn;
    }

    /**
     * Adjust the large row count of the AbstractTreeUI the receiver was
     * created with.
     */
    private void adjustRowCountBy(int changeAmount) {
        rowCount += changeAmount;
    }

    /**
     * Adds a mapping for node.
     */
    private void addMapping(FHTreeStateNode node) {
        treePathMapping.put(node.getTreePath(), node);
    }

    /**
     * Removes the mapping for a previously added node.
     */
    private void removeMapping(FHTreeStateNode node) {
        treePathMapping.remove(node.getTreePath());
    }

    /**
     * Returns the node previously added for <code>path</code>. This may
     * return null, if you to create a node use getNodeForPath.
     */
    private FHTreeStateNode getMapping(TreePath path) {
        return treePathMapping.get(path);
    }

    /**
     * Sent to completely rebuild the visible tree. All nodes are collapsed.
     */
    private void rebuild(boolean clearSelection) {
        Object            rootUO;

        treePathMapping.clear();
        if(treeModel != null && (rootUO = treeModel.getRoot()) != null) {
            root = createNodeForValue(rootUO, 0);
            root.path = new TreePath(rootUO);
            addMapping(root);
            if(isRootVisible()) {
                rowCount = 1;
                root.row = 0;
            }
            else {
                rowCount = 0;
                root.row = -1;
            }
            root.expand();
        }
        else {
            root = null;
            rowCount = 0;
        }
        if(clearSelection && treeSelectionModel != null) {
            treeSelectionModel.clearSelection();
        }
        this.visibleNodesChanged();
    }

    /**
      * Returns the index of the row containing location.  If there
      * are no rows, -1 is returned.  If location is beyond the last
      * row index, the last row index is returned.
      */
    private int getRowContainingYLocation(int location) {
        if(getRowCount() == 0)
            return -1;
        return Math.max(0, Math.min(getRowCount() - 1,
                                    location / getRowHeight()));
    }

    /**
     * Ensures that all the path components in path are expanded, accept
     * for the last component which will only be expanded if expandLast
     * is true.
     * Returns true if succesful in finding the path.
     */
    private boolean ensurePathIsExpanded(TreePath aPath,
                                           boolean expandLast) {
        if(aPath != null) {
            // Make sure the last entry isn't a leaf.
            if(treeModel.isLeaf(aPath.getLastPathComponent())) {
                aPath = aPath.getParentPath();
                expandLast = true;
            }
            if(aPath != null) {
                FHTreeStateNode     lastNode = getNodeForPath(aPath, false,
                                                              true);

                if(lastNode != null) {
                    lastNode.makeVisible();
                    if(expandLast)
                        lastNode.expand();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates and returns an instance of FHTreeStateNode.
     */
    private FHTreeStateNode createNodeForValue(Object value,int childIndex) {
        return new FHTreeStateNode(value, childIndex, -1);
    }

    /**
     * Messages getTreeNodeForPage(path, onlyIfVisible, shouldCreate,
     * path.length) as long as path is non-null and the length is {@literal >} 0.
     * Otherwise returns null.
     */
    private FHTreeStateNode getNodeForPath(TreePath path,
                                             boolean onlyIfVisible,
                                             boolean shouldCreate) {
        if(path != null) {
            FHTreeStateNode      node;

            node = getMapping(path);
            if(node != null) {
                if(onlyIfVisible && !node.isVisible())
                    return null;
                return node;
            }
            if(onlyIfVisible)
                return null;

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
                            node = node.createChildFor(path.
                                                       getLastPathComponent());
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
            return null;
        }
        return null;
    }

    /**
     * FHTreeStateNode is used to track what has been expanded.
     * FHTreeStateNode differs from VariableHeightTreeState.TreeStateNode
     * in that it is highly model intensive. That is almost all queries to a
     * FHTreeStateNode result in the TreeModel being queried. And it
     * obviously does not support variable sized row heights.
     */
    private class FHTreeStateNode extends DefaultMutableTreeNode {
        /** Is this node expanded? */
        protected boolean         isExpanded;

        /** Index of this node from the model. */
        protected int             childIndex;

        /** Child count of the receiver. */
        protected int             childCount;

        /** Row of the receiver. This is only valid if the row is expanded.
         */
        protected int             row;

        /** Path of this node. */
        protected TreePath        path;


        public FHTreeStateNode(Object userObject, int childIndex, int row) {
            super(userObject);
            this.childIndex = childIndex;
            this.row = row;
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
                path = ((FHTreeStateNode)parent).getTreePath().
                            pathByAddingChild(getUserObject());
                addMapping(this);
            }
        }

        /**
         * Messaged when this node is removed from its parent, this messages
         * <code>removedFromMapping</code> to remove all the children.
         */
        public void remove(int childIndex) {
            FHTreeStateNode     node = (FHTreeStateNode)getChildAt(childIndex);

            node.removeFromMapping();
            super.remove(childIndex);
        }

        /**
         * Messaged to set the user object. This resets the path.
         */
        public void setUserObject(Object o) {
            super.setUserObject(o);
            if(path != null) {
                FHTreeStateNode      parent = (FHTreeStateNode)getParent();

                if(parent != null)
                    resetChildrenPaths(parent.getTreePath());
                else
                    resetChildrenPaths(null);
            }
        }

        //
        //

        /**
         * Returns the index of the receiver in the model.
         */
        public int getChildIndex() {
            return childIndex;
        }

        /**
         * Returns the <code>TreePath</code> of the receiver.
         */
        public TreePath getTreePath() {
            return path;
        }

        /**
         * Returns the child for the passed in model index, this will
         * return <code>null</code> if the child for <code>index</code>
         * has not yet been created (expanded).
         */
        public FHTreeStateNode getChildAtModelIndex(int index) {
            // PENDING: Make this a binary search!
            for(int counter = getChildCount() - 1; counter >= 0; counter--)
                if(((FHTreeStateNode)getChildAt(counter)).childIndex == index)
                    return (FHTreeStateNode)getChildAt(counter);
            return null;
        }

        /**
         * Returns true if this node is visible. This is determined by
         * asking all the parents if they are expanded.
         */
        public boolean isVisible() {
            FHTreeStateNode         parent = (FHTreeStateNode)getParent();

            if(parent == null)
                return true;
            return (parent.isExpanded() && parent.isVisible());
        }

        /**
         * Returns the row of the receiver.
         */
        public int getRow() {
            return row;
        }

        /**
         * Returns the row of the child with a model index of
         * <code>index</code>.
         */
        public int getRowToModelIndex(int index) {
            FHTreeStateNode      child;
            int                  lastRow = getRow() + 1;
            int                  retValue = lastRow;

            // This too could be a binary search!
            for(int counter = 0, maxCounter = getChildCount();
                counter < maxCounter; counter++) {
                child = (FHTreeStateNode)getChildAt(counter);
                if(child.childIndex >= index) {
                    if(child.childIndex == index)
                        return child.row;
                    if(counter == 0)
                        return getRow() + 1 + index;
                    return child.row - (child.childIndex - index);
                }
            }
            // YECK!
            return getRow() + 1 + getTotalChildCount() -
                             (childCount - index);
        }

        /**
         * Returns the number of children in the receiver by descending all
         * expanded nodes and messaging them with getTotalChildCount.
         */
        public int getTotalChildCount() {
            if(isExpanded()) {
                FHTreeStateNode      parent = (FHTreeStateNode)getParent();
                int                  pIndex;

                if(parent != null && (pIndex = parent.getIndex(this)) + 1 <
                   parent.getChildCount()) {
                    // This node has a created sibling, to calc total
                    // child count directly from that!
                    FHTreeStateNode  nextSibling = (FHTreeStateNode)parent.
                                           getChildAt(pIndex + 1);

                    return nextSibling.row - row -
                           (nextSibling.childIndex - childIndex);
                }
                else {
                    int retCount = childCount;

                    for(int counter = getChildCount() - 1; counter >= 0;
                        counter--) {
                        retCount += ((FHTreeStateNode)getChildAt(counter))
                                                  .getTotalChildCount();
                    }
                    return retCount;
                }
            }
            return 0;
        }

        /**
         * Returns true if this node is expanded.
         */
        public boolean isExpanded() {
            return isExpanded;
        }

        /**
         * The highest visible nodes have a depth of 0.
         */
        public int getVisibleLevel() {
            if (isRootVisible()) {
                return getLevel();
            } else {
                return getLevel() - 1;
            }
        }

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
                ((FHTreeStateNode)getChildAt(counter)).
                               resetChildrenPaths(path);
        }

        /**
         * Removes the receiver, and all its children, from the mapping
         * table.
         */
        protected void removeFromMapping() {
            if(path != null) {
                removeMapping(this);
                for(int counter = getChildCount() - 1; counter >= 0; counter--)
                    ((FHTreeStateNode)getChildAt(counter)).removeFromMapping();
            }
        }

        /**
         * Creates a new node to represent <code>userObject</code>.
         * This does NOT check to ensure there isn't already a child node
         * to manage <code>userObject</code>.
         */
        protected FHTreeStateNode createChildFor(Object userObject) {
            int      newChildIndex = treeModel.getIndexOfChild
                                     (getUserObject(), userObject);

            if(newChildIndex < 0)
                return null;

            FHTreeStateNode     aNode;
            FHTreeStateNode     child = createNodeForValue(userObject,
                                                           newChildIndex);
            int                 childRow;

            if(isVisible()) {
                childRow = getRowToModelIndex(newChildIndex);
            }
            else {
                childRow = -1;
            }
            child.row = childRow;
            for(int counter = 0, maxCounter = getChildCount();
                counter < maxCounter; counter++) {
                aNode = (FHTreeStateNode)getChildAt(counter);
                if(aNode.childIndex > newChildIndex) {
                    insert(child, counter);
                    return child;
                }
            }
            add(child);
            return child;
        }

        /**
         * Adjusts the receiver, and all its children rows by
         * <code>amount</code>.
         */
        protected void adjustRowBy(int amount) {
            row += amount;
            if(isExpanded) {
                for(int counter = getChildCount() - 1; counter >= 0;
                    counter--)
                    ((FHTreeStateNode)getChildAt(counter)).adjustRowBy(amount);
            }
        }

        /**
         * Adjusts this node, its child, and its parent starting at
         * an index of <code>index</code> index is the index of the child
         * to start adjusting from, which is not necessarily the model
         * index.
         */
        protected void adjustRowBy(int amount, int startIndex) {
            // Could check isVisible, but probably isn't worth it.
            if(isExpanded) {
                // children following startIndex.
                for(int counter = getChildCount() - 1; counter >= startIndex;
                    counter--)
                    ((FHTreeStateNode)getChildAt(counter)).adjustRowBy(amount);
            }
            // Parent
            FHTreeStateNode        parent = (FHTreeStateNode)getParent();

            if(parent != null) {
                parent.adjustRowBy(amount, parent.getIndex(this) + 1);
            }
        }

        /**
         * Messaged when the node has expanded. This updates all of
         * the receivers children rows, as well as the total row count.
         */
        protected void didExpand() {
            int               nextRow = setRowAndChildren(row);
            FHTreeStateNode   parent = (FHTreeStateNode)getParent();
            int               childRowCount = nextRow - row - 1;

            if(parent != null) {
                parent.adjustRowBy(childRowCount, parent.getIndex(this) + 1);
            }
            adjustRowCountBy(childRowCount);
        }

        /**
         * Sets the receivers row to <code>nextRow</code> and recursively
         * updates all the children of the receivers rows. The index the
         * next row is to be placed as is returned.
         */
        protected int setRowAndChildren(int nextRow) {
            row = nextRow;

            if(!isExpanded())
                return row + 1;

            int              lastRow = row + 1;
            int              lastModelIndex = 0;
            FHTreeStateNode  child;
            int              maxCounter = getChildCount();

            for(int counter = 0; counter < maxCounter; counter++) {
                child = (FHTreeStateNode)getChildAt(counter);
                lastRow += (child.childIndex - lastModelIndex);
                lastModelIndex = child.childIndex + 1;
                if(child.isExpanded) {
                    lastRow = child.setRowAndChildren(lastRow);
                }
                else {
                    child.row = lastRow++;
                }
            }
            return lastRow + childCount - lastModelIndex;
        }

        /**
         * Resets the receivers children's rows. Starting with the child
         * at <code>childIndex</code> (and <code>modelIndex</code>) to
         * <code>newRow</code>. This uses <code>setRowAndChildren</code>
         * to recursively descend children, and uses
         * <code>resetRowSelection</code> to ascend parents.
         */
        // This can be rather expensive, but is needed for the collapse
        // case this is resulting from a remove (although I could fix
        // that by having instances of FHTreeStateNode hold a ref to
        // the number of children). I prefer this though, making determing
        // the row of a particular node fast is very nice!
        protected void resetChildrenRowsFrom(int newRow, int childIndex,
                                            int modelIndex) {
            int              lastRow = newRow;
            int              lastModelIndex = modelIndex;
            FHTreeStateNode  node;
            int              maxCounter = getChildCount();

            for(int counter = childIndex; counter < maxCounter; counter++) {
                node = (FHTreeStateNode)getChildAt(counter);
                lastRow += (node.childIndex - lastModelIndex);
                lastModelIndex = node.childIndex + 1;
                if(node.isExpanded) {
                    lastRow = node.setRowAndChildren(lastRow);
                }
                else {
                    node.row = lastRow++;
                }
            }
            lastRow += childCount - lastModelIndex;
            node = (FHTreeStateNode)getParent();
            if(node != null) {
                node.resetChildrenRowsFrom(lastRow, node.getIndex(this) + 1,
                                           this.childIndex + 1);
            }
            else { // This is the root, reset total ROWCOUNT!
                rowCount = lastRow;
            }
        }

        /**
         * Makes the receiver visible, but invoking
         * <code>expandParentAndReceiver</code> on the superclass.
         */
        protected void makeVisible() {
            FHTreeStateNode       parent = (FHTreeStateNode)getParent();

            if(parent != null)
                parent.expandParentAndReceiver();
        }

        /**
         * Invokes <code>expandParentAndReceiver</code> on the parent,
         * and expands the receiver.
         */
        protected void expandParentAndReceiver() {
            FHTreeStateNode       parent = (FHTreeStateNode)getParent();

            if(parent != null)
                parent.expandParentAndReceiver();
            expand();
        }

        /**
         * Expands the receiver.
         */
        protected void expand() {
            if(!isExpanded && !isLeaf()) {
                boolean            visible = isVisible();

                isExpanded = true;
                childCount = treeModel.getChildCount(getUserObject());

                if(visible) {
                    didExpand();
                }

                // Update the selection model.
                if(visible && treeSelectionModel != null) {
                    treeSelectionModel.resetRowSelection();
                }
            }
        }

        /**
         * Collapses the receiver. If <code>adjustRows</code> is true,
         * the rows of nodes after the receiver are adjusted.
         */
        protected void collapse(boolean adjustRows) {
            if(isExpanded) {
                if(isVisible() && adjustRows) {
                    int              childCount = getTotalChildCount();

                    isExpanded = false;
                    adjustRowCountBy(-childCount);
                    // We can do this because adjustRowBy won't descend
                    // the children.
                    adjustRowBy(-childCount, 0);
                }
                else
                    isExpanded = false;

                if(adjustRows && isVisible() && treeSelectionModel != null)
                    treeSelectionModel.resetRowSelection();
            }
        }

        /**
         * Returns true if the receiver is a leaf.
         */
        public boolean isLeaf() {
            TreeModel model = getModel();

            return (model != null) ? model.isLeaf(this.getUserObject()) :
                   true;
        }

        /**
         * Adds newChild to this nodes children at the appropriate location.
         * The location is determined from the childIndex of newChild.
         */
        protected void addNode(FHTreeStateNode newChild) {
            boolean         added = false;
            int             childIndex = newChild.getChildIndex();

            for(int counter = 0, maxCounter = getChildCount();
                counter < maxCounter; counter++) {
                if(((FHTreeStateNode)getChildAt(counter)).getChildIndex() >
                   childIndex) {
                    added = true;
                    insert(newChild, counter);
                    counter = maxCounter;
                }
            }
            if(!added)
                add(newChild);
        }

        /**
         * Removes the child at <code>modelIndex</code>.
         * <code>isChildVisible</code> should be true if the receiver
         * is visible and expanded.
         */
        protected void removeChildAtModelIndex(int modelIndex,
                                               boolean isChildVisible) {
            FHTreeStateNode     childNode = getChildAtModelIndex(modelIndex);

            if(childNode != null) {
                int          row = childNode.getRow();
                int          index = getIndex(childNode);

                childNode.collapse(false);
                remove(index);
                adjustChildIndexs(index, -1);
                childCount--;
                if(isChildVisible) {
                    // Adjust the rows.
                    resetChildrenRowsFrom(row, index, modelIndex);
                }
            }
            else {
                int                  maxCounter = getChildCount();
                FHTreeStateNode      aChild;

                for(int counter = 0; counter < maxCounter; counter++) {
                    aChild = (FHTreeStateNode)getChildAt(counter);
                    if(aChild.childIndex >= modelIndex) {
                        if(isChildVisible) {
                            adjustRowBy(-1, counter);
                            adjustRowCountBy(-1);
                        }
                        // Since matched and children are always sorted by
                        // index, no need to continue testing with the
                        // above.
                        for(; counter < maxCounter; counter++)
                            ((FHTreeStateNode)getChildAt(counter)).
                                              childIndex--;
                        childCount--;
                        return;
                    }
                }
                // No children to adjust, but it was a child, so we still need
                // to adjust nodes after this one.
                if(isChildVisible) {
                    adjustRowBy(-1, maxCounter);
                    adjustRowCountBy(-1);
                }
                childCount--;
            }
        }

        /**
         * Adjusts the child indexs of the receivers children by
         * <code>amount</code>, starting at <code>index</code>.
         */
        protected void adjustChildIndexs(int index, int amount) {
            for(int counter = index, maxCounter = getChildCount();
                counter < maxCounter; counter++) {
                ((FHTreeStateNode)getChildAt(counter)).childIndex += amount;
            }
        }

        /**
         * Messaged when a child has been inserted at index. For all the
         * children that have a childIndex &ge; index their index is incremented
         * by one.
         */
        protected void childInsertedAtModelIndex(int index,
                                               boolean isExpandedAndVisible) {
            FHTreeStateNode                aChild;
            int                            maxCounter = getChildCount();

            for(int counter = 0; counter < maxCounter; counter++) {
                aChild = (FHTreeStateNode)getChildAt(counter);
                if(aChild.childIndex >= index) {
                    if(isExpandedAndVisible) {
                        adjustRowBy(1, counter);
                        adjustRowCountBy(1);
                    }
                    /* Since matched and children are always sorted by
                       index, no need to continue testing with the above. */
                    for(; counter < maxCounter; counter++)
                        ((FHTreeStateNode)getChildAt(counter)).childIndex++;
                    childCount++;
                    return;
                }
            }
            // No children to adjust, but it was a child, so we still need
            // to adjust nodes after this one.
            if(isExpandedAndVisible) {
                adjustRowBy(1, maxCounter);
                adjustRowCountBy(1);
            }
            childCount++;
        }

        /**
         * Returns true if there is a row for <code>row</code>.
         * <code>nextRow</code> gives the bounds of the receiver.
         * Information about the found row is returned in <code>info</code>.
         * This should be invoked on root with <code>nextRow</code> set
         * to <code>getRowCount</code>().
         */
        protected boolean getPathForRow(int row, int nextRow,
                                        SearchInfo info) {
            if(this.row == row) {
                info.node = this;
                info.isNodeParentNode = false;
                info.childIndex = childIndex;
                return true;
            }

            FHTreeStateNode            child;
            FHTreeStateNode            lastChild = null;

            for(int counter = 0, maxCounter = getChildCount();
                counter < maxCounter; counter++) {
                child = (FHTreeStateNode)getChildAt(counter);
                if(child.row > row) {
                    if(counter == 0) {
                        // No node exists for it, and is first.
                        info.node = this;
                        info.isNodeParentNode = true;
                        info.childIndex = row - this.row - 1;
                        return true;
                    }
                    else {
                        // May have been in last child's bounds.
                        int          lastChildEndRow = 1 + child.row -
                                     (child.childIndex - lastChild.childIndex);

                        if(row < lastChildEndRow) {
                            return lastChild.getPathForRow(row,
                                                       lastChildEndRow, info);
                        }
                        // Between last child and child, but not in last child
                        info.node = this;
                        info.isNodeParentNode = true;
                        info.childIndex = row - lastChildEndRow +
                                                lastChild.childIndex + 1;
                        return true;
                    }
                }
                lastChild = child;
            }

            // Not in children, but we should have it, offset from
            // nextRow.
            if(lastChild != null) {
                int        lastChildEndRow = nextRow -
                                  (childCount - lastChild.childIndex) + 1;

                if(row < lastChildEndRow) {
                    return lastChild.getPathForRow(row, lastChildEndRow, info);
                }
                // Between last child and child, but not in last child
                info.node = this;
                info.isNodeParentNode = true;
                info.childIndex = row - lastChildEndRow +
                                             lastChild.childIndex + 1;
                return true;
            }
            else {
                // No children.
                int         retChildIndex = row - this.row - 1;

                if(retChildIndex >= childCount) {
                    return false;
                }
                info.node = this;
                info.isNodeParentNode = true;
                info.childIndex = retChildIndex;
                return true;
            }
        }

        /**
         * Asks all the children of the receiver for their totalChildCount
         * and returns this value (plus stopIndex).
         */
        protected int getCountTo(int stopIndex) {
            FHTreeStateNode    aChild;
            int                retCount = stopIndex + 1;

            for(int counter = 0, maxCounter = getChildCount();
                counter < maxCounter; counter++) {
                aChild = (FHTreeStateNode)getChildAt(counter);
                if(aChild.childIndex >= stopIndex)
                    counter = maxCounter;
                else
                    retCount += aChild.getTotalChildCount();
            }
            if(parent != null)
                return retCount + ((FHTreeStateNode)getParent())
                                   .getCountTo(childIndex);
            if(!isRootVisible())
                return (retCount - 1);
            return retCount;
        }

        /**
         * Returns the number of children that are expanded to
         * <code>stopIndex</code>. This does not include the number
         * of children that the child at <code>stopIndex</code> might
         * have.
         */
        protected int getNumExpandedChildrenTo(int stopIndex) {
            FHTreeStateNode    aChild;
            int                retCount = stopIndex;

            for(int counter = 0, maxCounter = getChildCount();
                counter < maxCounter; counter++) {
                aChild = (FHTreeStateNode)getChildAt(counter);
                if(aChild.childIndex >= stopIndex)
                    return retCount;
                else {
                    retCount += aChild.getTotalChildCount();
                }
            }
            return retCount;
        }

        /**
         * Messaged when this node either expands or collapses.
         */
        protected void didAdjustTree() {
        }

    } // FixedHeightLayoutCache.FHTreeStateNode


    /**
     * Used as a placeholder when getting the path in FHTreeStateNodes.
     */
    private class SearchInfo {
        protected FHTreeStateNode   node;
        protected boolean           isNodeParentNode;
        protected int               childIndex;

        protected TreePath getPath() {
            if(node == null)
                return null;

            if(isNodeParentNode)
                return node.getTreePath().pathByAddingChild(treeModel.getChild
                                            (node.getUserObject(),
                                             childIndex));
            return node.path;
        }
    } // FixedHeightLayoutCache.SearchInfo


    /**
     * An enumerator to iterate through visible nodes.
     */
    // This is very similar to
    // VariableHeightTreeState.VisibleTreeStateNodeEnumeration
    private class VisibleFHTreeStateNodeEnumeration
        implements Enumeration<TreePath>
    {
        /** Parent thats children are being enumerated. */
        protected FHTreeStateNode     parent;
        /** Index of next child. An index of -1 signifies parent should be
         * visibled next. */
        protected int                 nextIndex;
        /** Number of children in parent. */
        protected int                 childCount;

        protected VisibleFHTreeStateNodeEnumeration(FHTreeStateNode node) {
            this(node, -1);
        }

        protected VisibleFHTreeStateNodeEnumeration(FHTreeStateNode parent,
                                                    int startIndex) {
            this.parent = parent;
            this.nextIndex = startIndex;
            this.childCount = treeModel.getChildCount(this.parent.
                                                      getUserObject());
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

            if(nextIndex == -1)
                retObject = parent.getTreePath();
            else {
                FHTreeStateNode  node = parent.getChildAtModelIndex(nextIndex);

                if(node == null)
                    retObject = parent.getTreePath().pathByAddingChild
                                  (treeModel.getChild(parent.getUserObject(),
                                                      nextIndex));
                else
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
                FHTreeStateNode      newParent = (FHTreeStateNode)parent.
                                                  getParent();

                if(newParent != null) {
                    nextIndex = parent.childIndex;
                    parent = newParent;
                    childCount = treeModel.getChildCount
                                            (parent.getUserObject());
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
            if(nextIndex == -1 && !parent.isExpanded()) {
                return false;
            }

            // Check that it can have kids
            if(childCount == 0) {
                return false;
            }
            // Make sure next index not beyond child count.
            else if(++nextIndex >= childCount) {
                return false;
            }

            FHTreeStateNode    child = parent.getChildAtModelIndex(nextIndex);

            if(child != null && child.isExpanded()) {
                parent = child;
                nextIndex = -1;
                childCount = treeModel.getChildCount(child.getUserObject());
            }
            return true;
        }
    } // FixedHeightLayoutCache.VisibleFHTreeStateNodeEnumeration
}
