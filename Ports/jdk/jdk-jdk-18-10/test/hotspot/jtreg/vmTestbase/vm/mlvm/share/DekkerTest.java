/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import nsk.share.Log.TraceLevel;
import vm.share.options.Option;

/**
 * The test looks for late CPU stores, which is not visible for other CPU.
 * <p>
 * In absence of synchronization (such as memory barriers and so on), typical modern CPUs
 * (Intel x86 32/64, SPARC in TSO mode) put stores into buffer instead of immediately writing them to memory.
 *
 * So the following program:
 * <code>
 *    write A
 *    read B
 * </code>
 * can actually be transformed to and seen by other processors as:
 * <code>
 *    read B
 *    write A
 *  </code>
 * <p>
 * DekkerTest runs two threads, A and B, which perform operations on a shared array of Actors concurrently.
 * Reordering mentioned above is detected by the test and reported as failure
 * (unless mayFail option is specified in the constructor or command-line option).
 *
 * <p>
 * The tests should subclass Actor with test-specific data and code.
 * The Actor subclass requirements:
 *
 * <ul>
 * <li>the class should have two data fields:
 * <code>
 *   A
 *   B
 * </code>
 *   of the same type, which are able hold one of two values.
 *   Let's call these values TRUE and FALSE (these can be any two values, say 0 and 1).
 *
 * <li>* actorA() is called from thread A and should implement the following pseudo-code:
 * <code>
 *   A = TRUE
 *   optional MEMBAR #StoreLoad
 *   return (B == TRUE)
 * </code>
 *
 * <li>actorB() is called from thread B and should do the following:
 * <code>
 *   {
 *       B = TRUE
 *       optional MEMBAR #StoreLoad
 *       return (A == TRUE)
 *   }
 * </code>
 *
 * <li>reset() method should do the following:
 * <code>
 *   {
 *       A = FALSE
 *       B = FALSE
 *   }
 * </code>
 *
 *   The use of memory barriers in actorX() methods is up to the implementor -- depending on the goal of testing.
 *
 */
public class DekkerTest extends MlvmTest {

    @Option(name = "actorClass", default_value = "", description = "Name of actor class (see test comments)")
    private String actorClassNameOpt = "";

    @Option(name = "mayFail", default_value = "false", description = "Don't report test failure (use it just a stress test)")
    private boolean mayFailOpt;

    @Option(name = "iterations", default_value = "1000000", description = "Number of iterations in each Actor thread (i.e., shared array size)")
    private int iterationsOpt = 1000000;

    /**
     * Actor interface, which should be implemented for using with DekkerTest.
     * The requirements for Actor implementation are outlined in {@link DekkerTest} class documentation.
     */
    public interface Actor {
        /**
         * Sets fields A and B to false
         */
        void reset();

        /**
         * Sets field A to true, and returns field B contents
         * @return field B contents
         * @throws Throwable in case of error
         */
        boolean actorA() throws Throwable;

        /**
         * Sets field B to true, and returns field A contents
         * @return field A contents
         * @throws Throwable in case of error
         */
        boolean actorB() throws Throwable;
    }

    private static final class ResultData {
        public boolean a;
        public boolean b;

        public ResultData() {
        }

        public void reset() {
            a = false;
            b = false;
        }
    }

    private static class RaceStatistics {
        public int syncErrors;
        public int runSideBySide;
        public int A_outruns_B;
        public int B_outruns_A;

        public void reset() {
            syncErrors = 0;
            runSideBySide = 0;
            A_outruns_B = 0;
            B_outruns_A = 0;
        }
    }

    private Class<? extends Actor> actorClass;
    private final CyclicBarrier startBarrier = new CyclicBarrier(2); // We have two actors
    private Actor[] testData;
    private ResultData[] results;
    private final RaceStatistics raceStatistics = new RaceStatistics();

    public DekkerTest() {
    }

    /**
     * Sets actor class.
     * @param ac Actor class, which implements DekkerTest.Actor interface
     */
    public void setActorClass(Class<? extends Actor> ac) {
        actorClass = ac;
    }

