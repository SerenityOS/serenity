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
package jdk.internal.org.objectweb.asm.tree.analysis;

import java.util.List;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * A semantic bytecode interpreter. More precisely, this interpreter only manages the computation of
 * values from other values: it does not manage the transfer of values to or from the stack, and to
 * or from the local variables. This separation allows a generic bytecode {@link Analyzer} to work
 * with various semantic interpreters, without needing to duplicate the code to simulate the
 * transfer of values.
 *
 * @param <V> type of the Value used for the analysis.
 * @author Eric Bruneton
 */
public abstract class Interpreter<V extends Value> {

    /**
      * The ASM API version supported by this interpreter. The value of this field must be one of
      * {@link jdk.internal.org.objectweb.asm.Opcodes#ASM4}, {@link jdk.internal.org.objectweb.asm.Opcodes#ASM5}, {@link
      * jdk.internal.org.objectweb.asm.Opcodes#ASM6} or {@link jdk.internal.org.objectweb.asm.Opcodes#ASM7}.
      */
    protected final int api;

    /**
      * Constructs a new {@link Interpreter}.
      *
      * @param api the ASM API version supported by this interpreter. Must be one of {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM4}, {@link jdk.internal.org.objectweb.asm.Opcodes#ASM5}, {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM6} or {@link jdk.internal.org.objectweb.asm.Opcodes#ASM7}.
      */
    protected Interpreter(final int api) {
        this.api = api;
    }

    /**
      * Creates a new value that represents the given type.
      *
      * <p>Called for method parameters (including <code>this</code>), exception handler variable and
      * with <code>null</code> type for variables reserved by long and double types.
      *
      * <p>An interpreter may choose to implement one or more of {@link
      * Interpreter#newReturnTypeValue(Type)}, {@link Interpreter#newParameterValue(boolean, int,
      * Type)}, {@link Interpreter#newEmptyValue(int)}, {@link
      * Interpreter#newExceptionValue(TryCatchBlockNode, Frame, Type)} to distinguish different types
      * of new value.
      *
      * @param type a primitive or reference type, or {@literal null} to represent an uninitialized
      *     value.
      * @return a value that represents the given type. The size of the returned value must be equal to
      *     the size of the given type.
      */
    public abstract V newValue(Type type);

    /**
      * Creates a new value that represents the given parameter type. This method is called to
      * initialize the value of a local corresponding to a method parameter in a frame.
      *
      * <p>By default, calls <code>newValue(type)</code>.
      *
      * @param isInstanceMethod {@literal true} if the method is non-static.
      * @param local the local variable index.
      * @param type a primitive or reference type.
      * @return a value that represents the given type. The size of the returned value must be equal to
      *     the size of the given type.
      */
    public V newParameterValue(final boolean isInstanceMethod, final int local, final Type type) {
        return newValue(type);
    }

    /**
      * Creates a new value that represents the given return type. This method is called to initialize
      * the return type value of a frame.
      *
      * <p>By default, calls <code>newValue(type)</code>.
      *
      * @param type a primitive or reference type.
      * @return a value that represents the given type. The size of the returned value must be equal to
      *     the size of the given type.
      */
    public V newReturnTypeValue(final Type type) {
        return newValue(type);
    }

    /**
      * Creates a new uninitialized value for a local variable. This method is called to initialize the
      * value of a local that does not correspond to a method parameter, and to reset one half of a
      * size-2 value when the other half is assigned a size-1 value.
      *
      * <p>By default, calls <code>newValue(null)</code>.
      *
      * @param local the local variable index.
      * @return a value representing an uninitialized value. The size of the returned value must be
      *     equal to 1.
      */
    public V newEmptyValue(final int local) {
        return newValue(null);
    }

    /**
      * Creates a new value that represents the given exception type. This method is called to
      * initialize the exception value on the call stack at the entry of an exception handler.
      *
      * <p>By default, calls <code>newValue(exceptionType)</code>.
      *
      * @param tryCatchBlockNode the exception handler.
      * @param handlerFrame the exception handler frame.
      * @param exceptionType the exception type handled by this handler.
      * @return a value that represents the given {@code exceptionType}. The size of the returned value
      *     must be equal to 1.
      */
    public V newExceptionValue(
            final TryCatchBlockNode tryCatchBlockNode,
            final Frame<V> handlerFrame,
            final Type exceptionType) {
        return newValue(exceptionType);
    }

