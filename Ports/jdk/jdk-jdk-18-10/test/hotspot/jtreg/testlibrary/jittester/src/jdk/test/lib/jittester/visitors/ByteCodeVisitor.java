/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.test.lib.jittester.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.test.lib.util.Pair;
import jdk.test.lib.jittester.BinaryOperator;
import jdk.test.lib.jittester.Block;
import jdk.test.lib.jittester.BuiltInType;
import jdk.test.lib.jittester.Break;
import jdk.test.lib.jittester.CastOperator;
import jdk.test.lib.jittester.CatchBlock;
import jdk.test.lib.jittester.Continue;
import jdk.test.lib.jittester.Declaration;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.If;
import jdk.test.lib.jittester.Initialization;
import jdk.test.lib.jittester.Literal;
import jdk.test.lib.jittester.LocalVariable;
import jdk.test.lib.jittester.NonStaticMemberVariable;
import jdk.test.lib.jittester.Nothing;
import jdk.test.lib.jittester.Operator;
import jdk.test.lib.jittester.OperatorKind;
import jdk.test.lib.jittester.PrintVariables;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Statement;
import jdk.test.lib.jittester.StaticMemberVariable;
import jdk.test.lib.jittester.Switch;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.TernaryOperator;
import jdk.test.lib.jittester.Throw;
import jdk.test.lib.jittester.TryCatchBlock;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.UnaryOperator;
import jdk.test.lib.jittester.VariableBase;
import jdk.test.lib.jittester.VariableDeclaration;
import jdk.test.lib.jittester.VariableDeclarationBlock;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.VariableInitialization;
import jdk.test.lib.jittester.arrays.ArrayCreation;
import jdk.test.lib.jittester.arrays.ArrayElement;
import jdk.test.lib.jittester.arrays.ArrayExtraction;
import jdk.test.lib.jittester.classes.ClassDefinitionBlock;
import jdk.test.lib.jittester.classes.Interface;
import jdk.test.lib.jittester.classes.Klass;
import jdk.test.lib.jittester.classes.MainKlass;
import jdk.test.lib.jittester.functions.ArgumentDeclaration;
import jdk.test.lib.jittester.functions.ConstructorDefinition;
import jdk.test.lib.jittester.functions.ConstructorDefinitionBlock;
import jdk.test.lib.jittester.functions.Function;
import jdk.test.lib.jittester.functions.FunctionDeclaration;
import jdk.test.lib.jittester.functions.FunctionDeclarationBlock;
import jdk.test.lib.jittester.functions.FunctionDefinition;
import jdk.test.lib.jittester.functions.FunctionDefinitionBlock;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.functions.FunctionRedefinition;
import jdk.test.lib.jittester.functions.FunctionRedefinitionBlock;
import jdk.test.lib.jittester.functions.Return;
import jdk.test.lib.jittester.functions.StaticConstructorDefinition;
import jdk.test.lib.jittester.loops.CounterInitializer;
import jdk.test.lib.jittester.loops.CounterManipulator;
import jdk.test.lib.jittester.loops.DoWhile;
import jdk.test.lib.jittester.loops.For;
import jdk.test.lib.jittester.loops.Loop;
import jdk.test.lib.jittester.loops.LoopingCondition;
import jdk.test.lib.jittester.loops.While;
import jdk.test.lib.jittester.types.TypeArray;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.FixedTrees;
import jdk.test.lib.jittester.utils.PseudoRandom;

public class ByteCodeVisitor implements Visitor<byte[]> {
    private final GeneratedClassesContext context = new GeneratedClassesContext();
    private final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private final int CLASS_WRITER_FLAGS = ContextDependedClassWriter.COMPUTE_MAXS | ContextDependedClassWriter.COMPUTE_FRAMES;
    private final HashMap<String, ContextDependedClassWriter> classWriters = new HashMap<>();
    private MethodVisitor currentMV;
    private TypeKlass currentClass;
    private final LocalVariablesTable locals = new LocalVariablesTable();
    private final Deque<Label> endLabels = new ArrayDeque<>();
    private final Deque<Label> beginLabels = new ArrayDeque<>();