    /**
     * Sets mayFail option. When set to true, synchronization failure is not reported as test failure (DekkerTest is used as a stress test).
     * @param mayFail if true, don't report sync failure as test failure, false otherwise
     */
    public void setMayFailOpt(boolean mayFail) {
        mayFailOpt = mayFail;
    }

    /**
     * Initializes test data, parses command-line options and checks Actor class
     */
    @Override
    public void initializeTest() {
        // Override values set by setActorClass() by command-line option, if any
        try {
            if (!actorClassNameOpt.isEmpty()) {
                actorClass = Class.forName(actorClassNameOpt).asSubclass(Actor.class);
            } else {
                if (actorClass == null) {
                    throw new RuntimeException("Test error: the actor class should be specified via command-line options or in the constructor");
                }
            }

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Actor class '" + actorClassNameOpt + "' not found", e);
        } catch (ClassCastException e) {
            throw new RuntimeException("Test error: the actor class " + actorClass.getName() + " should implement " + Actor.class.getName() + " interface", e);
        }

        // Generate data
        int iterations = iterationsOpt * getStressOptions().getIterationsFactor();
        if (iterations <= 0) {
            throw new RuntimeException("Invalid number of iterations specified in -iterations and -stressIterationsFactor options: " + iterations);
        }

        results = new ResultData[iterations];
        for (int i = 0; i < results.length; ++i) {
            results[i] = new ResultData();
        }

        testData = new Actor[results.length];
        try {
            for (int i = 0; i < testData.length; ++i) {
                testData[i] = actorClass.newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Test error: can't instantiate class " + actorClass.getName(), e);
        }
    }

    /**
     * Resets the test data between test runs
     */
    @Override
    public void resetTest() {
        for (Actor a : testData) {
            a.reset();
        }
        for (ResultData d : results) {
            d.reset();
        }
    }

    private class RunnerA extends Thread {
        @Override
        public void run() {
            try {
                startBarrier.await();
                for (int i = 0; i < results.length; ++i) {
                    results[i].a = testData[i].actorA();
                }
            } catch (Throwable t) {
                markTestFailed("Exception in RunnerA", t);
            }
        }
    }

    private class RunnerB extends Thread {
        @Override
        public void run() {
            try {
                startBarrier.await();
                for (int i = 0; i < results.length; ++i) {
                    results[i].b = testData[i].actorB();
                }
            } catch (Throwable t) {
                markTestFailed("Exception in RunnerB", t);
            }
        }
    }

    @Override
    public boolean run() throws Throwable {
        RunnerA threadA = new RunnerA();
        threadA.start();

        RunnerB threadB = new RunnerB();
        threadB.start();

        threadA.join();
        threadB.join();

        if (isMarkedFailed())
            return false;

        analyzeResults();
        printResults();

        if (mayFailOpt) {
            return true;
        }

        return (raceStatistics.syncErrors == 0);
    }

    private void analyzeResults() {
        raceStatistics.reset();

        for (int i = 0; i < results.length; ++i) {
            boolean resultA = results[i].a;
            boolean resultB = results[i].b;

            if (resultA && resultB) {

                ++raceStatistics.runSideBySide;

            } else if (resultA && !resultB) {

                ++raceStatistics.A_outruns_B;

            } else if (!resultA && resultB) {

                ++raceStatistics.B_outruns_A;

            } else if (!resultA && !resultB) {

                ++raceStatistics.syncErrors;

            } else {

                throw new RuntimeException("Should not reach here");
            }
        }
    }

    private void printResults() {
        int logLevel = (raceStatistics.syncErrors != 0) ? TraceLevel.TRACE_IMPORTANT : TraceLevel.TRACE_NORMAL;

        Env.getLog().trace(logLevel, "\n"
                + "Late stores (sync. errors): " + raceStatistics.syncErrors + "\n"
                + "B outruns A               : " + raceStatistics.B_outruns_A + "\n"
                + "A outruns B               : " + raceStatistics.A_outruns_B + "\n"
                + "A and B run side by side  : " + raceStatistics.runSideBySide);
    }

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }
}

