/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8032010
 * @summary method lookup on an abstract method in a concrete class should be successful
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main TestConcreteClassWithAbstractMethod
 */

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

/*
 *   class T1 { public int m() {} }
 *   class T2 { public abstract int m(); }
 *   class T3 { public int m() {} }
 *
 *   Call site: T3.test() { invokevirtual T2.m() }
 *   T3.m() should be invoked
 */
public class TestConcreteClassWithAbstractMethod {
    static final String classT1 = "p1.T1";
    static final String classT2 = "p1.T2";
    static final String classT3 = "p1.T3";

    static final String callerName = classT3;

    public static void main(String[] args) throws Exception {
        ClassLoader cl = new ClassLoader() {
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (findLoadedClass(name) != null) {
                    return findLoadedClass(name);
                }

                if (classT1.equals(name)) {
                    byte[] classFile = dumpT1();
                    return defineClass(classT1, classFile, 0, classFile.length);
                }
                if (classT2.equals(name)) {
                    byte[] classFile = dumpT2();
                    return defineClass(classT2, classFile, 0, classFile.length);
                }
                if (classT3.equals(name)) {
                    byte[] classFile = dumpT3();
                    return defineClass(classT3, classFile, 0, classFile.length);
                }

                return super.loadClass(name);
            }
        };

        cl.loadClass(classT1);
        cl.loadClass(classT2);
        cl.loadClass(classT3);

        //cl.loadClass(callerName).getDeclaredMethod("m");
        cl.loadClass(callerName).newInstance();

        int result = (Integer)cl.loadClass(callerName).getDeclaredMethod("test").invoke(null);
        System.out.println(""+result);
    }

    public static byte[] dumpT1() {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(52, ACC_PUBLIC | ACC_SUPER, "p1/T1", null, "java/lang/Object", null);
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
            mv.visitLdcInsn("p1/T1.m()");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);
            mv.visitIntInsn(BIPUSH, 3);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    public static byte[] dumpT2() {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(52, ACC_PUBLIC | ACC_SUPER, "p1/T2", null, "p1/T1", null);
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "p1/T1", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "m", "()I", null, null);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    public static byte[] dumpT3() {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "p1/T3", null, "p1/T2", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "p1/T2", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "m", "()I", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("p1/T3.m()");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);
            mv.visitIntInsn(BIPUSH, 2);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "test", "()I", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, "p1/T3");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "p1/T3", "<init>", "()V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "p1/T2", "m", "()I", false);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