    /**
      * Interprets a bytecode instruction without arguments. This method is called for the following
      * opcodes:
      *
      * <p>ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
      * LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, BIPUSH, SIPUSH, LDC, JSR,
      * GETSTATIC, NEW
      *
      * @param insn the bytecode instruction to be interpreted.
      * @return the result of the interpretation of the given instruction.
      * @throws AnalyzerException if an error occurred during the interpretation.
      */
    public abstract V newOperation(AbstractInsnNode insn) throws AnalyzerException;

    /**
      * Interprets a bytecode instruction that moves a value on the stack or to or from local
      * variables. This method is called for the following opcodes:
      *
      * <p>ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, DUP, DUP_X1,
      * DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP
      *
      * @param insn the bytecode instruction to be interpreted.
      * @param value the value that must be moved by the instruction.
      * @return the result of the interpretation of the given instruction. The returned value must be
      *     {@code equal} to the given value.
      * @throws AnalyzerException if an error occurred during the interpretation.
      */
    public abstract V copyOperation(AbstractInsnNode insn, V value) throws AnalyzerException;

    /**
      * Interprets a bytecode instruction with a single argument. This method is called for the
      * following opcodes:
      *
      * <p>INEG, LNEG, FNEG, DNEG, IINC, I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F,
      * I2B, I2C, I2S, IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN,
      * FRETURN, DRETURN, ARETURN, PUTSTATIC, GETFIELD, NEWARRAY, ANEWARRAY, ARRAYLENGTH, ATHROW,
      * CHECKCAST, INSTANCEOF, MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL
      *
      * @param insn the bytecode instruction to be interpreted.
      * @param value the argument of the instruction to be interpreted.
      * @return the result of the interpretation of the given instruction.
      * @throws AnalyzerException if an error occurred during the interpretation.
      */
    public abstract V unaryOperation(AbstractInsnNode insn, V value) throws AnalyzerException;

    /**
      * Interprets a bytecode instruction with two arguments. This method is called for the following
      * opcodes:
      *
      * <p>IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IADD, LADD, FADD, DADD,
      * ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM,
      * ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR, LCMP, FCMPL, FCMPG,
      * DCMPL, DCMPG, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
      * IF_ACMPNE, PUTFIELD
      *
      * @param insn the bytecode instruction to be interpreted.
      * @param value1 the first argument of the instruction to be interpreted.
      * @param value2 the second argument of the instruction to be interpreted.
      * @return the result of the interpretation of the given instruction.
      * @throws AnalyzerException if an error occurred during the interpretation.
      */
    public abstract V binaryOperation(AbstractInsnNode insn, V value1, V value2)
            throws AnalyzerException;

    /**
      * Interprets a bytecode instruction with three arguments. This method is called for the following
      * opcodes:
      *
      * <p>IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE
      *
      * @param insn the bytecode instruction to be interpreted.
      * @param value1 the first argument of the instruction to be interpreted.
      * @param value2 the second argument of the instruction to be interpreted.
      * @param value3 the third argument of the instruction to be interpreted.
      * @return the result of the interpretation of the given instruction.
      * @throws AnalyzerException if an error occurred during the interpretation.
      */
    public abstract V ternaryOperation(AbstractInsnNode insn, V value1, V value2, V value3)
            throws AnalyzerException;

    /**
      * Interprets a bytecode instruction with a variable number of arguments. This method is called
      * for the following opcodes:
      *
      * <p>INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE, MULTIANEWARRAY and
      * INVOKEDYNAMIC
      *
      * @param insn the bytecode instruction to be interpreted.
      * @param values the arguments of the instruction to be interpreted.
      * @return the result of the interpretation of the given instruction.
      * @throws AnalyzerException if an error occurred during the interpretation.
      */
    public abstract V naryOperation(AbstractInsnNode insn, List<? extends V> values)
            throws AnalyzerException;

    /**
      * Interprets a bytecode return instruction. This method is called for the following opcodes:
      *
      * <p>IRETURN, LRETURN, FRETURN, DRETURN, ARETURN
      *
      * @param insn the bytecode instruction to be interpreted.
      * @param value the argument of the instruction to be interpreted.
      * @param expected the expected return type of the analyzed method.
      * @throws AnalyzerException if an error occurred during the interpretation.
      */
    public abstract void returnOperation(AbstractInsnNode insn, V value, V expected)
            throws AnalyzerException;

    /**
      * Merges two values. The merge operation must return a value that represents both values (for
      * instance, if the two values are two types, the merged value must be a common super type of the
      * two types. If the two values are integer intervals, the merged value must be an interval that
      * contains the previous ones. Likewise for other types of values).
      *
      * @param value1 a value.
      * @param value2 another value.
      * @return the merged value. If the merged value is equal to {@code value1}, this method
      *     <i>must</i> return {@code value1}.
      */
    public abstract V merge(V value1, V value2);
}
