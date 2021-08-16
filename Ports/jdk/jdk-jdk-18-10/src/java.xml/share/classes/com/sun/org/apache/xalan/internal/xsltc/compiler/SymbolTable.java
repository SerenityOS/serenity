/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * $Id: SymbolTable.java,v 1.5 2005/09/28 13:48:16 pvedula Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @LastModified: June 2021
 */
final class SymbolTable {

    // These maps are used for all stylesheets
    private final Map<String, Stylesheet> _stylesheets = new HashMap<>();
    private final Map<String, List<MethodType>> _primops = new HashMap<>();

    // These maps are used for some stylesheets
    private Map<String, VariableBase> _variables = null;
    private Map<String, Template> _templates = null;
    private Map<String, AttributeSet> _attributeSets = null;
    private Map<String, String> _aliases = null;
    private Map<String, Integer> _excludedURI = null;
    private Stack<Map<String, Integer>>     _excludedURIStack = null;
    private Map<String, DecimalFormatting> _decimalFormats = null;
    private Map<String, Key> _keys = null;

    public DecimalFormatting getDecimalFormatting(QName name) {
        if (_decimalFormats == null) return null;
        return(_decimalFormats.get(name.getStringRep()));
    }

    public void addDecimalFormatting(QName name, DecimalFormatting symbols) {
        if (_decimalFormats == null) _decimalFormats = new HashMap<>();
        _decimalFormats.put(name.getStringRep(), symbols);
    }

    public Key getKey(QName name) {
        if (_keys == null) return null;
        return _keys.get(name.getStringRep());
    }

    public void addKey(QName name, Key key) {
        if (_keys == null) _keys = new HashMap<>();
        _keys.put(name.getStringRep(), key);
    }

    public Stylesheet addStylesheet(QName name, Stylesheet node) {
        return _stylesheets.put(name.getStringRep(), node);
    }

    public Stylesheet lookupStylesheet(QName name) {
        return _stylesheets.get(name.getStringRep());
    }

    public Template addTemplate(Template template) {
        final QName name = template.getName();
        if (_templates == null) _templates = new HashMap<>();
        return _templates.put(name.getStringRep(), template);
    }

    public Template lookupTemplate(QName name) {
        if (_templates == null) return null;
        return _templates.get(name.getStringRep());
    }

    public Variable addVariable(Variable variable) {
        if (_variables == null) _variables = new HashMap<>();
        VariableBase v = _variables.put(variable.getName().getStringRep(), variable);
        return v instanceof Variable ? (Variable)v : null;
    }

    public Param addParam(Param parameter) {
        if (_variables == null) _variables = new HashMap<>();
        VariableBase v = _variables.put(parameter.getName().getStringRep(), parameter);
        return v instanceof Param ? (Param)v : null;
    }

    public Variable lookupVariable(QName qname) {
        if (_variables == null) return null;
        final String name = qname.getStringRep();
        final VariableBase obj = _variables.get(name);
        return obj instanceof Variable ? (Variable)obj : null;
    }

    public Param lookupParam(QName qname) {
        if (_variables == null) return null;
        final String name = qname.getStringRep();
        final VariableBase obj = _variables.get(name);
        return obj instanceof Param ? (Param)obj : null;
    }

    public SyntaxTreeNode lookupName(QName qname) {
        if (_variables == null) return null;
        final String name = qname.getStringRep();
        return (SyntaxTreeNode)_variables.get(name);
    }

    public AttributeSet addAttributeSet(AttributeSet atts) {
        if (_attributeSets == null) _attributeSets = new HashMap<>();
        return _attributeSets.put(atts.getName().getStringRep(), atts);
    }

    public AttributeSet lookupAttributeSet(QName name) {
        if (_attributeSets == null) return null;
        return _attributeSets.get(name.getStringRep());
    }

    /**
     * Add a primitive operator or function to the symbol table. To avoid
     * name clashes with user-defined names, the prefix <tt>PrimopPrefix</tt>
     * is prepended.
     */
    public void addPrimop(String name, MethodType mtype) {
        List<MethodType> methods = _primops.get(name);
        if (methods == null) {
            _primops.put(name, methods = new ArrayList<>());
        }
        methods.add(mtype);
    }

    /**
     * Lookup a primitive operator or function in the symbol table by
     * prepending the prefix <tt>PrimopPrefix</tt>.
     */
    public List<MethodType> lookupPrimop(String name) {
        return _primops.get(name);
    }

