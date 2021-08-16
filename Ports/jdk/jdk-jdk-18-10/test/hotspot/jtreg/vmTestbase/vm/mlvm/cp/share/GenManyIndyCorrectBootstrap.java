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

import vm.mlvm.share.ClassfileGenerator;
import vm.mlvm.share.Env;

public class GenManyIndyCorrectBootstrap extends GenFullCP {

    /**
     * Generates a class file and writes it to a file
     * @see vm.mlvm.share.ClassfileGenerator
     * @param args Parameters for ClassfileGenerator.main() method
     */
    public static void main(String[] args) {
        ClassfileGenerator.main(args);
    }

    /**
     * Creates static init method, which constructs a call site object, which refers to the target method
     * and invokes Dummy.setMH() on this call site
     * @param cw Class writer object
     */
    @Override
    protected void createClassInitMethod(ClassWriter cw) {
        MethodVisitor mw = cw.visitMethod(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                STATIC_INIT_METHOD_NAME, INIT_METHOD_SIGNATURE,
                null,
                new String[0]);

        mw.visitMethodInsn(Opcodes.INVOKESTATIC, JLI_METHODHANDLES, "lookup", "()" + fd(JLI_METHODHANDLES_LOOKUP));
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
        mw.visitMethodInsn(Opcodes.INVOKESTATIC, NEW_INVOKE_SPECIAL_CLASS_NAME,
                "setMH", "(" + fd(JLI_METHODHANDLE) + ")V");

        finishMethodCode(mw);
    }

    /**
     * Disables invoke dynamic CP entry caching and generate default common data
     * @param cw Class writer object
     */
    @Override
    protected void generateCommonData(ClassWriterExt cw) {
        cw.setCacheInvokeDynamic(false);
        super.generateCommonData(cw);
    }

    /**
     * Generates an invokedynamic instruction (plus CP entry)
     * which has a valid reference kind in the CP method handle entry for the bootstrap method
     * @param cw Class writer object
     * @param mw Method writer object
     */
    @Override
    protected void generateCPEntryData(ClassWriter cw, MethodVisitor mw) {
        Handle bsm;
        if (Env.getRNG().nextBoolean()) {
            bsm = new Handle(Opcodes.H_NEWINVOKESPECIAL,
                    NEW_INVOKE_SPECIAL_CLASS_NAME,
                    INIT_METHOD_NAME,
                    NEW_INVOKE_SPECIAL_BOOTSTRAP_METHOD_SIGNATURE);
        } else {
            bsm = new Handle(Opcodes.H_INVOKESTATIC,
                    this.fullClassName,
                    BOOTSTRAP_METHOD_NAME,
                    BOOTSTRAP_METHOD_SIGNATURE);
        }
        mw.visitInvokeDynamicInsn(TARGET_METHOD_NAME,
                TARGET_METHOD_SIGNATURE,
                bsm);
    }
}
