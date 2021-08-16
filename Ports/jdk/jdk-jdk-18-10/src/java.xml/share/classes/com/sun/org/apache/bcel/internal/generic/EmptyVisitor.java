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

/**
 * Supplies empty method bodies to be overridden by subclasses.
 *
 */
public abstract class EmptyVisitor implements Visitor {

    @Override
    public void visitStackInstruction( final StackInstruction obj ) {
    }


    @Override
    public void visitLocalVariableInstruction( final LocalVariableInstruction obj ) {
    }


    @Override
    public void visitBranchInstruction( final BranchInstruction obj ) {
    }


    @Override
    public void visitLoadClass( final LoadClass obj ) {
    }


    @Override
    public void visitFieldInstruction( final FieldInstruction obj ) {
    }


    @Override
    public void visitIfInstruction( final IfInstruction obj ) {
    }


    @Override
    public void visitConversionInstruction( final ConversionInstruction obj ) {
    }


    @Override
    public void visitPopInstruction( final PopInstruction obj ) {
    }


    @Override
    public void visitJsrInstruction( final JsrInstruction obj ) {
    }


    @Override
    public void visitGotoInstruction( final GotoInstruction obj ) {
    }


    @Override
    public void visitStoreInstruction( final StoreInstruction obj ) {
    }


    @Override
    public void visitTypedInstruction( final TypedInstruction obj ) {
    }


    @Override
    public void visitSelect( final Select obj ) {
    }


    @Override
    public void visitUnconditionalBranch( final UnconditionalBranch obj ) {
    }


    @Override
    public void visitPushInstruction( final PushInstruction obj ) {
    }


    @Override
    public void visitArithmeticInstruction( final ArithmeticInstruction obj ) {
    }


    @Override
    public void visitCPInstruction( final CPInstruction obj ) {
    }


    @Override
    public void visitInvokeInstruction( final InvokeInstruction obj ) {
    }


    @Override
    public void visitArrayInstruction( final ArrayInstruction obj ) {
    }


    @Override
    public void visitAllocationInstruction( final AllocationInstruction obj ) {
    }


    @Override
    public void visitReturnInstruction( final ReturnInstruction obj ) {
    }


    @Override
    public void visitFieldOrMethod( final FieldOrMethod obj ) {
    }


    @Override
    public void visitConstantPushInstruction( final ConstantPushInstruction obj ) {
    }


    @Override
    public void visitExceptionThrower( final ExceptionThrower obj ) {
    }


    @Override
    public void visitLoadInstruction( final LoadInstruction obj ) {
    }


    @Override
    public void visitVariableLengthInstruction( final VariableLengthInstruction obj ) {
    }


    @Override
    public void visitStackProducer( final StackProducer obj ) {
    }


    @Override
    public void visitStackConsumer( final StackConsumer obj ) {
    }


    @Override
    public void visitACONST_NULL( final ACONST_NULL obj ) {
    }


    @Override
    public void visitGETSTATIC( final GETSTATIC obj ) {
    }


    @Override
    public void visitIF_ICMPLT( final IF_ICMPLT obj ) {
    }


    @Override
    public void visitMONITOREXIT( final MONITOREXIT obj ) {
    }


    @Override
    public void visitIFLT( final IFLT obj ) {
    }


    @Override
    public void visitLSTORE( final LSTORE obj ) {
    }


    @Override
    public void visitPOP2( final POP2 obj ) {
    }


    @Override
    public void visitBASTORE( final BASTORE obj ) {
    }


    @Override
    public void visitISTORE( final ISTORE obj ) {
    }


    @Override
    public void visitCHECKCAST( final CHECKCAST obj ) {
    }


    @Override
    public void visitFCMPG( final FCMPG obj ) {
    }


    @Override
    public void visitI2F( final I2F obj ) {
    }


    @Override
    public void visitATHROW( final ATHROW obj ) {
    }


    @Override
    public void visitDCMPL( final DCMPL obj ) {
    }


    @Override
    public void visitARRAYLENGTH( final ARRAYLENGTH obj ) {
    }


    @Override
    public void visitDUP( final DUP obj ) {
    }


    @Override
    public void visitINVOKESTATIC( final INVOKESTATIC obj ) {
    }


    @Override
    public void visitLCONST( final LCONST obj ) {
    }


    @Override
    public void visitDREM( final DREM obj ) {
    }


    @Override
    public void visitIFGE( final IFGE obj ) {
    }


    @Override
    public void visitCALOAD( final CALOAD obj ) {
    }


    @Override
    public void visitLASTORE( final LASTORE obj ) {
    }


    @Override
    public void visitI2D( final I2D obj ) {
    }


    @Override
    public void visitDADD( final DADD obj ) {
    }


    @Override
    public void visitINVOKESPECIAL( final INVOKESPECIAL obj ) {
    }


    @Override
    public void visitIAND( final IAND obj ) {
    }