    /**
     * This is used for xsl:attribute elements that have a "namespace"
     * attribute that is currently not defined using xmlns:
     */
    private int _nsCounter = 0;

    public String generateNamespacePrefix() {
        return("ns"+(_nsCounter++));
    }

    /**
     * Use a namespace prefix to lookup a namespace URI
     */
    private SyntaxTreeNode _current = null;

    public void setCurrentNode(SyntaxTreeNode node) {
        _current = node;
    }

    public String lookupNamespace(String prefix) {
        if (_current == null) return(Constants.EMPTYSTRING);
        return(_current.lookupNamespace(prefix));
    }

    /**
     * Adds an alias for a namespace prefix
     */
    public void addPrefixAlias(String prefix, String alias) {
        if (_aliases == null) _aliases = new HashMap<>();
        _aliases.put(prefix,alias);
    }

    /**
     * Retrieves any alias for a given namespace prefix
     */
    public String lookupPrefixAlias(String prefix) {
        if (_aliases == null) return null;
        return _aliases.get(prefix);
    }

    /**
     * Register a namespace URI so that it will not be declared in the output
     * unless it is actually referenced in the output.
     */
    public void excludeURI(String uri) {
        // The null-namespace cannot be excluded
        if (uri == null) return;

        // Create a new map of exlcuded URIs if none exists
        if (_excludedURI == null) _excludedURI = new HashMap<>();

        // Register the namespace URI
        Integer refcnt = _excludedURI.get(uri);
        if (refcnt == null)
            refcnt = 1;
        else
            refcnt = refcnt + 1;
        _excludedURI.put(uri,refcnt);
    }

    /**
     * Exclude a series of namespaces given by a list of whitespace
     * separated namespace prefixes.
     */
    public void excludeNamespaces(String prefixes) {
        if (prefixes != null) {
            StringTokenizer tokens = new StringTokenizer(prefixes);
            while (tokens.hasMoreTokens()) {
                final String prefix = tokens.nextToken();
                final String uri;
                if (prefix.equals("#default"))
                    uri = lookupNamespace(Constants.EMPTYSTRING);
                else
                    uri = lookupNamespace(prefix);
                if (uri != null) excludeURI(uri);
            }
        }
    }

    /**
     * Check if a namespace should not be declared in the output (unless used)
     */
    public boolean isExcludedNamespace(String uri) {
        if (uri != null && _excludedURI != null) {
            final Integer refcnt = _excludedURI.get(uri);
            return (refcnt != null && refcnt > 0);
        }
        return false;
    }

    /**
     * Turn of namespace declaration exclusion
     */
    public void unExcludeNamespaces(String prefixes) {
        if (_excludedURI == null) return;
        if (prefixes != null) {
            StringTokenizer tokens = new StringTokenizer(prefixes);
            while (tokens.hasMoreTokens()) {
                final String prefix = tokens.nextToken();
                final String uri;
                if (prefix.equals("#default"))
                    uri = lookupNamespace(Constants.EMPTYSTRING);
                else
                    uri = lookupNamespace(prefix);
                Integer refcnt = _excludedURI.get(uri);
                if (refcnt != null)
                    _excludedURI.put(uri, refcnt - 1);
            }
        }
    }
    /**
     * Exclusion of namespaces by a stylesheet does not extend to any stylesheet
     * imported or included by the stylesheet.  Upon entering the context of a
     * new stylesheet, a call to this method is needed to clear the current set
     * of excluded namespaces temporarily.  Every call to this method requires
     * a corresponding call to {@link #popExcludedNamespacesContext()}.
     */
    public void pushExcludedNamespacesContext() {
        if (_excludedURIStack == null) {
            _excludedURIStack = new Stack<>();
        }
        _excludedURIStack.push(_excludedURI);
        _excludedURI = null;
    }

    /**
     * Exclusion of namespaces by a stylesheet does not extend to any stylesheet
     * imported or included by the stylesheet.  Upon exiting the context of a
     * stylesheet, a call to this method is needed to restore the set of
     * excluded namespaces that was in effect prior to entering the context of
     * the current stylesheet.
     */
    public void popExcludedNamespacesContext() {
        _excludedURI = _excludedURIStack.pop();
        if (_excludedURIStack.isEmpty()) {
            _excludedURIStack = null;
        }
    }

}
