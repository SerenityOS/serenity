/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.rowset;

import java.io.*;
import java.util.*;
import java.sql.*;
import javax.sql.*;
import java.math.*;

import javax.sql.rowset.*;
import javax.sql.rowset.spi.*;
import javax.sql.rowset.serial.*;
import com.sun.rowset.providers.*;
import com.sun.rowset.internal.*;

/**
 * The standard implementation of the <code>FilteredRowSet</code> interface. See the interface
 * definition for full behavior and implementation requirements.
 *
 * @see javax.sql.rowset.Predicate
 * @author Jonathan Bruce, Amit Handa
 */

public class FilteredRowSetImpl extends WebRowSetImpl implements Serializable, Cloneable, FilteredRowSet {

    private Predicate p;

    private boolean onInsertRow = false;


    /**
     * Construct a <code>FilteredRowSet</code>
     */
    public FilteredRowSetImpl() throws SQLException {
        super();
    }

    /**
     * Construct a <code>FilteredRowSet</code> with a specified synchronization
     * provider.
     *
     * @param env a Hashtable containing a desired synchconizatation provider
     * name-value pair.
     */
    @SuppressWarnings("rawtypes")
    public FilteredRowSetImpl(Hashtable env) throws SQLException {
        super(env);
    }

    /**
     * Apply the predicate for this filter
     *
     * @param p an implementation of the predicate interface
     */
    public void setFilter(Predicate p) throws SQLException {
        this.p = p;
    }

    /**
     * Retrieve the filter active for this <code>FilteredRowSet</code>
     *
     * @return a <code>Predicate</code> object instance
     */
    public Predicate getFilter() {
        return this.p;
    }

    /**
     * Over-riding <code>internalNext()</code> implementation. This method
     * applies the filter on the <code>RowSet</code> each time the cursor is advanced or
     * manipulated. It moves the cursor to the next row according to the set
     * predicate and returns <code>true</code> if the cursor is still within the rowset or
     * <code>false</code> if the cursor position is over the last row
     *
     * @return true if over the valid row in the rowset; false if over the last
     * row
     */
    protected boolean internalNext() throws SQLException {
        // CachedRowSetImpl.next() internally calls
        // this(crs).internalNext() NOTE: this holds crs object
        // So when frs.next() is called,
        // internally this(frs).internalNext() will be called
        // which will be nothing but this method.
        // because this holds frs object

        // keep on doing super.internalNext()
        // rather than doing it once.


         // p.evaluate will help us in changing the cursor
         // and checking the next value by returning true or false.
         // to fit the filter

         // So while() loop will have a "random combination" of
         // true and false returned depending upon the records
         // are in or out of filter.
         // We need to traverse from present cursorPos till end,
         // whether true or false and check each row for "filter"
         // "till we get a "true"


         boolean bool = false;

         for(int rows=this.getRow(); rows<=this.size();rows++) {
             bool = super.internalNext();

             if( !bool || p == null) {
               return bool;
             }
             if(p.evaluate(this)){
                   break;
             }

         }

       return bool;
    }


    /**
     * Over-riding <code>internalPrevious()</code> implementation. This method
     * applies the filter on the <code>RowSet</code> each time the cursor is moved backward or
     * manipulated. It moves the cursor to the previous row according to the set
     * predicate and returns <code>true</code> if the cursor is still within the rowset or
     * <code>false</code> if the cursor position is over the last row
     *
     * @return true if over the valid row in the rowset; false if over the last
     * row
     */
    protected boolean internalPrevious() throws SQLException {
         boolean bool = false;
         // with previous move backwards,
         // i.e. from any record towards first record

         for(int rows=this.getRow(); rows>0;rows--) {

             bool = super.internalPrevious();

             if( p == null) {
               return bool;
             }

             if(p.evaluate(this)){
                   break;
             }

         }

       return bool;
    }


    /**
     * Over-riding <code>internalFirst()</code> implementation. This method
     * applies the filter on the <code>RowSet</code> each time the cursor is moved to first
     * row. It moves the cursor to the first row according to the set
     * predicate and returns <code>true</code> if the cursor is still within the rowset or
     * <code>false</code> if the cursor position is over the last row
     *
     * @return true if over the valid row in the rowset; false if over the last
     * row
     */
    protected boolean internalFirst() throws SQLException {

        // from first till present cursor position(go forward),
        // find the actual first which matches the filter.

         boolean bool = super.internalFirst();

         if( p == null) {
               return bool;
             }

         while(bool) {

             if(p.evaluate(this)){
                   break;
             }
        bool = super.internalNext();
        }
     return bool;
    }


