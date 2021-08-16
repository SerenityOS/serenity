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
 * @summary converted from VM testbase nsk/stress/except/except001.
 * VM testbase keywords: [stress, diehard, slow, nonconcurrent, quick]
 * VM testbase readme:
 * DESCRIPTION
 *     This checks if OutOfMemoryError exception is correctly enwrapped into
 *     InvocationTargetException when thrown inside a method invoked via
 *     reflection.
 *     The test tries to occupy all of memory available in the heap by
 *     allocating lots of new Object() instances. Instances of the "empty"
 *     type Object are the smallest objects, so they apparently should occupy
 *     most fine-grained fragments in the heap. Thus, there apparently should
 *     not remain any free space to incarnate new Throwable instance, and VM
 *     possibly could crash while trying to throw new OutOfMemoryError and
 *     enwrap it into new InvocationTargetException instance.
 *     By the way, the test checks time elapsed to allocate memory. Both
 *     classic VM and HotSpot seem to fall into poor performance of memory
 *     allocation when heap is almost over. E.g.: HotSpot 1.3-betaH may spend
 *     more than 1 minute to allocate next Object in this case (tested on
 *     Pentium-II, 350MHz, 128Mb RAM). To avoid this problem, the test enforce
 *     OutOfMemoryError if more then 5 minutes is spent to allocate "last bytes"
 *     of memory.
 * COMMENTS
 *     HotSpot releases 1.0-fcsE (both Win32 and Sparc), and 1.3-betaH (Win32)
 *     fail on this test due to poor performance of memory allocation.
 *         #4248801 (P3/S5) slow memory allocation when heap is almost exhausted
 *     Despite this bug is treated fixed in HotSpot 1.0.1, it still does suffer
 *     slow memory allocation when running on PC having 64Mb or less of RAM.
 *     There is also a workaround involved to avoid the following bugs known
 *     for HotSpot and for classic VM:
 *         #4239841 (P1/S5) 1.1: poor garbage collector performance  (HotSpot bug)
 *         #4245060 (P4/S5) poor garbage collector performance       (Classic VM bug)
 *     However, printing of the test's error messages, warnings, and of execution
 *     trace may fail under JDK 1.2 for Win32 even so.
 *     HotSpot 2.0-devA (Win32) crashes due to the known HotSpot bug:
 *         #4239828 (P1/S4) 1.3c1: VM crashes when heap is exhausted
 *
 * @run main/othervm -Xms50M -Xmx200M nsk.stress.except.except001
 */

package nsk.stress.except;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This checks if <code>OutOfMemoryError</code> exception is correctly
 * enwrapped into <code>InvocationTargetException</code> when thrown inside
 * a method invoked via reflection.
 * <p>
 * <p>The test tries to occupy all of memory available in the heap by
 * allocating lots of new <code>Object()</code> instances. Instances of the
 * ``empty'' type <code>Object</code> are the smallest objects, so they
 * apparently should occupy most fine-grained fragments in the heap.
 * Thus, there apparently should not remain any free space to incarnate new
 * <code>Throwable</code> instance, and VM possibly could crash while trying
 * to throw new <code>OutOfMemoryError</code> and enwrap it into new
 * <code>InvocationTargetException</code> instance.
 * <p>
 * <p>By the way, the test checks time elapsed to allocate memory.
 * Both classic VM and HotSpot seem to fall into poor performance of memory
 * allocation when heap is almost over. E.g.: HotSpot 1.3-betaH may spend
 * more than 1 minute to allocate next <code>Object</code> in this case
 * (tested on Pentium-II, 350MHz, 128Mb RAM). To workaround this problem,
 * the test enforces <code>OutOfMemoryError</code> if more then 5 minutes
 * is spent to allocate ``last bytes'' of the memory.
 */
public class except001 {
    /**
     * This field allows or supresses printing with <code>display()</code>
     * method.
     *
     * @see #display(Object)
     * @see #complain(Object)
     * @see #out
     */
    private static boolean MODE_VERBOSE = true;
    /*
    * Storage for a lot of tiny objects
    * "static volatile" keywords are for preventing heap optimization
    */
    private static volatile Object pool[] = null;

    /**
     * Print execution trace if <code>MODE_VERBOSE</code> is <code>true</code>
     * (optional).
     *
     * @see #MODE_VERBOSE
     * @see #complain(Object)
     * @see #out
     */
    private static void display(Object message) {
        if (MODE_VERBOSE)
            out.println(message.toString());
        out.flush();
    }

