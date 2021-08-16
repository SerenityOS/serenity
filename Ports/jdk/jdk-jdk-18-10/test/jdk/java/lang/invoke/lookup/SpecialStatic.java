/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8032400
 * @summary JSR292: invokeSpecial: InternalError attempting to lookup a method
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @compile -XDignore.symbol.file SpecialStatic.java
 * @run testng test.java.lang.invoke.lookup.SpecialStatic
 */
package test.java.lang.invoke.lookup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import jdk.internal.org.objectweb.asm.*;
import org.testng.annotations.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static org.testng.Assert.*;

/**
 * Test case:
 *   class T1            {        int m() { return 1; }}
 *   class T2 extends T1 { static int m() { return 2; }}
 *   class T3 extends T2 {        int m() { return 3; }}
 *
 *   T3::test { invokespecial T1.m() T3 } ==> T1::m
 */
public class SpecialStatic {
    static class CustomClassLoader extends ClassLoader {
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (findLoadedClass(name) != null) {
                return findLoadedClass(name);
            }

            if ("T1".equals(name)) {
                byte[] classFile = dumpT1();
                return defineClass("T1", classFile, 0, classFile.length);
            }
            if ("T2".equals(name)) {
                byte[] classFile = dumpT2();
                return defineClass("T2", classFile, 0, classFile.length);
            }
            if ("T3".equals(name)) {
                byte[] classFile = dumpT3();
                return defineClass("T3", classFile, 0, classFile.length);
            }

            return super.loadClass(name);
        }
    }

    private static ClassLoader cl = new CustomClassLoader();
    private static Class t1, t3;
    static {
        try {
            t1 = cl.loadClass("T1");
            t3 = cl.loadClass("T3");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    public static void main(String[] args) throws Throwable {
        SpecialStatic test = new SpecialStatic();
        test.testConstant();
        test.testFindSpecial();
    }

    @Test
    public void testConstant() throws Throwable {
        MethodHandle mh = (MethodHandle)t3.getDeclaredMethod("getMethodHandle").invoke(null);
        int result = (int)mh.invoke(t3.newInstance());
        assertEquals(result, 1); // T1.m should be invoked.
    }

    @Test
    public void testFindSpecial() throws Throwable {
        MethodHandles.Lookup lookup = (MethodHandles.Lookup)t3.getDeclaredMethod("getLookup").invoke(null);
        MethodHandle mh = lookup.findSpecial(t1, "m", MethodType.methodType(int.class), t3);
        int result = (int)mh.invoke(t3.newInstance());
        assertEquals(result, 1); // T1.m should be invoked.
    }

    public static byte[] dumpT1() {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "T1", null, "java/lang/Object", null);

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "m", "()I", null, null);
        mv.visitCode();
        mv.visitIntInsn(BIPUSH, 1);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    public static byte[] dumpT2() {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "T2", null, "T1", null);

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "T1", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "m", "()I", null, null);
        mv.visitCode();
        mv.visitIntInsn(BIPUSH, 2);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    public static byte[] dumpT3() {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "T3", null, "T2", null);

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "T2", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "m", "()I", null, null);
        mv.visitCode();
        mv.visitIntInsn(BIPUSH, 3);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // getMethodHandle
        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "getMethodHandle", "()Ljava/lang/invoke/MethodHandle;", null, null);
        mv.visitCode();
        mv.visitLdcInsn(new Handle(H_INVOKESPECIAL, "T1", "m", "()I"));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();

        // getLookup
        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "getLookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", null, null);
        mv.visitCode();
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
