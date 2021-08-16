/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package test.java.lang.invoke.lib;

import jdk.experimental.bytecode.BasicClassBuilder;
import jdk.experimental.bytecode.BasicTypeHelper;
import jdk.experimental.bytecode.Flag;
import jdk.experimental.bytecode.PoolHelper;
import jdk.experimental.bytecode.TypedCodeBuilder;

import java.io.FileOutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.invoke.MethodType.fromMethodDescriptorString;
import static java.lang.invoke.MethodType.methodType;

public class InstructionHelper {

    static final BasicTypeHelper BTH = new BasicTypeHelper();

    static final AtomicInteger COUNT = new AtomicInteger();

    static BasicClassBuilder classBuilder(MethodHandles.Lookup l) {
        String className = l.lookupClass().getCanonicalName().replace('.', '/') + "$Code_" + COUNT.getAndIncrement();
        return new BasicClassBuilder(className, 55, 0)
                .withSuperclass("java/lang/Object")
                .withMethod("<init>", "()V", M ->
                        M.withFlags(Flag.ACC_PUBLIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.aload_0().invokespecial("java/lang/Object", "<init>", "()V", false).return_()
                                ));
    }

    public static MethodHandle invokedynamic(MethodHandles.Lookup l,
                                      String name, MethodType type,
                                      String bsmMethodName, MethodType bsmType,
                                      Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        byte[] byteArray = classBuilder(l)
                .withMethod("m", type.toMethodDescriptorString(), M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new,
                                          C -> {
                                              for (int i = 0; i < type.parameterCount(); i++) {
                                                  C.load(BTH.tag(cref(type.parameterType(i))), i);
                                              }
                                              C.invokedynamic(name, type.toMethodDescriptorString(),
                                                              csym(l.lookupClass()), bsmMethodName, bsmType.toMethodDescriptorString(),
                                                              staticArgs);
                                              C.return_(BTH.tag(cref(type.returnType())));
                                          }
                                ))
                .build();
        Class<?> gc = l.defineClass(byteArray);
        return l.findStatic(gc, "m", type);
    }

    public static MethodHandle ldcMethodHandle(MethodHandles.Lookup l,
                                        int refKind, Class<?> owner, String name, MethodType type) throws Exception {
        return ldc(l, MethodHandle.class,
                   P -> P.putHandle(refKind, csym(owner), name, type.toMethodDescriptorString()));
    }

    public static MethodHandle ldcDynamicConstant(MethodHandles.Lookup l,
                                                  String name, Class<?> type,
                                                  String bsmMethodName, MethodType bsmType,
                                                  Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        return ldcDynamicConstant(l, name, type, l.lookupClass(), bsmMethodName, bsmType, staticArgs);
    }

    public static MethodHandle ldcDynamicConstant(MethodHandles.Lookup l,
                                                  String name, Class<?> type,
                                                  Class<?> bsmClass, String bsmMethodName, MethodType bsmType,
                                                  Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        return ldcDynamicConstant(l, name, cref(type), csym(bsmClass), bsmMethodName, bsmType.toMethodDescriptorString(), staticArgs);
    }

    public static MethodHandle ldcDynamicConstant(MethodHandles.Lookup l,
                                                  String name, String type,
                                                  String bsmMethodName, String bsmType,
                                                  Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        return ldcDynamicConstant(l, name, type, csym(l.lookupClass()), bsmMethodName, bsmType, staticArgs);
    }

    public static MethodHandle ldcDynamicConstant(MethodHandles.Lookup l,
                                                  String name, String type,
                                                  String bsmClass, String bsmMethodName, String bsmType,
                                                  Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        return ldc(l, type,
                   P -> P.putDynamicConstant(name, type,
                                             bsmClass, bsmMethodName, bsmType,
                                             staticArgs));
    }

    public static MethodHandle ldc(MethodHandles.Lookup l,
                            Class<?> type,
                            Function<PoolHelper<String, String, byte[]>, Integer> poolFunc) throws Exception {
        return ldc(l, cref(type), poolFunc);
    }

    public static MethodHandle ldc(MethodHandles.Lookup l,
                                   String type,
                                   Function<PoolHelper<String, String, byte[]>, Integer> poolFunc) throws Exception {
        String methodType = "()" + type;
        byte[] byteArray = classBuilder(l)
                .withMethod("m", "()" + type, M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new,
                                          C -> {
                                              C.ldc(null, (P, v) -> poolFunc.apply(P));
                                              C.return_(BTH.tag(type));
                                          }
                                ))
                .build();
        Class<?> gc = l.defineClass(byteArray);
        return l.findStatic(gc, "m", fromMethodDescriptorString(methodType, l.lookupClass().getClassLoader()));
    }

    public static String csym(Class<?> c) {
        return c.getCanonicalName().replace('.', '/');
    }

    public static String cref(Class<?> c) {
        return methodType(c).toMethodDescriptorString().substring(2);
    }
}