    /**
     * Over-riding <code>internalLast()</code> implementation. This method
     * applies the filter on the <code>RowSet</code> each time the cursor is moved to
     * last row. It moves the cursor to the last row according to the set
     * predicate and returns <code>true</code> if the cursor is still within the rowset or
     * <code>false</code> if the cursor position is over the last row
     *
     * @return true if over the valid row in the rowset; false if over the last
     * row
     */
    protected boolean internalLast() throws SQLException {
        // from last to the present cursor position(go backward),
        // find the actual last which matches the filter.

         boolean bool = super.internalLast();

         if( p == null) {
               return bool;
             }

         while(bool) {

             if(p.evaluate(this)){
                   break;
             }

        bool = super.internalPrevious();

        }
     return bool;

   } // end internalLast()
   /**
     * Moves the cursor the specified number of rows from the current
     * position, with a positive number moving it forward and a
     * negative number moving it backward.
     * <P>
     * If the number is positive, the cursor moves the specified number of
     * rows toward the end of the rowset, starting at the current row.
     * For example, the following command, in which
     * <code>crs</code> is a <code>CachedRowSetImpl</code> object with 100 rows,
     * moves the cursor forward four rows from the current row.  If the
     * current row is 50, the cursor would move to row 54.
     * <PRE><code>
     *
     *    crs.relative(4);
     *
     * </code> </PRE>
     * <P>
     * If the number is negative, the cursor moves back toward the beginning
     * the specified number of rows, starting at the current row.
     * For example, calling the method
     * <code>absolute(-1)</code> positions the cursor on the last row,
     * <code>absolute(-2)</code> moves it on the next-to-last row, and so on.
     * If the <code>CachedRowSetImpl</code> object <code>crs</code> has five rows,
     * the following command moves the cursor to the fourth-to-last row, which
     * in the case of a  rowset with five rows, is also the second row
     * from the beginning.
     * <PRE><code>
     *
     *    crs.absolute(-4);
     *
     * </code> </PRE>
     *
     * If the number specified is larger than the number of rows, the cursor
     * will move to the position after the last row. If the number specified
     * would move the cursor one or more rows before the first row, the cursor
     * moves to the position before the first row. In both cases, this method
     * throws an <code>SQLException</code>.
     * <P>
     * Note: Calling <code>absolute(1)</code> is the same as calling the
     * method <code>first()</code>.  Calling <code>absolute(-1)</code> is the
     * same as calling <code>last()</code>.  Calling <code>relative(0)</code>
     * is valid, but it does not change the cursor position.
     *
     * @param rows an <code>int</code> indicating the number of rows to move
     *             the cursor, starting at the current row; a positive number
     *             moves the cursor forward; a negative number moves the cursor
     *             backward; must not move the cursor past the valid
     *             rows
     * @return <code>true</code> if the cursor is on a row in this
     *         <code>CachedRowSetImpl</code> object; <code>false</code>
     *         otherwise
     * @throws SQLException if the rowset is type <code>ResultSet.TYPE_FORWARD_ONLY</code>
     */
   public boolean relative(int rows) throws SQLException {

      boolean retval;
      boolean bool = false;
      boolean boolval = false;

      if(getType() == ResultSet.TYPE_FORWARD_ONLY) {
         throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.relative").toString());
      }

      if( rows > 0 ) {

         int i = 0;
         while( i < (rows)) {

            if( isAfterLast() ) {
               return false;
            }
            bool = internalNext();
            i++;
         }

         retval = bool;
      } else {
         int j = rows;
         while( (j) < 0 ) {

           if( isBeforeFirst() ) {
              return false;
           }
           boolval = internalPrevious();
           j++;
         }
         retval = boolval;
      }
      if(rows != 0)
          notifyCursorMoved();
      return retval;
   }

