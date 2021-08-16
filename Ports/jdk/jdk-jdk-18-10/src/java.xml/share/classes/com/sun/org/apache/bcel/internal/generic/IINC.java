/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

import com.sun.org.apache.bcel.internal.util.ByteSequence;

/**
 * IINC - Increment local variable by constant
 *
 */
public class IINC extends LocalVariableInstruction {

    private boolean wide;
    private int c;


    /**
     * Empty constructor needed for Instruction.readInstruction.
     * Not to be used otherwise.
     */
    IINC() {
    }


    /**
     * @param n index of local variable
     * @param c increment factor
     */
    public IINC(final int n, final int c) {
        super(); // Default behavior of LocalVariableInstruction causes error
        super.setOpcode(com.sun.org.apache.bcel.internal.Const.IINC);
        super.setLength((short) 3);
        setIndex(n); // May set wide as side effect
        setIncrement(c);
    }


    /**
     * Dump instruction as byte code to stream out.
     * @param out Output stream
     */
    @Override
    public void dump( final DataOutputStream out ) throws IOException {
        if (wide) {
            out.writeByte(com.sun.org.apache.bcel.internal.Const.WIDE);
        }
        out.writeByte(super.getOpcode());
        if (wide) {
            out.writeShort(super.getIndex());
            out.writeShort(c);
        } else {
            out.writeByte(super.getIndex());
            out.writeByte(c);
        }
    }


    private void setWide() {
        wide = super.getIndex() > com.sun.org.apache.bcel.internal.Const.MAX_BYTE;
        if (c > 0) {
            wide = wide || (c > Byte.MAX_VALUE);
        } else {
            wide = wide || (c < Byte.MIN_VALUE);
        }
        if (wide) {
            super.setLength(6); // wide byte included
        } else {
            super.setLength(3);
        }
    }


    /**
     * Read needed data (e.g. index) from file.
     */
    @Override
    protected void initFromFile( final ByteSequence bytes, final boolean wide ) throws IOException {
        this.wide = wide;
        if (wide) {
            super.setLength(6);
            super.setIndexOnly(bytes.readUnsignedShort());
            c = bytes.readShort();
        } else {
            super.setLength(3);
            super.setIndexOnly(bytes.readUnsignedByte());
            c = bytes.readByte();
        }
    }


    /**
     * @return mnemonic for instruction
     */
    @Override
    public String toString( final boolean verbose ) {
        return super.toString(verbose) + " " + c;
    }


    /**
     * Set index of local variable.
     */
    @Override
    public final void setIndex( final int n ) {
        if (n < 0) {
            throw new ClassGenException("Negative index value: " + n);
        }
        super.setIndexOnly(n);
        setWide();
    }


    /**
     * @return increment factor
     */
    public final int getIncrement() {
        return c;
    }


    /**
     * Set increment factor.
     */
    public final void setIncrement( final int c ) {
        this.c = c;
        setWide();
    }


    /** @return int type
     */
    @Override
    public Type getType( final ConstantPoolGen cp ) {
        return Type.INT;
    }


    /**
     * Call corresponding visitor method(s). The order is:
     * Call visitor methods of implemented interfaces first, then
     * call methods according to the class hierarchy in descending order,
     * i.e., the most specific visitXXX() call comes last.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitLocalVariableInstruction(this);
        v.visitIINC(this);
    }
}
