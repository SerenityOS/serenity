/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EventListener;
import java.util.BitSet;
import java.io.Serializable;
import java.beans.Transient;

import javax.swing.event.*;


/**
 * Default data model for list selections.
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
 * @author Philip Milne
 * @author Hans Muller
 * @see ListSelectionModel
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultListSelectionModel implements ListSelectionModel, Cloneable, Serializable
{
    private static final int MIN = -1;
    private static final int MAX = Integer.MAX_VALUE;
    private int selectionMode = MULTIPLE_INTERVAL_SELECTION;
    private int minIndex = MAX;
    private int maxIndex = MIN;
    private int anchorIndex = -1;
    private int leadIndex = -1;
    private int firstAdjustedIndex = MAX;
    private int lastAdjustedIndex = MIN;
    private boolean isAdjusting = false;

    private int firstChangedIndex = MAX;
    private int lastChangedIndex = MIN;

    private BitSet value = new BitSet(32);
    /**
     * The list of listeners.
     */
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * Whether or not the lead anchor notification is enabled.
     */
    protected boolean leadAnchorNotificationEnabled = true;

    /**
     * Constructs a {@code DefaultListSelectionModel}.
     */
    public DefaultListSelectionModel() {}

    /** {@inheritDoc} */
    public int getMinSelectionIndex() { return isSelectionEmpty() ? -1 : minIndex; }

    /** {@inheritDoc} */
    public int getMaxSelectionIndex() { return maxIndex; }

    /** {@inheritDoc} */
    public boolean getValueIsAdjusting() { return isAdjusting; }

    /** {@inheritDoc} */
    public int getSelectionMode() { return selectionMode; }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public void setSelectionMode(int selectionMode) {
        int oldMode = this.selectionMode;
        switch (selectionMode) {
            case SINGLE_SELECTION:
            case SINGLE_INTERVAL_SELECTION:
            case MULTIPLE_INTERVAL_SELECTION:
                this.selectionMode = selectionMode;
                break;
            default:
                throw new IllegalArgumentException("invalid selectionMode");
        }

        /*
        This code will only be executed when selection needs to be updated on
        changing selection mode. It will happen only if selection mode is changed
        from MULTIPLE_INTERVAL to SINGLE_INTERVAL or SINGLE or from
        SINGLE_INTERVAL to SINGLE
         */
        if (oldMode > this.selectionMode) {
            if (this.selectionMode == SINGLE_SELECTION) {
                if (!isSelectionEmpty()) {
                    setSelectionInterval(minIndex, minIndex);
                }
            } else if (this.selectionMode == SINGLE_INTERVAL_SELECTION) {
                if(!isSelectionEmpty()) {
                    int selectionEndindex = minIndex;
                    while (value.get(selectionEndindex + 1)) {
                        selectionEndindex++;
                    }
                    setSelectionInterval(minIndex, selectionEndindex);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public boolean isSelectedIndex(int index) {
        return ((index < minIndex) || (index > maxIndex)) ? false : value.get(index);
    }

    /** {@inheritDoc} */
    public boolean isSelectionEmpty() {
        return (minIndex > maxIndex);
    }

    /** {@inheritDoc} */
    public void addListSelectionListener(ListSelectionListener l) {
        listenerList.add(ListSelectionListener.class, l);
    }

    /** {@inheritDoc} */
    public void removeListSelectionListener(ListSelectionListener l) {
        listenerList.remove(ListSelectionListener.class, l);
    }

    /**
     * Returns an array of all the list selection listeners
     * registered on this <code>DefaultListSelectionModel</code>.
     *
     * @return all of this model's <code>ListSelectionListener</code>s
     *         or an empty
     *         array if no list selection listeners are currently registered
     *
     * @see #addListSelectionListener
     * @see #removeListSelectionListener
     *
     * @since 1.4
     */
    public ListSelectionListener[] getListSelectionListeners() {
        return listenerList.getListeners(ListSelectionListener.class);
    }

    /**
     * Notifies listeners that we have ended a series of adjustments.
     * @param isAdjusting true if this is the final change in a series of
     *          adjustments
     */
    protected void fireValueChanged(boolean isAdjusting) {
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
        fireValueChanged(oldFirstChangedIndex, oldLastChangedIndex, isAdjusting);
    }


    /**
     * Notifies <code>ListSelectionListeners</code> that the value
     * of the selection, in the closed interval <code>firstIndex</code>,
     * <code>lastIndex</code>, has changed.
     *
     * @param firstIndex the first index in the interval
     * @param lastIndex the last index in the interval
     */
    protected void fireValueChanged(int firstIndex, int lastIndex) {
        fireValueChanged(firstIndex, lastIndex, getValueIsAdjusting());
    }

    /**
     * @param firstIndex the first index in the interval
     * @param lastIndex the last index in the interval
     * @param isAdjusting true if this is the final change in a series of
     *          adjustments
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
        if (lastAdjustedIndex == MIN) {
            return;
        }
        /* If getValueAdjusting() is true, (eg. during a drag opereration)
         * record the bounds of the changes so that, when the drag finishes (and
         * setValueAdjusting(false) is called) we can post a single event
         * with bounds covering all of these individual adjustments.
         */
        if (getValueIsAdjusting()) {
            firstChangedIndex = Math.min(firstChangedIndex, firstAdjustedIndex);
            lastChangedIndex = Math.max(lastChangedIndex, lastAdjustedIndex);
        }
        /* Change the values before sending the event to the
         * listeners in case the event causes a listener to make
         * another change to the selection.
         */
        int oldFirstAdjustedIndex = firstAdjustedIndex;
        int oldLastAdjustedIndex = lastAdjustedIndex;
        firstAdjustedIndex = MAX;
        lastAdjustedIndex = MIN;

        fireValueChanged(oldFirstAdjustedIndex, oldLastAdjustedIndex);
    }

    /**
     * Returns an array of all the objects currently registered as
     * <code><em>Foo</em>Listener</code>s
     * upon this model.
     * <code><em>Foo</em>Listener</code>s
     * are registered using the <code>add<em>Foo</em>Listener</code> method.
     * <p>
     * You can specify the <code>listenerType</code> argument
     * with a class literal, such as <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a <code>DefaultListSelectionModel</code>
     * instance <code>m</code>
     * for its list selection listeners
     * with the following code:
     *
     * <pre>ListSelectionListener[] lsls = (ListSelectionListener[])(m.getListeners(ListSelectionListener.class));</pre>
     *
     * If no such listeners exist,
     * this method returns an empty array.
     *
     * @param <T> the type of {@code EventListener} class being requested
     * @param listenerType  the type of listeners requested;
     *          this parameter should specify an interface
     *          that descends from <code>java.util.EventListener</code>
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s
     *          on this model,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if <code>listenerType</code> doesn't
     *          specify a class or interface that implements
     *          <code>java.util.EventListener</code>
     *
     * @see #getListSelectionListeners
     *
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    // Updates first and last change indices
    private void markAsDirty(int r) {
        if (r == -1) {
            return;
        }

        firstAdjustedIndex = Math.min(firstAdjustedIndex, r);
        lastAdjustedIndex =  Math.max(lastAdjustedIndex, r);
    }

    // Sets the state at this index and update all relevant state.
    private void set(int r) {
        if (value.get(r)) {
            return;
        }
        value.set(r);
        markAsDirty(r);

        // Update minimum and maximum indices
        minIndex = Math.min(minIndex, r);
        maxIndex = Math.max(maxIndex, r);
    }

    // Clears the state at this index and update all relevant state.
    private void clear(int r) {
        if (!value.get(r)) {
            return;
        }
        value.clear(r);
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
     *
     * @param flag boolean value for {@code leadAnchorNotificationEnabled}
     * @see             #isLeadAnchorNotificationEnabled()
     */
    public void setLeadAnchorNotificationEnabled(boolean flag) {
        leadAnchorNotificationEnabled = flag;
    }

    /**
     * Returns the value of the <code>leadAnchorNotificationEnabled</code> flag.
     * When <code>leadAnchorNotificationEnabled</code> is true the model
     * generates notification events with bounds that cover all the changes to
     * the selection plus the changes to the lead and anchor indices.
     * Setting the flag to false causes a narrowing of the event's bounds to
     * include only the elements that have been selected or deselected since
     * the last change. Either way, the model continues to maintain the lead
     * and anchor variables internally. The default is true.
     * <p>
     * Note: It is possible for the lead or anchor to be changed without a
     * change to the selection. Notification of these changes is often
     * important, such as when the new lead or anchor needs to be updated in
     * the view. Therefore, caution is urged when changing the default value.
     *
     * @return  the value of the <code>leadAnchorNotificationEnabled</code> flag
     * @see             #setLeadAnchorNotificationEnabled(boolean)
     */
    public boolean isLeadAnchorNotificationEnabled() {
        return leadAnchorNotificationEnabled;
    }

    private void updateLeadAnchorIndices(int anchorIndex, int leadIndex) {
        if (leadAnchorNotificationEnabled) {
            if (this.anchorIndex != anchorIndex) {
                markAsDirty(this.anchorIndex);
                markAsDirty(anchorIndex);
            }

            if (this.leadIndex != leadIndex) {
                markAsDirty(this.leadIndex);
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

   /**
    * Change the selection with the effect of first clearing the values
    * in the inclusive range [clearMin, clearMax] then setting the values
    * in the inclusive range [setMin, setMax]. Do this in one pass so
    * that no values are cleared if they would later be set.
    */
    private void changeSelection(int clearMin, int clearMax, int setMin, int setMax) {
        changeSelection(clearMin, clearMax, setMin, setMax, true);
    }

    /** {@inheritDoc} */
    public void clearSelection() {
        removeSelectionIntervalImpl(minIndex, maxIndex, false);
    }

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
     * <p>
     * If either index is {@code -1}, this method does nothing and returns
     * without exception. Otherwise, if either index is less than {@code -1},
     * an {@code IndexOutOfBoundsException} is thrown.
     *
     * @param index0 one end of the interval.
     * @param index1 other end of the interval
     * @throws IndexOutOfBoundsException if either index is less than {@code -1}
     *         (and neither index is {@code -1})
     * @see #addListSelectionListener
     */
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

    /**
     * Changes the selection to be the set union of the current selection
     * and the indices between {@code index0} and {@code index1} inclusive.
     * <p>
     * In {@code SINGLE_SELECTION} selection mode, this is equivalent
     * to calling {@code setSelectionInterval}, and only the second index
     * is used. In {@code SINGLE_INTERVAL_SELECTION} selection mode, this
     * method behaves like {@code setSelectionInterval}, unless the given
     * interval is immediately adjacent to or overlaps the existing selection,
     * and can therefore be used to grow it.
     * <p>
     * If this represents a change to the current selection, then each
     * {@code ListSelectionListener} is notified of the change. Note that
     * {@code index0} doesn't have to be less than or equal to {@code index1}.
     * <p>
     * If either index is {@code -1}, this method does nothing and returns
     * without exception. Otherwise, if either index is less than {@code -1},
     * an {@code IndexOutOfBoundsException} is thrown.
     *
     * @param index0 one end of the interval.
     * @param index1 other end of the interval
     * @throws IndexOutOfBoundsException if either index is less than {@code -1}
     *         (and neither index is {@code -1})
     * @see #addListSelectionListener
     * @see #setSelectionInterval
     */
    public void addSelectionInterval(int index0, int index1)
    {
        if (index0 == -1 || index1 == -1) {
            return;
        }

        // If we only allow a single selection, channel through
        // setSelectionInterval() to enforce the rule.
        if (getSelectionMode() == SINGLE_SELECTION) {
            setSelectionInterval(index0, index1);
            return;
        }

        updateLeadAnchorIndices(index0, index1);

        int clearMin = MAX;
        int clearMax = MIN;
        int setMin = Math.min(index0, index1);
        int setMax = Math.max(index0, index1);

        // If we only allow a single interval and this would result
        // in multiple intervals, then set the selection to be just
        // the new range.
        if (getSelectionMode() == SINGLE_INTERVAL_SELECTION &&
                (setMax < minIndex - 1 || setMin > maxIndex + 1)) {

            setSelectionInterval(index0, index1);
            return;
        }

        changeSelection(clearMin, clearMax, setMin, setMax);
    }


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
     * <p>
     * If either index is {@code -1}, this method does nothing and returns
     * without exception. Otherwise, if either index is less than {@code -1},
     * an {@code IndexOutOfBoundsException} is thrown.
     *
     * @param index0 one end of the interval
     * @param index1 other end of the interval
     * @throws IndexOutOfBoundsException if either index is less than {@code -1}
     *         (and neither index is {@code -1})
     * @see #addListSelectionListener
     */
    public void removeSelectionInterval(int index0, int index1)
    {
        removeSelectionIntervalImpl(index0, index1, true);
    }

    // private implementation allowing the selection interval
    // to be removed without affecting the lead and anchor
    private void removeSelectionIntervalImpl(int index0, int index1,
                                             boolean changeLeadAnchor) {

        if (index0 == -1 || index1 == -1) {
            return;
        }

        if (changeLeadAnchor) {
            updateLeadAnchorIndices(index0, index1);
        }

        int clearMin = Math.min(index0, index1);
        int clearMax = Math.max(index0, index1);
        int setMin = MAX;
        int setMax = MIN;

        // If the removal would produce to two disjoint selections in a mode
        // that only allows one, extend the removal to the end of the selection.
        if (getSelectionMode() != MULTIPLE_INTERVAL_SELECTION &&
               clearMin > minIndex && clearMax < maxIndex) {
            clearMax = maxIndex;
        }

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
     * at index is itself selected and the selection mode is not
     * SINGLE_SELECTION, set all of the newly inserted items as selected.
     * Otherwise leave them unselected. This method is typically
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
        boolean setInsertedValues = ((getSelectionMode() == SINGLE_SELECTION) ?
                                        false : value.get(index));
        for(int i = insMinIndex; i <= insMaxIndex; i++) {
            setState(i, setInsertedValues);
        }

        int leadIndex = this.leadIndex;
        if (leadIndex > index || (before && leadIndex == index)) {
            leadIndex = this.leadIndex + length;
        }
        int anchorIndex = this.anchorIndex;
        if (anchorIndex > index || (before && anchorIndex == index)) {
            anchorIndex = this.anchorIndex + length;
        }
        if (leadIndex != this.leadIndex || anchorIndex != this.anchorIndex) {
            updateLeadAnchorIndices(anchorIndex, leadIndex);
        }

        fireValueChanged();
    }


    /**
     * Remove the indices in the interval index0,index1 (inclusive) from
     * the selection model.  This is typically called to sync the selection
     * model width a corresponding change in the data model.  Note
     * that (as always) index0 need not be &lt;= index1.
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

        int leadIndex = this.leadIndex;
        if (leadIndex == 0 && rmMinIndex == 0) {
            // do nothing
        } else if (leadIndex > rmMaxIndex) {
            leadIndex = this.leadIndex - gapLength;
        } else if (leadIndex >= rmMinIndex) {
            leadIndex = rmMinIndex - 1;
        }

        int anchorIndex = this.anchorIndex;
        if (anchorIndex == 0 && rmMinIndex == 0) {
            // do nothing
        } else if (anchorIndex > rmMaxIndex) {
            anchorIndex = this.anchorIndex - gapLength;
        } else if (anchorIndex >= rmMinIndex) {
            anchorIndex = rmMinIndex - 1;
        }

        if (leadIndex != this.leadIndex || anchorIndex != this.anchorIndex) {
            updateLeadAnchorIndices(anchorIndex, leadIndex);
        }

        fireValueChanged();
    }


    /** {@inheritDoc} */
    public void setValueIsAdjusting(boolean isAdjusting) {
        if (isAdjusting != this.isAdjusting) {
            this.isAdjusting = isAdjusting;
            this.fireValueChanged(isAdjusting);
        }
    }


    /**
     * Returns a string that displays and identifies this
     * object's properties.
     *
     * @return a <code>String</code> representation of this object
     */
    public String toString() {
        String s =  ((getValueIsAdjusting()) ? "~" : "=") + value.toString();
        return getClass().getName() + " " + Integer.toString(hashCode()) + " " + s;
    }

    /**
     * Returns a clone of this selection model with the same selection.
     * <code>listenerLists</code> are not duplicated.
     *
     * @exception CloneNotSupportedException if the selection model does not
     *    both (a) implement the Cloneable interface and (b) define a
     *    <code>clone</code> method.
     */
    public Object clone() throws CloneNotSupportedException {
        DefaultListSelectionModel clone = (DefaultListSelectionModel)super.clone();
        clone.value = (BitSet)value.clone();
        clone.listenerList = new EventListenerList();
        return clone;
    }

    /** {@inheritDoc} */
    @Transient
    public int getAnchorSelectionIndex() {
        return anchorIndex;
    }

    /** {@inheritDoc} */
    @Transient
    public int getLeadSelectionIndex() {
        return leadIndex;
    }

    /**
     * Set the anchor selection index, leaving all selection values unchanged.
     * If leadAnchorNotificationEnabled is true, send a notification covering
     * the old and new anchor cells.
     *
     * @see #getAnchorSelectionIndex
     * @see #setLeadSelectionIndex
     */
    public void setAnchorSelectionIndex(int anchorIndex) {
        updateLeadAnchorIndices(anchorIndex, this.leadIndex);
        fireValueChanged();
    }

    /**
     * Set the lead selection index, leaving all selection values unchanged.
     * If leadAnchorNotificationEnabled is true, send a notification covering
     * the old and new lead cells.
     *
     * @param leadIndex the new lead selection index
     *
     * @see #setAnchorSelectionIndex
     * @see #setLeadSelectionIndex
     * @see #getLeadSelectionIndex
     *
     * @since 1.5
     */
    public void moveLeadSelectionIndex(int leadIndex) {
        // disallow a -1 lead unless the anchor is already -1
        if (leadIndex == -1) {
            if (this.anchorIndex != -1) {
                return;
            }

/* PENDING(shannonh) - The following check is nice, to be consistent with
                       setLeadSelectionIndex. However, it is not absolutely
                       necessary: One could work around it by setting the anchor
                       to something valid, modifying the lead, and then moving
                       the anchor back to -1. For this reason, there's no sense
                       in adding it at this time, as that would require
                       updating the spec and officially committing to it.

        // otherwise, don't do anything if the anchor is -1
        } else if (this.anchorIndex == -1) {
            return;
*/

        }

        updateLeadAnchorIndices(this.anchorIndex, leadIndex);
        fireValueChanged();
    }

    /**
     * Sets the lead selection index, ensuring that values between the
     * anchor and the new lead are either all selected or all deselected.
     * If the value at the anchor index is selected, first clear all the
     * values in the range [anchor, oldLeadIndex], then select all the values
     * values in the range [anchor, newLeadIndex], where oldLeadIndex is the old
     * leadIndex and newLeadIndex is the new one.
     * <p>
     * If the value at the anchor index is not selected, do the same thing in
     * reverse selecting values in the old range and deselecting values in the
     * new one.
     * <p>
     * Generate a single event for this change and notify all listeners.
     * For the purposes of generating minimal bounds in this event, do the
     * operation in a single pass; that way the first and last index inside the
     * ListSelectionEvent that is broadcast will refer to cells that actually
     * changed value because of this method. If, instead, this operation were
     * done in two steps the effect on the selection state would be the same
     * but two events would be generated and the bounds around the changed
     * values would be wider, including cells that had been first cleared only
     * to later be set.
     * <p>
     * This method can be used in the <code>mouseDragged</code> method
     * of a UI class to extend a selection.
     *
     * @see #getLeadSelectionIndex
     * @see #setAnchorSelectionIndex
     */
    public void setLeadSelectionIndex(int leadIndex) {
        int anchorIndex = this.anchorIndex;

        // only allow a -1 lead if the anchor is already -1
        if (leadIndex == -1) {
            if (anchorIndex == -1) {
                updateLeadAnchorIndices(anchorIndex, leadIndex);
                fireValueChanged();
            }

            return;
        // otherwise, don't do anything if the anchor is -1
        } else if (anchorIndex == -1) {
            return;
        }

        if (this.leadIndex == -1) {
            this.leadIndex = leadIndex;
        }

        boolean shouldSelect = value.get(this.anchorIndex);

        if (getSelectionMode() == SINGLE_SELECTION) {
            anchorIndex = leadIndex;
            shouldSelect = true;
        }

        int oldMin = Math.min(this.anchorIndex, this.leadIndex);
        int oldMax = Math.max(this.anchorIndex, this.leadIndex);
        int newMin = Math.min(anchorIndex, leadIndex);
        int newMax = Math.max(anchorIndex, leadIndex);

        updateLeadAnchorIndices(anchorIndex, leadIndex);

        if (shouldSelect) {
            changeSelection(oldMin, oldMax, newMin, newMax);
        }
        else {
            changeSelection(newMin, newMax, oldMin, oldMax, false);
        }
    }
}
