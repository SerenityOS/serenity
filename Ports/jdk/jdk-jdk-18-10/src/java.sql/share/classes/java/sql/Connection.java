/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * <P>A connection (session) with a specific
 * database. SQL statements are executed and results are returned
 * within the context of a connection.
 * <P>
 * A {@code Connection} object's database is able to provide information
 * describing its tables, its supported SQL grammar, its stored
 * procedures, the capabilities of this connection, and so on. This
 * information is obtained with the {@code getMetaData} method.
 *
 * <P><B>Note:</B> When configuring a {@code Connection}, JDBC applications
 *  should use the appropriate {@code Connection} method such as
 *  {@code setAutoCommit} or {@code setTransactionIsolation}.
 *  Applications should not invoke SQL commands directly to change the connection's
 *   configuration when there is a JDBC method available.  By default a {@code Connection} object is in
 * auto-commit mode, which means that it automatically commits changes
 * after executing each statement. If auto-commit mode has been
 * disabled, the method {@code commit} must be called explicitly in
 * order to commit changes; otherwise, database changes will not be saved.
 * <P>
 * A new {@code Connection} object created using the JDBC 2.1 core API
 * has an initially empty type map associated with it. A user may enter a
 * custom mapping for a UDT in this type map.
 * When a UDT is retrieved from a data source with the
 * method {@code ResultSet.getObject}, the {@code getObject} method
 * will check the connection's type map to see if there is an entry for that
 * UDT.  If so, the {@code getObject} method will map the UDT to the
 * class indicated.  If there is no entry, the UDT will be mapped using the
 * standard mapping.
 * <p>
 * A user may create a new type map, which is a {@code java.util.Map}
 * object, make an entry in it, and pass it to the {@code java.sql}
 * methods that can perform custom mapping.  In this case, the method
 * will use the given type map instead of the one associated with
 * the connection.
 * <p>
 * For example, the following code fragment specifies that the SQL
 * type {@code ATHLETES} will be mapped to the class
 * {@code Athletes} in the Java programming language.
 * The code fragment retrieves the type map for the {@code Connection
 * } object {@code con}, inserts the entry into it, and then sets
 * the type map with the new entry as the connection's type map.
 * <pre>
 *      java.util.Map map = con.getTypeMap();
 *      map.put("mySchemaName.ATHLETES", Class.forName("Athletes"));
 *      con.setTypeMap(map);
 * </pre>
 *
 * @see DriverManager#getConnection
 * @see Statement
 * @see ResultSet
 * @see DatabaseMetaData
 * @since 1.1
 */
public interface Connection  extends Wrapper, AutoCloseable {

    /**
     * Creates a {@code Statement} object for sending
     * SQL statements to the database.
     * SQL statements without parameters are normally
     * executed using {@code Statement} objects. If the same SQL statement
     * is executed many times, it may be more efficient to use a
     * {@code PreparedStatement} object.
     * <P>
     * Result sets created using the returned {@code Statement}
     * object will by default be type {@code TYPE_FORWARD_ONLY}
     * and have a concurrency level of {@code CONCUR_READ_ONLY}.
     * The holdability of the created result sets can be determined by
     * calling {@link #getHoldability}.
     *
     * @return a new default {@code Statement} object
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     */
    Statement createStatement() throws SQLException;

    /**
     * Creates a {@code PreparedStatement} object for sending
     * parameterized SQL statements to the database.
     * <P>
     * A SQL statement with or without IN parameters can be
     * pre-compiled and stored in a {@code PreparedStatement} object. This
     * object can then be used to efficiently execute this statement
     * multiple times.
     *
     * <P><B>Note:</B> This method is optimized for handling
     * parametric SQL statements that benefit from precompilation. If
     * the driver supports precompilation,
     * the method {@code prepareStatement} will send
     * the statement to the database for precompilation. Some drivers
     * may not support precompilation. In this case, the statement may
     * not be sent to the database until the {@code PreparedStatement}
     * object is executed.  This has no direct effect on users; however, it does
     * affect which methods throw certain {@code SQLException} objects.
     * <P>
     * Result sets created using the returned {@code PreparedStatement}
     * object will by default be type {@code TYPE_FORWARD_ONLY}
     * and have a concurrency level of {@code CONCUR_READ_ONLY}.
     * The holdability of the created result sets can be determined by
     * calling {@link #getHoldability}.
     *
     * @param sql an SQL statement that may contain one or more '?' IN
     * parameter placeholders
     * @return a new default {@code PreparedStatement} object containing the
     * pre-compiled SQL statement
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     */
    PreparedStatement prepareStatement(String sql)
        throws SQLException;

    /**
     * Creates a {@code CallableStatement} object for calling
     * database stored procedures.
     * The {@code CallableStatement} object provides
     * methods for setting up its IN and OUT parameters, and
     * methods for executing the call to a stored procedure.
     *
     * <P><B>Note:</B> This method is optimized for handling stored
     * procedure call statements. Some drivers may send the call
     * statement to the database when the method {@code prepareCall}
     * is done; others
     * may wait until the {@code CallableStatement} object
     * is executed. This has no
     * direct effect on users; however, it does affect which method
     * throws certain SQLExceptions.
     * <P>
     * Result sets created using the returned {@code CallableStatement}
     * object will by default be type {@code TYPE_FORWARD_ONLY}
     * and have a concurrency level of {@code CONCUR_READ_ONLY}.
     * The holdability of the created result sets can be determined by
     * calling {@link #getHoldability}.
     *
     * @param sql an SQL statement that may contain one or more '?'
     * parameter placeholders. Typically this statement is specified using JDBC
     * call escape syntax.
     * @return a new default {@code CallableStatement} object containing the
     * pre-compiled SQL statement
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     */
    CallableStatement prepareCall(String sql) throws SQLException;

    /**
     * Converts the given SQL statement into the system's native SQL grammar.
     * A driver may convert the JDBC SQL grammar into its system's
     * native SQL grammar prior to sending it. This method returns the
     * native form of the statement that the driver would have sent.
     *
     * @param sql an SQL statement that may contain one or more '?'
     * parameter placeholders
     * @return the native form of this statement
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     */
    String nativeSQL(String sql) throws SQLException;

    /**
     * Sets this connection's auto-commit mode to the given state.
     * If a connection is in auto-commit mode, then all its SQL
     * statements will be executed and committed as individual
     * transactions.  Otherwise, its SQL statements are grouped into
     * transactions that are terminated by a call to either
     * the method {@code commit} or the method {@code rollback}.
     * By default, new connections are in auto-commit
     * mode.
     * <P>
     * The commit occurs when the statement completes. The time when the statement
     * completes depends on the type of SQL Statement:
     * <ul>
     * <li>For DML statements, such as Insert, Update or Delete, and DDL statements,
     * the statement is complete as soon as it has finished executing.
     * <li>For Select statements, the statement is complete when the associated result
     * set is closed.
     * <li>For {@code CallableStatement} objects or for statements that return
     * multiple results, the statement is complete
     * when all of the associated result sets have been closed, and all update
     * counts and output parameters have been retrieved.
     *</ul>
     * <P>
     * <B>NOTE:</B>  If this method is called during a transaction and the
     * auto-commit mode is changed, the transaction is committed.  If
     * {@code setAutoCommit} is called and the auto-commit mode is
     * not changed, the call is a no-op.
     *
     * @param autoCommit {@code true} to enable auto-commit mode;
     *         {@code false} to disable it
     * @throws SQLException if a database access error occurs,
     *  setAutoCommit(true) is called while participating in a distributed transaction,
     * or this method is called on a closed connection
     * @see #getAutoCommit
     */
    void setAutoCommit(boolean autoCommit) throws SQLException;

