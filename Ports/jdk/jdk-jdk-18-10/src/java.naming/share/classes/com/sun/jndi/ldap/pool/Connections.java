/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList; // JDK 1.2
import java.util.List;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import javax.naming.NamingException;
import javax.naming.InterruptedNamingException;
import javax.naming.CommunicationException;

/**
 * Represents a list of PooledConnections (actually, ConnectionDescs) with the
 * same pool id.
 * The list starts out with an initial number of connections.
 * Additional PooledConnections are created lazily upon demand.
 * The list has a maximum size. When the number of connections
 * reaches the maximum size, a request for a PooledConnection blocks until
 * a connection is returned to the list. A maximum size of zero means that
 * there is no maximum: connection creation will be attempted when
 * no idle connection is available.
 *
 * The list may also have a preferred size. If the current list size
 * is less than the preferred size, a request for a connection will result in
 * a PooledConnection being created (even if an idle connection is available).
 * If the current list size is greater than the preferred size,
 * a connection being returned to the list will be closed and removed from
 * the list. A preferred size of zero means that there is no preferred size:
 * connections are created only when no idle connection is available and
 * a connection being returned to the list is not closed. Regardless of the
 * preferred size, connection creation always observes the maximum size:
 * a connection won't be created if the list size is at or exceeds the
 * maximum size.
 *
 * @author Rosanna Lee
 */

// Package private: accessed only by Pool
final class Connections implements PoolCallback {
    private static final boolean debug = Pool.debug;
    private static final boolean trace =
        com.sun.jndi.ldap.LdapPoolManager.trace;
    private static final int DEFAULT_SIZE = 10;

    private final int maxSize;
    private final int prefSize;
    private final List<ConnectionDesc> conns;

    private boolean closed = false;   // Closed for business
    private Reference<Object> ref; // maintains reference to id to prevent premature GC

    /**
     * @param id the identity (connection request) of the connections in the list
     * @param initSize the number of connections to create initially
     * @param prefSize the preferred size of the pool. The pool will try
     * to maintain a pool of this size by creating and closing connections
     * as needed.
     * @param maxSize the maximum size of the pool. The pool will not exceed
     * this size. If the pool is at this size, a request for a connection
     * will block until an idle connection is released to the pool or
     * when one is removed.
     * @param factory The factory responsible for creating a connection
     */
    Connections(Object id, int initSize, int prefSize, int maxSize,
        PooledConnectionFactory factory) throws NamingException {

        this.maxSize = maxSize;
        if (maxSize > 0) {
            // prefSize and initSize cannot exceed specified maxSize
            this.prefSize = Math.min(prefSize, maxSize);
            initSize = Math.min(initSize, maxSize);
        } else {
            this.prefSize = prefSize;
        }
        conns = new ArrayList<>(maxSize > 0 ? maxSize : DEFAULT_SIZE);

        // Maintain soft ref to id so that this Connections' entry in
        // Pool doesn't get GC'ed prematurely
        ref = new SoftReference<>(id);

        d("init size=", initSize);
        d("max size=", maxSize);
        d("preferred size=", prefSize);

        // Create initial connections
        PooledConnection conn;
        for (int i = 0; i < initSize; i++) {
            conn = factory.createPooledConnection(this);
            td("Create ", conn ,factory);
            conns.add(new ConnectionDesc(conn)); // Add new idle conn to pool
        }
    }

    /**
     * Retrieves a PooledConnection from this list of connections.
     * Use an existing one if one is idle, or create one if the list's
     * max size hasn't been reached. If max size has been reached, wait
     * for a PooledConnection to be returned, or one to be removed (thus
     * not reaching the max size any longer).
     *
     * @param timeout if > 0, msec to wait until connection is available
     * @param factory creates the PooledConnection if one needs to be created
     *
     * @return A non-null PooledConnection
     * @throws NamingException PooledConnection cannot be created, because this
     * thread was interrupted while it waited for an available connection,
     * or if it timed out while waiting, or the creation of a connection
     * resulted in an error.
     */
    synchronized PooledConnection get(long timeout,
        PooledConnectionFactory factory) throws NamingException {
        PooledConnection conn;
        long start = (timeout > 0 ? System.currentTimeMillis() : 0);
        long waittime = timeout;

        d("get(): before");
        while ((conn = getOrCreateConnection(factory)) == null) {
            if (timeout > 0 && waittime <= 0) {
                throw new CommunicationException(
                    "Timeout exceeded while waiting for a connection: " +
                    timeout + "ms");
            }
            try {
                d("get(): waiting");
                if (waittime > 0) {
                    wait(waittime);  // Wait until one is released or removed
                } else {
                    wait();
                }
            } catch (InterruptedException e) {
                throw new InterruptedNamingException(
                    "Interrupted while waiting for a connection");
            }
            // Check whether we timed out
            if (timeout > 0) {
                long now = System.currentTimeMillis();
                waittime = timeout - (now - start);
            }
        }

        d("get(): after");
        return conn;
    }

