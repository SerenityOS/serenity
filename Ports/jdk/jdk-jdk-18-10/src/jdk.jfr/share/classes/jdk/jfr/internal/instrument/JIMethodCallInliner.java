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

import java.util.ArrayList;
import java.util.List;

import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.commons.LocalVariablesSorter;
import jdk.internal.org.objectweb.asm.commons.Remapper;
import jdk.internal.org.objectweb.asm.commons.SimpleRemapper;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;

/**
 * Class responsible for finding the call to inline and inlining it.
 *
 * This code is heavily influenced by section 3.2.6 "Inline Method" in
 * "Using ASM framework to implement common bytecode transformation patterns",
 * E. Kuleshov, AOSD.07, March 2007, Vancouver, Canada.
 * http://asm.ow2.org/index.html
 */
@Deprecated
final class JIMethodCallInliner extends LocalVariablesSorter {

    private final String oldClass;
    private final String newClass;
    private final MethodNode inlineTarget;
    private final List<CatchBlock> blocks = new ArrayList<>();
    private boolean inlining;

    /**
     * inlineTarget defines the method to inline and also contains the actual
     * code to inline.
     *
     * @param access
     * @param desc
     * @param mv
     * @param inlineTarget
     * @param oldClass
     * @param newClass
     * @param logger
     */
    public JIMethodCallInliner(int access, String desc, MethodVisitor mv,
            MethodNode inlineTarget, String oldClass, String newClass) {
        super(Opcodes.ASM7, access, desc, mv);
        this.oldClass = oldClass;
        this.newClass = newClass;
        this.inlineTarget = inlineTarget;

        if (Logger.shouldLog(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.DEBUG)) {
            Logger.log(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.DEBUG,
                "MethodCallInliner: targetMethod=" + newClass + "."
                + inlineTarget.name + inlineTarget.desc);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
            String desc, boolean itf) {
        // Now we are looking at method call in the source method
        if (!shouldBeInlined(owner, name, desc)) {
            // If this method call should not be inlined, just keep it
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        // If the call should be inlined, we create a MethodInliningAdapter
        // The MIA will walk the instructions in the inlineTarget and add them
        // to the current method, doing the necessary name remappings.
        Logger.log(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.DEBUG, "Inlining call to " + name + desc);
        Remapper remapper = new SimpleRemapper(oldClass, newClass);
        Label end = new Label();
        inlining = true;
        inlineTarget.instructions.resetLabels();
        JIMethodInliningAdapter mia = new JIMethodInliningAdapter(this, end,
                opcode == Opcodes.INVOKESTATIC ? Opcodes.ACC_STATIC : 0, desc,
                remapper);
        inlineTarget.accept(mia);
        inlining = false;
        super.visitLabel(end);
    }

    /**
     * Determine if the method should be inlined or not.
     */
    private boolean shouldBeInlined(String owner, String name, String desc) {
        return inlineTarget.desc.equals(desc) && inlineTarget.name.equals(name)
                && owner.equals(newClass.replace('.', '/'));
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler,
            String type) {
        if (!inlining) {
            // try-catch blocks are saved here and replayed at the end
            // of the method (in visitMaxs)
            blocks.add(new CatchBlock(start, end, handler, type));
        } else {
            super.visitTryCatchBlock(start, end, handler, type);
        }
    }

    @Override
    public void visitMaxs(int stack, int locals) {
        for (CatchBlock b : blocks) {
            super.visitTryCatchBlock(b.start, b.end, b.handler, b.type);
        }
        super.visitMaxs(stack, locals);
    }

    static final class CatchBlock {

        final Label start;
        final Label end;
        final Label handler;
        final String type;

        CatchBlock(Label start, Label end, Label handler, String type) {
            this.start = start;
            this.end = end;
            this.handler = handler;
            this.type = type;
        }
    }
}