    @Override
    public byte[] visit(ArgumentDeclaration node) {
        /* handled by FunctionDefinition for ByteCodeVisitor */
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(ArrayCreation node) {
        int dimensions = node.getDimensionsCount();
        TypeArray arrayType = node.getArrayType();
        Type basicType = arrayType.type;
        for (IRNode child : node.getChildren()) {
            child.accept(this);
        }
        if (dimensions == 1) {
            if (basicType.equals(TypeList.BOOLEAN)) {
                currentMV.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
            } else if (basicType.equals(TypeList.BYTE)) {
                currentMV.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
            } else if (basicType.equals(TypeList.CHAR)) {
                currentMV.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR);
            } else if (basicType.equals(TypeList.SHORT)) {
                currentMV.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT);
            } else if (basicType.equals(TypeList.INT)) {
                currentMV.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
            } else if (basicType.equals(TypeList.LONG)) {
                currentMV.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
            } else if (basicType.equals(TypeList.FLOAT)) {
                currentMV.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
            } else if (basicType.equals(TypeList.DOUBLE)) {
                currentMV.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
            } else {
                currentMV.visitTypeInsn(Opcodes.ANEWARRAY, asInternalName(basicType.getName()));
            }
        } else {
            currentMV.visitMultiANewArrayInsn(new String(arrayType.accept(this)), dimensions);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(ArrayElement node) {
        node.getChild(0).accept(this);
        int dimensions = node.getChildren().size() - 1;
        Type resultType = node.getResultType();
        for (int i = 1; i < dimensions; i++) {
            node.getChild(i).accept(this);
            currentMV.visitInsn(Opcodes.AALOAD);
        }
        node.getChild(dimensions).accept(this);
        if (resultType.equals(TypeList.BOOLEAN) || resultType.equals(TypeList.BYTE)) {
            currentMV.visitInsn(Opcodes.BALOAD);
        } else if (resultType.equals(TypeList.CHAR)) {
            currentMV.visitInsn(Opcodes.CALOAD);
        } else if (resultType.equals(TypeList.SHORT)) {
            currentMV.visitInsn(Opcodes.SALOAD);
        } else if (resultType.equals(TypeList.INT)) {
            currentMV.visitInsn(Opcodes.IALOAD);
        } else if (resultType.equals(TypeList.LONG)) {
            currentMV.visitInsn(Opcodes.LALOAD);
        } else if (resultType.equals(TypeList.FLOAT)) {
            currentMV.visitInsn(Opcodes.FALOAD);
        } else if (resultType.equals(TypeList.DOUBLE)) {
            currentMV.visitInsn(Opcodes.DALOAD);
        } else {
            currentMV.visitInsn(Opcodes.AALOAD);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(ArrayExtraction node) {
        node.getChild(0).accept(this);
        int dimensions = node.getChildren().size() - 1;
        Type resultType = node.getResultType();
        for (int i = 1; i < dimensions; i++) {
            node.getChild(i).accept(this);
            currentMV.visitInsn(Opcodes.AALOAD);
        }
        node.getChild(dimensions).accept(this);
        if (resultType.equals(TypeList.BOOLEAN) || resultType.equals(TypeList.BYTE)) {
            currentMV.visitInsn(Opcodes.BALOAD);
        } else if (resultType.equals(TypeList.CHAR)) {
            currentMV.visitInsn(Opcodes.CALOAD);
        } else if (resultType.equals(TypeList.SHORT)) {
            currentMV.visitInsn(Opcodes.SALOAD);
        } else if (resultType.equals(TypeList.INT)) {
            currentMV.visitInsn(Opcodes.IALOAD);
        } else if (resultType.equals(TypeList.LONG)) {
            currentMV.visitInsn(Opcodes.LALOAD);
        } else if (resultType.equals(TypeList.FLOAT)) {
            currentMV.visitInsn(Opcodes.FALOAD);
        } else if (resultType.equals(TypeList.DOUBLE)) {
            currentMV.visitInsn(Opcodes.DALOAD);
        } else {
            currentMV.visitInsn(Opcodes.AALOAD);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(BinaryOperator node) {
        OperatorKind kind = node.getOperationKind();
        IRNode left = node.getChild(Operator.Order.LEFT.ordinal());
        IRNode right = node.getChild(Operator.Order.RIGHT.ordinal());
        Type resultType = node.getResultType();
        if (left == null || right == null) {
            return EMPTY_BYTE_ARRAY;
        }
        boolean needTypeConversion = false;
        boolean convertRightArg = false;
        Type leftType = left.getResultType();
        Type rightType = right.getResultType();
        if (!leftType.equals(rightType) && leftType instanceof BuiltInType
                && rightType instanceof BuiltInType
                && kind != OperatorKind.SAR && kind != OperatorKind.SHL
                && kind != OperatorKind.SHR && kind != OperatorKind.ASSIGN
                && kind != OperatorKind.AND && kind != OperatorKind.OR) {
            needTypeConversion = true;
            BuiltInType leftBuiltIn = (BuiltInType) leftType;
            BuiltInType rightBuiltIn = (BuiltInType) rightType;
            convertRightArg = leftBuiltIn.isMoreCapaciousThan(rightBuiltIn);
        }
        Type mostCapacious = convertRightArg ? leftType : rightType;
        if (!rightType.equals(TypeList.INT)
                && (kind == OperatorKind.SHL || kind == OperatorKind.SHR
                || kind == OperatorKind.SAR)) {
            left.accept(this);
            right.accept(this);
            convertTopType(rightType, TypeList.INT);
        } else if (kind != OperatorKind.ASSIGN && kind != OperatorKind.OR
                && kind != OperatorKind.AND && kind != OperatorKind.COMPOUND_ADD
                && kind != OperatorKind.COMPOUND_AND && kind != OperatorKind.COMPOUND_DIV
                && kind != OperatorKind.COMPOUND_MOD && kind != OperatorKind.COMPOUND_MUL
                && kind != OperatorKind.COMPOUND_OR && kind != OperatorKind.COMPOUND_SAR
                && kind != OperatorKind.COMPOUND_SHL && kind != OperatorKind.COMPOUND_SHR
                && kind != OperatorKind.COMPOUND_SUB && kind != OperatorKind.COMPOUND_XOR
                && kind != OperatorKind.STRADD) {
                /* "assign", "and", "or", concat and all compound operators are
                    handled differently and shouldn't just place left and right
                    operands on stack */
            left.accept(this);
            if (needTypeConversion && !convertRightArg) {
                convertTopType(leftType, rightType);
            }
            right.accept(this);
            if (needTypeConversion && convertRightArg) {
                convertTopType(rightType, leftType);
            }
        }
        switch (kind) {
            case ASSIGN:
                VariableInfo vi = ((VariableBase)left).getVariableInfo();
                Type varType = vi.type;
                if (left instanceof LocalVariable) {
                    right.accept(this);
                    convertTopType(rightType, leftType);
                    int index = locals.getLocalIndex(vi);
                    if (varType.equals(TypeList.LONG)) {
                        currentMV.visitVarInsn(Opcodes.LSTORE, index);
                        currentMV.visitVarInsn(Opcodes.LLOAD, index);
                    } else if (varType.equals(TypeList.DOUBLE)) {
                        currentMV.visitVarInsn(Opcodes.DSTORE, index);
                        currentMV.visitVarInsn(Opcodes.DLOAD, index);
                    } else if (varType.equals(TypeList.FLOAT)) {
                        currentMV.visitVarInsn(Opcodes.FSTORE, index);
                        currentMV.visitVarInsn(Opcodes.FLOAD, index);
                    } else if (varType instanceof TypeKlass) {
                        currentMV.visitVarInsn(Opcodes.ASTORE, index);
                        currentMV.visitVarInsn(Opcodes.ALOAD, index);
                    } else {
                        currentMV.visitVarInsn(Opcodes.ISTORE, index);
                        currentMV.visitVarInsn(Opcodes.ILOAD, index);
                    }
                } else if (left instanceof StaticMemberVariable) {
                    right.accept(this);
                    convertTopType(rightType, leftType);
                    String typeDescr = new String(vi.type.accept(this));
                    String ownerName = asInternalName(vi.getOwner().getName());
                    currentMV.visitFieldInsn(Opcodes.PUTSTATIC, ownerName,
                            vi.name, typeDescr);
                    currentMV.visitFieldInsn(Opcodes.GETSTATIC, ownerName,
                            vi.name, typeDescr);
                } else if (left instanceof NonStaticMemberVariable) {
                    // put object to stack for putfield
                    left.getChild(0).accept(this);
                    // put object to stack for getfield
                    currentMV.visitInsn(Opcodes.DUP);
                    right.accept(this);
                    convertTopType(rightType, leftType);
                    String typeDescr = new String(vi.type.accept(this));
                    String ownerName = asInternalName(vi.getOwner().getName());
                    currentMV.visitFieldInsn(Opcodes.PUTFIELD, ownerName,
                            vi.name, typeDescr);
                    currentMV.visitFieldInsn(Opcodes.GETFIELD, ownerName,
                            vi.name, typeDescr);
                } else {
                    throw new IllegalArgumentException("illegal left operand : "
                            + left + "("+left.getClass()+")");
                }
                break;
            case OR:
                generateBasicLogicOperator(Opcodes.IFNE, false, left, right);
                break;
            case AND:
                generateBasicLogicOperator(Opcodes.IFEQ, true,  left, right);
                break;
            case BIT_OR:
                if (mostCapacious.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LOR);
                } else {
                    currentMV.visitInsn(Opcodes.IOR);
                }
                break;
            case BIT_XOR:
                if (mostCapacious.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LXOR);
                } else {
                    currentMV.visitInsn(Opcodes.IXOR);
                }
                break;
            case BIT_AND:
                if (mostCapacious.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LAND);
                } else {
                    currentMV.visitInsn(Opcodes.IAND);
                }
                break;
            case EQ:
                generateCmpBasedCode(mostCapacious, Opcodes.IFEQ, Opcodes.IF_ICMPEQ);
                break;
            case NE:
                generateCmpBasedCode(mostCapacious, Opcodes.IFNE, Opcodes.IF_ICMPNE);
                break;
            case GT:
                generateCmpBasedCode(mostCapacious, Opcodes.IFGT, Opcodes.IF_ICMPGT);
                break;
            case LT:
                generateCmpBasedCode(mostCapacious, Opcodes.IFLT, Opcodes.IF_ICMPLT);
                break;
            case GE:
                generateCmpBasedCode(mostCapacious, Opcodes.IFGE, Opcodes.IF_ICMPGE);
                break;
            case LE:
                generateCmpBasedCode(mostCapacious, Opcodes.IFLE, Opcodes.IF_ICMPLE);
                break;
            case SHR:
                if (leftType.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LSHR);
                } else {
                    currentMV.visitInsn(Opcodes.ISHR);
                }
                break;
            case SHL:
                if (leftType.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LSHL);
                } else {
                    currentMV.visitInsn(Opcodes.ISHL);
                }
                break;
            case SAR:
                if (leftType.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LUSHR);
                } else {
                    currentMV.visitInsn(Opcodes.IUSHR);
                }
                break;
            case STRADD:
                // we use String::valueOf to change null to "null"
                left.accept(this);
                currentMV.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf",
                        "(Ljava/lang/Object;)Ljava/lang/String;", false /* not interface */);
                right.accept(this);
                currentMV.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf",
                        "(Ljava/lang/Object;)Ljava/lang/String;", false /* not interface */);
                currentMV.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat",
                        "(Ljava/lang/String;)Ljava/lang/String;", false /* not interface */);
                break;
            case ADD:
                if (mostCapacious.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LADD);
                } else if (mostCapacious.equals(TypeList.DOUBLE)) {
                    currentMV.visitInsn(Opcodes.DADD);
                } else if (mostCapacious.equals(TypeList.FLOAT)) {
                    currentMV.visitInsn(Opcodes.FADD);
                } else {
                    currentMV.visitInsn(Opcodes.IADD);
                }
                break;
            case SUB:
                if (mostCapacious.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LSUB);
                } else if (mostCapacious.equals(TypeList.DOUBLE)) {
                    currentMV.visitInsn(Opcodes.DSUB);
                } else if (mostCapacious.equals(TypeList.FLOAT)) {
                    currentMV.visitInsn(Opcodes.FSUB);
                } else {
                    currentMV.visitInsn(Opcodes.ISUB);
                }
                break;
            case MUL:
                if (mostCapacious.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LMUL);
                } else if (mostCapacious.equals(TypeList.DOUBLE)) {
                    currentMV.visitInsn(Opcodes.DMUL);
                } else if (mostCapacious.equals(TypeList.FLOAT)) {
                    currentMV.visitInsn(Opcodes.FMUL);
                } else {
                    currentMV.visitInsn(Opcodes.IMUL);
                }
                break;
            case DIV:
                if (mostCapacious.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LDIV);
                } else if (mostCapacious.equals(TypeList.DOUBLE)) {
                    currentMV.visitInsn(Opcodes.DDIV);
                } else if (mostCapacious.equals(TypeList.FLOAT)) {
                    currentMV.visitInsn(Opcodes.FDIV);
                } else {
                    currentMV.visitInsn(Opcodes.IDIV);
                }
                break;
            case MOD:
                if (mostCapacious.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LREM);
                } else if (mostCapacious.equals(TypeList.DOUBLE)) {
                    currentMV.visitInsn(Opcodes.DREM);
                } else if (mostCapacious.equals(TypeList.FLOAT)) {
                    currentMV.visitInsn(Opcodes.FREM);
                } else {
                    currentMV.visitInsn(Opcodes.IREM);
                }
                break;
            case COMPOUND_ADD:
                lowerCompoundBinaryOperator("java.lang.String".equals(leftType.getName())
                        ? OperatorKind.STRADD : OperatorKind.ADD, node);
                break;
            case COMPOUND_SUB:
                lowerCompoundBinaryOperator(OperatorKind.SUB, node);
                break;
            case COMPOUND_MUL:
                lowerCompoundBinaryOperator(OperatorKind.MUL, node);
                break;
            case COMPOUND_DIV:
                lowerCompoundBinaryOperator(OperatorKind.DIV, node);
                break;
            case COMPOUND_MOD:
                lowerCompoundBinaryOperator(OperatorKind.MOD, node);
                break;
            case COMPOUND_AND:
                lowerCompoundBinaryOperator(leftType.equals(TypeList.BOOLEAN)
                        ? OperatorKind.AND : OperatorKind.BIT_AND, node);
                break;
            case COMPOUND_OR:
                lowerCompoundBinaryOperator(leftType.equals(TypeList.BOOLEAN)
                        ? OperatorKind.OR : OperatorKind.BIT_OR, node);
                break;
            case COMPOUND_XOR:
                lowerCompoundBinaryOperator(OperatorKind.BIT_XOR, node);
                break;
            case COMPOUND_SHR:
                lowerCompoundBinaryOperator(OperatorKind.SHR, node);
                break;
            case COMPOUND_SHL:
                lowerCompoundBinaryOperator(OperatorKind.SHL, node);
                break;
            case COMPOUND_SAR:
                lowerCompoundBinaryOperator(OperatorKind.SAR, node);
                break;
            default:
                throw new Error("Unsupported binary operator");
        }
        return EMPTY_BYTE_ARRAY;
    }

    private static int tmpObject;
    private void lowerCompoundBinaryOperator(OperatorKind kind, IRNode node) {
        IRNode left = node.getChild(Operator.Order.LEFT.ordinal());
        IRNode right = node.getChild(Operator.Order.RIGHT.ordinal());

        if (left instanceof NonStaticMemberVariable) {
            NonStaticMemberVariable var = (NonStaticMemberVariable) left;
            IRNode holder = var.getChild(0);
            Type type = holder.getResultType();
            VariableInfo tmpInfo = new VariableInfo("tmpObject_" + tmpObject++,
                    currentClass, type, VariableInfo.LOCAL);
            new Statement(new VariableInitialization(tmpInfo, holder), true).accept(this);
            left = new NonStaticMemberVariable(new LocalVariable(tmpInfo), var.getVariableInfo());
        }
        Type leftType = left.getResultType();
        Type rightType = right.getResultType();
        Type resultType = leftType;
        if (leftType instanceof BuiltInType && rightType instanceof BuiltInType) {
            if (kind != OperatorKind.SHL && kind != OperatorKind.SHR && kind != OperatorKind.SAR
                    && ((BuiltInType) rightType).isMoreCapaciousThan((BuiltInType) leftType)) {
                resultType = rightType;
            }
        }
        IRNode result = new CastOperator(leftType,
                new BinaryOperator(kind, resultType, left, right));
        new BinaryOperator(OperatorKind.ASSIGN, leftType, left, result).accept(this);
    }

    private void generateBasicLogicOperator(int ifOpcode, boolean retTrueFirst, IRNode left,
            IRNode right) {
        Label secondCase = new Label();
        Label endLabel = new Label();
        left.accept(this);
        currentMV.visitJumpInsn(ifOpcode, secondCase);
        right.accept(this);
        currentMV.visitJumpInsn(ifOpcode, secondCase);
        currentMV.visitInsn(retTrueFirst ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        currentMV.visitJumpInsn(Opcodes.GOTO, endLabel);
        currentMV.visitLabel(secondCase);
        currentMV.visitInsn(retTrueFirst ? Opcodes.ICONST_0 : Opcodes.ICONST_1);
        currentMV.visitLabel(endLabel);
    }

    private void generateCmpBasedCode(Type type, int nonIntOpcode, int intOpcode) {
        boolean useNonIntOpcode = false;
        if (type.equals(TypeList.LONG) || type.equals(TypeList.FLOAT)
                || type.equals(TypeList.DOUBLE)) {
            if (type.equals(TypeList.LONG)) {
                currentMV.visitInsn(Opcodes.LCMP);
            } else if (type.equals(TypeList.FLOAT)) {
                currentMV.visitInsn(Opcodes.FCMPL);
            } else {
                currentMV.visitInsn(Opcodes.DCMPL);
            }
            useNonIntOpcode = true;
        }
        int opcodeToUse;
        if (!useNonIntOpcode) {
            if (type instanceof TypeKlass) {
                if (intOpcode == Opcodes.IF_ICMPEQ) {
                    opcodeToUse = Opcodes.IF_ACMPEQ;
                } else if (intOpcode == Opcodes.IF_ICMPNE) {
                    opcodeToUse = Opcodes.IF_ACMPNE;
                } else {
                    throw new Error("Can't compare references");
                }
            } else {
                opcodeToUse = intOpcode;
            }
        } else {
            opcodeToUse = nonIntOpcode;
        }
        Label retTrue = new Label();
        Label end = new Label();
        currentMV.visitJumpInsn(opcodeToUse, retTrue);
        currentMV.visitInsn(Opcodes.ICONST_0);
        currentMV.visitJumpInsn(Opcodes.GOTO, end);
        currentMV.visitLabel(retTrue);
        currentMV.visitInsn(Opcodes.ICONST_1);
        currentMV.visitLabel(end);
    }

    /*
     * Converts top-stack element from one builtin type to another
     */
    private void convertTopType(Type from, Type to) {
        if (!(from instanceof BuiltInType) || !(to instanceof BuiltInType) || from.equals(to)) {
            return; // skip
        }
        boolean castedToInt = false;
        if (from.equals(TypeList.FLOAT)) {
            if (to.equals(TypeList.DOUBLE)) {
                currentMV.visitInsn(Opcodes.F2D);
            } else if (to.equals(TypeList.LONG)) {
                currentMV.visitInsn(Opcodes.F2L);
            } else {
                currentMV.visitInsn(Opcodes.F2I);
                castedToInt = true;
            }
        } else if (from.equals(TypeList.DOUBLE)) {
            if (to.equals(TypeList.FLOAT)) {
                currentMV.visitInsn(Opcodes.D2F);
            } else if (to.equals(TypeList.LONG)) {
                currentMV.visitInsn(Opcodes.D2L);
            } else {
                currentMV.visitInsn(Opcodes.D2I);
                castedToInt = true;
            }
        } else if (from.equals(TypeList.LONG)) {
            if (to.equals(TypeList.DOUBLE)) {
                currentMV.visitInsn(Opcodes.L2D);
            } else if (to.equals(TypeList.FLOAT)) {
                currentMV.visitInsn(Opcodes.L2F);
            } else {
                currentMV.visitInsn(Opcodes.L2I);
                castedToInt = true;
            }
        } else {
            if (to.equals(TypeList.BYTE)) {
                currentMV.visitInsn(Opcodes.I2B);
            } else if (to.equals(TypeList.CHAR)) {
                currentMV.visitInsn(Opcodes.I2C);
            } else if (to.equals(TypeList.SHORT)) {
                currentMV.visitInsn(Opcodes.I2S);
            } else if (to.equals(TypeList.LONG)) {
                currentMV.visitInsn(Opcodes.I2L);
            } else if (to.equals(TypeList.FLOAT)) {
                currentMV.visitInsn(Opcodes.I2F);
            } else if (to.equals(TypeList.DOUBLE)) {
                currentMV.visitInsn(Opcodes.I2D);
            }
        }
        if (castedToInt) {
            if (to.equals(TypeList.BYTE)) {
                currentMV.visitInsn(Opcodes.I2B);
            } else if (to.equals(TypeList.CHAR)) {
                currentMV.visitInsn(Opcodes.I2C);
            } else if (to.equals(TypeList.SHORT)) {
                currentMV.visitInsn(Opcodes.I2S);
            }
        }
    }

    @Override
    public byte[] visit(Block node) {
        return iterateBlock(node);
    }

    @Override
    public byte[] visit(Break node) {
        Label label = endLabels.peek();
        if (label != null) {
            currentMV.visitJumpInsn(Opcodes.GOTO, label);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(CastOperator node) {
        IRNode expression = node.getChild(0);
        expression.accept(this);
        Type to = node.getResultType();
        Type from = expression.getResultType();
        // TODO boxing/unboxing
        if (!TypeList.isBuiltIn(to) || !TypeList.isBuiltIn(from)) {
            // class cast
            currentMV.visitTypeInsn(Opcodes.CHECKCAST, asInternalName(to.getName()));
        } else {
            convertTopType(from, to);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(CatchBlock node) {
        Type type = node.throwables.get(0);
        VariableInfo exInfo = new VariableInfo("ex", currentClass,
                type, VariableInfo.LOCAL);
        int index = locals.getLocalIndex(exInfo);
        currentMV.visitVarInsn(Opcodes.ASTORE, index);
        node.getChild(0).accept(this);
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(ClassDefinitionBlock node) {
        return iterateBlock(node);
    }

    @Override
    public byte[] visit(ConstructorDefinition node) {
        FunctionInfo info = node.getFunctionInfo();
        String ownerName = node.getOwner().getName();
        TypeKlass parentClass = currentClass.getParent();
        ContextDependedClassWriter cw = classWriters.get(ownerName);

        String descriptor = getDescriptor(node, 1, "V");
        currentMV = cw.visitMethod(asAccessFlags(info), "<init>", descriptor, null, null);
        currentMV.visitVarInsn(Opcodes.ALOAD, 0);
        currentMV.visitMethodInsn(Opcodes.INVOKESPECIAL,
                parentClass != null ? asInternalName(parentClass.getName()) : "java/lang/Object",
                "<init>", "()V", false);
        locals.initConstructorArguments(node.getOwner(), info);
        // TODO: add datamemebers as child to all ctors
        generateDataMembers(node.getParent().getParent().getChild(Klass.KlassPart.DATA_MEMBERS.ordinal()));
        IRNode body = node.getChild(0);
        body.accept(this);
        currentMV.visitInsn(Opcodes.RETURN);
        currentMV.visitMaxs(0, 0);
        currentMV.visitEnd();
        return EMPTY_BYTE_ARRAY;
    }

    private void generateDataMembers(IRNode node) {
        // TODO shouldn't we skip declaration?
        if (node != null) {
            node.accept(this);
        }
    }

    @Override
    public byte[] visit(ConstructorDefinitionBlock node) {
        return iterateBlock(node);
    }

    @Override
    public byte[] visit(Continue node) {
        Label label = beginLabels.peek();
        if (label != null) {
            currentMV.visitJumpInsn(Opcodes.GOTO, label);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(CounterInitializer node) {
        visitLocalVar(node);
        emitPop(node.getVariableInfo().type);
        return EMPTY_BYTE_ARRAY;
    }

    private byte[] visitLocalVar(Initialization node) {
        VariableInfo vi = node.getVariableInfo();
        int index = locals.addLocal(vi);
        int store;
        node.getChild(0).accept(this); // place initialization expression on stack
        emitDup(vi.type);
        if (vi.type instanceof TypeKlass) {
            store = Opcodes.ASTORE;
        } else if (vi.type.equals(TypeList.DOUBLE)) {
            store = Opcodes.DSTORE;
        } else if (vi.type.equals(TypeList.LONG)) {
            store = Opcodes.LSTORE;
        } else if (vi.type.equals(TypeList.FLOAT)) {
            store = Opcodes.FSTORE;
        } else {
            store = Opcodes.ISTORE;
        }
        currentMV.visitVarInsn(store, index);
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(CounterManipulator node) {
        return node.getChild(0).accept(this);
    }

    @Override
    public byte[] visit(Declaration node) {
        IRNode child = node.getChild(0);
        child.accept(this);
        if (child instanceof Initialization) {
            emitPop(((Initialization) child).getVariableInfo().type);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(DoWhile node) {
        Loop loop = node.getLoop();
        loop.initialization.accept(this);
        node.getChild(DoWhile.DoWhilePart.HEADER.ordinal()).accept(this);
        Label currentLoopBegin = new Label();
        beginLabels.push(currentLoopBegin);
        Label currentLoopEnd = new Label();
        endLabels.push(currentLoopEnd);
        currentMV.visitLabel(currentLoopBegin);
        node.getChild(DoWhile.DoWhilePart.BODY1.ordinal()).accept(this);
        loop.manipulator.accept(this);
        node.getChild(DoWhile.DoWhilePart.BODY2.ordinal()).accept(this);
        loop.condition.accept(this);
        assert loop.condition.getResultType() == TypeList.BOOLEAN;
        currentMV.visitJumpInsn(Opcodes.IFEQ, currentLoopBegin);
        currentMV.visitLabel(currentLoopEnd);
        Label a = beginLabels.pop();
        assert currentLoopBegin == a;
        a = endLabels.pop();
        assert currentLoopEnd == a;
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(For node) {
        Loop loop = node.getLoop();
        loop.initialization.accept(this);
        node.getChild(For.ForPart.HEADER.ordinal()).accept(this);
        node.getChild(For.ForPart.STATEMENT1.ordinal()).accept(this);
        Label currentLoopBegin = new Label();
        beginLabels.push(currentLoopBegin);
        currentMV.visitLabel(currentLoopBegin);
        loop.condition.accept(this);
        assert loop.condition.getResultType() == TypeList.BOOLEAN;
        Label currentLoopEnd = new Label();
        endLabels.push(currentLoopEnd);
        currentMV.visitJumpInsn(Opcodes.IFEQ, currentLoopEnd);
        node.getChild(For.ForPart.STATEMENT2.ordinal()).accept(this);
        node.getChild(For.ForPart.BODY1.ordinal()).accept(this);
        loop.manipulator.accept(this);
        node.getChild(For.ForPart.BODY2.ordinal()).accept(this);
        node.getChild(For.ForPart.BODY3.ordinal()).accept(this);
        currentMV.visitJumpInsn(Opcodes.GOTO, currentLoopBegin);
        currentMV.visitLabel(currentLoopEnd);
        Label a = beginLabels.pop();
        assert currentLoopBegin == a;
        a = endLabels.pop();
        assert currentLoopEnd == a;
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(Function node) {
        FunctionInfo info = node.getValue();
        boolean needInstance = !info.isStatic() && !info.isConstructor();
        if (needInstance) {
            node.getChild(0).accept(this); // placing instance on stack
        }
        // call itself with specific invoke*
        String signature = info.argTypes.stream()
                .skip(!needInstance ? 0 : 1)
                .map(vi -> new String(vi.type.accept(this)))
                .collect(Collectors.joining("", "(", ")"))
                + (info.isConstructor() ? "V" : new String(node.getResultType().accept(this)));
        int invokeCode = Opcodes.INVOKEVIRTUAL;
        if (info.isStatic()) {
            invokeCode = Opcodes.INVOKESTATIC;
        } else if (info.isConstructor() || info.isPrivate()) {
            // TODO : superclass method invocation?
            invokeCode = Opcodes.INVOKESPECIAL;
        } else {
            if (info.owner.isInterface()) {
                invokeCode = Opcodes.INVOKEINTERFACE;
            }
        }
        if (info.isConstructor()) {
            currentMV.visitTypeInsn(Opcodes.NEW, asInternalName(info.owner.getName()));
            currentMV.visitInsn(Opcodes.DUP);
        }
        // calculating parameters
        node.getChildren().stream()
                .skip(!needInstance ? 0 : 1)
                .forEachOrdered(c -> c.accept(this));
        currentMV.visitMethodInsn(invokeCode, asInternalName(info.owner.getName()),
                info.isConstructor() ? "<init>" : info.name, signature,
                invokeCode == Opcodes.INVOKEINTERFACE);
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(FunctionDeclaration node) {
        FunctionInfo info = node.getFunctionInfo();
        String ownerName = node.getOwner().getName();
        ContextDependedClassWriter cw = classWriters.get(ownerName);
        String returnType = new String(info.type.accept(this));

        String descriptor = getDescriptor(node, 0, returnType);
        currentMV = cw.visitMethod(asAccessFlags(info) + Opcodes.ACC_ABSTRACT,
                info.name, descriptor, null, null);
        currentMV.visitEnd();
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(FunctionDeclarationBlock node) {
        return iterateBlock(node);
    }

    @Override
    public byte[] visit(FunctionDefinition node) {
        FunctionInfo info = node.getFunctionInfo();
        String ownerName = node.getOwner().getName();
        ContextDependedClassWriter cw = classWriters.get(ownerName);
        String returnType = new String(info.type.accept(this));

        String descriptor = getDescriptor(node, 2, returnType);
        currentMV = cw.visitMethod(asAccessFlags(info), info.name, descriptor, null, null);
        locals.initFunctionArguments(info);
        IRNode body = node.getChild(0);
        body.accept(this);
        IRNode ret = node.getChild(1);
        ret.accept(this);
        currentMV.visitMaxs(0, 0);
        currentMV.visitEnd();
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(FunctionDefinitionBlock node) {
        return iterateBlock(node);
    }

    @Override
    public byte[] visit(FunctionRedefinition node) {
        FunctionInfo info = node.getFunctionInfo();
        String ownerName = node.getOwner().getName();
        ContextDependedClassWriter cw = classWriters.get(ownerName);
        String returnType = new String(info.type.accept(this));
        String descriptor = getDescriptor(node, 2, returnType);
        currentMV = cw.visitMethod(asAccessFlags(info), info.name, descriptor, null, null);
        locals.initFunctionArguments(info);
        IRNode body = node.getChild(0);
        body.accept(this);
        IRNode ret = node.getChild(1);
        ret.accept(this);
        currentMV.visitMaxs(0, 0);
        currentMV.visitEnd();
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(FunctionRedefinitionBlock node) {
        return iterateBlock(node);
    }

    @Override
    public byte[] visit(If node) {
        IRNode conditionBlock = node.getChild(If.IfPart.CONDITION.ordinal());
        // get the condition type to emit correct if
        conditionBlock.accept(this);
        generateIf(Opcodes.IFEQ, node.getChild(If.IfPart.THEN.ordinal()),
                node.getChild(If.IfPart.ELSE.ordinal()));
        return EMPTY_BYTE_ARRAY;
    }

    /*
     * Generates if block with then and else blocks for the given IF opcode
     */
    private void generateIf(int ifOpcode, IRNode thenBlock, IRNode elseBlock) {
        Label elseLabel = new Label();
        // if the opposite condition is met then go to the else statement
        currentMV.visitJumpInsn(ifOpcode, elseLabel);
        // write THEN block
        thenBlock.accept(this);
        if (elseBlock != null) {
            // goto the end after THEN
            Label endLabel = new Label();
            currentMV.visitJumpInsn(Opcodes.GOTO, endLabel);
            // ELSE block
            currentMV.visitLabel(elseLabel);
            elseBlock.accept(this);
            currentMV.visitLabel(endLabel);
        } else {
            currentMV.visitLabel(elseLabel);
        }
    }

    @Override
    public byte[] visit(Initialization node) {
        VariableInfo vi = node.getVariableInfo();
        if (vi.isLocal()) {
            return visitLocalVar(node);
        }
        String ownerName = vi.getOwner().getName();
        ContextDependedClassWriter cw = classWriters.get(ownerName);
        String typeName = new String(vi.type.accept(this));
        // constant value used only for final static fields
        FieldVisitor fw = cw.visitField(asAccessFlags(vi), vi.name,
                typeName,
                null /* Generic */,
                null /* Constant value */);
        fw.visitEnd(); // doesn't need visitAnnotation and visitAttribute
        if (vi.isStatic()) {
            node.getChild(0).accept(this); // put value to stack
            emitDup(vi.type);
            currentMV.visitFieldInsn(Opcodes.PUTSTATIC,
                    asInternalName(vi.getOwner().getName()),
                    vi.name,
                    new String(vi.type.accept(this)));
        } else {
            // TODO : can it be another object?
            currentMV.visitVarInsn(Opcodes.ALOAD, 0); // put this to stack
            node.getChild(0).accept(this); // put value to stack
            emitDupX1(vi.type);
            currentMV.visitFieldInsn(Opcodes.PUTFIELD,
                    asInternalName(vi.getOwner().getName()),
                    vi.name,
                    new String(vi.type.accept(this)));
        }
        return EMPTY_BYTE_ARRAY;
    }

    private void emitDupX1(Type type) {
        if (TypeList.DOUBLE.equals(type) || TypeList.LONG.equals(type)) {
            currentMV.visitInsn(Opcodes.DUP2_X1);
        } else if (!TypeList.VOID.equals(type)){
            currentMV.visitInsn(Opcodes.DUP_X1);
        }
    }

    private void emitDup(Type type) {
        if (TypeList.DOUBLE.equals(type) || TypeList.LONG.equals(type)) {
            currentMV.visitInsn(Opcodes.DUP2);
        } else if (!TypeList.VOID.equals(type)){
            currentMV.visitInsn(Opcodes.DUP);
        }
    }

    @Override
    public byte[] visit(Interface node) {
        String name = node.getName();
        ContextDependedClassWriter classWriter = new ContextDependedClassWriter(CLASS_WRITER_FLAGS);
        classWriters.put(name, classWriter);
        TypeKlass parentKlass = node.getParentKlass();
        classWriter.visit(Opcodes.V1_8,
                          Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
                          asInternalName(name),
                          null /* Generic */,
                          "java/lang/Object",
                          parentKlass == null ? null : new String[] {
                                  asInternalName(parentKlass.getName())});
        if (node.getChildren().size() > 0) {
            node.getChild(0).accept(this);
        }

        classWriter.visitEnd();
        byte[] byteCode = classWriter.toByteArray();
        context.register(name, byteCode);
        return byteCode;
    }

    @Override
    public byte[] visit(Klass node) {
        String name = node.getName();
        TypeKlass prevClass = currentClass;
        currentClass = node.getThisKlass();
        ContextDependedClassWriter classWriter = new ContextDependedClassWriter(CLASS_WRITER_FLAGS);
        classWriters.put(name, classWriter);
        TypeKlass thisClass = node.getThisKlass();
        TypeKlass parentClass = node.getParentKlass();
        String[] interfaces = node.getInterfaces().stream()
                .map(IRNode::getName)
                .map(ByteCodeVisitor::asInternalName)
                .toArray(String[]::new);
        classWriter.visit(Opcodes.V1_8, asAccessFlags(thisClass),
                asInternalName(name),
                null /* Generic */,
                parentClass != null ? asInternalName(parentClass.getName()) : "java/lang/Object",
                interfaces);

        IRNode constructors = node.getChild(Klass.KlassPart.CONSTRUCTORS.ordinal());
        if (constructors != null) {
            constructors.accept(this);
        } else {
            // generate default ctor
            currentMV = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            currentMV.visitVarInsn(Opcodes.ALOAD, 0);
            currentMV.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            locals.clear();
            locals.addLocal(new VariableInfo("this", thisClass, thisClass, VariableInfo.NONE));
            generateDataMembers(node.getChild(Klass.KlassPart.DATA_MEMBERS.ordinal()));
            currentMV.visitInsn(Opcodes.RETURN);
            currentMV.visitMaxs(0, 0);
            currentMV.visitEnd();
        }
        IRNode redefinedFunctions = node.getChild(Klass.KlassPart.REDEFINED_FUNCTIONS.ordinal());
        if (redefinedFunctions != null) {
            redefinedFunctions.accept(this);
        }
        IRNode overridenFunctions = node.getChild(Klass.KlassPart.OVERRIDEN_FUNCTIONS.ordinal());
        if (overridenFunctions != null) {
            overridenFunctions.accept(this);
        }
        IRNode memberFunctions = node.getChild(Klass.KlassPart.MEMBER_FUNCTIONS.ordinal());
        if (memberFunctions != null) {
            memberFunctions.accept(this);
        }
        IRNode memberFunctionDecls = node.getChild(Klass.KlassPart.MEMBER_FUNCTIONS_DECLARATIONS.ordinal());
        if (memberFunctionDecls != null) {
            memberFunctionDecls.accept(this);
        }
        IRNode printVariables = node.getChild(Klass.KlassPart.PRINT_VARIABLES.ordinal());
        if (printVariables != null) {
            printVariables.accept(this);
        }
        classWriter.visitEnd();
        byte[] byteCode = classWriter.toByteArray();
        context.register(name, byteCode);
        currentClass = prevClass;
        return byteCode;
    }

    private void visitLiteral(boolean value) {
        double chance = PseudoRandom.random();
        if (chance < CONSTANT_INST_CHANCE) {
            currentMV.visitInsn(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        } else {
            currentMV.visitIntInsn(Opcodes.BIPUSH, value ? 1 : 0);
        }
    }

    private void visitLiteral(byte value) {
        double chance = PseudoRandom.random();
        if (chance < CONSTANT_INST_CHANCE && value > -2 && value < 6) {
            currentMV.visitInsn(Opcodes.ICONST_0 + value);
        } else {
            currentMV.visitIntInsn(Opcodes.BIPUSH, value);
        }
    }

    private void visitLiteral(short value) {
        double chance = PseudoRandom.random();
        if (chance < CONSTANT_INST_CHANCE && value > -2 && value < 6) {
            currentMV.visitInsn(Opcodes.ICONST_0 + value);
        } else {
            currentMV.visitIntInsn(Opcodes.SIPUSH, value);
        }
    }

    private void visitLiteral(char value) {
        double chance = PseudoRandom.random();
        if (chance < CONSTANT_INST_CHANCE && value < 6) {
            currentMV.visitInsn(Opcodes.ICONST_0 + value);
        } else {
            // TODO : check for widechar/unicode
            currentMV.visitIntInsn(Opcodes.BIPUSH, value);
        }
    }

    private void visitLiteral(int value) {
        double chance = PseudoRandom.random();
        if (chance < CONSTANT_INST_CHANCE && value > -2 && value < 6) {
            currentMV.visitInsn(Opcodes.ICONST_0 + value);
        } else {
            currentMV.visitLdcInsn(value);
        }
    }

    private void visitLiteral(long value) {
        double chance = PseudoRandom.random();
        if (chance < CONSTANT_INST_CHANCE && value > -1 && value < 2) {
            currentMV.visitInsn(Opcodes.LCONST_0 + (int)value);
        } else {
            currentMV.visitLdcInsn(value);
        }
    }

    private void visitLiteral(float value) {
        double chance = PseudoRandom.random();
        if (chance < CONSTANT_INST_CHANCE && (value == 0.0f || value == 1.0f || value == 2.0f)) {
            currentMV.visitInsn(Opcodes.FCONST_0 + (int)value);
        } else {
            currentMV.visitLdcInsn(value);
        }
    }

    private void visitLiteral(double value) {
        double chance = PseudoRandom.random();
        if (chance < CONSTANT_INST_CHANCE && (value == 0.0 || value == 1.0)) {
            currentMV.visitInsn(Opcodes.DCONST_0 + (int)value);
        } else {
            currentMV.visitLdcInsn(value);
        }
    }

    @Override
    public byte[] visit(Literal node) {
        /*
            ICONST_n (−1 ≤ n ≤ 5) <==> BIPUSH <n>
            LCONST_n (0 ≤ n ≤ 1)
            FCONST_n (0 ≤ n ≤ 2)
            DCONST_n (0 ≤ n ≤ 1)
            ACONST_NULL

            BIPUSH b, −128 ≤ b < 127
            SIPUSH s, −32768 ≤ s < 32767
            LDC cst (int, float, long, double, String or Type)
        */
        Type type = node.getResultType();
        double chance = PseudoRandom.random();
        if (type.equals(TypeList.BOOLEAN)) {
            visitLiteral(Boolean.valueOf(node.getValue().toString()));
        } else if (type.equals(TypeList.BYTE)) {
            visitLiteral(Byte.valueOf(node.getValue().toString()));
        } else if (type.equals(TypeList.SHORT)) {
            visitLiteral(Short.valueOf(node.getValue().toString()));
        } else if (type.equals(TypeList.CHAR)) {
            visitLiteral(node.getValue().toString().charAt(0));
        } else if (type.equals(TypeList.INT)) {
            visitLiteral(Integer.valueOf(node.getValue().toString()));
        } else if (type.equals(TypeList.LONG)) {
            visitLiteral(Long.valueOf(node.getValue().toString()));
        } else if (type.equals(TypeList.FLOAT)) {
            visitLiteral(Float.valueOf(node.getValue().toString()));
        } else if (type.equals(TypeList.DOUBLE)) {
            visitLiteral(Double.valueOf(node.getValue().toString()));
        } else {
            currentMV.visitLdcInsn(node.getValue());
        }
        return EMPTY_BYTE_ARRAY;
    }
    private static final double CONSTANT_INST_CHANCE = 0.5;

    @Override
    public byte[] visit(LocalVariable node) {
        // This node is for "reading" only. Writing is handled in BinaryOperator visit(see ASSIGN)
        VariableInfo vi = node.getVariableInfo();
        Type varType = vi.type;
        int index = locals.getLocalIndex(vi);
        if (varType.equals(TypeList.LONG)) {
            currentMV.visitVarInsn(Opcodes.LLOAD, index);
        } else if (varType.equals(TypeList.DOUBLE)) {
            currentMV.visitVarInsn(Opcodes.DLOAD, index);
        } else if (varType.equals(TypeList.FLOAT)) {
            currentMV.visitVarInsn(Opcodes.FLOAD, index);
        } else if (varType instanceof TypeKlass) {
            currentMV.visitVarInsn(Opcodes.ALOAD, index);
        } else {
            currentMV.visitVarInsn(Opcodes.ILOAD, index);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(LoopingCondition node) {
        return node.getCondition().accept(this);
    }

    @Override
    public byte[] visit(MainKlass node) {
        TypeKlass prevClass = currentClass;
        currentClass = node.getThisKlass();
        String name = node.getName();
        ContextDependedClassWriter mainClassWriter = new ContextDependedClassWriter(CLASS_WRITER_FLAGS);
        classWriters.put(name, mainClassWriter);

        TypeKlass thisClass = node.getThisKlass();
        mainClassWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC,
                asInternalName(name),
                null /* Generic */,
                "java/lang/Object",
                null /* interfaces */);
        // TODO: constructor for main class
        currentMV = mainClassWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        currentMV.visitVarInsn(Opcodes.ALOAD, 0);
        currentMV.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        locals.clear();
        locals.addLocal(new VariableInfo("this", thisClass, thisClass, VariableInfo.NONE));
        generateDataMembers(node.getChild(MainKlass.MainKlassPart.DATA_MEMBERS.ordinal()));
        currentMV.visitInsn(Opcodes.RETURN);
        currentMV.visitMaxs(0, 0);
        currentMV.visitEnd();

        IRNode memberFunctions = node.getChild(MainKlass.MainKlassPart.MEMBER_FUNCTIONS.ordinal());
        if (memberFunctions != null) {
            memberFunctions.accept(this);
        }
        IRNode testFunction = node.getChild(MainKlass.MainKlassPart.TEST_FUNCTION.ordinal());
        if (testFunction != null) {
            currentMV = mainClassWriter.visitMethod(
                    Opcodes.ACC_PRIVATE,
                    "test",
                    "()V",
                    null,
                    null);
            locals.clear();
            locals.addLocal(new VariableInfo("this", thisClass, thisClass, VariableInfo.NONE));
            testFunction.accept(this);
            currentMV.visitInsn(Opcodes.RETURN);
            currentMV.visitMaxs(0, 0);
            currentMV.visitEnd();
        }
        IRNode printVariables = node.getChild(MainKlass.MainKlassPart.PRINT_VARIABLES.ordinal());
        if (printVariables != null) {
            printVariables.accept(this);
        }

        mainClassWriter.visitEnd();

        byte[] byteCode = mainClassWriter.toByteArray();
        context.register(name, byteCode);
        currentClass = prevClass;
        return byteCode;
    }

    @Override
    public byte[] visit(NonStaticMemberVariable node) {
        // This node is for "reading" only. Writing is handled in BinaryOperator visit(see ASSIGN)
        VariableInfo vi = node.getVariableInfo();
        // put object to stack
        node.getChild(0).accept(this);
        currentMV.visitFieldInsn(Opcodes.GETFIELD, asInternalName(vi.getOwner().getName()), vi.name,
                new String(vi.type.accept(this)));
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(Nothing node) {
        // TODO : add randomness
        currentMV.visitInsn(Opcodes.NOP);
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(PrintVariables node) {
        return FixedTrees.printVariablesAsFunction(node).accept(this);
    }

    @Override
    public byte[] visit(Return node) {
        node.getExpression().accept(this);
        Type result = node.getResultType();
        if (result instanceof TypeKlass) {
            currentMV.visitInsn(Opcodes.ARETURN);
        } else if (result.equals(TypeList.VOID)) {
            currentMV.visitInsn(Opcodes.RETURN);
        } else if (result.equals(TypeList.DOUBLE)) {
            currentMV.visitInsn(Opcodes.DRETURN);
        } else if (result.equals(TypeList.FLOAT)) {
            currentMV.visitInsn(Opcodes.FRETURN);
        } else if (result.equals(TypeList.LONG)) {
            currentMV.visitInsn(Opcodes.LRETURN);
        } else {
            currentMV.visitInsn(Opcodes.IRETURN);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(Statement node) {
        IRNode child = node.getChild(0);
        child.accept(this);
        Type resultType = child.getResultType();
        emitPop(resultType);
        return EMPTY_BYTE_ARRAY;
    }

    private void emitPop(Type resultType) {
        if (resultType.equals(TypeList.LONG) || resultType.equals(TypeList.DOUBLE)) {
            currentMV.visitInsn(Opcodes.POP2);
        } else if (!resultType.equals(TypeList.VOID)) {
            currentMV.visitInsn(Opcodes.POP);
        }
    }

    @Override
    public byte[] visit(StaticConstructorDefinition node) {
        String ownerName = node.getOwner().getName();
        ContextDependedClassWriter cw = classWriters.get(ownerName);
        String descriptor = getDescriptor(node, 1, "V");
        currentMV = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", descriptor, null, null);
        locals.clear();
        IRNode body = node.getChild(0);
        body.accept(this);
        currentMV.visitInsn(Opcodes.RETURN);
        currentMV.visitMaxs(0, 0);
        currentMV.visitEnd();
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(StaticMemberVariable node) {
        // This node is for "reading" only. Writing is handled in BinaryOperator visit(see ASSIGN)
        VariableInfo vi = node.getVariableInfo();
        currentMV.visitFieldInsn(Opcodes.GETSTATIC,
                asInternalName(vi.getOwner().getName()),
                vi.name,
                new String(vi.type.accept(this)));
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(Switch node) {
        node.getChild(0).accept(this);
        int caseBlockIdx = node.getCaseBlockIndex();
        Label defaultCase = new Label();
        IRNode defaultBlock = null;
        SortedMap<Integer, Pair<Label, IRNode>> cases = new TreeMap<>();
        for (int i = 0; i < caseBlockIdx - 1; ++i) {
            if (node.getChild(i + 1) instanceof Nothing) {
                defaultBlock = node.getChild(i + caseBlockIdx);
            } else {
                Literal literal = (Literal) node.getChild(i + 1);
                int value = 0;
                if (literal.value instanceof Integer) {
                    value = (Integer) literal.value;
                } else if (literal.value instanceof Short) {
                    value = (Short) literal.value;
                } else if (literal.value instanceof Byte) {
                    value = (Byte) literal.value;
                } else if (literal.value instanceof Character) {
                    value = (Character) literal.value;
                }
                cases.put(value, new Pair<>(new Label(), node.getChild(i + caseBlockIdx)));
            }
        }
        Label breakLabel = new Label();
        endLabels.push(breakLabel);
        currentMV.visitLookupSwitchInsn(defaultCase,
                cases.keySet().stream()
                        .mapToInt(Integer::intValue)
                        .toArray(),
                cases.values().stream()
                        .map(p -> p.first)
                        .toArray(Label[]::new));
        for (Pair<Label, IRNode> p : cases.values()) {
            currentMV.visitLabel(p.first);
            p.second.accept(this);
        }
        currentMV.visitLabel(defaultCase);
        if (defaultBlock != null) {
            defaultBlock.accept(this);
        }
        Label a = endLabels.pop();
        assert breakLabel == a;
        currentMV.visitLabel(breakLabel);
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(TernaryOperator node) {
        IRNode conditionBlock = node.getChild(TernaryOperator.TernaryPart.CONDITION.ordinal());
        // get the condition type to emit correct if
        conditionBlock.accept(this);
        generateIf(Opcodes.IFEQ, node.getChild(TernaryOperator.TernaryPart.TRUE.ordinal()),
                node.getChild(TernaryOperator.TernaryPart.FALSE.ordinal()));
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(Throw node) {
        node.getThowable().accept(this);
        currentMV.visitInsn(Opcodes.ATHROW);
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(TryCatchBlock node) {
        List<? extends IRNode> children = node.getChildren();
        IRNode tryBlock = children.get(0);
        IRNode finallyBlock = children.get(1);
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label finallyStart = new Label();
        Label finallyEnd = new Label();

        currentMV.visitLabel(tryStart);
        tryBlock.accept(this);
        currentMV.visitLabel(tryEnd);
        finallyBlock.accept(this);
        currentMV.visitJumpInsn(Opcodes.GOTO, finallyEnd);
        VariableInfo exInfo = new VariableInfo("ex", currentClass,
                new TypeKlass("java.lang.Throwable"), VariableInfo.LOCAL);
        int index = locals.addLocal(exInfo);
        for (int i = 2; i < children.size(); ++i) {
            Label handlerBegin = new Label();
            Label handlerEnd = new Label();
            CatchBlock catchBlock = (CatchBlock) children.get(i);
            for (Type t : catchBlock.throwables) {
                currentMV.visitTryCatchBlock(tryStart, tryEnd, handlerBegin, asInternalName(t.getName()));
            }
            currentMV.visitLabel(handlerBegin);
            catchBlock.accept(this);
            currentMV.visitLabel(handlerEnd);
            finallyBlock.accept(this);
            currentMV.visitJumpInsn(Opcodes.GOTO, finallyEnd);
            currentMV.visitTryCatchBlock(handlerBegin, handlerEnd, finallyStart, null);
        }

        currentMV.visitTryCatchBlock(tryStart, tryEnd, finallyStart, null);
        currentMV.visitLabel(finallyStart);
        currentMV.visitVarInsn(Opcodes.ASTORE, index);
        finallyBlock.accept(this);
        currentMV.visitVarInsn(Opcodes.ALOAD, index);
        currentMV.visitInsn(Opcodes.ATHROW);
        currentMV.visitLabel(finallyEnd);
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(Type node) {
        String name;
        if (TypeList.isBuiltIn(node)) {
            switch (node.getName()) {
                case "void":
                    name = "V";
                    break;
                case "boolean":
                    name = "Z";
                    break;
                case "byte":
                    name = "B";
                    break;
                case "char":
                    name = "C";
                    break;
                case "short":
                    name = "S";
                    break;
                case "int":
                    name = "I";
                    break;
                case "long":
                    name = "J";
                    break;
                case "float":
                    name = "F";
                    break;
                case "double":
                    name = "D";
                    break;
                default:
                    throw new IllegalArgumentException("Unknown type '" + node.getName());
            }
        } else {
            name = "L" + asInternalName(node.getName()) + ";";
        }
        return name.getBytes();
    }

    @Override
    public byte[] visit(TypeArray node) {
        String name;
        String prefix = Stream.generate(() -> "[")
                .limit(node.dimensions)
                .collect(Collectors.joining());
        name = prefix + new String(node.getType().accept(this));
        return name.getBytes();
    }

    @Override
    public byte[] visit(UnaryOperator node) {
        OperatorKind opKind = node.getOperationKind();
        IRNode exp = node.getChild(0);
        // argument expression is handled separately for inc and dec operators
        if (opKind != OperatorKind.POST_DEC && opKind != OperatorKind.POST_INC
                && opKind != OperatorKind.PRE_DEC && opKind != OperatorKind.PRE_INC) {
            exp.accept(this);
        }
        Type resultType = exp.getResultType();
        switch (opKind) {
            case NOT:
                Label retTrueForNot = new Label();
                Label endForNot = new Label();
                currentMV.visitJumpInsn(Opcodes.IFEQ, retTrueForNot);
                currentMV.visitInsn(Opcodes.ICONST_0);
                currentMV.visitJumpInsn(Opcodes.GOTO, endForNot);
                currentMV.visitLabel(retTrueForNot);
                currentMV.visitInsn(Opcodes.ICONST_1);
                currentMV.visitLabel(endForNot);
                break;
            case BIT_NOT:
                if (resultType.equals(TypeList.LONG)) {
                    currentMV.visitLdcInsn(-1L);
                    currentMV.visitInsn(Opcodes.LXOR);
                } else {
                    currentMV.visitInsn(Opcodes.ICONST_M1);
                    currentMV.visitInsn(Opcodes.IXOR);
                }
                break;
            case UNARY_MINUS:
                if (resultType.equals(TypeList.LONG)) {
                    currentMV.visitInsn(Opcodes.LNEG);
                } else if (resultType.equals(TypeList.FLOAT)) {
                    currentMV.visitInsn(Opcodes.FNEG);
                } else if (resultType.equals(TypeList.DOUBLE)) {
                    currentMV.visitInsn(Opcodes.DNEG);
                } else {
                    currentMV.visitInsn(Opcodes.INEG);
                }
                break;
            case UNARY_PLUS:
                break;
            case PRE_DEC:
                lowerIncDecUnaryOperator(OperatorKind.SUB, true, node);
                break;
            case POST_DEC:
                lowerIncDecUnaryOperator(OperatorKind.SUB, false, node);
                break;
            case PRE_INC:
                lowerIncDecUnaryOperator(OperatorKind.ADD, true, node);
                break;
            case POST_INC:
                lowerIncDecUnaryOperator(OperatorKind.ADD, false, node);
                break;
            default:
                throw new RuntimeException("Incorrect unary operator: " + opKind);
        }
        return EMPTY_BYTE_ARRAY;
    }

    private void lowerIncDecUnaryOperator(OperatorKind kind, boolean isPrefix, IRNode node) {
        IRNode var = node.getChild(0);
        Literal one;
        Type resultType = node.getResultType();
        if (resultType.equals(TypeList.LONG)) {
            one = new Literal(1L, TypeList.LONG);
        } else if (resultType.equals(TypeList.INT)) {
            one = new Literal(1, TypeList.INT);
        } else if (resultType.equals(TypeList.SHORT)) {
            one = new Literal((short) 1, TypeList.SHORT);
        } else {
            one = new Literal((byte) 1, TypeList.BYTE);
        }
        if (var instanceof NonStaticMemberVariable) {
            IRNode holder = var.getChild(0);
            Type type = holder.getResultType();
            VariableInfo tmpInfo = new VariableInfo("tmpObject_" + tmpObject++,
                    currentClass, type, VariableInfo.LOCAL);
            new Statement(new VariableInitialization(tmpInfo, holder), true).accept(this);
            var = new NonStaticMemberVariable(new LocalVariable(tmpInfo),
                    ((NonStaticMemberVariable) var).getVariableInfo());
        }
        BinaryOperator calculation = new BinaryOperator(kind, resultType, var, one);
        BinaryOperator changeValue = new BinaryOperator(OperatorKind.ASSIGN, resultType, var, calculation);
        Statement finalChangeStatement = new Statement(changeValue, true);
        if (isPrefix) {
            finalChangeStatement.accept(this);
            var.accept(this);
        } else {
            var.accept(this);
            finalChangeStatement.accept(this);
        }
    }

    @Override
    public byte[] visit(VariableDeclaration node) {
        VariableInfo vi = node.getVariableInfo();
        String ownerName = vi.getOwner().getName();
        ContextDependedClassWriter cw = classWriters.get(ownerName);
        String typeName = new String(vi.type.accept(this));
        if (vi.isLocal()) {
            locals.addLocal(vi);
        } else {
            FieldVisitor fv = cw.visitField(asAccessFlags(vi),
                    vi.name,
                    typeName,
                    null /* Generic */,
                    null /* Constant value */);
            fv.visitEnd(); // doesn't need visitAnnotation and visitAttribute
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public byte[] visit(VariableDeclarationBlock node) {
        return iterateBlock(node);
    }

    @Override
    public byte[] visit(While node) {
        Loop loop = node.getLoop();
        loop.initialization.accept(this);
        node.getChild(While.WhilePart.HEADER.ordinal()).accept(this);
        Label currentLoopBegin = new Label();
        beginLabels.push(currentLoopBegin);
        currentMV.visitLabel(currentLoopBegin);
        loop.condition.accept(this);
        assert loop.condition.getResultType() == TypeList.BOOLEAN;
        Label currentLoopEnd = new Label();
        endLabels.push(currentLoopEnd);
        currentMV.visitJumpInsn(Opcodes.IFEQ, currentLoopEnd);
        node.getChild(While.WhilePart.BODY1.ordinal()).accept(this);
        loop.manipulator.accept(this);
        node.getChild(While.WhilePart.BODY2.ordinal()).accept(this);
        node.getChild(While.WhilePart.BODY3.ordinal()).accept(this);
        currentMV.visitJumpInsn(Opcodes.GOTO, currentLoopBegin);
        currentMV.visitLabel(currentLoopEnd);
        Label a = beginLabels.pop();
        assert currentLoopBegin == a;
        a = endLabels.pop();
        assert currentLoopEnd == a;
        return EMPTY_BYTE_ARRAY;
    }

    public byte[] getByteCode(String name) {
        return context.get(name);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private String argTypeToString(ArgumentDeclaration declarations) {
        return new String(declarations.variableInfo.type.accept(this));
    }

    private byte[] iterateBlock(IRNode node) {
        return node.getChildren().stream()
                .map(ch -> ch.accept(this))
                .reduce(new byte[0], ByteCodeVisitor::concat);
    }

    private String getDescriptor(IRNode node, int skipChilds, String returnType) {
        return node.getChildren().stream()
                .skip(skipChilds)
                .map(c -> argTypeToString((ArgumentDeclaration)c))
                .collect(Collectors.joining("", "(", ")" + returnType));
    }

    private static String asInternalName(String type) {
        return type.replace('.', '/');
    }

    private static int asAccessFlags(TypeKlass klass) {
        int attr = Opcodes.ACC_SUPER;
        attr |= klass.isFinal() ? Opcodes.ACC_FINAL : 0;
        attr |= klass.isAbstract() ? Opcodes.ACC_ABSTRACT : 0;
        attr |= klass.isInterface() ? Opcodes.ACC_INTERFACE : 0;

        return attr;
    }

    private static int asAccessFlags(FunctionInfo fi) {
        int result = asAccessFlags((Symbol) fi);
        result |= ProductionParams.enableStrictFP.value() ? Opcodes.ACC_STRICT : 0;
        result |= fi.isSynchronized() ? Opcodes.ACC_SYNCHRONIZED : 0;
        return result;
    }

    private static int asAccessFlags(Symbol s) {
        int attr = 0;
        attr |= s.isPublic() ? Opcodes.ACC_PUBLIC : 0;
        attr |= s.isPrivate() ? Opcodes.ACC_PRIVATE : 0;
        attr |= s.isProtected() ? Opcodes.ACC_PROTECTED : 0;
        attr |= s.isStatic() ? Opcodes.ACC_STATIC : 0;
        attr |= s.isFinal() ? Opcodes.ACC_FINAL : 0;

        return attr;
    }

    private static class LocalVariablesTable {
        private int nextLocalIndex = 0;
        // a map keeping local variable table index for a local variable
        private final HashMap<String, Integer> locals = new HashMap<>();

        public int addLocal(VariableInfo vi) {
            int indexToReturn = nextLocalIndex;
            locals.put(vi.name, nextLocalIndex++);
            if (vi.type.equals(TypeList.DOUBLE) || vi.type.equals(TypeList.LONG)) {
                nextLocalIndex++;
            }
            return indexToReturn;
        }

        public int getLocalIndex(VariableInfo vi) {
            if (!locals.containsKey(vi.name)) {
                throw new NoSuchElementException(vi.name);
            }
            return locals.get(vi.name);
        }

        public void clear() {
            locals.clear();
            nextLocalIndex = 0;
        }

        public void initFunctionArguments(FunctionInfo info) {
            initArguments(null, info);
        }

        public void initConstructorArguments(TypeKlass owner, FunctionInfo info) {
            Objects.requireNonNull(owner, "owner is null");
            initArguments(owner, info);
        }

        private void initArguments(TypeKlass owner, FunctionInfo info) {
            clear();
            if (owner != null) {
                addLocal(new VariableInfo("this", owner, owner, VariableInfo.LOCAL | VariableInfo.INITIALIZED));
            }
            for (VariableInfo vi : info.argTypes) {
                addLocal(vi);
            }
        }
    }

    private static class GeneratedClassesContext {
        private final HashMap<String, byte[]> byteCodes = new HashMap<>();

        public void register(String name, byte[] bytecode) {
            byteCodes.put(name, bytecode);
        }

        public byte[] get(String name) {
            return byteCodes.get(name);
        }
    }


    private static class ContextDependedClassWriter extends ClassWriter {
        public ContextDependedClassWriter(int flags) {
            super(flags);
        }

        protected String getCommonSuperClass(String className1, String className2) {
            TypeKlass type1 = (TypeKlass) TypeList.find(className1.replace('/', '.'));
            TypeKlass type2 = (TypeKlass) TypeList.find(className2.replace('/', '.'));
            if (type1 == null || type2 == null) {
                return super.getCommonSuperClass(className1, className2);
            }

            if (type2.canImplicitlyCastTo(type1)) {
                return className1;
            }
            if (type1.canImplicitlyCastTo(type2)) {
                return className2;
            }
            if (type1.isInterface() || type2.isInterface()) {
                return "java/lang/Object";
            }

            do {
                type1 = type1.getParent();
            } while (!type2.canImplicitlyCastTo(type1));

            return asInternalName(type1.getName());
        }
    }
}
