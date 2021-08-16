/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.SortOrder;
import javax.swing.event.*;
import java.util.*;

/**
 * <code>RowSorter</code> provides the basis for sorting and filtering.
 * Beyond creating and installing a <code>RowSorter</code>, you very rarely
 * need to interact with one directly.  Refer to
 * {@link javax.swing.table.TableRowSorter TableRowSorter} for a concrete
 * implementation of <code>RowSorter</code> for <code>JTable</code>.
 * <p>
 * <code>RowSorter</code>'s primary role is to provide a mapping between
 * two coordinate systems: that of the view (for example a
 * <code>JTable</code>) and that of the underlying data source, typically a
 * model.
 * <p>
 * The view invokes the following methods on the <code>RowSorter</code>:
 * <ul>
 * <li><code>toggleSortOrder</code> &#8212; The view invokes this when the
 *     appropriate user gesture has occurred to trigger a sort.  For example,
 *     the user clicked a column header in a table.
 * <li>One of the model change methods &#8212; The view invokes a model
 *     change method when the underlying model
 *     has changed.  There may be order dependencies in how the events are
 *     delivered, so a <code>RowSorter</code> should not update its mapping
 *     until one of these methods is invoked.
 * </ul>
 * Because the view makes extensive use of  the
 * <code>convertRowIndexToModel</code>,
 * <code>convertRowIndexToView</code> and <code>getViewRowCount</code> methods,
 * these methods need to be fast.
 * <p>
 * <code>RowSorter</code> provides notification of changes by way of
 * <code>RowSorterListener</code>.  Two types of notification are sent:
 * <ul>
 * <li><code>RowSorterEvent.Type.SORT_ORDER_CHANGED</code> &#8212; notifies
 *     listeners that the sort order has changed.  This is typically followed
 *     by a notification that the sort has changed.
 * <li><code>RowSorterEvent.Type.SORTED</code> &#8212; notifies listeners that
 *     the mapping maintained by the <code>RowSorter</code> has changed in
 *     some way.
 * </ul>
 * <code>RowSorter</code> implementations typically don't have a one-to-one
 * mapping with the underlying model, but they can.
 * For example, if a database does the sorting,
 * <code>toggleSortOrder</code> might call through to the database
 * (on a background thread), and override the mapping methods to return the
 * argument that is passed in.
 * <p>
 * Concrete implementations of <code>RowSorter</code>
 * need to reference a model such as <code>TableModel</code> or
 * <code>ListModel</code>.  The view classes, such as
 * <code>JTable</code> and <code>JList</code>, will also have a
 * reference to the model.  To avoid ordering dependencies,
 * <code>RowSorter</code> implementations should not install a
 * listener on the model.  Instead the view class will call into the
 * <code>RowSorter</code> when the model changes.  For
 * example, if a row is updated in a <code>TableModel</code>
 * <code>JTable</code> invokes <code>rowsUpdated</code>.
 * When the model changes, the view may call into any of the following methods:
 * <code>modelStructureChanged</code>, <code>allRowsChanged</code>,
 * <code>rowsInserted</code>, <code>rowsDeleted</code> and
 * <code>rowsUpdated</code>.
 *
 * @param <M> the type of the underlying model
 * @see javax.swing.table.TableRowSorter
 * @since 1.6
 */
public abstract class RowSorter<M> {
    private EventListenerList listenerList = new EventListenerList();

    /**
     * Creates a <code>RowSorter</code>.
     */
    public RowSorter() {
    }

    /**
     * Returns the underlying model.
     *
     * @return the underlying model
     */
    public abstract M getModel();

    /**
     * Reverses the sort order of the specified column.  It is up to
     * subclasses to provide the exact behavior when invoked.  Typically
     * this will reverse the sort order from ascending to descending (or
     * descending to ascending) if the specified column is already the
     * primary sorted column; otherwise, makes the specified column
     * the primary sorted column, with an ascending sort order.  If
     * the specified column is not sortable, this method has no
     * effect.
     * <p>
     * If this results in changing the sort order and sorting, the
     * appropriate <code>RowSorterListener</code> notification will be
     * sent.
     *
     * @param column the column to toggle the sort ordering of, in
     *        terms of the underlying model
     * @throws IndexOutOfBoundsException if column is outside the range of
     *         the underlying model
     */
    public abstract void toggleSortOrder(int column);

