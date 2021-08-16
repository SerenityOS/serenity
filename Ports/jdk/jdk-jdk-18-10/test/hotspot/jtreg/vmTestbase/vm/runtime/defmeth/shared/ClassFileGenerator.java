/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared;


import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Type;
import nsk.share.TestFailure;
import nsk.share.test.TestUtils;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;

import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import jdk.internal.org.objectweb.asm.ClassWriter;
import static jdk.internal.org.objectweb.asm.ClassWriter.*;

import vm.runtime.defmeth.shared.data.*;
import vm.runtime.defmeth.shared.data.method.*;
import vm.runtime.defmeth.shared.data.method.body.*;
import vm.runtime.defmeth.shared.data.method.param.*;
import vm.runtime.defmeth.shared.data.method.result.*;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static vm.runtime.defmeth.shared.ExecutionMode.*;

/**
 * Constructs class file from {@code Clazz} instance.
 */
public class ClassFileGenerator implements Visitor {
    private final ExecutionMode invocationType;

    /** Default major version for generated class files
     * Used when a class doesn't specify what major version should be specified. */
    private final int defaultMajorVer;

    /** Default access flags for generated class files
     * Used when a class doesn't specify it's own access flags. */
    private final int defaultClassAccFlags;

    /** Represent current state of class file traversal.
     * Used to avoid passing instances around. */
    private ClassWriter cw;
    private MethodVisitor mv;
    private Tester t;

    private String className;

    public ClassFileGenerator() {
        this.defaultMajorVer = 52;
        this.defaultClassAccFlags = ACC_PUBLIC;
        this.invocationType = ExecutionMode.DIRECT;
    }

    public ClassFileGenerator(int ver, int acc, ExecutionMode invocationType) {
        this.defaultMajorVer = ver;
        this.defaultClassAccFlags = acc;
        this.invocationType = invocationType;
    }

    /**
     * Produce constructed class file as a {@code byte[]}.
     *
     * @return
     */
    public byte[] getClassFile() {
        return cw.toByteArray();
    }

    /**
     * Push integer constant on stack.
     *
     * Choose most suitable bytecode to represent integer constant on stack.
     *
     * @param value
     */
    private void pushIntConst(int value) {
        switch (value) {
            case 0:
                mv.visitInsn(ICONST_0);
                break;
            case 1:
                mv.visitInsn(ICONST_1);
                break;
            case 2:
                mv.visitInsn(ICONST_2);
                break;
            case 3:
                mv.visitInsn(ICONST_3);
                break;
            case 4:
                mv.visitInsn(ICONST_4);
                break;
            case 5:
                mv.visitInsn(ICONST_5);
                break;
            default:
                mv.visitIntInsn(BIPUSH, value);
        }
    }

    @Override
    public void visitClass(Clazz clz) {
        throw new IllegalStateException("More specific method should be called");
    }

    @Override
    public void visitMethod(Method m) {
        throw new IllegalStateException("More specific method should be called");
    }

    @Override
    public void visitConcreteClass(ConcreteClass clz) {
        cw = new ClassWriter(COMPUTE_FRAMES | COMPUTE_MAXS);

        int ver = clz.ver();
        int flags = clz.flags();

        className = clz.intlName();

        cw.visit((ver != 0) ? ver : defaultMajorVer,
                ACC_SUPER | ((flags != -1) ? flags : defaultClassAccFlags),
                className,
                /* signature */ clz.sig(),
                clz.parent().intlName(),
                Util.asStrings(clz.interfaces()));

        { // Default constructor: <init>()V
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, clz.parent().intlName(), "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = null;
        }

        for (Method m : clz.methods()) {
            m.visit(this);
        }

        cw.visitEnd();
    }

    @Override
    public void visitInterface(Interface intf) {
        cw = new ClassWriter(COMPUTE_FRAMES | COMPUTE_MAXS);

        int ver = intf.ver();
        int flags = intf.flags();

        className = intf.intlName();

        cw.visit(
                (ver != 0) ? ver : defaultMajorVer,
                ACC_ABSTRACT | ACC_INTERFACE | ((flags != -1) ? flags : defaultClassAccFlags),
                className,
                intf.sig(),
                "java/lang/Object",
                Util.asStrings(intf.parents()));

        for (Method m : intf.methods()) {
            m.visit(this);
        }

        cw.visitEnd();
    }

