/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package shared;

import static jdk.internal.org.objectweb.asm.ClassWriter.*;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.ClassWriter;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class ExecutorGenerator {
    public static final String className = Utils.getInternalName("Test");
    private String caseDescription;
    private String staticTargetName;
    private String dynamicTargetName;

    private String callerSignature;

    public ExecutorGenerator(String caseDescription,
                             String staticTargetName,
                             String dynamicTargetName) {
        this.caseDescription = caseDescription;
        this.staticTargetName = Utils.getInternalName(staticTargetName);
        this.dynamicTargetName = Utils.getInternalName(dynamicTargetName);
        callerSignature = String.format("(L%s;)Ljava/lang/String;", this.staticTargetName);
    }

    public byte[] generateExecutor(String[] callSites) {
        ClassWriter cw = new ClassWriter(COMPUTE_MAXS);

        cw.visit(Utils.version, ACC_PUBLIC | (Utils.isACC_SUPER ? ACC_SUPER : 0), className, null, "java/lang/Object", null);

        // Generate constructor
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitEnd();
            mv.visitMaxs(0, 0);
        }

        // public static void main(String[] args) {
        //      new Test().run();
        // }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, className);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V");
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V");
            mv.visitInsn(RETURN);
            mv.visitEnd();
            mv.visitMaxs(0, 0);
        }

        //    private String indent(String result) {
        //        while (result.length() < 8) {
        //            result = " "+result;
        //        }
        //        return result;
        //    }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "indent", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I");
            mv.visitIntInsn(BIPUSH, 8);
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l1);
            mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V");
            mv.visitLdcInsn(" ");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
            mv.visitVarInsn(ASTORE, 1);
            mv.visitJumpInsn(GOTO, l0);
            mv.visitLabel(l1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARETURN);
            mv.visitEnd();
            mv.visitMaxs(0, 0);
        }

        //private String abbr(String result) {
        //      result = result.replaceAll("java.lang.NullPointerException", "NPE");
        //      result = result.replaceAll("java.lang.IllegalAccessError", "IAE");
        //      result = result.replaceAll("java.lang.IllegalAccessException", "IAExc");
        //      result = result.replaceAll("java.lang.NoSuchMethodError", "NSME");
        //      result = result.replaceAll("java.lang.AbstractMethodError", "AME");
        //      result = result.replaceAll("java.lang.IncompatibleClassChangeError", "ICCE");
        //
        //      return result;
        //}
        {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "abbr", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("java.lang.NullPointerException");
            mv.visitLdcInsn("NPE");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("java.lang.IllegalAccessError");
            mv.visitLdcInsn("IAE");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("java.lang.IllegalAccessException");
            mv.visitLdcInsn("IAExc");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("java.lang.NoSuchMethodError");
            mv.visitLdcInsn("NSME");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("java.lang.AbstractMethodError");
            mv.visitLdcInsn("AME");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("java.lang.IncompatibleClassChangeError");
            mv.visitLdcInsn("ICCE");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARETURN);
            mv.visitEnd();
            mv.visitMaxs(0, 0);
        }

        // Generate execution method
        //        public void run() {
        //            System.out.print("2048| ! a.A PUB    ! b.B PP     a.C PROT    |");
        //
        //            C object = new C();
        //
        //            try {
        //              System.out.print(indent(A.call(object)));
        //            } catch (Throwable e) {
        //              System.out.print(indent(abbr(e.getClass().getName())));
        //            }
        //
        //            ...
        //
        //            System.out.println();
        //        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
            mv.visitCode();

            // Generate try/catch blocks
            Label[][] tryCatchLabels = new Label[callSites.length][3];
            for (int I = 0; I < tryCatchLabels.length; I++) {
                Label[] labels = tryCatchLabels[I];
                for (int K = 0; K < labels.length; K++) {
                    labels[K] = new Label();
                }

                mv.visitTryCatchBlock(labels[0], labels[1], labels[2], "java/lang/Throwable");
            }

            // System.out.print("2048| ! a.A PUB    ! b.B PP     a.C PROT    |");
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(caseDescription);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");

            // C object = new C();
            mv.visitTypeInsn(NEW, dynamicTargetName);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, dynamicTargetName, "<init>", "()V");
            mv.visitVarInsn(ASTORE, 1);

//            for (String site: callSites) {
            // System.out.print(indent(A.call(object)));
//                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//                mv.visitVarInsn(ALOAD, 0);
//                mv.visitVarInsn(ALOAD, 1);
//                mv.visitMethodInsn(INVOKESTATIC, AbstractGenerator.getInternalName(site), "call", callerSignature);
//                mv.visitMethodInsn(INVOKESPECIAL, className, "indent", "(Ljava/lang/String;)Ljava/lang/String;");
//                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
//        }

            Label returnLabel = new Label();
            for (int I = 0; I < callSites.length; I++) {
                String site = callSites[I];
                Label[] l = tryCatchLabels[I];

                Label nextBlock = (I+1 < callSites.length ? tryCatchLabels[I+1][0] : returnLabel);

                mv.visitLabel(l[0]);
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESTATIC, Utils.getInternalName(site), "call", callerSignature);
                mv.visitMethodInsn(INVOKESPECIAL, className, "indent", "(Ljava/lang/String;)Ljava/lang/String;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
                mv.visitLabel(l[1]);
                mv.visitJumpInsn(GOTO, nextBlock);
                mv.visitLabel(l[2]);
                mv.visitVarInsn(ASTORE, 2);
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
                mv.visitMethodInsn(INVOKESPECIAL, className, "abbr", "(Ljava/lang/String;)Ljava/lang/String;");
                mv.visitMethodInsn(INVOKESPECIAL, className, "indent", "(Ljava/lang/String;)Ljava/lang/String;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
            }
            mv.visitLabel(returnLabel);

            // System.out.println();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "()V");
            mv.visitInsn(RETURN);

            mv.visitEnd();
            mv.visitMaxs(0, 0);
        }

        cw.visitEnd();

        return cw.toByteArray();
    }
}
