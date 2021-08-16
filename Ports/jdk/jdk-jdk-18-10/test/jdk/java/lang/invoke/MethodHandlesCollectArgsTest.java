/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8259922
 * @run testng/othervm MethodHandlesCollectArgsTest
 */

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import static java.lang.invoke.MethodType.methodType;

import static org.testng.Assert.*;

public class MethodHandlesCollectArgsTest {

    private static final MethodHandle TARGET_II_I = MethodHandles.empty(methodType(int.class, int.class, int.class));
    private static final MethodHandle TARGET__V = MethodHandles.empty(methodType(void.class));
    private static final MethodHandle FILTER_INT = MethodHandles.empty(methodType(int.class, String.class));
    private static final MethodHandle FILTER_VOID = MethodHandles.empty(methodType(void.class, String.class));

    @DataProvider(name = "illegalPos")
    public static Object[][] illegalPos() {
        return new Object[][] {
            {TARGET_II_I, 2, FILTER_INT},
            {TARGET_II_I, 3, FILTER_VOID},
            {TARGET_II_I, -1, FILTER_INT},
            {TARGET_II_I, -1, FILTER_VOID},
            {TARGET__V, 0, FILTER_INT},
            {TARGET__V, 1, FILTER_VOID},
            {TARGET__V, -1, FILTER_VOID},
            {TARGET__V, -1, FILTER_VOID}
        };
    }

    @DataProvider(name = "validPos")
    public static Object[][] validPos() {
        return new Object[][] {
            {TARGET_II_I, 0, FILTER_INT, methodType(int.class, String.class, int.class)},
            {TARGET_II_I, 1, FILTER_INT, methodType(int.class, int.class, String.class)},
            {TARGET_II_I, 0, FILTER_VOID, methodType(int.class, String.class, int.class, int.class)},
            {TARGET_II_I, 1, FILTER_VOID, methodType(int.class, int.class, String.class, int.class)},
            {TARGET_II_I, 2, FILTER_VOID, methodType(int.class, int.class, int.class, String.class)},
            {TARGET__V, 0, FILTER_VOID, methodType(void.class, String.class)}
        };
    }

    @Test(dataProvider="illegalPos", expectedExceptions = {IllegalArgumentException.class})
    public void illegalPosition(MethodHandle target, int position, MethodHandle filter) {
        MethodHandles.collectArguments(target, position, filter);
    }

    @Test(dataProvider="validPos")
    public void legalPosition(MethodHandle target, int position, MethodHandle filter, MethodType expectedType) {
        MethodHandle result = MethodHandles.collectArguments(target, position, filter);
        assertEquals(result.type(), expectedType);
    }
}
