/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.org.objectweb.asm.ByteVector;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.ClassWriterExt;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;

import vm.mlvm.share.ClassfileGenerator;
import vm.mlvm.share.Env;

public abstract class GenFullCP extends ClassfileGenerator {

    /**
     * Generate field description for object type from class name:
     * return "L" + className + ";";
     * @param className Class name
     * @return field descriptor representing the class type
     */
    protected static String fd(String className) {
        return "L" + className + ";";
    }

    // Universal constants
    protected static final String JL_OBJECT = "java/lang/Object";
    protected static final String JL_CLASS = "java/lang/Class";
    protected static final String JL_CLASSLOADER  = "java/lang/ClassLoader";
    protected static final String JL_STRING = "java/lang/String";
    protected static final String JL_RUNTIMEEXCEPTION = "java/lang/RuntimeException";
    protected static final String JL_BOOTSTRAPMETHODERROR = "java/lang/BootstrapMethodError";
    protected static final String JL_THROWABLE = "java/lang/Throwable";
    protected static final String JLI_METHODTYPE = "java/lang/invoke/MethodType";
    protected static final String JLI_METHODHANDLE = "java/lang/invoke/MethodHandle";
    protected static final String JLI_METHODHANDLES = "java/lang/invoke/MethodHandles";
    protected static final String JLI_METHODHANDLES_LOOKUP = "java/lang/invoke/MethodHandles$Lookup";
    protected static final String JLI_CALLSITE = "java/lang/invoke/CallSite";
    protected static final String JLI_CONSTANTCALLSITE = "java/lang/invoke/ConstantCallSite";

    protected static final String VOID_NO_ARG_METHOD_SIGNATURE = "()V";

    protected static final String NEW_INVOKE_SPECIAL_CLASS_NAME = "java/lang/invoke/NewInvokeSpecialCallSite";
    protected static final String NEW_INVOKE_SPECIAL_BOOTSTRAP_METHOD_SIGNATURE = "(" + fd(JLI_METHODHANDLES_LOOKUP) + fd(JL_STRING) + fd(JLI_METHODTYPE) + ")V";

    protected static final String INIT_METHOD_NAME = "<init>";
    protected static final String STATIC_INIT_METHOD_NAME = "<clinit>";

    // Generated class constants
    protected static final int CLASSFILE_VERSION = 51;

    protected static final int CP_CONST_COUNT = 65400;
    protected static final int MAX_METHOD_SIZE = 65400;
    protected static final int BYTES_PER_LDC = 5;
    protected static final int LDC_PER_METHOD = MAX_METHOD_SIZE / BYTES_PER_LDC;
    protected static final int METHOD_COUNT = CP_CONST_COUNT / LDC_PER_METHOD;

    protected static final String PARENT_CLASS_NAME = JL_OBJECT;

    protected static final String INIT_METHOD_SIGNATURE = VOID_NO_ARG_METHOD_SIGNATURE;

    protected static final String MAIN_METHOD_NAME = "main";
    protected static final String MAIN_METHOD_SIGNATURE = "(" + "[" + fd(JL_STRING) + ")V";

    protected static final String TEST_METHOD_NAME = "test";
    protected static final String TEST_METHOD_SIGNATURE = VOID_NO_ARG_METHOD_SIGNATURE;

    protected static final String STATIC_FIELD_NAME = "testStatic";
    protected static final String STATIC_FIELD_SIGNATURE = "Z";

    protected static final String INSTANCE_FIELD_NAME = "testInstance";
    protected static final String INSTANCE_FIELD_SIGNATURE = "Z";

    protected static final String STATIC_BOOTSTRAP_FIELD_NAME = "testCSStatic";
    protected static final String STATIC_BOOTSTRAP_FIELD_SIGNATURE = fd(JLI_CALLSITE);

    protected static final String INSTANCE_BOOTSTRAP_FIELD_NAME = "testCSInstance";
    protected static final String INSTANCE_BOOTSTRAP_FIELD_SIGNATURE = fd(JLI_CALLSITE);

    protected static final String BOOTSTRAP_METHOD_NAME = "bootstrap";
    protected static final String BOOTSTRAP_METHOD_SIGNATURE = "(" +  fd(JLI_METHODHANDLES_LOOKUP) + fd(JL_STRING) + fd(JLI_METHODTYPE) + ")" + fd(JLI_CALLSITE);

