/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @run testng VersionTest
 * @summary Basic tests for java.lang.module.ModuleDescriptor.Version.
 */

import java.lang.module.ModuleDescriptor.Version;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.*;

@Test
public class VersionTest {

    // valid version strings
    @DataProvider(name = "validVersions")
    public Object[][] validVersions() {
        return new Object[][]{

            { "1.0",            null },
            { "1.0.0",          null },
            { "1.0.0.0",        null },

            { "99",             null },
            { "99.99",          null },
            { "99.99.99",       null },

            { "1-SNAPSHOT",     null },
            { "1.0-SNAPSHOT",   null },
            { "1.0.0-SNAPSHOT", null },

            { "9-ea",           null },
            { "9-ea+110",       null },
            { "9.3.2.1+42-8839942", null}

        };
    }

    // invalid version strings
    @DataProvider(name = "invalidVersions")
    public Object[][] invalidVersions() {
        return new Object[][]{

            { null,            null },
            { "",              null },
            { "A1",            null },  // does not start with number
            { "1.0-",          null },  // empty branch

        };
    }

    // Test parsing valid version strings
    @Test(dataProvider = "validVersions")
    public void testParseValidVersions(String vs, String ignore) {
        Version v = Version.parse(vs);
        assertEquals(v.toString(), vs);
    }

    // Test parsing an invalid version strings
    @Test(dataProvider = "invalidVersions",
          expectedExceptions = IllegalArgumentException.class )
    public void testParseInvalidVersions(String vs, String ignore) {
        Version.parse(vs);
    }

    // Test equals and hashCode
    @Test(dataProvider = "validVersions")
    public void testEqualsAndHashCode(String vs, String ignore) {

        Version v1 = Version.parse(vs);
        Version v2 = Version.parse(vs);
        assertEquals(v1, v2);
        assertEquals(v2, v1);
        assertEquals(v1.hashCode(), v2.hashCode());

        Version v3 = Version.parse("1.0-rhubarb");
        assertNotEquals(v1, v3);
        assertNotEquals(v2, v3);
        assertNotEquals(v3, v1);
        assertNotEquals(v3, v2);

    }

    // ordered version strings
    @DataProvider(name = "orderedVersions")
    public Object[][] orderedVersions() {
        return new Object[][]{

            { "1.0",     "2.0" },
            { "1.0-SNAPSHOT", "1.0" },
            { "1.0-SNAPSHOT2", "1.0" },
            { "1.2.3-ea", "1.2.3" },
            { "1.2.3-ea+104", "1.2.3-ea+105" },
            { "1.2.3-ea+104-4084552", "1.2.3-ea+104-4084552+8849330" },
            { "1+104", "1+105" },
            { "1.0-alpha1", "1.0-alpha2" }

        };
    }

    /**
     * Test compareTo with ordered versions.
     */
    @Test(dataProvider = "orderedVersions")
    public void testCompareOrderedVersions(String vs1, String vs2) {

        Version v1 = Version.parse(vs1);
        assertTrue(v1.compareTo(v1) == 0);

        Version v2 = Version.parse(vs2);
        assertTrue(v2.compareTo(v2) == 0);

        // v1 < v2
        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);

    }

    // equal version strings
    @DataProvider(name = "equalVersions")
    public Object[][] equalVersions() {
        return new Object[][]{

            { "1",             "1.0.0" },
            { "1.0",           "1.0.0" },
            { "1.0-beta",      "1.0.0-beta" },

        };
    }

    /**
     * Test compareTo with equal versions.
     */
    @Test(dataProvider = "equalVersions")
    public void testCompareEqualsVersions(String vs1, String vs2) {

        Version v1 = Version.parse(vs1);
        assertTrue(v1.compareTo(v1) == 0);

        Version v2 = Version.parse(vs2);
        assertTrue(v2.compareTo(v2) == 0);

        assertTrue(v1.compareTo(v2) == 0);
        assertTrue(v2.compareTo(v1) == 0);
        assertEquals(v1, v2);
        assertEquals(v2, v1);

    }

}
