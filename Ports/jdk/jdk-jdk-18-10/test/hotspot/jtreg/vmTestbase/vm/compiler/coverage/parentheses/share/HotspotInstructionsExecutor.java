/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.coverage.parentheses.share;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;

import static jdk.internal.org.objectweb.asm.Opcodes.*;


import java.lang.reflect.Method;
import java.util.List;

/**
 * This class convert instructions sequence to java class file, load it to same JVM and then execute.
 * This class uses hidden classes for class loading
 */
public class HotspotInstructionsExecutor implements InstructionsExecutor {

    private static final Object[] NO_CP_PATCHES = new Object[0];

    private int stackSize;

    public HotspotInstructionsExecutor(int stackSize) {
        this.stackSize = stackSize;
    }

    @Override
    public int execute(List<Instruction> instructions) throws ReflectiveOperationException {
        Class execClass = generateClass(instructions);
        Method execMethod = execClass.getMethod("exec");
        return (Integer) execMethod.invoke(null);
    }

    private Class generateClass(List<Instruction> instructions) throws ReflectiveOperationException {
        // Needs to be in the same package as the lookup class.
        String classNameForASM = "vm/compiler/coverage/parentheses/share/ExecClass";

        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_1, ACC_PUBLIC, classNameForASM, null, "java/lang/Object", null);

        // creates a MethodWriter for the 'main' method
        MethodVisitor mw = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "exec", "()I", null, null);

        for (Instruction instruction : instructions) {
            mw.visitInsn(instruction.opCode);
        }

        mw.visitInsn(IRETURN);
        mw.visitMaxs(stackSize, 2);
        mw.visitEnd();

        Lookup lookup = MethodHandles.lookup();
        Class<?> hc = lookup.defineHiddenClass(cw.toByteArray(), false).lookupClass();
        return hc;
    }
}
