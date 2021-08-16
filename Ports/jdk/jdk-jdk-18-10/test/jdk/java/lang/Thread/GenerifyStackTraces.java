/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4919105 8004177
 * @summary Generified basic unit test of Thread.getAllStackTraces()
 * @author  Mandy Chung
 */

import java.util.*;

public class GenerifyStackTraces {

    private static Object go = new Object();
    private static String[] methodNames = {"run", "A", "B", "C", "Done"};
    private static int DONE_DEPTH = 5;
    private static boolean testFailed = false;

    private static Thread one;
    private static boolean trace = false;
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("trace")) {
            trace = true;
        }

        one = new ThreadOne();
        one.start();

        DumpThread dt = new DumpThread();
        dt.start();

        try {
            one.join();
        } finally {
            dt.shutdown();
        }

        if (testFailed) {
            throw new RuntimeException("Test Failed.");
        }
    }

    static class DumpThread extends Thread {
        private volatile boolean finished = false;

        public void run() {
            int depth = 2;
            while (!finished) {
                // At each iterator, wait until ThreadOne blocks
                // to wait for thread dump.
                // Then dump stack trace and notify ThreadOne to continue.
                try {
                    sleep(2000);
                    dumpStacks(depth);
                    depth++;
                    finishDump();
                } catch (Exception e) {
                    e.printStackTrace();
                    testFailed = true;
                }
            }
        }

        public void shutdown() throws InterruptedException {
            finished = true;
            this.join();
        }
    }

    static class ThreadOne extends Thread {
        public void run() {
            A();
        }
        private void A() {
            waitForDump();
            B();
        }
        private void B() {
            waitForDump();
            C();
        }
        private void C() {
            waitForDump();
            Done();
        }
        private void Done() {
            waitForDump();

            // Get stack trace of current thread
            StackTraceElement[] stack = getStackTrace();
            try {
                checkStack(this, stack, DONE_DEPTH);
            } catch (Exception e) {
                e.printStackTrace();
                testFailed = true;
            }
        }

    }


    private static void waitForDump() {
        synchronized(go) {
            try {
               go.wait();
            } catch (Exception e) {
               throw new RuntimeException("Unexpected exception" + e);
            }
        }
    }

    private static void finishDump() {
        synchronized(go) {
            try {
               go.notifyAll();
            } catch (Exception e) {
               throw new RuntimeException("Unexpected exception" + e);
            }
        }
    }

    public static void dumpStacks(int depth) throws Exception {
        // Get stack trace of another thread
        StackTraceElement[] stack = one.getStackTrace();
        checkStack(one, stack, depth);


        // Get stack traces of all Threads
        for (Map.Entry<Thread, StackTraceElement[]> entry :
                 Thread.getAllStackTraces().entrySet()) {
            Thread t = entry.getKey();
            stack = entry.getValue();
            if (t == null || stack == null) {
                throw new RuntimeException("Null thread or stacktrace returned");
            }
            if (t == one) {
                checkStack(t, stack, depth);
            }
        }
    }

    private static void checkStack(Thread t, StackTraceElement[] stack,
                                   int depth) throws Exception {
        if (trace) {
            printStack(t, stack);
        }
        int frame = stack.length - 1;
        for (int i = 0; i < depth && frame >= 0; i++) {
            if (! stack[frame].getMethodName().equals(methodNames[i])) {
                throw new RuntimeException("Expected " + methodNames[i] +
                                           " in frame " + frame + " but got " +
                                           stack[frame].getMethodName());
            }
            frame--;
        }
    }

    private static void printStack(Thread t, StackTraceElement[] stack) {
        System.out.println(t +
                           " stack: (length = " + stack.length + ")");
        if (t != null) {
            for (int j = 0; j < stack.length; j++) {
                System.out.println(stack[j]);
            }
            System.out.println();
        }
    }
}
