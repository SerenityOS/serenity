/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/*
 * @test
 * @bug 8010285
 * @summary Tests the fallback for creating JMX proxies for private interfaces
 *          It needs to be a separate class because the "jdk.jmx.mbeans.allowNonPublic"
 *          system property must be set before c.s.j.m.MBeanAnalyzer has been loaded.
 * @author Jaroslav Bachorik
 *
 * @run clean JMXProxyFallbackTest
 * @run build JMXProxyFallbackTest
 * @run main/othervm -Djdk.jmx.mbeans.allowNonPublic=true JMXProxyFallbackTest
 */
public class JMXProxyFallbackTest {
    private static interface PrivateMBean {
        public int[] getInts();
    }

    private static interface PrivateMXBean {
        public int[] getInts();
    }

    public static class Private implements PrivateMXBean, PrivateMBean {
        public int[] getInts() {
            return new int[]{1,2,3};
        }
    }

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testPrivate(PrivateMBean.class);
        testPrivate(PrivateMXBean.class);

        if (failures == 0)
            System.out.println("Test passed");
        else
            throw new Exception("TEST FAILURES: " + failures);
    }

    private static void fail(String msg) {
        failures++;
        System.out.println("FAIL: " + msg);
    }

    private static void success(String msg) {
        System.out.println("OK: " + msg);
    }

    private static void testPrivate(Class<?> iface) throws Exception {
        try {
            System.out.println("Creating a proxy for private M(X)Bean " +
                                iface.getName() + " ...");

            MBeanServer mbs = MBeanServerFactory.newMBeanServer();
            ObjectName on = new ObjectName("test:type=Proxy");

            JMX.newMBeanProxy(mbs, on, iface);
            success("Created a proxy for private M(X)Bean - " + iface.getName());
        } catch (Exception e) {
            Throwable t = e;
            while (t != null && !(t instanceof NotCompliantMBeanException)) {
                t = t.getCause();
            }
            if (t != null) {
                fail("Proxy not created");
            } else {
                throw e;
            }
        }
    }
}
