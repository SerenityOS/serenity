/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6692027
 * @summary Check that custom subclasses of QueryEval can be serialized.
 * @author Eamonn McManus
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryEval;
import javax.management.QueryExp;

public class CustomQueryTest {
    public static interface CountMBean {
        public int getCount();
        public void increment();
    }

    public static class Count implements CountMBean {
        private AtomicInteger count = new AtomicInteger();

        public int getCount() {
            return count.get();
        }

        public void increment() {
            count.incrementAndGet();
        }

    }

    public static final ObjectName countName;
    static {
        try {
            countName = new ObjectName("d:type=Count");
        } catch (MalformedObjectNameException e) {
            throw new AssertionError(e);
        }
    }

    /* A query that calls the increment method of the Count MBean every time
     * it is evaluated.  If there is no ObjectName filter, the query will be
     * evaluated for every MBean in the MBean Server, so the count will be
     * incremented by the number of MBeans.
     */
    public static class IncrQuery extends QueryEval implements QueryExp {
        public boolean apply(ObjectName name) {
            try {
                getMBeanServer().invoke(countName, "increment", null, null);
                return true;
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
                throw new AssertionError(); // not reached
            }
        }
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        boolean isSecondAttempt = false;
        // The test may fail if some new MBean is registered while the test
        // is running (e.g. Graal MBean). In this case just retry the test.
        while (true) {
            mbs.registerMBean(new Count(), countName);
            int mbeanCount = mbs.getMBeanCount();
            QueryExp query = new IncrQuery();
            Set<ObjectName> names = mbs.queryNames(null, query);
            assertEquals(mbeanCount, names.size());
            assertEquals(mbeanCount, mbs.getAttribute(countName, "Count"));
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(query);
            oout.close();
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            ObjectInputStream oin = new ObjectInputStream(bin);
            query = (QueryExp) oin.readObject();
            names = mbs.queryNames(null, query);
            int counterCount = (int)mbs.getAttribute(countName, "Count");
            if (mbeanCount * 2 == counterCount) {
                break;
            }
            if (isSecondAttempt) {
                assertEquals(mbeanCount * 2, counterCount);
                break;
            }
            isSecondAttempt = true;
            System.out.println("New MBean was registered. Retrying...");
            mbs.unregisterMBean(countName);
        }
    }

    private static void assertEquals(Object expected, Object actual)
            throws Exception {
        if (!expected.equals(actual)) {
            String failure = "FAILED: expected " + expected + ", got " + actual;
            throw new Exception(failure);
        }
    }
}
