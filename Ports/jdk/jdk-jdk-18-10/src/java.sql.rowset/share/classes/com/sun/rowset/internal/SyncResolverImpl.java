/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
import javax.sql.*;
import java.util.*;
import java.math.BigDecimal;

import javax.sql.rowset.*;
import javax.sql.rowset.spi.*;

import com.sun.rowset.*;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * There will be two sets of data which will be maintained by the rowset at the
 * time of synchronization. The {@code SyncProvider} will utilize the
 * {@code SyncResolver} to synchronize the changes back to database.
 */
public class SyncResolverImpl extends CachedRowSetImpl implements SyncResolver {
    /**
     * This CachedRowSet object will encapsulate a rowset
     * which will be sync'ed with the datasource but will
     * contain values in rows where there is conflict.
     * For rows other than conflict, it will *not* contain
     * any data. For rows containing conflict it will
     * return either of the three values set by SyncResolver.*_CONFLICT
     * from getStatus()
     */
    private CachedRowSetImpl crsRes;

    /**
     * This is the actual CachedRowSet object
     * which is being synchronized back to
     * datasource.
     */
    private CachedRowSetImpl crsSync;

    /**
     *  This ArrayList will contain the status of a row
     *  from the SyncResolver.* values else it will be null.
     */
    private ArrayList<?> stats;

    /**
     * The RowSetWriter associated with the original
     * CachedRowSet object which is being synchronized.
     */
    private CachedRowSetWriter crw;

    /**
     * Row number identifier
     */
    private int rowStatus;

    /**
     * This will contain the size of the {@code CachedRowSet} object
     */
    private int sz;

    /**
     * The {@code Connection} handle used to synchronize the changes
     * back to datasource. This is the same connection handle as was passed
     * to the CachedRowSet while fetching the data.
     */
    private transient Connection con;

    /**
     * The {@code CachedRowSet} object which will encapsulate
     * a row at any time. This will be built from CachedRowSet and
     * SyncResolver values. Synchronization takes place on a row by
     * row basis encapsulated as a CahedRowSet.
     */
    private CachedRowSet row;

    private JdbcRowSetResourceBundle resBundle;

    /**
     * Public constructor
     */
    public SyncResolverImpl() throws SQLException {
        try {
            crsSync = new CachedRowSetImpl();
            crsRes = new CachedRowSetImpl();
            crw = new CachedRowSetWriter();
            row = new CachedRowSetImpl();
            rowStatus = 1;
            try {
                resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
            } catch(IOException ioe) {
                throw new RuntimeException(ioe);
            }

        } catch(SQLException sqle) {
        }
     }


    /**
     * Retrieves the conflict status of the current row of this
     * {@code SyncResolver}, which indicates the operation the {@code RowSet}
     * object was attempting when the conflict occurred.
     *
     * @return one of the following constants:
     *         {@code SyncResolver.UPDATE_ROW_CONFLICT},
     *         {@code SyncResolver.DELETE_ROW_CONFLICT}, or
     *         {@code SyncResolver.INSERT_ROW_CONFLICT}
     */
    public int getStatus() {
        return stats != null ? (Integer) stats.get(rowStatus - 1) :
                SyncResolver.NO_ROW_CONFLICT;
    }

    /**
     * Retrieves the value in the designated column in the current row of this
     * {@code SyncResolver} object, which is the value that caused a conflict.
     *
     * @param index {@code int} designating the column in this row of this
     *        {@code SyncResolver} object from which to retrieve the value
     *        causing a conflict
     */
    public Object getConflictValue(int index) throws SQLException {
        try {
             return crsRes.getObject(index);
        } catch(SQLException sqle) {
            throw new SQLException(sqle.getMessage());
        } catch (Exception e ) {
            throw new SQLException("Problem obtaining conflicted value!", e);
        }
    }

    /**
     * Retrieves the value in the designated column in the current row of this
     * {@code SyncResolver} object, which is the value that caused a conflict.
     *
     * @param columnName a {@code String} object designating the column in this row of this
     *        {@code SyncResolver} object from which to retrieve the value
     *        causing a conflict
     */
    public Object getConflictValue(String columnName) throws SQLException {
        try {
             return crsRes.getObject(columnName);
        } catch(SQLException sqle) {
             throw new SQLException(sqle.getMessage());
        } catch (Exception e ) {
            throw new SQLException("Problem obtaining conflicted value!", e);
        }
    }

    /**
     * Sets <i>obj</i> as the value in column <i>index</i> in the current row of the
     * {@code RowSet} object. This value is the resolved value that is to be
     * persisted in the data source.
     *
     * @param index an {@code int} giving the number of the column into which to
     *        set the value to be persisted
     * @param obj an {@code Object} that is the value to be set in the data source
     */
    public void setResolvedValue(int index, Object obj) throws SQLException {
        // modify method to throw SQLException in spec

        /**
         * When a value is resolved properly make it to null
         * inside crsRes for that column.
         *
         * For more than one conflicts in the row,
         * check for the last resolved value of the current row
         * (Note: it can be resolved randomly for same row)
         * then sync back immediately.
         **/
        try {
            ResultSetMetaData rsmd = crsSync.getMetaData();
            // check whether the index is in range
            if(index<=0 || rsmd == null || index > rsmd.getColumnCount() ) {
                throw new SQLException(resBundle.handleGetObject("syncrsimpl.indexval").toString()+ index);
            }
             // check whether index col is in conflict
            if(crsRes.getObject(index) == null) {
                throw new SQLException(resBundle.handleGetObject("syncrsimpl.noconflict").toString());
            }
        } catch (SQLException sqle) {
            // modify method to throw for SQLException
            throw new SQLException(sqle.getMessage());
        }
        try {
             boolean bool = true;
             /** Check resolved value to be either of conflict
               * or in rowset else throw sql exception.
               * If we allow a value other than that in CachedRowSet or
               * datasource we will end up in looping the loop of exceptions.
              **/

             if( ((crsSync.getObject(index)).toString()).equals(obj.toString()) ||
                     ((crsRes.getObject(index)).toString()).equals(obj.toString()) ) {

                /**
                 * Check whether this is the only conflict in the row.
                 * If yes, synchronize this row back
                 * which has been resolved, else wait
                 * for all conflicts of current row to be resolved
                 *
                 * Step 1: Update crsRes and make the index col as null
                 * i.e. resolved
                 * crsRes.updateObject(index, obj);
                 **/
                  crsRes.updateNull(index);
                  crsRes.updateRow();

                 /**
                  * Step 2: Change the value in the CachedRowSetImpl object
                  * crsSync.updateObject(index, obj);
                  * crsSync.updateRow();
                  **/
                 if(row.size() != 1) {
                    row = buildCachedRow();
                 }

                 row.updateObject(index, obj);
                 row.updateRow();

                 for(int j=1; j < crsRes.getMetaData().getColumnCount(); j++) {
                     if(crsRes.getObject(j) != null) {
                        bool = false;
                        break;
                         // break out of loop and wait for other cols
                         // in same row to get resolved
                     } //end if

                  } //end for

                  if(bool) {
                     /**
                      * sync data back using CachedRowSetWriter
                      * construct the present row and pass it to the writer
                      * to write back to db.
                      **/
                     try {
                           /**
                            * Note : The use of CachedRowSetWriter to get *same* Connection handle.
                            * The CachedRowSetWriter uses the connection handle
                            * from the reader, Hence will use the same connection handle
                            * as of original CachedRowSetImpl
                            **/

                          writeData(row);

                          //crw.writeData( (RowSetInternal)crsRow);
                          //System.out.printlnt.println("12");

                     } catch(SyncProviderException spe) {
                         /**
                          * This will occur if db is not allowing
                          * even after resolving the conflicts
                          * due to some reasons.
                          * Also will prevent from going into a loop of SPE's
                          **/
                         throw new SQLException(resBundle.handleGetObject("syncrsimpl.syncnotpos").toString());
                     }
                  } //end if(bool)

             } else {
                 throw new SQLException(resBundle.handleGetObject("syncrsimpl.valtores").toString());
             } //end if (crs.getObject ...) block


        } catch(SQLException sqle) {
           throw new SQLException(sqle.getMessage());
        }
    }

    /**
     * This passes a CachedRowSet as a row to the CachedRowSetWriter
     * after the values have been resolved, back to the datasource.
     *
     * @param row a {@code CachedRowSet} object which will hold the
     *        values of a particular row after they have been resolved by
     *        the user to synchronize back to datasource.
     * @throws SQLException if synchronization does not happen properly
     *         maybe beacuse {@code Connection} has timed out.
     **/
     private void writeData(CachedRowSet row) throws SQLException {
        crw.updateResolvedConflictToDB(row, crw.getReader().connect((RowSetInternal)crsSync));
     }

    /**
     * This function builds a row  as a {@code CachedRowSet} object
     * which has been resolved and is ready to be synchrinized to the datasource
     *
     * @throws SQLException if there is problem in building
     *         the metadata of the row.
     **/
     private CachedRowSet buildCachedRow() throws SQLException {
       int iColCount;
       CachedRowSetImpl crsRow = new CachedRowSetImpl();

       RowSetMetaDataImpl rsmd = new RowSetMetaDataImpl();
       RowSetMetaDataImpl rsmdWrite = (RowSetMetaDataImpl)crsSync.getMetaData();
       RowSetMetaDataImpl rsmdRow = new RowSetMetaDataImpl();

       iColCount = rsmdWrite.getColumnCount();
       rsmdRow.setColumnCount(iColCount);

       for(int i =1;i<=iColCount;i++) {
          rsmdRow.setColumnType(i,rsmdWrite.getColumnType(i));
          rsmdRow.setColumnName(i,rsmdWrite.getColumnName(i));
          rsmdRow.setNullable(i,ResultSetMetaData.columnNullableUnknown);

          try {
             rsmdRow.setCatalogName(i, rsmdWrite.getCatalogName(i));
             rsmdRow.setSchemaName(i, rsmdWrite.getSchemaName(i));
          } catch(SQLException e) {
               e.printStackTrace();
          }
        } //end for

       crsRow.setMetaData(rsmdRow);

       crsRow.moveToInsertRow();

       for(int col=1;col<=crsSync.getMetaData().getColumnCount();col++) {
           crsRow.updateObject(col, crsSync.getObject(col));
       }

       crsRow.insertRow();
       crsRow.moveToCurrentRow();

       crsRow.absolute(1);
       crsRow.setOriginalRow();

      try {
          crsRow.setUrl(crsSync.getUrl());
      } catch(SQLException sqle) {

      }

      try {
          crsRow.setDataSourceName(crsSync.getCommand());
       } catch(SQLException sqle) {

       }

       try {
           if(crsSync.getTableName()!= null){
              crsRow.setTableName(crsSync.getTableName());
           }
        } catch(SQLException sqle) {

        }

       try {
            if(crsSync.getCommand() != null)
                crsRow.setCommand(crsSync.getCommand());
       } catch(SQLException sqle) {

       }

       try {
            crsRow.setKeyColumns(crsSync.getKeyColumns());
       } catch(SQLException sqle) {

       }
       return crsRow;
    }



    /**
     * Sets <i>obj</i> as the value in column <i>columnName</i> in the current row of the
     * {@code RowSet} object. This value is the resolved value that is to be
     * persisted in the data source.
     *
     * @param columnName a {@code String} object giving the name of the column
     *        into which to set the value to be persisted
     * @param obj an {@code Object} that is the value to be set in the data source
     */
    public void setResolvedValue(String columnName, Object obj) throws SQLException {
        // %%% Missing implementation!
        throw new SQLException("Method not supported");
    }

    /**
     * This function is package private,
     * i.e. cannot be accesses outside this package.
     * This is used to set the actual CachedRowSet
     * which is being synchronized to the database
     **/
   void setCachedRowSet(CachedRowSet crs) {
           crsSync = (CachedRowSetImpl)crs;
    }

    /**
     * This function is package private,
     * i.e. cannot be accesses outside this package.
     * This is used to set the CachedRowSet formed
     * with conflict values.
     **/
    void setCachedRowSetResolver(CachedRowSet crs){
         try {
              crsRes = (CachedRowSetImpl)crs;
              crsRes.afterLast();
              sz = crsRes.size();
         } catch (SQLException sqle) {
            // do nothing
         }
    }

    /**
     * This function is package private,
     * i.e. cannot be accesses outside this package.
     * This is used to set the status of each row
     * to either of the values SyncResolver.*_CONFLICT
     **/
    @SuppressWarnings("rawtypes")
    void setStatus(ArrayList status){
             stats = status;
    }

    /**
     * This function is package private,
     * i.e. cannot be accesses outside this package.
     * This is used to set the handle to the writer object
     * which will write the resolved values back to datasource
     **/
    void setCachedRowSetWriter(CachedRowSetWriter CRWriter) {
         crw = CRWriter;
    }

