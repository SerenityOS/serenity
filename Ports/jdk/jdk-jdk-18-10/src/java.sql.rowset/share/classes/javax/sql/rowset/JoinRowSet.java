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

import java.sql.*;
import javax.sql.*;
import javax.naming.*;
import java.io.*;
import java.math.*;
import java.util.*;

import javax.sql.rowset.*;

/**
 * The <code>JoinRowSet</code> interface provides a mechanism for combining related
 * data from different <code>RowSet</code> objects into one <code>JoinRowSet</code>
 * object, which represents an SQL <code>JOIN</code>.
 * In other words, a <code>JoinRowSet</code> object acts as a
 * container for the data from <code>RowSet</code> objects that form an SQL
 * <code>JOIN</code> relationship.
 * <P>
 * The <code>Joinable</code> interface provides the methods for setting,
 * retrieving, and unsetting a match column, the basis for
 * establishing an SQL <code>JOIN</code> relationship. The match column may
 * alternatively be set by supplying it to the appropriate version of the
 * <code>JointRowSet</code> method <code>addRowSet</code>.
 *
 * <h2>1.0 Overview</h2>
 * Disconnected <code>RowSet</code> objects (<code>CachedRowSet</code> objects
 * and implementations extending the <code>CachedRowSet</code> interface)
 * do not have a standard way to establish an SQL <code>JOIN</code> between
 * <code>RowSet</code> objects without the expensive operation of
 * reconnecting to the data source. The <code>JoinRowSet</code>
 * interface is specifically designed to address this need.
 * <P>
 * Any <code>RowSet</code> object
 * can be added to a <code>JoinRowSet</code> object to become
 * part of an SQL <code>JOIN</code> relationship. This means that both connected
 * and disconnected <code>RowSet</code> objects can be part of a <code>JOIN</code>.
 * <code>RowSet</code> objects operating in a connected environment
 * (<code>JdbcRowSet</code> objects) are
 * encouraged to use the database to which they are already
 * connected to establish SQL <code>JOIN</code> relationships between
 * tables directly. However, it is possible for a
 * <code>JdbcRowSet</code> object to be added to a <code>JoinRowSet</code> object
 * if necessary.
 * <P>
 * Any number of <code>RowSet</code> objects can be added to an
 * instance of <code>JoinRowSet</code> provided that they
 * can be related in an SQL <code>JOIN</code>.
 * By definition, the SQL <code>JOIN</code> statement is used to
 * combine the data contained in two or more relational database tables based
 * upon a common attribute. The <code>Joinable</code> interface provides the methods
 * for establishing a common attribute, which is done by setting a
 * <i>match column</i>. The match column commonly coincides with
 * the primary key, but there is
 * no requirement that the match column be the same as the primary key.
 * By establishing and then enforcing column matches,
 * a <code>JoinRowSet</code> object establishes <code>JOIN</code> relationships
 * between <code>RowSet</code> objects without the assistance of an available
 * relational database.
 * <P>
 * The type of <code>JOIN</code> to be established is determined by setting
 * one of the <code>JoinRowSet</code> constants using the method
 * <code>setJoinType</code>. The following SQL <code>JOIN</code> types can be set:
 * <UL>
 *  <LI><code>CROSS_JOIN</code>
 *  <LI><code>FULL_JOIN</code>
 *  <LI><code>INNER_JOIN</code> - the default if no <code>JOIN</code> type has been set
 *  <LI><code>LEFT_OUTER_JOIN</code>
 *  <LI><code>RIGHT_OUTER_JOIN</code>
 * </UL>
 * Note that if no type is set, the <code>JOIN</code> will automatically be an
 * inner join. The comments for the fields in the
 * <code>JoinRowSet</code> interface explain these <code>JOIN</code> types, which are
 * standard SQL <code>JOIN</code> types.
 *
 * <h2>2.0 Using a <code>JoinRowSet</code> Object for Creating a <code>JOIN</code></h2>
 * When a <code>JoinRowSet</code> object is created, it is empty.
 * The first <code>RowSet</code> object to be added becomes the basis for the
 * <code>JOIN</code> relationship.
 * Applications must determine which column in each of the
 * <code>RowSet</code> objects to be added to the <code>JoinRowSet</code> object
 * should be the match column. All of the
 * <code>RowSet</code> objects must contain a match column, and the values in
 * each match column must be ones that can be compared to values in the other match
 * columns. The columns do not have to have the same name, though they often do,
 * and they do not have to store the exact same data type as long as the data types
 * can be compared.
 * <P>
 * A match column can be set in two ways:
 * <ul>
 *  <li>By calling the <code>Joinable</code> method <code>setMatchColumn</code><br>
 *  This is the only method that can set the match column before a <code>RowSet</code>
 *  object is added to a <code>JoinRowSet</code> object. The <code>RowSet</code> object
 *  must have implemented the <code>Joinable</code> interface in order to use the method
 *  <code>setMatchColumn</code>. Once the match column value
 *  has been set, this method can be used to reset the match column at any time.
 *  <li>By calling one of the versions of the <code>JoinRowSet</code> method
 *  <code>addRowSet</code> that takes a column name or number (or an array of
 *  column names or numbers)<BR>
 *  Four of the five <code>addRowSet</code> methods take a match column as a parameter.
 *  These four methods set or reset the match column at the time a <code>RowSet</code>
 *  object is being added to a <code>JoinRowSet</code> object.
 * </ul>
 * <h2>3.0 Sample Usage</h2>
 * <p>
 * The following code fragment adds two <code>CachedRowSet</code>
 * objects to a <code>JoinRowSet</code> object. Note that in this example,
 * no SQL <code>JOIN</code> type is set, so the default <code>JOIN</code> type,
 * which is <i>INNER_JOIN</i>, is established.
 * <p>
 * In the following code fragment, the table <code>EMPLOYEES</code>, whose match
 * column is set to the first column (<code>EMP_ID</code>), is added to the
 * <code>JoinRowSet</code> object <i>jrs</i>. Then
 * the table <code>ESSP_BONUS_PLAN</code>, whose match column is likewise
 * the <code>EMP_ID</code> column, is added. When this second
 * table is added to <i>jrs</i>, only the rows in
 * <code>ESSP_BONUS_PLAN</code> whose <code>EMP_ID</code> value matches an
 * <code>EMP_ID</code> value in the <code>EMPLOYEES</code> table are added.
 * In this case, everyone in the bonus plan is an employee, so all of the rows
 * in the table <code>ESSP_BONUS_PLAN</code> are added to the <code>JoinRowSet</code>
 * object.  In this example, both <code>CachedRowSet</code> objects being added
 * have implemented the <code>Joinable</code> interface and can therefore call
 * the <code>Joinable</code> method <code>setMatchColumn</code>.
 * <PRE>
 *     JoinRowSet jrs = new JoinRowSetImpl();
 *
 *     ResultSet rs1 = stmt.executeQuery("SELECT * FROM EMPLOYEES");
 *     CachedRowSet empl = new CachedRowSetImpl();
 *     empl.populate(rs1);
 *     empl.setMatchColumn(1);
 *     jrs.addRowSet(empl);
 *
 *     ResultSet rs2 = stmt.executeQuery("SELECT * FROM ESSP_BONUS_PLAN");
 *     CachedRowSet bonus = new CachedRowSetImpl();
 *     bonus.populate(rs2);
 *     bonus.setMatchColumn(1); // EMP_ID is the first column
 *     jrs.addRowSet(bonus);
 * </PRE>
 * <P>
 * At this point, <i>jrs</i> is an inside JOIN of the two <code>RowSet</code> objects
 * based on their <code>EMP_ID</code> columns. The application can now browse the
 * combined data as if it were browsing one single <code>RowSet</code> object.
 * Because <i>jrs</i> is itself a <code>RowSet</code> object, an application can
 * navigate or modify it using <code>RowSet</code> methods.
 * <PRE>
 *     jrs.first();
 *     int employeeID = jrs.getInt(1);
 *     String employeeName = jrs.getString(2);
 * </PRE>
 * <P>
 * Note that because the SQL <code>JOIN</code> must be enforced when an application
 * adds a second or subsequent <code>RowSet</code> object, there
 * may be an initial degradation in performance while the <code>JOIN</code> is
 * being performed.
 * <P>
 * The following code fragment adds an additional <code>CachedRowSet</code> object.
 * In this case, the match column (<code>EMP_ID</code>) is set when the
 * <code>CachedRowSet</code> object is added to the <code>JoinRowSet</code> object.
 * <PRE>
 *     ResultSet rs3 = stmt.executeQuery("SELECT * FROM 401K_CONTRIB");
 *     CachedRowSet fourO1k = new CachedRowSetImpl();
 *     four01k.populate(rs3);
 *     jrs.addRowSet(four01k, 1);
 * </PRE>
 * <P>
 * The <code>JoinRowSet</code> object <i>jrs</i> now contains values from all three
 * tables. The data in each row in <i>four01k</i> in which the value for the
 * <code>EMP_ID</code> column matches a value for the <code>EMP_ID</code> column
 * in <i>jrs</i> has been added to <i>jrs</i>.
 *
 * <h2>4.0 <code>JoinRowSet</code> Methods</h2>
 * The <code>JoinRowSet</code> interface supplies several methods for adding
 * <code>RowSet</code> objects and for getting information about the
 * <code>JoinRowSet</code> object.
 * <UL>
 *   <LI>Methods for adding one or more <code>RowSet</code> objects<BR>
 *       These methods allow an application to add one <code>RowSet</code> object
 *       at a time or to add multiple <code>RowSet</code> objects at one time. In
 *       either case, the methods may specify the match column for each
 *       <code>RowSet</code> object being added.
 *   <LI>Methods for getting information<BR>
 *       One method retrieves the <code>RowSet</code> objects in the
 *       <code>JoinRowSet</code> object, and another method retrieves the
 *       <code>RowSet</code> names.  A third method retrieves either the SQL
 *       <code>WHERE</code> clause used behind the scenes to form the
 *       <code>JOIN</code> or a text description of what the <code>WHERE</code>
 *       clause does.
 *   <LI>Methods related to the type of <code>JOIN</code><BR>
 *       One method sets the <code>JOIN</code> type, and five methods find out whether
 *       the <code>JoinRowSet</code> object supports a given type.
 *   <LI>A method to make a separate copy of the <code>JoinRowSet</code> object<BR>
 *       This method creates a copy that can be persisted to the data source.
 * </UL>
 *
 * @since 1.5
 */