    /**
     * Returns the location of <code>index</code> in terms of the
     * underlying model.  That is, for the row <code>index</code> in
     * the coordinates of the view this returns the row index in terms
     * of the underlying model.
     *
     * @param index the row index in terms of the underlying view
     * @return row index in terms of the view
     * @throws IndexOutOfBoundsException if <code>index</code> is outside the
     *         range of the view
     */
    public abstract int convertRowIndexToModel(int index);

    /**
     * Returns the location of <code>index</code> in terms of the
     * view.  That is, for the row <code>index</code> in the
     * coordinates of the underlying model this returns the row index
     * in terms of the view.
     *
     * @param index the row index in terms of the underlying model
     * @return row index in terms of the view, or -1 if index has been
     *         filtered out of the view
     * @throws IndexOutOfBoundsException if <code>index</code> is outside
     *         the range of the model
     */
    public abstract int convertRowIndexToView(int index);

    /**
     * Sets the current sort keys.
     *
     * @param keys the new <code>SortKeys</code>; <code>null</code>
     *        is a shorthand for specifying an empty list,
     *        indicating that the view should be unsorted
     */
    public abstract void setSortKeys(List<? extends SortKey> keys);

    /**
     * Returns the current sort keys.  This must return a {@code
     * non-null List} and may return an unmodifiable {@code List}. If
     * you need to change the sort keys, make a copy of the returned
     * {@code List}, mutate the copy and invoke {@code setSortKeys}
     * with the new list.
     *
     * @return the current sort order
     */
    public abstract List<? extends SortKey> getSortKeys();

    /**
     * Returns the number of rows in the view.  If the contents have
     * been filtered this might differ from the row count of the
     * underlying model.
     *
     * @return number of rows in the view
     * @see #getModelRowCount
     */
    public abstract int getViewRowCount();

    /**
     * Returns the number of rows in the underlying model.
     *
     * @return number of rows in the underlying model
     * @see #getViewRowCount
     */
    public abstract int getModelRowCount();

    /**
     * Invoked when the underlying model structure has completely
     * changed.  For example, if the number of columns in a
     * <code>TableModel</code> changed, this method would be invoked.
     * <p>
     * You normally do not call this method.  This method is public
     * to allow view classes to call it.
     */
    public abstract void modelStructureChanged();

    /**
     * Invoked when the contents of the underlying model have
     * completely changed. The structure of the table is the same,
     * only the contents have changed. This is typically sent when it
     * is too expensive to characterize the change in terms of the
     * other methods.
     * <p>
     * You normally do not call this method.  This method is public
     * to allow view classes to call it.
     */
    public abstract void allRowsChanged();

    /**
     * Invoked when rows have been inserted into the underlying model
     * in the specified range (inclusive).
     * <p>
     * The arguments give the indices of the effected range.
     * The first argument is in terms of the model before the change, and
     * must be less than or equal to the size of the model before the change.
     * The second argument is in terms of the model after the change and must
     * be less than the size of the model after the change. For example,
     * if you have a 5-row model and add 3 items to the end of the model
     * the indices are 5, 7.
     * <p>
     * You normally do not call this method.  This method is public
     * to allow view classes to call it.
     *
     * @param firstRow the first row
     * @param endRow the last row
     * @throws IndexOutOfBoundsException if either argument is invalid, or
     *         <code>firstRow</code> &gt; <code>endRow</code>
     */
    public abstract void rowsInserted(int firstRow, int endRow);

    /**
     * Invoked when rows have been deleted from the underlying model
     * in the specified range (inclusive).
     * <p>
     * The arguments give the indices of the effected range and
     * are in terms of the model <b>before</b> the change.
     * For example, if you have a 5-row model and delete 3 items from the end
     * of the model the indices are 2, 4.
     * <p>
     * You normally do not call this method.  This method is public
     * to allow view classes to call it.
     *
     * @param firstRow the first row
     * @param endRow the last row
     * @throws IndexOutOfBoundsException if either argument is outside
     *         the range of the model before the change, or
     *         <code>firstRow</code> &gt; <code>endRow</code>
     */
    public abstract void rowsDeleted(int firstRow, int endRow);

