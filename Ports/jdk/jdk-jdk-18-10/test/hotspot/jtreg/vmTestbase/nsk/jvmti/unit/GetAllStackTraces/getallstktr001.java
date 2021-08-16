/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.unit.GetAllStackTraces;

import java.io.PrintStream;
import nsk.share.Consts;

public class getallstktr001 {

    final static int THREADS_LIMIT   = 40;
    final static String NAME_PREFIX  = "getallstktr001-";
    final static String THREAD_WAIT_METHOD_NAME = "RawMonitorEnter";

    native static int  GetResult();
    native static void CreateRawMonitor();
    native static void RawMonitorEnter();
    native static void RawMonitorExit();
    native static void GetAllStackTraces();
    native static void GetThreadsInfo();
    native static void GetThreadListStackTraces();
    native static void ForceGC();
    native static void CompareStackTraces();
    native static void DeallocateBuffers();

    static volatile int thrCount = 0;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        CreateRawMonitor();
        TestThread thr[] = new TestThread[THREADS_LIMIT];

        RawMonitorEnter();

        for (int i=0; i < THREADS_LIMIT; i++) {
            thr[i] = new TestThread(NAME_PREFIX + i);
            thr[i].start();
        }

        // Wait until all hreads reach the expected state.
        TestThread.waitAll();

        // Now some of them have increased the counter,
        // but may haven't reached RawMonitorEnter yet
        // Wait for such threads to enter raw monitor
        int tries = 100;
        for ( ;; ) {
            try { Thread.sleep(500); } catch ( InterruptedException e ) {}

            boolean allThreadsReady = true;
            for ( Thread t : thr ) {
                StackTraceElement[] stack = t.getStackTrace();
                if ( stack.length == 0 ) {
                    System.out.println("Thread " + t.getName() + " has an empty stack. Seems strange.");
                    continue;
                }

                if ( ! stack[0].getMethodName().equals(THREAD_WAIT_METHOD_NAME) ) {
                    System.out.println("Thread " + t.getName() + " hasn't reached the raw monitor yet:");
                    for ( StackTraceElement s : stack ) {
                        System.out.println("\t" + s);
                    }
                    allThreadsReady = false;
                    break;
                }
            }

            if ( allThreadsReady )
                break;

            if ( --tries <= 0 )
                throw new Error("Timed out waiting for all threads reaching the raw monitor");
        }

        GetAllStackTraces();
        GetThreadsInfo();
        GetThreadListStackTraces();
        ForceGC();
        CompareStackTraces();
        DeallocateBuffers();

        RawMonitorExit();

        try {
          for (int i=0; i < THREADS_LIMIT; i++) {
              thr[i].join();
          }

        } catch (InterruptedException e) {
            throw new Error("Unexpected: " + e);
        }
        return GetResult();
    }

    static class TestThread extends Thread {
        static int counter=0;
        int ind;
        public TestThread(String name) {
            super(name);
        }
        public void run() {
            thrCount++;
            foo(0);
        }
        private static synchronized void incrCounter() {
           if (++counter == THREADS_LIMIT) {
               TestThread.class.notify();
           }
        }

        public static void waitAll() {
           synchronized(TestThread.class) {
               while (counter < THREADS_LIMIT) {
                   try {
                       TestThread.class.wait();
                   } catch (InterruptedException ex) {
                   }
               }
           }
        }

        public void foo(int i) {
            i++;
            if ( i < 10 ) {
                foo(i);
            } else {
                incrCounter();
                RawMonitorEnter();
                RawMonitorExit();
            }

        }
    }
}
