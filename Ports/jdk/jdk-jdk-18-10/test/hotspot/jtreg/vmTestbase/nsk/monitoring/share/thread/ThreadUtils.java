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

package nsk.monitoring.share.thread;

import nsk.share.log.Log;
import nsk.share.TestFailure;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

public final class ThreadUtils {
        private ThreadUtils() {
        }

        /**
         * Obtain human readable string representation of lock info.
         *
         * @param info lock ingo
         * @return string representation of info
         */
        public static String strLockInfo(LockInfo info) {
                if (info == null)
                        return "LockInfo: " + null;
                else
                        return "LockInfo: className: " + info.getClassName() + " identityHashCode: " + info.getIdentityHashCode();
        }

        /**
         * Obtain human readable string representation of monitor info.
         *
         * @param info monitor info
         * @return string representation of info
         */
        public static String strMonitorInfo(MonitorInfo info) {
                if (info == null)
                        return "MonitorInfo: null";
                else
                        return "MonitorInfo: lockedStackDepth: " + info.getLockedStackDepth() + " lockedStackFrame: " + info.getLockedStackFrame() + strLockInfo(info);
        }


        /**
         * Obtain human readable string representation of array of lock info.
         *
         * @param infos array of lock info
         * @return string representation of infos
         */
        public static String strLockInfoArr(LockInfo[] infos) {
                String s = "";
                for (LockInfo info : infos)
                        s += strLockInfo(info) + " ";
                return s;
        }

        /**
         * Obtain human readable string representation of array of monitor info.
         *
         * @param infos array of monitor info
         * @return string representation of infos
         */
        public static String strMonitorInfoArr(MonitorInfo[] infos) {
                String s = "";
                for (MonitorInfo info : infos)
                        s += strMonitorInfo(info) + " ";
                return s;
        }

        /**
         * Print monitor information.
         *
         * @param log log
         * @param monitorInfo monitor info
         */
        public static void printMonitorInfo(Log log, MonitorInfo monitorInfo) {
                log.info(strMonitorInfo(monitorInfo));
        }

        /**
         * Print monitor information array.
         *
         * @param log log
         * @param monitorInfos array of monitor info
         */
        public static void printMonitorInfo(Log log, MonitorInfo[] monitorInfos) {
                for (MonitorInfo monitorInfo : monitorInfos)
                        printMonitorInfo(log, monitorInfo);
        }

        /**
         * Print lock information.
         *
         * @param log log
         * @param lockInfo lock info
         */
        public static void printLockInfo(Log log, LockInfo lockInfo) {
                log.info(strLockInfo(lockInfo));
        }

        /**
         * Print lock information array.
         *
         * @param log log
         * @param lockInfo array of lock info
         */
        public static void printLockInfo(Log log, LockInfo[] lockInfos) {
                for (LockInfo lockInfo : lockInfos)
                        printLockInfo(log, lockInfo);
        }

        public static final String INDENT = "   ";

        /**
         * Print all thread information for given ThreadInfo.
         *
         * @param log log
         * @param ti thread info
         */
        public static void threadInfo(Log log, ThreadInfo info) {
                //log.info("Thread info: thread " + info.getThreadId() + " " + info.getThreadName() " state: " + info.getThreadState());
                threadDump(log, info);
                log.info("Blocked count: " + info.getBlockedCount() + " time: " + info.getBlockedTime());
                log.info("Waited count: " + info.getWaitedCount() + " time: " + info.getWaitedTime());
                LockInfo lockInfo = info.getLockInfo();
                log.info("Lock: " + strLockInfo(lockInfo));
                log.info("Lock name: " + info.getLockName() + " ownerId: " + info.getLockOwnerId() + " ownerName: " + info.getLockOwnerName());
                MonitorInfo[] monitorInfo = info.getLockedMonitors();
                log.info("Locked monitors: total " + monitorInfo.length);
                printMonitorInfo(log, info.getLockedMonitors());
                LockInfo[] lockInfos = info.getLockedSynchronizers();
                log.info("Locked synchronizers: total " + lockInfos.length);
                printLockInfo(log, lockInfos);
        }

        /**
         * Print thread dump information for given ThreadInfo.
         *
         * @param log log
         * @param ti thread info
         */
        public static void threadDump(Log log, ThreadInfo ti) {
                String s = "";
                s += "\"";
                s += ti.getThreadName();
                s += "\"";
                s += " Id=" + ti.getThreadId();
                s += " in " + ti.getThreadState();
                if (ti.getLockName() != null) {
                        s += " on lock=" + ti.getLockName();
                }
                if (ti.isSuspended()) {
                        s += " (suspended)";
                }
                if (ti.isInNative()) {
                        s += " (running in native)";
                }
                if (ti.getLockOwnerName() != null)
                        s += " owned by " + ti.getLockOwnerName() + " Id=" + ti.getLockOwnerId();
                log.info(s);
                printStackTrace(log, ti);
        }

        /**
         * Print stack trace information.
         *
         * @param log log
         * @param ti thread info
         */
        public static void printStackTrace(Log log, ThreadInfo ti) {
                StackTraceElement[] stacktrace = ti.getStackTrace();
                MonitorInfo[] monitors = ti.getLockedMonitors();
                for (int i = 0; i < stacktrace.length; i++) {
                        StackTraceElement ste = stacktrace[i];
                        log.info(INDENT + "at " + ste.toString());

                        for (MonitorInfo mi : monitors) {
                                if (mi.getLockedStackDepth() == i) {
                                        log.info(INDENT + "  - locked " + mi);
                                }
                        }
                }
                System.out.println();

        }

        /**
         * Dump information about threads.
         *
         * @param log log
         * @param tinfos array of thread information
         */
        public static void threadDump(Log log, ThreadInfo[] tinfos) {
                log.info("Java thread dump");
                for (ThreadInfo ti : tinfos) {
                        threadDump(log, ti);
                        LockInfo[] syncs = ti.getLockedSynchronizers();
                        printLockInfo(log, syncs);
                }
        }

        public final static int waitThreadStateRetries = 10;
        public final static long waitThreadStateSleepTime = 100;

        public static void waitThreadState(Thread thread, Thread.State state) {
                int retries = 0;
                long ctime = System.currentTimeMillis();
                while (thread.getState() != state) {
                        if (retries++ > waitThreadStateRetries)
                                throw new TestFailure("Thread " + thread + " with current state " + thread.getState() + " did not reach state " + state + " with number of  retries: " + retries + ", time: " + (System.currentTimeMillis() - ctime));
                        try {
                                Thread.sleep(waitThreadStateSleepTime);
                        } catch (InterruptedException e) {
                        }
                }
        }

        /**
         * Verify condition and throw TestFailure if it does not hold.
         *
         * @param condition boolean condition
         * @param message TestFailure message
         */
        public static void verify(boolean condition, String message) {
                if (!condition)
                        throw new TestFailure(message);
        }

        /**
         * Obtain human readable string representation of array of thread ids.
         *
         * @param ids array of ids
         * @return string representation of ids
         */
        public static String strIds(long[] ids) {
                if (ids == null)
                        return null;
                String s = "";
                for (int i = 0; i < ids.length; ++i) {
                        s += ids[i];
                        s += ' ';
                }
                return s;
        }
}
