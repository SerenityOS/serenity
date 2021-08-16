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
 * @summary converted from VM testbase nsk/stress/strace/strace005.
 * VM testbase keywords: [stress, strace]
 * VM testbase readme:
 * DESCRIPTION
 *     The test checks up java.lang.Thread.getStackTrace() method for many threads,
 *     that recursively invoke pure java and native methods by turns in running
 *     mode ("alive" stack).
 *     The test fails if:
 *     - amount of stack trace elements is more than depth of recursion plus
 *       four elements corresponding to invocations of Thread.run(), Thread.wait(),
 *       Thread.exit(), Thread.yield() and ThreadGroup.remove() methods;
 *     - there is at least one element corresponding to invocation of unexpected
 *       method.
 *     This test is almost the same as nsk.stress.strace.strace001 and
 *     nsk.stress.strace.strace003 except for the recursive methods are
 *     pure java and native one.
 * COMMENTS
 * Below assertion is revealed on engineer's build. It is needed to check
 * on a promoted build.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * waiting for all threads started ...
 * Unexpected Signal : 11 occurred at PC=0xFDBB7820
 * Function=[Unknown. Nearest: SUNWprivate_1.1+0x3B7820]
 * Library=java/vitp/jdk/4593133/solaris-sparc/jre/lib/sparc/client/libjvm.so
 * Current Java thread:
 *         at nsk.stress.strace.strace005Thread.recursiveMethod2(Native Method)
 *         at nsk.stress.strace.strace005Thread.recursiveMethod1(strace005.java:285)
 *     . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
 *         at nsk.stress.strace.strace005Thread.recursiveMethod2(Native Method)
 *         at nsk.stress.strace.strace005Thread.recursiveMethod1(strace005.java:285)
 *         at nsk.stress.strace.strace005Thread.recursiveMethod2(Native Method)
 * Dynamic libraries:
 * 0x10000         jdk/4593133/solaris-sparc/bin/java
 * 0xff350000      /usr/lib/libthread.so.1
 * 0xff390000      /usr/lib/libdl.so.1
 * 0xff200000      /usr/lib/libc.so.1
 * 0xff330000      /usr/platform/SUNW,Ultra-60/lib/libc_psr.so.1
 * 0xfd800000      java/vitp/jdk/4593133/solaris-sparc/jre/lib/sparc/client/libjvm.so
 * 0xff2d0000      /usr/lib/libCrun.so.1
 * 0xff1d0000      /usr/lib/libsocket.so.1
 * 0xff100000      /usr/lib/libnsl.so.1
 * 0xff0d0000      /usr/lib/libm.so.1
 * 0xff0b0000      /usr/lib/libsched.so.1
 * 0xff300000      /usr/lib/libw.so.1
 * 0xff090000      /usr/lib/libmp.so.2
 * 0xff050000      java/vitp/jdk/4593133/solaris-sparc/jre/lib/sparc/native_threads/libhpi.so
 * 0xfd7d0000      java/vitp/jdk/4593133/solaris-sparc/jre/lib/sparc/libverify.so
 * 0xfd790000      java/vitp/jdk/4593133/solaris-sparc/jre/lib/sparc/libjava.so
 * 0xfe7e0000      java/vitp/jdk/4593133/solaris-sparc/jre/lib/sparc/libzip.so
 * 0xfc6e0000      java/vitp/tests/4593133/src/libstrace005.so
 * Heap at VM Abort:
 * Heap
 *  def new generation   total 2112K, used 336K [0xf1800000, 0xf1a20000, 0xf1f10000)
 *   eden space 2048K,  16% used [0xf1800000, 0xf1854300, 0xf1a00000)
 *   from space 64K,   0% used [0xf1a00000, 0xf1a00000, 0xf1a10000)
 *   to   space 64K,   0% used [0xf1a10000, 0xf1a10000, 0xf1a20000)
 *  tenured generation   total 1408K, used 0K [0xf1f10000, 0xf2070000, 0xf5800000)
 *    the space 1408K,   0% used [0xf1f10000, 0xf1f10000, 0xf1f10200, 0xf2070000)
 *  compacting perm gen  total 4096K, used 1020K [0xf5800000, 0xf5c00000, 0xf9800000)
 *    the space 4096K,  24% used [0xf5800000, 0xf58ff028, 0xf58ff200, 0xf5c00000)
 * Local Time = Fri Apr 25 18:09:16 2003
 * Elapsed Time = 13
 * #
 * # HotSpot Virtual Machine Error : 11
 * # Error ID : src/share/vm/runtime/os.cpp, 753 [ Patched ]
 * # Please report this error at
 * # http://java.sun.com/cgi-bin/bugreport.cgi
 * #
 * # Java VM: Java HotSpot(TM) Client VM (1.4.1-internal-debug mixed mode)
 * #
 * # An error report file has been saved as hs_err_pid16847.log.
 * # Please refer to the file for further information.
 * #
 * Dumping core....
 * Abort
 * Finished at: Fri Apr 25 18:09:17 NSK 2003
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native nsk.stress.strace.strace005
 */