   /**
     * Moves this <code>CachedRowSetImpl</code> object's cursor to the row number
     * specified.
     *
     * <p>If the number is positive, the cursor moves to an absolute row with
     * respect to the beginning of the rowset.  The first row is row 1, the second
     * is row 2, and so on.  For example, the following command, in which
     * <code>crs</code> is a <code>CachedRowSetImpl</code> object, moves the cursor
     * to the fourth row, starting from the beginning of the rowset.
     * <PRE><code>
     *
     *    crs.absolute(4);
     *
     * </code> </PRE>
     * <P>
     * If the number is negative, the cursor moves to an absolute row position
     * with respect to the end of the rowset.  For example, calling
     * <code>absolute(-1)</code> positions the cursor on the last row,
     * <code>absolute(-2)</code> moves it on the next-to-last row, and so on.
     * If the <code>CachedRowSetImpl</code> object <code>crs</code> has five rows,
     * the following command moves the cursor to the fourth-to-last row, which
     * in the case of a  rowset with five rows, is also the second row, counting
     * from the beginning.
     * <PRE><code>
     *
     *    crs.absolute(-4);
     *
     * </code> </PRE>
     *
     * If the number specified is larger than the number of rows, the cursor
     * will move to the position after the last row. If the number specified
     * would move the cursor one or more rows before the first row, the cursor
     * moves to the position before the first row.
     * <P>
     * Note: Calling <code>absolute(1)</code> is the same as calling the
     * method <code>first()</code>.  Calling <code>absolute(-1)</code> is the
     * same as calling <code>last()</code>.
     *
     * @param rows a positive number to indicate the row, starting row numbering from
     *        the first row, which is <code>1</code>; a negative number to indicate
     *        the row, starting row numbering from the last row, which is
     *        <code>-1</code>; it must not be <code>0</code>
     * @return <code>true</code> if the cursor is on the rowset; <code>false</code>
     *         otherwise
     * @throws SQLException if the given cursor position is <code>0</code> or the
     *            type of this rowset is <code>ResultSet.TYPE_FORWARD_ONLY</code>
     */
    public boolean absolute(int rows) throws SQLException {

      boolean retval;
      boolean bool = false;

      if(rows == 0 || getType() == ResultSet.TYPE_FORWARD_ONLY) {
         throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.absolute").toString());
      }

      if (rows > 0) {
         bool = internalFirst();

         int i = 0;
         while(i < (rows-1)) {
            if( isAfterLast() ) {
               return false;
            }
            bool = internalNext();
            i++;
         }
         retval = bool;
      } else {
         bool = internalLast();

         int j = rows;
         while((j+1) < 0 ) {
            if( isBeforeFirst() ) {
               return false;
            }
            bool = internalPrevious();
            j++;
         }
         retval = bool;
      }
      notifyCursorMoved();
      return retval;
   }

