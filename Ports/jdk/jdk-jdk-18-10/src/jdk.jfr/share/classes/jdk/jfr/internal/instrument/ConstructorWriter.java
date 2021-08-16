/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.internal.org.objectweb.asm.Opcodes.ACONST_NULL;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

final class ConstructorWriter extends MethodVisitor {

    private boolean useInputParameter;
    private String shortClassName;
    private String fullClassName;

    ConstructorWriter(Class<?> classToChange, boolean useInputParameter) {
        super(Opcodes.ASM7);
        this.useInputParameter = useInputParameter;
        shortClassName = classToChange.getSimpleName();
        fullClassName = classToChange.getName().replace('.', '/');
    }

    @Override
    public void visitInsn(int opcode)
    {
        if (opcode == RETURN) {
            if (useInputParameter) {
                useInput();
            } else {
                noInput();
            }
        }
        mv.visitInsn(opcode);
    }
    @SuppressWarnings("deprecation")
    private void useInput()
    {
        //Load 'this' from local variable 0
        //Load first input parameter
        //Invoke ThrowableTracer.traceCLASS(this, parameter) for current class
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, "jdk/jfr/internal/instrument/ThrowableTracer",
                "trace" + shortClassName, "(L" + fullClassName +
                ";Ljava/lang/String;)V");
    }

    @SuppressWarnings("deprecation")
    private void noInput()
    {
        //Load 'this' from local variable 0
        //Load ""
        //Invoke ThrowableTracer.traceCLASS(this, "") for current class
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitMethodInsn(INVOKESTATIC, "jdk/jfr/internal/instrument/ThrowableTracer",
                "trace" + shortClassName, "(L" + fullClassName +
                ";Ljava/lang/String;)V");
    }

    public void setMethodVisitor(MethodVisitor mv) {
        this.mv = mv;
    }
}
