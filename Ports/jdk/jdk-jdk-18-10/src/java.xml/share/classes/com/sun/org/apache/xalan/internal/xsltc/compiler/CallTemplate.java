/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import com.sun.org.apache.xml.internal.utils.XML11Char;
import java.util.List;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Erwin Bolwidt <ejb@klomp.org>
 * @LastModified: Oct 2017
 */
final class CallTemplate extends Instruction {

    /**
     * Name of template to call.
     */
    private QName _name;

    /**
     * The array of effective parameters in this CallTemplate. An object in
     * this array can be either a WithParam or a Param if no WithParam
     * exists for a particular parameter.
     */
    private SyntaxTreeNode[] _parameters = null;

    /**
     * The corresponding template which this CallTemplate calls.
     */
    private Template _calleeTemplate = null;

    public void display(int indent) {
        indent(indent);
        System.out.print("CallTemplate");
        Util.println(" name " + _name);
        displayContents(indent + IndentIncrement);
    }

    public boolean hasWithParams() {
        return elementCount() > 0;
    }

    public void parseContents(Parser parser) {
        final String name = getAttribute("name");
        if (name.length() > 0) {
            if (!XML11Char.isXML11ValidQName(name)) {
                ErrorMsg err = new ErrorMsg(ErrorMsg.INVALID_QNAME_ERR, name, this);
                parser.reportError(Constants.ERROR, err);
            }
            _name = parser.getQNameIgnoreDefaultNs(name);
        }
        else {
            reportError(this, parser, ErrorMsg.REQUIRED_ATTR_ERR, "name");
        }
        parseChildren(parser);
    }

    /**
     * Verify that a template with this name exists.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        final Template template = stable.lookupTemplate(_name);
        if (template != null) {
            typeCheckContents(stable);
        }
        else {
            ErrorMsg err = new ErrorMsg(ErrorMsg.TEMPLATE_UNDEF_ERR,_name,this);
            throw new TypeCheckError(err);
        }
        return Type.Void;
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final Stylesheet stylesheet = classGen.getStylesheet();
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        // If there are Params in the stylesheet or WithParams in this call?
        if (stylesheet.hasLocalParams() || hasContents()) {
            _calleeTemplate = getCalleeTemplate();

            // Build the parameter list if the called template is simple named
            if (_calleeTemplate != null) {
                buildParameterList();
            }
            // This is only needed when the called template is not
            // a simple named template.
            else {
                // Push parameter frame
                final int push = cpg.addMethodref(TRANSLET_CLASS,
                                                  PUSH_PARAM_FRAME,
                                                  PUSH_PARAM_FRAME_SIG);
                il.append(classGen.loadTranslet());
                il.append(new INVOKEVIRTUAL(push));
                translateContents(classGen, methodGen);
            }
        }

        // Generate a valid Java method name
        final String className = stylesheet.getClassName();
        String methodName = Util.escape(_name.toString());

        // Load standard arguments
        il.append(classGen.loadTranslet());
        il.append(methodGen.loadDOM());
        il.append(methodGen.loadIterator());
        il.append(methodGen.loadHandler());
        il.append(methodGen.loadCurrentNode());

        // Initialize prefix of method signature
        StringBuffer methodSig = new StringBuffer("(" + DOM_INTF_SIG
            + NODE_ITERATOR_SIG + TRANSLET_OUTPUT_SIG + NODE_SIG);

        // If calling a simply named template, push actual arguments
        if (_calleeTemplate != null) {
            int numParams = _parameters.length;

            for (int i = 0; i < numParams; i++) {
                SyntaxTreeNode node = _parameters[i];
                methodSig.append(OBJECT_SIG);   // append Object to signature

                // Push 'null' if Param to indicate no actual parameter specified
                if (node instanceof Param) {
                    il.append(ACONST_NULL);
                }
                else {  // translate WithParam
                    node.translate(classGen, methodGen);
                }
            }
        }

        // Complete signature and generate invokevirtual call
        methodSig.append(")V");
        il.append(new INVOKEVIRTUAL(cpg.addMethodref(className,
                                                     methodName,
                                                     methodSig.toString())));

        // release temporary result trees
        if (_parameters != null) {
            for (int i = 0; i < _parameters.length; i++) {
                if (_parameters[i] instanceof WithParam) {
                    ((WithParam)_parameters[i]).releaseResultTree(classGen, methodGen);
                }
            }
        }

        // Do not need to call Translet.popParamFrame() if we are
        // calling a simple named template.
        if (_calleeTemplate == null && (stylesheet.hasLocalParams() || hasContents())) {
            // Pop parameter frame
            final int pop = cpg.addMethodref(TRANSLET_CLASS,
                                             POP_PARAM_FRAME,
                                             POP_PARAM_FRAME_SIG);
            il.append(classGen.loadTranslet());
            il.append(new INVOKEVIRTUAL(pop));
        }
    }

    /**
     * Return the simple named template which this CallTemplate calls.
     * Return false if there is no matched template or the matched
     * template is not a simple named template.
     */
    public Template getCalleeTemplate() {
        Template foundTemplate
            = getXSLTC().getParser().getSymbolTable().lookupTemplate(_name);

        return foundTemplate.isSimpleNamedTemplate() ? foundTemplate : null;
    }

    /**
     * Build the list of effective parameters in this CallTemplate.
     * The parameters of the called template are put into the array first.
     * Then we visit the WithParam children of this CallTemplate and replace
     * the Param with a corresponding WithParam having the same name.
     */
    private void buildParameterList() {
        // Put the parameters from the called template into the array first.
        // This is to ensure the order of the parameters.
        List<Param> defaultParams = _calleeTemplate.getParameters();
        int numParams = defaultParams.size();
        _parameters = new SyntaxTreeNode[numParams];
        for (int i = 0; i < numParams; i++) {
            _parameters[i] = defaultParams.get(i);
        }

        // Replace a Param with a WithParam if they have the same name.
        int count = elementCount();
        for (int i = 0; i < count; i++) {
            Object node = elementAt(i);

            // Ignore if not WithParam
            if (node instanceof WithParam) {
                WithParam withParam = (WithParam)node;
                QName name = withParam.getName();

                // Search for a Param with the same name
                for (int k = 0; k < numParams; k++) {
                    SyntaxTreeNode parm = _parameters[k];
                    if (parm instanceof Param
                        && ((Param)parm).getName().equals(name)) {
                        withParam.setDoParameterOptimization(true);
                        _parameters[k] = withParam;
                        break;
                    }
                    else if (parm instanceof WithParam
                        && ((WithParam)parm).getName().equals(name)) {
                        withParam.setDoParameterOptimization(true);
                        _parameters[k] = withParam;
                        break;
                    }
                }
            }
        }
     }
}
