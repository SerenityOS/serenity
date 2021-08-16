/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8163808
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main TransitiveOverrideCFV50
 */

import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/*
 * Test mixed classfile version overriding handling.
 * Key is to generate P2/C with an older classfile version <=50
 * Correct response is B.m:2 for older classfiles
 * This test was added to ensure no assertions in debug
 * note: for P2/C classfile version >=51, correct answer becomes C.m:3
 * public class  P1.A {             public int m() { return 1; }
 *
 * public class  P1.B extends A {          int m() { return 2; }
 *
 * public class  P2.c extends P1.B { public int m() { return 3; }
 */

public class TransitiveOverrideCFV50 implements Opcodes{
  static final String classP1A = "P1.A";
    static final String classP1B = "P1.B";
    static final String classP2C = "P2.C";

    static final String callerName = classP2C;

    public static void main(String[] args) throws Exception {
        ClassLoader cl = new ClassLoader() {
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (findLoadedClass(name) != null) {
                    return findLoadedClass(name);
                }

                if (classP1A.equals(name)) {
                    byte[] classFile = dumpP1A();
                    return defineClass(classP1A, classFile, 0, classFile.length);
                }
                if (classP1B.equals(name)) {
                    byte[] classFile = dumpP1B();
                    return defineClass(classP1B, classFile, 0, classFile.length);
                }
                if (classP2C.equals(name)) {
                    byte[] classFile = dumpP2C();
                    return defineClass(classP2C, classFile, 0, classFile.length);
                }

                return super.loadClass(name);
            }
        };

        cl.loadClass(classP1A);
        cl.loadClass(classP1B);
        cl.loadClass(classP2C);

        //cl.loadClass(callerName).getDeclaredMethod("test");
        cl.loadClass(callerName).newInstance();

        int result = (Integer)cl.loadClass(callerName).getDeclaredMethod("test").invoke(null);
        if (result != 2) {
          throw new RuntimeException("expected B.m:2, got " + result);
        }
    }

    public static byte[] dumpP1A() {

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, "P1/A", null, "java/lang/Object", null);

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
            mv = cw.visitMethod(ACC_PUBLIC, "m", "()I", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("A.m");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            mv.visitIntInsn(BIPUSH, 1);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
    public static byte[] dumpP1B() {

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, "P1/B", null, "P1/A", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "P1/A", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(0, "m", "()I", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("B.m");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            mv.visitIntInsn(BIPUSH, 2);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
    public static byte[] dumpP2C() {

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "P2/C", null, "P1/B", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "P1/B", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "m", "()I", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("P2/C.m");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            mv.visitIntInsn(BIPUSH, 3);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "test", "()I", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, "P2/C");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "P2/C", "<init>", "()V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "P1/A", "m", "()I", false);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }
}
