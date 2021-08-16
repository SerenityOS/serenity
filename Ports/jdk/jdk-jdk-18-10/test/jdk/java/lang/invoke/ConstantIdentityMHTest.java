/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary unit tests for java.lang.invoke.MethodHandles
 * @run testng/othervm -ea -esa test.java.lang.invoke.ConstantIdentityMHTest
 */
package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
import static org.testng.Assert.*;
import org.testng.annotations.*;

public class ConstantIdentityMHTest {

    @DataProvider(name = "testZeroData")
    private Object[][] testZeroData() {
       return new Object[][] {
           {void.class, "()void"},
           {int.class, "()int"},
           {byte.class, "()byte"},
           {short.class, "()short"},
           {long.class, "()long"},
           {float.class, "()float"},
           {double.class, "()double"},
           {boolean.class, "()boolean"},
           {char.class, "()char"},
           {Integer.class, "()Integer"}
       };
    }

    @Test(dataProvider = "testZeroData")
    public void testZero(Class<?> expectedtype, String expected) throws Throwable {
        assertEquals(MethodHandles.zero(expectedtype).type().toString(), expected);
    }

    @Test(expectedExceptions={ NullPointerException.class })
    public void testZeroNPE() {
        MethodHandle mh = MethodHandles.zero(null);
    }

    @Test
    void testEmpty() throws Throwable {
        MethodHandle cat = lookup().findVirtual(String.class, "concat", methodType(String.class, String.class));
        assertEquals((String)cat.invoke("x","y"), "xy");
        MethodHandle mhEmpty = MethodHandles.empty(cat.type());
        assertEquals((String)mhEmpty.invoke("x","y"), null);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    void testEmptyNPE() {
        MethodHandle lenEmptyMH = MethodHandles.empty(null);
    }
}
