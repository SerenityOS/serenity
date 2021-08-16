/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CountDownLatch;

import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

public class FinalizationRunner {
    public static final String FAILED = "Failed";
    public static final String PASSED = "Passed";

    static volatile boolean wasFinalized = false;
    private static final CountDownLatch finRunLatch = new CountDownLatch(1);
    private static final CountDownLatch finBlockLatch = new CountDownLatch(1);

    static class MyObject {
        @Override
        protected void finalize() {
            if (Thread.currentThread().getName().equals("Finalizer")) {
                try {
                    System.out.println("inside the regular finalizer thread; blocking");
                    // 'regular' finalizer thread is ready to be effectively blocked -
                    //    we can continue with the GC.run_finalization test
                    finRunLatch.countDown();
                    // prevent the 'regular' finalizer from finalizing this instance
                    // until the GC.run_finalization has had its chance to do so
                    finBlockLatch.await();
                } catch (InterruptedException e) {
                }
            } else {
                if (Thread.currentThread().getName().equals("Secondary finalizer")) {
                    System.out.println("finalizing the test instance");
                    // finalizing on behalf of GC.run_finalization -
                    //   unblock the 'regular' finalizer and the test main method
                    wasFinalized = true;
                } else {
                    fail("Unexpected finalizer thread name: " +
                            Thread.currentThread().getName());
                }
                finBlockLatch.countDown();
            }
        }
    }

    // this instance will be used to provoke the regular finalization
    // so the finalizer thread can be blocked for the duration of
    // GC.run_finalization test
    public static MyObject o1;

    // this instance will be used to perform the GC.run_finalization test
    public static MyObject o2;

    private static void run(CommandExecutor executor) {
        o2 = new MyObject();
        o2 = null;
        System.out.println("running GC.run_finalization");
        System.gc();
        executor.execute("GC.run_finalization");

        System.out.println("Waiting for finalization");

        try {
            finBlockLatch.await();
            if (wasFinalized) {
                System.out.println(PASSED + ": Object was finalized");
            } else {
                fail("Object was not finalized");
            }
        } catch (InterruptedException e) {
            fail("Interrupted while waiting for finalization", e);
        }
    }

    public static void main(String ... args) {
        System.out.println("\n=== FinalizationRunner");
        try {
            blockFinalizerThread();

            Runtime.getRuntime().addShutdownHook(new Thread(()->{
                run(new JMXExecutor());
            }));
        } catch (InterruptedException e) {
            fail("Interrupted while trying to block the finalizer thread", e);
        }
    }

    private static void blockFinalizerThread() throws InterruptedException {
        System.out.println("trying to block the finalizer thread");
        o1 = new MyObject();
        o1 = null;
        System.gc();
        finRunLatch.await();
    }

    private static void fail(String msg, Exception e) {
        fail(msg);
        e.printStackTrace(System.err);
    }

    private static void fail(String msg) {
        System.err.println(FAILED + ": " + msg);
    }
}
