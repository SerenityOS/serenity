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

package javax.swing.plaf;

import java.awt.Rectangle;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * Pluggable look and feel interface for JTree.
 *
 * @author Rob Davis
 * @author Scott Violet
 */
public abstract class TreeUI extends ComponentUI
{
    /**
     * Constructor for subclasses to call.
     */
    protected TreeUI() {}

    /**
     * Returns the Rectangle enclosing the label portion that the
     * last item in path will be drawn into.  Will return null if
     * any component in path is currently valid.
     *
     * @param tree the {@code JTree} for {@code path}
     * @param path the {@code TreePath} identifying the node
     * @return the {@code Rectangle} enclosing the label portion that the
     *         last item in path will be drawn into, {@code null} if any
     *         component in path is currently valid.
     */
    public abstract Rectangle getPathBounds(JTree tree, TreePath path);

    /**
     * Returns the path for passed in row.  If row is not visible
     * null is returned.
     *
     * @param tree a {@code JTree} object
     * @param row an integer specifying a row
     * @return the {@code path} for {@code row} or {@code null} if {@code row}
     *         is not visible
     */
    public abstract TreePath getPathForRow(JTree tree, int row);

    /**
     * Returns the row that the last item identified in path is visible
     * at.  Will return -1 if any of the elements in path are not
     * currently visible.
     *
     * @param tree the {@code JTree} for {@code path}
     * @param path the {@code TreePath} object to look in
     * @return an integer specifying the row at which the last item
     *         identified is visible, -1 if any of the elements in
     *         {@code path} are not currently visible
     */
    public abstract int getRowForPath(JTree tree, TreePath path);

    /**
     * Returns the number of rows that are being displayed.
     *
     * @param tree the {@code JTree} for which to count rows
     * @return an integer specifying the number of row being displayed
     */
    public abstract int getRowCount(JTree tree);

    /**
     * Returns the path to the node that is closest to x,y.  If
     * there is nothing currently visible this will return null, otherwise
     * it'll always return a valid path.  If you need to test if the
     * returned object is exactly at x, y you should get the bounds for
     * the returned path and test x, y against that.
     *
     * @param tree a {@code JTree} object
     * @param x an integer giving the number of pixels horizontally from the
     *        left edge of the display area
     * @param y an integer giving the number of pixels vertically from the top
     *        of the display area, minus any top margin
     * @return the {@code TreePath} node closest to {@code x,y} or {@code null}
     *         if there is nothing currently visible
     */
    public abstract TreePath getClosestPathForLocation(JTree tree, int x,
                                                       int y);

    /**
     * Returns true if the tree is being edited.  The item that is being
     * edited can be returned by getEditingPath().
     *
     * @param tree a {@code JTree} object
     * @return true if {@code tree} is being edited
     */
    public abstract boolean isEditing(JTree tree);

    /**
     * Stops the current editing session.  This has no effect if the
     * tree isn't being edited.  Returns true if the editor allows the
     * editing session to stop.
     *
     * @param tree a {@code JTree} object
     * @return true if the editor allows the editing session to stop
     */
    public abstract boolean stopEditing(JTree tree);

    /**
     * Cancels the current editing session. This has no effect if the
     * tree isn't being edited.
     *
     * @param tree a {@code JTree} object
     */
    public abstract void cancelEditing(JTree tree);

    /**
     * Selects the last item in path and tries to edit it.  Editing will
     * fail if the CellEditor won't allow it for the selected item.
     *
     * @param tree the {@code JTree} being edited
     * @param path the {@code TreePath} to be edited
     */
    public abstract void startEditingAtPath(JTree tree, TreePath path);

    /**
     * Returns the path to the element that is being edited.
     *
     * @param tree the {@code JTree} for which to return a path
     * @return a {@code TreePath} containing the path to {@code tree}
     */
    public abstract TreePath getEditingPath(JTree tree);
}
