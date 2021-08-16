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
package jdk.internal.org.objectweb.asm.commons;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.JumpInsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.LocalVariableNode;
import jdk.internal.org.objectweb.asm.tree.LookupSwitchInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.TableSwitchInsnNode;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * A {@link jdk.internal.org.objectweb.asm.MethodVisitor} that removes JSR instructions and inlines the
 * referenced subroutines.
 *
 * @author Niko Matsakis
 */
// DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
public class JSRInlinerAdapter extends MethodNode implements Opcodes {

    /**
      * The instructions that belong to the main "subroutine". Bit i is set iff instruction at index i
      * belongs to this main "subroutine".
      */
    private final BitSet mainSubroutineInsns = new BitSet();

    /**
      * The instructions that belong to each subroutine. For each label which is the target of a JSR
      * instruction, bit i of the corresponding BitSet in this map is set iff instruction at index i
      * belongs to this subroutine.
      */
    private final Map<LabelNode, BitSet> subroutinesInsns = new HashMap<>();

    /**
      * The instructions that belong to more that one subroutine. Bit i is set iff instruction at index
      * i belongs to more than one subroutine.
      */
    final BitSet sharedSubroutineInsns = new BitSet();

    /**
      * Constructs a new {@link JSRInlinerAdapter}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #JSRInlinerAdapter(int, MethodVisitor, int, String, String,
      * String, String[])} version.
      *
      * @param methodVisitor the method visitor to send the resulting inlined method code to, or <code>
      *     null</code>.
      * @param access the method's access flags.
      * @param name the method's name.
      * @param descriptor the method's descriptor.
      * @param signature the method's signature. May be {@literal null}.
      * @param exceptions the internal names of the method's exception classes. May be {@literal null}.
      * @throws IllegalStateException if a subclass calls this constructor.
      */
    public JSRInlinerAdapter(
            final MethodVisitor methodVisitor,
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        this(
                /* latest api = */ Opcodes.ASM8,
                methodVisitor,
                access,
                name,
                descriptor,
                signature,
                exceptions);
        if (getClass() != JSRInlinerAdapter.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link JSRInlinerAdapter}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      * @param methodVisitor the method visitor to send the resulting inlined method code to, or <code>
      *     null</code>.
      * @param access the method's access flags (see {@link Opcodes}). This parameter also indicates if
      *     the method is synthetic and/or deprecated.
      * @param name the method's name.
      * @param descriptor the method's descriptor.
      * @param signature the method's signature. May be {@literal null}.
      * @param exceptions the internal names of the method's exception classes. May be {@literal null}.
      */
    protected JSRInlinerAdapter(
            final int api,
            final MethodVisitor methodVisitor,
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        super(api, access, name, descriptor, signature, exceptions);
        this.mv = methodVisitor;
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode, label);
        LabelNode labelNode = ((JumpInsnNode) instructions.getLast()).label;
        if (opcode == JSR && !subroutinesInsns.containsKey(labelNode)) {
            subroutinesInsns.put(labelNode, new BitSet());
        }
    }

    @Override
    public void visitEnd() {
        if (!subroutinesInsns.isEmpty()) {
            // If the code contains at least one JSR instruction, inline the subroutines.
            findSubroutinesInsns();
            emitCode();
        }
        if (mv != null) {
            accept(mv);
        }
    }

    /** Determines, for each instruction, to which subroutine(s) it belongs. */
    private void findSubroutinesInsns() {
        // Find the instructions that belong to main subroutine.
        BitSet visitedInsns = new BitSet();
        findSubroutineInsns(0, mainSubroutineInsns, visitedInsns);
        // For each subroutine, find the instructions that belong to this subroutine.
        for (Map.Entry<LabelNode, BitSet> entry : subroutinesInsns.entrySet()) {
            LabelNode jsrLabelNode = entry.getKey();
            BitSet subroutineInsns = entry.getValue();
            findSubroutineInsns(instructions.indexOf(jsrLabelNode), subroutineInsns, visitedInsns);
        }
    }

