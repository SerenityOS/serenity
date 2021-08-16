/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.management;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import javax.management.ObjectName;
import java.util.Objects;

/**
 * Implementation for java.lang.management.ThreadMXBean as well as providing the
 * supporting method for com.sun.management.ThreadMXBean.
 * The supporting method for com.sun.management.ThreadMXBean can be moved to
 * jdk.management in the future.
 */

public class ThreadImpl implements ThreadMXBean {
    private final VMManagement jvm;

    // default for thread contention monitoring is disabled.
    private boolean contentionMonitoringEnabled = false;
    private boolean cpuTimeEnabled;
    private boolean allocatedMemoryEnabled;

    /**
     * Constructor of ThreadImpl class.
     */
    protected ThreadImpl(VMManagement vm) {
        this.jvm = vm;
        this.cpuTimeEnabled = jvm.isThreadCpuTimeEnabled();
        this.allocatedMemoryEnabled = jvm.isThreadAllocatedMemoryEnabled();
    }

    @Override
    public int getThreadCount() {
        return jvm.getLiveThreadCount();
    }

    @Override
    public int getPeakThreadCount() {
        return jvm.getPeakThreadCount();
    }

    @Override
    public long getTotalStartedThreadCount() {
        return jvm.getTotalThreadCount();
    }

    @Override
    public int getDaemonThreadCount() {
        return jvm.getDaemonThreadCount();
    }

    @Override
    public boolean isThreadContentionMonitoringSupported() {
        return jvm.isThreadContentionMonitoringSupported();
    }

    @Override
    public synchronized boolean isThreadContentionMonitoringEnabled() {
       if (!isThreadContentionMonitoringSupported()) {
            throw new UnsupportedOperationException(
                "Thread contention monitoring is not supported.");
        }
        return contentionMonitoringEnabled;
    }

    @Override
    public boolean isThreadCpuTimeSupported() {
        return jvm.isOtherThreadCpuTimeSupported();
    }

    @Override
    public boolean isCurrentThreadCpuTimeSupported() {
        return jvm.isCurrentThreadCpuTimeSupported();
    }

    protected boolean isThreadAllocatedMemorySupported() {
        return jvm.isThreadAllocatedMemorySupported();
    }

    @Override
    public boolean isThreadCpuTimeEnabled() {
        if (!isThreadCpuTimeSupported() &&
            !isCurrentThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException(
                "Thread CPU time measurement is not supported");
        }
        return cpuTimeEnabled;
    }

    private void ensureThreadAllocatedMemorySupported() {
        if (!isThreadAllocatedMemorySupported()) {
            throw new UnsupportedOperationException(
                "Thread allocated memory measurement is not supported.");
        }
    }

    protected boolean isThreadAllocatedMemoryEnabled() {
        ensureThreadAllocatedMemorySupported();
        return allocatedMemoryEnabled;
    }

    @Override
    public long[] getAllThreadIds() {
        Util.checkMonitorAccess();

        Thread[] threads = getThreads();
        int length = threads.length;
        long[] ids = new long[length];
        for (int i = 0; i < length; i++) {
            Thread t = threads[i];
            ids[i] = t.getId();
        }
        return ids;
    }

    @Override
    public ThreadInfo getThreadInfo(long id) {
        long[] ids = new long[1];
        ids[0] = id;
        final ThreadInfo[] infos = getThreadInfo(ids, 0);
        return infos[0];
    }

    @Override
    public ThreadInfo getThreadInfo(long id, int maxDepth) {
        long[] ids = new long[1];
        ids[0] = id;
        final ThreadInfo[] infos = getThreadInfo(ids, maxDepth);
        return infos[0];
    }

    @Override
    public ThreadInfo[] getThreadInfo(long[] ids) {
        return getThreadInfo(ids, 0);
    }

