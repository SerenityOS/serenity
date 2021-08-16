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
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.InvokeDynamicInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;

/**
 * An extended {@link BasicInterpreter} that checks that bytecode instructions are correctly used.
 *
 * @author Eric Bruneton
 * @author Bing Ran
 */
public class BasicVerifier extends BasicInterpreter {

    /**
      * Constructs a new {@link BasicVerifier} for the latest ASM API version. <i>Subclasses must not
      * use this constructor</i>. Instead, they must use the {@link #BasicVerifier(int)} version.
      */
    public BasicVerifier() {
        super(/* latest api = */ ASM8);
        if (getClass() != BasicVerifier.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link BasicVerifier}.
      *
      * @param api the ASM API version supported by this interpreter. Must be one of {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM4}, {@link jdk.internal.org.objectweb.asm.Opcodes#ASM5}, {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM6}, {@link jdk.internal.org.objectweb.asm.Opcodes#ASM7} or {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM8}.
      */
    protected BasicVerifier(final int api) {
        super(api);
    }

    @Override
    public BasicValue copyOperation(final AbstractInsnNode insn, final BasicValue value)
            throws AnalyzerException {
        Value expected;
        switch (insn.getOpcode()) {
            case ILOAD:
            case ISTORE:
                expected = BasicValue.INT_VALUE;
                break;
            case FLOAD:
            case FSTORE:
                expected = BasicValue.FLOAT_VALUE;
                break;
            case LLOAD:
            case LSTORE:
                expected = BasicValue.LONG_VALUE;
                break;
            case DLOAD:
            case DSTORE:
                expected = BasicValue.DOUBLE_VALUE;
                break;
            case ALOAD:
                if (!value.isReference()) {
                    throw new AnalyzerException(insn, null, "an object reference", value);
                }
                return value;
            case ASTORE:
                if (!value.isReference() && !BasicValue.RETURNADDRESS_VALUE.equals(value)) {
                    throw new AnalyzerException(insn, null, "an object reference or a return address", value);
                }
                return value;
            default:
                return value;
        }
        if (!expected.equals(value)) {
            throw new AnalyzerException(insn, null, expected, value);
        }
        return value;
    }

    @Override
    public BasicValue unaryOperation(final AbstractInsnNode insn, final BasicValue value)
            throws AnalyzerException {
        BasicValue expected;
        switch (insn.getOpcode()) {
            case INEG:
            case IINC:
            case I2F:
            case I2L:
            case I2D:
            case I2B:
            case I2C:
            case I2S:
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case TABLESWITCH:
            case LOOKUPSWITCH:
            case IRETURN:
            case NEWARRAY:
            case ANEWARRAY:
                expected = BasicValue.INT_VALUE;
                break;
            case FNEG:
            case F2I:
            case F2L:
            case F2D:
            case FRETURN:
                expected = BasicValue.FLOAT_VALUE;
                break;
            case LNEG:
            case L2I:
            case L2F:
            case L2D:
            case LRETURN:
                expected = BasicValue.LONG_VALUE;
                break;
            case DNEG:
            case D2I:
            case D2F:
            case D2L:
            case DRETURN:
                expected = BasicValue.DOUBLE_VALUE;
                break;
            case GETFIELD:
                expected = newValue(Type.getObjectType(((FieldInsnNode) insn).owner));
                break;
            case ARRAYLENGTH:
                if (!isArrayValue(value)) {
                    throw new AnalyzerException(insn, null, "an array reference", value);
                }
                return super.unaryOperation(insn, value);
            case CHECKCAST:
            case ARETURN:
            case ATHROW:
            case INSTANCEOF:
            case MONITORENTER:
            case MONITOREXIT:
            case IFNULL:
            case IFNONNULL:
                if (!value.isReference()) {
                    throw new AnalyzerException(insn, null, "an object reference", value);
                }
                return super.unaryOperation(insn, value);
            case PUTSTATIC:
                expected = newValue(Type.getType(((FieldInsnNode) insn).desc));
                break;
            default:
                throw new AssertionError();
        }
        if (!isSubTypeOf(value, expected)) {
            throw new AnalyzerException(insn, null, expected, value);
        }
        return super.unaryOperation(insn, value);
    }

    @Override
    public BasicValue binaryOperation(
            final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2)
            throws AnalyzerException {
        BasicValue expected1;
        BasicValue expected2;
        switch (insn.getOpcode()) {
            case IALOAD:
                expected1 = newValue(Type.getType("[I"));
                expected2 = BasicValue.INT_VALUE;
                break;
            case BALOAD:
                if (isSubTypeOf(value1, newValue(Type.getType("[Z")))) {
                    expected1 = newValue(Type.getType("[Z"));
                } else {
                    expected1 = newValue(Type.getType("[B"));
                }
                expected2 = BasicValue.INT_VALUE;
                break;
            case CALOAD:
                expected1 = newValue(Type.getType("[C"));
                expected2 = BasicValue.INT_VALUE;
                break;
            case SALOAD:
                expected1 = newValue(Type.getType("[S"));
                expected2 = BasicValue.INT_VALUE;
                break;
            case LALOAD:
                expected1 = newValue(Type.getType("[J"));
                expected2 = BasicValue.INT_VALUE;
                break;
            case FALOAD:
                expected1 = newValue(Type.getType("[F"));
                expected2 = BasicValue.INT_VALUE;
                break;
            case DALOAD:
                expected1 = newValue(Type.getType("[D"));
                expected2 = BasicValue.INT_VALUE;
                break;
            case AALOAD:
                expected1 = newValue(Type.getType("[Ljava/lang/Object;"));
                expected2 = BasicValue.INT_VALUE;
                break;
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:
            case ISHL:
            case ISHR:
            case IUSHR:
            case IAND:
            case IOR:
            case IXOR:
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
                expected1 = BasicValue.INT_VALUE;
                expected2 = BasicValue.INT_VALUE;
                break;
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
            case FCMPL:
            case FCMPG:
                expected1 = BasicValue.FLOAT_VALUE;
                expected2 = BasicValue.FLOAT_VALUE;
                break;
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:
            case LAND:
            case LOR:
            case LXOR:
            case LCMP:
                expected1 = BasicValue.LONG_VALUE;
                expected2 = BasicValue.LONG_VALUE;
                break;
            case LSHL:
            case LSHR:
            case LUSHR:
                expected1 = BasicValue.LONG_VALUE;
                expected2 = BasicValue.INT_VALUE;
                break;
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
            case DCMPL:
            case DCMPG:
                expected1 = BasicValue.DOUBLE_VALUE;
                expected2 = BasicValue.DOUBLE_VALUE;
                break;
            case IF_ACMPEQ:
            case IF_ACMPNE:
                expected1 = BasicValue.REFERENCE_VALUE;
                expected2 = BasicValue.REFERENCE_VALUE;
                break;
            case PUTFIELD:
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                expected1 = newValue(Type.getObjectType(fieldInsn.owner));
                expected2 = newValue(Type.getType(fieldInsn.desc));
                break;
            default:
                throw new AssertionError();
        }
        if (!isSubTypeOf(value1, expected1)) {
            throw new AnalyzerException(insn, "First argument", expected1, value1);
        } else if (!isSubTypeOf(value2, expected2)) {
            throw new AnalyzerException(insn, "Second argument", expected2, value2);
        }
        if (insn.getOpcode() == AALOAD) {
            return getElementValue(value1);
        } else {
            return super.binaryOperation(insn, value1, value2);
        }
    }

    @Override
    public BasicValue ternaryOperation(
            final AbstractInsnNode insn,
            final BasicValue value1,
            final BasicValue value2,
            final BasicValue value3)
            throws AnalyzerException {
        BasicValue expected1;
        BasicValue expected3;
        switch (insn.getOpcode()) {
            case IASTORE:
                expected1 = newValue(Type.getType("[I"));
                expected3 = BasicValue.INT_VALUE;
                break;
            case BASTORE:
                if (isSubTypeOf(value1, newValue(Type.getType("[Z")))) {
                    expected1 = newValue(Type.getType("[Z"));
                } else {
                    expected1 = newValue(Type.getType("[B"));
                }
                expected3 = BasicValue.INT_VALUE;
                break;
            case CASTORE:
                expected1 = newValue(Type.getType("[C"));
                expected3 = BasicValue.INT_VALUE;
                break;
            case SASTORE:
                expected1 = newValue(Type.getType("[S"));
                expected3 = BasicValue.INT_VALUE;
                break;
            case LASTORE:
                expected1 = newValue(Type.getType("[J"));
                expected3 = BasicValue.LONG_VALUE;
                break;
            case FASTORE:
                expected1 = newValue(Type.getType("[F"));
                expected3 = BasicValue.FLOAT_VALUE;
                break;
            case DASTORE:
                expected1 = newValue(Type.getType("[D"));
                expected3 = BasicValue.DOUBLE_VALUE;
                break;
            case AASTORE:
                expected1 = value1;
                expected3 = BasicValue.REFERENCE_VALUE;
                break;
            default:
                throw new AssertionError();
        }
        if (!isSubTypeOf(value1, expected1)) {
            throw new AnalyzerException(
                    insn, "First argument", "a " + expected1 + " array reference", value1);
        } else if (!BasicValue.INT_VALUE.equals(value2)) {
            throw new AnalyzerException(insn, "Second argument", BasicValue.INT_VALUE, value2);
        } else if (!isSubTypeOf(value3, expected3)) {
            throw new AnalyzerException(insn, "Third argument", expected3, value3);
        }
        return null;
    }

    @Override
    public BasicValue naryOperation(
            final AbstractInsnNode insn, final List<? extends BasicValue> values)
            throws AnalyzerException {
        int opcode = insn.getOpcode();
        if (opcode == MULTIANEWARRAY) {
            for (BasicValue value : values) {
                if (!BasicValue.INT_VALUE.equals(value)) {
                    throw new AnalyzerException(insn, null, BasicValue.INT_VALUE, value);
                }
            }
        } else {
            int i = 0;
            int j = 0;
            if (opcode != INVOKESTATIC && opcode != INVOKEDYNAMIC) {
                Type owner = Type.getObjectType(((MethodInsnNode) insn).owner);
                if (!isSubTypeOf(values.get(i++), newValue(owner))) {
                    throw new AnalyzerException(insn, "Method owner", newValue(owner), values.get(0));
                }
            }
            String methodDescriptor =
                    (opcode == INVOKEDYNAMIC)
                            ? ((InvokeDynamicInsnNode) insn).desc
                            : ((MethodInsnNode) insn).desc;
            Type[] args = Type.getArgumentTypes(methodDescriptor);
            while (i < values.size()) {
                BasicValue expected = newValue(args[j++]);
                BasicValue actual = values.get(i++);
                if (!isSubTypeOf(actual, expected)) {
                    throw new AnalyzerException(insn, "Argument " + j, expected, actual);
                }
            }
        }
        return super.naryOperation(insn, values);
    }

    @Override
    public void returnOperation(
            final AbstractInsnNode insn, final BasicValue value, final BasicValue expected)
            throws AnalyzerException {
        if (!isSubTypeOf(value, expected)) {
            throw new AnalyzerException(insn, "Incompatible return type", expected, value);
        }
    }

    /**
      * Returns whether the given value corresponds to an array reference.
      *
      * @param value a value.
      * @return whether 'value' corresponds to an array reference.
      */
    protected boolean isArrayValue(final BasicValue value) {
        return value.isReference();
    }

    /**
      * Returns the value corresponding to the type of the elements of the given array reference value.
      *
      * @param objectArrayValue a value corresponding to array of object (or array) references.
      * @return the value corresponding to the type of the elements of 'objectArrayValue'.
      * @throws AnalyzerException if objectArrayValue does not correspond to an array type.
      */
    protected BasicValue getElementValue(final BasicValue objectArrayValue) throws AnalyzerException {
        return BasicValue.REFERENCE_VALUE;
    }

    /**
      * Returns whether the type corresponding to the first argument is a subtype of the type
      * corresponding to the second argument.
      *
      * @param value a value.
      * @param expected another value.
      * @return whether the type corresponding to 'value' is a subtype of the type corresponding to
      *     'expected'.
      */
    protected boolean isSubTypeOf(final BasicValue value, final BasicValue expected) {
        return value.equals(expected);
    }
}
