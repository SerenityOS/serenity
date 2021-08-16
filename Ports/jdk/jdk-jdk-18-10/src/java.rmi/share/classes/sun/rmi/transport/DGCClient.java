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

import java.io.InvalidClassException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.net.SocketPermission;
import java.rmi.UnmarshalException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.dgc.DGC;
import java.rmi.dgc.Lease;
import java.rmi.dgc.VMID;
import java.rmi.server.ObjID;

import sun.rmi.runtime.Log;
import sun.rmi.runtime.NewThreadAction;
import sun.rmi.server.UnicastRef;
import sun.rmi.server.Util;

import java.security.AccessControlContext;
import java.security.Permissions;
import java.security.ProtectionDomain;

/**
 * DGCClient implements the client-side of the RMI distributed garbage
 * collection system.
 *
 * The external interface to DGCClient is the "registerRefs" method.
 * When a LiveRef to a remote object enters the VM, it needs to be
 * registered with the DGCClient to participate in distributed garbage
 * collection.
 *
 * When the first LiveRef to a particular remote object is registered,
 * a "dirty" call is made to the server-side distributed garbage
 * collector for the remote object, which returns a lease guaranteeing
 * that the server-side DGC will not collect the remote object for a
 * certain period of time.  While LiveRef instances to remote objects
 * on a particular server exist, the DGCClient periodically sends more
 * "dirty" calls to renew its lease.
 *
 * The DGCClient tracks the local reachability of registered LiveRef
 * instances (using phantom references).  When the LiveRef instance
 * for a particular remote object becomes garbage collected locally,
 * a "clean" call is made to the server-side distributed garbage
 * collector, indicating that the server no longer needs to keep the
 * remote object alive for this client.
 *
 * @see java.rmi.dgc.DGC, sun.rmi.transport.DGCImpl
 *
 * @author  Ann Wollrath
 * @author  Peter Jones
 */
final class DGCClient {

    /** next sequence number for DGC calls (access synchronized on class) */
    private static long nextSequenceNum = Long.MIN_VALUE;

    /** unique identifier for this VM as a client of DGC */
    private static VMID vmid = new VMID();

    /** lease duration to request (usually ignored by server) */
    @SuppressWarnings("removal")
    private static final long leaseValue =              // default 10 minutes
        AccessController.doPrivileged((PrivilegedAction<Long>) () ->
            Long.getLong("java.rmi.dgc.leaseValue", 600000));

    /** maximum interval between retries of failed clean calls */
    @SuppressWarnings("removal")
    private static final long cleanInterval =           // default 3 minutes
        AccessController.doPrivileged((PrivilegedAction<Long>) () ->
            Long.getLong("sun.rmi.dgc.cleanInterval", 180000));

    /** maximum interval between complete garbage collections of local heap */
    @SuppressWarnings("removal")
    private static final long gcInterval =              // default 1 hour
        AccessController.doPrivileged((PrivilegedAction<Long>) () ->
            Long.getLong("sun.rmi.dgc.client.gcInterval", 3600000));

    /** minimum retry count for dirty calls that fail */
    private static final int dirtyFailureRetries = 5;

    /** retry count for clean calls that fail with ConnectException */
    private static final int cleanFailureRetries = 5;

    /** constant empty ObjID array for lease renewal optimization */
    private static final ObjID[] emptyObjIDArray = new ObjID[0];

    /** ObjID for server-side DGC object */
    private static final ObjID dgcID = new ObjID(ObjID.DGC_ID);

    /**
     * An AccessControlContext with only socket permissions,
     * suitable for an RMIClientSocketFactory.
     */
    @SuppressWarnings("removal")
    private static final AccessControlContext SOCKET_ACC = createSocketAcc();

    @SuppressWarnings("removal")
    private static AccessControlContext createSocketAcc() {
        Permissions perms = new Permissions();
        perms.add(new SocketPermission("*", "connect,resolve"));
        ProtectionDomain[] pd = { new ProtectionDomain(null, perms) };
        return new AccessControlContext(pd);
    }

    /*
     * Disallow anyone from creating one of these.
     */
    private DGCClient() {}

