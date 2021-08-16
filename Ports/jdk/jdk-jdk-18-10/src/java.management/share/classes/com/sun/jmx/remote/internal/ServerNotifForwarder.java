/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.internal;

import com.sun.jmx.remote.security.NotificationAccessController;
import com.sun.jmx.remote.util.ClassLogger;
import com.sun.jmx.remote.util.EnvHelp;
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanPermission;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationFilter;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.NotificationResult;
import javax.management.remote.TargetedNotification;
import javax.management.MalformedObjectNameException;
import javax.security.auth.Subject;

public class ServerNotifForwarder {


    public ServerNotifForwarder(MBeanServer mbeanServer,
                                Map<String, ?> env,
                                NotificationBuffer notifBuffer,
                                String connectionId) {
        this.mbeanServer = mbeanServer;
        this.notifBuffer = notifBuffer;
        this.connectionId = connectionId;
        connectionTimeout = EnvHelp.getServerConnectionTimeout(env);

        String stringBoolean = (String) env.get("jmx.remote.x.check.notification.emission");
        checkNotificationEmission = EnvHelp.computeBooleanFromString( stringBoolean );
        notificationAccessController =
                EnvHelp.getNotificationAccessController(env);
    }

    public Integer addNotificationListener(final ObjectName name,
        final NotificationFilter filter)
        throws InstanceNotFoundException, IOException {

        if (logger.traceOn()) {
            logger.trace("addNotificationListener",
                "Add a listener at " + name);
        }

        checkState();

        // Explicitly check MBeanPermission for addNotificationListener
        //
        checkMBeanPermission(name, "addNotificationListener");
        if (notificationAccessController != null) {
            notificationAccessController.addNotificationListener(
                connectionId, name, getSubject());
        }
        try {
            @SuppressWarnings("removal")
            boolean instanceOf =
            AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Boolean>() {
                        public Boolean run() throws InstanceNotFoundException {
                            return mbeanServer.isInstanceOf(name, broadcasterClass);
                        }
            });
            if (!instanceOf) {
                throw new IllegalArgumentException("The specified MBean [" +
                    name + "] is not a " +
                    "NotificationBroadcaster " +
                    "object.");
            }
        } catch (PrivilegedActionException e) {
            throw (InstanceNotFoundException) extractException(e);
        }

        final Integer id = getListenerID();

        // 6238731: set the default domain if no domain is set.
        ObjectName nn = name;
        if (name.getDomain() == null || name.getDomain().isEmpty()) {
            try {
                nn = ObjectName.getInstance(mbeanServer.getDefaultDomain(),
                                            name.getKeyPropertyList());
            } catch (MalformedObjectNameException mfoe) {
                // impossible, but...
                IOException ioe = new IOException(mfoe.getMessage());
                ioe.initCause(mfoe);
                throw ioe;
            }
        }

        synchronized (listenerMap) {
            IdAndFilter idaf = new IdAndFilter(id, filter);
            Set<IdAndFilter> set = listenerMap.get(nn);
            // Tread carefully because if set.size() == 1 it may be the
            // Collections.singleton we make here, which is unmodifiable.
            if (set == null)
                set = Collections.singleton(idaf);
            else {
                if (set.size() == 1)
                    set = new HashSet<IdAndFilter>(set);
                set.add(idaf);
            }
            listenerMap.put(nn, set);
        }

