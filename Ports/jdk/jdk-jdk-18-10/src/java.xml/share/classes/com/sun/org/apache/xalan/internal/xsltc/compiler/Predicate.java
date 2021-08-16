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

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.generic.ASTORE;
import com.sun.org.apache.bcel.internal.generic.CHECKCAST;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.GETFIELD;
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.LocalVariableGen;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.bcel.internal.generic.PUTFIELD;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.BooleanType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.FilterGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.IntType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NumberType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ReferenceType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ResultTreeType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TestGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import com.sun.org.apache.xalan.internal.xsltc.runtime.Operators;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @LastModified: Oct 2017
 */
final class Predicate extends Expression implements Closure {

    /**
     * The predicate's expression.
     */
    private Expression _exp = null;

    /**
     * This flag indicates if optimizations are turned on. The
     * method <code>dontOptimize()</code> can be called to turn
     * optimizations off.
     */
    private boolean _canOptimize = true;

    /**
     * Flag indicatig if the nth position optimization is on. It
     * is set in <code>typeCheck()</code>.
     */
    private boolean _nthPositionFilter = false;

    /**
     * Flag indicatig if the nth position descendant is on. It
     * is set in <code>typeCheck()</code>.
     */
    private boolean _nthDescendant = false;

    /**
     * Cached node type of the expression that owns this predicate.
     */
    int _ptype = -1;

    /**
     * Name of the inner class.
     */
    private String _className = null;

    /**
     * List of variables in closure.
     */
    private List<VariableRefBase> _closureVars = null;

    /**
     * Reference to parent closure.
     */
    private Closure _parentClosure = null;

    /**
     * Cached value of method <code>getCompareValue()</code>.
     */
    private Expression _value = null;

    /**
     * Cached value of method <code>getCompareValue()</code>.
     */
    private Step _step = null;

    /**
     * Initializes a predicate.
     */
    public Predicate(Expression exp) {
        _exp = exp;
        _exp.setParent(this);

    }

    /**
     * Set the parser for this expression.
     */
    public void setParser(Parser parser) {
        super.setParser(parser);
        _exp.setParser(parser);
    }

    /**
     * Returns a boolean value indicating if the nth position optimization
     * is on. Must be call after type checking!
     */
    public boolean isNthPositionFilter() {
        return _nthPositionFilter;
    }

    /**
     * Returns a boolean value indicating if the nth descendant optimization
     * is on. Must be call after type checking!
     */
    public boolean isNthDescendant() {
        return _nthDescendant;
    }

    /**
     * Turns off all optimizations for this predicate.
     */
    public void dontOptimize() {
        _canOptimize = false;
    }

    /**
     * Returns true if the expression in this predicate contains a call
     * to position().
     */
    public boolean hasPositionCall() {
        return _exp.hasPositionCall();
    }

    /**
     * Returns true if the expression in this predicate contains a call
     * to last().
     */
    public boolean hasLastCall() {
        return _exp.hasLastCall();
    }

    // -- Begin Closure interface --------------------

    /**
     * Returns true if this closure is compiled in an inner class (i.e.
     * if this is a real closure).
     */
    public boolean inInnerClass() {
        return (_className != null);
    }

    /**
     * Returns a reference to its parent closure or null if outermost.
     */
    public Closure getParentClosure() {
        if (_parentClosure == null) {
            SyntaxTreeNode node = getParent();
            do {
                if (node instanceof Closure) {
                    _parentClosure = (Closure) node;
                    break;
                }
                if (node instanceof TopLevelElement) {
                    break;      // way up in the tree
                }
                node = node.getParent();
            } while (node != null);
        }
        return _parentClosure;
    }

    /**
     * Returns the name of the auxiliary class or null if this predicate
     * is compiled inside the Translet.
     */
    public String getInnerClassName() {
        return _className;
    }

    /**
     * Add new variable to the closure.
     */
    public void addVariable(VariableRefBase variableRef) {
        if (_closureVars == null) {
            _closureVars = new ArrayList<>();
        }

        // Only one reference per variable
        if (!_closureVars.contains(variableRef)) {
            _closureVars.add(variableRef);

            // Add variable to parent closure as well
            Closure parentClosure = getParentClosure();
            if (parentClosure != null) {
                parentClosure.addVariable(variableRef);
            }
        }
    }

