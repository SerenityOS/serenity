/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6977677 8004928
 * @summary Deadlock between LogManager.<clinit> and Logger.getLogger()
 * @author  Daniel D. Daugherty
 * @modules java.base/sun.util.logging
 *          java.logging
 * @compile -XDignore.symbol.file LoggingDeadlock4.java
 * @run main/othervm/timeout=15 LoggingDeadlock4
 */

import java.util.concurrent.CountDownLatch;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingDeadlock4 {
    private static CountDownLatch barrier      = new CountDownLatch(1);
    private static CountDownLatch lmIsRunning  = new CountDownLatch(1);
    private static CountDownLatch logIsRunning = new CountDownLatch(1);

    // Create a sun.util.logging.PlatformLogger$JavaLogger object
    // that has to be redirected when the LogManager class
    // is initialized. This can cause a deadlock between
    // LogManager.<clinit> and Logger.getLogger().
    private static final sun.util.logging.PlatformLogger log =
        sun.util.logging.PlatformLogger.getLogger("java.util.logging");

    public static void main(String[] args) {
        System.out.println("main: LoggingDeadlock4 is starting.");

        Thread lmThread = new Thread("LogManagerThread") {
            public void run() {
                // let main know LogManagerThread is running
                lmIsRunning.countDown();

                System.out.println(Thread.currentThread().getName()
                    + ": is running.");

                try {
                    barrier.await();  // wait for race to start
                } catch (InterruptedException e) {
                }

                LogManager.getLogManager();
            }
        };
        lmThread.start();

        Thread logThread = new Thread("LoggerThread") {
            public void run() {
                // let main know LoggerThread is running
                logIsRunning.countDown();

                System.out.println(Thread.currentThread().getName()
                    + ": is running.");

                try {
                    barrier.await();  // wait for race to start
                } catch (InterruptedException e) {
                }

                Logger.getLogger("foo logger");
            }
        };
        logThread.start();

        try {
            // wait for LogManagerThread and LoggerThread to get going
            lmIsRunning.await();
            logIsRunning.await();
        } catch (InterruptedException e) {
        }

        barrier.countDown();  // start the race

        try {
            lmThread.join();
            logThread.join();
        } catch (InterruptedException ie) {
        }

        System.out.println("main: LoggingDeadlock4 is done.");
    }
}
