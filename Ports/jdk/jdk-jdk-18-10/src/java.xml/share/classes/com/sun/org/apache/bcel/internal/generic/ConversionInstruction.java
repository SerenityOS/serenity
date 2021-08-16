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

import com.sun.org.apache.bcel.internal.Const;

/**
 * Super class for the x2y family of instructions.
 *
 */
public abstract class ConversionInstruction extends Instruction implements TypedInstruction,
        StackProducer, StackConsumer {

    /**
     * Empty constructor needed for Instruction.readInstruction.
     * Not to be used otherwise.
     */
    ConversionInstruction() {
    }


    /**
     * @param opcode opcode of instruction
     */
    protected ConversionInstruction(final short opcode) {
        super(opcode, (short) 1);
    }


    /** @return type associated with the instruction
     */
    @Override
    public Type getType( final ConstantPoolGen cp ) {
        final short _opcode = super.getOpcode();
        switch (_opcode) {
            case Const.D2I:
            case Const.F2I:
            case Const.L2I:
                return Type.INT;
            case Const.D2F:
            case Const.I2F:
            case Const.L2F:
                return Type.FLOAT;
            case Const.D2L:
            case Const.F2L:
            case Const.I2L:
                return Type.LONG;
            case Const.F2D:
            case Const.I2D:
            case Const.L2D:
                return Type.DOUBLE;
            case Const.I2B:
                return Type.BYTE;
            case Const.I2C:
                return Type.CHAR;
            case Const.I2S:
                return Type.SHORT;
            default: // Never reached
                throw new ClassGenException("Unknown type " + _opcode);
        }
    }
}
