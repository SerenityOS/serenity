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

package nsk.share.gc;

import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.share.gc.gp.*;
import nsk.share.test.ExecutionController;

/**
 * <tt>Algorithms</tt> class collects main algorithms that are used in
 * GC testing.
 */
public class Algorithms {
        /** Number of threads that one CPU can manage. */
        public final static long THREADS_MANAGED_BY_ONE_CPU = 100;

        /** Number of threads that one block of memory can manage. */
        public final static long THREADS_MANAGED_BY_ONE_BLOCK = 200;

        /** Default maximum number of elements in array. */
        public final static int MAX_ARRAY_SIZE = 65535;

        // Block of memory is 64M
        private final static long BLOCK_SIZE = 64 * 1024 * 1024; // 64M

        // Minimal memory chunk size to eat
        private final static int MIN_MEMORY_CHUNK = 512; // Bytes

        // Number of attempts to print a string. Print may fail because of
        // OutOfMemoryError, so this constat specifies the number of attemts
        // to print despite of OOME.
        private final static int ATTEMPTS_TO_PRINT = 3;

        // This object stores any Failure that is thrown in Gourmand class
        // and used in eatMemory(int) method
        private static Failure failure = null;

        // This object is intended for wait()
        private static Object object = new Object();

        /**
         * Default constructor.
         */
        private Algorithms() {}

        /**
         * Stresses memory by allocating arrays of bytes. The method allocates
         * objects in the same thread and does not invoke GC explicitly by
         * calling <tt>System.gc()</tt>.
         * <p>
         *
         * Note that this method can throw Failure if any exception
         * is thrown while eating memory. To avoid OOM while allocating
         * exception we preallocate it before the lunch starts. It means
         * that exception stack trace does not correspond to the place
         * where exception is thrown, but points at start of the method.
         *
         * This method uses nsk.share.test.Stresser class to control
         * it's execution. Consumed number of iterations depends on
         * available memory.
         *
         * @throws <tt>nsk.share.Failure</tt> if any unexpected exception is
         * thrown during allocating of the objects.
         *
         * @see nsk.share.test.Stresser
         */
        public static void eatMemory(ExecutionController stresser) {
            GarbageUtils.eatMemory(stresser, 50, MIN_MEMORY_CHUNK, 2);
        }

        /**
         * Calculates and returns recomended number of threads to start in the
         * particular machine (with particular amount of memory and number of CPUs).
         * The returned value is minimum of two values:
         * {@link #THREADS_MANAGED_BY_ONE_CPU} * (number of processors) and
         * {@link #THREADS_MANAGED_BY_ONE_BLOCK} * (number of blocks of the memory).
         *
         * @return recomended number of threads to start.
         *
         */
        public static int getThreadsCount() {
                Runtime runtime = Runtime.getRuntime();
                int processors = runtime.availableProcessors();
                long maxMemory = runtime.maxMemory();
                long blocks = Math.round((double) maxMemory / BLOCK_SIZE);

                return (int) Math.min(THREADS_MANAGED_BY_ONE_CPU * processors,
                                THREADS_MANAGED_BY_ONE_BLOCK * blocks);
        }

        /**
         * Returns the number of processors available to the Java virtual machine.
         *
         * @return number of processors available to the Java virtual machine.
         *
         * @see Runtime#availableProcessors
         *
         */
        public static int availableProcessors() {
                Runtime runtime = Runtime.getRuntime();
                return runtime.availableProcessors();
        }

        /**
         * Makes a few attempts to print the string into specified PrintStream.
         * If <code>PrintStream.println(String)</code> throws OutOfMemoryError,
         * the method waits for a few milliseconds and repeats the attempt.
         *
         * @param out PrintStream to print the string.
         * @param s the string to print.
         */
        public static void tryToPrintln(PrintStream out, String s) {
                for (int i = 0; i < ATTEMPTS_TO_PRINT; i++) {
                        try {
                                out.println(s);

                                // The string is printed into the PrintStream
                                return;
                        } catch (OutOfMemoryError e) {

                                // Catch the error and wait for a while
                                synchronized(object) {
                                        try {
                                                object.wait(500);
                                        } catch (InterruptedException ie) {

                                                // Ignore the exception
                                        }
                                } // synchronized
                        }
                }
        } // tryToPrintln()

        /**
         * Makes a few attempts to print each stack trace of <code>Throwable</code>
         * into specified PrintStream. If <code>PrintStream.println(String)</code>
         * throws OutOfMemoryError, the method waits for a few milliseconds and
         * repeats the attempt.
         *
         * @param out PrintStream to print the string.
         * @param t the throwable to print.
         *
         * @see #tryToPrintln
         */
        public static void tryToPrintStack(PrintStream out, Throwable t) {
                StackTraceElement[] trace = t.getStackTrace();

                for (int i = 0; i < trace.length; i++) {
                        for (int j = 0; j < ATTEMPTS_TO_PRINT; j++) {
                                try {
                                        out.println(trace[i].toString());

                                        // The string is printed into the PrintStream
                                        return;
                                } catch (OutOfMemoryError e) {

                                        // Catch the error and wait for a while
                                        synchronized(object) {
                                                try {
                                                        object.wait(500);
                                                } catch (InterruptedException ie) {

                                                        // Ignore the exception
                                                }
                                        } // synchronized
                                } // try
                        } // for j
                } // for i
        } // tryToPrintStack()

        /**
         * Returns recommended size for an array. Default implemetation returns
         * minimum between <code>size</code> and
         * {@link #MAX_ARRAY_SIZE MAX_ARRAY_SIZE}.
         *
         * @return recommended size for an array.
         *
         */
        public static int getArraySize(long size) {
                long min = Math.min(MAX_ARRAY_SIZE, size);
                return (int) min;
        } // getArraySize()
} // class Algorithms
