/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.unit.timers;

import java.io.PrintStream;
import java.util.*;

public class JvmtiTest {

    final static int JCK_STATUS_BASE = 95;
    final static int THREADS_LIMIT = 5;
    final static String NAME_PREFIX = "JvmtiTest-";
    static Object counterLock = new Object();
    static int counter = 0;
    static Object finalLock = new Object();

    static {
        try {
            System.loadLibrary("timers");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load timers library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int GetResult();
    native static void RegisterCompletedThread(Thread thread,
        int threadNumber, int iterationCount);
    native static void Analyze();

    static volatile int thrCount = 0;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        TestThread t[] = new TestThread[THREADS_LIMIT+1];

        synchronized(counterLock) {
            for (int i=1; i <= THREADS_LIMIT; i++) {
                t[i] = new TestThread(NAME_PREFIX + i, i, i * 100);
                t[i].start();
            }

            try {
                while (counter < THREADS_LIMIT) {
                    counterLock.wait();
                }
            } catch (InterruptedException exc) {
                throw new Error("Unexpected: " + exc);
            }
        }

        // all thread waiting to exit now
        Analyze();
        synchronized(finalLock) {
            // let them finish
            finalLock.notifyAll();
        }

        try {
            for (int i=1; i <= THREADS_LIMIT; i++) {
                t[i].join();
            }
        } catch (InterruptedException e) {
            throw new Error("Unexpected: " + e);
        }
        return GetResult();
    }

    static void completeThread() {
        try {
            synchronized(finalLock) {
                synchronized(counterLock) {
                    ++counter;
                    counterLock.notifyAll();
                }
                finalLock.wait();
            }
        } catch (InterruptedException exc) {
            throw new Error("Unexpected: " + exc);
        }
    }

    static class TestThread extends Thread {
        int threadNumber;
        int iterations;

        public TestThread(String name, int threadNumber, int iterations) {
            super(name);
            this.threadNumber = threadNumber;
            this.iterations = iterations;
        }

        public void run() {
            for (int i = iterations; i > 0; --i) {
                List<Integer> list = new ArrayList<Integer>();
                for (int j = 10000; j > 0; --j) {
                    list.add(Integer.valueOf(j));
                }
                Collections.sort(list);
            }
            JvmtiTest.RegisterCompletedThread(this, threadNumber, iterations);
            JvmtiTest.completeThread();
        }
    }
}
