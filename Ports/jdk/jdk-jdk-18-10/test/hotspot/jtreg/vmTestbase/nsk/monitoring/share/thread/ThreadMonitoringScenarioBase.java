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

import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.LockInfo;
import nsk.share.log.Log;
import nsk.share.log.LogAware;
import nsk.share.TestFailure;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

/**
 * Base class for all threads that are used in monitoring testing.
 */
public abstract class ThreadMonitoringScenarioBase implements LogAware, ThreadMonitoringScenario {
        protected static boolean lockedMonitorsAvailable = true;
        protected static boolean lockedSynchronizersAvailable = true;
        protected Log log;

        public ThreadMonitoringScenarioBase(Log log) {
                setLog(log);
        }

        public abstract void begin();

        public abstract void waitState();

        public abstract void finish();

        public abstract void end();

        protected void printThreadInfo(ThreadInfo info) {
                //ThreadUtils.threadDump(log, threadMXBean.dumpAllThreads(true, true));
                ThreadUtils.threadInfo(log, info);
        }


        /**
         * Check that there are no unexpected elements in stack trace.
         */
        protected boolean checkStackTrace(StackTraceElement[] elements) {
                boolean unexpected = false;
                for (StackTraceElement element : elements)
                        if (!isStackTraceElementExpected(element)) {
                                if (!unexpected) {
                                        log.info("Unexpected stack trace elements for: " + this);
                                        unexpected = true;
                                }
                                log.info(ThreadUtils.INDENT + "at " + element);
                        }
                return !unexpected;
        }

        /**
         * Verifies that given stack trace element from stack trace is expected
         * in pre-defined state. This method will be called by checkStackTrace
         * for each element.
         *
         * @param element stack trace element
         * @return true if element is expected, false otherwise
         */
        protected boolean isStackTraceElementExpected(StackTraceElement element) {
                return false;
        }

        /**
         * Check that stack trace element is expected.
         */
        protected boolean checkStackTraceElement(StackTraceElement element, String[] expectedMethods) {
                String name = element.getClassName() + "." + element.getMethodName();
                for (String method : expectedMethods)
                        if (method.equals(name))
                                return true;
                return false;
        }

