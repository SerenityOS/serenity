/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * BIPUSH - Push byte on stack
 *
 * <PRE>Stack: ... -&gt; ..., value</PRE>
 *
 */
public class BIPUSH extends Instruction implements ConstantPushInstruction {

    private byte b;


    /**
     * Empty constructor needed for Instruction.readInstruction.
     * Not to be used otherwise.
     */
    BIPUSH() {
    }


    /** Push byte on stack
     */
    public BIPUSH(final byte b) {
        super(com.sun.org.apache.bcel.internal.Const.BIPUSH, (short) 2);
        this.b = b;
    }


    /**
     * Dump instruction as byte code to stream out.
     */
    @Override
    public void dump( final DataOutputStream out ) throws IOException {
        super.dump(out);
        out.writeByte(b);
    }


    /**
     * @return mnemonic for instruction
     */
    @Override
    public String toString( final boolean verbose ) {
        return super.toString(verbose) + " " + b;
    }


    /**
     * Read needed data (e.g. index) from file.
     */
    @Override
    protected void initFromFile( final ByteSequence bytes, final boolean wide ) throws IOException {
        super.setLength(2);
        b = bytes.readByte();
    }


    @Override
    public Number getValue() {
        return Integer.valueOf(b);
    }


    /** @return Type.BYTE
     */
    @Override
    public Type getType( final ConstantPoolGen cp ) {
        return Type.BYTE;
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
        v.visitPushInstruction(this);
        v.visitStackProducer(this);
        v.visitTypedInstruction(this);
        v.visitConstantPushInstruction(this);
        v.visitBIPUSH(this);
    }
}