    // -- End Closure interface ----------------------

    /**
     * Returns the node type of the expression owning this predicate. The
     * return value is cached in <code>_ptype</code>.
     */
    public int getPosType() {
        if (_ptype == -1) {
            SyntaxTreeNode parent = getParent();
            if (parent instanceof StepPattern) {
                _ptype = ((StepPattern)parent).getNodeType();
            }
            else if (parent instanceof AbsoluteLocationPath) {
                AbsoluteLocationPath path = (AbsoluteLocationPath)parent;
                Expression exp = path.getPath();
                if (exp instanceof Step) {
                    _ptype = ((Step)exp).getNodeType();
                }
            }
            else if (parent instanceof VariableRefBase) {
                final VariableRefBase ref = (VariableRefBase)parent;
                final VariableBase var = ref.getVariable();
                final Expression exp = var.getExpression();
                if (exp instanceof Step) {
                    _ptype = ((Step)exp).getNodeType();
                }
            }
            else if (parent instanceof Step) {
                _ptype = ((Step)parent).getNodeType();
            }
        }
        return _ptype;
    }

    public boolean parentIsPattern() {
        return (getParent() instanceof Pattern);
    }

    public Expression getExpr() {
        return _exp;
    }

    public String toString() {
        return "pred(" + _exp + ')';
    }

