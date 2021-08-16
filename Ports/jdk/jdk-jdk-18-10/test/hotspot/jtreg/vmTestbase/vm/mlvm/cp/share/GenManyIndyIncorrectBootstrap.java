/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.cp.share;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.ClassWriterExt;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.Label;

import vm.mlvm.share.ClassfileGenerator;
import vm.mlvm.share.Env;

public class GenManyIndyIncorrectBootstrap extends GenFullCP {

    /**
     * Generates a class file and writes it to a file
     * @see vm.mlvm.share.ClassfileGenerator
     * @param args Parameters for ClassfileGenerator.main() method
     */
    public static void main(String[] args) {
        ClassfileGenerator.main(args);
    }

    /**
     * Create class constructor, which
     * create a call site for target method
     * and puts it into static and instance fields
     * @param cw Class writer object
     */
    @Override
    protected void createInitMethod(ClassWriter cw) {
        MethodVisitor mw = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                INIT_METHOD_NAME, INIT_METHOD_SIGNATURE,
                null,
                new String[0]);

        mw.visitVarInsn(Opcodes.ALOAD, 0);
        mw.visitMethodInsn(Opcodes.INVOKESPECIAL, PARENT_CLASS_NAME,
                INIT_METHOD_NAME, INIT_METHOD_SIGNATURE);

