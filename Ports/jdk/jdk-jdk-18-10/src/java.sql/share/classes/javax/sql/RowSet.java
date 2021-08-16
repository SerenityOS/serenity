/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql;

import java.sql.*;
import java.io.*;
import java.math.*;
import java.util.*;

/**
 * The interface that adds support to the JDBC API for the
 * JavaBeans component model.
 * A rowset, which can be used as a JavaBeans component in
 * a visual Bean development environment, can be created and
 * configured at design time and executed at run time.
 * <P>
 * The {@code RowSet}
 * interface provides a set of JavaBeans properties that allow a {@code RowSet}
 * instance to be configured to connect to a JDBC data source and read
 * some data from the data source.  A group of setter methods ({@code setInt},
 * {@code setBytes}, {@code setString}, and so on)
 * provide a way to pass input parameters to a rowset's command property.
 * This command is the SQL query the rowset uses when it gets its data from
 * a relational database, which is generally the case.
 * <P>
 * The {@code RowSet}
 * interface supports JavaBeans events, allowing other components in an
 * application to be notified when an event occurs on a rowset,
 * such as a change in its value.
 *
 * <P>The {@code RowSet} interface is unique in that it is intended to be
 * implemented using the rest of the JDBC API.  In other words, a
 * {@code RowSet} implementation is a layer of software that executes "on top"
 * of a JDBC driver.  Implementations of the {@code RowSet} interface can
 * be provided by anyone, including JDBC driver vendors who want to
 * provide a {@code RowSet} implementation as part of their JDBC products.
 * <P>
 * A {@code RowSet} object may make a connection with a data source and
 * maintain that connection throughout its life cycle, in which case it is
 * called a <i>connected</i> rowset.  A rowset may also make a connection with
 * a data source, get data from it, and then close the connection. Such a rowset
 * is called a <i>disconnected</i> rowset.  A disconnected rowset may make
 * changes to its data while it is disconnected and then send the changes back
 * to the original source of the data, but it must reestablish a connection to do so.
 * <P>
 * A disconnected rowset may have a {@code Reader} (a {@code RowSetReader} object)
 * and a writer (a {@code RowSetWriter} object) associated with it.
 * The {@code Reader} may be implemented in many different ways to populate a rowset
 * with data, including getting data from a non-relational data source. The
 * writer can also be implemented in many different ways to propagate changes
 * made to the rowset's data back to the underlying data source.
 * <P>
 * Rowsets are easy to use.  The {@code RowSet} interface extends the standard
 * {@code java.sql.ResultSet} interface.  The {@code RowSetMetaData}
 * interface extends the {@code java.sql.ResultSetMetaData} interface.
 * Thus, developers familiar
 * with the JDBC API will have to learn a minimal number of new APIs to
 * use rowsets.  In addition, third-party software tools that work with
 * JDBC {@code ResultSet} objects will also easily be made to work with rowsets.
 *
 * @since 1.4
 */

public interface RowSet extends ResultSet {

  //-----------------------------------------------------------------------
  // Properties
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  // The following properties may be used to create a Connection.
  //-----------------------------------------------------------------------

  /**
   * Retrieves the url property this {@code RowSet} object will use to
   * create a connection if it uses the {@code DriverManager}
   * instead of a {@code DataSource} object to establish the connection.
   * The default value is {@code null}.
   *
   * @return a string url
   * @throws SQLException if a database access error occurs
   * @see #setUrl
   */
  String getUrl() throws SQLException;

  /**
   * Sets the URL this {@code RowSet} object will use when it uses the
   * {@code DriverManager} to create a connection.
   *
   * Setting this property is optional.  If a URL is used, a JDBC driver
   * that accepts the URL must be loaded before the
   * rowset is used to connect to a database.  The rowset will use the URL
   * internally to create a database connection when reading or writing
   * data.  Either a URL or a data source name is used to create a
   * connection, whichever was set to non null value most recently.
   *
   * @param url a string value; may be {@code null}
   * @throws SQLException if a database access error occurs
   * @see #getUrl
   */
  void setUrl(String url) throws SQLException;

  /**
   * Retrieves the logical name that identifies the data source for this
   * {@code RowSet} object.
   *
   * @return a data source name
   * @see #setDataSourceName
   * @see #setUrl
   */
  String getDataSourceName();

  /**
   * Sets the data source name property for this {@code RowSet} object to the
   * given {@code String}.
   * <P>
   * The value of the data source name property can be used to do a lookup of
   * a {@code DataSource} object that has been registered with a naming
   * service.  After being retrieved, the {@code DataSource} object can be
   * used to create a connection to the data source that it represents.
   *
   * @param name the logical name of the data source for this {@code RowSet}
   *        object; may be {@code null}
   * @throws SQLException if a database access error occurs
   * @see #getDataSourceName
   */
  void setDataSourceName(String name) throws SQLException;

  /**
   * Retrieves the username used to create a database connection for this
   * {@code RowSet} object.
   * The username property is set at run time before calling the method
   * {@code execute}.  It is
   * not usually part of the serialized state of a {@code RowSet} object.
   *
   * @return the username property
   * @see #setUsername
   */
  String getUsername();

  /**
   * Sets the username property for this {@code RowSet} object to the
   * given {@code String}.
   *
   * @param name a user name
   * @throws SQLException if a database access error occurs
   * @see #getUsername
   */
  void setUsername(String name) throws SQLException;

  /**
   * Retrieves the password used to create a database connection.
   * The password property is set at run time before calling the method
   * {@code execute}.  It is not usually part of the serialized state
   * of a {@code RowSet} object.
   *
   * @return the password for making a database connection
   * @see #setPassword
   */
  String getPassword();

  /**
   * Sets the database password for this {@code RowSet} object to
   * the given {@code String}.
   *
   * @param password the password string
   * @throws SQLException if a database access error occurs
   * @see #getPassword
   */
  void setPassword(String password) throws SQLException;

  /**
   * Retrieves the transaction isolation level set for this
   * {@code RowSet} object.
   *
   * @return the transaction isolation level; one of
   *      {@code Connection.TRANSACTION_READ_UNCOMMITTED},
   *      {@code Connection.TRANSACTION_READ_COMMITTED},
   *      {@code Connection.TRANSACTION_REPEATABLE_READ}, or
   *      {@code Connection.TRANSACTION_SERIALIZABLE}
   * @see #setTransactionIsolation
   */
  int getTransactionIsolation();

  /**
   * Sets the transaction isolation level for this {@code RowSet} object.
   *
   * @param level the transaction isolation level; one of
   *      {@code Connection.TRANSACTION_READ_UNCOMMITTED},
   *      {@code Connection.TRANSACTION_READ_COMMITTED},
   *      {@code Connection.TRANSACTION_REPEATABLE_READ}, or
   *      {@code Connection.TRANSACTION_SERIALIZABLE}
   * @throws SQLException if a database access error occurs
   * @see #getTransactionIsolation
   */
  void setTransactionIsolation(int level) throws SQLException;

  /**
   * Retrieves the {@code Map} object associated with this
   * {@code RowSet} object, which specifies the custom mapping
   * of SQL user-defined types, if any.  The default is for the
   * type map to be empty.
   *
   * @return a {@code java.util.Map} object containing the names of
   *         SQL user-defined types and the Java classes to which they are
   *         to be mapped
   *
   * @throws SQLException if a database access error occurs
   * @see #setTypeMap
   */
   java.util.Map<String,Class<?>> getTypeMap() throws SQLException;

