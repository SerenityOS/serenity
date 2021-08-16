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
 * @summary converted from VM testbase nsk/stress/strace/strace004.
 * VM testbase keywords: [stress, strace]
 * VM testbase readme:
 * DESCRIPTION
 *     The test checks up java.lang.Thread.getAllStackTraces() method for many
 *     threads, that recursively invoke a native method in running mode
 *     ("alive" stack).
 *     The test fails if:
 *     - amount of stack trace elements is more than depth of recursion plus
 *       four elements corresponding to invocations of Thread.run(), Thread.wait(),
 *       Thread.exit(), Thread.yield() and ThreadGroup.remove() methods;
 *     - there is at least one element corresponding to invocation of unexpected
 *       method.
 *     This test is almost the same as nsk.stress.strace.strace003 except for
 *     checking is performed for java.lang.Thread.getAllStackTraces() method.
 * COMMENTS
 * java.lang.Thread.getAllStackTraces() is too slow method. So it is not successed
 * to catch an alive thread during execution of this method for the first snapshot
 * and it is needed to check on a promoted build due to the below assertion.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * waiting for all threads started ...
 * >>> snapshot 1
 * waiting for threads finished
 * # To suppress the following error report, specify this argument
 * # after -XX: or in .hotspotrc:  SuppressErrorAt=/jniHandles.hpp:157
 * #
 * # HotSpot Virtual Machine Error, assertion failure
 * # Please report this error at
 * # http://java.sun.com/cgi-bin/bugreport.cgi
 * #
 * # Java VM: Java HotSpot(TM) Client VM (1.4.1-internal-debug mixed mode)
 * #
 * # assert(result != ((oop)::badJNIHandleVal), "Pointing to zapped jni handle area")
 * #
 * # Error ID: src/share/vm/runtime/jniHandles.hpp, 157 [ Patched ]
 * #
 * # Problematic Thread: prio=5 tid=0x001976e0 nid=0x96 runnable
 * #
 * Heap at VM Abort:
 * Heap
 *  def new generation   total 2112K, used 455K [0xf1800000, 0xf1a20000, 0xf1f10000)
 *   eden space 2048K,  22% used [0xf1800000, 0xf1871f60, 0xf1a00000)
 *   from space 64K,   0% used [0xf1a00000, 0xf1a00000, 0xf1a10000)
 *   to   space 64K,   0% used [0xf1a10000, 0xf1a10000, 0xf1a20000)
 *  tenured generation   total 1408K, used 0K [0xf1f10000, 0xf2070000, 0xf5800000)
 *    the space 1408K,   0% used [0xf1f10000, 0xf1f10000, 0xf1f10200, 0xf2070000)
 *  compacting perm gen  total 4096K, used 1025K [0xf5800000, 0xf5c00000, 0xf9800000)
 *    the space 4096K,  25% used [0xf5800000, 0xf5900660, 0xf5900800, 0xf5c00000)
 * Dumping core....
 * Abort
 * Finished at: Fri Apr 25 18:01:37 NSK 2003
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native nsk.stress.strace.strace004
 */

package nsk.stress.strace;

import nsk.share.ArgumentParser;
import nsk.share.Log;

import java.io.PrintStream;
import java.util.Map;

/**
 * The test check up <code>java.lang.Thread.getAllStackTraces()</code> method for many
 * threads, that recursively invoke a native method in running mode ("alive" stack).
 * <p>
 * <p>The test creates <code>THRD_COUNT</code> instances of <code>strace004Thread</code>
 * class, tries to get their stack traces and checks up that returned array contains
 * correct stack frames. Each stack frame must be corresponded to one of the following
 * methods defined by the <code>EXPECTED_METHODS</code> array.</p>
 * <p>These checking are performed <code>REPEAT_COUNT</code> times.</p>
 */
public class strace004 {

    static final int DEPTH = 100;
    static final int THRD_COUNT = 100;
    static final int REPEAT_COUNT = 10;
    static final String[] EXPECTED_METHODS = {
            "java.lang.System.arraycopy",
            "java.lang.Object.wait",
            "java.lang.Thread.exit",
            "java.lang.Thread.yield",
            "java.lang.ThreadGroup.remove",
            "java.lang.ThreadGroup.threadTerminated",
            "nsk.stress.strace.strace004Thread.run",
            "nsk.stress.strace.strace004Thread.recursiveMethod"
    };


