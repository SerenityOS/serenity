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

package javax.sql.rowset.spi;

import javax.sql.RowSet;
import java.sql.SQLException;

/**
 * Defines a framework that allows applications to use a manual decision tree
 * to decide what should be done when a synchronization conflict occurs.
 * Although it is not mandatory for
 * applications to resolve synchronization conflicts manually, this
 * framework provides the means to delegate to the application when conflicts
 * arise.
 * <p>
 * Note that a conflict is a situation where the <code>RowSet</code> object's original
 * values for a row do not match the values in the data source, which indicates that
 * the data source row has been modified since the last synchronization. Note also that
 * a <code>RowSet</code> object's original values are the values it had just prior to the
 * the last synchronization, which are not necessarily its initial values.
 *
 *
 * <H2>Description of a <code>SyncResolver</code> Object</H2>
 *
 * A <code>SyncResolver</code> object is a specialized <code>RowSet</code> object
 * that implements the <code>SyncResolver</code> interface.
 * It <b>may</b> operate as either a connected <code>RowSet</code> object (an
 * implementation of the <code>JdbcRowSet</code> interface) or a connected
 * <code>RowSet</code> object (an implementation of the
 * <code>CachedRowSet</code> interface or one of its subinterfaces). For information
 * on the subinterfaces, see the
 * <a href="../package-summary.html"><code>javax.sql.rowset</code></a> package
 * description. The reference implementation for <code>SyncResolver</code> implements
 * the <code>CachedRowSet</code> interface, but other implementations
 * may choose to implement the <code>JdbcRowSet</code> interface to satisfy
 * particular needs.
 * <P>
 * After an application has attempted to synchronize a <code>RowSet</code> object with
 * the data source (by calling the <code>CachedRowSet</code>
 * method <code>acceptChanges</code>), and one or more conflicts have been found,
 * a rowset's <code>SyncProvider</code> object creates an instance of
 * <code>SyncResolver</code>. This new <code>SyncResolver</code> object has
 * the same number of rows and columns as the
 * <code>RowSet</code> object that was attempting the synchronization. The
 * <code>SyncResolver</code> object contains the values from the data source that caused
 * the conflict(s) and <code>null</code> for all other values.
 * In addition, it contains information about each conflict.
 *
 *
 * <H2>Getting and Using a <code>SyncResolver</code> Object</H2>
 *
 * When the method <code>acceptChanges</code> encounters conflicts, the
 * <code>SyncProvider</code> object creates a <code>SyncProviderException</code>
 * object and sets it with the new <code>SyncResolver</code> object. The method
 * <code>acceptChanges</code> will throw this exception, which
 * the application can then catch and use to retrieve the
 * <code>SyncResolver</code> object it contains. The following code snippet uses the
 * <code>SyncProviderException</code> method <code>getSyncResolver</code> to get
 * the <code>SyncResolver</code> object <i>resolver</i>.
 * <PRE>
 * {@code
 *     } catch (SyncProviderException spe) {
 *         SyncResolver resolver = spe.getSyncResolver();
 *     ...
 *     }
 *
 * }
 * </PRE>
 * <P>
 * With <i>resolver</i> in hand, an application can use it to get the information
 * it contains about the conflict or conflicts.  A <code>SyncResolver</code> object
 * such as <i>resolver</i> keeps
 * track of the conflicts for each row in which there is a conflict.  It also places a
 * lock on the table or tables affected by the rowset's command so that no more
 * conflicts can occur while the current conflicts are being resolved.
 * <P>
 * The following kinds of information can be obtained from a <code>SyncResolver</code>
 * object:
 *
 *    <h3>What operation was being attempted when a conflict occurred</h3>
 * The <code>SyncProvider</code> interface defines four constants
 * describing states that may occur. Three
 * constants describe the type of operation (update, delete, or insert) that a
 * <code>RowSet</code> object was attempting to perform when a conflict was discovered,
 * and the fourth indicates that there is no conflict.
 * These constants are the possible return values when a <code>SyncResolver</code> object
 * calls the method <code>getStatus</code>.
 * <PRE>
 *     {@code int operation = resolver.getStatus(); }
 * </PRE>
 *
 *    <h3>The value in the data source that caused a conflict</h3>
 * A conflict exists when a value that a <code>RowSet</code> object has changed
 * and is attempting to write to the data source
 * has also been changed in the data source since the last synchronization.  An
 * application can call the <code>SyncResolver</code> method
 * <code>getConflictValue</code > to retrieve the
 * value in the data source that is the cause of the conflict because the values in a
 * <code>SyncResolver</code> object are the conflict values from the data source.
 * <PRE>
 *     java.lang.Object conflictValue = resolver.getConflictValue(2);
 * </PRE>
 * Note that the column in <i>resolver</i> can be designated by the column number,
 * as is done in the preceding line of code, or by the column name.
 * <P>
 * With the information retrieved from the methods <code>getStatus</code> and
 * <code>getConflictValue</code>, the application may make a determination as to
 * which value should be persisted in the data source. The application then calls the
 * <code>SyncResolver</code> method <code>setResolvedValue</code>, which sets the value
 * to be persisted in the <code>RowSet</code> object and also in the data source.
 * <PRE>
 *     resolver.setResolvedValue("DEPT", 8390426);
 * </PRE>
 * In the preceding line of code,
 * the column name designates the column in the <code>RowSet</code> object
 * that is to be set with the given value. The column number can also be used to
 * designate the column.
 * <P>
 * An application calls the method <code>setResolvedValue</code> after it has
 * resolved all of the conflicts in the current conflict row and repeats this process
 * for each conflict row in the <code>SyncResolver</code> object.
 *
 *
 * <H2>Navigating a <code>SyncResolver</code> Object</H2>
 *
 * Because a <code>SyncResolver</code> object is a <code>RowSet</code> object, an
 * application can use all of the <code>RowSet</code> methods for moving the cursor
 * to navigate a <code>SyncResolver</code> object. For example, an application can
 * use the <code>RowSet</code> method <code>next</code> to get to each row and then
 * call the <code>SyncResolver</code> method <code>getStatus</code> to see if the row
 * contains a conflict.  In a row with one or more conflicts, the application can
 * iterate through the columns to find any non-null values, which will be the values
 * from the data source that are in conflict.
 * <P>
 * To make it easier to navigate a <code>SyncResolver</code> object, especially when
 * there are large numbers of rows with no conflicts, the <code>SyncResolver</code>
 * interface defines the methods <code>nextConflict</code> and
 * <code>previousConflict</code>, which move only to rows
 * that contain at least one conflict value. Then an application can call the
 * <code>SyncResolver</code> method <code>getConflictValue</code>, supplying it
 * with the column number, to get the conflict value itself. The code fragment in the
 * next section gives an example.
 *
 * <H2>Code Example</H2>
 *
 * The following code fragment demonstrates how a disconnected <code>RowSet</code>
 * object <i>crs</i> might attempt to synchronize itself with the
 * underlying data source and then resolve the conflicts. In the <code>try</code>
 * block, <i>crs</i> calls the method <code>acceptChanges</code>, passing it the
 * <code>Connection</code> object <i>con</i>.  If there are no conflicts, the
 * changes in <i>crs</i> are simply written to the data source.  However, if there
 * is a conflict, the method <code>acceptChanges</code> throws a
 * <code>SyncProviderException</code> object, and the
 * <code>catch</code> block takes effect.  In this example, which
 * illustrates one of the many ways a <code>SyncResolver</code> object can be used,
 * the <code>SyncResolver</code> method <code>nextConflict</code> is used in a
 * <code>while</code> loop. The loop will end when <code>nextConflict</code> returns
 * <code>false</code>, which will occur when there are no more conflict rows in the
 * <code>SyncResolver</code> object <i>resolver</i>. In This particular code fragment,
 * <i>resolver</i> looks for rows that have update conflicts (rows with the status
 * <code>SyncResolver.UPDATE_ROW_CONFLICT</code>), and the rest of this code fragment
 * executes only for rows where conflicts occurred because <i>crs</i> was attempting an
 * update.
 * <P>
 * After the cursor for <i>resolver</i> has moved to the next conflict row that
 * has an update conflict, the method <code>getRow</code> indicates the number of the
 * current row, and
 * the cursor for the <code>CachedRowSet</code> object <i>crs</i> is moved to
 * the comparable row in <i>crs</i>. By iterating
 * through the columns of that row in both <i>resolver</i> and <i>crs</i>, the conflicting
 * values can be retrieved and compared to decide which one should be persisted. In this
 * code fragment, the value in <i>crs</i> is the one set as the resolved value, which means
 * that it will be used to overwrite the conflict value in the data source.
 *
 * <PRE>
 * {@code
 *     try {
 *
 *         crs.acceptChanges(con);
 *
 *     } catch (SyncProviderException spe) {
 *
 *         SyncResolver resolver = spe.getSyncResolver();
 *
 *         Object crsValue;  // value in the RowSet object
 *         Object resolverValue:  // value in the SyncResolver object
 *         Object resolvedValue:  // value to be persisted
 *
 *         while(resolver.nextConflict())  {
 *             if(resolver.getStatus() == SyncResolver.UPDATE_ROW_CONFLICT)  {
 *                 int row = resolver.getRow();
 *                 crs.absolute(row);
 *
 *                 int colCount = crs.getMetaData().getColumnCount();
 *                 for(int j = 1; j <= colCount; j++) {
 *                     if (resolver.getConflictValue(j) != null)  {
 *                         crsValue = crs.getObject(j);
 *                         resolverValue = resolver.getConflictValue(j);
 *                         . . .
 *                         // compare crsValue and resolverValue to determine
 *                         // which should be the resolved value (the value to persist)
 *                         resolvedValue = crsValue;
 *
 *                         resolver.setResolvedValue(j, resolvedValue);
 *                      }
 *                  }
 *              }
 *          }
 *      }
 * }</PRE>
 *
 * @author  Jonathan Bruce
 * @since 1.5
 */

