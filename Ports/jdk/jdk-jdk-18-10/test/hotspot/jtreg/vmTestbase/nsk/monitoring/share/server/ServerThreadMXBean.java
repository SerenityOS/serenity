/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share.server;

import java.util.*;
import nsk.monitoring.share.*;
import javax.management.*;
import java.lang.management.*;
import javax.management.openmbean.*;

/**
 * ThreadMXBean implementation that delegates functionality to MBeanServer.
 */
public class ServerThreadMXBean extends ServerMXBean implements ThreadMXBean {
        public ServerThreadMXBean(MBeanServer mbeanServer) {
                super(mbeanServer, ManagementFactory.THREAD_MXBEAN_NAME);
        }

        public ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers) {
                return getThreadInfoArr(invokeMethod("dumpAllThreads", new Object[] { lockedMonitors, lockedSynchronizers },
                        new String[] { boolean.class.getName(), boolean.class.getName() }));
        }

        public ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers, int maxDepth) {
                return getThreadInfoArr(invokeMethod("dumpAllThreads", new Object[] { lockedMonitors, lockedSynchronizers, maxDepth },
                        new String[] { boolean.class.getName(), boolean.class.getName(), int.class.getName() }));
        }

        public long[] findDeadlockedThreads() {
                return (long[]) invokeMethod("findDeadlockedThreads", null, null);
        }

        public long[] findMonitorDeadlockedThreads() {
                return (long[]) invokeMethod("findMonitorDeadlockedThreads", null, null);
        }

        public long[] getAllThreadIds() {
                return (long[]) invokeMethod("getAllThreadIds", null, null);
        }

        public long getCurrentThreadCpuTime() {
                return getLongAttribute("CurrentThreadCpuTime");
        }

        public long getCurrentThreadUserTime() {
                return getLongAttribute("CurrentThreadUserTime");
        }

        public int getDaemonThreadCount() {
                return getIntAttribute("DaemonThreadCount");
        }

        public int getPeakThreadCount() {
                return getIntAttribute("PeakThreadCount");
        }

        public int getThreadCount() {
                return getIntAttribute("ThreadCount");
        }

        public long getThreadCpuTime(long id) {
                throw new UnsupportedOperationException("This method is not supported");
        }

        public ThreadInfo getThreadInfo(long id) {
                return getThreadInfo(invokeMethod("getThreadInfo", new Object[] { id }, new String[] { long.class.getName() }));
        }

        public ThreadInfo[] getThreadInfo(long[] ids) {
                return getThreadInfoArr(invokeMethod("getThreadInfo", new Object[] { ids },
                        new String[] { long[].class.getName() }));
        }

        public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors, boolean lockedSynchronizers) {
                return getThreadInfoArr(invokeMethod("getThreadInfo", new Object[] { ids, lockedMonitors, lockedSynchronizers },
                        new String[] { long[].class.getName(), boolean.class.getName(), boolean.class.getName() }));
        }

        public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors, boolean lockedSynchronizers, int maxDepth) {
                return getThreadInfoArr(invokeMethod("getThreadInfo", new Object[] { ids, lockedMonitors, lockedSynchronizers, maxDepth },
                        new String[] { long[].class.getName(), boolean.class.getName(), boolean.class.getName(), int.class.getName() }));
        }

        public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
                return getThreadInfoArr(invokeMethod("getThreadInfo", new Object[] { ids, maxDepth }, new String[] { long[].class.getName(), int.class.getName() }));
        }

        public ThreadInfo getThreadInfo(long id, int maxDepth) {
                return getThreadInfo(invokeMethod("getThreadInfo", new Object[] { id, maxDepth }, new String[] { long.class.getName(), int.class.getName() }));
        }

        public long getThreadUserTime(long id) {
                throw new UnsupportedOperationException("This method is not supported");
        }

        public long getTotalStartedThreadCount() {
                return getLongAttribute("TotalStartedThreadCount");
        }

        public boolean isCurrentThreadCpuTimeSupported() {
                return getBooleanAttribute("CurrentThreadCpuTimeSupported");
        }

        public boolean isObjectMonitorUsageSupported() {
                return getBooleanAttribute("ObjectMonitorUsageSupported");
        }

        public boolean isSynchronizerUsageSupported() {
                return getBooleanAttribute("SynchronizerUsageSupported");
        }

        public boolean isThreadContentionMonitoringEnabled() {
                return getBooleanAttribute("ThreadContentionMonitoringEnabled");
        }

        public boolean isThreadContentionMonitoringSupported() {
                return getBooleanAttribute("ThreadContentionMonitoringSupported");
        }

        public boolean isThreadCpuTimeEnabled() {
                return getBooleanAttribute("ThreadCpuTimeEnabled");
        }

        public boolean isThreadCpuTimeSupported() {
                return getBooleanAttribute("ThreadCpuTimeSupported");
        }

        public void resetPeakThreadCount() {
                invokeVoidMethod("resetPeakThreadCount");
        }

        public void setThreadContentionMonitoringEnabled(boolean enable) {
                setBooleanAttribute("ThreadContentionMonitorinEnabled", enable);
        }

        public void setThreadCpuTimeEnabled(boolean enable) {
                setBooleanAttribute("ThreadCpuTimeEnabled", enable);
        }

        protected ThreadInfo getThreadInfo(Object o) {
                return convertObject(o, ThreadInfo.class);
        }

        protected ThreadInfo[] getThreadInfoArr(Object o) {
                return convertArray(o, ThreadInfo[].class);
        }
}
