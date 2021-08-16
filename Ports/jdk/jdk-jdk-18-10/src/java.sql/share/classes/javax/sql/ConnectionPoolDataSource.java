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

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;


/**
 * A factory for {@code PooledConnection}
 * objects.  An object that implements this interface will typically be
 * registered with a naming service that is based on the
 * Java Naming and Directory Interface
 * (JNDI).
 *
 * @since 1.4
 */

public interface ConnectionPoolDataSource  extends CommonDataSource {

  /**
   * Attempts to establish a physical database connection that can
   * be used as a pooled connection.
   *
   * @return  a {@code PooledConnection} object that is a physical
   *         connection to the database that this
   *         {@code ConnectionPoolDataSource} object represents
   * @throws SQLException if a database access error occurs
   * @throws java.sql.SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  PooledConnection getPooledConnection() throws SQLException;

  /**
   * Attempts to establish a physical database connection that can
   * be used as a pooled connection.
   *
   * @param user the database user on whose behalf the connection is being made
   * @param password the user's password
   * @return  a {@code PooledConnection} object that is a physical
   *         connection to the database that this
   *         {@code ConnectionPoolDataSource} object represents
   * @throws SQLException if a database access error occurs
   * @throws java.sql.SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  PooledConnection getPooledConnection(String user, String password)
    throws SQLException;

  /**
   * {@inheritDoc}
   * @since 1.4
   */
  @Override
  java.io.PrintWriter getLogWriter() throws SQLException;

  /**
   * {@inheritDoc}
   * @since 1.4
   */
  @Override
  void setLogWriter(java.io.PrintWriter out) throws SQLException;

  /**
   * {@inheritDoc}
   * @since 1.4
   */
  @Override
  void setLoginTimeout(int seconds) throws SQLException;

  /**
   * {@inheritDoc}
   * @since 1.4
   */
  @Override
  int getLoginTimeout() throws SQLException;

  //------------------------- JDBC 4.3 -----------------------------------

  /**
   * Creates a new {@code PooledConnectionBuilder} instance
   * @implSpec
   * The default implementation will throw a {@code SQLFeatureNotSupportedException}.
   * @return The ConnectionBuilder instance that was created
   * @throws SQLException if an error occurs creating the builder
   * @throws SQLFeatureNotSupportedException if the driver does not support sharding
   * @since 9
   * @see PooledConnectionBuilder
   */
  default PooledConnectionBuilder createPooledConnectionBuilder() throws SQLException {
        throw new SQLFeatureNotSupportedException("createPooledConnectionBuilder not implemented");
  };
 }
