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

import java.rmi.Remote;
import java.rmi.NoSuchObjectException;
import java.rmi.dgc.VMID;
import java.rmi.server.ObjID;
import java.rmi.server.Unreferenced;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import sun.rmi.runtime.Log;
import sun.rmi.runtime.NewThreadAction;
import sun.rmi.server.Dispatcher;

/**
 * A target contains information pertaining to a remote object that
 * resides in this address space.  Targets are located via the
 * ObjectTable.
 */
public final class Target {
    /** object id for target */
    private final ObjID id;
    /** flag indicating whether target is subject to collection */
    private final boolean permanent;
    /** weak reference to remote object implementation */
    private final WeakRef weakImpl;
    /** dispatcher for remote object */
    private volatile Dispatcher disp;
    /** stub for remote object */
    private final Remote stub;
    /** set of clients that hold references to this target */
    private final Vector<VMID> refSet = new Vector<>();
    /** table that maps client endpoints to sequence numbers */
    private final Hashtable<VMID, SequenceEntry> sequenceTable =
        new Hashtable<>(5);
    /** access control context in which target was created */
    @SuppressWarnings("removal")
    private final AccessControlContext acc;
    /** context class loader in which target was created */
    private final ClassLoader ccl;
    /** number of pending/executing calls */
    private int callCount = 0;
    /** true if this target has been removed from the object table */
    private boolean removed = false;
    /**
     * the transport through which this target was exported and
     * through which remote calls will be allowed
     */
    private volatile Transport exportedTransport = null;

    /** number to identify next callback thread created here */
    private static int nextThreadNum = 0;

    /**
     * Construct a Target for a remote object "impl" with
     * a specific object id.
     *
     * If "permanent" is true, then the impl is pinned permanently
     * (the impl will not be collected via distributed and/or local
     * GC).  If "on" is false, than the impl is subject to
     * collection. Permanent objects do not keep a server from
     * exiting.
     */
    @SuppressWarnings("removal")
    public Target(Remote impl, Dispatcher disp, Remote stub, ObjID id,
                  boolean permanent)
    {
        this.weakImpl = new WeakRef(impl, ObjectTable.reapQueue);
        this.disp = disp;
        this.stub = stub;
        this.id = id;
        this.acc = AccessController.getContext();

        /*
         * Fix for 4149366: so that downloaded parameter types unmarshalled
         * for this impl will be compatible with types known only to the
         * impl class's class loader (when it's not identical to the
         * exporting thread's context class loader), mark the impl's class
         * loader as the loader to use as the context class loader in the
         * server's dispatch thread while a call to this impl is being
         * processed (unless this exporting thread's context class loader is
         * a child of the impl's class loader, such as when a registry is
         * exported by an application, in which case this thread's context
         * class loader is preferred).
         */
        ClassLoader threadContextLoader =
            Thread.currentThread().getContextClassLoader();
        ClassLoader serverLoader = impl.getClass().getClassLoader();
        if (checkLoaderAncestry(threadContextLoader, serverLoader)) {
            this.ccl = threadContextLoader;
        } else {
            this.ccl = serverLoader;
        }

        this.permanent = permanent;
        if (permanent) {
            pinImpl();
        }
    }

