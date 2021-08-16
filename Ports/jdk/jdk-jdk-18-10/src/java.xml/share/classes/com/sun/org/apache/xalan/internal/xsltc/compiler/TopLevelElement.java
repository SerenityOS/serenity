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

import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import java.util.ArrayList;
import java.util.List;

/**
 * @LastModified: Oct 2017
 */
class TopLevelElement extends SyntaxTreeNode {

    /*
     * List of dependencies with other variables, parameters or
     * keys defined at the top level.
     */
    protected List<SyntaxTreeNode> _dependencies = null;

    /**
     * Type check all the children of this node.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        return typeCheckContents(stable);
    }

    /**
     * Translate this node into JVM bytecodes.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        ErrorMsg msg = new ErrorMsg(ErrorMsg.NOT_IMPLEMENTED_ERR,
                                    getClass(), this);
        getParser().reportError(FATAL, msg);
    }

    /**
     * Translate this node into a fresh instruction list.
     * The original instruction list is saved and restored.
     */
    public InstructionList compile(ClassGenerator classGen,
                                   MethodGenerator methodGen) {
        final InstructionList result, save = methodGen.getInstructionList();
        methodGen.setInstructionList(result = new InstructionList());
        translate(classGen, methodGen);
        methodGen.setInstructionList(save);
        return result;
    }

    public void display(int indent) {
        indent(indent);
        Util.println("TopLevelElement");
        displayContents(indent + IndentIncrement);
    }

    /**
     * Add a dependency with other top-level elements like
     * variables, parameters or keys.
     */
    public void addDependency(TopLevelElement other) {
        if (_dependencies == null) {
            _dependencies = new ArrayList<>();
        }
        if (!_dependencies.contains(other)) {
            _dependencies.add(other);
        }
    }

    /**
     * Get the list of dependencies with other top-level elements
     * like variables, parameteres or keys.
     */
    public List<SyntaxTreeNode> getDependencies() {
        return _dependencies;
    }

}
