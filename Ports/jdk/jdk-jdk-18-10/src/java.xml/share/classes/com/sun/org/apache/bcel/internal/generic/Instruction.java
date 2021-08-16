/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.org.apache.bcel.internal.generic;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.classfile.ConstantPool;
import com.sun.org.apache.bcel.internal.util.ByteSequence;

/**
 * Abstract super class for all Java byte codes.
 *
 * @LastModified: July 2020
 */
public abstract class Instruction implements Cloneable {

    private short length = 1; // Length of instruction in bytes
    private short opcode = -1; // Opcode number

    private static InstructionComparator cmp = InstructionComparator.DEFAULT;


    /**
     * Empty constructor needed for Instruction.readInstruction.
     * Not to be used otherwise.
     */
    Instruction() {
    }


    public Instruction(final short opcode, final short length) {
        this.length = length;
        this.opcode = opcode;
    }


    /**
     * Dump instruction as byte code to stream out.
     * @param out Output stream
     */
    public void dump( final DataOutputStream out ) throws IOException {
        out.writeByte(opcode); // Common for all instructions
    }


    /** @return name of instruction, i.e., opcode name
     */
    public String getName() {
        return Const.getOpcodeName(opcode);
    }


    /**
     * Long output format:
     *
     * &lt;name of opcode&gt; "["&lt;opcode number&gt;"]"
     * "("&lt;length of instruction&gt;")"
     *
     * @param verbose long/short format switch
     * @return mnemonic for instruction
     */
    public String toString( final boolean verbose ) {
        if (verbose) {
            return getName() + "[" + opcode + "](" + length + ")";
        }
        return getName();
    }


    /**
     * @return mnemonic for instruction in verbose format
     */
    @Override
    public String toString() {
        return toString(true);
    }


    /**
     * @return mnemonic for instruction with sumbolic references resolved
     */
    public String toString( final ConstantPool cp ) {
        return toString(false);
    }


    /**
     * Use with caution, since `BranchInstruction's have a `target' reference which
     * is not copied correctly (only basic types are). This also applies for
     * `Select' instructions with their multiple branch targets.
     *
     * @see BranchInstruction
     * @return (shallow) copy of an instruction
     */
    public Instruction copy() {
        Instruction i = null;
        // "Constant" instruction, no need to duplicate
        if (InstructionConst.getInstruction(this.getOpcode()) != null) {
            i = this;
        } else {
            try {
                i = (Instruction) clone();
            } catch (final CloneNotSupportedException e) {
                System.err.println(e);
            }
        }
        return i;
    }


    /**
     * Read needed data (e.g. index) from file.
     *
     * @param bytes byte sequence to read from
     * @param wide "wide" instruction flag
     * @throws IOException may be thrown if the implementation needs to read data from the file
     */
    protected void initFromFile( final ByteSequence bytes, final boolean wide ) throws IOException {
    }


