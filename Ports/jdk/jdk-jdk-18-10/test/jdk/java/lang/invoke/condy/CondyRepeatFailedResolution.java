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
 * @bug 8186211
 * @summary Test basic invocation of multiple ldc's of the same dynamic constant that fail resolution
 * @library /lib/testlibrary/bytecode /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder
 * @run testng CondyRepeatFailedResolution
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyRepeatFailedResolution
 */

import jdk.experimental.bytecode.BasicClassBuilder;
import jdk.experimental.bytecode.Flag;
import jdk.experimental.bytecode.TypedCodeBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Test
public class CondyRepeatFailedResolution {
    // Counter used to determine if a given BSM is invoked more than once
    static int bsm_called = 0;

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
                                       int value) throws Throwable {
        ++bsm_called;
        // replace constantName with a bogus value to trigger failed resolution
        constantName = "Foo";

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
                throw new BootstrapMethodError("Failure to generate a dynamic constant");
        }
    }

    @BeforeClass
    public void generateClass() throws Exception {
        String genClassName = CondyRepeatFailedResolution.class.getSimpleName() + "$Code";
        String bsmClassName = CondyRepeatFailedResolution.class.getCanonicalName().replace('.', '/');
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

        bsm_called = 0;
        try {
            Object r1 = m.invoke(null);
            Assert.fail("InvocationTargetException expected to be thrown after first invocation");
        } catch (InvocationTargetException e1) {
            // bsm_called should have been incremented prior to the exception
            Assert.assertEquals(bsm_called, 1);
            Assert.assertTrue(e1.getCause() instanceof BootstrapMethodError);
            // Try invoking method again to ensure that the bootstrap
            // method is not invoked twice and a resolution failure
            // results.
            try {
                Object r2 = m.invoke(null);
                Assert.fail("InvocationTargetException expected to be thrown after second invocation");
            } catch (InvocationTargetException e2) {
                // bsm_called should remain at 1 since the bootstrap
                // method should not have been invoked.
                Assert.assertEquals(bsm_called, 1);
                Assert.assertTrue(e2.getCause() instanceof BootstrapMethodError);
            } catch (Throwable t2) {
                Assert.fail("InvocationTargetException expected to be thrown");
            }
        } catch (Throwable t1) {
                Assert.fail("InvocationTargetException expected to be thrown");
        }
    }
}
