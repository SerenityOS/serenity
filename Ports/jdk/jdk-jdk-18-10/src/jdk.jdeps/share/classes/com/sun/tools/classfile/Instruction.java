/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.util.Locale;

/**
 * See JVMS, chapter 6.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @see Code_attribute#getInstructions
 */
public class Instruction {
    /** The kind of an instruction, as determined by the position, size and
     *  types of its operands. */
    public static enum Kind {
        /** Opcode is not followed by any operands. */
        NO_OPERANDS(1),
        /** Opcode is followed by a byte indicating a type. */
        ATYPE(2),
        /** Opcode is followed by a 2-byte branch offset. */
        BRANCH(3),
        /** Opcode is followed by a 4-byte branch offset. */
        BRANCH_W(5),
        /** Opcode is followed by a signed byte value. */
        BYTE(2),
        /** Opcode is followed by a 1-byte index into the constant pool. */
        CPREF(2),
        /** Opcode is followed by a 2-byte index into the constant pool. */
        CPREF_W(3),
        /** Opcode is followed by a 2-byte index into the constant pool,
         *  an unsigned byte value. */
        CPREF_W_UBYTE(4),
        /** Opcode is followed by a 2-byte index into the constant pool.,
         *  an unsigned byte value, and a zero byte. */
        CPREF_W_UBYTE_ZERO(5),
        /** Opcode is followed by variable number of operands, depending
         * on the instruction.*/
        DYNAMIC(-1),
        /** Opcode is followed by a 1-byte reference to a local variable. */
        LOCAL(2),
        /** Opcode is followed by a 1-byte reference to a local variable,
         *  and a signed byte value. */
        LOCAL_BYTE(3),
        /** Opcode is followed by a signed short value. */
        SHORT(3),
        /** Wide opcode is not followed by any operands. */
        WIDE_NO_OPERANDS(2),
        /** Wide opcode is followed by a 2-byte index into the local variables array. */
        WIDE_LOCAL(4),
        /** Wide opcode is followed by a 2-byte index into the constant pool. */
        WIDE_CPREF_W(4),
        /** Wide opcode is followed by a 2-byte index into the constant pool,
         *  and a signed short value. */
        WIDE_CPREF_W_SHORT(6),
        /** Wide opcode is followed by a 2-byte reference to a local variable,
         *  and a signed short value. */
        WIDE_LOCAL_SHORT(6),
        /** Opcode was not recognized. */
        UNKNOWN(1);

        Kind(int length) {
            this.length = length;
        }

        /** The length, in bytes, of this kind of instruction, or -1 is the
         *  length depends on the specific instruction. */
        public final int length;
    }

    /** A utility visitor to help decode the operands of an instruction.
     *  @see Instruction#accept */
    public interface KindVisitor<R,P> {
        /** See {@link Kind#NO_OPERANDS}, {@link Kind#WIDE_NO_OPERANDS}. */
        R visitNoOperands(Instruction instr, P p);
        /** See {@link Kind#ATYPE}. */
        R visitArrayType(Instruction instr, TypeKind kind, P p);
        /** See {@link Kind#BRANCH}, {@link Kind#BRANCH_W}. */
        R visitBranch(Instruction instr, int offset, P p);
        /** See {@link Kind#CPREF}, {@link Kind#CPREF_W}, {@link Kind#WIDE_CPREF_W}. */
        R visitConstantPoolRef(Instruction instr, int index, P p);
        /** See {@link Kind#CPREF_W_UBYTE}, {@link Kind#CPREF_W_UBYTE_ZERO}, {@link Kind#WIDE_CPREF_W_SHORT}. */
        R visitConstantPoolRefAndValue(Instruction instr, int index, int value, P p);
        /** See {@link Kind#LOCAL}, {@link Kind#WIDE_LOCAL}. */
        R visitLocal(Instruction instr, int index, P p);
        /** See {@link Kind#LOCAL_BYTE}. */
        R visitLocalAndValue(Instruction instr, int index, int value, P p);
        /** See {@link Kind#DYNAMIC}. */
        R visitLookupSwitch(Instruction instr, int default_, int npairs, int[] matches, int[] offsets, P p);
        /** See {@link Kind#DYNAMIC}. */
        R visitTableSwitch(Instruction instr, int default_, int low, int high, int[] offsets, P p);
        /** See {@link Kind#BYTE}, {@link Kind#SHORT}. */
        R visitValue(Instruction instr, int value, P p);
        /** Instruction is unrecognized. */
        R visitUnknown(Instruction instr, P p);

    }

