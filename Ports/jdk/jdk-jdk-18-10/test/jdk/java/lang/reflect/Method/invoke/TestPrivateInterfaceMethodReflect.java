/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8026213
 * @summary Reflection support for private methods in interfaces
 * @author  Robert Field
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main TestPrivateInterfaceMethodReflect
 */

import java.lang.reflect.*;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

public class TestPrivateInterfaceMethodReflect {

    static final String INTERFACE_NAME = "PrivateInterfaceMethodReflectTest_Interface";
    static final String CLASS_NAME = "PrivateInterfaceMethodReflectTest_Class";
    static final int EXPECTED = 1234;

    static class TestClassLoader extends ClassLoader implements Opcodes {

        @Override
        public Class findClass(String name) throws ClassNotFoundException {
            byte[] b;
            try {
                b = loadClassData(name);
            } catch (Throwable th) {
                // th.printStackTrace();
                throw new ClassNotFoundException("Loading error", th);
            }
            return defineClass(name, b, 0, b.length);
        }

        private byte[] loadClassData(String name) throws Exception {
            ClassWriter cw = new ClassWriter(0);
            MethodVisitor mv;
            switch (name) {
                case INTERFACE_NAME:
                    cw.visit(V1_8, ACC_ABSTRACT | ACC_INTERFACE | ACC_PUBLIC, INTERFACE_NAME, null, "java/lang/Object", null);
                    {
                        mv = cw.visitMethod(ACC_PRIVATE, "privInstance", "()I", null, null);
                        mv.visitCode();
                        mv.visitLdcInsn(EXPECTED);
                        mv.visitInsn(IRETURN);
                        mv.visitMaxs(1, 1);
                        mv.visitEnd();
                    }
                    break;
                case CLASS_NAME:
                    cw.visit(52, ACC_SUPER | ACC_PUBLIC, CLASS_NAME, null, "java/lang/Object", new String[]{INTERFACE_NAME});
                    {
                        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                        mv.visitCode();
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
                        mv.visitInsn(RETURN);
                        mv.visitMaxs(1, 1);
                        mv.visitEnd();
                    }
                    break;
                default:
                    break;
            }
            cw.visitEnd();

            return cw.toByteArray();
        }
    }

    public static void main(String[] args) throws Exception {
        TestClassLoader tcl = new TestClassLoader();
        Class<?> itf = tcl.loadClass(INTERFACE_NAME);
        Class<?> k = tcl.loadClass(CLASS_NAME);
        Object inst = k.newInstance();
        Method[] meths = itf.getDeclaredMethods();
        if (meths.length != 1) {
            throw new Exception("Expected one method in " + INTERFACE_NAME + " instead " + meths.length);
        }

        Method m = meths[0];
        int mod = m.getModifiers();
        if ((mod & Modifier.PRIVATE) == 0) {
            throw new Exception("Expected " + m + " to be private");
        }
        if ((mod & Modifier.STATIC) != 0) {
            throw new Exception("Expected " + m + " to be instance method");
        }

        m.setAccessible(true);
        for (int i = 1; i < 200; i++) {
            if (!m.invoke(inst).equals(EXPECTED)) {
                throw new Exception("Expected " + EXPECTED + " from " + m);
            }
        }

        System.out.println("Passed.");
    }
}
