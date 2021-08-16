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
 * @summary Stress test ldc to ensure HotSpot correctly manages oop maps
 * @library /lib/testlibrary/bytecode /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run testng CondyWithGarbageTest
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyWithGarbageTest
 */


import jdk.experimental.bytecode.BasicClassBuilder;
import jdk.experimental.bytecode.Flag;
import jdk.experimental.bytecode.TypedCodeBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;
import static test.java.lang.invoke.lib.InstructionHelper.cref;
import static test.java.lang.invoke.lib.InstructionHelper.csym;

public class CondyWithGarbageTest {
    static final MethodHandles.Lookup L = MethodHandles.lookup();

    @Test
    public void testString() throws Throwable {
        MethodHandle mh = lcdStringBasher();
        int l = 0;
        for (int i = 0; i < 100000; i++) {
            l += +((String) mh.invoke()).length();
        }
        Assert.assertTrue(l > 0);
    }

    public static Object bsmString(MethodHandles.Lookup l,
                                   String constantName,
                                   Class<?> constantType) {
        return new StringBuilder(constantName).toString();
    }

    static MethodHandle lcdStringBasher() throws Exception {
        byte[] byteArray = new BasicClassBuilder(csym(L.lookupClass()) + "$Code$String", 55, 0)
                .withSuperclass("java/lang/Object")
                .withMethod("<init>", "()V", M ->
                        M.withFlags(Flag.ACC_PUBLIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.aload_0().invokespecial("java/lang/Object", "<init>", "()V", false).return_()
                                ))
                .withMethod("m", "()" + cref(String.class), M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C -> {
                                              C.new_(csym(StringBuilder.class))
                                                      .dup()
                                                      .invokespecial(csym(StringBuilder.class), "<init>", "()V", false)
                                                      .astore_0();

                                              for (int i = 10; i < 100; i++) {
                                                  ldcString(C, Integer.toString(i));
                                                  C.astore_1().aload_0().aload_1();
                                                  C.invokevirtual(csym(StringBuilder.class), "append", methodType(StringBuilder.class, String.class).toMethodDescriptorString(), false);
                                                  C.pop();
                                              }

                                              C.aload_0();
                                              C.invokevirtual(csym(StringBuilder.class), "toString", methodType(String.class).toMethodDescriptorString(), false);
                                              C.areturn();
                                          }
                                ))
                .build();

        Class<?> gc = L.defineClass(byteArray);
        return L.findStatic(gc, "m", methodType(String.class));
    }

    static void ldcString(TypedCodeBuilder<String, String, byte[], ?> C, String name) {
        C.ldc(name, cref(String.class),
              csym(L.lookupClass()),
              "bsmString",
              methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class).toMethodDescriptorString(),
              S -> {
              });
    }


    @Test
    public void testStringArray() throws Throwable {
        MethodHandle mh = lcdStringArrayBasher();
        int l = 0;
        for (int i = 0; i < 100000; i++) {
            l += +((String) mh.invoke()).length();
        }
        Assert.assertTrue(l > 0);
    }

    public static Object bsmStringArray(MethodHandles.Lookup l,
                                        String constantName,
                                        Class<?> constantType) {
        return new String[]{new StringBuilder(constantName).toString()};
    }

    static MethodHandle lcdStringArrayBasher() throws Exception {
        byte[] byteArray = new BasicClassBuilder(csym(L.lookupClass()) + "$Code$StringArray", 55, 0)
                .withSuperclass("java/lang/Object")
                .withMethod("<init>", "()V", M ->
                        M.withFlags(Flag.ACC_PUBLIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.aload_0().invokespecial("java/lang/Object", "<init>", "()V", false).return_()
                                ))
                .withMethod("m", "()" + cref(String.class), M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C -> {
                                              C.new_(csym(StringBuilder.class))
                                                      .dup()
                                                      .invokespecial(csym(StringBuilder.class), "<init>", "()V", false)
                                                      .astore_0();

                                              for (int i = 10; i < 100; i++) {
                                                  ldcStringArray(C, Integer.toString(i));
                                                  C.bipush(0).aaload().astore_1();
                                                  C.aload_0().aload_1();
                                                  C.invokevirtual(csym(StringBuilder.class), "append", methodType(StringBuilder.class, String.class).toMethodDescriptorString(), false);
                                                  C.pop();
                                              }

                                              C.aload_0();
                                              C.invokevirtual(csym(StringBuilder.class), "toString", methodType(String.class).toMethodDescriptorString(), false);
                                              C.areturn();
                                          }
                                ))
                .build();

        Class<?> gc = L.defineClass(byteArray);
        return L.findStatic(gc, "m", methodType(String.class));
    }

    static void ldcStringArray(TypedCodeBuilder<String, String, byte[], ?> C, String name) {
        C.ldc(name, cref(String[].class),
              csym(L.lookupClass()),
              "bsmStringArray",
              methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class).toMethodDescriptorString(),
              S -> {
              });
    }
}
