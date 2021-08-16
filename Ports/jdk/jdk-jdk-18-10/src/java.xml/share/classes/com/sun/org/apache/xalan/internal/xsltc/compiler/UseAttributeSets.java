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

import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @LastModified: Oct 2017
 */
final class UseAttributeSets extends Instruction {

    // Only error that can occur:
    private final static String ATTR_SET_NOT_FOUND =
        "";

    // Contains the names of all references attribute sets
    private final List<QName> _sets = new ArrayList<>(2);

    /**
     * Constructur - define initial attribute sets to use
     */
    public UseAttributeSets(String setNames, Parser parser) {
        setParser(parser);
        addAttributeSets(setNames);
    }

    /**
     * This method is made public to enable an AttributeSet object to merge
     * itself with another AttributeSet (including any other AttributeSets
     * the two may inherit from).
     */
    public void addAttributeSets(String setNames) {
        if ((setNames != null) && (!setNames.equals(Constants.EMPTYSTRING))) {
            final StringTokenizer tokens = new StringTokenizer(setNames);
            while (tokens.hasMoreTokens()) {
                final QName qname =
                    getParser().getQNameIgnoreDefaultNs(tokens.nextToken());
                _sets.add(qname);
            }
        }
    }

    /**
     * Do nada.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        return Type.Void;
    }

    /**
     * Generate a call to the method compiled for this attribute set
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {

        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        final SymbolTable symbolTable = getParser().getSymbolTable();

        for (QName name : _sets) {
            // Get the AttributeSet reference from the symbol table
            final AttributeSet attrs = symbolTable.lookupAttributeSet(name);
            // Compile the call to the set's method if the set exists
            if (attrs != null) {
                final String methodName = attrs.getMethodName();
                il.append(classGen.loadTranslet());
                il.append(methodGen.loadDOM());
                il.append(methodGen.loadIterator());
                il.append(methodGen.loadHandler());
                il.append(methodGen.loadCurrentNode());
                final int method = cpg.addMethodref(classGen.getClassName(),
                        methodName, ATTR_SET_SIG);
                il.append(new INVOKESPECIAL(method));
            }
            // Generate an error if the attribute set does not exist
            else {
                final Parser parser = getParser();
                final String atrs = name.toString();
                reportError(this, parser, ErrorMsg.ATTRIBSET_UNDEF_ERR, atrs);
            }
        }
    }
}
