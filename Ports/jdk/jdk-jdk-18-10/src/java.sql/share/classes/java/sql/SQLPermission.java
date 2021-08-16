/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;

/**
 * The permission for which the {@code SecurityManager} will check
 * when code that is running an application with a
 * {@code SecurityManager} enabled, calls the
 * {@code DriverManager.deregisterDriver} method,
 * {@code DriverManager.setLogWriter} method,
 * {@code DriverManager.setLogStream} (deprecated) method,
 * {@code SyncFactory.setJNDIContext} method,
 * {@code SyncFactory.setLogger} method,
 * {@code Connection.setNetworkTimeout} method,
 * or the {@code Connection.abort} method.
 * If there is no {@code SQLPermission} object, these methods
 * throw a {@code java.lang.SecurityException} as a runtime exception.
 * <P>
 * A {@code SQLPermission} object contains
 * a name (also referred to as a "target name") but no actions
 * list; there is either a named permission or there is not.
 * The target name is the name of the permission (see below). The
 * naming convention follows the  hierarchical property naming convention.
 * In addition, an asterisk
 * may appear at the end of the name, following a ".", or by itself, to
 * signify a wildcard match. For example: {@code loadLibrary.*}
 * and {@code *} signify a wildcard match,
 * while {@code *loadLibrary} and {@code a*b} do not.
 * <P>
 * The following table lists all the possible {@code SQLPermission} target names.
 * The table gives a description of what the permission allows
 * and a discussion of the risks of granting code the permission.
 *
 *
 * <table class="striped">
 * <caption style="display:none">permission target name, what the permission allows, and associated risks</caption>
 * <thead>
 * <tr>
 * <th scope="col">Permission Target Name</th>
 * <th scope="col">What the Permission Allows</th>
 * <th scope="col">Risks of Allowing this Permission</th>
 * </tr>
 * </thead>
 *
 * <tbody>
 * <tr>
 *   <th scope="row">setLog</th>
 *   <td>Setting of the logging stream</td>
 *   <td>This is a dangerous permission to grant.
 * The contents of the log may contain usernames and passwords,
 * SQL statements, and SQL data.</td>
 * </tr>
 * <tr>
 * <th scope="row">callAbort</th>
 *   <td>Allows the invocation of the {@code Connection} method
 *   {@code abort}</td>
 *   <td>Permits an application to terminate a physical connection to a
 *  database.</td>
 * </tr>
 * <tr>
 * <th scope="row">setSyncFactory</th>
 *   <td>Allows the invocation of the {@code SyncFactory} methods
 *   {@code setJNDIContext} and {@code setLogger}</td>
 *   <td>Permits an application to specify the JNDI context from which the
 *   {@code SyncProvider} implementations can be retrieved from and the logging
 *   object to be used by the {@code SyncProvider} implementation.</td>
 * </tr>
 *
 * <tr>
 * <th scope="row">setNetworkTimeout</th>
 *   <td>Allows the invocation of the {@code Connection} method
 *   {@code setNetworkTimeout}</td>
 *   <td>Permits an application to specify the maximum period a
 * {@code Connection} or
 * objects created from the {@code Connection}
 * will wait for the database to reply to any one request.</td>
 * <tr>
 * <th scope="row">deregisterDriver</th>
 *   <td>Allows the invocation of the {@code DriverManager}
 * method {@code deregisterDriver}</td>
 *   <td>Permits an application to remove a JDBC driver from the list of
 * registered Drivers and release its resources.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @since 1.3
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 *
 */

public final class SQLPermission extends BasicPermission {

    /**
     * Creates a new {@code SQLPermission} object with the specified name.
     * The name is the symbolic name of the {@code SQLPermission}.
     *
     * @param name the name of this {@code SQLPermission} object, which must
     * be either {@code  setLog}, {@code callAbort}, {@code setSyncFactory},
     *  {@code deregisterDriver}, or {@code setNetworkTimeout}
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.

     */

    public SQLPermission(String name) {
        super(name);
    }

    /**
     * Creates a new {@code SQLPermission} object with the specified name.
     * The name is the symbolic name of the {@code SQLPermission}; the
     * actions {@code String} is currently unused and should be
     * {@code null}.
     *
     * @param name the name of this {@code SQLPermission} object, which must
     * be either {@code  setLog}, {@code callAbort}, {@code setSyncFactory},
     *  {@code deregisterDriver}, or {@code setNetworkTimeout}
     * @param actions should be {@code null}
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.

     */

    public SQLPermission(String name, String actions) {
        super(name, actions);
    }

    /**
     * Private serial version unique ID to ensure serialization
     * compatibility.
     */
    static final long serialVersionUID = -1439323187199563495L;

}
