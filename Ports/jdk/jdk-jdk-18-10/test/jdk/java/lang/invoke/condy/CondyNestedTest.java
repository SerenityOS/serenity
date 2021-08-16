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
 * @bug 8186046
 * @summary Test nested dynamic constant declarations that are recursive
 * @compile CondyNestedTest_Code.jcod
 * @run testng CondyNestedTest
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyNestedTest
 */

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CondyNestedTest {

    static final Class[] THROWABLES = {InvocationTargetException.class, StackOverflowError.class};

    Class<?> c;

// Add the following annotations to the test description if uncommenting the
// following code
//
// * @library /lib/testlibrary/bytecode
// * @build jdk.experimental.bytecode.BasicClassBuilder
//
//    static final MethodHandles.Lookup L = MethodHandles.lookup();
//
//    /**
//     * Generate class file bytes for a class named CondyNestedTest_Code
//     * whose bytes are converted to a jcod file:
//     *
//     * java -jar asmtools.jar jdec CondyNestedTest_Code.class >
//     * CondyNestedTest_Code.jcod
//     *
//     * which was then edited so that dynamic constant declarations are
//     * recursive both for an ldc or invokedynamic (specifically declaring a
//     * BSM+attributes whose static argument is a dynamic constant
//     * that refers to the same BSM+attributes).
//     */
//    public static byte[] generator() throws Exception {
//        String genClassName = L.lookupClass().getSimpleName() + "_Code";
//        String bsmDescriptor = MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Object.class, Object.class).toMethodDescriptorString();
//        String bsmIndyDescriptor = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, Object.class, Object.class).toMethodDescriptorString();
//
//        byte[] byteArray = new BasicClassBuilder(genClassName, 55, 0)
//                .withSuperclass("java/lang/Object")
//                .withMethod("<init>", "()V", M ->
//                        M.withFlags(Flag.ACC_PUBLIC)
//                                .withCode(TypedCodeBuilder::new, C ->
//                                        C.aload_0().invokespecial("java/lang/Object", "<init>", "()V", false).return_()
//                                ))
//                .withMethod("main", "([Ljava/lang/String;)V", M ->
//                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
//                                .withCode(TypedCodeBuilder::new, C -> {
//                                    C.aload_0().iconst_0().aaload();
//                                    C.invokevirtual("java/lang/String", "intern", "()Ljava/lang/String;", false);
//                                    C.astore_1();
//
//                                    C.aload_1();
//                                    C.ldc("condy_bsm_condy_bsm");
//                                    C.ifcmp(TypeTag.A, MacroCodeBuilder.CondKind.NE, "CASE1");
//                                    C.invokestatic(genClassName, "condy_bsm_condy_bsm", "()Ljava/lang/Object;", false).return_();
//
//                                    C.label("CASE1");
//                                    C.aload_1();
//                                    C.ldc("indy_bsmIndy_condy_bsm");
//                                    C.ifcmp(TypeTag.A, MacroCodeBuilder.CondKind.NE, "CASE2");
//                                    C.invokestatic(genClassName, "indy_bsmIndy_condy_bsm", "()Ljava/lang/Object;", false).return_();
//
//                                    C.label("CASE2");
//                                    C.aload_1();
//                                    C.ldc("indy_bsm_condy_bsm");
//                                    C.ifcmp(TypeTag.A, MacroCodeBuilder.CondKind.NE, "CASE3");
//                                    C.invokestatic(genClassName, "indy_bsm_condy_bsm", "()Ljava/lang/Object;", false).return_();
//
//                                    C.label("CASE3");
//                                    C.return_();
//                                }))
//                .withMethod("bsm", bsmDescriptor, M ->
//                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
//                                .withCode(TypedCodeBuilder::new, C -> {
//                                    C.aload_2();
//                                    C.instanceof_("java/lang/invoke/MethodType");
//                                    C.iconst_0();
//                                    C.ifcmp(TypeTag.I, MacroCodeBuilder.CondKind.EQ, "CONDY");
//                                    C.new_("java/lang/invoke/ConstantCallSite").dup();
//                                    C.ldc("java/lang/String", PoolHelper::putClass);
//                                    C.aload_1();
//                                    C.invokestatic("java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
//                                    C.invokespecial("java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
//                                    C.areturn();
//                                    C.label("CONDY");
//                                    C.aload_1().areturn();
//                                }))
//                .withMethod("bsmIndy", bsmIndyDescriptor, M ->
//                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
//                                .withCode(TypedCodeBuilder::new, C -> {
//                                    C.new_("java/lang/invoke/ConstantCallSite").dup();
//                                    C.ldc("java/lang/String", PoolHelper::putClass);
//                                    C.aload_1();
//                                    C.invokestatic("java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
//                                    C.invokespecial("java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
//                                    C.areturn();
//                                }))
//                .withMethod("condy_bsm_condy_bsm", "()Ljava/lang/Object;", M ->
//                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
//                                .withCode(TypedCodeBuilder::new, C ->
//                                        C.ldc("name", "Ljava/lang/String;", genClassName, "bsm", bsmDescriptor,
//                                              S -> S.add(null, (P, v) -> {
//                                                  return P.putDynamicConstant("name", "Ljava/lang/String;", genClassName, "bsm", bsmDescriptor,
//                                                                              S2 -> S2.add("DUMMY_ARG", PoolHelper::putString));
//                                              }))
//                                                .areturn()))
//                .withMethod("indy_bsmIndy_condy_bsm", "()Ljava/lang/Object;", M ->
//                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
//                                .withCode(TypedCodeBuilder::new, C ->
//                                        C.invokedynamic("name", "()Ljava/lang/String;", genClassName, "bsmIndy", bsmIndyDescriptor,
//                                                        S -> S.add(null, (P, v) -> {
//                                                            return P.putDynamicConstant("name", "Ljava/lang/String;", genClassName, "bsm", bsmDescriptor,
//                                                                                        S2 -> S2.add("DUMMY_ARG", PoolHelper::putString));
//                                                        }))
//                                                .areturn()))
//                .withMethod("indy_bsm_condy_bsm", "()Ljava/lang/Object;", M ->
//                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
//                                .withCode(TypedCodeBuilder::new, C ->
//                                        C.invokedynamic("name", "()Ljava/lang/String;", genClassName, "bsm", bsmDescriptor,
//                                                        S -> S.add(null, (P, v) -> {
//                                                            return P.putDynamicConstant("name", "Ljava/lang/String;", genClassName, "bsm", bsmDescriptor,
//                                                                                        S2 -> S2.add("DUMMY_ARG", PoolHelper::putString));
//                                                        }))
//                                                .areturn()))
//                .build();
//
//        File f = new File(genClassName + ".class");
//        if (f.getParentFile() != null) {
//            f.getParentFile().mkdirs();
//        }
//        new FileOutputStream(f).write(byteArray);
//        return byteArray;
//
//    }

    static void test(Method m, Class<? extends Throwable>... ts) {
        Throwable caught = null;
        try {
            m.invoke(null);
        }
        catch (Throwable t) {
            caught = t;
        }

        if (caught == null) {
            Assert.fail("Throwable expected");
        }

        String actualMessage = null;
        for (int i = 0; i < ts.length; i++) {
            actualMessage = caught.getMessage();
            Assert.assertNotNull(caught);
            Assert.assertTrue(ts[i].isAssignableFrom(caught.getClass()));
            caught = caught.getCause();
        }
    }

    @BeforeClass
    public void findClass() throws Exception {
        c = Class.forName("CondyNestedTest_Code");
    }

    /**
     * Testing an ldc of a dynamic constant, C say, with a BSM whose static
     * argument is C.
     */
    @Test
    public void testCondyBsmCondyBsm() throws Exception {
        test("condy_bsm_condy_bsm", THROWABLES);
    }

    /**
     * Testing an invokedynamic with a BSM whose static argument is a constant
     * dynamic, C say, with a BSM whose static argument is C.
     */
    @Test
    public void testIndyBsmIndyCondyBsm() throws Exception {
        test("indy_bsmIndy_condy_bsm", THROWABLES);
    }

    /**
     * Testing an invokedynamic with a BSM, B say, whose static argument is
     * a dynamic constant, C say, that uses BSM B.
     */
    @Test
    public void testIndyBsmCondyBsm() throws Exception {
        test("indy_bsm_condy_bsm", THROWABLES);
    }

    void test(String methodName, Class<? extends Throwable>... ts) throws Exception {
        Method m = c.getMethod(methodName);
        m.setAccessible(true);
        test(m, ts);
    }

}