    /**
     * Retrieves the current auto-commit mode for this {@code Connection}
     * object.
     *
     * @return the current state of this {@code Connection} object's
     *         auto-commit mode
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     * @see #setAutoCommit
     */
    boolean getAutoCommit() throws SQLException;

    /**
     * Makes all changes made since the previous
     * commit/rollback permanent and releases any database locks
     * currently held by this {@code Connection} object.
     * This method should be
     * used only when auto-commit mode has been disabled.
     *
     * @throws SQLException if a database access error occurs,
     * this method is called while participating in a distributed transaction,
     * if this method is called on a closed connection or this
     *            {@code Connection} object is in auto-commit mode
     * @see #setAutoCommit
     */
    void commit() throws SQLException;

    /**
     * Undoes all changes made in the current transaction
     * and releases any database locks currently held
     * by this {@code Connection} object. This method should be
     * used only when auto-commit mode has been disabled.
     *
     * @throws SQLException if a database access error occurs,
     * this method is called while participating in a distributed transaction,
     * this method is called on a closed connection or this
     *            {@code Connection} object is in auto-commit mode
     * @see #setAutoCommit
     */
    void rollback() throws SQLException;

    /**
     * Releases this {@code Connection} object's database and JDBC resources
     * immediately instead of waiting for them to be automatically released.
     * <P>
     * Calling the method {@code close} on a {@code Connection}
     * object that is already closed is a no-op.
     * <P>
     * It is <b>strongly recommended</b> that an application explicitly
     * commits or rolls back an active transaction prior to calling the
     * {@code close} method.  If the {@code close} method is called
     * and there is an active transaction, the results are implementation-defined.
     *
     * @throws SQLException if a database access error occurs
     */
    void close() throws SQLException;

    /**
     * Retrieves whether this {@code Connection} object has been
     * closed.  A connection is closed if the method {@code close}
     * has been called on it or if certain fatal errors have occurred.
     * This method is guaranteed to return {@code true} only when
     * it is called after the method {@code Connection.close} has
     * been called.
     * <P>
     * This method generally cannot be called to determine whether a
     * connection to a database is valid or invalid.  A typical client
     * can determine that a connection is invalid by catching any
     * exceptions that might be thrown when an operation is attempted.
     *
     * @return {@code true} if this {@code Connection} object
     *         is closed; {@code false} if it is still open
     * @throws SQLException if a database access error occurs
     */
    boolean isClosed() throws SQLException;

    //======================================================================
    // Advanced features:

    /**
     * Retrieves a {@code DatabaseMetaData} object that contains
     * metadata about the database to which this
     * {@code Connection} object represents a connection.
     * The metadata includes information about the database's
     * tables, its supported SQL grammar, its stored
     * procedures, the capabilities of this connection, and so on.
     *
     * @return a {@code DatabaseMetaData} object for this
     *         {@code Connection} object
     * @throws  SQLException if a database access error occurs
     * or this method is called on a closed connection
     */
    DatabaseMetaData getMetaData() throws SQLException;

    /**
     * Puts this connection in read-only mode as a hint to the driver to enable
     * database optimizations.
     *
     * <P><B>Note:</B> This method cannot be called during a transaction.
     *
     * @param readOnly {@code true} enables read-only mode;
     *        {@code false} disables it
     * @throws SQLException if a database access error occurs, this
     *  method is called on a closed connection or this
     *            method is called during a transaction
     */
    void setReadOnly(boolean readOnly) throws SQLException;

    /**
     * Retrieves whether this {@code Connection}
     * object is in read-only mode.
     *
     * @return {@code true} if this {@code Connection} object
     *         is read-only; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     */
    boolean isReadOnly() throws SQLException;

    /**
     * Sets the given catalog name in order to select
     * a subspace of this {@code Connection} object's database
     * in which to work.
     * <P>
     * If the driver does not support catalogs, it will
     * silently ignore this request.
     * <p>
     * Calling {@code setCatalog} has no effect on previously created or prepared
     * {@code Statement} objects. It is implementation defined whether a DBMS
     * prepare operation takes place immediately when the {@code Connection}
     * method {@code prepareStatement} or {@code prepareCall} is invoked.
     * For maximum portability, {@code setCatalog} should be called before a
     * {@code Statement} is created or prepared.
     *
     * @param catalog the name of a catalog (subspace in this
     *        {@code Connection} object's database) in which to work
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     * @see #getCatalog
     */
    void setCatalog(String catalog) throws SQLException;

    /**
     * Retrieves this {@code Connection} object's current catalog name.
     *
     * @return the current catalog name or {@code null} if there is none
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     * @see #setCatalog
     */
    String getCatalog() throws SQLException;

    /**
     * A constant indicating that transactions are not supported.
     */
    int TRANSACTION_NONE             = 0;

    /**
     * A constant indicating that
     * dirty reads, non-repeatable reads and phantom reads can occur.
     * This level allows a row changed by one transaction to be read
     * by another transaction before any changes in that row have been
     * committed (a "dirty read").  If any of the changes are rolled back,
     * the second transaction will have retrieved an invalid row.
     */
    int TRANSACTION_READ_UNCOMMITTED = 1;

    /**
     * A constant indicating that
     * dirty reads are prevented; non-repeatable reads and phantom
     * reads can occur.  This level only prohibits a transaction
     * from reading a row with uncommitted changes in it.
     */
    int TRANSACTION_READ_COMMITTED   = 2;

    /**
     * A constant indicating that
     * dirty reads and non-repeatable reads are prevented; phantom
     * reads can occur.  This level prohibits a transaction from
     * reading a row with uncommitted changes in it, and it also
     * prohibits the situation where one transaction reads a row,
     * a second transaction alters the row, and the first transaction
     * rereads the row, getting different values the second time
     * (a "non-repeatable read").
     */
    int TRANSACTION_REPEATABLE_READ  = 4;

    /**
     * A constant indicating that
     * dirty reads, non-repeatable reads and phantom reads are prevented.
     * This level includes the prohibitions in
     * {@code TRANSACTION_REPEATABLE_READ} and further prohibits the
     * situation where one transaction reads all rows that satisfy
     * a {@code WHERE} condition, a second transaction inserts a row that
     * satisfies that {@code WHERE} condition, and the first transaction
     * rereads for the same condition, retrieving the additional
     * "phantom" row in the second read.
     */
    int TRANSACTION_SERIALIZABLE     = 8;

    /**
     * Attempts to change the transaction isolation level for this
     * {@code Connection} object to the one given.
     * The constants defined in the interface {@code Connection}
     * are the possible transaction isolation levels.
     * <P>
     * <B>Note:</B> If this method is called during a transaction, the result
     * is implementation-defined.
     *
     * @param level one of the following {@code Connection} constants:
     *        {@code Connection.TRANSACTION_READ_UNCOMMITTED},
     *        {@code Connection.TRANSACTION_READ_COMMITTED},
     *        {@code Connection.TRANSACTION_REPEATABLE_READ}, or
     *        {@code Connection.TRANSACTION_SERIALIZABLE}.
     *        (Note that {@code Connection.TRANSACTION_NONE} cannot be used
     *        because it specifies that transactions are not supported.)
     * @throws SQLException if a database access error occurs, this
     * method is called on a closed connection
     *            or the given parameter is not one of the {@code Connection}
     *            constants
     * @see DatabaseMetaData#supportsTransactionIsolationLevel
     * @see #getTransactionIsolation
     */
    void setTransactionIsolation(int level) throws SQLException;