        // Create a call site for the target method and store it into bootstrap fields
        mw.visitVarInsn(Opcodes.ALOAD, 0);
        mw.visitTypeInsn(Opcodes.NEW, JLI_CONSTANTCALLSITE);
        mw.visitInsn(Opcodes.DUP);
        mw.visitMethodInsn(Opcodes.INVOKESTATIC, JLI_METHODHANDLES,
                "lookup", "()" + fd(JLI_METHODHANDLES_LOOKUP));
        mw.visitLdcInsn(Type.getObjectType(fullClassName));
        mw.visitLdcInsn(TARGET_METHOD_NAME);
        mw.visitLdcInsn(TARGET_METHOD_SIGNATURE);
        mw.visitLdcInsn(Type.getObjectType(fullClassName));
        mw.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JL_CLASS,
                "getClassLoader", "()" + fd(JL_CLASSLOADER));
        mw.visitMethodInsn(Opcodes.INVOKESTATIC, JLI_METHODTYPE,
                "fromMethodDescriptorString", "(" + fd(JL_STRING) + fd(JL_CLASSLOADER) + ")" + fd(JLI_METHODTYPE));
        mw.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JLI_METHODHANDLES_LOOKUP,
                "findStatic", "(" + fd(JL_CLASS) + fd(JL_STRING) + fd(JLI_METHODTYPE) + ")" + fd(JLI_METHODHANDLE));
        mw.visitMethodInsn(Opcodes.INVOKESPECIAL, JLI_CONSTANTCALLSITE,
                INIT_METHOD_NAME, "(" + fd(JLI_METHODHANDLE) + ")V");
        mw.visitInsn(Opcodes.DUP);
        mw.visitFieldInsn(Opcodes.PUTSTATIC, fullClassName, STATIC_BOOTSTRAP_FIELD_NAME, STATIC_BOOTSTRAP_FIELD_SIGNATURE);
        mw.visitFieldInsn(Opcodes.PUTFIELD, fullClassName, INSTANCE_BOOTSTRAP_FIELD_NAME, INSTANCE_BOOTSTRAP_FIELD_SIGNATURE);

        finishMethodCode(mw);
    }

    /**
     * Creates a target method which always throw. It should not be called,
     * since all invokedynamic instructions have invalid bootstrap method types
     * @param cw Class writer object
     */
    @Override
    protected void createTargetMethod(ClassWriter cw) {
        createThrowRuntimeExceptionMethod(cw, true, TARGET_METHOD_NAME, TARGET_METHOD_SIGNATURE);
    }

    /**
     * Creates a bootstrap method which always throw. It should not be called,
     * since all invokedynamic instructions have invalid bootstrap method types
     * @param cw Class writer object
     */
    @Override
    protected void createBootstrapMethod(ClassWriter cw) {
        createThrowRuntimeExceptionMethod(cw, true, BOOTSTRAP_METHOD_NAME, BOOTSTRAP_METHOD_SIGNATURE);
    }

    /**
     * Generates common data for class plus two fields that hold CallSite
     * and used as bootstrap targets
     * @param cw Class writer object
     */
    @Override
    protected void generateCommonData(ClassWriterExt cw) {
        cw.setCacheInvokeDynamic(false);

        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                STATIC_BOOTSTRAP_FIELD_NAME,
                STATIC_BOOTSTRAP_FIELD_SIGNATURE, null, null);

        cw.visitField(Opcodes.ACC_PUBLIC,
                INSTANCE_BOOTSTRAP_FIELD_NAME,
                INSTANCE_BOOTSTRAP_FIELD_SIGNATURE, null, null);

        super.generateCommonData(cw);

        createThrowRuntimeExceptionMethod(cw, false, INSTANCE_BOOTSTRAP_METHOD_NAME, INSTANCE_BOOTSTRAP_METHOD_SIGNATURE);
    }

    Label throwMethodLabel;

    // The exception to expect that is wrapped in a BootstrapMethodError
    static final String WRAPPED_EXCEPTION = "java/lang/invoke/WrongMethodTypeException";

    // The error to expect that is not wrapped in a BootstrapMethodError and
    // is thrown directly
    static final String DIRECT_ERROR = "java/lang/IncompatibleClassChangeError";

    /**
     * Generates an invokedynamic instruction (plus CP entry)
     * which has invalid reference kind in the CP method handle entry for the bootstrap method
     * @param cw Class writer object
     * @param mw Method writer object
     */
    @Override
    protected void generateCPEntryData(ClassWriter cw, MethodVisitor mw) {
        HandleType[] types = HandleType.values();
        HandleType type = types[Env.getRNG().nextInt(types.length)];

        switch (type) {
            case GETFIELD:
            case PUTFIELD:
            case GETSTATIC:
            case PUTSTATIC:
            case INVOKESPECIAL:
            case INVOKEVIRTUAL:
            case INVOKEINTERFACE:
                // Handle these cases
                break;
            default:
                // And don't generate code for all other cases
                return;
        }

        Label indyThrowableBegin = new Label();
        Label indyThrowableEnd = new Label();
        Label catchThrowableLabel = new Label();

        Label indyBootstrapBegin = new Label();
        Label indyBootstrapEnd = new Label();
        Label catchBootstrapLabel = new Label();

        mw.visitTryCatchBlock(indyBootstrapBegin, indyBootstrapEnd, catchBootstrapLabel, JL_BOOTSTRAPMETHODERROR);
        mw.visitLabel(indyBootstrapBegin);

        mw.visitTryCatchBlock(indyThrowableBegin, indyThrowableEnd, catchThrowableLabel, JL_THROWABLE);
        mw.visitLabel(indyThrowableBegin);

        Handle bsm;
        switch (type) {
            case GETFIELD:
            case PUTFIELD:
                bsm = new Handle(type.asmTag,
                        fullClassName,
                        INSTANCE_BOOTSTRAP_FIELD_NAME,
                        INSTANCE_BOOTSTRAP_FIELD_SIGNATURE);
                break;
            case GETSTATIC:
            case PUTSTATIC:
                bsm = new Handle(type.asmTag,
                        fullClassName,
                        STATIC_BOOTSTRAP_FIELD_NAME,
                        STATIC_BOOTSTRAP_FIELD_SIGNATURE);
                break;
            case INVOKESPECIAL:
            case INVOKEVIRTUAL:
            case INVOKEINTERFACE:
                bsm = new Handle(type.asmTag,
                        fullClassName,
                        INSTANCE_BOOTSTRAP_METHOD_NAME,
                        INSTANCE_BOOTSTRAP_METHOD_SIGNATURE);
                break;
            default:
                throw new Error("Unexpected handle type " + type);
        }

        mw.visitInvokeDynamicInsn(TARGET_METHOD_NAME,
                TARGET_METHOD_SIGNATURE,
                bsm);

        mw.visitLabel(indyBootstrapEnd);
        mw.visitLabel(indyThrowableEnd);

        // No exception at all, throw error
        Label throwLabel = new Label();
        mw.visitJumpInsn(Opcodes.GOTO, throwLabel);

        // JDK-8079697 workaround: we have to generate stackmaps manually
        mw.visitFrame(Opcodes.F_SAME1, 0, new Object[0], 1, new Object[] { JL_BOOTSTRAPMETHODERROR });

        // Got a bootstrapmethoderror as expected, check that it is wrapping what we expect
        mw.visitLabel(catchBootstrapLabel);

        // Save error in case we need to rethrow it
        mw.visitInsn(Opcodes.DUP);
        mw.visitVarInsn(Opcodes.ASTORE, 1);
        mw.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JL_THROWABLE, "getCause", "()" + fd(JL_THROWABLE));

        // If it is the expected exception, goto next block
        mw.visitTypeInsn(Opcodes.INSTANCEOF, WRAPPED_EXCEPTION);
        Label nextBlockLabel = new Label();
        mw.visitJumpInsn(Opcodes.IFNE, nextBlockLabel);

        // Not the exception we were expectiong, throw error
        mw.visitVarInsn(Opcodes.ALOAD, 1); // Use full chain as cause
        createThrowRuntimeExceptionCodeWithCause(mw,
                "invokedynamic got an unexpected wrapped exception (expected " + WRAPPED_EXCEPTION
                + ", bootstrap type=" + type
                + ", opcode=" + type.asmTag + ")!");

        // JDK-8079697 workaround: we have to generate stackmaps manually
        mw.visitFrame(Opcodes.F_SAME1, 0, new Object[0], 1, new Object[] { JL_THROWABLE });
        mw.visitLabel(catchThrowableLabel);

        // Save error in case we need to rethrow it
        mw.visitInsn(Opcodes.DUP);
        mw.visitVarInsn(Opcodes.ASTORE, 1);

        // If it is the expected exception, goto next block
        mw.visitTypeInsn(Opcodes.INSTANCEOF, DIRECT_ERROR);
        mw.visitJumpInsn(Opcodes.IFNE, nextBlockLabel);

        // Not the exception we were expectiong, throw error
        mw.visitVarInsn(Opcodes.ALOAD, 1); // Use full chain as cause
        createThrowRuntimeExceptionCodeWithCause(mw,
                "invokedynamic got an unexpected exception (expected " + DIRECT_ERROR
                + ", bootstrap type" + type
                + ", opcode=" + type.asmTag + ")!");

        // JDK-8079697 workaround: we have to generate stackmaps manually
        mw.visitFrame(Opcodes.F_CHOP, 0, new Object[0], 0, new Object[0]);

        // Unable to place this code once in the method epilog due to bug in ASM
        mw.visitLabel(throwLabel);
        createThrowRuntimeExceptionCode(mw,
                "invokedynamic should always throw (bootstrap type" + type +", opcode=" + type.asmTag + ")!");

        mw.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
        mw.visitLabel(nextBlockLabel);
    }
}
