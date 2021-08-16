/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: Choose.java,v 1.2.4.1 2005/09/01 12:00:14 pvedula Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.bcel.internal.generic.IFEQ;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @LastModified: Oct 2017
 */
final class Choose extends Instruction {

    /**
     * Display the element contents (a lot of when's and an otherwise)
     */
    public void display(int indent) {
        indent(indent);
        Util.println("Choose");
        indent(indent + IndentIncrement);
        displayContents(indent + IndentIncrement);
    }

    /**
     * Translate this Choose element. Generate a test-chain for the various
     * <xsl:when> elements and default to the <xsl:otherwise> if present.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final List<SyntaxTreeNode> whenElements = new ArrayList<>();
        Otherwise otherwise = null;
        Iterator<SyntaxTreeNode> elements = elements();

        // These two are for reporting errors only
        ErrorMsg error = null;
        final int line = getLineNumber();

        // Traverse all child nodes - must be either When or Otherwise
        while (elements.hasNext()) {
            SyntaxTreeNode element = elements.next();
            // Add a When child element
            if (element instanceof When) {
                whenElements.add(element);
            }
            // Add an Otherwise child element
            else if (element instanceof Otherwise) {
                if (otherwise == null) {
                    otherwise = (Otherwise)element;
                }
                else {
                    error = new ErrorMsg(ErrorMsg.MULTIPLE_OTHERWISE_ERR, this);
                    getParser().reportError(Constants.ERROR, error);
                }
            }
            else if (element instanceof Text) {
                ((Text)element).ignore();
            }
            // It is an error if we find some other element here
            else {
                error = new ErrorMsg(ErrorMsg.WHEN_ELEMENT_ERR, this);
                getParser().reportError(Constants.ERROR, error);
            }
        }

        // Make sure that there is at least one <xsl:when> element
        if (whenElements.size() == 0) {
            error = new ErrorMsg(ErrorMsg.MISSING_WHEN_ERR, this);
            getParser().reportError(Constants.ERROR, error);
            return;
        }

        InstructionList il = methodGen.getInstructionList();

        // next element will hold a handle to the beginning of next
        // When/Otherwise if test on current When fails
        BranchHandle nextElement = null;
        List<InstructionHandle> exitHandles = new ArrayList<>();
        InstructionHandle exit = null;

        Enumeration<SyntaxTreeNode> whens = Collections.enumeration(whenElements);
        while (whens.hasMoreElements()) {
            final When when = (When)whens.nextElement();
            final Expression test = when.getTest();

            InstructionHandle truec = il.getEnd();

            if (nextElement != null)
                nextElement.setTarget(il.append(NOP));
            test.translateDesynthesized(classGen, methodGen);

            if (test instanceof FunctionCall) {
                FunctionCall call = (FunctionCall)test;
                try {
                    Type type = call.typeCheck(getParser().getSymbolTable());
                    if (type != Type.Boolean) {
                        test._falseList.add(il.append(new IFEQ(null)));
                    }
                }
                catch (TypeCheckError e) {
                    // handled later!
                }
            }
            // remember end of condition
            truec = il.getEnd();

            // The When object should be ignored completely in case it tests
            // for the support of a non-available element
            if (!when.ignore()) when.translateContents(classGen, methodGen);

            // goto exit after executing the body of when
            exitHandles.add(il.append(new GOTO(null)));
            if (whens.hasMoreElements() || otherwise != null) {
                nextElement = il.append(new GOTO(null));
                test.backPatchFalseList(nextElement);
            }
            else
                test.backPatchFalseList(exit = il.append(NOP));
            test.backPatchTrueList(truec.getNext());
        }

        // Translate any <xsl:otherwise> element
        if (otherwise != null) {
            nextElement.setTarget(il.append(NOP));
            otherwise.translateContents(classGen, methodGen);
            exit = il.append(NOP);
        }

        // now that end is known set targets of exit gotos
        Enumeration<InstructionHandle> exitGotos = Collections.enumeration(exitHandles);
        while (exitGotos.hasMoreElements()) {
            BranchHandle gotoExit = (BranchHandle)exitGotos.nextElement();
            gotoExit.setTarget(exit);
        }
    }
}
