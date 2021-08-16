/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.management.loading.MLet;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;

import static org.testng.Assert.*;

/*
 * @test
 * @bug 8058089
 * @summary Tests various internal functions provided by MLet for correctness
 * @author Jaroslav Bachorik
 * @modules java.management/javax.management.loading:open
 * @run testng MLetInternalsTest
 */
public class MLetInternalsTest {
    private final static String CONSTRUCT_PARAMETER = "constructParameter";

    private final static Map<String, Method> testedMethods = new HashMap<>();

    @BeforeClass
    public static void setupClass() {
        testedMethods.clear();
        try {
            Method m = MLet.class.getDeclaredMethod(
                    CONSTRUCT_PARAMETER,
                    String.class, String.class
            );
            m.setAccessible(true);

            testedMethods.put(CONSTRUCT_PARAMETER, m);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private MLet mlet;

    @BeforeTest
    public void setupTest() {
        mlet = new MLet();
    }

    @Test
    public void testConstructParameter() throws Exception {
        assertEquals(constructParameter("120", "int"), 120);
        assertEquals(constructParameter("120", "java.lang.Integer"), Integer.valueOf(120));
        assertEquals(constructParameter("120", "long"), 120L);
        assertEquals(constructParameter("120", "java.lang.Long"), Long.valueOf(120));
        assertEquals(constructParameter("120.0", "float"), 120.0f);
        assertEquals(constructParameter("120.0", "java.lang.Float"), Float.valueOf(120.0f));
        assertEquals(constructParameter("120.0", "double"), 120.0d);
        assertEquals(constructParameter("120", "java.lang.Double"), Double.valueOf(120d));
        assertEquals(constructParameter("120", "java.lang.String"), "120");
        assertEquals(constructParameter("120", "byte"), (byte)120);
        assertEquals(constructParameter("120", "java.lang.Byte"), (byte)120);
        assertEquals(constructParameter("120", "short"), (short)120);
        assertEquals(constructParameter("120", "java.lang.Short"), (short)120);
        assertEquals(constructParameter("true", "boolean"), true);
        assertEquals(constructParameter("true", "java.lang.Boolean"), Boolean.valueOf(true));
    }

    private Object constructParameter(String param, String type) throws Exception {
        return testedMethods.get(CONSTRUCT_PARAMETER).invoke(mlet, param, type);
    }
}
