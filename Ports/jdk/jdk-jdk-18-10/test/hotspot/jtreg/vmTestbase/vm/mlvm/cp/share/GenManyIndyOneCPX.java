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
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Handle;

import vm.mlvm.share.ClassfileGenerator;

public class GenManyIndyOneCPX extends GenFullCP {

    public static void main(String[] args) {
        ClassfileGenerator.main(args);
    }

    @Override
    protected void generateCPEntryData(ClassWriter cw, MethodVisitor mw) {
        Handle bsm = new Handle(Opcodes.H_INVOKESTATIC,
                fullClassName,
                BOOTSTRAP_METHOD_NAME,
                BOOTSTRAP_METHOD_SIGNATURE);

        mw.visitInvokeDynamicInsn(TARGET_METHOD_NAME,
                TARGET_METHOD_SIGNATURE,
                bsm);
    }

}
