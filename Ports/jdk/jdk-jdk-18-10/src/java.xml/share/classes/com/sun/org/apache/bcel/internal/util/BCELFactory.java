/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.bcel.internal.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.classfile.Utility;
import com.sun.org.apache.bcel.internal.generic.AllocationInstruction;
import com.sun.org.apache.bcel.internal.generic.ArrayInstruction;
import com.sun.org.apache.bcel.internal.generic.ArrayType;
import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.BranchInstruction;
import com.sun.org.apache.bcel.internal.generic.CHECKCAST;
import com.sun.org.apache.bcel.internal.generic.CPInstruction;
import com.sun.org.apache.bcel.internal.generic.CodeExceptionGen;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.ConstantPushInstruction;
import com.sun.org.apache.bcel.internal.generic.EmptyVisitor;
import com.sun.org.apache.bcel.internal.generic.FieldInstruction;
import com.sun.org.apache.bcel.internal.generic.IINC;
import com.sun.org.apache.bcel.internal.generic.INSTANCEOF;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionConst;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InvokeInstruction;
import com.sun.org.apache.bcel.internal.generic.LDC;
import com.sun.org.apache.bcel.internal.generic.LDC2_W;
import com.sun.org.apache.bcel.internal.generic.LocalVariableInstruction;
import com.sun.org.apache.bcel.internal.generic.MULTIANEWARRAY;
import com.sun.org.apache.bcel.internal.generic.MethodGen;
import com.sun.org.apache.bcel.internal.generic.NEWARRAY;
import com.sun.org.apache.bcel.internal.generic.ObjectType;
import com.sun.org.apache.bcel.internal.generic.RET;
import com.sun.org.apache.bcel.internal.generic.ReturnInstruction;
import com.sun.org.apache.bcel.internal.generic.Select;
import com.sun.org.apache.bcel.internal.generic.Type;

/**
 * Factory creates il.append() statements, and sets instruction targets.
 * A helper class for BCELifier.
 *
 * @see BCELifier
 * @LastModified: May 2021
 */
class BCELFactory extends EmptyVisitor {

    private static final String CONSTANT_PREFIX = Const.class.getSimpleName()+".";
    private final MethodGen _mg;
    private final PrintWriter _out;
    private final ConstantPoolGen _cp;


    BCELFactory(final MethodGen mg, final PrintWriter out) {
        _mg = mg;
        _cp = mg.getConstantPool();
        _out = out;
    }

    private final Map<Instruction, InstructionHandle> branch_map = new HashMap<>();


    public void start() {
        if (!_mg.isAbstract() && !_mg.isNative()) {
            for (InstructionHandle ih = _mg.getInstructionList().getStart(); ih != null; ih = ih
                    .getNext()) {
                final Instruction i = ih.getInstruction();
                if (i instanceof BranchInstruction) {
                    branch_map.put(i, ih); // memorize container
                }
                if (ih.hasTargeters()) {
                    if (i instanceof BranchInstruction) {
                        _out.println("    InstructionHandle ih_" + ih.getPosition() + ";");
                    } else {
                        _out.print("    InstructionHandle ih_" + ih.getPosition() + " = ");
                    }
                } else {
                    _out.print("    ");
                }
                if (!visitInstruction(i)) {
                    i.accept(this);
                }
            }
            updateBranchTargets();
            updateExceptionHandlers();
        }
    }


    private boolean visitInstruction( final Instruction i ) {
        final short opcode = i.getOpcode();
        if ((InstructionConst.getInstruction(opcode) != null)
                && !(i instanceof ConstantPushInstruction) && !(i instanceof ReturnInstruction)) { // Handled below
            _out.println("il.append(InstructionConst."
                    + i.getName().toUpperCase(Locale.ENGLISH) + ");");
            return true;
        }
        return false;
    }


