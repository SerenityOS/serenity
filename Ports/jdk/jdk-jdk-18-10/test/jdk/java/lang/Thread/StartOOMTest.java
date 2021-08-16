/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * This test is relatively useful for verifying 6379235, but
 * is too resource intensive, especially on 64 bit systems,
 * to be run automatically, see 6721694.
 *
 * When run it should be typically be run with the server vm
 * and a relatively small java heap, and a large stack size
 * ( to provoke the OOM quicker ).
 *    java -server -Xmx32m -Xms32m -Xss256m StartOOMTest
 */

import java.util.*;

public class StartOOMTest
{
    public static void main(String[] args) throws Throwable {
        Runnable r = new SleepRunnable();
        ThreadGroup tg = new ThreadGroup("buggy");
        List<Thread> threads = new ArrayList<Thread>();
        Thread failedThread;
        int i = 0;
        for (i = 0; ; i++) {
            Thread t = new Thread(tg, r);
            try {
                t.start();
                threads.add(t);
            } catch (Throwable x) {
                failedThread = t;
                System.out.println(x);
                System.out.println(i);
                break;
            }
        }

        int j = 0;
        for (Thread t : threads)
            t.interrupt();

        while (tg.activeCount() > i/2)
            Thread.yield();
        failedThread.start();
        failedThread.interrupt();

        for (Thread t : threads)
            t.join();
        failedThread.join();

        try {
            Thread.sleep(1000);
        } catch (Throwable ignore) {
        }

        int activeCount = tg.activeCount();
        System.out.println("activeCount = " + activeCount);

        if (activeCount > 0) {
            throw new RuntimeException("Failed: there  should be no active Threads in the group");
        }
    }

    static class SleepRunnable implements Runnable
    {
        public void run() {
            try {
                Thread.sleep(60*1000);
            } catch (Throwable t) {
            }
        }
    }
}
