/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.ReferenceQueue;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.dgc.VMID;
import java.rmi.server.ExportException;
import java.rmi.server.ObjID;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import sun.rmi.runtime.Log;
import sun.rmi.runtime.NewThreadAction;

/**
 * Object table shared by all implementors of the Transport interface.
 * This table maps object ids to remote object targets in this address
 * space.
 *
 * @author  Ann Wollrath
 * @author  Peter Jones
 */
public final class ObjectTable {

    /** maximum interval between complete garbage collections of local heap */
    @SuppressWarnings("removal")
    private final static long gcInterval =              // default 1 hour
        AccessController.doPrivileged((PrivilegedAction<Long>) () ->
            Long.getLong("sun.rmi.dgc.server.gcInterval", 3600000));

    /**
     * lock guarding objTable and implTable.
     * Holders MAY acquire a Target instance's lock or keepAliveLock.
     */
    private static final Object tableLock = new Object();

    /** tables mapping to Target, keyed from ObjectEndpoint and impl object */
    private static final Map<ObjectEndpoint,Target> objTable =
        new HashMap<>();
    private static final Map<WeakRef,Target> implTable =
        new HashMap<>();

    /**
     * lock guarding keepAliveCount, reaper, and gcLatencyRequest.
     * Holders may NOT acquire a Target instance's lock or tableLock.
     */
    private static final Object keepAliveLock = new Object();

    /** count of non-permanent objects in table or still processing calls */
    private static int keepAliveCount = 0;

    /** thread to collect unreferenced objects from table */
    private static Thread reaper = null;

    /** queue notified when weak refs in the table are cleared */
    static final ReferenceQueue<Object> reapQueue = new ReferenceQueue<>();

    /** handle for GC latency request (for future cancellation) */
    private static GC.LatencyRequest gcLatencyRequest = null;

    /*
     * Disallow anyone from creating one of these.
     */
    private ObjectTable() {}

    /**
     * Returns the target associated with the object id.
     */
    static Target getTarget(ObjectEndpoint oe) {
        synchronized (tableLock) {
            return objTable.get(oe);
        }
    }

    /**
     * Returns the target associated with the remote object
     */
    public static Target getTarget(Remote impl) {
        synchronized (tableLock) {
            return implTable.get(new WeakRef(impl));
        }
    }

    /**
     * Returns the stub for the remote object <b>obj</b> passed
     * as a parameter. This operation is only valid <i>after</i>
     * the object has been exported.
     *
     * @return the stub for the remote object, <b>obj</b>.
     * @exception NoSuchObjectException if the stub for the
     * remote object could not be found.
     */
    public static Remote getStub(Remote impl)
        throws NoSuchObjectException
    {
        Target target = getTarget(impl);
        if (target == null) {
            throw new NoSuchObjectException("object not exported");
        } else {
            return target.getStub();
        }
    }

