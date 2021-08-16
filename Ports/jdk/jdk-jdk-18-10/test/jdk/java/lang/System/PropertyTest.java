/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.System;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/*
 * @test
 * @bug 4463345 4244670 8030781 8265989
 * @summary Simple test of System getProperty, setProperty, clearProperty,
 *      getProperties, and setProperties
 * @run testng/othervm PropertyTest
 */

@Test
public class PropertyTest {

    @DataProvider(name = "requiredProperties")
    static Object[][] requiredProperties() {
        return new Object[][]{
                {"java.version"},
                {"java.version.date"},
                {"java.vendor"},
                {"java.vendor.url"},
                {"java.home"},
                {"java.vm.specification.version"},
                {"java.vm.specification.vendor"},
                {"java.vm.specification.name"},
                {"java.vm.version"},
                {"java.vm.vendor"},
                {"java.vm.name"},
                {"java.specification.version"},
                {"java.specification.vendor"},
                {"java.specification.name"},
                {"java.class.version"},
                {"java.class.path"},
                {"java.library.path"},
                {"java.io.tmpdir"},
                {"os.arch"},
                {"os.version"},
                {"file.separator"},
                {"path.separator"},
                {"line.separator"},
                {"user.name"},
                {"user.home"},
                {"user.dir"},
                {"java.runtime.version"},
                {"java.runtime.name"},
                {"native.encoding"},
        };
    }

    @Test
    static void getTest() {
        System.setProperty("blah", "blech");
        Assert.assertEquals(System.getProperty("blah"), "blech");

        try {
            System.getProperty(null);
            Assert.fail("Failed: expected NullPointerException");
        } catch (NullPointerException npe) {
            // Correct result
        }
        try {
            System.getProperty("");
            Assert.fail("Failed: expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // Correct result
        }
    }

    @Test
    static void clearTest() {
        System.setProperty("blah", "blech");
        Assert.assertEquals(System.getProperty("blah"), "blech");

        System.clearProperty("blah");
        Assert.assertNull(System.getProperty("blah"));

        try {
            System.clearProperty(null);
            Assert.fail("Failed: expected NullPointerException");
        } catch (NullPointerException npe) {
            // Correct result
        }
        try {
            System.clearProperty("");
            Assert.fail("Failed: expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // Correct result
        }
    }

    @Test
    static void setTest() {
        System.setProperty("blah", "blech");
        Assert.assertEquals(System.getProperty("blah"), "blech");

        System.setProperty("blah", "");
        Assert.assertEquals(System.getProperty("blah"), "");

        try {
            System.setProperty(null, null);
            Assert.fail("Failed: expected NullPointerException");
        } catch (NullPointerException npe) {
            // Correct result
        }

        try {
            System.setProperty("blah", null);
            Assert.fail("Failed: expected NullPointerException");
        } catch (NullPointerException npe) {
            // Correct result
        }

        try {
            System.setProperty(null, "blech");
            Assert.fail("Failed: expected NullPointerException");
        } catch (NullPointerException npe) {
            // Correct result
        }

        try {
            System.setProperty("", "blech");
            Assert.fail("Failed: expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // Correct result
        }
        try {
            System.setProperty("", "");
            Assert.fail("Failed: expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // Correct result
        }
    }

    @Test
    static void replaceSetProperties() {
        Properties oldProps = System.getProperties();
        Properties newProps = new Properties();
        oldProps.forEach( (k,v) -> newProps.put(k,v));
        System.setProperties(newProps);

        Assert.assertSame(System.getProperties(), newProps,
                "getProperties not the same as setProperties");

        final String KEY = "blah";
        final String VALUE = "blech";

        // Set via Property instance; get via System methods
        newProps.setProperty(KEY, VALUE);
        Assert.assertEquals(System.getProperty(KEY), VALUE, KEY);

        String s = (String)newProps.remove(KEY);
        Assert.assertEquals(s, VALUE);
        Assert.assertNull(System.getProperty(KEY), KEY);

        // Set via System methods; Get via Property instance;
        System.setProperty(KEY, VALUE);
        Assert.assertEquals(newProps.getProperty(KEY), VALUE);

        String t = System.clearProperty(KEY);
        Assert.assertEquals(t, VALUE, KEY);
        Assert.assertNull(newProps.getProperty(KEY), KEY);
    }

    @Test
    static void setNullProperties() {
        Properties oldProps = System.getProperties();
        Properties savedProps = new Properties();
        oldProps.forEach((k,v) -> {
            if (v == null) {
                throw new RuntimeException("null value, key: " + k);
            }
            savedProps.put(k,v);
        });

        // Re-initialize properties
        System.setProperties(null);

        Properties newProps = System.getProperties();
        Object[][] propnames = requiredProperties();
        for (Object[] p : propnames) {
            String name = (String)p[0];
            Assert.assertEquals(System.getProperty(name), savedProps.getProperty(name), name);

            Assert.assertEquals(newProps.getProperty(name), savedProps.getProperty(name), name);
        }
    }

    // Verify all the required properties have values from System.getProperty and
    // System.getProperties()
    @Test(dataProvider = "requiredProperties")
    static void checkRequiredProperties(String name) {
        Assert.assertNotNull(System.getProperty(name), name);

        Properties props = System.getProperties();
        Assert.assertNotNull(props.getProperty(name), name);
    }

    @SuppressWarnings("raw_types")
    @Test(enabled=false)
    public static void main(String[] args) {
        TestListenerAdapter tla = new TestListenerAdapter();

        Class<?>[] testclass = {PropertyTest.class};
        TestNG testng = new TestNG();
        testng.setTestClasses(testclass);
        testng.addListener(tla);
        if (args.length > 0) {
            IMethodInterceptor intercept = (m, c) -> {
                List<IMethodInstance> x = m.stream()
                        .filter(m1 -> m1.getMethod().getMethodName().contains(args[0]))
                        .collect(Collectors.toList());
                return x;
            };
            testng.setMethodInterceptor(intercept);
        }
        testng.run();
        tla.getPassedTests()
                .stream().forEach(t -> System.out.printf("Passed: %s%s%n", t.getName(),
                List.of(t.getParameters())));
        tla.getFailedTests()
                .stream().forEach(t -> System.out.printf("Failed: %s%s%n", t.getName(),
                List.of(t.getParameters())));
    }
}
