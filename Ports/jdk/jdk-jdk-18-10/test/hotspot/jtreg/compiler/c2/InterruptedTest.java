/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6772683
 * @summary Thread.isInterrupted() fails to return true on multiprocessor PC
 *
 * @run main/othervm compiler.c2.InterruptedTest 100
 */

package compiler.c2;

public class InterruptedTest {

    public static void main(String[] args) throws Exception {
        /* The value of the threshold determines for how many seconds
         * the main thread must wait for the worker thread. On highly
         * loaded systems it can take a while until the worker thread
         * obtains CPU time and is able to check if it was interrupted
         * by the main thread. The higher the threshold the likelier
         * the worker thread can check if it was interrupted (that is
         * required for successul test execution).
         */
        int threshold = 100;

        if (args.length != 1) {
            System.out.println("Incorrect number of arguments");
            System.exit(1);
        }

        try {
            threshold = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid argument format");
            System.exit(1);
        }

        if (threshold < 1) {
            System.out.println("Threshold must be at least 1");
            System.exit(1);
        }

        Thread workerThread = new Thread("worker") {
            public void run() {
                System.out.println("Worker thread: running...");
                while (!Thread.currentThread().isInterrupted()) {
                }
                System.out.println("Worker thread: bye");
            }
        };
        System.out.println("Main thread: starts a worker thread...");
        workerThread.start();
        System.out.println("Main thread: waits 5 seconds after starting the worker thread");
        workerThread.join(5000); // Wait 5 sec to let run() method to be compiled

        int ntries = 0;
        while (workerThread.isAlive() && ntries < threshold) {
            System.out.println("Main thread: interrupts the worker thread...");
            workerThread.interrupt();
            if (workerThread.isInterrupted()) {
                System.out.println("Main thread: worker thread is interrupted");
            }
            ntries++;
            System.out.println("Main thread: waits 1 second for the worker thread to die...");
            workerThread.join(1000); // Wait 1 sec and try again
        }

        if (ntries == threshold) {
          System.out.println("Main thread: the worker thread did not die after " +
                             ntries + " seconds have elapsed");
          System.exit(97);
        }

        System.out.println("Main thread: bye");
    }
}
