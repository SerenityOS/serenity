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

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.ALOAD;
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Parser;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Stylesheet;

/**
 * The class that implements any class that inherits from
 * <tt>AbstractTranslet</tt>, i.e. any translet. Methods in this
 * class may be of the following kinds:
 *
 * 1. Main method: applyTemplates, implemented by intances of
 * <tt>MethodGenerator</tt>.
 *
 * 2. Named methods: for named templates, implemented by instances
 * of <tt>NamedMethodGenerator</tt>.
 *
 * 3. Rt methods: for result tree fragments, implemented by
 * instances of <tt>RtMethodGenerator</tt>.
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public class ClassGenerator extends ClassGen {
    protected final static int TRANSLET_INDEX = 0;
    protected static int INVALID_INDEX  = -1;

    private Stylesheet _stylesheet;
    private final Parser _parser;               // --> can be moved to XSLT
    // a  single instance cached here
    private final Instruction _aloadTranslet;
    private final String _domClass;
    private final String _domClassSig;
    private final String _applyTemplatesSig;
        private final String _applyTemplatesSigForImport;

    public ClassGenerator(String class_name, String super_class_name,
                          String file_name,
                          int access_flags, String[] interfaces,
                          Stylesheet stylesheet) {
        super(class_name, super_class_name, file_name,
              access_flags, interfaces);
        _stylesheet = stylesheet;
        _parser = stylesheet.getParser();
        _aloadTranslet = new ALOAD(TRANSLET_INDEX);

        if (stylesheet.isMultiDocument()) {
            _domClass = "com.sun.org.apache.xalan.internal.xsltc.dom.MultiDOM";
            _domClassSig = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/MultiDOM;";
        }
        else {
            _domClass = "com.sun.org.apache.xalan.internal.xsltc.dom.DOMAdapter";
            _domClassSig = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/DOMAdapter;";
        }
        _applyTemplatesSig = "("
            + Constants.DOM_INTF_SIG
            + Constants.NODE_ITERATOR_SIG
            + Constants.TRANSLET_OUTPUT_SIG
            + ")V";

    _applyTemplatesSigForImport = "("
        + Constants.DOM_INTF_SIG
        + Constants.NODE_ITERATOR_SIG
        + Constants.TRANSLET_OUTPUT_SIG
        + Constants.NODE_FIELD_SIG
        + ")V";
    }

    public final Parser getParser() {
        return _parser;
    }

    public final Stylesheet getStylesheet() {
        return _stylesheet;
    }

    /**
     * Pretend this is the stylesheet class. Useful when compiling
     * references to global variables inside a predicate.
     */
    public final String getClassName() {
        return _stylesheet.getClassName();
    }

    public Instruction loadTranslet() {
        return _aloadTranslet;
    }

    public final String getDOMClass() {
        return _domClass;
    }

    public final String getDOMClassSig() {
        return _domClassSig;
    }

    public final String getApplyTemplatesSig() {
        return _applyTemplatesSig;
    }

    public final String getApplyTemplatesSigForImport() {
    return _applyTemplatesSigForImport;
    }

    /**
     * Returns <tt>true</tt> or <tt>false</tt> depending on whether
     * this class inherits from <tt>AbstractTranslet</tt> or not.
     */
    public boolean isExternal() {
        return false;
    }
    public void addMethod(MethodGenerator methodGen) {
        Method[] methodsToAdd = methodGen.getGeneratedMethods(this);
        for (int i = 0; i < methodsToAdd.length; i++) {
            addMethod(methodsToAdd[i]);
}
    }
}
