/*
 * Copyright (c) 1997, 2006, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import javax.swing.event.*;

/**
 * This interface represents the current state of the
 * selection for any of the components that display a
 * list of values with stable indices.  The selection is
 * modeled as a set of intervals, each interval represents
 * a contiguous range of selected list elements.
 * The methods for modifying the set of selected intervals
 * all take a pair of indices, index0 and index1, that represent
 * a closed interval, i.e. the interval includes both index0 and
 * index1.
 *
 * @author Hans Muller
 * @author Philip Milne
 * @see DefaultListSelectionModel
 * @since 1.2
 */

public interface ListSelectionModel
{
    /**
     * A value for the selectionMode property: select one list index
     * at a time.
     *
     * @see #setSelectionMode
     */
    int SINGLE_SELECTION = 0;

    /**
     * A value for the selectionMode property: select one contiguous
     * range of indices at a time.
     *
     * @see #setSelectionMode
     */
    int SINGLE_INTERVAL_SELECTION = 1;

    /**
     * A value for the selectionMode property: select one or more
     * contiguous ranges of indices at a time.
     *
     * @see #setSelectionMode
     */
    int MULTIPLE_INTERVAL_SELECTION = 2;


    /**
     * Changes the selection to be between {@code index0} and {@code index1}
     * inclusive. {@code index0} doesn't have to be less than or equal to
     * {@code index1}.
     * <p>
     * In {@code SINGLE_SELECTION} selection mode, only the second index
     * is used.
     * <p>
     * If this represents a change to the current selection, then each
     * {@code ListSelectionListener} is notified of the change.
     *
     * @param index0 one end of the interval.
     * @param index1 other end of the interval
     * @see #addListSelectionListener
     */
    void setSelectionInterval(int index0, int index1);


    /**
     * Changes the selection to be the set union of the current selection
     * and the indices between {@code index0} and {@code index1} inclusive.
     * {@code index0} doesn't have to be less than or equal to {@code index1}.
     * <p>
     * In {@code SINGLE_SELECTION} selection mode, this is equivalent
     * to calling {@code setSelectionInterval}, and only the second index
     * is used. In {@code SINGLE_INTERVAL_SELECTION} selection mode, this
     * method behaves like {@code setSelectionInterval}, unless the given
     * interval is immediately adjacent to or overlaps the existing selection,
     * and can therefore be used to grow the selection.
     * <p>
     * If this represents a change to the current selection, then each
     * {@code ListSelectionListener} is notified of the change.
     *
     * @param index0 one end of the interval.
     * @param index1 other end of the interval
     * @see #addListSelectionListener
     * @see #setSelectionInterval
     */
    void addSelectionInterval(int index0, int index1);


    /**
     * Changes the selection to be the set difference of the current selection
     * and the indices between {@code index0} and {@code index1} inclusive.
     * {@code index0} doesn't have to be less than or equal to {@code index1}.
     * <p>
     * In {@code SINGLE_INTERVAL_SELECTION} selection mode, if the removal
     * would produce two disjoint selections, the removal is extended through
     * the greater end of the selection. For example, if the selection is
     * {@code 0-10} and you supply indices {@code 5,6} (in any order) the
     * resulting selection is {@code 0-4}.
     * <p>
     * If this represents a change to the current selection, then each
     * {@code ListSelectionListener} is notified of the change.
     *
     * @param index0 one end of the interval.
     * @param index1 other end of the interval
     * @see #addListSelectionListener
     */
    void removeSelectionInterval(int index0, int index1);


    /**
     * Returns the first selected index or -1 if the selection is empty.
     *
     * @return the first selected index or -1 if the selection is empty.
     */
    int getMinSelectionIndex();


    /**
     * Returns the last selected index or -1 if the selection is empty.
     *
     * @return the last selected index or -1 if the selection is empty.
     */
    int getMaxSelectionIndex();


    /**
     * Returns true if the specified index is selected.
     *
     * @param index an index
     * @return {@code true} if the specified index is selected
     */
    boolean isSelectedIndex(int index);


    /**
     * Return the first index argument from the most recent call to
     * setSelectionInterval(), addSelectionInterval() or removeSelectionInterval().
     * The most recent index0 is considered the "anchor" and the most recent
     * index1 is considered the "lead".  Some interfaces display these
     * indices specially, e.g. Windows95 displays the lead index with a
     * dotted yellow outline.
     *
     * @return the anchor selection index
     * @see #getLeadSelectionIndex
     * @see #setSelectionInterval
     * @see #addSelectionInterval
     */
    int getAnchorSelectionIndex();


    /**
     * Set the anchor selection index.
     *
     * @param index the anchor selection index
     * @see #getAnchorSelectionIndex
     */
    void setAnchorSelectionIndex(int index);


    /**
     * Return the second index argument from the most recent call to
     * setSelectionInterval(), addSelectionInterval() or removeSelectionInterval().
     *
     * @return the lead selection index.
     * @see #getAnchorSelectionIndex
     * @see #setSelectionInterval
     * @see #addSelectionInterval
     */
    int getLeadSelectionIndex();

    /**
     * Set the lead selection index.
     *
     * @param index the lead selection index
     * @see #getLeadSelectionIndex
     */
    void setLeadSelectionIndex(int index);

    /**
     * Change the selection to the empty set.  If this represents
     * a change to the current selection then notify each ListSelectionListener.
     *
     * @see #addListSelectionListener
     */
    void clearSelection();

    /**
     * Returns true if no indices are selected.
     *
     * @return {@code true} if no indices are selected.
     */
    boolean isSelectionEmpty();

