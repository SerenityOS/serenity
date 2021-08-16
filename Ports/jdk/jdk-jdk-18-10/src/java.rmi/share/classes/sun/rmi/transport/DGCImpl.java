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

import java.io.ObjectInputFilter;
import java.net.SocketPermission;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.dgc.DGC;
import java.rmi.dgc.Lease;
import java.rmi.dgc.VMID;
import java.rmi.server.LogStream;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UID;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import sun.rmi.runtime.Log;
import sun.rmi.runtime.RuntimeUtil;
import sun.rmi.server.UnicastRef;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.server.Util;

/**
 * This class implements the guts of the server-side distributed GC
 * algorithm
 *
 * @author Ann Wollrath
 */
@SuppressWarnings("deprecation")
final class DGCImpl implements DGC {

    /* dgc system log */
    @SuppressWarnings("removal")
    static final Log dgcLog = Log.getLog("sun.rmi.dgc", "dgc",
        LogStream.parseLevel(AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("sun.rmi.dgc.logLevel"))));

    /** lease duration to grant to clients */
    @SuppressWarnings("removal")
    private static final long leaseValue =              // default 10 minutes
        AccessController.doPrivileged(
            (PrivilegedAction<Long>) () -> Long.getLong("java.rmi.dgc.leaseValue", 600000));

    /** lease check interval; default is half of lease grant duration */
    @SuppressWarnings("removal")
    private static final long leaseCheckInterval =
        AccessController.doPrivileged(
            (PrivilegedAction<Long>) () -> Long.getLong("sun.rmi.dgc.checkInterval", leaseValue / 2));

    /** thread pool for scheduling delayed tasks */
    @SuppressWarnings("removal")
    private static final ScheduledExecutorService scheduler =
        AccessController.doPrivileged(
            new RuntimeUtil.GetInstanceAction()).getScheduler();

    /** remote implementation of DGC interface for this VM */
    private static DGCImpl dgc;
    /** table that maps VMID to LeaseInfo */
    private Map<VMID,LeaseInfo> leaseTable = new HashMap<>();
    /** checks for lease expiration */
    private Future<?> checker = null;

    /**
     * Return the remote implementation of the DGC interface for
     * this VM.
     */
    static DGCImpl getDGCImpl() {
        return dgc;
    }

    /**
     * Property name of the DGC serial filter to augment
     * the built-in list of allowed types.
     * Setting the property in the {@code conf/security/java.security} file
     * or system property will enable the augmented filter.
     */
    private static final String DGC_FILTER_PROPNAME = "sun.rmi.transport.dgcFilter";

    /** Registry max depth of remote invocations. **/
    private static int DGC_MAX_DEPTH = 5;

    /** Registry maximum array size in remote invocations. **/
    private static int DGC_MAX_ARRAY_SIZE = 10000;

    /**
     * The dgcFilter created from the value of the {@code  "sun.rmi.transport.dgcFilter"}
     * property.
     */
    @SuppressWarnings("removal")
    private static final ObjectInputFilter dgcFilter =
            AccessController.doPrivileged((PrivilegedAction<ObjectInputFilter>)DGCImpl::initDgcFilter);

    /**
     * Initialize the dgcFilter from the security properties or system property; if any
     * @return an ObjectInputFilter, or null
     */
    private static ObjectInputFilter initDgcFilter() {
        ObjectInputFilter filter = null;
        String props = System.getProperty(DGC_FILTER_PROPNAME);
        if (props == null) {
            props = Security.getProperty(DGC_FILTER_PROPNAME);
        }
        if (props != null) {
            filter = ObjectInputFilter.Config.createFilter(props);
            if (dgcLog.isLoggable(Log.BRIEF)) {
                dgcLog.log(Log.BRIEF, "dgcFilter = " + filter);
            }
        }
        return filter;
    }

    /**
     * Construct a new server-side remote object collector at
     * a particular port. Disallow construction from outside.
     */
    private DGCImpl() {}

    /**
     * The dirty call adds the VMID "vmid" to the set of clients
     * that hold references to the object associated with the ObjID
     * id.  The long "sequenceNum" is used to detect late dirty calls.  If
     * the VMID "vmid" is null, a VMID will be generated on the
     * server (for use by the client in subsequent calls) and
     * returned.
     *
     * The client must call the "dirty" method to renew the lease
     * before the "lease" time expires or all references to remote
     * objects in this VM that the client holds are considered
     * "unreferenced".
     */
    public Lease dirty(ObjID[] ids, long sequenceNum, Lease lease) {
        VMID vmid = lease.getVMID();
        /*
         * The server specifies the lease value; the client has
         * no say in the matter.
         */
        long duration = leaseValue;

        if (dgcLog.isLoggable(Log.VERBOSE)) {
            dgcLog.log(Log.VERBOSE, "vmid = " + vmid);
        }

        // create a VMID if one wasn't supplied
        if (vmid == null) {
            vmid = new VMID();

            if (dgcLog.isLoggable(Log.BRIEF)) {
                String clientHost;
                try {
                    clientHost = RemoteServer.getClientHost();
                } catch (ServerNotActiveException e) {
                    clientHost = "<unknown host>";
                }
                dgcLog.log(Log.BRIEF, " assigning vmid " + vmid +
                           " to client " + clientHost);
            }
        }

        lease = new Lease(vmid, duration);
        // record lease information
        synchronized (leaseTable) {
            LeaseInfo info = leaseTable.get(vmid);
            if (info == null) {
                leaseTable.put(vmid, new LeaseInfo(vmid, duration));
                if (checker == null) {
                    checker = scheduler.scheduleWithFixedDelay(
                        new Runnable() {
                            public void run() {
                                checkLeases();
                            }
                        },
                        leaseCheckInterval,
                        leaseCheckInterval, TimeUnit.MILLISECONDS);
                }
            } else {
                info.renew(duration);
            }
        }

        for (ObjID id : ids) {
            if (dgcLog.isLoggable(Log.VERBOSE)) {
                dgcLog.log(Log.VERBOSE, "id = " + id +
                           ", vmid = " + vmid + ", duration = " + duration);
            }

            ObjectTable.referenced(id, sequenceNum, vmid);
        }

        // return the VMID used
        return lease;
    }

    /**
     * The clean call removes the VMID from the set of clients
     * that hold references to the object associated with the LiveRef
     * ref.  The sequence number is used to detect late clean calls.  If the
     * argument "strong" is true, then the clean call is a result of a
     * failed "dirty" call, thus the sequence number for the VMID needs
     * to be remembered until the client goes away.
     */
    public void clean(ObjID[] ids, long sequenceNum, VMID vmid, boolean strong)
    {
        for (ObjID id : ids) {
            if (dgcLog.isLoggable(Log.VERBOSE)) {
                dgcLog.log(Log.VERBOSE, "id = " + id +
                    ", vmid = " + vmid + ", strong = " + strong);
            }

            ObjectTable.unreferenced(id, sequenceNum, vmid, strong);
        }
    }

    /**
     * Register interest in receiving a callback when this VMID
     * becomes inaccessible.
     */
    void registerTarget(VMID vmid, Target target) {
        synchronized (leaseTable) {
            LeaseInfo info = leaseTable.get(vmid);
            if (info == null) {
                target.vmidDead(vmid);
            } else {
                info.notifySet.add(target);
            }
        }
    }

    /**
     * Remove notification request.
     */
    void unregisterTarget(VMID vmid, Target target) {
        synchronized (leaseTable) {
            LeaseInfo info = leaseTable.get(vmid);
            if (info != null) {
                info.notifySet.remove(target);
            }
        }
    }

    /**
     * Check if leases have expired.  If a lease has expired, remove
     * it from the table and notify all interested parties that the
     * VMID is essentially "dead".
     *
     * @return if true, there are leases outstanding; otherwise leases
     * no longer need to be checked
     */
    private void checkLeases() {
        long time = System.currentTimeMillis();

        /* List of vmids that need to be removed from the leaseTable */
        List<LeaseInfo> toUnregister = new ArrayList<>();

        /* Build a list of leaseInfo objects that need to have
         * targets removed from their notifySet.  Remove expired
         * leases from leaseTable.
         */
        synchronized (leaseTable) {
            Iterator<LeaseInfo> iter = leaseTable.values().iterator();
            while (iter.hasNext()) {
                LeaseInfo info = iter.next();
                if (info.expired(time)) {
                    toUnregister.add(info);
                    iter.remove();
                }
            }

            if (leaseTable.isEmpty()) {
                checker.cancel(false);
                checker = null;
            }
        }

        /* Notify and unegister targets without holding the lock on
         * the leaseTable so we avoid deadlock.
         */
        for (LeaseInfo info : toUnregister) {
            for (Target target : info.notifySet) {
                target.vmidDead(info.vmid);
            }
        }
    }

    static {
        exportSingleton();
    }

    @SuppressWarnings("removal")
    private static void exportSingleton() {
        /*
         * "Export" the singleton DGCImpl in a context isolated from
         * the arbitrary current thread context.
         */
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                ClassLoader savedCcl =
                    Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(
                        ClassLoader.getSystemClassLoader());

                    /*
                     * Put remote collector object in table by hand to prevent
                     * listen on port.  (UnicastServerRef.exportObject would
                     * cause transport to listen.)
                     */
                    try {
                        dgc = new DGCImpl();
                        ObjID dgcID = new ObjID(ObjID.DGC_ID);
                        LiveRef ref = new LiveRef(dgcID, 0);
                        UnicastServerRef disp = new UnicastServerRef(ref,
                                DGCImpl::checkInput);
                        Remote stub =
                            Util.createProxy(DGCImpl.class,
                                             new UnicastRef(ref), true);
                        disp.setSkeleton(dgc);

                        Permissions perms = new Permissions();
                        perms.add(new SocketPermission("*", "accept,resolve"));
                        ProtectionDomain[] pd = { new ProtectionDomain(null, perms) };
                        AccessControlContext acceptAcc = new AccessControlContext(pd);

                        Target target = AccessController.doPrivileged(
                            new PrivilegedAction<Target>() {
                                public Target run() {
                                    return new Target(dgc, disp, stub, dgcID, true);
                                }
                            }, acceptAcc);

                        ObjectTable.putTarget(target);
                    } catch (RemoteException e) {
                        throw new Error(
                            "exception initializing server-side DGC", e);
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(savedCcl);
                }
                return null;
            }
        });
    }

    /**
     * ObjectInputFilter to filter DGC input objects.
     * The list of acceptable classes is very short and explicit.
     * The depth and array sizes are limited.
     *
     * @param filterInfo access to class, arrayLength, etc.
     * @return  {@link ObjectInputFilter.Status#ALLOWED} if allowed,
     *          {@link ObjectInputFilter.Status#REJECTED} if rejected,
     *          otherwise {@link ObjectInputFilter.Status#UNDECIDED}
     */
    private static ObjectInputFilter.Status checkInput(ObjectInputFilter.FilterInfo filterInfo) {
        if (dgcFilter != null) {
            ObjectInputFilter.Status status = dgcFilter.checkInput(filterInfo);
            if (status != ObjectInputFilter.Status.UNDECIDED) {
                // The DGC filter can override the built-in allow-list
                return status;
            }
        }

        if (filterInfo.depth() > DGC_MAX_DEPTH) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> clazz = filterInfo.serialClass();
        if (clazz != null) {
            while (clazz.isArray()) {
                if (filterInfo.arrayLength() >= 0 && filterInfo.arrayLength() > DGC_MAX_ARRAY_SIZE) {
                    return ObjectInputFilter.Status.REJECTED;
                }
                // Arrays are allowed depending on the component type
                clazz = clazz.getComponentType();
            }
            if (clazz.isPrimitive()) {
                // Arrays of primitives are allowed
                return ObjectInputFilter.Status.ALLOWED;
            }
            return (clazz == ObjID.class ||
                    clazz == UID.class ||
                    clazz == VMID.class ||
                    clazz == Lease.class)
                    ? ObjectInputFilter.Status.ALLOWED
                    : ObjectInputFilter.Status.REJECTED;
        }
        // Not a class, not size limited
        return ObjectInputFilter.Status.UNDECIDED;
    }


    private static class LeaseInfo {
        VMID vmid;
        long expiration;
        Set<Target> notifySet = new HashSet<>();

        LeaseInfo(VMID vmid, long lease) {
            this.vmid = vmid;
            expiration = System.currentTimeMillis() + lease;
        }

        synchronized void renew(long lease) {
            long newExpiration = System.currentTimeMillis() + lease;
            if (newExpiration > expiration)
                expiration = newExpiration;
        }

        boolean expired(long time) {
            if (expiration < time) {
                if (dgcLog.isLoggable(Log.BRIEF)) {
                    dgcLog.log(Log.BRIEF, vmid.toString());
                }
                return true;
            } else {
                return false;
            }
        }
    }
}
