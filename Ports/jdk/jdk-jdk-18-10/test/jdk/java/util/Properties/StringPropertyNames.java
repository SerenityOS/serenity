/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6253413 8059361
 * @summary Test for Properties.stringPropertyNames() if the system
 *          properties contain another list of properties as the defaults.
 * @author  Mandy Chung
 *
 * @run build StringPropertyNames
 * @run main  StringPropertyNames
 */

import java.util.Properties;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

public class StringPropertyNames {
    private static int NUM_SHARE_PROPS = 2;
    private static int NUM_PROPS1 = 3;
    private static int NUM_PROPS2 = 5;
    private static String KEY = "good.property.";
    private static String VALUE = "good.value.";
    public static void main(String[] argv) throws Exception {
        Properties props1 = new Properties();
        Properties props2 = new Properties(props1);

        // add several new properties
        for (int i = 0; i < NUM_PROPS1; i++) {
            props1.put(KEY + "1." + i, VALUE + "1." + i);
        }
        for (int i = 0; i < NUM_PROPS2; i++) {
            props2.put(KEY + "2." + i, VALUE + "2." + i);
        }

        // add the same properties in both props1 and props2
        for (int i = 0; i < NUM_SHARE_PROPS; i++) {
            props1.put(KEY + i, VALUE + "1." + i);
            props2.put(KEY + i, VALUE + "2." + i);
        }
        checkProperties(props1,
                        NUM_PROPS1 + NUM_SHARE_PROPS, // size of props1
                        NUM_PROPS1 + NUM_SHARE_PROPS, // num of string keys
                        NUM_PROPS1 + NUM_SHARE_PROPS, // num of keys in propertyName(),
                        false);
        checkProperties(props2,
                        NUM_PROPS2 + NUM_SHARE_PROPS, // size of props2
                        NUM_PROPS1 + NUM_PROPS2 + NUM_SHARE_PROPS, // num of string keys
                        NUM_PROPS1 + NUM_PROPS2 + NUM_SHARE_PROPS, // num of keys in propertyName(),
                        false);

        // Add non-String value
        props1.put(KEY + "9", new Integer(4));
        checkProperties(props1,
                        NUM_PROPS1 + NUM_SHARE_PROPS + 1, // size of props1
                        NUM_PROPS1 + NUM_SHARE_PROPS, // num of string keys
                        NUM_PROPS1 + NUM_SHARE_PROPS + 1, // num of keys in propertyName(),
                        false);
        checkProperties(props2,
                        NUM_PROPS2 + NUM_SHARE_PROPS, // size of props2
                        NUM_PROPS1 + NUM_PROPS2 + NUM_SHARE_PROPS, // num of string keys
                        NUM_PROPS1 + NUM_PROPS2 + NUM_SHARE_PROPS + 1, // num of keys in propertyName(),
                        false);
        Object v = props1.remove(KEY + "9");
        if (v == null) {
            throw new RuntimeException("Test Failed: " +
                "Key " + KEY + "9" + " not found");
        }

        // Add a non-String key
        props1.put(new Integer(5), "good.value.5");
        props2.put(new Object(), new Object());
        checkProperties(props1,
                        NUM_PROPS1 + NUM_SHARE_PROPS + 1, // size of props1
                        NUM_PROPS1 + NUM_SHARE_PROPS, // num of string keys
                        NUM_PROPS1 + NUM_SHARE_PROPS + 1, // num of keys in propertyName(),
                        true);
        checkProperties(props2,
                        NUM_PROPS2 + NUM_SHARE_PROPS + 1, // size of props2
                        NUM_PROPS1 + NUM_PROPS2 + NUM_SHARE_PROPS, // num of string keys
                        NUM_PROPS1 + NUM_PROPS2 + NUM_SHARE_PROPS + 2, // num of keys in propertyName(),
                        true);
        System.out.println("Test passed.");
    }

    private static void checkProperties(Properties props,
                                        int propSize,
                                        int numStringKeys,
                                        int enumerateSize,
                                        boolean hasNonStringKeys) {
        // check the size of the properties
        if (props.size() != propSize) {
            throw new RuntimeException("Test Failed: " +
                "Expected number of properties = " +
                propSize + " but found = " + props.size());
        }

        // check the number of properties whose key and value
        // are both strings
        Set<String> keys = props.stringPropertyNames();
        if (keys.size() != numStringKeys) {
            throw new RuntimeException("Test Failed: " +
                "Expected number of String keys = " +
                numStringKeys + " but found = " + keys.size());
        }
        boolean cceThrown = false;
        try {
            // check the number of properties whose key are strings
            // but its value can be anything in the current impl
            int count = 0;
            Enumeration<?> e = props.propertyNames();
            for (;e.hasMoreElements(); e.nextElement()) {
                count++;
            }
            if (count != enumerateSize) {
                throw new RuntimeException("Test Failed: " +
                    "Expected number of enumerated keys = " +
                    enumerateSize + " but found = " + count);
            }
        } catch (ClassCastException e) {
            if (!hasNonStringKeys) {
                RuntimeException re = new RuntimeException("Test Failed: " +
                    "ClassCastException is expected not to be thrown");
                re.initCause(e);
                throw re;
            }
            cceThrown = true;
        }

        if ((hasNonStringKeys && !cceThrown)) {
            throw new RuntimeException("Test Failed: " +
                "ClassCastException is expected to be thrown");
        }

        // make sure the set cannot be modified
        try {
            keys.add("xyzzy");
            throw new RuntimeException("Test Failed: " +
                "add() should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException ignore) { }

        Iterator<String> it = keys.iterator();
        if (it.hasNext()) {
            try {
                keys.remove(it.next());
                throw new RuntimeException("Test Failed: " +
                    "remove() should have thrown UnsupportedOperationException");
            } catch (UnsupportedOperationException ignore) { }
        }
    }
}
