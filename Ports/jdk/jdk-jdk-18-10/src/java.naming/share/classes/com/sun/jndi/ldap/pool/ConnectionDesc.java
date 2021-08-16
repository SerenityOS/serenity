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
 * Represents a description of PooledConnection in Connections.
 * Contains a PooledConnection, its state (busy, idle, expired), and idle time.
 *
 * Any access or update to a descriptor's state is synchronized.
 *
 * @author Rosanna Lee
 */
final class ConnectionDesc {
    private static final boolean debug = Pool.debug;

    // Package private because used by Pool.showStats()
    static final byte BUSY = (byte)0;
    static final byte IDLE = (byte)1;
    static final byte EXPIRED = (byte)2;

    private final PooledConnection conn;

    private byte state = IDLE;  // initial state
    private long idleSince;
    private long useCount = 0;  // for stats & debugging only

    ConnectionDesc(PooledConnection conn) {
        this.conn = conn;
    }

    ConnectionDesc(PooledConnection conn, boolean use) {
        this.conn = conn;
        if (use) {
            state = BUSY;
            ++useCount;
        }
    }

    /**
     * Two desc are equal if their PooledConnections are the same.
     * This is useful when searching for a ConnectionDesc using only its
     * PooledConnection.
     */
    public boolean equals(Object obj) {
        return obj != null
            && obj instanceof ConnectionDesc
            && ((ConnectionDesc)obj).conn == conn;
    }

    /**
     * Hashcode is that of PooledConnection to facilitate
     * searching for a ConnectionDesc using only its PooledConnection.
     */
    public int hashCode() {
        return conn.hashCode();
    }

    /**
     * Changes the state of a ConnectionDesc from BUSY to IDLE and
     * records the current time so that we will know how long it has been idle.
     * @return true if state change occurred.
     */
    synchronized boolean release() {
        d("release()");
        if (state == BUSY) {
            state = IDLE;

            idleSince = System.currentTimeMillis();
            return true;  // Connection released, ready for reuse
        } else {
            return false; // Connection wasn't busy to begin with
        }
    }

    /**
     * If ConnectionDesc is IDLE, change its state to BUSY and return
     * its connection.
     *
     * @return ConnectionDesc's PooledConnection if it was idle; null otherwise.
     */
    synchronized PooledConnection tryUse() {
        d("tryUse()");

        if (state == IDLE) {
            state = BUSY;
            ++useCount;
            return conn;
        }

        return null;
    }

    /**
     * If ConnectionDesc is IDLE and has expired, close the corresponding
     * PooledConnection.
     *
     * @param threshold a connection that has been idle before this time
     *     have expired.
     *
     * @return true if entry is idle and has expired; false otherwise.
     */
    synchronized boolean expire(long threshold) {
        if (state == IDLE && idleSince < threshold) {

            d("expire(): expired");

            state = EXPIRED;
            conn.closeConnection();  // Close real connection

            return true;  // Expiration successful
        } else {
            d("expire(): not expired");
            return false; // Expiration did not occur
        }
    }

    public String toString() {
        return conn.toString() + " " +
            (state == BUSY ? "busy" : (state == IDLE ? "idle" : "expired"));
    }

    // Used by Pool.showStats()
    int getState() {
        return state;
    }

    // Used by Pool.showStats()
    long getUseCount() {
        return useCount;
    }

    private void d(String msg) {
        if (debug) {
            System.err.println("ConnectionDesc." + msg + " " + toString());
        }
    }
}