public interface JoinRowSet extends WebRowSet {

    /**
     * Adds the given <code>RowSet</code> object to this <code>JoinRowSet</code>
     * object. If the <code>RowSet</code> object
     * is the first to be added to this <code>JoinRowSet</code>
     * object, it forms the basis of the <code>JOIN</code> relationship to be
     * established.
     * <P>
     * This method should be used only when the given <code>RowSet</code>
     * object already has a match column that was set with the <code>Joinable</code>
     * method <code>setMatchColumn</code>.
     * <p>
     * Note: A <code>Joinable</code> object is any <code>RowSet</code> object
     * that has implemented the <code>Joinable</code> interface.
     *
     * @param rowset the <code>RowSet</code> object that is to be added to this
     *        <code>JoinRowSet</code> object; it must implement the
     *        <code>Joinable</code> interface and have a match column set
     * @throws SQLException if (1) an empty rowset is added to the to this
     *         <code>JoinRowSet</code> object, (2) a match column has not been
     *         set for <i>rowset</i>, or (3) <i>rowset</i>
     *         violates the active <code>JOIN</code>
     * @see Joinable#setMatchColumn
     */
    public void addRowSet(Joinable rowset) throws SQLException;

    /**
     * Adds the given <code>RowSet</code> object to this <code>JoinRowSet</code>
     * object and sets the designated column as the match column for
     * the <code>RowSet</code> object. If the <code>RowSet</code> object
     * is the first to be added to this <code>JoinRowSet</code>
     * object, it forms the basis of the <code>JOIN</code> relationship to be
     * established.
     * <P>
     * This method should be used when <i>RowSet</i> does not already have a match
     * column set.
     *
     * @param rowset the <code>RowSet</code> object that is to be added to this
     *        <code>JoinRowSet</code> object; it may implement the
     *        <code>Joinable</code> interface
     * @param columnIdx an <code>int</code> that identifies the column to become the
     *         match column
     * @throws SQLException if (1) <i>rowset</i> is an empty rowset or
     *         (2) <i>rowset</i> violates the active <code>JOIN</code>
     * @see Joinable#unsetMatchColumn
     */
    public void addRowSet(RowSet rowset, int columnIdx) throws SQLException;

