/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6601652
 * @summary Test exception when SortedMap or SortedSet has non-null Comparator
 * @author Eamonn McManus
 */

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

public class ComparatorExceptionTest {
    public static interface TestMXBean {
        public SortedSet<String> getSortedSet();
        public SortedMap<String, String> getSortedMap();
    }

    public static class TestImpl implements TestMXBean {
        public SortedSet<String> getSortedSet() {
            return new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        }

        public SortedMap<String, String> getSortedMap() {
            return new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        }
    }

    private static String failure;

    private static void fail(String why) {
        failure = "FAILED: " + why;
        System.out.println(failure);
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("a:b=c");
        mbs.registerMBean(new TestImpl(), name);

        for (String attr : new String[] {"SortedSet", "SortedMap"}) {
            try {
                Object value = mbs.getAttribute(name, attr);
                fail("get " + attr + " did not throw exception");
            } catch (Exception e) {
                Throwable t = e;
                while (!(t instanceof IllegalArgumentException)) {
                    if (t == null)
                        break;
                    t = t.getCause();
                }
                if (t != null)
                    System.out.println("Correct exception for " + attr);
                else {
                    fail("get " + attr + " got wrong exception");
                    e.printStackTrace(System.out);
                }
            }
        }

        if (failure != null)
            throw new Exception(failure);
    }
}
