/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress
 *
 * @summary converted from VM testbase nsk/stress/strace/strace014.
 * VM testbase keywords: [stress, strace]
 * VM testbase readme:
 * DESCRIPTION
 *     The test runs many threads, that recursively invoke a native method.
 *     After arriving at defined depth of recursion, each thread is switched to
 *     waits for a monitor. Then the test calls java.lang.Thread.getStackTrace()
 *     and java.lang.Thread.getAllStackTraces() methods and checks their results.
 *     The test fails if:
 *     - amount of stack trace elements and stack trace elements themselves are
 *       the same for both methods;
 *     - there is at least one element corresponding to invocation of unexpected
 *       method. Expected methods are Thread.sleep(), Thread.run() and the
 *       recursive method.
 *     This test is almost the same as nsk.stress.strace.013 except for
 *     the recursive method is a native one.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native nsk.stress.strace.strace014
 */

package nsk.stress.strace;

import nsk.share.ArgumentParser;
import nsk.share.Log;

import java.io.PrintStream;
import java.util.Map;

/**
 * The test runs <code>THRD_COUNT</code> instances of <code>strace010Thread</code>,
 * that recursively invoke a native method. After arriving at defined depth
 * <code>DEPTH</code> of recursion, each thread is switched to wait a monitor.
 * Then the test calls <code>java.lang.Thread.getStackTrace()</code> and
 * <code>java.lang.Thread.getAllStackTraces()</code> methods and checks their results.
 * <p>
 * <p>It is expected that these methods return the same stack traces. Each stack frame
 * for both stack traces must be corresponded to invocation of one of the methods
 * defined by the <code>EXPECTED_METHODS</code> array.</p>
 */
public class strace014 {

    static final int DEPTH = 100;
    static final int THRD_COUNT = 100;
    static final String[] EXPECTED_METHODS = {
            "java.lang.Object.wait",
            "nsk.stress.strace.strace014Thread.run",
            "nsk.stress.strace.strace014Thread.recursiveMethod"
    };

    static PrintStream out;
    static long waitTime = 2;

    static Object lockedObject = new Object();
    static volatile boolean proceed = false;

    static volatile int achivedCount = 0;
    strace014Thread[] threads;
    static Log log;

    public static void main(String[] args) {
        out = System.out;
        int exitCode = run(args);
        System.exit(exitCode + 95);
    }

    public static int run(String[] args) {
        ArgumentParser argHandler = new ArgumentParser(args);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        strace014 test = new strace014();
        boolean res = true;

        test.startThreads();

        res = test.makeSnapshot();

        test.finishThreads();

        if (!res) {
            complain("***>>>Test failed<<<***");
            return 2;
        }

        display(">>>Test passed<<<");
        return 0;
    }

    void startThreads() {
        threads = new strace014Thread[THRD_COUNT];
        achivedCount = 0;

        String tmp_name;
        display("starting threads...");
        for (int i = 0; i < THRD_COUNT; i++) {
            tmp_name = "strace014Thread" + Integer.toString(i);
            threads[i] = new strace014Thread(this, tmp_name);
            threads[i].start();
        }

        waitFor("the defined recursion depth ...");
    }

    void waitFor(String msg) {
        if (msg.length() > 0)
            display("waiting for " + msg);

        while (achivedCount < THRD_COUNT) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                complain("" + e);
            }
        }
        achivedCount = 0;
    }

    boolean makeSnapshot() {

        Map traces = null;
        int count = 0;
        StackTraceElement[][] elements = null;

        display("making all threads snapshots...");
        traces = Thread.getAllStackTraces();
        count = ((StackTraceElement[]) traces.get(threads[0])).length;

        display("making snapshots of each thread...");
        elements = new StackTraceElement[THRD_COUNT][];
        for (int i = 0; i < THRD_COUNT; i++) {
            elements[i] = threads[i].getStackTrace();
        }


        display("notifying ...");
        synchronized (lockedObject) {
            proceed = true;
            lockedObject.notifyAll();
        }

        display("");

        display("checking lengths of stack traces...");
        StackTraceElement[] all;
        for (int i = 1; i < THRD_COUNT; i++) {
            all = (StackTraceElement[]) traces.get(threads[i]);
            int k = all.length;
            if (count - k > 2) {
                complain("wrong lengths of stack traces:\n\t"
                        + threads[0].getName() + ": " + count
                        + "\t"
                        + threads[i].getName() + ": " + k);
                return false;
            }
        }

        display("checking stack traces...");
        boolean res = true;
        for (int i = 0; i < THRD_COUNT; i++) {
            all = (StackTraceElement[]) traces.get(threads[i]);
            if (!checkTraces(threads[i].getName(), elements[i], all)) {
                res = false;
            }
        }
        return res;
    }

    boolean checkTraces(String threadName, StackTraceElement[] threadSnap,
                        StackTraceElement[] allSnap) {

        int checkedLength = threadSnap.length < allSnap.length ?
                threadSnap.length : allSnap.length;
        boolean res = true;

        for (int j = 0; j < checkedLength; j++) {
            if (!checkElement(threadSnap[j])) {
                complain("Unexpected " + j + "-element:");
                complain("\tmethod name: " + threadSnap[j].getMethodName());
                complain("\tclass name: " + threadSnap[j].getClassName());
                if (threadSnap[j].isNativeMethod()) {
                    complain("\tline number: (native method)");
                } else {
                    complain("\tline number: " + threadSnap[j].getLineNumber());
                    complain("\tfile name: " + threadSnap[j].getFileName());
                }
                complain("");
                res = false;
            }
        }
        return res;
    }

    boolean checkElement(StackTraceElement element) {
        String name = element.getClassName() + "." + element.getMethodName();
        for (int i = 0; i < EXPECTED_METHODS.length; i++) {
            if (EXPECTED_METHODS[i].compareTo(name) == 0)
                return true;
        }
        return false;
    }

    void finishThreads() {
        try {
            for (int i = 0; i < threads.length; i++) {
                if (threads[i].isAlive()) {
                    display("waiting for finish " + threads[i].getName());
                    threads[i].join(waitTime);
                }
            }
        } catch (InterruptedException e) {
            complain("" + e);
        }
    }

    static void display(String message) {
        log.display(message);
    }

    static void complain(String message) {
        log.complain(message);
    }

}

/**
 * The test creates many instances of <code>strace014Thread</code> class and tries
 * to get their stack traces.
 */
class strace014Thread extends Thread {

    private int currentDepth = 0;

    strace014 test;

    static {
        try {
            System.loadLibrary("strace014");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load strace014 library");
            System.err.println("java.library.path:"
                    + System.getProperty("java.library.path"));
            throw e;
        }
    }

    strace014Thread(strace014 test, String name) {
        this.test = test;
        setName(name);
    }

    public void run() {

        recursiveMethod();

    }

    native void recursiveMethod();
}
