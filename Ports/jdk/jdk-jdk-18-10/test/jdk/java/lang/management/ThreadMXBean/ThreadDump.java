/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Thread Dump utility class for printing
 * @author  Mandy Chung
 */

import java.lang.management.*;
import java.util.*;

public class ThreadDump {
    private static final String INDENT = "   ";

    public static void printThreadInfo(ThreadInfo ti) {
        StringBuilder sb = new StringBuilder("\"" + ti.getThreadName() + "\"" +
                                             (ti.isDaemon() ? " daemon" : "") +
                                             " Id=" + ti.getThreadId() +
                                             " in " + ti.getThreadState());
        if (ti.getLockName() != null) {
            sb.append(" on lock=" + ti.getLockName());
        }
        if (ti.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (ti.isInNative()) {
            sb.append(" (running in native)");
        }
        System.out.println(sb.toString());
        if (ti.getLockOwnerName() != null) {
             System.out.println(INDENT + " owned by " + ti.getLockOwnerName() +
                                " Id=" + ti.getLockOwnerId());
        }
        StackTraceElement[] stacktrace = ti.getStackTrace();
        MonitorInfo[] monitors = ti.getLockedMonitors();
        for (int i = 0; i < stacktrace.length; i++) {
            StackTraceElement ste = stacktrace[i];
            System.out.println(INDENT + "at " + ste.toString());

            for (MonitorInfo mi : monitors) {
                if (mi.getLockedStackDepth() == i) {
                    System.out.println(INDENT + "  - locked " + mi);
                }
            }
        }
        System.out.println();
    }

    public static void printStack(StackTraceElement[] stack) {
        System.out.println(INDENT + "Stack: (length = " + stack.length + ")");
        for (int j = 0; j < stack.length; j++) {
            System.out.println(INDENT + INDENT + stack[j]);
        }
        System.out.println();
    }

    public static void dumpStacks() {
        // Get stack traces of all Threads
        Map m = Thread.getAllStackTraces();
        Set s = m.entrySet();
        Iterator iter = s.iterator();

        Map.Entry entry;
        while (iter.hasNext()) {
            entry = (Map.Entry) iter.next();
            Thread t = (Thread) entry.getKey();
            StackTraceElement[] stack = (StackTraceElement[]) entry.getValue();
            System.out.println(t);
            printStack(stack);
        }
    }

    public static void printLockInfo(LockInfo[] locks) {
       System.out.println(INDENT + "Locked synchronizers: count = " + locks.length);
       for (LockInfo li : locks) {
           System.out.println(INDENT + "  - " + li);
       }
    }

    static ThreadMXBean tmbean = ManagementFactory.getThreadMXBean();
    public static void threadDump() {
       System.out.println("Full Java thread dump");
       ThreadInfo[] tinfos = tmbean.dumpAllThreads(true, true);
       for (ThreadInfo ti : tinfos) {
           printThreadInfo(ti);
           LockInfo[] syncs = ti.getLockedSynchronizers();
           printLockInfo(syncs);
           System.out.println();
       }
    }

}
