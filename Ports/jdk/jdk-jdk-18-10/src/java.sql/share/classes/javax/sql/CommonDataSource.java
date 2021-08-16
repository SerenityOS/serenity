/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.sql.ShardingKeyBuilder;
import java.util.logging.Logger;

/**
 * Interface that defines the methods which are common between {@code DataSource},
 * {@code XADataSource} and {@code ConnectionPoolDataSource}.
 *
 * @since 1.6
 */
public interface CommonDataSource {

    /**
     * <p>Retrieves the log writer for this {@code DataSource}
     * object.
     *
     * <p>The log writer is a character output stream to which all logging
     * and tracing messages for this data source will be
     * printed.  This includes messages printed by the methods of this
     * object, messages printed by methods of other objects manufactured
     * by this object, and so on.  Messages printed to a data source
     * specific log writer are not printed to the log writer associated
     * with the {@code java.sql.DriverManager} class.  When a
     * {@code DataSource} object is
     * created, the log writer is initially null; in other words, the
     * default is for logging to be disabled.
     *
     * @return the log writer for this data source or null if
     *        logging is disabled
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setLogWriter
     */
    java.io.PrintWriter getLogWriter() throws SQLException;

    /**
     * <p>Sets the log writer for this {@code DataSource}
     * object to the given {@code java.io.PrintWriter} object.
     *
     * <p>The log writer is a character output stream to which all logging
     * and tracing messages for this data source will be
     * printed.  This includes messages printed by the methods of this
     * object, messages printed by methods of other objects manufactured
     * by this object, and so on.  Messages printed to a data source-
     * specific log writer are not printed to the log writer associated
     * with the {@code java.sql.DriverManager} class. When a
     * {@code DataSource} object is created the log writer is
     * initially null; in other words, the default is for logging to be
     * disabled.
     *
     * @param out the new log writer; to disable logging, set to null
     * @throws SQLException if a database access error occurs
     * @see #getLogWriter
     */
    void setLogWriter(java.io.PrintWriter out) throws SQLException;

    /**
     * <p>Sets the maximum time in seconds that this data source will wait
     * while attempting to connect to a database.  A value of zero
     * specifies that the timeout is the default system timeout
     * if there is one; otherwise, it specifies that there is no timeout.
     * When a {@code DataSource} object is created, the login timeout is
     * initially zero.
     *
     * @param seconds the data source login time limit
     * @throws SQLException if a database access error occurs.
     * @see #getLoginTimeout
     */
    void setLoginTimeout(int seconds) throws SQLException;

    /**
     * Gets the maximum time in seconds that this data source can wait
     * while attempting to connect to a database.  A value of zero
     * means that the timeout is the default system timeout
     * if there is one; otherwise, it means that there is no timeout.
     * When a {@code DataSource} object is created, the login timeout is
     * initially zero.
     *
     * @return the data source login time limit
     * @throws SQLException if a database access error occurs.
     * @see #setLoginTimeout
     */
    int getLoginTimeout() throws SQLException;

    //------------------------- JDBC 4.1 -----------------------------------

    /**
     * Return the parent Logger of all the Loggers used by this data source. This
     * should be the Logger farthest from the root Logger that is
     * still an ancestor of all of the Loggers used by this data source. Configuring
     * this Logger will affect all of the log messages generated by the data source.
     * In the worst case, this may be the root Logger.
     *
     * @return the parent Logger for this data source
     * @throws SQLFeatureNotSupportedException if the data source does not use
     * {@code java.util.logging}
     * @since 1.7
     */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException;

    //------------------------- JDBC 4.3 -----------------------------------

    /**
     * Creates a new {@code ShardingKeyBuilder} instance
     * @implSpec
     * The default implementation will throw a {@code SQLFeatureNotSupportedException}.
     * @return The ShardingKeyBuilder instance that was created
     * @throws SQLException if an error occurs creating the builder
     * @throws SQLFeatureNotSupportedException if the driver does not support this method
     * @since 9
     * @see ShardingKeyBuilder
    */
    default ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
        throw new SQLFeatureNotSupportedException("createShardingKeyBuilder not implemented");
  };
}