    /** The kind of primitive array type to create.
     *  See JVMS chapter 6, newarray. */
    public static enum TypeKind {
        T_BOOLEAN(4, "boolean"),
        T_CHAR(5, "char"),
        T_FLOAT(6, "float"),
        T_DOUBLE(7, "double"),
        T_BYTE(8, "byte"),
        T_SHORT(9, "short"),
        T_INT (10, "int"),
        T_LONG (11, "long");
        TypeKind(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public static TypeKind get(int value) {
            switch (value) {
                case  4: return T_BOOLEAN;
                case  5: return T_CHAR;
                case  6: return T_FLOAT;
                case  7: return T_DOUBLE;
                case  8: return T_BYTE;
                case  9: return T_SHORT;
                case  10: return T_INT;
                case  11: return T_LONG;
                default: return null;
            }
        }

        public final int value;
        public final String name;
    }

    /** An instruction is defined by its position in a bytecode array. */
    public Instruction(byte[] bytes, int pc) {
        this.bytes = bytes;
        this.pc = pc;
    }

    /** Get the position of the instruction within the bytecode array. */
    public int getPC() {
        return pc;
    }

    /** Get a byte value, relative to the start of this instruction. */
    public int getByte(int offset) {
        return bytes[pc + offset];
    }

    /** Get an unsigned byte value, relative to the start of this instruction. */
    public int getUnsignedByte(int offset) {
        return getByte(offset) & 0xff;
    }

    /** Get a 2-byte value, relative to the start of this instruction. */
    public int getShort(int offset) {
        return (getByte(offset) << 8) | getUnsignedByte(offset + 1);
    }

    /** Get a unsigned 2-byte value, relative to the start of this instruction. */
    public int getUnsignedShort(int offset) {
        return getShort(offset) & 0xFFFF;
    }

    /** Get a 4-byte value, relative to the start of this instruction. */
    public int getInt(int offset) {
        return (getShort(offset) << 16) | (getUnsignedShort(offset + 2));
    }

    /** Get the Opcode for this instruction, or null if the instruction is
     * unrecognized. */
    public Opcode getOpcode() {
        int b = getUnsignedByte(0);
        switch (b) {
            case Opcode.NONPRIV:
            case Opcode.PRIV:
            case Opcode.WIDE:
                return Opcode.get(b, getUnsignedByte(1));
        }
        return Opcode.get(b);
    }

    /** Get the mnemonic for this instruction, or a default string if the
     * instruction is unrecognized. */
    public String getMnemonic() {
        Opcode opcode = getOpcode();
        if (opcode == null)
            return "bytecode " + getUnsignedByte(0);
        else
            return opcode.toString().toLowerCase(Locale.US);
    }

    /** Get the length, in bytes, of this instruction, including the opcode
     * and all its operands. */
    public int length() {
        Opcode opcode = getOpcode();
        if (opcode == null)
            return 1;

        switch (opcode) {
            case TABLESWITCH: {
                int pad = align(pc + 1) - pc;
                int low = getInt(pad + 4);
                int high = getInt(pad + 8);
                return pad + 12 + 4 * (high - low + 1);
            }
            case LOOKUPSWITCH: {
                int pad = align(pc + 1) - pc;
                int npairs = getInt(pad + 4);
                return pad + 8 + 8 * npairs;

            }
            default:
                return opcode.kind.length;
        }
    }

