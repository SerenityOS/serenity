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

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public abstract class Pattern extends Expression {
    /**
     * Returns the type of a pattern, which is always a <code>NodeType</code>.
     * A <code>NodeType</code> has a number of subtypes defined by
     * <code>NodeType._type</code> corresponding to each type of node.
     */
    public abstract Type typeCheck(SymbolTable stable) throws TypeCheckError;

    /**
     * Translate this node into JVM bytecodes. Patterns are translated as
     * boolean expressions with true/false lists. Before calling
     * <code>translate</code> on a pattern, make sure that the node being
     * matched is on top of the stack. After calling <code>translate</code>,
     * make sure to backpatch both true and false lists. True lists are the
     * default, in the sense that they always <em>"fall through"</em>. If this
     * is not the intended semantics (e.g., see
     * {@link com.sun.org.apache.xalan.internal.xsltc.compiler.AlternativePattern#translate})
     * then a GOTO must be appended to the instruction list after calling
     * <code>translate</code>.
     */
    public abstract void translate(ClassGenerator classGen,
                                   MethodGenerator methodGen);

    /**
     * Returns the priority of this pattern (section 5.5 in the XSLT spec).
     */
    public abstract double getPriority();
}