    @Override
    public void visitLocalVariableInstruction( final LocalVariableInstruction i ) {
        final short opcode = i.getOpcode();
        final Type type = i.getType(_cp);
        if (opcode == Const.IINC) {
            _out.println("il.append(new IINC(" + i.getIndex() + ", " + ((IINC) i).getIncrement()
                    + "));");
        } else {
            final String kind = (opcode < Const.ISTORE) ? "Load" : "Store";
            _out.println("il.append(_factory.create" + kind + "(" + BCELifier.printType(type)
                    + ", " + i.getIndex() + "));");
        }
    }


    @Override
    public void visitArrayInstruction( final ArrayInstruction i ) {
        final short opcode = i.getOpcode();
        final Type type = i.getType(_cp);
        final String kind = (opcode < Const.IASTORE) ? "Load" : "Store";
        _out.println("il.append(_factory.createArray" + kind + "(" + BCELifier.printType(type)
                + "));");
    }


    @Override
    public void visitFieldInstruction( final FieldInstruction i ) {
        final short opcode = i.getOpcode();
        final String class_name = i.getReferenceType(_cp).getSignature();
        final String field_name = i.getFieldName(_cp);
        final Type type = i.getFieldType(_cp);
        _out.println("il.append(_factory.createFieldAccess(\"" + class_name + "\", \"" + field_name
                + "\", " + BCELifier.printType(type) + ", " + CONSTANT_PREFIX
                + Const.getOpcodeName(opcode).toUpperCase(Locale.ENGLISH) + "));");
    }


    @Override
    public void visitInvokeInstruction( final InvokeInstruction i ) {
        final short opcode = i.getOpcode();
        final String class_name = i.getReferenceType(_cp).getSignature();
        final String method_name = i.getMethodName(_cp);
        final Type type = i.getReturnType(_cp);
        final Type[] arg_types = i.getArgumentTypes(_cp);
        _out.println("il.append(_factory.createInvoke(\"" + class_name + "\", \"" + method_name
                + "\", " + BCELifier.printType(type) + ", "
                + BCELifier.printArgumentTypes(arg_types) + ", " + CONSTANT_PREFIX
                + Const.getOpcodeName(opcode).toUpperCase(Locale.ENGLISH) + "));");
    }


    @Override
    @SuppressWarnings("fallthrough") // by design for case Const.ANEWARRAY
    public void visitAllocationInstruction( final AllocationInstruction i ) {
        Type type;
        if (i instanceof CPInstruction) {
            type = ((CPInstruction) i).getType(_cp);
        } else {
            type = ((NEWARRAY) i).getType();
        }
        final short opcode = ((Instruction) i).getOpcode();
        int dim = 1;
        switch (opcode) {
            case Const.NEW:
                _out.println("il.append(_factory.createNew(\"" + ((ObjectType) type).getClassName()
                        + "\"));");
                break;
            case Const.MULTIANEWARRAY:
                dim = ((MULTIANEWARRAY) i).getDimensions();
                //$FALL-THROUGH$
            case Const.ANEWARRAY:
            case Const.NEWARRAY:
                if (type instanceof ArrayType) {
                    type = ((ArrayType) type).getBasicType();
                }
                _out.println("il.append(_factory.createNewArray(" + BCELifier.printType(type)
                        + ", (short) " + dim + "));");
                break;
            default:
                throw new IllegalArgumentException("Unhandled opcode: " + opcode);
        }
    }


    private void createConstant( final Object value ) {
        String embed = value.toString();
        if (value instanceof String) {
            embed = '"' + Utility.convertString(embed) + '"';
        } else if (value instanceof Character) {
            embed = "(char)0x" + Integer.toHexString(((Character) value).charValue());
        } else if (value instanceof Float) {
            embed += "f";
        } else if (value instanceof Long) {
            embed += "L";
        } else if (value instanceof ObjectType) {
            final ObjectType ot = (ObjectType) value;
            embed = "new ObjectType(\""+ot.getClassName()+"\")";
        }

        _out.println("il.append(new PUSH(_cp, " + embed + "));");
    }


    @Override
    public void visitLDC( final LDC i ) {
        createConstant(i.getValue(_cp));
    }


    @Override
    public void visitLDC2_W( final LDC2_W i ) {
        createConstant(i.getValue(_cp));
    }