    static volatile boolean isLocked = false;
    static PrintStream out;
    static long waitTime = 2;

    static Object waitStart = new Object();

    static strace004Thread[] threads;
    static StackTraceElement[][] snapshots = new StackTraceElement[THRD_COUNT][];
    static Log log;

    volatile int achivedCount = 0;

    public static void main(String[] args) {
        out = System.out;
        int exitCode = run(args);
        System.exit(exitCode + 95);
    }

    public static int run(String[] args) {
        ArgumentParser argHandler = new ArgumentParser(args);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        strace004 test = new strace004();
        boolean res = true;

        for (int j = 0; j < REPEAT_COUNT; j++) {
            test.startThreads();

            if (!test.makeSnapshot(j + 1)) res = false;

            display("waiting for threads finished\n");
            test.finishThreads();
        }

        if (!res) {
            complain("***>>>Test failed<<<***");
            return 2;
        }

        return 0;
    }

    void startThreads() {
        threads = new strace004Thread[THRD_COUNT];
        achivedCount = 0;

        String tmp_name;
        for (int i = 0; i < THRD_COUNT; i++) {
            tmp_name = "strace004Thread" + Integer.toString(i);
            threads[i] = new strace004Thread(this, tmp_name);
        }

        for (int i = 0; i < THRD_COUNT; i++) {
            threads[i].start();
        }

        waitFor("all threads started ...");
        synchronized (waitStart) {
            isLocked = true;
            waitStart.notifyAll();
        }
        try {
            Thread.yield();
            Thread.sleep(1);
        } catch (InterruptedException e) {
            complain("" + e);
        }
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

    boolean makeSnapshot(int repeat_number) {

        Map traces = Thread.getAllStackTraces();
        for (int i = 0; i < threads.length; i++) {
            snapshots[i] = (StackTraceElement[]) traces.get(threads[i]);
        }

        return checkTraces(repeat_number);
    }

    boolean checkTraces(int repeat_number) {
        StackTraceElement[] elements;

        boolean res = true;
        display(">>> snapshot " + repeat_number);
        int expectedCount = DEPTH + 1;

        for (int i = 0; i < threads.length; i++) {
            elements = snapshots[i];

            if (elements == null)
                continue;

            if (elements.length == 0)
                continue;

            if (elements.length > 3) {
                display("\tchecking " + threads[i].getName()
                        + "(trace elements: " + elements.length + ")");
            }

            if (elements.length > expectedCount) {
                complain(threads[i].getName() + ">Contains more then " +
                        +expectedCount + " elements");
            }

            for (int j = 0; j < elements.length; j++) {
                if (!checkElement(elements[j])) {
                    complain(threads[i].getName() + ">Unexpected method name: "
                            + elements[j].getMethodName());
                    complain("\tat " + j + " position");
                    if (elements[j].isNativeMethod()) {
                        complain("\tline number: (native method)");
                        complain("\tclass name: " + elements[j].getClassName());
                    } else {
                        complain("\tline number: " + elements[j].getLineNumber());
                        complain("\tclass name: " + elements[j].getClassName());
                        complain("\tfile name: " + elements[j].getFileName());
                    }
                    res = false;
                }
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
                if (threads[i].isAlive())
                    threads[i].join(waitTime / THRD_COUNT);
            }
        } catch (InterruptedException e) {
            complain("" + e);
        }
        isLocked = false;
    }

    static void display(String message) {
        log.display(message);
    }

    static void complain(String message) {
        log.complain(message);
    }

}

class strace004Thread extends Thread {

    private int currentDepth = 0;

    strace004 test;

    static {
        try {
            System.loadLibrary("strace004");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load strace004 library");
            System.err.println("java.library.path:"
                    + System.getProperty("java.library.path"));
            throw e;
        }
    }

    strace004Thread(strace004 test, String name) {
        this.test = test;
        setName(name);
    }

    public void run() {

        recursiveMethod();

    }

    native void recursiveMethod();
}
