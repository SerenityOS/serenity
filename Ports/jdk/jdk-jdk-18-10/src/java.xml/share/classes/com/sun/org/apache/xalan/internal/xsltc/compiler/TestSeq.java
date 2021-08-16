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
 * $Id: TestSeq.java,v 1.2.4.1 2005/09/12 11:31:38 pvedula Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.GOTO_W;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A test sequence is a sequence of patterns that
 *
 *  (1) occured in templates in the same mode
 *  (2) share the same kernel node type (e.g. A/B and C/C/B)
 *  (3) may also contain patterns matching "*" and "node()"
 *      (element sequence only) or matching "@*" (attribute
 *      sequence only).
 *
 * A test sequence may have a default template, which will be
 * instantiated if none of the other patterns match.
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Erwin Bolwidt <ejb@klomp.org>
 * @author Morten Jorgensen
 * @LastModified: Nov 2017
 */
final class TestSeq {

    /**
     * Integer code for the kernel type of this test sequence
     */
    private int _kernelType;

    /**
     * ArrayList of all patterns in the test sequence. May include
     * patterns with "*", "@*" or "node()" kernel.
     */
    private List<LocationPathPattern> _patterns = null;

    /**
     * A reference to the Mode object.
     */
    private Mode _mode = null;

    /**
     * Default template for this test sequence
     */
    private Template _default = null;

    /**
     * Instruction list representing this test sequence.
     */
    private InstructionList _instructionList;

    /**
     * Cached handle to avoid compiling more than once.
     */
    private InstructionHandle _start = null;

    /**
     * Creates a new test sequence given a set of patterns and a mode.
     */
    public TestSeq(List<LocationPathPattern> patterns, Mode mode) {
        this(patterns, -2, mode);
    }

    public TestSeq(List<LocationPathPattern> patterns, int kernelType, Mode mode) {
        _patterns = patterns;
        _kernelType = kernelType;
        _mode = mode;
    }

    /**
     * Returns a string representation of this test sequence. Notice
     * that test sequences are mutable, so the value returned by this
     * method is different before and after calling reduce().
     */
    public String toString() {
        final int count = _patterns.size();
        final StringBuffer result = new StringBuffer();

        for (int i = 0; i < count; i++) {
            final LocationPathPattern pattern = _patterns.get(i);

            if (i == 0) {
                result.append("Testseq for kernel ").append(_kernelType)
                      .append('\n');
            }
            result.append("   pattern ").append(i).append(": ")
                  .append(pattern.toString())
                  .append('\n');
        }
        return result.toString();
    }

    /**
     * Returns the instruction list for this test sequence
     */
    public InstructionList getInstructionList() {
        return _instructionList;
    }

    /**
     * Return the highest priority for a pattern in this test
     * sequence. This is either the priority of the first or
     * of the default pattern.
     */
    public double getPriority() {
        final Template template = (_patterns.isEmpty()) ? _default
            : ((Pattern) _patterns.get(0)).getTemplate();
        return template.getPriority();
    }

    /**
     * Returns the position of the highest priority pattern in
     * this test sequence.
     */
    public int getPosition() {
        final Template template = (_patterns.isEmpty()) ? _default
            : ((Pattern) _patterns.get(0)).getTemplate();
        return template.getPosition();
    }

    /**
     * Reduce the patterns in this test sequence. Creates a new
     * vector of patterns and sets the default pattern if it
     * finds a patterns that is fully reduced.
     */
    public void reduce() {
        final List<LocationPathPattern> newPatterns = new ArrayList<>();

        for (LocationPathPattern pattern : _patterns) {
            // Reduce this pattern
            pattern.reduceKernelPattern();

            // Is this pattern fully reduced?
            if (pattern.isWildcard()) {
                _default = pattern.getTemplate();
                break;          // Ignore following patterns
            }
            else {
                newPatterns.add(pattern);
            }
        }
        _patterns = newPatterns;
    }

    /**
     * Returns, by reference, the templates that are included in
     * this test sequence. Note that a single template can occur
     * in several test sequences if its pattern is a union.
     */
    public void findTemplates(Map<Template, Object> templates) {
        if (_default != null) {
            templates.put(_default, this);
        }
        for (LocationPathPattern pattern : _patterns) {
            templates.put(pattern.getTemplate(), this);
        }
    }

    /**
     * Get the instruction handle to a template's code. This is
     * used when a single template occurs in several test
     * sequences; that is, if its pattern is a union of patterns
     * (e.g. match="A/B | A/C").
     */
    private InstructionHandle getTemplateHandle(Template template) {
        return _mode.getTemplateInstructionHandle(template);
    }

    /**
     * Returns pattern n in this test sequence
     */
    private LocationPathPattern getPattern(int n) {
        return _patterns.get(n);
    }

    /**
     * Compile the code for this test sequence. Compile patterns
     * from highest to lowest priority. Note that since patterns
     * can be share by multiple test sequences, instruction lists
     * must be copied before backpatching.
     */
    public InstructionHandle compile(ClassGenerator classGen,
                                     MethodGenerator methodGen,
                                     InstructionHandle continuation)
    {
        // Returned cached value if already compiled
        if (_start != null) {
            return _start;
        }

        // If not patterns, then return handle for default template
        final int count = _patterns.size();
        if (count == 0) {
            return (_start = getTemplateHandle(_default));
        }

        // Init handle to jump when all patterns failed
        InstructionHandle fail = (_default == null) ? continuation
            : getTemplateHandle(_default);

        // Compile all patterns in reverse order
        for (int n = count - 1; n >= 0; n--) {
            final LocationPathPattern pattern = getPattern(n);
            final Template template = pattern.getTemplate();
            final InstructionList il = new InstructionList();

            // Patterns expect current node on top of stack
            il.append(methodGen.loadCurrentNode());

            // Apply the test-code compiled for the pattern
            InstructionList ilist = methodGen.getInstructionList(pattern);
            if (ilist == null) {
                ilist = pattern.compile(classGen, methodGen);
                methodGen.addInstructionList(pattern, ilist);
            }

            // Make a copy of the instruction list for backpatching
            InstructionList copyOfilist = ilist.copy();

            FlowList trueList = pattern.getTrueList();
            if (trueList != null) {
                trueList = trueList.copyAndRedirect(ilist, copyOfilist);
            }
            FlowList falseList = pattern.getFalseList();
            if (falseList != null) {
                falseList = falseList.copyAndRedirect(ilist, copyOfilist);
            }

            il.append(copyOfilist);

            // On success branch to the template code
            final InstructionHandle gtmpl = getTemplateHandle(template);
            final InstructionHandle success = il.append(new GOTO_W(gtmpl));

            if (trueList != null) {
                trueList.backPatch(success);
            }
            if (falseList != null) {
                falseList.backPatch(fail);
            }

            // Next pattern's 'fail' target is this pattern's first instruction
            fail = il.getStart();

            // Append existing instruction list to the end of this one
            if (_instructionList != null) {
                il.append(_instructionList);
            }

            // Set current instruction list to be this one
            _instructionList = il;
        }
        return (_start = fail);
    }
}
