/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8194246
 * @summary JVM crashes on stack trace for large number of methods.
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 * @run driver LargeClassTest
 */

import java.io.File;
import java.io.FileOutputStream;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class LargeClassTest implements Opcodes {
    public static void main(String... args) throws Exception {
        writeClassFile();
        ProcessBuilder pb = ProcessTools.createTestJvm("-cp", ".",  "Large");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }

    // Writes a Large class with > signed 16 bit int methods
    public static void writeClassFile() throws Exception {

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(55, ACC_PUBLIC + ACC_SUPER, "Large", null, "java/lang/Object", null);

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
          // public static void main(String[] args) {
          //     Large large = new Large();
          //     large.f_1(55);
          // }
          mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
          mv.visitCode();
          mv.visitTypeInsn(NEW, "Large");
          mv.visitInsn(DUP);
          mv.visitMethodInsn(INVOKESPECIAL, "Large", "<init>", "()V", false);
          mv.visitVarInsn(ASTORE, 1);
          mv.visitVarInsn(ALOAD, 1);
          mv.visitIntInsn(BIPUSH, 55);
          mv.visitMethodInsn(INVOKEVIRTUAL, "Large", "f_1", "(I)I", false);
          mv.visitInsn(POP);
          mv.visitInsn(RETURN);
          mv.visitMaxs(2, 2);
          mv.visitEnd();
        }

        // Write 33560 methods called f_$i
        for (int i = 1000; i < 34560; i++)
        {
          mv = cw.visitMethod(ACC_PUBLIC, "f_" + i, "()V", null, null);
          mv.visitCode();
          mv.visitInsn(RETURN);
          mv.visitMaxs(0, 1);
          mv.visitEnd();
        }
        {
          // public int f_1(int prior) {
          //   int total = prior + new java.util.Random(1).nextInt();
          //   return total + f_2(total);
          // }
          mv = cw.visitMethod(ACC_PUBLIC, "f_1", "(I)I", null, null);
          mv.visitCode();
          mv.visitVarInsn(ILOAD, 1);
          mv.visitTypeInsn(NEW, "java/util/Random");
          mv.visitInsn(DUP);
          mv.visitInsn(LCONST_1);
          mv.visitMethodInsn(INVOKESPECIAL, "java/util/Random", "<init>", "(J)V", false);
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Random", "nextInt", "()I", false);
          mv.visitInsn(IADD);
          mv.visitVarInsn(ISTORE, 2);
          mv.visitVarInsn(ILOAD, 2);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitVarInsn(ILOAD, 2);
          mv.visitMethodInsn(INVOKEVIRTUAL, "Large", "f_2", "(I)I", false);
          mv.visitInsn(IADD);
          mv.visitInsn(IRETURN);
          mv.visitMaxs(5, 3);
          mv.visitEnd();
        }
        {
          // public int f_2(int total) {
          //   System.out.println(java.util.Arrays.toString(Thread.currentThread().getStackTrace()));
          //   return 10;
          // }
          mv = cw.visitMethod(ACC_PUBLIC, "f_2", "(I)I", null, null);
          mv.visitCode();
          mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false);
          mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;", false);
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
          mv.visitIntInsn(BIPUSH, 10);
          mv.visitInsn(IRETURN);
          mv.visitMaxs(2, 2);
          mv.visitEnd();
        }
        cw.visitEnd();

        try (FileOutputStream fos = new FileOutputStream(new File("Large.class"))) {
          fos.write(cw.toByteArray());
        }
    }
}
