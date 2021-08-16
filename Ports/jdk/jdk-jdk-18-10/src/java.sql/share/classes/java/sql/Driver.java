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

import java.util.logging.Logger;

/**
 * The interface that every driver class must implement.
 * <P>The Java SQL framework allows for multiple database drivers.
 *
 * <P>Each driver should supply a class that implements
 * the Driver interface.
 *
 * <P>The DriverManager will try to load as many drivers as it can
 * find and then for any given connection request, it will ask each
 * driver in turn to try to connect to the target URL.
 *
 * <P>It is strongly recommended that each Driver class should be
 * small and standalone so that the Driver class can be loaded and
 * queried without bringing in vast quantities of supporting code.
 *
 * <P>When a Driver class is loaded, it should create an instance of
 * itself and register it with the DriverManager. This means that a
 * user can load and register a driver by calling:
 * <p>
 * {@code Class.forName("foo.bah.Driver")}
 * <p>
 * A JDBC driver may create a {@linkplain DriverAction} implementation in order
 * to receive notifications when {@linkplain DriverManager#deregisterDriver} has
 * been called.
 * @see DriverManager
 * @see Connection
 * @see DriverAction
 * @since 1.1
 */
public interface Driver {

    /**
     * Attempts to make a database connection to the given URL.
     * The driver should return "null" if it realizes it is the wrong kind
     * of driver to connect to the given URL.  This will be common, as when
     * the JDBC driver manager is asked to connect to a given URL it passes
     * the URL to each loaded driver in turn.
     *
     * <P>The driver should throw an {@code SQLException} if it is the right
     * driver to connect to the given URL but has trouble connecting to
     * the database.
     *
     * <P>The {@code Properties} argument can be used to pass
     * arbitrary string tag/value pairs as connection arguments.
     * Normally at least "user" and "password" properties should be
     * included in the {@code Properties} object.
     * <p>
     * <B>Note:</B> If a property is specified as part of the {@code url} and
     * is also specified in the {@code Properties} object, it is
     * implementation-defined as to which value will take precedence. For
     * maximum portability, an application should only specify a property once.
     *
     * @param url the URL of the database to which to connect
     * @param info a list of arbitrary string tag/value pairs as
     * connection arguments. Normally at least a "user" and
     * "password" property should be included.
     * @return a {@code Connection} object that represents a
     *         connection to the URL
     * @throws SQLException if a database access error occurs or the url is
     * {@code null}
     */
    Connection connect(String url, java.util.Properties info)
        throws SQLException;

    /**
     * Retrieves whether the driver thinks that it can open a connection
     * to the given URL.  Typically drivers will return {@code true} if they
     * understand the sub-protocol specified in the URL and {@code false} if
     * they do not.
     *
     * @param url the URL of the database
     * @return {@code true} if this driver understands the given URL;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs or the url is
     * {@code null}
     */
    boolean acceptsURL(String url) throws SQLException;


    /**
     * Gets information about the possible properties for this driver.
     * <P>
     * The {@code getPropertyInfo} method is intended to allow a generic
     * GUI tool to discover what properties it should prompt
     * a human for in order to get
     * enough information to connect to a database.  Note that depending on
     * the values the human has supplied so far, additional values may become
     * necessary, so it may be necessary to iterate though several calls
     * to the {@code getPropertyInfo} method.
     *
     * @param url the URL of the database to which to connect
     * @param info a proposed list of tag/value pairs that will be sent on
     *          connect open
     * @return an array of {@code DriverPropertyInfo} objects describing
     *          possible properties.  This array may be an empty array if
     *          no properties are required.
     * @throws SQLException if a database access error occurs
     */
    DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info)
                         throws SQLException;


    /**
     * Retrieves the driver's major version number. Initially this should be 1.
     *
     * @return this driver's major version number
     */
    int getMajorVersion();

    /**
     * Gets the driver's minor version number. Initially this should be 0.
     * @return this driver's minor version number
     */
    int getMinorVersion();


    /**
     * Reports whether this driver is a genuine JDBC
     * Compliant driver.
     * A driver may only report {@code true} here if it passes the JDBC
     * compliance tests; otherwise it is required to return {@code false}.
     * <P>
     * JDBC compliance requires full support for the JDBC API and full support
     * for SQL 92 Entry Level.  It is expected that JDBC compliant drivers will
     * be available for all the major commercial databases.
     * <P>
     * This method is not intended to encourage the development of non-JDBC
     * compliant drivers, but is a recognition of the fact that some vendors
     * are interested in using the JDBC API and framework for lightweight
     * databases that do not support full database functionality, or for
     * special databases such as document information retrieval where a SQL
     * implementation may not be feasible.
     * @return {@code true} if this driver is JDBC Compliant; {@code false}
     *         otherwise
     */
    boolean jdbcCompliant();

    //------------------------- JDBC 4.1 -----------------------------------

    /**
     * Return the parent Logger of all the Loggers used by this driver. This
     * should be the Logger farthest from the root Logger that is
     * still an ancestor of all of the Loggers used by this driver. Configuring
     * this Logger will affect all of the log messages generated by the driver.
     * In the worst case, this may be the root Logger.
     *
     * @return the parent Logger for this driver
     * @throws SQLFeatureNotSupportedException if the driver does not use
     * {@code java.util.logging}.
     * @since 1.7
     */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException;
}
