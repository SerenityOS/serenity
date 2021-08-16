/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4176355 7181748
 * @summary Suspending a ThreadGroup that contains the current thread has
 *          unpredictable results.
 */

public class Suspend implements Runnable {

    private static volatile int count = 0;
    private static final ThreadGroup group = new ThreadGroup("");
    private static final Thread first = new Thread(group, new Suspend());
    private static final Thread second = new Thread(group, new Suspend());

    public void run() {
        while (true) {
            try {
                Thread.sleep(100);
                if (Thread.currentThread() == first) {
                    if (second.isAlive()) {
                        group.suspend();
                    }
                } else {
                    count++;
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Launch two threads as part of the same thread group
        first.start();
        second.start();

        // Wait for the thread group suspend to be issued
        while (!first.isAlive() || !second.isAlive()) {
            Thread.sleep(100);
        }
        Thread.sleep(1000);
        // Suppose, the thread group is now suspended

        count = 0;
        Thread.sleep(1000);

        // Increment of the count indicates that the second thread is still running
        boolean failed = (count > 1);
        first.stop();
        second.stop();
        if (failed) {
            throw new RuntimeException("Failure.");
        }
    }
}
