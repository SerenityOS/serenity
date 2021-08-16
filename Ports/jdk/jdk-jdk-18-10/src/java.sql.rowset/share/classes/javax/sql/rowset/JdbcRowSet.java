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
import java.io.*;

/**
 * The standard interface that all standard implementations of
 * <code>JdbcRowSet</code> must implement.
 *
 * <h2>1.0 Overview</h2>
 * A wrapper around a <code>ResultSet</code> object that makes it possible
 * to use the result set as a JavaBeans
 * component.  Thus, a <code>JdbcRowSet</code> object can be one of the Beans that
 * a tool makes available for composing an application.  Because
 * a <code>JdbcRowSet</code> is a connected rowset, that is, it continually
 * maintains its connection to a database using a JDBC technology-enabled
 * driver, it also effectively makes the driver a JavaBeans component.
 * <P>
 * Because it is always connected to its database, an instance of
 * <code>JdbcRowSet</code>
 * can simply take calls invoked on it and in turn call them on its
 * <code>ResultSet</code> object. As a consequence, a result set can, for
 * example, be a component in a Swing application.
 * <P>
 * Another advantage of a <code>JdbcRowSet</code> object is that it can be
 * used to make a <code>ResultSet</code> object scrollable and updatable.  All
 * <code>RowSet</code> objects are by default scrollable and updatable. If
 * the driver and database being used do not support scrolling and/or updating
 * of result sets, an application can populate a <code>JdbcRowSet</code> object
 * with the data of a <code>ResultSet</code> object and then operate on the
 * <code>JdbcRowSet</code> object as if it were the <code>ResultSet</code>
 * object.
 *
 * <h2>2.0 Creating a <code>JdbcRowSet</code> Object</h2>
 * The reference implementation of the <code>JdbcRowSet</code> interface,
 * <code>JdbcRowSetImpl</code>, provides an implementation of
 * the default constructor.  A new instance is initialized with
 * default values, which can be set with new values as needed. A
 * new instance is not really functional until its <code>execute</code>
 * method is called. In general, this method does the following:
 * <UL>
 *   <LI> establishes a connection with a database
 *   <LI> creates a <code>PreparedStatement</code> object and sets any of its
 *        placeholder parameters
 *   <LI> executes the statement to create a <code>ResultSet</code> object
 * </UL>
 * If the <code>execute</code> method is successful, it will set the
 * appropriate private <code>JdbcRowSet</code> fields with the following:
 * <UL>
 *  <LI> a <code>Connection</code> object -- the connection between the rowset
 *       and the database
 *  <LI> a <code>PreparedStatement</code> object -- the query that produces
 *       the result set
 *  <LI> a <code>ResultSet</code> object -- the result set that the rowset's
 *       command produced and that is being made, in effect, a JavaBeans
 *       component
 * </UL>
 * If these fields have not been set, meaning that the <code>execute</code>
 * method has not executed successfully, no methods other than
 * <code>execute</code> and <code>close</code> may be called on the
 * rowset.  All other public methods will throw an exception.
 * <P>
 * Before calling the <code>execute</code> method, however, the command
 * and properties needed for establishing a connection must be set.
 * The following code fragment creates a <code>JdbcRowSetImpl</code> object,
 * sets the command and connection properties, sets the placeholder parameter,
 * and then invokes the method <code>execute</code>.
 * <PRE>
 *     JdbcRowSetImpl jrs = new JdbcRowSetImpl();
 *     jrs.setCommand("SELECT * FROM TITLES WHERE TYPE = ?");
 *     jrs.setURL("jdbc:myDriver:myAttribute");
 *     jrs.setUsername("cervantes");
 *     jrs.setPassword("sancho");
 *     jrs.setString(1, "BIOGRAPHY");
 *     jrs.execute();
 * </PRE>
 * The variable <code>jrs</code> now represents an instance of
 * <code>JdbcRowSetImpl</code> that is a thin wrapper around the
 * <code>ResultSet</code> object containing all the rows in the
 * table <code>TITLES</code> where the type of book is biography.
 * At this point, operations called on <code>jrs</code> will
 * affect the rows in the result set, which is effectively a JavaBeans
 * component.
 * <P>
 * The implementation of the <code>RowSet</code> method <code>execute</code> in the
 * <code>JdbcRowSet</code> reference implementation differs from that in the
 * <code>CachedRowSet</code>
 * reference implementation to account for the different
 * requirements of connected and disconnected <code>RowSet</code> objects.
 *
 * @author Jonathan Bruce
 * @since 1.5
 */

