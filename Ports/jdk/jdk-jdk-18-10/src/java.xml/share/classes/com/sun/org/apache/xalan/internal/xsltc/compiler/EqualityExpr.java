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

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.BranchInstruction;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.bcel.internal.generic.IFEQ;
import com.sun.org.apache.bcel.internal.generic.IFNE;
import com.sun.org.apache.bcel.internal.generic.IF_ICMPEQ;
import com.sun.org.apache.bcel.internal.generic.IF_ICMPNE;
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.BooleanType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.IntType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeSetType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NumberType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.RealType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ReferenceType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ResultTreeType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.StringType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.runtime.Operators;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @author Erwin Bolwidt <ejb@klomp.org>
 */
final class EqualityExpr extends Expression {

    private final int _op;
    private Expression _left;
    private Expression _right;

    public EqualityExpr(int op, Expression left, Expression right) {
        _op = op;
        (_left = left).setParent(this);
        (_right = right).setParent(this);
    }

    public void setParser(Parser parser) {
        super.setParser(parser);
        _left.setParser(parser);
        _right.setParser(parser);
    }

    public String toString() {
        return Operators.getOpNames(_op) + '(' + _left + ", " + _right + ')';
    }

    public Expression getLeft() {
        return _left;
    }

    public Expression getRight() {
        return _right;
    }

    public boolean getOp() {
        return (_op != Operators.NE);
    }

    /**
     * Returns true if this expressions contains a call to position(). This is
     * needed for context changes in node steps containing multiple predicates.
     */
    public boolean hasPositionCall() {
        if (_left.hasPositionCall()) return true;
        if (_right.hasPositionCall()) return true;
        return false;
    }

    public boolean hasLastCall() {
        if (_left.hasLastCall()) return true;
        if (_right.hasLastCall()) return true;
        return false;
    }

    private void swapArguments() {
        final Expression temp = _left;
        _left = _right;
        _right = temp;
    }

