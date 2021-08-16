/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6302839
 * @summary SecurityRace System field accesses in two threads
 * @author Pete Soper
 * @build SecurityRace
 * @run main/othervm/policy=System.policy SecurityRace
 */

/*
 * By default the test runs for a very short time.  Use main arg "stress"
 * to have a real chance of exposing races. Use main arg "time" to report
 * the average nanoseconds per invocation of System.setSecurityManager and
 * System.getSecurityManager. No testing for access races is performed with
 * argument "time."
 *
 * Requires security permissions "setSecurityManager" and
 * "createSecurityManager."
 */



public class SecurityRace implements Runnable {

    // Number of iterations to "warm up" and get methods compiled/inlined.
    // (this is conservative)
    static final int WARMUP_LOOPS = 100000;

    // Number of timing trials
    static final int TIMING_TRIALS = 10;

    // Seconds to run this in "stress" mode. This is double the average
    // time to expose the races of bug 6302839 on a Blade 1000. Invoke
    // the program with the "stress" option multiple times for more
    // confidence.
    static final int STRESS_MILLISECONDS = 300000;
    static final int SET_TIMING_LOOPS    = 10000;

    // Max seconds to run before terminating the test ("declaring victory").
    static int MAX_MILLISECONDS = 100;

    // Number of iterations to time
    static final int GET_TIMING_LOOPS = 10000000;

    // Set true by main thread when NPE caught or time to terminate.
    // Set true by other thread when NPE caught. It makes
    // no difference where the NPE is thrown.
    static volatile boolean stopthreads = false;

    // Number of getProperty invocations between main loop checks
    static final int       GETPROPERTY_LOOPS = 30000;

    // Used by race and timing tests. Must get set non-null at lease once.
    static SecurityManager sm = new SecurityManager();

    public static void main(String[] argv) throws Exception {
        String s;

        if (argv.length > 0) {
            if (argv[0].equals("time")) {

                // Run the timing method
                // First warm up the method to make sure it gets compiled
                for (int i = 0; i < WARMUP_LOOPS; i++) {
                    timeit(1, 1, 1);
                }

                System.out.println("boo");

                // Now do the actual timing
                timeit(TIMING_TRIALS, GET_TIMING_LOOPS, SET_TIMING_LOOPS);
            } else if (argv[0].equals("stress")) {

                // For stress test the test duration is boosted
                MAX_MILLISECONDS = STRESS_MILLISECONDS;
            } else {
                throw new RuntimeException(
                    "SecurityRace: " + argv[0]
                    + " argument to main not recognized");
            }    // if argv
        }        // if length

        long start = System.currentTimeMillis(),
             end   = start + MAX_MILLISECONDS;

        // Create and start racing thread
        (new Thread(new SecurityRace())).start();

        // main thread alternates batches of getProperty() with time checks
        try {
            do {
                if (stopthreads) {

                    // other thread suffered an NPE
                    throw new RuntimeException("SecurityRace failed with NPE");
                }

                for (int i = 0; i < GETPROPERTY_LOOPS; i++) {
                    s = System.getProperty("java.version");
                }
            } while (System.currentTimeMillis() < end);
        } catch (NullPointerException e) {
            throw new RuntimeException("SecurityRace failed with NPE");
        } finally {

            // make sure other thread terminates
            stopthreads = true;
        }
    }    // main

    // System.security mutator.
    public void run() {
        try {
            while (true) {
                if (stopthreads) {
                    return;
                }

                System.setSecurityManager(sm);

                // The goal is to catch another thread testing the
                // value set above and trying to use it after it's
                // nulled below.
                System.setSecurityManager(null);
            }
        } catch (NullPointerException e) {
            stopthreads = true;

            return;
        }
    }

    // Time method execution. Collects trials number of timings
    // for the number of accessor and mutator invocation loops
    // specified.
    public static void timeit(int timing_trials, int get_timing_loops,
                              int set_timing_loops) {
        try {
            long start;

            // Time the methods and report average.
            // Time multiple trials so noise is apparent and a
            // T test can be used to establish significance.
            for (int j = 0; j < timing_trials; j++) {
                start = System.nanoTime();

                for (int i = 0; i < get_timing_loops; i++) {
                    sm = System.getSecurityManager();
                }

                // Don't print for "warmup" case. This might mean that
                // the compiler fails to compile the println (setting it
                // up to execute via interpretation using an "uncommon trap")
                // but we don't care if this println runs slowly!
                if (timing_trials > 1) {
                    System.out.println((float) (System.nanoTime() - start)
                                       / (float) get_timing_loops);
                }
            }

            for (int j = 0; j < timing_trials; j++) {
                start = System.nanoTime();

                for (int i = 0; i < set_timing_loops; i++) {
                    System.setSecurityManager(sm);
                }

                if (timing_trials > 1) {
                    System.out.println((float) (System.nanoTime() - start)
                                       / (float) set_timing_loops);
                }
            }

            return;
        } catch (Exception e) {
            throw new RuntimeException("SecurityRace got unexpected: " + e);
        }
    }    // timeit
}    // SecurityRace
