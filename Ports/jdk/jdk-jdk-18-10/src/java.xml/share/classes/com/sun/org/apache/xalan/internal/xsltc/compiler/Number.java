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
import com.sun.org.apache.bcel.internal.generic.ALOAD;
import com.sun.org.apache.bcel.internal.generic.ASTORE;
import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.CHECKCAST;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.D2I;
import com.sun.org.apache.bcel.internal.generic.GETFIELD;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.bcel.internal.generic.IFNONNULL;
import com.sun.org.apache.bcel.internal.generic.ILOAD;
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.LocalVariableGen;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.bcel.internal.generic.PUTFIELD;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MatchGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeCounterGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.RealType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @LastModified: Nov 2017
 */
final class Number extends Instruction implements Closure {
    private static final int LEVEL_SINGLE   = 0;
    private static final int LEVEL_MULTIPLE = 1;
    private static final int LEVEL_ANY      = 2;

    static final private String[] ClassNames = {
        "com.sun.org.apache.xalan.internal.xsltc.dom.SingleNodeCounter",          // LEVEL_SINGLE
        "com.sun.org.apache.xalan.internal.xsltc.dom.MultipleNodeCounter", // LEVEL_MULTIPLE
        "com.sun.org.apache.xalan.internal.xsltc.dom.AnyNodeCounter"      // LEVEL_ANY
    };

    static final private String[] FieldNames = {
        "___single_node_counter",                  // LEVEL_SINGLE
        "___multiple_node_counter",                // LEVEL_MULTIPLE
        "___any_node_counter"                      // LEVEL_ANY
    };

    private Pattern _from = null;
    private Pattern _count = null;
    private Expression _value = null;

    private AttributeValueTemplate _lang = null;
    private AttributeValueTemplate _format = null;
    private AttributeValueTemplate _letterValue = null;
    private AttributeValueTemplate _groupingSeparator = null;
    private AttributeValueTemplate _groupingSize = null;

    private int _level = LEVEL_SINGLE;
    private boolean _formatNeeded = false;