    /**
     * Retrieves this {@code Connection} object's current
     * transaction isolation level.
     *
     * @return the current transaction isolation level, which will be one
     *         of the following constants:
     *        {@code Connection.TRANSACTION_READ_UNCOMMITTED},
     *        {@code Connection.TRANSACTION_READ_COMMITTED},
     *        {@code Connection.TRANSACTION_REPEATABLE_READ},
     *        {@code Connection.TRANSACTION_SERIALIZABLE}, or
     *        {@code Connection.TRANSACTION_NONE}.
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     * @see #setTransactionIsolation
     */
    int getTransactionIsolation() throws SQLException;

    /**
     * Retrieves the first warning reported by calls on this
     * {@code Connection} object.  If there is more than one
     * warning, subsequent warnings will be chained to the first one
     * and can be retrieved by calling the method
     * {@code SQLWarning.getNextWarning} on the warning
     * that was retrieved previously.
     * <P>
     * This method may not be
     * called on a closed connection; doing so will cause an
     * {@code SQLException} to be thrown.
     *
     * <P><B>Note:</B> Subsequent warnings will be chained to this
     * SQLWarning.
     *
     * @return the first {@code SQLWarning} object or {@code null}
     *         if there are none
     * @throws SQLException if a database access error occurs or
     *         this method is called on a closed connection
     * @see SQLWarning
     */
    SQLWarning getWarnings() throws SQLException;

    /**
     * Clears all warnings reported for this {@code Connection} object.
     * After a call to this method, the method {@code getWarnings}
     * returns {@code null} until a new warning is
     * reported for this {@code Connection} object.
     *
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     */
    void clearWarnings() throws SQLException;


    //--------------------------JDBC 2.0-----------------------------

    /**
     * Creates a {@code Statement} object that will generate
     * {@code ResultSet} objects with the given type and concurrency.
     * This method is the same as the {@code createStatement} method
     * above, but it allows the default result set
     * type and concurrency to be overridden.
     * The holdability of the created result sets can be determined by
     * calling {@link #getHoldability}.
     *
     * @param resultSetType a result set type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency a concurrency type; one of
     *        {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}
     * @return a new {@code Statement} object that will generate
     *         {@code ResultSet} objects with the given type and
     *         concurrency
     * @throws SQLException if a database access error occurs, this
     * method is called on a closed connection
     *         or the given parameters are not {@code ResultSet}
     *         constants indicating type and concurrency
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method or this method is not supported for the specified result
     * set type and result set concurrency.
     * @since 1.2
     */
    Statement createStatement(int resultSetType, int resultSetConcurrency)
        throws SQLException;

    /**
     *
     * Creates a {@code PreparedStatement} object that will generate
     * {@code ResultSet} objects with the given type and concurrency.
     * This method is the same as the {@code prepareStatement} method
     * above, but it allows the default result set
     * type and concurrency to be overridden.
     * The holdability of the created result sets can be determined by
     * calling {@link #getHoldability}.
     *
     * @param sql a {@code String} object that is the SQL statement to
     *            be sent to the database; may contain one or more '?' IN
     *            parameters
     * @param resultSetType a result set type; one of
     *         {@code ResultSet.TYPE_FORWARD_ONLY},
     *         {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *         {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency a concurrency type; one of
     *         {@code ResultSet.CONCUR_READ_ONLY} or
     *         {@code ResultSet.CONCUR_UPDATABLE}
     * @return a new PreparedStatement object containing the
     * pre-compiled SQL statement that will produce {@code ResultSet}
     * objects with the given type and concurrency
     * @throws SQLException if a database access error occurs, this
     * method is called on a closed connection
     *         or the given parameters are not {@code ResultSet}
     *         constants indicating type and concurrency
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method or this method is not supported for the specified result
     * set type and result set concurrency.
     * @since 1.2
     */
    PreparedStatement prepareStatement(String sql, int resultSetType,
                                       int resultSetConcurrency)
        throws SQLException;

    /**
     * Creates a {@code CallableStatement} object that will generate
     * {@code ResultSet} objects with the given type and concurrency.
     * This method is the same as the {@code prepareCall} method
     * above, but it allows the default result set
     * type and concurrency to be overridden.
     * The holdability of the created result sets can be determined by
     * calling {@link #getHoldability}.
     *
     * @param sql a {@code String} object that is the SQL statement to
     *            be sent to the database; may contain on or more '?' parameters
     * @param resultSetType a result set type; one of
     *         {@code ResultSet.TYPE_FORWARD_ONLY},
     *         {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *         {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency a concurrency type; one of
     *         {@code ResultSet.CONCUR_READ_ONLY} or
     *         {@code ResultSet.CONCUR_UPDATABLE}
     * @return a new {@code CallableStatement} object containing the
     * pre-compiled SQL statement that will produce {@code ResultSet}
     * objects with the given type and concurrency
     * @throws SQLException if a database access error occurs, this method
     * is called on a closed connection
     *         or the given parameters are not {@code ResultSet}
     *         constants indicating type and concurrency
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method or this method is not supported for the specified result
     * set type and result set concurrency.
     * @since 1.2
     */
    CallableStatement prepareCall(String sql, int resultSetType,
                                  int resultSetConcurrency) throws SQLException;

    /**
     * Retrieves the {@code Map} object associated with this
     * {@code Connection} object.
     * Unless the application has added an entry, the type map returned
     * will be empty.
     * <p>
     * You must invoke {@code setTypeMap} after making changes to the
     * {@code Map} object returned from
     *  {@code getTypeMap} as a JDBC driver may create an internal
     * copy of the {@code Map} object passed to {@code setTypeMap}:
     *
     * <pre>
     *      Map&lt;String,Class&lt;?&gt;&gt; myMap = con.getTypeMap();
     *      myMap.put("mySchemaName.ATHLETES", Athletes.class);
     *      con.setTypeMap(myMap);
     * </pre>
     * @return the {@code java.util.Map} object associated
     *         with this {@code Connection} object
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     * @see #setTypeMap
     */
    java.util.Map<String,Class<?>> getTypeMap() throws SQLException;

    /**
     * Installs the given {@code TypeMap} object as the type map for
     * this {@code Connection} object.  The type map will be used for the
     * custom mapping of SQL structured types and distinct types.
     * <p>
     * You must set the values for the {@code TypeMap} prior to
     * callng {@code setMap} as a JDBC driver may create an internal copy
     * of the {@code TypeMap}:
     *
     * <pre>
     *      Map myMap&lt;String,Class&lt;?&gt;&gt; = new HashMap&lt;String,Class&lt;?&gt;&gt;();
     *      myMap.put("mySchemaName.ATHLETES", Athletes.class);
     *      con.setTypeMap(myMap);
     * </pre>
     * @param map the {@code java.util.Map} object to install
     *        as the replacement for this {@code Connection}
     *        object's default type map
     * @throws SQLException if a database access error occurs, this
     * method is called on a closed connection or
     *        the given parameter is not a {@code java.util.Map}
     *        object
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     * @see #getTypeMap
     */
    void setTypeMap(java.util.Map<String,Class<?>> map) throws SQLException;

    //--------------------------JDBC 3.0-----------------------------


    /**
     * Changes the default holdability of {@code ResultSet} objects
     * created using this {@code Connection} object to the given
     * holdability.  The default holdability of {@code ResultSet} objects
     * can be determined by invoking
     * {@link DatabaseMetaData#getResultSetHoldability}.
     *
     * @param holdability a {@code ResultSet} holdability constant; one of
     *        {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or
     *        {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}
     * @throws SQLException if a database access occurs, this method is called
     * on a closed connection, or the given parameter
     *         is not a {@code ResultSet} constant indicating holdability
     * @throws SQLFeatureNotSupportedException if the given holdability is not supported
     * @see #getHoldability
     * @see DatabaseMetaData#getResultSetHoldability
     * @see ResultSet
     * @since 1.4
     */
    void setHoldability(int holdability) throws SQLException;

