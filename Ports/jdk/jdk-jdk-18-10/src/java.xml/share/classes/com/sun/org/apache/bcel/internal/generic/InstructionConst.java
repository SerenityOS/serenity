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
 * This interface contains shareable instruction objects.
 *
 * In order to save memory you can use some instructions multiply,
 * since they have an immutable state and are directly derived from
 * Instruction.  I.e. they have no instance fields that could be
 * changed. Since some of these instructions like ICONST_0 occur
 * very frequently this can save a lot of time and space. This
 * feature is an adaptation of the FlyWeight design pattern, we
 * just use an array instead of a factory.
 *
 * The Instructions can also accessed directly under their names, so
 * it's possible to write il.append(Instruction.ICONST_0);
 *
 */
public final class InstructionConst {

    /**
     * Predefined instruction objects
     */
    /*
     * NOTE these are not currently immutable, because Instruction
     * has mutable protected fields opcode and length.
     */
    public static final Instruction NOP = new NOP();
    public static final Instruction ACONST_NULL = new ACONST_NULL();
    public static final Instruction ICONST_M1 = new ICONST(-1);
    public static final Instruction ICONST_0 = new ICONST(0);
    public static final Instruction ICONST_1 = new ICONST(1);
    public static final Instruction ICONST_2 = new ICONST(2);
    public static final Instruction ICONST_3 = new ICONST(3);
    public static final Instruction ICONST_4 = new ICONST(4);
    public static final Instruction ICONST_5 = new ICONST(5);
    public static final Instruction LCONST_0 = new LCONST(0);
    public static final Instruction LCONST_1 = new LCONST(1);
    public static final Instruction FCONST_0 = new FCONST(0);
    public static final Instruction FCONST_1 = new FCONST(1);
    public static final Instruction FCONST_2 = new FCONST(2);
    public static final Instruction DCONST_0 = new DCONST(0);
    public static final Instruction DCONST_1 = new DCONST(1);
    public static final ArrayInstruction IALOAD = new IALOAD();
    public static final ArrayInstruction LALOAD = new LALOAD();
    public static final ArrayInstruction FALOAD = new FALOAD();
    public static final ArrayInstruction DALOAD = new DALOAD();
    public static final ArrayInstruction AALOAD = new AALOAD();
    public static final ArrayInstruction BALOAD = new BALOAD();
    public static final ArrayInstruction CALOAD = new CALOAD();
    public static final ArrayInstruction SALOAD = new SALOAD();
    public static final ArrayInstruction IASTORE = new IASTORE();
    public static final ArrayInstruction LASTORE = new LASTORE();
    public static final ArrayInstruction FASTORE = new FASTORE();
    public static final ArrayInstruction DASTORE = new DASTORE();
    public static final ArrayInstruction AASTORE = new AASTORE();
    public static final ArrayInstruction BASTORE = new BASTORE();
    public static final ArrayInstruction CASTORE = new CASTORE();
    public static final ArrayInstruction SASTORE = new SASTORE();
    public static final StackInstruction POP = new POP();
    public static final StackInstruction POP2 = new POP2();
    public static final StackInstruction DUP = new DUP();
    public static final StackInstruction DUP_X1 = new DUP_X1();
    public static final StackInstruction DUP_X2 = new DUP_X2();
    public static final StackInstruction DUP2 = new DUP2();
    public static final StackInstruction DUP2_X1 = new DUP2_X1();
    public static final StackInstruction DUP2_X2 = new DUP2_X2();
    public static final StackInstruction SWAP = new SWAP();
    public static final ArithmeticInstruction IADD = new IADD();
    public static final ArithmeticInstruction LADD = new LADD();
    public static final ArithmeticInstruction FADD = new FADD();
    public static final ArithmeticInstruction DADD = new DADD();
    public static final ArithmeticInstruction ISUB = new ISUB();
    public static final ArithmeticInstruction LSUB = new LSUB();
    public static final ArithmeticInstruction FSUB = new FSUB();
    public static final ArithmeticInstruction DSUB = new DSUB();
    public static final ArithmeticInstruction IMUL = new IMUL();
    public static final ArithmeticInstruction LMUL = new LMUL();
    public static final ArithmeticInstruction FMUL = new FMUL();
    public static final ArithmeticInstruction DMUL = new DMUL();
    public static final ArithmeticInstruction IDIV = new IDIV();
    public static final ArithmeticInstruction LDIV = new LDIV();
    public static final ArithmeticInstruction FDIV = new FDIV();
    public static final ArithmeticInstruction DDIV = new DDIV();
    public static final ArithmeticInstruction IREM = new IREM();
    public static final ArithmeticInstruction LREM = new LREM();
    public static final ArithmeticInstruction FREM = new FREM();
    public static final ArithmeticInstruction DREM = new DREM();
    public static final ArithmeticInstruction INEG = new INEG();
    public static final ArithmeticInstruction LNEG = new LNEG();
    public static final ArithmeticInstruction FNEG = new FNEG();
    public static final ArithmeticInstruction DNEG = new DNEG();
    public static final ArithmeticInstruction ISHL = new ISHL();
    public static final ArithmeticInstruction LSHL = new LSHL();
    public static final ArithmeticInstruction ISHR = new ISHR();
    public static final ArithmeticInstruction LSHR = new LSHR();
    public static final ArithmeticInstruction IUSHR = new IUSHR();
    public static final ArithmeticInstruction LUSHR = new LUSHR();
    public static final ArithmeticInstruction IAND = new IAND();
    public static final ArithmeticInstruction LAND = new LAND();
    public static final ArithmeticInstruction IOR = new IOR();
    public static final ArithmeticInstruction LOR = new LOR();
    public static final ArithmeticInstruction IXOR = new IXOR();
    public static final ArithmeticInstruction LXOR = new LXOR();
    public static final ConversionInstruction I2L = new I2L();
    public static final ConversionInstruction I2F = new I2F();
    public static final ConversionInstruction I2D = new I2D();
    public static final ConversionInstruction L2I = new L2I();
    public static final ConversionInstruction L2F = new L2F();
    public static final ConversionInstruction L2D = new L2D();
    public static final ConversionInstruction F2I = new F2I();
    public static final ConversionInstruction F2L = new F2L();
    public static final ConversionInstruction F2D = new F2D();
    public static final ConversionInstruction D2I = new D2I();
    public static final ConversionInstruction D2L = new D2L();
    public static final ConversionInstruction D2F = new D2F();
    public static final ConversionInstruction I2B = new I2B();
    public static final ConversionInstruction I2C = new I2C();
    public static final ConversionInstruction I2S = new I2S();
    public static final Instruction LCMP = new LCMP();
    public static final Instruction FCMPL = new FCMPL();
    public static final Instruction FCMPG = new FCMPG();
    public static final Instruction DCMPL = new DCMPL();
    public static final Instruction DCMPG = new DCMPG();
    public static final ReturnInstruction IRETURN = new IRETURN();
    public static final ReturnInstruction LRETURN = new LRETURN();
    public static final ReturnInstruction FRETURN = new FRETURN();
    public static final ReturnInstruction DRETURN = new DRETURN();
    public static final ReturnInstruction ARETURN = new ARETURN();
    public static final ReturnInstruction RETURN = new RETURN();
    public static final Instruction ARRAYLENGTH = new ARRAYLENGTH();
    public static final Instruction ATHROW = new ATHROW();
    public static final Instruction MONITORENTER = new MONITORENTER();
    public static final Instruction MONITOREXIT = new MONITOREXIT();