    private String _className = null;
    private List<VariableRefBase> _closureVars = null;

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
        return null;
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
        }
    }

    // -- End Closure interface ----------------------

   public void parseContents(Parser parser) {
        final int count = _attributes.getLength();

        for (int i = 0; i < count; i++) {
            final String name = _attributes.getQName(i);
            final String value = _attributes.getValue(i);

            if (name.equals("value")) {
                _value = parser.parseExpression(this, name, null);
            }
            else if (name.equals("count")) {
                _count = parser.parsePattern(this, name, null);
            }
            else if (name.equals("from")) {
                _from = parser.parsePattern(this, name, null);
            }
            else if (name.equals("level")) {
                if (value.equals("single")) {
                    _level = LEVEL_SINGLE;
                }
                else if (value.equals("multiple")) {
                    _level = LEVEL_MULTIPLE;
                }
                else if (value.equals("any")) {
                    _level = LEVEL_ANY;
                }
            }
            else if (name.equals("format")) {
                _format = new AttributeValueTemplate(value, parser, this);
                _formatNeeded = true;
            }
            else if (name.equals("lang")) {
                _lang = new AttributeValueTemplate(value, parser, this);
                _formatNeeded = true;
            }
            else if (name.equals("letter-value")) {
                _letterValue = new AttributeValueTemplate(value, parser, this);
                _formatNeeded = true;
            }
            else if (name.equals("grouping-separator")) {
                _groupingSeparator = new AttributeValueTemplate(value, parser, this);
                _formatNeeded = true;
            }
            else if (name.equals("grouping-size")) {
                _groupingSize = new AttributeValueTemplate(value, parser, this);
                _formatNeeded = true;
            }
        }
    }

    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        if (_value != null) {
            Type tvalue = _value.typeCheck(stable);
            if (tvalue instanceof RealType == false) {
                _value = new CastExpr(_value, Type.Real);
            }
        }
        if (_count != null) {
            _count.typeCheck(stable);
        }
        if (_from != null) {
            _from.typeCheck(stable);
        }
        if (_format != null) {
            _format.typeCheck(stable);
        }
        if (_lang != null) {
            _lang.typeCheck(stable);
        }
        if (_letterValue != null) {
            _letterValue.typeCheck(stable);
        }
        if (_groupingSeparator != null) {
            _groupingSeparator.typeCheck(stable);
        }
        if (_groupingSize != null) {
            _groupingSize.typeCheck(stable);
        }
        return Type.Void;
    }

    /**
     * True if the has specified a value for this instance of number.
     */
    public boolean hasValue() {
        return _value != null;
    }

    /**
     * Returns <tt>true</tt> if this instance of number has neither
     * a from nor a count pattern.
     */
    public boolean isDefault() {
        return _from == null && _count == null;
    }

    private void compileDefault(ClassGenerator classGen,
                                MethodGenerator methodGen) {
        int index;
        ConstantPoolGen cpg = classGen.getConstantPool();
        InstructionList il = methodGen.getInstructionList();

        int[] fieldIndexes = getXSLTC().getNumberFieldIndexes();

        if (fieldIndexes[_level] == -1) {
            Field defaultNode = new Field(ACC_PRIVATE,
                                          cpg.addUtf8(FieldNames[_level]),
                                          cpg.addUtf8(NODE_COUNTER_SIG),
                                          null,
                                          cpg.getConstantPool());

            // Add a new private field to this class
            classGen.addField(defaultNode);

            // Get a reference to the newly added field
            fieldIndexes[_level] = cpg.addFieldref(classGen.getClassName(),
                                                   FieldNames[_level],
                                                   NODE_COUNTER_SIG);
        }

        // Check if field is initialized (runtime)
        il.append(classGen.loadTranslet());
        il.append(new GETFIELD(fieldIndexes[_level]));
        final BranchHandle ifBlock1 = il.append(new IFNONNULL(null));

        // Create an instance of DefaultNodeCounter
        index = cpg.addMethodref(ClassNames[_level],
                                 "getDefaultNodeCounter",
                                 "(" + TRANSLET_INTF_SIG
                                 + DOM_INTF_SIG
                                 + NODE_ITERATOR_SIG
                                 + ")" + NODE_COUNTER_SIG);
        il.append(classGen.loadTranslet());
        il.append(methodGen.loadDOM());
        il.append(methodGen.loadIterator());
        il.append(new INVOKESTATIC(index));
        il.append(DUP);

        // Store the node counter in the field
        il.append(classGen.loadTranslet());
        il.append(SWAP);
        il.append(new PUTFIELD(fieldIndexes[_level]));
        final BranchHandle ifBlock2 = il.append(new GOTO(null));

        // Backpatch conditionals
        ifBlock1.setTarget(il.append(classGen.loadTranslet()));
        il.append(new GETFIELD(fieldIndexes[_level]));

        ifBlock2.setTarget(il.append(NOP));
    }

    /**
     * Compiles a constructor for the class <tt>_className</tt> that
     * inherits from {Any,Single,Multiple}NodeCounter. This constructor
     * simply calls the same constructor in the super class.
     */
    private void compileConstructor(ClassGenerator classGen) {
        MethodGenerator cons;
        final InstructionList il = new InstructionList();
        final ConstantPoolGen cpg = classGen.getConstantPool();

        cons = new MethodGenerator(ACC_PUBLIC,
                                   com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                   new com.sun.org.apache.bcel.internal.generic.Type[] {
                                       Util.getJCRefType(TRANSLET_INTF_SIG),
                                       Util.getJCRefType(DOM_INTF_SIG),
                                       Util.getJCRefType(NODE_ITERATOR_SIG),
                                       com.sun.org.apache.bcel.internal.generic.Type.BOOLEAN
                                   },
                                   new String[] {
                                       "dom",
                                       "translet",
                                       "iterator",
                                       "hasFrom"
                                   },
                                   "<init>", _className, il, cpg);

        il.append(ALOAD_0);         // this
        il.append(ALOAD_1);         // translet
        il.append(ALOAD_2);         // DOM
        il.append(new ALOAD(3));    // iterator
        il.append(new ILOAD(4));    // hasFrom

        int index = cpg.addMethodref(ClassNames[_level],
                                     "<init>",
                                     "(" + TRANSLET_INTF_SIG
                                     + DOM_INTF_SIG
                                     + NODE_ITERATOR_SIG
                                     + "Z)V");
        il.append(new INVOKESPECIAL(index));
        il.append(RETURN);

        classGen.addMethod(cons);
    }

    /**
     * This method compiles code that is common to matchesFrom() and
     * matchesCount() in the auxillary class.
     */
    private void compileLocals(NodeCounterGenerator nodeCounterGen,
                               MatchGenerator matchGen,
                               InstructionList il)
    {
        int field;
        LocalVariableGen local;
        ConstantPoolGen cpg = nodeCounterGen.getConstantPool();

        // Get NodeCounter._iterator and store locally
        local = matchGen.addLocalVariable("iterator",
                                          Util.getJCRefType(NODE_ITERATOR_SIG),
                                          null, null);
        field = cpg.addFieldref(NODE_COUNTER, "_iterator",
                                ITERATOR_FIELD_SIG);
        il.append(ALOAD_0); // 'this' pointer on stack
        il.append(new GETFIELD(field));
        local.setStart(il.append(new ASTORE(local.getIndex())));
        matchGen.setIteratorIndex(local.getIndex());

        // Get NodeCounter._translet and store locally
        local = matchGen.addLocalVariable("translet",
                                  Util.getJCRefType(TRANSLET_SIG),
                                  null, null);
        field = cpg.addFieldref(NODE_COUNTER, "_translet",
                                "Lcom/sun/org/apache/xalan/internal/xsltc/Translet;");
        il.append(ALOAD_0); // 'this' pointer on stack
        il.append(new GETFIELD(field));
        il.append(new CHECKCAST(cpg.addClass(TRANSLET_CLASS)));
        local.setStart(il.append(new ASTORE(local.getIndex())));
        nodeCounterGen.setTransletIndex(local.getIndex());

        // Get NodeCounter._document and store locally
        local = matchGen.addLocalVariable("document",
                                          Util.getJCRefType(DOM_INTF_SIG),
                                          null, null);
        field = cpg.addFieldref(_className, "_document", DOM_INTF_SIG);
        il.append(ALOAD_0); // 'this' pointer on stack
        il.append(new GETFIELD(field));
        // Make sure we have the correct DOM type on the stack!!!
        local.setStart(il.append(new ASTORE(local.getIndex())));
        matchGen.setDomIndex(local.getIndex());
    }

    private void compilePatterns(ClassGenerator classGen,
                                 MethodGenerator methodGen)
    {
        int current;
        int field;
        LocalVariableGen local;
        MatchGenerator matchGen;
        NodeCounterGenerator nodeCounterGen;

        _className = getXSLTC().getHelperClassName();
        nodeCounterGen = new NodeCounterGenerator(_className,
                                                  ClassNames[_level],
                                                  toString(),
                                                  ACC_PUBLIC | ACC_SUPER,
                                                  null,
                                                  classGen.getStylesheet());
        InstructionList il = null;
        ConstantPoolGen cpg = nodeCounterGen.getConstantPool();

        // Add a new instance variable for each var in closure
        final int closureLen = (_closureVars == null) ? 0 :
            _closureVars.size();

        for (int i = 0; i < closureLen; i++) {
            VariableBase var = (_closureVars.get(i)).getVariable();

            nodeCounterGen.addField(new Field(ACC_PUBLIC,
                                        cpg.addUtf8(var.getEscapedName()),
                                        cpg.addUtf8(var.getType().toSignature()),
                                        null, cpg.getConstantPool()));
        }

        // Add a single constructor to the class
        compileConstructor(nodeCounterGen);

        /*
         * Compile method matchesFrom()
         */
        if (_from != null) {
            il = new InstructionList();
            matchGen =
                new MatchGenerator(ACC_PUBLIC | ACC_FINAL,
                                   com.sun.org.apache.bcel.internal.generic.Type.BOOLEAN,
                                   new com.sun.org.apache.bcel.internal.generic.Type[] {
                                       com.sun.org.apache.bcel.internal.generic.Type.INT,
                                   },
                                   new String[] {
                                       "node",
                                   },
                                   "matchesFrom", _className, il, cpg);

            compileLocals(nodeCounterGen,matchGen,il);

            // Translate Pattern
            il.append(matchGen.loadContextNode());
            _from.translate(nodeCounterGen, matchGen);
            _from.synthesize(nodeCounterGen, matchGen);
            il.append(IRETURN);

            nodeCounterGen.addMethod(matchGen);
        }

        /*
         * Compile method matchesCount()
         */
        if (_count != null) {
            il = new InstructionList();
            matchGen = new MatchGenerator(ACC_PUBLIC | ACC_FINAL,
                                          com.sun.org.apache.bcel.internal.generic.Type.BOOLEAN,
                                          new com.sun.org.apache.bcel.internal.generic.Type[] {
                                              com.sun.org.apache.bcel.internal.generic.Type.INT,
                                          },
                                          new String[] {
                                              "node",
                                          },
                                          "matchesCount", _className, il, cpg);

            compileLocals(nodeCounterGen,matchGen,il);

            // Translate Pattern
            il.append(matchGen.loadContextNode());
            _count.translate(nodeCounterGen, matchGen);
            _count.synthesize(nodeCounterGen, matchGen);

            il.append(IRETURN);

            nodeCounterGen.addMethod(matchGen);
        }

        getXSLTC().dumpClass(nodeCounterGen.getJavaClass());

        // Push an instance of the newly created class
        cpg = classGen.getConstantPool();
        il = methodGen.getInstructionList();

        final int index = cpg.addMethodref(_className, "<init>",
                                           "(" + TRANSLET_INTF_SIG
                                           + DOM_INTF_SIG
                                           + NODE_ITERATOR_SIG
                                           + "Z)V");
        il.append(new NEW(cpg.addClass(_className)));
        il.append(DUP);
        il.append(classGen.loadTranslet());
        il.append(methodGen.loadDOM());
        il.append(methodGen.loadIterator());
        il.append(_from != null ? ICONST_1 : ICONST_0);
        il.append(new INVOKESPECIAL(index));

        // Initialize closure variables
        for (int i = 0; i < closureLen; i++) {
            final VariableRefBase varRef = _closureVars.get(i);
            final VariableBase var = varRef.getVariable();
            final Type varType = var.getType();

            // Store variable in new closure
            il.append(DUP);
            il.append(var.loadInstruction());
            il.append(new PUTFIELD(
                    cpg.addFieldref(_className, var.getEscapedName(),
                        varType.toSignature())));
        }
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        int index;
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        // Push "this" for the call to characters()
        il.append(classGen.loadTranslet());

        if (hasValue()) {
            compileDefault(classGen, methodGen);
            _value.translate(classGen, methodGen);

            // Using java.lang.Math.floor(number + 0.5) to return a double value
            il.append(new PUSH(cpg, 0.5));
            il.append(DADD);
            index = cpg.addMethodref(MATH_CLASS, "floor", "(D)D");
            il.append(new INVOKESTATIC(index));

            // Call setValue on the node counter
            index = cpg.addMethodref(NODE_COUNTER,
                                     "setValue",
                                     "(D)" + NODE_COUNTER_SIG);
            il.append(new INVOKEVIRTUAL(index));
        }
        else if (isDefault()) {
            compileDefault(classGen, methodGen);
        }
        else {
            compilePatterns(classGen, methodGen);
        }

        // Call setStartNode()
        if (!hasValue()) {
            il.append(methodGen.loadContextNode());
            index = cpg.addMethodref(NODE_COUNTER,
                                     SET_START_NODE,
                                     "(I)" + NODE_COUNTER_SIG);
            il.append(new INVOKEVIRTUAL(index));
        }

        // Call getCounter() with or without args
        if (_formatNeeded) {
            if (_format != null) {
                _format.translate(classGen, methodGen);
            }
            else {
                il.append(new PUSH(cpg, "1"));
            }

            if (_lang != null) {
                _lang.translate(classGen, methodGen);
            }
            else {
                il.append(new PUSH(cpg, "en"));         // TODO ??
            }

            if (_letterValue != null) {
                _letterValue.translate(classGen, methodGen);
            }
            else {
                il.append(new PUSH(cpg, Constants.EMPTYSTRING));
            }

            if (_groupingSeparator != null) {
                _groupingSeparator.translate(classGen, methodGen);
            }
            else {
                il.append(new PUSH(cpg, Constants.EMPTYSTRING));
            }

            if (_groupingSize != null) {
                _groupingSize.translate(classGen, methodGen);
            }
            else {
                il.append(new PUSH(cpg, "0"));
            }

            index = cpg.addMethodref(NODE_COUNTER, "getCounter",
                                     "(" + STRING_SIG + STRING_SIG
                                     + STRING_SIG + STRING_SIG
                                     + STRING_SIG + ")" + STRING_SIG);
            il.append(new INVOKEVIRTUAL(index));
        }
        else {
            index = cpg.addMethodref(NODE_COUNTER, "setDefaultFormatting",
                                     "()" + NODE_COUNTER_SIG);
            il.append(new INVOKEVIRTUAL(index));

            index = cpg.addMethodref(NODE_COUNTER, "getCounter",
                                     "()" + STRING_SIG);
            il.append(new INVOKEVIRTUAL(index));
        }

        // Output the resulting string to the handler
        il.append(methodGen.loadHandler());
        index = cpg.addMethodref(TRANSLET_CLASS,
                                 CHARACTERSW,
                                 CHARACTERSW_SIG);
        il.append(new INVOKEVIRTUAL(index));
    }
}
