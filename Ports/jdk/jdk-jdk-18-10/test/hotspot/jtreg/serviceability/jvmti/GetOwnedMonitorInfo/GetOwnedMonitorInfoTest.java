/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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


/**
 * @test
 * @bug 8185164
 * @summary Checks that a contended monitor does not show up in the list of owned monitors
 * @requires vm.jvmti
 * @compile GetOwnedMonitorInfoTest.java
 * @run main/othervm/native -agentlib:GetOwnedMonitorInfoTest GetOwnedMonitorInfoTest
 */

import java.io.PrintStream;

public class GetOwnedMonitorInfoTest {

    static {
        try {
            System.loadLibrary("GetOwnedMonitorInfoTest");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load GetOwnedMonitorInfoTest library");
            System.err.println("java.library.path: "
                               + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    private static native int check();
    private static native boolean hasEventPosted();

    public static void main(String[] args) throws Exception {
        final GetOwnedMonitorInfoTest lock = new GetOwnedMonitorInfoTest();

        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("Thread in sync section: "
                                   + Thread.currentThread().getName());
            }
        });

        // Make sure t1 contends on the monitor.
        synchronized (lock) {
            System.out.println("Main starting worker thread.");
            t1.start();

            // Wait for the MonitorContendedEnter event
            while (!hasEventPosted()) {
                System.out.println("Main waiting for event.");
                Thread.sleep(100);
            }
        }

        t1.join();

        if (check() != 0) {
            throw new RuntimeException("FAILED status returned from the agent");
        }
    }
}
