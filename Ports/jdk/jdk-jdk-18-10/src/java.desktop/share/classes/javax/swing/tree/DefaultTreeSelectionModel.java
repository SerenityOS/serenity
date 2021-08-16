/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

/**
 * Default implementation of TreeSelectionModel.  Listeners are notified
 * whenever
 * the paths in the selection change, not the rows. In order
 * to be able to track row changes you may wish to become a listener
 * for expansion events on the tree and test for changes from there.
 * <p>resetRowSelection is called from any of the methods that update
 * the selected paths. If you subclass any of these methods to
 * filter what is allowed to be selected, be sure and message
 * <code>resetRowSelection</code> if you do not message super.
 *
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see javax.swing.JTree
 *
 * @author Scott Violet
 */
@SuppressWarnings("serial")
public class DefaultTreeSelectionModel implements Cloneable, Serializable, TreeSelectionModel
{
    /** Property name for selectionMode. */
    public static final String          SELECTION_MODE_PROPERTY = "selectionMode";

    /** Used to messaged registered listeners. */
    protected SwingPropertyChangeSupport     changeSupport;

    /** Paths that are currently selected.  Will be null if nothing is
      * currently selected. */
    protected TreePath[]                selection;

    /** Event listener list. */
    protected EventListenerList   listenerList = new EventListenerList();

    /** Provides a row for a given path. */
    protected transient RowMapper               rowMapper;

    /** Handles maintaining the list selection model. The RowMapper is used
     * to map from a TreePath to a row, and the value is then placed here. */
    protected DefaultListSelectionModel     listSelectionModel;

    /** Mode for the selection, will be either SINGLE_TREE_SELECTION,
     * CONTIGUOUS_TREE_SELECTION or DISCONTIGUOUS_TREE_SELECTION.
     */
    protected int                           selectionMode;

    /** Last path that was added. */
    protected TreePath                      leadPath;
    /** Index of the lead path in selection. */
    protected int                           leadIndex;
    /** Lead row. */
    protected int                           leadRow;

    /** Used to make sure the paths are unique, will contain all the paths
     * in <code>selection</code>.
     */
    private Hashtable<TreePath, Boolean>    uniquePaths;
    private Hashtable<TreePath, Boolean>    lastPaths;
    private TreePath[]                      tempPaths;


    /**
     * Creates a new instance of DefaultTreeSelectionModel that is
     * empty, with a selection mode of DISCONTIGUOUS_TREE_SELECTION.
     */
    public DefaultTreeSelectionModel() {
        listSelectionModel = new DefaultListSelectionModel();
        selectionMode = DISCONTIGUOUS_TREE_SELECTION;
        leadIndex = leadRow = -1;
        uniquePaths = new Hashtable<TreePath, Boolean>();
        lastPaths = new Hashtable<TreePath, Boolean>();
        tempPaths = new TreePath[1];
    }

    /**
     * Sets the RowMapper instance. This instance is used to determine
     * the row for a particular TreePath.
     */
    public void setRowMapper(RowMapper newMapper) {
        rowMapper = newMapper;
        resetRowSelection();
    }

    /**
     * Returns the RowMapper instance that is able to map a TreePath to a
     * row.
     */
    public RowMapper getRowMapper() {
        return rowMapper;
    }

    /**
     * Sets the selection model, which must be one of SINGLE_TREE_SELECTION,
     * CONTIGUOUS_TREE_SELECTION or DISCONTIGUOUS_TREE_SELECTION. If mode
     * is not one of the defined value,
     * <code>DISCONTIGUOUS_TREE_SELECTION</code> is assumed.
     * <p>This may change the selection if the current selection is not valid
     * for the new mode. For example, if three TreePaths are
     * selected when the mode is changed to <code>SINGLE_TREE_SELECTION</code>,
     * only one TreePath will remain selected. It is up to the particular
     * implementation to decide what TreePath remains selected.
     * <p>
     * Setting the mode to something other than the defined types will
     * result in the mode becoming <code>DISCONTIGUOUS_TREE_SELECTION</code>.
     */
    public void setSelectionMode(int mode) {
        int            oldMode = selectionMode;

        selectionMode = validateSelectionMode(mode);
        if(oldMode != selectionMode && changeSupport != null)
            changeSupport.firePropertyChange(SELECTION_MODE_PROPERTY,
                                             Integer.valueOf(oldMode),
                                             Integer.valueOf(selectionMode));
    }

