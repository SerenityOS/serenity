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
 * @bug 8174954
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @compile -XDignore.symbol.file BSMCalledTwice.java
 * @run main BSMCalledTwice
 */

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

// BSMCalledTwice generates a class file named "TestC.class" that contains
// bytecodes that represent the following program
//
// public class TestC {
//     public static void main(java.lang.String[] arg) {
//         for (int i=0; i < 2; i++) {
//             try {
//                 String f = "friend";
//
//                 // The "hello " + f in the following statement produces an
//                 // invokedynamic with a BSM of
//                 // StringConcatFactory.java/makeConcatWithConstants.
//                 // The ASM below erroneously puts 2 static arguments, "hello "
//                 // and "goodbye" on the stack for the BSM. Causing a exception to
//                 // be thrown when creatingthe CallSite object.
//                 System.out.println("hello " + f); <--------------- invokedynamic
//
//             } catch (Error e) {
//                System.out.println("Caught Error:");
//                System.out.println(e.getMessage());
//                e.printStackTrace();
//             }
//         }
//     }
// }
//
public class BSMCalledTwice implements Opcodes {
    static final String classTestCName = "TestC";

    public static int count_makeSite(String text) {
        int count = 0;
        String text_ptr = text;
        while (text_ptr.indexOf("makeSite") != -1) {
            text_ptr = text_ptr.substring(text_ptr.indexOf("makeSite") + 1);
            count++;
        }
        return count;
    }

    public static void main(String[] args) throws Exception {
        ClassLoader cl = new ClassLoader() {
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (findLoadedClass(name) != null) {
                    return findLoadedClass(name);
                }

                if (classTestCName.equals(name)) {
                    byte[] classFile = null;
                    try {
                        classFile = dumpTestC();
                    } catch (Exception e) {
                    }
                    return defineClass(classTestCName, classFile, 0, classFile.length);
                }
                return super.loadClass(name);
             }
        };

        cl.loadClass(classTestCName);
        ProcessBuilder pb = ProcessTools.createTestJvm("-cp", ".",  classTestCName);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        String test_output = output.getOutput();
        if (test_output == null) {
            throw new RuntimeException("Test failed, null test output");
        }
        // "makeSite" is currently listed twice in the exception stacks for each
        // failing call to the BootstrapMethod.  So more that two calls means
        // that the BootstrapMethod was called more than once.
        int count = count_makeSite(test_output);
        if (count < 1 || count > 2) {
            throw new RuntimeException("Test failed, bad number of calls to BootstrapMethod");
        }
        output.shouldHaveExitValue(0);
    }

    public static byte[] dumpTestC () throws Exception {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(53, ACC_PUBLIC + ACC_SUPER, classTestCName, null, "java/lang/Object", null);

        cw.visitInnerClass("java/lang/invoke/MethodHandles$Lookup",
                           "java/lang/invoke/MethodHandles", "Lookup",
                           ACC_PUBLIC + ACC_FINAL + ACC_STATIC);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Error");
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 1);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ICONST_2);
            Label l4 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l4);
            mv.visitLabel(l0);
            mv.visitLdcInsn("friend");
            mv.visitVarInsn(ASTORE, 2);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInvokeDynamicInsn("makeConcatWithConstants",
                                      "(Ljava/lang/String;)Ljava/lang/String;",
                                      new Handle(Opcodes.H_INVOKESTATIC,
                                                 "java/lang/invoke/StringConcatFactory",
                                                 "makeConcatWithConstants",
                                                 "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"),
                                      new Object[]{"hello \u0001", "goodbye"});
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            mv.visitLabel(l1);
            Label l5 = new Label();
            mv.visitJumpInsn(GOTO, l5);
            mv.visitLabel(l2);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Error"});
            mv.visitVarInsn(ASTORE, 2);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("Caught Error:");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Error", "getMessage", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Error", "printStackTrace", "()V", false);
            mv.visitLabel(l5);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitIincInsn(1, 1);
            mv.visitJumpInsn(GOTO, l3);
            mv.visitLabel(l4);
            mv.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
        cw.visitEnd();

        try(FileOutputStream fos = new FileOutputStream(new File("TestC.class"))) {
            fos.write(cw.toByteArray());
        }
        return cw.toByteArray();
    }
}
