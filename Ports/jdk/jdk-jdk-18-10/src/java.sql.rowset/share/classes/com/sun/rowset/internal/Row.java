/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.rowset.internal;

import java.sql.*;
import java.io.*;
import java.lang.*;
import java.util.*;

/**
 * A class that keeps track of a row's values. A <code>Row</code> object
 * maintains an array of current column values and an array of original
 * column values, and it provides methods for getting and setting the
 * value of a column.  It also keeps track of which columns have
 * changed and whether the change was a delete, insert, or update.
 * <P>
 * Note that column numbers for rowsets start at <code>1</code>,
 * whereas the first element of an array or bitset is <code>0</code>.
 * The argument for the method <code>getColumnUpdated</code> refers to
 * the column number in the rowset (the first column is <code>1</code>);
 * the argument for <code>setColumnUpdated</code> refers to the index
 * into the rowset's internal bitset (the first bit is <code>0</code>).
 */
public class Row extends BaseRow implements Serializable, Cloneable {

static final long serialVersionUID = 5047859032611314762L;

/**
 * An array containing the current column values for this <code>Row</code>
 * object.
 * @serial
 */
    private Object[] currentVals;

/**
 * A <code>BitSet</code> object containing a flag for each column in
 * this <code>Row</code> object, with each flag indicating whether or
 * not the value in the column has been changed.
 * @serial
 */
    private BitSet colsChanged;

/**
 * A <code>boolean</code> indicating whether or not this <code>Row</code>
 * object has been deleted.  <code>true</code> indicates that it has
 * been deleted; <code>false</code> indicates that it has not.
 * @serial
 */
    private boolean deleted;

/**
 * A <code>boolean</code> indicating whether or not this <code>Row</code>
 * object has been updated.  <code>true</code> indicates that it has
 * been updated; <code>false</code> indicates that it has not.
 * @serial
 */
    private boolean updated;

/**
 * A <code>boolean</code> indicating whether or not this <code>Row</code>
 * object has been inserted.  <code>true</code> indicates that it has
 * been inserted; <code>false</code> indicates that it has not.
 * @serial
 */
    private boolean inserted;

/**
 * The number of columns in this <code>Row</code> object.
 * @serial
 */
    private int numCols;

/**
 * Creates a new <code>Row</code> object with the given number of columns.
 * The newly-created row includes an array of original values,
 * an array for storing its current values, and a <code>BitSet</code>
 * object for keeping track of which column values have been changed.
 */
    public Row(int numCols) {
        origVals = new Object[numCols];
        currentVals = new Object[numCols];
        colsChanged = new BitSet(numCols);
        this.numCols = numCols;
    }

/**
 * Creates a new <code>Row</code> object with the given number of columns
 * and with its array of original values initialized to the given array.
 * The new <code>Row</code> object also has an array for storing its
 * current values and a <code>BitSet</code> object for keeping track
 * of which column values have been changed.
 */
    public Row(int numCols, Object[] vals) {
        origVals = new Object[numCols];
        System.arraycopy(vals, 0, origVals, 0, numCols);
        currentVals = new Object[numCols];
        colsChanged = new BitSet(numCols);
        this.numCols = numCols;
    }

/**
 *
 * This method is called internally by the <code>CachedRowSet.populate</code>
 * methods.
 *
 * @param idx the number of the column in this <code>Row</code> object
 *            that is to be set; the index of the first column is
 *            <code>1</code>
 * @param val the new value to be set
 */
    public void initColumnObject(int idx, Object val) {
        origVals[idx - 1] = val;
    }


/**
 *
 * This method is called internally by the <code>CachedRowSet.updateXXX</code>
 * methods.
 *
 * @param idx the number of the column in this <code>Row</code> object
 *            that is to be set; the index of the first column is
 *            <code>1</code>
 * @param val the new value to be set
 */
    public void setColumnObject(int idx, Object val) {
            currentVals[idx - 1] = val;
            setColUpdated(idx - 1);
    }

/**
 * Retrieves the column value stored in the designated column of this
 * <code>Row</code> object.
 *
 * @param columnIndex the index of the column value to be retrieved;
 *                    the index of the first column is <code>1</code>
 * @return an <code>Object</code> in the Java programming language that
 *         represents the value stored in the designated column
 * @throws SQLException if there is a database access error
 */
    public Object getColumnObject(int columnIndex) throws SQLException {
        if (getColUpdated(columnIndex - 1)) {
            return(currentVals[columnIndex - 1]); // maps to array!!
        } else {
            return(origVals[columnIndex - 1]); // maps to array!!
        }
    }

/**
 * Indicates whether the designated column of this <code>Row</code> object
 * has been changed.
 * @param idx the index into the <code>BitSet</code> object maintained by
 *            this <code>Row</code> object to keep track of which column
 *            values have been modified; the index of the first bit is
 *            <code>0</code>
 * @return <code>true</code> if the designated column value has been changed;
 *         <code>false</code> otherwise
 *
 */
    public boolean getColUpdated(int idx) {
        return colsChanged.get(idx);
    }

/**
 * Sets this <code>Row</code> object's <code>deleted</code> field
 * to <code>true</code>.
 *
 * @see #getDeleted
 */
    public void setDeleted() { // %%% was public
        deleted = true;
    }


/**
 * Retrieves the value of this <code>Row</code> object's <code>deleted</code> field,
 * which will be <code>true</code> if one or more of its columns has been
 * deleted.
 * @return <code>true</code> if a column value has been deleted; <code>false</code>
 *         otherwise
 *
 * @see #setDeleted
 */
    public boolean getDeleted() {
        return(deleted);
    }

/**
 * Sets the <code>deleted</code> field for this <code>Row</code> object to
 * <code>false</code>.
 */
    public void clearDeleted() {
        deleted = false;
    }


/**
 * Sets the value of this <code>Row</code> object's <code>inserted</code> field
 * to <code>true</code>.
 *
 * @see #getInserted
 */
    public void setInserted() {
        inserted = true;
    }


/**
 * Retrieves the value of this <code>Row</code> object's <code>inserted</code> field,
 * which will be <code>true</code> if this row has been inserted.
 * @return <code>true</code> if this row has been inserted; <code>false</code>
 *         otherwise
 *
 * @see #setInserted
 */
    public boolean getInserted() {
        return(inserted);
    }


/**
 * Sets the <code>inserted</code> field for this <code>Row</code> object to
 * <code>false</code>.
 */
    public void clearInserted() { // %%% was public
        inserted = false;
    }

/**
 * Retrieves the value of this <code>Row</code> object's
 * <code>updated</code> field.
 * @return <code>true</code> if this <code>Row</code> object has been
 *         updated; <code>false</code> if it has not
 *
 * @see #setUpdated
 */
    public boolean getUpdated() {
        return(updated);
    }

/**
 * Sets the <code>updated</code> field for this <code>Row</code> object to
 * <code>true</code> if one or more of its column values has been changed.
 *
 * @see #getUpdated
 */
    public void setUpdated() {
        // only mark something as updated if one or
        // more of the columns has been changed.
        for (int i = 0; i < numCols; i++) {
            if (getColUpdated(i) == true) {
                updated = true;
                return;
            }
        }
    }

/**
 * Sets the bit at the given index into this <code>Row</code> object's internal
 * <code>BitSet</code> object, indicating that the corresponding column value
 * (column <code>idx</code> + 1) has been changed.
 *
 * @param idx the index into the <code>BitSet</code> object maintained by
 *            this <code>Row</code> object; the first bit is at index
 *            <code>0</code>
 *
 */
    private void setColUpdated(int idx) {
        colsChanged.set(idx);
    }

/**
 * Sets the <code>updated</code> field for this <code>Row</code> object to
 * <code>false</code>, sets all the column values in this <code>Row</code>
 * object's internal array of current values to <code>null</code>, and clears
 * all of the bits in the <code>BitSet</code> object maintained by this
 * <code>Row</code> object.
 */
    public void clearUpdated() {
        updated = false;
        for (int i = 0; i < numCols; i++) {
            currentVals[i] = null;
            colsChanged.clear(i);
        }
    }

   /**
    * Sets the column values in this <code>Row</code> object's internal
    * array of original values with the values in its internal array of
    * current values, sets all the values in this <code>Row</code>
    * object's internal array of current values to <code>null</code>,
    * clears all the bits in this <code>Row</code> object's internal bitset,
    * and sets its <code>updated</code> field to <code>false</code>.
    * <P>
    * This method is called internally by the <code>CachedRowSet</code>
    * method <code>makeRowOriginal</code>.
    */
    public void moveCurrentToOrig() {
        for (int i = 0; i < numCols; i++) {
            if (getColUpdated(i) == true) {
                origVals[i] = currentVals[i];
                currentVals[i] = null;
                colsChanged.clear(i);
            }
        }
        updated = false;
    }

   /**
    * Returns the row on which the cursor is positioned.
    *
    * @return the <code>Row</code> object on which the <code>CachedRowSet</code>
    *           implementation objects's cursor is positioned
    */
    public BaseRow getCurrentRow() {
        return null;
    }
}
