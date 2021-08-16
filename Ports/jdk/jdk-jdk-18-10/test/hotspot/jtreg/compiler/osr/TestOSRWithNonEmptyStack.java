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

/**
 * @test
 * @bug 8051344
 * @summary Force OSR compilation with non-empty stack at the OSR entry point.
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main/othervm -XX:CompileCommand=compileonly,TestCase::test
 *                   compiler.osr.TestOSRWithNonEmptyStack
 */

package compiler.osr;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.DUP;
import static jdk.internal.org.objectweb.asm.Opcodes.IADD;
import static jdk.internal.org.objectweb.asm.Opcodes.ICONST_0;
import static jdk.internal.org.objectweb.asm.Opcodes.ICONST_1;
import static jdk.internal.org.objectweb.asm.Opcodes.IF_ICMPLT;
import static jdk.internal.org.objectweb.asm.Opcodes.ILOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.ISTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.POP;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;

public class TestOSRWithNonEmptyStack extends ClassLoader {
    private static final int CLASS_FILE_VERSION = 52;
    private static final String CLASS_NAME = "TestCase";
    private static final String METHOD_NAME = "test";
    private static final int ITERATIONS = 1_000_000;

    private static byte[] generateTestClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(TestOSRWithNonEmptyStack.CLASS_FILE_VERSION, ACC_PUBLIC,
                TestOSRWithNonEmptyStack.CLASS_NAME, null, "java/lang/Object",
                null);

        TestOSRWithNonEmptyStack.generateConstructor(cw);
        TestOSRWithNonEmptyStack.generateTestMethod(cw);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void generateConstructor(ClassWriter classWriter) {
        MethodVisitor mv = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V",
                null, null);

        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V",
                false);
        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void generateTestMethod(ClassWriter classWriter) {
        MethodVisitor mv = classWriter.visitMethod(ACC_PUBLIC,
                TestOSRWithNonEmptyStack.METHOD_NAME, "()V", null, null);
        Label osrEntryPoint = new Label();

        mv.visitCode();
        // Push 'this' into stack before OSR entry point to bail out compilation
        mv.visitVarInsn(ALOAD, 0);
        // Setup loop counter
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 1);
        // Begin loop
        mv.visitLabel(osrEntryPoint);
        // Increment loop counter
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        // Duplicate it for loop condition check
        mv.visitInsn(DUP);
        mv.visitVarInsn(ISTORE, 1);
        // Check loop condition
        mv.visitLdcInsn(TestOSRWithNonEmptyStack.ITERATIONS);
        mv.visitJumpInsn(IF_ICMPLT, osrEntryPoint);
        // Pop 'this'.
        mv.visitInsn(POP);
        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void run() {
        byte[] bytecode = TestOSRWithNonEmptyStack.generateTestClass();

        try {
            Class klass = defineClass(TestOSRWithNonEmptyStack.CLASS_NAME,
                    bytecode, 0, bytecode.length);

            Constructor ctor = klass.getConstructor();
            Method method = klass.getDeclaredMethod(
                    TestOSRWithNonEmptyStack.METHOD_NAME);

            Object testCase = ctor.newInstance();
            method.invoke(testCase);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Test bug: generated class should be valid.", e);
        }
    }

    public static void main(String args[]) {
        new TestOSRWithNonEmptyStack().run();
    }
}
