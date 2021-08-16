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

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import java.util.Objects;

/**
 * @author Morten Jorgensen
 * @author Santiago Pericas-Geertsen
 */
class VariableRefBase extends Expression {

    /**
     * A reference to the associated variable.
     */
    protected VariableBase _variable;

    /**
     * A reference to the enclosing expression/instruction for which a
     * closure is needed (Predicate, Number or Sort).
     */
    protected Closure _closure = null;

    public VariableRefBase(VariableBase variable) {
        _variable = variable;
        variable.addReference(this);
    }

    public VariableRefBase() {
        _variable = null;
    }

    /**
     * Returns a reference to the associated variable
     */
    public VariableBase getVariable() {
        return _variable;
    }

    /**
     * If this variable reference is in a top-level element like
     * another variable, param or key, add a dependency between
     * that top-level element and the referenced variable. For
     * example,
     *
     *   <xsl:variable name="x" .../>
     *   <xsl:variable name="y" select="$x + 1"/>
     *
     * and assuming this class represents "$x", add a reference
     * between variable y and variable x.
     */
    public void addParentDependency() {
        SyntaxTreeNode node = this;
        while (node != null && node instanceof TopLevelElement == false) {
            node = node.getParent();
        }

        TopLevelElement parent = (TopLevelElement) node;
        if (parent != null) {
            VariableBase var = _variable;
            if (_variable._ignore) {
                if (_variable instanceof Variable) {
                    var = parent.getSymbolTable()
                                .lookupVariable(_variable._name);
                } else if (_variable instanceof Param) {
                    var = parent.getSymbolTable().lookupParam(_variable._name);
                }
            }

            parent.addDependency(var);
        }
    }

    /**
     * Two variable references are deemed equal if they refer to the
     * same variable.
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof VariableRefBase)
            && (_variable == ((VariableRefBase) obj)._variable);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this._variable);
    }

    /**
     * Returns a string representation of this variable reference on the
     * format 'variable-ref(<var-name>)'.
     * @return Variable reference description
     */
    @Override
    public String toString() {
        return "variable-ref("+_variable.getName()+'/'+_variable.getType()+')';
    }

    @Override
    public Type typeCheck(SymbolTable stable)
        throws TypeCheckError
    {
        // Returned cached type if available
        if (_type != null) return _type;

        // Find nearest closure to add a variable reference
        if (_variable.isLocal()) {
            SyntaxTreeNode node = getParent();
            do {
                if (node instanceof Closure) {
                    _closure = (Closure) node;
                    break;
                }
                if (node instanceof TopLevelElement) {
                    break;      // way up in the tree
                }
                node = node.getParent();
            } while (node != null);

            if (_closure != null) {
                _closure.addVariable(this);
            }
        }

        // Attempt to get the cached variable type
        _type = _variable.getType();

        // If that does not work we must force a type-check (this is normally
        // only needed for globals in included/imported stylesheets
        if (_type == null) {
            _variable.typeCheck(stable);
            _type = _variable.getType();
        }

        // If in a top-level element, create dependency to the referenced var
        addParentDependency();

        // Return the type of the referenced variable
        return _type;
    }

}