    private void verifyThreadId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                "Invalid thread ID parameter: " + id);
        }
    }

    private void verifyThreadIds(long[] ids) {
        Objects.requireNonNull(ids);

        for (int i = 0; i < ids.length; i++) {
            verifyThreadId(ids[i]);
        }
    }

    @Override
    public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
        verifyThreadIds(ids);

        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                "Invalid maxDepth parameter: " + maxDepth);
        }

        // ids has been verified to be non-null
        // an empty array of ids should return an empty array of ThreadInfos
        if (ids.length == 0) return new ThreadInfo[0];

        Util.checkMonitorAccess();

        ThreadInfo[] infos = new ThreadInfo[ids.length]; // nulls
        if (maxDepth == Integer.MAX_VALUE) {
            getThreadInfo1(ids, -1, infos);
        } else {
            getThreadInfo1(ids, maxDepth, infos);
        }
        return infos;
    }

    @Override
    public void setThreadContentionMonitoringEnabled(boolean enable) {
        if (!isThreadContentionMonitoringSupported()) {
            throw new UnsupportedOperationException(
                "Thread contention monitoring is not supported");
        }

        Util.checkControlAccess();

        synchronized (this) {
            if (contentionMonitoringEnabled != enable) {
                if (enable) {
                    // if reeabled, reset contention time statistics
                    // for all threads
                    resetContentionTimes0(0);
                }

                // update the VM of the state change
                setThreadContentionMonitoringEnabled0(enable);

                contentionMonitoringEnabled = enable;
            }
        }
    }

    private boolean verifyCurrentThreadCpuTime() {
        // check if Thread CPU time measurement is supported.
        if (!isCurrentThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException(
                "Current thread CPU time measurement is not supported.");
        }
        return isThreadCpuTimeEnabled();
    }

    @Override
    public long getCurrentThreadCpuTime() {
        if (verifyCurrentThreadCpuTime()) {
            return getThreadTotalCpuTime0(0);
        }
        return -1;
    }

    @Override
    public long getThreadCpuTime(long id) {
        long[] ids = new long[1];
        ids[0] = id;
        final long[] times = getThreadCpuTime(ids);
        return times[0];
    }

    private boolean verifyThreadCpuTime(long[] ids) {
        verifyThreadIds(ids);

        // check if Thread CPU time measurement is supported.
        if (!isThreadCpuTimeSupported() &&
            !isCurrentThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException(
                "Thread CPU time measurement is not supported.");
        }

        if (!isThreadCpuTimeSupported()) {
            // support current thread only
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] != Thread.currentThread().getId()) {
                    throw new UnsupportedOperationException(
                        "Thread CPU time measurement is only supported" +
                        " for the current thread.");
                }
            }
        }

        return isThreadCpuTimeEnabled();
    }

    protected long[] getThreadCpuTime(long[] ids) {
        boolean verified = verifyThreadCpuTime(ids);

        int length = ids.length;
        long[] times = new long[length];
        java.util.Arrays.fill(times, -1);

        if (verified) {
            if (length == 1) {
                long id = ids[0];
                if (id == Thread.currentThread().getId()) {
                    id = 0;
                }
                times[0] = getThreadTotalCpuTime0(id);
            } else {
                getThreadTotalCpuTime1(ids, times);
            }
        }
        return times;
    }

    @Override
    public long getCurrentThreadUserTime() {
        if (verifyCurrentThreadCpuTime()) {
            return getThreadUserCpuTime0(0);
        }
        return -1;
    }

    @Override
    public long getThreadUserTime(long id) {
        long[] ids = new long[1];
        ids[0] = id;
        final long[] times = getThreadUserTime(ids);
        return times[0];
    }

    protected long[] getThreadUserTime(long[] ids) {
        boolean verified = verifyThreadCpuTime(ids);

        int length = ids.length;
        long[] times = new long[length];
        java.util.Arrays.fill(times, -1);

        if (verified) {
            if (length == 1) {
                long id = ids[0];
                if (id == Thread.currentThread().getId()) {
                    id = 0;
                }
                times[0] = getThreadUserCpuTime0(id);
            } else {
                getThreadUserCpuTime1(ids, times);
            }
        }
        return times;
    }

    @Override
    public void setThreadCpuTimeEnabled(boolean enable) {
        if (!isThreadCpuTimeSupported() &&
            !isCurrentThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException(
                "Thread CPU time measurement is not supported");
        }

        Util.checkControlAccess();
        synchronized (this) {
            if (cpuTimeEnabled != enable) {
                // notify VM of the state change
                setThreadCpuTimeEnabled0(enable);
                cpuTimeEnabled = enable;
            }
        }
    }

    protected long getCurrentThreadAllocatedBytes() {
        if (isThreadAllocatedMemoryEnabled()) {
            return getThreadAllocatedMemory0(0);
        }
        return -1;
    }

    private boolean verifyThreadAllocatedMemory(long id) {
        verifyThreadId(id);
        return isThreadAllocatedMemoryEnabled();
    }

    protected long getThreadAllocatedBytes(long id) {
        boolean verified = verifyThreadAllocatedMemory(id);

        if (verified) {
            return getThreadAllocatedMemory0(
                Thread.currentThread().getId() == id ? 0 : id);
        }
        return -1;
    }

    private boolean verifyThreadAllocatedMemory(long[] ids) {
        verifyThreadIds(ids);
        return isThreadAllocatedMemoryEnabled();
    }

    protected long[] getThreadAllocatedBytes(long[] ids) {
        Objects.requireNonNull(ids);

        if (ids.length == 1) {
            long size = getThreadAllocatedBytes(ids[0]);
            return new long[] { size };
        }

        boolean verified = verifyThreadAllocatedMemory(ids);

        long[] sizes = new long[ids.length];
        java.util.Arrays.fill(sizes, -1);

        if (verified) {
            getThreadAllocatedMemory1(ids, sizes);
        }
        return sizes;
    }

    protected void setThreadAllocatedMemoryEnabled(boolean enable) {
        ensureThreadAllocatedMemorySupported();

        Util.checkControlAccess();
        synchronized (this) {
            if (allocatedMemoryEnabled != enable) {
                // notify VM of the state change
                setThreadAllocatedMemoryEnabled0(enable);
                allocatedMemoryEnabled = enable;
            }
        }
    }

    @Override
    public long[] findMonitorDeadlockedThreads() {
        Util.checkMonitorAccess();

        Thread[] threads = findMonitorDeadlockedThreads0();
        if (threads == null) {
            return null;
        }

        long[] ids = new long[threads.length];
        for (int i = 0; i < threads.length; i++) {
            Thread t = threads[i];
            ids[i] = t.getId();
        }
        return ids;
    }

    @Override
    public long[] findDeadlockedThreads() {
        if (!isSynchronizerUsageSupported()) {
            throw new UnsupportedOperationException(
                "Monitoring of Synchronizer Usage is not supported.");
        }

        Util.checkMonitorAccess();

        Thread[] threads = findDeadlockedThreads0();
        if (threads == null) {
            return null;
        }

        long[] ids = new long[threads.length];
        for (int i = 0; i < threads.length; i++) {
            Thread t = threads[i];
            ids[i] = t.getId();
        }
        return ids;
    }

    @Override
    public void resetPeakThreadCount() {
        Util.checkControlAccess();
        resetPeakThreadCount0();
    }

    @Override
    public boolean isObjectMonitorUsageSupported() {
        return jvm.isObjectMonitorUsageSupported();
    }

    @Override
    public boolean isSynchronizerUsageSupported() {
        return jvm.isSynchronizerUsageSupported();
    }

    private void verifyDumpThreads(boolean lockedMonitors,
                                   boolean lockedSynchronizers) {
        if (lockedMonitors && !isObjectMonitorUsageSupported()) {
            throw new UnsupportedOperationException(
                "Monitoring of Object Monitor Usage is not supported.");
        }

        if (lockedSynchronizers && !isSynchronizerUsageSupported()) {
            throw new UnsupportedOperationException(
                "Monitoring of Synchronizer Usage is not supported.");
        }

        Util.checkMonitorAccess();
    }

    @Override
    public ThreadInfo[] getThreadInfo(long[] ids,
                                      boolean lockedMonitors,
                                      boolean lockedSynchronizers) {
        return dumpThreads0(ids, lockedMonitors, lockedSynchronizers,
                            Integer.MAX_VALUE);
    }

    public ThreadInfo[] getThreadInfo(long[] ids,
                                      boolean lockedMonitors,
                                      boolean lockedSynchronizers,
                                      int maxDepth) {
        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                    "Invalid maxDepth parameter: " + maxDepth);
        }
        verifyThreadIds(ids);
        // ids has been verified to be non-null
        // an empty array of ids should return an empty array of ThreadInfos
        if (ids.length == 0) return new ThreadInfo[0];

        verifyDumpThreads(lockedMonitors, lockedSynchronizers);
        return dumpThreads0(ids, lockedMonitors, lockedSynchronizers, maxDepth);
    }

    @Override
    public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
                                       boolean lockedSynchronizers) {
        return dumpAllThreads(lockedMonitors, lockedSynchronizers,
                              Integer.MAX_VALUE);
    }

    public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
                                       boolean lockedSynchronizers,
                                       int maxDepth) {
        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                    "Invalid maxDepth parameter: " + maxDepth);
        }
        verifyDumpThreads(lockedMonitors, lockedSynchronizers);
        return dumpThreads0(null, lockedMonitors, lockedSynchronizers, maxDepth);
    }

    // VM support where maxDepth == -1 to request entire stack dump
    private static native Thread[] getThreads();
    private static native void getThreadInfo1(long[] ids,
                                              int maxDepth,
                                              ThreadInfo[] result);
    private static native long getThreadTotalCpuTime0(long id);
    private static native void getThreadTotalCpuTime1(long[] ids, long[] result);
    private static native long getThreadUserCpuTime0(long id);
    private static native void getThreadUserCpuTime1(long[] ids, long[] result);
    private static native long getThreadAllocatedMemory0(long id);
    private static native void getThreadAllocatedMemory1(long[] ids, long[] result);
    private static native void setThreadCpuTimeEnabled0(boolean enable);
    private static native void setThreadAllocatedMemoryEnabled0(boolean enable);
    private static native void setThreadContentionMonitoringEnabled0(boolean enable);
    private static native Thread[] findMonitorDeadlockedThreads0();
    private static native Thread[] findDeadlockedThreads0();
    private static native void resetPeakThreadCount0();
    private static native ThreadInfo[] dumpThreads0(long[] ids,
                                                    boolean lockedMonitors,
                                                    boolean lockedSynchronizers,
                                                    int maxDepth);

    // tid == 0 to reset contention times for all threads
    private static native void resetContentionTimes0(long tid);

    @Override
    public ObjectName getObjectName() {
        return Util.newObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
    }

}
