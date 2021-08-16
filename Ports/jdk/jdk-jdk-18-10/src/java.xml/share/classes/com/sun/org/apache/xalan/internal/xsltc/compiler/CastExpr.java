/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: CastExpr.java,v 1.2.4.1 2005/09/12 10:06:35 pvedula Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.IF_ICMPNE;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.SIPUSH;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.BooleanType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MultiHashtable;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ResultTreeType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xml.internal.dtm.Axis;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @author Erwin Bolwidt <ejb@klomp.org>
 */
final class CastExpr extends Expression {
    private final Expression _left;

    /**
     * Legal conversions between internal types.
     */
    private static final MultiHashtable<Type, Type> InternalTypeMap = new MultiHashtable<>();

    static {
        // Possible type conversions between internal types
        InternalTypeMap.put(Type.Boolean, Type.Boolean);
        InternalTypeMap.put(Type.Boolean, Type.Real);
        InternalTypeMap.put(Type.Boolean, Type.String);
        InternalTypeMap.put(Type.Boolean, Type.Reference);
        InternalTypeMap.put(Type.Boolean, Type.Object);

        InternalTypeMap.put(Type.Real, Type.Real);
        InternalTypeMap.put(Type.Real, Type.Int);
        InternalTypeMap.put(Type.Real, Type.Boolean);
        InternalTypeMap.put(Type.Real, Type.String);
        InternalTypeMap.put(Type.Real, Type.Reference);
        InternalTypeMap.put(Type.Real, Type.Object);

        InternalTypeMap.put(Type.Int, Type.Int);
        InternalTypeMap.put(Type.Int, Type.Real);
        InternalTypeMap.put(Type.Int, Type.Boolean);
        InternalTypeMap.put(Type.Int, Type.String);
        InternalTypeMap.put(Type.Int, Type.Reference);
        InternalTypeMap.put(Type.Int, Type.Object);

        InternalTypeMap.put(Type.String, Type.String);
        InternalTypeMap.put(Type.String, Type.Boolean);
        InternalTypeMap.put(Type.String, Type.Real);
        InternalTypeMap.put(Type.String, Type.Reference);
        InternalTypeMap.put(Type.String, Type.Object);

        InternalTypeMap.put(Type.NodeSet, Type.NodeSet);
        InternalTypeMap.put(Type.NodeSet, Type.Boolean);
        InternalTypeMap.put(Type.NodeSet, Type.Real);
        InternalTypeMap.put(Type.NodeSet, Type.String);
        InternalTypeMap.put(Type.NodeSet, Type.Node);
        InternalTypeMap.put(Type.NodeSet, Type.Reference);
        InternalTypeMap.put(Type.NodeSet, Type.Object);

        InternalTypeMap.put(Type.Node, Type.Node);
        InternalTypeMap.put(Type.Node, Type.Boolean);
        InternalTypeMap.put(Type.Node, Type.Real);
        InternalTypeMap.put(Type.Node, Type.String);
        InternalTypeMap.put(Type.Node, Type.NodeSet);
        InternalTypeMap.put(Type.Node, Type.Reference);
        InternalTypeMap.put(Type.Node, Type.Object);

        InternalTypeMap.put(Type.ResultTree, Type.ResultTree);
        InternalTypeMap.put(Type.ResultTree, Type.Boolean);
        InternalTypeMap.put(Type.ResultTree, Type.Real);
        InternalTypeMap.put(Type.ResultTree, Type.String);
        InternalTypeMap.put(Type.ResultTree, Type.NodeSet);
        InternalTypeMap.put(Type.ResultTree, Type.Reference);
        InternalTypeMap.put(Type.ResultTree, Type.Object);

        InternalTypeMap.put(Type.Reference, Type.Reference);
        InternalTypeMap.put(Type.Reference, Type.Boolean);
        InternalTypeMap.put(Type.Reference, Type.Int);
        InternalTypeMap.put(Type.Reference, Type.Real);
        InternalTypeMap.put(Type.Reference, Type.String);
        InternalTypeMap.put(Type.Reference, Type.Node);
        InternalTypeMap.put(Type.Reference, Type.NodeSet);
        InternalTypeMap.put(Type.Reference, Type.ResultTree);
        InternalTypeMap.put(Type.Reference, Type.Object);

        InternalTypeMap.put(Type.Object, Type.String);

        InternalTypeMap.put(Type.Void, Type.String);

        InternalTypeMap.makeUnmodifiable();
    }