    /**
     * Retrieves the current holdability of {@code ResultSet} objects
     * created using this {@code Connection} object.
     *
     * @return the holdability, one of
     *        {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or
     *        {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     * @see #setHoldability
     * @see DatabaseMetaData#getResultSetHoldability
     * @see ResultSet
     * @since 1.4
     */
    int getHoldability() throws SQLException;

    /**
     * Creates an unnamed savepoint in the current transaction and
     * returns the new {@code Savepoint} object that represents it.
     *
     *<p> if setSavepoint is invoked outside of an active transaction, a transaction will be started at this newly created
     *savepoint.
     *
     * @return the new {@code Savepoint} object
     * @throws SQLException if a database access error occurs,
     * this method is called while participating in a distributed transaction,
     * this method is called on a closed connection
     *            or this {@code Connection} object is currently in
     *            auto-commit mode
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see Savepoint
     * @since 1.4
     */
    Savepoint setSavepoint() throws SQLException;

    /**
     * Creates a savepoint with the given name in the current transaction
     * and returns the new {@code Savepoint} object that represents it.
     *
     * <p> if setSavepoint is invoked outside of an active transaction, a transaction will be started at this newly created
     *savepoint.
     *
     * @param name a {@code String} containing the name of the savepoint
     * @return the new {@code Savepoint} object
     * @throws SQLException if a database access error occurs,
          * this method is called while participating in a distributed transaction,
     * this method is called on a closed connection
     *            or this {@code Connection} object is currently in
     *            auto-commit mode
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see Savepoint
     * @since 1.4
     */
    Savepoint setSavepoint(String name) throws SQLException;

    /**
     * Undoes all changes made after the given {@code Savepoint} object
     * was set.
     * <P>
     * This method should be used only when auto-commit has been disabled.
     *
     * @param savepoint the {@code Savepoint} object to roll back to
     * @throws SQLException if a database access error occurs,
     * this method is called while participating in a distributed transaction,
     * this method is called on a closed connection,
     *            the {@code Savepoint} object is no longer valid,
     *            or this {@code Connection} object is currently in
     *            auto-commit mode
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see Savepoint
     * @see #rollback
     * @since 1.4
     */
    void rollback(Savepoint savepoint) throws SQLException;

    /**
     * Removes the specified {@code Savepoint}  and subsequent {@code Savepoint} objects from the current
     * transaction. Any reference to the savepoint after it have been removed
     * will cause an {@code SQLException} to be thrown.
     *
     * @param savepoint the {@code Savepoint} object to be removed
     * @throws SQLException if a database access error occurs, this
     *  method is called on a closed connection or
     *            the given {@code Savepoint} object is not a valid
     *            savepoint in the current transaction
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void releaseSavepoint(Savepoint savepoint) throws SQLException;

    /**
     * Creates a {@code Statement} object that will generate
     * {@code ResultSet} objects with the given type, concurrency,
     * and holdability.
     * This method is the same as the {@code createStatement} method
     * above, but it allows the default result set
     * type, concurrency, and holdability to be overridden.
     *
     * @param resultSetType one of the following {@code ResultSet}
     *        constants:
     *         {@code ResultSet.TYPE_FORWARD_ONLY},
     *         {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *         {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency one of the following {@code ResultSet}
     *        constants:
     *         {@code ResultSet.CONCUR_READ_ONLY} or
     *         {@code ResultSet.CONCUR_UPDATABLE}
     * @param resultSetHoldability one of the following {@code ResultSet}
     *        constants:
     *         {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or
     *         {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}
     * @return a new {@code Statement} object that will generate
     *         {@code ResultSet} objects with the given type,
     *         concurrency, and holdability
     * @throws SQLException if a database access error occurs, this
     * method is called on a closed connection
     *            or the given parameters are not {@code ResultSet}
     *            constants indicating type, concurrency, and holdability
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method or this method is not supported for the specified result
     * set type, result set holdability and result set concurrency.
     * @see ResultSet
     * @since 1.4
     */
    Statement createStatement(int resultSetType, int resultSetConcurrency,
                              int resultSetHoldability) throws SQLException;

    /**
     * Creates a {@code PreparedStatement} object that will generate
     * {@code ResultSet} objects with the given type, concurrency,
     * and holdability.
     * <P>
     * This method is the same as the {@code prepareStatement} method
     * above, but it allows the default result set
     * type, concurrency, and holdability to be overridden.
     *
     * @param sql a {@code String} object that is the SQL statement to
     *            be sent to the database; may contain one or more '?' IN
     *            parameters
     * @param resultSetType one of the following {@code ResultSet}
     *        constants:
     *         {@code ResultSet.TYPE_FORWARD_ONLY},
     *         {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *         {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency one of the following {@code ResultSet}
     *        constants:
     *         {@code ResultSet.CONCUR_READ_ONLY} or
     *         {@code ResultSet.CONCUR_UPDATABLE}
     * @param resultSetHoldability one of the following {@code ResultSet}
     *        constants:
     *         {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or
     *         {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}
     * @return a new {@code PreparedStatement} object, containing the
     *         pre-compiled SQL statement, that will generate
     *         {@code ResultSet} objects with the given type,
     *         concurrency, and holdability
     * @throws SQLException if a database access error occurs, this
     * method is called on a closed connection
     *            or the given parameters are not {@code ResultSet}
     *            constants indicating type, concurrency, and holdability
      * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method or this method is not supported for the specified result
     * set type, result set holdability and result set concurrency.
     * @see ResultSet
     * @since 1.4
     */
    PreparedStatement prepareStatement(String sql, int resultSetType,
                                       int resultSetConcurrency, int resultSetHoldability)
        throws SQLException;

    /**
     * Creates a {@code CallableStatement} object that will generate
     * {@code ResultSet} objects with the given type and concurrency.
     * This method is the same as the {@code prepareCall} method
     * above, but it allows the default result set
     * type, result set concurrency type and holdability to be overridden.
     *
     * @param sql a {@code String} object that is the SQL statement to
     *            be sent to the database; may contain on or more '?' parameters
     * @param resultSetType one of the following {@code ResultSet}
     *        constants:
     *         {@code ResultSet.TYPE_FORWARD_ONLY},
     *         {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *         {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency one of the following {@code ResultSet}
     *        constants:
     *         {@code ResultSet.CONCUR_READ_ONLY} or
     *         {@code ResultSet.CONCUR_UPDATABLE}
     * @param resultSetHoldability one of the following {@code ResultSet}
     *        constants:
     *         {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or
     *         {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}
     * @return a new {@code CallableStatement} object, containing the
     *         pre-compiled SQL statement, that will generate
     *         {@code ResultSet} objects with the given type,
     *         concurrency, and holdability
     * @throws SQLException if a database access error occurs, this
     * method is called on a closed connection
     *            or the given parameters are not {@code ResultSet}
     *            constants indicating type, concurrency, and holdability
      * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method or this method is not supported for the specified result
     * set type, result set holdability and result set concurrency.
     * @see ResultSet
     * @since 1.4
     */
    CallableStatement prepareCall(String sql, int resultSetType,
                                  int resultSetConcurrency,
                                  int resultSetHoldability) throws SQLException;


