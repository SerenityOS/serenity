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
 * A {@link MethodVisitor} that keeps track of stack map frame changes between {@link
 * #visitFrame(int, int, Object[], int, Object[])} calls. This adapter must be used with the {@link
 * jdk.internal.org.objectweb.asm.ClassReader#EXPAND_FRAMES} option. Each visit<i>X</i> instruction delegates to
 * the next visitor in the chain, if any, and then simulates the effect of this instruction on the
 * stack map frame, represented by {@link #locals} and {@link #stack}. The next visitor in the chain
 * can get the state of the stack map frame <i>before</i> each instruction by reading the value of
 * these fields in its visit<i>X</i> methods (this requires a reference to the AnalyzerAdapter that
 * is before it in the chain). If this adapter is used with a class that does not contain stack map
 * table attributes (i.e., pre Java 6 classes) then this adapter may not be able to compute the
 * stack map frame for each instruction. In this case no exception is thrown but the {@link #locals}
 * and {@link #stack} fields will be null for these instructions.
 *
 * @author Eric Bruneton
 */
public class AnalyzerAdapter extends MethodVisitor {

    /**
      * The local variable slots for the current execution frame. Primitive types are represented by
      * {@link Opcodes#TOP}, {@link Opcodes#INTEGER}, {@link Opcodes#FLOAT}, {@link Opcodes#LONG},
      * {@link Opcodes#DOUBLE},{@link Opcodes#NULL} or {@link Opcodes#UNINITIALIZED_THIS} (long and
      * double are represented by two elements, the second one being TOP). Reference types are
      * represented by String objects (representing internal names), and uninitialized types by Label
      * objects (this label designates the NEW instruction that created this uninitialized value). This
      * field is {@literal null} for unreachable instructions.
      */
    public List<Object> locals;

    /**
      * The operand stack slots for the current execution frame. Primitive types are represented by
      * {@link Opcodes#TOP}, {@link Opcodes#INTEGER}, {@link Opcodes#FLOAT}, {@link Opcodes#LONG},
      * {@link Opcodes#DOUBLE},{@link Opcodes#NULL} or {@link Opcodes#UNINITIALIZED_THIS} (long and
      * double are represented by two elements, the second one being TOP). Reference types are
      * represented by String objects (representing internal names), and uninitialized types by Label
      * objects (this label designates the NEW instruction that created this uninitialized value). This
      * field is {@literal null} for unreachable instructions.
      */
    public List<Object> stack;

    /** The labels that designate the next instruction to be visited. May be {@literal null}. */
    private List<Label> labels;

    /**
      * The uninitialized types in the current execution frame. This map associates internal names to
      * Label objects. Each label designates a NEW instruction that created the currently uninitialized
      * types, and the associated internal name represents the NEW operand, i.e. the final, initialized
      * type value.
      */
    public Map<Object, Object> uninitializedTypes;

    /** The maximum stack size of this method. */
    private int maxStack;

    /** The maximum number of local variables of this method. */
    private int maxLocals;

    /** The owner's class name. */
    private String owner;

    /**
      * Constructs a new {@link AnalyzerAdapter}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #AnalyzerAdapter(int, String, int, String, String,
      * MethodVisitor)} version.
      *
      * @param owner the owner's class name.
      * @param access the method's access flags (see {@link Opcodes}).
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param methodVisitor the method visitor to which this adapter delegates calls. May be {@literal
      *     null}.
      * @throws IllegalStateException If a subclass calls this constructor.
      */
    public AnalyzerAdapter(
            final String owner,
            final int access,
            final String name,
            final String descriptor,
            final MethodVisitor methodVisitor) {
        this(/* latest api = */ Opcodes.ASM8, owner, access, name, descriptor, methodVisitor);
        if (getClass() != AnalyzerAdapter.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link AnalyzerAdapter}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      * @param owner the owner's class name.
      * @param access the method's access flags (see {@link Opcodes}).
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param methodVisitor the method visitor to which this adapter delegates calls. May be {@literal
      *     null}.
      */
    protected AnalyzerAdapter(
            final int api,
            final String owner,
            final int access,
            final String name,
            final String descriptor,
            final MethodVisitor methodVisitor) {
        super(api, methodVisitor);
        this.owner = owner;
        locals = new ArrayList<>();
        stack = new ArrayList<>();
        uninitializedTypes = new HashMap<>();

        if ((access & Opcodes.ACC_STATIC) == 0) {
            if ("<init>".equals(name)) {
                locals.add(Opcodes.UNINITIALIZED_THIS);
            } else {
                locals.add(owner);
            }
        }
        for (Type argumentType : Type.getArgumentTypes(descriptor)) {
            switch (argumentType.getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    locals.add(Opcodes.INTEGER);
                    break;
                case Type.FLOAT:
                    locals.add(Opcodes.FLOAT);
                    break;
                case Type.LONG:
                    locals.add(Opcodes.LONG);
                    locals.add(Opcodes.TOP);
                    break;
                case Type.DOUBLE:
                    locals.add(Opcodes.DOUBLE);
                    locals.add(Opcodes.TOP);
                    break;
                case Type.ARRAY:
                    locals.add(argumentType.getDescriptor());
                    break;
                case Type.OBJECT:
                    locals.add(argumentType.getInternalName());
                    break;
                default:
                    throw new AssertionError();
            }
        }
        maxLocals = locals.size();
    }

    @Override
    public void visitFrame(
            final int type,
            final int numLocal,
            final Object[] local,
            final int numStack,
            final Object[] stack) {
        if (type != Opcodes.F_NEW) { // Uncompressed frame.
            throw new IllegalArgumentException(
                    "AnalyzerAdapter only accepts expanded frames (see ClassReader.EXPAND_FRAMES)");
        }

        super.visitFrame(type, numLocal, local, numStack, stack);

        if (this.locals != null) {
            this.locals.clear();
            this.stack.clear();
        } else {
            this.locals = new ArrayList<>();
            this.stack = new ArrayList<>();
        }
        visitFrameTypes(numLocal, local, this.locals);
        visitFrameTypes(numStack, stack, this.stack);
        maxLocals = Math.max(maxLocals, this.locals.size());
        maxStack = Math.max(maxStack, this.stack.size());
    }

    private static void visitFrameTypes(
            final int numTypes, final Object[] frameTypes, final List<Object> result) {
        for (int i = 0; i < numTypes; ++i) {
            Object frameType = frameTypes[i];
            result.add(frameType);
            if (frameType == Opcodes.LONG || frameType == Opcodes.DOUBLE) {
                result.add(Opcodes.TOP);
            }
        }
    }

    @Override
    public void visitInsn(final int opcode) {
        super.visitInsn(opcode);
        execute(opcode, 0, null);
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
            this.locals = null;
            this.stack = null;
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        super.visitIntInsn(opcode, operand);
        execute(opcode, operand, null);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        super.visitVarInsn(opcode, var);
        boolean isLongOrDouble =
                opcode == Opcodes.LLOAD
                        || opcode == Opcodes.DLOAD
                        || opcode == Opcodes.LSTORE
                        || opcode == Opcodes.DSTORE;
        maxLocals = Math.max(maxLocals, var + (isLongOrDouble ? 2 : 1));
        execute(opcode, var, null);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        if (opcode == Opcodes.NEW) {
            if (labels == null) {
                Label label = new Label();
                labels = new ArrayList<>(3);
                labels.add(label);
                if (mv != null) {
                    mv.visitLabel(label);
                }
            }
            for (Label label : labels) {
                uninitializedTypes.put(label, type);
            }
        }
        super.visitTypeInsn(opcode, type);
        execute(opcode, 0, type);
    }

    @Override
    public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        execute(opcode, 0, descriptor);
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

        if (this.locals == null) {
            labels = null;
            return;
        }
        pop(descriptor);
        if (opcode != Opcodes.INVOKESTATIC) {
            Object value = pop();
            if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
                Object initializedValue;
                if (value == Opcodes.UNINITIALIZED_THIS) {
                    initializedValue = this.owner;
                } else {
                    initializedValue = uninitializedTypes.get(value);
                }
                for (int i = 0; i < locals.size(); ++i) {
                    if (locals.get(i) == value) {
                        locals.set(i, initializedValue);
                    }
                }
                for (int i = 0; i < stack.size(); ++i) {
                    if (stack.get(i) == value) {
                        stack.set(i, initializedValue);
                    }
                }
            }
        }
        pushDescriptor(descriptor);
        labels = null;
    }

    @Override
    public void visitInvokeDynamicInsn(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        if (this.locals == null) {
            labels = null;
            return;
        }
        pop(descriptor);
        pushDescriptor(descriptor);
        labels = null;
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode, label);
        execute(opcode, 0, null);
        if (opcode == Opcodes.GOTO) {
            this.locals = null;
            this.stack = null;
        }
    }

    @Override
    public void visitLabel(final Label label) {
        super.visitLabel(label);
        if (labels == null) {
            labels = new ArrayList<>(3);
        }
        labels.add(label);
    }

    @Override
    public void visitLdcInsn(final Object value) {
        super.visitLdcInsn(value);
        if (this.locals == null) {
            labels = null;
            return;
        }
        if (value instanceof Integer) {
            push(Opcodes.INTEGER);
        } else if (value instanceof Long) {
            push(Opcodes.LONG);
            push(Opcodes.TOP);
        } else if (value instanceof Float) {
            push(Opcodes.FLOAT);
        } else if (value instanceof Double) {
            push(Opcodes.DOUBLE);
            push(Opcodes.TOP);
        } else if (value instanceof String) {
            push("java/lang/String");
        } else if (value instanceof Type) {
            int sort = ((Type) value).getSort();
            if (sort == Type.OBJECT || sort == Type.ARRAY) {
                push("java/lang/Class");
            } else if (sort == Type.METHOD) {
                push("java/lang/invoke/MethodType");
            } else {
                throw new IllegalArgumentException();
            }
        } else if (value instanceof Handle) {
            push("java/lang/invoke/MethodHandle");
        } else if (value instanceof ConstantDynamic) {
            pushDescriptor(((ConstantDynamic) value).getDescriptor());
        } else {
            throw new IllegalArgumentException();
        }
        labels = null;
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        super.visitIincInsn(var, increment);
        maxLocals = Math.max(maxLocals, var + 1);
        execute(Opcodes.IINC, var, null);
    }

    @Override
    public void visitTableSwitchInsn(
            final int min, final int max, final Label dflt, final Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        execute(Opcodes.TABLESWITCH, 0, null);
        this.locals = null;
        this.stack = null;
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        execute(Opcodes.LOOKUPSWITCH, 0, null);
        this.locals = null;
        this.stack = null;
    }

    @Override
    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        execute(Opcodes.MULTIANEWARRAY, numDimensions, descriptor);
    }

    @Override
    public void visitLocalVariable(
            final String name,
            final String descriptor,
            final String signature,
            final Label start,
            final Label end,
            final int index) {
        char firstDescriptorChar = descriptor.charAt(0);
        maxLocals =
                Math.max(
                        maxLocals, index + (firstDescriptorChar == 'J' || firstDescriptorChar == 'D' ? 2 : 1));
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        if (mv != null) {
            this.maxStack = Math.max(this.maxStack, maxStack);
            this.maxLocals = Math.max(this.maxLocals, maxLocals);
            mv.visitMaxs(this.maxStack, this.maxLocals);
        }
    }

    // -----------------------------------------------------------------------------------------------

    private Object get(final int local) {
        maxLocals = Math.max(maxLocals, local + 1);
        return local < locals.size() ? locals.get(local) : Opcodes.TOP;
    }

    private void set(final int local, final Object type) {
        maxLocals = Math.max(maxLocals, local + 1);
        while (local >= locals.size()) {
            locals.add(Opcodes.TOP);
        }
        locals.set(local, type);
    }

    private void push(final Object type) {
        stack.add(type);
        maxStack = Math.max(maxStack, stack.size());
    }

    private void pushDescriptor(final String fieldOrMethodDescriptor) {
        String descriptor =
                fieldOrMethodDescriptor.charAt(0) == '('
                        ? Type.getReturnType(fieldOrMethodDescriptor).getDescriptor()
                        : fieldOrMethodDescriptor;
        switch (descriptor.charAt(0)) {
            case 'V':
                return;
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
                push(Opcodes.INTEGER);
                return;
            case 'F':
                push(Opcodes.FLOAT);
                return;
            case 'J':
                push(Opcodes.LONG);
                push(Opcodes.TOP);
                return;
            case 'D':
                push(Opcodes.DOUBLE);
                push(Opcodes.TOP);
                return;
            case '[':
                push(descriptor);
                break;
            case 'L':
                push(descriptor.substring(1, descriptor.length() - 1));
                break;
            default:
                throw new AssertionError();
        }
    }

    private Object pop() {
        return stack.remove(stack.size() - 1);
    }

    private void pop(final int numSlots) {
        int size = stack.size();
        int end = size - numSlots;
        for (int i = size - 1; i >= end; --i) {
            stack.remove(i);
        }
    }

    private void pop(final String descriptor) {
        char firstDescriptorChar = descriptor.charAt(0);
        if (firstDescriptorChar == '(') {
            int numSlots = 0;
            Type[] types = Type.getArgumentTypes(descriptor);
            for (Type type : types) {
                numSlots += type.getSize();
            }
            pop(numSlots);
        } else if (firstDescriptorChar == 'J' || firstDescriptorChar == 'D') {
            pop(2);
        } else {
            pop(1);
        }
    }

    private void execute(final int opcode, final int intArg, final String stringArg) {
        if (opcode == Opcodes.JSR || opcode == Opcodes.RET) {
            throw new IllegalArgumentException("JSR/RET are not supported");
        }
        if (this.locals == null) {
            labels = null;
            return;
        }
        Object value1;
        Object value2;
        Object value3;
        Object t4;
        switch (opcode) {
            case Opcodes.NOP:
            case Opcodes.INEG:
            case Opcodes.LNEG:
            case Opcodes.FNEG:
            case Opcodes.DNEG:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
            case Opcodes.GOTO:
            case Opcodes.RETURN:
                break;
            case Opcodes.ACONST_NULL:
                push(Opcodes.NULL);
                break;
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                push(Opcodes.INTEGER);
                break;
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
                push(Opcodes.LONG);
                push(Opcodes.TOP);
                break;
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
                push(Opcodes.FLOAT);
                break;
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                push(Opcodes.DOUBLE);
                push(Opcodes.TOP);
                break;
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
            case Opcodes.ALOAD:
                push(get(intArg));
                break;
            case Opcodes.LLOAD:
            case Opcodes.DLOAD:
                push(get(intArg));
                push(Opcodes.TOP);
                break;
            case Opcodes.LALOAD:
            case Opcodes.D2L:
                pop(2);
                push(Opcodes.LONG);
                push(Opcodes.TOP);
                break;
            case Opcodes.DALOAD:
            case Opcodes.L2D:
                pop(2);
                push(Opcodes.DOUBLE);
                push(Opcodes.TOP);
                break;
            case Opcodes.AALOAD:
                pop(1);
                value1 = pop();
                if (value1 instanceof String) {
                    pushDescriptor(((String) value1).substring(1));
                } else if (value1 == Opcodes.NULL) {
                    push(value1);
                } else {
                    push("java/lang/Object");
                }
                break;
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
            case Opcodes.ASTORE:
                value1 = pop();
                set(intArg, value1);
                if (intArg > 0) {
                    value2 = get(intArg - 1);
                    if (value2 == Opcodes.LONG || value2 == Opcodes.DOUBLE) {
                        set(intArg - 1, Opcodes.TOP);
                    }
                }
                break;
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
                pop(1);
                value1 = pop();
                set(intArg, value1);
                set(intArg + 1, Opcodes.TOP);
                if (intArg > 0) {
                    value2 = get(intArg - 1);
                    if (value2 == Opcodes.LONG || value2 == Opcodes.DOUBLE) {
                        set(intArg - 1, Opcodes.TOP);
                    }
                }
                break;
            case Opcodes.IASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            case Opcodes.FASTORE:
            case Opcodes.AASTORE:
                pop(3);
                break;
            case Opcodes.LASTORE:
            case Opcodes.DASTORE:
                pop(4);
                break;
            case Opcodes.POP:
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IRETURN:
            case Opcodes.FRETURN:
            case Opcodes.ARETURN:
            case Opcodes.TABLESWITCH:
            case Opcodes.LOOKUPSWITCH:
            case Opcodes.ATHROW:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                pop(1);
                break;
            case Opcodes.POP2:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.LRETURN:
            case Opcodes.DRETURN:
                pop(2);
                break;
            case Opcodes.DUP:
                value1 = pop();
                push(value1);
                push(value1);
                break;
            case Opcodes.DUP_X1:
                value1 = pop();
                value2 = pop();
                push(value1);
                push(value2);
                push(value1);
                break;
            case Opcodes.DUP_X2:
                value1 = pop();
                value2 = pop();
                value3 = pop();
                push(value1);
                push(value3);
                push(value2);
                push(value1);
                break;
            case Opcodes.DUP2:
                value1 = pop();
                value2 = pop();
                push(value2);
                push(value1);
                push(value2);
                push(value1);
                break;
            case Opcodes.DUP2_X1:
                value1 = pop();
                value2 = pop();
                value3 = pop();
                push(value2);
                push(value1);
                push(value3);
                push(value2);
                push(value1);
                break;
            case Opcodes.DUP2_X2:
                value1 = pop();
                value2 = pop();
                value3 = pop();
                t4 = pop();
                push(value2);
                push(value1);
                push(t4);
                push(value3);
                push(value2);
                push(value1);
                break;
            case Opcodes.SWAP:
                value1 = pop();
                value2 = pop();
                push(value1);
                push(value2);
                break;
            case Opcodes.IALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.L2I:
            case Opcodes.D2I:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
                pop(2);
                push(Opcodes.INTEGER);
                break;
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
                pop(4);
                push(Opcodes.LONG);
                push(Opcodes.TOP);
                break;
            case Opcodes.FALOAD:
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
            case Opcodes.L2F:
            case Opcodes.D2F:
                pop(2);
                push(Opcodes.FLOAT);
                break;
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
                pop(4);
                push(Opcodes.DOUBLE);
                push(Opcodes.TOP);
                break;
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
                pop(3);
                push(Opcodes.LONG);
                push(Opcodes.TOP);
                break;
            case Opcodes.IINC:
                set(intArg, Opcodes.INTEGER);
                break;
            case Opcodes.I2L:
            case Opcodes.F2L:
                pop(1);
                push(Opcodes.LONG);
                push(Opcodes.TOP);
                break;
            case Opcodes.I2F:
                pop(1);
                push(Opcodes.FLOAT);
                break;
            case Opcodes.I2D:
            case Opcodes.F2D:
                pop(1);
                push(Opcodes.DOUBLE);
                push(Opcodes.TOP);
                break;
            case Opcodes.F2I:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.INSTANCEOF:
                pop(1);
                push(Opcodes.INTEGER);
                break;
            case Opcodes.LCMP:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                pop(4);
                push(Opcodes.INTEGER);
                break;
            case Opcodes.GETSTATIC:
                pushDescriptor(stringArg);
                break;
            case Opcodes.PUTSTATIC:
                pop(stringArg);
                break;
            case Opcodes.GETFIELD:
                pop(1);
                pushDescriptor(stringArg);
                break;
            case Opcodes.PUTFIELD:
                pop(stringArg);
                pop();
                break;
            case Opcodes.NEW:
                push(labels.get(0));
                break;
            case Opcodes.NEWARRAY:
                pop();
                switch (intArg) {
                    case Opcodes.T_BOOLEAN:
                        pushDescriptor("[Z");
                        break;
                    case Opcodes.T_CHAR:
                        pushDescriptor("[C");
                        break;
                    case Opcodes.T_BYTE:
                        pushDescriptor("[B");
                        break;
                    case Opcodes.T_SHORT:
                        pushDescriptor("[S");
                        break;
                    case Opcodes.T_INT:
                        pushDescriptor("[I");
                        break;
                    case Opcodes.T_FLOAT:
                        pushDescriptor("[F");
                        break;
                    case Opcodes.T_DOUBLE:
                        pushDescriptor("[D");
                        break;
                    case Opcodes.T_LONG:
                        pushDescriptor("[J");
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid array type " + intArg);
                }
                break;
            case Opcodes.ANEWARRAY:
                pop();
                pushDescriptor("[" + Type.getObjectType(stringArg));
                break;
            case Opcodes.CHECKCAST:
                pop();
                pushDescriptor(Type.getObjectType(stringArg).getDescriptor());
                break;
            case Opcodes.MULTIANEWARRAY:
                pop(intArg);
                pushDescriptor(stringArg);
                break;
            default:
                throw new IllegalArgumentException("Invalid opcode " + opcode);
        }
        labels = null;
    }
}