    /**
      * Finds the instructions that belong to the subroutine starting at the given instruction index.
      * For this the control flow graph is visited with a depth first search (this includes the normal
      * control flow and the exception handlers).
      *
      * @param startInsnIndex the index of the first instruction of the subroutine.
      * @param subroutineInsns where the indices of the instructions of the subroutine must be stored.
      * @param visitedInsns the indices of the instructions that have been visited so far (including in
      *     previous calls to this method). This bitset is updated by this method each time a new
      *     instruction is visited. It is used to make sure each instruction is visited at most once.
      */
    private void findSubroutineInsns(
            final int startInsnIndex, final BitSet subroutineInsns, final BitSet visitedInsns) {
        // First find the instructions reachable via normal execution.
        findReachableInsns(startInsnIndex, subroutineInsns, visitedInsns);

        // Then find the instructions reachable via the applicable exception handlers.
        while (true) {
            boolean applicableHandlerFound = false;
            for (TryCatchBlockNode tryCatchBlockNode : tryCatchBlocks) {
                // If the handler has already been processed, skip it.
                int handlerIndex = instructions.indexOf(tryCatchBlockNode.handler);
                if (subroutineInsns.get(handlerIndex)) {
                    continue;
                }

                // If an instruction in the exception handler range belongs to the subroutine, the handler
                // can be reached from the routine, and its instructions must be added to the subroutine.
                int startIndex = instructions.indexOf(tryCatchBlockNode.start);
                int endIndex = instructions.indexOf(tryCatchBlockNode.end);
                int firstSubroutineInsnAfterTryCatchStart = subroutineInsns.nextSetBit(startIndex);
                if (firstSubroutineInsnAfterTryCatchStart >= startIndex
                        && firstSubroutineInsnAfterTryCatchStart < endIndex) {
                    findReachableInsns(handlerIndex, subroutineInsns, visitedInsns);
                    applicableHandlerFound = true;
                }
            }
            // If an applicable exception handler has been found, other handlers may become applicable, so
            // we must examine them again.
            if (!applicableHandlerFound) {
                return;
            }
        }
    }