    /**
     * Moves the cursor down one row from its current position. A {@code SyncResolver}
     * cursor is initially positioned before the first conflict row; the first call to the
     * method {@code nextConflict()} makes the first conflict row the current row;
     * the second call makes the second conflict row the current row, and so on.
     * <p>
     * If an input stream is open for the current row, a call to the method next will
     * implicitly close it. A {@code SyncResolver} object's warning chain is cleared
     * when a new row
     *
     * @return true if the new current row is valid; false if there are no more rows
     * @throws SQLException if a database access occurs
     *
     */
    public boolean nextConflict() throws SQLException {
        /**
          * The next() method will hop from
          * one conflict to another
          *
          * Internally do a crs.next() until
          * next conflict.
          **/
      boolean bool = false;

      crsSync.setShowDeleted(true);
      while(crsSync.next()) {
           crsRes.previous();
           rowStatus++;  //sz--;

          if((rowStatus-1) >= stats.size()) {
             bool = false;
             break;
          }

          if(((Integer)stats.get(rowStatus-1)).intValue() == SyncResolver.NO_ROW_CONFLICT) {
              // do nothing
              // bool remains as false
             ;
           } else {
             bool = true;
             break;
           } //end if

      } //end while

        crsSync.setShowDeleted(false);
        return bool;
   } // end next() method


    /**
     * Moves the cursor to the previous conflict row in this {@code SyncResolver} object.
     *
     * @return {@code true} if the cursor is on a valid row; {@code false}
     *     if it is off the result set
     * @throws SQLException if a database access error occurs or the result set type
     *     is TYPE_FORWARD_ONLY
     */
   public boolean previousConflict() throws SQLException {
       return false;
   }

    //-----------------------------------------------------------------------
    // Properties
    //-----------------------------------------------------------------------

    /**
     * Sets this {@code CachedRowSetImpl} object's command property
     * to the given {@code String} object and clears the parameters,
     * if any, that were set for the previous command.
     * <P>
     * The command property may not be needed
     * if the rowset is produced by a data source, such as a spreadsheet,
     * that does not support commands. Thus, this property is optional
     * and may be {@code null}.
     *
     * @param cmd a {@code String} object containing an SQL query
     *            that will be set as the command; may be {@code null}
     * @throws SQLException if an error occurs
     */
    public void setCommand(String cmd) throws SQLException {
         throw new UnsupportedOperationException();
    }


    //---------------------------------------------------------------------
    // Reading and writing data
    //---------------------------------------------------------------------

    /**
     * Populates this {@code CachedRowSetImpl} object with data from
     * the given {@code ResultSet} object.  This
     * method is an alternative to the method {@code execute}
     * for filling the rowset with data.  The method {@code populate}
     * does not require that the properties needed by the method
     * {@code execute}, such as the {@code command} property,
     * be set. This is true because the method {@code populate}
     * is given the {@code ResultSet} object from
     * which to get data and thus does not need to use the properties
     * required for setting up a connection and executing this
     * {@code CachedRowSetImpl} object's command.
     * <P>
     * After populating this rowset with data, the method
     * {@code populate} sets the rowset's metadata and
     * then sends a {@code RowSetChangedEvent} object
     * to all registered listeners prior to returning.
     *
     * @param data the {@code ResultSet} object containing the data
     *             to be read into this {@code CachedRowSetImpl} object
     * @throws SQLException if an error occurs; or the max row setting is
     *          violated while populating the RowSet
     * @see #execute
     */
    public void populate(ResultSet data) throws SQLException {
         throw new UnsupportedOperationException();
    }

    /**
     * Populates this {@code CachedRowSetImpl} object with data,
     * using the given connection to produce the result set from
     * which data will be read.  A second form of this method,
     * which takes no arguments, uses the values from this rowset's
     * user, password, and either url or data source properties to
     * create a new database connection. The form of {@code execute}
     * that is given a connection ignores these properties.
     *
     * @param conn A standard JDBC {@code Connection} object that this
     * {@code CachedRowSet} object can pass to a synchronization provider
     * to establish a connection to the data source
     * @throws SQLException if an invalid {@code Connection} is supplied
     *           or an error occurs in establishing the connection to the
     *           data source
     * @see #populate
     * @see java.sql.Connection
     */
    public void execute(Connection conn) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Propagates all row update, insert, and delete changes to the
     * underlying data source backing this {@code CachedRowSetImpl}
     * object.
     * <P>
     * <b>Note</b>In the reference implementation an optimistic concurrency implementation
     * is provided as a sample implementation of a the {@code SyncProvider}
     * abstract class.
     * <P>
     * This method fails if any of the updates cannot be propagated back
     * to the data source.  When it fails, the caller can assume that
     * none of the updates are reflected in the data source.
     * When an exception is thrown, the current row
     * is set to the first "updated" row that resulted in an exception
     * unless the row that caused the exception is a "deleted" row.
     * In that case, when deleted rows are not shown, which is usually true,
     * the current row is not affected.
     * <P>
     * If no {@code SyncProvider} is configured, the reference implementation
     * leverages the {@code RIOptimisticProvider} available which provides the
     * default and reference synchronization capabilities for disconnected
     * {@code RowSets}.
     *
     * @throws SQLException if the cursor is on the insert row or the underlying
     *          reference synchronization provider fails to commit the updates
     *          to the datasource
     * @throws SyncProviderException if an internal error occurs within the
     *          {@code SyncProvider} instance during either during the
     *          process or at any time when the {@code SyncProvider}
     *          instance touches the data source.
     * @see #acceptChanges(java.sql.Connection)
     * @see javax.sql.RowSetWriter
     * @see javax.sql.rowset.spi.SyncProvider
     */
    public void acceptChanges() throws SyncProviderException {
        throw new UnsupportedOperationException();
    }

    /**
     * Propagates all row update, insert, and delete changes to the
     * data source backing this {@code CachedRowSetImpl} object
     * using the given {@code Connection} object.
     * <P>
     * The reference implementation {@code RIOptimisticProvider}
     * modifies its synchronization to a write back function given
     * the updated connection
     * The reference implementation modifies its synchronization behaviour
     * via the {@code SyncProvider} to ensure the synchronization
     * occurs according to the updated JDBC {@code Connection}
     * properties.
     *
     * @param con a standard JDBC {@code Connection} object
     * @throws SQLException if the cursor is on the insert row or the underlying
     *                   synchronization provider fails to commit the updates
     *                   back to the data source
     * @see #acceptChanges
     * @see javax.sql.RowSetWriter
     * @see javax.sql.rowset.spi.SyncFactory
     * @see javax.sql.rowset.spi.SyncProvider
     */
    public void acceptChanges(Connection con) throws SyncProviderException{
     throw new UnsupportedOperationException();
    }

    /**
     * Restores this {@code CachedRowSetImpl} object to its original state,
     * that is, its state before the last set of changes.
     * <P>
     * Before returning, this method moves the cursor before the first row
     * and sends a {@code rowSetChanged} event to all registered
     * listeners.
     * @throws SQLException if an error is occurs rolling back the RowSet
     *           state to the definied original value.
     * @see javax.sql.RowSetListener#rowSetChanged
     */
    public void restoreOriginal() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Releases the current contents of this {@code CachedRowSetImpl}
     * object and sends a {@code rowSetChanged} event object to all
     * registered listeners.
     *
     * @throws SQLException if an error occurs flushing the contents of
     *           RowSet.
     * @see javax.sql.RowSetListener#rowSetChanged
     */
    public void release() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Cancels deletion of the current row and notifies listeners that
     * a row has changed.
     * <P>
     * Note:  This method can be ignored if deleted rows are not being shown,
     * which is the normal case.
     *
     * @throws SQLException if the cursor is not on a valid row
     */
    public void undoDelete() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Immediately removes the current row from this
     * {@code CachedRowSetImpl} object if the row has been inserted, and
     * also notifies listeners the a row has changed.  An exception is thrown
     * if the row is not a row that has been inserted or the cursor is before
     * the first row, after the last row, or on the insert row.
     * <P>
     * This operation cannot be undone.
     *
     * @throws SQLException if an error occurs,
     *                         the cursor is not on a valid row,
     *                         or the row has not been inserted
     */
    public void undoInsert() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Immediately reverses the last update operation if the
     * row has been modified. This method can be
     * called to reverse updates on a all columns until all updates in a row have
     * been rolled back to their originating state since the last synchronization
     * ({@code acceptChanges}) or population. This method may also be called
     * while performing updates to the insert row.
     * <P>
     * {@code undoUpdate} may be called at any time during the life-time of a
     * rowset, however after a synchronization has occurs this method has no
     * affect until further modification to the RowSet data occurs.
     *
     * @throws SQLException if cursor is before the first row, after the last
     *     row in rowset.
     * @see #undoDelete
     * @see #undoInsert
     * @see java.sql.ResultSet#cancelRowUpdates
     */
    public void undoUpdate() throws SQLException {
        throw new UnsupportedOperationException();

    }

    //--------------------------------------------------------------------
    // Views
    //--------------------------------------------------------------------

