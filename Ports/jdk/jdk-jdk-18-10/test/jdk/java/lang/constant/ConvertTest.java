/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng ConvertTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.ConstantBootstraps;
import java.math.BigInteger;

import static org.testng.Assert.assertEquals;

public class ConvertTest {

    @DataProvider
    public static Object[][] cceInputs() {
        return new Object[][]{
            { void.class, null },
            { Integer.class, "a" },
            { int.class, BigInteger.ZERO },
        };
    }

    @Test(dataProvider = "cceInputs", expectedExceptions = ClassCastException.class)
    public void testBadConversion(Class<?> dstType, Object value) {
        ConstantBootstraps.explicitCast(null, null, dstType, value);
    }

    @DataProvider
    public static Object[][] goodInputs() {
        Object o = new Object();
        return new Object[][]{
            { Object.class, null, null },
            { Object.class, o, o },
            { String.class, "abc", "abc" },
            { short.class, 10, (short) 10 },
            { int.class, (short) 10, 10 },
            { boolean.class, 1, true },
            { boolean.class, 2, false },
            { int.class, true, 1 },
            { int.class, false, 0 },
            { int.class, 10, 10 },
            { Integer.class, 10, 10 },
            { Object.class, 10, 10 },
            { Number.class, 10, 10 },
        };
    }

    @Test(dataProvider = "goodInputs")
    public void testSuccess(Class<?> dstType, Object value, Object expected) {
        Object actual = ConstantBootstraps.explicitCast(null, null, dstType, value);
        assertEquals(actual, expected);
    }

}
