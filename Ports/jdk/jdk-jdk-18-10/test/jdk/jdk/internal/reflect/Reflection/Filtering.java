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

/**
 * @test
 * @bug 8210496
 * @modules java.base/jdk.internal.reflect
 * @run testng Filtering
 * @summary Test that security sensitive fields that filtered by core reflection
 */

import java.lang.reflect.*;
import java.lang.invoke.MethodHandles.Lookup;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

public class Filtering {

    @DataProvider(name = "sensitiveClasses")
    private Object[][] sensitiveClasses() {
        return new Object[][]{
            { jdk.internal.reflect.Reflection.class, null },
            { AccessibleObject.class, null },
            { ClassLoader.class, null },
            { Constructor.class, null },
            { Field.class, null },
            { Method.class, null },
        };
    }

    @DataProvider(name = "sensitiveFields")
    private Object[][] sensitiveFields() {
        return new Object[][]{
            { AccessibleObject.class, "override" },
            { Class.class, "classLoader" },
            { Class.class, "classData" },
            { ClassLoader.class, "parent" },
            { Field.class, "clazz" },
            { Field.class, "modifiers" },
            { Lookup.class, "lookupClass" },
            { Lookup.class, "allowedModes" },
            { Method.class, "clazz" },
            { Method.class, "modifiers" },
            { Module.class, "name" },
            { Module.class, "loader" },
            { System.class, "security" },
        };
    }

    @Test(dataProvider = "sensitiveClasses")
    public void testClass(Class<?> clazz, Object ignore) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        assertTrue(fields.length == 0);
    }

    @Test(dataProvider = "sensitiveFields",
          expectedExceptions = NoSuchFieldException.class)
    public void testField(Class<?> clazz, String name) throws Exception {
        clazz.getDeclaredField(name);
    }

}