    /** Get the {@link Kind} of this instruction. */
    public Kind getKind() {
        Opcode opcode = getOpcode();
        return (opcode != null ? opcode.kind : Kind.UNKNOWN);
    }

    /** Invoke a method on the visitor according to the kind of this
     * instruction, passing in the decoded operands for the instruction. */
    public <R,P> R accept(KindVisitor<R,P> visitor, P p) {
        switch (getKind()) {
            case NO_OPERANDS:
                return visitor.visitNoOperands(this, p);

            case ATYPE:
                return visitor.visitArrayType(
                        this, TypeKind.get(getUnsignedByte(1)), p);

            case BRANCH:
                return visitor.visitBranch(this, getShort(1), p);

            case BRANCH_W:
                return visitor.visitBranch(this, getInt(1), p);

            case BYTE:
                return visitor.visitValue(this, getByte(1), p);

            case CPREF:
                return visitor.visitConstantPoolRef(this, getUnsignedByte(1), p);

            case CPREF_W:
                return visitor.visitConstantPoolRef(this, getUnsignedShort(1), p);

            case CPREF_W_UBYTE:
            case CPREF_W_UBYTE_ZERO:
                return visitor.visitConstantPoolRefAndValue(
                        this, getUnsignedShort(1), getUnsignedByte(3), p);

            case DYNAMIC: {
                switch (getOpcode()) {
                    case TABLESWITCH: {
                        int pad = align(pc + 1) - pc;
                        int default_ = getInt(pad);
                        int low = getInt(pad + 4);
                        int high = getInt(pad + 8);
                        if (low > high)
                            throw new IllegalStateException();
                        int[] values = new int[high - low + 1];
                        for (int i = 0; i < values.length; i++)
                            values[i] = getInt(pad + 12 + 4 * i);
                        return visitor.visitTableSwitch(
                                this, default_, low, high, values, p);
                    }
                    case LOOKUPSWITCH: {
                        int pad = align(pc + 1) - pc;
                        int default_ = getInt(pad);
                        int npairs = getInt(pad + 4);
                        if (npairs < 0)
                            throw new IllegalStateException();
                        int[] matches = new int[npairs];
                        int[] offsets = new int[npairs];
                        for (int i = 0; i < npairs; i++) {
                            matches[i] = getInt(pad +  8 + i * 8);
                            offsets[i] = getInt(pad + 12 + i * 8);
                        }
                        return visitor.visitLookupSwitch(
                                this, default_, npairs, matches, offsets, p);
                    }
                    default:
                        throw new IllegalStateException();
                }
            }

            case LOCAL:
                return visitor.visitLocal(this, getUnsignedByte(1), p);

            case LOCAL_BYTE:
                return visitor.visitLocalAndValue(
                        this, getUnsignedByte(1), getByte(2), p);

            case SHORT:
                return visitor.visitValue(this, getShort(1), p);

            case WIDE_NO_OPERANDS:
                return visitor.visitNoOperands(this, p);

            case WIDE_LOCAL:
                return visitor.visitLocal(this, getUnsignedShort(2), p);

            case WIDE_CPREF_W:
                return visitor.visitConstantPoolRef(this, getUnsignedShort(2), p);

            case WIDE_CPREF_W_SHORT:
                return visitor.visitConstantPoolRefAndValue(
                        this, getUnsignedShort(2), getUnsignedByte(4), p);

            case WIDE_LOCAL_SHORT:
                return visitor.visitLocalAndValue(
                        this, getUnsignedShort(2), getShort(4), p);

            case UNKNOWN:
                return visitor.visitUnknown(this, p);

            default:
                throw new IllegalStateException();
        }
    }

    private static int align(int n) {
        return (n + 3) & ~3;
    }

    private byte[] bytes;
    private int pc;
}
