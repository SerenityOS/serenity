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

/*
 * @test
 * @bug 8076110
 * @summary Redefine running methods that have cached resolution errors
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineClassHelper
 * @run main/othervm -javaagent:redefineagent.jar -Xlog:redefine+class+iklass+add=trace,redefine+class+iklass+purge=trace RedefineRunningMethodsWithResolutionErrors
 */

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.lang.reflect.InvocationTargetException;

public class RedefineRunningMethodsWithResolutionErrors extends ClassLoader implements Opcodes {

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.equals("C")) {
            byte[] b = loadC(false);
            return defineClass(name, b, 0, b.length);
        } else {
            return super.findClass(name);
        }
    }

    private static byte[] loadC(boolean redefine) {
        ClassWriter cw = new ClassWriter(0);

        cw.visit(52, ACC_SUPER | ACC_PUBLIC, "C", null, "java/lang/Object", null);
        {
            MethodVisitor mv;

            mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "m", "()V", null, null);
            mv.visitCode();

            // First time we run we will:
            // 1) Cache resolution errors
            // 2) Redefine the class / method
            // 3) Try to read the resolution errors that were cached
            //
            // The redefined method will never run, throw error to be sure
            if (redefine) {
                createThrowRuntimeExceptionCode(mv, "The redefined method was called");
            } else {
                createMethodBody(mv);
            }
            mv.visitMaxs(3, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void createMethodBody(MethodVisitor mv) {
        Label classExists = new Label();

        // Cache resolution errors
        createLoadNonExistentClassCode(mv, classExists);

        // Redefine our own class and method
        mv.visitMethodInsn(INVOKESTATIC, "RedefineRunningMethodsWithResolutionErrors", "redefine", "()V");

        // Provoke the same error again to make sure the resolution error cache works
        createLoadNonExistentClassCode(mv, classExists);

        // Test passed
        mv.visitInsn(RETURN);

        mv.visitFrame(F_SAME, 0, new Object[0], 0, new Object[0]);
        mv.visitLabel(classExists);

        createThrowRuntimeExceptionCode(mv, "Loaded class that shouldn't exist (\"NonExistentClass\")");
    }

    private static void createLoadNonExistentClassCode(MethodVisitor mv, Label classExists) {
        Label tryLoadBegin = new Label();
        Label tryLoadEnd = new Label();
        Label catchLoadBlock = new Label();
        mv.visitTryCatchBlock(tryLoadBegin, tryLoadEnd, catchLoadBlock, "java/lang/NoClassDefFoundError");

        // Try to load a class that does not exist to provoke resolution errors
        mv.visitLabel(tryLoadBegin);
        mv.visitMethodInsn(INVOKESTATIC, "NonExistentClass", "nonExistentMethod", "()V");
        mv.visitLabel(tryLoadEnd);

        // No NoClassDefFoundError means NonExistentClass existed, which shouldn't happen
        mv.visitJumpInsn(GOTO, classExists);

        mv.visitFrame(F_SAME1, 0, new Object[0], 1, new Object[] { "java/lang/NoClassDefFoundError" });
        mv.visitLabel(catchLoadBlock);

        // Ignore the expected NoClassDefFoundError
        mv.visitInsn(POP);
    }

    private static void createThrowRuntimeExceptionCode(MethodVisitor mv, String msg) {
        mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(msg);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(ATHROW);
    }

    private static Class<?> c;

    public static void redefine() throws Exception {
        RedefineClassHelper.redefineClass(c, loadC(true));
    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        c = Class.forName("C", true, new RedefineRunningMethodsWithResolutionErrors());
        c.getMethod("m").invoke(null);
    }
}