        /**
         * Check that lock info matches given lock object.
         *
         * @param lockInfo lock info
         * @param lock lock object
         */
        protected void checkLockInfo(LockInfo lockInfo, Object lock) {
                if (lock != null) {
                        verify(lockInfo.getClassName().equals(lock.getClass().getName()), "LockInfo.getClassName() = " + lockInfo.getClassName() + " differs from lock.getClass().getName() = " + lock.getClass().getName());
                        verify(lockInfo.getIdentityHashCode() == System.identityHashCode(lock), "LockInfo.getIdentityHashCode() = " + lockInfo.getIdentityHashCode() + " differs from System.identityHashCode(lock) = " + System.identityHashCode(lock));
                        String expectedToString = lock.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(lock));
                        verify(lockInfo.toString().equals(expectedToString), "LockInfo.toString() = " + lockInfo.toString() + " differs from expected toString() = " + expectedToString);
                } else
                        verify(lockInfo == null, "Unexpected ThreadInfo.getLockInfo(): " + ThreadUtils.strLockInfo(lockInfo));
        }

        /**
         * Check that given MonitorInfo matches given lock object and method name.
         *
         * @param monitorInfo monitor info
         * @param lock lock object
         * @param methodName method name
         */
        protected void checkMonitorInfo(MonitorInfo monitorInfo, Object lock, String methodName) {
                checkLockInfo(monitorInfo, lock);
                StackTraceElement element = monitorInfo.getLockedStackFrame();
                String expectedMethodName = element.getClassName() + '.' + element.getMethodName();
                verify(expectedMethodName.equals(methodName), "Unexpected method name in " + ThreadUtils.strMonitorInfo(monitorInfo) + " expected: " + methodName);
        }

        /**
         * Check that monitor info for all given method names and locks is present.
         *
         * @param monitorInfo array of monitor info to check
         * @param lockMap map with method names as keys and locks as values
         */
        protected void checkMonitorInfo(MonitorInfo[] monitorInfos, Map<String, Object[]> lockMap) {
                try {
                        if (lockMap == null || !lockedMonitorsAvailable) {
                                verify(monitorInfos.length == 0, "Unexpected MonitorInfo[] objects: " + ThreadUtils.strMonitorInfoArr(monitorInfos));
                        } else {
                                int n = 0;
                                // Check that each entry in the map has corresponding monitorInfo
                                for (Map.Entry<String, Object[]> entry : lockMap.entrySet()) {
                                        String methodName = entry.getKey();
                                        Object[] locks = entry.getValue();
                                        n += locks.length;
                                        for (Object lock : locks)
                                                checkMonitorInfo(monitorInfos, methodName, lock);
                                }
                                // Check that each monitorInfo entry corresponds to entry in lockMap
                                for (MonitorInfo monitorInfo : monitorInfos) {
                                        StackTraceElement element = monitorInfo.getLockedStackFrame();
                                        if (element == null)
                                                continue;
                                        Object[] locks = lockMap.get(element.getMethodName());
                                        checkMonitorInfo(monitorInfo, element.getMethodName(), locks);
                                }
                                verify(n == monitorInfos.length, "Unexpected monitor info array length: " + monitorInfos.length + " expected: " + n);
                        }
                } catch (TestFailure t) {
                        log.info("Expected monitor info for locks:");
                        for (Map.Entry<String, Object[]> entry : lockMap.entrySet()) {
                                for (Object lock : entry.getValue()) {
                                        String s = "";
                                        s +=  "methodName: " + entry.getKey();
                                        s += " className: " + lock.getClass().getName();
                                        s += " identityHashCode: " + System.identityHashCode(lock);
                                        log.info(s);
                                }
                        }
                        throw t;
                }
        }

        /**
         * Check that monitor info for given method name and lock is present.
         *
         * @param monitorInfos monitor info array
         * @param methodName method name
         * @param lock lock object
         */
        protected void checkMonitorInfo(MonitorInfo[] monitorInfos, String methodName, Object lock) {
                String className = lock.getClass().getName();
                int hashCode = System.identityHashCode(lock);
                for (MonitorInfo monitorInfo : monitorInfos) {
                        if (className.equals(monitorInfo.getClassName()) &&
                            hashCode == monitorInfo.getIdentityHashCode()) {
                                if (monitorInfo.getLockedStackFrame() == null)
                                        return;
                                verify(methodName.equals(monitorInfo.getLockedStackFrame().getMethodName()), "Invalid method name: " + monitorInfo.getLockedStackFrame().getMethodName() + " expected: " + methodName);
                                return;
                        }
                }
                throw new TestFailure("Expected monitor not found: methodName: " + methodName + " lock: " + lock);
        }

        /**
         * Check that monitor info for given method name corresponds to one of locks.
         *
         * @param monitorInfo monitor info
         * @param methodName method name
         * @param locks lock array
         */
        protected void checkMonitorInfo(MonitorInfo monitorInfo, String methodName, Object[] locks) {
                for (Object lock : locks) {
                        String className = lock.getClass().getName();
                        int hashCode = System.identityHashCode(lock);
                        if (className.equals(monitorInfo.getClassName()) &&
                            hashCode == monitorInfo.getIdentityHashCode() &&
                            methodName.equals(monitorInfo.getLockedStackFrame().getMethodName()))
                                return;
                }
                throw new TestFailure("Lock for MonitorInfo not found: " + ThreadUtils.strMonitorInfo(monitorInfo));
        }

        /**
         * Check that lock info corresponds to given locks.
         *
         * We can only check number of items here.
         *
         * @param lockInfos lock info array
         * @param lockMap lock map
         */
        protected void checkSynchronizers(LockInfo[] lockInfos, Map<String, Lock[]> lockMap) {
                if (lockMap == null || !lockedSynchronizersAvailable)
                        verify(lockInfos.length == 0, "Unexpected LockInfo[] objects: " + ThreadUtils.strLockInfoArr(lockInfos));
                else {
                        // Only check length
                        int n = 0;
                        for (Map.Entry<String, Lock[]> entry : lockMap.entrySet()) {
                                Lock[] locks = entry.getValue();
                                n += locks.length;
                        }
                        verify(lockInfos.length == n, "Unexpected LockInfo[] length: " + lockInfos.length + " expected: " + n);
                }
        }

        /**
         * Obtain full method name for given stack trace element.
         *
         * @param element stack trace element
         * @return full method name, i.e. className.methodName
         */
        protected String getMethodName(StackTraceElement element) {
                return element.getClassName() + '.' + element.getMethodName();
        }

        /**
         * Verify condition and throw TestFailure if it does not hold.
         *
         * @param condition boolean condition
         * @param message TestFailure message
         */
        protected void verify(boolean condition, String message) {
                if (!condition)
                        throw new TestFailure(message + " in: " + this);
        }

        public final void setLog(Log log) {
                this.log = log;
        }
}
