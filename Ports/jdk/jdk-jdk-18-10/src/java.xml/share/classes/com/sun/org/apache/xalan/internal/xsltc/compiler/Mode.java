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
 * $Id: Mode.java,v 1.2.4.1 2005/09/19 05:18:11 pvedula Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.DUP;
import com.sun.org.apache.bcel.internal.generic.GOTO_W;
import com.sun.org.apache.bcel.internal.generic.IFLT;
import com.sun.org.apache.bcel.internal.generic.ILOAD;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.ISTORE;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.LocalVariableGen;
import com.sun.org.apache.bcel.internal.generic.SWITCH;
import com.sun.org.apache.bcel.internal.generic.TargetLostException;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;
import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NamedMethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import com.sun.org.apache.xml.internal.dtm.Axis;
import com.sun.org.apache.xml.internal.dtm.DTM;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mode gathers all the templates belonging to a given mode;
 * it is responsible for generating an appropriate
 * applyTemplates + (mode name) method in the translet.
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @author Erwin Bolwidt <ejb@klomp.org>
 * @author G. Todd Miller
 * @LastModified: Nov 2017
 */
final class Mode implements Constants {

    /**
     * The name of this mode as defined in the stylesheet.
     */
    private final QName _name;

    /**
     * A reference to the stylesheet object that owns this mode.
     */
    private final Stylesheet _stylesheet;

    /**
     * The name of the method in which this mode is compiled.
     */
    private final String _methodName;

    /**
     * A vector of all the templates in this mode.
     */
    private List<Template> _templates;

    /**
     * Group for patterns with node()-type kernel and child axis.
     */
    private List<LocationPathPattern> _childNodeGroup = null;

    /**
     * Test sequence for patterns with node()-type kernel and child axis.
     */
    private TestSeq _childNodeTestSeq = null;

    /**
     * Group for patterns with node()-type kernel and attribute axis.
     */
    private List<LocationPathPattern> _attribNodeGroup = null;

    /**
     * Test sequence for patterns with node()-type kernel and attribute axis.
     */
    private TestSeq _attribNodeTestSeq = null;

    /**
     * Group for patterns with id() or key()-type kernel.
     */
    private List<LocationPathPattern> _idxGroup = null;

    /**
     * Test sequence for patterns with id() or key()-type kernel.
     */
    private TestSeq _idxTestSeq = null;

    /**
     * Group for patterns with any other kernel type.
     */
    private List<LocationPathPattern>[] _patternGroups;

    /**
     * Test sequence for patterns with any other kernel type.
     */
    private TestSeq[] _testSeq;


    /**
     * A mapping between templates and test sequences.
     */
    private Map<Template, Object> _neededTemplates = new HashMap<>();

    /**
     * A mapping between named templates and Mode objects.
     */
    private Map<Template, Mode> _namedTemplates = new HashMap<>();

    /**
     * A mapping between templates and instruction handles.
     */
    private Map<Template, InstructionHandle> _templateIHs = new HashMap<>();

    /**
     * A mapping between templates and instruction lists.
     */
    private Map<Template, InstructionList> _templateILs = new HashMap<>();

    /**
     * A reference to the pattern matching the root node.
     */
    private LocationPathPattern _rootPattern = null;

    /**
     * Stores ranges of template precendences for the compilation
     * of apply-imports.
     */
    private Map<Integer, Integer> _importLevels = null;

    /**
     * A mapping between key names and keys.
     */
    private Map<String, Key> _keys = null;

    /**
     * Variable index for the current node used in code generation.
     */
    private int _currentIndex;

    /**
     * Creates a new Mode.
     *
     * @param name A textual representation of the mode's QName
     * @param stylesheet The Stylesheet in which the mode occured
     * @param suffix A suffix to append to the method name for this mode
     *               (normally a sequence number - still in a String).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Mode(QName name, Stylesheet stylesheet, String suffix) {
        _name = name;
        _stylesheet = stylesheet;
        _methodName = APPLY_TEMPLATES + suffix;
        _templates = new ArrayList<>();
        _patternGroups = (List<LocationPathPattern>[])new ArrayList[32];
    }

    /**
     * Returns the name of the method (_not_ function) that will be
     * compiled for this mode. Normally takes the form 'applyTemplates()'
     * or * 'applyTemplates2()'.
     *
     * @return Method name for this mode
     */
    public String functionName() {
        return _methodName;
    }

    public String functionName(int min, int max) {
        if (_importLevels == null) {
            _importLevels = new HashMap<>();
        }
        _importLevels.put(max, min);
        return _methodName + '_' + max;
    }

    /**
     * Shortcut to get the class compiled for this mode (will be inlined).
     */
    private String getClassName() {
        return _stylesheet.getClassName();
    }

    public Stylesheet getStylesheet() {
        return _stylesheet;
    }

    public void addTemplate(Template template) {
        _templates.add(template);
    }

    private List<Template> quicksort(List<Template> templates, int p, int r) {
        if (p < r) {
            final int q = partition(templates, p, r);
            quicksort(templates, p, q);
            quicksort(templates, q + 1, r);
        }
        return templates;
    }

    private int partition(List<Template> templates, int p, int r) {
        final Template x = templates.get(p);
        int i = p - 1;
        int j = r + 1;
        while (true) {
            while (x.compareTo(templates.get(--j)) > 0);
            while (x.compareTo(templates.get(++i)) < 0);
            if (i < j) {
                templates.set(j, templates.set(i, templates.get(j)));
            }
            else {
                return j;
            }
        }
    }

    /**
     * Process all the test patterns in this mode
     */
    public void processPatterns(Map<String, Key> keys) {
        _keys = keys;
        _templates = quicksort(_templates, 0, _templates.size() - 1);

        // Traverse all templates
        for (Template template : _templates) {
            /*
             * Add this template to a table of named templates if it has a name.
             * If there are multiple templates with the same name, all but one
             * (the one with highest priority) will be disabled.
             */
            if (template.isNamed() && !template.disabled()) {
                _namedTemplates.put(template, this);
            }

            // Add this template to a test sequence if it has a pattern
            final Pattern pattern = template.getPattern();
            if (pattern != null) {
                flattenAlternative(pattern, template, keys);
            }
        }
        prepareTestSequences();
    }