    /**
     * Print error <code>message</code>.
     *
     * @see #display(Object)
     * @see #out
     */
    private static void complain(Object message) {
        out.println("# " + message);
        out.flush();
    }

    /**
     * The log-stream assigned at runtime by the method
     * <code>run(args,out)</code>.
     *
     * @see #display(Object)
     * @see #complain(Object)
     * @see #run(String[], PrintStream)
     */
    private static PrintStream out;

    /**
     * Try to allocate lots of instances of the type <code>Object</code>.
     * Such instances are most fine-grained, and thus they should occupy
     * smallest fragments of free memory in the heap.
     * <p>
     * <p>By the way, break the test, if JVM has spent more than
     * 5 minutes to allocate latest portions of memory.
     */
    public static void raiseOutOfMemory() throws OutOfMemoryError {
        try {
            // Repository for objects, which should be allocated:
            int index = 0;
            for (int size = 1 << 30; size > 0 && pool == null; size >>= 1)
                try {
                    pool = new Object[size];
                } catch (OutOfMemoryError oome) {
                }
            if (pool == null)
                throw new Error("HS bug: cannot allocate new Object[1]");

            // Sum up time spent, when it was hard to JVM to allocate next object
            // (i.e.: when JVM has spent more than 1 second to allocate new object):
            double totalDelay = 0;
            long timeMark = System.currentTimeMillis();

            for (; index < pool.length; index++) {
                //-------------------------
                pool[index] = new Object();
                long nextTimeMark = System.currentTimeMillis();
                long elapsed = nextTimeMark - timeMark;
                timeMark = nextTimeMark;
                //----------------------
                if (elapsed > 1000) {
                    double seconds = elapsed / 1000.0;
                    display(
                            "pool[" + index +
                                    "]=new Object(); // elapsed " + seconds + "s");
                    totalDelay += seconds;
                    if (totalDelay > 300) {
                        complain(
                                "Memory allocation became slow: so heap seems exhausted.");
                        throw new OutOfMemoryError();
                    }
                }
            }

            // This method should never return:
            throw new Error("TEST_BUG: failed to provoke OutOfMemoryError");
        } finally {
            // Make sure there will be enough memory for next object allocation
             pool = null;
        }
    }

    /**
     * Invoke the method <code>raiseOutOfMemory()</code> with reflection,
     * and check if the exception it throws is just
     * <code>OutOfMemoryError</code> enwrapped into
     * <code>InvocationTargetException</code> instance.
     * <p>
     * <p>Before the test begins, <code>this.out</code> filed is assigned
     * to the parameter <code>out</code>. Parameter <code>args[]</code>
     * is ignored.
     *
     * @see #raiseOutOfMemory()
     */
    public static int run(String args[], PrintStream out) {
        out.println("# While printing this message, JVM seems to initiate the output");
        out.println("# stream, so that it will not need more memory to print later,");
        out.println("# when the heap would fail to provide more memory.");
        out.println("# ");
        out.println("# That problem is caused by the known JDK/HotSpot bugs:");
        out.println("#     4239841 (P1/S5) 1.1: poor garbage collector performance");
        out.println("#     4245060 (P4/S5) poor garbage collector performance");
        out.println("# ");
        out.println("# This message is just intended to work-around that problem.");
        out.println("# If printing should fail even so.");

        if (args.length > 0) {
            if (args[0].toLowerCase().startsWith("-v"))
                MODE_VERBOSE = true;
        }

        except001.out = out;
        Class testClass = except001.class;
        try {
            Method testMethod = testClass.getMethod("raiseOutOfMemory", new Class [0]);
            Object junk = testMethod.invoke(null, new Object [0]);

        } catch (InvocationTargetException ite) {
            Throwable targetException = ite.getTargetException();
            if (targetException instanceof OutOfMemoryError) {
                display("OutOfMemoryError thrown as expected.");
                display("Test passed.");
                return 0;
            }
            complain("Unexpected InvocationTargetException: " + targetException);
            complain("Test failed.");
            return 2;

        } catch (Exception exception) {
            complain("Unexpected exception: " + exception);
            complain("Test failed.");
            return 2;
        }
        //
        complain("The test has finished unexpectedly.");
        complain("Test failed.");
        return 2;
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
                    System.gc();
                    if (e instanceof OutOfMemoryError) {
                        try {
                            System.out.println("OOME : Test Skipped");
                            System.exit(95);
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
