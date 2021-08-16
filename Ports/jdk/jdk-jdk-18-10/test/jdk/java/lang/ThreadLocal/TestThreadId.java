/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug     6434084
 * @summary Exercise ThreadLocal javadoc "demo" class ThreadId
 * @author  Pete Soper
 */

public final class TestThreadId extends Thread {

    // number of times to create threads and gather their ids
    private static final int ITERATIONCOUNT = 50;

    // Threads constructed per iteration. ITERATIONCOUNT=50 and
    // THREADCOUNT=50 takes about one second on a sun Blade 1000 (2x750mhz)
    private static final int THREADCOUNT = 50;

    // The thread local storage object for holding per-thread ids
    private static ThreadId id = new ThreadId();

    // Holds the per-thread so main method thread can collect it. JMM
    // guarantees this is valid after this thread joins main method thread.
    private int value;

    private synchronized int getIdValue() {
        return value;
    }

    // Each child thread just publishes its id value for validation
    public void run() {
        value = id.get();
    }

    public static void main(String args[]) throws Throwable {

        // holds true corresponding to a used id value
        boolean check[] = new boolean[THREADCOUNT*ITERATIONCOUNT];

        // the test threads
        TestThreadId u[] = new TestThreadId[THREADCOUNT];

        for (int i = 0; i < ITERATIONCOUNT; i++) {
            // Create and start the threads
            for (int t=0;t<THREADCOUNT;t++) {
                u[t] = new TestThreadId();
                u[t].start();
            }
            // Join with each thread and get/check its id
            for (int t=0;t<THREADCOUNT;t++) {
                try {
                    u[t].join();
                } catch (InterruptedException e) {
                     throw new RuntimeException(
                        "TestThreadId: Failed with unexpected exception" + e);
                }
                try {
                    if (check[u[t].getIdValue()]) {
                        throw new RuntimeException(
                            "TestThreadId: Failed with duplicated id: " +
                                u[t].getIdValue());
                    } else {
                        check[u[t].getIdValue()] = true;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(
                        "TestThreadId: Failed with unexpected id value" + e);
                }
            }
        }
    } // main
} // TestThreadId
