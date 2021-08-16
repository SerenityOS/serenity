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

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import java.io.PrintStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import javax.naming.NamingException;

/**
 * A map of pool ids to Connections.
 * Key is an object that uniquely identifies a PooledConnection request
 * (typically information needed to create the connection).
 * The definitions of the key's equals() and hashCode() methods are
 * vital to its unique identification in a Pool.
 *
 * Value is a ConnectionsRef, which is a reference to Connections,
 * a list of equivalent connections.
 *
 * Supports methods that
 * - retrieves (or creates as necessary) a connection from the pool
 * - removes expired connections from the pool
 *
 * Connections cleanup:
 * A WeakHashMap is used for mapping the pool ids and Connections.
 * A SoftReference from the value to the key is kept to hold the map
 * entry as long as possible. This allows the GC to remove Connections
 * from the Pool under situations of VM running out of resources.
 * To take an appropriate action of 'closing the connections' before the GC
 * reclaims the ConnectionsRef objects, the ConnectionsRef objects are made
 * weakly reachable through a list of weak references registered with
 * a reference queue.
 * Upon an entry gets removed from the WeakHashMap, the ConnectionsRef (value
 * in the map) object is weakly reachable. When another sweep of
 * clearing the weak references is made by the GC it puts the corresponding
 * ConnectionsWeakRef object into the reference queue.
 * The reference queue is monitored lazily for reclaimable Connections
 * whenever a pooled connection is requested or a call to remove the expired
 * connections is made. The monitoring is done regularly when idle connection
 * timeout is set as the PoolCleaner removes expired connections periodically.
 * As determined by experimentation, cleanup of resources using the
 * ReferenceQueue mechanism is reliable and has more immediate effect than the
 * finalizer approach.
 *
 * @author Rosanna Lee
 */

public final class Pool {

    static final boolean debug = com.sun.jndi.ldap.LdapPoolManager.debug;

    /*
     * Used for connections cleanup
     */
    private static final ReferenceQueue<ConnectionsRef> queue =
        new ReferenceQueue<>();
    private static final Collection<Reference<ConnectionsRef>> weakRefs =
        Collections.synchronizedList(new LinkedList<Reference<ConnectionsRef>>());

    private final int maxSize;    // max num of identical conn per pool
    private final int prefSize;   // preferred num of identical conn per pool
    private final int initSize;   // initial number of identical conn to create
    private final Map<Object, ConnectionsRef> map;

    public Pool(int initSize, int prefSize, int maxSize) {
        map = new WeakHashMap<>();
        this.prefSize = prefSize;
        this.maxSize = maxSize;
        this.initSize = initSize;
    }

    /**
     * Gets a pooled connection for id. The pooled connection might be
     * newly created, as governed by the maxSize and prefSize settings.
     * If a pooled connection is unavailable and cannot be created due
     * to the maxSize constraint, this call blocks until the constraint
     * is removed or until 'timeout' ms has elapsed.
     *
     * @param id identity of the connection to get
     * @param timeout the number of milliseconds to wait before giving up
     * @param factory the factory to use for creating the connection if
     *          creation is necessary
     * @return a pooled connection
     * @throws NamingException the connection could not be created due to
     *                          an error.
     */
    public PooledConnection getPooledConnection(Object id, long timeout,
        PooledConnectionFactory factory) throws NamingException {

        d("get(): ", id);
        if (debug) {
            synchronized (map) {
                d("size: ", map.size());
            }
        }

        expungeStaleConnections();

        Connections conns;
        synchronized (map) {
            conns = getConnections(id);
            if (conns == null) {
                d("get(): creating new connections list for ", id);

                // No connections for this id so create a new list
                conns = new Connections(id, initSize, prefSize, maxSize,
                    factory);
                ConnectionsRef connsRef = new ConnectionsRef(conns);
                map.put(id, connsRef);

                // Create a weak reference to ConnectionsRef
                Reference<ConnectionsRef> weakRef =
                        new ConnectionsWeakRef(connsRef, queue);

                // Keep the weak reference through the element of a linked list
                weakRefs.add(weakRef);
            }
            d("get(): size after: ", map.size());
        }

        return conns.get(timeout, factory); // get one connection from list
    }

    private Connections getConnections(Object id) {
        ConnectionsRef ref = map.get(id);
        return (ref != null) ? ref.getConnections() : null;
    }

    /**
     * Goes through the connections in this Pool and expires ones that
     * have been idle before 'threshold'. An expired connection is closed
     * and then removed from the pool (removePooledConnection() will eventually
     * be called, and the list of pools itself removed if it becomes empty).
     *
     * @param threshold connections idle before 'threshold' should be closed
     *          and removed.
     */
    public void expire(long threshold) {
        Collection<ConnectionsRef> copy;
        synchronized (map) {
            copy = new ArrayList<>(map.values());
        }

        ArrayList<ConnectionsRef> removed = new ArrayList<>();
        Connections conns;
        for (ConnectionsRef ref : copy) {
            conns = ref.getConnections();
            if (conns.expire(threshold)) {
                d("expire(): removing ", conns);
                removed.add(ref);
            }
        }

        synchronized (map) {
            map.values().removeAll(removed);
        }

        expungeStaleConnections();
    }

    /*
     * Closes the connections contained in the ConnectionsRef object that
     * is going to be reclaimed by the GC. Called by getPooledConnection()
     * and expire() methods of this class.
     */
    private static void expungeStaleConnections() {
        ConnectionsWeakRef releaseRef = null;
        while ((releaseRef = (ConnectionsWeakRef) queue.poll())
                                        != null) {
            Connections conns = releaseRef.getConnections();

            if (debug) {
                System.err.println(
                        "weak reference cleanup: Closing Connections:" + conns);
            }

            // cleanup
            conns.close();
            weakRefs.remove(releaseRef);
            releaseRef.clear();
         }
    }


    public void showStats(PrintStream out) {
        Object id;
        Connections conns;

        out.println("===== Pool start ======================");
        out.println("maximum pool size: " + maxSize);
        out.println("preferred pool size: " + prefSize);
        out.println("initial pool size: " + initSize);

        synchronized (map) {
            out.println("current pool size: " + map.size());

            for (Map.Entry<Object, ConnectionsRef> entry : map.entrySet()) {
                id = entry.getKey();
                conns = entry.getValue().getConnections();
                out.println("   " + id + ":" + conns.getStats());
            }
        }

        out.println("====== Pool end =====================");
    }

    public String toString() {
        synchronized (map) {
            return super.toString() + " " + map.toString();
        }
    }

    private void d(String msg, int i) {
        if (debug) {
            System.err.println(this + "." + msg + i);
        }
    }

    private void d(String msg, Object obj) {
        if (debug) {
            System.err.println(this + "." + msg + obj);
        }
    }
}
