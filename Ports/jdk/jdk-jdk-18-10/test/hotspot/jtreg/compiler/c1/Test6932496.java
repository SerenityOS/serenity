/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6932496
 * @summary incorrect deopt of jsr subroutine on 64 bit c1
 * @modules java.base/jdk.internal.org.objectweb.asm
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.c1.Test6932496::test
 *      compiler.c1.Test6932496
 */

package compiler.c1;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Test6932496 extends ClassLoader {
    private static final int CLASS_FILE_VERSION = 49;
    private static final String CLASS_TEST = "Test";
    private static final String CLASS_OBJECT = "java/lang/Object";
    private static final String METHOD_INIT = "<init>";
    private static final String METHOD_TEST = "test";
    private static final String DESC_VOID_METHOD = "()V";
    private static final String FIELD_FLAG = "flag";

    public static void main(String[] args) {
        Test6932496 test = new Test6932496();
        test.execute();
    }

    private void execute() {
        byte[] bytecode = Test6932496.generateTestClass();

        try {
            Files.write(Paths.get("Test.class.dump"), bytecode);
        } catch (IOException e) {
            System.err.println("classfile dump failed : " + e.getMessage());
            e.printStackTrace();
        }
        try {
            Class aClass = defineClass(CLASS_TEST, bytecode, 0, bytecode.length);
            Method test = aClass.getDeclaredMethod(METHOD_TEST);
            test.invoke(null);
        } catch (ClassFormatError | IllegalArgumentException
                    | ReflectiveOperationException e) {
            throw new RuntimeException("TESTBUG : generated class is invalid", e);
        }
    }

    /*
        public class Test {
            volatile boolean flag = false;
            public static void m() {
                try {
                } finally {
                    Test test = new Test();
                    test.flag = true;
                }
            }
        }
    */
    private static byte[] generateTestClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(CLASS_FILE_VERSION, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                CLASS_TEST, null, CLASS_OBJECT, null);
        // volatile boolean flag;
        {
            FieldVisitor fv = cw.visitField(Opcodes.ACC_VOLATILE, FIELD_FLAG,
                    Type.BOOLEAN_TYPE.getDescriptor(),
                    /* signature = */ null, /* value = */ null);
        }

        /*
            public Test() {
                flag = false;
            }
        */
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                    METHOD_INIT, DESC_VOID_METHOD,
                    /* signature = */ null, /* exceptions = */ null);

            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CLASS_OBJECT, METHOD_INIT,
                    DESC_VOID_METHOD, false);

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitFieldInsn(Opcodes.PUTFIELD, CLASS_TEST, FIELD_FLAG,
                    Type.BOOLEAN_TYPE.getDescriptor());

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(/* stack = */ 2, /* locals = */ 1);
            mv.visitEnd();
        }

        /*
            public static void m() {
                try {
                } finally {
                    Test test = new Test();
                    test.flag = true;
                }
            }
        */
        {
            MethodVisitor mv = cw.visitMethod(
                    Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC,
                    METHOD_TEST, DESC_VOID_METHOD,
                    /* signature = */ null, /* exceptions = */ null);
            Label beginLabel = new Label();
            Label block1EndLabel = new Label();
            Label handlerLabel = new Label();
            Label block2EndLabel = new Label();
            Label label = new Label();
            Label endLabel = new Label();

            mv.visitCode();
            mv.visitTryCatchBlock(beginLabel, block1EndLabel, handlerLabel,
                    /* type = <any> */ null);
            mv.visitTryCatchBlock(handlerLabel, block2EndLabel, handlerLabel,
                    /* type = <any> */ null);

            mv.visitLabel(beginLabel);
            mv.visitJumpInsn(Opcodes.JSR, label);
            mv.visitLabel(block1EndLabel);
            mv.visitJumpInsn(Opcodes.GOTO, endLabel);

            mv.visitLabel(handlerLabel);
            mv.visitVarInsn(Opcodes.ASTORE, 0);
            mv.visitJumpInsn(Opcodes.JSR, label);
            mv.visitLabel(block2EndLabel);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ATHROW);

            mv.visitLabel(label);
            mv.visitVarInsn(Opcodes.ASTORE, 1);
            mv.visitTypeInsn(Opcodes.NEW, CLASS_TEST);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CLASS_TEST, METHOD_INIT,
                    DESC_VOID_METHOD);
            mv.visitVarInsn(Opcodes.ASTORE, 2);

            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, CLASS_TEST, FIELD_FLAG,
                    Type.BOOLEAN_TYPE.getDescriptor());

            mv.visitVarInsn(Opcodes.RET, 1);

            mv.visitLabel(endLabel);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(/* stack = */ 2, /* locals = */ 3);
            mv.visitEnd();
        }

        cw.visitEnd();
        return cw.toByteArray();
    }
}
