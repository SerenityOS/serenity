/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8213397 8217337
 * @summary Check that the thread dump shows when a thread is blocked
 *          on a class initialization monitor
 *
 * @library /test/lib
 * @run main/othervm TestThreadDumpClassInitMonitor
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Platform;

import java.io.IOException;
import java.util.List;

public class TestThreadDumpClassInitMonitor {
    // jstack tends to be closely bound to the VM that we are running
    // so use getTestJDKTool() instead of getCompileJDKTool() or even
    // getJDKTool() which can fall back to "compile.jdk".
    final static String JSTACK = JDKToolFinder.getTestJDKTool("jstack");
    final static String PID = "" + ProcessHandle.current().pid();

    final static Thread current = Thread.currentThread();

    /*
     * This is the output we're looking for:
     *
     * "TestThread" #22 prio=5 os_prio=0 cpu=1.19ms elapsed=0.80s tid=0x00007f8f9405d800 nid=0x568b in Object.wait()  [0x00007f8fd94d0000]
     *   java.lang.Thread.State: RUNNABLE
     * Thread: 0x00007f8f9405d800  [0x568b] State: _at_safepoint _has_called_back 0 _at_poll_safepoint 0  // DEBUG ONLY
     *   JavaThread state: _thread_blocked                                                                // DEBUG ONLY
     *         at TestThreadDumpClassInitMonitor$Target$1.run(TestThreadDumpClassInitMonitor.java:69)
     *         - waiting on the Class initialization monitor for TestThreadDumpClassInitMonitor$Target
     *
     */
    final static String TEST_THREAD = "TestThread";
    final static String TEST_THREAD_ENTRY = "\"" + TEST_THREAD;
    final static String IN_OBJECT_WAIT = "in Object.wait()";
    final static String THREAD_STATE = "java.lang.Thread.State: RUNNABLE";
    final static String THREAD_INFO = "Thread:"; // the details are not important
    final static String JAVATHREAD_STATE = "JavaThread state: _thread_blocked";
    final static String CURRENT_METHOD = "at TestThreadDumpClassInitMonitor$Target$1.run";
    final static String WAIT_INFO = "- waiting on the Class initialization monitor for TestThreadDumpClassInitMonitor$Target";

    volatile static boolean ready = false;

    static List<String> stackDump;  // jstack output as lines

    static class Target {

        static int field;

        // The main thread will initialize this class and so
        // execute the actual test logic here.
        static {
            if (Thread.currentThread() != current) {
                throw new Error("Initialization logic error");
            }
            System.out.println("Initializing Target class in main thread");

            Thread t  = new Thread() {
                    public void run() {
                        System.out.println("Test thread about to access Target");
                        ready = true; // tell main thread we're close
                        // This will block until the main thread completes
                        // static initialization of target
                        Target.field = 42;
                        System.out.println("Test thread done");
                    }
                };
            t.setName(TEST_THREAD);
            t.start();

            // We want to run jstack once the test thread is blocked but
            // there's no programmatic way to detect that. So we check the flag
            // that will be set just before it should block, then by the time
            // we can exec jstack it should be ready.
            try {
                while (!ready) {
                    Thread.sleep(200);
                }
            }
            catch (InterruptedException ie) {
                throw new Error("Shouldn't happen");
            }

            // Now run jstack
            try {
                ProcessBuilder pb = new ProcessBuilder(JSTACK, PID);
                OutputAnalyzer output = new OutputAnalyzer(pb.start());
                output.shouldHaveExitValue(0);
                stackDump = output.asLines();
            }
            catch (IOException ioe) {
                throw new Error("Launching jstack failed", ioe);
            }
        }
    }


    public static void main(String[] args) throws Throwable {
        // Implicitly run the main test logic
        Target.field = 21;

        // Now check the output of jstack
        try {
            // product builds miss 2 lines of information in the stack
            boolean isProduct = !Platform.isDebugBuild();
            int foundLines = 0;
            parseStack: for (String line : stackDump) {
                switch(foundLines) {
                case 0: {
                    if (!line.startsWith(TEST_THREAD_ENTRY)) {
                        continue;
                    }
                    foundLines++;
                    if (!line.contains(IN_OBJECT_WAIT)) {
                        throw new Error("Unexpected initial stack line: " + line);
                    }
                    continue;
                }
                case 1: {
                    if (!line.trim().equals(THREAD_STATE)) {
                        throw new Error("Unexpected thread state line: " + line);
                    }
                    if (isProduct) {
                        foundLines += 3;
                    } else {
                        foundLines++;
                    }
                    continue;
                }
                case 2: { // Debug build
                    if (!line.startsWith(THREAD_INFO)) {
                        throw new Error("Unexpected thread info line: " + line);
                    }
                    foundLines++;
                    continue;
                }
                case 3: { // Debug build
                    if (!line.trim().equals(JAVATHREAD_STATE)) {
                        throw new Error("Unexpected JavaThread state line: " + line);
                    }
                    foundLines++;
                    continue;
                }
                case 4: {
                    if (!line.trim().startsWith(CURRENT_METHOD)) {
                        throw new Error("Unexpected current method line: " + line);
                    }
                    foundLines++;
                    continue;
                }
                case 5: {
                    if (!line.trim().equals(WAIT_INFO)) {
                        throw new Error("Unexpected monitor information line: " + line);
                    }
                    break parseStack;
                }
                default: throw new Error("Logic error in case statement");
                }
            }

            if (foundLines == 0) {
                throw new Error("Unexpected stack content - did not find line starting with "
                                + TEST_THREAD_ENTRY);
            }
        }
        catch (Error e) {
            // Dump the full stack trace on error so we can check the content
            for (String line : stackDump) {
                System.out.println(line);
            }
            throw e;
        }
    }
}