    /**
     * Register the LiveRef instances in the supplied list to participate
     * in distributed garbage collection.
     *
     * All of the LiveRefs in the list must be for remote objects at the
     * given endpoint.
     */
    static void registerRefs(Endpoint ep, List<LiveRef> refs) {
        /*
         * Look up the given endpoint and register the refs with it.
         * The retrieved entry may get removed from the global endpoint
         * table before EndpointEntry.registerRefs() is able to acquire
         * its lock; in this event, it returns false, and we loop and
         * try again.
         */
        EndpointEntry epEntry;
        do {
            epEntry = EndpointEntry.lookup(ep);
        } while (!epEntry.registerRefs(refs));
    }

    /**
     * Get the next sequence number to be used for a dirty or clean
     * operation from this VM.  This method should only be called while
     * synchronized on the EndpointEntry whose data structures the
     * operation affects.
     */
    private static synchronized long getNextSequenceNum() {
        return nextSequenceNum++;
    }

    /**
     * Given the length of a lease and the time that it was granted,
     * compute the absolute time at which it should be renewed, giving
     * room for reasonable computational and communication delays.
     */
    private static long computeRenewTime(long grantTime, long duration) {
        /*
         * REMIND: This algorithm should be more sophisticated, waiting
         * a longer fraction of the lease duration for longer leases.
         */
        return grantTime + (duration / 2);
    }

    /**
     * EndpointEntry encapsulates the client-side DGC information specific
     * to a particular Endpoint.  Of most significance is the table that
     * maps LiveRef value to RefEntry objects and the renew/clean thread
     * that handles asynchronous client-side DGC operations.
     */
    private static class EndpointEntry {

        /** the endpoint that this entry is for */
        private Endpoint endpoint;
        /** synthesized reference to the remote server-side DGC */
        private DGC dgc;

        /** table of refs held for endpoint: maps LiveRef to RefEntry */
        private Map<LiveRef, RefEntry> refTable = new HashMap<>(5);
        /** set of RefEntry instances from last (failed) dirty call */
        private Set<RefEntry> invalidRefs = new HashSet<>(5);

        /** true if this entry has been removed from the global table */
        private boolean removed = false;

        /** absolute time to renew current lease to this endpoint */
        private long renewTime = Long.MAX_VALUE;
        /** absolute time current lease to this endpoint will expire */
        private long expirationTime = Long.MIN_VALUE;
        /** count of recent dirty calls that have failed */
        private int dirtyFailures = 0;
        /** absolute time of first recent failed dirty call */
        private long dirtyFailureStartTime;
        /** (average) elapsed time for recent failed dirty calls */
        private long dirtyFailureDuration;

        /** renew/clean thread for handling lease renewals and clean calls */
        private Thread renewCleanThread;
        /** true if renew/clean thread may be interrupted */
        private boolean interruptible = false;

        /** reference queue for phantom references */
        private ReferenceQueue<LiveRef> refQueue = new ReferenceQueue<>();
        /** set of clean calls that need to be made */
        private Set<CleanRequest> pendingCleans = new HashSet<>(5);

        /** global endpoint table: maps Endpoint to EndpointEntry */
        private static Map<Endpoint,EndpointEntry> endpointTable = new HashMap<>(5);
        /** handle for GC latency request (for future cancellation) */
        private static GC.LatencyRequest gcLatencyRequest = null;

        /**
         * Look up the EndpointEntry for the given Endpoint.  An entry is
         * created if one does not already exist.
         */
        public static EndpointEntry lookup(Endpoint ep) {
            synchronized (endpointTable) {
                EndpointEntry entry = endpointTable.get(ep);
                if (entry == null) {
                    entry = new EndpointEntry(ep);
                    endpointTable.put(ep, entry);
                    /*
                     * While we are tracking live remote references registered
                     * in this VM, request a maximum latency for inspecting the
                     * entire heap from the local garbage collector, to place
                     * an upper bound on the time to discover remote references
                     * that have become unreachable (see bugid 4171278).
                     */
                    if (gcLatencyRequest == null) {
                        gcLatencyRequest = GC.requestLatency(gcInterval);
                    }
                }
                return entry;
            }
        }

        @SuppressWarnings("removal")
        private EndpointEntry(final Endpoint endpoint) {
            this.endpoint = endpoint;
            try {
                LiveRef dgcRef = new LiveRef(dgcID, endpoint, false);
                dgc = (DGC) Util.createProxy(DGCImpl.class,
                                             new UnicastRef(dgcRef), true);
            } catch (RemoteException e) {
                throw new Error("internal error creating DGC stub");
            }
            renewCleanThread =  AccessController.doPrivileged(
                new NewThreadAction(new RenewCleanThread(),
                                    "RenewClean-" + endpoint, true));
            renewCleanThread.start();
        }

