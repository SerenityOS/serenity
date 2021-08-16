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
 **/

/* @test
 * @summary unit tests for arity limits of methods in java.lang.invoke.MethodHandles
 * @run junit/othervm test.java.lang.invoke.MethodHandlesArityLimitsTest
 **/

package test.java.lang.invoke;

import org.junit.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.List;

import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class MethodHandlesArityLimitsTest {

    private static MethodType mt254 = null;
    private static MethodType mt255 = null;

    static {
        Class<?>[] classes254 = IntStream.range(0, 254)
                                         .mapToObj(i -> int.class)
                                         .toArray(Class[]::new);
        mt254 = MethodType.methodType(void.class, classes254);
        mt255 = mt254.appendParameterTypes(int.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDropArgumentsToMatch() {
        MethodHandles.dropArgumentsToMatch(MethodHandles.empty(mt254), 0, mt255.parameterList(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmpty() {
        MethodHandles.empty(mt255);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExplicitCastArguments() {
        MethodHandles.explicitCastArguments(
                                         MethodHandles.empty(mt254),
                                         mt254.dropParameterTypes(0, 1).insertParameterTypes(0, long.class) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPermuteArguments() {
        MethodHandles.permuteArguments(
                                    MethodHandles.empty(MethodType.methodType(void.class)),
                                    mt255);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVarHandleInvoker() {
        MethodHandles.varHandleInvoker(VarHandle.AccessMode.GET, mt254);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVarHandleExactInvoker() {
        MethodHandles.varHandleExactInvoker(VarHandle.AccessMode.GET, mt254);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMHExactInvoker() {
        MethodHandles.exactInvoker(mt255);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMHInvoker() {
        MethodHandles.invoker(mt255);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMHSpreadInvoker() {
        MethodHandles.spreadInvoker(mt255, 255);
    }

    @Test(expected = WrongMethodTypeException.class)
    public void testAsType() throws ReflectiveOperationException {
        MethodHandle asList = MethodHandles.lookup().findStatic(
                                                java.util.Arrays.class,
                                                "asList",
                                                MethodType.methodType(List.class, Object[].class));
        try {
           asList.asType(MethodType.genericMethodType(254));//does not throw IAE or WMTE
        }
        catch(WrongMethodTypeException wmte) {
           throw new AssertionError("Unexpected WrongMethodTypeException thrown", wmte);
        }
        asList.asType(MethodType.genericMethodType(255));//throws WMTE
    }
}
