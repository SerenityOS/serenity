/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8240588
 * @summary Use the WhiteBox API to ensure that we can safely access the
 *          threadObj oop of a JavaThread during termination, after it has
 *          removed itself from the main ThreadsList.
 * @library /testlibrary /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @comment run with a small heap, but we need at least 11M for ZGC with JFR
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xmx11m -XX:-DisableExplicitGC ThreadObjAccessAtExit
 */

import sun.hotspot.WhiteBox;

// We need to coordinate the actions of the target thread, the GC thread
// and the WB main test method. To simplify things we use the target
// thread's priority field to indicate the state of the test as follows:
// - Start the target thread with priority 5 (NORM_PRIORITY), it will run
//   until its priority is boosted by 1
// - Start the GC thread, which will spin until the target thread's priority
//   has been boosted by 2
// - Call into the WB test method and:
//   - Grab a ThreadsListHandle and ensure it contains the target
//   - Increase the target priority by one so it will terminate
//   - Wait until we see the JavaThread has terminated
//   - Increase the target thread priority by one again to release the GC thread
//   - Check the original Thread oop with the target->threadObj to see if they
//     are the same (looping a few times to improve the chances of GC having
//     time to move the underlying object).
//   - If the oop has changed throw an exception

public class ThreadObjAccessAtExit {

    static class GCThread extends Thread {

        // Allocate a moderate-size array
        static Object[] arr = new Object[64*1024];

        // Wait till we see the main thread is ready then clear the storage
        // we consumed at class initialization and run an explicit GC cycle.
        // This is sufficient (via experimentation) to cause the oop to be
        // relocated.
        public void run() {
            System.out.println("GCThread waiting ... ");
            try {
                while (target.getPriority() != Thread.NORM_PRIORITY + 2) {
                    Thread.sleep(10);
                }
            }
            catch(InterruptedException ie) {
                throw new RuntimeException(ie);
            }

            System.out.println("GCThread running ... ");

            arr = null;
            System.gc();
        }
    }

    static Thread target;  // for easy access from GCThread

    public static void main(String[] args) throws Throwable {
        WhiteBox wb = WhiteBox.getWhiteBox();

        // Create the GCThread, which performs the initial large
        // allocation that will later be released when it runs.
        GCThread g = new GCThread();
        g.setName("GCThread");

        // Create the target thread (hopefully in a region that will cause
        // it to move when GC happens later).
        target = new Thread("Target") {
                public void run() {
                    Thread current = Thread.currentThread();
                    // Wait until we are told to terminate by the main thread
                    try {
                        while (current.getPriority() != Thread.NORM_PRIORITY + 1) {
                            Thread.sleep(10);
                        }
                    }
                    catch(InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                    System.out.println("Target is terminating");
                }
            };
        g.start();
        target.setPriority(Thread.NORM_PRIORITY); // just to be explicit
        target.start();
        wb.checkThreadObjOfTerminatingThread(target);
    }
}
