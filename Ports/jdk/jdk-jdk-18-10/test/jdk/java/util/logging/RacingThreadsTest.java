/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


/**
 * RacingThreadsTest is a support class for creating a test
 * where multiple threads are needed to exercise a code path.
 * The RacingThreadsTest class is typically used as follows:
 * <ul>
 * <li>
 *     Extend RacingThreadsTest class in order to provide the test
 *     specific variables and/or code, e.g., <br>
 *     public class MyRacingThreadsTest extends RacingThreadsTest
 * <li>
 *     Use
 *     "new MyRacingThreadsTest(name, n_threads, n_loops, n_secs)"
 *     to create your test with the specified name and the specified
 *     number of WorkerThreads that execute the test code in parallel
 *     up to n_loops iterations or n_secs seconds.
 * <li>
 *     Use
 *     "new DriverThread(test)"
 *     to create the test DriverThread that manages all the
 *     WorkerThreads. The DriverThread class can be extended to
 *     provide test specific code and/or variables. However, that
 *     is typically done in your test's subclass.
 * <li>
 *     Use
 *     "new WorkerThread(workerNum, test)"
 *     to create WorkerThread-workerNum that executes the test code.
 *     The WorkerThread class can be extended to provide test thread
 *     specific code and/or variables.
 * <li>
 *     Use
 *     "RacingThreadsTest.runTest(driver, workers)"
 *     to run the test. If the test fails, then a RuntimeException
 *     is thrown.
 * </ul>
 *
 * The RacingThreadsTest class provides many methods that can be
 * overridden in order to provide test specific semantics at each
 * identified test execution point. At a minimum, your test's
 * subclass needs to override the
 * "void executeRace(WorkerThread)"
 * method in order to exercise your race condition and it needs to
 * override the
 * "void checkRaceResults(DriverThread)"
 * method in order to check the results of the race. Your
 * checkRaceResults() method should call the
 * "int incAndGetFailCnt()"
 * method when it detects a failure. It can also call the
 * "void unexpectedException(Thread, Exception)"
 * method if it detects an unexpected exception; this will cause
 * an error message to be output and the failure count to be
 * incremented. When the RacingThreadsTest.runTest() method is
 * done running the races, if there is a non-zero failure count,
 * then a RuntimeException will be thrown.
 * <p>
 * The RacingThreadsTest class uses three internal barriers to
 * coordinate actions between the DriverThread and the WorkerThreads.
 * These barriers should not be managed or used by your test's
 * subclass and are only mentioned here to provide clarity about
 * interactions between the DriverThread and the WorkerThreads.
 * The following transaction diagram shows when the different
 * RacingThreadsTest methods are called relative to the different
 * barriers:
 *
 * <pre>
 * DriverThread           WorkerThread-0         WorkerThread-N-1
 * ---------------------  ---------------------  ---------------------
 * run(workers)
 * oneTimeDriverInit()
 * &lt;start WorkerThreads&gt;  run()                  run()
 * &lt;top of race loop&gt;     :                      :
 * perRaceDriverInit()    oneTimeWorkerInit()    oneTimeWorkerInit()
 * :                      &lt;top of race loop&gt;     &lt;top of race loop&gt;
 * :                      perRaceWorkerInit()    perRaceWorkerInit()
 * startBarrier           startBarrier           startBarrier
 * :                      executeRace()          executeRace()
 * finishBarrier          finishBarrier          finishBarrier
 * checkRaceResults()     :                      :
 * resetBarrier           resetBarrier           resetBarrier
 * perRaceDriverEpilog()  perRaceWorkerEpilog()  perRaceWorkerEpilog()
 * &lt;repeat race or done&gt;  &lt;repeat race or done&gt;  &lt;repeat race or done&gt;
 * :                      oneTimeWorkerEpilog()  oneTimeWorkerEpilog()
 * &lt;join WorkerThreads&gt;   &lt;WorkerThread ends&gt;    &lt;WorkerThread ends&gt;
 * oneTimeDriverEpilog()
 * &lt;DriverThread ends&gt;
 * </pre>
 *
 * Just to be clear about the parallel parts of this infrastructure:
 * <ul>
 * <li>
 *     After the DriverThread starts the WorkerThreads, the DriverThread
 *     and the WorkerThreads are running in parallel until the startBarrier
 *     is reached.
 * <li>
 *     After the WorkerThreads leave the startBarrier, they are running
 *     the code in executeRace() in parallel which is the whole point
 *     of this class.
 * <li>
 *     The DriverThread heads straight to the finishBarrier and waits for
 *     the WorkerThreads to get there.
 * <li>
 *     After the DriverThread leaves the finishBarrier, it checks the
 *     results of the race.
 * <li>
 *     The WorkerThreads head straight to the resetBarrier and wait for
 *     the DriverThread to get there.
 * <li>
 *     If this is not the last race, then after the DriverThread and
 *     WorkerThreads leave the resetBarrier, the DriverThread and the
 *     WorkerThreads are running in parallel until the startBarrier
 *     is reached.
 * <li>
 *     If this is the last race, then after the DriverThread and
 *     WorkerThreads leave the resetBarrier, the DriverThread and the
 *     WorkerThreads are running in parallel as each WorkerThread ends.
 * <li>
 *     The DriverThread waits for the WorkerThreads to end and
 *     then it ends
 * </ul>
 *
 * Once the DriverThread has ended, the RacingThreadsTest.runTest()
 * method checks the failure count. If there were no failures, then
 * a "Test PASSed" message is printed. Otherwise, the failure count
 * is printed, a "Test FAILed" message is printed and a RuntimeException
 * is thrown.
 */