    /**
     * Creates a default {@code PreparedStatement} object that has
     * the capability to retrieve auto-generated keys. The given constant
     * tells the driver whether it should make auto-generated keys
     * available for retrieval.  This parameter is ignored if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <P>
     * <B>Note:</B> This method is optimized for handling
     * parametric SQL statements that benefit from precompilation. If
     * the driver supports precompilation,
     * the method {@code prepareStatement} will send
     * the statement to the database for precompilation. Some drivers
     * may not support precompilation. In this case, the statement may
     * not be sent to the database until the {@code PreparedStatement}
     * object is executed.  This has no direct effect on users; however, it does
     * affect which methods throw certain SQLExceptions.
     * <P>
     * Result sets created using the returned {@code PreparedStatement}
     * object will by default be type {@code TYPE_FORWARD_ONLY}
     * and have a concurrency level of {@code CONCUR_READ_ONLY}.
     * The holdability of the created result sets can be determined by
     * calling {@link #getHoldability}.
     *
     * @param sql an SQL statement that may contain one or more '?' IN
     *        parameter placeholders
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *        should be returned; one of
     *        {@code Statement.RETURN_GENERATED_KEYS} or
     *        {@code Statement.NO_GENERATED_KEYS}
     * @return a new {@code PreparedStatement} object, containing the
     *         pre-compiled SQL statement, that will have the capability of
     *         returning auto-generated keys
     * @throws SQLException if a database access error occurs, this
     *  method is called on a closed connection
     *         or the given parameter is not a {@code Statement}
     *         constant indicating whether auto-generated keys should be
     *         returned
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @since 1.4
     */
    PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException;

    /**
     * Creates a default {@code PreparedStatement} object capable
     * of returning the auto-generated keys designated by the given array.
     * This array contains the indexes of the columns in the target
     * table that contain the auto-generated keys that should be made
     * available.  The driver will ignore the array if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     *<p>
     * An SQL statement with or without IN parameters can be
     * pre-compiled and stored in a {@code PreparedStatement} object. This
     * object can then be used to efficiently execute this statement
     * multiple times.
     * <P>
     * <B>Note:</B> This method is optimized for handling
     * parametric SQL statements that benefit from precompilation. If
     * the driver supports precompilation,
     * the method {@code prepareStatement} will send
     * the statement to the database for precompilation. Some drivers
     * may not support precompilation. In this case, the statement may
     * not be sent to the database until the {@code PreparedStatement}
     * object is executed.  This has no direct effect on users; however, it does
     * affect which methods throw certain SQLExceptions.
     * <P>
     * Result sets created using the returned {@code PreparedStatement}
     * object will by default be type {@code TYPE_FORWARD_ONLY}
     * and have a concurrency level of {@code CONCUR_READ_ONLY}.
     * The holdability of the created result sets can be determined by
     * calling {@link #getHoldability}.
     *
     * @param sql an SQL statement that may contain one or more '?' IN
     *        parameter placeholders
     * @param columnIndexes an array of column indexes indicating the columns
     *        that should be returned from the inserted row or rows
     * @return a new {@code PreparedStatement} object, containing the
     *         pre-compiled statement, that is capable of returning the
     *         auto-generated keys designated by the given array of column
     *         indexes
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     *
     * @since 1.4
     */
    PreparedStatement prepareStatement(String sql, int columnIndexes[])
        throws SQLException;

    /**
     * Creates a default {@code PreparedStatement} object capable
     * of returning the auto-generated keys designated by the given array.
     * This array contains the names of the columns in the target
     * table that contain the auto-generated keys that should be returned.
     * The driver will ignore the array if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <P>
     * An SQL statement with or without IN parameters can be
     * pre-compiled and stored in a {@code PreparedStatement} object. This
     * object can then be used to efficiently execute this statement
     * multiple times.
     * <P>
     * <B>Note:</B> This method is optimized for handling
     * parametric SQL statements that benefit from precompilation. If
     * the driver supports precompilation,
     * the method {@code prepareStatement} will send
     * the statement to the database for precompilation. Some drivers
     * may not support precompilation. In this case, the statement may
     * not be sent to the database until the {@code PreparedStatement}
     * object is executed.  This has no direct effect on users; however, it does
     * affect which methods throw certain SQLExceptions.
     * <P>
     * Result sets created using the returned {@code PreparedStatement}
     * object will by default be type {@code TYPE_FORWARD_ONLY}
     * and have a concurrency level of {@code CONCUR_READ_ONLY}.
     * The holdability of the created result sets can be determined by
     * calling {@link #getHoldability}.
     *
     * @param sql an SQL statement that may contain one or more '?' IN
     *        parameter placeholders
     * @param columnNames an array of column names indicating the columns
     *        that should be returned from the inserted row or rows
     * @return a new {@code PreparedStatement} object, containing the
     *         pre-compiled statement, that is capable of returning the
     *         auto-generated keys designated by the given array of column
     *         names
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     *
     * @since 1.4
     */
    PreparedStatement prepareStatement(String sql, String columnNames[])
        throws SQLException;

    /**
     * Constructs an object that implements the {@code Clob} interface. The object
     * returned initially contains no data.  The {@code setAsciiStream},
     * {@code setCharacterStream} and {@code setString} methods of
     * the {@code Clob} interface may be used to add data to the {@code Clob}.
     * @return An object that implements the {@code Clob} interface
     * @throws SQLException if an object that implements the
     * {@code Clob} interface can not be constructed, this method is
     * called on a closed connection or a database access error occurs.
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this data type
     *
     * @since 1.6
     */
    Clob createClob() throws SQLException;

    /**
     * Constructs an object that implements the {@code Blob} interface. The object
     * returned initially contains no data.  The {@code setBinaryStream} and
     * {@code setBytes} methods of the {@code Blob} interface may be used to add data to
     * the {@code Blob}.
     * @return  An object that implements the {@code Blob} interface
     * @throws SQLException if an object that implements the
     * {@code Blob} interface can not be constructed, this method is
     * called on a closed connection or a database access error occurs.
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this data type
     *
     * @since 1.6
     */
    Blob createBlob() throws SQLException;

    /**
     * Constructs an object that implements the {@code NClob} interface. The object
     * returned initially contains no data.  The {@code setAsciiStream},
     * {@code setCharacterStream} and {@code setString} methods of the {@code NClob} interface may
     * be used to add data to the {@code NClob}.
     * @return An object that implements the {@code NClob} interface
     * @throws SQLException if an object that implements the
     * {@code NClob} interface can not be constructed, this method is
     * called on a closed connection or a database access error occurs.
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this data type
     *
     * @since 1.6
     */
    NClob createNClob() throws SQLException;

    /**
     * Constructs an object that implements the {@code SQLXML} interface. The object
     * returned initially contains no data. The {@code createXmlStreamWriter} object and
     * {@code setString} method of the {@code SQLXML} interface may be used to add data to the {@code SQLXML}
     * object.
     * @return An object that implements the {@code SQLXML} interface
     * @throws SQLException if an object that implements the {@code SQLXML} interface can not
     * be constructed, this method is
     * called on a closed connection or a database access error occurs.
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this data type
     * @since 1.6
     */
    SQLXML createSQLXML() throws SQLException;

        /**
         * Returns true if the connection has not been closed and is still valid.
         * The driver shall submit a query on the connection or use some other
         * mechanism that positively verifies the connection is still valid when
         * this method is called.
         * <p>
         * The query submitted by the driver to validate the connection shall be
         * executed in the context of the current transaction.
         *
         * @param timeout -             The time in seconds to wait for the database operation
         *                                              used to validate the connection to complete.  If
         *                                              the timeout period expires before the operation
         *                                              completes, this method returns false.  A value of
         *                                              0 indicates a timeout is not applied to the
         *                                              database operation.
         *
         * @return true if the connection is valid, false otherwise
         * @throws SQLException if the value supplied for {@code timeout}
         * is less than 0
         * @since 1.6
         *
         * @see java.sql.DatabaseMetaData#getClientInfoProperties
         */
         boolean isValid(int timeout) throws SQLException;