    private boolean _typeTest = false;

    /**
     * Construct a cast expression and check that the conversion is
     * valid by calling typeCheck().
     */
    public CastExpr(Expression left, Type type) throws TypeCheckError {
        _left = left;
        _type = type;           // use inherited field

        if ((_left instanceof Step) && (_type == Type.Boolean)) {
            Step step = (Step)_left;
            if ((step.getAxis() == Axis.SELF) && (step.getNodeType() != -1))
                _typeTest = true;
        }

        // check if conversion is valid
        setParser(left.getParser());
        setParent(left.getParent());
        left.setParent(this);
        typeCheck(left.getParser().getSymbolTable());
    }

    public Expression getExpr() {
        return _left;
    }

    /**
     * Returns true if this expressions contains a call to position(). This is
     * needed for context changes in node steps containing multiple predicates.
     */
    public boolean hasPositionCall() {
        return(_left.hasPositionCall());
    }

    public boolean hasLastCall() {
        return(_left.hasLastCall());
    }

    public String toString() {
        return "cast(" + _left + ", " + _type + ")";
    }

    /**
     * Type checking a cast expression amounts to verifying that the
     * type conversion is legal. Cast expressions are created during
     * type checking, but typeCheck() is usually not called on them.
     * As a result, this method is called from the constructor.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        Type tleft = _left.getType();
        if (tleft == null) {
            tleft = _left.typeCheck(stable);
        }
        if (tleft instanceof NodeType) {
            tleft = Type.Node;  // multiple instances
        }
        else if (tleft instanceof ResultTreeType) {
            tleft = Type.ResultTree; // multiple instances
        }
        if (InternalTypeMap.maps(tleft, _type) != null) {
            return _type;
        }
        // throw new TypeCheckError(this);
        throw new TypeCheckError(new ErrorMsg(
            ErrorMsg.DATA_CONVERSION_ERR, tleft.toString(), _type.toString()));
    }

    public void translateDesynthesized(ClassGenerator classGen,
                                       MethodGenerator methodGen) {
        FlowList fl;
        final Type ltype = _left.getType();

        // This is a special case for the self:: axis. Instead of letting
        // the Step object create and iterator that we cast back to a single
        // node, we simply ask the DOM for the node type.
        if (_typeTest) {
            final ConstantPoolGen cpg = classGen.getConstantPool();
            final InstructionList il = methodGen.getInstructionList();

            final int idx = cpg.addInterfaceMethodref(DOM_INTF,
                                                      "getExpandedTypeID",
                                                      "(I)I");
            il.append(new SIPUSH((short)((Step)_left).getNodeType()));
            il.append(methodGen.loadDOM());
            il.append(methodGen.loadContextNode());
            il.append(new INVOKEINTERFACE(idx, 2));
            _falseList.add(il.append(new IF_ICMPNE(null)));
        }
        else {

            _left.translate(classGen, methodGen);
            if (_type != ltype) {
                _left.startIterator(classGen, methodGen);
                if (_type instanceof BooleanType) {
                    fl = ltype.translateToDesynthesized(classGen, methodGen,
                                                        _type);
                    if (fl != null) {
                        _falseList.append(fl);
                    }
                }
                else {
                    ltype.translateTo(classGen, methodGen, _type);
                }
            }
        }
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final Type ltype = _left.getType();
        _left.translate(classGen, methodGen);
        if (_type.identicalTo(ltype) == false) {
            _left.startIterator(classGen, methodGen);
            ltype.translateTo(classGen, methodGen, _type);
        }
    }
}