    /**
     * This method will break up alternative patterns (ie. unions of patterns,
     * such as match="A/B | C/B") and add the basic patterns to their
     * respective pattern groups.
     */
    private void flattenAlternative(Pattern pattern,
                                    Template template,
                                    Map<String, Key> keys) {
        // Patterns on type id() and key() are special since they do not have
        // any kernel node type (it can be anything as long as the node is in
        // the id's or key's index).
        if (pattern instanceof IdKeyPattern) {
            final IdKeyPattern idkey = (IdKeyPattern)pattern;
            idkey.setTemplate(template);
            if (_idxGroup == null) _idxGroup = new ArrayList<>();
            _idxGroup.add((IdKeyPattern)pattern);
        }
        // Alternative patterns are broken up and re-processed recursively
        else if (pattern instanceof AlternativePattern) {
            final AlternativePattern alt = (AlternativePattern)pattern;
            flattenAlternative(alt.getLeft(), template, keys);
            flattenAlternative(alt.getRight(), template, keys);
        }
        // Finally we have a pattern that can be added to a test sequence!
        else if (pattern instanceof LocationPathPattern) {
            final LocationPathPattern lpp = (LocationPathPattern)pattern;
            lpp.setTemplate(template);
            addPatternToGroup(lpp);
        }
    }

    /**
     * Group patterns by NodeTests of their last Step
     * Keep them sorted by priority within group
     */
    private void addPatternToGroup(final LocationPathPattern lpp) {
        // id() and key()-type patterns do not have a kernel type
        if (lpp instanceof IdKeyPattern) {
            addPattern(-1, lpp);
        }
        // Otherwise get the kernel pattern from the LPP
        else {
            // kernel pattern is the last (maybe only) Step
            final StepPattern kernel = lpp.getKernelPattern();
            if (kernel != null) {
                addPattern(kernel.getNodeType(), lpp);
            }
            else if (_rootPattern == null ||
                     lpp.noSmallerThan(_rootPattern)) {
                _rootPattern = lpp;
            }
        }
    }

    /**
     * Adds a pattern to a pattern group
     */
    private void addPattern(int kernelType, LocationPathPattern pattern) {
        // Make sure the array of pattern groups is long enough
        final int oldLength = _patternGroups.length;
        if (kernelType >= oldLength) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            List<LocationPathPattern>[] newGroups =
                    (List<LocationPathPattern>[])new ArrayList[kernelType * 2];

            System.arraycopy(_patternGroups, 0, newGroups, 0, oldLength);
            _patternGroups = newGroups;
        }

        // Find the vector to put this pattern into
        List<LocationPathPattern> patterns;

        if (kernelType == DOM.NO_TYPE) {
            if (pattern.getAxis() == Axis.ATTRIBUTE) {
                patterns = (_attribNodeGroup == null) ?
                    (_attribNodeGroup = new ArrayList<>(2)) : _attribNodeGroup;
            }
            else {
                patterns = (_childNodeGroup == null) ?
                    (_childNodeGroup = new ArrayList<>(2)) : _childNodeGroup;
            }
        }
        else {
            patterns = (_patternGroups[kernelType] == null) ?
                (_patternGroups[kernelType] = new ArrayList<>(2)) :
                _patternGroups[kernelType];
        }

