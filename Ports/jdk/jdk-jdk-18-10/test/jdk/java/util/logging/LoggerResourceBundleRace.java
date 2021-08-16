/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7045594 8002070
 * @summary ResourceBundle setting race in Logger.getLogger(name, rbName)
 * @author  Daniel D. Daugherty
 * @build RacingThreadsTest LoggerResourceBundleRace
 * @run main/othervm LoggerResourceBundleRace
 *
 * (In samevm mode, the bundle classes don't end up in the classpath.)
 */
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


public class LoggerResourceBundleRace extends RacingThreadsTest {
    private static final int N_LOOPS   = 500000;   // # of race loops
    private static final int N_SECS    = 15;       // # of secs to run test
    // # of parallel threads; must match number of MyResources inner classes
    private static final int N_THREADS = 3;

    private static final String LOGGER_PREFIX = "myLogger-";
    private static final String RESOURCE_PREFIX
        = "LoggerResourceBundleRace$MyResources";
    // these counters are AtomicInteger since any worker thread can increment
    private static final AtomicInteger iaeCnt = new AtomicInteger();
    private static final AtomicInteger worksCnt = new AtomicInteger();

    Logger dummy;   // dummy Logger

    LoggerResourceBundleRace(String name, int n_threads, int n_loops,
        int n_secs) {
        super(name, n_threads, n_loops, n_secs);
    }


    // Main test driver
    //
    public static void main(String[] args) {
        LoggerResourceBundleRace test
            = new LoggerResourceBundleRace("LoggerResourceBundleRace",
                                           N_THREADS, N_LOOPS, N_SECS);
        test.setVerbose(
            Boolean.getBoolean("LoggerResourceBundleRace.verbose"));

        DriverThread driver = new DriverThread(test);
        MyWorkerThread[] workers = new MyWorkerThread[N_THREADS];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new MyWorkerThread(i, test);
        }
        test.runTest(driver, workers);
    }

    public void oneTimeDriverInit(DriverThread dt) {
        super.oneTimeDriverInit(dt);
        dummy = null;
    }

    public void perRaceDriverInit(DriverThread dt) {
        super.perRaceDriverInit(dt);

        // - allocate a new dummy Logger without a ResourceBundle;
        //   this gives the racing threads less to do
        // - reset the counters
        dummy = Logger.getLogger(LOGGER_PREFIX + getLoopCnt());
        iaeCnt.set(0);
        worksCnt.set(0);
    }

    public void executeRace(WorkerThread wt) {
        super.executeRace(wt);

        Logger myLogger = null;
        try {
            MyWorkerThread mwt = (MyWorkerThread) wt;  // short hand

            // Here is the race:
            // - the target Logger object has already been created by
            //   the DriverThread without a ResourceBundle name
            // - in parallel, each WorkerThread calls Logger.getLogger()
            //   with a different ResourceBundle name
            // - Logger.getLogger() should only successfully set the
            //   ResourceBundle name for one WorkerThread; all other
            //   WorkerThread calls to Logger.getLogger() should throw
            //   IllegalArgumentException
            myLogger = Logger.getLogger(LOGGER_PREFIX + getLoopCnt(),
                                        mwt.rbName);
            if (myLogger.getResourceBundleName().equals(mwt.rbName)) {
                // no exception and the ResourceBundle names match
                worksCnt.incrementAndGet();  // ignore return
            } else {
                System.err.println(wt.getName()
                    + ": ERROR: expected ResourceBundleName '"
                    + mwt.rbName + "' does not match actual '"
                    + myLogger.getResourceBundleName() + "'");
                incAndGetFailCnt();  // ignore return
            }
        } catch (IllegalArgumentException iae) {
            iaeCnt.incrementAndGet();  // ignore return
        } catch (MissingResourceException mre) {
            // This exception happens when N_THREADS above does not
            // match the number of MyResources inner classes below.
            // We exit since this is a coding error.
            unexpectedException(wt, mre);
            System.exit(2);
        }
    }

    public void checkRaceResults(DriverThread dt) {
        super.checkRaceResults(dt);

        if (worksCnt.get() != 1) {
            System.err.println(dt.getName() + ": ERROR: worksCnt should be 1"
                + ": loopCnt=" + getLoopCnt() + ", worksCnt=" + worksCnt.get());
            incAndGetFailCnt();  // ignore return
        } else if (iaeCnt.get() != N_THREADS - 1) {
            System.err.println(dt.getName() + ": ERROR: iaeCnt should be "
                + (N_THREADS - 1) + ": loopCnt=" + getLoopCnt()
                + ", iaeCnt=" + iaeCnt.get());
            incAndGetFailCnt();  // ignore return
        }
    }

    public void oneTimeDriverEpilog(DriverThread dt) {
        super.oneTimeDriverEpilog(dt);

        // Use the dummy Logger after the testing loop to make sure that
        // dummy doesn't get optimized away in the testing loop.
        dummy.info("This is a test message.");
    }

    // N_THREADS above must match number of MyResources inner classes
    //
    public static class MyResources0 extends ListResourceBundle {
        static final Object[][] contents = {
            {"sample1", "translation #1 for sample1"},
            {"sample2", "translation #1 for sample2"},
        };

        public Object[][] getContents() {
            return contents;
        }
    }

    public static class MyResources1 extends ListResourceBundle {
        static final Object[][] contents = {
            {"sample1", "translation #2 for sample1"},
            {"sample2", "translation #2 for sample2"},
        };

        public Object[][] getContents() {
            return contents;
        }
    }

    public static class MyResources2 extends ListResourceBundle {
        static final Object[][] contents = {
            {"sample1", "translation #3 for sample1"},
            {"sample2", "translation #3 for sample2"},
        };

        public Object[][] getContents() {
            return contents;
        }
    }


    // WorkerThread with a thread specific ResourceBundle name
    //
    public static class MyWorkerThread extends WorkerThread {
        public final String rbName;  // ResourceBundle name

        MyWorkerThread(int workerNum, RacingThreadsTest test) {
            super(workerNum, test);

            rbName = RESOURCE_PREFIX + workerNum;
        }
    }
}