public interface JdbcRowSet extends RowSet, Joinable {

    /**
     * Retrieves a <code>boolean</code> indicating whether rows marked
     * for deletion appear in the set of current rows. If <code>true</code> is
     * returned, deleted rows are visible with the current rows. If
     * <code>false</code> is returned, rows are not visible with the set of
     * current rows. The default value is <code>false</code>.
     * <P>
     * Standard rowset implementations may choose to restrict this behavior
     * for security considerations or for certain deployment
     * scenarios. The visibility of deleted rows is implementation-defined
     * and does not represent standard behavior.
     * <P>
     * Note: Allowing deleted rows to remain visible complicates the behavior
     * of some standard JDBC <code>RowSet</code> implementations methods.
     * However, most rowset users can simply ignore this extra detail because
     * only very specialized applications will likely want to take advantage of
     * this feature.
     *
     * @return <code>true</code> if deleted rows are visible;
     *         <code>false</code> otherwise
     * @exception SQLException if a rowset implementation is unable to
     *          to determine whether rows marked for deletion remain visible
     * @see #setShowDeleted
     */
    public boolean getShowDeleted() throws SQLException;

    /**
     * Sets the property <code>showDeleted</code> to the given
     * <code>boolean</code> value. This property determines whether
     * rows marked for deletion continue to appear in the set of current rows.
     * If the value is set to <code>true</code>, deleted rows are immediately
     * visible with the set of current rows. If the value is set to
     * <code>false</code>, the deleted rows are set as invisible with the
     * current set of rows.
     * <P>
     * Standard rowset implementations may choose to restrict this behavior
     * for security considerations or for certain deployment
     * scenarios. This is left as implementation-defined and does not
     * represent standard behavior.
     *
     * @param b <code>true</code> if deleted rows should be shown;
     *              <code>false</code> otherwise
     * @exception SQLException if a rowset implementation is unable to
     *          to reset whether deleted rows should be visible
     * @see #getShowDeleted
     */
    public void setShowDeleted(boolean b) throws SQLException;

    /**
     * Retrieves the first warning reported by calls on this <code>JdbcRowSet</code>
     * object.
     * If a second warning was reported on this <code>JdbcRowSet</code> object,
     * it will be chained to the first warning and can be retrieved by
     * calling the method <code>RowSetWarning.getNextWarning</code> on the
     * first warning. Subsequent warnings on this <code>JdbcRowSet</code>
     * object will be chained to the <code>RowSetWarning</code> objects
     * returned by the method <code>RowSetWarning.getNextWarning</code>.
     *
     * The warning chain is automatically cleared each time a new row is read.
     * This method may not be called on a <code>RowSet</code> object
     * that has been closed;
     * doing so will cause an <code>SQLException</code> to be thrown.
     * <P>
     * Because it is always connected to its data source, a <code>JdbcRowSet</code>
     * object can rely on the presence of active
     * <code>Statement</code>, <code>Connection</code>, and <code>ResultSet</code>
     * instances. This means that  applications can obtain additional
     * <code>SQLWarning</code>
     * notifications by calling the <code>getNextWarning</code> methods that
     * they provide.
     * Disconnected <code>Rowset</code> objects, such as a
     * <code>CachedRowSet</code> object, do not have access to
     * these <code>getNextWarning</code> methods.
     *
     * @return the first <code>RowSetWarning</code>
     * object reported on this <code>JdbcRowSet</code> object
     * or <code>null</code> if there are none
     * @throws SQLException if this method is called on a closed
     * <code>JdbcRowSet</code> object
     * @see RowSetWarning
     */
    public RowSetWarning getRowSetWarnings() throws SQLException;

