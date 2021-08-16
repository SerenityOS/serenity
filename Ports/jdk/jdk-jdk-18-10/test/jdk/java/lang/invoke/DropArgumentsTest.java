/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/* @test
 * @bug  8158169
 * @summary unit tests for java.lang.invoke.MethodHandles
 * @run testng test.java.lang.invoke.DropArgumentsTest
 */
package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.List;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.*;

public class DropArgumentsTest {

    @Test
    public void testDropArgumentsToMatch() throws Throwable {
        MethodHandle cat = lookup().findVirtual(String.class, "concat", methodType(String.class, String.class));
        MethodType bigType = cat.type().insertParameterTypes(0, String.class, String.class, int.class);
        MethodHandle d0 = MethodHandles.dropArgumentsToMatch(cat, 0, bigType.parameterList(), 3);
        assertEquals("xy",(String)d0.invokeExact("m", "n", 1, "x", "y"));
        MethodHandle d1 = MethodHandles.dropArgumentsToMatch(cat, 0, bigType.parameterList(), 0);
        assertEquals("mn",(String)d1.invokeExact("m", "n", 1, "x", "y"));
        MethodHandle d2 = MethodHandles.dropArgumentsToMatch(cat, 1, bigType.parameterList(), 4);
        assertEquals("xy",(String)d2.invokeExact("x", "b", "c", 1, "a", "y"));

    }

    @DataProvider(name = "dropArgumentsToMatchNPEData")
    private Object[][] dropArgumentsToMatchNPEData()
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandle cat = lookup().findVirtual(String.class, "concat", methodType(String.class, String.class));
        return new Object[][] {
                { (MethodHandle) null, 0, cat.type().parameterList(), 0 },
                { cat, 0, null, 0 }
        };
    }

    @Test(dataProvider = "dropArgumentsToMatchNPEData", expectedExceptions = { NullPointerException.class })
    public void dropArgumentsToMatchNPE(MethodHandle target, int pos, List<Class<?>> valueType, int skip) {
        MethodHandles.dropArgumentsToMatch(target, pos, valueType , skip);
    }

    @DataProvider(name = "dropArgumentsToMatchIAEData")
    private Object[][] dropArgumentsToMatchIAEData()
        throws NoSuchMethodException, IllegalAccessException {
        MethodHandle cat = lookup().findVirtual(String.class, "concat", methodType(String.class, String.class));
        MethodType bigType = cat.type().insertParameterTypes(0, String.class, String.class, int.class);
        return new Object[][] {
            {cat, -1, bigType.parameterList(), 0},
            {cat, 0, bigType.parameterList(), -1},
            {cat, 3, bigType.parameterList(), 0},
            {cat, 0, bigType.parameterList(), 6},
            {cat, 0, bigType.parameterList(), 2}
        };
    }

    @Test(dataProvider = "dropArgumentsToMatchIAEData", expectedExceptions = { IllegalArgumentException.class })
    public void dropArgumentsToMatchIAE(MethodHandle target, int pos, List<Class<?>> valueType, int skip) {
        MethodHandles.dropArgumentsToMatch(target, pos, valueType , skip);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void dropArgumentsToMatchTestWithVoid() throws Throwable {
        MethodHandle cat = lookup().findVirtual(String.class, "concat",
                                   MethodType.methodType(String.class, String.class));
        MethodType bigTypewithVoid = cat.type().insertParameterTypes(0, void.class, String.class, int.class);
        MethodHandle handle2 = MethodHandles.dropArgumentsToMatch(cat, 0, bigTypewithVoid.parameterList(), 1);
    }

    public static class MethodSet {

        static void mVoid() {

        }

        static void mVoid(int t) {

        }
    }

    @Test
    public void dropArgumentsToMatchPosSkipRange() throws Throwable {
        // newTypes.size() == 1, pos == 1   &&   target.paramSize() == 0, skip == 0
        MethodHandle mh1 = MethodHandles.lookup().findStatic(MethodSet.class, "mVoid",
                                                             MethodType.methodType(void.class));
        MethodHandle handle1 = dropArgumentsToMatch(mh1, 0, Collections.singletonList(int.class), 1);
        assertEquals(1, handle1.type().parameterList().size());

        // newTypes.size() == 1, pos == 0   &&   target.paramSize() == 1, skip == 1
        MethodHandle mh2 = MethodHandles.lookup().findStatic(MethodSet.class, "mVoid",
                                                             MethodType.methodType(void.class, int.class));
        MethodHandle handle2 = dropArgumentsToMatch(mh2, 1, Collections.singletonList(int.class), 0);
        assertEquals(2, handle2.type().parameterList().size());
    }
}
