/*
 * Copyright (c) 2018 SAP SE. All rights reserved.
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
 * @bug 8141551
 * @summary C2 can not handle returns with inccompatible interface arrays
 * @requires vm.compMode == "Xmixed" & vm.flavor == "server"
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *        -Xbootclasspath/a:.
 *        -XX:+UnlockDiagnosticVMOptions
 *        -XX:+WhiteBoxAPI
 *        -Xbatch
 *        -XX:-TieredCompilation
 *        -XX:TieredStopAtLevel=4
 *        -XX:CICompilerCount=1
 *        -XX:+PrintCompilation
 *        -XX:+PrintInlining
 *        -XX:CompileCommand=compileonly,MeetIncompatibleInterfaceArrays*::run
 *        -XX:CompileCommand=dontinline,compiler.types.TestMeetIncompatibleInterfaceArrays$Helper::createI2*
 *        -XX:CompileCommand=quiet
 *        compiler.types.TestMeetIncompatibleInterfaceArrays 0
 * @run main/othervm
 *        -Xbootclasspath/a:.
 *        -XX:+UnlockDiagnosticVMOptions
 *        -XX:+WhiteBoxAPI
 *        -Xbatch
 *        -XX:-TieredCompilation
 *        -XX:TieredStopAtLevel=4
 *        -XX:CICompilerCount=1
 *        -XX:+PrintCompilation
 *        -XX:+PrintInlining
 *        -XX:CompileCommand=compileonly,MeetIncompatibleInterfaceArrays*::run
 *        -XX:CompileCommand=inline,compiler.types.TestMeetIncompatibleInterfaceArrays$Helper::createI2*
 *        -XX:CompileCommand=quiet
 *        compiler.types.TestMeetIncompatibleInterfaceArrays 1
 * @run main/othervm
 *        -Xbootclasspath/a:.
 *        -XX:+UnlockDiagnosticVMOptions
 *        -XX:+WhiteBoxAPI
 *        -Xbatch
 *        -XX:+TieredCompilation
 *        -XX:TieredStopAtLevel=4
 *        -XX:CICompilerCount=2
 *        -XX:+PrintCompilation
 *        -XX:+PrintInlining
 *        -XX:CompileCommand=compileonly,MeetIncompatibleInterfaceArrays*::run
 *        -XX:CompileCommand=compileonly,compiler.types.TestMeetIncompatibleInterfaceArrays$Helper::createI2*
 *        -XX:CompileCommand=inline,compiler.types.TestMeetIncompatibleInterfaceArrays$Helper::createI2*
 *        -XX:CompileCommand=quiet
 *        compiler.types.TestMeetIncompatibleInterfaceArrays 2
 *
 * @author volker.simonis@gmail.com
 */

package compiler.types;

import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import sun.hotspot.WhiteBox;

import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static jdk.internal.org.objectweb.asm.Opcodes.AALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.ASTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.GETSTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ICONST_0;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.V1_8;

public class TestMeetIncompatibleInterfaceArrays extends ClassLoader {

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static interface I1 { public String getName(); }
    public static interface I2 { public String getName(); }
    public static class I2C implements I2 { public String getName() { return "I2";} }
    public static class I21C implements I2, I1 { public String getName() { return "I2 and I1";} }

    public static class Helper {
        public static I2 createI2Array0() {
            return new I2C();
        }
        public static I2[] createI2Array1() {
            return new I2C[] { new I2C() };
        }
        public static I2[][] createI2Array2() {
            return new I2C[][] { new I2C[] { new I2C() } };
        }
        public static I2[][][] createI2Array3() {
            return new I2C[][][] { new I2C[][] { new I2C[] { new I2C() } } };
        }
        public static I2[][][][] createI2Array4() {
            return new I2C[][][][] { new I2C[][][] { new I2C[][] { new I2C[] { new I2C() } } } };
        }
        public static I2[][][][][] createI2Array5() {
            return new I2C[][][][][] { new I2C[][][][] { new I2C[][][] { new I2C[][] { new I2C[] { new I2C() } } } } };
        }
        public static I2 createI21Array0() {
            return new I21C();
        }
        public static I2[] createI21Array1() {
            return new I21C[] { new I21C() };
        }
        public static I2[][] createI21Array2() {
            return new I21C[][] { new I21C[] { new I21C() } };
        }
        public static I2[][][] createI21Array3() {
            return new I21C[][][] { new I21C[][] { new I21C[] { new I21C() } } };
        }
        public static I2[][][][] createI21Array4() {
            return new I21C[][][][] { new I21C[][][] { new I21C[][] { new I21C[] { new I21C() } } } };
        }
        public static I2[][][][][] createI21Array5() {
            return new I21C[][][][][] { new I21C[][][][] { new I21C[][][] { new I21C[][] { new I21C[] { new I21C() } } } } };
        }
    }

    // Location for the generated class files
    public static final String PATH = System.getProperty("test.classes", ".") + java.io.File.separator;

