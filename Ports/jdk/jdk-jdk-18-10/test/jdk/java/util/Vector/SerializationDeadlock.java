/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2010, 2011 IBM Corporation
 */

/*
 * @test
 * @bug 6934356
 * @summary Serializing Vector objects which refer to each other should not be able to deadlock.
 * @author Neil Richards <neil.richards@ngmr.net>, <neil_richards@uk.ibm.com>
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CyclicBarrier;

public class SerializationDeadlock {
    public static void main(final String[] args) throws Exception {
        // Test for Vector serialization deadlock
        final Vector<Object> v1 = new Vector<>();
        final Vector<Object> v2 = new Vector<>();
        final TestBarrier testStart = new TestBarrier(3);

        // Populate the vectors so that they refer to each other
        v1.add(testStart);
        v1.add(v2);
        v2.add(testStart);
        v2.add(v1);

        final CyclicBarrier testEnd = new CyclicBarrier(3);
        final TestThread t1 = new TestThread(v1, testEnd);
        final TestThread t2 = new TestThread(v2, testEnd);

        t1.start();
        t2.start();

        // Wait for both test threads to have initiated serialization
        // of the 'testStart' object (and hence of both 'v1' and 'v2')
        testStart.await();

        // Wait for both test threads to successfully finish serialization
        // of 'v1' and 'v2'.
        System.out.println("Waiting for Vector serialization to complete ...");
        System.out.println("(This test will hang if serialization deadlocks)");
        testEnd.await();
        System.out.println("Test PASSED: serialization completed successfully");

        TestThread.handleExceptions();
    }

    static final class TestBarrier extends CyclicBarrier
            implements Serializable {
        public TestBarrier(final int count) {
            super(count);
        }

        private void writeObject(final ObjectOutputStream oos)
                throws IOException {
            oos.defaultWriteObject();
            // Wait until all test threads have started serializing data
            try {
                await();
            } catch (final Exception e) {
                throw new IOException("Test ERROR: Unexpected exception caught", e);
            }
        }
    }

    static final class TestThread extends Thread {
        private static final List<Exception> exceptions = new ArrayList<>();

        private final Vector vector;
        private final CyclicBarrier testEnd;

        public TestThread(final Vector vector, final CyclicBarrier testEnd) {
            this.vector = vector;
            this.testEnd = testEnd;
            setDaemon(true);
        }

        public void run() {
            try {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeObject(vector);
                oos.close();
            } catch (final IOException ioe) {
                addException(ioe);
            } finally {
                try {
                    testEnd.await();
                } catch (Exception e) {
                    addException(e);
                }
            }
        }

        private static synchronized void addException(final Exception exception) {
            exceptions.add(exception);
        }

        public static synchronized void handleExceptions() {
            if (false == exceptions.isEmpty()) {
                throw new RuntimeException(getErrorText(exceptions));
            }
        }

        private static String getErrorText(final List<Exception> exceptions) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);

            pw.println("Test ERROR: Unexpected exceptions thrown on test threads:");
            for (Exception exception : exceptions) {
                pw.print("\t");
                pw.println(exception);
                for (StackTraceElement element : exception.getStackTrace()) {
                    pw.print("\t\tat ");
                    pw.println(element);
                }
            }

            pw.close();
            return sw.toString();
        }
    }
}

