/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * An interface that must be implemented when a {@linkplain Driver} wants to be
 * notified by {@code DriverManager}.
 *<P>
 * A {@code DriverAction} implementation is not intended to be used
 * directly by applications. A JDBC Driver  may choose
 * to create its {@code DriverAction} implementation in a private class
 * to avoid it being called directly.
 * <p>
 * The JDBC driver's static initialization block must call
 * {@linkplain DriverManager#registerDriver(java.sql.Driver, java.sql.DriverAction) } in order
 * to inform {@code DriverManager} which {@code DriverAction} implementation to
 * call when the JDBC driver is de-registered.
 * @since 1.8
 */
public interface DriverAction {
    /**
     * Method called by
     * {@linkplain DriverManager#deregisterDriver(Driver) }
     *  to notify the JDBC driver that it was de-registered.
     * <p>
     * The {@code deregister} method is intended only to be used by JDBC Drivers
     * and not by applications.  JDBC drivers are recommended to not implement
     * {@code DriverAction} in a public class.  If there are active
     * connections to the database at the time that the {@code deregister}
     * method is called, it is implementation specific as to whether the
     * connections are closed or allowed to continue. Once this method is
     * called, it is implementation specific as to whether the driver may
     * limit the ability to create new connections to the database, invoke
     * other {@code Driver} methods or throw a {@code SQLException}.
     * Consult your JDBC driver's documentation for additional information
     * on its behavior.
     * @see DriverManager#registerDriver(java.sql.Driver, java.sql.DriverAction)
     * @see DriverManager#deregisterDriver(Driver)
     * @since 1.8
     */
    void deregister();

}