        /**
         * Register the LiveRef instances in the supplied list to participate
         * in distributed garbage collection.
         *
         * This method returns false if this entry was removed from the
         * global endpoint table (because it was empty) before these refs
         * could be registered.  In that case, a new EndpointEntry needs
         * to be looked up.
         *
         * This method must NOT be called while synchronized on this entry.
         */
        public boolean registerRefs(List<LiveRef> refs) {
            assert !Thread.holdsLock(this);

            Set<RefEntry> refsToDirty = null;     // entries for refs needing dirty
            long sequenceNum;           // sequence number for dirty call

            synchronized (this) {
                if (removed) {
                    return false;
                }

                Iterator<LiveRef> iter = refs.iterator();
                while (iter.hasNext()) {
                    LiveRef ref = iter.next();
                    assert ref.getEndpoint().equals(endpoint);

                    RefEntry refEntry = refTable.get(ref);
                    if (refEntry == null) {
                        LiveRef refClone = (LiveRef) ref.clone();
                        refEntry = new RefEntry(refClone);
                        refTable.put(refClone, refEntry);
                        if (refsToDirty == null) {
                            refsToDirty = new HashSet<>(5);
                        }
                        refsToDirty.add(refEntry);
                    }

                    refEntry.addInstanceToRefSet(ref);
                }

                if (refsToDirty == null) {
                    return true;
                }

                refsToDirty.addAll(invalidRefs);
                invalidRefs.clear();

                sequenceNum = getNextSequenceNum();
            }

            makeDirtyCall(refsToDirty, sequenceNum);
            return true;
        }

        /**
         * Remove the given RefEntry from the ref table.  If that makes
         * the ref table empty, remove this entry from the global endpoint
         * table.
         *
         * This method must ONLY be called while synchronized on this entry.
         */
        private void removeRefEntry(RefEntry refEntry) {
            assert Thread.holdsLock(this);
            assert !removed;
            assert refTable.containsKey(refEntry.getRef());

            refTable.remove(refEntry.getRef());
            invalidRefs.remove(refEntry);
            if (refTable.isEmpty()) {
                synchronized (endpointTable) {
                    endpointTable.remove(endpoint);
                    Transport transport = endpoint.getOutboundTransport();
                    transport.free(endpoint);
                    /*
                     * If there are no longer any live remote references
                     * registered, we are no longer concerned with the
                     * latency of local garbage collection here.
                     */
                    if (endpointTable.isEmpty()) {
                        assert gcLatencyRequest != null;
                        gcLatencyRequest.cancel();
                        gcLatencyRequest = null;
                    }
                    removed = true;
                }
            }
        }

