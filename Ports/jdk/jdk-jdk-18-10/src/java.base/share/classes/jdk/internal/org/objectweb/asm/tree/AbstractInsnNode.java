/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package jdk.internal.org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jdk.internal.org.objectweb.asm.MethodVisitor;

/**
 * A node that represents a bytecode instruction. <i>An instruction can appear at most once in at
 * most one {@link InsnList} at a time</i>.
 *
 * @author Eric Bruneton
 */
public abstract class AbstractInsnNode {

    /** The type of {@link InsnNode} instructions. */
    public static final int INSN = 0;

    /** The type of {@link IntInsnNode} instructions. */
    public static final int INT_INSN = 1;

    /** The type of {@link VarInsnNode} instructions. */
    public static final int VAR_INSN = 2;

    /** The type of {@link TypeInsnNode} instructions. */
    public static final int TYPE_INSN = 3;

    /** The type of {@link FieldInsnNode} instructions. */
    public static final int FIELD_INSN = 4;

    /** The type of {@link MethodInsnNode} instructions. */
    public static final int METHOD_INSN = 5;

    /** The type of {@link InvokeDynamicInsnNode} instructions. */
    public static final int INVOKE_DYNAMIC_INSN = 6;

    /** The type of {@link JumpInsnNode} instructions. */
    public static final int JUMP_INSN = 7;

    /** The type of {@link LabelNode} "instructions". */
    public static final int LABEL = 8;

    /** The type of {@link LdcInsnNode} instructions. */
    public static final int LDC_INSN = 9;

    /** The type of {@link IincInsnNode} instructions. */
    public static final int IINC_INSN = 10;

    /** The type of {@link TableSwitchInsnNode} instructions. */
    public static final int TABLESWITCH_INSN = 11;

    /** The type of {@link LookupSwitchInsnNode} instructions. */
    public static final int LOOKUPSWITCH_INSN = 12;

    /** The type of {@link MultiANewArrayInsnNode} instructions. */
    public static final int MULTIANEWARRAY_INSN = 13;

    /** The type of {@link FrameNode} "instructions". */
    public static final int FRAME = 14;

    /** The type of {@link LineNumberNode} "instructions". */
    public static final int LINE = 15;

    /** The opcode of this instruction. */
    protected int opcode;

    /**
      * The runtime visible type annotations of this instruction. This field is only used for real
      * instructions (i.e. not for labels, frames, or line number nodes). This list is a list of {@link
      * TypeAnnotationNode} objects. May be {@literal null}.
      */
    public List<TypeAnnotationNode> visibleTypeAnnotations;

    /**
      * The runtime invisible type annotations of this instruction. This field is only used for real
      * instructions (i.e. not for labels, frames, or line number nodes). This list is a list of {@link
      * TypeAnnotationNode} objects. May be {@literal null}.
      */
    public List<TypeAnnotationNode> invisibleTypeAnnotations;

    /** The previous instruction in the list to which this instruction belongs. */
    AbstractInsnNode previousInsn;

    /** The next instruction in the list to which this instruction belongs. */
    AbstractInsnNode nextInsn;

    /**
      * The index of this instruction in the list to which it belongs. The value of this field is
      * correct only when {@link InsnList#cache} is not null. A value of -1 indicates that this
      * instruction does not belong to any {@link InsnList}.
      */
    int index;

    /**
      * Constructs a new {@link AbstractInsnNode}.
      *
      * @param opcode the opcode of the instruction to be constructed.
      */
    protected AbstractInsnNode(final int opcode) {
        this.opcode = opcode;
        this.index = -1;
    }

    /**
      * Returns the opcode of this instruction.
      *
      * @return the opcode of this instruction.
      */
    public int getOpcode() {
        return opcode;
    }

    /**
      * Returns the type of this instruction.
      *
      * @return the type of this instruction, i.e. one the constants defined in this class.
      */
    public abstract int getType();

