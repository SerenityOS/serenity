/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8087223 8195650
 * @summary Adding constantTag to keep method call consistent with it.
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 *          java.management
 * @compile -XDignore.symbol.file BadMethodHandles.java
 * @run main/othervm BadMethodHandles
 */

import jdk.internal.org.objectweb.asm.*;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class BadMethodHandles {

    static byte[] dumpBadInterfaceMethodref() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(52, ACC_PUBLIC | ACC_SUPER, "BadInterfaceMethodref", null, "java/lang/Object", null);
        Handle handle1 =
            new Handle(Opcodes.H_INVOKEINTERFACE, "BadInterfaceMethodref", "m", "()V", true);
        Handle handle2 =
            new Handle(Opcodes.H_INVOKEINTERFACE, "BadInterfaceMethodref", "staticM", "()V", true);

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
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "m", "()V", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("hello from m");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false/*intf*/);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 1);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "staticM", "()V", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("hello from staticM");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false/*intf*/);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "runm", "()V", null, null);
            mv.visitCode();
            // REF_invokeStatic
            mv.visitLdcInsn(handle1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "runStaticM", "()V", null, null);
            mv.visitCode();
            // REF_invokeStatic
            mv.visitLdcInsn(handle2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    static byte[] dumpIBad() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(52, ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE, "IBad", null, "java/lang/Object", null);
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "m", "()V", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("hello from m");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false/*intf*/);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 1);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "staticM", "()V", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("hello from staticM");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false/*intf*/);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    static byte[] dumpBadMethodref() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(52, ACC_PUBLIC | ACC_SUPER,  "BadMethodref", null, "java/lang/Object", new String[]{"IBad"});
        Handle handle1 =
            new Handle(Opcodes.H_INVOKEINTERFACE, "BadMethodref", "m", "()V", true);
        Handle handle2 =
            new Handle(Opcodes.H_INVOKEINTERFACE, "BadMethodref", "staticM", "()V", true);

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
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "runm", "()V", null, null);
            mv.visitCode();
            // REF_invokeStatic
            mv.visitLdcInsn(handle1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "runStaticM", "()V", null, null);
            mv.visitCode();
            // REF_invokeStatic
            mv.visitLdcInsn(handle2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    static byte[] dumpInvokeBasic() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(52, ACC_PUBLIC | ACC_SUPER,  "InvokeBasicref", null, "java/lang/Object", null);
        Handle handle =
                new Handle(Opcodes.H_INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeBasic", "([Ljava/lang/Object;)Ljava/lang/Object;", false);

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
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "runInvokeBasicM", "()V", null, null);
            mv.visitCode();
            mv.visitLdcInsn(handle);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    static class CL extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] classBytes = null;
            switch (name) {
            case "BadInterfaceMethodref": classBytes = dumpBadInterfaceMethodref(); break;
            case "BadMethodref"         : classBytes = dumpBadMethodref(); break;
            case "IBad"                 : classBytes = dumpIBad(); break;
            case "InvokeBasicref"       : classBytes = dumpInvokeBasic(); break;
            default                     : throw new ClassNotFoundException(name);
            }
            return defineClass(name, classBytes, 0, classBytes.length);
        }
    }

    public static void main(String[] args) throws Throwable {
        try (FileOutputStream fos = new FileOutputStream("BadInterfaceMethodref.class")) {
          fos.write(dumpBadInterfaceMethodref());
        }
        try (FileOutputStream fos = new FileOutputStream("IBad.class")) {
          fos.write(dumpIBad());
        }
        try (FileOutputStream fos = new FileOutputStream("BadMethodref.class")) {
          fos.write(dumpBadMethodref());
        }
        try (FileOutputStream fos = new FileOutputStream("InvokeBasicref.class")) {
            fos.write(dumpInvokeBasic());
        }

        Class<?> cls = (new CL()).loadClass("BadInterfaceMethodref");
        String[] methods = {"runm", "runStaticM"};
        System.out.println("Test BadInterfaceMethodref:");
        int success = 0;
        for (String name : methods) {
            try {
                System.out.printf("invoke %s: \n", name);
                cls.getMethod(name).invoke(cls.newInstance());
                System.out.println("FAILED - ICCE should be thrown");
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException && e.getCause() != null &&
                    e.getCause() instanceof IncompatibleClassChangeError) {
                    System.out.println("PASSED - expected ICCE thrown");
                    success++;
                    continue;
                } else {
                    System.out.println("FAILED with wrong exception" + e);
                    throw e;
                }
            }
        }
        if (success != methods.length) {
           throw new Exception("BadInterfaceMethodRef Failed to catch IncompatibleClassChangeError");
        }
        System.out.println("Test BadMethodref:");
        cls = (new CL()).loadClass("BadMethodref");
        success = 0;
        for (String name : methods) {
            try {
                System.out.printf("invoke %s: \n", name);
                cls.getMethod(name).invoke(cls.newInstance());
                System.out.println("FAILED - ICCE should be thrown");
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException && e.getCause() != null &&
                    e.getCause() instanceof IncompatibleClassChangeError) {
                    System.out.println("PASSED - expected ICCE thrown");
                    success++;
                    continue;
                } else {
                    System.out.println("FAILED with wrong exception" + e);
                    throw e;
                }
            }
         }
         if (success != methods.length) {
            throw new Exception("BadMethodRef Failed to catch IncompatibleClassChangeError");
         }

        System.out.println("Test InvokeBasicref:");
        cls = (new CL()).loadClass("InvokeBasicref");
        success = 0;
        methods = new String[] {"runInvokeBasicM"};
        for (String name : methods) {
            try {
                System.out.printf("invoke %s: \n", name);
                cls.getMethod(name).invoke(cls.newInstance());
                System.out.println("FAILED - ICCE should be thrown");
            } catch (Throwable e) {
                e.printStackTrace();
                if (e instanceof InvocationTargetException && e.getCause() != null &&
                    e.getCause() instanceof IncompatibleClassChangeError) {
                    System.out.println("PASSED - expected ICCE thrown");
                    success++;
                    continue;
                } else {
                    System.out.println("FAILED with wrong exception" + e);
                    throw e;
                }
            }
        }
        if (success != methods.length) {
            throw new Exception("InvokeBasicref Failed to catch IncompatibleClassChangeError");
        }
    }
}