    /**
     * Typing rules: see XSLT Reference by M. Kay page 345.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        final Type tleft = _left.typeCheck(stable);
        final Type tright = _right.typeCheck(stable);

        if (tleft.isSimple() && tright.isSimple()) {
            if (tleft != tright) {
                if (tleft instanceof BooleanType) {
                    _right = new CastExpr(_right, Type.Boolean);
                }
                else if (tright instanceof BooleanType) {
                    _left = new CastExpr(_left, Type.Boolean);
                }
                else if (tleft instanceof NumberType ||
                         tright instanceof NumberType) {
                    _left = new CastExpr(_left, Type.Real);
                    _right = new CastExpr(_right, Type.Real);
                }
                else {          // both compared as strings
                    _left = new CastExpr(_left,   Type.String);
                    _right = new CastExpr(_right, Type.String);
                }
            }
        }
        else if (tleft instanceof ReferenceType) {
            _right = new CastExpr(_right, Type.Reference);
        }
        else if (tright instanceof ReferenceType) {
            _left = new CastExpr(_left, Type.Reference);
        }
        // the following 2 cases optimize @attr|.|.. = 'string'
        else if (tleft instanceof NodeType && tright == Type.String) {
            _left = new CastExpr(_left, Type.String);
        }
        else if (tleft == Type.String && tright instanceof NodeType) {
            _right = new CastExpr(_right, Type.String);
        }
        // optimize node/node
        else if (tleft instanceof NodeType && tright instanceof NodeType) {
            _left = new CastExpr(_left, Type.String);
            _right = new CastExpr(_right, Type.String);
        }
        else if (tleft instanceof NodeType && tright instanceof NodeSetType) {
            // compare(Node, NodeSet) will be invoked
        }
        else if (tleft instanceof NodeSetType && tright instanceof NodeType) {
            swapArguments();    // for compare(Node, NodeSet)
        }
        else {
            // At least one argument is of type node, node-set or result-tree

            // Promote an expression of type node to node-set
            if (tleft instanceof NodeType) {
                _left = new CastExpr(_left, Type.NodeSet);
            }
            if (tright instanceof NodeType) {
                _right = new CastExpr(_right, Type.NodeSet);
            }

            // If one arg is a node-set then make it the left one
            if (tleft.isSimple() ||
                tleft instanceof ResultTreeType &&
                tright instanceof NodeSetType) {
                swapArguments();
            }

            // Promote integers to doubles to have fewer compares
            if (_right.getType() instanceof IntType) {
                _right = new CastExpr(_right, Type.Real);
            }
        }
        return _type = Type.Boolean;
    }

    public void translateDesynthesized(ClassGenerator classGen,
                                       MethodGenerator methodGen) {
        final Type tleft = _left.getType();
        final InstructionList il = methodGen.getInstructionList();

        if (tleft instanceof BooleanType) {
            _left.translate(classGen, methodGen);
            _right.translate(classGen, methodGen);
        _falseList.add(il.append(_op == Operators.EQ ?
                                     (BranchInstruction)new IF_ICMPNE(null) :
                                     (BranchInstruction)new IF_ICMPEQ(null)));
        }
        else if (tleft instanceof NumberType) {
            _left.translate(classGen, methodGen);
            _right.translate(classGen, methodGen);

            if (tleft instanceof RealType) {
                il.append(DCMPG);
        _falseList.add(il.append(_op == Operators.EQ ?
                                         (BranchInstruction)new IFNE(null) :
                                         (BranchInstruction)new IFEQ(null)));
            }
            else {
            _falseList.add(il.append(_op == Operators.EQ ?
                                         (BranchInstruction)new IF_ICMPNE(null) :
                                         (BranchInstruction)new IF_ICMPEQ(null)));
            }
        }
        else {
            translate(classGen, methodGen);
            desynthesize(classGen, methodGen);
        }
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        final Type tleft = _left.getType();
        Type tright = _right.getType();

        if (tleft instanceof BooleanType || tleft instanceof NumberType) {
            translateDesynthesized(classGen, methodGen);
            synthesize(classGen, methodGen);
            return;
        }

        if (tleft instanceof StringType) {
            final int equals = cpg.addMethodref(STRING_CLASS,
                                                "equals",
                                                "(" + OBJECT_SIG +")Z");
            _left.translate(classGen, methodGen);
            _right.translate(classGen, methodGen);
            il.append(new INVOKEVIRTUAL(equals));

        if (_op == Operators.NE) {
                il.append(ICONST_1);
                il.append(IXOR);                        // not x <-> x xor 1
            }
            return;
        }

        BranchHandle truec, falsec;

        if (tleft instanceof ResultTreeType) {
            if (tright instanceof BooleanType) {
                _right.translate(classGen, methodGen);
        if (_op == Operators.NE) {
                    il.append(ICONST_1);
                    il.append(IXOR); // not x <-> x xor 1
                }
                return;
            }

            if (tright instanceof RealType) {
                _left.translate(classGen, methodGen);
                tleft.translateTo(classGen, methodGen, Type.Real);
                _right.translate(classGen, methodGen);

                il.append(DCMPG);
        falsec = il.append(_op == Operators.EQ ?
                                   (BranchInstruction) new IFNE(null) :
                                   (BranchInstruction) new IFEQ(null));
                il.append(ICONST_1);
                truec = il.append(new GOTO(null));
                falsec.setTarget(il.append(ICONST_0));
                truec.setTarget(il.append(NOP));
                return;
            }

            // Next, result-tree/string and result-tree/result-tree comparisons

            _left.translate(classGen, methodGen);
            tleft.translateTo(classGen, methodGen, Type.String);
            _right.translate(classGen, methodGen);

            if (tright instanceof ResultTreeType) {
                tright.translateTo(classGen, methodGen, Type.String);
            }

            final int equals = cpg.addMethodref(STRING_CLASS,
                                                "equals",
                                                "(" +OBJECT_SIG+ ")Z");
            il.append(new INVOKEVIRTUAL(equals));

        if (_op == Operators.NE) {
                il.append(ICONST_1);
                il.append(IXOR);                        // not x <-> x xor 1
            }
            return;
        }

        if (tleft instanceof NodeSetType && tright instanceof BooleanType) {
            _left.translate(classGen, methodGen);
            _left.startIterator(classGen, methodGen);
            Type.NodeSet.translateTo(classGen, methodGen, Type.Boolean);
            _right.translate(classGen, methodGen);

            il.append(IXOR); // x != y <-> x xor y
        if (_op == Operators.EQ) {
                il.append(ICONST_1);
                il.append(IXOR); // not x <-> x xor 1
            }
            return;
        }

        if (tleft instanceof NodeSetType && tright instanceof StringType) {
            _left.translate(classGen, methodGen);
            _left.startIterator(classGen, methodGen); // needed ?
            _right.translate(classGen, methodGen);
            il.append(new PUSH(cpg, _op));
            il.append(methodGen.loadDOM());
            final int cmp = cpg.addMethodref(BASIS_LIBRARY_CLASS,
                                             "compare",
                                             "("
                                             + tleft.toSignature()
                                             + tright.toSignature()
                                             + "I"
                                             + DOM_INTF_SIG
                                             + ")Z");
            il.append(new INVOKESTATIC(cmp));
            return;
        }

        // Next, node-set/t for t in {real, string, node-set, result-tree}
        _left.translate(classGen, methodGen);
        _left.startIterator(classGen, methodGen);
        _right.translate(classGen, methodGen);
        _right.startIterator(classGen, methodGen);

        // Cast a result tree to a string to use an existing compare
        if (tright instanceof ResultTreeType) {
            tright.translateTo(classGen, methodGen, Type.String);
            tright = Type.String;
        }

        // Call the appropriate compare() from the BasisLibrary
        il.append(new PUSH(cpg, _op));
        il.append(methodGen.loadDOM());

        final int compare = cpg.addMethodref(BASIS_LIBRARY_CLASS,
                                             "compare",
                                             "("
                                             + tleft.toSignature()
                                             + tright.toSignature()
                                             + "I"
                                             + DOM_INTF_SIG
                                             + ")Z");
        il.append(new INVOKESTATIC(compare));
    }
}
