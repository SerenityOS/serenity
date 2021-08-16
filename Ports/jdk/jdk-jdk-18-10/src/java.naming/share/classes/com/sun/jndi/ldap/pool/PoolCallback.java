/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap.pool;

/**
 * Represents a callback used to release or remove a PooledConnection back
 * into the pool.
 *
 * A pooled connection typically has a close method that its clients
 * use to indicate that they no longer need the connection. This close
 * method should use the methods defined in this interface to
 * interact with the connection pool to return the connection
 * to the pool.
 *
 * The methods in this interface are typically invoked by a PooledConnection.
 * The methods in this interface are typically implemented by the connection
 * pool manager.
 *
 * @author Rosanna Lee
 */
public interface PoolCallback {
    /**
     * Releases a useable connection back to the pool.
     *
     * @param conn The connection to release.
     * @return true if the connection released; false if the connection
     * is no longer in the pool.
     */
    public abstract boolean releasePooledConnection(PooledConnection conn);

    /**
     * Removes a connection from the pool. The connection should not be reused.
     * The physical connection should have already been closed.
     *
     * @param conn The connection to return.
     * @return true if the connection was removed; false if the connection
     * is no longer in the pool prior to removal.
     */
    public abstract boolean removePooledConnection(PooledConnection conn);
}