  /**
   * Installs the given {@code java.util.Map} object as the default
   * type map for this {@code RowSet} object. This type map will be
   * used unless another type map is supplied as a method parameter.
   *
   * @param map  a {@code java.util.Map} object containing the names of
   *         SQL user-defined types and the Java classes to which they are
   *         to be mapped
   * @throws SQLException if a database access error occurs
   * @see #getTypeMap
   */
   void setTypeMap(java.util.Map<String,Class<?>> map) throws SQLException;

  //-----------------------------------------------------------------------
  // The following properties may be used to create a Statement.
  //-----------------------------------------------------------------------

  /**
   * Retrieves this {@code RowSet} object's command property.
   *
   * The command property contains a command string, which must be an SQL
   * query, that can be executed to fill the rowset with data.
   * The default value is {@code null}.
   *
   * @return the command string; may be {@code null}
   * @see #setCommand
   */
  String getCommand();

  /**
   * Sets this {@code RowSet} object's command property to the given
   * SQL query.
   *
   * This property is optional
   * when a rowset gets its data from a data source that does not support
   * commands, such as a spreadsheet.
   *
   * @param cmd the SQL query that will be used to get the data for this
   *        {@code RowSet} object; may be {@code null}
   * @throws SQLException if a database access error occurs
   * @see #getCommand
   */
  void setCommand(String cmd) throws SQLException;

  /**
   * Retrieves whether this {@code RowSet} object is read-only.
   * If updates are possible, the default is for a rowset to be
   * updatable.
   * <P>
   * Attempts to update a read-only rowset will result in an
   * {@code SQLException} being thrown.
   *
   * @return {@code true} if this {@code RowSet} object is
   *         read-only; {@code false} if it is updatable
   * @see #setReadOnly
   */
  boolean isReadOnly();

  /**
   * Sets whether this {@code RowSet} object is read-only to the
   * given {@code boolean}.
   *
   * @param value {@code true} if read-only; {@code false} if
   *        updatable
   * @throws SQLException if a database access error occurs
   * @see #isReadOnly
   */
  void setReadOnly(boolean value) throws SQLException;

  /**
   * Retrieves the maximum number of bytes that may be returned
   * for certain column values.
   * This limit applies only to {@code BINARY},
   * {@code VARBINARY}, {@code LONGVARBINARYBINARY}, {@code CHAR},
   * {@code VARCHAR}, {@code LONGVARCHAR}, {@code NCHAR}
   * and {@code NVARCHAR} columns.
   * If the limit is exceeded, the excess data is silently discarded.
   *
   * @return the current maximum column size limit; zero means that there
   *          is no limit
   * @throws SQLException if a database access error occurs
   * @see #setMaxFieldSize
   */
  int getMaxFieldSize() throws SQLException;

  /**
   * Sets the maximum number of bytes that can be returned for a column
   * value to the given number of bytes.
   * This limit applies only to {@code BINARY},
   * {@code VARBINARY}, {@code LONGVARBINARYBINARY}, {@code CHAR},
   * {@code VARCHAR}, {@code LONGVARCHAR}, {@code NCHAR}
   * and {@code NVARCHAR} columns.
   * If the limit is exceeded, the excess data is silently discarded.
   * For maximum portability, use values greater than 256.
   *
   * @param max the new max column size limit in bytes; zero means unlimited
   * @throws SQLException if a database access error occurs
   * @see #getMaxFieldSize
   */
  void setMaxFieldSize(int max) throws SQLException;

  /**
   * Retrieves the maximum number of rows that this {@code RowSet}
   * object can contain.
   * If the limit is exceeded, the excess rows are silently dropped.
   *
   * @return the current maximum number of rows that this {@code RowSet}
   *         object can contain; zero means unlimited
   * @throws SQLException if a database access error occurs
   * @see #setMaxRows
   */
  int getMaxRows() throws SQLException;

  /**
   * Sets the maximum number of rows that this {@code RowSet}
   * object can contain to the specified number.
   * If the limit is exceeded, the excess rows are silently dropped.
   *
   * @param max the new maximum number of rows; zero means unlimited
   * @throws SQLException if a database access error occurs
   * @see #getMaxRows
   */
  void setMaxRows(int max) throws SQLException;

  /**
   * Retrieves whether escape processing is enabled for this
   * {@code RowSet} object.
   * If escape scanning is enabled, which is the default, the driver will do
   * escape substitution before sending an SQL statement to the database.
   *
   * @return {@code true} if escape processing is enabled;
   *         {@code false} if it is disabled
   * @throws SQLException if a database access error occurs
   * @see #setEscapeProcessing
   */
  boolean getEscapeProcessing() throws SQLException;

  /**
   * Sets escape processing for this {@code RowSet} object on or
   * off. If escape scanning is on (the default), the driver will do
   * escape substitution before sending an SQL statement to the database.
   *
   * @param enable {@code true} to enable escape processing;
   *        {@code false} to disable it
   * @throws SQLException if a database access error occurs
   * @see #getEscapeProcessing
   */
  void setEscapeProcessing(boolean enable) throws SQLException;

  /**
   * Retrieves the maximum number of seconds the driver will wait for
   * a statement to execute.
   * If this limit is exceeded, an {@code SQLException} is thrown.
   *
   * @return the current query timeout limit in seconds; zero means
   *          unlimited
   * @throws SQLException if a database access error occurs
   * @see #setQueryTimeout
   */
  int getQueryTimeout() throws SQLException;

  /**
   * Sets the maximum time the driver will wait for
   * a statement to execute to the given number of seconds.
   * If this limit is exceeded, an {@code SQLException} is thrown.
   *
   * @param seconds the new query timeout limit in seconds; zero means
   *        that there is no limit
   * @throws SQLException if a database access error occurs
   * @see #getQueryTimeout
   */
  void setQueryTimeout(int seconds) throws SQLException;

  /**
   * Sets the type of this {@code RowSet} object to the given type.
   * This method is used to change the type of a rowset, which is by
   * default read-only and non-scrollable.
   *
   * @param type one of the {@code ResultSet} constants specifying a type:
   *        {@code ResultSet.TYPE_FORWARD_ONLY},
   *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
   *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
   * @throws SQLException if a database access error occurs
   * @see java.sql.ResultSet#getType
   */
  void setType(int type) throws SQLException;

  /**
   * Sets the concurrency of this {@code RowSet} object to the given
   * concurrency level. This method is used to change the concurrency level
   * of a rowset, which is by default {@code ResultSet.CONCUR_READ_ONLY}
   *
   * @param concurrency one of the {@code ResultSet} constants specifying a
   *        concurrency level:  {@code ResultSet.CONCUR_READ_ONLY} or
   *        {@code ResultSet.CONCUR_UPDATABLE}
   * @throws SQLException if a database access error occurs
   * @see ResultSet#getConcurrency
   */
  void setConcurrency(int concurrency) throws SQLException;

  //-----------------------------------------------------------------------
  // Parameters
  //-----------------------------------------------------------------------

  /**
   * The {@code RowSet} setter methods are used to set any input parameters
   * needed by the {@code RowSet} object's command.
   * Parameters are set at run time, as opposed to design time.
   */

