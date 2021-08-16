/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql.rowset;

import java.sql.SQLException;

/**
 * <h2>1.0 Background</h2>
 * The <code>Joinable</code> interface provides the methods for getting and
 * setting a match column, which is the basis for forming the SQL <code>JOIN</code>
 * formed by adding <code>RowSet</code> objects to a <code>JoinRowSet</code>
 * object.
 * <P>
 * Any standard <code>RowSet</code> implementation <b>may</b> implement
 * the <code>Joinable</code> interface in order to be
 * added to a <code>JoinRowSet</code> object. Implementing this interface gives
 * a <code>RowSet</code> object the ability to use <code>Joinable</code> methods,
 * which set, retrieve, and get information about match columns.  An
 * application may add a
 * <code>RowSet</code> object that has not implemented the <code>Joinable</code>
 * interface to a <code>JoinRowSet</code> object, but to do so it must use one
 * of the <code>JoinRowSet.addRowSet</code> methods that takes both a
 * <code>RowSet</code> object and a match column or an array of <code>RowSet</code>
 * objects and an array of match columns.
 * <P>
 * To get access to the methods in the <code>Joinable</code> interface, a
 * <code>RowSet</code> object implements at least one of the
 * five standard <code>RowSet</code> interfaces and also implements the
 * <code>Joinable</code> interface.  In addition, most <code>RowSet</code>
 * objects extend the <code>BaseRowSet</code> class.  For example:
 * <pre>
 *     class MyRowSetImpl extends BaseRowSet implements CachedRowSet, Joinable {
 *         :
 *         :
 *     }
 * </pre>
 *
 * <h2>2.0 Usage Guidelines</h2>
 * <P>
 * The methods in the <code>Joinable</code> interface allow a <code>RowSet</code> object
 * to set a match column, retrieve a match column, or unset a match column, which is
 * the column upon which an SQL <code>JOIN</code> can be based.
 * An instance of a class that implements these methods can be added to a
 * <code>JoinRowSet</code> object to allow an SQL <code>JOIN</code> relationship to
 *  be established.
 *
 * <pre>
 *     CachedRowSet crs = new MyRowSetImpl();
 *     crs.populate((ResultSet)rs);
 *     (Joinable)crs.setMatchColumnIndex(1);
 *
 *     JoinRowSet jrs = new JoinRowSetImpl();
 *     jrs.addRowSet(crs);
 * </pre>
 * In the previous example, <i>crs</i> is a <code>CachedRowSet</code> object that
 * has implemented the <code>Joinable</code> interface.  In the following example,
 * <i>crs2</i> has not, so it must supply the match column as an argument to the
 * <code>addRowSet</code> method. This example assumes that column 1 is the match
 * column.
 * <PRE>
 *     CachedRowSet crs2 = new MyRowSetImpl();
 *     crs2.populate((ResultSet)rs);
 *
 *     JoinRowSet jrs2 = new JoinRowSetImpl();
 *     jrs2.addRowSet(crs2, 1);
 * </PRE>
 * <p>
 * The <code>JoinRowSet</code> interface makes it possible to get data from one or
 * more <code>RowSet</code> objects consolidated into one table without having to incur
 * the expense of creating a connection to a database. It is therefore ideally suited
 * for use by disconnected <code>RowSet</code> objects. Nevertheless, any
 * <code>RowSet</code> object <b>may</b> implement this interface
 * regardless of whether it is connected or disconnected. Note that a
 * <code>JdbcRowSet</code> object, being always connected to its data source, can
 * become part of an SQL <code>JOIN</code> directly without having to become part
 * of a <code>JoinRowSet</code> object.
 *
 * <h2>3.0 Managing Multiple Match Columns</h2>
 * The index array passed into the <code>setMatchColumn</code> methods indicates
 * how many match columns are being set (the length of the array) in addition to
 * which columns will be used for the match. For example:
 * <pre>
 *     int[] i = {1, 2, 4, 7}; // indicates four match columns, with column
 *                             // indexes 1, 2, 4, 7 participating in the JOIN.
 *     Joinable.setMatchColumn(i);
 * </pre>
 * Subsequent match columns may be added as follows to a different <code>Joinable</code>
 * object (a <code>RowSet</code> object that has implemented the <code>Joinable</code>
 * interface).
 * <pre>
 *     int[] w = {3, 2, 5, 3};
 *     Joinable2.setMatchColumn(w);
 * </pre>
 * When an application adds two or more <code>RowSet</code> objects to a
 * <code>JoinRowSet</code> object, the order of the indexes in the array is
 * particularly important. Each index of
 * the array maps directly to the corresponding index of the previously added
 * <code>RowSet</code> object. If overlap or underlap occurs, the match column
 * data is maintained in the event an additional <code>Joinable</code> RowSet is
 * added and needs to relate to the match column data. Therefore, applications
 * can set multiple match columns in any order, but
 * this order has a direct effect on the outcome of the <code>SQL</code> JOIN.
 * <p>
 * This assertion applies in exactly the same manner when column names are used
 * rather than column indexes to indicate match columns.
 *
 * @see JoinRowSet
 * @author  Jonathan Bruce
 * @since 1.5
 */
public interface Joinable {

    /**
     * Sets the designated column as the match column for this <code>RowSet</code>
     * object. A <code>JoinRowSet</code> object can now add this <code>RowSet</code>
     * object based on the match column.
     * <p>
     * Sub-interfaces such as the <code>CachedRowSet</code>
     * interface define the method <code>CachedRowSet.setKeyColumns</code>, which allows
     * primary key semantics to be enforced on specific columns.
     * Implementations of the <code>setMatchColumn(int columnIdx)</code> method
     * should ensure that the constraints on the key columns are maintained when
     * a <code>CachedRowSet</code> object sets a primary key column as a match column.
     *
     * @param columnIdx an <code>int</code> identifying the index of the column to be
     *        set as the match column
     * @throws SQLException if an invalid column index is set
     * @see #setMatchColumn(int[])
     * @see #unsetMatchColumn(int)
     *
     */
    public void setMatchColumn(int columnIdx) throws SQLException;