        return id;
    }

    public void removeNotificationListener(ObjectName name,
        Integer[] listenerIDs)
        throws Exception {

        if (logger.traceOn()) {
            logger.trace("removeNotificationListener",
                "Remove some listeners from " + name);
        }

        checkState();

        // Explicitly check MBeanPermission for removeNotificationListener
        //
        checkMBeanPermission(name, "removeNotificationListener");
        if (notificationAccessController != null) {
            notificationAccessController.removeNotificationListener(
                connectionId, name, getSubject());
        }

        Exception re = null;
        for (int i = 0 ; i < listenerIDs.length ; i++) {
            try {
                removeNotificationListener(name, listenerIDs[i]);
            } catch (Exception e) {
                // Give back the first exception
                //
                if (re != null) {
                    re = e;
                }
            }
        }
        if (re != null) {
            throw re;
        }
    }

    public void removeNotificationListener(ObjectName name, Integer listenerID)
    throws
        InstanceNotFoundException,
        ListenerNotFoundException,
        IOException {

        if (logger.traceOn()) {
            logger.trace("removeNotificationListener",
                "Remove the listener " + listenerID + " from " + name);
        }

        checkState();

        if (name != null && !name.isPattern()) {
            if (!mbeanServer.isRegistered(name)) {
                throw new InstanceNotFoundException("The MBean " + name +
                    " is not registered.");
            }
        }

        synchronized (listenerMap) {
            // Tread carefully because if set.size() == 1 it may be a
            // Collections.singleton, which is unmodifiable.
            Set<IdAndFilter> set = listenerMap.get(name);
            IdAndFilter idaf = new IdAndFilter(listenerID, null);
            if (set == null || !set.contains(idaf))
                throw new ListenerNotFoundException("Listener not found");
            if (set.size() == 1)
                listenerMap.remove(name);
            else
                set.remove(idaf);
        }
    }

    /* This is the object that will apply our filtering to candidate
     * notifications.  First of all, if there are no listeners for the
     * ObjectName that the notification is coming from, we go no further.
     * Then, for each listener, we must apply the corresponding filter (if any)
     * and ignore the listener if the filter rejects.  Finally, we apply
     * some access checks which may also reject the listener.
     *
     * A given notification may trigger several listeners on the same MBean,
     * which is why listenerMap is a Map<ObjectName, Set<IdAndFilter>> and
     * why we add the found notifications to a supplied List rather than
     * just returning a boolean.
     */
    private final NotifForwarderBufferFilter bufferFilter = new NotifForwarderBufferFilter();

    final class NotifForwarderBufferFilter implements NotificationBufferFilter {
        public void apply(List<TargetedNotification> targetedNotifs,
                          ObjectName source, Notification notif) {
            // We proceed in two stages here, to avoid holding the listenerMap
            // lock while invoking the filters (which are user code).
            final IdAndFilter[] candidates;
            synchronized (listenerMap) {
                final Set<IdAndFilter> set = listenerMap.get(source);
                if (set == null) {
                    logger.debug("bufferFilter", "no listeners for this name");
                    return;
                }
                candidates = new IdAndFilter[set.size()];
                set.toArray(candidates);
            }
            // We don't synchronize on targetedNotifs, because it is a local
            // variable of our caller and no other thread can see it.
            for (IdAndFilter idaf : candidates) {
                final NotificationFilter nf = idaf.getFilter();
                if (nf == null || nf.isNotificationEnabled(notif)) {
                    logger.debug("bufferFilter", "filter matches");
                    final TargetedNotification tn =
                            new TargetedNotification(notif, idaf.getId());
                    if (allowNotificationEmission(source, tn))
                        targetedNotifs.add(tn);
                }
            }
        }
    };

    public NotificationResult fetchNotifs(long startSequenceNumber,
        long timeout,
        int maxNotifications) {
        if (logger.traceOn()) {
            logger.trace("fetchNotifs", "Fetching notifications, the " +
                "startSequenceNumber is " + startSequenceNumber +
                ", the timeout is " + timeout +
                ", the maxNotifications is " + maxNotifications);
        }

        NotificationResult nr;
        final long t = Math.min(connectionTimeout, timeout);
        try {
            nr = notifBuffer.fetchNotifications(bufferFilter,
                startSequenceNumber,
                t, maxNotifications);
            snoopOnUnregister(nr);
        } catch (InterruptedException ire) {
            nr = new NotificationResult(0L, 0L, new TargetedNotification[0]);
        }

        if (logger.traceOn()) {
            logger.trace("fetchNotifs", "Forwarding the notifs: "+nr);
        }

        return nr;
    }

    // The standard RMI connector client will register a listener on the MBeanServerDelegate
    // in order to be told when MBeans are unregistered.  We snoop on fetched notifications
    // so that we can know too, and remove the corresponding entry from the listenerMap.
    // See 6957378.
    private void snoopOnUnregister(NotificationResult nr) {
        List<IdAndFilter> copy = null;
        synchronized (listenerMap) {
            Set<IdAndFilter> delegateSet = listenerMap.get(MBeanServerDelegate.DELEGATE_NAME);
            if (delegateSet == null || delegateSet.isEmpty()) {
                return;
            }
            copy = new ArrayList<>(delegateSet);
        }

        for (TargetedNotification tn : nr.getTargetedNotifications()) {
            Integer id = tn.getListenerID();
            for (IdAndFilter idaf : copy) {
                if (idaf.id == id) {
                    // This is a notification from the MBeanServerDelegate.
                    Notification n = tn.getNotification();
                    if (n instanceof MBeanServerNotification &&
                            n.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
                        MBeanServerNotification mbsn = (MBeanServerNotification) n;
                        ObjectName gone = mbsn.getMBeanName();
                        synchronized (listenerMap) {
                            listenerMap.remove(gone);
                        }
                    }
                }
            }
        }
    }

    public void terminate() {
        if (logger.traceOn()) {
            logger.trace("terminate", "Be called.");
        }

        synchronized(terminationLock) {
            if (terminated) {
                return;
            }

            terminated = true;

            synchronized(listenerMap) {
                listenerMap.clear();
            }
        }

        if (logger.traceOn()) {
            logger.trace("terminate", "Terminated.");
        }
    }

    //----------------
    // PRIVATE METHODS
    //----------------

    @SuppressWarnings("removal")
    private Subject getSubject() {
        return Subject.getSubject(AccessController.getContext());
    }

    private void checkState() throws IOException {
        synchronized(terminationLock) {
            if (terminated) {
                throw new IOException("The connection has been terminated.");
            }
        }
    }

    private Integer getListenerID() {
        synchronized(listenerCounterLock) {
            return listenerCounter++;
        }
    }

    /**
     * Explicitly check the MBeanPermission for
     * the current access control context.
     */
    public final void checkMBeanPermission(
            final ObjectName name, final String actions)
            throws InstanceNotFoundException, SecurityException {
        checkMBeanPermission(mbeanServer,name,actions);
    }

    @SuppressWarnings("removal")
    static void checkMBeanPermission(
            final MBeanServer mbs, final ObjectName name, final String actions)
            throws InstanceNotFoundException, SecurityException {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            AccessControlContext acc = AccessController.getContext();
            ObjectInstance oi;
            try {
                oi = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<ObjectInstance>() {
                        public ObjectInstance run()
                        throws InstanceNotFoundException {
                            return mbs.getObjectInstance(name);
                        }
                });
            } catch (PrivilegedActionException e) {
                throw (InstanceNotFoundException) extractException(e);
            }
            String classname = oi.getClassName();
            MBeanPermission perm = new MBeanPermission(
                classname,
                null,
                name,
                actions);
            sm.checkPermission(perm, acc);
        }
    }

    /**
     * Check if the caller has the right to get the following notifications.
     */
    private boolean allowNotificationEmission(ObjectName name,
                                              TargetedNotification tn) {
        try {
            if (checkNotificationEmission) {
                checkMBeanPermission(name, "addNotificationListener");
            }
            if (notificationAccessController != null) {
                notificationAccessController.fetchNotification(
                        connectionId, name, tn.getNotification(), getSubject());
            }
            return true;
        } catch (SecurityException e) {
            if (logger.debugOn()) {
                logger.debug("fetchNotifs", "Notification " +
                        tn.getNotification() + " not forwarded: the " +
                        "caller didn't have the required access rights");
            }
            return false;
        } catch (Exception e) {
            if (logger.debugOn()) {
                logger.debug("fetchNotifs", "Notification " +
                        tn.getNotification() + " not forwarded: " +
                        "got an unexpected exception: " + e);
            }
            return false;
        }
    }

    /**
     * Iterate until we extract the real exception
     * from a stack of PrivilegedActionExceptions.
     */
    private static Exception extractException(Exception e) {
        while (e instanceof PrivilegedActionException) {
            e = ((PrivilegedActionException)e).getException();
        }
        return e;
    }

    private static class IdAndFilter {
        private Integer id;
        private NotificationFilter filter;

        IdAndFilter(Integer id, NotificationFilter filter) {
            this.id = id;
            this.filter = filter;
        }

        Integer getId() {
            return this.id;
        }

        NotificationFilter getFilter() {
            return this.filter;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return ((o instanceof IdAndFilter) &&
                    ((IdAndFilter) o).getId().equals(getId()));
        }
    }


    //------------------
    // PRIVATE VARIABLES
    //------------------

    private MBeanServer mbeanServer;

    private final String connectionId;

    private final long connectionTimeout;

    private static int listenerCounter = 0;
    private static final int[] listenerCounterLock = new int[0];

    private NotificationBuffer notifBuffer;
    private final Map<ObjectName, Set<IdAndFilter>> listenerMap =
            new HashMap<ObjectName, Set<IdAndFilter>>();

    private boolean terminated = false;
    private final int[] terminationLock = new int[0];

    static final String broadcasterClass =
        NotificationBroadcaster.class.getName();

    private final boolean checkNotificationEmission;

    private final NotificationAccessController notificationAccessController;

    private static final ClassLogger logger =
        new ClassLogger("javax.management.remote.misc", "ServerNotifForwarder");
}