public interface SyncResolver extends RowSet {
    /**
     * Indicates that a conflict occurred while the <code>RowSet</code> object was
     * attempting to update a row in the data source.
     * The values in the data source row to be updated differ from the
     * <code>RowSet</code> object's original values for that row, which means that
     * the row in the data source has been updated or deleted since the last
     * synchronization.
     */
     public static int UPDATE_ROW_CONFLICT = 0;

    /**
     * Indicates that a conflict occurred while the <code>RowSet</code> object was
     * attempting to delete a row in the data source.
     * The values in the data source row to be updated differ from the
     * <code>RowSet</code> object's original values for that row, which means that
     * the row in the data source has been updated or deleted since the last
     * synchronization.
     */
    public static int DELETE_ROW_CONFLICT = 1;

   /**
    * Indicates that a conflict occurred while the <code>RowSet</code> object was
    * attempting to insert a row into the data source.  This means that a
    * row with the same primary key as the row to be inserted has been inserted
    * into the data source since the last synchronization.
    */
    public static int INSERT_ROW_CONFLICT = 2;

    /**
     * Indicates that <b>no</b> conflict occurred while the <code>RowSet</code> object
     * was attempting to update, delete or insert a row in the data source. The values in
     * the <code>SyncResolver</code> will contain <code>null</code> values only as an indication
     * that no information in pertinent to the conflict resolution in this row.
     */
    public static int NO_ROW_CONFLICT = 3;

