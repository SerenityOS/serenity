/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import sun.management.StackTraceElementCompositeData;

import org.testng.annotations.*;
import static org.testng.Assert.*;

/*
 * @test
 * @bug     8139587 8212197
 * @modules java.management/sun.management
 * @summary Check backward compatibility of StackTraceElementCompositeData
 * @author  Jaroslav Bachorik
 *
 * @run testng CompatibilityTest
 */

public class CompatibilityTest {
    private static CompositeType compositeTypeV6;
    private static CompositeType compositeType;

    // Attribute names
    private static final String CLASS_LOADER_NAME = "classLoaderName";
    private static final String MODULE_NAME       = "moduleName";
    private static final String MODULE_VERSION    = "moduleVersion";
    private static final String CLASS_NAME        = "className";
    private static final String METHOD_NAME       = "methodName";
    private static final String FILE_NAME         = "fileName";
    private static final String LINE_NUMBER       = "lineNumber";
    private static final String NATIVE_METHOD     = "nativeMethod";

    @BeforeClass
    public static void setup() throws Exception {
        String[] v6Names = {
            CLASS_NAME, METHOD_NAME, FILE_NAME, NATIVE_METHOD, LINE_NUMBER
        };
        String[] names = {
            CLASS_LOADER_NAME, MODULE_NAME, MODULE_VERSION,
            CLASS_NAME, METHOD_NAME, FILE_NAME, NATIVE_METHOD, LINE_NUMBER
        };
        compositeTypeV6 = new CompositeType(
            StackTraceElement.class.getName(),
            "StackTraceElement",
            v6Names,
            v6Names,
            new OpenType[] {
                SimpleType.STRING,
                SimpleType.STRING,
                SimpleType.STRING,
                SimpleType.BOOLEAN,
                SimpleType.INTEGER
            }
        );
        compositeType = new CompositeType(
            StackTraceElement.class.getName(),
            "StackTraceElement",
            names,
            names,
            new OpenType[] {
                SimpleType.STRING,
                SimpleType.STRING,
                SimpleType.STRING,
                SimpleType.STRING,
                SimpleType.STRING,
                SimpleType.STRING,
                SimpleType.BOOLEAN,
                SimpleType.INTEGER
            }
        );
    }

    private static CompositeData makeCompositeDataV6() throws Exception {
        Map<String, Object> itemsV6 = new HashMap<>();
        itemsV6.put(CLASS_NAME, "MyClass");
        itemsV6.put(METHOD_NAME, "myMethod");
        itemsV6.put(FILE_NAME, "MyClass.java");
        itemsV6.put(NATIVE_METHOD, false);
        itemsV6.put(LINE_NUMBER, 123);

        return new CompositeDataSupport(compositeTypeV6, itemsV6);
    }

    private static CompositeData makeCompositeData() throws Exception {
        Map<String, Object> items = new HashMap<>();
        items.put(CLASS_LOADER_NAME, "app");
        items.put(MODULE_NAME, "m");
        items.put(MODULE_VERSION, "1.0");
        items.put(CLASS_NAME, "MyClass");
        items.put(METHOD_NAME, "myMethod");
        items.put(FILE_NAME, "MyClass.java");
        items.put(NATIVE_METHOD, false);
        items.put(LINE_NUMBER, 123);

        return new CompositeDataSupport(compositeType, items);
    }

    @Test
    public void testV6Compatibility() throws Exception {
        StackTraceElement ste = StackTraceElementCompositeData.from(makeCompositeDataV6());

        assertNotNull(ste);
        assertEquals(ste.getClassName(), "MyClass");
        assertEquals(ste.getMethodName(), "myMethod");
        assertEquals(ste.getFileName(), "MyClass.java");
        assertEquals(ste.isNativeMethod(), false);
        assertEquals(ste.getLineNumber(), 123);

        assertNull(ste.getModuleName());
        assertNull(ste.getModuleVersion());
    }

    @Test
    public void test() throws Exception {
        StackTraceElement ste = StackTraceElementCompositeData.from(makeCompositeData());

        assertNotNull(ste);

        assertEquals(ste.getModuleName(), "m");
        assertEquals(ste.getModuleVersion(), "1.0");
        assertEquals(ste.getClassLoaderName(), "app");

        assertEquals(ste.getClassName(), "MyClass");
        assertEquals(ste.getMethodName(), "myMethod");
        assertEquals(ste.getFileName(), "MyClass.java");
        assertEquals(ste.isNativeMethod(), false);
        assertEquals(ste.getLineNumber(), 123);
    }

    @Test
    public void testCompositeData() throws Exception {
        StackTraceElement ste = new StackTraceElement("app",
                                                      "m", "1.0",
                                                      "p.MyClass", "myMethod",
                                                      "MyClass.java", 123);
        CompositeData cd = StackTraceElementCompositeData.toCompositeData(ste);
        StackTraceElement ste1 = StackTraceElementCompositeData.from(cd);
        assertEquals(ste, ste1);
    }
}

