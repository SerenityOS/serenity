/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM testbase nsk/stress/except/except008.
 * VM testbase keywords: [stress, diehard, slow, nonconcurrent, quick]
 * VM testbase readme:
 * DESCRIPTION
 *     This checks if various exceptions are thrown (and caught) correctly
 *     when there apparently are no free space in the heap to allocate new
 *     Throwable instance.
 *     The test tries to occupy all of memory available in the heap by allocating
 *     lots of new Object() instances. Instances of the type Object are the smallest
 *     objects, so they apparently should occupy most fine-grained fragments in the
 *     heap and leave no free space for new Throwable instance. After that, the test
 *     provokes various exceptions (e.g.: by executing integer division by 0 and so
 *     on), and checks if appropriate exceptions are thrown.
 * COMMENTS
 *     The test needs a lot of memory to start up, so it should not run under older
 *     JDK 1.1.x release due to its poorer heap utilization. Also, some checks are
 *     skipped when testing classic VM, because OutOfMemoryError is correctly thrown
 *     instead of target exception.
 *     When the test is being self-initiating (i.e.: eating heap), memory occupation
 *     is terminated if memory allocation slows down crucially. This is a workaround
 *     intended to avoid the HotSpot bug:
 *         #4248801 (P1/S5) slow memory allocation when heap is almost exhausted
 *     There is also a workaround involved to avoid the following bugs known
 *     for HotSpot and for classic VM:
 *         #4239841 (P1/S5) 1.1: poor garbage collector performance  (HotSpot bug)
 *         #4245060 (P4/S5) poor garbage collector performance       (Classic VM bug)
 *     However, printing of the test's error messages, warnings, and of execution
 *     trace fails under JDK 1.2 for Win32 even so. If the test fails due to this
 *     problem, exit status 96 is returned instead of 97.
 *     JDK 1.3 classic VM for Sparc may crash (core dump) due to the known bug:
 *         #4245057 (P2/S3) VM crashes when heap is exhausted
 *
 * @run main/othervm -Xms50M -Xmx200M -XX:-UseGCOverheadLimit nsk.stress.except.except008
 */

package nsk.stress.except;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * This checks if various exceptions are thrown (and caught) correctly
 * when there apparently are no free space in the heap to allocate new
 * <code>Throwable</code> instance.
 * <p>
 * <p>The test tries to occupy all of memory available in the heap by
 * allocating lots of new <code>Object()</code> instances. Instances of the
 * type <code>Object</code> are the smallest objects, so they apparently should
 * occupy most fine-grained fragments in the heap and leave no free space for
 * new <code>Throwable</code> instance. After that, the test provokes various
 * exceptions (e.g.: by executing integer division by 0 and so on), and checks
 * if appropriate exceptions are thrown.
 * <p>
 * <p>Note, that memory occupation is terminated if memory allocation slows
 * down crucially. This is a workaround intended to avoid the HotSpot bug:
 * <br>&nbsp;&nbsp;
 * #4248801 (P1/S5) slow memory allocation when heap is almost exhausted
 * <p>
 * <p>There is also a workaround involved to avoid the following bugs known
 * for HotSpot and for classic VM:
 * <br>&nbsp;&nbsp;
 * #4239841 (P1/S5) 1.1: poor garbage collector performance
 * <br>&nbsp;&nbsp;
 * #4245060 (P4/S5) poor garbage collector performance
 * <br>However, printing of the test's error messages, warnings, and of
 * execution trace may fail even so. If the test fails due to poor GC
 * performance, exit status 96 is returned instead of 97.
 * <p>
 * <p>Also note, that the test needs a lot of memory to start up, so it should
 * not run under older JDK 1.1.x release due to its poor heap utilization.
 */
public class except008 {
    /**
     * Either allow or supress printing of execution trace.
     */
    private static boolean TRACE_ON = false;
    /**
     * Either allow or supress printing of warning messages.
     */
    private static final boolean WARN_ON = true;
    /*
     * Storage for a lot of tiny objects
     * "static volatile" keywords are for preventing heap optimization
     */
    private static volatile Object pool[] = null;
    /**
     * Temporary <code>log</code> for error messages, warnings and/or execution trace.
     *
     * @see #messages
     */
    private static String log[] = new String[1000]; // up to 1000 messages
    /**
     * How many <code>messages</code> were submitted to the <code>log</code>.
     *
     * @see #log
     */
    private static int messages = 0;