   /**
    * Each <code>JdbcRowSet</code> contains a <code>Connection</code> object from
    * the <code>ResultSet</code> or JDBC properties passed to it's constructors.
    * This method wraps the <code>Connection</code> commit method to allow flexible
    * auto commit or non auto commit transactional control support.
    * <p>
    * Makes all changes made since the previous commit/rollback permanent
    * and releases any database locks currently held by this Connection
    * object. This method should be used only when auto-commit mode has
    * been disabled.
    *
    * @throws SQLException if a database access error occurs or this
    * Connection object within this <code>JdbcRowSet</code> is in auto-commit mode
    * @see java.sql.Connection#setAutoCommit
    */
    public void commit() throws SQLException;


   /**
    * Each <code>JdbcRowSet</code> contains a <code>Connection</code> object from
    * the original <code>ResultSet</code> or JDBC properties passed to it. This
    * method wraps the <code>Connection</code>'s <code>getAutoCommit</code> method
    * to allow an application to determine the <code>JdbcRowSet</code> transaction
    * behavior.
    * <p>
    * Sets this connection's auto-commit mode to the given state. If a
    * connection is in auto-commit mode, then all its SQL statements will
    * be executed and committed as individual transactions. Otherwise, its
    * SQL statements are grouped into transactions that are terminated by a
    * call to either the method commit or the method rollback. By default,
    * new connections are in auto-commit mode.
    *
    * @return {@code true} if auto-commit is enabled; {@code false} otherwise
    * @throws SQLException if a database access error occurs
    * @see java.sql.Connection#getAutoCommit()
    */
    public boolean getAutoCommit() throws SQLException;


   /**
    * Each <code>JdbcRowSet</code> contains a <code>Connection</code> object from
    * the original <code>ResultSet</code> or JDBC properties passed to it. This
    * method wraps the <code>Connection</code>'s <code>getAutoCommit</code> method
    * to allow an application to set the <code>JdbcRowSet</code> transaction behavior.
    * <p>
    * Sets the current auto-commit mode for this <code>Connection</code> object.
    * @param autoCommit {@code true} to enable auto-commit; {@code false} to
    * disable auto-commit
    * @throws SQLException if a database access error occurs
    * @see java.sql.Connection#setAutoCommit(boolean)
    */
    public void setAutoCommit(boolean autoCommit) throws SQLException;

    /**
     * Each <code>JdbcRowSet</code> contains a <code>Connection</code> object from
     * the original <code>ResultSet</code> or JDBC properties passed to it.
     * Undoes all changes made in the current transaction and releases any
     * database locks currently held by this <code>Connection</code> object. This method
     * should be used only when auto-commit mode has been disabled.
     *
     * @throws SQLException if a database access error occurs or this <code>Connection</code>
     * object within this <code>JdbcRowSet</code> is in auto-commit mode.
     * @see #rollback(Savepoint)
     */
     public void rollback() throws SQLException;


    /**
     * Each <code>JdbcRowSet</code> contains a <code>Connection</code> object from
     * the original <code>ResultSet</code> or JDBC properties passed to it.
     * Undoes all changes made in the current transaction to the last set savepoint
     * and releases any database locks currently held by this <code>Connection</code>
     * object. This method should be used only when auto-commit mode has been disabled.
     * @param s The {@code Savepoint} to rollback to
     * @throws SQLException if a database access error occurs or this <code>Connection</code>
     * object within this <code>JdbcRowSet</code> is in auto-commit mode.
     * @see #rollback
     */
    public void rollback(Savepoint s) throws SQLException;

}
