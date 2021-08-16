/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * <code>ThreadMonitor</code> class is a wrapper of <code>ThreadMXBean</code>.
 * Depending on command line arguments, an instance of this class redirects
 * invocations to the <code>ThreadMXBean</code> interface. If
 * <code>-testMode="directly"</code> option is set, this instance directly
 * invokes corresponding method of the <code>ThreadMXBean</code> interface.
 * If <code>-testMode="server"</code> option is set it will make invocations
 * via MBeanServer. If <code>-testMode="proxy"</code> option is set it will
 * make invocations via MBeanServer proxy.
 *
 * @see ArgumentHandler
 */
public class ThreadMonitor extends Monitor {

    // Names of the attributes of ThreadMXBean
    private static final String GET_THREAD_INFO = "getThreadInfo";
    private static final String GET_THREAD_CPU_TIME = "ThreadCpuTime";
    private static final String ALL_THREAD_IDS = "AllThreadIds";
    private static final String RESET_PEAK = "resetPeakThreadCount";
    private static final String GET_PEAK_COUNT = "PeakThreadCount";
    private static final String THREAD_COUNT = "ThreadCount";
    private static final String FIND_THREADS = "findMonitorDeadlockedThreads";
    private static final String IS_CURRENT = "CurrentThreadCpuTimeSupported";
    private static final String IS_CPUTIME = "ThreadCpuTimeSupported";
    private static final String IS_CONT_SUPP
        = "ThreadContentionMonitoringSupported";
    private static final String IS_CONT_ENAB
        = "ThreadContentionMonitoringEnabled";

    // An instance of ThreadMXBean
    private static ThreadMXBean mbean = ManagementFactory.getThreadMXBean();

    // proxy instance
    private ThreadMXBean proxyInstance;

    // Internal trace level
    private static final int TRACE_LEVEL = 10;

    static {
        Monitor.logPrefix = "ThreadMonitor> ";
    }

    /**
     * Creates a new <code>ThreadMonitor</code> object.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     */
    public ThreadMonitor(Log log, ArgumentHandler argumentHandler) {
        super(log, argumentHandler);
    }

    /**
     *
     * Return a proxy instance for a platform
     * {@link java.lang.management.ThreadMXBean
     * <code>ThreadMXBean</code>} interface.
     *
     */
    synchronized ThreadMXBean getProxy() {
        if (proxyInstance == null) {
            // create proxy instance
            try {
                proxyInstance = (ThreadMXBean)
                ManagementFactory.newPlatformMXBeanProxy(
                    getMBeanServer(),
                    ManagementFactory.THREAD_MXBEAN_NAME,
                    ThreadMXBean.class
                );
            } catch (Exception e) {
                throw new Failure(e);
            }
        }
        return proxyInstance;
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#getAllThreadIds()
     * <code>ThreadMXBean.getAllThreadIds()</code>}.
     *
     * @return an array of <code>long</code>, each is a thread ID.
     */
    public long[] getAllThreadIds() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            logger.trace(TRACE_LEVEL,"[getAllThreadIds] getAllThreadIds()"
                            + " directly invoked");
            return mbean.getAllThreadIds();

        case SERVER_MODE:
            logger.trace(TRACE_LEVEL,"[getAllThreadIds] getAllThreadIds()"
                       + " invoked through MBeanServer");
            return getLongArrayAttribute(mbeanObjectName, ALL_THREAD_IDS);

