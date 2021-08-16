/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

package sun.jvm.hotspot.ui.table;

import java.util.*;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;

/**
 * A comparator which compares rows in a table model
 */
public abstract class TableModelComparator implements Comparator<Object> {

    private boolean ascending;
    protected TableModel model;

    private int[] columns;

    public TableModelComparator(TableModel model) {
        this.model = model;

        // XXX - Should actually listen for column changes and resize
        columns = new int[model.getColumnCount()];
        columns[0] = -1;
    }

    /**
     * Add the column to the sort criteria
     */
    public void addColumn(int column) {
        // Shift columns in the array
        int[] tempArray = new int[model.getColumnCount()];
        System.arraycopy(columns, 1, tempArray, 0, columns.length - 1);

        columns = tempArray;
        columns[0] = column;
    }

    /**
     * Get the last column that was sorted
     */
    public int getColumn() {
        return columns[0];
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    public boolean isAscending() {
        return ascending;
    }

    /**
     * Implementation of the comparator method. A comparison is
     * made for rows.
     */
    public int compare(Object row1, Object row2) {
        for (int i = 0; i < columns.length; i++) {

            @SuppressWarnings("unchecked")
            Comparable<Object> o1 = (Comparable<Object>) getValueForColumn(row1, columns[i]);
            @SuppressWarnings("unchecked")
            Comparable<Object> o2 = (Comparable<Object>) getValueForColumn(row2, columns[i]);

            // If both values are null, return 0.
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) { // Define null less than everything.
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            int result = o1.compareTo(o2);

            if (result != 0) {
                return ascending ? result : -result;
            }
        }
        return 0;
    }

    /**
     * Returns the value for the comparing object for the
     * column.
     *
     * @param obj Row object that was passed into Comparator.
     * @param column the column to retrieve
     */
    public abstract Comparable<?> getValueForColumn(Object obj, int column);

}
