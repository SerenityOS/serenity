/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package nsk.monitoring.share;

import java.lang.management.*;
import javax.management.*;
import java.io.IOException;
import java.util.*;

import nsk.share.*;

/**
 * <code>GarbageCollectorMonitor</code> class is a wrapper of
 * <code>GarbageCollectorMXBean</code>.
 * Depending on command line arguments, an instance of this class redirects
 * invocations to the <code>GarbageCollectorMXBean</code> interface. If
 * <code>-testMode="directly"</code> option is set, this instance directly
 * invokes corresponding method of the <code>GarbageCollectorMXBean</code>
 * interface. If <code>-testMode="server"</code> option is set it will make
 * invocations via MBeanServer. If <code>-testMode="proxy"</code> option
 * is set it will make invocations via MBeanServer proxy.
 *
 * @see ArgumentHandler
 */

public class GarbageCollectorMonitor extends Monitor {

    public static final String GARBAGECOLLECTOR_MXBEAN_NAME =
        ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE;

    private static List<GarbageCollectorMXBean> mbeans
        = ManagementFactory.getGarbageCollectorMXBeans();

    // Internal trace level
    private static final int TRACE_LEVEL = 10;

    // Names of the attributes of ClassLoadingMXBean
    private static final String COLLECTION_COUNT = "CollectionCount";
    private static final String COLLECTION_TIME = "CollectionTime";

    static {
        Monitor.logPrefix = "GarbageCollectorMonitor> ";
    }

    /**
     * Creates a new <code>GarbageCollectorMonitor</code> object.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     */
    public GarbageCollectorMonitor(Log log, ArgumentHandler argumentHandler) {

        super(log, argumentHandler);
    }

    public ObjectName getMBeanObjectName() {

        return mbeanObjectName;
    }

    public Object[] getGarbageCollectorMXBeans() {

        List<Object> list = new ArrayList<Object>();
        int mode = getTestMode();

        switch (mode) {
            case DIRECTLY_MODE:
                list.addAll(mbeans);
                break;
            case SERVER_MODE:
                try {
                    Set query = getMBeanServer().queryNames(null, null);
                    Iterator it = query.iterator();
                    while (it.hasNext()) {
                        ObjectName oname = (ObjectName)it.next();
                        if (oname.toString().startsWith(
                            GARBAGECOLLECTOR_MXBEAN_NAME + ",")) {
                            list.add(oname);
                        }
                    }
                } catch (Exception e) {
                    throw new Failure(e);
                }
                break;
            case PROXY_MODE:
                try {
                    Set query = getMBeanServer().queryNames(null, null);
                    Iterator it = query.iterator();
                    while (it.hasNext()) {
                        ObjectName oname = (ObjectName)it.next();
                        if (oname.toString().startsWith(
                            GARBAGECOLLECTOR_MXBEAN_NAME + ",")) {
                            list.add(getProxy(oname));
                        }
                    }
                } catch (Exception e) {
                    throw new Failure(e);
                }
                break;
            default:
                throw new TestBug("Unknown testMode " + mode);
        }

        return list.toArray();
    }

    private GarbageCollectorMXBean getProxy(ObjectName gcMXBean) {

        try {
            GarbageCollectorMXBean proxy = (GarbageCollectorMXBean)
            ManagementFactory.newPlatformMXBeanProxy(
                getMBeanServer(),
                gcMXBean.toString(),
                GarbageCollectorMXBean.class
            );

            return proxy;
        } catch (Exception e) {
            throw new Failure(e);
        }
    }

    /**
     * Redirects the invocation to
     * {@link java.lang.management.GarbageCollectorMXBean#getCollectionCount()
     * <code>GarbageCollectorMXBean.getCollectionCount()</code>}.
     *
     * @return the total number of collections that have occurred.
     *
     * @see java.lang.management.GarbageCollectorMXBean#getCollectionCount()
     */
    public long getCollectionCount(Object gcMXBean) {

        int mode = getTestMode();

        switch (mode) {
            case DIRECTLY_MODE:
                return ((GarbageCollectorMXBean)gcMXBean).getCollectionCount();
            case SERVER_MODE:
                return getLongAttribute((ObjectName)gcMXBean,
                    COLLECTION_COUNT);
            case PROXY_MODE:
                return ((GarbageCollectorMXBean)gcMXBean).getCollectionCount();
            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    }

    /**
     * Redirects the invocation to
     * {@link java.lang.management.GarbageCollectorMXBean#getCollectionTime()
     * <code>GarbageCollectorMXBean.getCollectionTime()</code>}.
     *
     * @return the approximate accumulated collection elapsed time
     * in milliseconds.
     *
     * @see java.lang.management.GarbageCollectorMXBean#getCollectionTime()
     */
    public long getCollectionTime(Object gcMXBean) {

        int mode = getTestMode();

        switch (mode) {
            case DIRECTLY_MODE:
                return ((GarbageCollectorMXBean)gcMXBean).getCollectionTime();
            case SERVER_MODE:
                return getLongAttribute(((ObjectName)gcMXBean),
                    COLLECTION_TIME);
            case PROXY_MODE:
                return ((GarbageCollectorMXBean)gcMXBean).getCollectionTime();
            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    }

} // GarbageCollectorMonitor
