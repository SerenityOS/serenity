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

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.ClassWriterExt;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Handle;

import vm.mlvm.share.ClassfileGenerator;
import vm.mlvm.share.Env;

public class GenCPFullOfMH extends GenFullCP {

    public static void main(String[] args) {
        ClassfileGenerator.main(args);
    }

    @Override
    protected void generateCommonData(ClassWriterExt cw) {
        cw.setCacheMHandles(false);

        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                STATIC_FIELD_NAME,
                STATIC_FIELD_SIGNATURE, null, false);

        cw.visitField(Opcodes.ACC_PUBLIC,
                INSTANCE_FIELD_NAME,
                INSTANCE_FIELD_SIGNATURE, null, false);

        createInitMethod(cw);
        createTargetMethod(cw);

        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                INSTANCE_TARGET_METHOD_NAME,
                INSTANCE_TARGET_METHOD_SIGNATURE,
                null,
                new String[0]);
        finishMethodCode(mv);
    }

    @Override
    protected void generateCPEntryData(ClassWriter cw, MethodVisitor mw) {
        HandleType[] types = HandleType.values();
        HandleType type = types[Env.getRNG().nextInt(types.length)];

        switch (type) {
            case PUTFIELD:
            case PUTSTATIC:
                mw.visitInsn(Opcodes.ICONST_0);
                break;
            case INVOKESPECIAL:
            case INVOKEVIRTUAL:
            case INVOKEINTERFACE:
                mw.visitInsn(Opcodes.ACONST_NULL);
                break;
        }

        Handle handle;
        switch (type) {
            case GETFIELD:
            case PUTFIELD:
                handle = new Handle(type.asmTag,
                        fullClassName,
                        INSTANCE_FIELD_NAME,
                        INSTANCE_FIELD_SIGNATURE);
                break;
            case GETSTATIC:
            case PUTSTATIC:
                handle = new Handle(type.asmTag,
                        fullClassName,
                        STATIC_FIELD_NAME,
                        STATIC_FIELD_SIGNATURE);
                break;
            case NEWINVOKESPECIAL:
                handle = new Handle(type.asmTag,
                        fullClassName,
                        INIT_METHOD_NAME,
                        INIT_METHOD_SIGNATURE);
                break;
            case INVOKESTATIC:
                handle = new Handle(type.asmTag,
                        fullClassName,
                        TARGET_METHOD_NAME,
                        TARGET_METHOD_SIGNATURE);
                break;
            case INVOKEINTERFACE:
                handle = new Handle(type.asmTag,
                        getDummyInterfaceClassName(),
                        INSTANCE_TARGET_METHOD_NAME,
                        INSTANCE_TARGET_METHOD_SIGNATURE);
                break;
            case INVOKESPECIAL:
            case INVOKEVIRTUAL:
                handle = new Handle(type.asmTag,
                        fullClassName,
                        INSTANCE_TARGET_METHOD_NAME,
                        INSTANCE_TARGET_METHOD_SIGNATURE);
                break;
            default:
                throw new Error("Unexpected handle type " + type);
        }
        mw.visitLdcInsn(handle);

        switch (type) {
            case GETFIELD:
            case GETSTATIC:
                mw.visitInsn(Opcodes.POP);
                break;
        }
    }
}