        /**
         * Sets the value of the client info property specified by name to the
         * value specified by value.
         * <p>
         * Applications may use the {@code DatabaseMetaData.getClientInfoProperties}
         * method to determine the client info properties supported by the driver
         * and the maximum length that may be specified for each property.
         * <p>
         * The driver stores the value specified in a suitable location in the
         * database.  For example in a special register, session parameter, or
         * system table column.  For efficiency the driver may defer setting the
         * value in the database until the next time a statement is executed or
         * prepared.  Other than storing the client information in the appropriate
         * place in the database, these methods shall not alter the behavior of
         * the connection in anyway.  The values supplied to these methods are
         * used for accounting, diagnostics and debugging purposes only.
         * <p>
         * The driver shall generate a warning if the client info name specified
         * is not recognized by the driver.
         * <p>
         * If the value specified to this method is greater than the maximum
         * length for the property the driver may either truncate the value and
         * generate a warning or generate a {@code SQLClientInfoException}.  If the driver
         * generates a {@code SQLClientInfoException}, the value specified was not set on the
         * connection.
         * <p>
         * The following are standard client info properties.  Drivers are not
         * required to support these properties however if the driver supports a
         * client info property that can be described by one of the standard
         * properties, the standard property name should be used.
         *
         * <ul>
         * <li>ApplicationName  -       The name of the application currently utilizing
         *                                                      the connection</li>
         * <li>ClientUser               -       The name of the user that the application using
         *                                                      the connection is performing work for.  This may
         *                                                      not be the same as the user name that was used
         *                                                      in establishing the connection.</li>
         * <li>ClientHostname   -       The hostname of the computer the application
         *                                                      using the connection is running on.</li>
         * </ul>
         *
         * @param name          The name of the client info property to set
         * @param value         The value to set the client info property to.  If the
         *                                      value is null, the current value of the specified
         *                                      property is cleared.
         *
         * @throws      SQLClientInfoException if the database server returns an error while
         *                      setting the client info value on the database server or this method
         * is called on a closed connection
         *
         * @since 1.6
         */
         void setClientInfo(String name, String value)
                throws SQLClientInfoException;

        /**
     * Sets the value of the connection's client info properties.  The
     * {@code Properties} object contains the names and values of the client info
     * properties to be set.  The set of client info properties contained in
     * the properties list replaces the current set of client info properties
     * on the connection.  If a property that is currently set on the
     * connection is not present in the properties list, that property is
     * cleared.  Specifying an empty properties list will clear all of the
     * properties on the connection.  See {@code setClientInfo (String, String)} for
     * more information.
     * <p>
     * If an error occurs in setting any of the client info properties, a
     * {@code SQLClientInfoException} is thrown. The {@code SQLClientInfoException}
     * contains information indicating which client info properties were not set.
     * The state of the client information is unknown because
     * some databases do not allow multiple client info properties to be set
     * atomically.  For those databases, one or more properties may have been
     * set before the error occurred.
     *
     *
     * @param properties                the list of client info properties to set
     *
     * @see java.sql.Connection#setClientInfo(String, String) setClientInfo(String, String)
     * @since 1.6
     *
     * @throws SQLClientInfoException if the database server returns an error while
     *                  setting the clientInfo values on the database server or this method
     * is called on a closed connection
     *
     */
         void setClientInfo(Properties properties)
                throws SQLClientInfoException;

        /**
         * Returns the value of the client info property specified by name.  This
         * method may return null if the specified client info property has not
         * been set and does not have a default value.  This method will also
         * return null if the specified client info property name is not supported
         * by the driver.
         * <p>
         * Applications may use the {@code DatabaseMetaData.getClientInfoProperties}
         * method to determine the client info properties supported by the driver.
         *
         * @param name          The name of the client info property to retrieve
         *
         * @return                      The value of the client info property specified
         *
         * @throws SQLException         if the database server returns an error when
         *                              fetching the client info value from the database
         *                              or this method is called on a closed connection
         *
         * @since 1.6
         *
         * @see java.sql.DatabaseMetaData#getClientInfoProperties
         */
         String getClientInfo(String name)
                throws SQLException;

        /**
         * Returns a list containing the name and current value of each client info
         * property supported by the driver.  The value of a client info property
         * may be null if the property has not been set and does not have a
         * default value.
         *
         * @return      A {@code Properties} object that contains the name and current value of
         *                      each of the client info properties supported by the driver.
         *
         * @throws      SQLException if the database server returns an error when
         *                      fetching the client info values from the database
         * or this method is called on a closed connection
         *
         * @since 1.6
         */
         Properties getClientInfo()
                throws SQLException;

/**
  * Factory method for creating Array objects.
  *<p>
  * <b>Note: </b>When {@code createArrayOf} is used to create an array object
  * that maps to a primitive data type, then it is implementation-defined
  * whether the {@code Array} object is an array of that primitive
  * data type or an array of {@code Object}.
  * <p>
  * <b>Note: </b>The JDBC driver is responsible for mapping the elements
  * {@code Object} array to the default JDBC SQL type defined in
  * java.sql.Types for the given class of {@code Object}. The default
  * mapping is specified in Appendix B of the JDBC specification.  If the
  * resulting JDBC type is not the appropriate type for the given typeName then
  * it is implementation defined whether an {@code SQLException} is
  * thrown or the driver supports the resulting conversion.
  *
  * @param typeName the SQL name of the type the elements of the array map to. The typeName is a
  * database-specific name which may be the name of a built-in type, a user-defined type or a standard  SQL type supported by this database. This
  *  is the value returned by {@code Array.getBaseTypeName}
  * @param elements the elements that populate the returned object
  * @return an Array object whose elements map to the specified SQL type
  * @throws SQLException if a database error occurs, the JDBC type is not
  *  appropriate for the typeName and the conversion is not supported, the typeName is null or this method is called on a closed connection
  * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this data type
  * @since 1.6
  */
 Array createArrayOf(String typeName, Object[] elements) throws
SQLException;

/**
  * Factory method for creating Struct objects.
  *
  * @param typeName the SQL type name of the SQL structured type that this {@code Struct}
  * object maps to. The typeName is the name of  a user-defined type that
  * has been defined for this database. It is the value returned by
  * {@code Struct.getSQLTypeName}.

  * @param attributes the attributes that populate the returned object
  * @return a Struct object that maps to the given SQL type and is populated with the given attributes
  * @throws SQLException if a database error occurs, the typeName is null or this method is called on a closed connection
  * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this data type
  * @since 1.6
  */
 Struct createStruct(String typeName, Object[] attributes)
throws SQLException;

   //--------------------------JDBC 4.1 -----------------------------

   /**
    * Sets the given schema name to access.
    * <P>
    * If the driver does not support schemas, it will
    * silently ignore this request.
    * <p>
    * Calling {@code setSchema} has no effect on previously created or prepared
    * {@code Statement} objects. It is implementation defined whether a DBMS
    * prepare operation takes place immediately when the {@code Connection}
    * method {@code prepareStatement} or {@code prepareCall} is invoked.
    * For maximum portability, {@code setSchema} should be called before a
    * {@code Statement} is created or prepared.
    *
    * @param schema the name of a schema  in which to work
    * @throws SQLException if a database access error occurs
    * or this method is called on a closed connection
    * @see #getSchema
    * @since 1.7
    */
    void setSchema(String schema) throws SQLException;