    /**
     * Retrieves the conflict status of the current row of this <code>SyncResolver</code>,
     * which indicates the operation
     * the <code>RowSet</code> object was attempting when the conflict occurred.
     *
     * @return one of the following constants:
     *         <code>SyncResolver.UPDATE_ROW_CONFLICT</code>,
     *         <code>SyncResolver.DELETE_ROW_CONFLICT</code>,
     *         <code>SyncResolver.INSERT_ROW_CONFLICT</code>, or
     *         <code>SyncResolver.NO_ROW_CONFLICT</code>
     */
    public int getStatus();

    /**
     * Retrieves the value in the designated column in the current row of this
     * <code>SyncResolver</code> object, which is the value in the data source
     * that caused a conflict.
     *
     * @param index an <code>int</code> designating the column in this row of this
     *        <code>SyncResolver</code> object from which to retrieve the value
     *        causing a conflict
     * @return the value of the designated column in the current row of this
     *         <code>SyncResolver</code> object
     * @throws SQLException if a database access error occurs
     */
    public Object getConflictValue(int index) throws SQLException;

    /**
     * Retrieves the value in the designated column in the current row of this
     * <code>SyncResolver</code> object, which is the value in the data source
     * that caused a conflict.
     *
     * @param columnName a <code>String</code> object designating the column in this row of this
     *        <code>SyncResolver</code> object from which to retrieve the value
     *        causing a conflict
     * @return the value of the designated column in the current row of this
     *         <code>SyncResolver</code> object
     * @throws SQLException if a database access error occurs
     */
    public Object getConflictValue(String columnName) throws SQLException;