   /**
     * Moves the cursor for this <code>CachedRowSetImpl</code> object
     * to the insert row.  The current row in the rowset is remembered
     * while the cursor is on the insert row.
     * <P>
     * The insert row is a special row associated with an updatable
     * rowset.  It is essentially a buffer where a new row may
     * be constructed by calling the appropriate <code>updateXXX</code>
     * methods to assign a value to each column in the row.  A complete
     * row must be constructed; that is, every column that is not nullable
     * must be assigned a value.  In order for the new row to become part
     * of this rowset, the method <code>insertRow</code> must be called
     * before the cursor is moved back to the rowset.
     * <P>
     * Only certain methods may be invoked while the cursor is on the insert
     * row; many methods throw an exception if they are called while the
     * cursor is there.  In addition to the <code>updateXXX</code>
     * and <code>insertRow</code> methods, only the <code>getXXX</code> methods
     * may be called when the cursor is on the insert row.  A <code>getXXX</code>
     * method should be called on a column only after an <code>updateXXX</code>
     * method has been called on that column; otherwise, the value returned is
     * undetermined.
     *
     * @throws SQLException if this <code>CachedRowSetImpl</code> object is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void moveToInsertRow() throws SQLException {

      onInsertRow = true;
      super.moveToInsertRow();
   }

   /**
     * This is explanation for the overriding of the updateXXX functions.
     * These functions have been overriden to ensure that only correct
     * values that pass the criteria for the filter are actaully inserted.
     * The evaluation of whether a particular value passes the criteria
     * of the filter is done using the evaluate function in the Predicate
     * interface.
     *
     * The checking can will done in the evaluate function which is implemented
     * in the class that implements the Predicate interface. So the checking
     * can vary from one implementation to another.
     *
     * Some additional points here on the following:
     * 1. updateBytes()     - since the evaluate function takes Object as parameter
     *                        a String is constructed from the byte array and would
     *                        passed to the evaluate function.
     * 2. updateXXXstream() - here it would suffice to pass the stream handle
     *                        to the evaluate function and the implementation
     *                        of the evaluate function can do the comparison
     *                        based on the stream and also type of data.
     */


   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>int</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateInt(int columnIndex , int x) throws SQLException {

     boolean bool;

     if(onInsertRow) {
        if(p != null) {
           bool = p.evaluate(Integer.valueOf(x),columnIndex);

           if(!bool) {
              throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
           }
        }
     }

     super.updateInt(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>int</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateInt(String columnName , int x) throws SQLException {

       this.updateInt(findColumn(columnName), x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>boolean</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateBoolean(int columnIndex, boolean x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
            bool = p.evaluate(Boolean.valueOf(x) , columnIndex);

            if(!bool) {
               throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
            }
         }
      }

      super.updateBoolean(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>boolean</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateBoolean(String columnName , boolean x) throws SQLException {

      this.updateBoolean(findColumn(columnName),x);
   }



    /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>byte</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateByte(int columnIndex , byte x) throws SQLException {
      boolean bool;

      if(onInsertRow) {
         if(p != null) {
            bool = p.evaluate(Byte.valueOf(x),columnIndex);

            if(!bool) {
                throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
            }
          }
      }

      super.updateByte(columnIndex,x);
   }


   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>byte</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateByte(String columnName , byte x) throws SQLException {

      this.updateByte(findColumn(columnName),x);
   }


   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>short</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateShort( int columnIndex , short x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
            bool = p.evaluate(Short.valueOf(x), columnIndex);

            if(!bool) {
               throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
            }
          }
      }

      super.updateShort(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>short</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateShort( String columnName , short x) throws SQLException {

      this.updateShort(findColumn(columnName),x);
   }


   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>long</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateLong(int columnIndex , long x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
            bool = p.evaluate(Long.valueOf(x), columnIndex);

            if(!bool) {
               throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
            }
          }
      }

      super.updateLong(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>long</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateLong( String columnName , long x) throws SQLException {

      this.updateLong(findColumn(columnName) , x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>float</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateFloat(int columnIndex , float x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
            bool = p.evaluate(Float.valueOf(x), columnIndex);

            if(!bool) {
               throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
            }
          }
      }

      super.updateFloat(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>float</code> value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateFloat(String columnName , float x) throws SQLException {

      this.updateFloat(findColumn(columnName),x);
   }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>double</code> value.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateDouble(int columnIndex , double x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
            bool = p.evaluate(Double.valueOf(x) , columnIndex);

            if(!bool) {
               throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
            }
          }
      }

      super.updateDouble(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>double</code> value.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateDouble(String columnName , double x) throws SQLException {

      this.updateDouble(findColumn(columnName),x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>java.math.BigDecimal</code> object.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateBigDecimal(int columnIndex , BigDecimal x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
            bool = p.evaluate(x,columnIndex);

            if(!bool) {
               throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
            }
          }
      }

      super.updateBigDecimal(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>java.math.BigDecimal</code> object.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateBigDecimal(String columnName , BigDecimal x) throws SQLException {

      this.updateBigDecimal(findColumn(columnName),x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>String</code> object.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to mark the row as updated.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called to insert the new row into this rowset and mark it
     * as inserted. Both of these methods must be called before the
     * cursor moves to another row.
     * <P>
     * The method <code>acceptChanges</code> must be called if the
     * updated values are to be written back to the underlying database.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateString(int columnIndex , String x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
           bool = p.evaluate(x,columnIndex);

           if(!bool) {
              throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
           }
         }
      }

      super.updateString(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>String</code> object.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateString(String columnName , String x) throws SQLException {

      this.updateString(findColumn(columnName),x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>byte</code> array.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateBytes(int columnIndex , byte []x) throws SQLException {

      boolean bool;
      String val = "";

      Byte [] obj_arr = new Byte[x.length];

      for(int i = 0; i < x.length; i++) {
         obj_arr[i] = Byte.valueOf(x[i]);
         val = val.concat(obj_arr[i].toString());
     }


      if(onInsertRow) {
         if(p != null) {
             bool = p.evaluate(val,columnIndex);

             if(!bool) {
                 throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
             }
         }
      }

      super.updateBytes(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>byte</code> array.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateBytes(String columnName , byte []x) throws SQLException {

      this.updateBytes(findColumn(columnName),x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Date</code> object.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the type of the designated column is not
     *            an SQL <code>DATE</code> or <code>TIMESTAMP</code>, or
     *            (4) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateDate(int columnIndex , java.sql.Date x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
             bool = p.evaluate(x,columnIndex);

             if(!bool) {
                 throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
             }
         }
      }

      super.updateDate(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Date</code> object.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the type
     *            of the designated column is not an SQL <code>DATE</code> or
     *            <code>TIMESTAMP</code>, or (4) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateDate(String columnName , java.sql.Date x) throws SQLException {

      this.updateDate(findColumn(columnName),x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Time</code> object.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the type of the designated column is not
     *            an SQL <code>TIME</code> or <code>TIMESTAMP</code>, or
     *            (4) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateTime(int columnIndex , Time x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
             bool = p.evaluate(x, columnIndex);

             if(!bool) {
                 throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
             }
         }
      }

      super.updateTime(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Time</code> object.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the type
     *            of the designated column is not an SQL <code>TIME</code> or
     *            <code>TIMESTAMP</code>, or (4) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateTime(String columnName , Time x) throws SQLException {

      this.updateTime(findColumn(columnName),x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Timestamp</code> object.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the type of the designated column is not
     *            an SQL <code>DATE</code>, <code>TIME</code>, or
     *            <code>TIMESTAMP</code>, or (4) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateTimestamp(int columnIndex , Timestamp x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
             bool = p.evaluate(x,columnIndex);

             if(!bool) {
                 throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
             }
         }
      }

      super.updateTimestamp(columnIndex,x);
   }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Timestamp</code> object.
     *
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if the given column index is out of bounds or
     *            the cursor is not on one of this rowset's rows or its
     *            insert row
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the type
     *            of the designated column is not an SQL <code>DATE</code>,
     *            <code>TIME</code>, or <code>TIMESTAMP</code>, or (4) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateTimestamp(String columnName , Timestamp x) throws SQLException {

      this.updateTimestamp(findColumn(columnName),x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * ASCII stream value.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @param length the number of one-byte ASCII characters in the stream
     * @throws SQLException if this method is invoked
     */
   public void updateAsciiStream(int columnIndex , java.io.InputStream x ,int length) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
             bool = p.evaluate(x,columnIndex);

             if(!bool) {
                 throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
             }
         }
      }

      super.updateAsciiStream(columnIndex,x,length);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * ASCII stream value.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @param length the number of one-byte ASCII characters in the stream
     */
   public void updateAsciiStream(String columnName , java.io.InputStream x , int length) throws SQLException {

      this.updateAsciiStream(findColumn(columnName),x,length);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>java.io.Reader</code> object.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value; must be a <code>java.io.Reader</code>
     *          containing <code>BINARY</code>, <code>VARBINARY</code>,
     *          <code>LONGVARBINARY</code>, <code>CHAR</code>, <code>VARCHAR</code>,
     *          or <code>LONGVARCHAR</code> data
     * @param length the length of the stream in characters
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the data in the stream is not a binary or
     *            character type, or (4) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateCharacterStream(int columnIndex , java.io.Reader x , int length) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
             bool = p.evaluate(x,columnIndex);

             if(!bool) {
                 throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
             }
         }
      }

      super.updateCharacterStream(columnIndex,x,length);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>java.io.Reader</code> object.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param reader the new column value; must be a
     * <code>java.io.Reader</code> containing <code>BINARY</code>,
     * <code>VARBINARY</code>, <code>LONGVARBINARY</code>, <code>CHAR</code>,
     * <code>VARCHAR</code>, or <code>LONGVARCHAR</code> data
     * @param length the length of the stream in characters
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the data
     *            in the stream is not a binary or character type, or (4) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateCharacterStream(String columnName , java.io.Reader reader, int length) throws SQLException {
      this.updateCharacterStream(findColumn(columnName), reader, length);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>java.io.InputStream</code> object.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value; must be a <code>java.io.InputStream</code>
     *          containing <code>BINARY</code>, <code>VARBINARY</code>, or
     *          <code>LONGVARBINARY</code> data
     * @param length the length of the stream in bytes
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the data in the stream is not binary, or
     *            (4) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateBinaryStream(int columnIndex , java.io.InputStream x , int length) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
             bool = p.evaluate(x,columnIndex);

             if(!bool) {
                 throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
             }
         }
      }

      super.updateBinaryStream(columnIndex,x,length);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>java.io.InputStream</code> object.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value; must be a <code>java.io.InputStream</code>
     *          containing <code>BINARY</code>, <code>VARBINARY</code>, or
     *          <code>LONGVARBINARY</code> data
     * @param length the length of the stream in bytes
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the data
     *            in the stream is not binary, or (4) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateBinaryStream(String columnName , java.io.InputStream x, int length) throws SQLException {

      this.updateBinaryStream(findColumn(columnName),x,length);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Object</code> value.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateObject(int columnIndex , Object x) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
             bool = p.evaluate(x,columnIndex);

             if(!bool) {
                 throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
             }
         }
      }

      super.updateObject(columnIndex,x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Object</code> value.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateObject(String columnName , Object x) throws SQLException {

      this.updateObject(findColumn(columnName),x);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Object</code> value.  The <code>scale</code> parameter indicates
     * the number of digits to the right of the decimal point and is ignored
     * if the new column value is not a type that will be mapped to an SQL
     * <code>DECIMAL</code> or <code>NUMERIC</code> value.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnIndex the first column is <code>1</code>, the second
     *        is <code>2</code>, and so on; must be <code>1</code> or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @param scale the number of digits to the right of the decimal point (for
     *              <code>DECIMAL</code> and <code>NUMERIC</code> types only)
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateObject(int columnIndex , Object x , int scale) throws SQLException {

      boolean bool;

      if(onInsertRow) {
         if(p != null) {
             bool = p.evaluate(x,columnIndex);

             if(!bool) {
                 throw new SQLException(resBundle.handleGetObject("filteredrowsetimpl.notallowed").toString());
             }
         }
      }

      super.updateObject(columnIndex,x,scale);
   }

   /**
     * Sets the designated column in either the current row or the insert
     * row of this <code>CachedRowSetImpl</code> object with the given
     * <code>Object</code> value.  The <code>scale</code> parameter
     * indicates the number of digits to the right of the decimal point
     * and is ignored if the new column value is not a type that will be
     *  mapped to an SQL <code>DECIMAL</code> or <code>NUMERIC</code> value.
     * <P>
     * This method updates a column value in either the current row or
     * the insert row of this rowset, but it does not update the
     * database.  If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database. Both of these methods must be called before the
     * cursor moves to another row.
     *
     * @param columnName a <code>String</code> object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @param scale the number of digits to the right of the decimal point (for
     *              <code>DECIMAL</code> and <code>NUMERIC</code> types only)
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void updateObject(String columnName , Object x, int scale) throws SQLException {

      this.updateObject(findColumn(columnName),x,scale);
   }

   /**
     * Inserts the contents of this <code>CachedRowSetImpl</code> object's insert
     * row into this rowset immediately following the current row.
     * If the current row is the
     * position after the last row or before the first row, the new row will
     * be inserted at the end of the rowset.  This method also notifies
     * listeners registered with this rowset that the row has changed.
     * <P>
     * The cursor must be on the insert row when this method is called.
     *
     * @throws SQLException if (1) the cursor is not on the insert row,
     *            (2) one or more of the non-nullable columns in the insert
     *            row has not been given a value, or (3) this rowset is
     *            <code>ResultSet.CONCUR_READ_ONLY</code>
     */
   public void insertRow() throws SQLException {

      onInsertRow = false;
      super.insertRow();
   }

   /**
    * This method re populates the resBundle
    * during the deserialization process
    *
    */
   private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
       // Default state initialization happens here
       ois.defaultReadObject();
       // Initialization of transient Res Bundle happens here .
       try {
          resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
       } catch(IOException ioe) {
           throw new RuntimeException(ioe);
       }

   }

   static final long serialVersionUID = 6178454588413509360L;
} // end FilteredRowSetImpl class