    /**
     * Retrieves this {@code Connection} object's current schema name.
     *
     * @return the current schema name or {@code null} if there is none
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     * @see #setSchema
     * @since 1.7
     */
    String getSchema() throws SQLException;

    /**
     * Terminates an open connection.  Calling {@code abort} results in:
     * <ul>
     * <li>The connection marked as closed
     * <li>Closes any physical connection to the database
     * <li>Releases resources used by the connection
     * <li>Insures that any thread that is currently accessing the connection
     * will either progress to completion or throw an {@code SQLException}.
     * </ul>
     * <p>
     * Calling {@code abort} marks the connection closed and releases any
     * resources. Calling {@code abort} on a closed connection is a
     * no-op.
     * <p>
     * It is possible that the aborting and releasing of the resources that are
     * held by the connection can take an extended period of time.  When the
     * {@code abort} method returns, the connection will have been marked as
     * closed and the {@code Executor} that was passed as a parameter to abort
     * may still be executing tasks to release resources.
     * <p>
     * This method checks to see that there is an {@code SQLPermission}
     * object before allowing the method to proceed.  If a
     * {@code SecurityManager} exists and its
     * {@code checkPermission} method denies calling {@code abort},
     * this method throws a
     * {@code java.lang.SecurityException}.
     * @param executor  The {@code Executor}  implementation which will
     * be used by {@code abort}.
     * @throws java.sql.SQLException if a database access error occurs or
     * the {@code executor} is {@code null},
     * @throws java.lang.SecurityException if a security manager exists and its
     *    {@code checkPermission} method denies calling {@code abort}
     * @see SecurityManager#checkPermission
     * @see Executor
     * @since 1.7
     */
    void abort(Executor executor) throws SQLException;

    /**
     *
     * Sets the maximum period a {@code Connection} or
     * objects created from the {@code Connection}
     * will wait for the database to reply to any one request. If any
     *  request remains unanswered, the waiting method will
     * return with a {@code SQLException}, and the {@code Connection}
     * or objects created from the {@code Connection}  will be marked as
     * closed. Any subsequent use of
     * the objects, with the exception of the {@code close},
     * {@code isClosed} or {@code Connection.isValid}
     * methods, will result in  a {@code SQLException}.
     * <p>
     * <b>Note</b>: This method is intended to address a rare but serious
     * condition where network partitions can cause threads issuing JDBC calls
     * to hang uninterruptedly in socket reads, until the OS TCP-TIMEOUT
     * (typically 10 minutes). This method is related to the
     * {@link #abort abort() } method which provides an administrator
     * thread a means to free any such threads in cases where the
     * JDBC connection is accessible to the administrator thread.
     * The {@code setNetworkTimeout} method will cover cases where
     * there is no administrator thread, or it has no access to the
     * connection. This method is severe in it's effects, and should be
     * given a high enough value so it is never triggered before any more
     * normal timeouts, such as transaction timeouts.
     * <p>
     * JDBC driver implementations  may also choose to support the
     * {@code setNetworkTimeout} method to impose a limit on database
     * response time, in environments where no network is present.
     * <p>
     * Drivers may internally implement some or all of their API calls with
     * multiple internal driver-database transmissions, and it is left to the
     * driver implementation to determine whether the limit will be
     * applied always to the response to the API call, or to any
     * single  request made during the API call.
     * <p>
     *
     * This method can be invoked more than once, such as to set a limit for an
     * area of JDBC code, and to reset to the default on exit from this area.
     * Invocation of this method has no impact on already outstanding
     * requests.
     * <p>
     * The {@code Statement.setQueryTimeout()} timeout value is independent of the
     * timeout value specified in {@code setNetworkTimeout}. If the query timeout
     * expires  before the network timeout then the
     * statement execution will be canceled. If the network is still
     * active the result will be that both the statement and connection
     * are still usable. However if the network timeout expires before
     * the query timeout or if the statement timeout fails due to network
     * problems, the connection will be marked as closed, any resources held by
     * the connection will be released and both the connection and
     * statement will be unusable.
     * <p>
     * When the driver determines that the {@code setNetworkTimeout} timeout
     * value has expired, the JDBC driver marks the connection
     * closed and releases any resources held by the connection.
     * <p>
     *
     * This method checks to see that there is an {@code SQLPermission}
     * object before allowing the method to proceed.  If a
     * {@code SecurityManager} exists and its
     * {@code checkPermission} method denies calling
     * {@code setNetworkTimeout}, this method throws a
     * {@code java.lang.SecurityException}.
     *
     * @param executor  The {@code Executor}  implementation which will
     * be used by {@code setNetworkTimeout}.
     * @param milliseconds The time in milliseconds to wait for the database
     * operation
     *  to complete.  If the JDBC driver does not support milliseconds, the
     * JDBC driver will round the value up to the nearest second.  If the
     * timeout period expires before the operation
     * completes, a SQLException will be thrown.
     * A value of 0 indicates that there is not timeout for database operations.
     * @throws java.sql.SQLException if a database access error occurs, this
     * method is called on a closed connection,
     * the {@code executor} is {@code null},
     * or the value specified for {@code seconds} is less than 0.
     * @throws java.lang.SecurityException if a security manager exists and its
     *    {@code checkPermission} method denies calling
     * {@code setNetworkTimeout}.
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see SecurityManager#checkPermission
     * @see Statement#setQueryTimeout
     * @see #getNetworkTimeout
     * @see #abort
     * @see Executor
     * @since 1.7
     */
    void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException;


    /**
     * Retrieves the number of milliseconds the driver will
     * wait for a database request to complete.
     * If the limit is exceeded, a
     * {@code SQLException} is thrown.
     *
     * @return the current timeout limit in milliseconds; zero means there is
     *         no limit
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code Connection}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #setNetworkTimeout
     * @since 1.7
     */
    int getNetworkTimeout() throws SQLException;

    // JDBC 4.3

     /**
     * Hints to the driver that a request, an independent unit of work, is beginning
     * on this connection. Each request is independent of all other requests
     * with regard to state local to the connection either on the client or the
     * server. Work done between {@code beginRequest}, {@code endRequest}
     * pairs does not depend on any other work done on the connection either as
     * part of another request or outside of any request. A request may include multiple
     * transactions. There may be dependencies on committed database state as
     * that is not local to the connection.
     * <p>
     * Local state is defined as any state associated with a Connection that is
     * local to the current Connection either in the client or the database that
     * is not transparently reproducible.
     * <p>
     * Calls to {@code beginRequest} and {@code endRequest}  are not nested.
     * Multiple calls to {@code beginRequest} without an intervening call
     * to {@code endRequest} is not an error. The first {@code beginRequest} call
     * marks the start of the request and subsequent calls are treated as
     * a no-op
     * <p>
     * Use of {@code beginRequest} and {@code endRequest} is optional, vendor
     * specific and should largely be transparent. In particular
     * implementations may detect conditions that indicate dependence on
     * other work such as an open transaction. It is recommended though not
     * required that implementations throw a {@code SQLException} if there is an active
     * transaction and {@code beginRequest} is called.
     * Using these methods may improve performance or provide other benefits.
     * Consult your vendors documentation for additional information.
     * <p>
     * It is recommended to
     * enclose each unit of work in {@code beginRequest}, {@code endRequest}
     * pairs such that there is no open transaction at the beginning or end of
     * the request and no dependency on local state that crosses request
     * boundaries. Committed database state is not local.
     *
     * @implSpec
     * The default implementation is a no-op.
     *
     * @apiNote
     * This method is to be used by Connection pooling managers.
     * <p>
     * The pooling manager should call {@code beginRequest} on the underlying connection
     * prior to returning a connection to the caller.
     * <p>
     * The pooling manager does not need to call {@code beginRequest} if:
     * <ul>
     * <li>The connection pool caches {@code PooledConnection} objects</li>
     * <li>Returns a logical connection handle when {@code getConnection} is
     * called by the application</li>
     * <li>The logical {@code Connection} is closed by calling
     * {@code Connection.close} prior to returning the {@code PooledConnection}
     * to the cache.</li>
     * </ul>
     * @throws SQLException if an error occurs
     * @since 9
     * @see endRequest
     * @see javax.sql.PooledConnection
     */
    default void beginRequest() throws SQLException {
       // Default method takes no action
    }

