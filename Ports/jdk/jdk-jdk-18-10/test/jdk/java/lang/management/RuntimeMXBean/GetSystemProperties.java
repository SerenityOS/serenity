/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4990512
 * @summary Basic Test for RuntimeMXBean.getSystemProperties().
 * @author  Mandy Chung
 */

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;

public class GetSystemProperties {
    private static final String KEY1   = "test.property.key1";
    private static final String VALUE1 = "test.property.value1";
    private static final String KEY2   = "test.property.key2";
    private static final String VALUE2 = "test.property.value2";

    // system properties to be omitted
    private static final String KEY3   = "test.property.key3";
    private static final Long VALUE3   = new Long(0);;

    private static final Object KEY4   = new Object();
    private static final String VALUE4 = "test.property.value4";

    public static void main(String[] argv) throws Exception {
        // Save a copy of the original system properties
        Properties props = System.getProperties();

        try {
            // replace the system Properties object for any modification
            // in case jtreg caches a copy
            System.setProperties(new Properties(props));
            runTest();
        } finally {
            // restore original system properties
            System.setProperties(props);
        }
    }

    private static void runTest() throws Exception {
        RuntimeMXBean mbean = ManagementFactory.getRuntimeMXBean();

        // Print all system properties
        Map<String,String> props = mbean.getSystemProperties();
        printProperties(props);

        // Add new system properties
        System.setProperty(KEY1, VALUE1);
        System.setProperty(KEY2, VALUE2);

        Map<String,String> props1 = mbean.getSystemProperties();
        String value1 = props1.get(KEY1);
        if (value1 == null || !value1.equals(VALUE1)) {
            throw new RuntimeException(KEY1 + " property found" +
                 " with value = " + value1 +
                 " but expected to be " + VALUE1);
        }

        String value2 = props1.get(KEY2);
        if (value2 == null || !value2.equals(VALUE2)) {
            throw new RuntimeException(KEY2 + " property found" +
                 " with value = " + value2 +
                 " but expected to be " + VALUE2);
        }

        String value3 = props1.get(KEY3);
        if (value3 != null) {
            throw new RuntimeException(KEY3 + " property found" +
                 " but should not exist" );
        }

        // Add new system properties but are not Strings
        Properties sp = System.getProperties();
        sp.put(KEY3, VALUE3);
        sp.put(KEY4, VALUE4);

        Map<String,String> props2 = mbean.getSystemProperties();
        // expect the system properties returned should be
        // same as the one before adding KEY3 and KEY4
        if (!props1.equals(props2)) {
            throw new RuntimeException("Two copies of system properties " +
                "are expected to be equal");
        }

        System.out.println("Test passed.");
    }

    private static void printProperties(Map<String,String> props) {
        Set<Map.Entry<String,String>> set = props.entrySet();
        for (Map.Entry<String,String> p : set) {
            System.out.println(p.getKey() + ": " + p.getValue());
        }
    }
}
