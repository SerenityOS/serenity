/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.unit.StackTrace;

import java.io.PrintStream;

public class JvmtiTest {

    final static int JCK_STATUS_BASE = 95;
    final static int THREADS_LIMIT = 200;
    final static String NAME_PREFIX = "JvmtiTest-";
    static int fail_id = 0;

    native static int GetResult();
    native static void CreateRawMonitor(int i);
    native static void RawMonitorEnter(int i);
    native static void RawMonitorExit(int i);
    native static void RawMonitorWait(int i);
    native static void GetStackTrace(TestThread t);
    native static int GetFrameCount(TestThread t);


    static volatile int thrCount = 0;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        CreateRawMonitor(0);
        CreateRawMonitor(4);
        TestThread t[] = new TestThread[THREADS_LIMIT];

        RawMonitorEnter(4);

        for (int i=0; i < THREADS_LIMIT; i++) {
            t[i] = new TestThread(NAME_PREFIX + thrCount);
            t[i].start();
        }


        for (int i=0; i < THREADS_LIMIT-1; i++) {
            GetStackTrace(t[i]);
            GetFrameCount(t[i]);
        }

        RawMonitorExit(4);

        try {
          for (int i=0; i < THREADS_LIMIT; i++) {
              t[i].join();
          }

        } catch (InterruptedException e) {
            throw new Error("Unexpected: " + e);
        }
        return GetResult() + fail_id;
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
        public void foo(int i) {
            i++;
            if ( i < 10 ) {
                foo(i);
            } else {
                RawMonitorEnter(4);
                RawMonitorExit(4);
            }

        }
    }
}
