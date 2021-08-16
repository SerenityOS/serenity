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
 * @bug 8028553
 * @summary Test that VerifyError is not thrown when 'overriding' a static method.
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main FinalStatic
 */

import java.lang.reflect.*;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/*
 *  class A { static final int m() {return FAIL; } }
 *  class B extends A { int m() { return PASS; } }
 *  class FinalStatic {
 *      public static void main () {
 *          Object b = new B();
 *          b.m();
 *      }
 *  }
 */
public class FinalStatic {

    static final String CLASS_NAME_A = "A";
    static final String CLASS_NAME_B = "B";
    static final int FAILED = 0;
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
               case CLASS_NAME_A:
                    cw.visit(52, ACC_SUPER | ACC_PUBLIC, CLASS_NAME_A, null, "java/lang/Object", null);
                    {
                        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                        mv.visitCode();
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
                        mv.visitInsn(RETURN);
                        mv.visitMaxs(1, 1);
                        mv.visitEnd();

                        mv = cw.visitMethod(ACC_FINAL | ACC_STATIC, "m", "()I", null, null);
                        mv.visitCode();
                        mv.visitLdcInsn(FAILED);
                        mv.visitInsn(IRETURN);
                        mv.visitMaxs(1, 1);
                        mv.visitEnd();
                    }
                    break;
                case CLASS_NAME_B:
                    cw.visit(52, ACC_SUPER | ACC_PUBLIC, CLASS_NAME_B, null, CLASS_NAME_A, null);
                    {
                        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                        mv.visitCode();
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESPECIAL, CLASS_NAME_A, "<init>", "()V");
                        mv.visitInsn(RETURN);
                        mv.visitMaxs(1, 1);
                        mv.visitEnd();

                        mv = cw.visitMethod(ACC_PUBLIC, "m", "()I", null, null);
                        mv.visitCode();
                        mv.visitLdcInsn(EXPECTED);
                        mv.visitInsn(IRETURN);
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
        Class<?> a = tcl.loadClass(CLASS_NAME_A);
        Class<?> b = tcl.loadClass(CLASS_NAME_B);
        Object inst = b.newInstance();
        Method[] meths = b.getDeclaredMethods();

        Method m = meths[0];
        int mod = m.getModifiers();
        if ((mod & Modifier.FINAL) != 0) {
            throw new Exception("FAILED: " + m + " is FINAL");
        }
        if ((mod & Modifier.STATIC) != 0) {
            throw new Exception("FAILED: " + m + " is STATIC");
        }

        m.setAccessible(true);
        if (!m.invoke(inst).equals(EXPECTED)) {
              throw new Exception("FAILED: " + EXPECTED + " from " + m);
        }

        System.out.println("Passed.");
    }
}