    /**
      * Returns the previous instruction in the list to which this instruction belongs, if any.
      *
      * @return the previous instruction in the list to which this instruction belongs, if any. May be
      *     {@literal null}.
      */
    public AbstractInsnNode getPrevious() {
        return previousInsn;
    }

    /**
      * Returns the next instruction in the list to which this instruction belongs, if any.
      *
      * @return the next instruction in the list to which this instruction belongs, if any. May be
      *     {@literal null}.
      */
    public AbstractInsnNode getNext() {
        return nextInsn;
    }

    /**
      * Makes the given method visitor visit this instruction.
      *
      * @param methodVisitor a method visitor.
      */
    public abstract void accept(MethodVisitor methodVisitor);

    /**
      * Makes the given visitor visit the annotations of this instruction.
      *
      * @param methodVisitor a method visitor.
      */
    protected final void acceptAnnotations(final MethodVisitor methodVisitor) {
        if (visibleTypeAnnotations != null) {
            for (int i = 0, n = visibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode typeAnnotation = visibleTypeAnnotations.get(i);
                typeAnnotation.accept(
                        methodVisitor.visitInsnAnnotation(
                                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
            }
        }
        if (invisibleTypeAnnotations != null) {
            for (int i = 0, n = invisibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode typeAnnotation = invisibleTypeAnnotations.get(i);
                typeAnnotation.accept(
                        methodVisitor.visitInsnAnnotation(
                                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
            }
        }
    }

    /**
      * Returns a copy of this instruction.
      *
      * @param clonedLabels a map from LabelNodes to cloned LabelNodes.
      * @return a copy of this instruction. The returned instruction does not belong to any {@link
      *     InsnList}.
      */
    public abstract AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels);

    /**
      * Returns the clone of the given label.
      *
      * @param label a label.
      * @param clonedLabels a map from LabelNodes to cloned LabelNodes.
      * @return the clone of the given label.
      */
    static LabelNode clone(final LabelNode label, final Map<LabelNode, LabelNode> clonedLabels) {
        return clonedLabels.get(label);
    }

    /**
      * Returns the clones of the given labels.
      *
      * @param labels a list of labels.
      * @param clonedLabels a map from LabelNodes to cloned LabelNodes.
      * @return the clones of the given labels.
      */
    static LabelNode[] clone(
            final List<LabelNode> labels, final Map<LabelNode, LabelNode> clonedLabels) {
        LabelNode[] clones = new LabelNode[labels.size()];
        for (int i = 0, n = clones.length; i < n; ++i) {
            clones[i] = clonedLabels.get(labels.get(i));
        }
        return clones;
    }

    /**
      * Clones the annotations of the given instruction into this instruction.
      *
      * @param insnNode the source instruction.
      * @return this instruction.
      */
    protected final AbstractInsnNode cloneAnnotations(final AbstractInsnNode insnNode) {
        if (insnNode.visibleTypeAnnotations != null) {
            this.visibleTypeAnnotations = new ArrayList<>();
            for (int i = 0, n = insnNode.visibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode sourceAnnotation = insnNode.visibleTypeAnnotations.get(i);
                TypeAnnotationNode cloneAnnotation =
                        new TypeAnnotationNode(
                                sourceAnnotation.typeRef, sourceAnnotation.typePath, sourceAnnotation.desc);
                sourceAnnotation.accept(cloneAnnotation);
                this.visibleTypeAnnotations.add(cloneAnnotation);
            }
        }
        if (insnNode.invisibleTypeAnnotations != null) {
            this.invisibleTypeAnnotations = new ArrayList<>();
            for (int i = 0, n = insnNode.invisibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode sourceAnnotation = insnNode.invisibleTypeAnnotations.get(i);
                TypeAnnotationNode cloneAnnotation =
                        new TypeAnnotationNode(
                                sourceAnnotation.typeRef, sourceAnnotation.typePath, sourceAnnotation.desc);
                sourceAnnotation.accept(cloneAnnotation);
                this.invisibleTypeAnnotations.add(cloneAnnotation);
            }
        }
        return this;
    }
}