    private static int validateSelectionMode(int mode) {
        return (mode != TreeSelectionModel.SINGLE_TREE_SELECTION
                && mode != TreeSelectionModel.CONTIGUOUS_TREE_SELECTION
                && mode != TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
                ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION : mode;
    }

    /**
     * Returns the selection mode, one of <code>SINGLE_TREE_SELECTION</code>,
     * <code>DISCONTIGUOUS_TREE_SELECTION</code> or
     * <code>CONTIGUOUS_TREE_SELECTION</code>.
     */
    public int getSelectionMode() {
        return selectionMode;
    }

    /**
      * Sets the selection to path. If this represents a change, then
      * the TreeSelectionListeners are notified. If <code>path</code> is
      * null, this has the same effect as invoking <code>clearSelection</code>.
      *
      * @param path new path to select
      */
    public void setSelectionPath(TreePath path) {
        if(path == null)
            setSelectionPaths(null);
        else {
            TreePath[]          newPaths = new TreePath[1];

            newPaths[0] = path;
            setSelectionPaths(newPaths);
        }
    }

    /**
     * Sets the selection. Whether the supplied paths are taken as the
     * new selection depends upon the selection mode. If the supplied
     * array is {@code null}, or empty, the selection is cleared. If
     * the selection mode is {@code SINGLE_TREE_SELECTION}, only the
     * first path in {@code pPaths} is used. If the selection
     * mode is {@code CONTIGUOUS_TREE_SELECTION} and the supplied paths
     * are not contiguous, then only the first path in {@code pPaths} is
     * used. If the selection mode is
     * {@code DISCONTIGUOUS_TREE_SELECTION}, then all paths are used.
     * <p>
     * All {@code null} paths in {@code pPaths} are ignored.
     * <p>
     * If this represents a change, all registered {@code
     * TreeSelectionListener}s are notified.
     * <p>
     * The lead path is set to the last unique path.
     * <p>
     * The paths returned from {@code getSelectionPaths} are in the same
     * order as those supplied to this method.
     *
     * @param pPaths the new selection
     */
    public void setSelectionPaths(TreePath[] pPaths) {
        int            newCount, newCounter, oldCount, oldCounter;
        TreePath[]     paths = pPaths;

        if(paths == null)
            newCount = 0;
        else
            newCount = paths.length;
        if(selection == null)
            oldCount = 0;
        else
            oldCount = selection.length;
        if((newCount + oldCount) != 0) {
            if(selectionMode == TreeSelectionModel.SINGLE_TREE_SELECTION) {
                /* If single selection and more than one path, only allow
                   first. */
                if(newCount > 1) {
                    paths = new TreePath[1];
                    paths[0] = pPaths[0];
                    newCount = 1;
                }
            }
            else if(selectionMode ==
                    TreeSelectionModel.CONTIGUOUS_TREE_SELECTION) {
                /* If contiguous selection and paths aren't contiguous,
                   only select the first path item. */
                if(newCount > 0 && !arePathsContiguous(paths)) {
                    paths = new TreePath[1];
                    paths[0] = pPaths[0];
                    newCount = 1;
                }
            }

            TreePath         beginLeadPath = leadPath;
            Vector<PathPlaceHolder> cPaths = new Vector<PathPlaceHolder>(newCount + oldCount);
            List<TreePath> newSelectionAsList =
                    new ArrayList<TreePath>(newCount);

            lastPaths.clear();
            leadPath = null;
            /* Find the paths that are new. */
            for(newCounter = 0; newCounter < newCount; newCounter++) {
                TreePath path = paths[newCounter];
                if (path != null && lastPaths.get(path) == null) {
                    lastPaths.put(path, Boolean.TRUE);
                    if (uniquePaths.get(path) == null) {
                        cPaths.addElement(new PathPlaceHolder(path, true));
                    }
                    leadPath = path;
                    newSelectionAsList.add(path);
                }
            }

            TreePath[] newSelection = newSelectionAsList.toArray(
                    new TreePath[newSelectionAsList.size()]);

            /* Get the paths that were selected but no longer selected. */
            for(oldCounter = 0; oldCounter < oldCount; oldCounter++)
                if(selection[oldCounter] != null &&
                    lastPaths.get(selection[oldCounter]) == null)
                    cPaths.addElement(new PathPlaceHolder
                                      (selection[oldCounter], false));

            selection = newSelection;

            Hashtable<TreePath, Boolean>  tempHT = uniquePaths;

            uniquePaths = lastPaths;
            lastPaths = tempHT;
            lastPaths.clear();

            // No reason to do this now, but will still call it.
            insureUniqueness();

            updateLeadIndex();

            resetRowSelection();
            /* Notify of the change. */
            if(cPaths.size() > 0)
                notifyPathChange(cPaths, beginLeadPath);
        }
    }

    /**
      * Adds path to the current selection. If path is not currently
      * in the selection the TreeSelectionListeners are notified. This has
      * no effect if <code>path</code> is null.
      *
      * @param path the new path to add to the current selection
      */
    public void addSelectionPath(TreePath path) {
        if(path != null) {
            TreePath[]            toAdd = new TreePath[1];

            toAdd[0] = path;
            addSelectionPaths(toAdd);
        }
    }

    /**
      * Adds paths to the current selection. If any of the paths in
      * paths are not currently in the selection the TreeSelectionListeners
      * are notified. This has
      * no effect if <code>paths</code> is null.
      * <p>The lead path is set to the last element in <code>paths</code>.
      * <p>If the selection mode is <code>CONTIGUOUS_TREE_SELECTION</code>,
      * and adding the new paths would make the selection discontiguous.
      * Then two things can result: if the TreePaths in <code>paths</code>
      * are contiguous, then the selection becomes these TreePaths,
      * otherwise the TreePaths aren't contiguous and the selection becomes
      * the first TreePath in <code>paths</code>.
      *
      * @param paths the new path to add to the current selection
      */
    public void addSelectionPaths(TreePath[] paths) {
        int       newPathLength = ((paths == null) ? 0 : paths.length);

        if(newPathLength > 0) {
            if(selectionMode == TreeSelectionModel.SINGLE_TREE_SELECTION) {
                setSelectionPaths(paths);
            }
            else if(selectionMode == TreeSelectionModel.
                    CONTIGUOUS_TREE_SELECTION && !canPathsBeAdded(paths)) {
                if(arePathsContiguous(paths)) {
                    setSelectionPaths(paths);
                }
                else {
                    TreePath[]          newPaths = new TreePath[1];

                    newPaths[0] = paths[0];
                    setSelectionPaths(newPaths);
                }
            }
            else {
                int               counter, validCount;
                int               oldCount;
                TreePath          beginLeadPath = leadPath;
                Vector<PathPlaceHolder>  cPaths = null;

                if(selection == null)
                    oldCount = 0;
                else
                    oldCount = selection.length;
                /* Determine the paths that aren't currently in the
                   selection. */
                lastPaths.clear();
                for(counter = 0, validCount = 0; counter < newPathLength;
                    counter++) {
                    if(paths[counter] != null) {
                        if (uniquePaths.get(paths[counter]) == null) {
                            validCount++;
                            if(cPaths == null)
                                cPaths = new Vector<PathPlaceHolder>();
                            cPaths.addElement(new PathPlaceHolder
                                              (paths[counter], true));
                            uniquePaths.put(paths[counter], Boolean.TRUE);
                            lastPaths.put(paths[counter], Boolean.TRUE);
                        }
                        leadPath = paths[counter];
                    }
                }

                if(leadPath == null) {
                    leadPath = beginLeadPath;
                }

                if(validCount > 0) {
                    TreePath[]         newSelection = new TreePath[oldCount +
                                                                  validCount];

                    /* And build the new selection. */
                    if(oldCount > 0)
                        System.arraycopy(selection, 0, newSelection, 0,
                                         oldCount);
                    if(validCount != paths.length) {
                        /* Some of the paths in paths are already in
                           the selection. */
                        Enumeration<TreePath> newPaths = lastPaths.keys();

                        counter = oldCount;
                        while (newPaths.hasMoreElements()) {
                            newSelection[counter++] = newPaths.nextElement();
                        }
                    }
                    else {
                        System.arraycopy(paths, 0, newSelection, oldCount,
                                         validCount);
                    }

                    selection = newSelection;

                    insureUniqueness();

                    updateLeadIndex();

                    resetRowSelection();

                    notifyPathChange(cPaths, beginLeadPath);
                }
                else
                    leadPath = beginLeadPath;
                lastPaths.clear();
            }
        }
    }

    /**
      * Removes path from the selection. If path is in the selection
      * The TreeSelectionListeners are notified. This has no effect if
      * <code>path</code> is null.
      *
      * @param path the path to remove from the selection
      */
    public void removeSelectionPath(TreePath path) {
        if(path != null) {
            TreePath[]             rPath = new TreePath[1];

            rPath[0] = path;
            removeSelectionPaths(rPath);
        }
    }

    /**
      * Removes paths from the selection.  If any of the paths in paths
      * are in the selection the TreeSelectionListeners are notified.
      * This has no effect if <code>paths</code> is null.
      *
      * @param paths the paths to remove from the selection
      */
    public void removeSelectionPaths(TreePath[] paths) {
        if (paths != null && selection != null && paths.length > 0) {
            if(!canPathsBeRemoved(paths)) {
                /* Could probably do something more interesting here! */
                clearSelection();
            }
            else {
                Vector<PathPlaceHolder> pathsToRemove = null;

                /* Find the paths that can be removed. */
                for (int removeCounter = paths.length - 1; removeCounter >= 0;
                     removeCounter--) {
                    if(paths[removeCounter] != null) {
                        if (uniquePaths.get(paths[removeCounter]) != null) {
                            if(pathsToRemove == null)
                                pathsToRemove = new Vector<PathPlaceHolder>(paths.length);
                            uniquePaths.remove(paths[removeCounter]);
                            pathsToRemove.addElement(new PathPlaceHolder
                                         (paths[removeCounter], false));
                        }
                    }
                }
                if(pathsToRemove != null) {
                    int         removeCount = pathsToRemove.size();
                    TreePath    beginLeadPath = leadPath;

                    if(removeCount == selection.length) {
                        selection = null;
                    }
                    else {
                        Enumeration<TreePath> pEnum = uniquePaths.keys();
                        int                  validCount = 0;

                        selection = new TreePath[selection.length -
                                                removeCount];
                        while (pEnum.hasMoreElements()) {
                            selection[validCount++] = pEnum.nextElement();
                        }
                    }
                    if (leadPath != null &&
                        uniquePaths.get(leadPath) == null) {
                        if (selection != null) {
                            leadPath = selection[selection.length - 1];
                        }
                        else {
                            leadPath = null;
                        }
                    }
                    else if (selection != null) {
                        leadPath = selection[selection.length - 1];
                    }
                    else {
                        leadPath = null;
                    }
                    updateLeadIndex();

                    resetRowSelection();

                    notifyPathChange(pathsToRemove, beginLeadPath);
                }
            }
        }
    }

    /**
      * Returns the first path in the selection. This is useful if there
      * if only one item currently selected.
      */
    public TreePath getSelectionPath() {
        if (selection != null && selection.length > 0) {
            return selection[0];
        }
        return null;
    }

    /**
      * Returns the selection.
      *
      * @return the selection
      */
    public TreePath[] getSelectionPaths() {
        if(selection != null) {
            int                 pathSize = selection.length;
            TreePath[]          result = new TreePath[pathSize];

            System.arraycopy(selection, 0, result, 0, pathSize);
            return result;
        }
        return new TreePath[0];
    }

    /**
     * Returns the number of paths that are selected.
     */
    public int getSelectionCount() {
        return (selection == null) ? 0 : selection.length;
    }

    /**
      * Returns true if the path, <code>path</code>,
      * is in the current selection.
      */
    public boolean isPathSelected(TreePath path) {
        return (path != null) ? (uniquePaths.get(path) != null) : false;
    }

    /**
      * Returns true if the selection is currently empty.
      */
    public boolean isSelectionEmpty() {
        return (selection == null || selection.length == 0);
    }

    /**
      * Empties the current selection.  If this represents a change in the
      * current selection, the selection listeners are notified.
      */
    public void clearSelection() {
        if (selection != null && selection.length > 0) {
            int                    selSize = selection.length;
            boolean[]              newness = new boolean[selSize];

            for(int counter = 0; counter < selSize; counter++)
                newness[counter] = false;

            TreeSelectionEvent     event = new TreeSelectionEvent
                (this, selection, newness, leadPath, null);

            leadPath = null;
            leadIndex = leadRow = -1;
            uniquePaths.clear();
            selection = null;
            resetRowSelection();
            fireValueChanged(event);
        }
    }

    /**
      * Adds x to the list of listeners that are notified each time the
      * set of selected TreePaths changes.
      *
      * @param x the new listener to be added
      */
    public void addTreeSelectionListener(TreeSelectionListener x) {
        listenerList.add(TreeSelectionListener.class, x);
    }

    /**
      * Removes x from the list of listeners that are notified each time
      * the set of selected TreePaths changes.
      *
      * @param x the listener to remove
      */
    public void removeTreeSelectionListener(TreeSelectionListener x) {
        listenerList.remove(TreeSelectionListener.class, x);
    }

    /**
     * Returns an array of all the tree selection listeners
     * registered on this model.
     *
     * @return all of this model's <code>TreeSelectionListener</code>s
     *         or an empty
     *         array if no tree selection listeners are currently registered
     *
     * @see #addTreeSelectionListener
     * @see #removeTreeSelectionListener
     *
     * @since 1.4
     */
    public TreeSelectionListener[] getTreeSelectionListeners() {
        return listenerList.getListeners(TreeSelectionListener.class);
    }

    /**
     * Notifies all listeners that are registered for
     * tree selection events on this object.
     *
     * @param e the event that characterizes the change
     *
     * @see #addTreeSelectionListener
     * @see EventListenerList
     */
    protected void fireValueChanged(TreeSelectionEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // TreeSelectionEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TreeSelectionListener.class) {
                // Lazily create the event:
                // if (e == null)
                // e = new ListSelectionEvent(this, firstIndex, lastIndex);
                ((TreeSelectionListener)listeners[i+1]).valueChanged(e);
            }
        }
    }

    /**
     * Returns an array of all the objects currently registered
     * as <code><em>Foo</em>Listener</code>s
     * upon this model.
     * <code><em>Foo</em>Listener</code>s are registered using the
     * <code>add<em>Foo</em>Listener</code> method.
     *
     * <p>
     *
     * You can specify the <code>listenerType</code> argument
     * with a class literal,
     * such as
     * <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a
     * <code>DefaultTreeSelectionModel</code> <code>m</code>
     * for its tree selection listeners with the following code:
     *
     * <pre>TreeSelectionListener[] tsls = (TreeSelectionListener[])(m.getListeners(TreeSelectionListener.class));</pre>
     *
     * If no such listeners exist, this method returns an empty array.
     *
     * @param <T> the listener type
     * @param listenerType the type of listeners requested
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s on this component,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if <code>listenerType</code>
     *          doesn't specify a class or interface that implements
     *          <code>java.util.EventListener</code>
     *
     * @see #getTreeSelectionListeners
     * @see #getPropertyChangeListeners
     *
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /**
     * Returns the selection in terms of rows. There is not
     * necessarily a one-to-one mapping between the {@code TreePath}s
     * returned from {@code getSelectionPaths} and this method. In
     * particular, if a {@code TreePath} is not viewable (the {@code
     * RowMapper} returns {@code -1} for the row corresponding to the
     * {@code TreePath}), then the corresponding row is not included
     * in the returned array. For example, if the selection consists
     * of two paths, {@code A} and {@code B}, with {@code A} at row
     * {@code 10}, and {@code B} not currently viewable, then this method
     * returns an array with the single entry {@code 10}.
     *
     * @return the selection in terms of rows
     */
    public int[] getSelectionRows() {
        // This is currently rather expensive.  Needs
        // to be better support from ListSelectionModel to speed this up.
        if (rowMapper != null && selection != null && selection.length > 0) {
            int[]      rows = rowMapper.getRowsForPaths(selection);

            if (rows != null) {
                int       invisCount = 0;

                for (int counter = rows.length - 1; counter >= 0; counter--) {
                    if (rows[counter] == -1) {
                        invisCount++;
                    }
                }
                if (invisCount > 0) {
                    if (invisCount == rows.length) {
                        rows = null;
                    }
                    else {
                        int[]    tempRows = new int[rows.length - invisCount];

                        for (int counter = rows.length - 1, visCounter = 0;
                             counter >= 0; counter--) {
                            if (rows[counter] != -1) {
                                tempRows[visCounter++] = rows[counter];
                            }
                        }
                        rows = tempRows;
                    }
                }
            }
            return rows;
        }
        return new int[0];
    }

    /**
     * Returns the smallest value obtained from the RowMapper for the
     * current set of selected TreePaths. If nothing is selected,
     * or there is no RowMapper, this will return -1.
      */
    public int getMinSelectionRow() {
        return listSelectionModel.getMinSelectionIndex();
    }

    /**
     * Returns the largest value obtained from the RowMapper for the
     * current set of selected TreePaths. If nothing is selected,
     * or there is no RowMapper, this will return -1.
      */
    public int getMaxSelectionRow() {
        return listSelectionModel.getMaxSelectionIndex();
    }

    /**
      * Returns true if the row identified by <code>row</code> is selected.
      */
    public boolean isRowSelected(int row) {
        return listSelectionModel.isSelectedIndex(row);
    }

    /**
     * Updates this object's mapping from TreePath to rows. This should
     * be invoked when the mapping from TreePaths to integers has changed
     * (for example, a node has been expanded).
     * <p>You do not normally have to call this, JTree and its associated
     * Listeners will invoke this for you. If you are implementing your own
     * View class, then you will have to invoke this.
     * <p>This will invoke <code>insureRowContinuity</code> to make sure
     * the currently selected TreePaths are still valid based on the
     * selection mode.
     */
    public void resetRowSelection() {
        listSelectionModel.clearSelection();
        if(selection != null && rowMapper != null) {
            int               aRow;
            int               validCount = 0;
            int[]             rows = rowMapper.getRowsForPaths(selection);

            for(int counter = 0, maxCounter = selection.length;
                counter < maxCounter; counter++) {
                aRow = rows[counter];
                if(aRow != -1) {
                    listSelectionModel.addSelectionInterval(aRow, aRow);
                }
            }
            if(leadIndex != -1 && rows != null) {
                leadRow = rows[leadIndex];
            }
            else if (leadPath != null) {
                // Lead selection path doesn't have to be in the selection.
                tempPaths[0] = leadPath;
                rows = rowMapper.getRowsForPaths(tempPaths);
                leadRow = (rows != null) ? rows[0] : -1;
            }
            else {
                leadRow = -1;
            }
            insureRowContinuity();

        }
        else
            leadRow = -1;
    }

    /**
     * Returns the lead selection index. That is the last index that was
     * added.
     */
    public int getLeadSelectionRow() {
        return leadRow;
    }

    /**
     * Returns the last path that was added. This may differ from the
     * leadSelectionPath property maintained by the JTree.
     */
    public TreePath getLeadSelectionPath() {
        return leadPath;
    }

    /**
     * Adds a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     * <p>
     * A PropertyChangeEvent will get fired when the selection mode
     * changes.
     *
     * @param listener  the PropertyChangeListener to be added
     */
    public synchronized void addPropertyChangeListener(
                                PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     *
     * @param listener  the PropertyChangeListener to be removed
     */

    public synchronized void removePropertyChangeListener(
                                PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }
        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Returns an array of all the property change listeners
     * registered on this <code>DefaultTreeSelectionModel</code>.
     *
     * @return all of this model's <code>PropertyChangeListener</code>s
     *         or an empty
     *         array if no property change listeners are currently registered
     *
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     *
     * @since 1.4
     */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners();
    }

    /**
     * Makes sure the currently selected <code>TreePath</code>s are valid
     * for the current selection mode.
     * If the selection mode is <code>CONTIGUOUS_TREE_SELECTION</code>
     * and a <code>RowMapper</code> exists, this will make sure all
     * the rows are contiguous, that is, when sorted all the rows are
     * in order with no gaps.
     * If the selection isn't contiguous, the selection is
     * reset to contain the first set, when sorted, of contiguous rows.
     * <p>
     * If the selection mode is <code>SINGLE_TREE_SELECTION</code> and
     * more than one TreePath is selected, the selection is reset to
     * contain the first path currently selected.
     */
    protected void insureRowContinuity() {
        if(selectionMode == TreeSelectionModel.CONTIGUOUS_TREE_SELECTION &&
           selection != null && rowMapper != null) {
            DefaultListSelectionModel lModel = listSelectionModel;
            int                       min = lModel.getMinSelectionIndex();

            if(min != -1) {
                for(int counter = min,
                        maxCounter = lModel.getMaxSelectionIndex();
                        counter <= maxCounter; counter++) {
                    if(!lModel.isSelectedIndex(counter)) {
                        if(counter == min) {
                            clearSelection();
                        }
                        else {
                            TreePath[] newSel = new TreePath[counter - min];
                            int[] selectionIndex = rowMapper.getRowsForPaths(selection);
                            // find the actual selection pathes corresponded to the
                            // rows of the new selection
                            for (int i = 0; i < selectionIndex.length; i++) {
                                if (selectionIndex[i]<counter) {
                                    newSel[selectionIndex[i]-min] = selection[i];
                                }
                            }
                            setSelectionPaths(newSel);
                            break;
                        }
                    }
                }
            }
        }
        else if(selectionMode == TreeSelectionModel.SINGLE_TREE_SELECTION &&
                selection != null && selection.length > 1) {
            setSelectionPath(selection[0]);
        }
    }

    /**
     * Returns true if the paths are contiguous,
     * or this object has no RowMapper.
     *
     * @param paths array of paths to check
     * @return      whether the paths are contiguous, or this object has no RowMapper
     */
    protected boolean arePathsContiguous(TreePath[] paths) {
        if(rowMapper == null || paths.length < 2)
            return true;
        else {
            BitSet                             bitSet = new BitSet(32);
            int                                anIndex, counter, min;
            int                                pathCount = paths.length;
            int                                validCount = 0;
            TreePath[]                         tempPath = new TreePath[1];

            tempPath[0] = paths[0];
            min = rowMapper.getRowsForPaths(tempPath)[0];
            for(counter = 0; counter < pathCount; counter++) {
                if(paths[counter] != null) {
                    tempPath[0] = paths[counter];
                    int[] rows = rowMapper.getRowsForPaths(tempPath);
                    if (rows == null) {
                        return false;
                    }
                    anIndex = rows[0];
                    if(anIndex == -1 || anIndex < (min - pathCount) ||
                       anIndex > (min + pathCount))
                        return false;
                    if(anIndex < min)
                        min = anIndex;
                    if(!bitSet.get(anIndex)) {
                        bitSet.set(anIndex);
                        validCount++;
                    }
                }
            }
            int          maxCounter = validCount + min;

            for(counter = min; counter < maxCounter; counter++)
                if(!bitSet.get(counter))
                    return false;
        }
        return true;
    }

    /**
     * Used to test if a particular set of <code>TreePath</code>s can
     * be added. This will return true if <code>paths</code> is null (or
     * empty), or this object has no RowMapper, or nothing is currently selected,
     * or the selection mode is <code>DISCONTIGUOUS_TREE_SELECTION</code>, or
     * adding the paths to the current selection still results in a
     * contiguous set of <code>TreePath</code>s.
     *
     * @param paths array of {@code TreePaths} to check
     * @return      whether the particular set of {@code TreePaths} can be added
     */
    protected boolean canPathsBeAdded(TreePath[] paths) {
        if(paths == null || paths.length == 0 || rowMapper == null ||
           selection == null || selectionMode ==
           TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
            return true;
        else {
            BitSet                       bitSet = new BitSet();
            DefaultListSelectionModel    lModel = listSelectionModel;
            int                          anIndex;
            int                          counter;
            int                          min = lModel.getMinSelectionIndex();
            int                          max = lModel.getMaxSelectionIndex();
            TreePath[]                   tempPath = new TreePath[1];

            if(min != -1) {
                for(counter = min; counter <= max; counter++) {
                    if(lModel.isSelectedIndex(counter))
                        bitSet.set(counter);
                }
            }
            else {
                tempPath[0] = paths[0];
                min = max = rowMapper.getRowsForPaths(tempPath)[0];
            }
            for(counter = paths.length - 1; counter >= 0; counter--) {
                if(paths[counter] != null) {
                    tempPath[0] = paths[counter];
                    int[]   rows = rowMapper.getRowsForPaths(tempPath);
                    if (rows == null) {
                        return false;
                    }
                    anIndex = rows[0];
                    min = Math.min(anIndex, min);
                    max = Math.max(anIndex, max);
                    if(anIndex == -1)
                        return false;
                    bitSet.set(anIndex);
                }
            }
            for(counter = min; counter <= max; counter++)
                if(!bitSet.get(counter))
                    return false;
        }
        return true;
    }

    /**
     * Returns true if the paths can be removed without breaking the
     * continuity of the model.
     * This is rather expensive.
     *
     * @param paths array of {@code TreePath} to check
     * @return      whether the paths can be removed without breaking the
     *              continuity of the model
     */
    protected boolean canPathsBeRemoved(TreePath[] paths) {
        if(rowMapper == null || selection == null ||
           selectionMode == TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
            return true;
        else {
            BitSet               bitSet = new BitSet();
            int                  counter;
            int                  pathCount = paths.length;
            int                  anIndex;
            int                  min = -1;
            int                  validCount = 0;
            TreePath[]           tempPath = new TreePath[1];
            int[]                rows;

            /* Determine the rows for the removed entries. */
            lastPaths.clear();
            for (counter = 0; counter < pathCount; counter++) {
                if (paths[counter] != null) {
                    lastPaths.put(paths[counter], Boolean.TRUE);
                }
            }
            for(counter = selection.length - 1; counter >= 0; counter--) {
                if(lastPaths.get(selection[counter]) == null) {
                    tempPath[0] = selection[counter];
                    rows = rowMapper.getRowsForPaths(tempPath);
                    if(rows != null && rows[0] != -1 && !bitSet.get(rows[0])) {
                        validCount++;
                        if(min == -1)
                            min = rows[0];
                        else
                            min = Math.min(min, rows[0]);
                        bitSet.set(rows[0]);
                    }
                }
            }
            lastPaths.clear();
            /* Make sure they are contiguous. */
            if(validCount > 1) {
                for(counter = min + validCount - 1; counter >= min;
                    counter--)
                    if(!bitSet.get(counter))
                        return false;
            }
        }
        return true;
    }

    /**
     * Notifies listeners of a change in path. changePaths should contain
     * instances of PathPlaceHolder.
     *
     * @deprecated As of JDK version 1.7
     *
     * @param changedPaths      the vector of the changed paths
     * @param oldLeadSelection  the old selection path
     */
    @Deprecated
    protected void notifyPathChange(Vector<?> changedPaths,
                                    TreePath oldLeadSelection) {
        int                    cPathCount = changedPaths.size();
        boolean[]              newness = new boolean[cPathCount];
        TreePath[]            paths = new TreePath[cPathCount];
        PathPlaceHolder        placeholder;

        for(int counter = 0; counter < cPathCount; counter++) {
            placeholder = (PathPlaceHolder) changedPaths.elementAt(counter);
            newness[counter] = placeholder.isNew;
            paths[counter] = placeholder.path;
        }

        TreeSelectionEvent     event = new TreeSelectionEvent
                          (this, paths, newness, oldLeadSelection, leadPath);

        fireValueChanged(event);
    }

    /**
     * Updates the leadIndex instance variable.
     */
    protected void updateLeadIndex() {
        if(leadPath != null) {
            if(selection == null) {
                leadPath = null;
                leadIndex = leadRow = -1;
            }
            else {
                leadRow = leadIndex = -1;
                for(int counter = selection.length - 1; counter >= 0;
                    counter--) {
                    // Can use == here since we know leadPath came from
                    // selection
                    if(selection[counter] == leadPath) {
                        leadIndex = counter;
                        break;
                    }
                }
            }
        }
        else {
            leadIndex = -1;
        }
    }

    /**
     * This method is obsolete and its implementation is now a noop.  It's
     * still called by setSelectionPaths and addSelectionPaths, but only
     * for backwards compatibility.
     */
    protected void insureUniqueness() {
    }


    /**
     * Returns a string that displays and identifies this
     * object's properties.
     *
     * @return a String representation of this object
     */
    public String toString() {
        int                selCount = getSelectionCount();
        StringBuilder      sb = new StringBuilder();
        int[]              rows;

        if(rowMapper != null)
            rows = rowMapper.getRowsForPaths(selection);
        else
            rows = null;
        sb.append(getClass().getName() + " " + hashCode() + " [ ");
        for(int counter = 0; counter < selCount; counter++) {
            if(rows != null)
                sb.append(selection[counter].toString() + "@" +
                          Integer.toString(rows[counter])+ " ");
            else
                sb.append(selection[counter].toString() + " ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns a clone of this object with the same selection.
     * This method does not duplicate
     * selection listeners and property listeners.
     *
     * @exception CloneNotSupportedException never thrown by instances of
     *                                       this class
     */
    public Object clone() throws CloneNotSupportedException {
        DefaultTreeSelectionModel        clone = (DefaultTreeSelectionModel)
                            super.clone();

        clone.changeSupport = null;
        if(selection != null) {
            int              selLength = selection.length;

            clone.selection = new TreePath[selLength];
            System.arraycopy(selection, 0, clone.selection, 0, selLength);
        }
        clone.listenerList = new EventListenerList();
        clone.listSelectionModel = (DefaultListSelectionModel)
            listSelectionModel.clone();
        clone.uniquePaths = new Hashtable<TreePath, Boolean>();
        clone.lastPaths = new Hashtable<TreePath, Boolean>();
        clone.tempPaths = new TreePath[1];
        return clone;
    }

    // Serialization support.
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        Object[]             tValues;

        s.defaultWriteObject();
        // Save the rowMapper, if it implements Serializable
        if(rowMapper != null && rowMapper instanceof Serializable) {
            tValues = new Object[2];
            tValues[0] = "rowMapper";
            tValues[1] = rowMapper;
        }
        else
            tValues = new Object[0];
        s.writeObject(tValues);
    }


    @Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField f = s.readFields();

        changeSupport = (SwingPropertyChangeSupport) f.get("changeSupport", null);
        selection = (TreePath[]) f.get("selection", null);
        EventListenerList newListenerList = (EventListenerList) f.get("listenerList", null);
        if (newListenerList == null) {
            throw new InvalidObjectException("Null listenerList");
        }
        listenerList = newListenerList;
        listSelectionModel = (DefaultListSelectionModel) f.get("listSelectionModel", null);
        selectionMode = validateSelectionMode(f.get("selectionMode", 0));
        leadPath = (TreePath) f.get("leadPath", null);
        leadIndex = f.get("leadIndex", 0);
        leadRow = f.get("leadRow", 0);
        @SuppressWarnings("unchecked")
        Hashtable<TreePath, Boolean> newUniquePaths =
                (Hashtable<TreePath, Boolean>) f.get("uniquePaths", null);
        uniquePaths = newUniquePaths;
        @SuppressWarnings("unchecked")
        Hashtable<TreePath, Boolean> newLastPaths =
                (Hashtable<TreePath, Boolean>) f.get("lastPaths", null);
        lastPaths = newLastPaths;
        tempPaths = (TreePath[]) f.get("tempPaths", null);

        Object[]      tValues;
        tValues = (Object[])s.readObject();

        if (tValues.length > 0 && tValues[0].equals("rowMapper")) {
            RowMapper newRowMapper = (RowMapper) tValues[1];
            if (newRowMapper == null) {
                throw new InvalidObjectException("Null newRowMapper");
            }
            rowMapper = newRowMapper;
        }
    }
}

/**
 * Holds a path and whether or not it is new.
 */
class PathPlaceHolder {
    protected boolean             isNew;
    protected TreePath           path;

    PathPlaceHolder(TreePath path, boolean isNew) {
        this.path = path;
        this.isNew = isNew;
    }
}
