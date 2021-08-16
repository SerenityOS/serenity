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

import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xml.internal.utils.XML11Char;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 */
final class DecimalFormatting extends TopLevelElement {

    private static final String DFS_CLASS = "java.text.DecimalFormatSymbols";
    private static final String DFS_SIG   = "Ljava/text/DecimalFormatSymbols;";

    private QName _name = null;

    /**
     * No type check needed for the <xsl:decimal-formatting/> element
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        return Type.Void;
    }

    /**
     * Parse the name of the <xsl:decimal-formatting/> element
     */
    public void parseContents(Parser parser) {
        // Get the name of these decimal formatting symbols
        final String name = getAttribute("name");
        if (name.length() > 0) {
            if (!XML11Char.isXML11ValidQName(name)){
                ErrorMsg err = new ErrorMsg(ErrorMsg.INVALID_QNAME_ERR, name, this);
                parser.reportError(Constants.ERROR, err);
            }
        }
        _name = parser.getQNameIgnoreDefaultNs(name);
        if (_name == null) {
            _name = parser.getQNameIgnoreDefaultNs(EMPTYSTRING);
        }

        // Check if a set of symbols has already been registered under this name
        SymbolTable stable = parser.getSymbolTable();
        if (stable.getDecimalFormatting(_name) != null) {
            reportWarning(this, parser, ErrorMsg.SYMBOLS_REDEF_ERR,
                _name.toString());
        }
        else {
            stable.addDecimalFormatting(_name, this);
        }
    }

