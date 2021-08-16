/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * Utility class for ThreadMXBean tests
 */
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class Utils {
    private static final ThreadMXBean tm = ManagementFactory.getThreadMXBean();
    private static final int MAX_RETRY = 200;

    public static boolean waitForBlockWaitingState(Thread t) {
        // wait for the thread to transition to the expected state
        int retryCount=0;
        while (t.getState() == Thread.State.RUNNABLE && retryCount < MAX_RETRY) {
            goSleep(100);
            retryCount++;
        }
        return (t.getState() != Thread.State.RUNNABLE);
    }

    public static boolean waitForThreadState(Thread t, Thread.State expected) {
        // wait for the thread to transition to the expected state
        int retryCount=0;
        while (t.getState() != expected && retryCount < MAX_RETRY) {
            goSleep(100);
            retryCount++;
        }
        return (t.getState() == expected);
    }

    public static void checkThreadState(Thread t, Thread.State expected) {
        waitForThreadState(t, expected);

        Thread.State state = tm.getThreadInfo(t.getId()).getThreadState();
        if (state == null) {
            throw new RuntimeException(t.getName() + " expected to have " +
                expected + " but got null.");
        }
        if (state != expected) {
            t.dumpStack();
            throw new RuntimeException(t.getName() +
                 " expected to have " + expected + " but got " + state);
        }
    }

    public static void goSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("TEST FAILED: Unexpected exception.");
            throw new RuntimeException(e);
        }
    }

}