        case PROXY_MODE:
            logger.trace(TRACE_LEVEL,"[getAllThreadIds] getAllThreadIds()"
                            + " invoked through MBeanServer proxy");
            return getProxy().getAllThreadIds();
        }

        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#getThreadInfo
     * <code>ThreadMXBean.getThreadInfo()</code>}.
     *
     * @param id the thread ID of the thread.
     * @param maxDepth the maximum number of entries in the stack trace to
     *        be dumped. <code>Integer.MAX_VALUE</code> could be used to request
     *        entire stack to be dumped.
     *
     * @return A <code>ThreadInfo</code> of the thread of the given ID.
     *          <code>null</code> if the thread of the given ID is not alive
     *          or it does not exist
     */
    public ThreadInfo getThreadInfo(long id, int maxDepth) {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            logger.trace(TRACE_LEVEL, "[getThreadInfo] getThreadInfo(long, "
                       + "int) directly invoked");
            return mbean.getThreadInfo(id, maxDepth);

        case SERVER_MODE:
            Object[] params = {Long.valueOf(id), Integer.valueOf(maxDepth)};
            String[] signature = {"long", "int"};

            try {
                logger.trace(TRACE_LEVEL, "[getThreadInfo] getThreadInfo(long, "
                           + "int) invoked through MBeanServer");
                Object value = getMBeanServer().invoke(mbeanObjectName,
                                                             GET_THREAD_INFO,
                                                             params, signature);
                if (value instanceof ThreadInfo)
                        return (ThreadInfo) value;
                else {
                        CompositeData data = (CompositeData) value;
                        return ThreadInfo.from(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Failure(e);
            }
        case PROXY_MODE:
            logger.trace(TRACE_LEVEL, "[getThreadInfo] getThreadInfo(long, "
                       + "int) invoked through MBeanServer proxy");
            return getProxy().getThreadInfo(id, maxDepth);
        }

        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#isCurrentThreadCpuTimeSupported()
     * <code>ThreadMXBean.isCurrentThreadCpuTimeSupported()</code>}.
     *
     * @return <code>true</code>, if the JVM supports CPU time measurement for
     *         current thread, <code>false</code> otherwise.
     */
    public boolean isCurrentThreadCpuTimeSupported() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return mbean.isCurrentThreadCpuTimeSupported();

        case SERVER_MODE:
            return getBooleanAttribute(mbeanObjectName, IS_CURRENT);

        case PROXY_MODE:
            return getProxy().isCurrentThreadCpuTimeSupported();
        }

        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#isThreadCpuTimeSupported()
     * <code>ThreadMXBean.isThreadCpuTimeSupported()</code>}.
     *
     * @return <code>true</code>, if the JVM supports CPU time measurement for
     *         any threads, <code>false</code> otherwise.
     */
    public boolean isThreadCpuTimeSupported() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return mbean.isThreadCpuTimeSupported();

        case SERVER_MODE:
            return getBooleanAttribute(mbeanObjectName, IS_CPUTIME);

        case PROXY_MODE:
            return getProxy().isThreadCpuTimeSupported();
        }

        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#isThreadContentionMonitoringSupported()
     * <code>ThreadMXBean.isThreadContentionMonitoringSupported()</code>}.
     *
     * @return <code>true</code>, if the JVM supports thread contantion
     *         monitoring, <code>false</code> otherwise.
     */
    public boolean isThreadContentionMonitoringSupported() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return mbean.isThreadContentionMonitoringSupported();

        case SERVER_MODE:
            return getBooleanAttribute(mbeanObjectName, IS_CONT_SUPP);

        case PROXY_MODE:
            return getProxy().isThreadContentionMonitoringSupported();
        }

        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#setThreadContentionMonitoringEnabled
     * <code>ThreadMXBean.setThreadContentionMonitoringEnabled()</code>}.
     *
     * @param enable <code>true</code> to enable, <code>false</code> to disable.
     */
    public void setThreadContentionMonitoringEnabled(boolean enable) {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            mbean.setThreadContentionMonitoringEnabled(enable);
            break;

        case SERVER_MODE:
            setBooleanAttribute(mbeanObjectName, IS_CONT_ENAB, enable);
            break;

        case PROXY_MODE:
            getProxy().setThreadContentionMonitoringEnabled(enable);
            break;

        default:
            throw new TestBug("Unknown testMode " + mode);
        }
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#getThreadCpuTime
     * <code>ThreadMXBean.getThreadCpuTime()</code>}.
     *
     * @param id the id of a thread
     *
     * @return the CPU time for a thread of the specified ID, if the thread
     *         existsand is alive and CPU time measurement is enabled, -1
     *         otherwise.
     */
    public long getThreadCpuTime(long id) {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return mbean.getThreadCpuTime(id);

        case SERVER_MODE:
            Object[] params = {Long.valueOf(id)};
            String[] signature = {"long"};

            try {
                Long l = (Long) getMBeanServer().invoke(mbeanObjectName,
                                                            GET_THREAD_CPU_TIME,
                                                             params, signature);
                return l.longValue();
            } catch (Exception e) {
                e.printStackTrace();
                throw new Failure(e);
            }

        case PROXY_MODE:
            return getProxy().getThreadCpuTime(id);
        }

        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#getThreadCount()
     * <code>ThreadMXBean.getThreadCount()</code>}.
     *
     * @return the current number of live threads.
     */
    public int getThreadCount() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return mbean.getThreadCount();

        case SERVER_MODE:
            return getIntAttribute(mbeanObjectName, THREAD_COUNT);

        case PROXY_MODE:
            return getProxy().getThreadCount();
        }

        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#getPeakThreadCount()
     * <code>ThreadMXBean.getPeakThreadCount()</code>}.
     *
     * @return the peak live thrad count.
     */
    public int getPeakThreadCount() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return mbean.getPeakThreadCount();

        case SERVER_MODE:
            return getIntAttribute(mbeanObjectName, GET_PEAK_COUNT);

        case PROXY_MODE:
            return getProxy().getPeakThreadCount();
        }

        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#resetPeakThreadCount()
     * <code>ThreadMXBean.resetPeakThreadCount()</code>}.
     */
    public void resetPeakThreadCount() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            mbean.resetPeakThreadCount();
            break;

        case SERVER_MODE:
            try {
               getMBeanServer().invoke(mbeanObjectName, RESET_PEAK, null, null);
            } catch (Exception e) {
                e.printStackTrace(logger.getOutStream());
                throw new Failure(e);
            }
            break;
        case PROXY_MODE:
            getProxy().resetPeakThreadCount();
            break;

        default:
            throw new TestBug("Unknown testMode " + mode);
        }

    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ThreadMXBean#findMonitorDeadlockedThreads
     * <code>ThreadMXBean.findMonitorDeadlockedThreads()</code>}.
     *
     * @return an array of IDs of the reads that are monitor deadlocked, if any;
     *         <code>null</code> otherwise.
     */
    public long[] findMonitorDeadlockedThreads() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return mbean.findMonitorDeadlockedThreads();

        case SERVER_MODE:
            try {
                Object o = (Object) getMBeanServer().invoke(mbeanObjectName,
                                                      FIND_THREADS, null, null);
                return (long[]) o;
            } catch (Exception e) {
                e.printStackTrace(logger.getOutStream());
                throw new Failure(e);
            }
        case PROXY_MODE:
            return getProxy().findMonitorDeadlockedThreads();
        }
        throw new TestBug("Unknown testMode " + mode);
    }
} // ThreadMonitor
