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
 * @summary Test for condy BSMs returning primitive values or null
 * @library /lib/testlibrary/bytecode
 * @build jdk.experimental.bytecode.BasicClassBuilder
 * @run testng CondyReturnPrimitiveTest
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyReturnPrimitiveTest
 */

import jdk.experimental.bytecode.BasicClassBuilder;
import jdk.experimental.bytecode.Flag;
import jdk.experimental.bytecode.TypedCodeBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

@Test
public class CondyReturnPrimitiveTest {
    // Counter for number of BSM calls
    // Use of an AtomicInteger is not strictly necessary in this test
    // since the BSM is not be called concurrently, but in general
    // a BSM can be called concurrently for linking different or the *same*
    // constant so care should be taken if a BSM operates on shared state
    static final AtomicInteger callCount = new AtomicInteger();
    // Generated class with methods containing condy ldc
    Class<?> gc;

    // Bootstrap method used to represent primitive values
    // that cannot be represented directly in the constant pool,
    // such as byte, and for completeness of testing primitive values
    // that can be represented directly, such as double or long that
    // take two slots
    public static Object intConversion(MethodHandles.Lookup l,
                                       String constantName,
                                       Class<?> constantType,
                                       int value) {
        callCount.getAndIncrement();

        switch (constantName) {
            case "B":
                return (byte) value;
            case "C":
                return (char) value;
            case "D":
                return (double) value;
            case "F":
                return (float) value;
            case "I":
                return value;
            case "J":
                return (long) value;
            case "S":
                return (short) value;
            case "Z":
                return value > 0;
            case "nullRef":
                return null;
            case "string":
                return "string";
            case "stringArray":
                return new String[]{"string", "string"};
            default:
                throw new UnsupportedOperationException();
        }
    }

    @BeforeClass
    public void generateClass() throws Exception {
        String genClassName = CondyReturnPrimitiveTest.class.getSimpleName() + "$Code";
        String bsmClassName = CondyReturnPrimitiveTest.class.getCanonicalName().replace('.', '/');
        String bsmMethodName = "intConversion";
        String bsmDescriptor = MethodType.methodType(Object.class, MethodHandles.Lookup.class,
                                                     String.class, Class.class, int.class).toMethodDescriptorString();

        byte[] byteArray = new BasicClassBuilder(genClassName, 55, 0)
                .withSuperclass("java/lang/Object")
                .withMethod("<init>", "()V", M ->
                        M.withFlags(Flag.ACC_PUBLIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.aload_0().invokespecial("java/lang/Object", "<init>", "()V", false).return_()
                                ))
                .withMethod("B", "()B", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("B", "B", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Byte.MAX_VALUE))
                                                .ireturn()
                                ))
                .withMethod("C", "()C", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("C", "C", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Character.MAX_VALUE))
                                                .ireturn()
                                ))
                .withMethod("D", "()D", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("D", "D", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .dreturn()
                                ))
                .withMethod("D_AsType", "()D", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("I", "D", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .dreturn()
                                ))
                .withMethod("F", "()F", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("F", "F", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .freturn()
                                ))
                .withMethod("F_AsType", "()F", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("I", "F", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .freturn()
                                ))
                .withMethod("I", "()I", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("I", "I", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .ireturn()
                                ))
                .withMethod("J", "()J", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("J", "J", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .lreturn()
                                ))
                .withMethod("J_AsType", "()J", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("I", "J", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .lreturn()
                                ))
                .withMethod("S", "()S", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("S", "S", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Short.MAX_VALUE))
                                                .ireturn()
                                ))
                .withMethod("Z_F", "()Z", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("Z", "Z", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(0))
                                                .ireturn()
                                ))
                .withMethod("Z_T", "()Z", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("Z", "Z", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(1))
                                                .ireturn()
                                ))
                .withMethod("null", "()Ljava/lang/Object;", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("nullRef", "Ljava/lang/Object;", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .areturn()
                                ))
                .withMethod("string", "()Ljava/lang/String;", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("string", "Ljava/lang/String;", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .areturn()
                                ))
                .withMethod("stringArray", "()[Ljava/lang/String;", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("stringArray", "[Ljava/lang/String;", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> S.add(Integer.MAX_VALUE))
                                                .areturn()
                                ))
                .build();

        gc = MethodHandles.lookup().defineClass(byteArray);
    }

    @Test
    public void testPrimitives() throws Exception {
        testConstants();
        int expectedCallCount = callCount.get();

        // Ensure when run a second time that the bootstrap method is not
        // invoked and the constants are cached
        testConstants();
        Assert.assertEquals(callCount.get(), expectedCallCount);
    }

    @Test
    public void testRefs() throws Exception {
        testConstant("string", "string");
        testConstant("stringArray", new String[]{"string", "string"});
    }

    void testConstants() throws Exception {
        // Note: for the _asType methods the BSM returns an int which is
        // then converted by an asType transformation

        testConstant("B", Byte.MAX_VALUE);
        testConstant("C", Character.MAX_VALUE);
        testConstant("D", (double) Integer.MAX_VALUE);
        testConstant("D_AsType", (double) Integer.MAX_VALUE);
        testConstant("F", (float) Integer.MAX_VALUE);
        testConstant("F_AsType", (float) Integer.MAX_VALUE);
        testConstant("I", Integer.MAX_VALUE);
        testConstant("J", (long) Integer.MAX_VALUE);
        testConstant("J_AsType", (long) Integer.MAX_VALUE);
        testConstant("S", Short.MAX_VALUE);
        testConstant("Z_F", false);
        testConstant("Z_T", true);
        testConstant("null", null);
    }

    void testConstant(String name, Object expected) throws Exception {
        Method m = gc.getDeclaredMethod(name);
        Assert.assertEquals(m.invoke(null), expected);
    }
}