    /**
     * This method is called when the constructor is compiled in
     * Stylesheet.compileConstructor() and not as the syntax tree is traversed.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {

        ConstantPoolGen cpg = classGen.getConstantPool();
        InstructionList il = methodGen.getInstructionList();

        // DecimalFormatSymbols.<init>(Locale);
        // xsl:decimal-format - except for the NaN and infinity attributes.
        final int init = cpg.addMethodref(DFS_CLASS, "<init>",
                                          "("+LOCALE_SIG+")V");

        // Push the format name on the stack for call to addDecimalFormat()
        il.append(classGen.loadTranslet());
        il.append(new PUSH(cpg, _name.toString()));

        // Manufacture a DecimalFormatSymbols on the stack
        // for call to addDecimalFormat()
        // Use the US Locale as the default, as most of its settings
        // are equivalent to the default settings required of
        il.append(new NEW(cpg.addClass(DFS_CLASS)));
        il.append(DUP);
        il.append(new GETSTATIC(cpg.addFieldref(LOCALE_CLASS, "US",
                                                LOCALE_SIG)));
        il.append(new INVOKESPECIAL(init));

        String tmp = getAttribute("NaN");
        if ((tmp == null) || (tmp.equals(EMPTYSTRING))) {
            int nan = cpg.addMethodref(DFS_CLASS,
                                       "setNaN", "(Ljava/lang/String;)V");
            il.append(DUP);
            il.append(new PUSH(cpg, "NaN"));
            il.append(new INVOKEVIRTUAL(nan));
        }

        tmp = getAttribute("infinity");
        if ((tmp == null) || (tmp.equals(EMPTYSTRING))) {
            int inf = cpg.addMethodref(DFS_CLASS,
                                       "setInfinity",
                                       "(Ljava/lang/String;)V");
            il.append(DUP);
            il.append(new PUSH(cpg, "Infinity"));
            il.append(new INVOKEVIRTUAL(inf));
        }

        final int nAttributes = _attributes.getLength();
        for (int i = 0; i < nAttributes; i++) {
            final String name = _attributes.getQName(i);
            final String value = _attributes.getValue(i);

            boolean valid = true;
            int method = 0;

            if (name.equals("decimal-separator")) {
                // DecimalFormatSymbols.setDecimalSeparator();
                method = cpg.addMethodref(DFS_CLASS,
                                          "setDecimalSeparator", "(C)V");
            }
            else if (name.equals("grouping-separator")) {
                method =  cpg.addMethodref(DFS_CLASS,
                                           "setGroupingSeparator", "(C)V");
            }
            else if (name.equals("minus-sign")) {
                method = cpg.addMethodref(DFS_CLASS,
                                          "setMinusSign", "(C)V");
            }
            else if (name.equals("percent")) {
                method = cpg.addMethodref(DFS_CLASS,
                                          "setPercent", "(C)V");
            }
            else if (name.equals("per-mille")) {
                method = cpg.addMethodref(DFS_CLASS,
                                          "setPerMill", "(C)V");
            }
            else if (name.equals("zero-digit")) {
                method = cpg.addMethodref(DFS_CLASS,
                                          "setZeroDigit", "(C)V");
            }
            else if (name.equals("digit")) {
                method = cpg.addMethodref(DFS_CLASS,
                                          "setDigit", "(C)V");
            }
            else if (name.equals("pattern-separator")) {
                method = cpg.addMethodref(DFS_CLASS,
                                          "setPatternSeparator", "(C)V");
            }
            else if (name.equals("NaN")) {
                method = cpg.addMethodref(DFS_CLASS,
                                          "setNaN", "(Ljava/lang/String;)V");
                il.append(DUP);
                il.append(new PUSH(cpg, value));
                il.append(new INVOKEVIRTUAL(method));
                valid = false;
            }
            else if (name.equals("infinity")) {
                method = cpg.addMethodref(DFS_CLASS,
                                          "setInfinity",
                                          "(Ljava/lang/String;)V");
                il.append(DUP);
                il.append(new PUSH(cpg, value));
                il.append(new INVOKEVIRTUAL(method));
                valid = false;
            }
            else {
                valid = false;
            }

            if (valid) {
                il.append(DUP);
                il.append(new PUSH(cpg, value.charAt(0)));
                il.append(new INVOKEVIRTUAL(method));
            }

        }

        final int put = cpg.addMethodref(TRANSLET_CLASS,
                                         "addDecimalFormat",
                                         "("+STRING_SIG+DFS_SIG+")V");
        il.append(new INVOKEVIRTUAL(put));
    }

    /**
     * Creates the default, nameless, DecimalFormat object in
     * AbstractTranslet's format_symbols hashtable.
     * This should be called for every stylesheet, and the entry
     * may be overridden by later nameless xsl:decimal-format instructions.
     */
    public static void translateDefaultDFS(ClassGenerator classGen,
                                           MethodGenerator methodGen) {

        ConstantPoolGen cpg = classGen.getConstantPool();
        InstructionList il = methodGen.getInstructionList();
        final int init = cpg.addMethodref(DFS_CLASS, "<init>",
                                          "("+LOCALE_SIG+")V");

        // Push the format name, which is empty, on the stack
        // for call to addDecimalFormat()
        il.append(classGen.loadTranslet());
        il.append(new PUSH(cpg, EMPTYSTRING));

        // Manufacture a DecimalFormatSymbols on the stack for
        // call to addDecimalFormat().  Use the US Locale as the
        // default, as most of its settings are equivalent to
        // the default settings required of xsl:decimal-format -
        // except for the NaN and infinity attributes.
        il.append(new NEW(cpg.addClass(DFS_CLASS)));
        il.append(DUP);
        il.append(new GETSTATIC(cpg.addFieldref(LOCALE_CLASS, "US",
                                                LOCALE_SIG)));
        il.append(new INVOKESPECIAL(init));

        int nan = cpg.addMethodref(DFS_CLASS,
                                   "setNaN", "(Ljava/lang/String;)V");
        il.append(DUP);
        il.append(new PUSH(cpg, "NaN"));
        il.append(new INVOKEVIRTUAL(nan));

        int inf = cpg.addMethodref(DFS_CLASS,
                                   "setInfinity",
                                   "(Ljava/lang/String;)V");
        il.append(DUP);
        il.append(new PUSH(cpg, "Infinity"));
        il.append(new INVOKEVIRTUAL(inf));

        final int put = cpg.addMethodref(TRANSLET_CLASS,
                                         "addDecimalFormat",
                                         "("+STRING_SIG+DFS_SIG+")V");
        il.append(new INVOKEVIRTUAL(put));
    }
}