    /**
     * Hints to the driver that a request, an independent unit of work,
     * has completed. Calls to {@code beginRequest}
     * and {@code endRequest} are not nested. Multiple
     * calls to {@code endRequest} without an intervening call to {@code beginRequest}
     * is not an error. The first {@code endRequest} call
     * marks the request completed and subsequent calls are treated as
     * a no-op. If {@code endRequest} is called without an initial call to
     * {@code beginRequest} is a no-op.
     *<p>
     * The exact behavior of this method is vendor specific. In particular
     * implementations may detect conditions that indicate dependence on
     * other work such as an open transaction. It is recommended though not
     * required that implementations throw a {@code SQLException} if there is an active
     * transaction and {@code endRequest} is called.
     *
     * @implSpec
     * The default implementation is a no-op.
     * @apiNote
     *
     * This method is to be used by Connection pooling managers.
     * <p>
     * The pooling manager should call {@code endRequest} on the underlying connection
     * when the applications returns the connection back to the connection pool.
     * <p>
     * The pooling manager does not need to call {@code endRequest} if:
     * <ul>
     * <li>The connection pool caches {@code PooledConnection} objects</li>
     * <li>Returns a logical connection handle when {@code getConnection} is
     * called by the application</li>
     * <li>The logical {@code Connection} is closed by calling
     * {@code Connection.close} prior to returning the {@code PooledConnection}
     * to the cache.</li>
     * </ul>
     * @throws SQLException if an error occurs
     * @since 9
     * @see beginRequest
     * @see javax.sql.PooledConnection
     */
    default void endRequest() throws SQLException {
            // Default method takes no action
    }

    /**
     * Sets and validates the sharding keys for this connection. A {@code null}
     * value may be specified for the sharding Key. The validity
     * of a {@code null} sharding key is vendor-specific. Consult your vendor&#39;s
     * documentation for additional information.
     * @implSpec
     * The default implementation will throw a
     * {@code SQLFeatureNotSupportedException}.
     *
     * @apiNote
     * This method validates that the sharding keys are valid for the
     * {@code Connection}. The timeout value indicates how long the driver
     * should wait for the {@code Connection} to verify that the sharding key
     * is valid before {@code setShardingKeyIfValid} returns false.
     * @param shardingKey the sharding key to be validated against this connection.
     * The sharding key may be {@code null}
     * @param superShardingKey the super sharding key to be validated against this
     * connection. The super sharding key may be {@code null}.
     * @param timeout time in seconds before which the validation process is expected to
     * be completed, otherwise the validation process is aborted. A value of 0 indicates
     * the validation process will not time out.
     * @return true if the connection is valid and the sharding keys are valid
     * and set on this connection; false if the sharding keys are not valid or
     * the timeout period expires before the operation completes.
     * @throws SQLException if an error occurs while performing this validation;
     * a {@code superSharedingKey} is specified
     * without a {@code shardingKey};
     * this method is called on a closed {@code connection}; or
     * the {@code timeout} value is negative.
     * @throws SQLFeatureNotSupportedException if the driver does not support sharding
     * @since 9
     * @see ShardingKey
     * @see ShardingKeyBuilder
     */
    default boolean setShardingKeyIfValid(ShardingKey shardingKey,
            ShardingKey superShardingKey, int timeout)
            throws SQLException {
        throw new SQLFeatureNotSupportedException("setShardingKeyIfValid not implemented");
    }

    /**
     * Sets and validates the sharding key for this connection. A {@code null}
     * value may be specified for the sharding Key. The validity
     * of a {@code null} sharding key is vendor-specific. Consult your vendor&#39;s
     * documentation for additional information.
     * @implSpec
     * The default implementation will throw a
     * {@code SQLFeatureNotSupportedException}.
     * @apiNote
     * This method validates  that the sharding key is valid for the
     * {@code Connection}. The timeout value indicates how long the driver
     * should wait for the {@code Connection} to verify that the sharding key
     * is valid before {@code setShardingKeyIfValid} returns false.
     * @param shardingKey the sharding key to be validated against this connection.
     * The sharding key may be {@code null}
     * @param timeout time in seconds before which the validation process is expected to
     * be completed,else the validation process is aborted. A value of 0 indicates
     * the validation process will not time out.
     * @return true if the connection is valid and the sharding key is valid to be
     * set on this connection; false if the sharding key is not valid or
     * the timeout period expires before the operation completes.
     * @throws SQLException if there is an error while performing this validation;
     * this method is called on a closed {@code connection};
     * or the {@code timeout} value is negative.
     * @throws SQLFeatureNotSupportedException if the driver does not support sharding
     * @since 9
     * @see ShardingKey
     * @see ShardingKeyBuilder
     */
    default boolean setShardingKeyIfValid(ShardingKey shardingKey, int timeout)
            throws SQLException {
        throw new SQLFeatureNotSupportedException("setShardingKeyIfValid not implemented");
    }

    /**
     * Specifies a shardingKey and superShardingKey to use with this Connection
     * @implSpec
     * The default implementation will throw a
     * {@code SQLFeatureNotSupportedException}.
     * @apiNote
     * This method sets the specified sharding keys but does not require a
     * round trip to the database to validate that the sharding keys are valid
     * for the {@code Connection}.
     * @param shardingKey the sharding key to set on this connection. The sharding
     * key may be {@code null}
     * @param superShardingKey the super sharding key to set on this connection.
     * The super sharding key may be {@code null}
     * @throws SQLException if an error  occurs setting the sharding keys;
     * this method is called on a closed {@code connection}; or
     * a {@code superSharedingKey} is specified without a {@code shardingKey}
     * @throws SQLFeatureNotSupportedException if the driver does not support sharding
     * @since 9
     * @see ShardingKey
     * @see ShardingKeyBuilder
     */
    default void setShardingKey(ShardingKey shardingKey, ShardingKey superShardingKey)
            throws SQLException {
        throw new SQLFeatureNotSupportedException("setShardingKey not implemented");
    }

    /**
     * Specifies a shardingKey to use with this Connection
     * @implSpec
     * The default implementation will throw a
     * {@code SQLFeatureNotSupportedException}.
     * @apiNote
     * This method sets the specified sharding key but does not require a
     * round trip to the database to validate that the sharding key is valid
     * for the {@code Connection}.
     * @param shardingKey the sharding key to set on this connection. The sharding
     * key may be {@code null}
     * @throws SQLException if an error occurs setting the sharding key; or
     * this method is called on a closed {@code connection}
     * @throws SQLFeatureNotSupportedException if the driver does not support sharding
     * @since 9
     * @see ShardingKey
     * @see ShardingKeyBuilder
     */
    default void setShardingKey(ShardingKey shardingKey)
            throws SQLException {
        throw new SQLFeatureNotSupportedException("setShardingKey not implemented");
    }
}