    /**
     * Adds <i>rowset</i> to this <code>JoinRowSet</code> object and
     * sets the designated column as the match column. If <i>rowset</i>
     * is the first to be added to this <code>JoinRowSet</code>
     * object, it forms the basis for the <code>JOIN</code> relationship to be
     * established.
     * <P>
     * This method should be used when the given <code>RowSet</code> object
     * does not already have a match column.
     *
     * @param rowset the <code>RowSet</code> object that is to be added to this
     *        <code>JoinRowSet</code> object; it may implement the
     *        <code>Joinable</code> interface
     * @param columnName the <code>String</code> object giving the name of the
     *        column to be set as the match column
     * @throws SQLException if (1) <i>rowset</i> is an empty rowset or
     *         (2) the match column for <i>rowset</i> does not satisfy the
     *         conditions of the <code>JOIN</code>
     */
     public void addRowSet(RowSet rowset,
                           String columnName) throws SQLException;

    /**
     * Adds one or more <code>RowSet</code> objects contained in the given
     * array of <code>RowSet</code> objects to this <code>JoinRowSet</code>
     * object and sets the match column for
     * each of the <code>RowSet</code> objects to the match columns
     * in the given array of column indexes. The first element in
     * <i>columnIdx</i> is set as the match column for the first
     * <code>RowSet</code> object in <i>rowset</i>, the second element of
     * <i>columnIdx</i> is set as the match column for the second element
     * in <i>rowset</i>, and so on.
     * <P>
     * The first <code>RowSet</code> object added to this <code>JoinRowSet</code>
     * object forms the basis for the <code>JOIN</code> relationship.
     * <P>
     * This method should be used when the given <code>RowSet</code> object
     * does not already have a match column.
     *
     * @param rowset an array of one or more <code>RowSet</code> objects
     *        to be added to the <code>JOIN</code>; it may implement the
     *        <code>Joinable</code> interface
     * @param columnIdx an array of <code>int</code> values indicating the index(es)
     *        of the columns to be set as the match columns for the <code>RowSet</code>
     *        objects in <i>rowset</i>
     * @throws SQLException if (1) an empty rowset is added to this
     *         <code>JoinRowSet</code> object, (2) a match column is not set
     *         for a <code>RowSet</code> object in <i>rowset</i>, or (3)
     *         a <code>RowSet</code> object being added violates the active
     *         <code>JOIN</code>
     */
    public void addRowSet(RowSet[] rowset,
                          int[] columnIdx) throws SQLException;