public class RacingThreadsTest {
    /**
     * name of the test
     */
    public final String TEST_NAME;
    /**
     * maximum number of test iterations (race loops)
     */
    public final int N_LOOPS;
    /**
     * the maximum number of seconds to execute the test loop
     */
    public final int N_SECS;
    /**
     * number of WorkerThreads
     */
    public final int N_THREADS;

    /**
     * Creates a test with the specified name and the specified number
     * of WorkerThreads that execute the test code in parallel up to
     * n_loops iterations or n_secs seconds. The RacingThreadsTest
     * class is extended in order to provide the test specific variables
     * and/or code.
     * @param name the name of the test
     * @param n_threads the number of WorkerThreads
     * @param n_loops the maximum number of test iterations
     * @param n_secs the maximum number of seconds to execute the test loop
     */
    RacingThreadsTest(String name, int n_threads, int n_loops, int n_secs) {
        TEST_NAME = name;
        N_THREADS = n_threads;
        N_LOOPS = n_loops;
        N_SECS = n_secs;

        finishBarrier = new CyclicBarrier(N_THREADS + 1);
        resetBarrier = new CyclicBarrier(N_THREADS + 1);
        startBarrier = new CyclicBarrier(N_THREADS + 1);
    }


    /**
     * Entry point for exercising the RacingThreadsTest class.
     */
    public static void main(String[] args) {
        // a dummy test:
        // - 2 threads
        // - 3 loops
        // - 2 seconds
        // - standard DriverThread
        // - standard WorkerThread
        RacingThreadsTest test = new RacingThreadsTest("dummy", 2, 3, 2);
        DriverThread driver = new DriverThread(test);
        WorkerThread[] workers = new WorkerThread[2];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new WorkerThread(i, test);
        }
        test.runTest(driver, workers);
    }

    private static volatile boolean done = false;  // test done flag

    // # of fails; AtomicInteger since any WorkerThread can increment
    private static final AtomicInteger failCnt = new AtomicInteger();
    // # of loops; volatile is OK since only DriverThread increments
    // but using AtomicInteger for consistency
    private static final AtomicInteger loopCnt = new AtomicInteger();
    private static boolean verbose
        = Boolean.getBoolean("RacingThreadsTest.verbose");

    // barriers for starting, finishing and resetting the race
    private final CyclicBarrier finishBarrier;
    private final CyclicBarrier resetBarrier;
    private final CyclicBarrier startBarrier;


    /**
     * Get current done flag value.
     * @return the current done flag value
     */
    public boolean getDone() {
        return done;
    }

    /**
     * Set done flag to specified value.
     * @param v the new done flag value
     */
    public void setDone(boolean v) {
        done = v;
    }

    /**
     * Get current failure counter value.
     * @return the current failure count
     */
    public int getFailCnt() {
        return failCnt.get();
    }

    /**
     * Increment and get current failure counter value.
     * @return the current failure count after incrementing
     */
    public int incAndGetFailCnt() {
        return failCnt.incrementAndGet();
    }

    /**
     * Get current loop counter value.
     * @return the current loop count
     */
    public int getLoopCnt() {
        return loopCnt.get();
    }

    /**
     * Increment and get current loop counter value.
     * @return the current loop count after incrementing
     */
    public int incAndGetLoopCnt() {
        return loopCnt.incrementAndGet();
    }

    /**
     * Get current verbose flag value.
     * @return the current verbose flag value
     */
    public boolean getVerbose() {
        return verbose;
    }

    /**
     * Set verbose flag to specified value.
     * @param v the new verbose flag value
     */
    public void setVerbose(boolean v) {
        verbose = v;
    }

    /**
     * Run the test with the specified DriverThread and the
     * specified WorkerThreads.
     * @param driver the DriverThread for running the test
     * @param workers the WorkerThreads for executing the race
     * @exception RuntimeException the test has failed
     */
    public void runTest(DriverThread driver, WorkerThread[] workers) {
        driver.run(workers);

        try {
            driver.join();
        } catch (InterruptedException ie) {
            unexpectedException(Thread.currentThread(), ie);
            // fall through to test failed below
        }

        if (failCnt.get() == 0) {
            System.out.println(TEST_NAME + ": Test PASSed.");
        } else {
            System.out.println(TEST_NAME + ": failCnt=" + failCnt.get());
            System.out.println(TEST_NAME + ": Test FAILed.");
            throw new RuntimeException("Test Failed");
        }
    }

    /**
     * Helper method for reporting an unexpected Exception and
     * calling incAndGetFailCnt();
     * @param t the Thread that caught the exception
     * @param e the Exception that was caught
     */
    public void unexpectedException(Thread t, Exception e) {
        System.err.println(t.getName() + ": ERROR: unexpected exception: " + e);
        incAndGetFailCnt();  // ignore return
    }


    // The following methods are typically overridden by the subclass
    // of RacingThreadsTest to provide test specific semantics at each
    // identified test execution point:

    /**
     * Initialize 1-time items for the DriverThread.
     * Called by the DriverThread before WorkerThreads are started.
     * @param dt the DriverThread
     */
    public void oneTimeDriverInit(DriverThread dt) {
        if (verbose)
            System.out.println(dt.getName() + ": oneTimeDriverInit() called");
    }

    /**
     * Initialize 1-time items for a WorkerThread. Called by a
     * WorkerThread after oneTimeDriverInit() and before the
     * WorkerThread checks in with startBarrier. May execute in
     * parallel with perRaceDriverInit() or with another
     * WorkerThread's oneTimeWorkerInit() call or another
     * WorkerThread's perRaceWorkerInit() call.
     * @param wt the WorkerThread
     */
    public void oneTimeWorkerInit(WorkerThread wt) {
        if (verbose)
            System.out.println(wt.getName() + ": oneTimeWorkerInit() called");
    }

    /**
     * Initialize per-race items for the DriverThread. Called by the
     * DriverThread before it checks in with startBarrier. May execute
     * in parallel with oneTimeWorkerInit() and perRaceWorkerInit()
     * calls. After any race except for the last race, this method may
     * execute in parallel with perRaceWorkerEpilog().
     * @param dt the DriverThread
     */
    public void perRaceDriverInit(DriverThread dt) {
        if (verbose)
            System.out.println(dt.getName() + ": perRaceDriverInit() called");
    }

    /**
     * Initialize per-race items for a WorkerThread. Called by each
     * WorkerThread before it checks in with startBarrier. On the first
     * call, this method may execute in parallel with another
     * WorkerThread's oneTimeWorkerInit() call. On any call, this method
     * may execute in parallel with perRaceDriverInit() or another
     * WorkerThread's perRaceWorkerInit() call. After any race except
     * for the last race, this method may execute in parallel with
     * perRaceDriverEpilog() or another WorkerThread's
     * perRaceWorkerEpilog() call.
     * @param wt the WorkerThread
     */
    public void perRaceWorkerInit(WorkerThread wt) {
        if (verbose)
            System.out.println(wt.getName() + ": perRaceWorkerInit() called");
    }

    /**
     * Execute the race in a WorkerThread. Called by each WorkerThread
     * after it has been released from startBarrier.
     * @param wt the WorkerThread
     */
    public void executeRace(WorkerThread wt) {
        if (verbose)
            System.out.println(wt.getName() + ": executeRace() called");
    }

    /**
     * Check race results in the DriverThread. Called by the DriverThread
     * after it has been released from finishBarrier and before the
     * DriverThread checks in with resetBarrier.
     * @param dt the DriverThread
     */
    public void checkRaceResults(DriverThread dt) {
        if (verbose)
            System.out.println(dt.getName() + ": checkRaceResults() called");
    }

    /**
     * Handle end-of-race items for the DriverThread. Called by the
     * DriverThread after it has been released from resetBarrier and
     * before the DriverThread checks in again with startBarrier. Can
     * execute in parallel with perRaceWorkerEpilog(). If this is not
     * the last race, can execute in parallel with perRaceWorkerInit().
     * If this is the last race, can execute in parallel with
     * oneTimeWorkerEpilog().
     * @param dt the DriverThread
     */
    public void perRaceDriverEpilog(DriverThread dt) {
        if (verbose)
            System.out.println(dt.getName() + ": perRaceDriverEpilog() called");
    }

    /**
     * Handle end-of-race items for a WorkerThread. Called by each
     * WorkerThread after it has been released from resetBarrier and
     * before the WorkerThread checks in again with startBarrier.
     * Can execute in parallel with perRaceDriverEpilog() or another
     * WorkerThread's perRaceWorkerEpilog() call. If this is not the
     * last race, can execute in parallel with perRaceDriverInit(),
     * or another WorkerThread's perRaceWorkerInit() call. If this
     * is the last race, can execute in parallel with another
     * WorkerThread's oneTimeWorkerEpilog() call.
     * @param wt the WorkerThread
     */
    public void perRaceWorkerEpilog(WorkerThread wt) {
        if (verbose)
            System.out.println(wt.getName() + ": perRaceWorkerEpilog() called");
    }

    /**
     * Handle end-of-test items for a WorkerThread. Called by each
     * WorkerThread after it has detected that all races are done and
     * before oneTimeDriverEpilog() is called. Can execute in parallel
     * with perRaceDriverEpilog(), with another WorkerThread's
     * perRaceWorkerEpilog() call or with another WorkerThread's
     * oneTimeWorkerEpilog() call.
     * @param wt the WorkerThread
     */
    public void oneTimeWorkerEpilog(WorkerThread wt) {
        if (verbose)
            System.out.println(wt.getName() + ": oneTimeWorkerEpilog() called");
    }

    /**
     * Handle end-of-test items for the DriverThread. Called by the
     * DriverThread after all the WorkerThreads have called
     * oneTimeWorkerEpilog().
     * @param dt the DriverThread
     */
    public void oneTimeDriverEpilog(DriverThread dt) {
        if (verbose)
            System.out.println(dt.getName() + ": oneTimeDriverEpilog() called");
    }


    /**
     * DriverThread for executing the test.
     */
    public static class DriverThread extends Thread {
        private final RacingThreadsTest test;

        /**
         * Create the test DriverThread that manages all the WorkerThreads.
         * The DriverThread class can be extended to provide test specific
         * variables and/or code. However, that is typically done in the
         * subclass of RacingThreadsTest.
         * @parameter test the RacingThreadsTest being run
         */
        DriverThread(RacingThreadsTest test) {
            super("DriverThread");
            this.test = test;
        }

        private void run(WorkerThread[] workers) {
            System.out.println(getName() + ": is starting.");
            System.out.println(getName() + ": # WorkerThreads: " + test.N_THREADS);
            System.out.println(getName() + ": max # loops: " + test.N_LOOPS);
            System.out.println(getName() + ": max # secs: " + test.N_SECS);

            // initialize 1-time items for the DriverThread
            test.oneTimeDriverInit(this);

            // start all the threads
            for (int i = 0; i < workers.length; i++) {
                workers[i].start();
            }

            // All WorkerThreads call oneTimeWorkerInit() and
            // perRaceWorkerInit() on the way to startBarrier.

            long endTime = System.currentTimeMillis() + test.N_SECS * 1000;

            for (; !test.getDone() && test.getLoopCnt() < test.N_LOOPS;
                test.incAndGetLoopCnt()) {

                if (test.getVerbose() && (test.N_LOOPS < 10 ||
                    (test.getLoopCnt() % (test.N_LOOPS / 10)) == 0)) {
                    System.out.println(getName() + ": race loop #"
                        + test.getLoopCnt());
                }

                // initialize per-race items for the DriverThread
                test.perRaceDriverInit(this);

                try {
                    // we've setup the race so start it when all
                    // WorkerThreads get to the startBarrier
                    test.startBarrier.await();
                } catch (BrokenBarrierException bbe) {
                    test.unexpectedException(this, bbe);
                    return;
                } catch (InterruptedException ie) {
                    test.unexpectedException(this, ie);
                    return;
                }

                // All WorkerThreads are racing via executeRace()
                // at this point

                // wait for all threads to finish the race
                try {
                    test.finishBarrier.await();
                } catch (BrokenBarrierException bbe) {
                    test.unexpectedException(this, bbe);
                    return;
                } catch (InterruptedException ie) {
                    test.unexpectedException(this, ie);
                    return;
                }
                // All WorkerThreads are heading to resetBarrier at this
                // point so we can check the race results before we reset
                // for another race (or bail because we are done).

                test.checkRaceResults(this);

                if (test.getLoopCnt() + 1 >= test.N_LOOPS ||
                    System.currentTimeMillis() >= endTime) {
                    // This is the last loop or we're out of time.
                    // Let test threads know we are done before we release
                    // them from resetBarrier
                    test.setDone(true);
                }

                // release the WorkerThreads from resetBarrier
                try {
                    test.resetBarrier.await();
                } catch (BrokenBarrierException bbe) {
                    test.unexpectedException(this, bbe);
                    return;
                } catch (InterruptedException ie) {
                    test.unexpectedException(this, ie);
                    return;
                }

                // All WorkerThreads call perRaceWorkerEpilog(). If
                // this is not the last loop, then all WorkerThreads
                // will also call perRaceWorkerInit() on the way to
                // startBarrier. If this is the last loop, then all
                // WorkerThreads will call oneTimeWorkerEpilog() on
                // their way to ending.

                // handle end-of-race items for the DriverThread
                test.perRaceDriverEpilog(this);
            }

            System.out.println(getName() + ": completed " + test.getLoopCnt()
                + " race loops.");
            if (test.getLoopCnt() < test.N_LOOPS) {
                System.out.println(getName() + ": race stopped @ " + test.N_SECS
                    + " seconds.");
            }

            for (int i = 0; i < workers.length; i++) {
                try {
                    workers[i].join();
                } catch (InterruptedException ie) {
                    test.unexpectedException(this, ie);
                    return;
                }
            }

            // handle end-of-test items for the DriverThread
            test.oneTimeDriverEpilog(this);

            System.out.println(getName() + ": is done.");
        }
    }


    /**
     * WorkerThread for executing the race.
     */
    public static class WorkerThread extends Thread {
        private final RacingThreadsTest test;
        private final int workerNum;

        /**
         * Creates WorkerThread-N that executes the test code. The
         * WorkerThread class can be extended to provide test thread
         * specific variables and/or code.
         * @param workerNum the number for the new WorkerThread
         * @parameter test the RacingThreadsTest being run
         */
        WorkerThread(int workerNum, RacingThreadsTest test) {
            super("WorkerThread-" + workerNum);
            this.test = test;
            this.workerNum = workerNum;
        }

        /**
         * get the WorkerThread's number
         * @return the WorkerThread's number
         */
        public int getWorkerNum() {
            return workerNum;
        }

        /**
         * Run the race in a WorkerThread.
         */
        public void run() {
            System.out.println(getName() + ": is running.");

            // initialize 1-time items for the WorkerThread
            test.oneTimeWorkerInit(this);

            while (!test.getDone()) {
                // initialize per-race items for the WorkerThread
                test.perRaceWorkerInit(this);

                try {
                    test.startBarrier.await();  // wait for race to start
                } catch (BrokenBarrierException bbe) {
                    test.unexpectedException(this, bbe);
                    return;
                } catch (InterruptedException ie) {
                    test.unexpectedException(this, ie);
                    return;
                }

                // execute the race for the WorkerThread
                test.executeRace(this);

                try {
                    test.finishBarrier.await();  // this thread is done
                } catch (BrokenBarrierException bbe) {
                    test.unexpectedException(this, bbe);
                    return;
                } catch (InterruptedException ie) {
                    test.unexpectedException(this, ie);
                    return;
                }

                try {
                    test.resetBarrier.await();  // wait for race to reset
                } catch (BrokenBarrierException bbe) {
                    test.unexpectedException(this, bbe);
                    return;
                } catch (InterruptedException ie) {
                    test.unexpectedException(this, ie);
                    return;
                }

               // handle end-of-race items for the WorkerThread
                test.perRaceWorkerEpilog(this);
            }

            // handle end-of-test items for the WorkerThread
            test.oneTimeWorkerEpilog(this);

            System.out.println(getName() + ": is ending.");
        }
    }
}