        /**
         * Make a DGC dirty call to this entry's endpoint, for the ObjIDs
         * corresponding to the given set of refs and with the given
         * sequence number.
         *
         * This method must NOT be called while synchronized on this entry.
         */
        private void makeDirtyCall(Set<RefEntry> refEntries, long sequenceNum) {
            assert !Thread.holdsLock(this);

            ObjID[] ids;
            if (refEntries != null) {
                ids = createObjIDArray(refEntries);
            } else {
                ids = emptyObjIDArray;
            }

            long startTime = System.currentTimeMillis();
            try {
                Lease lease =
                    dgc.dirty(ids, sequenceNum, new Lease(vmid, leaseValue));
                long duration = lease.getValue();

                long newRenewTime = computeRenewTime(startTime, duration);
                long newExpirationTime = startTime + duration;

                synchronized (this) {
                    dirtyFailures = 0;
                    setRenewTime(newRenewTime);
                    expirationTime = newExpirationTime;
                }

            } catch (Exception e) {
                long endTime = System.currentTimeMillis();

                synchronized (this) {
                    dirtyFailures++;

                    if (e instanceof UnmarshalException
                            && e.getCause() instanceof InvalidClassException) {
                        DGCImpl.dgcLog.log(Log.BRIEF, "InvalidClassException exception in DGC dirty call", e);
                        return;             // protocol error, do not register these refs
                    }

                    if (dirtyFailures == 1) {
                        /*
                         * If this was the first recent failed dirty call,
                         * reschedule another one immediately, in case there
                         * was just a transient network problem, and remember
                         * the start time and duration of this attempt for
                         * future calculations of the delays between retries.
                         */
                        dirtyFailureStartTime = startTime;
                        dirtyFailureDuration = endTime - startTime;
                        setRenewTime(endTime);
                    } else {
                        /*
                         * For each successive failed dirty call, wait for a
                         * (binary) exponentially increasing delay before
                         * retrying, to avoid network congestion.
                         */
                        int n = dirtyFailures - 2;
                        if (n == 0) {
                            /*
                             * Calculate the initial retry delay from the
                             * average time elapsed for each of the first
                             * two failed dirty calls.  The result must be
                             * at least 1000ms, to prevent a tight loop.
                             */
                            dirtyFailureDuration =
                                Math.max((dirtyFailureDuration +
                                          (endTime - startTime)) >> 1, 1000);
                        }
                        long newRenewTime =
                            endTime + (dirtyFailureDuration << n);

                        /*
                         * Continue if the last known held lease has not
                         * expired, or else at least a fixed number of times,
                         * or at least until we've tried for a fixed amount
                         * of time (the default lease value we request).
                         */
                        if (newRenewTime < expirationTime ||
                            dirtyFailures < dirtyFailureRetries ||
                            newRenewTime < dirtyFailureStartTime + leaseValue)
                        {
                            setRenewTime(newRenewTime);
                        } else {
                            /*
                             * Give up: postpone lease renewals until next
                             * ref is registered for this endpoint.
                             */
                            setRenewTime(Long.MAX_VALUE);
                        }
                    }

                    if (refEntries != null) {
                        /*
                         * Add all of these refs to the set of refs for this
                         * endpoint that may be invalid (this VM may not be in
                         * the server's referenced set), so that we will
                         * attempt to explicitly dirty them again in the
                         * future.
                         */
                        invalidRefs.addAll(refEntries);

                        /*
                         * Record that a dirty call has failed for all of these
                         * refs, so that clean calls for them in the future
                         * will be strong.
                         */
                        Iterator<RefEntry> iter = refEntries.iterator();
                        while (iter.hasNext()) {
                            RefEntry refEntry = iter.next();
                            refEntry.markDirtyFailed();
                        }
                    }

                    /*
                     * If the last known held lease will have expired before
                     * the next renewal, all refs might be invalid.
                     */
                    if (renewTime >= expirationTime) {
                        invalidRefs.addAll(refTable.values());
                    }
                }
            }
        }

        /**
         * Set the absolute time at which the lease for this entry should
         * be renewed.
         *
         * This method must ONLY be called while synchronized on this entry.
         */
        @SuppressWarnings("removal")
        private void setRenewTime(long newRenewTime) {
            assert Thread.holdsLock(this);

            if (newRenewTime < renewTime) {
                renewTime = newRenewTime;
                if (interruptible) {
                    AccessController.doPrivileged(
                        new PrivilegedAction<Void>() {
                            public Void run() {
                            renewCleanThread.interrupt();
                            return null;
                        }
                    });
                }
            } else {
                renewTime = newRenewTime;
            }
        }

        /**
         * RenewCleanThread handles the asynchronous client-side DGC activity
         * for this entry: renewing the leases and making clean calls.
         */
        private class RenewCleanThread implements Runnable {

