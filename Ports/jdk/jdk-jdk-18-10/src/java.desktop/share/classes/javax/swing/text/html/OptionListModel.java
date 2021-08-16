/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import javax.swing.*;
import javax.swing.event.*;
import java.util.BitSet;
import java.io.Serializable;


/**
 * This class extends DefaultListModel, and also implements
 * the ListSelectionModel interface, allowing for it to store state
 * relevant to a SELECT form element which is implemented as a List.
 * If SELECT has a size attribute whose value is greater than 1,
 * or if allows multiple selection then a JList is used to
 * represent it and the OptionListModel is used as its model.
 * It also stores the initial state of the JList, to ensure an
 * accurate reset, if the user requests a reset of the form.
 *
 * @author Sunita Mani
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class OptionListModel<E> extends DefaultListModel<E> implements ListSelectionModel, Serializable {


    private static final int MIN = -1;
    private static final int MAX = Integer.MAX_VALUE;
    private int selectionMode = SINGLE_SELECTION;
    private int minIndex = MAX;
    private int maxIndex = MIN;
    private int anchorIndex = -1;
    private int leadIndex = -1;
    private int firstChangedIndex = MAX;
    private int lastChangedIndex = MIN;
    private boolean isAdjusting = false;
    private BitSet value = new BitSet(32);
    private BitSet initialValue = new BitSet(32);
    protected EventListenerList listenerList = new EventListenerList();

    protected boolean leadAnchorNotificationEnabled = true;

    public int getMinSelectionIndex() { return isSelectionEmpty() ? -1 : minIndex; }

    public int getMaxSelectionIndex() { return maxIndex; }

    public boolean getValueIsAdjusting() { return isAdjusting; }

    public int getSelectionMode() { return selectionMode; }

    public void setSelectionMode(int selectionMode) {
        switch (selectionMode) {
        case SINGLE_SELECTION:
        case SINGLE_INTERVAL_SELECTION:
        case MULTIPLE_INTERVAL_SELECTION:
            this.selectionMode = selectionMode;
            break;
        default:
            throw new IllegalArgumentException("invalid selectionMode");
        }
    }

    public boolean isSelectedIndex(int index) {
        return ((index < minIndex) || (index > maxIndex)) ? false : value.get(index);
    }

    public boolean isSelectionEmpty() {
        return (minIndex > maxIndex);
    }

    public void addListSelectionListener(ListSelectionListener l) {
        listenerList.add(ListSelectionListener.class, l);
    }

    public void removeListSelectionListener(ListSelectionListener l) {
        listenerList.remove(ListSelectionListener.class, l);
    }

    /**
     * Returns an array of all the <code>ListSelectionListener</code>s added
     * to this OptionListModel with addListSelectionListener().
     *
     * @return all of the <code>ListSelectionListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public ListSelectionListener[] getListSelectionListeners() {
        return listenerList.getListeners(ListSelectionListener.class);
    }

    /**
     * Notify listeners that we are beginning or ending a
     * series of value changes
     */
    protected void fireValueChanged(boolean isAdjusting) {
        fireValueChanged(getMinSelectionIndex(), getMaxSelectionIndex(), isAdjusting);
    }


    /**
     * Notify ListSelectionListeners that the value of the selection,
     * in the closed interval firstIndex,lastIndex, has changed.
     */
    protected void fireValueChanged(int firstIndex, int lastIndex) {
        fireValueChanged(firstIndex, lastIndex, getValueIsAdjusting());
    }

    /**
     * @param firstIndex The first index in the interval.
     * @param lastIndex The last index in the interval.
     * @param isAdjusting True if this is the final change in a series of them.
     * @see EventListenerList
     */
    protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting)
    {
        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListSelectionListener.class) {
                if (e == null) {
                    e = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
                }
                ((ListSelectionListener)listeners[i+1]).valueChanged(e);
            }
        }
    }

    private void fireValueChanged() {
        if (lastChangedIndex == MIN) {
            return;
        }
        /* Change the values before sending the event to the
         * listeners in case the event causes a listener to make
         * another change to the selection.
         */
        int oldFirstChangedIndex = firstChangedIndex;
        int oldLastChangedIndex = lastChangedIndex;
        firstChangedIndex = MAX;
        lastChangedIndex = MIN;
        fireValueChanged(oldFirstChangedIndex, oldLastChangedIndex);
    }


    // Update first and last change indices
    private void markAsDirty(int r) {
        firstChangedIndex = Math.min(firstChangedIndex, r);
        lastChangedIndex =  Math.max(lastChangedIndex, r);
    }

    // Set the state at this index and update all relevant state.
    private void set(int r) {
        if (value.get(r)) {
            return;
        }
        value.set(r);
        Option option = (Option)get(r);
        option.setSelection(true);
        markAsDirty(r);

        // Update minimum and maximum indices
        minIndex = Math.min(minIndex, r);
        maxIndex = Math.max(maxIndex, r);
    }

    // Clear the state at this index and update all relevant state.
    private void clear(int r) {
        if (!value.get(r)) {
            return;
        }
        value.clear(r);
        Option option = (Option)get(r);
        option.setSelection(false);
        markAsDirty(r);

        // Update minimum and maximum indices
        /*
           If (r > minIndex) the minimum has not changed.
           The case (r < minIndex) is not possible because r'th value was set.
           We only need to check for the case when lowest entry has been cleared,
           and in this case we need to search for the first value set above it.
        */
        if (r == minIndex) {
            for(minIndex = minIndex + 1; minIndex <= maxIndex; minIndex++) {
                if (value.get(minIndex)) {
                    break;
                }
            }
        }
        /*
           If (r < maxIndex) the maximum has not changed.
           The case (r > maxIndex) is not possible because r'th value was set.
           We only need to check for the case when highest entry has been cleared,
           and in this case we need to search for the first value set below it.
        */
        if (r == maxIndex) {
            for(maxIndex = maxIndex - 1; minIndex <= maxIndex; maxIndex--) {
                if (value.get(maxIndex)) {
                    break;
                }
            }
        }
        /* Performance note: This method is called from inside a loop in
           changeSelection() but we will only iterate in the loops
           above on the basis of one iteration per deselected cell - in total.
           Ie. the next time this method is called the work of the previous
           deselection will not be repeated.

           We also don't need to worry about the case when the min and max
           values are in their unassigned states. This cannot happen because
           this method's initial check ensures that the selection was not empty
           and therefore that the minIndex and maxIndex had 'real' values.

           If we have cleared the whole selection, set the minIndex and maxIndex
           to their cannonical values so that the next set command always works
           just by using Math.min and Math.max.
        */
        if (isSelectionEmpty()) {
            minIndex = MAX;
            maxIndex = MIN;
        }
    }

    /**
     * Sets the value of the leadAnchorNotificationEnabled flag.
     * @see             #isLeadAnchorNotificationEnabled()
     */
    public void setLeadAnchorNotificationEnabled(boolean flag) {
        leadAnchorNotificationEnabled = flag;
    }

    /**
     * Returns the value of the leadAnchorNotificationEnabled flag.
     * When leadAnchorNotificationEnabled is true the model
     * generates notification events with bounds that cover all the changes to
     * the selection plus the changes to the lead and anchor indices.
     * Setting the flag to false causes a norrowing of the event's bounds to
     * include only the elements that have been selected or deselected since
     * the last change. Either way, the model continues to maintain the lead
     * and anchor variables internally. The default is true.
     * @return          the value of the leadAnchorNotificationEnabled flag
     * @see             #setLeadAnchorNotificationEnabled(boolean)
     */
    public boolean isLeadAnchorNotificationEnabled() {
        return leadAnchorNotificationEnabled;
    }

    private void updateLeadAnchorIndices(int anchorIndex, int leadIndex) {
        if (leadAnchorNotificationEnabled) {
            if (this.anchorIndex != anchorIndex) {
                if (this.anchorIndex != -1) { // The unassigned state.
                    markAsDirty(this.anchorIndex);
                }
                markAsDirty(anchorIndex);
            }

            if (this.leadIndex != leadIndex) {
                if (this.leadIndex != -1) { // The unassigned state.
                    markAsDirty(this.leadIndex);
                }
                markAsDirty(leadIndex);
            }
        }
        this.anchorIndex = anchorIndex;
        this.leadIndex = leadIndex;
    }

    private boolean contains(int a, int b, int i) {
        return (i >= a) && (i <= b);
    }

    private void changeSelection(int clearMin, int clearMax,
                                 int setMin, int setMax, boolean clearFirst) {
        for(int i = Math.min(setMin, clearMin); i <= Math.max(setMax, clearMax); i++) {

            boolean shouldClear = contains(clearMin, clearMax, i);
            boolean shouldSet = contains(setMin, setMax, i);

            if (shouldSet && shouldClear) {
                if (clearFirst) {
                    shouldClear = false;
                }
                else {
                    shouldSet = false;
                }
            }

            if (shouldSet) {
                set(i);
            }
            if (shouldClear) {
                clear(i);
            }
        }
        fireValueChanged();
    }

   /*   Change the selection with the effect of first clearing the values
    *   in the inclusive range [clearMin, clearMax] then setting the values
    *   in the inclusive range [setMin, setMax]. Do this in one pass so
    *   that no values are cleared if they would later be set.
    */
    private void changeSelection(int clearMin, int clearMax, int setMin, int setMax) {
        changeSelection(clearMin, clearMax, setMin, setMax, true);
    }

    public void clearSelection() {
        removeSelectionInterval(minIndex, maxIndex);
    }

    public void setSelectionInterval(int index0, int index1) {
        if (index0 == -1 || index1 == -1) {
            return;
        }

        if (getSelectionMode() == SINGLE_SELECTION) {
            index0 = index1;
        }

        updateLeadAnchorIndices(index0, index1);

        int clearMin = minIndex;
        int clearMax = maxIndex;
        int setMin = Math.min(index0, index1);
        int setMax = Math.max(index0, index1);
        changeSelection(clearMin, clearMax, setMin, setMax);
    }

    public void addSelectionInterval(int index0, int index1)
    {
        if (index0 == -1 || index1 == -1) {
            return;
        }

        if (getSelectionMode() != MULTIPLE_INTERVAL_SELECTION) {
            setSelectionInterval(index0, index1);
            return;
        }

        updateLeadAnchorIndices(index0, index1);

        int clearMin = MAX;
        int clearMax = MIN;
        int setMin = Math.min(index0, index1);
        int setMax = Math.max(index0, index1);
        changeSelection(clearMin, clearMax, setMin, setMax);
    }


    public void removeSelectionInterval(int index0, int index1)
    {
        if (index0 == -1 || index1 == -1) {
            return;
        }

        updateLeadAnchorIndices(index0, index1);

        int clearMin = Math.min(index0, index1);
        int clearMax = Math.max(index0, index1);
        int setMin = MAX;
        int setMax = MIN;
        changeSelection(clearMin, clearMax, setMin, setMax);
    }

    private void setState(int index, boolean state) {
        if (state) {
            set(index);
        }
        else {
            clear(index);
        }
    }

    /**
     * Insert length indices beginning before/after index. If the value
     * at index is itself selected, set all of the newly inserted
     * items, otherwise leave them unselected. This method is typically
     * called to sync the selection model with a corresponding change
     * in the data model.
     */
    public void insertIndexInterval(int index, int length, boolean before)
    {
        /* The first new index will appear at insMinIndex and the last
         * one will appear at insMaxIndex
         */
        int insMinIndex = (before) ? index : index + 1;
        int insMaxIndex = (insMinIndex + length) - 1;

        /* Right shift the entire bitset by length, beginning with
         * index-1 if before is true, index+1 if it's false (i.e. with
         * insMinIndex).
         */
        for(int i = maxIndex; i >= insMinIndex; i--) {
            setState(i + length, value.get(i));
        }

        /* Initialize the newly inserted indices.
         */
        boolean setInsertedValues = value.get(index);
        for(int i = insMinIndex; i <= insMaxIndex; i++) {
            setState(i, setInsertedValues);
        }
    }


    /**
     * Remove the indices in the interval index0,index1 (inclusive) from
     * the selection model.  This is typically called to sync the selection
     * model width a corresponding change in the data model.  Note
     * that (as always) index0 can be greater than index1.
     */
    public void removeIndexInterval(int index0, int index1)
    {
        int rmMinIndex = Math.min(index0, index1);
        int rmMaxIndex = Math.max(index0, index1);
        int gapLength = (rmMaxIndex - rmMinIndex) + 1;

        /* Shift the entire bitset to the left to close the index0, index1
         * gap.
         */
        for(int i = rmMinIndex; i <= maxIndex; i++) {
            setState(i, value.get(i + gapLength));
        }
    }


    public void setValueIsAdjusting(boolean isAdjusting) {
        if (isAdjusting != this.isAdjusting) {
            this.isAdjusting = isAdjusting;
            this.fireValueChanged(isAdjusting);
        }
    }


    public String toString() {
        String s =  ((getValueIsAdjusting()) ? "~" : "=") + value.toString();
        return getClass().getName() + " " + Integer.toString(hashCode()) + " " + s;
    }

    /**
     * Returns a clone of the receiver with the same selection.
     * <code>listenerLists</code> are not duplicated.
     *
     * @return a clone of the receiver
     * @exception CloneNotSupportedException if the receiver does not
     *    both (a) implement the <code>Cloneable</code> interface
     *    and (b) define a <code>clone</code> method
     */
    public Object clone() throws CloneNotSupportedException {
        @SuppressWarnings("unchecked")
        OptionListModel<E> clone = (OptionListModel)super.clone();
        clone.value = (BitSet)value.clone();
        clone.listenerList = new EventListenerList();
        return clone;
    }

    public int getAnchorSelectionIndex() {
        return anchorIndex;
    }

    public int getLeadSelectionIndex() {
        return leadIndex;
    }

    /**
     * Set the anchor selection index, leaving all selection values unchanged.
     *
     * @see #getAnchorSelectionIndex
     * @see #setLeadSelectionIndex
     */
    public void setAnchorSelectionIndex(int anchorIndex) {
        this.anchorIndex = anchorIndex;
    }

    /**
     * Set the lead selection index, ensuring that values between the
     * anchor and the new lead are either all selected or all deselected.
     * If the value at the anchor index is selected, first clear all the
     * values in the range [anchor, oldLeadIndex], then select all the values
     * values in the range [anchor, newLeadIndex], where oldLeadIndex is the old
     * leadIndex and newLeadIndex is the new one.
     * <p>
     * If the value at the anchor index is not selected, do the same thing in reverse,
     * selecting values in the old range and deselecting values in the new one.
     * <p>
     * Generate a single event for this change and notify all listeners.
     * For the purposes of generating minimal bounds in this event, do the
     * operation in a single pass; that way the first and last index inside the
     * ListSelectionEvent that is broadcast will refer to cells that actually
     * changed value because of this method. If, instead, this operation were
     * done in two steps the effect on the selection state would be the same
     * but two events would be generated and the bounds around the changed values
     * would be wider, including cells that had been first cleared and only
     * to later be set.
     * <p>
     * This method can be used in the mouseDragged() method of a UI class
     * to extend a selection.
     *
     * @see #getLeadSelectionIndex
     * @see #setAnchorSelectionIndex
     */
    public void setLeadSelectionIndex(int leadIndex) {
        int anchorIndex = this.anchorIndex;
        if (getSelectionMode() == SINGLE_SELECTION) {
            anchorIndex = leadIndex;
        }

        int oldMin = Math.min(this.anchorIndex, this.leadIndex);
        int oldMax = Math.max(this.anchorIndex, this.leadIndex);
        int newMin = Math.min(anchorIndex, leadIndex);
        int newMax = Math.max(anchorIndex, leadIndex);
        if (value.get(this.anchorIndex)) {
            changeSelection(oldMin, oldMax, newMin, newMax);
        }
        else {
            changeSelection(newMin, newMax, oldMin, oldMax, false);
        }
        this.anchorIndex = anchorIndex;
        this.leadIndex = leadIndex;
    }


    /**
     * This method is responsible for storing the state
     * of the initial selection.  If the selectionMode
     * is the default, i.e allowing only for SINGLE_SELECTION,
     * then the very last OPTION that has the selected
     * attribute set wins.
     */
    public void setInitialSelection(int i) {
        if (initialValue.get(i)) {
            return;
        }
        if (selectionMode == SINGLE_SELECTION) {
            // reset to empty
            initialValue.and(new BitSet());
        }
        initialValue.set(i);
    }

    /**
     * Fetches the BitSet that represents the initial
     * set of selected items in the list.
     */
    public BitSet getInitialSelection() {
        return initialValue;
    }
}