    protected static final String INSTANCE_BOOTSTRAP_METHOD_NAME = "bootstrapInstance";
    protected static final String INSTANCE_BOOTSTRAP_METHOD_SIGNATURE = BOOTSTRAP_METHOD_SIGNATURE;

    protected static final String TARGET_METHOD_NAME = "target";
    protected static final String TARGET_METHOD_SIGNATURE = VOID_NO_ARG_METHOD_SIGNATURE;

    protected static final String INSTANCE_TARGET_METHOD_NAME = "targetInstance";
    protected static final String INSTANCE_TARGET_METHOD_SIGNATURE = VOID_NO_ARG_METHOD_SIGNATURE;

    protected interface DummyInterface {
        public void targetInstance();
    }

    // Helper methods

    protected static String getDummyInterfaceClassName() {
        return DummyInterface.class.getName().replace('.', '/');
    }

    protected static void createLogMsgCode(MethodVisitor mv, String msg) {
        mv.visitLdcInsn(msg);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "vm/mlvm/share/Env", "traceVerbose", "(Ljava/lang/String;)V");
    }

    protected static void createThrowRuntimeExceptionCode(MethodVisitor mv, String msg) {
        createThrowRuntimeExceptionCodeHelper(mv, msg, false);
    }

    // Expects a throwable (the cause) to be on top of the stack when called.
    protected static void createThrowRuntimeExceptionCodeWithCause(MethodVisitor mv, String msg) {
        createThrowRuntimeExceptionCodeHelper(mv, msg, true);
    }

    // If set_cause is true it expects a Throwable (the cause) to be on top of the stack when called.
    protected static void createThrowRuntimeExceptionCodeHelper(MethodVisitor mv, String msg, boolean set_cause) {
        mv.visitTypeInsn(Opcodes.NEW, JL_RUNTIMEEXCEPTION);
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(msg);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, JL_RUNTIMEEXCEPTION,
                INIT_METHOD_NAME, "(" + fd(JL_STRING) + ")V");
        if (set_cause) {
          mv.visitInsn(Opcodes.SWAP);
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JL_RUNTIMEEXCEPTION,
                  "initCause", "(" + fd(JL_THROWABLE) + ")"+ fd(JL_THROWABLE));
        }
        mv.visitInsn(Opcodes.ATHROW);
    }

    protected static void createThrowRuntimeExceptionMethod(ClassWriter cw, boolean isStatic, String methodName, String methodSignature) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0),
                methodName, methodSignature,
                null,
                new String[0]);

        createThrowRuntimeExceptionCode(mv, "Method " + methodName + methodSignature + " should not be called!");

        mv.visitMaxs(-1,  -1);
        mv.visitEnd();
    }

    protected static void finishMethodCode(MethodVisitor mv) {
        finishMethodCode(mv, Opcodes.RETURN);
    }

    protected static void finishMethodCode(MethodVisitor mv, int returnOpcode) {
        mv.visitInsn(returnOpcode);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    protected void createClassInitMethod(ClassWriter cw) {
    }

    protected void createInitMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                INIT_METHOD_NAME, INIT_METHOD_SIGNATURE,
                null,
                new String[0]);

        mv.visitIntInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                PARENT_CLASS_NAME,
                INIT_METHOD_NAME, INIT_METHOD_SIGNATURE);

        createLogMsgCode(mv, fullClassName + " constructor called");

        finishMethodCode(mv);
    }

    protected void createTargetMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                TARGET_METHOD_NAME, TARGET_METHOD_SIGNATURE,
                null,
                new String[0]);

        createLogMsgCode(mv, fullClassName + "." + TARGET_METHOD_NAME + TARGET_METHOD_SIGNATURE + " called");

        finishMethodCode(mv);
    }

    protected void createBootstrapMethod(ClassWriter cw) {
         createBootstrapMethod(cw, true, BOOTSTRAP_METHOD_NAME, BOOTSTRAP_METHOD_SIGNATURE);
    }

    protected void createBootstrapMethod(ClassWriter cw, boolean isStatic, String methodName, String methodSignature) {
        MethodVisitor mv = cw.visitMethod(
                (isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC,
                methodName, methodSignature,
                null, new String[0]);

        createLogMsgCode(mv, fullClassName + "." + BOOTSTRAP_METHOD_NAME + BOOTSTRAP_METHOD_SIGNATURE + " called");

        int argShift = isStatic ? 0 : 1;

        mv.visitTypeInsn(Opcodes.NEW, JLI_CONSTANTCALLSITE);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 0 + argShift);
        mv.visitLdcInsn(Type.getObjectType(fullClassName));
        mv.visitVarInsn(Opcodes.ALOAD, 1 + argShift);
        mv.visitVarInsn(Opcodes.ALOAD, 2 + argShift);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                JLI_METHODHANDLES_LOOKUP, "findStatic",
                "(" + fd(JL_CLASS) + fd(JL_STRING) + fd(JLI_METHODTYPE) + ")" + fd(JLI_METHODHANDLE));
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, JLI_CONSTANTCALLSITE,
                INIT_METHOD_NAME, "(" + fd(JLI_METHODHANDLE) + ")V");

        finishMethodCode(mv, Opcodes.ARETURN);
    }

    @Override
    public Klass[] generateBytecodes() {

        // COMPUTE_FRAMES were disabled due to JDK-8079697
        ClassWriterExt cw = new ClassWriterExt(/*ClassWriter.COMPUTE_FRAMES |*/ ClassWriter.COMPUTE_MAXS);

        String[] interfaces = new String[1];
        interfaces[0] = getDummyInterfaceClassName();
        cw.visit(CLASSFILE_VERSION, Opcodes.ACC_PUBLIC, fullClassName, null, PARENT_CLASS_NAME, interfaces);

        generateCommonData(cw);

        MethodVisitor mainMV = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                MAIN_METHOD_NAME, MAIN_METHOD_SIGNATURE,
                null, new String[0]);

        mainMV.visitTypeInsn(Opcodes.NEW, fullClassName);
        mainMV.visitInsn(Opcodes.DUP);
        mainMV.visitMethodInsn(Opcodes.INVOKESPECIAL, fullClassName, INIT_METHOD_NAME, INIT_METHOD_SIGNATURE);

        int constCount = 0;
        int methodNum = 0;

        // TODO: check real CP size and also limit number of iterations in this cycle
        while (constCount < CP_CONST_COUNT) {
            final String methodName = TEST_METHOD_NAME + String.format("%02d", methodNum);

            MethodVisitor mw = cw.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    methodName, TEST_METHOD_SIGNATURE,
                    null, new String[0]);

            generateTestMethodProlog(mw);

            // TODO: check real CP size and also limit number of iterations in this cycle
            while (constCount < CP_CONST_COUNT && cw.getBytecodeLength(mw) < MAX_METHOD_SIZE) {
                generateCPEntryData(cw, mw);
                ++constCount;
            }

            generateTestMethodEpilog(mw);

            mw.visitMaxs(-1, -1);
            mw.visitEnd();

            Env.traceNormal("Method " + fullClassName + "." + methodName + "(): "
                          + constCount + " constants in CP, "
                          + cw.getBytecodeLength(mw) + " bytes of code");

            mainMV.visitInsn(Opcodes.DUP);
            mainMV.visitMethodInsn(Opcodes.INVOKEVIRTUAL, fullClassName, methodName, TEST_METHOD_SIGNATURE);

            ++methodNum;
        }

        mainMV.visitInsn(Opcodes.POP);
        finishMethodCode(mainMV);

        cw.visitEnd();
        return new Klass[] { new Klass(this.pkgName, this.shortClassName, MAIN_METHOD_NAME, MAIN_METHOD_SIGNATURE, cw.toByteArray()) };
    }

    protected void generateCommonData(ClassWriterExt cw) {
        createClassInitMethod(cw);
        createInitMethod(cw);
        createTargetMethod(cw);
        createBootstrapMethod(cw);
    }

    protected void generateTestMethodProlog(MethodVisitor mw) {
    }

    protected abstract void generateCPEntryData(ClassWriter cw, MethodVisitor mw);

    protected void generateTestMethodEpilog(MethodVisitor mw) {
        mw.visitInsn(Opcodes.RETURN);
    }

}