            @SuppressWarnings("removal")
            public void run() {
                do {
                    long timeToWait;
                    RefEntry.PhantomLiveRef phantom = null;
                    boolean needRenewal = false;
                    Set<RefEntry> refsToDirty = null;
                    long sequenceNum = Long.MIN_VALUE;

                    synchronized (EndpointEntry.this) {
                        /*
                         * Calculate time to block (waiting for phantom
                         * reference notifications).  It is the time until the
                         * lease renewal should be done, bounded on the low
                         * end by 1 ms so that the reference queue will always
                         * get processed, and if there are pending clean
                         * requests (remaining because some clean calls
                         * failed), bounded on the high end by the maximum
                         * clean call retry interval.
                         */
                        long timeUntilRenew =
                            renewTime - System.currentTimeMillis();
                        timeToWait = Math.max(timeUntilRenew, 1);
                        if (!pendingCleans.isEmpty()) {
                            timeToWait = Math.min(timeToWait, cleanInterval);
                        }

                        /*
                         * Set flag indicating that it is OK to interrupt this
                         * thread now, such as if a earlier lease renewal time
                         * is set, because we are only going to be blocking
                         * and can deal with interrupts.
                         */
                        interruptible = true;
                    }

                    try {
                        /*
                         * Wait for the duration calculated above for any of
                         * our phantom references to be enqueued.
                         */
                        phantom = (RefEntry.PhantomLiveRef)
                            refQueue.remove(timeToWait);
                    } catch (InterruptedException e) {
                    }

                    synchronized (EndpointEntry.this) {
                        /*
                         * Set flag indicating that it is NOT OK to interrupt
                         * this thread now, because we may be undertaking I/O
                         * operations that should not be interrupted (and we
                         * will not be blocking arbitrarily).
                         */
                        interruptible = false;
                        Thread.interrupted();   // clear interrupted state

                        /*
                         * If there was a phantom reference enqueued, process
                         * it and all the rest on the queue, generating
                         * clean requests as necessary.
                         */
                        if (phantom != null) {
                            processPhantomRefs(phantom);
                        }

                        /*
                         * Check if it is time to renew this entry's lease.
                         */
                        long currentTime = System.currentTimeMillis();
                        if (currentTime > renewTime) {
                            needRenewal = true;
                            if (!invalidRefs.isEmpty()) {
                                refsToDirty = invalidRefs;
                                invalidRefs = new HashSet<>(5);
                            }
                            sequenceNum = getNextSequenceNum();
                        }
                    }

                    boolean needRenewal_ = needRenewal;
                    Set<RefEntry> refsToDirty_ = refsToDirty;
                    long sequenceNum_ = sequenceNum;
                    AccessController.doPrivileged((PrivilegedAction<Void>)() -> {
                        if (needRenewal_) {
                            makeDirtyCall(refsToDirty_, sequenceNum_);
                        }

                        if (!pendingCleans.isEmpty()) {
                            makeCleanCalls();
                        }
                        return null;
                    }, SOCKET_ACC);
                } while (!removed || !pendingCleans.isEmpty());
            }
        }

        /**
         * Process the notification of the given phantom reference and any
         * others that are on this entry's reference queue.  Each phantom
         * reference is removed from its RefEntry's ref set.  All ref
         * entries that have no more registered instances are collected
         * into up to two batched clean call requests: one for refs
         * requiring a "strong" clean call, and one for the rest.
         *
         * This method must ONLY be called while synchronized on this entry.
         */
        private void processPhantomRefs(RefEntry.PhantomLiveRef phantom) {
            assert Thread.holdsLock(this);

            Set<RefEntry> strongCleans = null;
            Set<RefEntry> normalCleans = null;

            do {
                RefEntry refEntry = phantom.getRefEntry();
                refEntry.removeInstanceFromRefSet(phantom);
                if (refEntry.isRefSetEmpty()) {
                    if (refEntry.hasDirtyFailed()) {
                        if (strongCleans == null) {
                            strongCleans = new HashSet<>(5);
                        }
                        strongCleans.add(refEntry);
                    } else {
                        if (normalCleans == null) {
                            normalCleans = new HashSet<>(5);
                        }
                        normalCleans.add(refEntry);
                    }
                    removeRefEntry(refEntry);
                }
            } while ((phantom =
                (RefEntry.PhantomLiveRef) refQueue.poll()) != null);

            if (strongCleans != null) {
                pendingCleans.add(
                    new CleanRequest(createObjIDArray(strongCleans),
                                     getNextSequenceNum(), true));
            }
            if (normalCleans != null) {
                pendingCleans.add(
                    new CleanRequest(createObjIDArray(normalCleans),
                                     getNextSequenceNum(), false));
            }
        }

        /**
         * CleanRequest holds the data for the parameters of a clean call
         * that needs to be made.
         */
        private static class CleanRequest {

            final ObjID[] objIDs;
            final long sequenceNum;
            final boolean strong;

            /** how many times this request has failed */
            int failures = 0;

            CleanRequest(ObjID[] objIDs, long sequenceNum, boolean strong) {
                this.objIDs = objIDs;
                this.sequenceNum = sequenceNum;
                this.strong = strong;
            }
        }

