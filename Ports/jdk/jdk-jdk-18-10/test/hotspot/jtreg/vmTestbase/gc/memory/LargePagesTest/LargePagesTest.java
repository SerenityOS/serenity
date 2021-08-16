/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/memory/LargePagesTest.
 * VM Testbase keywords: [gc, stress, stressopt]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.memory.LargePagesTest.LargePagesTest noThreads=5 duration=60
 */

package gc.memory.LargePagesTest;

import java.io.PrintStream;
import nsk.share.test.LocalRandom;

/*
 * Allocators purpose is to create pressure on the garbage collector
 * for a certain amount of time.
 * Note: this test moved from the "jr", the original name is func.vm.largepages.LargePagesTest
 */


/**
 * @run main/othervm/timeout=150
 *      -XX:+UseLargePages
 *      -Xmx64m
 *      -Xms16m
 *      LargePagesTest
 *      noThreads=5 duration=60
 * @summary heap shrink/grow test
 */
final public class LargePagesTest extends Thread {

    private static int cnt;
    private static final int SMALL_OBJECT_ALLOCATER = 1;
    private static final int LARGE_OBJECT_ALLOCATER = 2;
    private static final int ANY_OBJECT_ALLOCATER = 3;
    private static final int ANY_NO_MULTIARRAYS_ALLOCATER = 4;
    private int myType;

    /** Dummy thingies to update. */
    public LargePagesTest.Dummy[] d0;
    /** Dummy thingies to update. */
    public LargePagesTest.Dummy[] d1;
    /** Dummy thingies to update. */
    public LargePagesTest.Dummy[][] d2;
    /** Dummy thingies to update. */
    public LargePagesTest.Dummy[][] d3;
    /** Dummy thingies to update. */
    public LargePagesTest.Dummy[][][] d4;
    /** Dummy thingies to update. */
    public LargePagesTest.Dummy d5;

    /** Time to run execute(). */
    private long duration;

    /** Boolean of progress should be printed. */
    private boolean verbose;

    private static int noThreads = 5;

    /** Iterations. */
    public long iterations = 0L;

    /** Result. */
    public boolean result = false;

    private PrintStream out;

    /**
     * Creates DurAllocator.
     * @param name Parameter
     * @param duration Parameter
     * @param out Parameter
     * @param verbose Parameter
     */
    LargePagesTest(String name, long duration, PrintStream out, boolean verbose) {

        super(name);
        this.duration = duration;
        this.out = out;
        this.verbose = verbose;
    }

    /**
     * Print status.
     */
    void describe() {
        out.println("DurAllocator run: ");
        out.println("   test duration:     " + duration / 1000 + " seconds");
        out.println("   number of threads: " + noThreads + " threads");
    }

    /**
     * Allocates memory in a loop.
     */
    public void run() {

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < duration) {
            try {
                allocate();
            } catch (Throwable t) {
                out.println(getName() + " FAILED: " + t.getClass().getName() + " in iteration " + iterations + ": " + t.getMessage());
                return;
            }
            iterations++;
            if (verbose && iterations % 1000 == 0) {
                out.print(".");
            }
            if (verbose && iterations % 60000 == 0) {
                out.println(".");
            }
        }
        if (verbose) {
            out.println("");
        }


        long endTime = System.currentTimeMillis();
        long runTime = endTime - startTime;
        if (duration > runTime) {
            out.println(getName() + "  FAILED: Execution time < requested execution time.");
            out.println("                execution time is " + runTime);
            out.println("                requested time is " + duration);
        } else if (iterations <= 0) {
            out.println(getName() + "  FAILED: No executions finished");
        } else {
            result = true;
        }
    }

    private void allocate() {
        for (int j = 0; j < 1000; j++) {
            int i = 0;

            switch (myType) {
            case SMALL_OBJECT_ALLOCATER:
                i = 5;
                break;
            case LARGE_OBJECT_ALLOCATER:
                i = 1;
                break;
            case ANY_OBJECT_ALLOCATER:
                i = LocalRandom.nextInt(100);
                break;
            case ANY_NO_MULTIARRAYS_ALLOCATER:
                i = LocalRandom.nextInt(100);
                if ((i >= 2) && (i <= 4)) {
                    i = 5;
                }
                break;
            default:
                break;
            }

            switch (i) {
            case 0:
                d0 = new LargePagesTest.Dummy[10];
                break;
            case 1:
                d1 = new LargePagesTest.Dummy[1000];
                break;
            case 2:
                d2 = new LargePagesTest.Dummy[10][10];
                break;
            case 3:
                d3 = new LargePagesTest.Dummy[100][100];
                break;
            case 4:
                d4 = new LargePagesTest.Dummy[10][10][10];
                break;
            default:
                d5 = new LargePagesTest.Dummy();
                break;
            }
        }
    }

    /**
     * A Dummy class.
     */
    private class Dummy {
        /**
         * Creates a dummy.
         */
        Dummy() {
            cnt++;
        }
    }

    /**
     * @param args Input arguments
     */
    public static void main(String[] args) {

        long duration = 30 * 60 * 1000;
        PrintStream out = System.out;
        boolean verbose = true;

        for (int i = 0; i < args.length; i++) {
            String noThreadsArgName = "noThreads=";
            String executionTimeArgName = "duration=";
            String verboseArgName = "verbose=";
            if (args[i].indexOf(noThreadsArgName) != -1) {
                noThreads = Integer.parseInt(args[i].substring(noThreadsArgName.length(), args[i].length()));
            } else if (args[i].indexOf(executionTimeArgName) != -1) {
                duration = 1000 * Long.parseLong(args[i].substring(executionTimeArgName.length(), args[i].length()));
            } else if (args[i].indexOf(verboseArgName) != -1) {
                verbose = Boolean.parseBoolean(args[i].substring(verboseArgName.length(), args[i].length()));
            } else {
                System.out.println("ERROR: Unknown argument string: " + args[i]);
                System.exit(-1);
            }
        }

        // Please don't...
        if (noThreads <= 0) {
            noThreads = 1;
        }

        LargePagesTest[] runners = new LargePagesTest[noThreads];

        for (int i = 0; i < noThreads; i++) {
            runners[i] = new LargePagesTest("DurAllocator " + i, duration, out, verbose);
        }

        runners[0].describe();

        for (int i = 0; i < noThreads; i++) {
            runners[i].start();
        }

        for (int i = 0; i < noThreads; i++) {
            try {
                runners[i].join(duration + 10 * 60 * 1000);
            } catch (InterruptedException e) {
                out.println(runners[i].getName() + " FAILED: " + e.getClass().getName() + " " + e.getMessage());
                System.exit(-1);
            }
        }

        for (int i = 0; i < noThreads; i++) {
            if (!runners[i].result) {
                out.println(runners[i].getName() + " FAILED: status=" + runners[i].result);
                System.exit(-1);
            }
        }

        if (verbose) {
            out.println();
        }

        out.print("DurAllocator PASSED with (");
        for (int i = 0; i < noThreads; i++) {
            out.print("" + runners[i].iterations + (i + 1 < noThreads ? "+" : ""));
        }
        out.println(") iterations.");
        // System.exit(90); // use to return 90 as indication of success
    }

}
