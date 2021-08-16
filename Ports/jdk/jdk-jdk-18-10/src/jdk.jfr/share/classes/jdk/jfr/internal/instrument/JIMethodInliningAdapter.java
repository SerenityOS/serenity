/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jfr.internal.instrument;

import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.commons.LocalVariablesSorter;
import jdk.internal.org.objectweb.asm.commons.Remapper;
import jdk.internal.org.objectweb.asm.commons.RemappingMethodAdapter;

@Deprecated
final class JIMethodInliningAdapter extends RemappingMethodAdapter {
    private final LocalVariablesSorter lvs;
    private final Label end;

    public JIMethodInliningAdapter(LocalVariablesSorter mv, Label end, int acc, String desc, Remapper remapper) {
        super(acc, desc, mv, remapper);
        this.lvs = mv;
        this.end = end;
        int offset = isStatic(acc) ? 0 : 1;
        Type[] args = Type.getArgumentTypes(desc);
        for (int i = args.length - 1; i >= 0; i--) {
            super.visitVarInsn(args[i].getOpcode(Opcodes.ISTORE), i + offset);
        }
        if (offset > 0) {
            super.visitVarInsn(Opcodes.ASTORE, 0);
        }
    }

    private boolean isStatic(int acc) {
        return (acc & Opcodes.ACC_STATIC) != 0;
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN
                || opcode == Opcodes.ARETURN || opcode == Opcodes.LRETURN) {
            super.visitJumpInsn(Opcodes.GOTO, end);
        } else {
            super.visitInsn(opcode);
        }
    }

    @Override
    public void visitMaxs(int stack, int locals) {
    }

    @Override
    protected int newLocalMapping(Type type) {
        return lvs.newLocal(type);
    }
}