    /**
      * Finds the instructions that are reachable from the given instruction, without following any JSR
      * instruction nor any exception handler. For this the control flow graph is visited with a depth
      * first search.
      *
      * @param insnIndex the index of an instruction of the subroutine.
      * @param subroutineInsns where the indices of the instructions of the subroutine must be stored.
      * @param visitedInsns the indices of the instructions that have been visited so far (including in
      *     previous calls to this method). This bitset is updated by this method each time a new
      *     instruction is visited. It is used to make sure each instruction is visited at most once.
      */
    private void findReachableInsns(
            final int insnIndex, final BitSet subroutineInsns, final BitSet visitedInsns) {
        int currentInsnIndex = insnIndex;
        // We implicitly assume below that execution can always fall through to the next instruction
        // after a JSR. But a subroutine may never return, in which case the code after the JSR is
        // unreachable and can be anything. In particular, it can seem to fall off the end of the
        // method, so we must handle this case here (we could instead detect whether execution can
        // return or not from a JSR, but this is more complicated).
        while (currentInsnIndex < instructions.size()) {
            // Visit each instruction at most once.
            if (subroutineInsns.get(currentInsnIndex)) {
                return;
            }
            subroutineInsns.set(currentInsnIndex);

            // Check if this instruction has already been visited by another subroutine.
            if (visitedInsns.get(currentInsnIndex)) {
                sharedSubroutineInsns.set(currentInsnIndex);
            }
            visitedInsns.set(currentInsnIndex);

            AbstractInsnNode currentInsnNode = instructions.get(currentInsnIndex);
            if (currentInsnNode.getType() == AbstractInsnNode.JUMP_INSN
                    && currentInsnNode.getOpcode() != JSR) {
                // Don't follow JSR instructions in the control flow graph.
                JumpInsnNode jumpInsnNode = (JumpInsnNode) currentInsnNode;
                findReachableInsns(instructions.indexOf(jumpInsnNode.label), subroutineInsns, visitedInsns);
            } else if (currentInsnNode.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
                TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) currentInsnNode;
                findReachableInsns(
                        instructions.indexOf(tableSwitchInsnNode.dflt), subroutineInsns, visitedInsns);
                for (LabelNode labelNode : tableSwitchInsnNode.labels) {
                    findReachableInsns(instructions.indexOf(labelNode), subroutineInsns, visitedInsns);
                }
            } else if (currentInsnNode.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
                LookupSwitchInsnNode lookupSwitchInsnNode = (LookupSwitchInsnNode) currentInsnNode;
                findReachableInsns(
                        instructions.indexOf(lookupSwitchInsnNode.dflt), subroutineInsns, visitedInsns);
                for (LabelNode labelNode : lookupSwitchInsnNode.labels) {
                    findReachableInsns(instructions.indexOf(labelNode), subroutineInsns, visitedInsns);
                }
            }

            // Check if this instruction falls through to the next instruction; if not, return.
            switch (instructions.get(currentInsnIndex).getOpcode()) {
                case GOTO:
                case RET:
                case TABLESWITCH:
                case LOOKUPSWITCH:
                case IRETURN:
                case LRETURN:
                case FRETURN:
                case DRETURN:
                case ARETURN:
                case RETURN:
                case ATHROW:
                    // Note: this either returns from this subroutine, or from a parent subroutine.
                    return;
                default:
                    // Go to the next instruction.
                    currentInsnIndex++;
                    break;
            }
        }
    }

    /**
      * Creates the new instructions, inlining each instantiation of each subroutine until the code is
      * fully elaborated.
      */
    private void emitCode() {
        LinkedList<Instantiation> worklist = new LinkedList<>();
        // Create an instantiation of the main "subroutine", which is just the main routine.
        worklist.add(new Instantiation(null, mainSubroutineInsns));

        // Emit instantiations of each subroutine we encounter, including the main subroutine.
        InsnList newInstructions = new InsnList();
        List<TryCatchBlockNode> newTryCatchBlocks = new ArrayList<>();
        List<LocalVariableNode> newLocalVariables = new ArrayList<>();
        while (!worklist.isEmpty()) {
            Instantiation instantiation = worklist.removeFirst();
            emitInstantiation(
                    instantiation, worklist, newInstructions, newTryCatchBlocks, newLocalVariables);
        }
        instructions = newInstructions;
        tryCatchBlocks = newTryCatchBlocks;
        localVariables = newLocalVariables;
    }

    /**
      * Emits an instantiation of a subroutine, specified by <code>instantiation</code>. May add new
      * instantiations that are invoked by this one to the <code>worklist</code>, and new try/catch
      * blocks to <code>newTryCatchBlocks</code>.
      *
      * @param instantiation the instantiation that must be performed.
      * @param worklist list of the instantiations that remain to be done.
      * @param newInstructions the instruction list to which the instantiated code must be appended.
      * @param newTryCatchBlocks the exception handler list to which the instantiated handlers must be
      *     appended.
      * @param newLocalVariables the local variables list to which the instantiated local variables
      *     must be appended.
      */
    private void emitInstantiation(
            final Instantiation instantiation,
            final List<Instantiation> worklist,
            final InsnList newInstructions,
            final List<TryCatchBlockNode> newTryCatchBlocks,
            final List<LocalVariableNode> newLocalVariables) {
        LabelNode previousLabelNode = null;
        for (int i = 0; i < instructions.size(); ++i) {
            AbstractInsnNode insnNode = instructions.get(i);
            if (insnNode.getType() == AbstractInsnNode.LABEL) {
                // Always clone all labels, while avoiding to add the same label more than once.
                LabelNode labelNode = (LabelNode) insnNode;
                LabelNode clonedLabelNode = instantiation.getClonedLabel(labelNode);
                if (clonedLabelNode != previousLabelNode) {
                    newInstructions.add(clonedLabelNode);
                    previousLabelNode = clonedLabelNode;
                }
            } else if (instantiation.findOwner(i) == instantiation) {
                // Don't emit instructions that were already emitted by an ancestor subroutine. Note that it
                // is still possible for a given instruction to be emitted twice because it may belong to
                // two subroutines that do not invoke each other.

                if (insnNode.getOpcode() == RET) {
                    // Translate RET instruction(s) to a jump to the return label for the appropriate
                    // instantiation. The problem is that the subroutine may "fall through" to the ret of a
                    // parent subroutine; therefore, to find the appropriate ret label we find the oldest
                    // instantiation that claims to own this instruction.
                    LabelNode retLabel = null;
                    for (Instantiation retLabelOwner = instantiation;
                            retLabelOwner != null;
                            retLabelOwner = retLabelOwner.parent) {
                        if (retLabelOwner.subroutineInsns.get(i)) {
                            retLabel = retLabelOwner.returnLabel;
                        }
                    }
                    if (retLabel == null) {
                        // This is only possible if the mainSubroutine owns a RET instruction, which should
                        // never happen for verifiable code.
                        throw new IllegalArgumentException(
                                "Instruction #" + i + " is a RET not owned by any subroutine");
                    }
                    newInstructions.add(new JumpInsnNode(GOTO, retLabel));
                } else if (insnNode.getOpcode() == JSR) {
                    LabelNode jsrLabelNode = ((JumpInsnNode) insnNode).label;
                    BitSet subroutineInsns = subroutinesInsns.get(jsrLabelNode);
                    Instantiation newInstantiation = new Instantiation(instantiation, subroutineInsns);
                    LabelNode clonedJsrLabelNode = newInstantiation.getClonedLabelForJumpInsn(jsrLabelNode);
                    // Replace the JSR instruction with a GOTO to the instantiated subroutine, and push NULL
                    // for what was once the return address value. This hack allows us to avoid doing any sort
                    // of data flow analysis to figure out which instructions manipulate the old return
                    // address value pointer which is now known to be unneeded.
                    newInstructions.add(new InsnNode(ACONST_NULL));
                    newInstructions.add(new JumpInsnNode(GOTO, clonedJsrLabelNode));
                    newInstructions.add(newInstantiation.returnLabel);
                    // Insert this new instantiation into the queue to be emitted later.
                    worklist.add(newInstantiation);
                } else {
                    newInstructions.add(insnNode.clone(instantiation));
                }
            }
        }

        // Emit the try/catch blocks that are relevant for this instantiation.
        for (TryCatchBlockNode tryCatchBlockNode : tryCatchBlocks) {
            final LabelNode start = instantiation.getClonedLabel(tryCatchBlockNode.start);
            final LabelNode end = instantiation.getClonedLabel(tryCatchBlockNode.end);
            if (start != end) {
                final LabelNode handler =
                        instantiation.getClonedLabelForJumpInsn(tryCatchBlockNode.handler);
                if (start == null || end == null || handler == null) {
                    throw new AssertionError("Internal error!");
                }
                newTryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, tryCatchBlockNode.type));
            }
        }

        // Emit the local variable nodes that are relevant for this instantiation.
        for (LocalVariableNode localVariableNode : localVariables) {
            final LabelNode start = instantiation.getClonedLabel(localVariableNode.start);
            final LabelNode end = instantiation.getClonedLabel(localVariableNode.end);
            if (start != end) {
                newLocalVariables.add(
                        new LocalVariableNode(
                                localVariableNode.name,
                                localVariableNode.desc,
                                localVariableNode.signature,
                                start,
                                end,
                                localVariableNode.index));
            }
        }
    }

    /** An instantiation of a subroutine. */
    private class Instantiation extends AbstractMap<LabelNode, LabelNode> {

        /**
          * The instantiation from which this one was created (or {@literal null} for the instantiation
          * of the main "subroutine").
          */
        final Instantiation parent;

        /**
          * The original instructions that belong to the subroutine which is instantiated. Bit i is set
          * iff instruction at index i belongs to this subroutine.
          */
        final BitSet subroutineInsns;

        /**
          * A map from labels from the original code to labels pointing at code specific to this
          * instantiation, for use in remapping try/catch blocks, as well as jumps.
          *
          * <p>Note that in the presence of instructions belonging to several subroutines, we map the
          * target label of a GOTO to the label used by the oldest instantiation (parent instantiations
          * are older than their children). This avoids code duplication during inlining in most cases.
          */
        final Map<LabelNode, LabelNode> clonedLabels;

        /** The return label for this instantiation, to which all original returns will be mapped. */
        final LabelNode returnLabel;

        Instantiation(final Instantiation parent, final BitSet subroutineInsns) {
            for (Instantiation instantiation = parent;
                    instantiation != null;
                    instantiation = instantiation.parent) {
                if (instantiation.subroutineInsns == subroutineInsns) {
                    throw new IllegalArgumentException("Recursive invocation of " + subroutineInsns);
                }
            }

            this.parent = parent;
            this.subroutineInsns = subroutineInsns;
            this.returnLabel = parent == null ? null : new LabelNode();
            this.clonedLabels = new HashMap<>();

            // Create a clone of each label in the original code of the subroutine. Note that we collapse
            // labels which point at the same instruction into one.
            LabelNode clonedLabelNode = null;
            for (int insnIndex = 0; insnIndex < instructions.size(); insnIndex++) {
                AbstractInsnNode insnNode = instructions.get(insnIndex);
                if (insnNode.getType() == AbstractInsnNode.LABEL) {
                    LabelNode labelNode = (LabelNode) insnNode;
                    // If we already have a label pointing at this spot, don't recreate it.
                    if (clonedLabelNode == null) {
                        clonedLabelNode = new LabelNode();
                    }
                    clonedLabels.put(labelNode, clonedLabelNode);
                } else if (findOwner(insnIndex) == this) {
                    // We will emit this instruction, so clear the duplicateLabelNode flag since the next
                    // Label will refer to a distinct instruction.
                    clonedLabelNode = null;
                }
            }
        }

        /**
          * Returns the "owner" of a particular instruction relative to this instantiation: the owner
          * refers to the Instantiation which will emit the version of this instruction that we will
          * execute.
          *
          * <p>Typically, the return value is either <code>this</code> or <code>null</code>. <code>this
          * </code> indicates that this instantiation will generate the version of this instruction that
          * we will execute, and <code>null</code> indicates that this instantiation never executes the
          * given instruction.
          *
          * <p>Sometimes, however, an instruction can belong to multiple subroutines; this is called a
          * shared instruction, and occurs when multiple subroutines branch to common points of control.
          * In this case, the owner is the oldest instantiation which owns the instruction in question
          * (parent instantiations are older than their children).
          *
          * @param insnIndex the index of an instruction in the original code.
          * @return the "owner" of a particular instruction relative to this instantiation.
          */
        Instantiation findOwner(final int insnIndex) {
            if (!subroutineInsns.get(insnIndex)) {
                return null;
            }
            if (!sharedSubroutineInsns.get(insnIndex)) {
                return this;
            }
            Instantiation owner = this;
            for (Instantiation instantiation = parent;
                    instantiation != null;
                    instantiation = instantiation.parent) {
                if (instantiation.subroutineInsns.get(insnIndex)) {
                    owner = instantiation;
                }
            }
            return owner;
        }

        /**
          * Returns the clone of the given original label that is appropriate for use in a jump
          * instruction.
          *
          * @param labelNode a label of the original code.
          * @return a clone of the given label for use in a jump instruction in the inlined code.
          */
        LabelNode getClonedLabelForJumpInsn(final LabelNode labelNode) {
            // findOwner should never return null, because owner is null only if an instruction cannot be
            // reached from this subroutine.
            return findOwner(instructions.indexOf(labelNode)).clonedLabels.get(labelNode);
        }

        /**
          * Returns the clone of the given original label that is appropriate for use by a try/catch
          * block or a variable annotation.
          *
          * @param labelNode a label of the original code.
          * @return a clone of the given label for use by a try/catch block or a variable annotation in
          *     the inlined code.
          */
        LabelNode getClonedLabel(final LabelNode labelNode) {
            return clonedLabels.get(labelNode);
        }

        // AbstractMap implementation

        @Override
        public Set<Map.Entry<LabelNode, LabelNode>> entrySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public LabelNode get(final Object key) {
            return getClonedLabelForJumpInsn((LabelNode) key);
        }

        @Override
        public boolean equals(final Object other) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }
    }
}