    /**
     * Adds one or more <code>RowSet</code> objects contained in the given
     * array of <code>RowSet</code> objects to this <code>JoinRowSet</code>
     * object and sets the match column for
     * each of the <code>RowSet</code> objects to the match columns
     * in the given array of column names. The first element in
     * <i>columnName</i> is set as the match column for the first
     * <code>RowSet</code> object in <i>rowset</i>, the second element of
     * <i>columnName</i> is set as the match column for the second element
     * in <i>rowset</i>, and so on.
     * <P>
     * The first <code>RowSet</code> object added to this <code>JoinRowSet</code>
     * object forms the basis for the <code>JOIN</code> relationship.
     * <P>
     * This method should be used when the given <code>RowSet</code> object(s)
     * does not already have a match column.
     *
     * @param rowset an array of one or more <code>RowSet</code> objects
     *        to be added to the <code>JOIN</code>; it may implement the
     *        <code>Joinable</code> interface
     * @param columnName an array of <code>String</code> values indicating the
     *        names of the columns to be set as the match columns for the
     *        <code>RowSet</code> objects in <i>rowset</i>
     * @throws SQLException if (1) an empty rowset is added to this
     *         <code>JoinRowSet</code> object, (2) a match column is not set
     *         for a <code>RowSet</code> object in <i>rowset</i>, or (3)
     *         a <code>RowSet</code> object being added violates the active
     *         <code>JOIN</code>
     */
    public void addRowSet(RowSet[] rowset,
                          String[] columnName) throws SQLException;

