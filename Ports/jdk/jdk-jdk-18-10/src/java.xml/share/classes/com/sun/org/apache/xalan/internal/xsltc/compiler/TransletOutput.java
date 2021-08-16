/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.StringType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import jdk.xml.internal.JdkXmlFeatures;

/**
 * @author Morten Jorgensen
 */
final class TransletOutput extends Instruction {

    private Expression _filename;
    private boolean _append;

    /**
     * Displays the contents of this <xsltc:output> element.
     */
    public void display(int indent) {
        indent(indent);
        Util.println("TransletOutput: " + _filename);
    }

    /**
     * Parse the contents of this <xsltc:output> element. The only attribute
     * we recognise is the 'file' attribute that contains teh output filename.
     */
    public void parseContents(Parser parser) {
        // Get the output filename from the 'file' attribute
        String filename = getAttribute("file");

        // If the 'append' attribute is set to "yes" or "true",
        // the output is appended to the file.
        String append   = getAttribute("append");

        // Verify that the filename is in fact set
        if ((filename == null) || (filename.equals(EMPTYSTRING))) {
            reportError(this, parser, ErrorMsg.REQUIRED_ATTR_ERR, "file");
        }

        // Save filename as an attribute value template
        _filename = AttributeValue.create(this, filename, parser);

        if (append != null && (append.toLowerCase().equals("yes") ||
            append.toLowerCase().equals("true"))) {
          _append = true;
        }
        else
          _append = false;

        parseChildren(parser);
    }

    /**
     * Type checks the 'file' attribute (must be able to convert it to a str).
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        final Type type = _filename.typeCheck(stable);
        if (type instanceof StringType == false) {
            _filename = new CastExpr(_filename, Type.String);
        }
        typeCheckContents(stable);
        return Type.Void;
    }

    /**
     * Compile code that opens the give file for output, dumps the contents of
     * the element to the file, then closes the file.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        final boolean isSecureProcessing = classGen.getParser().getXSLTC()
                                           .isSecureProcessing();
        final boolean isExtensionFunctionEnabled = classGen.getParser().getXSLTC()
                .getFeature(JdkXmlFeatures.XmlFeature.ENABLE_EXTENSION_FUNCTION);

        if (isSecureProcessing && !isExtensionFunctionEnabled) {
            int index = cpg.addMethodref(BASIS_LIBRARY_CLASS,
                                         "unallowed_extension_elementF",
                                         "(Ljava/lang/String;)V");
            il.append(new PUSH(cpg, "redirect"));
            il.append(new INVOKESTATIC(index));
            return;
        }

        // Save the current output handler on the stack
        il.append(methodGen.loadHandler());

        final int open =  cpg.addMethodref(TRANSLET_CLASS,
                                           "openOutputHandler",
                                           "(" + STRING_SIG + "Z)" +
                                           TRANSLET_OUTPUT_SIG);

        final int close =  cpg.addMethodref(TRANSLET_CLASS,
                                            "closeOutputHandler",
                                            "("+TRANSLET_OUTPUT_SIG+")V");

        // Create the new output handler (leave it on stack)
        il.append(classGen.loadTranslet());
        _filename.translate(classGen, methodGen);
        il.append(new PUSH(cpg, _append));
        il.append(new INVOKEVIRTUAL(open));

        // Overwrite current handler
        il.append(methodGen.storeHandler());

        // Translate contents with substituted handler
        translateContents(classGen, methodGen);

        // Close the output handler (close file)
        il.append(classGen.loadTranslet());
        il.append(methodGen.loadHandler());
        il.append(new INVOKEVIRTUAL(close));

        // Restore old output handler from stack
        il.append(methodGen.storeHandler());
    }
}
