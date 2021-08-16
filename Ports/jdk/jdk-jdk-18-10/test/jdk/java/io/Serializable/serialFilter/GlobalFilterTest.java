/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

import java.io.SerializablePermission;
import java.security.Security;
import java.util.Objects;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

/* @test
 * @bug 8231422
 * @build GlobalFilterTest SerialFilterTest
 * @run testng/othervm GlobalFilterTest
 * @run testng/othervm -Djdk.serialFilter=java.**
 *          -Dexpected-jdk.serialFilter=java.** GlobalFilterTest
 * @run testng/othervm/policy=security.policy GlobalFilterTest
 * @run testng/othervm/policy=security.policy
 *        -Djava.security.properties=${test.src}/java.security-extra1
 *        -Djava.security.debug=properties GlobalFilterTest
 *
 * @summary Test Global Filters
 */

/* @test
 * @bug 8261160
 * @summary Add a deserialization JFR event
 * @build GlobalFilterTest SerialFilterTest
 * @requires vm.hasJFR
 * @run testng/othervm/policy=security.policy
 *        -XX:StartFlightRecording:name=DeserializationEvent,dumponexit=true
 *        -Djava.security.properties=${test.src}/java.security-extra1
 *        -Djava.security.debug=properties GlobalFilterTest
 */

@Test
public class GlobalFilterTest {
    private static final String serialPropName = "jdk.serialFilter";
    private static final String badSerialFilter = "java.lang.StringBuffer;!*";
    private static final String origSerialFilterProperty =
            System.setProperty(serialPropName, badSerialFilter);

    /**
     * DataProvider of patterns and objects derived from the configured process-wide filter.
     * @return Array of arrays of pattern, object, allowed boolean, and API factory
     */
    @DataProvider(name="globalPatternElements")
    Object[][] globalPatternElements() {
        String globalFilter =
                System.getProperty("expected-" + serialPropName,
                        Security.getProperty(serialPropName));
        if (globalFilter == null) {
            return new Object[0][];
        }

        String[] patterns = globalFilter.split(";");
        Object[][] objects = new Object[patterns.length][];

        for (int i = 0; i < patterns.length; i++) {
            Object o;
            boolean allowed;
            String pattern = patterns[i].trim();
            if (pattern.contains("=")) {
                allowed = false;
                o = SerialFilterTest.genTestObject(pattern, false);
            } else {
                allowed = !pattern.startsWith("!");
                o = (allowed)
                    ? SerialFilterTest.genTestObject(pattern, true)
                    : SerialFilterTest.genTestObject(pattern.substring(1), false);

                Assert.assertNotNull(o, "fail generation failed");
            }
            objects[i] = new Object[3];
            objects[i][0] = pattern;
            objects[i][1] = allowed;
            objects[i][2] = o;
        }
        return objects;
    }

    /**
     * Test that the process-wide filter is set when the properties are set
     * and has the toString matching the configured pattern.
     */
    @Test()
    static void globalFilter() {
        ObjectInputFilter filter = ObjectInputFilter.Config.getSerialFilter();

        // Check that the System.setProperty(jdk.serialFilter) DOES NOT affect the filter.
        String asSetSystemProp = System.getProperty(serialPropName,
                Security.getProperty(serialPropName));
        Assert.assertNotEquals(Objects.toString(filter, null), asSetSystemProp,
                "System.setProperty(\"jdk.serialfilter\", ...) should not change filter: " +
                asSetSystemProp);

        String pattern =
                System.getProperty("expected-" + serialPropName,
                        Security.getProperty(serialPropName));
        System.out.printf("global pattern: %s, filter: %s%n", pattern, filter);
        Assert.assertEquals(Objects.toString(filter, null), pattern,
                "process-wide filter pattern does not match");
    }

    /**
     * If the Global filter is already set, it should always refuse to be
     * set again.
     * If there is a security manager, setting the serialFilter should fail
     * without the appropriate permission.
     * If there is no security manager then setting it should work.
     */
    @Test()
    @SuppressWarnings("removal")
    static void setGlobalFilter() {
        SecurityManager sm = System.getSecurityManager();
        ObjectInputFilter filter = new SerialFilterTest.Validator();
        ObjectInputFilter global = ObjectInputFilter.Config.getSerialFilter();
        if (global != null) {
            // once set, can never be re-set
            try {
                ObjectInputFilter.Config.setSerialFilter(filter);
                Assert.fail("set only once process-wide filter");
            } catch (IllegalStateException ise) {
                // Normal, once set can never be re-set even if no security manager
            } catch (SecurityException se) {
                if (sm == null) {
                    Assert.fail("wrong exception when security manager is not set", se);
                }
            }
        } else {
            if (sm == null) {
                // no security manager
                try {
                    ObjectInputFilter.Config.setSerialFilter(filter);
                    // Note once set, it can not be reset; so other tests
                    System.out.printf("Global Filter set to Validator%n");
                } catch (SecurityException se) {
                    Assert.fail("setGlobalFilter should not get SecurityException", se);
                }
                try {
                    // Try to set it again, expecting it to throw
                    ObjectInputFilter.Config.setSerialFilter(filter);
                    Assert.fail("set only once process-wide filter");
                } catch (IllegalStateException ise) {
                    // Normal case
                }
            } else {
                // Security manager
                SecurityException expectSE = null;
                try {
                    sm.checkPermission(new SerializablePermission("serialFilter"));
                } catch (SecurityException se1) {
                    expectSE = se1;
                }
                SecurityException actualSE = null;
                try {
                    ObjectInputFilter.Config.setSerialFilter(filter);
                } catch (SecurityException se2) {
                    actualSE = se2;
                }
                if (expectSE == null | actualSE == null) {
                    Assert.assertEquals(expectSE, actualSE, "SecurityException");
                } else {
                    Assert.assertEquals(expectSE.getClass(), actualSE.getClass(),
                            "SecurityException class");
                }
            }
        }
    }

    /**
     * For each pattern in the process-wide filter test a generated object
     * against the default process-wide filter.
     *
     * @param pattern a pattern extracted from the configured global pattern
     */
    @Test(dataProvider = "globalPatternElements")
    static void globalFilterElements(String pattern, boolean allowed,Object obj) {
        testGlobalPattern(pattern, obj, allowed);
    }

    /**
     * Serialize and deserialize an object using the default process-wide filter
     * and check allowed or reject.
     *
     * @param pattern the pattern
     * @param object the test object
     * @param allowed the expected result from ObjectInputStream (exception or not)
     */
    static void testGlobalPattern(String pattern, Object object, boolean allowed) {
        try {
//            System.out.printf("global %s pattern: %s, obj: %s%n", (allowed ? "allowed" : "not allowed"), pattern, object);
            byte[] bytes = SerialFilterTest.writeObjects(object);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                Object o = ois.readObject();
            } catch (EOFException eof) {
                // normal completion
            } catch (ClassNotFoundException cnf) {
                Assert.fail("Deserializing", cnf);
            }
            Assert.assertTrue(allowed, "filter should have thrown an exception");
        } catch (IllegalArgumentException iae) {
            Assert.fail("bad format pattern", iae);
        } catch (InvalidClassException ice) {
            Assert.assertFalse(allowed, "filter should not have thrown an exception: " + ice);
        } catch (IOException ioe) {
            Assert.fail("Unexpected IOException", ioe);
        }
    }
}
