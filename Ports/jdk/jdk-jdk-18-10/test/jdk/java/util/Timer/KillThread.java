/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4288198
 * @summary Killing a Timer thread causes the Timer to fail silently on
 *          subsequent use.
 */

import java.util.*;

public class KillThread {
    static volatile Thread tdThread;
    public static void main (String[] args) throws Exception  {
        Timer t = new Timer();

        // Start a mean event that kills the timer thread
        t.schedule(new TimerTask() {
            public void run() {
                tdThread = Thread.currentThread();
                throw new ThreadDeath();
            }
        }, 0);

        // Wait for mean event to do the deed and thread to die.
        try {
            do {
                Thread.sleep(100);
            } while(tdThread == null);
        } catch(InterruptedException e) {
        }
        tdThread.join();

        // Try to start another event
        try {
            // Timer thread is dead now
            t.schedule(new TimerTask() {
                public void run() {
                }
            }, 0);
            throw new Exception("We failed silently");
        } catch(IllegalStateException e) {
            // Killing the Timer thread is equivalent to cancelling the Timer
        }
    }
}
