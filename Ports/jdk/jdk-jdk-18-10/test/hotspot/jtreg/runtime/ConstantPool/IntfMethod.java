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
 * @test
 * $bug 8087223
 * @summary Adding constantTag to keep method call consistent with it.
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 *          java.management
 * @compile -XDignore.symbol.file IntfMethod.java
 * @run main/othervm IntfMethod
 * @run main/othervm -Xint IntfMethod
 * @run main/othervm -Xcomp IntfMethod
 */


import jdk.internal.org.objectweb.asm.*;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class IntfMethod {
    static byte[] dumpC() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(52, ACC_PUBLIC | ACC_SUPER, "C", null, "java/lang/Object", new String[]{"I"});
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "testSpecialIntf", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "I", "f1", "()V", /*itf=*/false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "testStaticIntf", "()V", null, null);
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "I", "f2", "()V", /*itf=*/false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "testSpecialClass", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "C", "f1", "()V", /*itf=*/true);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "f2", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "testStaticClass", "()V", null, null);
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "C", "f2", "()V", /*itf=*/true);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    static byte[] dumpI() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(52, ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE, "I", null, "java/lang/Object", null);
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "f1", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "f2", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    static class CL extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] classFile;
            switch (name) {
                case "I": classFile = dumpI(); break;
                case "C": classFile = dumpC(); break;
                default:
                    throw new ClassNotFoundException(name);
            }
            return defineClass(name, classFile, 0, classFile.length);
        }
    }

    public static void main(String[] args) throws Throwable {
        Class<?> cls = (new CL()).loadClass("C");
        try (FileOutputStream fos = new FileOutputStream("I.class")) { fos.write(dumpI()); }
        try (FileOutputStream fos = new FileOutputStream("C.class")) { fos.write(dumpC()); }

        int success = 0;
        for (String name : new String[] { "testSpecialIntf", "testStaticIntf", "testSpecialClass", "testStaticClass"}) {
            System.out.printf("%s: ", name);
            try {
                cls.getMethod(name).invoke(cls.newInstance());
                System.out.println("FAILED - ICCE not thrown");
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException &&
                    e.getCause() != null && e.getCause() instanceof IncompatibleClassChangeError) {
                    System.out.println("PASSED - expected ICCE thrown");
                    success++;
                    continue;
                }
            }
        }
        if (success != 4) throw new Exception("Failed to catch ICCE");
    }
}