    /**
     * Read an instruction from (byte code) input stream and return the
     * appropiate object.
     * <p>
     * If the Instruction is defined in {@link InstructionConst}, then the
     * singleton instance is returned.
     * @param bytes input stream bytes
     * @return instruction object being read
     * @see InstructionConst#getInstruction(int)
     */
    // @since 6.0 no longer final
    public static Instruction readInstruction( final ByteSequence bytes ) throws IOException {
        boolean wide = false;
        short opcode = (short) bytes.readUnsignedByte();
        Instruction obj = null;
        if (opcode == Const.WIDE) { // Read next opcode after wide byte
            wide = true;
            opcode = (short) bytes.readUnsignedByte();
        }
        final Instruction instruction = InstructionConst.getInstruction(opcode);
        if (instruction != null) {
            return instruction; // Used predefined immutable object, if available
        }

        switch (opcode) {
            case Const.BIPUSH:
                obj = new BIPUSH();
                break;
            case Const.SIPUSH:
                obj = new SIPUSH();
                break;
            case Const.LDC:
                obj = new LDC();
                break;
            case Const.LDC_W:
                obj = new LDC_W();
                break;
            case Const.LDC2_W:
                obj = new LDC2_W();
                break;
            case Const.ILOAD:
                obj = new ILOAD();
                break;
            case Const.LLOAD:
                obj = new LLOAD();
                break;
            case Const.FLOAD:
                obj = new FLOAD();
                break;
            case Const.DLOAD:
                obj = new DLOAD();
                break;
            case Const.ALOAD:
                obj = new ALOAD();
                break;
            case Const.ILOAD_0:
                obj = new ILOAD(0);
                break;
            case Const.ILOAD_1:
                obj = new ILOAD(1);
                break;
            case Const.ILOAD_2:
                obj = new ILOAD(2);
                break;
            case Const.ILOAD_3:
                obj = new ILOAD(3);
                break;
            case Const.LLOAD_0:
                obj = new LLOAD(0);
                break;
            case Const.LLOAD_1:
                obj = new LLOAD(1);
                break;
            case Const.LLOAD_2:
                obj = new LLOAD(2);
                break;
            case Const.LLOAD_3:
                obj = new LLOAD(3);
                break;
            case Const.FLOAD_0:
                obj = new FLOAD(0);
                break;
            case Const.FLOAD_1:
                obj = new FLOAD(1);
                break;
            case Const.FLOAD_2:
                obj = new FLOAD(2);
                break;
            case Const.FLOAD_3:
                obj = new FLOAD(3);
                break;
            case Const.DLOAD_0:
                obj = new DLOAD(0);
                break;
            case Const.DLOAD_1:
                obj = new DLOAD(1);
                break;
            case Const.DLOAD_2:
                obj = new DLOAD(2);
                break;
            case Const.DLOAD_3:
                obj = new DLOAD(3);
                break;
            case Const.ALOAD_0:
                obj = new ALOAD(0);
                break;
            case Const.ALOAD_1:
                obj = new ALOAD(1);
                break;
            case Const.ALOAD_2:
                obj = new ALOAD(2);
                break;
            case Const.ALOAD_3:
                obj = new ALOAD(3);
                break;
            case Const.ISTORE:
                obj = new ISTORE();
                break;
            case Const.LSTORE:
                obj = new LSTORE();
                break;
            case Const.FSTORE:
                obj = new FSTORE();
                break;
            case Const.DSTORE:
                obj = new DSTORE();
                break;
            case Const.ASTORE:
                obj = new ASTORE();
                break;
            case Const.ISTORE_0:
                obj = new ISTORE(0);
                break;
            case Const.ISTORE_1:
                obj = new ISTORE(1);
                break;
            case Const.ISTORE_2:
                obj = new ISTORE(2);
                break;
            case Const.ISTORE_3:
                obj = new ISTORE(3);
                break;
            case Const.LSTORE_0:
                obj = new LSTORE(0);
                break;
            case Const.LSTORE_1:
                obj = new LSTORE(1);
                break;
            case Const.LSTORE_2:
                obj = new LSTORE(2);
                break;
            case Const.LSTORE_3:
                obj = new LSTORE(3);
                break;
            case Const.FSTORE_0:
                obj = new FSTORE(0);
                break;
            case Const.FSTORE_1:
                obj = new FSTORE(1);
                break;
            case Const.FSTORE_2:
                obj = new FSTORE(2);
                break;
            case Const.FSTORE_3:
                obj = new FSTORE(3);
                break;
            case Const.DSTORE_0:
                obj = new DSTORE(0);
                break;
            case Const.DSTORE_1:
                obj = new DSTORE(1);
                break;
            case Const.DSTORE_2:
                obj = new DSTORE(2);
                break;
            case Const.DSTORE_3:
                obj = new DSTORE(3);
                break;
            case Const.ASTORE_0:
                obj = new ASTORE(0);
                break;
            case Const.ASTORE_1:
                obj = new ASTORE(1);
                break;
            case Const.ASTORE_2:
                obj = new ASTORE(2);
                break;
            case Const.ASTORE_3:
                obj = new ASTORE(3);
                break;
            case Const.IINC:
                obj = new IINC();
                break;
            case Const.IFEQ:
                obj = new IFEQ();
                break;
            case Const.IFNE:
                obj = new IFNE();
                break;
            case Const.IFLT:
                obj = new IFLT();
                break;
            case Const.IFGE:
                obj = new IFGE();
                break;
            case Const.IFGT:
                obj = new IFGT();
                break;
            case Const.IFLE:
                obj = new IFLE();
                break;
            case Const.IF_ICMPEQ:
                obj = new IF_ICMPEQ();
                break;
            case Const.IF_ICMPNE:
                obj = new IF_ICMPNE();
                break;
            case Const.IF_ICMPLT:
                obj = new IF_ICMPLT();
                break;
            case Const.IF_ICMPGE:
                obj = new IF_ICMPGE();
                break;
            case Const.IF_ICMPGT:
                obj = new IF_ICMPGT();
                break;
            case Const.IF_ICMPLE:
                obj = new IF_ICMPLE();
                break;
            case Const.IF_ACMPEQ:
                obj = new IF_ACMPEQ();
                break;
            case Const.IF_ACMPNE:
                obj = new IF_ACMPNE();
                break;
            case Const.GOTO:
                obj = new GOTO();
                break;
            case Const.JSR:
                obj = new JSR();
                break;
            case Const.RET:
                obj = new RET();
                break;
            case Const.TABLESWITCH:
                obj = new TABLESWITCH();
                break;
            case Const.LOOKUPSWITCH:
                obj = new LOOKUPSWITCH();
                break;
            case Const.GETSTATIC:
                obj = new GETSTATIC();
                break;
            case Const.PUTSTATIC:
                obj = new PUTSTATIC();
                break;
            case Const.GETFIELD:
                obj = new GETFIELD();
                break;
            case Const.PUTFIELD:
                obj = new PUTFIELD();
                break;
            case Const.INVOKEVIRTUAL:
                obj = new INVOKEVIRTUAL();
                break;
            case Const.INVOKESPECIAL:
                obj = new INVOKESPECIAL();
                break;
            case Const.INVOKESTATIC:
                obj = new INVOKESTATIC();
                break;
            case Const.INVOKEINTERFACE:
                obj = new INVOKEINTERFACE();
                break;
            case Const.INVOKEDYNAMIC:
                obj = new INVOKEDYNAMIC();
                break;
            case Const.NEW:
                obj = new NEW();
                break;
            case Const.NEWARRAY:
                obj = new NEWARRAY();
                break;
            case Const.ANEWARRAY:
                obj = new ANEWARRAY();
                break;
            case Const.CHECKCAST:
                obj = new CHECKCAST();
                break;
            case Const.INSTANCEOF:
                obj = new INSTANCEOF();
                break;
            case Const.MULTIANEWARRAY:
                obj = new MULTIANEWARRAY();
                break;
            case Const.IFNULL:
                obj = new IFNULL();
                break;
            case Const.IFNONNULL:
                obj = new IFNONNULL();
                break;
            case Const.GOTO_W:
                obj = new GOTO_W();
                break;
            case Const.JSR_W:
                obj = new JSR_W();
                break;
            case Const.BREAKPOINT:
                obj = new BREAKPOINT();
                break;
            case Const.IMPDEP1:
                obj = new IMPDEP1();
                break;
            case Const.IMPDEP2:
                obj = new IMPDEP2();
                break;
            default:
                throw new ClassGenException("Illegal opcode detected: " + opcode);

        }

        if (wide
                && !((obj instanceof LocalVariableInstruction) || (obj instanceof IINC) || (obj instanceof RET))) {
            throw new ClassGenException("Illegal opcode after wide: " + opcode);
        }
        obj.setOpcode(opcode);
        obj.initFromFile(bytes, wide); // Do further initializations, if any
        return obj;
    }

