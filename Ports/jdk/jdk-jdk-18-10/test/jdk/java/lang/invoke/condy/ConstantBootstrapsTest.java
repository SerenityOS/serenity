/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186046 8195694
 * @summary Test dynamic constant bootstraps
 * @library /lib/testlibrary/bytecode /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run testng ConstantBootstrapsTest
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 ConstantBootstrapsTest
 */

import jdk.experimental.bytecode.PoolHelper;
import org.testng.annotations.Test;
import test.java.lang.invoke.lib.InstructionHelper;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test
public class ConstantBootstrapsTest {
    static final MethodHandles.Lookup L = MethodHandles.lookup();

    static MethodType lookupMT(Class<?> ret, Class<?>... params) {
        return MethodType.methodType(ret, MethodHandles.Lookup.class, String.class, Class.class).
                appendParameterTypes(params);
    }

    public void testNullConstant() throws Throwable {
        var handle = InstructionHelper.ldcDynamicConstant(L, "_", Object.class,
                                                          ConstantBootstraps.class, "nullConstant", lookupMT(Object.class),
                                                          S -> {});
        assertNull(handle.invoke());

        handle = InstructionHelper.ldcDynamicConstant(L, "_", MethodType.class,
                                                      ConstantBootstraps.class, "nullConstant", lookupMT(Object.class),
                                                      S -> {});
        assertNull(handle.invoke());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullConstantPrimitiveClass() {
        ConstantBootstraps.nullConstant(MethodHandles.lookup(), null, int.class);
    }


    public void testPrimitiveClass() throws Throwable {
        var pm = Map.of(
                "I", int.class,
                "J", long.class,
                "S", short.class,
                "B", byte.class,
                "C", char.class,
                "F", float.class,
                "D", double.class,
                "Z", boolean.class,
                "V", void.class
        );

        for (var desc : pm.keySet()) {
            var handle = InstructionHelper.ldcDynamicConstant(L, desc, Class.class,
                                                              ConstantBootstraps.class, "primitiveClass", lookupMT(Class.class),
                                                              S -> {});
            assertEquals(handle.invoke(), pm.get(desc));
        }
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testPrimitiveClassNullName() {
        ConstantBootstraps.primitiveClass(MethodHandles.lookup(), null, Class.class);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testPrimitiveClassNullType() {
        ConstantBootstraps.primitiveClass(MethodHandles.lookup(), "I", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPrimitiveClassEmptyName() {
        ConstantBootstraps.primitiveClass(MethodHandles.lookup(), "", Class.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPrimitiveClassWrongNameChar() {
        ConstantBootstraps.primitiveClass(MethodHandles.lookup(), "L", Class.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPrimitiveClassWrongNameString() {
        ConstantBootstraps.primitiveClass(MethodHandles.lookup(), "Ljava/lang/Object;", Class.class);
    }


    public void testEnumConstant() throws Throwable {
        for (var v : StackWalker.Option.values()) {
            var handle = InstructionHelper.ldcDynamicConstant(L, v.name(), StackWalker.Option.class,
                                                              ConstantBootstraps.class, "enumConstant", lookupMT(Enum.class),
                                                              S -> { });
            assertEquals(handle.invoke(), v);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEnumConstantUnknown() {
        ConstantBootstraps.enumConstant(MethodHandles.lookup(), "DOES_NOT_EXIST", StackWalker.Option.class);
    }


    public void testGetStaticDecl() throws Throwable {
        var handle = InstructionHelper.ldcDynamicConstant(L, "TYPE", Class.class,
                                                          ConstantBootstraps.class, "getStaticFinal", lookupMT(Object.class, Class.class),
                                                          S -> { S.add("java/lang/Integer", PoolHelper::putClass); });
        assertEquals(handle.invoke(), int.class);
    }

    public void testGetStaticSelf() throws Throwable {
        var handle = InstructionHelper.ldcDynamicConstant(L, "MAX_VALUE", int.class,
                                                          ConstantBootstraps.class, "getStaticFinal", lookupMT(Object.class),
                                                          S -> { });
        assertEquals(handle.invoke(), Integer.MAX_VALUE);


        handle = InstructionHelper.ldcDynamicConstant(L, "ZERO", BigInteger.class,
                                                      ConstantBootstraps.class, "getStaticFinal", lookupMT(Object.class),
                                                      S -> { });
        assertEquals(handle.invoke(), BigInteger.ZERO);
    }


    public void testInvoke() throws Throwable {
        var handle = InstructionHelper.ldcDynamicConstant(
                L, "_", List.class,
                ConstantBootstraps.class, "invoke", lookupMT(Object.class, MethodHandle.class, Object[].class),
                S -> {
                    S.add("", (P, Z) -> {
                        return P.putHandle(MethodHandleInfo.REF_invokeStatic, "java/util/List", "of",
                                           MethodType.methodType(List.class, Object[].class).toMethodDescriptorString(),
                                           true);
                    });
                    S.add(1).add(2).add(3).add(4);
                });
        assertEquals(handle.invoke(), List.of(1, 2, 3, 4));
    }

    public void testInvokeAsType() throws Throwable {
        var handle = InstructionHelper.ldcDynamicConstant(
                L, "_", int.class,
                ConstantBootstraps.class, "invoke", lookupMT(Object.class, MethodHandle.class, Object[].class),
                S -> {
                    S.add("", (P, Z) -> {
                        return P.putHandle(MethodHandleInfo.REF_invokeStatic, "java/lang/Integer", "valueOf",
                                           MethodType.methodType(Integer.class, String.class).toMethodDescriptorString(),
                                           false);
                    });
                    S.add("42");
                });
        assertEquals(handle.invoke(), 42);
    }

    public void testInvokeAsTypeVariableArity() throws Throwable {
        // The constant type is Collection but the invoke return type is List
        var handle = InstructionHelper.ldcDynamicConstant(
                L, "_", Collection.class,
                ConstantBootstraps.class, "invoke", lookupMT(Object.class, MethodHandle.class, Object[].class),
                S -> {
                    S.add("", (P, Z) -> {
                        return P.putHandle(MethodHandleInfo.REF_invokeStatic, "java/util/List", "of",
                                           MethodType.methodType(List.class, Object[].class).toMethodDescriptorString(),
                                           true);
                    });
                    S.add(1).add(2).add(3).add(4);
                });
        assertEquals(handle.invoke(), List.of(1, 2, 3, 4));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testInvokeAsTypeClassCast() throws Throwable {
        ConstantBootstraps.invoke(MethodHandles.lookup(), "_", String.class,
                                  MethodHandles.lookup().findStatic(Integer.class, "valueOf", MethodType.methodType(Integer.class, String.class)),
                                  "42");
    }

    @Test(expectedExceptions = WrongMethodTypeException.class)
    public void testInvokeAsTypeWrongReturnType() throws Throwable {
        ConstantBootstraps.invoke(MethodHandles.lookup(), "_", short.class,
                                  MethodHandles.lookup().findStatic(Integer.class, "parseInt", MethodType.methodType(int.class, String.class)),
                                  "42");
    }


    static class X {
        public String f;
        public static String sf;
    }

    public void testVarHandleField() throws Throwable {
        var handle = InstructionHelper.ldcDynamicConstant(
                L, "f", VarHandle.class,
                ConstantBootstraps.class, "fieldVarHandle", lookupMT(VarHandle.class, Class.class, Class.class),
                S -> {
                    S.add(X.class.getName().replace('.', '/'), PoolHelper::putClass).
                            add("java/lang/String", PoolHelper::putClass);
                });

        var vhandle = (VarHandle) handle.invoke();
        assertEquals(vhandle.varType(), String.class);
        assertEquals(vhandle.coordinateTypes(), List.of(X.class));
    }

    public void testVarHandleStaticField() throws Throwable {
        var handle = InstructionHelper.ldcDynamicConstant(
                L, "sf", VarHandle.class,
                ConstantBootstraps.class, "staticFieldVarHandle", lookupMT(VarHandle.class, Class.class, Class.class),
                S -> {
                    S.add(X.class.getName().replace('.', '/'), PoolHelper::putClass).
                            add("java/lang/String", PoolHelper::putClass);
                });

        var vhandle = (VarHandle) handle.invoke();
        assertEquals(vhandle.varType(), String.class);
        assertEquals(vhandle.coordinateTypes(), List.of());
    }

    public void testVarHandleArray() throws Throwable {
        var handle = InstructionHelper.ldcDynamicConstant(
                L, "_", VarHandle.class,
                ConstantBootstraps.class, "arrayVarHandle", lookupMT(VarHandle.class, Class.class),
                S -> {
                    S.add(String[].class.getName().replace('.', '/'), PoolHelper::putClass);
                });

        var vhandle = (VarHandle) handle.invoke();
        assertEquals(vhandle.varType(), String.class);
        assertEquals(vhandle.coordinateTypes(), List.of(String[].class, int.class));
    }
}
