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

/*
 * @test
 * @bug 8186046
 * @summary Test bootstrap methods throwing an exception
 * @library /lib/testlibrary/bytecode /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run testng BootstrapMethodJumboArgsTest
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 BootstrapMethodJumboArgsTest
 */

import jdk.experimental.bytecode.PoolHelper;
import org.testng.Assert;
import org.testng.annotations.Test;
import test.java.lang.invoke.lib.InstructionHelper;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodType.methodType;

public class BootstrapMethodJumboArgsTest {
    static final MethodHandles.Lookup L = MethodHandles.lookup();


    static Object bsmZero(MethodHandles.Lookup l, String name, Object type,
                      Object... args) {
        Object[] a = args.clone();
        if (type instanceof MethodType) {
            return new ConstantCallSite(MethodHandles.constant(Object[].class, a));
        }
        else {
            return a;
        }
    }

    static Object bsmOne(MethodHandles.Lookup l, String name, Object type,
                         Object first, Object... args) {
        Object[] a = new Object[args.length + 1];
        a[0] = first;
        System.arraycopy(args, 0, a, 1, args.length);
        if (type instanceof MethodType) {
            return new ConstantCallSite(MethodHandles.constant(Object[].class, a));
        }
        else {
            return a;
        }
    }

    static Object bsmTwo(MethodHandles.Lookup l, String name, Object type,
                         Object first, Object second, Object... args) {
        Object[] a = new Object[args.length + 2];
        a[0] = first;
        a[1] = second;
        System.arraycopy(args, 0, a, 2, args.length);
        if (type instanceof MethodType) {
            return new ConstantCallSite(MethodHandles.constant(Object[].class, a));
        }
        else {
            return a;
        }
    }

    static void manyStaticStrings(String[] args, PoolHelper.StaticArgListBuilder<String, String, byte[]> staticArgs) {
        for (String s : args) {
            staticArgs.add(s);
        }
    }

    @Test
    public void testCondyWithJumboArgs() throws Throwable {
        String[] expected = IntStream.range(0, 1000).mapToObj(Integer::toString).toArray(String[]::new);

        {
            MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                    L, "name", Object[].class,
                    "bsmZero", methodType(Object.class, MethodHandles.Lookup.class, String.class, Object.class, Object[].class),
                    S -> manyStaticStrings(expected, S));

            Object[] actual = (Object[]) mh.invoke();
            Assert.assertEquals(actual, expected);
        }

        {
            MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                    L, "name", Object[].class,
                    "bsmOne", methodType(Object.class, MethodHandles.Lookup.class, String.class, Object.class, Object.class, Object[].class),
                    S -> manyStaticStrings(expected, S));

            Object[] actual = (Object[]) mh.invoke();
            Assert.assertEquals(actual, expected);
        }

        {
            MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                    L, "name", Object[].class,
                    "bsmTwo", methodType(Object.class, MethodHandles.Lookup.class, String.class, Object.class, Object.class, Object.class, Object[].class),
                    S -> manyStaticStrings(expected, S));

            Object[] actual = (Object[]) mh.invoke();
            Assert.assertEquals(actual, expected);
        }
    }

    @Test
    public void testIndyWithJumboArgs() throws Throwable {
        String[] expected = IntStream.range(0, 1000).mapToObj(Integer::toString).toArray(String[]::new);

        {
            MethodHandle mh = InstructionHelper.invokedynamic(
                    L, "name", methodType(Object[].class),
                    "bsmZero", methodType(Object.class, MethodHandles.Lookup.class, String.class, Object.class, Object[].class),
                    S -> manyStaticStrings(expected, S));

            Object[] actual = (Object[]) mh.invoke();
            Assert.assertEquals(actual, expected);
        }

        {
            MethodHandle mh = InstructionHelper.invokedynamic(
                    L, "name", methodType(Object[].class),
                    "bsmOne", methodType(Object.class, MethodHandles.Lookup.class, String.class, Object.class, Object.class, Object[].class),
                    S -> manyStaticStrings(expected, S));

            Object[] actual = (Object[]) mh.invoke();
            Assert.assertEquals(actual, expected);
        }

        {
            MethodHandle mh = InstructionHelper.invokedynamic(
                    L, "name", methodType(Object[].class),
                    "bsmTwo", methodType(Object.class, MethodHandles.Lookup.class, String.class, Object.class, Object.class, Object.class, Object[].class),
                    S -> manyStaticStrings(expected, S));

            Object[] actual = (Object[]) mh.invoke();
            Assert.assertEquals(actual, expected);
        }
    }
}
