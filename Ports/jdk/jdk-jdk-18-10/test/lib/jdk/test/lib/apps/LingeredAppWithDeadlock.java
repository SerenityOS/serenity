/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.test.lib.apps;

import java.util.concurrent.Phaser;

public class LingeredAppWithDeadlock extends LingeredApp {

    private static final Object Lock1 = new Object();
    private static final Object Lock2 = new Object();

    private static volatile int reachCount = 0;

    private static final Phaser p = new Phaser(2);

    private static class ThreadOne extends Thread {
        public void run() {
            // wait Lock2 is locked
            p.arriveAndAwaitAdvance();
            synchronized (Lock1) {
                // signal Lock1 is locked
                p.arriveAndAwaitAdvance();
                synchronized (Lock2) {
                    reachCount += 1;
                }
            }
        }
    }

    private static class ThreadTwo extends Thread {
        public void run() {
            synchronized (Lock2) {
                // signal Lock2 is locked
                p.arriveAndAwaitAdvance();
                // wait Lock1 is locked
                p.arriveAndAwaitAdvance();
                synchronized (Lock1) {
                    reachCount += 1;
                }
            }
        }
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("Lock file name is not specified");
            System.exit(7);
        }

        // Run two theads that should come to deadlock
        new ThreadOne().start();
        new ThreadTwo().start();

        if (reachCount > 0) {
            // Not able to deadlock, exiting
            System.exit(3);
        }

        LingeredApp.main(args);
    }
 }
