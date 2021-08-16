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
 * Represents a connection that is managed in a pool. The connection
 * may be reused by multiple clients.
 *
 * A pooled connection typically has a close method that its clients
 * use to indicate that they no longer need the connection. This close
 * method would interact with the connection pool to return the connection
 * to the pool (see PoolCallback).
 *<p>
 * The pooled connection also needs to provide a close method that the
 * connection pool can use to physically close the connection.
 * The pool might need to physically close the connection as determined
 * by the pool's policy (for example, to manage the pool size or idle
 * connections). This second close method should *not* use PoolCallback
 * methods. It should only do what is required to close the physical
 * connection.
 *
 * @author Rosanna Lee
 */
public interface PooledConnection {

    /**
     * Closes the physical connection.
     */
    public abstract void closeConnection();
}