    @Override
    public void visitConstantPushInstruction( final ConstantPushInstruction i ) {
        createConstant(i.getValue());
    }


    @Override
    public void visitINSTANCEOF( final INSTANCEOF i ) {
        final Type type = i.getType(_cp);
        _out.println("il.append(new INSTANCEOF(_cp.addClass(" + BCELifier.printType(type) + ")));");
    }


    @Override
    public void visitCHECKCAST( final CHECKCAST i ) {
        final Type type = i.getType(_cp);
        _out.println("il.append(_factory.createCheckCast(" + BCELifier.printType(type) + "));");
    }


    @Override
    public void visitReturnInstruction( final ReturnInstruction i ) {
        final Type type = i.getType(_cp);
        _out.println("il.append(_factory.createReturn(" + BCELifier.printType(type) + "));");
    }

    // Memorize BranchInstructions that need an update
    private final List<BranchInstruction> branches = new ArrayList<>();


    @Override
    public void visitBranchInstruction( final BranchInstruction bi ) {
        final BranchHandle bh = (BranchHandle) branch_map.get(bi);
        final int pos = bh.getPosition();
        final String name = bi.getName() + "_" + pos;
        if (bi instanceof Select) {
            final Select s = (Select) bi;
            branches.add(bi);
            final StringBuilder args = new StringBuilder("new int[] { ");
            final int[] matchs = s.getMatchs();
            for (int i = 0; i < matchs.length; i++) {
                args.append(matchs[i]);
                if (i < matchs.length - 1) {
                    args.append(", ");
                }
            }
            args.append(" }");
            _out.print("Select " + name + " = new " + bi.getName().toUpperCase(Locale.ENGLISH)
                    + "(" + args + ", new InstructionHandle[] { ");
            for (int i = 0; i < matchs.length; i++) {
                _out.print("null");
                if (i < matchs.length - 1) {
                    _out.print(", ");
                }
            }
            _out.println(" }, null);");
        } else {
            final int t_pos = bh.getTarget().getPosition();
            String target;
            if (pos > t_pos) {
                target = "ih_" + t_pos;
            } else {
                branches.add(bi);
                target = "null";
            }
            _out.println("    BranchInstruction " + name + " = _factory.createBranchInstruction("
                    + CONSTANT_PREFIX + bi.getName().toUpperCase(Locale.ENGLISH) + ", " + target
                    + ");");
        }
        if (bh.hasTargeters()) {
            _out.println("    ih_" + pos + " = il.append(" + name + ");");
        } else {
            _out.println("    il.append(" + name + ");");
        }
    }


    @Override
    public void visitRET( final RET i ) {
        _out.println("il.append(new RET(" + i.getIndex() + ")));");
    }


    private void updateBranchTargets() {
        for (final BranchInstruction bi : branches) {
            final BranchHandle bh = (BranchHandle) branch_map.get(bi);
            final int pos = bh.getPosition();
            final String name = bi.getName() + "_" + pos;
            int t_pos = bh.getTarget().getPosition();
            _out.println("    " + name + ".setTarget(ih_" + t_pos + ");");
            if (bi instanceof Select) {
                final InstructionHandle[] ihs = ((Select) bi).getTargets();
                for (int j = 0; j < ihs.length; j++) {
                    t_pos = ihs[j].getPosition();
                    _out.println("    " + name + ".setTarget(" + j + ", ih_" + t_pos + ");");
                }
            }
        }
    }


    private void updateExceptionHandlers() {
        final CodeExceptionGen[] handlers = _mg.getExceptionHandlers();
        for (final CodeExceptionGen h : handlers) {
            final String type = (h.getCatchType() == null) ? "null" : BCELifier.printType(h
                    .getCatchType());
            _out.println("    method.addExceptionHandler(" + "ih_" + h.getStartPC().getPosition()
                    + ", " + "ih_" + h.getEndPC().getPosition() + ", " + "ih_"
                    + h.getHandlerPC().getPosition() + ", " + type + ");");
        }
    }
}
