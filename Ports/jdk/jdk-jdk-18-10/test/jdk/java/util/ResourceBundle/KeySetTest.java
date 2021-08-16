/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4095319 4286358
 * @summary Test cases for the containsKey, keySet, and handleKeySet
 * methods that are new in Mustang.
 * @build KeySetMessages KeySetMessages_zh_CN
 * @run main KeySetTest
 */

import java.lang.reflect.*;
import java.util.*;

public class KeySetTest {
    static final List<String> fullKeys = Arrays.asList("food", "drink", "tea");
    static final List<String> localKeys = Arrays.asList("food", "tea");

    public static void main(String[] args) {
        // Test PropertyResourceBundle
        testKeys("KeySetResources", Locale.JAPAN);

        // Test ListResourceBundle
        testKeys("KeySetMessages", Locale.CHINA);
    }

    static void testKeys(String bundleName, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(bundleName, locale);
        System.out.println("food = " + rb.getString("food"));

        // Test keySet()
        Set<String> allKeys = rb.keySet();
        if (!(allKeys.containsAll(fullKeys) && fullKeys.containsAll(allKeys))) {
            throw new RuntimeException("got "+allKeys + ", expected " + fullKeys);
        }

        // Test containsKey()
        for (String key : fullKeys) {
            if (!rb.containsKey(key)) {
                throw new RuntimeException("rb doesn't contain: " + key);
            }
        }
        for (String key : new String[] { "snack", "beer" }) {
            if (rb.containsKey(key)) {
                throw new RuntimeException("rb contains: " + key);
            }
        }

        // Make sure that the default handleKeySet implementation
        // returns the subset keys of the given locale.
        TestBundle tb = new TestBundle(bundleName, locale);
        Set<String> childKeys = tb.handleKeySet();
        if (!(childKeys.containsAll(localKeys) || localKeys.containsAll(childKeys))) {
            throw new RuntimeException("get " + childKeys + ", expected " + localKeys);
        }
    }

    static class TestBundle extends ResourceBundle {
        ResourceBundle bundle;
        Method m;

        public TestBundle() {}

        public TestBundle(String name, Locale locale) {
            bundle = ResourceBundle.getBundle(name, locale);

            // Prepare for the handleGetObject call
            try {
                Class clazz = bundle.getClass();
                m = clazz.getMethod("handleGetObject", String.class);
                m.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException("method preparation error", e);
            }
        }

        public Enumeration<String> getKeys() {
            return bundle.getKeys();
        }

        // handleGetObject() doesn't look up its parent bundles.
        protected Object handleGetObject(String key) {
            try {
                return m.invoke(bundle, key);
            } catch (Exception e) {
                throw new RuntimeException("handleGetObject error", e);
            }
        }

        public Set<String> handleKeySet() {
            return super.handleKeySet();
        }
    }
}