        if (patterns.size() == 0) {
            patterns.add(pattern);
        }
        else {
            boolean inserted = false;
            for (int i = 0; i < patterns.size(); i++) {
                final LocationPathPattern lppToCompare =
                    patterns.get(i);

                if (pattern.noSmallerThan(lppToCompare)) {
                    inserted = true;
                    patterns.add(i, pattern);
                    break;
                }
            }
            if (inserted == false) {
                patterns.add(pattern);
            }
        }
    }

    /**
     * Complete test sequences of a given type by adding all patterns
     * from a given group.
     */
    private void completeTestSequences(int nodeType, List<LocationPathPattern> patterns) {
        if (patterns != null) {
            if (_patternGroups[nodeType] == null) {
                _patternGroups[nodeType] = patterns;
            }
            else {
                final int m = patterns.size();
                for (int j = 0; j < m; j++) {
                    addPattern(nodeType, patterns.get(j));
                }
            }
        }
    }

    /**
     * Build test sequences. The first step is to complete the test sequences
     * by including patterns of "*" and "node()" kernel to all element test
     * sequences, and of "@*" to all attribute test sequences.
     */
    private void prepareTestSequences() {
        final List<LocationPathPattern> starGroup = _patternGroups[DTM.ELEMENT_NODE];
        final List<LocationPathPattern> atStarGroup = _patternGroups[DTM.ATTRIBUTE_NODE];

        // Complete test sequence for "text()" with "child::node()"
        completeTestSequences(DTM.TEXT_NODE, _childNodeGroup);

        // Complete test sequence for "*" with "child::node()"
        completeTestSequences(DTM.ELEMENT_NODE, _childNodeGroup);

        // Complete test sequence for "pi()" with "child::node()"
        completeTestSequences(DTM.PROCESSING_INSTRUCTION_NODE, _childNodeGroup);

        // Complete test sequence for "comment()" with "child::node()"
        completeTestSequences(DTM.COMMENT_NODE, _childNodeGroup);

        // Complete test sequence for "@*" with "attribute::node()"
        completeTestSequences(DTM.ATTRIBUTE_NODE, _attribNodeGroup);

        final List<String> names = _stylesheet.getXSLTC().getNamesIndex();
        if (starGroup != null || atStarGroup != null ||
            _childNodeGroup != null || _attribNodeGroup != null)
        {
            final int n = _patternGroups.length;

            // Complete test sequence for user-defined types
            for (int i = DTM.NTYPES; i < n; i++) {
                if (_patternGroups[i] == null) continue;

                final String name = names.get(i - DTM.NTYPES);

                if (isAttributeName(name)) {
                    // If an attribute then copy "@*" to its test sequence
                    completeTestSequences(i, atStarGroup);

                    // And also copy "attribute::node()" to its test sequence
                    completeTestSequences(i, _attribNodeGroup);
                }
                else {
                    // If an element then copy "*" to its test sequence
                    completeTestSequences(i, starGroup);

                    // And also copy "child::node()" to its test sequence
                    completeTestSequences(i, _childNodeGroup);
                }
            }
        }

        _testSeq = new TestSeq[DTM.NTYPES + names.size()];

        final int n = _patternGroups.length;
        for (int i = 0; i < n; i++) {
            final List<LocationPathPattern> patterns = _patternGroups[i];
            if (patterns != null) {
                final TestSeq testSeq = new TestSeq(patterns, i, this);
// System.out.println("testSeq[" + i + "] = " + testSeq);
                testSeq.reduce();
                _testSeq[i] = testSeq;
                testSeq.findTemplates(_neededTemplates);
            }
        }

        if (_childNodeGroup != null && _childNodeGroup.size() > 0) {
            _childNodeTestSeq = new TestSeq(_childNodeGroup, -1, this);
            _childNodeTestSeq.reduce();
            _childNodeTestSeq.findTemplates(_neededTemplates);
        }

/*
        if (_attribNodeGroup != null && _attribNodeGroup.size() > 0) {
            _attribNodeTestSeq = new TestSeq(_attribNodeGroup, -1, this);
            _attribNodeTestSeq.reduce();
            _attribNodeTestSeq.findTemplates(_neededTemplates);
        }
*/

        if (_idxGroup != null && _idxGroup.size() > 0) {
            _idxTestSeq = new TestSeq(_idxGroup, this);
            _idxTestSeq.reduce();
            _idxTestSeq.findTemplates(_neededTemplates);
        }

        if (_rootPattern != null) {
            // doesn't matter what is 'put', only key matters
            _neededTemplates.put(_rootPattern.getTemplate(), this);
        }
    }

    private void compileNamedTemplate(Template template,
                                      ClassGenerator classGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = new InstructionList();
        String methodName = Util.escape(template.getName().toString());

        int numParams = 0;
        if (template.isSimpleNamedTemplate()) {
            List<Param> parameters = template.getParameters();
            numParams = parameters.size();
        }

        // Initialize the types and names arrays for the NamedMethodGenerator.
        com.sun.org.apache.bcel.internal.generic.Type[] types =
            new com.sun.org.apache.bcel.internal.generic.Type[4 + numParams];
        String[] names = new String[4 + numParams];
        types[0] = Util.getJCRefType(DOM_INTF_SIG);
        types[1] = Util.getJCRefType(NODE_ITERATOR_SIG);
        types[2] = Util.getJCRefType(TRANSLET_OUTPUT_SIG);
        types[3] = com.sun.org.apache.bcel.internal.generic.Type.INT;
        names[0] = DOCUMENT_PNAME;
        names[1] = ITERATOR_PNAME;
        names[2] = TRANSLET_OUTPUT_PNAME;
        names[3] = NODE_PNAME;

        // For simple named templates, the signature of the generated method
        // is not fixed. It depends on the number of parameters declared in the
        // template.
        for (int i = 4; i < 4 + numParams; i++) {
            types[i] = Util.getJCRefType(OBJECT_SIG);
            names[i] = "param" + String.valueOf(i-4);
        }

        NamedMethodGenerator methodGen =
                new NamedMethodGenerator(ACC_PUBLIC,
                                     com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                     types, names, methodName,
                                     getClassName(), il, cpg);

        il.append(template.compile(classGen, methodGen));
        il.append(RETURN);

        classGen.addMethod(methodGen);
    }

    private void compileTemplates(ClassGenerator classGen,
                                  MethodGenerator methodGen,
                                  InstructionHandle next)
    {
        Set<Template> templates = _namedTemplates.keySet();
        for (Template template : templates) {
            compileNamedTemplate(template, classGen);
        }

        templates = _neededTemplates.keySet();
        for (Template template : templates) {
            if (template.hasContents()) {
                // !!! TODO templates both named and matched
                InstructionList til = template.compile(classGen, methodGen);
                til.append(new GOTO_W(next));
                _templateILs.put(template, til);
                _templateIHs.put(template, til.getStart());
            }
            else {
                // empty template
                _templateIHs.put(template, next);
            }
        }
    }

    private void appendTemplateCode(InstructionList body) {
        for (Template template : _neededTemplates.keySet()) {
            final InstructionList iList = _templateILs.get(template);
            if (iList != null) {
                body.append(iList);
            }

        }
    }

    private void appendTestSequences(InstructionList body) {
        final int n = _testSeq.length;
        for (int i = 0; i < n; i++) {
            final TestSeq testSeq = _testSeq[i];
            if (testSeq != null) {
                InstructionList il = testSeq.getInstructionList();
                if (il != null)
                    body.append(il);
                // else trivial TestSeq
            }
        }
    }

    public static void compileGetChildren(ClassGenerator classGen,
                                          MethodGenerator methodGen,
                                          int node) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        final int git = cpg.addInterfaceMethodref(DOM_INTF,
                                                  GET_CHILDREN,
                                                  GET_CHILDREN_SIG);
        il.append(methodGen.loadDOM());
        il.append(new ILOAD(node));
        il.append(new INVOKEINTERFACE(git, 2));
    }

    /**
     * Compiles the default handling for DOM elements: traverse all children
     */
    private InstructionList compileDefaultRecursion(ClassGenerator classGen,
                                                    MethodGenerator methodGen,
                                                    InstructionHandle next) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = new InstructionList();
        final String applyTemplatesSig = classGen.getApplyTemplatesSig();
        final int git = cpg.addInterfaceMethodref(DOM_INTF,
                                                  GET_CHILDREN,
                                                  GET_CHILDREN_SIG);
        final int applyTemplates = cpg.addMethodref(getClassName(),
                                                    functionName(),
                                                    applyTemplatesSig);
        il.append(classGen.loadTranslet());
        il.append(methodGen.loadDOM());

        il.append(methodGen.loadDOM());
        il.append(new ILOAD(_currentIndex));
        il.append(new INVOKEINTERFACE(git, 2));
        il.append(methodGen.loadHandler());
        il.append(new INVOKEVIRTUAL(applyTemplates));
        il.append(new GOTO_W(next));
        return il;
    }

    /**
     * Compiles the default action for DOM text nodes and attribute nodes:
     * output the node's text value
     */
    private InstructionList compileDefaultText(ClassGenerator classGen,
                                               MethodGenerator methodGen,
                                               InstructionHandle next) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = new InstructionList();

        final int chars = cpg.addInterfaceMethodref(DOM_INTF,
                                                    CHARACTERS,
                                                    CHARACTERS_SIG);
        il.append(methodGen.loadDOM());
        il.append(new ILOAD(_currentIndex));
        il.append(methodGen.loadHandler());
        il.append(new INVOKEINTERFACE(chars, 3));
        il.append(new GOTO_W(next));
        return il;
    }

    private InstructionList compileNamespaces(ClassGenerator classGen,
                                              MethodGenerator methodGen,
                                              boolean[] isNamespace,
                                              boolean[] isAttribute,
                                              boolean attrFlag,
                                              InstructionHandle defaultTarget) {
        final XSLTC xsltc = classGen.getParser().getXSLTC();
        final ConstantPoolGen cpg = classGen.getConstantPool();

        // Append switch() statement - namespace test dispatch loop
        final List<String> namespaces = xsltc.getNamespaceIndex();
        final List<String> names = xsltc.getNamesIndex();
        final int namespaceCount = namespaces.size() + 1;
        final int namesCount = names.size();

        final InstructionList il = new InstructionList();
        final int[] types = new int[namespaceCount];
        final InstructionHandle[] targets = new InstructionHandle[types.length];

        if (namespaceCount > 0) {
            boolean compiled = false;

            // Initialize targets for namespace() switch statement
            for (int i = 0; i < namespaceCount; i++) {
                targets[i] = defaultTarget;
                types[i] = i;
            }

            // Add test sequences for known namespace types
            for (int i = DTM.NTYPES; i < (DTM.NTYPES+namesCount); i++) {
                if ((isNamespace[i]) && (isAttribute[i] == attrFlag)) {
                    String name = names.get(i-DTM.NTYPES);
                    String namespace = name.substring(0,name.lastIndexOf(':'));
                    final int type = xsltc.registerNamespace(namespace);

                    if ((i < _testSeq.length) &&
                        (_testSeq[i] != null)) {
                        targets[type] =
                            (_testSeq[i]).compile(classGen,
                                                       methodGen,
                                                       defaultTarget);
                        compiled = true;
                    }
                }
            }

            // Return "null" if no test sequences were compiled
            if (!compiled) return(null);

            // Append first code in applyTemplates() - get type of current node
            final int getNS = cpg.addInterfaceMethodref(DOM_INTF,
                                                        "getNamespaceType",
                                                        "(I)I");
            il.append(methodGen.loadDOM());
            il.append(new ILOAD(_currentIndex));
            il.append(new INVOKEINTERFACE(getNS, 2));
            il.append(new SWITCH(types, targets, defaultTarget));
            return(il);
        }
        else {
            return(null);
        }
    }

   /**
     * Compiles the applyTemplates() method and adds it to the translet.
     * This is the main dispatch method.
     */
    public void compileApplyTemplates(ClassGenerator classGen) {
        final XSLTC xsltc = classGen.getParser().getXSLTC();
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final List<String> names = xsltc.getNamesIndex();

        // Create the applyTemplates() method
        final com.sun.org.apache.bcel.internal.generic.Type[] argTypes =
            new com.sun.org.apache.bcel.internal.generic.Type[3];
        argTypes[0] = Util.getJCRefType(DOM_INTF_SIG);
        argTypes[1] = Util.getJCRefType(NODE_ITERATOR_SIG);
        argTypes[2] = Util.getJCRefType(TRANSLET_OUTPUT_SIG);

        final String[] argNames = new String[3];
        argNames[0] = DOCUMENT_PNAME;
        argNames[1] = ITERATOR_PNAME;
        argNames[2] = TRANSLET_OUTPUT_PNAME;

        final InstructionList mainIL = new InstructionList();
        final MethodGenerator methodGen =
            new MethodGenerator(ACC_PUBLIC | ACC_FINAL,
                                com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                argTypes, argNames, functionName(),
                                getClassName(), mainIL,
                                classGen.getConstantPool());
        methodGen.addException("com.sun.org.apache.xalan.internal.xsltc.TransletException");
        // Insert an extra NOP just to keep "current" from appearing as if it
        // has a value before the start of the loop.
        mainIL.append(NOP);


        // Create a local variable to hold the current node
        final LocalVariableGen current;
        current = methodGen.addLocalVariable2("current",
                                              com.sun.org.apache.bcel.internal.generic.Type.INT,
                                              null);
        _currentIndex = current.getIndex();

        // Create the "body" instruction list that will eventually hold the
        // code for the entire method (other ILs will be appended).
        final InstructionList body = new InstructionList();
        body.append(NOP);

        // Create an instruction list that contains the default next-node
        // iteration
        final InstructionList ilLoop = new InstructionList();
        ilLoop.append(methodGen.loadIterator());
        ilLoop.append(methodGen.nextNode());
        ilLoop.append(DUP);
        ilLoop.append(new ISTORE(_currentIndex));

        // The body of this code can get very large - large than can be handled
        // by a single IFNE(body.getStart()) instruction - need workaround:
        final BranchHandle ifeq = ilLoop.append(new IFLT(null));
        final BranchHandle loop = ilLoop.append(new GOTO_W(null));
        ifeq.setTarget(ilLoop.append(RETURN));  // applyTemplates() ends here!
        final InstructionHandle ihLoop = ilLoop.getStart();

        current.setStart(mainIL.append(new GOTO_W(ihLoop)));

        // Live range of "current" ends at end of loop
        current.setEnd(loop);

        // Compile default handling of elements (traverse children)
        InstructionList ilRecurse =
            compileDefaultRecursion(classGen, methodGen, ihLoop);
        InstructionHandle ihRecurse = ilRecurse.getStart();

        // Compile default handling of text/attribute nodes (output text)
        InstructionList ilText =
            compileDefaultText(classGen, methodGen, ihLoop);
        InstructionHandle ihText = ilText.getStart();

        // Distinguish attribute/element/namespace tests for further processing
        final int[] types = new int[DTM.NTYPES + names.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = i;
        }

        // Initialize isAttribute[] and isNamespace[] arrays
        final boolean[] isAttribute = new boolean[types.length];
        final boolean[] isNamespace = new boolean[types.length];
        for (int i = 0; i < names.size(); i++) {
            final String name = names.get(i);
            isAttribute[i + DTM.NTYPES] = isAttributeName(name);
            isNamespace[i + DTM.NTYPES] = isNamespaceName(name);
        }

        // Compile all templates - regardless of pattern type
        compileTemplates(classGen, methodGen, ihLoop);

        // Handle template with explicit "*" pattern
        final TestSeq elemTest = _testSeq[DTM.ELEMENT_NODE];
        InstructionHandle ihElem = ihRecurse;
        if (elemTest != null)
            ihElem = elemTest.compile(classGen, methodGen, ihRecurse);

        // Handle template with explicit "@*" pattern
        final TestSeq attrTest = _testSeq[DTM.ATTRIBUTE_NODE];
        InstructionHandle ihAttr = ihText;
        if (attrTest != null)
            ihAttr = attrTest.compile(classGen, methodGen, ihAttr);

        // Do tests for id() and key() patterns first
        InstructionList ilKey = null;
        if (_idxTestSeq != null) {
            loop.setTarget(_idxTestSeq.compile(classGen, methodGen, body.getStart()));
            ilKey = _idxTestSeq.getInstructionList();
        }
        else {
            loop.setTarget(body.getStart());
        }

        // If there is a match on node() we need to replace ihElem
        // and ihText if the priority of node() is higher
        if (_childNodeTestSeq != null) {
            // Compare priorities of node() and "*"
            double nodePrio = _childNodeTestSeq.getPriority();
            int    nodePos  = _childNodeTestSeq.getPosition();
            double elemPrio = (0 - Double.MAX_VALUE);
            int    elemPos  = Integer.MIN_VALUE;

            if (elemTest != null) {
                elemPrio = elemTest.getPriority();
                elemPos  = elemTest.getPosition();
            }
            if (elemPrio == Double.NaN || elemPrio < nodePrio ||
                (elemPrio == nodePrio && elemPos < nodePos))
            {
                ihElem = _childNodeTestSeq.compile(classGen, methodGen, ihLoop);
            }

            // Compare priorities of node() and text()
            final TestSeq textTest = _testSeq[DTM.TEXT_NODE];
            double textPrio = (0 - Double.MAX_VALUE);
            int    textPos  = Integer.MIN_VALUE;

            if (textTest != null) {
                textPrio = textTest.getPriority();
                textPos  = textTest.getPosition();
            }
            if (textPrio == Double.NaN || textPrio < nodePrio ||
                (textPrio == nodePrio && textPos < nodePos))
            {
                ihText = _childNodeTestSeq.compile(classGen, methodGen, ihLoop);
                _testSeq[DTM.TEXT_NODE] = _childNodeTestSeq;
            }
        }

        // Handle templates with "ns:*" pattern
        InstructionHandle elemNamespaceHandle = ihElem;
        InstructionList nsElem = compileNamespaces(classGen, methodGen,
                                                   isNamespace, isAttribute,
                                                   false, ihElem);
        if (nsElem != null) elemNamespaceHandle = nsElem.getStart();

        // Handle templates with "ns:@*" pattern
        InstructionHandle attrNamespaceHandle = ihAttr;
        InstructionList nsAttr = compileNamespaces(classGen, methodGen,
                                                   isNamespace, isAttribute,
                                                   true, ihAttr);
        if (nsAttr != null) attrNamespaceHandle = nsAttr.getStart();

        // Handle templates with "ns:elem" or "ns:@attr" pattern
        final InstructionHandle[] targets = new InstructionHandle[types.length];
        for (int i = DTM.NTYPES; i < targets.length; i++) {
            final TestSeq testSeq = _testSeq[i];
            // Jump straight to namespace tests ?
            if (isNamespace[i]) {
                if (isAttribute[i])
                    targets[i] = attrNamespaceHandle;
                else
                    targets[i] = elemNamespaceHandle;
            }
            // Test first, then jump to namespace tests
            else if (testSeq != null) {
                if (isAttribute[i])
                    targets[i] = testSeq.compile(classGen, methodGen,
                                                 attrNamespaceHandle);
                else
                    targets[i] = testSeq.compile(classGen, methodGen,
                                                 elemNamespaceHandle);
            }
            else {
                targets[i] = ihLoop;
            }
        }


        // Handle pattern with match on root node - default: traverse children
        targets[DTM.ROOT_NODE] = _rootPattern != null
            ? getTemplateInstructionHandle(_rootPattern.getTemplate())
            : ihRecurse;

        // Handle pattern with match on root node - default: traverse children
        targets[DTM.DOCUMENT_NODE] = _rootPattern != null
            ? getTemplateInstructionHandle(_rootPattern.getTemplate())
            : ihRecurse;

        // Handle any pattern with match on text nodes - default: output text
        targets[DTM.TEXT_NODE] = _testSeq[DTM.TEXT_NODE] != null
            ? _testSeq[DTM.TEXT_NODE].compile(classGen, methodGen, ihText)
            : ihText;

        // This DOM-type is not in use - default: process next node
        targets[DTM.NAMESPACE_NODE] = ihLoop;

        // Match unknown element in DOM - default: check for namespace match
        targets[DTM.ELEMENT_NODE] = elemNamespaceHandle;

        // Match unknown attribute in DOM - default: check for namespace match
        targets[DTM.ATTRIBUTE_NODE] = attrNamespaceHandle;

        // Match on processing instruction - default: process next node
        InstructionHandle ihPI = ihLoop;
        if (_childNodeTestSeq != null) ihPI = ihElem;
        if (_testSeq[DTM.PROCESSING_INSTRUCTION_NODE] != null)
            targets[DTM.PROCESSING_INSTRUCTION_NODE] =
                _testSeq[DTM.PROCESSING_INSTRUCTION_NODE].
                compile(classGen, methodGen, ihPI);
        else
            targets[DTM.PROCESSING_INSTRUCTION_NODE] = ihPI;

        // Match on comments - default: process next node
        InstructionHandle ihComment = ihLoop;
        if (_childNodeTestSeq != null) ihComment = ihElem;
        targets[DTM.COMMENT_NODE] = _testSeq[DTM.COMMENT_NODE] != null
            ? _testSeq[DTM.COMMENT_NODE].compile(classGen, methodGen, ihComment)
            : ihComment;

            // This DOM-type is not in use - default: process next node
        targets[DTM.CDATA_SECTION_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.DOCUMENT_FRAGMENT_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.DOCUMENT_TYPE_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.ENTITY_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.ENTITY_REFERENCE_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.NOTATION_NODE] = ihLoop;


        // Now compile test sequences for various match patterns:
        for (int i = DTM.NTYPES; i < targets.length; i++) {
            final TestSeq testSeq = _testSeq[i];
            // Jump straight to namespace tests ?
            if ((testSeq == null) || (isNamespace[i])) {
                if (isAttribute[i])
                    targets[i] = attrNamespaceHandle;
                else
                    targets[i] = elemNamespaceHandle;
            }
            // Match on node type
            else {
                if (isAttribute[i])
                    targets[i] = testSeq.compile(classGen, methodGen,
                                                 attrNamespaceHandle);
                else
                    targets[i] = testSeq.compile(classGen, methodGen,
                                                 elemNamespaceHandle);
            }
        }

        if (ilKey != null) body.insert(ilKey);

        // Append first code in applyTemplates() - get type of current node
        final int getType = cpg.addInterfaceMethodref(DOM_INTF,
                                                      "getExpandedTypeID",
                                                      "(I)I");
        body.append(methodGen.loadDOM());
        body.append(new ILOAD(_currentIndex));
        body.append(new INVOKEINTERFACE(getType, 2));

        // Append switch() statement - main dispatch loop in applyTemplates()
        InstructionHandle disp = body.append(new SWITCH(types, targets, ihLoop));

        // Append all the "case:" statements
        appendTestSequences(body);
        // Append the actual template code
        appendTemplateCode(body);

        // Append NS:* node tests (if any)
        if (nsElem != null) body.append(nsElem);
        // Append NS:@* node tests (if any)
        if (nsAttr != null) body.append(nsAttr);

        // Append default action for element and root nodes
        body.append(ilRecurse);
        // Append default action for text and attribute nodes
        body.append(ilText);

        // putting together constituent instruction lists
        mainIL.append(body);
        // fall through to ilLoop
        mainIL.append(ilLoop);

        peepHoleOptimization(methodGen);
        classGen.addMethod(methodGen);

        // Compile method(s) for <xsl:apply-imports/> for this mode
        if (_importLevels != null) {
            for (Map.Entry<Integer, Integer> entry : _importLevels.entrySet()) {
                compileApplyImports(classGen, entry.getValue(), entry.getKey());
            }
        }
    }

    private void compileTemplateCalls(ClassGenerator classGen,
                                      MethodGenerator methodGen,
                                      InstructionHandle next, int min, int max){
        _neededTemplates.keySet().stream().forEach((template) -> {
            final int prec = template.getImportPrecedence();
            if ((prec >= min) && (prec < max)) {
                if (template.hasContents()) {
                    InstructionList til = template.compile(classGen, methodGen);
                    til.append(new GOTO_W(next));
                    _templateILs.put(template, til);
                    _templateIHs.put(template, til.getStart());
                }
                else {
                    // empty template
                    _templateIHs.put(template, next);
                }
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void compileApplyImports(ClassGenerator classGen, int min, int max) {
        final XSLTC xsltc = classGen.getParser().getXSLTC();
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final List<String> names = xsltc.getNamesIndex();

        // Clear some datastructures
        _namedTemplates = new HashMap<>();
        _neededTemplates = new HashMap<>();
        _templateIHs = new HashMap<>();
        _templateILs = new HashMap<>();
        _patternGroups = (List<LocationPathPattern>[])new ArrayList[32];
        _rootPattern = null;

        // IMPORTANT: Save orignal & complete set of templates!!!!
        List<Template> oldTemplates = _templates;

        // Gather templates that are within the scope of this import
        _templates = new ArrayList<>();
        for (Template template : oldTemplates) {
            final int prec = template.getImportPrecedence();
            if ((prec >= min) && (prec < max)) addTemplate(template);
        }

        // Process all patterns from those templates
        processPatterns(_keys);

        // Create the applyTemplates() method
        final com.sun.org.apache.bcel.internal.generic.Type[] argTypes =
            new com.sun.org.apache.bcel.internal.generic.Type[4];
        argTypes[0] = Util.getJCRefType(DOM_INTF_SIG);
        argTypes[1] = Util.getJCRefType(NODE_ITERATOR_SIG);
        argTypes[2] = Util.getJCRefType(TRANSLET_OUTPUT_SIG);
        argTypes[3] = com.sun.org.apache.bcel.internal.generic.Type.INT;

        final String[] argNames = new String[4];
        argNames[0] = DOCUMENT_PNAME;
        argNames[1] = ITERATOR_PNAME;
        argNames[2] = TRANSLET_OUTPUT_PNAME;
        argNames[3] = NODE_PNAME;

        final InstructionList mainIL = new InstructionList();
        final MethodGenerator methodGen =
            new MethodGenerator(ACC_PUBLIC | ACC_FINAL,
                                com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                argTypes, argNames, functionName()+'_'+max,
                                getClassName(), mainIL,
                                classGen.getConstantPool());
        methodGen.addException("com.sun.org.apache.xalan.internal.xsltc.TransletException");

        // Create the local variable to hold the current node
        final LocalVariableGen current;
        current = methodGen.addLocalVariable2("current",
                                              com.sun.org.apache.bcel.internal.generic.Type.INT,
                                              null);
        _currentIndex = current.getIndex();

        mainIL.append(new ILOAD(methodGen.getLocalIndex(NODE_PNAME)));
        current.setStart(mainIL.append(new ISTORE(_currentIndex)));

        // Create the "body" instruction list that will eventually hold the
        // code for the entire method (other ILs will be appended).
        final InstructionList body = new InstructionList();
        body.append(NOP);

        // Create an instruction list that contains the default next-node
        // iteration
        final InstructionList ilLoop = new InstructionList();
        ilLoop.append(RETURN);
        final InstructionHandle ihLoop = ilLoop.getStart();

        // Compile default handling of elements (traverse children)
        InstructionList ilRecurse =
            compileDefaultRecursion(classGen, methodGen, ihLoop);
        InstructionHandle ihRecurse = ilRecurse.getStart();

        // Compile default handling of text/attribute nodes (output text)
        InstructionList ilText =
            compileDefaultText(classGen, methodGen, ihLoop);
        InstructionHandle ihText = ilText.getStart();

        // Distinguish attribute/element/namespace tests for further processing
        final int[] types = new int[DTM.NTYPES + names.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = i;
        }

        final boolean[] isAttribute = new boolean[types.length];
        final boolean[] isNamespace = new boolean[types.length];
        for (int i = 0; i < names.size(); i++) {
            final String name = names.get(i);
            isAttribute[i+DTM.NTYPES] = isAttributeName(name);
            isNamespace[i+DTM.NTYPES] = isNamespaceName(name);
        }

        // Compile all templates - regardless of pattern type
        compileTemplateCalls(classGen, methodGen, ihLoop, min, max);

        // Handle template with explicit "*" pattern
        final TestSeq elemTest = _testSeq[DTM.ELEMENT_NODE];
        InstructionHandle ihElem = ihRecurse;
        if (elemTest != null) {
            ihElem = elemTest.compile(classGen, methodGen, ihLoop);
        }

        // Handle template with explicit "@*" pattern
        final TestSeq attrTest = _testSeq[DTM.ATTRIBUTE_NODE];
        InstructionHandle ihAttr = ihLoop;
        if (attrTest != null) {
            ihAttr = attrTest.compile(classGen, methodGen, ihAttr);
        }

        // Do tests for id() and key() patterns first
        InstructionList ilKey = null;
        if (_idxTestSeq != null) {
            ilKey = _idxTestSeq.getInstructionList();
        }

        // If there is a match on node() we need to replace ihElem
        // and ihText if the priority of node() is higher
        if (_childNodeTestSeq != null) {
            // Compare priorities of node() and "*"
            double nodePrio = _childNodeTestSeq.getPriority();
            int    nodePos  = _childNodeTestSeq.getPosition();
            double elemPrio = (0 - Double.MAX_VALUE);
            int    elemPos  = Integer.MIN_VALUE;

            if (elemTest != null) {
                elemPrio = elemTest.getPriority();
                elemPos  = elemTest.getPosition();
            }

            if (elemPrio == Double.NaN || elemPrio < nodePrio ||
                (elemPrio == nodePrio && elemPos < nodePos))
            {
                ihElem = _childNodeTestSeq.compile(classGen, methodGen, ihLoop);
            }

            // Compare priorities of node() and text()
            final TestSeq textTest = _testSeq[DTM.TEXT_NODE];
            double textPrio = (0 - Double.MAX_VALUE);
            int    textPos  = Integer.MIN_VALUE;

            if (textTest != null) {
                textPrio = textTest.getPriority();
                textPos  = textTest.getPosition();
            }

            if (textPrio == Double.NaN || textPrio < nodePrio ||
                (textPrio == nodePrio && textPos < nodePos))
            {
                ihText = _childNodeTestSeq.compile(classGen, methodGen, ihLoop);
                _testSeq[DTM.TEXT_NODE] = _childNodeTestSeq;
            }
        }

        // Handle templates with "ns:*" pattern
        InstructionHandle elemNamespaceHandle = ihElem;
        InstructionList nsElem = compileNamespaces(classGen, methodGen,
                                                   isNamespace, isAttribute,
                                                   false, ihElem);
        if (nsElem != null) elemNamespaceHandle = nsElem.getStart();

        // Handle templates with "ns:@*" pattern
        InstructionList nsAttr = compileNamespaces(classGen, methodGen,
                                                   isNamespace, isAttribute,
                                                   true, ihAttr);
        InstructionHandle attrNamespaceHandle = ihAttr;
        if (nsAttr != null) attrNamespaceHandle = nsAttr.getStart();

        // Handle templates with "ns:elem" or "ns:@attr" pattern
        final InstructionHandle[] targets = new InstructionHandle[types.length];
        for (int i = DTM.NTYPES; i < targets.length; i++) {
            final TestSeq testSeq = _testSeq[i];
            // Jump straight to namespace tests ?
            if (isNamespace[i]) {
                if (isAttribute[i])
                    targets[i] = attrNamespaceHandle;
                else
                    targets[i] = elemNamespaceHandle;
            }
            // Test first, then jump to namespace tests
            else if (testSeq != null) {
                if (isAttribute[i])
                    targets[i] = testSeq.compile(classGen, methodGen,
                                                 attrNamespaceHandle);
                else
                    targets[i] = testSeq.compile(classGen, methodGen,
                                                 elemNamespaceHandle);
            }
            else {
                targets[i] = ihLoop;
            }
        }

        // Handle pattern with match on root node - default: traverse children
        targets[DTM.ROOT_NODE] = _rootPattern != null
            ? getTemplateInstructionHandle(_rootPattern.getTemplate())
            : ihRecurse;
        // Handle pattern with match on root node - default: traverse children
        targets[DTM.DOCUMENT_NODE] = _rootPattern != null
            ? getTemplateInstructionHandle(_rootPattern.getTemplate())
            : ihRecurse;    // %HZ%:  Was ihLoop in XSLTC_DTM branch

        // Handle any pattern with match on text nodes - default: loop
        targets[DTM.TEXT_NODE] = _testSeq[DTM.TEXT_NODE] != null
            ? _testSeq[DTM.TEXT_NODE].compile(classGen, methodGen, ihText)
            : ihText;

        // This DOM-type is not in use - default: process next node
        targets[DTM.NAMESPACE_NODE] = ihLoop;

        // Match unknown element in DOM - default: check for namespace match
        targets[DTM.ELEMENT_NODE] = elemNamespaceHandle;

        // Match unknown attribute in DOM - default: check for namespace match
        targets[DTM.ATTRIBUTE_NODE] = attrNamespaceHandle;

        // Match on processing instruction - default: loop
        InstructionHandle ihPI = ihLoop;
        if (_childNodeTestSeq != null) ihPI = ihElem;
        if (_testSeq[DTM.PROCESSING_INSTRUCTION_NODE] != null) {
            targets[DTM.PROCESSING_INSTRUCTION_NODE] =
                _testSeq[DTM.PROCESSING_INSTRUCTION_NODE].
                compile(classGen, methodGen, ihPI);
        }
        else {
            targets[DTM.PROCESSING_INSTRUCTION_NODE] = ihPI;
        }

        // Match on comments - default: process next node
        InstructionHandle ihComment = ihLoop;
        if (_childNodeTestSeq != null) ihComment = ihElem;
        targets[DTM.COMMENT_NODE] = _testSeq[DTM.COMMENT_NODE] != null
            ? _testSeq[DTM.COMMENT_NODE].compile(classGen, methodGen, ihComment)
            : ihComment;

                // This DOM-type is not in use - default: process next node
        targets[DTM.CDATA_SECTION_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.DOCUMENT_FRAGMENT_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.DOCUMENT_TYPE_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.ENTITY_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.ENTITY_REFERENCE_NODE] = ihLoop;

        // This DOM-type is not in use - default: process next node
        targets[DTM.NOTATION_NODE] = ihLoop;



        // Now compile test sequences for various match patterns:
        for (int i = DTM.NTYPES; i < targets.length; i++) {
            final TestSeq testSeq = _testSeq[i];
            // Jump straight to namespace tests ?
            if ((testSeq == null) || (isNamespace[i])) {
                if (isAttribute[i])
                    targets[i] = attrNamespaceHandle;
                else
                    targets[i] = elemNamespaceHandle;
            }
            // Match on node type
            else {
                if (isAttribute[i])
                    targets[i] = testSeq.compile(classGen, methodGen,
                                                 attrNamespaceHandle);
                else
                    targets[i] = testSeq.compile(classGen, methodGen,
                                                 elemNamespaceHandle);
            }
        }

        if (ilKey != null) body.insert(ilKey);

        // Append first code in applyTemplates() - get type of current node
        final int getType = cpg.addInterfaceMethodref(DOM_INTF,
                                                      "getExpandedTypeID",
                                                      "(I)I");
        body.append(methodGen.loadDOM());
        body.append(new ILOAD(_currentIndex));
        body.append(new INVOKEINTERFACE(getType, 2));

        // Append switch() statement - main dispatch loop in applyTemplates()
        InstructionHandle disp = body.append(new SWITCH(types,targets,ihLoop));

        // Append all the "case:" statements
        appendTestSequences(body);
        // Append the actual template code
        appendTemplateCode(body);

        // Append NS:* node tests (if any)
        if (nsElem != null) body.append(nsElem);
        // Append NS:@* node tests (if any)
        if (nsAttr != null) body.append(nsAttr);

        // Append default action for element and root nodes
        body.append(ilRecurse);
        // Append default action for text and attribute nodes
        body.append(ilText);

        // putting together constituent instruction lists
        mainIL.append(body);

        // Mark the end of the live range for the "current" variable
        current.setEnd(body.getEnd());

        // fall through to ilLoop
        mainIL.append(ilLoop);

        peepHoleOptimization(methodGen);
        classGen.addMethod(methodGen);

        // Restore original (complete) set of templates for this transformation
        _templates = oldTemplates;
    }

    /**
      * Peephole optimization.
      */
    private void peepHoleOptimization(MethodGenerator methodGen) {
        InstructionList il = methodGen.getInstructionList();
        InstructionFinder find = new InstructionFinder(il);
        InstructionHandle ih;
        String pattern;

        // LoadInstruction, POP => (removed)
        // pattern = "LoadInstruction POP";
        // changed to lower case - changing to all lower case although only the instruction with capital I
        // is creating a problem in the Turkish locale
        pattern = "loadinstruction pop";

        for (Iterator<InstructionHandle[]> iter = find.search(pattern); iter.hasNext();) {
            InstructionHandle[] match = iter.next();
            try {
                if (!match[0].hasTargeters() && !match[1].hasTargeters()) {
                    il.delete(match[0], match[1]);
                }
            }
            catch (TargetLostException e) {
                // TODO: move target down into the list
            }
        }

        // ILOAD_N, ILOAD_N, SWAP, ISTORE_N => ILOAD_N
        // pattern = "ILOAD ILOAD SWAP ISTORE";
        // changed to lower case - changing to all lower case although only the instruction with capital I
        // is creating a problem in the Turkish locale
        pattern = "iload iload swap istore";
        for (Iterator<InstructionHandle[]> iter = find.search(pattern); iter.hasNext();) {
            InstructionHandle[] match = iter.next();
            try {
                com.sun.org.apache.bcel.internal.generic.ILOAD iload1 =
                    (com.sun.org.apache.bcel.internal.generic.ILOAD) match[0].getInstruction();
                com.sun.org.apache.bcel.internal.generic.ILOAD iload2 =
                    (com.sun.org.apache.bcel.internal.generic.ILOAD) match[1].getInstruction();
                com.sun.org.apache.bcel.internal.generic.ISTORE istore =
                    (com.sun.org.apache.bcel.internal.generic.ISTORE) match[3].getInstruction();

                if (!match[1].hasTargeters() &&
                    !match[2].hasTargeters() &&
                    !match[3].hasTargeters() &&
                    iload1.getIndex() == iload2.getIndex() &&
                    iload2.getIndex() == istore.getIndex())
                {
                    il.delete(match[1], match[3]);
                }
            }
            catch (TargetLostException e) {
                // TODO: move target down into the list
            }
        }

        // LoadInstruction_N, LoadInstruction_M, SWAP => LoadInstruction_M, LoadInstruction_N
        // pattern = "LoadInstruction LoadInstruction SWAP";
        // changed to lower case - changing to all lower case although only the instruction with capital I
        // is creating a problem in the Turkish locale
        pattern = "loadinstruction loadinstruction swap";
        for (Iterator<InstructionHandle[]> iter = find.search(pattern); iter.hasNext();) {
            InstructionHandle[] match = iter.next();
            try {
                if (!match[0].hasTargeters() &&
                    !match[1].hasTargeters() &&
                    !match[2].hasTargeters())
                {
                    Instruction load_m = match[1].getInstruction();
                    il.insert(match[0], load_m);
                    il.delete(match[1], match[2]);
                }
            }
            catch (TargetLostException e) {
                // TODO: move target down into the list
            }
        }

        // ALOAD_N ALOAD_N => ALOAD_N DUP
        // pattern = "ALOAD ALOAD";
        // changed to lower case - changing to all lower case although only the instruction with capital I
        // is creating a problem in the Turkish locale
        pattern = "aload aload";
        for (Iterator<InstructionHandle[]> iter = find.search(pattern); iter.hasNext();) {
            InstructionHandle[] match = iter.next();
            try {
                if (!match[1].hasTargeters()) {
                    com.sun.org.apache.bcel.internal.generic.ALOAD aload1 =
                        (com.sun.org.apache.bcel.internal.generic.ALOAD) match[0].getInstruction();
                    com.sun.org.apache.bcel.internal.generic.ALOAD aload2 =
                        (com.sun.org.apache.bcel.internal.generic.ALOAD) match[1].getInstruction();

                    if (aload1.getIndex() == aload2.getIndex()) {
                        il.insert(match[1], new DUP());
                        il.delete(match[1]);
                    }
                }
            }
            catch (TargetLostException e) {
                // TODO: move target down into the list
            }
        }
    }

    public InstructionHandle getTemplateInstructionHandle(Template template) {
        return _templateIHs.get(template);
    }

    /**
     * Auxiliary method to determine if a qname is an attribute.
     */
    private static boolean isAttributeName(String qname) {
        final int col = qname.lastIndexOf(':') + 1;
        return (qname.charAt(col) == '@');
    }

    /**
     * Auxiliary method to determine if a qname is a namespace
     * qualified "*".
     */
    private static boolean isNamespaceName(String qname) {
        final int col = qname.lastIndexOf(':');
        return (col > -1 && qname.charAt(qname.length()-1) == '*');
    }
}
