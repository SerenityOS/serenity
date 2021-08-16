/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
import javax.management.openmbean.*;
import java.util.*;

import nsk.share.*;

/**
 * <code>MemoryMonitor</code> class is a wrapper of <code>MemoryMXBean</code> and
 * <code>MemoryPoolMXBean</code> interfaces. Depending on command line arguments,
 * an instance of this class redirects invocations to the
 * <code>MemoryMXBean</code> (or <code>MemoryPoolMXBean</code>) interface. If
 * <code>-testMode="directly"</code> option is set, this instance directly
 * invokes corresponding method of the <code>MemoryMXBean</code> (or
 * <code>MemoryPoolMXBean</code>) interface. If <code>-testMode="server"</code>
 * option is set it will make invocations via MBeanServer.
 *
 * @see ArgumentHandler
 */
public class MemoryMonitor extends Monitor implements NotificationListener,
        NotificationFilter {

    // Constants to define type of memory that will be allocated in
    // MemoryMonitor. For heap memory objects will be allocated; for nonheap
    // type classes will be loaded; for mixed type -- both (objects will be
    // allocated and classes will be loaded).
    public final static String HEAP_TYPE = "heap";
    public final static String NONHEAP_TYPE = "nonheap";
    public final static String MIXED_TYPE = "mixed";
    // Names of the attributes of MemoryMXBean
    private final static String POOL_TYPE = "Type";
    private final static String POOL_RESET_PEAK = "resetPeakUsage";
    private final static String POOL_PEAK = "PeakUsage";
    private final static String POOL_VALID = "Valid";
    private final static String POOL_U = "Usage";
    private final static String UT = "UsageThreshold";
    private final static String UT_COUNT = "UsageThresholdCount";
    private final static String UT_SUPPORT = "UsageThresholdSupported";
    private final static String UT_EXCEEDED = "UsageThresholdExceeded";
    private final static String POOL_CU = "CollectionUsage";
    private final static String CT = "CollectionUsageThreshold";
    private final static String CT_COUNT = "CollectionUsageThresholdCount";
    private final static String CT_SUPPORT = "CollectionUsageThresholdSupported";
    private final static String CT_EXCEEDED = "CollectionUsageThresholdExceeded";
    // Varibales to store options that are passed to the test
    private static String memory;
    private static int mode;
    private static boolean isNotification;
    private static boolean isUsageThreshold;
    private static volatile boolean passed = true;
    private Polling polling = new Polling();

    static {
        Monitor.logPrefix = "MemoryMonitor   > ";
    }

    /**
     * Creates a new <code>MemoryMonitor</code> object.
     *
     * @param log <code>Log</code> object to print info to.
     * @param handler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     */
    public MemoryMonitor(Log log, ArgumentHandler handler) {
        super(log, handler);

        memory = handler.getTestedMemory();
        mode = getTestMode();
        isNotification = (handler.MON_NOTIF.equals(handler.getMonitoring()));
        isUsageThreshold = (handler.TH_USAGE.equals(handler.getThreshold()));

        String s = "\t(This setting is used in lowmem* tests only)";

        display("Memory:\t" + handler.getTestedMemory() + s);
        display("Monitoring:\t" + handler.getMonitoring() + s);
        display("Threshold:\t" + handler.getThreshold() + s);
        display("Timeout:\t" + handler.getTimeout() + s);
    }

    /**
     * Returns <code>true</code> if no failures were revealed during the test,
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if no failures were revealed during the test,
     * <code>false</code> otherwise.
     *
     */
    public boolean getPassedStatus() {
        return passed;
    }

    /**
     * Enables memory monitoring.
     * <p>
     * If notification type of monitoring is chosen, the method adds {@link
     * javax.management.NotificationListener
     * <code>javax.management.NotificationListener</code>} to enables low
     * memory detection support. If monitoring type is polling, a new thread
     * that manages the low memory detection is started.
     *
     * @throws InstanceNotFoundException The MemoryMXBean is not registered on
     *                                   the server.
     */
    public void enableMonitoring() throws InstanceNotFoundException {
        if (isNotification) {
            switch (mode) {
                case DIRECTLY_MODE:
                    MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
                    NotificationEmitter emitter = (NotificationEmitter) mbean;
                    emitter.addNotificationListener(this, this, null);
                    break;

                case SERVER_MODE:
                case PROXY_MODE:
                    getMBeanServer().addNotificationListener(mbeanObjectName,
                            this, this, null);
                    break;

                default:
                    throw new TestBug("Unknown testMode " + mode);
            }
        } else {

            // Polling
            // Start a thread that will manage all test modes
            polling.start();

        }
    } // enableMonitoring()

    /**
     * Disables memory monitoring.
     * <p>
     * If monitoring type is polling, the thread that manages the low memory
     * detection is stopped.
     */
    public void disableMonitoring() {
        if (!isNotification) {

            // Stop polling thread
            polling.goOn = false;
        }
    } // disableMonitoring()

    /**
     * Updates thresholds. Thresholds values for all pools will be greater
     * than <code>used</code> value.
     * <p>
     * If <code>usage</code> thresholds are chosen, the method updates just
     * pools that support usage thresholds. If <code>collection</code>
     * thresholds are chosen, the method updates memory pools that support
     * collection usage thresholds.
     *
     * This method is synchronized because it may be invoked from
     * <code>handleNotification</code> which is potentially done from
     * multiple threads.
     */
    public synchronized void updateThresholds() {
        if (isUsageThreshold) {
            updateUsageThresholds();
        } else {
            updateCollectionThresholds();
        }
    }

    /**
     * Reset thresholds. Thresholds values for all pools will be 1.
     * If <code>usage</code> thresholds are chosen, the method updates just
     * pools that support usage thresholds. If <code>collection</code>
     * thresholds are chosen, the method updates memory pools that support
     * collection usage thresholds.
     *
     * This method is synchronized because it may be invoked from
     * multiple threads.
     */
    public synchronized void resetThresholds(MemoryType type) {
        List pools = getMemoryPoolMBeans();
        for (int i = 0; i < pools.size(); i++) {
            Object pool = pools.get(i);
            if (isUsageThresholdSupported(pool)) {
                if (getType(pool).equals(type)) {
                    setUsageThreshold(pool, 1);
                }
            }
        }
    }

    /**
     * The method is invoked before sending the notification to the listener.
     *
     * @param notification The notification to be sent.
     * @return <i>true</i> if the notification has to be sent to the listener;
     *         <i>false</i> otherwise.
     *
     * @see javax.management.NotificationFilter
     */
    public boolean isNotificationEnabled(Notification notification) {
        String type = notification.getType();
        String usage = MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED;
        String collection = MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED;

        if (isUsageThreshold) {
            return type.equals(usage);
        } else {
            return type.equals(collection);
        }
    } // isNotificationEnabled()

    /**
     * The method is invoked when a JMX notification occurs.
     *
     * @param notification The notification to be sent.
     * @param handback An opaque object which helps the listener to associate
     *        information regarding the MBean emitter.
     * @see javax.management.NotificationListener
     */
    public void handleNotification(Notification notification, Object handback) {
        CompositeData data = (CompositeData) notification.getUserData();
        MemoryNotificationInfo mn = MemoryNotificationInfo.from(data);

        display(mn.getCount() + " notification \"" + notification.getMessage()
                + "\" is caught on " + (new Date(notification.getTimeStamp()))
                + " by " + mn.getPoolName() + " (" + mn.getUsage() + ")");
        updateThresholds();
    } // handleNotification()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#resetPeakUsage
     * <code>MemoryPoolMXBean.resetPeakUsage()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     */
    public void resetPeakUsage(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                directPool.resetPeakUsage();
                break;

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;

                try {
                    getMBeanServer().invoke(serverPool, POOL_RESET_PEAK,
                            null, null);
                } catch (Exception e) {
                    e.printStackTrace(logger.getOutStream());
                    throw new Failure(e);
                }
                break;

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                proxyPool.resetPeakUsage();
                break;

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // resetPeakUsage()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#getPeakUsage
     * <code>MemoryPoolMXBean.getPeakUsage()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return a <code>MemoryUsage</code> object representing the peak memory
     *         usage; <code>null</code> otherwise.
     */
    public MemoryUsage getPeakUsage(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.getPeakUsage();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return getMemoryUsageAttribute(serverPool, POOL_PEAK);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.getPeakUsage();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // getPeakUsage()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#getUsage
     * <code>MemoryPoolMXBean.getUsage()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return a <code>MemoryUsage</code> object; or <code>null</code> if this
     *         pool not valid.
     */
    public MemoryUsage getUsage(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.getUsage();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return getUsageOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.getUsage();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // getUsage()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#getCollectionUsage
     * <code>MemoryPoolMXBean.getCollectionUsage()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return a <code>MemoryUsage</code> object; or <code>null</code> if this
     *         method is not supported.
     */
    public MemoryUsage getCollectionUsage(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.getCollectionUsage();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return getCollectionUsageOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.getCollectionUsage();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // getCollectionUsage()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#isValid
     * <code>MemoryPoolMXBean.isValid()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return a <code>true</code> if the memory pool is valid in the running
     *         JVM; <code>null</code> otherwise.
     */
    public boolean isValid(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.isValid();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return getBooleanAttribute(serverPool, POOL_VALID);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.isValid();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // isValid()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#isUsageThresholdSupported
     * <code>MemoryPoolMXBean.isUsageThresholdSupported()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return a <code>true</code> if the memory pool supports usage threshold;
     *         <code>null</code> otherwise.
     */
    public boolean isUsageThresholdSupported(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.isUsageThresholdSupported();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return isUsageThresholdSupportedOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.isUsageThresholdSupported();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // isUsageThresholdSupported()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#isCollectionUsageThresholdSupported
     * <code>MemoryPoolMXBean.isCollectionUsageThresholdSupported()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return a <code>true</code> if the memory pool supports collection
     *         usage threshold; <code>null</code> otherwise.
     */
    public boolean isCollectionThresholdSupported(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.isCollectionUsageThresholdSupported();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return isCollectionThresholdSupportedOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.isCollectionUsageThresholdSupported();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // isCollectionThresholdSupported()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#isUsageThresholdExceeded
     * <code>MemoryPoolMXBean.isUsageThresholdExceeded()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return a <code>true</code> if the memory usage of this pool reaches or
     *         exceeds the threshold value; <code>null</code> otherwise.
     */
    public boolean isUsageThresholdExceeded(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.isUsageThresholdExceeded();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return isUsageThresholdExceededOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.isUsageThresholdExceeded();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // isUsageThresholdExceeded()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#isCollectionUsageThresholdExceeded
     * <code>MemoryPoolMXBean.isCollectionUsageThresholdExceeded()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return a <code>true</code> if the memory usage of this pool reaches or
     *         exceeds the collection usage threshold value in the most recent
     *         collection; <code>null</code> otherwise.
     */
    public boolean isCollectionThresholdExceeded(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.isCollectionUsageThresholdExceeded();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return isCollectionThresholdExceededOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.isCollectionUsageThresholdExceeded();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // isCollectionThresholdExceeded()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#getUsageThreshold
     * <code>MemoryPoolMXBean.getUsageThreshold()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return the usage threshold value of this memory pool in bytes.
     */
    public long getUsageThreshold(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.getUsageThreshold();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return getUsageThresholdOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.getUsageThreshold();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // getUsageThreshold()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#getCollectionUsageThreshold
     * <code>MemoryPoolMXBean.getCollectionUsageThreshold()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return the collection usage threshold value of this memory pool in
     *         bytes.
     */
    public long getCollectionThreshold(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.getCollectionUsageThreshold();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return getCollectionThresholdOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.getCollectionUsageThreshold();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // getCollectionThreshold()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#getUsageThresholdCount
     * <code>MemoryPoolMXBean.getUsageThresholdCount()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return number of times that the memory usage has crossed its usage
     *         threshold value.
     */
    public long getUsageThresholdCount(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.getUsageThresholdCount();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return getUsageThresholdCountOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.getUsageThresholdCount();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // getUsageThresholdCount()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#getCollectionUsageThresholdCount
     * <code>MemoryPoolMXBean.getCollectionUsageThresholdCount()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return number of times that the memory usage has crossed its collection
     *         usage threshold value.
     */
    public long getCollectionThresholdCount(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.getCollectionUsageThresholdCount();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return getCollectionThresholdCountOnServer(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.getCollectionUsageThresholdCount();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // getCollectionThresholdCount()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#setUsageThreshold
     * <code>MemoryPoolMXBean.setUsageThreshold()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @param threshold the new threshold value.
     */
    public void setUsageThreshold(Object poolObject, long threshold) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                directPool.setUsageThreshold(threshold);
                break;

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                setUsageThresholdOnServer(serverPool, threshold);
                break;

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                proxyPool.setUsageThreshold(threshold);
                break;

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // setUsageThreshold()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#setCollectionUsageThreshold
     * <code>MemoryPoolMXBean.setCollectionUsageThreshold()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @param threshold the new collection usage threshold value.
     */
    public void setCollectionThreshold(Object poolObject, long threshold) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                directPool.setCollectionUsageThreshold(threshold);
                break;

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                setCollectionThresholdOnServer(serverPool, threshold);
                break;

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                proxyPool.setCollectionUsageThreshold(threshold);
                break;

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // setCollectionThreshold()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#getName
     * <code>MemoryPoolMXBean.getName()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return the name of the memory pool.
     */
    public String getName(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.getName();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return serverPool.toString();

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.getName();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // getName()

    /**
     * Redirects the invocation to {@link
     * java.lang.management.MemoryPoolMXBean#getType
     * <code>MemoryPoolMXBean.getType()</code>}.
     *
     * @param poolObject reference to the pool. The pool may be specified
     *        either by <code>ObjectName</code>, or
     *        <code>MemoryPoolMXBean</code>.
     * @return the name of the memory pool.
     */
    public MemoryType getType(Object poolObject) {
        switch (mode) {
            case DIRECTLY_MODE:
                MemoryPoolMXBean directPool = (MemoryPoolMXBean) poolObject;
                return directPool.getType();

            case SERVER_MODE:
                ObjectName serverPool = (ObjectName) poolObject;
                return getType(serverPool);

            case PROXY_MODE:
                MemoryPoolMXBean proxyPool = (MemoryPoolMXBean) poolObject;
                return proxyPool.getType();

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ManagementFactory#getMemoryPoolMXBeans
     * <code>ManagementFactory.getMemoryPoolMXBeans()</code>}.
     *
     * @return a list of <code>MemoryPoolMXBean</code> objects.
     */
    public List<? extends Object> getMemoryPoolMBeans() {
        switch (mode) {
            case DIRECTLY_MODE:
                return ManagementFactory.getMemoryPoolMXBeans();

            case SERVER_MODE: {
                ObjectName[] names = getMemoryPoolMXBeansOnServer();
                ArrayList<ObjectName> list = new ArrayList<ObjectName>();

                for (int i = 0; i < names.length; i++) {
                    list.add(names[i]);
                }
                return list;
            }

            case PROXY_MODE: {
                ObjectName[] names = getMemoryPoolMXBeansOnServer();
                ArrayList<MemoryPoolMXBean> list = new ArrayList<MemoryPoolMXBean>();

                for (int i = 0; i < names.length; i++) {
                    list.add(getProxy(names[i]));
                }
                return list;
            }

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // getMemoryPoolMXBeans()

    // **********************************************************************
    //
    // Private methods
    //
    // **********************************************************************
    private MemoryPoolMXBean getProxy(ObjectName objectName) {
        try {
            MemoryPoolMXBean proxy = (MemoryPoolMXBean) ManagementFactory.newPlatformMXBeanProxy(
                    getMBeanServer(),
                    objectName.toString(),
                    MemoryPoolMXBean.class);
            return proxy;
        } catch (Exception e) {
            throw new Failure(e);
        }
    }

    // Sets new usage threasholds in all pools that match the tested memory and
    // support low memory detetion. A new value will be greater than used value
    // for the pool.
    private void updateUsageThresholds() {
        switch (mode) {
            case DIRECTLY_MODE:
            // we can use the same code here for direct and proxy modes
            case PROXY_MODE:
                List poolsMBean = ManagementFactory.getMemoryPoolMXBeans();

                for (int i = 0; i < poolsMBean.size(); i++) {
                    MemoryPoolMXBean pool = (MemoryPoolMXBean) poolsMBean.get(i);
                    if (!pool.isUsageThresholdSupported()) {
                        continue;
                    }

                    MemoryType mt = pool.getType();
                    if ((!mt.equals(MemoryType.HEAP)
                            || !memory.equals(HEAP_TYPE))
                            && (!mt.equals(MemoryType.NON_HEAP)
                            || !memory.equals(NONHEAP_TYPE))
                            && !memory.equals(MIXED_TYPE)) {
                        continue;
                    }

                    // Yes! We got the pool that
                    // 1. supports usage threshold
                    // 2. has type that match tested type
                    // So, update the pool with new threshold
                    long oldT = pool.getUsageThreshold();
                    MemoryUsage usage = pool.getUsage();
                    long newT = newThreshold(usage, oldT, pool.getName());

                    try {
                        pool.setUsageThreshold(newT);
                    } catch (IllegalArgumentException e) {
                        /*
                         * Max value might have changed since the call to newThreshold()
                         * above. If it has fallen below the value of newT, which is certainly
                         * possible, an exception like this one will be thrown from
                         * sun.management.MemoryPoolImpl.setUsageThreshold():
                         *
                         * java.lang.IllegalArgumentException: Invalid threshold: 48332800 > max (47251456).
                         *
                         * We don't know the max value at the time of the failed call, and it
                         * might have changed since the call once more. So there is no point
                         * trying to detect whether the IllegalArgumentException had been
                         * justified, we cannot know it at this point.
                         *
                         * The best we can do is log the fact and continue.
                         */
                        displayInfo("setUsageThreshold() failed with " + e + ", ignoring... ",
                                pool,
                                "current usage after the call to setUsageThreshold(): ", getUsage(pool),
                                "threshold: ", newT);
                        continue;
                    }
                    displayInfo("Usage threshold is set", pool, "usage: ", pool.getUsage(), "threshold: ", pool.getUsageThreshold());
                    if (pool.getUsageThreshold() != newT) {
                        complain("Cannot reset usage threshold from " + oldT
                                + " to " + newT + " in pool " + pool.getName() + " "
                                + pool.getUsageThreshold());
                        passed = false;
                    }
                } // for i
                break;

            case SERVER_MODE:
                ObjectName[] pools = getMemoryPoolMXBeansOnServer();

                for (int i = 0; i < pools.length; i++) {
                    if (!isUsageThresholdSupportedOnServer(pools[i])) {
                        continue;
                    }

                    MemoryType mt = getType(pools[i]);
                    if ((!mt.equals(MemoryType.HEAP)
                            || !memory.equals(HEAP_TYPE))
                            && (!mt.equals(MemoryType.NON_HEAP)
                            || !memory.equals(NONHEAP_TYPE))
                            && !memory.equals(MIXED_TYPE)) {
                        continue;
                    }

                    // Yes! We got the pool that
                    // 1. supports usage threshold
                    // 2. has type that match tested type
                    // So, update the pool with new threshold
                    long oldT = getUsageThreshold(pools[i]);
                    long newT = newThreshold(getUsageOnServer(pools[i]), oldT,
                            pools[i].toString());
                    try {
                        setUsageThresholdOnServer(pools[i], newT);
                    } catch (Failure e) {
                        /*
                         * Max value might have changed since the call to newThreshold()
                         * above. If it has fallen below the value of newT, which is certainly
                         * possible, an exception like this one will be thrown from
                         * sun.management.MemoryPoolImpl.setUsageThreshold():
                         *
                         * java.lang.IllegalArgumentException: Invalid threshold: 48332800 > max (47251456).
                         *
                         * and we'll catch Failure here as a result (it'll be thrown by
                         * Monitor.setLongAttribute).
                         *
                         * We don't know the max value at the time of the failed call, and it
                         * might have changed since the call once more. So there is no point
                         * trying to detect whether the IllegalArgumentException had been
                         * justified, we cannot know it at this point.
                         *
                         * The best we can do is log the fact and continue.
                         */
                        displayInfo("setUsageThresholdOnServer() failed with " + e + ", ignoring... ",
                                pools[i],
                                "current usage after the call to setUsageThresholdOnServer(): ", getUsageOnServer(pools[i]),
                                "threshold: ", newT);
                        continue;
                    }
                    displayInfo("Usage threshold is set", null, "pool: ", pools[i], "usage:", getUsageOnServer(pools[i]));
                    if (getUsageThreshold(pools[i]) != newT) {
                        complain("Cannot reset usage threshold from " + oldT + " to "
                                + newT + " in pool " + pools[i].toString());
                        passed = false;
                    }
                } // for i
                break;

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // updateUsageThresholds()

    // Sets new collection usage threasholds in all pools that match the tested
    // memory and support low memory detetion. A new value will be greater than
    // used value for the pool.
    private void updateCollectionThresholds() {
        switch (mode) {
            case DIRECTLY_MODE:
            // we can use the same code here for direct and proxy modes
            case PROXY_MODE:
                List poolsMBean = ManagementFactory.getMemoryPoolMXBeans();

                for (int i = 0; i < poolsMBean.size(); i++) {
                    MemoryPoolMXBean pool = (MemoryPoolMXBean) poolsMBean.get(i);
                    if (!pool.isCollectionUsageThresholdSupported()) {
                        continue;
                    }

                    MemoryType mt = pool.getType();
                    if ((!mt.equals(MemoryType.HEAP)
                            || !memory.equals(HEAP_TYPE))
                            && (!mt.equals(MemoryType.NON_HEAP)
                            || !memory.equals(NONHEAP_TYPE))
                            && !memory.equals(MIXED_TYPE)) {
                        continue;
                    }

                    // Yes! We got the pool that
                    // 1. supports collection threshold
                    // 2. has type that match tested type
                    // So, update the pool with new threshold
                    long oldT = pool.getCollectionUsageThreshold();
                    MemoryUsage usage = pool.getUsage();
                    long newT = newThreshold(usage, oldT, pool.getName());

                    try {
                        pool.setCollectionUsageThreshold(newT);
                    } catch (IllegalArgumentException e) {

                        /*
                         * Max value might have changed since the call to newThreshold()
                         * above. If it has fallen below the value of newT, which is certainly
                         * possible, an exception like this one will be thrown from
                         * sun.management.MemoryPoolImpl.setCollectionUsageThreshold():
                         *
                         * java.lang.IllegalArgumentException: Invalid threshold: 48332800 > max (47251456).
                         *
                         * We don't know the max value at the time of the failed call, and it
                         * might have changed since the call once more. So there is no point
                         * trying to detect whether the IllegalArgumentException had been
                         * justified, we cannot know it at this point.
                         *
                         * The best we can do is log the fact and continue.
                         */
                        displayInfo("setCollectionUsageThreshold() failed with " + e + ", ignoring... ",
                                pool,
                                "current usage after the call to setCollectionUsageThreshold(): ", getUsage(pool),
                                "threshold: ", newT);
                        continue;
                    }
                    displayInfo("Collection threshold is set", pool, "usage: ", getUsage(pool), "threshold: ", newT);
                    if (pool.getCollectionUsageThreshold() != newT) {
                        complain("Cannot reset collection threshold from " + oldT
                                + " to " + newT + " in pool " + pool.getName() + " "
                                + pool.getCollectionUsageThreshold());
                        passed = false;
                    }
                } // for i
                break;

            case SERVER_MODE:
                ObjectName[] pools = getMemoryPoolMXBeansOnServer();

                for (int i = 0; i < pools.length; i++) {
                    if (!isCollectionThresholdSupportedOnServer(pools[i])) {
                        continue;
                    }

                    MemoryType mt = getType(pools[i]);
                    if ((!mt.equals(MemoryType.HEAP)
                            || !memory.equals(HEAP_TYPE))
                            && (!mt.equals(MemoryType.NON_HEAP)
                            || !memory.equals(NONHEAP_TYPE))
                            && !memory.equals(MIXED_TYPE)) {
                        continue;
                    }

                    // Yes! We got the pool that
                    // 1. supports usage threshold
                    // 2. has type that match tested type
                    // So, update the pool with new threshold
                    long oldT = getCollectionThresholdOnServer(pools[i]);
                    long newT = newThreshold(getUsageOnServer(pools[i]), oldT,
                            pools[i].toString());
                    try {
                        setCollectionThresholdOnServer(pools[i], newT);
                    } catch (Failure e) {
                        /*
                         * Max value might have changed since the call to newThreshold()
                         * above. If it has fallen below the value of newT, which is certainly
                         * possible, an exception like this one will be thrown from
                         * sun.management.MemoryPoolImpl.setCollectionUsageThreshold():
                         *
                         * java.lang.IllegalArgumentException: Invalid threshold: 48332800 > max (47251456).
                         *
                         * and we'll catch Failure here as a result (it'll be thrown by
                         * Monitor.setLongAttribute).
                         *
                         * We don't know the max value at the time of the failed call, and it
                         * might have changed since the call once more. So there is no point
                         * trying to detect whether the IllegalArgumentException had been
                         * justified, we cannot know it at this point.
                         *
                         * The best we can do is log the fact and continue.
                         */
                        displayInfo("setCollectionThresholdOnServer() failed with " + e + ", ignoring... ",
                                pools[i],
                                "current usage after the call to setCollectionThresholdOnServer(): ", getUsageOnServer(pools[i]),
                                "threshold: ", newT);
                        continue;
                    }
                    displayInfo("Collection threshold is set", pools[i], "usage: ", getUsageOnServer(pools[i]), "threshold: ", newT);
                    if (getCollectionThresholdOnServer(pools[i]) != newT) {
                        complain("Cannot reset collaction threshold from " + oldT
                                + " to " + newT + " in pool " + pools[i].toString());
                        passed = false;
                    }
                } // for i
                break;

            default:
                throw new TestBug("Unknown testMode " + mode);
        }
    } // updateCollectionThresholds()

    // Calculates a new value of threshold based on MemoryUsage and old value of
    // the threshold. New one will be not less than previous one.
    private long newThreshold(MemoryUsage mu, long oldT, String poolName) {
        long newT = mu.getCommitted() / 2 + mu.getUsed() / 2;
        long max = mu.getMax();

        if (newT < oldT) {
            newT = mu.getCommitted() / 2 + oldT / 2;
        }
        if ((max > -1) && (newT > max)) {
            newT = max;
        }
        displayInfo("Changing threshold", poolName, null, null, "new threshold: ", newT);
        return newT;
    }

    // **********************************************************************
    //
    // Methods to work with MBean server in SERVER_MODE
    //
    // **********************************************************************
    // Returns usage threshold value of the pool MBean that is accessed via
    // MBeanServer
    private long getUsageThresholdOnServer(ObjectName pool) {
        return getLongAttribute(pool, UT);
    }

    // Returns collection threshold value of the pool MBean that is accessed via
    // MBeanServer
    private long getCollectionThresholdOnServer(ObjectName pool) {
        return getLongAttribute(pool, CT);
    }

    // Sets new usage threshold value for the pool MBean that is accessed via
    // MBeanServer
    private void setUsageThresholdOnServer(ObjectName pool, long value) {
        setLongAttribute(pool, UT, value);
    }

    // Sets new collection threshold value for the pool MBean that is accessed
    // via MBeanServer
    private void setCollectionThresholdOnServer(ObjectName pool, long value) {
        setLongAttribute(pool, CT, value);
    }

    // Returns MemoryType of the pool MBean that is accessed via MBeanServer.
    private MemoryType getType(ObjectName pool) {
        try {
            Object value = getMBeanServer().getAttribute(pool, POOL_TYPE);
            if (value instanceof MemoryType) {
                return (MemoryType) value;
            } else if (value instanceof String) {
                String name = (String) value;
                return MemoryType.valueOf(name);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace(logger.getOutStream());
            throw new Failure(e);
        }
    }

    // Returns MemoryUsage of the pool MBean that is accessed via MBeanServer
    private MemoryUsage getUsageOnServer(ObjectName pool) {
        return getMemoryUsageAttribute(pool, POOL_U);
    }

    // Returns collection usage of the pool MBean that is accessed via
    // MBeanServer
    private MemoryUsage getCollectionUsageOnServer(ObjectName pool) {
        return getMemoryUsageAttribute(pool, POOL_CU);
    }

    // Returns if usage threshold is supported in the pool
    private boolean isUsageThresholdSupportedOnServer(ObjectName pool) {
        return getBooleanAttribute(pool, UT_SUPPORT);
    }

    // Returns if collection threshold is supported in the pool
    private boolean isCollectionThresholdSupportedOnServer(ObjectName pool) {
        return getBooleanAttribute(pool, CT_SUPPORT);
    }

    // Returns if usage threshold is exceeded in the pool
    private boolean isUsageThresholdExceededOnServer(ObjectName pool) {
        return getBooleanAttribute(pool, UT_EXCEEDED);
    }

    // Returns if collection threshold is exceeded in the pool
    private boolean isCollectionThresholdExceededOnServer(ObjectName pool) {
        return getBooleanAttribute(pool, CT_EXCEEDED);
    }

    // Returns the usage threshold count of the pool
    private long getUsageThresholdCountOnServer(ObjectName pool) {
        return getLongAttribute(pool, UT_COUNT);
    }

    // Returns the collection threshold count of the pool.
    private long getCollectionThresholdCountOnServer(ObjectName pool) {
        return getLongAttribute(pool, CT_COUNT);
    }
    private final StringBuffer buffer = new StringBuffer(1000);

    /**
     * Display information about execution ignoring OOM.
     */
    private void displayInfo(String message, Object pool, String message1, Object n1, String message2, long n2) {
        try {
            buffer.delete(0, buffer.length());
            buffer.append(message);
            if (pool != null) {
                buffer.append(", pool: ");
                buffer.append(pool.toString());
            }
            buffer.append(", ");
            buffer.append(message1);
            buffer.append(n1);
            if (message2 != null) {
                buffer.append(", ");
                buffer.append(message2);
                buffer.append(n2);
            }
            display(buffer.toString());
        } catch (OutOfMemoryError e) {
            // Ignore.
        }
    }

    /**
     * Display information about execution ignoring OOM.
     */
    private void displayInfo(String message, MemoryPoolMXBean pool, String message1, Object n1, String message2, Object n2) {
        try {
            buffer.delete(0, buffer.length());
            buffer.append(message);
            if (pool != null) {
                buffer.append(", pool: ");
                buffer.append(pool.getName());
            }
            buffer.append(", ");
            buffer.append(message1);
            buffer.append(n1);
            if (message2 != null) {
                buffer.append(", ");
                buffer.append(message2);
                buffer.append(n2);
            }
            display(buffer.toString());
        } catch (OutOfMemoryError e) {
            // Ignore.
        }
    }

    // Returns all MemoryPoolMXBeans that are registered on the MBeanServer
    private ObjectName[] getMemoryPoolMXBeansOnServer() {

        // Get all registered MBeans on the server
        ObjectName filterName = null;
        try {
            filterName = new ObjectName(
                 ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",*");

            Set<ObjectName> filteredSet = getMBeanServer().queryNames(filterName, null);
            return filteredSet.toArray(new ObjectName[0]);
        } catch(Exception e) {
            return new ObjectName[0];
        }

    } // getMemoryPoolMXBeansOnServer()

    // **********************************************************************
    //
    // Class to implement polling mechanism of monitoring
    //
    // **********************************************************************
    class Polling extends Thread {

        final static long WAIT_TIME = 100; // Milliseconds
        Object object = new Object();
        long[] thresholdCounts;
        boolean goOn = true;

        public void run() {
            try {
                if (isUsageThreshold) {
                    pollUsageThresholds();
                } else {
                    pollCollectionThresholds();
                }
            } catch (Failure e) {
                complain("Unexpected " + e + " in Polling thread");
                e.printStackTrace(logger.getOutStream());
                passed = false;
            }
        } // run()

        private void pollUsageThresholds() {
            switch (mode) {
                case DIRECTLY_MODE:
                // we can use the same code here for direct and proxy modes
                case PROXY_MODE:
                    List poolsMBean = ManagementFactory.getMemoryPoolMXBeans();

                    // Create an array to store all threshold values
                    thresholdCounts = new long[poolsMBean.size()];
                    for (int i = 0; i < thresholdCounts.length; i++) {
                        thresholdCounts[i] = 0;
                    }

                    while (goOn) {
                        synchronized (object) {
                            try {
                                object.wait(WAIT_TIME);
                            } catch (InterruptedException e) {

                                // Stop the thread
                                return;
                            }
                        } // synchronized

                        for (int i = 0; i < poolsMBean.size(); i++) {
                            MemoryPoolMXBean pool = (MemoryPoolMXBean) poolsMBean.get(i);
                            MemoryType mt = pool.getType();

                            if (!pool.isUsageThresholdSupported()) {
                                continue;
                            }

                            if ((!mt.equals(MemoryType.HEAP)
                                    || !memory.equals(HEAP_TYPE))
                                    && (!mt.equals(MemoryType.NON_HEAP)
                                    || !memory.equals(NONHEAP_TYPE))
                                    && !memory.equals(MIXED_TYPE)) {
                                continue;
                            }

                            boolean exceeded;

                            // The exception is not documented, but it may be
                            // erroneously thrown
                            try {
                                exceeded = pool.isUsageThresholdExceeded();
                            } catch (IllegalArgumentException e) {
                                complain("Unexpected exception while retrieving "
                                        + "isUsageThresholdExceeded() for pool "
                                        + pool.getName());
                                e.printStackTrace(logger.getOutStream());
                                passed = false;
                                continue;
                            }

                            if (!exceeded
                                    || pool.getUsageThresholdCount() == thresholdCounts[i]) {
                                continue;
                            }

                            // Yes! We got the pool that
                            // 1. supports usage threshold
                            // 2. has type that match tested type
                            // 3. its threshold is exceeded
                            // So, update all thresholds
                            long c = pool.getUsageThresholdCount();
                            if (c <= thresholdCounts[i]) {
                                complain("Usage threshold count is not greater "
                                        + "than previous one: " + c + " < "
                                        + thresholdCounts[i] + " in pool "
                                        + pool.getName());
                                passed = false;
                            }
                            thresholdCounts[i] = c;
                            displayInfo("Crossing is noticed", pool, "usage: ", pool.getUsage(), "count: ", c);
                            updateThresholds();
                        } // for i
                    } // while
                    break;

                case SERVER_MODE:
                    ObjectName[] pools = getMemoryPoolMXBeansOnServer();

                    // Create an array to store all threshold values
                    thresholdCounts = new long[pools.length];
                    for (int i = 0; i < thresholdCounts.length; i++) {
                        thresholdCounts[i] = 0;
                    }

                    while (goOn) {
                        synchronized (object) {
                            try {
                                object.wait(WAIT_TIME);
                            } catch (InterruptedException e) {

                                // Stop the thread
                                return;
                            }
                        } // synchronized

                        for (int i = 0; i < pools.length; i++) {
                            MemoryType mt = getType(pools[i]);

                            if (!isUsageThresholdSupportedOnServer(pools[i])) {
                                continue;
                            }

                            if ((!mt.equals(MemoryType.HEAP)
                                    || !memory.equals(HEAP_TYPE))
                                    && (!mt.equals(MemoryType.NON_HEAP)
                                    || !memory.equals(NONHEAP_TYPE))
                                    && !memory.equals(MIXED_TYPE)) {
                                continue;
                            }

                            boolean exceeded;

                            // The exception is not documented, but it may be
                            // erroneously thrown
                            try {
                                exceeded = isUsageThresholdExceededOnServer(pools[i]);
                            } catch (Failure e) {
                                complain("Unexpected exception while retrieving "
                                        + "isUsageThresholdExceeded() for pool "
                                        + pools[i].toString());
                                e.printStackTrace(logger.getOutStream());
                                passed = false;
                                continue;
                            }

                            if (!exceeded
                                    || getUsageThresholdCount(pools[i]) == thresholdCounts[i]) {
                                continue;
                            }

                            // Yes! We got the pool that
                            // 1. supports usage threshold
                            // 2. has type that match tested type
                            // 3. its threshold is exceeded
                            // So, update all thresholds
                            long c = getUsageThresholdCount(pools[i]);
                            if (c <= thresholdCounts[i]) {
                                complain("Usage threshold count is not greater "
                                        + "than previous one: " + c + " < "
                                        + thresholdCounts[i] + " in pool "
                                        + pools[i].toString());
                                passed = false;
                            }
                            thresholdCounts[i] = c;
                            displayInfo("Crossing is noticed", null, "pool: ", pools[i], "usage: ", getUsageOnServer(pools[i]));
                            updateThresholds();
                        } // for i
                    } // while
                    break;

                default:
                    throw new TestBug("Unknown testMode " + mode);
            } // switch
        } // pollUsageThresholds()

        private void pollCollectionThresholds() {
            switch (mode) {
                case DIRECTLY_MODE:
                // we can use the same code here for direct and proxy modes
                case PROXY_MODE:
                    List poolsMBean = ManagementFactory.getMemoryPoolMXBeans();

                    // Create an array to store all threshold values
                    thresholdCounts = new long[poolsMBean.size()];
                    for (int i = 0; i < thresholdCounts.length; i++) {
                        thresholdCounts[i] = 0;
                    }

                    while (goOn) {
                        synchronized (object) {
                            try {
                                object.wait(WAIT_TIME);
                            } catch (InterruptedException e) {

                                // Stop the thread
                                return;
                            }
                        } // synchronized

                        for (int i = 0; i < poolsMBean.size(); i++) {
                            MemoryPoolMXBean pool = (MemoryPoolMXBean) poolsMBean.get(i);
                            MemoryType mt = pool.getType();

                            if (!pool.isCollectionUsageThresholdSupported()) {
                                continue;
                            }

                            if ((!mt.equals(MemoryType.HEAP)
                                    || !memory.equals(HEAP_TYPE))
                                    && (!mt.equals(MemoryType.NON_HEAP)
                                    || !memory.equals(NONHEAP_TYPE))
                                    && !memory.equals(MIXED_TYPE)) {
                                continue;
                            }

                            boolean exceeded;

                            // The exception is not documented, but it may be
                            // erroneously thrown
                            try {
                                exceeded = pool.isCollectionUsageThresholdExceeded();
                            } catch (IllegalArgumentException e) {
                                complain("Unexpected exception while retrieving "
                                        + "isCollectionUsageThresholdExceeded()"
                                        + " for pool " + pool.getName());
                                e.printStackTrace(logger.getOutStream());
                                passed = false;
                                continue;
                            }

                            if (!exceeded
                                    || pool.getCollectionUsageThresholdCount()
                                    == thresholdCounts[i]) {
                                continue;
                            }

                            // Yes! We got thet pool that
                            // 1. supports collection usage threshold
                            // 2. has type that match tested type
                            // 3. its threshold is exceeded
                            // So, update all thresholds
                            long c = pool.getCollectionUsageThresholdCount();
                            if (c <= thresholdCounts[i]) {
                                complain("Collection usage threshold count is "
                                        + "not greater than previous one: " + c
                                        + " < " + thresholdCounts[i] + " in pool "
                                        + pool.getName());
                                passed = false;
                            }
                            thresholdCounts[i] = c;
                            displayInfo("Crossing is noticed", pool, "usage: ", pool.getUsage(), "count: ", c);
                            updateThresholds();
                        } // for i
                    } // while
                    break;

                case SERVER_MODE:
                    ObjectName[] pools = getMemoryPoolMXBeansOnServer();

                    // Create an array to store all threshold values
                    thresholdCounts = new long[pools.length];
                    for (int i = 0; i < thresholdCounts.length; i++) {
                        thresholdCounts[i] = 0;
                    }

                    while (goOn) {
                        synchronized (object) {
                            try {
                                object.wait(WAIT_TIME);
                            } catch (InterruptedException e) {

                                // Stop the thread
                                return;
                            }
                        } // synchronized

                        for (int i = 0; i < pools.length; i++) {
                            MemoryType mt = getType(pools[i]);

                            if (!isCollectionThresholdSupportedOnServer(pools[i])) {
                                continue;
                            }

                            if ((!mt.equals(MemoryType.HEAP)
                                    || !memory.equals(HEAP_TYPE))
                                    && (!mt.equals(MemoryType.NON_HEAP)
                                    || !memory.equals(NONHEAP_TYPE))
                                    && !memory.equals(MIXED_TYPE)) {
                                continue;
                            }

                            boolean exceeded;

                            // The exception is not documented, but it may be
                            // erroneously thrown
                            try {
                                exceeded = isCollectionThresholdExceededOnServer(pools[i]);
                            } catch (Failure e) {
                                complain("Unexpected exception while retrieving "
                                        + "isCollectionUsageThresholdExceeded() "
                                        + "for pool " + pools[i].toString());
                                e.printStackTrace(logger.getOutStream());
                                passed = false;
                                continue;
                            }

                            if (!exceeded
                                    || getCollectionThresholdCountOnServer(pools[i])
                                    == thresholdCounts[i]) {
                                continue;
                            }

                            // Yes! We got thet pool that
                            // 1. supports collection usage threshold
                            // 2. has type that match tested type
                            // 3. its threshold is exceeded
                            // So, update all thresholds
                            long c = getCollectionThresholdCountOnServer(pools[i]);
                            if (c <= thresholdCounts[i]) {
                                complain("Collection usage threshold count is "
                                        + "not greater than previous one: " + c
                                        + " < " + thresholdCounts[i] + " in pool "
                                        + pools[i].toString());
                                passed = false;
                            }
                            thresholdCounts[i] = c;
                            displayInfo("Crossing is noticed", pools[i], "usage: ", getUsageOnServer(pools[i]), "count: ", c);
                            updateThresholds();
                        } // for i
                    } // while
                    break;

                default:
                    throw new TestBug("Unknown testMode " + mode);
            } // switch
        } // pollCollectionThresholds()
    } // class Polling
} // MemoryMonitor