    /**
     * Sets <i>obj</i> as the value in column <i>index</i> in the current row of the
     * <code>RowSet</code> object that is being synchronized. <i>obj</i>
     * is set as the value in the data source internally.
     *
     * @param index an <code>int</code> giving the number of the column into which to
     *        set the value to be persisted
     * @param obj an <code>Object</code> that is the value to be set in the
     *        <code>RowSet</code> object and persisted in the data source
     * @throws SQLException if a database access error occurs
     */
    public void setResolvedValue(int index, Object obj) throws SQLException;

    /**
     * Sets <i>obj</i> as the value in column <i>columnName</i> in the current row of the
     * <code>RowSet</code> object that is being synchronized. <i>obj</i>
     * is set as the value in the data source internally.
     *
     * @param columnName a <code>String</code> object giving the name of the column
     *        into which to set the value to be persisted
     * @param obj an <code>Object</code> that is the value to be set in the
     *        <code>RowSet</code> object and persisted in the data source
     * @throws SQLException if a database access error occurs
     */
    public void setResolvedValue(String columnName, Object obj) throws SQLException;

    /**
     * Moves the cursor down from its current position to the next row that contains
     * a conflict value. A <code>SyncResolver</code> object's
     * cursor is initially positioned before the first conflict row; the first call to the
     * method <code>nextConflict</code> makes the first conflict row the current row;
     * the second call makes the second conflict row the current row, and so on.
     * <p>
     * A call to the method <code>nextConflict</code> will implicitly close
     * an input stream if one is open and will clear the <code>SyncResolver</code>
     * object's warning chain.
     *
     * @return <code>true</code> if the new current row is valid; <code>false</code>
     *         if there are no more rows
     * @throws SQLException if a database access error occurs or the result set type
     *     is <code>TYPE_FORWARD_ONLY</code>
     *
     */
    public boolean nextConflict() throws SQLException;

    /**
     * Moves the cursor up from its current position to the previous conflict
     * row in this <code>SyncResolver</code> object.
     * <p>
     * A call to the method <code>previousConflict</code> will implicitly close
     * an input stream if one is open and will clear the <code>SyncResolver</code>
     * object's warning chain.
     *
     * @return <code>true</code> if the cursor is on a valid row; <code>false</code>
     *     if it is off the result set
     * @throws SQLException if a database access error occurs or the result set type
     *     is <code>TYPE_FORWARD_ONLY</code>
     */
    public boolean previousConflict() throws SQLException;

}
