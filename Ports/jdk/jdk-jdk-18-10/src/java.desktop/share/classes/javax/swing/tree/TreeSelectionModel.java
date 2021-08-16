/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.event.*;
import java.beans.PropertyChangeListener;

/**
  * This interface represents the current state of the selection for
  * the tree component.
  * For information and examples of using tree selection models,
  * see <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/tree.html">How to Use Trees</a>
  * in <em>The Java Tutorial.</em>
  *
  * <p>
  * The state of the tree selection is characterized by
  * a set of TreePaths, and optionally a set of integers. The mapping
  * from TreePath to integer is done by way of an instance of RowMapper.
  * It is not necessary for a TreeSelectionModel to have a RowMapper to
  * correctly operate, but without a RowMapper <code>getSelectionRows</code>
  * will return null.
  *
  * <p>
  *
  * A TreeSelectionModel can be configured to allow only one
  * path (<code>SINGLE_TREE_SELECTION</code>) a number of
  * contiguous paths (<code>CONTIGUOUS_TREE_SELECTION</code>) or a number of
  * discontiguous paths (<code>DISCONTIGUOUS_TREE_SELECTION</code>).
  * A <code>RowMapper</code> is used to determine if TreePaths are
  * contiguous.
  * In the absence of a RowMapper <code>CONTIGUOUS_TREE_SELECTION</code> and
  * <code>DISCONTIGUOUS_TREE_SELECTION</code> behave the same, that is they
  * allow any number of paths to be contained in the TreeSelectionModel.
  *
  * <p>
  *
  * For a selection model of <code>CONTIGUOUS_TREE_SELECTION</code> any
  * time the paths are changed (<code>setSelectionPath</code>,
  * <code>addSelectionPath</code> ...) the TreePaths are again checked to
  * make they are contiguous. A check of the TreePaths can also be forced
  * by invoking <code>resetRowSelection</code>. How a set of discontiguous
  * TreePaths is mapped to a contiguous set is left to implementors of
  * this interface to enforce a particular policy.
  *
  * <p>
  *
  * Implementations should combine duplicate TreePaths that are
  * added to the selection. For example, the following code
  * <pre>
  *   TreePath[] paths = new TreePath[] { treePath, treePath };
  *   treeSelectionModel.setSelectionPaths(paths);
  * </pre>
  * should result in only one path being selected:
  * <code>treePath</code>, and
  * not two copies of <code>treePath</code>.
  *
  * <p>
  *
  * The lead TreePath is the last path that was added (or set). The lead
  * row is then the row that corresponds to the TreePath as determined
  * from the RowMapper.
  *
  * @author Scott Violet
  */

public interface TreeSelectionModel
{
    /** Selection can only contain one path at a time. */
    public static final int               SINGLE_TREE_SELECTION = 1;

    /** Selection can only be contiguous. This will only be enforced if
     * a RowMapper instance is provided. That is, if no RowMapper is set
     * this behaves the same as DISCONTIGUOUS_TREE_SELECTION. */
    public static final int               CONTIGUOUS_TREE_SELECTION = 2;

    /** Selection can contain any number of items that are not necessarily
     * contiguous. */
    public static final int               DISCONTIGUOUS_TREE_SELECTION = 4;

    /**
     * Sets the selection model, which must be one of SINGLE_TREE_SELECTION,
     * CONTIGUOUS_TREE_SELECTION or DISCONTIGUOUS_TREE_SELECTION.
     * <p>
     * This may change the selection if the current selection is not valid
     * for the new mode. For example, if three TreePaths are
     * selected when the mode is changed to <code>SINGLE_TREE_SELECTION</code>,
     * only one TreePath will remain selected. It is up to the particular
     * implementation to decide what TreePath remains selected.
     *
     * @param   mode    selection mode to be set
     */
    void setSelectionMode(int mode);

    /**
     * Returns the current selection mode, one of
     * <code>SINGLE_TREE_SELECTION</code>,
     * <code>CONTIGUOUS_TREE_SELECTION</code> or
     * <code>DISCONTIGUOUS_TREE_SELECTION</code>.
     *
     * @return          the current selection mode
     */
    int getSelectionMode();

    /**
      * Sets the selection to path. If this represents a change, then
      * the TreeSelectionListeners are notified. If <code>path</code> is
      * null, this has the same effect as invoking <code>clearSelection</code>.
      *
      * @param  path    new path to select
      */
    void setSelectionPath(TreePath path);

    /**
      * Sets the selection to path. If this represents a change, then
      * the TreeSelectionListeners are notified. If <code>paths</code> is
      * null, this has the same effect as invoking <code>clearSelection</code>.
      *
      * @param  paths   new selection
      */
    void setSelectionPaths(TreePath[] paths);

    /**
      * Adds path to the current selection. If path is not currently
      * in the selection the TreeSelectionListeners are notified. This has
      * no effect if <code>path</code> is null.
      *
      * @param  path    the new path to add to the current selection
      */
    void addSelectionPath(TreePath path);