    /** You can use these constants in multiple places safely, if you can guarantee
     * that you will never alter their internal values, e.g. call setIndex().
     */
    public static final LocalVariableInstruction THIS = new ALOAD(0);
    public static final LocalVariableInstruction ALOAD_0 = THIS;
    public static final LocalVariableInstruction ALOAD_1 = new ALOAD(1);
    public static final LocalVariableInstruction ALOAD_2 = new ALOAD(2);
    public static final LocalVariableInstruction ILOAD_0 = new ILOAD(0);
    public static final LocalVariableInstruction ILOAD_1 = new ILOAD(1);
    public static final LocalVariableInstruction ILOAD_2 = new ILOAD(2);
    public static final LocalVariableInstruction ASTORE_0 = new ASTORE(0);
    public static final LocalVariableInstruction ASTORE_1 = new ASTORE(1);
    public static final LocalVariableInstruction ASTORE_2 = new ASTORE(2);
    public static final LocalVariableInstruction ISTORE_0 = new ISTORE(0);
    public static final LocalVariableInstruction ISTORE_1 = new ISTORE(1);
    public static final LocalVariableInstruction ISTORE_2 = new ISTORE(2);

    /** Get object via its opcode, for immutable instructions like
     * branch instructions entries are set to null.
     */
    private static final Instruction[] INSTRUCTIONS = new Instruction[256];