    /**
     * Retrieves an idle connection from this list if one is available.
     * If none is available, create a new one if maxSize hasn't been reached.
     * If maxSize has been reached, return null.
     * Always called from a synchronized method.
     */
    private PooledConnection getOrCreateConnection(
        PooledConnectionFactory factory) throws NamingException {

        int size = conns.size(); // Current number of idle/nonidle conns
        PooledConnection conn = null;

        if (prefSize <= 0 || size >= prefSize) {
            // If no prefSize specified, or list size already meets or
            // exceeds prefSize, then first look for an idle connection
            ConnectionDesc entry;
            for (int i = 0; i < size; i++) {
                entry = conns.get(i);
                if ((conn = entry.tryUse()) != null) {
                    d("get(): use ", conn);
                    td("Use ", conn);
                    return conn;
                }
            }
        }

        // Check if list size already at maxSize specified
        if (maxSize > 0 && size >= maxSize) {
            return null;   // List size is at limit; cannot create any more
        }

        conn = factory.createPooledConnection(this);
        td("Create and use ", conn, factory);
        conns.add(new ConnectionDesc(conn, true)); // Add new conn to pool

        return conn;
    }

    /**
     * Releases connection back into list.
     * If the list size is below prefSize, the connection may be reused.
     * If the list size exceeds prefSize, then the connection is closed
     * and removed from the list.
     *
     * public because implemented as part of PoolCallback.
     */
    public synchronized boolean releasePooledConnection(PooledConnection conn) {
        ConnectionDesc entry;
        int loc = conns.indexOf(entry=new ConnectionDesc(conn));

        d("release(): ", conn);

        if (loc >= 0) {
            // Found entry

            if (closed || (prefSize > 0 && conns.size() > prefSize)) {
                // If list size exceeds prefSize, close connection

                d("release(): closing ", conn);
                td("Close ", conn);

                // size must be >= 2 so don't worry about empty list
                conns.remove(entry);
                conn.closeConnection();

            } else {
                d("release(): release ", conn);
                td("Release ", conn);

                // Get ConnectionDesc from list to get correct state info
                entry = conns.get(loc);
                // Return connection to list, ready for reuse
                entry.release();
            }
            notifyAll();
            d("release(): notify");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes PooledConnection from list of connections.
     * The closing of the connection is separate from this method.
     * This method is called usually when the caller encounters an error
     * when using the connection and wants it removed from the pool.
     *
     * @return true if conn removed; false if it was not in pool
     *
     * public because implemented as part of PoolCallback.
     */
    public synchronized boolean removePooledConnection(PooledConnection conn) {
        if (conns.remove(new ConnectionDesc(conn))) {
            d("remove(): ", conn);

            notifyAll();

            d("remove(): notify");
            td("Remove ", conn);

            if (conns.isEmpty()) {
                // Remove softref to make pool entry eligible for GC.
                // Once ref has been removed, it cannot be reinstated.
                ref = null;
            }

            return true;
        } else {
            d("remove(): not found ", conn);
            return false;
        }
    }

    /**
     * Goes through all entries in list, removes and closes ones that have been
     * idle before threshold.
     *
     * @param threshold an entry idle since this time has expired.
     * @return true if no more connections in list
     */
    boolean expire(long threshold) {
        List<ConnectionDesc> clonedConns;
        synchronized(this) {
            clonedConns = new ArrayList<>(conns);
        }
        List<ConnectionDesc> expired = new ArrayList<>();

        for (ConnectionDesc entry : clonedConns) {
            d("expire(): ", entry);
            if (entry.expire(threshold)) {
                expired.add(entry);
                td("expire(): Expired ", entry);
            }
        }

        synchronized (this) {
            conns.removeAll(expired);
            // Don't need to call notify() because we're
            // removing only idle connections. If there were
            // idle connections, then there should be no waiters.
            return conns.isEmpty();  // whether whole list has 'expired'
        }
    }

    /**
     * Called when this instance of Connections has been removed from Pool.
     * This means that no one can get any pooled connections from this
     * Connections any longer. Expire all idle connections as of 'now'
     * and leave indicator so that any in-use connections will be closed upon
     * their return.
     */
    synchronized void close() {
        expire(System.currentTimeMillis());     // Expire idle connections
        closed = true;   // Close in-use connections when they are returned
    }

    String getStats() {
        int idle = 0;
        int busy = 0;
        int expired = 0;
        long use = 0;
        int len;

        synchronized (this) {
            len = conns.size();

            ConnectionDesc entry;
            for (int i = 0; i < len; i++) {
                entry = conns.get(i);
                use += entry.getUseCount();
                switch (entry.getState()) {
                case ConnectionDesc.BUSY:
                    ++busy;
                    break;
                case ConnectionDesc.IDLE:
                    ++idle;
                    break;
                case ConnectionDesc.EXPIRED:
                    ++expired;
                }
            }
        }
        return "size=" + len + "; use=" + use + "; busy=" + busy
            + "; idle=" + idle + "; expired=" + expired;
    }

    private void d(String msg, Object o1) {
        if (debug) {
            d(msg + o1);
        }
    }

    private void d(String msg, int i) {
        if (debug) {
            d(msg + i);
        }
    }

    private void d(String msg) {
        if (debug) {
            System.err.println(this + "." + msg + "; size: " + conns.size());
        }
    }

    private void td(String msg, Object o1, Object o2) {
        if (trace) { // redo test to avoid object creation
            td(msg + o1 + "[" + o2 + "]");
        }
    }
    private void td(String msg, Object o1) {
        if (trace) { // redo test to avoid object creation
            td(msg + o1);
        }
    }
    private void td(String msg) {
        if (trace) {
            System.err.println(msg);
        }
    }
}
