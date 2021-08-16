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

/**
 * RecursiveMonitoringThread is a thread that recursively executes some
 * methods and runs method runInside inside. The method may be implemented
 * by subclasses. The types of methods are determined by recursionType,
 * which may be JAVA (only java methods), NATIVE (only native
 * methods) and MIXED (java and native methods).
 */
public abstract class RecursiveMonitoringThread extends MonitoringThread {
        private RunType recursionType;
        private int maxDepth;
        private static final String[] expectedMethodsJava = {
                "nsk.monitoring.share.thread.RecursiveMonitoringThread.recursiveMethod",
                "nsk.monitoring.share.thread.RecursiveMonitoringThread.run"
        };
        private static final String[] expectedMethodsNative = {
                "nsk.monitoring.share.thread.RecursiveMonitoringThread.nativeRecursiveMethod",
                "nsk.monitoring.share.thread.RecursiveMonitoringThread.run"
        };
        private static final String[] expectedMethodsMixed = {
                "nsk.monitoring.share.thread.RecursiveMonitoringThread.nativeRecursiveMethod",
                "nsk.monitoring.share.thread.RecursiveMonitoringThread.recursiveMethod",
                "nsk.monitoring.share.thread.RecursiveMonitoringThread.run"
        };

        static {
                System.loadLibrary("RecursiveMonitoringThread");
        }

        public RecursiveMonitoringThread(Log log, RunType recursionType, int maxDepth) {
                super(log);
                this.recursionType = recursionType;
                this.maxDepth = maxDepth;
        }

        public void run() {
                switch (recursionType) {
                case JAVA:
                case MIXED:
                        recursiveMethod(maxDepth);
                        break;
                case NATIVE:
                        nativeRecursiveMethod(maxDepth, true);
                        break;
                }
        }

        protected void recursiveMethod(int currentDepth) {
                if (currentDepth-- > 0) {
                        switch (recursionType) {
                        case JAVA:
                                recursiveMethod(currentDepth);
                                break;
                        case MIXED:
                                nativeRecursiveMethod(currentDepth, false);
                                break;
                        }
                } else {
                        runInside();
                }
        }

        protected boolean isStackTraceElementExpected(StackTraceElement element) {
                if (super.isStackTraceElementExpected(element))
                        return true;
                switch (recursionType) {
                case JAVA:
                        return checkStackTraceElement(element, expectedMethodsJava);
                case NATIVE:
                        return checkStackTraceElement(element, expectedMethodsNative);
                case MIXED:
                        return checkStackTraceElement(element, expectedMethodsMixed);
                default:
                        throw new IllegalArgumentException("Unknown recursionType: " + recursionType);
                }
        }

        protected native void nativeRecursiveMethod(int currentDepth, boolean pureNative);

        protected abstract void runInside();
}