   /**
    * Remove the remote object, obj, from the RMI runtime. If
    * successful, the object can no longer accept incoming RMI calls.
    * If the force parameter is true, the object is forcibly unexported
    * even if there are pending calls to the remote object or the
    * remote object still has calls in progress.  If the force
    * parameter is false, the object is only unexported if there are
    * no pending or in progress calls to the object.
    *
    * @param obj the remote object to be unexported
    * @param force if true, unexports the object even if there are
    * pending or in-progress calls; if false, only unexports the object
    * if there are no pending or in-progress calls
    * @return true if operation is successful, false otherwise
    * @exception NoSuchObjectException if the remote object is not
    * currently exported
    */
   public static boolean unexportObject(Remote obj, boolean force)
        throws java.rmi.NoSuchObjectException
    {
        synchronized (tableLock) {
            Target target = getTarget(obj);
            if (target == null) {
                throw new NoSuchObjectException("object not exported");
            } else {
                if (target.unexport(force)) {
                    removeTarget(target);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Add target to object table.  If it is not a permanent entry, then
     * make sure that reaper thread is running to remove collected entries
     * and keep VM alive.
     */
    static void putTarget(Target target) throws ExportException {
        ObjectEndpoint oe = target.getObjectEndpoint();
        WeakRef weakImpl = target.getWeakImpl();

        if (DGCImpl.dgcLog.isLoggable(Log.VERBOSE)) {
            DGCImpl.dgcLog.log(Log.VERBOSE, "add object " + oe);
        }

        synchronized (tableLock) {
            /**
             * Do nothing if impl has already been collected (see 6597112). Check while
             * holding tableLock to ensure that Reaper cannot process weakImpl in between
             * null check and put/increment effects.
             */
            if (target.getImpl() != null) {
                if (objTable.containsKey(oe)) {
                    throw new ExportException(
                        "internal error: ObjID already in use");
                } else if (implTable.containsKey(weakImpl)) {
                    throw new ExportException("object already exported");
                }

                objTable.put(oe, target);
                implTable.put(weakImpl, target);

                if (!target.isPermanent()) {
                    incrementKeepAliveCount();
                }
            }
        }
    }

    /**
     * Remove target from object table.
     *
     * NOTE: This method must only be invoked while synchronized on
     * the "tableLock" object, because it does not do so itself.
     */
    private static void removeTarget(Target target) {
        // assert Thread.holdsLock(tableLock);

        ObjectEndpoint oe = target.getObjectEndpoint();
        WeakRef weakImpl = target.getWeakImpl();

        if (DGCImpl.dgcLog.isLoggable(Log.VERBOSE)) {
            DGCImpl.dgcLog.log(Log.VERBOSE, "remove object " + oe);
        }

        objTable.remove(oe);
        implTable.remove(weakImpl);

        target.markRemoved();   // handles decrementing keep-alive count
    }

    /**
     * Process client VM signalling reference for given ObjID: forward to
     * corresponding Target entry.  If ObjID is not found in table,
     * no action is taken.
     */
    static void referenced(ObjID id, long sequenceNum, VMID vmid) {
        synchronized (tableLock) {
            ObjectEndpoint oe =
                new ObjectEndpoint(id, Transport.currentTransport());
            Target target = objTable.get(oe);
            if (target != null) {
                target.referenced(sequenceNum, vmid);
            }
        }
    }

    /**
     * Process client VM dropping reference for given ObjID: forward to
     * corresponding Target entry.  If ObjID is not found in table,
     * no action is taken.
     */
    static void unreferenced(ObjID id, long sequenceNum, VMID vmid,
                             boolean strong)
    {
        synchronized (tableLock) {
            ObjectEndpoint oe =
                new ObjectEndpoint(id, Transport.currentTransport());
            Target target = objTable.get(oe);
            if (target != null)
                target.unreferenced(sequenceNum, vmid, strong);
        }
    }

    /**
     * Increments the "keep-alive count".
     *
     * The "keep-alive count" is the number of non-permanent remote objects
     * that are either in the object table or still have calls in progress.
     * Therefore, this method should be invoked exactly once for every
     * non-permanent remote object exported (a remote object must be
     * exported before it can have any calls in progress).
     *
     * The VM is "kept alive" while the keep-alive count is greater than
     * zero; this is accomplished by keeping a non-daemon thread running.
     *
     * Because non-permanent objects are those that can be garbage
     * collected while exported, and thus those for which the "reaper"
     * thread operates, the reaper thread also serves as the non-daemon
     * VM keep-alive thread; a new reaper thread is created if necessary.
     */
    @SuppressWarnings("removal")
    static void incrementKeepAliveCount() {
        synchronized (keepAliveLock) {
            keepAliveCount++;

            if (reaper == null) {
                reaper = AccessController.doPrivileged(
                    new NewThreadAction(new Reaper(), "Reaper", false));
                reaper.start();
            }

            /*
             * While there are non-"permanent" objects in the object table,
             * request a maximum latency for inspecting the entire heap
             * from the local garbage collector, to place an upper bound
             * on the time to discover remote objects that have become
             * unreachable (and thus can be removed from the table).
             */
            if (gcLatencyRequest == null) {
                gcLatencyRequest = GC.requestLatency(gcInterval);
            }
        }
    }

    /**
     * Decrements the "keep-alive count".
     *
     * The "keep-alive count" is the number of non-permanent remote objects
     * that are either in the object table or still have calls in progress.
     * Therefore, this method should be invoked exactly once for every
     * previously-exported non-permanent remote object that both has been
     * removed from the object table and has no calls still in progress.
     *
     * If the keep-alive count is decremented to zero, then the current
     * reaper thread is terminated to cease keeping the VM alive (and
     * because there are no more non-permanent remote objects to reap).
     */
    @SuppressWarnings("removal")
    static void decrementKeepAliveCount() {
        synchronized (keepAliveLock) {
            keepAliveCount--;

            if (keepAliveCount == 0) {
                if (!(reaper != null)) { throw new AssertionError(); }
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        reaper.interrupt();
                        return null;
                    }
                });
                reaper = null;

                /*
                 * If there are no longer any non-permanent objects in the
                 * object table, we are no longer concerned with the latency
                 * of local garbage collection here.
                 */
                gcLatencyRequest.cancel();
                gcLatencyRequest = null;
            }
        }
    }

    /**
     * The Reaper thread waits for notifications that weak references in the
     * object table have been cleared.  When it receives a notification, it
     * removes the corresponding entry from the table.
     *
     * Since the Reaper is created as a non-daemon thread, it also serves
     * to keep the VM from exiting while there are objects in the table
     * (other than permanent entries that should neither be reaped nor
     * keep the VM alive).
     */
    private static class Reaper implements Runnable {

        public void run() {
            try {
                do {
                    // wait for next cleared weak reference
                    WeakRef weakImpl = (WeakRef) reapQueue.remove();

                    synchronized (tableLock) {
                        Target target = implTable.get(weakImpl);
                        if (target != null) {
                            if (!target.isEmpty()) {
                                throw new Error(
                                    "object with known references collected");
                            } else if (target.isPermanent()) {
                                throw new Error("permanent object collected");
                            }
                            removeTarget(target);
                        }
                    }
                } while (!Thread.interrupted());
            } catch (InterruptedException e) {
                // pass away if interrupted
            }
        }
    }
}
