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

package selectionresolution;

import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.MethodVisitor;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.DUP;
import static jdk.internal.org.objectweb.asm.Opcodes.POP;
import static jdk.internal.org.objectweb.asm.Opcodes.NEW;
import static jdk.internal.org.objectweb.asm.Opcodes.SWAP;
import static jdk.internal.org.objectweb.asm.Opcodes.ASTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKEINTERFACE;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKEVIRTUAL;

class Method {
    public static final String defaultMethodName        = "m";
    public static final String defaultMethodDescriptor  = "()Ljava/lang/Integer;";
    public static final String methodDescriptorTemplate = "(L%s;)Ljava/lang/Integer;";
    private final ClassConstruct ownerClass;
    private final String ownerClassName;
    private final ClassVisitor cv;
    private final MethodVisitor mv;
    private final ClassBuilder.ExecutionMode execMode;

    public Method(ClassConstruct ownerClass, ClassVisitor cv, String name, String descriptor, int access,
                  ClassBuilder.ExecutionMode execMode) {
        this.ownerClassName = ownerClass.getName();
        this.ownerClass = ownerClass;
        this.execMode = execMode;
        this.cv = cv;
        mv = cv.visitMethod(access, name, descriptor, null, null);
        mv.visitCode();
    }
    /**
     * Add code for the m()Ljava/lang/Integer; method, always returns null
     */
    public void makeDefaultMethod() {
        mv.visitTypeInsn(NEW, "java/lang/Integer");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(ownerClass.getIndex());
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void makePrivateCallMethod(String className) {
        makeSuperCallMethod(INVOKESPECIAL, className);
    }

    public void makeSuperCallMethod(int invokeInstruction, String className) {
        mv.visitVarInsn(ALOAD, 0);
        makeCall(invokeInstruction, className, false);
        mv.visitInsn(POP);
        done();
    }

    public void defaultInvoke(int instr, String className, String objectRef, boolean isInterface) {
        switch (instr) {
            case INVOKEVIRTUAL:
                defaultInvokeVirtual(className, objectRef);
                break;
            case INVOKEINTERFACE:
                defaultInvokeInterface(className, objectRef);
                break;
            case INVOKESTATIC:
                defaultInvokeStatic(className, isInterface);
                break;
            case INVOKESPECIAL:
                defaultInvokeSpecial(className, objectRef, isInterface);
                break;
            default:
                break;
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void defaultInvokeVirtual(String className, String objectRef) {
        String objectRefPackageName = objectRef.substring(0, objectRef.lastIndexOf("/"));
        makeNewObject(objectRef, objectRefPackageName);
        makeCall(INVOKEVIRTUAL, className, false);
    }

    private void defaultInvokeInterface(String className, String objectRef) {
        String objectRefPackageName = objectRef.substring(0, objectRef.lastIndexOf("/"));
        makeNewObject(objectRef, objectRefPackageName);
        makeCall(INVOKEINTERFACE, className, true);
    }

    private void defaultInvokeSpecial(String className, String objectRef, boolean isInterface) {
        String objectRefPackageName = objectRef.substring(0, objectRef.lastIndexOf("/"));
        makeNewObject(objectRef, objectRefPackageName);
        makeCall(INVOKESPECIAL, className, isInterface);
    }

    private void defaultInvokeStatic(String className, boolean isInterface) {
        makeCall(INVOKESTATIC, className, isInterface);
    }

    private Method makeCall(int invokeInstruction, String className, boolean isInterface) {
        switch(execMode) {
            case DIRECT: {
                mv.visitMethodInsn(invokeInstruction, className, defaultMethodName, defaultMethodDescriptor, isInterface);
                break;
            }
            case INDY: {
                Handle m = convertToHandle(invokeInstruction, className, defaultMethodName, defaultMethodDescriptor);
                Handle bsm = generateBootstrapMethod(m);
                mv.visitInvokeDynamicInsn(defaultMethodName, defaultMethodDescriptor, bsm);
                break;
            }
            case MH_INVOKE_EXACT:
            case MH_INVOKE_GENERIC: {
                String invokerName = execMode == ClassBuilder.ExecutionMode.MH_INVOKE_GENERIC
                        ? "invoke" : "invokeExact";

                Handle m = convertToHandle(invokeInstruction, className, defaultMethodName, defaultMethodDescriptor);
                mv.visitLdcInsn(m);
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandle",
                        invokerName,
                        String.format(methodDescriptorTemplate, className),
                        false);
                break;
            }
            default:
                throw new Error("Unknown execution mode: " + execMode);

        }
        return this;
    }

    private Handle generateBootstrapMethod(Handle h) {
        String bootstrapName = "bootstrapMethod";
        MethodType bootstrapType = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);

        MethodVisitor bmv = cv.visitMethod(ACC_PUBLIC | ACC_STATIC, bootstrapName, bootstrapType.toMethodDescriptorString(), null, null);
        bmv.visitCode();

        String constCallSite = "java/lang/invoke/ConstantCallSite";
        bmv.visitTypeInsn(NEW, constCallSite);
        bmv.visitInsn(DUP);

        bmv.visitLdcInsn(h);

        bmv.visitMethodInsn(INVOKESPECIAL, constCallSite, "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        bmv.visitInsn(ARETURN);

        bmv.visitMaxs(0,0);
        bmv.visitEnd();

        return new Handle(H_INVOKESTATIC, ownerClassName, bootstrapName, bootstrapType.toMethodDescriptorString());
    }


    private static Handle convertToHandle(int invokeInstruction, String className, String methodName, String methodDesc) {
        int tag;
        switch (invokeInstruction) {
            case INVOKEVIRTUAL:   tag = H_INVOKEVIRTUAL;   break;
            case INVOKEINTERFACE: tag = H_INVOKEINTERFACE; break;
            case INVOKESPECIAL:   tag = H_INVOKESPECIAL;   break;
            case INVOKESTATIC:    tag = H_INVOKESTATIC;    break;
            default:
                throw new Error("Unknown invoke instruction: "+invokeInstruction);
        }

        return new Handle(tag, className, methodName, methodDesc);
    }

    private void makeNewObject(String objectRef, String objectRefPackageName) {
        String className = objectRef.substring(objectRef.lastIndexOf("/") + 1);
        makeStaticCall( objectRefPackageName + "/Helper",
                        "get" + className,
                        "()L" + objectRef + ";", false);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitVarInsn(ALOAD, 1);
    }

    public void makeTestCall(String className) {
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "test", "()Ljava/lang/Integer;", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    public Method makeStaticCall(String classname, String method, String descriptor, boolean isInterface) {
        mv.visitMethodInsn(INVOKESTATIC, classname, method, descriptor, isInterface);
        return this;
    }

    public void makeConstructor(String extending, boolean isInterface) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, extending == null ? "java/lang/Object" : extending, "<init>", "()V", isInterface);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void makeInstantiateMethod(String className) {
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void done() {
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
