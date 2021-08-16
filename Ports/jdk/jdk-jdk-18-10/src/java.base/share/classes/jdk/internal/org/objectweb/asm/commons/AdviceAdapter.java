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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdk.internal.org.objectweb.asm.ConstantDynamic;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;

/**
 * A {@link MethodVisitor} to insert before, after and around advices in methods and constructors.
 * For constructors, the code keeps track of the elements on the stack in order to detect when the
 * super class constructor is called (note that there can be multiple such calls in different
 * branches). {@code onMethodEnter} is called after each super class constructor call, because the
 * object cannot be used before it is properly initialized.
 *
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
public abstract class AdviceAdapter extends GeneratorAdapter implements Opcodes {

    /** The "uninitialized this" value. */
    private static final Object UNINITIALIZED_THIS = new Object();

    /** Any value other than "uninitialized this". */
    private static final Object OTHER = new Object();

    /** Prefix of the error message when invalid opcodes are found. */
    private static final String INVALID_OPCODE = "Invalid opcode ";

    /** The access flags of the visited method. */
    protected int methodAccess;

    /** The descriptor of the visited method. */
    protected String methodDesc;

    /** Whether the visited method is a constructor. */
    private final boolean isConstructor;

    /**
      * Whether the super class constructor has been called (if the visited method is a constructor),
      * at the current instruction. There can be multiple call sites to the super constructor (e.g. for
      * Java code such as {@code super(expr ? value1 : value2);}), in different branches. When scanning
      * the bytecode linearly, we can move from one branch where the super constructor has been called
      * to another where it has not been called yet. Therefore, this value can change from false to
      * true, and vice-versa.
      */
    private boolean superClassConstructorCalled;

    /**
      * The values on the current execution stack frame (long and double are represented by two
      * elements). Each value is either {@link #UNINITIALIZED_THIS} (for the uninitialized this value),
      * or {@link #OTHER} (for any other value). This field is only maintained for constructors, in
      * branches where the super class constructor has not been called yet.
      */
    private List<Object> stackFrame;

    /**
      * The stack map frames corresponding to the labels of the forward jumps made *before* the super
      * class constructor has been called (note that the Java Virtual Machine forbids backward jumps
      * before the super class constructor is called). Note that by definition (cf. the 'before'), when
      * we reach a label from this map, {@link #superClassConstructorCalled} must be reset to false.
      * This field is only maintained for constructors.
      */
    private Map<Label, List<Object>> forwardJumpStackFrames;

    /**
      * Constructs a new {@link AdviceAdapter}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6} or {@link Opcodes#ASM7}.
      * @param methodVisitor the method visitor to which this adapter delegates calls.
      * @param access the method's access flags (see {@link Opcodes}).
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type Type}).
      */
    protected AdviceAdapter(
            final int api,
            final MethodVisitor methodVisitor,
            final int access,
            final String name,
            final String descriptor) {
        super(api, methodVisitor, access, name, descriptor);
        methodAccess = access;
        methodDesc = descriptor;
        isConstructor = "<init>".equals(name);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (isConstructor) {
            stackFrame = new ArrayList<>();
            forwardJumpStackFrames = new HashMap<>();
        } else {
            onMethodEnter();
        }
    }

    @Override
    public void visitLabel(final Label label) {
        super.visitLabel(label);
        if (isConstructor && forwardJumpStackFrames != null) {
            List<Object> labelStackFrame = forwardJumpStackFrames.get(label);
            if (labelStackFrame != null) {
                stackFrame = labelStackFrame;
                superClassConstructorCalled = false;
                forwardJumpStackFrames.remove(label);
            }
        }
    }

    @Override
    public void visitInsn(final int opcode) {
        if (isConstructor && !superClassConstructorCalled) {
            int stackSize;
            switch (opcode) {
                case IRETURN:
                case FRETURN:
                case ARETURN:
                case LRETURN:
                case DRETURN:
                    throw new IllegalArgumentException("Invalid return in constructor");
                case RETURN: // empty stack
                    onMethodExit(opcode);
                    break;
                case ATHROW: // 1 before n/a after
                    popValue();
                    onMethodExit(opcode);
                    break;
                case NOP:
                case LALOAD: // remove 2 add 2
                case DALOAD: // remove 2 add 2
                case LNEG:
                case DNEG:
                case FNEG:
                case INEG:
                case L2D:
                case D2L:
                case F2I:
                case I2B:
                case I2C:
                case I2S:
                case I2F:
                case ARRAYLENGTH:
                    break;
                case ACONST_NULL:
                case ICONST_M1:
                case ICONST_0:
                case ICONST_1:
                case ICONST_2:
                case ICONST_3:
                case ICONST_4:
                case ICONST_5:
                case FCONST_0:
                case FCONST_1:
                case FCONST_2:
                case F2L: // 1 before 2 after
                case F2D:
                case I2L:
                case I2D:
                    pushValue(OTHER);
                    break;
                case LCONST_0:
                case LCONST_1:
                case DCONST_0:
                case DCONST_1:
                    pushValue(OTHER);
                    pushValue(OTHER);
                    break;
                case IALOAD: // remove 2 add 1
                case FALOAD: // remove 2 add 1
                case AALOAD: // remove 2 add 1
                case BALOAD: // remove 2 add 1
                case CALOAD: // remove 2 add 1
                case SALOAD: // remove 2 add 1
                case POP:
                case IADD:
                case FADD:
                case ISUB:
                case LSHL: // 3 before 2 after
                case LSHR: // 3 before 2 after
                case LUSHR: // 3 before 2 after
                case L2I: // 2 before 1 after
                case L2F: // 2 before 1 after
                case D2I: // 2 before 1 after
                case D2F: // 2 before 1 after
                case FSUB:
                case FMUL:
                case FDIV:
                case FREM:
                case FCMPL: // 2 before 1 after
                case FCMPG: // 2 before 1 after
                case IMUL:
                case IDIV:
                case IREM:
                case ISHL:
                case ISHR:
                case IUSHR:
                case IAND:
                case IOR:
                case IXOR:
                case MONITORENTER:
                case MONITOREXIT:
                    popValue();
                    break;
                case POP2:
                case LSUB:
                case LMUL:
                case LDIV:
                case LREM:
                case LADD:
                case LAND:
                case LOR:
                case LXOR:
                case DADD:
                case DMUL:
                case DSUB:
                case DDIV:
                case DREM:
                    popValue();
                    popValue();
                    break;
                case IASTORE:
                case FASTORE:
                case AASTORE:
                case BASTORE:
                case CASTORE:
                case SASTORE:
                case LCMP: // 4 before 1 after
                case DCMPL:
                case DCMPG:
                    popValue();
                    popValue();
                    popValue();
                    break;
                case LASTORE:
                case DASTORE:
                    popValue();
                    popValue();
                    popValue();
                    popValue();
                    break;
                case DUP:
                    pushValue(peekValue());
                    break;
                case DUP_X1:
                    stackSize = stackFrame.size();
                    stackFrame.add(stackSize - 2, stackFrame.get(stackSize - 1));
                    break;
                case DUP_X2:
                    stackSize = stackFrame.size();
                    stackFrame.add(stackSize - 3, stackFrame.get(stackSize - 1));
                    break;
                case DUP2:
                    stackSize = stackFrame.size();
                    stackFrame.add(stackSize - 2, stackFrame.get(stackSize - 1));
                    stackFrame.add(stackSize - 2, stackFrame.get(stackSize - 1));
                    break;
                case DUP2_X1:
                    stackSize = stackFrame.size();
                    stackFrame.add(stackSize - 3, stackFrame.get(stackSize - 1));
                    stackFrame.add(stackSize - 3, stackFrame.get(stackSize - 1));
                    break;
                case DUP2_X2:
                    stackSize = stackFrame.size();
                    stackFrame.add(stackSize - 4, stackFrame.get(stackSize - 1));
                    stackFrame.add(stackSize - 4, stackFrame.get(stackSize - 1));
                    break;
                case SWAP:
                    stackSize = stackFrame.size();
                    stackFrame.add(stackSize - 2, stackFrame.get(stackSize - 1));
                    stackFrame.remove(stackSize);
                    break;
                default:
                    throw new IllegalArgumentException(INVALID_OPCODE + opcode);
            }
        } else {
            switch (opcode) {
                case RETURN:
                case IRETURN:
                case FRETURN:
                case ARETURN:
                case LRETURN:
                case DRETURN:
                case ATHROW:
                    onMethodExit(opcode);
                    break;
                default:
                    break;
            }
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        super.visitVarInsn(opcode, var);
        if (isConstructor && !superClassConstructorCalled) {
            switch (opcode) {
                case ILOAD:
                case FLOAD:
                    pushValue(OTHER);
                    break;
                case LLOAD:
                case DLOAD:
                    pushValue(OTHER);
                    pushValue(OTHER);
                    break;
                case ALOAD:
                    pushValue(var == 0 ? UNINITIALIZED_THIS : OTHER);
                    break;
                case ASTORE:
                case ISTORE:
                case FSTORE:
                    popValue();
                    break;
                case LSTORE:
                case DSTORE:
                    popValue();
                    popValue();
                    break;
                case RET:
                    break;
                default:
                    throw new IllegalArgumentException(INVALID_OPCODE + opcode);
            }
        }
    }

    @Override
    public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        if (isConstructor && !superClassConstructorCalled) {
            char firstDescriptorChar = descriptor.charAt(0);
            boolean longOrDouble = firstDescriptorChar == 'J' || firstDescriptorChar == 'D';
            switch (opcode) {
                case GETSTATIC:
                    pushValue(OTHER);
                    if (longOrDouble) {
                        pushValue(OTHER);
                    }
                    break;
                case PUTSTATIC:
                    popValue();
                    if (longOrDouble) {
                        popValue();
                    }
                    break;
                case PUTFIELD:
                    popValue();
                    popValue();
                    if (longOrDouble) {
                        popValue();
                    }
                    break;
                case GETFIELD:
                    if (longOrDouble) {
                        pushValue(OTHER);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(INVALID_OPCODE + opcode);
            }
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        super.visitIntInsn(opcode, operand);
        if (isConstructor && !superClassConstructorCalled && opcode != NEWARRAY) {
            pushValue(OTHER);
        }
    }

    @Override
    public void visitLdcInsn(final Object value) {
        super.visitLdcInsn(value);
        if (isConstructor && !superClassConstructorCalled) {
            pushValue(OTHER);
            if (value instanceof Double
                    || value instanceof Long
                    || (value instanceof ConstantDynamic && ((ConstantDynamic) value).getSize() == 2)) {
                pushValue(OTHER);
            }
        }
    }

    @Override
    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        if (isConstructor && !superClassConstructorCalled) {
            for (int i = 0; i < numDimensions; i++) {
                popValue();
            }
            pushValue(OTHER);
        }
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        super.visitTypeInsn(opcode, type);
        // ANEWARRAY, CHECKCAST or INSTANCEOF don't change stack.
        if (isConstructor && !superClassConstructorCalled && opcode == NEW) {
            pushValue(OTHER);
        }
    }

    @Override
    public void visitMethodInsn(
            final int opcodeAndSource,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
        if (api < Opcodes.ASM5 && (opcodeAndSource & Opcodes.SOURCE_DEPRECATED) == 0) {
            // Redirect the call to the deprecated version of this method.
            super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
            return;
        }
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
        int opcode = opcodeAndSource & ~Opcodes.SOURCE_MASK;

        doVisitMethodInsn(opcode, descriptor);
    }

    private void doVisitMethodInsn(final int opcode, final String descriptor) {
        if (isConstructor && !superClassConstructorCalled) {
            for (Type argumentType : Type.getArgumentTypes(descriptor)) {
                popValue();
                if (argumentType.getSize() == 2) {
                    popValue();
                }
            }
            switch (opcode) {
                case INVOKEINTERFACE:
                case INVOKEVIRTUAL:
                    popValue();
                    break;
                case INVOKESPECIAL:
                    Object value = popValue();
                    if (value == UNINITIALIZED_THIS && !superClassConstructorCalled) {
                        superClassConstructorCalled = true;
                        onMethodEnter();
                    }
                    break;
                default:
                    break;
            }

            Type returnType = Type.getReturnType(descriptor);
            if (returnType != Type.VOID_TYPE) {
                pushValue(OTHER);
                if (returnType.getSize() == 2) {
                    pushValue(OTHER);
                }
            }
        }
    }

    @Override
    public void visitInvokeDynamicInsn(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        doVisitMethodInsn(Opcodes.INVOKEDYNAMIC, descriptor);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode, label);
        if (isConstructor && !superClassConstructorCalled) {
            switch (opcode) {
                case IFEQ:
                case IFNE:
                case IFLT:
                case IFGE:
                case IFGT:
                case IFLE:
                case IFNULL:
                case IFNONNULL:
                    popValue();
                    break;
                case IF_ICMPEQ:
                case IF_ICMPNE:
                case IF_ICMPLT:
                case IF_ICMPGE:
                case IF_ICMPGT:
                case IF_ICMPLE:
                case IF_ACMPEQ:
                case IF_ACMPNE:
                    popValue();
                    popValue();
                    break;
                case JSR:
                    pushValue(OTHER);
                    break;
                default:
                    break;
            }
            addForwardJump(label);
        }
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        if (isConstructor && !superClassConstructorCalled) {
            popValue();
            addForwardJumps(dflt, labels);
        }
    }

    @Override
    public void visitTableSwitchInsn(
            final int min, final int max, final Label dflt, final Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        if (isConstructor && !superClassConstructorCalled) {
            popValue();
            addForwardJumps(dflt, labels);
        }
    }

    @Override
    public void visitTryCatchBlock(
            final Label start, final Label end, final Label handler, final String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        // By definition of 'forwardJumpStackFrames', 'handler' should be pushed only if there is an
        // instruction between 'start' and 'end' at which the super class constructor is not yet
        // called. Unfortunately, try catch blocks must be visited before their labels, so we have no
        // way to know this at this point. Instead, we suppose that the super class constructor has not
        // been called at the start of *any* exception handler. If this is wrong, normally there should
        // not be a second super class constructor call in the exception handler (an object can't be
        // initialized twice), so this is not issue (in the sense that there is no risk to emit a wrong
        // 'onMethodEnter').
        if (isConstructor && !forwardJumpStackFrames.containsKey(handler)) {
            List<Object> handlerStackFrame = new ArrayList<>();
            handlerStackFrame.add(OTHER);
            forwardJumpStackFrames.put(handler, handlerStackFrame);
        }
    }

    private void addForwardJumps(final Label dflt, final Label[] labels) {
        addForwardJump(dflt);
        for (Label label : labels) {
            addForwardJump(label);
        }
    }

    private void addForwardJump(final Label label) {
        if (forwardJumpStackFrames.containsKey(label)) {
            return;
        }
        forwardJumpStackFrames.put(label, new ArrayList<>(stackFrame));
    }

    private Object popValue() {
        return stackFrame.remove(stackFrame.size() - 1);
    }

    private Object peekValue() {
        return stackFrame.get(stackFrame.size() - 1);
    }

    private void pushValue(final Object value) {
        stackFrame.add(value);
    }

    /**
      * Generates the "before" advice for the visited method. The default implementation of this method
      * does nothing. Subclasses can use or change all the local variables, but should not change state
      * of the stack. This method is called at the beginning of the method or after super class
      * constructor has been called (in constructors).
      */
    protected void onMethodEnter() {}

    /**
      * Generates the "after" advice for the visited method. The default implementation of this method
      * does nothing. Subclasses can use or change all the local variables, but should not change state
      * of the stack. This method is called at the end of the method, just before return and athrow
      * instructions. The top element on the stack contains the return value or the exception instance.
      * For example:
      *
      * <pre>
      * public void onMethodExit(final int opcode) {
      *   if (opcode == RETURN) {
      *     visitInsn(ACONST_NULL);
      *   } else if (opcode == ARETURN || opcode == ATHROW) {
      *     dup();
      *   } else {
      *     if (opcode == LRETURN || opcode == DRETURN) {
      *       dup2();
      *     } else {
      *       dup();
      *     }
      *     box(Type.getReturnType(this.methodDesc));
      *   }
      *   visitIntInsn(SIPUSH, opcode);
      *   visitMethodInsn(INVOKESTATIC, owner, "onExit", "(Ljava/lang/Object;I)V");
      * }
      *
      * // An actual call back method.
      * public static void onExit(final Object exitValue, final int opcode) {
      *   ...
      * }
      * </pre>
      *
      * @param opcode one of {@link Opcodes#RETURN}, {@link Opcodes#IRETURN}, {@link Opcodes#FRETURN},
      *     {@link Opcodes#ARETURN}, {@link Opcodes#LRETURN}, {@link Opcodes#DRETURN} or {@link
      *     Opcodes#ATHROW}.
      */
    protected void onMethodExit(final int opcode) {}
}
