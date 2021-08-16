/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Exercise Class#getModule
 * @modules java.desktop
 * @run testng GetModuleTest
 */

import java.awt.Component;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class GetModuleTest {

    private static final Module TEST_MODULE = GetModuleTest.class.getModule();


    @DataProvider(name = "testclasses")
    public Object[][] testClasses() {
        return new Object[][] {

            // unnamed module

            { GetModuleTest.class,      null },
            { GetModuleTest[].class,    null },
            { GetModuleTest[][].class,  null },

            // should return named module

            { int.class,            "java.base" },
            { int[].class,          "java.base" },
            { int[][].class,        "java.base" },
            { void.class,           "java.base" },

            { Object.class,         "java.base" },
            { Object[].class,       "java.base" },
            { Object[][].class,     "java.base" },
            { Component.class,      "java.desktop" },
            { Component[].class,    "java.desktop" },
            { Component[][].class,  "java.desktop" },
        };
    }

    @Test(dataProvider = "testclasses")
    public void testGetModule(Class<?> type, String expected) {
        Module m = type.getModule();
        assertNotNull(m);
        if (expected == null) {
            assertTrue(m == TEST_MODULE);
        } else {
            assertEquals(m.getName(), expected);
        }
    }

}