    /*
     * With 'good == false' this helper method creates the following classes
     * (using the nested 'Helper' class and the nested interfaces 'I1' and 'I2').
     * For brevity I omit the enclosing class 'TestMeetIncompatibleInterfaceArrays' in the
     * following examples:
     *
     * public class MeetIncompatibleInterfaceArrays0ASM {
     *   public static I1 run() {
     *     return Helper.createI2Array0(); // returns I2
     *   }
     *   public static void test() {
     *     I1 i1 = run();
     *     System.out.println(i1.getName());
     *   }
     * }
     * public class MeetIncompatibleInterfaceArrays1ASM {
     *   public static I1[] run() {
     *     return Helper.createI2Array1(); // returns I2[]
     *   }
     *   public static void test() {
     *     I1[] i1 = run();
     *     System.out.println(i1[0].getName());
     *   }
     * }
     * ...
     * // MeetIncompatibleInterfaceArrays4ASM is special because it creates
     * // an illegal class which will be rejected by the verifier.
     * public class MeetIncompatibleInterfaceArrays4ASM {
     *   public static I1[][][][] run() {
     *     return Helper.createI2Array3(); // returns I1[][][] which gives a verifier error because return expects I1[][][][]
     *   }
     *   public static void test() {
     *     I1[][][][] i1 = run();
     *     System.out.println(i1[0][0][0][0].getName());
     *   }
     * ...
     * public class MeetIncompatibleInterfaceArrays5ASM {
     *   public static I1[][][][][] run() {
     *     return Helper.createI2Array5(); // returns I2[][][][][]
     *   }
     *   public static void test() {
     *     I1[][][][][] i1 = run();
     *     System.out.println(i1[0][0][0][0][0].getName());
     *   }
     * }
     *
     * Notice that this is not legal Java code. We would have to use a cast in "run()" to make it legal:
     *
     *   public static I1[] run() {
     *     return (I1[])Helper.createI2Array1(); // returns I2[]
     *   }
     *
     * But in pure bytecode, the "run()" methods are perfectly legal:
     *
     *   public static I1[] run();
     *     Code:
     *       0: invokestatic  #16  // Method Helper.createI2Array1:()[LI2;
     *       3: areturn
     *
     * The "test()" method calls the "getName()" function from I1 on the objects returned by "run()".
     * This will epectedly fail with an "IncompatibleClassChangeError" because the objects returned
     * by "run()" (and by createI2Array()) are actually of type "I2C" and only implement "I2" but not "I1".
     *
     *
     * With 'good == true' this helper method will create the following classes:
     *
     * public class MeetIncompatibleInterfaceArraysGood0ASM {
     *   public static I1 run() {
     *     return Helper.createI21Array0(); // returns I2
     *   }
     *   public static void test() {
     *     I1 i1 = run();
     *     System.out.println(i1.getName());
     *   }
     * }
     *
     * Calling "test()" on these objects will succeed and output "I2 and I1" because now the "run()"
     * method calls "createI21Array()" which actually return an object (or an array of objects) of
     * type "I21C" which implements both "I2" and "I1".
     *
     * Notice that at the bytecode level, the code for the "run()" and "test()" methods in
     * "MeetIncompatibleInterfaceArraysASM" and "MeetIncompatibleInterfaceArraysGoodASM" look exactly
     * the same. I.e. the verifier has no chance to verify if the I2 object returned by "createI1Array()"
     * or "createI21Array()" implements "I1" or not. That's actually the reason why both versions of
     * generated classes are legal from a verifier point of view.
     *
     */
    static void generateTestClass(int dim, boolean good) throws Exception {
        String baseClassName = "MeetIncompatibleInterfaceArrays";
        if (good)
            baseClassName += "Good";
        String createName = "createI2" + (good ? "1" : "") + "Array";
        String a = "";
        for (int i = 0; i < dim; i++)
            a += "[";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC, baseClassName + dim + "ASM", null, "java/lang/Object", null);
        MethodVisitor constr = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constr.visitCode();
        constr.visitVarInsn(ALOAD, 0);
        constr.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constr.visitInsn(RETURN);
        constr.visitMaxs(0, 0);
        constr.visitEnd();
        MethodVisitor run = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "run",
                "()" + a + "Lcompiler/types/TestMeetIncompatibleInterfaceArrays$I1;", null, null);
        run.visitCode();
        if (dim == 4) {
            run.visitMethodInsn(INVOKESTATIC, "compiler/types/TestMeetIncompatibleInterfaceArrays$Helper", createName + 3,
                    "()" + "[[[" + "Lcompiler/types/TestMeetIncompatibleInterfaceArrays$I2;", false);
        } else {
            run.visitMethodInsn(INVOKESTATIC, "compiler/types/TestMeetIncompatibleInterfaceArrays$Helper", createName + dim,
                    "()" + a + "Lcompiler/types/TestMeetIncompatibleInterfaceArrays$I2;", false);
        }
        run.visitInsn(ARETURN);
        run.visitMaxs(0, 0);
        run.visitEnd();
        MethodVisitor test = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "test", "()V", null, null);
        test.visitCode();
        test.visitMethodInsn(INVOKESTATIC, baseClassName + dim + "ASM", "run",
                "()" + a + "Lcompiler/types/TestMeetIncompatibleInterfaceArrays$I1;", false);
        test.visitVarInsn(ASTORE, 0);
        if (dim > 0) {
            test.visitVarInsn(ALOAD, 0);
            for (int i = 1; i <= dim; i++) {
                test.visitInsn(ICONST_0);
                test.visitInsn(AALOAD);
            }
            test.visitVarInsn(ASTORE, 1);
        }
        test.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        test.visitVarInsn(ALOAD, dim > 0 ? 1 : 0);
        test.visitMethodInsn(INVOKEINTERFACE, "compiler/types/TestMeetIncompatibleInterfaceArrays$I1", "getName",
                "()Ljava/lang/String;", true);
        test.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
        test.visitInsn(RETURN);
        test.visitMaxs(0, 0);
        test.visitEnd();

        // Get the bytes of the class..
        byte[] b = cw.toByteArray();
        // ..and write them into a class file (for debugging)
        FileOutputStream fos = new FileOutputStream(PATH + baseClassName + dim + "ASM.class");
        fos.write(b);
        fos.close();

    }

    public static String[][] tier = { { "interpreted (tier 0)",
                                        "C2 (tier 4) without inlining",
                                        "C2 (tier 4) without inlining" },
                                      { "interpreted (tier 0)",
                                        "C2 (tier 4) with inlining",
                                        "C2 (tier 4) with inlining" },
                                      { "interpreted (tier 0)",
                                        "C1 (tier 3) with inlining",
                                        "C2 (tier 4) with inlining" } };

    public static int[][] level = { { CompilerWhiteBoxTest.COMP_LEVEL_NONE,
                                      CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION,
                                      CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION },
                                    { CompilerWhiteBoxTest.COMP_LEVEL_NONE,
                                      CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION,
                                      CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION },
                                    { CompilerWhiteBoxTest.COMP_LEVEL_NONE,
                                      CompilerWhiteBoxTest.COMP_LEVEL_FULL_PROFILE,
                                      CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION } };

    public static void main(String[] args) throws Exception {
        final int pass = Integer.parseInt(args.length > 0 ? args[0] : "0");

        // Load and initialize some classes required for compilation
        Class.forName("compiler.types.TestMeetIncompatibleInterfaceArrays$I1");
        Class.forName("compiler.types.TestMeetIncompatibleInterfaceArrays$I2");
        Class.forName("compiler.types.TestMeetIncompatibleInterfaceArrays$Helper");

        for (int g = 0; g < 2; g++) {
            String baseClassName = "MeetIncompatibleInterfaceArrays";
            boolean good = (g == 0) ? false : true;
            if (good)
                baseClassName += "Good";
            for (int i = 0; i < 6; i++) {
                System.out.println();
                System.out.println("Creating " + baseClassName + i + "ASM.class");
                System.out.println("========================================" + "=" + "=========");
                // Create the "MeetIncompatibleInterfaceArrays<i>ASM" class
                generateTestClass(i, good);
                Class<?> c = null;
                try {
                    c = Class.forName(baseClassName + i + "ASM");
                } catch (VerifyError ve) {
                    if (i == 4) {
                        System.out.println("OK - must be (" + ve.getMessage() + ").");
                    } else {
                        throw ve;
                    }
                    continue;
                }
                // Call MeetIncompatibleInterfaceArrays<i>ASM.test()
                Method m = c.getMethod("test");
                Method r = c.getMethod("run");
                for (int j = 0; j < 3; j++) {
                    System.out.println((j + 1) + ". invokation of " + baseClassName + i + "ASM.test() [::" +
                                       r.getName() + "() should be '" + tier[pass][j] + "' compiled]");

                    // Skip Profiling compilation (C1) when Tiered is disabled.
                    boolean profile = (level[pass][j] == CompilerWhiteBoxTest.COMP_LEVEL_FULL_PROFILE);
                    if (profile && CompilerWhiteBoxTest.skipOnTieredCompilation(false)) {
                        continue;
                    }

                    WB.enqueueMethodForCompilation(r, level[pass][j]);

                    try {
                        m.invoke(null);
                    } catch (InvocationTargetException ite) {
                        if (good) {
                            throw ite;
                        } else {
                            if (ite.getCause() instanceof IncompatibleClassChangeError) {
                                System.out.println("  OK - catched InvocationTargetException("
                                        + ite.getCause().getMessage() + ").");
                            } else {
                                throw ite;
                            }
                        }
                    }

                    int r_comp_level = WB.getMethodCompilationLevel(r);
                    System.out.println("   invokation of " + baseClassName + i + "ASM.test() [::" +
                                       r.getName() + "() was compiled at tier " + r_comp_level + "]");

                    if (r_comp_level != level[pass][j]) {
                      throw new Exception("Method " + r + " must be compiled at tier " + level[pass][j] +
                                          " but was compiled at " + r_comp_level + " instead!");
                    }

                    WB.deoptimizeMethod(r);
                }
            }
        }
    }
}