  /**
   * Sets the designated parameter in this {@code RowSet} object's SQL
   * command to SQL {@code NULL}.
   *
   * <P><B>Note:</B> You must specify the parameter's SQL type.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param sqlType a SQL type code defined by {@code java.sql.Types}
   * @throws SQLException if a database access error occurs
   */
  void setNull(int parameterIndex, int sqlType) throws SQLException;

  /**
     * Sets the designated parameter to SQL {@code NULL}.
     *
     * <P><B>Note:</B> You must specify the parameter's SQL type.
     *
     * @param parameterName the name of the parameter
     * @param sqlType the SQL type code defined in {@code java.sql.Types}
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void setNull(String parameterName, int sqlType) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's SQL
   * command to SQL {@code NULL}. This version of the method {@code setNull}
   * should  be used for SQL user-defined types (UDTs) and {@code REF} type
   * parameters.  Examples of UDTs include: {@code STRUCT}, {@code DISTINCT},
   * {@code JAVA_OBJECT}, and named array types.
   *
   * <P><B>Note:</B> To be portable, applications must give the
   * SQL type code and the fully qualified SQL type name when specifying
   * a NULL UDT or {@code REF} parameter.  In the case of a UDT,
   * the name is the type name of the parameter itself.  For a {@code REF}
   * parameter, the name is the type name of the referenced type.  If
   * a JDBC driver does not need the type code or type name information,
   * it may ignore it.
   *
   * Although it is intended for UDT and {@code REF} parameters,
   * this method may be used to set a null parameter of any JDBC type.
   * If the parameter does not have a user-defined or {@code REF} type,
   * the typeName parameter is ignored.
   *
   *
   * @param paramIndex the first parameter is 1, the second is 2, ...
   * @param sqlType a value from {@code java.sql.Types}
   * @param typeName the fully qualified name of an SQL UDT or the type
   *        name of the SQL structured type being referenced by a {@code REF}
   *        type; ignored if the parameter is not a UDT or {@code REF} type
   * @throws SQLException if a database access error occurs
   */
  void setNull (int paramIndex, int sqlType, String typeName)
    throws SQLException;

