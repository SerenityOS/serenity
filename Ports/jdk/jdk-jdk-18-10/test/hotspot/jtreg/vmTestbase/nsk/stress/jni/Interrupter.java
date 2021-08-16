/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.stress.jni;

class Interrupter extends Thread {
    public Interrupter(
            Thread[] alloc,
            Synchronizer[] s
    ) {
        targets = alloc;
        sync = s;
        if (DEBUG) System.out.println("Interrupter created.");
    }

    public void run() {
        while (!done) {
            synchronized (sync[2]) {
                targets[turn].interrupt();
            }
            turn = (turn + 1) % targets.length;
            try {
                sleep(interval);
            } catch (InterruptedException e) {
            }
        }
        if (DEBUG) System.out.println("Interrupter::run(): done");
    }

    public void setInterval(int i) {
        interval = i;
    }

    public int getInterval() {
        return interval;
    }

    public void halt() {
        done = true;
    }

    Thread[] targets;
    Synchronizer[] sync;
    private int turn = 0;
    private int interval = 1000;
    private boolean done = false;
    final private static boolean DEBUG = false;
}
