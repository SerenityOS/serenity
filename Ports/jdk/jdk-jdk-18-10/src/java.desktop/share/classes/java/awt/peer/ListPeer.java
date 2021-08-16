/*
 * Copyright (c) 1995, 1998, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.peer;

import java.awt.Dimension;
import java.awt.List;

/**
 * The peer interface for {@link List}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface ListPeer extends ComponentPeer {

    /**
     * Returns the indices of the list items that are currently selected.
     * The returned array is not required to be a copy, the callers of this
     * method already make sure it is not modified.
     *
     * @return the indices of the list items that are currently selected
     *
     * @see List#getSelectedIndexes()
     */
    int[] getSelectedIndexes();

    /**
     * Adds an item to the list at the specified index.
     *
     * @param item the item to add to the list
     * @param index the index where to add the item into the list
     *
     * @see List#add(String, int)
     */
    void add(String item, int index);

    /**
     * Deletes items from the list. All items from start to end should are
     * deleted, including the item at the start and end indices.
     *
     * @param start the first item to be deleted
     * @param end the last item to be deleted
     */
    void delItems(int start, int end);

    /**
     * Removes all items from the list.
     *
     * @see List#removeAll()
     */
    void removeAll();

    /**
     * Selects the item at the specified {@code index}.
     *
     * @param index the index of the item to select
     *
     * @see List#select(int)
     */
    void select(int index);

    /**
     * De-selects the item at the specified {@code index}.
     *
     * @param index the index of the item to de-select
     *
     * @see List#deselect(int)
     */
    void deselect(int index);

    /**
     * Makes sure that the item at the specified {@code index} is visible,
     * by scrolling the list or similar.
     *
     * @param index the index of the item to make visible
     *
     * @see List#makeVisible(int)
     */
    void makeVisible(int index);

    /**
     * Toggles multiple selection mode on or off.
     *
     * @param m {@code true} for multiple selection mode,
     *        {@code false} for single selection mode
     *
     * @see List#setMultipleMode(boolean)
     */
    void setMultipleMode(boolean m);

    /**
     * Returns the preferred size for a list with the specified number of rows.
     *
     * @param rows the number of rows
     *
     * @return the preferred size of the list
     *
     * @see List#getPreferredSize(int)
     */
    Dimension getPreferredSize(int rows);

    /**
     * Returns the minimum size for a list with the specified number of rows.
     *
     * @param rows the number of rows
     *
     * @return the minimum size of the list
     *
     * @see List#getMinimumSize(int)
     */
    Dimension getMinimumSize(int rows);

}