  /**
     * Sets the designated parameter to SQL {@code NULL}.
     * This version of the method {@code setNull} should
     * be used for user-defined types and REF type parameters.  Examples
     * of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT, and
     * named array types.
     *
     * <P><B>Note:</B> To be portable, applications must give the
     * SQL type code and the fully-qualified SQL type name when specifying
     * a NULL user-defined or REF parameter.  In the case of a user-defined type
     * the name is the type name of the parameter itself.  For a REF
     * parameter, the name is the type name of the referenced type.  If
     * a JDBC driver does not need the type code or type name information,
     * it may ignore it.
     *
     * Although it is intended for user-defined and Ref parameters,
     * this method may be used to set a null parameter of any JDBC type.
     * If the parameter does not have a user-defined or REF type, the given
     * typeName is ignored.
     *
     *
     * @param parameterName the name of the parameter
     * @param sqlType a value from {@code java.sql.Types}
     * @param typeName the fully-qualified name of an SQL user-defined type;
     *        ignored if the parameter is not a user-defined type or
     *        SQL {@code REF} value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void setNull (String parameterName, int sqlType, String typeName)
        throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given Java {@code boolean} value. The driver converts this to
   * an SQL {@code BIT} value before sending it to the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setBoolean(int parameterIndex, boolean x) throws SQLException;

  /**
     * Sets the designated parameter to the given Java {@code boolean} value.
     * The driver converts this
     * to an SQL {@code BIT} or {@code BOOLEAN} value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @see #getBoolean
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void setBoolean(String parameterName, boolean x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given Java {@code byte} value. The driver converts this to
   * an SQL {@code TINYINT} value before sending it to the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setByte(int parameterIndex, byte x) throws SQLException;

  /**
     * Sets the designated parameter to the given Java {@code byte} value.
     * The driver converts this
     * to an SQL {@code TINYINT} value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getByte
     * @since 1.4
     */
    void setByte(String parameterName, byte x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given Java {@code short} value. The driver converts this to
   * an SQL {@code SMALLINT} value before sending it to the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setShort(int parameterIndex, short x) throws SQLException;

  /**
     * Sets the designated parameter to the given Java {@code short} value.
     * The driver converts this
     * to an SQL {@code SMALLINT} value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getShort
     * @since 1.4
     */
    void setShort(String parameterName, short x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given Java {@code int} value. The driver converts this to
   * an SQL {@code INTEGER} value before sending it to the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setInt(int parameterIndex, int x) throws SQLException;

  /**
     * Sets the designated parameter to the given Java {@code int} value.
     * The driver converts this
     * to an SQL {@code INTEGER} value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getInt
     * @since 1.4
     */
    void setInt(String parameterName, int x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given Java {@code long} value. The driver converts this to
   * an SQL {@code BIGINT} value before sending it to the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setLong(int parameterIndex, long x) throws SQLException;

  /**
     * Sets the designated parameter to the given Java {@code long} value.
     * The driver converts this
     * to an SQL {@code BIGINT} value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getLong
     * @since 1.4
     */
    void setLong(String parameterName, long x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given Java {@code float} value. The driver converts this to
   * an SQL {@code REAL} value before sending it to the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setFloat(int parameterIndex, float x) throws SQLException;

  /**
     * Sets the designated parameter to the given Java {@code float} value.
     * The driver converts this
     * to an SQL {@code FLOAT} value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getFloat
     * @since 1.4
     */
    void setFloat(String parameterName, float x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given Java {@code double} value. The driver converts this to
   * an SQL {@code DOUBLE} value before sending it to the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setDouble(int parameterIndex, double x) throws SQLException;

  /**
     * Sets the designated parameter to the given Java {@code double} value.
     * The driver converts this
     * to an SQL {@code DOUBLE} value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getDouble
     * @since 1.4
     */
    void setDouble(String parameterName, double x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given {@code java.math.BigDecimal} value.
   * The driver converts this to
   * an SQL {@code NUMERIC} value before sending it to the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException;

  /**
     * Sets the designated parameter to the given
     * {@code java.math.BigDecimal} value.
     * The driver converts this to an SQL {@code NUMERIC} value when
     * it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getBigDecimal
     * @since 1.4
     */
    void setBigDecimal(String parameterName, BigDecimal x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given Java {@code String} value. Before sending it to the
   * database, the driver converts this to an SQL {@code VARCHAR} or
   * {@code LONGVARCHAR} value, depending on the argument's size relative
   * to the driver's limits on {@code VARCHAR} values.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setString(int parameterIndex, String x) throws SQLException;

  /**
     * Sets the designated parameter to the given Java {@code String} value.
     * The driver converts this
     * to an SQL {@code VARCHAR} or {@code LONGVARCHAR} value
     * (depending on the argument's
     * size relative to the driver's limits on {@code VARCHAR} values)
     * when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getString
     * @since 1.4
     */
    void setString(String parameterName, String x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given Java array of {@code byte} values. Before sending it to the
   * database, the driver converts this to an SQL {@code VARBINARY} or
   * {@code LONGVARBINARY} value, depending on the argument's size relative
   * to the driver's limits on {@code VARBINARY} values.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setBytes(int parameterIndex, byte x[]) throws SQLException;

  /**
     * Sets the designated parameter to the given Java array of bytes.
     * The driver converts this to an SQL {@code VARBINARY} or
     * {@code LONGVARBINARY} (depending on the argument's size relative
     * to the driver's limits on {@code VARBINARY} values) when it sends
     * it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getBytes
     * @since 1.4
     */
    void setBytes(String parameterName, byte x[]) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given {@code java.sql.Date} value. The driver converts this to
   * an SQL {@code DATE} value before sending it to the database, using the
   * default {@code java.util.Calendar} to calculate the date.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setDate(int parameterIndex, java.sql.Date x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given {@code java.sql.Time} value. The driver converts this to
   * an SQL {@code TIME} value before sending it to the database, using the
   * default {@code java.util.Calendar} to calculate it.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setTime(int parameterIndex, java.sql.Time x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given {@code java.sql.Timestamp} value. The driver converts this to
   * an SQL {@code TIMESTAMP} value before sending it to the database, using the
   * default {@code java.util.Calendar} to calculate it.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   */
  void setTimestamp(int parameterIndex, java.sql.Timestamp x)
    throws SQLException;

  /**
     * Sets the designated parameter to the given {@code java.sql.Timestamp} value.
     * The driver
     * converts this to an SQL {@code TIMESTAMP} value when it sends it to the
     * database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getTimestamp
     * @since 1.4
     */
    void setTimestamp(String parameterName, java.sql.Timestamp x)
        throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given {@code java.io.InputStream} value.
   * It may be more practical to send a very large ASCII value via a
   * {@code java.io.InputStream} rather than as a {@code LONGVARCHAR}
   * parameter. The driver will read the data from the stream
   * as needed until it reaches end-of-file.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the Java input stream that contains the ASCII parameter value
   * @param length the number of bytes in the stream
   * @throws SQLException if a database access error occurs
   */
  void setAsciiStream(int parameterIndex, java.io.InputStream x, int length)
    throws SQLException;

  /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large ASCII value is input to a {@code LONGVARCHAR}
     * parameter, it may be more practical to send it via a
     * {@code java.io.InputStream}. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterName the name of the parameter
     * @param x the Java input stream that contains the ASCII parameter value
     * @param length the number of bytes in the stream
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void setAsciiStream(String parameterName, java.io.InputStream x, int length)
        throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given {@code java.io.InputStream} value.
   * It may be more practical to send a very large binary value via a
   * {@code java.io.InputStream} rather than as a {@code LONGVARBINARY}
   * parameter. The driver will read the data from the stream
   * as needed until it reaches end-of-file.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the java input stream which contains the binary parameter value
   * @param length the number of bytes in the stream
   * @throws SQLException if a database access error occurs
   */
  void setBinaryStream(int parameterIndex, java.io.InputStream x,
                       int length) throws SQLException;

  /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large binary value is input to a {@code LONGVARBINARY}
     * parameter, it may be more practical to send it via a
     * {@code java.io.InputStream} object. The data will be read from the stream
     * as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterName the name of the parameter
     * @param x the java input stream which contains the binary parameter value
     * @param length the number of bytes in the stream
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void setBinaryStream(String parameterName, java.io.InputStream x,
                         int length) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given {@code java.io.Reader} value.
   * It may be more practical to send a very large UNICODE value via a
   * {@code java.io.Reader} rather than as a {@code LONGVARCHAR}
   * parameter. The driver will read the data from the stream
   * as needed until it reaches end-of-file.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param reader the {@code Reader} object that contains the UNICODE data
   *        to be set
   * @param length the number of characters in the stream
   * @throws SQLException if a database access error occurs
   */
  void setCharacterStream(int parameterIndex,
                          Reader reader,
                          int length) throws SQLException;

  /**
     * Sets the designated parameter to the given {@code Reader}
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a {@code LONGVARCHAR}
     * parameter, it may be more practical to send it via a
     * {@code java.io.Reader} object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterName the name of the parameter
     * @param reader the {@code java.io.Reader} object that
     *        contains the UNICODE data used as the designated parameter
     * @param length the number of characters in the stream
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void setCharacterStream(String parameterName,
                            java.io.Reader reader,
                            int length) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given input stream.
   * When a very large ASCII value is input to a {@code LONGVARCHAR}
   * parameter, it may be more practical to send it via a
   * {@code java.io.InputStream}. Data will be read from the stream
   * as needed until end-of-file is reached.  The JDBC driver will
   * do any necessary conversion from ASCII to the database char format.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code setAsciiStream} which takes a length parameter.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the Java input stream that contains the ASCII parameter value
   * @throws SQLException if a database access error occurs or
   * this method is called on a closed {@code PreparedStatement}
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
   */
  void setAsciiStream(int parameterIndex, java.io.InputStream x)
                      throws SQLException;

   /**
     * Sets the designated parameter to the given input stream.
     * When a very large ASCII value is input to a {@code LONGVARCHAR}
     * parameter, it may be more practical to send it via a
     * {@code java.io.InputStream}. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setAsciiStream} which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param x the Java input stream that contains the ASCII parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
       * @since 1.6
    */
    void setAsciiStream(String parameterName, java.io.InputStream x)
            throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given input stream.
   * When a very large binary value is input to a {@code LONGVARBINARY}
   * parameter, it may be more practical to send it via a
   * {@code java.io.InputStream} object. The data will be read from the
   * stream as needed until end-of-file is reached.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code setBinaryStream} which takes a length parameter.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the java input stream which contains the binary parameter value
   * @throws SQLException if a database access error occurs or
   * this method is called on a closed {@code PreparedStatement}
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
   */
  void setBinaryStream(int parameterIndex, java.io.InputStream x)
                       throws SQLException;

  /**
     * Sets the designated parameter to the given input stream.
     * When a very large binary value is input to a {@code LONGVARBINARY}
     * parameter, it may be more practical to send it via a
     * {@code java.io.InputStream} object. The data will be read from the
     * stream as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setBinaryStream} which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param x the java input stream which contains the binary parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
    void setBinaryStream(String parameterName, java.io.InputStream x)
    throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to the given {@code Reader}
   * object.
   * When a very large UNICODE value is input to a {@code LONGVARCHAR}
   * parameter, it may be more practical to send it via a
   * {@code java.io.Reader} object. The data will be read from the stream
   * as needed until end-of-file is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code setCharacterStream} which takes a length parameter.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param reader the {@code java.io.Reader} object that contains the
   *        Unicode data
   * @throws SQLException if a database access error occurs or
   * this method is called on a closed {@code PreparedStatement}
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
   */
  void setCharacterStream(int parameterIndex,
                          java.io.Reader reader) throws SQLException;

  /**
     * Sets the designated parameter to the given {@code Reader}
     * object.
     * When a very large UNICODE value is input to a {@code LONGVARCHAR}
     * parameter, it may be more practical to send it via a
     * {@code java.io.Reader} object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setCharacterStream} which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param reader the {@code java.io.Reader} object that contains the
     *        Unicode data
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
    void setCharacterStream(String parameterName,
                          java.io.Reader reader) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * to a {@code Reader} object. The
   * {@code Reader} reads the data till end-of-file is reached. The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.

   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code setNCharacterStream} which takes a length parameter.
   *
   * @param parameterIndex of the first parameter is 1, the second is 2, ...
   * @param value the parameter value
   * @throws SQLException if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur ; if a database access error occurs; or
   * this method is called on a closed {@code PreparedStatement}
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
   */
   void setNCharacterStream(int parameterIndex, Reader value) throws SQLException;



  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with the given Java {@code Object}.  For integral values, the
   * {@code java.lang} equivalent objects should be used (for example,
   * an instance of the class {@code Integer} for an {@code int}).
   *
   * If the second argument is an {@code InputStream} then the stream must contain
   * the number of bytes specified by scaleOrLength.  If the second argument is a
   * {@code Reader} then the {@code Reader} must contain the number of characters specified
   * by scaleOrLength. If these conditions are not true the driver will generate a
   * {@code SQLException} when the prepared statement is executed.
   *
   * <p>The given Java object will be converted to the targetSqlType
   * before being sent to the database.
   * <P>
   * If the object is of a class implementing {@code SQLData},
   * the rowset should call the method {@code SQLData.writeSQL}
   * to write the object to an {@code SQLOutput} data stream.
   * If, on the other hand, the object is of a class implementing
   * {@code Ref}, {@code Blob}, {@code Clob},  {@code NClob},
   *  {@code Struct}, {@code java.net.URL},
   * or {@code Array}, the driver should pass it to the database as a
   * value of the corresponding SQL type.
   *
   *
   * <p>Note that this method may be used to pass database-specific
   * abstract data types.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the object containing the input parameter value
   * @param targetSqlType the SQL type (as defined in {@code java.sql.Types})
   *        to be sent to the database. The scale argument may further qualify this
   *        type.
   * @param scaleOrLength for {@code java.sql.Types.DECIMAL}
   *          or {@code java.sql.Types.NUMERIC types},
   *          this is the number of digits after the decimal point. For
   *          Java Object types {@code InputStream} and {@code Reader},
   *          this is the length
   *          of the data in the stream or {@code Reader}.  For all other types,
   *          this value will be ignored.
   * @throws SQLException if a database access error occurs
   * @see java.sql.Types
   */
  void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
            throws SQLException;

  /**
     * Sets the value of the designated parameter with the given object. The second
     * argument must be an object type; for integral values, the
     * {@code java.lang} equivalent objects should be used.
     *
     * <p>The given Java object will be converted to the given targetSqlType
     * before being sent to the database.
     *
     * If the object has a custom mapping (is of a class implementing the
     * interface {@code SQLData}),
     * the JDBC driver should call the method {@code SQLData.writeSQL} to write it
     * to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * {@code Ref}, {@code Blob}, {@code Clob},  {@code NClob},
     *  {@code Struct}, {@code java.net.URL},
     * or {@code Array}, the driver should pass it to the database as a
     * value of the corresponding SQL type.
     * <P>
     * Note that this method may be used to pass database-
     * specific abstract data types.
     *
     * @param parameterName the name of the parameter
     * @param x the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
     * sent to the database. The scale argument may further qualify this type.
     * @param scale for java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types,
     *          this is the number of digits after the decimal point.  For all other
     *          types, this value will be ignored.
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if {@code targetSqlType} is
     * a {@code ARRAY}, {@code BLOB}, {@code CLOB},
     * {@code DATALINK}, {@code JAVA_OBJECT}, {@code NCHAR},
     * {@code NCLOB}, {@code NVARCHAR}, {@code LONGNVARCHAR},
     *  {@code REF}, {@code ROWID}, {@code SQLXML}
     * or  {@code STRUCT} data type and the JDBC driver does not support
     * this data type
     * @see Types
     * @see #getObject
     * @since 1.4
     */
    void setObject(String parameterName, Object x, int targetSqlType, int scale)
        throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with a Java {@code Object}.  For integral values, the
   * {@code java.lang} equivalent objects should be used.
   * This method is like {@code setObject} above, but the scale used is the scale
   * of the second parameter.  Scalar values have a scale of zero.  Literal
   * values have the scale present in the literal.
   * <P>
   * Even though it is supported, it is not recommended that this method
   * be called with floating point input values.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the object containing the input parameter value
   * @param targetSqlType the SQL type (as defined in {@code java.sql.Types})
   *        to be sent to the database
   * @throws SQLException if a database access error occurs
   */
  void setObject(int parameterIndex, Object x,
                 int targetSqlType) throws SQLException;

  /**
     * Sets the value of the designated parameter with the given object.
     * This method is like the method {@code setObject}
     * above, except that it assumes a scale of zero.
     *
     * @param parameterName the name of the parameter
     * @param x the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
     *                      sent to the database
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if {@code targetSqlType} is
     * a {@code ARRAY}, {@code BLOB}, {@code CLOB},
     * {@code DATALINK}, {@code JAVA_OBJECT}, {@code NCHAR},
     * {@code NCLOB}, {@code NVARCHAR}, {@code LONGNVARCHAR},
     *  {@code REF}, {@code ROWID}, {@code SQLXML}
     * or  {@code STRUCT} data type and the JDBC driver does not support
     * this data type
     * @see #getObject
     * @since 1.4
     */
    void setObject(String parameterName, Object x, int targetSqlType)
        throws SQLException;

   /**
     * Sets the value of the designated parameter with the given object.
     * The second parameter must be of type {@code Object}; therefore, the
     * {@code java.lang} equivalent objects should be used for built-in types.
     *
     * <p>The JDBC specification specifies a standard mapping from
     * Java {@code Object} types to SQL types.  The given argument
     * will be converted to the corresponding SQL type before being
     * sent to the database.
     *
     * <p>Note that this method may be used to pass database-
     * specific abstract data types, by using a driver-specific Java
     * type.
     *
     * If the object is of a class implementing the interface {@code SQLData},
     * the JDBC driver should call the method {@code SQLData.writeSQL}
     * to write it to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * {@code Ref}, {@code Blob}, {@code Clob},  {@code NClob},
     *  {@code Struct}, {@code java.net.URL},
     * or {@code Array}, the driver should pass it to the database as a
     * value of the corresponding SQL type.
     * <P>
     * This method throws an exception if there is an ambiguity, for example, if the
     * object is of a class implementing more than one of the interfaces named above.
     *
     * @param parameterName the name of the parameter
     * @param x the object containing the input parameter value
     * @throws SQLException if a database access error occurs,
     * this method is called on a closed {@code CallableStatement} or if the given
     *            {@code Object} parameter is ambiguous
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getObject
     * @since 1.4
     */
    void setObject(String parameterName, Object x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with a Java {@code Object}.  For integral values, the
   * {@code java.lang} equivalent objects should be used.
   *
   * <p>The JDBC specification provides a standard mapping from
   * Java Object types to SQL types.  The driver will convert the
   * given Java object to its standard SQL mapping before sending it
   * to the database.
   *
   * <p>Note that this method may be used to pass database-specific
   * abstract data types by using a driver-specific Java type.
   *
   * If the object is of a class implementing {@code SQLData},
   * the rowset should call the method {@code SQLData.writeSQL}
   * to write the object to an {@code SQLOutput} data stream.
   * If, on the other hand, the object is of a class implementing
   * {@code Ref}, {@code Blob}, {@code Clob},  {@code NClob},
   *  {@code Struct}, {@code java.net.URL},
   * or {@code Array}, the driver should pass it to the database as a
   * value of the corresponding SQL type.
   *
   * <P>
   * An exception is thrown if there is an ambiguity, for example, if the
   * object is of a class implementing more than one of these interfaces.
   *
   * @param parameterIndex The first parameter is 1, the second is 2, ...
   * @param x The object containing the input parameter value
   * @throws SQLException if a database access error occurs
   */
  void setObject(int parameterIndex, Object x) throws SQLException;


  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with the given  {@code Ref} value.  The driver will convert this
   * to the appropriate {@code REF(<structured-type>)} value.
   *
   * @param i the first parameter is 1, the second is 2, ...
   * @param x an object representing data of an SQL {@code REF} type
   * @throws SQLException if a database access error occurs
   */
  void setRef (int i, Ref x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with the given  {@code Blob} value.  The driver will convert this
   * to the {@code BLOB} value that the {@code Blob} object
   * represents before sending it to the database.
   *
   * @param i the first parameter is 1, the second is 2, ...
   * @param x an object representing a BLOB
   * @throws SQLException if a database access error occurs
   */
  void setBlob (int i, Blob x) throws SQLException;

  /**
     * Sets the designated parameter to a {@code InputStream} object.
     * The {@code InputStream} must contain  the number
     * of characters specified by length otherwise a {@code SQLException} will be
     * generated when the {@code PreparedStatement} is executed.
     * This method differs from the {@code setBinaryStream (int, InputStream, int)}
     * method because it informs the driver that the parameter value should be
     * sent to the server as a {@code BLOB}.  When the {@code setBinaryStream} method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a {@code LONGVARBINARY} or a {@code BLOB}
     * @param parameterIndex index of the first parameter is 1,
     * the second is 2, ...
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @param length the number of bytes in the parameter data.
     * @throws SQLException if a database access error occurs,
     * this method is called on a closed {@code PreparedStatement},
     * if parameterIndex does not correspond
     * to a parameter marker in the SQL statement,  if the length specified
     * is less than zero or if the number of bytes in the {@code InputStream} does not match
     * the specified length.
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setBlob(int parameterIndex, InputStream inputStream, long length)
        throws SQLException;

  /**
     * Sets the designated parameter to a {@code InputStream} object.
     * This method differs from the {@code setBinaryStream (int, InputStream)}
     * method because it informs the driver that the parameter value should be
     * sent to the server as a {@code BLOB}.  When the {@code setBinaryStream} method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a {@code LONGVARBINARY} or a {@code BLOB}
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setBlob} which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1,
     * the second is 2, ...
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @throws SQLException if a database access error occurs,
     * this method is called on a closed {@code PreparedStatement} or
     * if parameterIndex does not correspond
     * to a parameter marker in the SQL statement,
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setBlob(int parameterIndex, InputStream inputStream)
        throws SQLException;

  /**
     * Sets the designated parameter to a {@code InputStream} object.
     * The {@code InputStream} must contain  the number
     * of characters specified by length, otherwise a {@code SQLException} will be
     * generated when the {@code CallableStatement} is executed.
     * This method differs from the {@code setBinaryStream (int, InputStream, int)}
     * method because it informs the driver that the parameter value should be
     * sent to the server as a {@code BLOB}.  When the {@code setBinaryStream} method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a {@code LONGVARBINARY} or a {@code BLOB}
     *
     * @param parameterName the name of the parameter to be set
     * the second is 2, ...
     *
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @param length the number of bytes in the parameter data.
     * @throws SQLException  if parameterIndex does not correspond
     * to a parameter marker in the SQL statement,  or if the length specified
     * is less than zero; if the number of bytes in the {@code InputStream} does not match
     * the specified length; if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     *
     * @since 1.6
     */
     void setBlob(String parameterName, InputStream inputStream, long length)
        throws SQLException;

  /**
     * Sets the designated parameter to the given {@code java.sql.Blob} object.
     * The driver converts this to an SQL {@code BLOB} value when it
     * sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x a {@code Blob} object that maps an SQL {@code BLOB} value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void setBlob (String parameterName, Blob x) throws SQLException;

  /**
     * Sets the designated parameter to a {@code InputStream} object.
     * This method differs from the {@code setBinaryStream (int, InputStream)}
     * method because it informs the driver that the parameter value should be
     * sent to the server as a {@code BLOB}.  When the {@code setBinaryStream} method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a {@code LONGVARBINARY} or a {@code BLOB}
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setBlob} which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setBlob(String parameterName, InputStream inputStream)
        throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with the given  {@code Clob} value.  The driver will convert this
   * to the {@code CLOB} value that the {@code Clob} object
   * represents before sending it to the database.
   *
   * @param i the first parameter is 1, the second is 2, ...
   * @param x an object representing a CLOB
   * @throws SQLException if a database access error occurs
   */
  void setClob (int i, Clob x) throws SQLException;

  /**
     * Sets the designated parameter to a {@code Reader} object.
     * The {@code Reader} must contain  the number
     * of characters specified by length otherwise a {@code SQLException} will be
     * generated when the {@code PreparedStatement} is executed.
     *This method differs from the {@code setCharacterStream (int, Reader, int)} method
     * because it informs the driver that the parameter value should be sent to
     * the server as a {@code CLOB}.  When the {@code setCharacterStream} method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a {@code LONGVARCHAR} or a {@code CLOB}
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if a database access error occurs, this method is called on
     * a closed {@code PreparedStatement}, if parameterIndex does not correspond to a parameter
     * marker in the SQL statement, or if the length specified is less than zero.
     *
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setClob(int parameterIndex, Reader reader, long length)
       throws SQLException;

  /**
     * Sets the designated parameter to a {@code Reader} object.
     * This method differs from the {@code setCharacterStream (int, Reader)} method
     * because it informs the driver that the parameter value should be sent to
     * the server as a {@code CLOB}.  When the {@code setCharacterStream} method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a {@code LONGVARCHAR} or a {@code CLOB}
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setClob} which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if a database access error occurs, this method is called on
     * a closed {@code PreparedStatement}or if parameterIndex does not correspond to a parameter
     * marker in the SQL statement
     *
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setClob(int parameterIndex, Reader reader)
       throws SQLException;

  /**
     * Sets the designated parameter to a {@code Reader} object.  The
     * {@code Reader} must contain  the number
     * of characters specified by length otherwise a {@code SQLException} will be
     * generated when the {@code CallableStatement} is executed.
     * This method differs from the {@code setCharacterStream (int, Reader, int)} method
     * because it informs the driver that the parameter value should be sent to
     * the server as a {@code CLOB}.  When the {@code setCharacterStream} method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a {@code LONGVARCHAR} or a {@code CLOB}
     * @param parameterName the name of the parameter to be set
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if the length specified is less than zero;
     * a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     *
     * @since 1.6
     */
     void setClob(String parameterName, Reader reader, long length)
       throws SQLException;

   /**
     * Sets the designated parameter to the given {@code java.sql.Clob} object.
     * The driver converts this to an SQL {@code CLOB} value when it
     * sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x a {@code Clob} object that maps an SQL {@code CLOB} value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void setClob (String parameterName, Clob x) throws SQLException;

  /**
     * Sets the designated parameter to a {@code Reader} object.
     * This method differs from the {@code setCharacterStream (int, Reader)} method
     * because it informs the driver that the parameter value should be sent to
     * the server as a {@code CLOB}.  When the {@code setCharacterStream} method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a {@code LONGVARCHAR} or a {@code CLOB}
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setClob} which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if a database access error occurs or this method is called on
     * a closed {@code CallableStatement}
     *
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setClob(String parameterName, Reader reader)
       throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with the given  {@code Array} value.  The driver will convert this
   * to the {@code ARRAY} value that the {@code Array} object
   * represents before sending it to the database.
   *
   * @param i the first parameter is 1, the second is 2, ...
   * @param x an object representing an SQL array
   * @throws SQLException if a database access error occurs
   */
  void setArray (int i, Array x) throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with the given  {@code java.sql.Date} value.  The driver will convert this
   * to an SQL {@code DATE} value, using the given {@code java.util.Calendar}
   * object to calculate the date.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @param cal the {@code java.util.Calendar} object to use for calculating the date
   * @throws SQLException if a database access error occurs
   */
  void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
    throws SQLException;

  /**
     * Sets the designated parameter to the given {@code java.sql.Date} value
     * using the default time zone of the virtual machine that is running
     * the application.
     * The driver converts this
     * to an SQL {@code DATE} value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getDate
     * @since 1.4
     */
    void setDate(String parameterName, java.sql.Date x)
        throws SQLException;

  /**
     * Sets the designated parameter to the given {@code java.sql.Date} value,
     * using the given {@code Calendar} object.  The driver uses
     * the {@code Calendar} object to construct an SQL {@code DATE} value,
     * which the driver then sends to the database.  With a
     * a {@code Calendar} object, the driver can calculate the date
     * taking into account a custom timezone.  If no
     * {@code Calendar} object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @param cal the {@code Calendar} object the driver will use
     *            to construct the date
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getDate
     * @since 1.4
     */
    void setDate(String parameterName, java.sql.Date x, Calendar cal)
        throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with the given  {@code java.sql.Time} value.  The driver will convert this
   * to an SQL {@code TIME} value, using the given {@code java.util.Calendar}
   * object to calculate it, before sending it to the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @param cal the {@code java.util.Calendar} object to use for calculating the time
   * @throws SQLException if a database access error occurs
   */
  void setTime(int parameterIndex, java.sql.Time x, Calendar cal)
    throws SQLException;

  /**
     * Sets the designated parameter to the given {@code java.sql.Time} value.
     * The driver converts this
     * to an SQL {@code TIME} value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getTime
     * @since 1.4
     */
    void setTime(String parameterName, java.sql.Time x)
        throws SQLException;

  /**
     * Sets the designated parameter to the given {@code java.sql.Time} value,
     * using the given {@code Calendar} object.  The driver uses
     * the {@code Calendar} object to construct an SQL {@code TIME} value,
     * which the driver then sends to the database.  With a
     * a {@code Calendar} object, the driver can calculate the time
     * taking into account a custom timezone.  If no
     * {@code Calendar} object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @param cal the {@code Calendar} object the driver will use
     *            to construct the time
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getTime
     * @since 1.4
     */
    void setTime(String parameterName, java.sql.Time x, Calendar cal)
        throws SQLException;

  /**
   * Sets the designated parameter in this {@code RowSet} object's command
   * with the given  {@code java.sql.Timestamp} value.  The driver will
   * convert this to an SQL {@code TIMESTAMP} value, using the given
   * {@code java.util.Calendar} object to calculate it, before sending it to the
   * database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @param cal the {@code java.util.Calendar} object to use for calculating the
   *        timestamp
   * @throws SQLException if a database access error occurs
   */
  void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal)
    throws SQLException;

  /**
     * Sets the designated parameter to the given {@code java.sql.Timestamp} value,
     * using the given {@code Calendar} object.  The driver uses
     * the {@code Calendar} object to construct an SQL {@code TIMESTAMP} value,
     * which the driver then sends to the database.  With a
     * a {@code Calendar} object, the driver can calculate the timestamp
     * taking into account a custom timezone.  If no
     * {@code Calendar} object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterName the name of the parameter
     * @param x the parameter value
     * @param cal the {@code Calendar} object the driver will use
     *            to construct the timestamp
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see #getTimestamp
     * @since 1.4
     */
    void setTimestamp(String parameterName, java.sql.Timestamp x, Calendar cal)
        throws SQLException;

  /**
   * Clears the parameters set for this {@code RowSet} object's command.
   * <P>In general, parameter values remain in force for repeated use of a
   * {@code RowSet} object. Setting a parameter value automatically clears its
   * previous value.  However, in some cases it is useful to immediately
   * release the resources used by the current parameter values, which can
   * be done by calling the method {@code clearParameters}.
   *
   * @throws SQLException if a database access error occurs
   */
  void clearParameters() throws SQLException;

  //---------------------------------------------------------------------
  // Reading and writing data
  //---------------------------------------------------------------------

  /**
   * Fills this {@code RowSet} object with data.
   * <P>
   * The {@code execute} method may use the following properties
   * to create a connection for reading data: url, data source name,
   * user name, password, transaction isolation, and type map.
   *
   * The {@code execute} method  may use the following properties
   * to create a statement to execute a command:
   * command, read only, maximum field size,
   * maximum rows, escape processing, and query timeout.
   * <P>
   * If the required properties have not been set, an exception is
   * thrown.  If this method is successful, the current contents of the rowset are
   * discarded and the rowset's metadata is also (re)set.  If there are
   * outstanding updates, they are ignored.
   * <P>
   * If this {@code RowSet} object does not maintain a continuous connection
   * with its source of data, it may use a {@code Reader} (a {@code RowSetReader}
   * object) to fill itself with data.  In this case, a {@code Reader} will have been
   * registered with this {@code RowSet} object, and the method
   * {@code execute} will call on the {@code Reader}'s {@code readData}
   * method as part of its implementation.
   *
   * @throws SQLException if a database access error occurs or any of the
   *         properties necessary for making a connection and creating
   *         a statement have not been set
   */
  void execute() throws SQLException;

  //--------------------------------------------------------------------
  // Events
  //--------------------------------------------------------------------

  /**
   * Registers the given listener so that it will be notified of events
   * that occur on this {@code RowSet} object.
   *
   * @param listener a component that has implemented the {@code RowSetListener}
   *        interface and wants to be notified when events occur on this
   *        {@code RowSet} object
   * @see #removeRowSetListener
   */
  void addRowSetListener(RowSetListener listener);

  /**
   * Removes the specified listener from the list of components that will be
   * notified when an event occurs on this {@code RowSet} object.
   *
   * @param listener a component that has been registered as a listener for this
   *        {@code RowSet} object
   * @see #addRowSetListener
   */
  void removeRowSetListener(RowSetListener listener);

    /**
      * Sets the designated parameter to the given {@code java.sql.SQLXML} object. The driver converts this to an
      * SQL {@code XML} value when it sends it to the database.
      * @param parameterIndex index of the first parameter is 1, the second is 2, ...
      * @param xmlObject a {@code SQLXML} object that maps an SQL {@code XML} value
      * @throws SQLException if a database access error occurs, this method
      *  is called on a closed result set,
      * the {@code java.xml.transform.Result},
      *  {@code Writer} or {@code OutputStream} has not been closed
      * for the {@code SQLXML} object  or
      *  if there is an error processing the XML value.  The {@code getCause} method
      *  of the exception may provide a more detailed exception, for example, if the
      *  stream does not contain valid XML.
      * @since 1.6
      */
     void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException;

    /**
     * Sets the designated parameter to the given {@code java.sql.SQLXML} object. The driver converts this to an
     * {@code SQL XML} value when it sends it to the database.
     * @param parameterName the name of the parameter
     * @param xmlObject a {@code SQLXML} object that maps an {@code SQL XML} value
     * @throws SQLException if a database access error occurs, this method
     *  is called on a closed result set,
     * the {@code java.xml.transform.Result},
     *  {@code Writer} or {@code OutputStream} has not been closed
     * for the {@code SQLXML} object  or
     *  if there is an error processing the XML value.  The {@code getCause} method
     *  of the exception may provide a more detailed exception, for example, if the
     *  stream does not contain valid XML.
     * @since 1.6
     */
    void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException;

    /**
     * Sets the designated parameter to the given {@code java.sql.RowId} object. The
     * driver converts this to a SQL {@code ROWID} value when it sends it
     * to the database
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @throws SQLException if a database access error occurs
     *
     * @since 1.6
     */
    void setRowId(int parameterIndex, RowId x) throws SQLException;

    /**
    * Sets the designated parameter to the given {@code java.sql.RowId} object. The
    * driver converts this to a SQL {@code ROWID} when it sends it to the
    * database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @throws SQLException if a database access error occurs
    * @since 1.6
    */
   void setRowId(String parameterName, RowId x) throws SQLException;

    /**
     * Sets the designated parameter to the given {@code String} object.
     * The driver converts this to a SQL {@code NCHAR} or
     * {@code NVARCHAR} or {@code LONGNVARCHAR} value
     * (depending on the argument's
     * size relative to the driver's limits on {@code NVARCHAR} values)
     * when it sends it to the database.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value the parameter value
     * @throws SQLException if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur ; or if a database access error occurs
     * @since 1.6
     */
     void setNString(int parameterIndex, String value) throws SQLException;

    /**
     * Sets the designated parameter to the given {@code String} object.
     * The driver converts this to a SQL {@code NCHAR} or
     * {@code NVARCHAR} or {@code LONGNVARCHAR}
     * @param parameterName the name of the column to be set
     * @param value the parameter value
     * @throws SQLException if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; or if a database access error occurs
     * @since 1.6
     */
    public void setNString(String parameterName, String value)
            throws SQLException;

    /**
     * Sets the designated parameter to a {@code Reader} object. The
     * {@code Reader} reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value the parameter value
     * @param length the number of characters in the parameter data.
     * @throws SQLException if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur ; or if a database access error occurs
     * @since 1.6
     */
     void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException;

    /**
     * Sets the designated parameter to a {@code Reader} object. The
     * {@code Reader} reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * @param parameterName the name of the column to be set
     * @param value the parameter value
     * @param length the number of characters in the parameter data.
     * @throws SQLException if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; or if a database access error occurs
     * @since 1.6
     */
    public void setNCharacterStream(String parameterName, Reader value, long length)
            throws SQLException;

    /**
     * Sets the designated parameter to a {@code Reader} object. The
     * {@code Reader} reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.

     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setNCharacterStream} which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param value the parameter value
     * @throws SQLException if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur ; if a database access error occurs; or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setNCharacterStream(String parameterName, Reader value) throws SQLException;

    /**
    * Sets the designated parameter to a {@code java.sql.NClob} object. The object
    * implements the {@code java.sql.NClob} interface. This {@code NClob}
    * object maps to a SQL {@code NCLOB}.
    * @param parameterName the name of the column to be set
    * @param value the parameter value
    * @throws SQLException if the driver does not support national
    *         character sets;  if the driver can detect that a data conversion
    *  error could occur; or if a database access error occurs
    * @since 1.6
    */
    void setNClob(String parameterName, NClob value) throws SQLException;

    /**
     * Sets the designated parameter to a {@code Reader} object.
     * The {@code Reader} must contain  the number
     * of characters specified by length otherwise a {@code SQLException} will be
     * generated when the {@code CallableStatement} is executed.
     * This method differs from the {@code setCharacterStream (int, Reader, int)} method
     * because it informs the driver that the parameter value should be sent to
     * the server as a {@code NCLOB}.  When the {@code setCharacterStream} method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a {@code LONGNVARCHAR} or a {@code NCLOB}
     *
     * @param parameterName the name of the parameter to be set
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if the length specified is less than zero;
     * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
     void setNClob(String parameterName, Reader reader, long length)
       throws SQLException;

    /**
     * Sets the designated parameter to a {@code Reader} object.
     * This method differs from the {@code setCharacterStream (int, Reader)} method
     * because it informs the driver that the parameter value should be sent to
     * the server as a {@code NCLOB}.  When the {@code setCharacterStream} method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a {@code LONGNVARCHAR} or a {@code NCLOB}
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setNClob} which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if the driver does not support national character sets;
     * if the driver can detect that a data conversion
     *  error could occur;  if a database access error occurs or
     * this method is called on a closed {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setNClob(String parameterName, Reader reader)
       throws SQLException;

    /**
     * Sets the designated parameter to a {@code Reader} object.
     * The {@code Reader} must contain  the number
     * of characters specified by length otherwise a {@code SQLException} will be
     * generated when the {@code PreparedStatement} is executed.
     * This method differs from the {@code setCharacterStream (int, Reader, int)} method
     * because it informs the driver that the parameter value should be sent to
     * the server as a {@code NCLOB}.  When the {@code setCharacterStream} method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a {@code LONGNVARCHAR} or a {@code NCLOB}
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if the length specified is less than zero;
     * if the driver does not support national character sets;
     * if the driver can detect that a data conversion
     *  error could occur;  if a database access error occurs or
     * this method is called on a closed {@code PreparedStatement}
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setNClob(int parameterIndex, Reader reader, long length)
       throws SQLException;

    /**
     * Sets the designated parameter to a {@code java.sql.NClob} object. The driver converts this to a
     * SQL {@code NCLOB} value when it sends it to the database.
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value the parameter value
     * @throws SQLException if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur ; or if a database access error occurs
     * @since 1.6
     */
     void setNClob(int parameterIndex, NClob value) throws SQLException;

    /**
     * Sets the designated parameter to a {@code Reader} object.
     * This method differs from the {@code setCharacterStream (int, Reader)} method
     * because it informs the driver that the parameter value should be sent to
     * the server as a {@code NCLOB}.  When the {@code setCharacterStream} method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a {@code LONGNVARCHAR} or a {@code NCLOB}
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * {@code setNClob} which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement;
     * if the driver does not support national character sets;
     * if the driver can detect that a data conversion
     *  error could occur;  if a database access error occurs or
     * this method is called on a closed {@code PreparedStatement}
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setNClob(int parameterIndex, Reader reader)
       throws SQLException;

    /**
     * Sets the designated parameter to the given {@code java.net.URL} value.
     * The driver converts this to an SQL {@code DATALINK} value
     * when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the {@code java.net.URL} object to be set
     * @throws SQLException if a database access error occurs or
     * this method is called on a closed {@code PreparedStatement}
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.4
     */
    void setURL(int parameterIndex, java.net.URL x) throws SQLException;



}