    /**
     * Type check a predicate expression. If the type of the expression is
     * number convert it to boolean by adding a comparison with position().
     * Note that if the expression is a parameter, we cannot distinguish
     * at compile time if its type is number or not. Hence, expressions of
     * reference type are always converted to booleans.
     *
     * This method may be called twice, before and after calling
     * <code>dontOptimize()</code>. If so, the second time it should honor
     * the new value of <code>_canOptimize</code>.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        Type texp = _exp.typeCheck(stable);

        // We need explicit type information for reference types - no good!
        if (texp instanceof ReferenceType) {
            _exp = new CastExpr(_exp, texp = Type.Real);
        }

        // A result tree fragment should not be cast directly to a number type,
        // but rather to a boolean value, and then to a numer (0 or 1).
        // Ref. section 11.2 of the XSLT 1.0 spec
        if (texp instanceof ResultTreeType) {
            _exp = new CastExpr(_exp, Type.Boolean);
            _exp = new CastExpr(_exp, Type.Real);
            texp = _exp.typeCheck(stable);
        }

        // Numerical types will be converted to a position filter
        if (texp instanceof NumberType) {
            // Cast any numerical types to an integer
            if (texp instanceof IntType == false) {
                _exp = new CastExpr(_exp, Type.Int);
            }

            if (_canOptimize) {
                // Nth position optimization. Expression must not depend on context
                _nthPositionFilter =
                    !_exp.hasLastCall() && !_exp.hasPositionCall();

                // _nthDescendant optimization - only if _nthPositionFilter is on
                if (_nthPositionFilter) {
                    SyntaxTreeNode parent = getParent();
                    _nthDescendant = (parent instanceof Step) &&
                        (parent.getParent() instanceof AbsoluteLocationPath);
                    return _type = Type.NodeSet;
                }
            }

           // Reset optimization flags
            _nthPositionFilter = _nthDescendant = false;

           // Otherwise, expand [e] to [position() = e]
           final QName position =
                getParser().getQNameIgnoreDefaultNs("position");
           final PositionCall positionCall =
                new PositionCall(position);
           positionCall.setParser(getParser());
           positionCall.setParent(this);

           _exp = new EqualityExpr(Operators.EQ, positionCall,
                                    _exp);
           if (_exp.typeCheck(stable) != Type.Boolean) {
               _exp = new CastExpr(_exp, Type.Boolean);
           }
           return _type = Type.Boolean;
        }
        else {
            // All other types will be handled as boolean values
            if (texp instanceof BooleanType == false) {
                _exp = new CastExpr(_exp, Type.Boolean);
            }
            return _type = Type.Boolean;
        }
    }

    /**
     * Create a new "Filter" class implementing
     * <code>CurrentNodeListFilter</code>. Allocate registers for local
     * variables and local parameters passed in the closure to test().
     * Notice that local variables need to be "unboxed".
     */
    private void compileFilter(ClassGenerator classGen,
                               MethodGenerator methodGen) {
        TestGenerator testGen;
        LocalVariableGen local;
        FilterGenerator filterGen;

        _className = getXSLTC().getHelperClassName();
        filterGen = new FilterGenerator(_className,
                                        "java.lang.Object",
                                        toString(),
                                        ACC_PUBLIC | ACC_SUPER,
                                        new String[] {
                                            CURRENT_NODE_LIST_FILTER
                                        },
                                        classGen.getStylesheet());

        final ConstantPoolGen cpg = filterGen.getConstantPool();
        final int length = (_closureVars == null) ? 0 : _closureVars.size();

        // Add a new instance variable for each var in closure
        for (int i = 0; i < length; i++) {
            VariableBase var = (_closureVars.get(i)).getVariable();

            filterGen.addField(new Field(ACC_PUBLIC,
                                        cpg.addUtf8(var.getEscapedName()),
                                        cpg.addUtf8(var.getType().toSignature()),
                                        null, cpg.getConstantPool()));
        }

        final InstructionList il = new InstructionList();
        testGen = new TestGenerator(ACC_PUBLIC | ACC_FINAL,
                                    com.sun.org.apache.bcel.internal.generic.Type.BOOLEAN,
                                    new com.sun.org.apache.bcel.internal.generic.Type[] {
                                        com.sun.org.apache.bcel.internal.generic.Type.INT,
                                        com.sun.org.apache.bcel.internal.generic.Type.INT,
                                        com.sun.org.apache.bcel.internal.generic.Type.INT,
                                        com.sun.org.apache.bcel.internal.generic.Type.INT,
                                        Util.getJCRefType(TRANSLET_SIG),
                                        Util.getJCRefType(NODE_ITERATOR_SIG)
                                    },
                                    new String[] {
                                        "node",
                                        "position",
                                        "last",
                                        "current",
                                        "translet",
                                        "iterator"
                                    },
                                    "test", _className, il, cpg);

        // Store the dom in a local variable
        local = testGen.addLocalVariable("document",
                                         Util.getJCRefType(DOM_INTF_SIG),
                                         null, null);
        final String className = classGen.getClassName();
        il.append(filterGen.loadTranslet());
        il.append(new CHECKCAST(cpg.addClass(className)));
        il.append(new GETFIELD(cpg.addFieldref(className,
                                               DOM_FIELD, DOM_INTF_SIG)));
        local.setStart(il.append(new ASTORE(local.getIndex())));

        // Store the dom index in the test generator
        testGen.setDomIndex(local.getIndex());

        _exp.translate(filterGen, testGen);
        il.append(IRETURN);

        filterGen.addEmptyConstructor(ACC_PUBLIC);
        filterGen.addMethod(testGen);

        getXSLTC().dumpClass(filterGen.getJavaClass());
    }

    /**
     * Returns true if the predicate is a test for the existance of an
     * element or attribute. All we have to do is to get the first node
     * from the step, check if it is there, and then return true/false.
     */
    public boolean isBooleanTest() {
        return (_exp instanceof BooleanExpr);
    }

    /**
     * Method to see if we can optimise the predicate by using a specialised
     * iterator for expressions like '/foo/bar[@attr = $var]', which are
     * very common in many stylesheets
     */
    public boolean isNodeValueTest() {
        if (!_canOptimize) return false;
        return (getStep() != null && getCompareValue() != null);
    }

