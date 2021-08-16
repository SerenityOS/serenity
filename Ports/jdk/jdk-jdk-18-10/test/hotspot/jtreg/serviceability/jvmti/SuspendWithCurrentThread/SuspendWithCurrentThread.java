/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231595
 * @summary [TEST] develop a test case for SuspendThreadList including current thread
 * @requires vm.jvmti
 * @library /test/lib
 * @compile SuspendWithCurrentThread.java
 * @run main/othervm/native -agentlib:SuspendWithCurrentThread SuspendWithCurrentThread SuspenderIndex=first
 * @run main/othervm/native -agentlib:SuspendWithCurrentThread SuspendWithCurrentThread SuspenderIndex=last
 */

import java.io.PrintStream;

public class SuspendWithCurrentThread {
    private static final String AGENT_LIB = "SuspendWithCurrentThread";
    private static final String SUSPENDER_OPT = "SuspenderIndex=";
    private static final int THREADS_COUNT = 10;

    private static void log(String msg) { System.out.println(msg); }

    private static native void    registerTestedThreads(Thread[] threads);
    private static native boolean checkTestedThreadsSuspended();
    private static native void    resumeTestedThreads();
    private static native void    releaseTestedThreadsInfo();

    // The suspender thread index defines the thread which has to suspend
    // the tested threads including itself with the JVMTI SuspendThreadList
    private static int suspenderIndex;

    public static void main(String args[]) throws Exception {
        try {
            System.loadLibrary(AGENT_LIB);
            log("Loaded library: " + AGENT_LIB);
        } catch (UnsatisfiedLinkError ule) {
            log("Failed to load library: " + AGENT_LIB);
            log("java.library.path: " + System.getProperty("java.library.path"));
            throw ule;
        }
        if (args.length != 1) {
            throw new RuntimeException("Main: wrong arguments count: " + args.length + ", expected: 1");
        }
        String arg = args[0];
        if (arg.equals(SUSPENDER_OPT + "first")) {
            suspenderIndex = 0;
        } else if (arg.equals(SUSPENDER_OPT + "last")) {
            suspenderIndex = THREADS_COUNT - 1;
        } else {
            throw new RuntimeException("Main: wrong argument: " + arg + ", expected: SuspenderIndex={first|last}");
        }
        log("Main: suspenderIndex: " + suspenderIndex);

        SuspendWithCurrentThread test = new SuspendWithCurrentThread();
        test.run();
    }

    private ThreadToSuspend[] startTestedThreads(int threadsCount) throws RuntimeException  {
        ThreadToSuspend[]threads = new ThreadToSuspend[threadsCount];

        // create tested threads
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ThreadToSuspend("ThreadToSuspend#" + i,
                                             i == suspenderIndex // isSuspender
                                            );
        }
        log("Main: starting tested threads");
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
            if (!threads[i].checkReady()) {
                throw new RuntimeException("Main: unable to prepare tested thread: " + threads[i]);
            }
        }
        log("Main: tested threads started");

        registerTestedThreads(threads);
        return threads;
    }

    private boolean checkSuspendedStatus() throws RuntimeException  {
        log("Main: checking all tested threads have been suspended");
        return checkTestedThreadsSuspended();
    }

    /* The test does the following steps:
     *  - main thread starts several (THREADS_COUNT) ThreadToSuspend tested threads
     *  - main thread waits for threads to be ready with the thread.checkReady()
     *  - main thread registers tested threads within the native agent library
     *    with the native method registerTestedThreads()
     *  - main thread triggers the suspender tested thread with the
     *    ThreadToSuspend.setAllThreadsReady() to suspend tested threads
     *  - suspender thread suspends tested threads including itself with the native
     *    method suspendTestedThreads() (uses the JVMTI SuspendThreadList function)
     *  - main thread checks tested threads suspended status with the native method
     *    checkSuspendedStatus(); the tested threads are expected to have suspended status
     *  - main thread resumes tested threads with the native method resumeTestedThreads()
     *  - main thread releases tested threads with the native method releaseTestedThreads()
     *  - main thread triggers the tested threads to finish with the thread.letFinish()
     */
    private void run() throws Exception {
        ThreadToSuspend[] threads = null; // tested threads

        log("Main: started");
        try {
            threads = startTestedThreads(THREADS_COUNT);

            log("Main: trigger " + threads[suspenderIndex].getName() +
                " to suspend all tested threads including itself");
            ThreadToSuspend.setAllThreadsReady();

            while (!checkSuspendedStatus()) {
                Thread.sleep(10);
            }

            log("Main: resuming all tested threads");
            resumeTestedThreads();
        } finally {
            // let threads to finish
            for (int i = 0; i < threads.length; i++) {
                threads[i].letFinish();
            }
            log("Main: tested threads finished");
        }

        // wait for threads to finish
        log("Main: joining tested threads");
        try {
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
            log("Main: tested thread joined");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log("Main: releasing tested threads native info");
        releaseTestedThreadsInfo();

        log("Main: finished");
    }
}

/* =================================================================== */

// tested threads
class ThreadToSuspend extends Thread {
    private static void log(String msg) { System.out.println(msg); }

    private static native void suspendTestedThreads();
    private static volatile boolean allThreadsReady = false;

    public static void setAllThreadsReady() {
        allThreadsReady = true;
    }

    private volatile boolean threadReady = false;
    private volatile boolean shouldFinish = false;
    private boolean isSuspender = false;

    // make thread with specific name
    public ThreadToSuspend(String name, boolean isSuspender) {
        super(name);
        this.isSuspender = isSuspender;
    }

    // run thread continuously
    public void run() {
        boolean needSuspend = true;
        threadReady = true;

        // run in a loop
        while (!shouldFinish) {
            if (isSuspender && needSuspend && allThreadsReady) {
                log(getName() + ": before suspending all tested threads including myself");
                needSuspend = false;
                suspendTestedThreads();
                log(getName() + ": after suspending all tested threads including myself");
            }
        }
    }

    // check if thread is ready
    public boolean checkReady() {
        try {
            while (!threadReady) {
                sleep(1);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("checkReady: sleep was interrupted\n\t" + e);
        }
        return threadReady;
    }

    // let thread to finish
    public void letFinish() {
        shouldFinish = true;
    }
}
