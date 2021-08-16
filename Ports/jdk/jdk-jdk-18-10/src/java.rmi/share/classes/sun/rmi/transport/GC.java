/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.rmi.transport;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.SortedSet;
import java.util.TreeSet;
import jdk.internal.misc.InnocuousThread;


/**
 * Support for garbage-collection latency requests.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

@SuppressWarnings("removal")
class GC {

    private GC() { }            /* To prevent instantiation */


    /* Latency-target value indicating that there's no active target
     */
    private static final long NO_TARGET = Long.MAX_VALUE;

    /* The current latency target, or NO_TARGET if there is no target
     */
    private static long latencyTarget = NO_TARGET;

    /* The daemon thread that implements the latency-target mechanism,
     * or null if there is presently no daemon thread
     */
    private static Thread daemon = null;

    /* The lock object for the latencyTarget and daemon fields.  The daemon
     * thread, if it exists, waits on this lock for notification that the
     * latency target has changed.
     */
    private static class LatencyLock extends Object { };
    private static Object lock = new LatencyLock();


    /**
     * Returns the maximum <em>object-inspection age</em>, which is the number
     * of real-time milliseconds that have elapsed since the
     * least-recently-inspected heap object was last inspected by the garbage
     * collector.
     *
     * <p> For simple stop-the-world collectors this value is just the time
     * since the most recent collection.  For generational collectors it is the
     * time since the oldest generation was most recently collected.  Other
     * collectors are free to return a pessimistic estimate of the elapsed
     * time, or simply the time since the last full collection was performed.
     *
     * <p> Note that in the presence of reference objects, a given object that
     * is no longer strongly reachable may have to be inspected multiple times
     * before it can be reclaimed.
     */
    public static native long maxObjectInspectionAge();

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                System.loadLibrary("rmi");
                return null;
            }});
    }

    private static class Daemon implements Runnable {

        @Override
        public void run() {
            for (;;) {
                long l;
                synchronized (lock) {

                    l = latencyTarget;
                    if (l == NO_TARGET) {
                        /* No latency target, so exit */
                        GC.daemon = null;
                        return;
                    }

                    long d = maxObjectInspectionAge();
                    if (d >= l) {
                        /* Do a full collection.  There is a remote possibility
                         * that a full collection will occurr between the time
                         * we sample the inspection age and the time the GC
                         * actually starts, but this is sufficiently unlikely
                         * that it doesn't seem worth the more expensive JVM
                         * interface that would be required.
                         */
                        System.gc();
                        d = 0;
                    }

                    /* Wait for the latency period to expire,
                     * or for notification that the period has changed
                     */
                    try {
                        lock.wait(l - d);
                    } catch (InterruptedException x) {
                        continue;
                    }
                }
            }
        }

        /* Create a new daemon thread */
        public static void create() {
            PrivilegedAction<Void> pa = new PrivilegedAction<Void>() {
                public Void run() {
                    Thread t = InnocuousThread.newSystemThread("RMI GC Daemon",
                                                               new Daemon());
                    assert t.getContextClassLoader() == null;
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY + 1);
                    t.start();
                    GC.daemon = t;
                    return null;
                }};
            AccessController.doPrivileged(pa);
        }

    }


    /* Sets the latency target to the given value.
     * Must be invoked while holding the lock.
     */
    private static void setLatencyTarget(long ms) {
        latencyTarget = ms;
        if (daemon == null) {
            /* Create a new daemon thread */
            Daemon.create();
        } else {
            /* Notify the existing daemon thread
             * that the lateency target has changed
             */
            lock.notify();
        }
    }


    /**
     * Represents an active garbage-collection latency request.  Instances of
     * this class are created by the <code>{@link #requestLatency}</code>
     * method.  Given a request, the only interesting operation is that of
     * cancellation.
     */
    public static class LatencyRequest
        implements Comparable<LatencyRequest> {

        /* Instance counter, used to generate unique identifers */
        private static long counter = 0;

        /* Sorted set of active latency requests */
        private static SortedSet<LatencyRequest> requests = null;

        /* Examine the request set and reset the latency target if necessary.
         * Must be invoked while holding the lock.
         */
        private static void adjustLatencyIfNeeded() {
            if ((requests == null) || requests.isEmpty()) {
                if (latencyTarget != NO_TARGET) {
                    setLatencyTarget(NO_TARGET);
                }
            } else {
                LatencyRequest r = requests.first();
                if (r.latency != latencyTarget) {
                    setLatencyTarget(r.latency);
                }
            }
        }

        /* The requested latency, or NO_TARGET
         * if this request has been cancelled
         */
        private long latency;

        /* Unique identifier for this request */
        private long id;

        private LatencyRequest(long ms) {
            if (ms <= 0) {
                throw new IllegalArgumentException("Non-positive latency: "
                                                   + ms);
            }
            this.latency = ms;
            synchronized (lock) {
                this.id = ++counter;
                if (requests == null) {
                    requests = new TreeSet<LatencyRequest>();
                }
                requests.add(this);
                adjustLatencyIfNeeded();
            }
        }

        /**
         * Cancels this latency request.
         *
         * @throws  IllegalStateException
         *          If this request has already been cancelled
         */
        public void cancel() {
            synchronized (lock) {
                if (this.latency == NO_TARGET) {
                    throw new IllegalStateException("Request already"
                                                    + " cancelled");
                }
                if (!requests.remove(this)) {
                    throw new InternalError("Latency request "
                                            + this + " not found");
                }
                if (requests.isEmpty()) requests = null;
                this.latency = NO_TARGET;
                adjustLatencyIfNeeded();
            }
        }

        public int compareTo(LatencyRequest r) {
            long d = this.latency - r.latency;
            if (d == 0) d = this.id - r.id;
            return (d < 0) ? -1 : ((d > 0) ? +1 : 0);
        }

        public String toString() {
            return (LatencyRequest.class.getName()
                    + "[" + latency + "," + id + "]");
        }

    }


    /**
     * Makes a new request for a garbage-collection latency of the given
     * number of real-time milliseconds.  A low-priority daemon thread makes a
     * best effort to ensure that the maximum object-inspection age never
     * exceeds the smallest of the currently active requests.
     *
     * @param   latency
     *          The requested latency
     *
     * @throws  IllegalArgumentException
     *          If the given <code>latency</code> is non-positive
     */
    public static LatencyRequest requestLatency(long latency) {
        return new LatencyRequest(latency);
    }


    /**
     * Returns the current smallest garbage-collection latency request, or zero
     * if there are no active requests.
     */
    public static long currentLatencyTarget() {
        long t = latencyTarget;
        return (t == NO_TARGET) ? 0 : t;
    }

}