    /**
     * This method also gives right results for instructions whose
     * effect on the stack depends on the constant pool entry they
     * reference.
     *  @return Number of words consumed from stack by this instruction,
     * or Constants.UNPREDICTABLE, if this can not be computed statically
     */
    public int consumeStack( final ConstantPoolGen cpg ) {
        return Const.getConsumeStack(opcode);
    }


    /**
     * This method also gives right results for instructions whose
     * effect on the stack depends on the constant pool entry they
     * reference.
     * @return Number of words produced onto stack by this instruction,
     * or Constants.UNPREDICTABLE, if this can not be computed statically
     */
    public int produceStack( final ConstantPoolGen cpg ) {
        return Const.getProduceStack(opcode);
    }


    /**
     * @return this instructions opcode
     */
    public short getOpcode() {
        return opcode;
    }


    /**
     * @return length (in bytes) of instruction
     */
    public int getLength() {
        return length;
    }


    /**
     * Needed in readInstruction and subclasses in this package
     */
    void setOpcode( final short opcode ) {
        this.opcode = opcode;
    }


    /**
     * Needed in readInstruction and subclasses in this package
     * @since 6.0
     */
    final void setLength( final int length ) {
        this.length = (short) length; // TODO check range?
    }


    /** Some instructions may be reused, so don't do anything by default.
     */
    void dispose() {
    }


    /**
     * Call corresponding visitor method(s). The order is:
     * Call visitor methods of implemented interfaces first, then
     * call methods according to the class hierarchy in descending order,
     * i.e., the most specific visitXXX() call comes last.
     *
     * @param v Visitor object
     */
    public abstract void accept( Visitor v );


    /** Get Comparator object used in the equals() method to determine
     * equality of instructions.
     *
     * @return currently used comparator for equals()
     * @deprecated (6.0) use the built in comparator, or wrap this class in another object that implements these methods
     */
    @Deprecated
    public static InstructionComparator getComparator() {
        return cmp;
    }


    /** Set comparator to be used for equals().
      * @deprecated (6.0) use the built in comparator, or wrap this class in another object that implements these methods
     */
    @Deprecated
    public static void setComparator( final InstructionComparator c ) {
        cmp = c;
    }


    /** Check for equality, delegated to comparator
     * @return true if that is an Instruction and has the same opcode
     */
    @Override
    public boolean equals( final Object that ) {
        return (that instanceof Instruction) ? cmp.equals(this, (Instruction) that) : false;
    }

    /** calculate the hashCode of this object
     * @return the hashCode
     * @since 6.0
     */
    @Override
    public int hashCode() {
        return opcode;
    }

    /**
     * Check if the value can fit in a byte (signed)
     * @param value the value to check
     * @return true if the value is in range
     * @since 6.0
     */
    public static boolean isValidByte(final int value) {
        return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
    }

    /**
     * Check if the value can fit in a short (signed)
     * @param value the value to check
     * @return true if the value is in range
     * @since 6.0
     */
    public static boolean isValidShort(final int value) {
        return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
    }
}
