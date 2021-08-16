/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.calls.common;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * A class which patch InvokeDynamic class bytecode with invokydynamic
 instruction, rewriting "caller" method to call "callee" method using
 invokedynamic
 */
public class InvokeDynamicPatcher extends ClassVisitor {

    private static final String CLASS = InvokeDynamic.class.getName()
            .replace('.', '/');
    private static final String CALLER_METHOD_NAME = "caller";
    private static final String CALLEE_METHOD_NAME = "callee";
    private static final String NATIVE_CALLEE_METHOD_NAME = "calleeNative";
    private static final String BOOTSTRAP_METHOD_NAME = "bootstrapMethod";
    private static final String CALL_NATIVE_FIELD = "nativeCallee";
    private static final String CALL_NATIVE_FIELD_DESC = "Z";
    private static final String CALLEE_METHOD_DESC
            = "(L" + CLASS + ";IJFDLjava/lang/String;)Z";
    private static final String ASSERTTRUE_METHOD_DESC
            = "(ZLjava/lang/String;)V";
    private static final String ASSERTS_CLASS = "jdk/test/lib/Asserts";
    private static final String ASSERTTRUE_METHOD_NAME = "assertTrue";

    public static void main(String args[]) {
        ClassReader cr;
        Path filePath;
        try {
            filePath = Paths.get(InvokeDynamic.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).resolve(CLASS + ".class");
        } catch (URISyntaxException ex) {
            throw new Error("TESTBUG: Can't get code source" + ex, ex);
        }
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            cr = new ClassReader(fis);
        } catch (IOException e) {
            throw new Error("Error reading file", e);
        }
        ClassWriter cw = new ClassWriter(cr,
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cr.accept(new InvokeDynamicPatcher(Opcodes.ASM5, cw), 0);
        try {
            Files.write(filePath, cw.toByteArray(),
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public InvokeDynamicPatcher(int api, ClassWriter cw) {
        super(api, cw);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name,
            final String desc, final String signature,
            final String[] exceptions) {
        /* a code generate looks like
         *  0: aload_0
         *  1: ldc           #125  // int 1
         *  3: ldc2_w        #126  // long 2l
         *  6: ldc           #128  // float 3.0f
         *  8: ldc2_w        #129  // double 4.0d
         * 11: ldc           #132  // String 5
         * 13: aload_0
         * 14: getfield      #135  // Field nativeCallee:Z
         * 17: ifeq          28
         * 20: invokedynamic #181,  0            // InvokeDynamic #1:calleeNative:(Lcompiler/calls/common/InvokeDynamic;IJFDLjava/lang/String;)Z
         * 25: goto          33
         * 28: invokedynamic #183,  0            // InvokeDynamic #1:callee:(Lcompiler/calls/common/InvokeDynamic;IJFDLjava/lang/String;)Z
         * 33: ldc           #185                // String Call insuccessfull
         * 35: invokestatic  #191                // Method jdk/test/lib/Asserts.assertTrue:(ZLjava/lang/String;)V
         * 38: return
         *
         * or, using java-like pseudo-code
         * if (this.nativeCallee == false) {
         *     invokedynamic-call-return-value = invokedynamic-of-callee
         * } else {
         *     invokedynamic-call-return-value = invokedynamic-of-nativeCallee
         * }
         * Asserts.assertTrue(invokedynamic-call-return-value, error-message);
         * return;
         */
        if (name.equals(CALLER_METHOD_NAME)) {
            MethodVisitor mv = cv.visitMethod(access, name, desc,
                    signature, exceptions);
            Label nonNativeLabel = new Label();
            Label checkLabel = new Label();
            MethodType mtype = MethodType.methodType(CallSite.class,
                    MethodHandles.Lookup.class, String.class, MethodType.class);
            Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, CLASS,
                    BOOTSTRAP_METHOD_NAME, mtype.toMethodDescriptorString());
            mv.visitCode();
            // push callee parameters onto stack
            mv.visitVarInsn(Opcodes.ALOAD, 0);//push "this"
            mv.visitLdcInsn(1);
            mv.visitLdcInsn(2L);
            mv.visitLdcInsn(3.0f);
            mv.visitLdcInsn(4.0d);
            mv.visitLdcInsn("5");
            // params loaded. let's decide what method to call
            mv.visitVarInsn(Opcodes.ALOAD, 0); // push "this"
            // get nativeCallee field
            mv.visitFieldInsn(Opcodes.GETFIELD, CLASS, CALL_NATIVE_FIELD,
                    CALL_NATIVE_FIELD_DESC);
            // if nativeCallee == false goto nonNativeLabel
            mv.visitJumpInsn(Opcodes.IFEQ, nonNativeLabel);
            // invokedynamic nativeCalleeMethod using bootstrap method
            mv.visitInvokeDynamicInsn(NATIVE_CALLEE_METHOD_NAME,
                    CALLEE_METHOD_DESC, bootstrap);
            // goto checkLabel
            mv.visitJumpInsn(Opcodes.GOTO, checkLabel);
            // label: nonNativeLabel
            mv.visitLabel(nonNativeLabel);
            // invokedynamic calleeMethod using bootstrap method
            mv.visitInvokeDynamicInsn(CALLEE_METHOD_NAME, CALLEE_METHOD_DESC,
                    bootstrap);
            mv.visitLabel(checkLabel);
            mv.visitLdcInsn(CallsBase.CALL_ERR_MSG);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, ASSERTS_CLASS,
                    ASSERTTRUE_METHOD_NAME, ASSERTTRUE_METHOD_DESC, false);
            // label: return
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return null;
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}
