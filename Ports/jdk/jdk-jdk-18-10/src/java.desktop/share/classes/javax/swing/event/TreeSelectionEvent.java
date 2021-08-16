/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.event;

import java.util.EventObject;
import javax.swing.tree.TreePath;

/**
 * An event that characterizes a change in the current
 * selection.  The change is based on any number of paths.
 * TreeSelectionListeners will generally query the source of
 * the event for the new selected status of each potentially
 * changed row.
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
 * @see TreeSelectionListener
 * @see javax.swing.tree.TreeSelectionModel
 *
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Same-version serialization only
public class TreeSelectionEvent extends EventObject
{
    /** Paths this event represents. */
    protected TreePath[]     paths;
    /** For each path identifies if that path is in fact new. */
    protected boolean[]       areNew;
    /** leadSelectionPath before the paths changed, may be null. */
    protected TreePath        oldLeadSelectionPath;
    /** leadSelectionPath after the paths changed, may be null. */
    protected TreePath        newLeadSelectionPath;

    /**
      * Represents a change in the selection of a {@code TreeSelectionModel}.
      * {@code paths} identifies the paths that have been either added or
      * removed from the selection.
      *
      * @param source source of event
      * @param paths the paths that have changed in the selection
      * @param areNew a {@code boolean} array indicating whether the paths in
      *               {@code paths} are new to the selection
      * @param oldLeadSelectionPath the previous lead selection path
      * @param newLeadSelectionPath the new lead selection path
      */
    public TreeSelectionEvent(Object source, TreePath[] paths,
                              boolean[] areNew, TreePath oldLeadSelectionPath,
                              TreePath newLeadSelectionPath)
    {
        super(source);
        this.paths = paths;
        this.areNew = areNew;
        this.oldLeadSelectionPath = oldLeadSelectionPath;
        this.newLeadSelectionPath = newLeadSelectionPath;
    }

    /**
      * Represents a change in the selection of a {@code TreeSelectionModel}.
      * {@code path} identifies the path that has been either added or
      * removed from the selection.
      *
      * @param source source of event
      * @param path the path that has changed in the selection
      * @param isNew whether or not the path is new to the selection, false
      *              means path was removed from the selection.
      * @param oldLeadSelectionPath the previous lead selection path
      * @param newLeadSelectionPath the new lead selection path
      */
    public TreeSelectionEvent(Object source, TreePath path, boolean isNew,
                              TreePath oldLeadSelectionPath,
                              TreePath newLeadSelectionPath)
    {
        super(source);
        paths = new TreePath[1];
        paths[0] = path;
        areNew = new boolean[1];
        areNew[0] = isNew;
        this.oldLeadSelectionPath = oldLeadSelectionPath;
        this.newLeadSelectionPath = newLeadSelectionPath;
    }

    /**
      * Returns the paths that have been added or removed from the selection.
      *
      * @return copy of the array of {@code TreePath} obects for this event.
      */
    public TreePath[] getPaths()
    {
        int                  numPaths;
        TreePath[]          retPaths;

        numPaths = paths.length;
        retPaths = new TreePath[numPaths];
        System.arraycopy(paths, 0, retPaths, 0, numPaths);
        return retPaths;
    }

    /**
      * Returns the first path element.
      *
      * @return the first {@code TreePath} element represented by this event
      */
    public TreePath getPath()
    {
        return paths[0];
    }

    /**
     * Returns whether the path identified by {@code getPath} was
     * added to the selection.  A return value of {@code true}
     * indicates the path identified by {@code getPath} was added to
     * the selection. A return value of {@code false} indicates {@code
     * getPath} was selected, but is no longer selected.
     *
     * @return {@code true} if {@code getPath} was added to the selection,
     *         {@code false} otherwise
     */
    public boolean isAddedPath() {
        return areNew[0];
    }

    /**
     * Returns whether the specified path was added to the selection.
     * A return value of {@code true} indicates the path identified by
     * {@code path} was added to the selection. A return value of
     * {@code false} indicates {@code path} is no longer selected. This method
     * is only valid for the paths returned from {@code getPaths()}; invoking
     * with a path not included in {@code getPaths()} throws an
     * {@code IllegalArgumentException}.
     *
     * @param path the path to test
     * @return {@code true} if {@code path} was added to the selection,
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code path} is not contained
     *         in {@code getPaths}
     * @see #getPaths
     */
    public boolean isAddedPath(TreePath path) {
        for(int counter = paths.length - 1; counter >= 0; counter--)
            if(paths[counter].equals(path))
                return areNew[counter];
        throw new IllegalArgumentException("path is not a path identified by the TreeSelectionEvent");
    }

    /**
     * Returns whether the path at {@code getPaths()[index]} was added
     * to the selection.  A return value of {@code true} indicates the
     * path was added to the selection. A return value of {@code false}
     * indicates the path is no longer selected.
     *
     * @param index the index of the path to test
     * @return {@code true} if the path was added to the selection,
     *         {@code false} otherwise
     * @throws IllegalArgumentException if index is outside the range of
     *         {@code getPaths}
     * @see #getPaths
     *
     * @since 1.3
     */
    public boolean isAddedPath(int index) {
        if (paths == null || index < 0 || index >= paths.length) {
            throw new IllegalArgumentException("index is beyond range of added paths identified by TreeSelectionEvent");
        }
        return areNew[index];
    }

    /**
     * Returns the path that was previously the lead path.
     *
     * @return a {@code TreePath} containing the old lead selection path
     */
    public TreePath getOldLeadSelectionPath() {
        return oldLeadSelectionPath;
    }

    /**
     * Returns the current lead path.
     *
     * @return a {@code TreePath} containing the new lead selection path
     */
    public TreePath getNewLeadSelectionPath() {
        return newLeadSelectionPath;
    }

    /**
     * Returns a copy of the receiver, but with the source being newSource.
     *
     * @param newSource source of event
     * @return an {@code Object} which is a copy of this event with the source
     *         being the {@code newSource} provided
     */
    public Object cloneWithSource(Object newSource) {
      // Fix for IE bug - crashing
      return new TreeSelectionEvent(newSource, paths, areNew,
                                    oldLeadSelectionPath,
                                    newLeadSelectionPath);
    }
}