package nsk.stress.strace;

import nsk.share.ArgumentParser;
import nsk.share.Failure;
import nsk.share.Log;

import java.io.PrintStream;

/**
 * The test checks up <code>java.lang.Thread.getStackTrace()</code> method for many threads,
 * that recursively invoke pure java and native methods by turns in running mode
 * ("alive" stack).
 * <p>
 * <p>The test creates <code>THRD_COUNT</code> instances of <code>strace005Thread</code>
 * class, tries to get their stack traces and checks up that returned array contains
 * correct stack frames. Each stack frame must be corresponded to one of the following
 * methods defined by the <code>EXPECTED_METHODS</code> array.</p>
 * <p>These checking are performed <code>REPEAT_COUNT</code> times.</p>
 */
public class strace005 {

    static final int DEPTH = 500;
    static final int THRD_COUNT = 100;
    static final int REPEAT_COUNT = 10;
    static final String[] EXPECTED_METHODS = {
            "java.lang.System.arraycopy",
            "java.lang.Object.wait",
            "java.lang.Thread.exit",
            "java.lang.Thread.yield",
            "java.lang.ThreadGroup.remove",
            "java.lang.ThreadGroup.threadTerminated",
            "nsk.stress.strace.strace005Thread.run",
            "nsk.stress.strace.strace005Thread.recursiveMethod1",
            "nsk.stress.strace.strace005Thread.recursiveMethod2"
    };


    static volatile boolean isLocked = false;
    static PrintStream out;
    static long waitTime = 2;

    static Object waitStart = new Object();

    static strace005Thread[] threads;
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

        strace005 test = new strace005();
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
        threads = new strace005Thread[THRD_COUNT];
        achivedCount = 0;

        String tmp_name;
        for (int i = 0; i < THRD_COUNT; i++) {
            tmp_name = "strace005Thread" + Integer.toString(i);
            threads[i] = new strace005Thread(this, tmp_name);
//            threads[i].setPriority(Thread.MIN_PRIORITY);
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
        // wait for native resolution completed (all threads have finished recursiveMethod2)
        boolean isNativeResolved = false;
        while (!isNativeResolved) {
            try {
                isNativeResolved = true;
                for (int i = 0; i < threads.length; ++i)
                    if (!threads[i].isNativeResolved)
                        isNativeResolved = false;
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }

        for (int i = 0; i < threads.length; i++) {
            snapshots[i] = threads[i].getStackTrace();
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

            if (elements == null || elements.length == 0)
                continue;

            if (elements.length > 0) {
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

/**
 * The test creates many instances of <code>strace005Thread</code> class and tries
 * to get their stack traces.
 */
class strace005Thread extends Thread {

    private int currentDepth = 0;
    public boolean isNativeResolved = false;

    strace005 test;

    static {
        try {
            System.loadLibrary("strace005");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load strace005 library");
            System.err.println("java.library.path:"
                    + System.getProperty("java.library.path"));
            throw e;
        }
    }

    strace005Thread(strace005 test, String name) {
        this.test = test;
        setName(name);
    }

    public void run() {

        recursiveMethod1();

    }

    void recursiveMethod1() {

        currentDepth++;

        if (currentDepth == 1) {
            synchronized (test) {
                test.achivedCount++;
            }

            int alltime = 0;
            while (!strace005.isLocked) {
                synchronized (test) {
                    try {
                        test.wait(1);
                        alltime++;
                    } catch (InterruptedException e) {
                        strace005.complain("" + e);
                    }
                    if (alltime > strace005.waitTime) {
                        throw new Failure("out of wait time");
                    }
                }
            }
        } else if (currentDepth > 1 && !isNativeResolved)
            isNativeResolved = true;

        if (strace005.DEPTH - currentDepth > 0) {
            try {
                Thread.yield();
                recursiveMethod2();
            } catch (StackOverflowError e) {
                // ignore this exception
            }
        }

        currentDepth--;
    }

    native void recursiveMethod2();
}