    /**
     * Returns a new {@code RowSet} object backed by the same data as
     * that of this {@code CachedRowSetImpl} object and sharing a set of cursors
     * with it. This allows cursors to interate over a shared set of rows, providing
     * multiple views of the underlying data.
     *
     * @return a {@code RowSet} object that is a copy of this {@code CachedRowSetImpl}
     * object and shares a set of cursors with it
     * @throws SQLException if an error occurs or cloning is
     *                         not supported
     * @see javax.sql.RowSetEvent
     * @see javax.sql.RowSetListener
     */
    public RowSet createShared() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a new {@code RowSet} object containing by the same data
     * as this {@code CachedRowSetImpl} object.  This method
     * differs from the method {@code createCopy} in that it throws a
     * {@code CloneNotSupportedException} object instead of an
     * {@code SQLException} object, as the method {@code createShared}
     * does.  This {@code clone}
     * method is called internally by the method {@code createShared},
     * which catches the {@code CloneNotSupportedException} object
     * and in turn throws a new {@code SQLException} object.
     *
     * @return a copy of this {@code CachedRowSetImpl} object
     * @throws CloneNotSupportedException if an error occurs when
     * attempting to clone this {@code CachedRowSetImpl} object
     * @see #createShared
     */
    protected Object clone() throws CloneNotSupportedException  {
       throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@code RowSet} object that is a deep copy of
     * this {@code CachedRowSetImpl} object's data, including
     * constraints.  Updates made
     * on a copy are not visible to the original rowset;
     * a copy of a rowset is completely independent from the original.
     * <P>
     * Making a copy saves the cost of creating an identical rowset
     * from first principles, which can be quite expensive.
     * For example, it can eliminate the need to query a
     * remote database server.
     * @return a new {@code CachedRowSet} object that is a deep copy
     *           of this {@code CachedRowSet} object and is
     *           completely independent from this {@code CachedRowSetImpl}
     *           object.
     * @throws SQLException if an error occurs in generating the copy of this
     *           of the {@code CachedRowSetImpl}
     * @see #createShared
     * @see javax.sql.RowSetEvent
     * @see javax.sql.RowSetListener
     */
    public CachedRowSet createCopy() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@code RowSet} object that is a copy of
     * this {@code CachedRowSetImpl} object's table structure
     * and the constraints only.
     * There will be no data in the object being returned.
     * Updates made on a copy are not visible to the original rowset.
     * <P>
     * This helps in getting the underlying XML schema which can
     * be used as the basis for populating a {@code WebRowSet}.
     *
     * @return a new {@code CachedRowSet} object that is a copy
     * of this {@code CachedRowSetImpl} object's schema and
     * retains all the constraints on the original rowset but contains
     * no data
     * @throws SQLException if an error occurs in generating the copy
     * of the {@code CachedRowSet} object
     * @see #createShared
     * @see #createCopy
     * @see #createCopyNoConstraints
     * @see javax.sql.RowSetEvent
     * @see javax.sql.RowSetListener
     */
    public CachedRowSet createCopySchema() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@code CachedRowSet} object that is a copy of
     * this {@code CachedRowSetImpl} object's data only.
     * All constraints set in this object will not be there
     * in the returning object.  Updates made
     * on a copy are not visible to the original rowset.
     *
     * @return a new {@code CachedRowSet} object that is a deep copy
     * of this {@code CachedRowSetImpl} object and is
     * completely independent from this {@code CachedRowSetImpl} object
     * @throws SQLException if an error occurs in generating the copy of the
     * of the {@code CachedRowSet}
     * @see #createShared
     * @see #createCopy
     * @see #createCopySchema
     * @see javax.sql.RowSetEvent
     * @see javax.sql.RowSetListener
     */
    public CachedRowSet createCopyNoConstraints() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts this {@code CachedRowSetImpl} object to a collection
     * of tables. The sample implementation utilitizes the {@code TreeMap}
     * collection type.
     * This class guarantees that the map will be in ascending key order,
     * sorted according to the natural order for the key's class.
     *
     * @return a {@code Collection} object consisting of tables,
     *         each of which is a copy of a row in this
     *         {@code CachedRowSetImpl} object
     * @throws SQLException if an error occurs in generating the collection
     * @see #toCollection(int)
     * @see #toCollection(String)
     * @see java.util.TreeMap
     */
    @SuppressWarnings("rawtypes")
    public Collection toCollection() throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Returns the specified column of this {@code CachedRowSetImpl} object
     * as a {@code Collection} object.  This method makes a copy of the
     * column's data and utilitizes the {@code Vector} to establish the
     * collection. The {@code Vector} class implements a growable array
     * objects allowing the individual components to be accessed using an
     * an integer index similar to that of an array.
     *
     * @return a {@code Collection} object that contains the value(s)
     *         stored in the specified column of this
     *         {@code CachedRowSetImpl}
     *         object
     * @throws SQLException if an error occurs generated the collection; or
     *          an invalid column is provided.
     * @see #toCollection()
     * @see #toCollection(String)
     * @see java.util.Vector
     */
    @SuppressWarnings("rawtypes")
    public Collection toCollection(int column) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Returns the specified column of this {@code CachedRowSetImpl} object
     * as a {@code Collection} object.  This method makes a copy of the
     * column's data and utilitizes the {@code Vector} to establish the
     * collection. The {@code Vector} class implements a growable array
     * objects allowing the individual components to be accessed using an
     * an integer index similar to that of an array.
     *
     * @return a {@code Collection} object that contains the value(s)
     *         stored in the specified column of this
     *         {@code CachedRowSetImpl}
     *         object
     * @throws SQLException if an error occurs generated the collection; or
     *          an invalid column is provided.
     * @see #toCollection()
     * @see #toCollection(int)
     * @see java.util.Vector
     */
    @SuppressWarnings("rawtypes")
    public Collection toCollection(String column) throws SQLException {
        throw new UnsupportedOperationException();
    }

    //--------------------------------------------------------------------
    // Advanced features
    //--------------------------------------------------------------------


    /**
     * Returns the {@code SyncProvider} implementation being used
     * with this {@code CachedRowSetImpl} implementation rowset.
     *
     * @return the SyncProvider used by the rowset. If not provider was
     *          set when the rowset was instantiated, the reference
     *          implementation (default) provider is returned.
     * @throws SQLException if error occurs while return the
     *          {@code SyncProvider} instance.
     */
    public SyncProvider getSyncProvider() throws SQLException {
      throw new UnsupportedOperationException();
    }

    /**
     * Sets the active {@code SyncProvider} and attempts to load
     * load the new provider using the {@code SyncFactory} SPI.
     *
     * @throws SQLException if an error occurs while resetting the
     *          {@code SyncProvider}.
     */
    public void setSyncProvider(String providerStr) throws SQLException {
        throw new UnsupportedOperationException();
    }


    //-----------------
    // methods inherited from RowSet
    //-----------------






    //---------------------------------------------------------------------
    // Reading and writing data
    //---------------------------------------------------------------------

    /**
     * Populates this {@code CachedRowSetImpl} object with data.
     * This form of the method uses the rowset's user, password, and url or
     * data source name properties to create a database
     * connection.  If properties that are needed
     * have not been set, this method will throw an exception.
     * <P>
     * Another form of this method uses an existing JDBC {@code Connection}
     * object instead of creating a new one; therefore, it ignores the
     * properties used for establishing a new connection.
     * <P>
     * The query specified by the command property is executed to create a
     * {@code ResultSet} object from which to retrieve data.
     * The current contents of the rowset are discarded, and the
     * rowset's metadata is also (re)set.  If there are outstanding updates,
     * they are also ignored.
     * <P>
     * The method {@code execute} closes any database connections that it
     * creates.
     *
     * @throws SQLException if an error occurs or the
     *                         necessary properties have not been set
     */
    public void execute() throws SQLException {
        throw new UnsupportedOperationException();
    }



    //-----------------------------------
    // Methods inherited from ResultSet
    //-----------------------------------

    /**
     * Moves the cursor down one row from its current position and
     * returns {@code true} if the new cursor position is a
     * valid row.
     * The cursor for a new {@code ResultSet} object is initially
     * positioned before the first row. The first call to the method
     * {@code next} moves the cursor to the first row, making it
     * the current row; the second call makes the second row the
     * current row, and so on.
     *
     * <P>If an input stream from the previous row is open, it is
     * implicitly closed. The {@code ResultSet} object's warning
     * chain is cleared when a new row is read.
     *
     * @return {@code true} if the new current row is valid;
     *         {@code false} if there are no more rows
     * @throws SQLException if an error occurs or
     *            the cursor is not positioned in the rowset, before
     *            the first row, or after the last row
     */
    public boolean next() throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Moves this {@code CachedRowSetImpl} object's cursor to the next
     * row and returns {@code true} if the cursor is still in the rowset;
     * returns {@code false} if the cursor has moved to the position after
     * the last row.
     * <P>
     * This method handles the cases where the cursor moves to a row that
     * has been deleted.
     * If this rowset shows deleted rows and the cursor moves to a row
     * that has been deleted, this method moves the cursor to the next
     * row until the cursor is on a row that has not been deleted.
     * <P>
     * The method {@code internalNext} is called by methods such as
     * {@code next}, {@code absolute}, and {@code relative},
     * and, as its name implies, is only called internally.
     * <p>
     * This is a implementation only method and is not required as a standard
     * implementation of the {@code CachedRowSet} interface.
     *
     * @return {@code true} if the cursor is on a valid row in this
     *         rowset; {@code false} if it is after the last row
     * @throws SQLException if an error occurs
     */
    protected boolean internalNext() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Closes this {@code CachedRowSetImpl} objecy and releases any resources
     * it was using.
     *
     * @throws SQLException if an error occurs when releasing any resources in use
     * by this {@code CachedRowSetImpl} object
     */
    public void close() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Reports whether the last column read was SQL {@code NULL}.
     * Note that you must first call the method {@code getXXX}
     * on a column to try to read its value and then call the method
     * {@code wasNull} to determine whether the value was
     * SQL {@code NULL}.
     *
     * @return {@code true} if the value in the last column read
     *         was SQL {@code NULL}; {@code false} otherwise
     * @throws SQLException if an error occurs
     */
    public boolean wasNull() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the insert row or the current row of this
     * {@code CachedRowSetImpl} object.
     *
     * @return the {@code Row} object on which this {@code CachedRowSetImpl}
     * objects's cursor is positioned
     */
    protected BaseRow getCurrentRow() {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the row on which the cursor is positioned.
     * <p>
     * This is a implementation only method and is not required as a standard
     * implementation of the {@code CachedRowSet} interface.
     *
     * @throws SQLException if the cursor is positioned on the insert
     *            row
     */
    protected void removeCurrentRow() {
        throw new UnsupportedOperationException();
    }


    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code String} object.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value; if the value is SQL {@code NULL}, the
     *         result is {@code null}
     * @throws SQLException if (1) the given column index is out of bounds,
     * (2) the cursor is not on one of this rowset's rows or its
     * insert row, or (3) the designated column does not store an
     * SQL {@code TINYINT, SMALLINT, INTEGER, BIGINT, REAL,
     * FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,} <b>{@code CHAR, VARCHAR}</b>
     * or {@code LONGVARCHAR} value. The bold SQL type designates the
     * recommended return type.
     */
    public String getString(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code boolean} value.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value as a {@code boolean} in the Java progamming language;
     *        if the value is SQL {@code NULL}, the result is {@code false}
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) the designated column does not store an
     *            SQL {@code BOOLEAN} value
     * @see #getBoolean(String)
     */
    public boolean getBoolean(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code byte} value.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value as a {@code byte} in the Java programming
     * language; if the value is SQL {@code NULL}, the result is {@code 0}
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) the designated column does not store an
     *            SQL <b>{@code TINYINT}</b>, {@code SMALLINT, INTEGER,
     *            BIGINT, REAL, FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR,
     *            VARCHAR} or {@code LONGVARCHAR} value. The bold SQL type
     *            designates the recommended return type.
     * @see #getByte(String)
     */
    public byte getByte(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code short} value.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value; if the value is SQL {@code NULL}, the
     *         result is {@code 0}
     * @throws SQLException if (1) the given column index is out of bounds,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code TINYINT}, <b>{@code SMALLINT}</b>,
     *         {@code INTEGER, BIGINT, REAL, FLOAT, DOUBLE,
     *         DECIMAL, NUMERIC, BIT, CHAR, VARCHAR}
     *         or {@code LONGVARCHAR} value. The bold SQL type
     *         designates the recommended return type.
     * @see #getShort(String)
     */
    public short getShort(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as an
     * {@code int} value.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value; if the value is SQL {@code NULL}, the
     *         result is {@code 0}
     * @throws SQLException if (1) the given column index is out of bounds,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code TINYINT, SMALLINT,} <b>{@code INTEGER}</b>,
     *         {@code BIGINT, REAL, FLOAT, DOUBLE, DECIMAL,
     *         NUMERIC, BIT, CHAR, VARCHAR} or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the
     *         recommended return type.
     */
    public int getInt(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code long} value.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value; if the value is SQL {@code NULL}, the
     *         result is {@code 0}
     * @throws SQLException if (1) the given column index is out of bounds,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code TINYINT, SMALLINT, INTEGER,}
     *         <b>{@code BIGINT}</b>, {@code REAL, FLOAT, DOUBLE,
     *         DECIMAL, NUMERIC, BIT, CHAR, VARCHAR}
     *         or {@code LONGVARCHAR} value. The bold SQL type
     * designates the recommended return type.
     * @see #getLong(String)
     */
    public long getLong(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code float} value.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value; if the value is SQL {@code NULL}, the
     *         result is {@code 0}
     * @throws SQLException if (1) the given column index is out of bounds,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code TINYINT, SMALLINT, INTEGER, BIGINT,}
     *         <b>{@code REAL}</b>, {@code FLOAT, DOUBLE, DECIMAL, NUMERIC,
     *         BIT, CHAR, VARCHAR} or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the recommended return type.
     * @see #getFloat(String)
     */
    public float getFloat(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code double} value.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value; if the value is SQL {@code NULL}, the
     *         result is {@code 0}
     * @throws SQLException if (1) the given column index is out of bounds,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code TINYINT, SMALLINT, INTEGER, BIGINT, REAL,}
     *         <b>{@code FLOAT, DOUBLE}</b>,
     *         {@code DECIMAL, NUMERIC, BIT, CHAR, VARCHAR}
     *         or {@code LONGVARCHAR} value. The bold SQL type
     *         designates the recommended return type.
     * @see #getDouble(String)
     *
     */
    public double getDouble(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code java.math.BigDecimal} object.
     * <P>
     * This method is deprecated; use the version of {@code getBigDecimal}
     * that does not take a scale parameter and returns a value with full
     * precision.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @param scale the number of digits to the right of the decimal point in the
     *        value returned
     * @return the column value with the specified number of digits to the right
     *         of the decimal point; if the value is SQL {@code NULL}, the
     *         result is {@code null}
     * @throws SQLException if the given column index is out of bounds,
     *            the cursor is not on a valid row, or this method fails
     * @deprecated
     */
    @Deprecated
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code byte} array value.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value as a {@code byte} array in the Java programming
     * language; if the value is SQL {@code NULL}, the
     * result is {@code null}
     *
     * @throws SQLException if (1) the given column index is out of bounds,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL <b>{@code BINARY, VARBINARY}</b> or
     *         {@code LONGVARBINARY} value.
     *         The bold SQL type designates the recommended return type.
     * @see #getBytes(String)
     */
    public byte[] getBytes(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code java.sql.Date} object.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value as a {@code java.sql.Data} object; if
     *        the value is SQL {@code NULL}, the
     *        result is {@code null}
     * @throws SQLException if the given column index is out of bounds,
     *         the cursor is not on a valid row, or this method fails
     */
    public java.sql.Date getDate(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code java.sql.Time} object.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value; if the value is SQL {@code NULL}, the
     *         result is {@code null}
     * @throws SQLException if the given column index is out of bounds,
     *         the cursor is not on a valid row, or this method fails
     */
    public java.sql.Time getTime(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code java.sql.Timestamp} object.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return the column value; if the value is SQL {@code NULL}, the
     *         result is {@code null}
     * @throws SQLException if the given column index is out of bounds,
     *            the cursor is not on a valid row, or this method fails
     */
    public java.sql.Timestamp getTimestamp(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row of this
     * {@code CachedRowSetImpl} object as a {@code java.io.InputStream}
     * object.
     *
     * A column value can be retrieved as a stream of ASCII characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large {@code LONGVARCHAR} values.  The JDBC
     * driver will do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a get method implicitly closes the stream. . Also, a
     * stream may return {@code 0} for {@code CachedRowSetImpl.available()}
     * whether there is data available or not.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @return a Java input stream that delivers the database column value
     *         as a stream of one-byte ASCII characters.  If the value is SQL
     *         {@code NULL}, the result is {@code null}.
     * @throws SQLException if (1) the given column index is out of bounds,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code CHAR, VARCHAR}, <b>{@code LONGVARCHAR}</b>,
     *         {@code BINARY, VARBINARY} or {@code LONGVARBINARY} value. The
     *         bold SQL type designates the recommended return types
     *         that this method is used to retrieve.
     * @see #getAsciiStream(String)
     */
    public java.io.InputStream getAsciiStream(int columnIndex) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * A column value can be retrieved as a stream of Unicode characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARCHAR values.  The JDBC driver will
     * do any necessary conversion from the database format into Unicode.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a get method implicitly closes the stream. . Also, a
     * stream may return 0 for available() whether there is data
     * available or not.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @return a Java input stream that delivers the database column value
     * as a stream of two byte Unicode characters.  If the value is SQL NULL
     * then the result is null.
     * @throws SQLException if an error occurs
     * @deprecated
     */
    @Deprecated
    public java.io.InputStream getUnicodeStream(int columnIndex) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row of this
     * {@code CachedRowSetImpl} object as a {@code java.io.InputStream}
     * object.
     * <P>
     * A column value can be retrieved as a stream of uninterpreted bytes
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large {@code LONGVARBINARY} values.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a get method implicitly closes the stream. Also, a
     * stream may return {@code 0} for
     * {@code CachedRowSetImpl.available()} whether there is data
     * available or not.
     *
     * @param columnIndex the first column is {@code 1}, the second
     * is {@code 2}, and so on; must be {@code 1} or larger
     * and equal to or less than the number of columns in the rowset
     * @return a Java input stream that delivers the database column value
     * as a stream of uninterpreted bytes.  If the value is SQL {@code NULL}
     * then the result is {@code null}.
     * @throws SQLException if (1) the given column index is out of bounds,
     * (2) the cursor is not on one of this rowset's rows or its
     * insert row, or (3) the designated column does not store an
     * SQL {@code BINARY, VARBINARY} or <b>{@code LONGVARBINARY}</b>.
     * The bold type indicates the SQL type that this method is recommened
     * to retrieve.
     * @see #getBinaryStream(String)
     */
    public java.io.InputStream getBinaryStream(int columnIndex) throws SQLException {
       throw new UnsupportedOperationException();

    }


    //======================================================================
    // Methods for accessing results by column name
    //======================================================================

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code String} object.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL {@code TINYINT, SMALLINT, INTEGER,
     *         BIGINT, REAL, FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,}
     *         <b>{@code CHAR, VARCHAR}</b> or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the recommended return type.
     */
    public String getString(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code boolean} value.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value as a {@code boolean} in the Java programming
     *        language; if the value is SQL {@code NULL},
     *        the result is {@code false}
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL {@code BOOLEAN} value
     * @see #getBoolean(int)
     */
    public boolean getBoolean(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code byte} value.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value as a {@code byte} in the Java programming
     * language; if the value is SQL {@code NULL}, the result is {@code 0}
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL <b>{@code TINYINT}</b>,
     *         {@code SMALLINT, INTEGER, BIGINT, REAL, FLOAT, DOUBLE,
     *         DECIMAL, NUMERIC, BIT, CHAR, VARCHAR} or {@code LONGVARCHAR}
     *         value. The bold type designates the recommended return type.
     */
    public byte getByte(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code short} value.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code 0}
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL {@code TINYINT,}
     *         <b>{@code SMALLINT}</b>, {@code INTEGER,
     *         BIGINT, REAL, FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR,
     *         VARCHAR} or {@code LONGVARCHAR} value. The bold SQL type
     *         designates the recommended return type.
     * @see #getShort(int)
     */
    public short getShort(String columnName) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as an {@code int} value.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code 0}
     * @throws SQLException if (1) the given column name is not the name
     *         of a column in this rowset,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code TINYINT, SMALLINT,} <b>{@code INTEGER}</b>,
     *         {@code BIGINT, REAL, FLOAT, DOUBLE, DECIMAL,
     *         NUMERIC, BIT, CHAR, VARCHAR} or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the
     *         recommended return type.
     */
    public int getInt(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code long} value.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code 0}
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL {@code TINYINT, SMALLINT, INTEGER,}
     *         <b>{@code BIGINT}</b>, {@code REAL, FLOAT, DOUBLE, DECIMAL,
     *         NUMERIC, BIT, CHAR, VARCHAR} or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the recommended return type.
     * @see #getLong(int)
     */
    public long getLong(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code float} value.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code 0}
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL {@code TINYINT, SMALLINT, INTEGER,
     *         BIGINT,} <b>{@code REAL}</b>, {@code FLOAT, DOUBLE, DECIMAL,
     *         NUMERIC, BIT, CHAR, VARCHAR} or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the recommended return type.
     * @see #getFloat(String)
     */
    public float getFloat(String columnName) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row of this {@code CachedRowSetImpl} object
     * as a {@code double} value.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code 0}
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL {@code TINYINT, SMALLINT, INTEGER,
     *         BIGINT, REAL,} <b>{@code FLOAT, DOUBLE}</b>, {@code DECIMAL,
     *         NUMERIC, BIT, CHAR, VARCHAR} or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the recommended return types.
     * @see #getDouble(int)
     */
    public double getDouble(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code java.math.BigDecimal} object.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @param scale the number of digits to the right of the decimal point
     * @return a java.math.BugDecimal object with <i>{@code scale}</i>
     *         number of digits to the right of the decimal point.
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL {@code TINYINT, SMALLINT, INTEGER,
     *         BIGINT, REAL, FLOAT, DOUBLE,} <b>{@code DECIMAL, NUMERIC}</b>,
     *         {@code BIT, CHAR, VARCHAR} or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the recommended return type
     *         that this method is used to retrieve.
     * @deprecated Use the {@code getBigDecimal(String columnName)}
     *             method instead
     */
    @Deprecated
    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code byte} array.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value as a {@code byte} array in the Java programming
     *         language; if the value is SQL {@code NULL}, the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL <b>{@code BINARY, VARBINARY}</b>
     *         or {@code LONGVARBINARY} values.
     *         The bold SQL type designates the recommended return type.
     * @see #getBytes(int)
     */
    public byte[] getBytes(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code java.sql.Date} object.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code DATE} or
     *            {@code TIMESTAMP} value
     */
    public java.sql.Date getDate(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code java.sql.Time} object.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if the given column name does not match one of
     *            this rowset's column names or the cursor is not on one of
     *            this rowset's rows or its insert row
     */
    public java.sql.Time getTime(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code java.sql.Timestamp} object.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if the given column name does not match one of
     *            this rowset's column names or the cursor is not on one of
     *            this rowset's rows or its insert row
     */
    public java.sql.Timestamp getTimestamp(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row of this
     * {@code CachedRowSetImpl} object as a {@code java.io.InputStream}
     * object.
     *
     * A column value can be retrieved as a stream of ASCII characters
     * and then read in chunks from the stream. This method is particularly
     * suitable for retrieving large {@code LONGVARCHAR} values. The
     * {@code SyncProvider} will rely on the JDBC driver to do any necessary
     * conversion from the database format into ASCII format.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a {@code getXXX} method implicitly closes the stream.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return a Java input stream that delivers the database column value
     *         as a stream of one-byte ASCII characters.  If the value is SQL
     *         {@code NULL}, the result is {@code null}.
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code CHAR, VARCHAR}, <b>{@code LONGVARCHAR}</b>,
     *         {@code BINARY, VARBINARY} or {@code LONGVARBINARY} value. The
     *         bold SQL type designates the recommended return types
     *         that this method is used to retrieve.
     * @see #getAsciiStream(int)
     */
    public java.io.InputStream getAsciiStream(String columnName) throws SQLException {
        throw new UnsupportedOperationException();

    }

    /**
     * A column value can be retrieved as a stream of Unicode characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large {@code LONGVARCHAR} values.
     * The JDBC driver will do any necessary conversion from the database
     * format into Unicode.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a {@code getXXX} method implicitly closes the stream.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return a Java input stream that delivers the database column value
     *         as a stream of two-byte Unicode characters.  If the value is
     *         SQL {@code NULL}, the result is {@code null}.
     * @throws SQLException if the given column name does not match one of
     *            this rowset's column names or the cursor is not on one of
     *            this rowset's rows or its insert row
     * @deprecated use the method {@code getCharacterStream} instead
     */
    @Deprecated
    public java.io.InputStream getUnicodeStream(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row of this
     * {@code CachedRowSetImpl} object as a {@code java.io.InputStream}
     * object.
     * <P>
     * A column value can be retrieved as a stream of uninterpreted bytes
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large {@code LONGVARBINARY} values.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a get method implicitly closes the stream. Also, a
     * stream may return {@code 0} for {@code CachedRowSetImpl.available()}
     * whether there is data available or not.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return a Java input stream that delivers the database column value
     *         as a stream of uninterpreted bytes.  If the value is SQL
     *         {@code NULL}, the result is {@code null}.
     * @throws SQLException if (1) the given column name is unknown,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code BINARY, VARBINARY} or <b>{@code LONGVARBINARY}</b>
     *         The bold type indicates the SQL type that this method is recommened
     *         to retrieve.
     * @see #getBinaryStream(int)
     *
     */
    public java.io.InputStream getBinaryStream(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }


    //=====================================================================
    // Advanced features:
    //=====================================================================

    /**
     * The first warning reported by calls on this {@code CachedRowSetImpl}
     * object is returned. Subsequent {@code CachedRowSetImpl} warnings will
     * be chained to this {@code SQLWarning}.
     *
     * <P>The warning chain is automatically cleared each time a new
     * row is read.
     *
     * <P><B>Note:</B> This warning chain only covers warnings caused
     * by {@code ResultSet} methods.  Any warning caused by statement
     * methods (such as reading OUT parameters) will be chained on the
     * {@code Statement} object.
     *
     * @return the first SQLWarning or null
     */
    public SQLWarning getWarnings() {
        throw new UnsupportedOperationException();
    }

    /**
     * Clears all the warnings reporeted for the {@code CachedRowSetImpl}
     * object. After a call to this method, the {@code getWarnings} method
     * returns {@code null} until a new warning is reported for this
     * {@code CachedRowSetImpl} object.
     */
    public void clearWarnings() {
       throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the name of the SQL cursor used by this
     * {@code CachedRowSetImpl} object.
     *
     * <P>In SQL, a result table is retrieved through a cursor that is
     * named. The current row of a {@code ResultSet} can be updated or deleted
     * using a positioned update/delete statement that references the
     * cursor name. To ensure that the cursor has the proper isolation
     * level to support an update operation, the cursor's {@code SELECT}
     * statement should be of the form {@code select for update}.
     * If the {@code for update} clause
     * is omitted, positioned updates may fail.
     *
     * <P>JDBC supports this SQL feature by providing the name of the
     * SQL cursor used by a {@code ResultSet} object. The current row
     * of a result set is also the current row of this SQL cursor.
     *
     * <P><B>Note:</B> If positioned updates are not supported, an
     * {@code SQLException} is thrown.
     *
     * @return the SQL cursor name for this {@code CachedRowSetImpl} object's
     *         cursor
     * @throws SQLException if an error occurs
     */
    public String getCursorName() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a {@code ResultSetMetaData} object instance that
     * contains information about the {@code CachedRowSet} object.
     * However, applications should cast the returned object to a
     * {@code RowSetMetaData} interface implementation. In the
     * reference implementation, this cast can be done on the
     * {@code RowSetMetaDataImpl} class.
     * <P>
     * For example:
     * <pre>
     * CachedRowSet crs = new CachedRowSetImpl();
     * RowSetMetaDataImpl metaData =
     *     (RowSetMetaDataImpl)crs.getMetaData();
     * // Set the number of columns in the RowSet object for
     * // which this RowSetMetaDataImpl object was created to the
     * // given number.
     * metaData.setColumnCount(3);
     * crs.setMetaData(metaData);
     * </pre>
     *
     * @return the {@code ResultSetMetaData} object that describes this
     *         {@code CachedRowSetImpl} object's columns
     * @throws SQLException if an error occurs in generating the RowSet
     * meta data; or if the {@code CachedRowSetImpl} is empty.
     * @see javax.sql.RowSetMetaData
     */
    public ResultSetMetaData getMetaData() throws SQLException {
        throw new UnsupportedOperationException();
    }


    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as an
     * {@code Object} value.
     * <P>
     * The type of the {@code Object} will be the default
     * Java object type corresponding to the column's SQL type,
     * following the mapping for built-in types specified in the JDBC 3.0
     * specification.
     * <P>
     * This method may also be used to read datatabase-specific
     * abstract data types.
     * <P>
     * This implementation of the method {@code getObject} extends its
     * behavior so that it gets the attributes of an SQL structured type
     * as an array of {@code Object} values.  This method also custom
     * maps SQL user-defined types to classes in the Java programming language.
     * When the specified column contains
     * a structured or distinct value, the behavior of this method is as
     * if it were a call to the method {@code getObject(columnIndex,
     * this.getStatement().getConnection().getTypeMap())}.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return a {@code java.lang.Object} holding the column value;
     *         if the value is SQL {@code NULL}, the result is {@code null}
     * @throws SQLException if the given column index is out of bounds,
     *            the cursor is not on a valid row, or there is a problem getting
     *            the {@code Class} object for a custom mapping
     * @see #getObject(String)
     */
    public Object getObject(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as an
     * {@code Object} value.
     * <P>
     * The type of the {@code Object} will be the default
     * Java object type corresponding to the column's SQL type,
     * following the mapping for built-in types specified in the JDBC 3.0
     * specification.
     * <P>
     * This method may also be used to read datatabase-specific
     * abstract data types.
     * <P>
     * This implementation of the method {@code getObject} extends its
     * behavior so that it gets the attributes of an SQL structured type
     * as an array of {@code Object} values.  This method also custom
     * maps SQL user-defined types to classes
     * in the Java programming language. When the specified column contains
     * a structured or distinct value, the behavior of this method is as
     * if it were a call to the method {@code getObject(columnIndex,
     * this.getStatement().getConnection().getTypeMap())}.
     *
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @return a {@code java.lang.Object} holding the column value;
     *         if the value is SQL {@code NULL}, the result is {@code null}
     * @throws SQLException if (1) the given column name does not match one of
     *            this rowset's column names, (2) the cursor is not
     *            on a valid row, or (3) there is a problem getting
     *            the {@code Class} object for a custom mapping
     * @see #getObject(int)
     */
    public Object getObject(String columnName) throws SQLException {
       throw new UnsupportedOperationException();
    }

    //----------------------------------------------------------------

    /**
     * Maps the given column name for one of this {@code CachedRowSetImpl}
     * object's columns to its column number.
     *
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @return the column index of the given column name
     * @throws SQLException if the given column name does not match one
     *            of this rowset's column names
     */
    public int findColumn(String columnName) throws SQLException {
       throw new UnsupportedOperationException();
    }

    //--------------------------JDBC 2.0-----------------------------------

    //---------------------------------------------------------------------
    // Getter's and Setter's
    //---------------------------------------------------------------------

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code java.io.Reader} object.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a {@code getXXX} method implicitly closes the stream.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return a Java character stream that delivers the database column value
     * as a stream of two-byte unicode characters in a
     * {@code java.io.Reader} object.  If the value is
     * SQL {@code NULL}, the result is {@code null}.
     * @throws SQLException if (1) the given column index is out of bounds,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code CHAR, VARCHAR,} <b>{@code LONGVARCHAR}</b>,
     *         {@code BINARY, VARBINARY} or {@code LONGVARBINARY} value.
     *         The bold SQL type designates the recommended return type.
     * @see #getCharacterStream(String)
     */
    public java.io.Reader getCharacterStream(int columnIndex) throws SQLException{
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value stored in the designated column
     * of the current row as a {@code java.io.Reader} object.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a {@code getXXX} method implicitly closes the stream.
     *
     * @param columnName a {@code String} object giving the SQL name of
     *        a column in this {@code CachedRowSetImpl} object
     * @return a Java input stream that delivers the database column value
     *         as a stream of two-byte Unicode characters.  If the value is
     *         SQL {@code NULL}, the result is {@code null}.
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL {@code CHAR, VARCHAR,}
     *         <b>{@code LONGVARCHAR}</b>,
     *         {@code BINARY, VARYBINARY} or {@code LONGVARBINARY} value.
     *         The bold SQL type designates the recommended return type.
     */
    public java.io.Reader getCharacterStream(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code java.math.BigDecimal} object.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @return a {@code java.math.BigDecimal} value with full precision;
     *         if the value is SQL {@code NULL}, the result is {@code null}
     * @throws SQLException if (1) the given column index is out of bounds,
     *         (2) the cursor is not on one of this rowset's rows or its
     *         insert row, or (3) the designated column does not store an
     *         SQL {@code TINYINT, SMALLINT, INTEGER, BIGINT, REAL,
     *         FLOAT, DOUBLE,} <b>{@code DECIMAL, NUMERIC}</b>,
     *         {@code BIT, CHAR, VARCHAR} or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the
     *         recommended return types that this method is used to retrieve.
     * @see #getBigDecimal(String)
     */
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code java.math.BigDecimal} object.
     *
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @return a {@code java.math.BigDecimal} value with full precision;
     *         if the value is SQL {@code NULL}, the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *         a column in this rowset, (2) the cursor is not on one of
     *         this rowset's rows or its insert row, or (3) the designated
     *         column does not store an SQL {@code TINYINT, SMALLINT, INTEGER,
     *         BIGINT, REAL, FLOAT, DOUBLE,} <b>{@code DECIMAL, NUMERIC}</b>,
     *         {@code BIT, CHAR, VARCHAR} or {@code LONGVARCHAR} value.
     *         The bold SQL type designates the recommended return type
     *         that this method is used to retrieve.
     * @see #getBigDecimal(int)
     */
    public BigDecimal getBigDecimal(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    //---------------------------------------------------------------------
    // Traversal/Positioning
    //---------------------------------------------------------------------

    /**
     * Returns the number of rows in this {@code CachedRowSetImpl} object.
     *
     * @return number of rows in the rowset
     */
    public int size() {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates whether the cursor is before the first row in this
     * {@code CachedRowSetImpl} object.
     *
     * @return {@code true} if the cursor is before the first row;
     *         {@code false} otherwise or if the rowset contains no rows
     * @throws SQLException if an error occurs
     */
    public boolean isBeforeFirst() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates whether the cursor is after the last row in this
     * {@code CachedRowSetImpl} object.
     *
     * @return {@code true} if the cursor is after the last row;
     *         {@code false} otherwise or if the rowset contains no rows
     * @throws SQLException if an error occurs
     */
    public boolean isAfterLast() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates whether the cursor is on the first row in this
     * {@code CachedRowSetImpl} object.
     *
     * @return {@code true} if the cursor is on the first row;
     *         {@code false} otherwise or if the rowset contains no rows
     * @throws SQLException if an error occurs
     */
    public boolean isFirst() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates whether the cursor is on the last row in this
     * {@code CachedRowSetImpl} object.
     * <P>
     * Note: Calling the method {@code isLast} may be expensive
     * because the JDBC driver might need to fetch ahead one row in order
     * to determine whether the current row is the last row in this rowset.
     *
     * @return {@code true} if the cursor is on the last row;
     *         {@code false} otherwise or if this rowset contains no rows
     * @throws SQLException if an error occurs
     */
    public boolean isLast() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves this {@code CachedRowSetImpl} object's cursor to the front of
     * the rowset, just before the first row. This method has no effect if
     * this rowset contains no rows.
     *
     * @throws SQLException if an error occurs or the type of this rowset
     *            is {@code ResultSet.TYPE_FORWARD_ONLY}
     */
    public void beforeFirst() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves this {@code CachedRowSetImpl} object's cursor to the end of
     * the rowset, just after the last row. This method has no effect if
     * this rowset contains no rows.
     *
     * @throws SQLException if an error occurs
     */
    public void afterLast() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves this {@code CachedRowSetImpl} object's cursor to the first row
     * and returns {@code true} if the operation was successful.  This
     * method also notifies registered listeners that the cursor has moved.
     *
     * @return {@code true} if the cursor is on a valid row;
     *         {@code false} otherwise or if there are no rows in this
     *         {@code CachedRowSetImpl} object
     * @throws SQLException if the type of this rowset
     *            is {@code ResultSet.TYPE_FORWARD_ONLY}
     */
    public boolean first() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves this {@code CachedRowSetImpl} object's cursor to the first
     * row and returns {@code true} if the operation is successful.
     * <P>
     * This method is called internally by the methods {@code first},
     * {@code isFirst}, and {@code absolute}.
     * It in turn calls the method {@code internalNext} in order to
     * handle the case where the first row is a deleted row that is not visible.
     * <p>
     * This is a implementation only method and is not required as a standard
     * implementation of the {@code CachedRowSet} interface.
     *
     * @return {@code true} if the cursor moved to the first row;
     *         {@code false} otherwise
     * @throws SQLException if an error occurs
     */
    protected boolean internalFirst() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves this {@code CachedRowSetImpl} object's cursor to the last row
     * and returns {@code true} if the operation was successful.  This
     * method also notifies registered listeners that the cursor has moved.
     *
     * @return {@code true} if the cursor is on a valid row;
     *         {@code false} otherwise or if there are no rows in this
     *         {@code CachedRowSetImpl} object
     * @throws SQLException if the type of this rowset
     *            is {@code ResultSet.TYPE_FORWARD_ONLY}
     */
    public boolean last() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves this {@code CachedRowSetImpl} object's cursor to the last
     * row and returns {@code true} if the operation is successful.
     * <P>
     * This method is called internally by the method {@code last}
     * when rows have been deleted and the deletions are not visible.
     * The method {@code internalLast} handles the case where the
     * last row is a deleted row that is not visible by in turn calling
     * the method {@code internalPrevious}.
     * <p>
     * This is a implementation only method and is not required as a standard
     * implementation of the {@code CachedRowSet} interface.
     *
     * @return {@code true} if the cursor moved to the last row;
     *         {@code false} otherwise
     * @throws SQLException if an error occurs
     */
    protected boolean internalLast() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the number of the current row in this {@code CachedRowSetImpl}
     * object. The first row is number 1, the second number 2, and so on.
     *
     * @return the number of the current row;  {@code 0} if there is no
     *         current row
     * @throws SQLException if an error occurs; or if the {@code CacheRowSetImpl}
     *         is empty
     */
    public int getRow() throws SQLException {
        return crsSync.getRow();
    }

    /**
     * Moves this {@code CachedRowSetImpl} object's cursor to the row number
     * specified.
     *
     * <p>If the number is positive, the cursor moves to an absolute row with
     * respect to the beginning of the rowset.  The first row is row 1, the second
     * is row 2, and so on.  For example, the following command, in which
     * {@code crs} is a {@code CachedRowSetImpl} object, moves the cursor
     * to the fourth row, starting from the beginning of the rowset.
     * <PRE>{@code
     *
     *    crs.absolute(4);
     *
     * }</PRE>
     * <P>
     * If the number is negative, the cursor moves to an absolute row position
     * with respect to the end of the rowset.  For example, calling
     * {@code absolute(-1)} positions the cursor on the last row,
     * {@code absolute(-2)} moves it on the next-to-last row, and so on.
     * If the {@code CachedRowSetImpl} object {@code crs} has five rows,
     * the following command moves the cursor to the fourth-to-last row, which
     * in the case of a  rowset with five rows, is also the second row, counting
     * from the beginning.
     * <PRE>{@code
     *
     *    crs.absolute(-4);
     *
     * }</PRE>
     *
     * If the number specified is larger than the number of rows, the cursor
     * will move to the position after the last row. If the number specified
     * would move the cursor one or more rows before the first row, the cursor
     * moves to the position before the first row.
     * <P>
     * Note: Calling {@code absolute(1)} is the same as calling the
     * method {@code first()}.  Calling {@code absolute(-1)} is the
     * same as calling {@code last()}.
     *
     * @param row a positive number to indicate the row, starting row numbering from
     *        the first row, which is {@code 1}; a negative number to indicate
     *        the row, starting row numbering from the last row, which is
     *        {@code -1}; it must not be {@code 0}
     * @return {@code true} if the cursor is on the rowset; {@code false}
     *         otherwise
     * @throws SQLException if the given cursor position is {@code 0} or the
     *            type of this rowset is {@code ResultSet.TYPE_FORWARD_ONLY}
     */
    public boolean absolute( int row ) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves the cursor the specified number of rows from the current
     * position, with a positive number moving it forward and a
     * negative number moving it backward.
     * <P>
     * If the number is positive, the cursor moves the specified number of
     * rows toward the end of the rowset, starting at the current row.
     * For example, the following command, in which
     * {@code crs} is a {@code CachedRowSetImpl} object with 100 rows,
     * moves the cursor forward four rows from the current row.  If the
     * current row is 50, the cursor would move to row 54.
     * <PRE>{@code
     *
     *    crs.relative(4);
     *
     * }</PRE>
     * <P>
     * If the number is negative, the cursor moves back toward the beginning
     * the specified number of rows, starting at the current row.
     * For example, calling the method
     * {@code absolute(-1)} positions the cursor on the last row,
     * {@code absolute(-2)} moves it on the next-to-last row, and so on.
     * If the {@code CachedRowSetImpl} object {@code crs} has five rows,
     * the following command moves the cursor to the fourth-to-last row, which
     * in the case of a  rowset with five rows, is also the second row
     * from the beginning.
     * <PRE>{@code
     *
     *    crs.absolute(-4);
     *
     * }</PRE>
     *
     * If the number specified is larger than the number of rows, the cursor
     * will move to the position after the last row. If the number specified
     * would move the cursor one or more rows before the first row, the cursor
     * moves to the position before the first row. In both cases, this method
     * throws an {@code SQLException}.
     * <P>
     * Note: Calling {@code absolute(1)} is the same as calling the
     * method {@code first()}.  Calling {@code absolute(-1)} is the
     * same as calling {@code last()}.  Calling {@code relative(0)}
     * is valid, but it does not change the cursor position.
     *
     * @param rows an {@code int} indicating the number of rows to move
     *             the cursor, starting at the current row; a positive number
     *             moves the cursor forward; a negative number moves the cursor
     *             backward; must not move the cursor past the valid
     *             rows
     * @return {@code true} if the cursor is on a row in this
     *         {@code CachedRowSetImpl} object; {@code false}
     *         otherwise
     * @throws SQLException if there are no rows in this rowset, the cursor is
     *         positioned either before the first row or after the last row, or
     *         the rowset is type {@code ResultSet.TYPE_FORWARD_ONLY}
     */
    public boolean relative(int rows) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves this {@code CachedRowSetImpl} object's cursor to the
     * previous row and returns {@code true} if the cursor is on
     * a valid row or {@code false} if it is not.
     * This method also notifies all listeners registered with this
     * {@code CachedRowSetImpl} object that its cursor has moved.
     * <P>
     * Note: calling the method {@code previous()} is not the same
     * as calling the method {@code relative(-1)}.  This is true
     * because it is possible to call {@code previous()} from the insert
     * row, from after the last row, or from the current row, whereas
     * {@code relative} may only be called from the current row.
     * <P>
     * The method {@code previous} may used in a {@code while}
     * loop to iterate through a rowset starting after the last row
     * and moving toward the beginning. The loop ends when {@code previous}
     * returns {@code false}, meaning that there are no more rows.
     * For example, the following code fragment retrieves all the data in
     * the {@code CachedRowSetImpl} object {@code crs}, which has
     * three columns.  Note that the cursor must initially be positioned
     * after the last row so that the first call to the method
     * {@code previous} places the cursor on the last line.
     * <PRE>{@code
     *
     *     crs.afterLast();
     *     while (previous()) {
     *         String name = crs.getString(1);
     *         int age = crs.getInt(2);
     *         short ssn = crs.getShort(3);
     *         System.out.println(name + "   " + age + "   " + ssn);
     *     }
     *
     * }</PRE>
     * This method throws an {@code SQLException} if the cursor is not
     * on a row in the rowset, before the first row, or after the last row.
     *
     * @return {@code true} if the cursor is on a valid row;
     *         {@code false} if it is before the first row or after the
     *         last row
     * @throws SQLException if the cursor is not on a valid position or the
     *           type of this rowset is {@code ResultSet.TYPE_FORWARD_ONLY}
     */
    public boolean previous() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves the cursor to the previous row in this {@code CachedRowSetImpl}
     * object, skipping past deleted rows that are not visible; returns
     * {@code true} if the cursor is on a row in this rowset and
     * {@code false} when the cursor goes before the first row.
     * <P>
     * This method is called internally by the method {@code previous}.
     * <P>
     * This is a implementation only method and is not required as a standard
     * implementation of the {@code CachedRowSet} interface.
     *
     * @return {@code true} if the cursor is on a row in this rowset;
     *         {@code false} when the cursor reaches the position before
     *         the first row
     * @throws SQLException if an error occurs
     */
    protected boolean internalPrevious() throws SQLException {
        throw new UnsupportedOperationException();
    }


    //---------------------------------------------------------------------
    // Updates
    //---------------------------------------------------------------------

    /**
     * Indicates whether the current row of this {@code CachedRowSetImpl}
     * object has been updated.  The value returned
     * depends on whether this rowset can detect updates: {@code false}
     * will always be returned if it does not detect updates.
     *
     * @return {@code true} if the row has been visibly updated
     *         by the owner or another and updates are detected;
     *         {@code false} otherwise
     * @throws SQLException if the cursor is on the insert row or not
     *            not on a valid row
     *
     * @see DatabaseMetaData#updatesAreDetected
     */
    public boolean rowUpdated() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates whether the designated column of the current row of
     * this {@code CachedRowSetImpl} object has been updated. The
     * value returned depends on whether this rowset can detcted updates:
     * {@code false} will always be returned if it does not detect updates.
     *
     * @param idx the index identifier of the column that may be have been updated.
     * @return {@code true} is the designated column has been updated
     * and the rowset detects updates; {@code false} if the rowset has not
     * been updated or the rowset does not detect updates
     * @throws SQLException if the cursor is on the insert row or not
     *          on a valid row
     * @see DatabaseMetaData#updatesAreDetected
     */
    public boolean columnUpdated(int idx) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates whether the designated column of the current row of
     * this {@code CachedRowSetImpl} object has been updated. The
     * value returned depends on whether this rowset can detcted updates:
     * {@code false} will always be returned if it does not detect updates.
     *
     * @param columnName the {@code String} column name column that may be have
     * been updated.
     * @return {@code true} is the designated column has been updated
     * and the rowset detects updates; {@code false} if the rowset has not
     * been updated or the rowset does not detect updates
     * @throws SQLException if the cursor is on the insert row or not
     *          on a valid row
     * @see DatabaseMetaData#updatesAreDetected
     */
    public boolean columnUpdated(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates whether the current row has been inserted.  The value returned
     * depends on whether or not the rowset can detect visible inserts.
     *
     * @return {@code true} if a row has been inserted and inserts are detected;
     *         {@code false} otherwise
     * @throws SQLException if the cursor is on the insert row or not
     *            not on a valid row
     *
     * @see DatabaseMetaData#insertsAreDetected
     */
    public boolean rowInserted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates whether the current row has been deleted.  A deleted row
     * may leave a visible "hole" in a rowset.  This method can be used to
     * detect such holes if the rowset can detect deletions. This method
     * will always return {@code false} if this rowset cannot detect
     * deletions.
     *
     * @return {@code true} if (1)the current row is blank, indicating that
     *         the row has been deleted, and (2)deletions are detected;
     *         {@code false} otherwise
     * @throws SQLException if the cursor is on a valid row in this rowset
     * @see DatabaseMetaData#deletesAreDetected
     */
    public boolean rowDeleted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated nullable column in the current row or the
     * insert row of this {@code CachedRowSetImpl} object with
     * {@code null} value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset; however, another method must be called to complete
     * the update process. If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to mark the row as updated
     * and to notify listeners that the row has changed.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called to insert the new row into this rowset and to notify
     * listeners that a row has changed.
     * <P>
     * In order to propagate updates in this rowset to the underlying
     * data source, an application must call the method {@link #acceptChanges}
     * after it calls either {@code updateRow} or {@code insertRow}.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateNull(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code boolean} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code byte} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code short} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code int} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateInt(int columnIndex, int x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code long} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateLong(int columnIndex, long x) throws SQLException {
       throw new UnsupportedOperationException();

    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code float} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code double} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.math.BigDecimal} object.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code String} object.
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
     * The method {@code acceptChanges} must be called if the
     * updated values are to be written back to the underlying database.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateString(int columnIndex, String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code byte} array.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBytes(int columnIndex, byte x[]) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Date} object.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the type of the designated column is not
     *            an SQL {@code DATE} or {@code TIMESTAMP}, or
     *            (4) this rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateDate(int columnIndex, java.sql.Date x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Time} object.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the type of the designated column is not
     *            an SQL {@code TIME} or {@code TIMESTAMP}, or
     *            (4) this rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateTime(int columnIndex, java.sql.Time x) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Timestamp} object.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the type of the designated column is not
     *            an SQL {@code DATE}, {@code TIME}, or
     *            {@code TIMESTAMP}, or (4) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateTimestamp(int columnIndex, java.sql.Timestamp x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @param length the number of one-byte ASCII characters in the stream
     * @throws SQLException if this method is invoked
     */
    public void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.io.InputStream} object.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value; must be a {@code java.io.InputStream}
     *          containing {@code BINARY}, {@code VARBINARY}, or
     *          {@code LONGVARBINARY} data
     * @param length the length of the stream in bytes
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the data in the stream is not binary, or
     *            (4) this rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBinaryStream(int columnIndex, java.io.InputStream x,int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.io.Reader} object.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value; must be a {@code java.io.Reader}
     *          containing {@code BINARY}, {@code VARBINARY},
     *          {@code LONGVARBINARY}, {@code CHAR}, {@code VARCHAR},
     *          or {@code LONGVARCHAR} data
     * @param length the length of the stream in characters
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, (3) the data in the stream is not a binary or
     *            character type, or (4) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateCharacterStream(int columnIndex, java.io.Reader x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Object} value.  The {@code scale} parameter indicates
     * the number of digits to the right of the decimal point and is ignored
     * if the new column value is not a type that will be mapped to an SQL
     * {@code DECIMAL} or {@code NUMERIC} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @param scale the number of digits to the right of the decimal point (for
     *              {@code DECIMAL} and {@code NUMERIC} types only)
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Object} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param x the new column value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw new UnsupportedOperationException();
    }


    /**
     * Sets the designated nullable column in the current row or the
     * insert row of this {@code CachedRowSetImpl} object with
     * {@code null} value.
     * <P>
     * This method updates a column value in the current row or the insert
     * row of this rowset, but it does not update the database.
     * If the cursor is on a row in the rowset, the
     * method {@link #updateRow} must be called to update the database.
     * If the cursor is on the insert row, the method {@link #insertRow}
     * must be called, which will insert the new row into both this rowset
     * and the database.
     *
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateNull(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code boolean} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBoolean(String columnName, boolean x) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code byte} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateByte(String columnName, byte x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code short} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateShort(String columnName, short x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code int} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateInt(String columnName, int x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code long} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateLong(String columnName, long x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code float} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateFloat(String columnName, float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code double} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateDouble(String columnName, double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.math.BigDecimal} object.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code String} object.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateString(String columnName, String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code byte} array.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBytes(String columnName, byte x[]) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Date} object.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the type
     *            of the designated column is not an SQL {@code DATE} or
     *            {@code TIMESTAMP}, or (4) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateDate(String columnName, java.sql.Date x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Time} object.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the type
     *            of the designated column is not an SQL {@code TIME} or
     *            {@code TIMESTAMP}, or (4) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateTime(String columnName, java.sql.Time x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Timestamp} object.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if the given column index is out of bounds or
     *            the cursor is not on one of this rowset's rows or its
     *            insert row
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the type
     *            of the designated column is not an SQL {@code DATE},
     *            {@code TIME}, or {@code TIMESTAMP}, or (4) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateTimestamp(String columnName, java.sql.Timestamp x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @param length the number of one-byte ASCII characters in the stream
     */
    public void updateAsciiStream(String columnName,
    java.io.InputStream x,
    int length) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.io.InputStream} object.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value; must be a {@code java.io.InputStream}
     *          containing {@code BINARY}, {@code VARBINARY}, or
     *          {@code LONGVARBINARY} data
     * @param length the length of the stream in bytes
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the data
     *            in the stream is not binary, or (4) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBinaryStream(String columnName, java.io.InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.io.Reader} object.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param reader the new column value; must be a
     * {@code java.io.Reader} containing {@code BINARY},
     * {@code VARBINARY}, {@code LONGVARBINARY}, {@code CHAR},
     * {@code VARCHAR}, or {@code LONGVARCHAR} data
     * @param length the length of the stream in characters
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, (3) the data
     *            in the stream is not a binary or character type, or (4) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateCharacterStream(String columnName,
    java.io.Reader reader,
    int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Object} value.  The {@code scale} parameter
     * indicates the number of digits to the right of the decimal point
     * and is ignored if the new column value is not a type that will be
     *  mapped to an SQL {@code DECIMAL} or {@code NUMERIC} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @param scale the number of digits to the right of the decimal point (for
     *              {@code DECIMAL} and {@code NUMERIC} types only)
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateObject(String columnName, Object x, int scale) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code Object} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param x the new column value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateObject(String columnName, Object x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Inserts the contents of this {@code CachedRowSetImpl} object's insert
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
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void insertRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Marks the current row of this {@code CachedRowSetImpl} object as
     * updated and notifies listeners registered with this rowset that the
     * row has changed.
     * <P>
     * This method  cannot be called when the cursor is on the insert row, and
     * it should be called before the cursor moves to another row.  If it is
     * called after the cursor moves to another row, this method has no effect,
     * and the updates made before the cursor moved will be lost.
     *
     * @throws SQLException if the cursor is on the insert row or this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes the current row from this {@code CachedRowSetImpl} object and
     * notifies listeners registered with this rowset that a row has changed.
     * This method cannot be called when the cursor is on the insert row.
     * <P>
     * This method marks the current row as deleted, but it does not delete
     * the row from the underlying data source.  The method
     * {@code acceptChanges} must be called to delete the row in
     * the data source.
     *
     * @throws SQLException if (1) this method is called when the cursor
     *            is on the insert row, before the first row, or after the
     *            last row or (2) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void deleteRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the current row with its original value and marks the row as
     * not updated, thus undoing any changes made to the row since the
     * last call to the methods {@code updateRow} or {@code deleteRow}.
     * This method should be called only when the cursor is on a row in
     * this rowset.
     *
     * @throws SQLException if the cursor is on the insert row, before the
     *            first row, or after the last row
     */
    public void refreshRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Rolls back any updates made to the current row of this
     * {@code CachedRowSetImpl} object and notifies listeners that
     * a row has changed.  To have an effect, this method
     * must be called after an {@code updateXXX} method has been
     * called and before the method {@code updateRow} has been called.
     * If no updates have been made or the method {@code updateRow}
     * has already been called, this method has no effect.
     *
     * @throws SQLException if the cursor is on the insert row, before the
     *            first row, or after the last row
     */
    public void cancelRowUpdates() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves the cursor for this {@code CachedRowSetImpl} object
     * to the insert row.  The current row in the rowset is remembered
     * while the cursor is on the insert row.
     * <P>
     * The insert row is a special row associated with an updatable
     * rowset.  It is essentially a buffer where a new row may
     * be constructed by calling the appropriate {@code updateXXX}
     * methods to assign a value to each column in the row.  A complete
     * row must be constructed; that is, every column that is not nullable
     * must be assigned a value.  In order for the new row to become part
     * of this rowset, the method {@code insertRow} must be called
     * before the cursor is moved back to the rowset.
     * <P>
     * Only certain methods may be invoked while the cursor is on the insert
     * row; many methods throw an exception if they are called while the
     * cursor is there.  In addition to the {@code updateXXX}
     * and {@code insertRow} methods, only the {@code getXXX} methods
     * may be called when the cursor is on the insert row.  A {@code getXXX}
     * method should be called on a column only after an {@code updateXXX}
     * method has been called on that column; otherwise, the value returned is
     * undetermined.
     *
     * @throws SQLException if this {@code CachedRowSetImpl} object is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void moveToInsertRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves the cursor for this {@code CachedRowSetImpl} object to
     * the current row.  The current row is the row the cursor was on
     * when the method {@code moveToInsertRow} was called.
     * <P>
     * Calling this method has no effect unless it is called while the
     * cursor is on the insert row.
     *
     * @throws SQLException if an error occurs
     */
    public void moveToCurrentRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code null}.
     *
     * @return {@code null}
     * @throws SQLException if an error occurs
     */
    public Statement getStatement() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as an {@code Object} in
     * the Java programming language, using the given
     * {@code java.util.Map} object to custom map the value if
     * appropriate.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param map a {@code java.util.Map} object showing the mapping
     *            from SQL type names to classes in the Java programming
     *            language
     * @return an {@code Object} representing the SQL value
     * @throws SQLException if the given column index is out of bounds or
     *            the cursor is not on one of this rowset's rows or its
     *            insert row
     */
    public Object getObject(int columnIndex,
                            java.util.Map<String,Class<?>> map)
          throws SQLException
    {
       throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as a {@code Ref} object
     * in the Java programming language.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @return a {@code Ref} object representing an SQL{@code  REF} value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) the designated column does not store an
     *            SQL {@code REF} value
     * @see #getRef(String)
     */
    public Ref getRef(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as a {@code Blob} object
     * in the Java programming language.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @return a {@code Blob} object representing an SQL {@code BLOB} value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) the designated column does not store an
     *            SQL {@code BLOB} value
     * @see #getBlob(String)
     */
    public Blob getBlob(int columnIndex) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as a {@code Clob} object
     * in the Java programming language.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @return a {@code Clob} object representing an SQL {@code CLOB} value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) the designated column does not store an
     *            SQL {@code CLOB} value
     * @see #getClob(String)
     */
    public Clob getClob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as an {@code Array} object
     * in the Java programming language.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @return an {@code Array} object representing an SQL
     *         {@code ARRAY} value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) the designated column does not store an
     *            SQL {@code ARRAY} value
     * @see #getArray(String)
     */
    public Array getArray(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as an {@code Object} in
     * the Java programming language, using the given
     * {@code java.util.Map} object to custom map the value if
     * appropriate.
     *
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param map a {@code java.util.Map} object showing the mapping
     *        from SQL type names to classes in the Java programming
     *        language
     * @return an {@code Object} representing the SQL value
     * @throws SQLException if the given column name is not the name of
     *         a column in this rowset or the cursor is not on one of
     *         this rowset's rows or its insert row
     */
    public Object getObject(String columnName,
                            java.util.Map<String,Class<?>> map)
    throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as a {@code Ref} object
     * in the Java programming language.
     *
     * @param colName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @return a {@code Ref} object representing an SQL{@code  REF} value
     * @throws SQLException  if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the column value
     *            is not an SQL {@code REF} value
     * @see #getRef(int)
     */
    public Ref getRef(String colName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as a {@code Blob} object
     * in the Java programming language.
     *
     * @param colName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @return a {@code Blob} object representing an SQL {@code BLOB} value
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code BLOB} value
     * @see #getBlob(int)
     */
    public Blob getBlob(String colName) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as a {@code Clob} object
     * in the Java programming language.
     *
     * @param colName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @return a {@code Clob} object representing an SQL
     *         {@code CLOB} value
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code CLOB} value
     * @see #getClob(int)
     */
    public Clob getClob(String colName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as an {@code Array} object
     * in the Java programming langugage.
     *
     * @param colName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @return an {@code Array} object representing an SQL
     *         {@code ARRAY} value
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code ARRAY} value
     * @see #getArray(int)
     */
    public Array getArray(String colName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a {@code java.sql.Date}
     * object, using the given {@code Calendar} object to construct an
     * appropriate millisecond value for the date.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @param cal the {@code java.util.Calendar} object to use in
     *            constructing the date
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code DATE} or
     *            {@code TIMESTAMP} value
     */
    public java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a {@code java.sql.Date}
     * object, using the given {@code Calendar} object to construct an
     * appropriate millisecond value for the date.
     *
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param cal the {@code java.util.Calendar} object to use in
     *            constructing the date
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code DATE} or
     *            {@code TIMESTAMP} value
     */
    public java.sql.Date getDate(String columnName, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a {@code java.sql.Time}
     * object, using the given {@code Calendar} object to construct an
     * appropriate millisecond value for the date.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @param cal the {@code java.util.Calendar} object to use in
     *            constructing the date
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code TIME} or
     *            {@code TIMESTAMP} value
     */
    public java.sql.Time getTime(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a {@code java.sql.Time}
     * object, using the given {@code Calendar} object to construct an
     * appropriate millisecond value for the date.
     *
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param cal the {@code java.util.Calendar} object to use in
     *            constructing the date
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code TIME} or
     *            {@code TIMESTAMP} value
     */
    public java.sql.Time getTime(String columnName, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a {@code java.sql.Timestamp}
     * object, using the given {@code Calendar} object to construct an
     * appropriate millisecond value for the date.
     *
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in the rowset
     * @param cal the {@code java.util.Calendar} object to use in
     *            constructing the date
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code TIME} or
     *            {@code TIMESTAMP} value
     */
    public java.sql.Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in the current row
     * of this {@code CachedRowSetImpl} object as a
     * {@code java.sql.Timestamp} object, using the given
     * {@code Calendar} object to construct an appropriate
     * millisecond value for the date.
     *
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param cal the {@code java.util.Calendar} object to use in
     *            constructing the date
     * @return the column value; if the value is SQL {@code NULL},
     *         the result is {@code null}
     * @throws SQLException if (1) the given column name is not the name of
     *            a column in this rowset, (2) the cursor is not on one of
     *            this rowset's rows or its insert row, or (3) the designated
     *            column does not store an SQL {@code DATE},
     *            {@code TIME}, or {@code TIMESTAMP} value
     */
    public java.sql.Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /*
     * RowSetInternal Interface
     */

    /**
     * Retrieves the {@code Connection} object passed to this
     * {@code CachedRowSetImpl} object.  This connection may be
     * used to populate this rowset with data or to write data back
     * to its underlying data source.
     *
     * @return the {@code Connection} object passed to this rowset;
     *         may be {@code null} if there is no connection
     * @throws SQLException if an error occurs
     */
    public Connection getConnection() throws SQLException{
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the metadata for this {@code CachedRowSetImpl} object
     * with the given {@code RowSetMetaData} object.
     *
     * @param md a {@code RowSetMetaData} object instance containing
     *            metadata about the columsn in the rowset
     * @throws SQLException if invalid meta data is supplied to the
     *            rowset
     */
    public void setMetaData(RowSetMetaData md) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a result set containing the original value of the rowset. The
     * original value is the state of the {@code CachedRowSetImpl} after the
     * last population or synchronization (whichever occurred most recently) with
     * the data source.
     * <p>
     * The cursor is positioned before the first row in the result set.
     * Only rows contained in the result set returned by {@code getOriginal()}
     * are said to have an original value.
     *
     * @return the original result set of the rowset
     * @throws SQLException if an error occurs produce the
     *           {@code ResultSet} object
     */
    public ResultSet getOriginal() throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Returns a result set containing the original value of the current
     * row only.
     * The original value is the state of the {@code CachedRowSetImpl} after
     * the last population or synchronization (whichever occurred most recently)
     * with the data source.
     *
     * @return the original result set of the row
     * @throws SQLException if there is no current row
     * @see #setOriginalRow
     */
    public ResultSet getOriginalRow() throws SQLException {
        throw new UnsupportedOperationException();

    }

    /**
     * Marks the current row in this rowset as being an original row.
     *
     * @throws SQLException if there is no current row
     * @see #getOriginalRow
     */
    public void setOriginalRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Marks all rows in this rowset as being original rows. Any updates
     * made to the rows become the original values for the rowset.
     * Calls to the method {@code setOriginal} connot be reversed.
     *
     * @throws SQLException if an error occurs
     */
    public void setOriginal() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an identifier for the object (table) that was used to create this
     * rowset.
     *
     * @return a {@code String} object that identifies the table from
     *         which this {@code CachedRowSetImpl} object was derived
     * @throws SQLException if an error occurs
     */
    public String getTableName() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the identifier for the table from which this rowset was derived
     * to the given table name.
     *
     * @param tabName a {@code String} object that identifies the
     *          table from which this {@code CachedRowSetImpl} object
     *          was derived
     * @throws SQLException if an error occurs
     */
    public void setTableName(String tabName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the columns that make a key to uniquely identify a
     * row in this {@code CachedRowSetImpl} object.
     *
     * @return an array of column numbers that constitutes a primary
     *           key for this rowset. This array should be empty
     *           if no column is representitive of a primary key
     * @throws SQLException if the rowset is empty or no columns
     *           are designated as primary keys
     * @see #setKeyColumns
     */
    public int[] getKeyColumns() throws SQLException {
        throw new UnsupportedOperationException();
    }


    /**
     * Sets this {@code CachedRowSetImpl} object's
     * {@code keyCols} field with the given array of column
     * numbers, which forms a key for uniquely identifying a row
     * in this rowset.
     *
     * @param keys an array of {@code int} indicating the
     *        columns that form a primary key for this
     *        {@code CachedRowSetImpl} object; every
     *        element in the array must be greater than
     *        {@code 0} and less than or equal to the number
     *        of columns in this rowset
     * @throws SQLException if any of the numbers in the
     *            given array is not valid for this rowset
     * @see #getKeyColumns
     */
    public void setKeyColumns(int [] keys) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code double} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param ref the new column {@code java.sql.Ref} value
     * @throws SQLException if (1) the given column index is out of bounds,
     *        (2) the cursor is not on one of this rowset's rows or its
     *        insert row, or (3) this rowset is
     *        {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateRef(int columnIndex, java.sql.Ref ref) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code double} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param ref the new column {@code java.sql.Ref} value
     * @throws SQLException if (1) the given column name does not match the
     *        name of a column in this rowset, (2) the cursor is not on
     *        one of this rowset's rows or its insert row, or (3) this
     *        rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateRef(String columnName, java.sql.Ref ref) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code double} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param c the new column {@code Clob} value
     * @throws SQLException if (1) the given column index is out of bounds,
     *        (2) the cursor is not on one of this rowset's rows or its
     *        insert row, or (3) this rowset is
     *        {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateClob(int columnIndex, Clob c) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code double} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param c the new column {@code Clob} value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateClob(String columnName, Clob c) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.sql.Blob} value.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param b the new column {@code Blob} value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBlob(int columnIndex, Blob b) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.sql.Blob } value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param b the new column {@code Blob} value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateBlob(String columnName, Blob b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.sql.Array} values.
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
     * @param columnIndex the first column is {@code 1}, the second
     *        is {@code 2}, and so on; must be {@code 1} or larger
     *        and equal to or less than the number of columns in this rowset
     * @param a the new column {@code Array} value
     * @throws SQLException if (1) the given column index is out of bounds,
     *            (2) the cursor is not on one of this rowset's rows or its
     *            insert row, or (3) this rowset is
     *            {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateArray(int columnIndex, Array a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated column in either the current row or the insert
     * row of this {@code CachedRowSetImpl} object with the given
     * {@code java.sql.Array} value.
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
     * @param columnName a {@code String} object that must match the
     *        SQL name of a column in this rowset, ignoring case
     * @param a the new column {@code Array} value
     * @throws SQLException if (1) the given column name does not match the
     *            name of a column in this rowset, (2) the cursor is not on
     *            one of this rowset's rows or its insert row, or (3) this
     *            rowset is {@code ResultSet.CONCUR_READ_ONLY}
     */
    public void updateArray(String columnName, Array a) throws SQLException {
        throw new UnsupportedOperationException();
    }


    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as a {@code java.net.URL} object
     * in the Java programming language.
     *
     * @return a java.net.URL object containing the resource reference described by
     * the URL
     * @throws SQLException if (1) the given column index is out of bounds,
     * (2) the cursor is not on one of this rowset's rows or its
     * insert row, or (3) the designated column does not store an
     * SQL {@code DATALINK} value.
     * @see #getURL(String)
     */
    public java.net.URL getURL(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the value of the designated column in this
     * {@code CachedRowSetImpl} object as a {@code java.net.URL} object
     * in the Java programming language.
     *
     * @return a java.net.URL object containing the resource reference described by
     * the URL
     * @throws SQLException if (1) the given column name not the name of a column
     * in this rowset, or
     * (2) the cursor is not on one of this rowset's rows or its
     * insert row, or (3) the designated column does not store an
     * SQL {@code DATALINK} value.
     * @see #getURL(int)
     */
    public java.net.URL getURL(String columnName) throws SQLException {
        throw new UnsupportedOperationException();

    }

    /**
     * The first warning reported by calls on this {@code CachedRowSetImpl}
     * object is returned. Subsequent {@code CachedRowSetImpl} warnings will
     * be chained to this {@code SQLWarning}. All {@code RowSetWarnings}
     * warnings are generated in the disconnected environment and remain a
     * seperate warning chain to that provided by the {@code getWarnings}
     * method.
     *
     * <P>The warning chain is automatically cleared each time a new
     * row is read.
     *
     * <P><B>Note:</B> This warning chain only covers warnings caused
     * by {@code CachedRowSet} (and their child interface)
     * methods. All {@code SQLWarnings} can be obtained using the
     * {@code getWarnings} method which tracks warnings generated
     * by the underlying JDBC driver.
     * @return the first SQLWarning or null
     *
     */
    public RowSetWarning getRowSetWarnings() {
        throw new UnsupportedOperationException();
    }

     /**
     * Commits all changes performed by the {@code acceptChanges()}
     * methods
     *
     * @see java.sql.Connection#commit
     */
    public void commit() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Rolls back all changes performed by the {@code acceptChanges()}
     * methods
     *
     * @see java.sql.Connection#rollback
     */
    public void rollback() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Rolls back all changes performed by the {@code acceptChanges()}
     * to the last {@code Savepoint} transaction marker.
     *
     * @see java.sql.Connection#rollback(Savepoint)
     */
    public void rollback(Savepoint s) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsets the designated parameter to the given int array.
     * This was set using {@code setMatchColumn}
     * as the column which will form the basis of the join.
     * <P>
     * The parameter value unset by this method should be same
     * as was set.
     *
     * @param columnIdxes the index into this rowset
     *        object's internal representation of parameter values
     * @throws SQLException if an error occurs or the
     *  parameter index is out of bounds or if the columnIdx is
     *  not the same as set using {@code setMatchColumn(int [])}
     */
    public void unsetMatchColumn(int[] columnIdxes) throws SQLException {
         throw new UnsupportedOperationException();
    }

   /**
     * Unsets the designated parameter to the given String array.
     * This was set using {@code setMatchColumn}
     * as the column which will form the basis of the join.
     * <P>
     * The parameter value unset by this method should be same
     * as was set.
     *
     * @param columnIdxes the index into this rowset
     *        object's internal representation of parameter values
     * @throws SQLException if an error occurs or the
     *  parameter index is out of bounds or if the columnName is
     *  not the same as set using {@code setMatchColumn(String [])}
     */
    public void unsetMatchColumn(String[] columnIdxes) throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the column name as {@code String} array
     * that was set using {@code setMatchColumn(String [])}
     * for this rowset.
     *
     * @return a {@code String} array object that contains the column names
     *         for the rowset which has this the match columns
     *
     * @throws SQLException if an error occurs or column name is not set
     */
    public String[] getMatchColumnNames() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the column id as {@code int} array that was set using
     * {@code setMatchColumn(int [])} for this rowset.
     *
     * @return an {@code int} array object that contains the column ids
     *         for the rowset which has this as the match columns.
     *
     * @throws SQLException if an error occurs or column index is not set
     */
    public int[] getMatchColumnIndexes() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated parameter to the given int array.
     * This forms the basis of the join for the
     * {@code JoinRowSet} as the column which will form the basis of the
     * join.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this rowset's
     * command when the method {@code getMatchColumnIndexes} is called.
     *
     * @param columnIdxes the indexes into this rowset
     *        object's internal representation of parameter values; the
     *        first parameter is 0, the second is 1, and so on; must be
     *        {@code 0} or greater
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     */
    public void setMatchColumn(int[] columnIdxes) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated parameter to the given String array.
     *  This forms the basis of the join for the
     * {@code JoinRowSet} as the column which will form the basis of the
     * join.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this rowset's
     * command when the method {@code getMatchColumn} is called.
     *
     * @param columnNames the name of the column into this rowset
     *        object's internal representation of parameter values
     * @throws SQLException if an error occurs or the
     *  parameter index is out of bounds
     */
    public void setMatchColumn(String[] columnNames) throws SQLException {
        throw new UnsupportedOperationException();
    }


    /**
     * Sets the designated parameter to the given {@code int}
     * object.  This forms the basis of the join for the
     * {@code JoinRowSet} as the column which will form the basis of the
     * join.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this rowset's
     * command when the method {@code getMatchColumn} is called.
     *
     * @param columnIdx the index into this rowset
     *        object's internal representation of parameter values; the
     *        first parameter is 0, the second is 1, and so on; must be
     *        {@code 0} or greater
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     */
    public void setMatchColumn(int columnIdx) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the designated parameter to the given {@code String}
     * object.  This forms the basis of the join for the
     * {@code JoinRowSet} as the column which will form the basis of the
     * join.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this rowset's
     * command when the method {@code getMatchColumn} is called.
     *
     * @param columnName the name of the column into this rowset
     *        object's internal representation of parameter values
     * @throws SQLException if an error occurs or the
     *  parameter index is out of bounds
     */
    public void setMatchColumn(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsets the designated parameter to the given {@code int}
     * object.  This was set using {@code setMatchColumn}
     * as the column which will form the basis of the join.
     * <P>
     * The parameter value unset by this method should be same
     * as was set.
     *
     * @param columnIdx the index into this rowset
     *        object's internal representation of parameter values
     * @throws SQLException if an error occurs or the
     *  parameter index is out of bounds or if the columnIdx is
     *  not the same as set using {@code setMatchColumn(int)}
     */
    public void unsetMatchColumn(int columnIdx) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsets the designated parameter to the given {@code String}
     * object.  This was set using {@code setMatchColumn}
     * as the column which will form the basis of the join.
     * <P>
     * The parameter value unset by this method should be same
     * as was set.
     *
     * @param columnName the index into this rowset
     *        object's internal representation of parameter values
     * @throws SQLException if an error occurs or the
     *  parameter index is out of bounds or if the columnName is
     *  not the same as set using {@code setMatchColumn(String)}
     */
    public void unsetMatchColumn(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Notifies registered listeners that a RowSet object in the given RowSetEvent
     * object has populated a number of additional rows. The {@code numRows} parameter
     * ensures that this event will only be fired every {@code numRow}.
     * <p>
     * The source of the event can be retrieved with the method event.getSource.
     *
     * @param event a {@code RowSetEvent} object that contains the
     *     {@code RowSet} object that is the source of the events
     * @param numRows when populating, the number of rows interval on which the
     *     {@code CachedRowSet} populated should fire; the default value
     *     is zero; cannot be less than {@code fetchSize} or zero
     */
    public void rowSetPopulated(RowSetEvent event, int numRows) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Populates this {@code CachedRowSet} object with data from
     * the given {@code ResultSet} object. While related to the {@code populate(ResultSet)}
     * method, an additional parameter is provided to allow starting position within
     * the {@code ResultSet} from where to populate the CachedRowSet
     * instance.
     *
     * This method is an alternative to the method {@code execute}
     * for filling the rowset with data.  The method {@code populate}
     * does not require that the properties needed by the method
     * {@code execute}, such as the {@code command} property,
     * be set. This is true because the method {@code populate}
     * is given the {@code ResultSet} object from
     * which to get data and thus does not need to use the properties
     * required for setting up a connection and executing this
     * {@code CachedRowSetImpl} object's command.
     * <P>
     * After populating this rowset with data, the method
     * {@code populate} sets the rowset's metadata and
     * then sends a {@code RowSetChangedEvent} object
     * to all registered listeners prior to returning.
     *
     * @param data the {@code ResultSet} object containing the data
     *             to be read into this {@code CachedRowSetImpl} object
     * @param start the integer specifing the position in the
     *        {@code ResultSet} object to popultate the
     *        {@code CachedRowSetImpl} object.
     * @throws SQLException if an error occurs; or the max row setting is
     *          violated while populating the RowSet.Also id the start position
     *          is negative.
     * @see #execute
     */
     public void populate(ResultSet data, int start) throws SQLException{
        throw new UnsupportedOperationException();

     }

    /**
     * The nextPage gets the next page, that is a {@code CachedRowSetImpl} object
     * containing the number of rows specified by page size.
     * @return boolean value true indicating whether there are more pages to come and
     *         false indicating that this is the last page.
     * @throws SQLException if an error occurs or this called before calling populate.
     */
     public boolean nextPage() throws SQLException {
         throw new UnsupportedOperationException();
     }

    /**
     * This is the setter function for setting the size of the page, which specifies
     * how many rows have to be retrived at a time.
     *
     * @param size which is the page size
     * @throws SQLException if size is less than zero or greater than max rows.
     */
     public void setPageSize (int size) throws SQLException {
        throw new UnsupportedOperationException();
     }

    /**
     * This is the getter function for the size of the page.
     *
     * @return an integer that is the page size.
     */
    public int getPageSize() {
        throw new UnsupportedOperationException();
    }


    /**
     * Retrieves the data present in the page prior to the page from where it is
     * called.
     * @return boolean value true if it retrieves the previous page, flase if it
     *         is on the first page.
     * @throws SQLException if it is called before populate is called or ResultSet
     *         is of type {@code ResultSet.TYPE_FORWARD_ONLY} or if an error
     *         occurs.
     */
    public boolean previousPage() throws SQLException {
       throw new UnsupportedOperationException();
    }

    /**
       * Updates the designated column with a character stream value, which will
       * have the specified number of bytes. The driver does the necessary conversion
       * from Java character format to the national character set in the database.
       * It is intended for use when updating NCHAR, NVARCHAR and LONGNVARCHAR columns.
       * The updater methods are used to update column values in the current row or
       * the insert row. The updater methods do not update the underlying database;
       * instead the updateRow or insertRow methods are called to update the database.
       *
       * @param columnIndex - the first column is 1, the second is 2, ...
       * @param x - the new column value
       * @param length - the length of the stream
       * @exception SQLException if a database access error occurs
       * @since 1.6
       */
       public void updateNCharacterStream(int columnIndex,
                            java.io.Reader x,
                            int length)
                            throws SQLException {
          throw new UnsupportedOperationException("Operation not yet supported");
       }

     /**
       * Updates the designated column with a character stream value, which will
       * have the specified number of bytes. The driver does the necessary conversion
       * from Java character format to the national character set in the database.
       * It is intended for use when updating NCHAR,NVARCHAR and LONGNVARCHAR columns.
       * The updater methods are used to update column values in the current row or
       * the insert row. The updater methods do not update the underlying database;
       * instead the updateRow or insertRow methods are called to update the database.
       *
       * @param columnName - name of the Column
       * @param x - the new column value
       * @param length - the length of the stream
       * @exception SQLException if a database access error occurs
       * @since 1.6
       */
       public void updateNCharacterStream(String columnName,
                            java.io.Reader x,
                            int length)
                            throws SQLException {
          throw new UnsupportedOperationException("Operation not yet supported");
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

       static final long serialVersionUID = -3345004441725080251L;
} //end class
