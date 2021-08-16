/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.Security;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/* @test
 * @build MixedFiltersTest SerialFilterTest
 * @run testng/othervm -Djdk.serialFilter=!java.**;!java.lang.Long;maxdepth=5;maxarray=5;maxbytes=90;maxrefs=5          MixedFiltersTest
 * @run testng/othervm -Djdk.serialFilter=java.**;java.lang.Long;maxdepth=1000;maxarray=1000;maxbytes=1000;maxrefs=1000 MixedFiltersTest
 *
 * @summary Test that when both global filter and specific filter are set,
 *          global filter will not affect specific filter.
 */

public class MixedFiltersTest implements Serializable {

    private static final long serialVersionUID = 1234567890L;


    boolean globalRejected;

    @BeforeClass
    public void setup() {
        String pattern = System.getProperty("jdk.serialFilter",
                Security.getProperty("jdk.serialFilter"));
        globalRejected = pattern.startsWith("!");
    }

    @DataProvider(name="RejectedInGlobal")
    Object[][] rejectedInGlobal() {
        if (!globalRejected) {
            return new Object[0][];
        }
        return new Object[][] {
                new Object[] { Long.MAX_VALUE, "java.**" },
                new Object[] { Long.MAX_VALUE, "java.lang.Long" },
                new Object[] { SerialFilterTest.genTestObject("java.lang.**", true), "java.lang.**" },
                new Object[] { SerialFilterTest.genTestObject("maxdepth=10", true), "maxdepth=100" },
                new Object[] { SerialFilterTest.genTestObject("maxarray=10", true), "maxarray=100" },
                new Object[] { SerialFilterTest.genTestObject("maxbytes=100", true), "maxbytes=1000" },
                new Object[] { SerialFilterTest.genTestObject("maxrefs=10", true), "maxrefs=100" },
        };
    }

    /**
     * Test:
     *   "global filter reject" + "specific ObjectInputStream filter is empty" => should reject
     *   "global filter reject" + "specific ObjectInputStream filter allow"    => should allow
     */
    @Test(dataProvider="RejectedInGlobal")
    public void testRejectedInGlobal(Object toDeserialized, String pattern) throws Exception {
        byte[] bytes = SerialFilterTest.writeObjects(toDeserialized);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object o = ois.readObject();
            fail("filter should have thrown an exception");
        } catch (InvalidClassException expected) { }

        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(pattern);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(filter);
            Object o = ois.readObject();
        }
    }

    @DataProvider(name="AllowedInGlobal")
    Object[][] allowedInGlobal() {
        if (globalRejected) {
            return new Object[0][];
        }

        return new Object[][] {
                new Object[] { Long.MAX_VALUE, "!java.**" },
                new Object[] { Long.MAX_VALUE, "!java.lang.Long" },
                new Object[] { SerialFilterTest.genTestObject("java.lang.**", true), "!java.lang.**" },
                new Object[] { SerialFilterTest.genTestObject("maxdepth=10", true), "maxdepth=5" },
                new Object[] { SerialFilterTest.genTestObject("maxarray=10", true), "maxarray=5" },
                new Object[] { SerialFilterTest.genTestObject("maxbytes=100", true), "maxbytes=5" },
                new Object[] { SerialFilterTest.genTestObject("maxrefs=10", true), "maxrefs=5" },
            };
    }

    /**
     * Test:
     *   "global filter allow" + "specific ObjectInputStream filter is empty" => should allow
     *   "global filter allow" + "specific ObjectInputStream filter reject"   => should reject
     */
    @Test(dataProvider="AllowedInGlobal")
    public void testAllowedInGlobal(Object toDeserialized, String pattern) throws Exception {
        byte[] bytes = SerialFilterTest.writeObjects(toDeserialized);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object o = ois.readObject();
        }

        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(pattern);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(filter);
            Object o = ois.readObject();
            assertTrue(false, "filter should have thrown an exception");
        } catch (InvalidClassException expected) { }
    }
}
