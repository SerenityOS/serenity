/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8184119
 * @summary test permutation when return value is directly derived from an argument
 * @run testng/othervm test.java.lang.invoke.PermuteArgsReturnVoidTest
 */


package test.java.lang.invoke;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.identity;

public class PermuteArgsReturnVoidTest {

    static String consumeIdentity(String s, int i1, int i2) {
        return s;
    }

    static void consumeVoid(String s, int i1, int i2) {
    }

    @Test
    public void testReturnOnStack() throws Throwable {
        MethodHandles.Lookup l = MethodHandles.lookup();

        MethodHandle consumeIdentity = l.findStatic(
                PermuteArgsReturnVoidTest.class, "consumeIdentity",
                MethodType.methodType(String.class, String.class, int.class, int.class));
        MethodHandle consumeVoid = l.findStatic(
                PermuteArgsReturnVoidTest.class, "consumeVoid",
                MethodType.methodType(void.class, String.class, int.class, int.class));

        MethodHandle f = MethodHandles.foldArguments(consumeIdentity, consumeVoid);

        MethodHandle p = MethodHandles.permuteArguments(f, MethodType.methodType(String.class, String.class, int.class, int.class), 0, 2, 1);

        String s = (String) p.invoke("IN", 0, 0);
        Assert.assertEquals(s.getClass(), String.class);
        Assert.assertEquals(s, "IN");
    }

    @Test
    public void testReturnFromArg() throws Throwable {
        MethodHandles.Lookup l = MethodHandles.lookup();

        MethodHandle consumeIdentity = dropArguments(
                identity(String.class), 1, int.class, int.class);
        MethodHandle consumeVoid = l.findStatic(
                PermuteArgsReturnVoidTest.class, "consumeVoid",
                MethodType.methodType(void.class, String.class, int.class, int.class));

        MethodHandle f = MethodHandles.foldArguments(consumeIdentity, consumeVoid);

        MethodHandle p = MethodHandles.permuteArguments(f, MethodType.methodType(String.class, String.class, int.class, int.class), 0, 2, 1);

        String s = (String) p.invoke("IN", 0, 0);
        Assert.assertEquals(s.getClass(), String.class);
        Assert.assertEquals(s, "IN");
    }
}
