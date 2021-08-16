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
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.bcel.internal.generic.IFNE;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
abstract class IdKeyPattern extends LocationPathPattern {

    protected RelativePathPattern _left = null;;
    private String _index = null;
    private String _value = null;;

    public IdKeyPattern(String index, String value) {
        _index = index;
        _value = value;
    }

    public String getIndexName() {
        return(_index);
    }

    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        return Type.NodeSet;
    }

    public boolean isWildcard() {
        return false;
    }

    public void setLeft(RelativePathPattern left) {
        _left = left;
    }

    public StepPattern getKernelPattern() {
        return(null);
    }

    public void reduceKernelPattern() { }

    public String toString() {
        return "id/keyPattern(" + _index + ", " + _value + ')';
    }

    /**
     * This method is called when the constructor is compiled in
     * Stylesheet.compileConstructor() and not as the syntax tree is traversed.
     */
    public void translate(ClassGenerator classGen,
                          MethodGenerator methodGen) {

        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        // Returns the KeyIndex object of a given name
        final int getKeyIndex = cpg.addMethodref(TRANSLET_CLASS,
                                                 "getKeyIndex",
                                                 "(Ljava/lang/String;)"+
                                                 KEY_INDEX_SIG);

        // Initialises a KeyIndex to return nodes with specific values
        final int lookupId = cpg.addMethodref(KEY_INDEX_CLASS,
                                              "containsID",
                                              "(ILjava/lang/Object;)I");
        final int lookupKey = cpg.addMethodref(KEY_INDEX_CLASS,
                                               "containsKey",
                                               "(ILjava/lang/Object;)I");
        final int getNodeIdent = cpg.addInterfaceMethodref(DOM_INTF,
                                                           "getNodeIdent",
                                                           "(I)"+NODE_SIG);

        // Call getKeyIndex in AbstractTranslet with the name of the key
        // to get the index for this key (which is also a node iterator).
        il.append(classGen.loadTranslet());
        il.append(new PUSH(cpg,_index));
        il.append(new INVOKEVIRTUAL(getKeyIndex));

        // Now use the value in the second argument to determine what nodes
        // the iterator should return.
        il.append(SWAP);
        il.append(new PUSH(cpg,_value));
        if (this instanceof IdPattern)
        {
            il.append(new INVOKEVIRTUAL(lookupId));
        }
        else
        {
            il.append(new INVOKEVIRTUAL(lookupKey));
        }

        _trueList.add(il.append(new IFNE(null)));
        _falseList.add(il.append(new GOTO(null)));
    }

}