    /**
     * Returns a <code>Collection</code> object containing the
     * <code>RowSet</code> objects that have been added to this
     * <code>JoinRowSet</code> object.
     * This should return the 'n' number of RowSet contained
     * within the <code>JOIN</code> and maintain any updates that have occurred while in
     * this union.
     *
     * @return a <code>Collection</code> object consisting of the
     *        <code>RowSet</code> objects added to this <code>JoinRowSet</code>
     *        object
     * @throws SQLException if an error occurs generating the
     *         <code>Collection</code> object to be returned
     */
    public Collection<?> getRowSets() throws java.sql.SQLException;

    /**
     * Returns a <code>String</code> array containing the names of the
     *         <code>RowSet</code> objects added to this <code>JoinRowSet</code>
     *         object.
     *
     * @return a <code>String</code> array of the names of the
     *         <code>RowSet</code> objects in this <code>JoinRowSet</code>
     *         object
     * @throws SQLException if an error occurs retrieving the names of
     *         the <code>RowSet</code> objects
     * @see CachedRowSet#setTableName
     */
    public String[] getRowSetNames() throws java.sql.SQLException;

    /**
     * Creates a new <code>CachedRowSet</code> object containing the
     * data in this <code>JoinRowSet</code> object, which can be saved
     * to a data source using the <code>SyncProvider</code> object for
     * the <code>CachedRowSet</code> object.
     * <P>
     * If any updates or modifications have been applied to the JoinRowSet
     * the CachedRowSet returned by the method will not be able to persist
     * it's changes back to the originating rows and tables in the
     * in the datasource. The CachedRowSet instance returned should not
     * contain modification data and it should clear all properties of
     * it's originating SQL statement. An application should reset the
     * SQL statement using the <code>RowSet.setCommand</code> method.
     * <p>
     * In order to allow changes to be persisted back to the datasource
     * to the originating tables, the <code>acceptChanges</code> method
     * should be used and called on a JoinRowSet object instance. Implementations
     * can leverage the internal data and update tracking in their
     * implementations to interact with the SyncProvider to persist any
     * changes.
     *
     * @return a CachedRowSet containing the contents of the JoinRowSet
     * @throws SQLException if an error occurs assembling the CachedRowSet
     * object
     * @see javax.sql.RowSet
     * @see javax.sql.rowset.CachedRowSet
     * @see javax.sql.rowset.spi.SyncProvider
     */
    public CachedRowSet toCachedRowSet() throws java.sql.SQLException;

    /**
     * Indicates if CROSS_JOIN is supported by a JoinRowSet
     * implementation
     *
     * @return true if the CROSS_JOIN is supported; false otherwise
     */
    public boolean supportsCrossJoin();