    @Override
    public void visitConcreteMethod(ConcreteMethod m) {
        mv = cw.visitMethod(
                m.acc(),
                m.name(),
                m.desc(),
                m.sig(),
                m.getExceptions());

        m.body().visit(this);

        mv = null;
    }

    @Override
    public void visitAbstractMethod(AbstractMethod m) {
        cw.visitMethod(
                ACC_ABSTRACT | m.acc(),
                m.name(),
                m.desc(),
                m.sig(),
                m.getExceptions());

    }

    @Override
    public void visitDefaultMethod(DefaultMethod m) {
        mv = cw.visitMethod(
                m.acc(),
                m.name(),
                m.desc(),
                m.sig(),
                m.getExceptions());

        m.body().visit(this);

        mv = null;
    }

    /* ====================================================================== */

    @Override
    public void visitEmptyBody(EmptyBody aThis) {
        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @Override
    public void visitThrowExBody(ThrowExBody body) {
        mv.visitCode();
        mv.visitTypeInsn(NEW, body.getExc().intlName());
        mv.visitInsn(DUP);
        //mv.visitLdcInsn(body.getMsg());
        //mv.visitMethodInsn(INVOKESPECIAL, body.getExc(), "<init>", "(Ljava/lang/String;)V", false);
        mv.visitMethodInsn(INVOKESPECIAL, body.getExc().intlName(), "<init>", "()V", false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @Override
    public void visitReturnIntBody(ReturnIntBody body) {
        mv.visitCode();
        //mv.visitIntInsn(BIPUSH, body.getValue());
        pushIntConst(body.getValue());
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @Override
    public void visitReturnNullBody(ReturnNullBody body) {
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateCall(CallMethod callSite, ExecutionMode invocationType) {
        switch (invocationType) {
            case DIRECT:
                generateDirectCall(callSite);
                break;
            case INVOKE_EXACT:
                generateMHInvokeCall(callSite, /* isExact = */ true);
                break;
            case INVOKE_GENERIC:
                generateMHInvokeCall(callSite, /* isExact = */ false);
                break;
            case INDY:
                generateIndyCall(callSite);
                break;
            default:
                throw new UnsupportedOperationException(invocationType.toString());
        }
    }

    private void prepareReceiver(CallMethod callSite) {
        switch(callSite.invokeInsn()) {
            case SPECIAL: // Put receiver (this) on stack
                if (!callSite.isConstructorCall()) {
                    mv.visitVarInsn(ALOAD, 0);
                }
                break;
            case VIRTUAL:
            case INTERFACE: // Construct receiver
                if (callSite.receiverClass() != null) {
                    String receiver = callSite.receiverClass().intlName();
                    // Construct new instance
                    mv.visitTypeInsn(NEW, receiver);
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(INVOKESPECIAL, receiver,
                            "<init>", "()V", false);
                } else {
                    // Use "this"
                    mv.visitVarInsn(ALOAD, 0);
                }
                mv.visitVarInsn(ASTORE, 1);
                mv.visitVarInsn(ALOAD, 1);
                break;
            case STATIC: break;
        }
    }

    private void prepareParams(CallMethod callSite) {
        prepareReceiver(callSite);
        // Push parameters on stack
        for (Param p : callSite.params()) {
            p.visit(this);
        }

    }

    private static Handle convertToHandle(CallMethod callSite) {
        if (callSite.isConstructorCall()) {
            return new Handle(
                    /* tag */   REF_newInvokeSpecial,
                    /* owner */ callSite.staticClass().intlName(),
                    /* name */  callSite.methodName(),
                    /* desc */  callSite.methodDesc(),
                    /* interface */ false);
        } else {
            return new Handle(
                    /* tag */ callSite.invokeInsn().tag(),
                    /* owner */ callSite.staticClass().intlName(),
                    /* name */ callSite.methodName(),
                    /* desc */ callSite.methodDesc(),
                    /* interface */ callSite.isInterface());
        }
    }

    private Handle generateBootstrapMethod(CallMethod callSite) {
        String bootstrapName = "bootstrapMethod";
        MethodType bootstrapType = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);

        MethodVisitor bmv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, bootstrapName, bootstrapType.toMethodDescriptorString(), null, null);
        bmv.visitCode();

        Handle mh = convertToHandle(callSite);

        String constCallSite = "java/lang/invoke/ConstantCallSite";
        bmv.visitTypeInsn(NEW, constCallSite);
        bmv.visitInsn(DUP);

        bmv.visitLdcInsn(mh);

        bmv.visitMethodInsn(INVOKESPECIAL, constCallSite, "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        bmv.visitInsn(ARETURN);

        bmv.visitMaxs(0,0);
        bmv.visitEnd();

        return new Handle(H_INVOKESTATIC, className, bootstrapName, bootstrapType.toMethodDescriptorString());
    }

    private static String mhCallSiteDesc(CallMethod callSite) {
        if (callSite.isConstructorCall()) {
            return String.format("()L%s;", callSite.staticClass().intlName());
        }
        if (callSite.invokeInsn() == CallMethod.Invoke.STATIC) {
            return callSite.methodDesc(); // ignore receiver
        }
        return prependType(callSite.methodDesc(), callSite.staticClass().intlName());
    }

    private void generateIndyCall(CallMethod callSite) {
        Handle bootstrap = generateBootstrapMethod(callSite);
        String callSiteDesc = mhCallSiteDesc(callSite);

        prepareParams(callSite);

        // Call method
        String name = callSite.isConstructorCall() ? "init" : callSite.methodName();
        mv.visitInvokeDynamicInsn(name, callSiteDesc, bootstrap);

        // Pop method result, if necessary
        if (callSite.popReturnValue()) {
            mv.visitInsn(POP);
        }
    }

    private void generateMHInvokeCall(CallMethod callSite, boolean isExact) {
        // Construct a method handle for a callee
        mv.visitLdcInsn(convertToHandle(callSite));

        prepareParams(callSite);

        // Call method using MH + MethodHandle.invokeExact
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandle",
                isExact ? "invokeExact" : "invoke",
                mhCallSiteDesc(callSite),
                false);

        // Pop method result, if necessary
        if (callSite.popReturnValue()) {
            mv.visitInsn(POP);
        }
    }

    // Prepend type as a first parameter
    private static String prependType(String desc, String type) {
        return desc.replaceFirst("\\(", "(L"+type+";");
    }

    private void generateDirectCall(CallMethod callSite) {
        if (callSite.isConstructorCall()) {
            String receiver = callSite.receiverClass().intlName();
            // Construct new instance
            mv.visitTypeInsn(NEW, receiver);
            mv.visitMethodInsn(INVOKESPECIAL, receiver,
                    "<init>", "()V", false);
        } else {
            prepareParams(callSite);

            // Call method
            mv.visitMethodInsn(
                    callSite.invokeInsn().opcode(),
                    callSite.staticClass().intlName(),
                    callSite.methodName(), callSite.methodDesc(),
                    callSite.isInterface());

            // Pop method result, if necessary
            if (callSite.popReturnValue()) {
                mv.visitInsn(POP);
            }
        }
    }

    @Override
    public void visitCallMethod(CallMethod callSite) {
        mv.visitCode();

        generateCall(callSite, ExecutionMode.DIRECT);

        String typeName = callSite.returnType();

        if (!callSite.popReturnValue()) {
            // Call produces some value & it isn't popped out of the stack
            // Need to return it
            switch (typeName) {
                // primitive types
                case "I" : case "B" : case "C" : case "S" : case "Z" :
                    mv.visitInsn(IRETURN);
                    break;
                case "L": mv.visitInsn(LRETURN); break;
                case "F": mv.visitInsn(FRETURN); break;
                case "D": mv.visitInsn(DRETURN); break;
                case "V": mv.visitInsn(RETURN); break;
                default:
                    // reference type
                    if ((typeName.startsWith("L") && typeName.endsWith(";"))
                        || typeName.startsWith("["))
                    {
                        mv.visitInsn(ARETURN);
                    } else {
                        throw new IllegalStateException(typeName);
                    }
            }
        } else {
            // Stack is empty. Plain return is enough.
            mv.visitInsn(RETURN);
        }

        mv.visitMaxs(0,0);
        mv.visitEnd();
    }

    @Override
    public void visitReturnNewInstanceBody(ReturnNewInstanceBody body) {
        String className = body.getType().intlName();
        mv.visitCode();
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
    }

    /* ====================================================================== */

    @Override
    public void visitTester(Tester tester) {
        // If:
        //   cw = new ClassWriter(COMPUTE_FRAMES | COMPUTE_MAXS);
        // then:
        // java.lang.RuntimeException: java.lang.ClassNotFoundException: S
        //   at jdk.internal.org.objectweb.asm.ClassWriter.getCommonSuperClass(ClassWriter.java:1588)
        //   at jdk.internal.org.objectweb.asm.ClassWriter.getMergedType(ClassWriter.java:1559)
        //   at jdk.internal.org.objectweb.asm.Frame.merge(Frame.java:1407)
        //   at jdk.internal.org.objectweb.asm.Frame.merge(Frame.java:1308)
        //   at jdk.internal.org.objectweb.asm.MethodWriter.visitMaxs(MethodWriter.java:1353)
        //mv.visitMaxs(t.getParams().length > 1 ? t.getParams().length+1 : 2, 2);

        cw = new ClassWriter(COMPUTE_MAXS);

        int testMajorVer = defaultMajorVer;

        // JSR 292 is available starting Java 7 (major version 51)
        if (invocationType == INVOKE_WITH_ARGS ||
            invocationType == INVOKE_EXACT ||
            invocationType == INVOKE_GENERIC) {
            testMajorVer = Math.max(defaultMajorVer, 51);
        }

        className = tester.intlName();

        cw.visit(testMajorVer, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", null);

        { // Test.<init>
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = null;
        }

        { // public static Test.test()V
            mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "test", "()V", null, null);
            try {
                // Generate result handling
                t = tester;
                try {
                    tester.getResult().visit(this);
                } finally {
                    t = null;
                }
            } finally {
                mv = null;
            }
        }

        cw.visitEnd();
    }

    /* ====================================================================== */

    @Override
    public void visitResultInt(IntResult res) {
        mv.visitCode();

        generateCall(t.getCall(), invocationType);

        mv.visitIntInsn(BIPUSH, res.getExpected());
        mv.visitMethodInsn(INVOKESTATIC, Util.getInternalName(TestUtils.class), "assertEquals", "(II)V", false);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }

    /**
     * Pseudo code:
     * <code>
     * {
     *   try {
     *       I i = new C(); i.m(...); // C.m(); if m is static
     *       Assert.fail();
     *   } catch (&lt;exception&gt; e) {
     *       Assert.assertEquals(&lt;message&gt;,e.getMessage());
     *   } catch (Throwable e) {
     *       throw new RuntimeException("...", e);
     *   }
     * }
     * </code>
     */
    @Override
    public void visitResultThrowExc(ThrowExResult res) {
        mv.visitCode();

        Label lblBegin = new Label();
        Label lblBootstrapMethodError = new Label();
        Label lblNoBME = new Label();
        if (invocationType == INDY) {
            mv.visitTryCatchBlock(lblBegin, lblNoBME, lblBootstrapMethodError, "java/lang/BootstrapMethodError");
        }

        Label lblExpected = new Label();
        mv.visitTryCatchBlock(lblBegin, lblExpected, lblExpected, res.getExc().intlName());

        Label lblThrowable = new Label();
        mv.visitTryCatchBlock(lblBegin, lblExpected, lblThrowable, "java/lang/Throwable");


        mv.visitLabel(lblBegin);

        generateCall(t.getCall(), invocationType);


        if (Util.isNonVoid(t.getCall().returnType())) {
            mv.visitInsn(POP);
        }

        mv.visitLabel(lblNoBME);

        // throw new TestFailure("No exception was thrown")
        mv.visitTypeInsn(NEW, "nsk/share/TestFailure");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("No exception was thrown");
        mv.visitMethodInsn(INVOKESPECIAL, "nsk/share/TestFailure", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);

        // Unwrap exception during call site resolution from BootstrapMethodError
        if (invocationType == INDY) {
            // } catch (BootstrapMethodError e) {
            //     throw e.getCause();
            // }
            mv.visitLabel(lblBootstrapMethodError);
            mv.visitFrame(F_SAME1, 0, null, 1, new Object[] {"java/lang/BootstrapMethodError"});
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/BootstrapMethodError", "getCause", "()Ljava/lang/Throwable;", false);

            Label lblIsNull = new Label();
            mv.visitJumpInsn(IFNULL, lblIsNull);
            // e.getCause() != null
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/BootstrapMethodError", "getCause", "()Ljava/lang/Throwable;", false);
            mv.visitInsn(ATHROW);

            // e.getCause() == null
            mv.visitLabel(lblIsNull);
            mv.visitFrame(F_APPEND, 2, new Object[] {TOP, "java/lang/BootstrapMethodError"}, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/BootstrapMethodError", "getCause", "()Ljava/lang/Throwable;", false);
            mv.visitInsn(ATHROW);
        }

        // } catch (<exception> e) {
        //   //if <message> != null
        //   Assert.assertEquals(<message>,e.getMessage());
        // }
        mv.visitLabel(lblExpected);
        mv.visitFrame(F_FULL, 0, new Object[] {}, 1, new Object[] { res.getExc().intlName() });

        mv.visitVarInsn(ASTORE, 1);

        // Exception class comparison, if exact match is requested
        if (res.isExact()) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(Type.getType("L"+res.getExc().intlName()+";"));
            mv.visitMethodInsn(INVOKESTATIC, Util.getInternalName(TestUtils.class), "assertExactClass", "(Ljava/lang/Object;Ljava/lang/Class;)V", false);
        }

        // Compare exception's message, if needed
        if (res.getMessage() != null) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(res.getMessage());
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "getMessage", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESTATIC, Util.getInternalName(TestUtils.class), "assertEquals", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        }

        mv.visitInsn(RETURN);

        // } catch (Throwable e) {
        //     throw new RuntimeException("Expected exception <exception>", e);
        // }
        mv.visitLabel(lblThrowable);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
        mv.visitVarInsn(ASTORE, 1);

        //     e.printStackTrace();
        //mv.visitVarInsn(ALOAD, 1);
        //mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);

        //     String msg = String.format("Expected exception J, got: %s: %s",
        //                                e.getClass(), e.getMessage());
        //     throw new RuntimeException(msg, e);
        mv.visitTypeInsn(NEW, Util.getInternalName(TestFailure.class));
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Expected exception " + res.getExc().name() + ", got: %s: %s");
        mv.visitInsn(ICONST_2);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitInsn(AASTORE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "getMessage", "()Ljava/lang/String;", false);
        mv.visitInsn(AASTORE);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);

        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, Util.getInternalName(TestFailure.class), "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
        mv.visitInsn(ATHROW);
        // end of lblThrowable

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @Override
    public void visitResultIgnore() {
        mv.visitCode();

        generateCall(t.getCall(), invocationType);

        if (Util.isNonVoid(t.getCall().returnType())) {
            mv.visitInsn(POP);
        }

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* ====================================================================== */

    @Override
    public void visitParamInt(IntParam i) {
        pushIntConst(i.value());
    }

    @Override
    public void visitParamLong(LongParam l) {
        long value = l.value();

        if (value == 0L) {
            mv.visitInsn(LCONST_0);
        } else {
            mv.visitLdcInsn(Long.valueOf(value));
        }
    }

    @Override
    public void visitParamFloat(FloatParam f) {
        float value = f.value();

        if (value == 0.0f) {
            mv.visitInsn(FCONST_0);
        } else if (value == 1.0f) {
            mv.visitInsn(FCONST_1);
        } else if (value == 2.0f) {
            mv.visitInsn(FCONST_2);
        } else {
            mv.visitLdcInsn(Float.valueOf(value));
        }
    }

    @Override
    public void visitParamDouble(DoubleParam d) {
        double value = d.value();

        if (value == 0.0d) {
            mv.visitInsn(DCONST_0);
        } else if (value == 1.0d) {
            mv.visitInsn(DCONST_1);
        } else {
            mv.visitLdcInsn(Double.valueOf(value));
        }
    }

    @Override
    public void visitParamNewInstance(NewInstanceParam param) {
        String className = param.clazz().intlName();

        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
    }

    @Override
    public void visitParamNull() {
        mv.visitInsn(ACONST_NULL);
    }

    @Override
    public void visitParamString(StringParam str) {
        mv.visitLdcInsn(str.value());
    }
}
