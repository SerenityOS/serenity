/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM testbase nsk/stress/except/except004.
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
 * @run main/othervm -Xms50M -Xmx200M -XX:-UseGCOverheadLimit nsk.stress.except.except004
 */

package nsk.stress.except;

import java.io.PrintStream;
import java.lang.reflect.Field;

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
public class except004 {
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

        int exitCode;
        try {
            exitCode = run(out);
        } finally { // ensure we have free memory for exception processing
            pool = null;
            System.gc();
        }
        if (TRACE_ON)
            out.println("Test completed.");

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
        out.println("# Nevertheless, printing could cause OutOfMemoryError even");
        out.println("# after all the memory allocated by the test is released.");
        out.println("# ");
        out.println("# That problem is caused by the known JDK/HotSpot bugs:");
        out.println("#     4239841 (P1/S5) 1.1: poor garbage collector performance");
        out.println("#     4245060 (P4/S5) poor garbage collector performance");
        out.println("# ");
        out.println("# This message is just intended to work-around that problem.");
        out.println("# If printing should fail even so, the test will return the");
        out.println("# exit status 96 instead of 97 to indicate the problem.");

        // run all tests normally to ensure all needed classes are loaded and
        // initialized before the heap is exhausted - else we may trigger OOME
        // in unexpected places.
        try {
            if (TRACE_ON)
                out.println("Running without heap exhaustion");
            runTests(out, false);
        } catch (Throwable unexpected) {
            out.println("Test pre-initialisation failed: " + unexpected);
            return 2;
        }

        if (TRACE_ON)
            out.println("Running with heap exhaustion");

