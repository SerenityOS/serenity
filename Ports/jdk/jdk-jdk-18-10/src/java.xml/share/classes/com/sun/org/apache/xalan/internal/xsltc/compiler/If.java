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

import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.BooleanType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 */
final class If extends Instruction {

    private Expression _test;
    private boolean    _ignore = false;

    /**
     * Display the contents of this element
     */
    public void display(int indent) {
        indent(indent);
        Util.println("If");
        indent(indent + IndentIncrement);
        System.out.print("test ");
        Util.println(_test.toString());
        displayContents(indent + IndentIncrement);
    }

    /**
     * Parse the "test" expression and contents of this element.
     */
    public void parseContents(Parser parser) {
        // Parse the "test" expression
        _test = parser.parseExpression(this, "test", null);

        // Make sure required attribute(s) have been set
        if (_test.isDummy()) {
            reportError(this, parser, ErrorMsg.REQUIRED_ATTR_ERR, "test");
            return;
        }

        // Ignore xsl:if when test is false (function-available() and
        // element-available())
        Object result = _test.evaluateAtCompileTime();
        if (result != null && result instanceof Boolean) {
            _ignore = !((Boolean) result).booleanValue();
        }

        parseChildren(parser);
    }

    /**
     * Type-check the "test" expression and contents of this element.
     * The contents will be ignored if we know the test will always fail.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        // Type-check the "test" expression
        if (_test.typeCheck(stable) instanceof BooleanType == false) {
            _test = new CastExpr(_test, Type.Boolean);
        }
        // Type check the element contents
        if (!_ignore) {
            typeCheckContents(stable);
        }
        return Type.Void;
    }

    /**
     * Translate the "test" expression and contents of this element.
     * The contents will be ignored if we know the test will always fail.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final InstructionList il = methodGen.getInstructionList();
        _test.translateDesynthesized(classGen, methodGen);
        // remember end of condition
        final InstructionHandle truec = il.getEnd();
        if (!_ignore) {
            translateContents(classGen, methodGen);
        }
        _test.backPatchFalseList(il.append(NOP));
        _test.backPatchTrueList(truec.getNext());
    }
}
