/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.management.internal;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.GcInfo;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import sun.management.GarbageCollectorImpl;
import sun.management.ManagementFactoryHelper;

/**
 * Implementation class for the garbage collector.
 * Standard and committed hotspot-specific metrics if any.
 *
 * ManagementFactory.getGarbageCollectorMXBeans() returns a list
 * of instances of this class.
 */
public class GarbageCollectorExtImpl extends GarbageCollectorImpl
    implements GarbageCollectorMXBean {

    public GarbageCollectorExtImpl(String name) {
        super(name);
    }

    // The memory pools are static and won't be changed.
    // TODO: If the hotspot implementation begins to have pools
    // dynamically created and removed, this needs to be modified.
    private String[] poolNames = null;
    private synchronized String[] getAllPoolNames() {
        if (poolNames == null) {
            // The order of all memory pool names is important as GcInfo is also created with same order.
            poolNames = ManagementFactoryHelper.getAllMemoryPoolNames();
        }
        return poolNames;
    }

    public GcInfo getLastGcInfo() {
        GcInfo info = getGcInfoBuilder().getLastGcInfo();
        return info;
    }

    private final static String notifName =
        "javax.management.Notification";

    private final static String[] gcNotifTypes = {
        GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION
    };

    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[]{
            new MBeanNotificationInfo(gcNotifTypes,
            notifName,
            "GC Notification")
        };
    }

    private static long seqNumber = 0;
    private static long getNextSeqNumber() {
        return ++seqNumber;
    }

    protected void createGCNotification(long timestamp,
                              String gcName,
                              String gcAction,
                              String gcCause,
                              GcInfo gcInfo)  {
        if (!hasListeners()) {
            return;
        }
        Notification notif = new Notification(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION,
                                              getObjectName(),
                                              getNextSeqNumber(),
                                              timestamp,
                                              gcName);
        GarbageCollectionNotificationInfo info =
            new GarbageCollectionNotificationInfo(gcName,
                                                  gcAction,
                                                  gcCause,
                                                  gcInfo);

        CompositeData cd =
            GarbageCollectionNotifInfoCompositeData.toCompositeData(info);
        notif.setUserData(cd);
        sendNotification(notif);
    }

    public synchronized void addNotificationListener(NotificationListener listener,
                                                     NotificationFilter filter,
                                                     Object handback)
    {
        boolean before = hasListeners();
        super.addNotificationListener(listener, filter, handback);
        boolean after = hasListeners();
        if (!before && after) {
            setNotificationEnabled(this, true);
        }
    }

    public synchronized void removeNotificationListener(NotificationListener listener)
        throws ListenerNotFoundException {
        boolean before = hasListeners();
        super.removeNotificationListener(listener);
        boolean after = hasListeners();

        if (before && !after) {
            setNotificationEnabled(this,false);
        }
    }

    public synchronized void removeNotificationListener(NotificationListener listener,
                                                        NotificationFilter filter,
                                                        Object handback)
            throws ListenerNotFoundException
    {
        boolean before = hasListeners();
        super.removeNotificationListener(listener,filter,handback);
        boolean after = hasListeners();
        if (before && !after) {
            setNotificationEnabled(this,false);
        }
    }

    private GcInfoBuilder gcInfoBuilder;
    // Invoked also by the VM
    private synchronized GcInfoBuilder getGcInfoBuilder() {
        if(gcInfoBuilder == null) {
            gcInfoBuilder = new GcInfoBuilder(this, getAllPoolNames());
        }
        return gcInfoBuilder;
    }

    private native void setNotificationEnabled(GarbageCollectorMXBean gc,
                                                 boolean enabled);

    // Invoked by the VM
    private static java.lang.management.GarbageCollectorMXBean
        createGarbageCollector(String name, String type) {

        return new GarbageCollectorExtImpl(name);
    }
}
