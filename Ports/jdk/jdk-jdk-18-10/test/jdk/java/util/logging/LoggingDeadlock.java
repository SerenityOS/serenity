/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4994705
 *
 * @summary deadlock in LogManager
 * @author  Serguei Spitsyn / SAP
 *
 * @library  /test/lib
 * @build    LoggingDeadlock
 * @run  main/timeout=15 LoggingDeadlock
 * @key randomness
 */

/*
 *
 * There can be a deadlock between two class initializations.
 * It happens if the LogManager.<clinit> and the Logger.<clinit>
 * are invoked concurrently on two different threads.
 * There is a cyclic dependence between the two static initializers:
 *   1. LogManager.<clinit> instantiate the class RootLogger which
 *      is a subclass of the Logger class.
 *      It requires the Logger class initialization to complete.
 *   2. Logger.<clinit> initializes the field "global", so it
 *      it makes a call: Logger.getLogger("global").
 *      Subsequently the LogManager static method getLogManager()
 *      is called which requires the LogManager class
 *      initialization to complete.
 * This cyclic dependence causes a deadlock, so two class
 * initializations are waiting for each other.
 * This is a regression test for this bug.
 */


import java.util.Random;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import jdk.test.lib.RandomFactory;

public class LoggingDeadlock {

    private static int preventLoopElision;
    private static final Random random = RandomFactory.getRandom();

    public static void randomDelay() {
        int runs = random.nextInt(1000000);
        int c = 0;

        for (int i = 0; i < runs; ++i) {
            c = c + i;
        }
        preventLoopElision = c;
    }

    public static void main(String[] args) throws InterruptedException{
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                randomDelay();
                // Trigger Logger.<clinit>
                Logger.getAnonymousLogger();
            }
        });

        Thread t2 = new Thread(new Runnable() {
            public void run() {
                randomDelay();
                // Trigger LogManager.<clinit>
                LogManager.getLogManager();
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();
        System.out.println("\nTest passed");
    }
}