    /**
      * Adds paths to the current selection.  If any of the paths in
      * paths are not currently in the selection the TreeSelectionListeners
      * are notified. This has
      * no effect if <code>paths</code> is null.
      *
      * @param  paths   the new paths to add to the current selection
      */
    void addSelectionPaths(TreePath[] paths);

    /**
      * Removes path from the selection. If path is in the selection
      * The TreeSelectionListeners are notified. This has no effect if
      * <code>path</code> is null.
      *
      * @param  path    the path to remove from the selection
      */
    void removeSelectionPath(TreePath path);

    /**
      * Removes paths from the selection.  If any of the paths in
      * <code>paths</code>
      * are in the selection, the TreeSelectionListeners are notified.
      * This method has no effect if <code>paths</code> is null.
      *
      * @param  paths   the path to remove from the selection
      */
    void removeSelectionPaths(TreePath[] paths);

    /**
      * Returns the first path in the selection. How first is defined is
      * up to implementors, and may not necessarily be the TreePath with
      * the smallest integer value as determined from the
      * <code>RowMapper</code>.
      *
      * @return         the first path in the selection
      */
    TreePath getSelectionPath();

    /**
      * Returns the paths in the selection. This will return null (or an
      * empty array) if nothing is currently selected.
      *
      * @return         the paths in the selection
      */
    TreePath[] getSelectionPaths();

    /**
     * Returns the number of paths that are selected.
     *
     * @return          the number of paths that are selected
     */
    int getSelectionCount();

    /**
      * Returns true if the path, <code>path</code>, is in the current
      * selection.
      *
      * @param  path    the path to be loked for
      * @return         whether the {@code path} is in the current selection
      */
    boolean isPathSelected(TreePath path);

    /**
      * Returns true if the selection is currently empty.
      *
      * @return         whether the selection is currently empty
      */
    boolean isSelectionEmpty();

    /**
      * Empties the current selection.  If this represents a change in the
      * current selection, the selection listeners are notified.
      */
    void clearSelection();

    /**
     * Sets the RowMapper instance. This instance is used to determine
     * the row for a particular TreePath.
     *
     * @param   newMapper   RowMapper to be set
     */
    void setRowMapper(RowMapper newMapper);

    /**
     * Returns the RowMapper instance that is able to map a TreePath to a
     * row.
     *
     * @return          the RowMapper instance that is able to map a TreePath
     *                  to a row
     */
    RowMapper getRowMapper();

    /**
      * Returns all of the currently selected rows. This will return
      * null (or an empty array) if there are no selected TreePaths or
      * a RowMapper has not been set.
      *
      * @return         all of the currently selected rows
      */
    int[] getSelectionRows();

    /**
     * Returns the smallest value obtained from the RowMapper for the
     * current set of selected TreePaths. If nothing is selected,
     * or there is no RowMapper, this will return -1.
     *
     * @return          the smallest value obtained from the RowMapper
     *                  for the current set of selected TreePaths
      */
    int getMinSelectionRow();

    /**
     * Returns the largest value obtained from the RowMapper for the
     * current set of selected TreePaths. If nothing is selected,
     * or there is no RowMapper, this will return -1.
     *
     * @return          the largest value obtained from the RowMapper
     *                  for the current set of selected TreePaths
      */
    int getMaxSelectionRow();

    /**
      * Returns true if the row identified by <code>row</code> is selected.
      *
      * @param  row     row to check
      * @return         whether the row is selected
      */
    boolean isRowSelected(int row);

    /**
     * Updates this object's mapping from TreePaths to rows. This should
     * be invoked when the mapping from TreePaths to integers has changed
     * (for example, a node has been expanded).
     * <p>
     * You do not normally have to call this; JTree and its associated
     * listeners will invoke this for you. If you are implementing your own
     * view class, then you will have to invoke this.
     */
    void resetRowSelection();

    /**
     * Returns the lead selection index. That is the last index that was
     * added.
     *
     * @return          the lead selection index
     */
    int getLeadSelectionRow();

    /**
     * Returns the last path that was added. This may differ from the
     * leadSelectionPath property maintained by the JTree.
     *
     * @return          the last path that was added
     */
    TreePath getLeadSelectionPath();

    /**
     * Adds a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     * <p>
     * A PropertyChangeEvent will get fired when the selection mode
     * changes.
     *
     * @param   listener    the PropertyChangeListener to be added
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     *
     * @param   listener    the PropertyChangeListener to be removed
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
      * Adds x to the list of listeners that are notified each time the
      * set of selected TreePaths changes.
      *
      * @param  x       the new listener to be added
      */
    void addTreeSelectionListener(TreeSelectionListener x);

    /**
      * Removes x from the list of listeners that are notified each time
      * the set of selected TreePaths changes.
      *
      * @param  x       the listener to remove
      */
    void removeTreeSelectionListener(TreeSelectionListener x);
}