    /**
     * Insert {@code length} indices beginning before/after {@code index}. This is typically
     * called to sync the selection model with a corresponding change
     * in the data model.
     *
     * @param index the beginning of the interval
     * @param length the length of the interval
     * @param before if {@code true}, interval inserts before the {@code index},
     *               otherwise, interval inserts after the {@code index}
     */
    void insertIndexInterval(int index, int length, boolean before);

    /**
     * Remove the indices in the interval {@code index0,index1} (inclusive) from
     * the selection model.  This is typically called to sync the selection
     * model width a corresponding change in the data model.
     *
     * @param index0 the beginning of the interval
     * @param index1 the end of the interval
     */
    void removeIndexInterval(int index0, int index1);

    /**
     * Sets the {@code valueIsAdjusting} property, which indicates whether
     * or not upcoming selection changes should be considered part of a single
     * change. The value of this property is used to initialize the
     * {@code valueIsAdjusting} property of the {@code ListSelectionEvent}s that
     * are generated.
     * <p>
     * For example, if the selection is being updated in response to a user
     * drag, this property can be set to {@code true} when the drag is initiated
     * and set to {@code false} when the drag is finished. During the drag,
     * listeners receive events with a {@code valueIsAdjusting} property
     * set to {@code true}. At the end of the drag, when the change is
     * finalized, listeners receive an event with the value set to {@code false}.
     * Listeners can use this pattern if they wish to update only when a change
     * has been finalized.
     * <p>
     * Setting this property to {@code true} begins a series of changes that
     * is to be considered part of a single change. When the property is changed
     * back to {@code false}, an event is sent out characterizing the entire
     * selection change (if there was one), with the event's
     * {@code valueIsAdjusting} property set to {@code false}.
     *
     * @param valueIsAdjusting the new value of the property
     * @see #getValueIsAdjusting
     * @see javax.swing.event.ListSelectionEvent#getValueIsAdjusting
     */
    void setValueIsAdjusting(boolean valueIsAdjusting);

    /**
     * Returns {@code true} if the selection is undergoing a series of changes.
     *
     * @return true if the selection is undergoing a series of changes
     * @see #setValueIsAdjusting
     */
    boolean getValueIsAdjusting();

    /**
     * Sets the selection mode. The following list describes the accepted
     * selection modes:
     * <ul>
     * <li>{@code ListSelectionModel.SINGLE_SELECTION} -
     *   Only one list index can be selected at a time. In this mode,
     *   {@code setSelectionInterval} and {@code addSelectionInterval} are
     *   equivalent, both replacing the current selection with the index
     *   represented by the second argument (the "lead").
     * <li>{@code ListSelectionModel.SINGLE_INTERVAL_SELECTION} -
     *   Only one contiguous interval can be selected at a time.
     *   In this mode, {@code addSelectionInterval} behaves like
     *   {@code setSelectionInterval} (replacing the current selection),
     *   unless the given interval is immediately adjacent to or overlaps
     *   the existing selection, and can therefore be used to grow it.
     * <li>{@code ListSelectionModel.MULTIPLE_INTERVAL_SELECTION} -
     *   In this mode, there's no restriction on what can be selected.
     * </ul>
     *
     * @param selectionMode the selection mode
     * @see #getSelectionMode
     * @throws IllegalArgumentException if the selection mode isn't
     *         one of those allowed
     */
    void setSelectionMode(int selectionMode);

    /**
     * Returns the current selection mode.
     *
     * @return the current selection mode
     * @see #setSelectionMode
     */
    int getSelectionMode();

    /**
     * Add a listener to the list that's notified each time a change
     * to the selection occurs.
     *
     * @param x the ListSelectionListener
     * @see #removeListSelectionListener
     * @see #setSelectionInterval
     * @see #addSelectionInterval
     * @see #removeSelectionInterval
     * @see #clearSelection
     * @see #insertIndexInterval
     * @see #removeIndexInterval
     */
    void addListSelectionListener(ListSelectionListener x);

    /**
     * Remove a listener from the list that's notified each time a
     * change to the selection occurs.
     *
     * @param x the ListSelectionListener
     * @see #addListSelectionListener
     */
    void removeListSelectionListener(ListSelectionListener x);

    /**
     * Returns an array of all of the selected indices in the selection model,
     * in increasing order.
     *
     * @return all of the selected indices, in increasing order,
     *         or an empty array if nothing is selected
     * @see #removeSelectionInterval
     * @see #addListSelectionListener
     * @since 11
     * @implSpec The default implementation iterates from minimum selected
     * index {@link #getMinSelectionIndex()} to maximum selected index {@link
     * #getMaxSelectionIndex()} and returns the selected indices {@link
     * #isSelectedIndex(int)} in a newly allocated int array.
     */
    default int[] getSelectedIndices() {
        int iMin = getMinSelectionIndex();
        int iMax = getMaxSelectionIndex();

        if ((iMin < 0) || (iMax < 0)) {
            return new int[0];
        }

        int[] rvTmp = new int[1+ (iMax - iMin)];
        int n = 0;
        for(int i = iMin; i <= iMax; i++) {
            if (isSelectedIndex(i)) {
                rvTmp[n++] = i;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    /**
     * Returns the number of selected items.
     *
     * @return the number of selected items, 0 if no items are selected
     * @since 11
     * @implSpec The default implementation iterates from minimum selected
     * index {@link #getMinSelectionIndex()} to maximum selected index {@link
     * #getMaxSelectionIndex()} and returns the number of selected indices
     * {@link #isSelectedIndex(int)}
     */
    default int getSelectedItemsCount() {
        int iMin = getMinSelectionIndex();
        int iMax = getMaxSelectionIndex();
        int count = 0;

        for(int i = iMin; i <= iMax; i++) {
            if (isSelectedIndex(i)) {
                count++;
            }
        }
        return count;
    }
}
