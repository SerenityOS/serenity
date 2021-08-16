/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6486655
 * @summary Test that attributes and operations appear in the same order
 * in MBeanInfo as they did in the Standard MBean or MXBean Interface.
 * @author Eamonn McManus
 */

/*
 * For more background on this test, see:
 * http://weblogs.java.net/blog/emcmanus/archive/2006/11/notes_on_unspec.html
 */

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

public class FeatureOrderTest {
    private static boolean failed;

    public static interface OrderMXBean {
        public int getMercury();

        public String getVenus();
        public void setVenus(String x);

        public BigInteger getEarth();
        public void setEarth(BigInteger x);

        public boolean isMars();

        public double getJupiter();

        public byte getSaturn();

        public short getUranus();
        public void setUranus(short x);

        public long getNeptune();

        // No more Pluto!  Yay!

        public void neptune();
        public void uranus(int x);
        public int saturn(int x, int y);
        public short jupiter(int x, long y, double z);
        public void mars(boolean x);
        public BigInteger earth();
        public double earth(double x);  // http://www.imdb.com/title/tt0064519/
        public String venus();
        public int mercury();
    }

    public static interface OrderMBean extends OrderMXBean {}

    public static class OrderImpl implements OrderMXBean {
        public int getMercury() {
            return 0;
        }

        public String getVenus() {
            return null;
        }

        public void setVenus(String x) {
        }

        public BigInteger getEarth() {
            return null;
        }

        public void setEarth(BigInteger x) {
        }

        public boolean isMars() {
            return true;
        }

        public double getJupiter() {
            return 0;
        }

        public byte getSaturn() {
            return 0;
        }

        public short getUranus() {
            return 0;
        }

        public void setUranus(short x) {
        }

        public long getNeptune() {
            return 0;
        }

        public void neptune() {
        }

        public void uranus(int x) {
        }

        public int saturn(int x, int y) {
            return 0;
        }

        public short jupiter(int x, long y, double z) {
            return 0;
        }

        public void mars(boolean x) {
        }

        public BigInteger earth() {
            return null;
        }

        public double earth(double x) {
            return 0;
        }

        public String venus() {
            return null;
        }

        public int mercury() {
            return 0;
        }
    }

    public static class Order extends OrderImpl implements OrderMBean {}

    private static final boolean[] booleans = {false, true};

    public static void main(String[] args) throws Exception {
        // Build the lists of attributes and operations that we would expect
        // if they are derived by reflection and preserve the reflection order
        List<String> expectedAttributeNames = new ArrayList<String>();
        List<String> expectedOperationNames = new ArrayList<String>();
        for (Method m : OrderMXBean.class.getMethods()) {
            String name = m.getName();
            String attrName = null;
            if (name.startsWith("get") && !name.equals("get") &&
                    m.getParameterTypes().length == 0 &&
                    m.getReturnType() != void.class)
                attrName = name.substring(3);
            else if (name.startsWith("is") && !name.equals("is") &&
                    m.getParameterTypes().length == 0 &&
                    m.getReturnType() == boolean.class)
                attrName = name.substring(2);
            else if (name.startsWith("set") && !name.equals("set") &&
                    m.getReturnType() == void.class &&
                    m.getParameterTypes().length == 1)
                attrName = name.substring(3);
            if (attrName != null) {
                if (!expectedAttributeNames.contains(attrName))
                    expectedAttributeNames.add(attrName);
            } else
                expectedOperationNames.add(name);
        }

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for (boolean mxbean : booleans) {
            for (boolean withStandardMBean : booleans) {
                String testName = "MXBean: " + mxbean + "; " +
                        "using javax.management.StandardMBean: " +
                        withStandardMBean;
                System.out.println("Test case: " + testName);
                Object mbean;
                if (mxbean) {
                    if (withStandardMBean) {
                        mbean = new StandardMBean(
                                new OrderImpl(), OrderMXBean.class, true);
                    } else
                        mbean = new OrderImpl();
                } else {
                    if (withStandardMBean)
                        mbean = new StandardMBean(new Order(), OrderMBean.class);
                    else
                        mbean = new Order();
                }
                ObjectName name = new ObjectName(
                        ":mxbean=" + mxbean + "," + "withStandardMBean=" +
                        withStandardMBean);
                mbs.registerMBean(mbean, name);

                /* Make sure we are testing what we think. */
                MBeanInfo mbi = mbs.getMBeanInfo(name);
                boolean isWithStandardMBean =
                        mbs.isInstanceOf(name, StandardMBean.class.getName());
                System.out.println("classname " +mbi.getClassName());
                String mxbeanField =
                        (String) mbi.getDescriptor().getFieldValue("mxbean");
                boolean isMXBean = "true".equalsIgnoreCase(mxbeanField);

                if (mxbean != isMXBean)
                    throw new Exception("Test error: MXBean mismatch");
                if (withStandardMBean != isWithStandardMBean)
                    throw new Exception("Test error: StandardMBean mismatch");

                // Check that order of attributes and operations matches
                MBeanAttributeInfo[] mbais = mbi.getAttributes();
                checkEqual(expectedAttributeNames.size(), mbais.length,
                        "number of attributes");
                List<String> attributeNames = new ArrayList<String>();
                for (MBeanAttributeInfo mbai : mbais)
                    attributeNames.add(mbai.getName());
                checkEqual(expectedAttributeNames, attributeNames,
                        "order of attributes");

                MBeanOperationInfo[] mbois = mbi.getOperations();
                checkEqual(expectedOperationNames.size(), mbois.length,
                        "number of operations");
                List<String> operationNames = new ArrayList<String>();
                for (MBeanOperationInfo mboi : mbois)
                    operationNames.add(mboi.getName());
                checkEqual(expectedOperationNames, operationNames,
                        "order of operations");
                System.out.println();
            }
        }

        if (failed)
            throw new Exception("TEST FAILED");
        System.out.println("TEST PASSED");
    }

    private static void checkEqual(Object expected, Object actual, String what) {
        if (expected.equals(actual))
            System.out.println("OK: " + what + " matches");
        else {
            System.out.println("FAILED: " + what + " differs:");
            System.out.println("  expected: " + expected);
            System.out.println("  actual:   " + actual);
            failed = true;
        }
    }
}
