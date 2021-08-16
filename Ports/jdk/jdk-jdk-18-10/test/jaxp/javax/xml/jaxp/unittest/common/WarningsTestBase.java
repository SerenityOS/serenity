/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package common;

import static jaxp.library.JAXPTestUtilities.runWithAllPerm;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import org.testng.Assert;

/*
 * This class helps to test suppression of unsupported parser properties
 * messages printed to standard error output.
 * It launches THREADS_COUNT tasks. Each task does ITERATIONS_PER_THREAD
 * sequential calls to doOneIteration method implemented by specific test class.
 */
public abstract class WarningsTestBase {

    /*
     * Abstract method that should be implemented by test class.
     * It is repeatedly called by each TestWorker task.
     */
    abstract void doOneTestIteration() throws Exception;

    /*
     * Launches parallel test tasks and check the output for the number of
     * generated warning messages. There should be no more than one message of
     * each type.
     */
    void startTest() throws Exception {
        //Save standard error stream
        PrintStream defStdErr = System.err;
        //Set new byte array stream as standard error stream
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
        runWithAllPerm(() -> System.setErr(new PrintStream(byteStream)));
        //Execute multiple TestWorker tasks
        for (int id = 0; id < THREADS_COUNT; id++) {
            EXECUTOR.execute(new TestWorker(id));
        }
        //Initiate shutdown of previously submitted task
        runWithAllPerm(EXECUTOR::shutdown);
        //Wait for termination of submitted tasks
        if (!EXECUTOR.awaitTermination(THREADS_COUNT, TimeUnit.SECONDS)) {
            //If not all tasks terminates during the time out force them to shutdown
            runWithAllPerm(EXECUTOR::shutdownNow);
        }
        //Restore default standard error stream
        runWithAllPerm(() -> System.setErr(defStdErr));
        //Print tasks stderr output
        String errContent = byteStream.toString();
        System.out.println("Standard error output content:");
        System.out.println(errContent);
        //Check if uncaught exceptions were observed by one or more threads
        Assert.assertFalse(uncaughtExceptions);
        //Check tasks stderr output for quantity of warning messages
        Assert.assertTrue(warningPrintedOnce(XMLConstants.ACCESS_EXTERNAL_DTD, errContent));
        Assert.assertTrue(warningPrintedOnce(ENT_EXP_PROPERTY, errContent));
        Assert.assertTrue(warningPrintedOnce(XMLConstants.FEATURE_SECURE_PROCESSING, errContent));
    }

    // Count occurences of warning messages in standard error and check if warning is printed
    // not more than once
    private boolean warningPrintedOnce(String propertyName, String testOutput) {
        //Count for property name in test output
        Pattern p = Pattern.compile(propertyName);
        Matcher m = p.matcher(testOutput);
        int count = 0;
        while (m.find()) {
            count += 1;
        }
        System.out.println("'" + propertyName + "' print count: " + count);
        //If count is more than 1 then consider test failed
        return count <= 1;
    }

    //TestWorker task that sequentially calls test method
    private class TestWorker implements Runnable {
        // Task id
        private final int id;

        TestWorker(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                System.out.printf("%d: waiting for barrier%n", id);
                //Synchronize startup of all tasks
                BARRIER.await();
                System.out.printf("%d: starting iterations%n", id);
                //Call test method multiple times
                for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                    doOneTestIteration();
                }
            } catch (Exception ex) {
                throw new RuntimeException("TestWorker id:" + id + " failed", ex);
            }
        }
    }

    // Thread factory that handles uncaughtExceptions and prints them
    // to stdout instead of stderr.
    private static class TestThreadFactory implements ThreadFactory {

        public Thread newThread(final Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable thr) {
                    thr.printStackTrace(System.out);
                    uncaughtExceptions = true;
                }
            });
            return t;
        }
    }

    //Flag that indicates if one or more threads from thread pool caught unhandled exception
    private static boolean uncaughtExceptions = false;
    //Entity expansion limit property name
    private static final String ENT_EXP_PROPERTY = "http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit";
    //Number of simultaneous test threads
    private static final int THREADS_COUNT = 10;
    //Number of iterations per one thread
    private static final int ITERATIONS_PER_THREAD = 4;
    //Test thread pool
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new TestThreadFactory());
    //Cyclic barrier for threads startup synchronization
    private static final CyclicBarrier BARRIER = new CyclicBarrier(THREADS_COUNT);
}