    /**
     * Sets the designated columns as the match column for this <code>RowSet</code>
     * object. A <code>JoinRowSet</code> object can now add this <code>RowSet</code>
     * object based on the match column.
     *
     * @param columnIdxes an array of <code>int</code> identifying the indexes of the
     *      columns to be set as the match columns
     * @throws SQLException if an invalid column index is set
     * @see #setMatchColumn(int[])
     * @see #unsetMatchColumn(int[])
     */
    public void setMatchColumn(int[] columnIdxes) throws SQLException;

    /**
     * Sets the designated column as the match column for this <code>RowSet</code>
     * object. A <code>JoinRowSet</code> object can now add this <code>RowSet</code>
     * object based on the match column.
     * <p>
     * Subinterfaces such as the <code>CachedRowSet</code> interface define
     * the method <code>CachedRowSet.setKeyColumns</code>, which allows
     * primary key semantics to be enforced on specific columns.
     * Implementations of the <code>setMatchColumn(String columnIdx)</code> method
     * should ensure that the constraints on the key columns are maintained when
     * a <code>CachedRowSet</code> object sets a primary key column as a match column.
     *
     * @param columnName a <code>String</code> object giving the name of the column
     *      to be set as the match column
     * @throws SQLException if an invalid column name is set, the column name
     *      is a null, or the column name is an empty string
     * @see #unsetMatchColumn
     * @see #setMatchColumn(int[])
     */
    public void setMatchColumn(String columnName) throws SQLException;

    /**
     * Sets the designated columns as the match column for this <code>RowSet</code>
     * object. A <code>JoinRowSet</code> object can now add this <code>RowSet</code>
     * object based on the match column.
     *
     * @param columnNames an array of <code>String</code> objects giving the names
     *     of the column to be set as the match columns
     * @throws SQLException if an invalid column name is set, the column name
     *      is a null, or the column name is an empty string
     * @see #unsetMatchColumn
     * @see #setMatchColumn(int[])
     */
    public void setMatchColumn(String[] columnNames) throws SQLException;

    /**
     * Retrieves the indexes of the match columns that were set for this
     * <code>RowSet</code> object with the method
     * <code>setMatchColumn(int[] columnIdxes)</code>.
     *
     * @return an <code>int</code> array identifying the indexes of the columns
     *         that were set as the match columns for this <code>RowSet</code> object
     * @throws SQLException if no match column has been set
     * @see #setMatchColumn
     * @see #unsetMatchColumn
     */
    public int[] getMatchColumnIndexes() throws SQLException;

    /**
     * Retrieves the names of the match columns that were set for this
     * <code>RowSet</code> object with the method
     * <code>setMatchColumn(String [] columnNames)</code>.
     *
     * @return an array of <code>String</code> objects giving the names of the columns
     *         set as the match columns for this <code>RowSet</code> object
     * @throws SQLException if no match column has been set
     * @see #setMatchColumn
     * @see #unsetMatchColumn
     *
     */
    public String[] getMatchColumnNames() throws SQLException;

    /**
     * Unsets the designated column as the match column for this <code>RowSet</code>
     * object.
     * <P>
     * <code>RowSet</code> objects that implement the <code>Joinable</code> interface
     * must ensure that a key-like constraint continues to be enforced until the
     * method <code>CachedRowSet.unsetKeyColumns</code> has been called on the
     * designated column.
     *
     * @param columnIdx an <code>int</code> that identifies the index of the column
     *          that is to be unset as a match column
     * @throws SQLException if an invalid column index is designated or if
     *          the designated column was not previously set as a match
     *          column
     * @see #setMatchColumn
     */
    public void unsetMatchColumn(int columnIdx) throws SQLException;

    /**
     * Unsets the designated columns as the match column for this <code>RowSet</code>
     * object.
     *
     * @param columnIdxes an array of <code>int</code> that identifies the indexes
     *     of the columns that are to be unset as match columns
     * @throws SQLException if an invalid column index is designated or if
     *          the designated column was not previously set as a match
     *          column
     * @see #setMatchColumn
     */
    public void unsetMatchColumn(int[] columnIdxes) throws SQLException;

    /**
     * Unsets the designated column as the match column for this <code>RowSet</code>
     * object.
     * <P>
     * <code>RowSet</code> objects that implement the <code>Joinable</code> interface
     * must ensure that a key-like constraint continues to be enforced until the
     * method <code>CachedRowSet.unsetKeyColumns</code> has been called on the
     * designated column.
     *
     * @param columnName a <code>String</code> object giving the name of the column
     *          that is to be unset as a match column
     * @throws SQLException if an invalid column name is designated or
     *          the designated column was not previously set as a match
     *          column
     * @see #setMatchColumn
     */
    public void unsetMatchColumn(String columnName) throws SQLException;

    /**
     * Unsets the designated columns as the match columns for this <code>RowSet</code>
     * object.
     *
     * @param columnName an array of <code>String</code> objects giving the names of
     *     the columns that are to be unset as the match columns
     * @throws SQLException if an invalid column name is designated or the
     *     designated column was not previously set as a match column
     * @see #setMatchColumn
     */
    public void unsetMatchColumn(String[] columnName) throws SQLException;
}