    static {
        INSTRUCTIONS[Const.NOP] = NOP;
        INSTRUCTIONS[Const.ACONST_NULL] = ACONST_NULL;
        INSTRUCTIONS[Const.ICONST_M1] = ICONST_M1;
        INSTRUCTIONS[Const.ICONST_0] = ICONST_0;
        INSTRUCTIONS[Const.ICONST_1] = ICONST_1;
        INSTRUCTIONS[Const.ICONST_2] = ICONST_2;
        INSTRUCTIONS[Const.ICONST_3] = ICONST_3;
        INSTRUCTIONS[Const.ICONST_4] = ICONST_4;
        INSTRUCTIONS[Const.ICONST_5] = ICONST_5;
        INSTRUCTIONS[Const.LCONST_0] = LCONST_0;
        INSTRUCTIONS[Const.LCONST_1] = LCONST_1;
        INSTRUCTIONS[Const.FCONST_0] = FCONST_0;
        INSTRUCTIONS[Const.FCONST_1] = FCONST_1;
        INSTRUCTIONS[Const.FCONST_2] = FCONST_2;
        INSTRUCTIONS[Const.DCONST_0] = DCONST_0;
        INSTRUCTIONS[Const.DCONST_1] = DCONST_1;
        INSTRUCTIONS[Const.IALOAD] = IALOAD;
        INSTRUCTIONS[Const.LALOAD] = LALOAD;
        INSTRUCTIONS[Const.FALOAD] = FALOAD;
        INSTRUCTIONS[Const.DALOAD] = DALOAD;
        INSTRUCTIONS[Const.AALOAD] = AALOAD;
        INSTRUCTIONS[Const.BALOAD] = BALOAD;
        INSTRUCTIONS[Const.CALOAD] = CALOAD;
        INSTRUCTIONS[Const.SALOAD] = SALOAD;
        INSTRUCTIONS[Const.IASTORE] = IASTORE;
        INSTRUCTIONS[Const.LASTORE] = LASTORE;
        INSTRUCTIONS[Const.FASTORE] = FASTORE;
        INSTRUCTIONS[Const.DASTORE] = DASTORE;
        INSTRUCTIONS[Const.AASTORE] = AASTORE;
        INSTRUCTIONS[Const.BASTORE] = BASTORE;
        INSTRUCTIONS[Const.CASTORE] = CASTORE;
        INSTRUCTIONS[Const.SASTORE] = SASTORE;
        INSTRUCTIONS[Const.POP] = POP;
        INSTRUCTIONS[Const.POP2] = POP2;
        INSTRUCTIONS[Const.DUP] = DUP;
        INSTRUCTIONS[Const.DUP_X1] = DUP_X1;
        INSTRUCTIONS[Const.DUP_X2] = DUP_X2;
        INSTRUCTIONS[Const.DUP2] = DUP2;
        INSTRUCTIONS[Const.DUP2_X1] = DUP2_X1;
        INSTRUCTIONS[Const.DUP2_X2] = DUP2_X2;
        INSTRUCTIONS[Const.SWAP] = SWAP;
        INSTRUCTIONS[Const.IADD] = IADD;
        INSTRUCTIONS[Const.LADD] = LADD;
        INSTRUCTIONS[Const.FADD] = FADD;
        INSTRUCTIONS[Const.DADD] = DADD;
        INSTRUCTIONS[Const.ISUB] = ISUB;
        INSTRUCTIONS[Const.LSUB] = LSUB;
        INSTRUCTIONS[Const.FSUB] = FSUB;
        INSTRUCTIONS[Const.DSUB] = DSUB;
        INSTRUCTIONS[Const.IMUL] = IMUL;
        INSTRUCTIONS[Const.LMUL] = LMUL;
        INSTRUCTIONS[Const.FMUL] = FMUL;
        INSTRUCTIONS[Const.DMUL] = DMUL;
        INSTRUCTIONS[Const.IDIV] = IDIV;
        INSTRUCTIONS[Const.LDIV] = LDIV;
        INSTRUCTIONS[Const.FDIV] = FDIV;
        INSTRUCTIONS[Const.DDIV] = DDIV;
        INSTRUCTIONS[Const.IREM] = IREM;
        INSTRUCTIONS[Const.LREM] = LREM;
        INSTRUCTIONS[Const.FREM] = FREM;
        INSTRUCTIONS[Const.DREM] = DREM;
        INSTRUCTIONS[Const.INEG] = INEG;
        INSTRUCTIONS[Const.LNEG] = LNEG;
        INSTRUCTIONS[Const.FNEG] = FNEG;
        INSTRUCTIONS[Const.DNEG] = DNEG;
        INSTRUCTIONS[Const.ISHL] = ISHL;
        INSTRUCTIONS[Const.LSHL] = LSHL;
        INSTRUCTIONS[Const.ISHR] = ISHR;
        INSTRUCTIONS[Const.LSHR] = LSHR;
        INSTRUCTIONS[Const.IUSHR] = IUSHR;
        INSTRUCTIONS[Const.LUSHR] = LUSHR;
        INSTRUCTIONS[Const.IAND] = IAND;
        INSTRUCTIONS[Const.LAND] = LAND;
        INSTRUCTIONS[Const.IOR] = IOR;
        INSTRUCTIONS[Const.LOR] = LOR;
        INSTRUCTIONS[Const.IXOR] = IXOR;
        INSTRUCTIONS[Const.LXOR] = LXOR;
        INSTRUCTIONS[Const.I2L] = I2L;
        INSTRUCTIONS[Const.I2F] = I2F;
        INSTRUCTIONS[Const.I2D] = I2D;
        INSTRUCTIONS[Const.L2I] = L2I;
        INSTRUCTIONS[Const.L2F] = L2F;
        INSTRUCTIONS[Const.L2D] = L2D;
        INSTRUCTIONS[Const.F2I] = F2I;
        INSTRUCTIONS[Const.F2L] = F2L;
        INSTRUCTIONS[Const.F2D] = F2D;
        INSTRUCTIONS[Const.D2I] = D2I;
        INSTRUCTIONS[Const.D2L] = D2L;
        INSTRUCTIONS[Const.D2F] = D2F;
        INSTRUCTIONS[Const.I2B] = I2B;
        INSTRUCTIONS[Const.I2C] = I2C;
        INSTRUCTIONS[Const.I2S] = I2S;
        INSTRUCTIONS[Const.LCMP] = LCMP;
        INSTRUCTIONS[Const.FCMPL] = FCMPL;
        INSTRUCTIONS[Const.FCMPG] = FCMPG;
        INSTRUCTIONS[Const.DCMPL] = DCMPL;
        INSTRUCTIONS[Const.DCMPG] = DCMPG;
        INSTRUCTIONS[Const.IRETURN] = IRETURN;
        INSTRUCTIONS[Const.LRETURN] = LRETURN;
        INSTRUCTIONS[Const.FRETURN] = FRETURN;
        INSTRUCTIONS[Const.DRETURN] = DRETURN;
        INSTRUCTIONS[Const.ARETURN] = ARETURN;
        INSTRUCTIONS[Const.RETURN] = RETURN;
        INSTRUCTIONS[Const.ARRAYLENGTH] = ARRAYLENGTH;
        INSTRUCTIONS[Const.ATHROW] = ATHROW;
        INSTRUCTIONS[Const.MONITORENTER] = MONITORENTER;
        INSTRUCTIONS[Const.MONITOREXIT] = MONITOREXIT;
    }

    private InstructionConst() { } // non-instantiable

    /**
     * Gets the Instruction.
     * @param index the index, e.g. {@link Const#RETURN}
     * @return the entry from the private INSTRUCTIONS table
     */
    public static Instruction getInstruction(final int index) {
        return INSTRUCTIONS[index];
    }
}
