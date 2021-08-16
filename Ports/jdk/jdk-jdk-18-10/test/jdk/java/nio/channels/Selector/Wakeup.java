/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6405995
 * @summary Unit test for selector wakeup and interruption
 * @library .. /test/lib
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.CyclicBarrier;

public class Wakeup {

    static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException x) {
            x.printStackTrace();
        }
    }

    static class Sleeper extends TestThread {
        private static final long TIMEOUT = jdk.test.lib.Utils.adjustTimeout(20_000);

        // barrier is used to synchronize sleeper thread and checking
        // thread which is the main thread: when go() get to the end,
        // then start checking the sleeper's status.
        private static CyclicBarrier barrier = new CyclicBarrier(2);
        private static int wakeups = 0;
        private static int waits = 0;

        volatile boolean interruptBeforeSelect = false;
        volatile boolean started = false;
        volatile boolean wantInterrupt = false;
        volatile boolean closed = false;
        Object gate = new Object();

        Selector sel;

        Sleeper(Selector sel, boolean wantInterrupt, boolean interruptBeforeSelect) {
            super("Sleeper", System.err);
            this.sel = sel;
            this.wantInterrupt = wantInterrupt;
            this.interruptBeforeSelect = interruptBeforeSelect;
        }

        public void go() throws Exception {
            started = true;
            if (interruptBeforeSelect) {
                synchronized (gate) { }
            }
            wakeups++;
            System.err.println("Wakeup, selecting, " + wakeups);
            try {
                sel.select();
            } catch (ClosedSelectorException x) {
                closed = true;
            }
            boolean intr = Thread.currentThread().isInterrupted();
            System.err.println("Wakeup " + wakeups
                               + (closed ? " (closed)" : "")
                               + (intr ? " (intr)" : ""));
            if (closed)
                return;
            if (wantInterrupt) {
                while (!Thread.interrupted())
                    Thread.yield();
            }
            System.err.println("Wakeup, waiting, " + wakeups);
            barrier.await();
            System.err.println("Wakeup, wait successfully, " + wakeups);
        }

        void check(boolean close) throws Exception {
            waits++;
            System.err.println("waiting sleeper, " + waits);
            if (!close) {
                barrier.await();
                System.err.println("wait barrier successfully, " + waits);
            }
            if (finish(TIMEOUT) == 0)
                throw new Exception("Test failed");
            if (this.closed != close)
                throw new Exception("Selector was closed");
        }

        void check() throws Exception {
            check(false);
        }

        static Sleeper createSleeper(Selector sel, boolean wantInterrupt,
                boolean interruptBeforeSelect) throws Exception {
            if (!wantInterrupt && interruptBeforeSelect) {
                throw new RuntimeException("Wrong parameters!");
            }

            Sleeper sleeper = new Sleeper(sel, wantInterrupt, interruptBeforeSelect);

            if (interruptBeforeSelect) {
                synchronized(sleeper.gate) {
                    sleeper.start();
                    while (!sleeper.started)
                        sleep(50);
                    sleeper.interrupt();
                }
            } else {
                sleeper.start();
                while (!sleeper.started)
                    sleep(50);
                if (wantInterrupt) {
                    sleep(50);
                    sleeper.interrupt();
                }
            }
            return sleeper;
        }
    }

    static Sleeper newSleeper(Selector sel) throws Exception {
        return Sleeper.createSleeper(sel, false, false);
    }

    static Sleeper newSleeperWantInterrupt(Selector sel) throws Exception {
        return Sleeper.createSleeper(sel, true, false);
    }

    static Sleeper newSleeperWantInterruptBeforeSelect(Selector sel) throws Exception {
        return Sleeper.createSleeper(sel, true, true);
    }

    public static void main(String[] args) throws Exception {

        Selector sel = Selector.open();

        // Wakeup before select
        sel.wakeup();

        Sleeper sleeper = newSleeper(sel); // 1
        sleeper.check();

        for (int i = 2; i < 5; i++) {
            // Wakeup during select
            sleeper = newSleeper(sel);
            sel.wakeup();
            sleeper.check();         // 2 .. 4
        }

        // Double wakeup
        sel.wakeup();
        sel.wakeup();
        sleeper = newSleeper(sel);
        sleeper.check();            // 5

        // Interrupt
        sleeper = newSleeperWantInterrupt(sel);
        sleeper.check();            // 6

        // Interrupt before select
        sleeper = newSleeperWantInterruptBeforeSelect(sel);
        sleeper.check();            // 7

        // Close during select
        sleeper = newSleeper(sel);
        sel.close();
        sleeper.check();           // 8

        sleeper = newSleeper(sel);
        sleeper.check(true);
    }

}