        return runTests(out, true);
    }

    private static int runTests(PrintStream out, boolean exhaustHeap) {
        // reset message index
        messages = 0;

        // Prepare some items, which will be used by the test:
        Object stringArray[] = new String[1];
        Object integerValue = Integer.valueOf(0);
        Object doubleValue = Double.valueOf(0);
        Object trash = null;
        Field abraIntegerField;
        Field abraBooleanField;
        Field extPrivateField;
        try {
            abraIntegerField = Abra.class.getDeclaredField("MAIN_CYR_NUMBER");
            abraBooleanField = Abra.class.getDeclaredField("NOT_AN_INTEGER");
            extPrivateField = Ext.class.getDeclaredField("DONT_TOUCH_ME");
        } catch (NoSuchFieldException nsfe) {
            out.println("Test initialisation failed: field not found: " + nsfe.getMessage());
            return 2;
        }

        Abra abra = new Abra("via public constructor");
        Abra.Cadabra cadabra = new Abra.Cadabra();
        // Sum up exit code:
        int exitCode = 0; // apparently PASSED
        int skipped = 0;  // some checks may correctly suffer OutOfMemoryError

        int poolSize = 0;
        int index = 0;

        if (exhaustHeap) {
            pool = null;
            // Allocate repository for lots of tiny objects:
            for (int size = 1 << 30; size > 0 && pool == null; size >>= 1) {
                try {
                    pool = new Object[size];
                } catch (OutOfMemoryError oome) {
                }
            }
            if (pool == null)
                throw new Error("HS bug: cannot allocate new Object[1]");
            poolSize = pool.length;
            index = 0;

            // Sum up time spent, when it was hard for JVM to allocate next object
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
                // Do not release any byte once allocated:
                pool[index++] = oome;
            }

            if (index > poolSize - 1000) {
                if (WARN_ON)
                    log[messages++] = "Warning: pool[] is full; so, checks would not be enough hard...";
            }
        } else {
            // pool gets used for array index tests
            pool = new Object[3];
            poolSize = pool.length;
        }

        // Check ArithmeticException:
        try {
            int x, y, z;
            x = y = 0;
            z = x / y;
            log[messages++] = "Failure: ArithmeticException";
            exitCode = 2; // FAILED
        } catch (ArithmeticException ae) {
            if (TRACE_ON)
                log[messages++] = "Success: ArithmeticException";
            if (exhaustHeap)
                pool[index++] = ae;
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: ArithmeticException";
            skipped++;
        }

        // Check ArrayIndexOutOfBoundsException:
        try {
            pool[poolSize] = pool[0];
            log[messages++] = "Failure: ArrayIndexOutOfBoundsException";
            exitCode = 2; // FAILED
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            if (TRACE_ON)
                log[messages++] = "Success: ArrayIndexOutOfBoundsException";
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: ArrayIndexOutOfBoundsException";
            skipped++;
        }

        // Check ArrayStoreException:
        try {
            stringArray[0] = integerValue;
            log[messages++] = "Failure: ArrayStoreException";
            exitCode = 2; // FAILED
        } catch (ArrayStoreException ase) {
            if (TRACE_ON)
                log[messages++] = "Success: ArrayStoreException";
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: ArrayStoreException";
            skipped++;
        }

        // Check ClassCastException:
        try {
            trash = (Double) integerValue;
            log[messages++] = "Failure: ClassCastException";
            exitCode = 2; // FAILED
        } catch (ClassCastException cce) {
            if (TRACE_ON)
                log[messages++] = "Success: ClassCastException";
            if (exhaustHeap)
                pool[index++] = cce;
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: ClassCastException";
            skipped++;
        }

        // Check CloneNotSupportedException:
        try {
            trash = abra.clone();    // illegal - should fail
//          trash = cadabra.clone(); //   legal - should pass
            log[messages++] = "Failure: CloneNotSupportedException";
            exitCode = 2; // FAILED
        } catch (CloneNotSupportedException cnse) {
            if (TRACE_ON)
                log[messages++] = "Success: CloneNotSupportedException";
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: CloneNotSupportedException";
            skipped++;
        }

        // Check IllegalAccessException (positive):
        try {
            int junkIt = abraIntegerField.getInt(null); //   legal - should pass
            if (TRACE_ON)
                log[messages++] = "Success: IllegalAccessException (positive)";
        } catch (IllegalAccessException iae) {
            log[messages++] = "Failure: IllegalAccessException (positive)";
            exitCode = 2;
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: IllegalAccessException (positive)";
            skipped++;
        }

        // Check IllegalAccessException (negative):
        try {
            int junkIt = extPrivateField.getInt(null); // illegal - should fail
            log[messages++] = "Failure: IllegalAccessException (negative)";
            exitCode = 2; // FAILED
        } catch (IllegalAccessException iae) {
            if (TRACE_ON)
                log[messages++] = "Success: IllegalAccessException (negative)";
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: IllegalAccessException (negative)";
            skipped++;
        }

        // Check IllegalArgumentException (positive):
        try {
            int junkIt = abraIntegerField.getInt(null); //   legal - should pass
//          int junkIt = abraBooleanField.getInt(null); // illegal - should fail
            if (TRACE_ON)
                log[messages++] = "Success: IllegalArgumentException (positive)";
        } catch (IllegalAccessException iae) {
            log[messages++] =
                    "Failure: IllegalArgumentException (positive) incorrectly thrown IllegalAccessException";
            exitCode = 2;
        } catch (IllegalArgumentException iae) {
            log[messages++] = "Failure: IllegalArgumentException (positive)";
            exitCode = 2;
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: IllegalArgumentException (positive)";
            skipped++;
        }

        // Check IllegalArgumentException (negative):
        try {
//          int junkIt = abraIntegerField.getInt(null); //   legal - should pass
            int junkIt = abraBooleanField.getInt(null); // illegal - should fail
            log[messages++] = "Failure: IllegalArgumentException (negative)";
            exitCode = 2; // FAILED
        } catch (IllegalAccessException iae) {
            log[messages++] =
                    "Failure: IllegalArgumentException (negative) incorrectly thrown IllegalAccessException";
            exitCode = 2;
        } catch (IllegalArgumentException iae) {
            if (TRACE_ON)
                log[messages++] = "Success: IllegalArgumentException (negative)";
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: IllegalArgumentException (negative)";
            skipped++;
        }

        // Check IllegalMonitorStateException (positive):
        try {
            synchronized (cadabra) {
                cadabra.notifyAll();    //   legal - should pass
            }
//          cadabra.notifyAll();        // illegal - should fail
            if (TRACE_ON)
                log[messages++] = "Success: IllegalMonitorStateException (positive)";
        } catch (IllegalMonitorStateException imse) {
            log[messages++] = "Failure: IllegalMonitorStateException (positive)";
            exitCode = 2;
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: IllegalMonitorStateException (positive)";
            skipped++;
        }

        // Check IllegalMonitorStateException (negative):
        try {
//          synchronized (cadabra) {
//              cadabra.notifyAll();    //   legal - should pass
//          }
            cadabra.notifyAll();        // illegal - should fail
            log[messages++] = "Failure: IllegalMonitorStateException (negative)";
            exitCode = 2;
        } catch (IllegalMonitorStateException imse) {
            if (TRACE_ON)
                log[messages++] = "Success: IllegalMonitorStateException (negative)";
        } catch (OutOfMemoryError oome) {
            if (WARN_ON)
                log[messages++] = "Skipped: IllegalMonitorStateException (negative)";
            skipped++;
        }

        return exitCode;
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
                        if (e instanceof RuntimeException)
                            throw (RuntimeException) e;
                        else if (e instanceof Error)
                            throw (Error) e;
                        else
                            throw new Error("Unexpected checked exception", e);
                    }
                } catch (OutOfMemoryError oome) {
                }
            }
        });
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
        // JCK-like exit status.
    }

    /**
     * This class should be used to check <code>CloneNotSupportedException</code>,
     * and <code>IllegalArgumentException</code>.
     * The class extends <code>except004</code> in order that its (protected)
     * method <code>clone()</code> be available from <code>except004</code>.
     */
    private static class Abra extends except004 {
        /**
         * Will try to incorrectly find this class as <code>Cadabra</code>
         * instead of <code>Abra$Cadabra</code>.
         */
        public static class Cadabra implements Cloneable {
        }

        /**
         * Will try to incorrectly access to this field from outside this class.
         */
        public static final int MAIN_CYR_NUMBER = 47;
        /**
         * Will try to get this field like <code>int<code> zero.
         */
        public static final boolean NOT_AN_INTEGER = false;

        /**
         * Will try to correctly instantiate <code>Abra.Cadabra</code>,
         * not <code>Abra</code>.
         */
        private Abra() {
        }

        /**
         * Yet another constructor, which is <code>public</code>.
         */
        public Abra(String nothingSpecial) {
        }
    }
}

/* Package accessible class that has non-accessible private member */
class Ext {
    /**
     * Will try to incorrectly access to this field from outside this class.
     */
    private static final int DONT_TOUCH_ME = 666;
}