    /**
     * Indicates if INNER_JOIN is supported by a JoinRowSet
     * implementation
     *
     * @return true is the INNER_JOIN is supported; false otherwise
     */
    public boolean supportsInnerJoin();

    /**
     * Indicates if LEFT_OUTER_JOIN is supported by a JoinRowSet
     * implementation
     *
     * @return true is the LEFT_OUTER_JOIN is supported; false otherwise
     */
    public boolean supportsLeftOuterJoin();

    /**
     * Indicates if RIGHT_OUTER_JOIN is supported by a JoinRowSet
     * implementation
     *
     * @return true is the RIGHT_OUTER_JOIN is supported; false otherwise
     */
    public boolean supportsRightOuterJoin();

    /**
     * Indicates if FULL_JOIN is supported by a JoinRowSet
     * implementation
     *
     * @return true is the FULL_JOIN is supported; false otherwise
     */
    public boolean supportsFullJoin();

    /**
     * Allow the application to adjust the type of <code>JOIN</code> imposed
     * on tables contained within the JoinRowSet object instance.
     * Implementations should throw a SQLException if they do
     * not support a given <code>JOIN</code> type.
     *
     * @param joinType the standard JoinRowSet.XXX static field definition
     * of a SQL <code>JOIN</code> to re-configure a JoinRowSet instance on
     * the fly.
     * @throws SQLException if an unsupported <code>JOIN</code> type is set
     * @see #getJoinType
     */
    public void setJoinType(int joinType) throws SQLException;

    /**
     * Return a SQL-like description of the WHERE clause being used
     * in a JoinRowSet object. An implementation can describe
     * the WHERE clause of the SQL <code>JOIN</code> by supplying a SQL
     * strings description of <code>JOIN</code> or provide a textual
     * description to assist applications using a <code>JoinRowSet</code>
     *
     * @return whereClause a textual or SQL description of the logical
     * WHERE clause used in the JoinRowSet instance
     * @throws SQLException if an error occurs in generating a representation
     * of the WHERE clause.
     */
    public String getWhereClause() throws SQLException;

    /**
     * Returns a <code>int</code> describing the set SQL <code>JOIN</code> type
     * governing this JoinRowSet instance. The returned type will be one of
     * standard JoinRowSet types: <code>CROSS_JOIN</code>, <code>INNER_JOIN</code>,
     * <code>LEFT_OUTER_JOIN</code>, <code>RIGHT_OUTER_JOIN</code> or
     * <code>FULL_JOIN</code>.
     *
     * @return joinType one of the standard JoinRowSet static field
     *     definitions of a SQL <code>JOIN</code>. <code>JoinRowSet.INNER_JOIN</code>
     *     is returned as the default <code>JOIN</code> type is no type has been
     *     explicitly set.
     * @throws SQLException if an error occurs determining the SQL <code>JOIN</code>
     *     type supported by the JoinRowSet instance.
     * @see #setJoinType
     */
    public int getJoinType() throws SQLException;

    /**
     * An ANSI-style <code>JOIN</code> providing a cross product of two tables
     */
    public static int CROSS_JOIN = 0;

    /**
     * An ANSI-style <code>JOIN</code> providing a inner join between two tables. Any
     * unmatched rows in either table of the join should be discarded.
     */
    public static int INNER_JOIN = 1;

    /**
     * An ANSI-style <code>JOIN</code> providing a left outer join between two
     * tables. In SQL, this is described where all records should be
     * returned from the left side of the JOIN statement.
     */
    public static int LEFT_OUTER_JOIN = 2;

    /**
     * An ANSI-style <code>JOIN</code> providing a right outer join between
     * two tables. In SQL, this is described where all records from the
     * table on the right side of the JOIN statement even if the table
     * on the left has no matching record.
     */
    public static int RIGHT_OUTER_JOIN = 3;

    /**
     * An ANSI-style <code>JOIN</code> providing a full JOIN. Specifies that all
     * rows from either table be returned regardless of matching
     * records on the other table.
     */
    public static int FULL_JOIN = 4;


}