    /**
     * Re-call to the method <code>run(out)</code> (ignore <code>args[]</code>),
     * and print the test summary - either test passed of failed.
     */
    public static int run(String args[], PrintStream out) {
        if (args.length > 0) {
            if (args[0].toLowerCase().startsWith("-v"))
                TRACE_ON = true;
        }

        int exitCode = run(out);
        pool = null;
        System.gc();
        // Print the log[] and the test summary:
        try {
            for (int i = 0; i < messages; i++)
                out.println(log[i]);
            if (exitCode == 0) {
                if (TRACE_ON)
                    out.println("Test passed.");
            } else
                out.println("Test failed.");
        } catch (OutOfMemoryError oome) {
            // Poor performance of garbage collector:
            exitCode = 1;
        }

        return exitCode;
    }

    /**
     * Allocate as much <code>Object</code> instances as possible to bring JVM
     * into stress, and then check if exceptions are correctly thrown accordingly
     * to various situations like integer division by 0, etc.
     */
    private static int run(PrintStream out) {
        out.println("# While printing this message, JVM seems to initiate the output");
        out.println("# stream, so that it will not need more memory to print later,");
        out.println("# when the heap would fail to provide more memory.");
        out.println("# ");
        out.println("# Note, that the test maintains especial static log[] field in");
        out.println("# order to avoid printing when the heap seems exhausted.");
        out.println("# Nevertheless, printing could arise OutOfMemoryError even");
        out.println("# after all the memory allocated by the test is released.");
        out.println("# ");
        out.println("# That problem is caused by the known JDK/HotSpot bugs:");
        out.println("#     4239841 (P1/S5) 1.1: poor garbage collector performance");
        out.println("#     4245060 (P4/S5) poor garbage collector performance");
        out.println("# ");
        out.println("# This message is just intended to work-around that problem.");
        out.println("# If printing should fail even so, the test will try to return");
        out.println("# the exit status 96 instead of 97 to indicate the problem.");
        out.println("# However, the test may fail or even crash on some platforms");
        out.println("# suffering the bug 4239841 or 4245060.");

        // Prepare some items, which will be used by the test:
        Zoo zoo = new Zoo(); // load the class Zoo
        Class noArgs[] = new Class[0];

        // Sum up exit code:
        int exitCode = 0; // apparently PASSED
        int skipped = 0;  // some checks may correctly suffer OutOfMemoryError

        // Allocate repository for a lots of tiny objects:
        for (int size = 1 << 30; size > 0 && pool == null; size >>= 1)
            try {
                pool = new Object[size];
            } catch (OutOfMemoryError oome) {
            }
        if (pool == null)
            throw new Error("HS bug: cannot allocate new Object[1]");
        int poolSize = pool.length;

        int index = 0;
        pool[index++] = new Object();

        // Sum up time spent, when it was hard to JVM to allocate next object
        // (i.e.: when JVM has spent more than 1 second to allocate new object):
        double totalDelay = 0;
        long timeMark = System.currentTimeMillis();
        try {
            for (; index < poolSize; index++) {
                //-------------------------
                pool[index] = new Object();
                long nextTimeMark = System.currentTimeMillis();
                long elapsed = nextTimeMark - timeMark;
                timeMark = nextTimeMark;
                //----------------------
                if (elapsed > 1000) {
                    double seconds = elapsed / 1000.0;
                    if (TRACE_ON)
                        out.println(
                                "pool[" + index + "]=new Object(); // elapsed " + seconds + "s");
                    totalDelay += seconds;
                    if (totalDelay > 60) {
                        if (TRACE_ON)
                            out.println(
                                    "Memory allocation became slow; so, heap seems exhausted.");
                        break;
                    }
                }
            }
        } catch (OutOfMemoryError oome) {
            if (TRACE_ON)
                log[messages++] = "Heap seems exhausted - OutOfMemoryError thrown.";
        }

        if (index > poolSize - 1000) {
            if (WARN_ON)
                log[messages++] = "Warning: pool[] is full; so, checks would not be enough hard...";
        }

        // Check NoSuchFieldException (positive):
        try {
            Field valid = Zoo.class.getField("PUBLIC_FIELD");  // should pass
//          Field wrong = Zoo.class.getField("PRIVATE_FIELD"); // should fail
            if (TRACE_ON)
                log[messages++] = "Success: NoSuchFieldException not thrown as expected";
        } catch (NoSuchFieldException nsfe) {
            pool[index++] = nsfe;
            log[messages++] = "Failure: NoSuchFieldException thrown unexpectedly";
            exitCode = 2;
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] =
                        "Skipped: NoSuchFieldException positive check - OutOfMemoryError thrown";
            skipped++;
        }