        /**
         * Make all of the clean calls described by the clean requests in
         * this entry's set of "pending cleans".  Clean requests for clean
         * calls that succeed are removed from the "pending cleans" set.
         *
         * This method must NOT be called while synchronized on this entry.
         */
        private void makeCleanCalls() {
            assert !Thread.holdsLock(this);

            Iterator<CleanRequest> iter = pendingCleans.iterator();
            while (iter.hasNext()) {
                CleanRequest request = iter.next();
                try {
                    dgc.clean(request.objIDs, request.sequenceNum, vmid,
                              request.strong);
                    iter.remove();
                } catch (Exception e) {
                    /*
                     * Many types of exceptions here could have been
                     * caused by a transient failure, so try again a
                     * few times, but not forever.
                     */
                    if (++request.failures >= cleanFailureRetries) {
                        iter.remove();
                    }
                }
            }
        }

        /**
         * Create an array of ObjIDs (needed for the DGC remote calls)
         * from the ids in the given set of refs.
         */
        private static ObjID[] createObjIDArray(Set<RefEntry> refEntries) {
            ObjID[] ids = new ObjID[refEntries.size()];
            Iterator<RefEntry> iter = refEntries.iterator();
            for (int i = 0; i < ids.length; i++) {
                ids[i] = iter.next().getRef().getObjID();
            }
            return ids;
        }

        /**
         * RefEntry encapsulates the client-side DGC information specific
         * to a particular LiveRef value.  In particular, it contains a
         * set of phantom references to all of the instances of the LiveRef
         * value registered in the system (but not garbage collected
         * locally).
         */
        private class RefEntry {

            /** LiveRef value for this entry (not a registered instance) */
            private LiveRef ref;
            /** set of phantom references to registered instances */
            private Set<PhantomLiveRef> refSet = new HashSet<>(5);
            /** true if a dirty call containing this ref has failed */
            private boolean dirtyFailed = false;

            public RefEntry(LiveRef ref) {
                this.ref = ref;
            }

            /**
             * Return the LiveRef value for this entry (not a registered
             * instance).
             */
            public LiveRef getRef() {
                return ref;
            }

            /**
             * Add a LiveRef to the set of registered instances for this entry.
             *
             * This method must ONLY be invoked while synchronized on this
             * RefEntry's EndpointEntry.
             */
            public void addInstanceToRefSet(LiveRef ref) {
                assert Thread.holdsLock(EndpointEntry.this);
                assert ref.equals(this.ref);

                /*
                 * Only keep a phantom reference to the registered instance,
                 * so that it can be garbage collected normally (and we can be
                 * notified when that happens).
                 */
                refSet.add(new PhantomLiveRef(ref));
            }

            /**
             * Remove a PhantomLiveRef from the set of registered instances.
             *
             * This method must ONLY be invoked while synchronized on this
             * RefEntry's EndpointEntry.
             */
            public void removeInstanceFromRefSet(PhantomLiveRef phantom) {
                assert Thread.holdsLock(EndpointEntry.this);
                assert refSet.contains(phantom);
                refSet.remove(phantom);
            }

            /**
             * Return true if there are no registered LiveRef instances for
             * this entry still reachable in this VM.
             *
             * This method must ONLY be invoked while synchronized on this
             * RefEntry's EndpointEntry.
             */
            public boolean isRefSetEmpty() {
                assert Thread.holdsLock(EndpointEntry.this);
                return refSet.size() == 0;
            }

            /**
             * Record that a dirty call that explicitly contained this
             * entry's ref has failed.
             *
             * This method must ONLY be invoked while synchronized on this
             * RefEntry's EndpointEntry.
             */
            public void markDirtyFailed() {
                assert Thread.holdsLock(EndpointEntry.this);
                dirtyFailed = true;
            }

            /**
             * Return true if a dirty call that explicitly contained this
             * entry's ref has failed (and therefore a clean call for this
             * ref needs to be marked "strong").
             *
             * This method must ONLY be invoked while synchronized on this
             * RefEntry's EndpointEntry.
             */
            public boolean hasDirtyFailed() {
                assert Thread.holdsLock(EndpointEntry.this);
                return dirtyFailed;
            }

            /**
             * PhantomLiveRef is a PhantomReference to a LiveRef instance,
             * used to detect when the LiveRef becomes permanently
             * unreachable in this VM.
             */
            private class PhantomLiveRef extends PhantomReference<LiveRef> {

                public PhantomLiveRef(LiveRef ref) {
                    super(ref, EndpointEntry.this.refQueue);
                }

                public RefEntry getRefEntry() {
                    return RefEntry.this;
                }
            }
        }
    }
}