    /**
     * Invoked when rows have been changed in the underlying model
     * between the specified range (inclusive).
     * <p>
     * You normally do not call this method.  This method is public
     * to allow view classes to call it.
     *
     * @param firstRow the first row, in terms of the underlying model
     * @param endRow the last row, in terms of the underlying model
     * @throws IndexOutOfBoundsException if either argument is outside
     *         the range of the underlying model, or
     *         <code>firstRow</code> &gt; <code>endRow</code>
     */
    public abstract void rowsUpdated(int firstRow, int endRow);

    /**
     * Invoked when the column in the rows have been updated in
     * the underlying model between the specified range.
     * <p>
     * You normally do not call this method.  This method is public
     * to allow view classes to call it.
     *
     * @param firstRow the first row, in terms of the underlying model
     * @param endRow the last row, in terms of the underlying model
     * @param column the column that has changed, in terms of the underlying
     *        model
     * @throws IndexOutOfBoundsException if either argument is outside
     *         the range of the underlying model after the change,
     *         <code>firstRow</code> &gt; <code>endRow</code>, or
     *         <code>column</code> is outside the range of the underlying
     *          model
     */
    public abstract void rowsUpdated(int firstRow, int endRow, int column);

    /**
     * Adds a <code>RowSorterListener</code> to receive notification
     * about this <code>RowSorter</code>.  If the same
     * listener is added more than once it will receive multiple
     * notifications.  If <code>l</code> is <code>null</code> nothing
     * is done.
     *
     * @param l the <code>RowSorterListener</code>
     */
    public void addRowSorterListener(RowSorterListener l) {
        listenerList.add(RowSorterListener.class, l);
    }

    /**
     * Removes a <code>RowSorterListener</code>.  If
     * <code>l</code> is <code>null</code> nothing is done.
     *
     * @param l the <code>RowSorterListener</code>
     */
    public void removeRowSorterListener(RowSorterListener l) {
        listenerList.remove(RowSorterListener.class, l);
    }

    /**
     * Notifies listener that the sort order has changed.
     */
    protected void fireSortOrderChanged() {
        fireRowSorterChanged(new RowSorterEvent(this));
    }

    /**
     * Notifies listener that the mapping has changed.
     *
     * @param lastRowIndexToModel the mapping from model indices to
     *        view indices prior to the sort, may be <code>null</code>
     */
    protected void fireRowSorterChanged(int[] lastRowIndexToModel) {
        fireRowSorterChanged(new RowSorterEvent(this,
                RowSorterEvent.Type.SORTED, lastRowIndexToModel));
    }

    void fireRowSorterChanged(RowSorterEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RowSorterListener.class) {
                ((RowSorterListener)listeners[i + 1]).
                        sorterChanged(event);
            }
        }
    }

    /**
     * SortKey describes the sort order for a particular column.  The
     * column index is in terms of the underlying model, which may differ
     * from that of the view.
     *
     * @since 1.6
     */
    public static class SortKey {
        private int column;
        private SortOrder sortOrder;

        /**
         * Creates a <code>SortKey</code> for the specified column with
         * the specified sort order.
         *
         * @param column index of the column, in terms of the model
         * @param sortOrder the sorter order
         * @throws IllegalArgumentException if <code>sortOrder</code> is
         *         <code>null</code>
         */
        public SortKey(int column, SortOrder sortOrder) {
            if (sortOrder == null) {
                throw new IllegalArgumentException(
                        "sort order must be non-null");
            }
            this.column = column;
            this.sortOrder = sortOrder;
        }

        /**
         * Returns the index of the column.
         *
         * @return index of column
         */
        public final int getColumn() {
            return column;
        }

        /**
         * Returns the sort order of the column.
         *
         * @return the sort order of the column
         */
        public final SortOrder getSortOrder() {
            return sortOrder;
        }

        /**
         * Returns the hash code for this <code>SortKey</code>.
         *
         * @return hash code
         */
        public int hashCode() {
            int result = 17;
            result = 37 * result + column;
            result = 37 * result + sortOrder.hashCode();
            return result;
        }

        /**
         * Returns true if this object equals the specified object.
         * If the specified object is a <code>SortKey</code> and
         * references the same column and sort order, the two objects
         * are equal.
         *
         * @param o the object to compare to
         * @return true if <code>o</code> is equal to this <code>SortKey</code>
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof SortKey) {
                return (((SortKey)o).column == column &&
                        ((SortKey)o).sortOrder == sortOrder);
            }
            return false;
        }
    }
}
