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
 * Super class for the family of arithmetic instructions.
 *
 */
public abstract class ArithmeticInstruction extends Instruction implements TypedInstruction,
        StackProducer, StackConsumer {

    /**
     * Empty constructor needed for Instruction.readInstruction.
     * Not to be used otherwise.
     */
    ArithmeticInstruction() {
    }


    /**
     * @param opcode of instruction
     */
    protected ArithmeticInstruction(final short opcode) {
        super(opcode, (short) 1);
    }


    /** @return type associated with the instruction
     */
    @Override
    public Type getType( final ConstantPoolGen cp ) {
        final short _opcode = super.getOpcode();
        switch (_opcode) {
            case Const.DADD:
            case Const.DDIV:
            case Const.DMUL:
            case Const.DNEG:
            case Const.DREM:
            case Const.DSUB:
                return Type.DOUBLE;
            case Const.FADD:
            case Const.FDIV:
            case Const.FMUL:
            case Const.FNEG:
            case Const.FREM:
            case Const.FSUB:
                return Type.FLOAT;
            case Const.IADD:
            case Const.IAND:
            case Const.IDIV:
            case Const.IMUL:
            case Const.INEG:
            case Const.IOR:
            case Const.IREM:
            case Const.ISHL:
            case Const.ISHR:
            case Const.ISUB:
            case Const.IUSHR:
            case Const.IXOR:
                return Type.INT;
            case Const.LADD:
            case Const.LAND:
            case Const.LDIV:
            case Const.LMUL:
            case Const.LNEG:
            case Const.LOR:
            case Const.LREM:
            case Const.LSHL:
            case Const.LSHR:
            case Const.LSUB:
            case Const.LUSHR:
            case Const.LXOR:
                return Type.LONG;
            default: // Never reached
                throw new ClassGenException("Unknown type " + _opcode);
        }
    }
}