    @Override
    public void visitPUTFIELD( final PUTFIELD obj ) {
    }


    @Override
    public void visitILOAD( final ILOAD obj ) {
    }


    @Override
    public void visitDLOAD( final DLOAD obj ) {
    }


    @Override
    public void visitDCONST( final DCONST obj ) {
    }


    @Override
    public void visitNEW( final NEW obj ) {
    }


    @Override
    public void visitIFNULL( final IFNULL obj ) {
    }


    @Override
    public void visitLSUB( final LSUB obj ) {
    }


    @Override
    public void visitL2I( final L2I obj ) {
    }


    @Override
    public void visitISHR( final ISHR obj ) {
    }


    @Override
    public void visitTABLESWITCH( final TABLESWITCH obj ) {
    }


    @Override
    public void visitIINC( final IINC obj ) {
    }


    @Override
    public void visitDRETURN( final DRETURN obj ) {
    }


    @Override
    public void visitFSTORE( final FSTORE obj ) {
    }


    @Override
    public void visitDASTORE( final DASTORE obj ) {
    }


    @Override
    public void visitIALOAD( final IALOAD obj ) {
    }


    @Override
    public void visitDDIV( final DDIV obj ) {
    }


    @Override
    public void visitIF_ICMPGE( final IF_ICMPGE obj ) {
    }


    @Override
    public void visitLAND( final LAND obj ) {
    }


    @Override
    public void visitIDIV( final IDIV obj ) {
    }


    @Override
    public void visitLOR( final LOR obj ) {
    }


    @Override
    public void visitCASTORE( final CASTORE obj ) {
    }


    @Override
    public void visitFREM( final FREM obj ) {
    }


    @Override
    public void visitLDC( final LDC obj ) {
    }


    @Override
    public void visitBIPUSH( final BIPUSH obj ) {
    }


    @Override
    public void visitDSTORE( final DSTORE obj ) {
    }


    @Override
    public void visitF2L( final F2L obj ) {
    }


    @Override
    public void visitFMUL( final FMUL obj ) {
    }


    @Override
    public void visitLLOAD( final LLOAD obj ) {
    }


    @Override
    public void visitJSR( final JSR obj ) {
    }


    @Override
    public void visitFSUB( final FSUB obj ) {
    }


    @Override
    public void visitSASTORE( final SASTORE obj ) {
    }


    @Override
    public void visitALOAD( final ALOAD obj ) {
    }


    @Override
    public void visitDUP2_X2( final DUP2_X2 obj ) {
    }


    @Override
    public void visitRETURN( final RETURN obj ) {
    }


    @Override
    public void visitDALOAD( final DALOAD obj ) {
    }


    @Override
    public void visitSIPUSH( final SIPUSH obj ) {
    }


    @Override
    public void visitDSUB( final DSUB obj ) {
    }


    @Override
    public void visitL2F( final L2F obj ) {
    }


    @Override
    public void visitIF_ICMPGT( final IF_ICMPGT obj ) {
    }


    @Override
    public void visitF2D( final F2D obj ) {
    }


    @Override
    public void visitI2L( final I2L obj ) {
    }


    @Override
    public void visitIF_ACMPNE( final IF_ACMPNE obj ) {
    }


    @Override
    public void visitPOP( final POP obj ) {
    }


    @Override
    public void visitI2S( final I2S obj ) {
    }


    @Override
    public void visitIFEQ( final IFEQ obj ) {
    }


    @Override
    public void visitSWAP( final SWAP obj ) {
    }


    @Override
    public void visitIOR( final IOR obj ) {
    }


    @Override
    public void visitIREM( final IREM obj ) {
    }


    @Override
    public void visitIASTORE( final IASTORE obj ) {
    }


    @Override
    public void visitNEWARRAY( final NEWARRAY obj ) {
    }


    @Override
    public void visitINVOKEINTERFACE( final INVOKEINTERFACE obj ) {
    }


    @Override
    public void visitINEG( final INEG obj ) {
    }


    @Override
    public void visitLCMP( final LCMP obj ) {
    }


    @Override
    public void visitJSR_W( final JSR_W obj ) {
    }


    @Override
    public void visitMULTIANEWARRAY( final MULTIANEWARRAY obj ) {
    }


    @Override
    public void visitDUP_X2( final DUP_X2 obj ) {
    }


    @Override
    public void visitSALOAD( final SALOAD obj ) {
    }


    @Override
    public void visitIFNONNULL( final IFNONNULL obj ) {
    }


    @Override
    public void visitDMUL( final DMUL obj ) {
    }


    @Override
    public void visitIFNE( final IFNE obj ) {
    }


    @Override
    public void visitIF_ICMPLE( final IF_ICMPLE obj ) {
    }


    @Override
    public void visitLDC2_W( final LDC2_W obj ) {
    }


    @Override
    public void visitGETFIELD( final GETFIELD obj ) {
    }


    @Override
    public void visitLADD( final LADD obj ) {
    }