    /**
     * Return true if the first class loader is a child of (or identical
     * to) the second class loader.  Either loader may be "null", which is
     * considered to be the parent of any non-null class loader.
     *
     * (utility method added for the 1.2beta4 fix for 4149366)
     */
    private static boolean checkLoaderAncestry(ClassLoader child,
                                               ClassLoader ancestor)
    {
        if (ancestor == null) {
            return true;
        } else if (child == null) {
            return false;
        } else {
            for (ClassLoader parent = child;
                 parent != null;
                 parent = parent.getParent())
            {
                if (parent == ancestor) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Get the stub (proxy) object for this target
     */
    public Remote getStub() {
        return stub;
    }

    /**
     * Returns the object endpoint for the target.
     */
    ObjectEndpoint getObjectEndpoint() {
        return new ObjectEndpoint(id, exportedTransport);
    }

    /**
     * Get the weak reference for the Impl of this target.
     */
    WeakRef getWeakImpl() {
        return weakImpl;
    }

    /**
     * Returns the dispatcher for this remote object target.
     */
    Dispatcher getDispatcher() {
        return disp;
    }

    @SuppressWarnings("removal")
    AccessControlContext getAccessControlContext() {
        return acc;
    }

    ClassLoader getContextClassLoader() {
        return ccl;
    }

    /**
     * Get the impl for this target.
     * Note: this may return null if the impl has been garbage collected.
     * (currently, there is no need to make this method public)
     */
    Remote getImpl() {
        return (Remote)weakImpl.get();
    }

    /**
     * Returns true if the target is permanent.
     */
    boolean isPermanent() {
        return permanent;
    }

    /**
     * Pin impl in target. Pin the WeakRef object so it holds a strong
     * reference to the object to it will not be garbage collected locally.
     * This way there is a single object responsible for the weak ref
     * mechanism.
     */
    synchronized void pinImpl() {
        weakImpl.pin();
    }

    /**
     * Unpin impl in target.  Weaken the reference to impl so that it
     * can be garbage collected locally. But only if there the refSet
     * is empty.  All of the weak/strong handling is in WeakRef
     */
    synchronized void unpinImpl() {
        /* only unpin if:
         * a) impl is not permanent, and
         * b) impl is not already unpinned, and
         * c) there are no external references (outside this
         *    address space) for the impl
         */
        if (!permanent && refSet.isEmpty()) {
            weakImpl.unpin();
        }
    }

    /**
     * Enable the transport through which remote calls to this target
     * are allowed to be set if it has not already been set.
     */
    void setExportedTransport(Transport exportedTransport) {
        if (this.exportedTransport == null) {
            this.exportedTransport = exportedTransport;
        }
    }

    /**
     * Add an endpoint to the remembered set.  Also adds a notifier
     * to call back if the address space associated with the endpoint
     * dies.
     */
    synchronized void referenced(long sequenceNum, VMID vmid) {
        // check sequence number for vmid
        SequenceEntry entry = sequenceTable.get(vmid);
        if (entry == null) {
            sequenceTable.put(vmid, new SequenceEntry(sequenceNum));
        } else if (entry.sequenceNum < sequenceNum) {
            entry.update(sequenceNum);
        } else  {
            // late dirty call; ignore.
            return;
        }

        if (!refSet.contains(vmid)) {
            /*
             * A Target must be pinned while its refSet is not empty.  It may
             * have become unpinned if external LiveRefs only existed in
             * serialized form for some period of time, or if a client failed
             * to renew its lease due to a transient network failure.  So,
             * make sure that it is pinned here; this fixes bugid 4069644.
             */
            pinImpl();
            if (getImpl() == null)      // too late if impl was collected
                return;

            if (DGCImpl.dgcLog.isLoggable(Log.VERBOSE)) {
                DGCImpl.dgcLog.log(Log.VERBOSE, "add to dirty set: " + vmid);
            }

            refSet.addElement(vmid);

            DGCImpl.getDGCImpl().registerTarget(vmid, this);
        }
    }

    /**
     * Remove endpoint from remembered set.  If set becomes empty,
     * remove server from Transport's object table.
     */
    synchronized void unreferenced(long sequenceNum, VMID vmid, boolean strong)
    {
        // check sequence number for vmid
        SequenceEntry entry = sequenceTable.get(vmid);
        if (entry == null || entry.sequenceNum > sequenceNum) {
            // late clean call; ignore
            return;
        } else if (strong) {
            // strong clean call; retain sequenceNum
            entry.retain(sequenceNum);
        } else if (entry.keep == false) {
            // get rid of sequence number
            sequenceTable.remove(vmid);
        }

        if (DGCImpl.dgcLog.isLoggable(Log.VERBOSE)) {
            DGCImpl.dgcLog.log(Log.VERBOSE, "remove from dirty set: " + vmid);
        }

        refSetRemove(vmid);
    }

    /**
     * Remove endpoint from the reference set.
     */
    @SuppressWarnings("removal")
    synchronized private void refSetRemove(VMID vmid) {
        // remove notification request
        DGCImpl.getDGCImpl().unregisterTarget(vmid, this);

        if (refSet.removeElement(vmid) && refSet.isEmpty()) {
            // reference set is empty, so server can be garbage collected.
            // remove object from table.
            if (DGCImpl.dgcLog.isLoggable(Log.VERBOSE)) {
                DGCImpl.dgcLog.log(Log.VERBOSE,
                    "reference set is empty: target = " + this);
            }

            /*
             * If the remote object implements the Unreferenced interface,
             * invoke its unreferenced callback in a separate thread.
             */
            Remote obj = getImpl();
            if (obj instanceof Unreferenced) {
                final Unreferenced unrefObj = (Unreferenced) obj;
                AccessController.doPrivileged(
                    new NewThreadAction(() -> {
                        Thread.currentThread().setContextClassLoader(ccl);
                        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                            unrefObj.unreferenced();
                            return null;
                        }, acc);
                    }, "Unreferenced-" + nextThreadNum++, false, true)).start();
                    // REMIND: access to nextThreadNum not synchronized; you care?
            }

            unpinImpl();
        }
    }

    /**
     * Mark this target as not accepting new calls if any of the
     * following conditions exist: a) the force parameter is true,
     * b) the target's call count is zero, or c) the object is already
     * not accepting calls. Returns true if target is marked as not
     * accepting new calls; returns false otherwise.
     */
    synchronized boolean unexport(boolean force) {

        if ((force == true) || (callCount == 0) || (disp == null)) {
            disp = null;
            /*
             * Fix for 4331349: unpin object so that it may be gc'd.
             * Also, unregister all vmids referencing this target
             * so target can be gc'd.
             */
            unpinImpl();
            DGCImpl dgc = DGCImpl.getDGCImpl();
            Enumeration<VMID> enum_ = refSet.elements();
            while (enum_.hasMoreElements()) {
                VMID vmid = enum_.nextElement();
                dgc.unregisterTarget(vmid, this);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Mark this target as having been removed from the object table.
     */
    synchronized void markRemoved() {
        if (!(!removed)) { throw new AssertionError(); }

        removed = true;
        if (!permanent && callCount == 0) {
            ObjectTable.decrementKeepAliveCount();
        }

        if (exportedTransport != null) {
            exportedTransport.targetUnexported();
        }
    }

    /**
     * Increment call count.
     */
    synchronized void incrementCallCount() throws NoSuchObjectException {

        if (disp != null) {
            callCount ++;
        } else {
            throw new NoSuchObjectException("object not accepting new calls");
        }
    }

    /**
     * Decrement call count.
     */
    synchronized void decrementCallCount() {

        if (--callCount < 0) {
            throw new Error("internal error: call count less than zero");
        }

        /*
         * The "keep-alive count" is the number of non-permanent remote
         * objects that are either in the object table or still have calls
         * in progress.  Therefore, this state change may affect the
         * keep-alive count: if this target is for a non-permanent remote
         * object that has been removed from the object table and now has a
         * call count of zero, it needs to be decremented.
         */
        if (!permanent && removed && callCount == 0) {
            ObjectTable.decrementKeepAliveCount();
        }
    }

    /**
     * Returns true if remembered set is empty; otherwise returns
     * false
     */
    boolean isEmpty() {
        return refSet.isEmpty();
    }

    /**
     * This method is called if the address space associated with the
     * vmid dies.  In that case, the vmid should be removed
     * from the reference set.
     */
    synchronized public void vmidDead(VMID vmid) {
        if (DGCImpl.dgcLog.isLoggable(Log.BRIEF)) {
            DGCImpl.dgcLog.log(Log.BRIEF, "removing endpoint " +
                            vmid + " from reference set");
        }

        sequenceTable.remove(vmid);
        refSetRemove(vmid);
    }
}

class SequenceEntry {
    long sequenceNum;
    boolean keep;

    SequenceEntry(long sequenceNum) {
        this.sequenceNum = sequenceNum;
        keep = false;
    }

    void retain(long sequenceNum) {
        this.sequenceNum = sequenceNum;
        keep = true;
    }

    void update(long sequenceNum) {
        this.sequenceNum = sequenceNum;
    }
}
