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

import jdk.internal.org.objectweb.asm.ConstantDynamic;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;

/**
 * A {@link MethodVisitor} providing a more detailed API to generate and transform instructions.
 *
 * @author Eric Bruneton
 */
public class InstructionAdapter extends MethodVisitor {

    /** The type of the java.lang.Object class. */
    public static final Type OBJECT_TYPE = Type.getType("Ljava/lang/Object;");

    /**
      * Constructs a new {@link InstructionAdapter}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #InstructionAdapter(int, MethodVisitor)} version.
      *
      * @param methodVisitor the method visitor to which this adapter delegates calls.
      * @throws IllegalStateException If a subclass calls this constructor.
      */
    public InstructionAdapter(final MethodVisitor methodVisitor) {
        this(/* latest api = */ Opcodes.ASM8, methodVisitor);
        if (getClass() != InstructionAdapter.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link InstructionAdapter}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      * @param methodVisitor the method visitor to which this adapter delegates calls.
      */
    protected InstructionAdapter(final int api, final MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    @Override
    public void visitInsn(final int opcode) {
        switch (opcode) {
            case Opcodes.NOP:
                nop();
                break;
            case Opcodes.ACONST_NULL:
                aconst(null);
                break;
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                iconst(opcode - Opcodes.ICONST_0);
                break;
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
                lconst((long) (opcode - Opcodes.LCONST_0));
                break;
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
                fconst((float) (opcode - Opcodes.FCONST_0));
                break;
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                dconst((double) (opcode - Opcodes.DCONST_0));
                break;
            case Opcodes.IALOAD:
                aload(Type.INT_TYPE);
                break;
            case Opcodes.LALOAD:
                aload(Type.LONG_TYPE);
                break;
            case Opcodes.FALOAD:
                aload(Type.FLOAT_TYPE);
                break;
            case Opcodes.DALOAD:
                aload(Type.DOUBLE_TYPE);
                break;
            case Opcodes.AALOAD:
                aload(OBJECT_TYPE);
                break;
            case Opcodes.BALOAD:
                aload(Type.BYTE_TYPE);
                break;
            case Opcodes.CALOAD:
                aload(Type.CHAR_TYPE);
                break;
            case Opcodes.SALOAD:
                aload(Type.SHORT_TYPE);
                break;
            case Opcodes.IASTORE:
                astore(Type.INT_TYPE);
                break;
            case Opcodes.LASTORE:
                astore(Type.LONG_TYPE);
                break;
            case Opcodes.FASTORE:
                astore(Type.FLOAT_TYPE);
                break;
            case Opcodes.DASTORE:
                astore(Type.DOUBLE_TYPE);
                break;
            case Opcodes.AASTORE:
                astore(OBJECT_TYPE);
                break;
            case Opcodes.BASTORE:
                astore(Type.BYTE_TYPE);
                break;
            case Opcodes.CASTORE:
                astore(Type.CHAR_TYPE);
                break;
            case Opcodes.SASTORE:
                astore(Type.SHORT_TYPE);
                break;
            case Opcodes.POP:
                pop();
                break;
            case Opcodes.POP2:
                pop2();
                break;
            case Opcodes.DUP:
                dup();
                break;
            case Opcodes.DUP_X1:
                dupX1();
                break;
            case Opcodes.DUP_X2:
                dupX2();
                break;
            case Opcodes.DUP2:
                dup2();
                break;
            case Opcodes.DUP2_X1:
                dup2X1();
                break;
            case Opcodes.DUP2_X2:
                dup2X2();
                break;
            case Opcodes.SWAP:
                swap();
                break;
            case Opcodes.IADD:
                add(Type.INT_TYPE);
                break;
            case Opcodes.LADD:
                add(Type.LONG_TYPE);
                break;
            case Opcodes.FADD:
                add(Type.FLOAT_TYPE);
                break;
            case Opcodes.DADD:
                add(Type.DOUBLE_TYPE);
                break;
            case Opcodes.ISUB:
                sub(Type.INT_TYPE);
                break;
            case Opcodes.LSUB:
                sub(Type.LONG_TYPE);
                break;
            case Opcodes.FSUB:
                sub(Type.FLOAT_TYPE);
                break;
            case Opcodes.DSUB:
                sub(Type.DOUBLE_TYPE);
                break;
            case Opcodes.IMUL:
                mul(Type.INT_TYPE);
                break;
            case Opcodes.LMUL:
                mul(Type.LONG_TYPE);
                break;
            case Opcodes.FMUL:
                mul(Type.FLOAT_TYPE);
                break;
            case Opcodes.DMUL:
                mul(Type.DOUBLE_TYPE);
                break;
            case Opcodes.IDIV:
                div(Type.INT_TYPE);
                break;
            case Opcodes.LDIV:
                div(Type.LONG_TYPE);
                break;
            case Opcodes.FDIV:
                div(Type.FLOAT_TYPE);
                break;
            case Opcodes.DDIV:
                div(Type.DOUBLE_TYPE);
                break;
            case Opcodes.IREM:
                rem(Type.INT_TYPE);
                break;
            case Opcodes.LREM:
                rem(Type.LONG_TYPE);
                break;
            case Opcodes.FREM:
                rem(Type.FLOAT_TYPE);
                break;
            case Opcodes.DREM:
                rem(Type.DOUBLE_TYPE);
                break;
            case Opcodes.INEG:
                neg(Type.INT_TYPE);
                break;
            case Opcodes.LNEG:
                neg(Type.LONG_TYPE);
                break;
            case Opcodes.FNEG:
                neg(Type.FLOAT_TYPE);
                break;
            case Opcodes.DNEG:
                neg(Type.DOUBLE_TYPE);
                break;
            case Opcodes.ISHL:
                shl(Type.INT_TYPE);
                break;
            case Opcodes.LSHL:
                shl(Type.LONG_TYPE);
                break;
            case Opcodes.ISHR:
                shr(Type.INT_TYPE);
                break;
            case Opcodes.LSHR:
                shr(Type.LONG_TYPE);
                break;
            case Opcodes.IUSHR:
                ushr(Type.INT_TYPE);
                break;
            case Opcodes.LUSHR:
                ushr(Type.LONG_TYPE);
                break;
            case Opcodes.IAND:
                and(Type.INT_TYPE);
                break;
            case Opcodes.LAND:
                and(Type.LONG_TYPE);
                break;
            case Opcodes.IOR:
                or(Type.INT_TYPE);
                break;
            case Opcodes.LOR:
                or(Type.LONG_TYPE);
                break;
            case Opcodes.IXOR:
                xor(Type.INT_TYPE);
                break;
            case Opcodes.LXOR:
                xor(Type.LONG_TYPE);
                break;
            case Opcodes.I2L:
                cast(Type.INT_TYPE, Type.LONG_TYPE);
                break;
            case Opcodes.I2F:
                cast(Type.INT_TYPE, Type.FLOAT_TYPE);
                break;
            case Opcodes.I2D:
                cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                break;
            case Opcodes.L2I:
                cast(Type.LONG_TYPE, Type.INT_TYPE);
                break;
            case Opcodes.L2F:
                cast(Type.LONG_TYPE, Type.FLOAT_TYPE);
                break;
            case Opcodes.L2D:
                cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
                break;
            case Opcodes.F2I:
                cast(Type.FLOAT_TYPE, Type.INT_TYPE);
                break;
            case Opcodes.F2L:
                cast(Type.FLOAT_TYPE, Type.LONG_TYPE);
                break;
            case Opcodes.F2D:
                cast(Type.FLOAT_TYPE, Type.DOUBLE_TYPE);
                break;
            case Opcodes.D2I:
                cast(Type.DOUBLE_TYPE, Type.INT_TYPE);
                break;
            case Opcodes.D2L:
                cast(Type.DOUBLE_TYPE, Type.LONG_TYPE);
                break;
            case Opcodes.D2F:
                cast(Type.DOUBLE_TYPE, Type.FLOAT_TYPE);
                break;
            case Opcodes.I2B:
                cast(Type.INT_TYPE, Type.BYTE_TYPE);
                break;
            case Opcodes.I2C:
                cast(Type.INT_TYPE, Type.CHAR_TYPE);
                break;
            case Opcodes.I2S:
                cast(Type.INT_TYPE, Type.SHORT_TYPE);
                break;
            case Opcodes.LCMP:
                lcmp();
                break;
            case Opcodes.FCMPL:
                cmpl(Type.FLOAT_TYPE);
                break;
            case Opcodes.FCMPG:
                cmpg(Type.FLOAT_TYPE);
                break;
            case Opcodes.DCMPL:
                cmpl(Type.DOUBLE_TYPE);
                break;
            case Opcodes.DCMPG:
                cmpg(Type.DOUBLE_TYPE);
                break;
            case Opcodes.IRETURN:
                areturn(Type.INT_TYPE);
                break;
            case Opcodes.LRETURN:
                areturn(Type.LONG_TYPE);
                break;
            case Opcodes.FRETURN:
                areturn(Type.FLOAT_TYPE);
                break;
            case Opcodes.DRETURN:
                areturn(Type.DOUBLE_TYPE);
                break;
            case Opcodes.ARETURN:
                areturn(OBJECT_TYPE);
                break;
            case Opcodes.RETURN:
                areturn(Type.VOID_TYPE);
                break;
            case Opcodes.ARRAYLENGTH:
                arraylength();
                break;
            case Opcodes.ATHROW:
                athrow();
                break;
            case Opcodes.MONITORENTER:
                monitorenter();
                break;
            case Opcodes.MONITOREXIT:
                monitorexit();
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        switch (opcode) {
            case Opcodes.BIPUSH:
                iconst(operand);
                break;
            case Opcodes.SIPUSH:
                iconst(operand);
                break;
            case Opcodes.NEWARRAY:
                switch (operand) {
                    case Opcodes.T_BOOLEAN:
                        newarray(Type.BOOLEAN_TYPE);
                        break;
                    case Opcodes.T_CHAR:
                        newarray(Type.CHAR_TYPE);
                        break;
                    case Opcodes.T_BYTE:
                        newarray(Type.BYTE_TYPE);
                        break;
                    case Opcodes.T_SHORT:
                        newarray(Type.SHORT_TYPE);
                        break;
                    case Opcodes.T_INT:
                        newarray(Type.INT_TYPE);
                        break;
                    case Opcodes.T_FLOAT:
                        newarray(Type.FLOAT_TYPE);
                        break;
                    case Opcodes.T_LONG:
                        newarray(Type.LONG_TYPE);
                        break;
                    case Opcodes.T_DOUBLE:
                        newarray(Type.DOUBLE_TYPE);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        switch (opcode) {
            case Opcodes.ILOAD:
                load(var, Type.INT_TYPE);
                break;
            case Opcodes.LLOAD:
                load(var, Type.LONG_TYPE);
                break;
            case Opcodes.FLOAD:
                load(var, Type.FLOAT_TYPE);
                break;
            case Opcodes.DLOAD:
                load(var, Type.DOUBLE_TYPE);
                break;
            case Opcodes.ALOAD:
                load(var, OBJECT_TYPE);
                break;
            case Opcodes.ISTORE:
                store(var, Type.INT_TYPE);
                break;
            case Opcodes.LSTORE:
                store(var, Type.LONG_TYPE);
                break;
            case Opcodes.FSTORE:
                store(var, Type.FLOAT_TYPE);
                break;
            case Opcodes.DSTORE:
                store(var, Type.DOUBLE_TYPE);
                break;
            case Opcodes.ASTORE:
                store(var, OBJECT_TYPE);
                break;
            case Opcodes.RET:
                ret(var);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        Type objectType = Type.getObjectType(type);
        switch (opcode) {
            case Opcodes.NEW:
                anew(objectType);
                break;
            case Opcodes.ANEWARRAY:
                newarray(objectType);
                break;
            case Opcodes.CHECKCAST:
                checkcast(objectType);
                break;
            case Opcodes.INSTANCEOF:
                instanceOf(objectType);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
        switch (opcode) {
            case Opcodes.GETSTATIC:
                getstatic(owner, name, descriptor);
                break;
            case Opcodes.PUTSTATIC:
                putstatic(owner, name, descriptor);
                break;
            case Opcodes.GETFIELD:
                getfield(owner, name, descriptor);
                break;
            case Opcodes.PUTFIELD:
                putfield(owner, name, descriptor);
                break;
            default:
                throw new IllegalArgumentException();
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
        int opcode = opcodeAndSource & ~Opcodes.SOURCE_MASK;

        switch (opcode) {
            case Opcodes.INVOKESPECIAL:
                invokespecial(owner, name, descriptor, isInterface);
                break;
            case Opcodes.INVOKEVIRTUAL:
                invokevirtual(owner, name, descriptor, isInterface);
                break;
            case Opcodes.INVOKESTATIC:
                invokestatic(owner, name, descriptor, isInterface);
                break;
            case Opcodes.INVOKEINTERFACE:
                invokeinterface(owner, name, descriptor);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitInvokeDynamicInsn(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
        invokedynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        switch (opcode) {
            case Opcodes.IFEQ:
                ifeq(label);
                break;
            case Opcodes.IFNE:
                ifne(label);
                break;
            case Opcodes.IFLT:
                iflt(label);
                break;
            case Opcodes.IFGE:
                ifge(label);
                break;
            case Opcodes.IFGT:
                ifgt(label);
                break;
            case Opcodes.IFLE:
                ifle(label);
                break;
            case Opcodes.IF_ICMPEQ:
                ificmpeq(label);
                break;
            case Opcodes.IF_ICMPNE:
                ificmpne(label);
                break;
            case Opcodes.IF_ICMPLT:
                ificmplt(label);
                break;
            case Opcodes.IF_ICMPGE:
                ificmpge(label);
                break;
            case Opcodes.IF_ICMPGT:
                ificmpgt(label);
                break;
            case Opcodes.IF_ICMPLE:
                ificmple(label);
                break;
            case Opcodes.IF_ACMPEQ:
                ifacmpeq(label);
                break;
            case Opcodes.IF_ACMPNE:
                ifacmpne(label);
                break;
            case Opcodes.GOTO:
                goTo(label);
                break;
            case Opcodes.JSR:
                jsr(label);
                break;
            case Opcodes.IFNULL:
                ifnull(label);
                break;
            case Opcodes.IFNONNULL:
                ifnonnull(label);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitLabel(final Label label) {
        mark(label);
    }

    @Override
    public void visitLdcInsn(final Object value) {
        if (api < Opcodes.ASM5
                && (value instanceof Handle
                        || (value instanceof Type && ((Type) value).getSort() == Type.METHOD))) {
            throw new UnsupportedOperationException("This feature requires ASM5");
        }
        if (api < Opcodes.ASM7 && value instanceof ConstantDynamic) {
            throw new UnsupportedOperationException("This feature requires ASM7");
        }
        if (value instanceof Integer) {
            iconst((Integer) value);
        } else if (value instanceof Byte) {
            iconst(((Byte) value).intValue());
        } else if (value instanceof Character) {
            iconst(((Character) value).charValue());
        } else if (value instanceof Short) {
            iconst(((Short) value).intValue());
        } else if (value instanceof Boolean) {
            iconst(((Boolean) value).booleanValue() ? 1 : 0);
        } else if (value instanceof Float) {
            fconst((Float) value);
        } else if (value instanceof Long) {
            lconst((Long) value);
        } else if (value instanceof Double) {
            dconst((Double) value);
        } else if (value instanceof String) {
            aconst(value);
        } else if (value instanceof Type) {
            tconst((Type) value);
        } else if (value instanceof Handle) {
            hconst((Handle) value);
        } else if (value instanceof ConstantDynamic) {
            cconst((ConstantDynamic) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        iinc(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(
            final int min, final int max, final Label dflt, final Label... labels) {
        tableswitch(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        lookupswitch(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
        multianewarray(descriptor, numDimensions);
    }

    // -----------------------------------------------------------------------------------------------

    /** Generates a nop instruction. */
    public void nop() {
        mv.visitInsn(Opcodes.NOP);
    }

    /**
      * Generates the instruction to push the given value on the stack.
      *
      * @param value the constant to be pushed on the stack. This parameter must be an {@link Integer},
      *     a {@link Float}, a {@link Long}, a {@link Double}, a {@link String}, a {@link Type} of
      *     OBJECT or ARRAY sort for {@code .class} constants, for classes whose version is 49, a
      *     {@link Type} of METHOD sort for MethodType, a {@link Handle} for MethodHandle constants,
      *     for classes whose version is 51 or a {@link ConstantDynamic} for a constant dynamic for
      *     classes whose version is 55.
      */
    public void aconst(final Object value) {
        if (value == null) {
            mv.visitInsn(Opcodes.ACONST_NULL);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    /**
      * Generates the instruction to push the given value on the stack.
      *
      * @param intValue the constant to be pushed on the stack.
      */
    public void iconst(final int intValue) {
        if (intValue >= -1 && intValue <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + intValue);
        } else if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, intValue);
        } else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, intValue);
        } else {
            mv.visitLdcInsn(intValue);
        }
    }

    /**
      * Generates the instruction to push the given value on the stack.
      *
      * @param longValue the constant to be pushed on the stack.
      */
    public void lconst(final long longValue) {
        if (longValue == 0L || longValue == 1L) {
            mv.visitInsn(Opcodes.LCONST_0 + (int) longValue);
        } else {
            mv.visitLdcInsn(longValue);
        }
    }

    /**
      * Generates the instruction to push the given value on the stack.
      *
      * @param floatValue the constant to be pushed on the stack.
      */
    public void fconst(final float floatValue) {
        int bits = Float.floatToIntBits(floatValue);
        if (bits == 0L || bits == 0x3F800000 || bits == 0x40000000) { // 0..2
            mv.visitInsn(Opcodes.FCONST_0 + (int) floatValue);
        } else {
            mv.visitLdcInsn(floatValue);
        }
    }

    /**
      * Generates the instruction to push the given value on the stack.
      *
      * @param doubleValue the constant to be pushed on the stack.
      */
    public void dconst(final double doubleValue) {
        long bits = Double.doubleToLongBits(doubleValue);
        if (bits == 0L || bits == 0x3FF0000000000000L) { // +0.0d and 1.0d
            mv.visitInsn(Opcodes.DCONST_0 + (int) doubleValue);
        } else {
            mv.visitLdcInsn(doubleValue);
        }
    }

    /**
      * Generates the instruction to push the given type on the stack.
      *
      * @param type the type to be pushed on the stack.
      */
    public void tconst(final Type type) {
        mv.visitLdcInsn(type);
    }

    /**
      * Generates the instruction to push the given handle on the stack.
      *
      * @param handle the handle to be pushed on the stack.
      */
    public void hconst(final Handle handle) {
        mv.visitLdcInsn(handle);
    }

    /**
      * Generates the instruction to push the given constant dynamic on the stack.
      *
      * @param constantDynamic the constant dynamic to be pushed on the stack.
      */
    public void cconst(final ConstantDynamic constantDynamic) {
        mv.visitLdcInsn(constantDynamic);
    }

    public void load(final int var, final Type type) {
        mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), var);
    }

    public void aload(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IALOAD));
    }

    public void store(final int var, final Type type) {
        mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), var);
    }

    public void astore(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IASTORE));
    }

    public void pop() {
        mv.visitInsn(Opcodes.POP);
    }

    public void pop2() {
        mv.visitInsn(Opcodes.POP2);
    }

    public void dup() {
        mv.visitInsn(Opcodes.DUP);
    }

    public void dup2() {
        mv.visitInsn(Opcodes.DUP2);
    }

    public void dupX1() {
        mv.visitInsn(Opcodes.DUP_X1);
    }

    public void dupX2() {
        mv.visitInsn(Opcodes.DUP_X2);
    }

    public void dup2X1() {
        mv.visitInsn(Opcodes.DUP2_X1);
    }

    public void dup2X2() {
        mv.visitInsn(Opcodes.DUP2_X2);
    }

    public void swap() {
        mv.visitInsn(Opcodes.SWAP);
    }

    public void add(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IADD));
    }

    public void sub(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.ISUB));
    }

    public void mul(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IMUL));
    }

    public void div(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IDIV));
    }

    public void rem(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IREM));
    }

    public void neg(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.INEG));
    }

    public void shl(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.ISHL));
    }

    public void shr(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.ISHR));
    }

    public void ushr(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IUSHR));
    }

    public void and(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IAND));
    }

    public void or(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IOR));
    }

    public void xor(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IXOR));
    }

    public void iinc(final int var, final int increment) {
        mv.visitIincInsn(var, increment);
    }

    /**
      * Generates the instruction to cast from the first given type to the other.
      *
      * @param from a Type.
      * @param to a Type.
      */
    public void cast(final Type from, final Type to) {
        cast(mv, from, to);
    }

    /**
      * Generates the instruction to cast from the first given type to the other.
      *
      * @param methodVisitor the method visitor to use to generate the instruction.
      * @param from a Type.
      * @param to a Type.
      */
    static void cast(final MethodVisitor methodVisitor, final Type from, final Type to) {
        if (from != to) {
            if (from == Type.DOUBLE_TYPE) {
                if (to == Type.FLOAT_TYPE) {
                    methodVisitor.visitInsn(Opcodes.D2F);
                } else if (to == Type.LONG_TYPE) {
                    methodVisitor.visitInsn(Opcodes.D2L);
                } else {
                    methodVisitor.visitInsn(Opcodes.D2I);
                    cast(methodVisitor, Type.INT_TYPE, to);
                }
            } else if (from == Type.FLOAT_TYPE) {
                if (to == Type.DOUBLE_TYPE) {
                    methodVisitor.visitInsn(Opcodes.F2D);
                } else if (to == Type.LONG_TYPE) {
                    methodVisitor.visitInsn(Opcodes.F2L);
                } else {
                    methodVisitor.visitInsn(Opcodes.F2I);
                    cast(methodVisitor, Type.INT_TYPE, to);
                }
            } else if (from == Type.LONG_TYPE) {
                if (to == Type.DOUBLE_TYPE) {
                    methodVisitor.visitInsn(Opcodes.L2D);
                } else if (to == Type.FLOAT_TYPE) {
                    methodVisitor.visitInsn(Opcodes.L2F);
                } else {
                    methodVisitor.visitInsn(Opcodes.L2I);
                    cast(methodVisitor, Type.INT_TYPE, to);
                }
            } else {
                if (to == Type.BYTE_TYPE) {
                    methodVisitor.visitInsn(Opcodes.I2B);
                } else if (to == Type.CHAR_TYPE) {
                    methodVisitor.visitInsn(Opcodes.I2C);
                } else if (to == Type.DOUBLE_TYPE) {
                    methodVisitor.visitInsn(Opcodes.I2D);
                } else if (to == Type.FLOAT_TYPE) {
                    methodVisitor.visitInsn(Opcodes.I2F);
                } else if (to == Type.LONG_TYPE) {
                    methodVisitor.visitInsn(Opcodes.I2L);
                } else if (to == Type.SHORT_TYPE) {
                    methodVisitor.visitInsn(Opcodes.I2S);
                }
            }
        }
    }

    public void lcmp() {
        mv.visitInsn(Opcodes.LCMP);
    }

    public void cmpl(final Type type) {
        mv.visitInsn(type == Type.FLOAT_TYPE ? Opcodes.FCMPL : Opcodes.DCMPL);
    }

    public void cmpg(final Type type) {
        mv.visitInsn(type == Type.FLOAT_TYPE ? Opcodes.FCMPG : Opcodes.DCMPG);
    }

    public void ifeq(final Label label) {
        mv.visitJumpInsn(Opcodes.IFEQ, label);
    }

    public void ifne(final Label label) {
        mv.visitJumpInsn(Opcodes.IFNE, label);
    }

    public void iflt(final Label label) {
        mv.visitJumpInsn(Opcodes.IFLT, label);
    }

    public void ifge(final Label label) {
        mv.visitJumpInsn(Opcodes.IFGE, label);
    }

    public void ifgt(final Label label) {
        mv.visitJumpInsn(Opcodes.IFGT, label);
    }

    public void ifle(final Label label) {
        mv.visitJumpInsn(Opcodes.IFLE, label);
    }

    public void ificmpeq(final Label label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, label);
    }

    public void ificmpne(final Label label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPNE, label);
    }

    public void ificmplt(final Label label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPLT, label);
    }

    public void ificmpge(final Label label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, label);
    }

    public void ificmpgt(final Label label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPGT, label);
    }

    public void ificmple(final Label label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPLE, label);
    }

    public void ifacmpeq(final Label label) {
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
    }

    public void ifacmpne(final Label label) {
        mv.visitJumpInsn(Opcodes.IF_ACMPNE, label);
    }

    public void goTo(final Label label) {
        mv.visitJumpInsn(Opcodes.GOTO, label);
    }

    public void jsr(final Label label) {
        mv.visitJumpInsn(Opcodes.JSR, label);
    }

    public void ret(final int var) {
        mv.visitVarInsn(Opcodes.RET, var);
    }

    public void tableswitch(final int min, final int max, final Label dflt, final Label... labels) {
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void lookupswitch(final Label dflt, final int[] keys, final Label[] labels) {
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void areturn(final Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
    }

    public void getstatic(final String owner, final String name, final String descriptor) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, owner, name, descriptor);
    }

    public void putstatic(final String owner, final String name, final String descriptor) {
        mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, name, descriptor);
    }

    public void getfield(final String owner, final String name, final String descriptor) {
        mv.visitFieldInsn(Opcodes.GETFIELD, owner, name, descriptor);
    }

    public void putfield(final String owner, final String name, final String descriptor) {
        mv.visitFieldInsn(Opcodes.PUTFIELD, owner, name, descriptor);
    }

    /**
      * Deprecated.
      *
      * @param owner the internal name of the method's owner class.
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @deprecated use {@link #invokevirtual(String, String, String, boolean)} instead.
      */
    @Deprecated
    public void invokevirtual(final String owner, final String name, final String descriptor) {
        if (api >= Opcodes.ASM5) {
            invokevirtual(owner, name, descriptor, false);
            return;
        }
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor);
    }

    /**
      * Generates the instruction to call the given virtual method.
      *
      * @param owner the internal name of the method's owner class (see {@link
      *     Type#getInternalName()}).
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param isInterface if the method's owner class is an interface.
      */
    public void invokevirtual(
            final String owner, final String name, final String descriptor, final boolean isInterface) {
        if (api < Opcodes.ASM5) {
            if (isInterface) {
                throw new IllegalArgumentException("INVOKEVIRTUAL on interfaces require ASM 5");
            }
            invokevirtual(owner, name, descriptor);
            return;
        }
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, isInterface);
    }

    /**
      * Deprecated.
      *
      * @param owner the internal name of the method's owner class.
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @deprecated use {@link #invokespecial(String, String, String, boolean)} instead.
      */
    @Deprecated
    public void invokespecial(final String owner, final String name, final String descriptor) {
        if (api >= Opcodes.ASM5) {
            invokespecial(owner, name, descriptor, false);
            return;
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, descriptor, false);
    }

    /**
      * Generates the instruction to call the given special method.
      *
      * @param owner the internal name of the method's owner class (see {@link
      *     Type#getInternalName()}).
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param isInterface if the method's owner class is an interface.
      */
    public void invokespecial(
            final String owner, final String name, final String descriptor, final boolean isInterface) {
        if (api < Opcodes.ASM5) {
            if (isInterface) {
                throw new IllegalArgumentException("INVOKESPECIAL on interfaces require ASM 5");
            }
            invokespecial(owner, name, descriptor);
            return;
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, descriptor, isInterface);
    }

    /**
      * Deprecated.
      *
      * @param owner the internal name of the method's owner class.
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @deprecated use {@link #invokestatic(String, String, String, boolean)} instead.
      */
    @Deprecated
    public void invokestatic(final String owner, final String name, final String descriptor) {
        if (api >= Opcodes.ASM5) {
            invokestatic(owner, name, descriptor, false);
            return;
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, false);
    }

    /**
      * Generates the instruction to call the given static method.
      *
      * @param owner the internal name of the method's owner class (see {@link
      *     Type#getInternalName()}).
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param isInterface if the method's owner class is an interface.
      */
    public void invokestatic(
            final String owner, final String name, final String descriptor, final boolean isInterface) {
        if (api < Opcodes.ASM5) {
            if (isInterface) {
                throw new IllegalArgumentException("INVOKESTATIC on interfaces require ASM 5");
            }
            invokestatic(owner, name, descriptor);
            return;
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, isInterface);
    }

    /**
      * Generates the instruction to call the given interface method.
      *
      * @param owner the internal name of the method's owner class (see {@link
      *     Type#getInternalName()}).
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      */
    public void invokeinterface(final String owner, final String name, final String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, name, descriptor, true);
    }

    /**
      * Generates the instruction to call the given dynamic method.
      *
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param bootstrapMethodHandle the bootstrap method.
      * @param bootstrapMethodArguments the bootstrap method constant arguments. Each argument must be
      *     an {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link
      *     Type}, {@link Handle} or {@link ConstantDynamic} value. This method is allowed to modify
      *     the content of the array so a caller should expect that this array may change.
      */
    public void invokedynamic(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object[] bootstrapMethodArguments) {
        mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    public void anew(final Type type) {
        mv.visitTypeInsn(Opcodes.NEW, type.getInternalName());
    }

    /**
      * Generates the instruction to create and push on the stack an array of the given type.
      *
      * @param type an array Type.
      */
    public void newarray(final Type type) {
        newarray(mv, type);
    }

    /**
      * Generates the instruction to create and push on the stack an array of the given type.
      *
      * @param methodVisitor the method visitor to use to generate the instruction.
      * @param type an array Type.
      */
    static void newarray(final MethodVisitor methodVisitor, final Type type) {
        int arrayType;
        switch (type.getSort()) {
            case Type.BOOLEAN:
                arrayType = Opcodes.T_BOOLEAN;
                break;
            case Type.CHAR:
                arrayType = Opcodes.T_CHAR;
                break;
            case Type.BYTE:
                arrayType = Opcodes.T_BYTE;
                break;
            case Type.SHORT:
                arrayType = Opcodes.T_SHORT;
                break;
            case Type.INT:
                arrayType = Opcodes.T_INT;
                break;
            case Type.FLOAT:
                arrayType = Opcodes.T_FLOAT;
                break;
            case Type.LONG:
                arrayType = Opcodes.T_LONG;
                break;
            case Type.DOUBLE:
                arrayType = Opcodes.T_DOUBLE;
                break;
            default:
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, type.getInternalName());
                return;
        }
        methodVisitor.visitIntInsn(Opcodes.NEWARRAY, arrayType);
    }

    public void arraylength() {
        mv.visitInsn(Opcodes.ARRAYLENGTH);
    }

    public void athrow() {
        mv.visitInsn(Opcodes.ATHROW);
    }

    public void checkcast(final Type type) {
        mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
    }

    public void instanceOf(final Type type) {
        mv.visitTypeInsn(Opcodes.INSTANCEOF, type.getInternalName());
    }

    public void monitorenter() {
        mv.visitInsn(Opcodes.MONITORENTER);
    }

    public void monitorexit() {
        mv.visitInsn(Opcodes.MONITOREXIT);
    }

    public void multianewarray(final String descriptor, final int numDimensions) {
        mv.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    public void ifnull(final Label label) {
        mv.visitJumpInsn(Opcodes.IFNULL, label);
    }

    public void ifnonnull(final Label label) {
        mv.visitJumpInsn(Opcodes.IFNONNULL, label);
    }

    public void mark(final Label label) {
        mv.visitLabel(label);
    }
}