        // Check NoSuchFieldException (negative):
        try {
//          Field valid = Zoo.class.getField("PUBLIC_FIELD");  // should pass
            Field wrong = Zoo.class.getField("PRIVATE_FIELD"); // should fail
            log[messages++] = "Failure: NoSuchFieldException incorrectly not thrown";
            exitCode = 2;
        } catch (NoSuchFieldException nsfe) {
            if (TRACE_ON)
                log[messages++] = "Success: NoSuchFieldException thrown as expected";
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] =
                        "NoSuchFieldException negative check - OutOfMemoryError thrown";
            skipped++;
        }

        // Check NoSuchMethodException (positive):
        try {
            Method valid = Zoo.class.getMethod("PUBLIC_METHOD", noArgs);  // should pass
//          Method wrong = Zoo.class.getMethod("PRIVATE_METHOD",noArgs); // should fail
            if (TRACE_ON)
                log[messages++] = "Success: NoSuchFieldException not thrown as expected";
        } catch (NoSuchMethodException nsme) {
            pool[index++] = nsme;
            log[messages++] = "Failure: NoSuchMethodException thrown unexpectedly";
            exitCode = 2;
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] =
                        "Skipped: NoSuchMethodException positive check - OutOfMemoryError thrown";
            pool[index++] = oome;
            skipped++;
        }

        // Check NoSuchMethodException (negative):
        try {
//          Method valid = Zoo.class.getMethod("PUBLIC_METHOD",noArgs);  // should pass
            Method wrong = Zoo.class.getMethod("PRIVATE_METHOD", noArgs); // should fail
            log[messages++] = "Failure: NoSuchMethodException incorrectly not thrown";
            exitCode = 2;
        } catch (NoSuchMethodException nsme) {
            pool[index++] = nsme;
            if (TRACE_ON)
                log[messages++] = "Success: NoSuchFieldException thrown as expected";
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] =
                        "Skipped: NoSuchMethodException negative check - OutOfMemoryError thrown";
            skipped++;
        }

        return exitCode;
    }

    /**
     * Several items used to check reflections.
     */
    private static class Zoo {
        public String PUBLIC_FIELD = "Accessible via reflection";
        private String PRIVATE_FIELD = "Inaccessible via reflection";

        public String PUBLIC_METHOD() {
            return "Accessible via reflection";
        }

        private String PRIVATE_METHOD() {
            return "Inaccessible via reflection";
        }

    }

    /**
     * Re-call to <code>run(args,out)</code>, and return JCK-like exit status.
     * (The stream <code>out</code> is assigned to <code>System.out</code> here.)
     *
     * @see #run(String[], PrintStream)
     */
    public static void main(String args[]) {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            // Last try. If there is some exception outside the code, test should end correctly
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    pool = null;
                    log = null;
                    System.gc();
                    if (e instanceof OutOfMemoryError) {
                        try {
                            System.out.println("OOME : Test Skipped");
                            System.exit(0);
                        } catch (Throwable ignore) {
                        } // No code in the handler can provoke correct exceptions.
                    } else {
                        e.printStackTrace();
                        throw (RuntimeException) e;
                    }
                } catch (OutOfMemoryError oome) {
                }
            }
        });
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
        // JCK-like exit status.
    }

}