    @Override
    public void visitNOP( final NOP obj ) {
    }


    @Override
    public void visitFALOAD( final FALOAD obj ) {
    }


    @Override
    public void visitINSTANCEOF( final INSTANCEOF obj ) {
    }


    @Override
    public void visitIFLE( final IFLE obj ) {
    }


    @Override
    public void visitLXOR( final LXOR obj ) {
    }


    @Override
    public void visitLRETURN( final LRETURN obj ) {
    }


    @Override
    public void visitFCONST( final FCONST obj ) {
    }


    @Override
    public void visitIUSHR( final IUSHR obj ) {
    }


    @Override
    public void visitBALOAD( final BALOAD obj ) {
    }


    @Override
    public void visitDUP2( final DUP2 obj ) {
    }


    @Override
    public void visitIF_ACMPEQ( final IF_ACMPEQ obj ) {
    }


    @Override
    public void visitIMPDEP1( final IMPDEP1 obj ) {
    }


    @Override
    public void visitMONITORENTER( final MONITORENTER obj ) {
    }


    @Override
    public void visitLSHL( final LSHL obj ) {
    }


    @Override
    public void visitDCMPG( final DCMPG obj ) {
    }


    @Override
    public void visitD2L( final D2L obj ) {
    }


    @Override
    public void visitIMPDEP2( final IMPDEP2 obj ) {
    }


    @Override
    public void visitL2D( final L2D obj ) {
    }


    @Override
    public void visitRET( final RET obj ) {
    }


    @Override
    public void visitIFGT( final IFGT obj ) {
    }


    @Override
    public void visitIXOR( final IXOR obj ) {
    }


    @Override
    public void visitINVOKEVIRTUAL( final INVOKEVIRTUAL obj ) {
    }


    @Override
    public void visitFASTORE( final FASTORE obj ) {
    }


    @Override
    public void visitIRETURN( final IRETURN obj ) {
    }


    @Override
    public void visitIF_ICMPNE( final IF_ICMPNE obj ) {
    }


    @Override
    public void visitFLOAD( final FLOAD obj ) {
    }


    @Override
    public void visitLDIV( final LDIV obj ) {
    }


    @Override
    public void visitPUTSTATIC( final PUTSTATIC obj ) {
    }


    @Override
    public void visitAALOAD( final AALOAD obj ) {
    }


    @Override
    public void visitD2I( final D2I obj ) {
    }


    @Override
    public void visitIF_ICMPEQ( final IF_ICMPEQ obj ) {
    }


    @Override
    public void visitAASTORE( final AASTORE obj ) {
    }


    @Override
    public void visitARETURN( final ARETURN obj ) {
    }


    @Override
    public void visitDUP2_X1( final DUP2_X1 obj ) {
    }


    @Override
    public void visitFNEG( final FNEG obj ) {
    }


    @Override
    public void visitGOTO_W( final GOTO_W obj ) {
    }


    @Override
    public void visitD2F( final D2F obj ) {
    }


    @Override
    public void visitGOTO( final GOTO obj ) {
    }


    @Override
    public void visitISUB( final ISUB obj ) {
    }


    @Override
    public void visitF2I( final F2I obj ) {
    }


    @Override
    public void visitDNEG( final DNEG obj ) {
    }


    @Override
    public void visitICONST( final ICONST obj ) {
    }


    @Override
    public void visitFDIV( final FDIV obj ) {
    }


    @Override
    public void visitI2B( final I2B obj ) {
    }


    @Override
    public void visitLNEG( final LNEG obj ) {
    }


    @Override
    public void visitLREM( final LREM obj ) {
    }


    @Override
    public void visitIMUL( final IMUL obj ) {
    }


    @Override
    public void visitIADD( final IADD obj ) {
    }


    @Override
    public void visitLSHR( final LSHR obj ) {
    }


    @Override
    public void visitLOOKUPSWITCH( final LOOKUPSWITCH obj ) {
    }


    @Override
    public void visitDUP_X1( final DUP_X1 obj ) {
    }


    @Override
    public void visitFCMPL( final FCMPL obj ) {
    }


    @Override
    public void visitI2C( final I2C obj ) {
    }


    @Override
    public void visitLMUL( final LMUL obj ) {
    }


    @Override
    public void visitLUSHR( final LUSHR obj ) {
    }


    @Override
    public void visitISHL( final ISHL obj ) {
    }


    @Override
    public void visitLALOAD( final LALOAD obj ) {
    }


    @Override
    public void visitASTORE( final ASTORE obj ) {
    }


    @Override
    public void visitANEWARRAY( final ANEWARRAY obj ) {
    }


    @Override
    public void visitFRETURN( final FRETURN obj ) {
    }


    @Override
    public void visitFADD( final FADD obj ) {
    }


    @Override
    public void visitBREAKPOINT( final BREAKPOINT obj ) {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitINVOKEDYNAMIC(final INVOKEDYNAMIC obj) {
    }
}