   /**
     * Returns the step in an expression of the form 'step = value'.
     * Null is returned if the expression is not of the right form.
     * Optimization if off if null is returned.
     */
    public Step getStep() {
        // Returned cached value if called more than once
        if (_step != null) {
            return _step;
        }

        // Nothing to do if _exp is null
        if (_exp == null) {
            return null;
        }

        // Ignore if not equality expression
        if (_exp instanceof EqualityExpr) {
            EqualityExpr exp = (EqualityExpr)_exp;
            Expression left = exp.getLeft();
            Expression right = exp.getRight();

            // Unwrap and set _step if appropriate
            if (left instanceof CastExpr) {
                left = ((CastExpr) left).getExpr();
            }
            if (left instanceof Step) {
                _step = (Step) left;
            }

            // Unwrap and set _step if appropriate
            if (right instanceof CastExpr) {
                right = ((CastExpr)right).getExpr();
            }
            if (right instanceof Step) {
                _step = (Step)right;
            }
        }
        return _step;
    }

    /**
     * Returns the value in an expression of the form 'step = value'.
     * A value may be either a literal string or a variable whose
     * type is string. Optimization if off if null is returned.
     */
    public Expression getCompareValue() {
        // Returned cached value if called more than once
        if (_value != null) {
            return _value;
        }

        // Nothing to to do if _exp is null
        if (_exp == null) {
            return null;
        }

        // Ignore if not an equality expression
        if (_exp instanceof EqualityExpr) {
            EqualityExpr exp = (EqualityExpr) _exp;
            Expression left = exp.getLeft();
            Expression right = exp.getRight();

            // Return if left is literal string
            if (left instanceof LiteralExpr) {
                _value = left;
                return _value;
            }
            // Return if left is a variable reference of type string
            if (left instanceof VariableRefBase &&
                left.getType() == Type.String)
            {
                _value = left;
                return _value;
            }

            // Return if right is literal string
            if (right instanceof LiteralExpr) {
                _value = right;
                return _value;
            }
            // Return if left is a variable reference whose type is string
            if (right instanceof VariableRefBase &&
                right.getType() == Type.String)
            {
                _value = right;
                return _value;
            }
        }
        return null;
    }

    /**
     * Translate a predicate expression. This translation pushes
     * two references on the stack: a reference to a newly created
     * filter object and a reference to the predicate's closure.
     */
    public void translateFilter(ClassGenerator classGen,
                                MethodGenerator methodGen)
    {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        // Compile auxiliary class for filter
        compileFilter(classGen, methodGen);

        // Create new instance of filter
        il.append(new NEW(cpg.addClass(_className)));
        il.append(DUP);
        il.append(new INVOKESPECIAL(cpg.addMethodref(_className,
                                                     "<init>", "()V")));

        // Initialize closure variables
        final int length = (_closureVars == null) ? 0 : _closureVars.size();

        for (int i = 0; i < length; i++) {
            VariableRefBase varRef = _closureVars.get(i);
            VariableBase var = varRef.getVariable();
            Type varType = var.getType();

            il.append(DUP);

            // Find nearest closure implemented as an inner class
            Closure variableClosure = _parentClosure;
            while (variableClosure != null) {
                if (variableClosure.inInnerClass()) break;
                variableClosure = variableClosure.getParentClosure();
            }

            // Use getfield if in an inner class
            if (variableClosure != null) {
                il.append(ALOAD_0);
                il.append(new GETFIELD(
                    cpg.addFieldref(variableClosure.getInnerClassName(),
                        var.getEscapedName(), varType.toSignature())));
            }
            else {
                // Use a load of instruction if in translet class
                il.append(var.loadInstruction());
            }

            // Store variable in new closure
            il.append(new PUTFIELD(
                    cpg.addFieldref(_className, var.getEscapedName(),
                        varType.toSignature())));
        }
    }

    /**
     * Translate a predicate expression. If non of the optimizations apply
     * then this translation pushes two references on the stack: a reference
     * to a newly created filter object and a reference to the predicate's
     * closure. See class <code>Step</code> for further details.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {

        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        if (_nthPositionFilter || _nthDescendant) {
            _exp.translate(classGen, methodGen);
        }
        else if (isNodeValueTest() && (getParent() instanceof Step)) {
            _value.translate(classGen, methodGen);
            il.append(new CHECKCAST(cpg.addClass(STRING_CLASS)));
            il.append(new PUSH(cpg, ((EqualityExpr)_exp).getOp()));
        }
        else {
            translateFilter(classGen, methodGen);
        }
    }
}
