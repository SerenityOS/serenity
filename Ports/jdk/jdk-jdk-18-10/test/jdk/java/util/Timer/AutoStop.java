/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8263903
 * @requires vm.gc != "Epsilon"
 * @summary Discarding a Timer causes the Timer thread to stop.
 */

import java.util.Timer;
import java.util.TimerTask;
import java.lang.ref.Reference;

public class AutoStop {
    static final Object wakeup = new Object();
    static Thread tdThread = null;
    static volatile int counter = 0;
    static final int COUNTER_LIMIT = 10;

    public static void main(String[] args) throws Exception {
        Timer t = new Timer();

        // Run an event that records the timer thread.
        t.schedule(new TimerTask() {
                public void run() {
                    synchronized(wakeup) {
                        tdThread = Thread.currentThread();
                        wakeup.notify();
                    }
                }
            }, 0);

        // Wait for the thread to be accessible.
        try {
            synchronized(wakeup) {
                while (tdThread == null) {
                    wakeup.wait();
                }
            }
        } catch (InterruptedException e) {
        }

        // Schedule some events that increment the counter.
        for (int i = 0; i < COUNTER_LIMIT; ++i) {
            t.schedule(new TimerTask() {
                    public void run() {
                        ++counter;
                    }
                }, 100);
        }

        // Ensure the timer is accessible at least until here.
        Reference.reachabilityFence(t);
        t = null;               // Remove the reference to the timer.
        System.gc();            // Run GC to trigger cleanup.
        tdThread.join();        // Wait for thread to stop.
        int finalCounter = counter;
        if (finalCounter != COUNTER_LIMIT) {
            throw new RuntimeException("Unrun events: counter = " + finalCounter);
        }
    }
}

