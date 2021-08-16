/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: MethodGenerator.java,v 1.2.4.1 2005/09/05 11:16:47 pvedula Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.ALOAD;
import com.sun.org.apache.bcel.internal.generic.ASTORE;
import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.BranchInstruction;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.DLOAD;
import com.sun.org.apache.bcel.internal.generic.DSTORE;
import com.sun.org.apache.bcel.internal.generic.FLOAD;
import com.sun.org.apache.bcel.internal.generic.FSTORE;
import com.sun.org.apache.bcel.internal.generic.GETFIELD;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.bcel.internal.generic.ICONST;
import com.sun.org.apache.bcel.internal.generic.ILOAD;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.ISTORE;
import com.sun.org.apache.bcel.internal.generic.IfInstruction;
import com.sun.org.apache.bcel.internal.generic.IndexedInstruction;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionConst;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.InstructionTargeter;
import com.sun.org.apache.bcel.internal.generic.LLOAD;
import com.sun.org.apache.bcel.internal.generic.LSTORE;
import com.sun.org.apache.bcel.internal.generic.LocalVariableGen;
import com.sun.org.apache.bcel.internal.generic.LocalVariableInstruction;
import com.sun.org.apache.bcel.internal.generic.MethodGen;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.bcel.internal.generic.PUTFIELD;
import com.sun.org.apache.bcel.internal.generic.RET;
import com.sun.org.apache.bcel.internal.generic.Select;
import com.sun.org.apache.bcel.internal.generic.TargetLostException;
import com.sun.org.apache.bcel.internal.generic.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Pattern;
import com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @LastModified: July 2019
 */
public class MethodGenerator extends MethodGen
    implements com.sun.org.apache.xalan.internal.xsltc.compiler.Constants {
    protected static final int INVALID_INDEX   = -1;

    private static final String START_ELEMENT_SIG
        = "(" + STRING_SIG + ")V";
    private static final String END_ELEMENT_SIG
        = START_ELEMENT_SIG;

    private static final int DOM_INDEX       = 1;
    private static final int ITERATOR_INDEX  = 2;
    private static final int HANDLER_INDEX   = 3;

    private static final int MAX_METHOD_SIZE = 65535;
    private static final int MAX_BRANCH_TARGET_OFFSET = 32767;
    private static final int MIN_BRANCH_TARGET_OFFSET = -32768;

    private static final int TARGET_METHOD_SIZE = 60000;
    private static final int MINIMUM_OUTLINEABLE_CHUNK_SIZE = 1000;

    private Instruction       _iloadCurrent;
    private Instruction       _istoreCurrent;
    private final Instruction _astoreHandler;
    private final Instruction _aloadHandler;
    private final Instruction _astoreIterator;
    private final Instruction _aloadIterator;
    private final Instruction _aloadDom;
    private final Instruction _astoreDom;

    private final Instruction _startElement;
    private final Instruction _endElement;
    private final Instruction _startDocument;
    private final Instruction _endDocument;
    private final Instruction _attribute;
    private final Instruction _uniqueAttribute;
    private final Instruction _namespace;

    private final Instruction _setStartNode;
    private final Instruction _reset;
    private final Instruction _nextNode;

    private SlotAllocator _slotAllocator;
    private boolean _allocatorInit = false;
    private LocalVariableRegistry _localVariableRegistry;
        /**
                 * A mapping between patterns and instruction lists used by
                 * test sequences to avoid compiling the same pattern multiple
                 * times. Note that patterns whose kernels are "*", "node()"
                 * and "@*" can between shared by test sequences.
                 */
        private Map<Pattern, InstructionList> _preCompiled = new HashMap<>();


    public MethodGenerator(int access_flags, Type return_type,
                           Type[] arg_types, String[] arg_names,
                           String method_name, String class_name,
                           InstructionList il, ConstantPoolGen cpg) {
        super(access_flags, return_type, arg_types, arg_names, method_name,
              class_name, il, cpg);

        _astoreHandler  = new ASTORE(HANDLER_INDEX);
        _aloadHandler   = new ALOAD(HANDLER_INDEX);
        _astoreIterator = new ASTORE(ITERATOR_INDEX);
        _aloadIterator  = new ALOAD(ITERATOR_INDEX);
        _aloadDom       = new ALOAD(DOM_INDEX);
        _astoreDom      = new ASTORE(DOM_INDEX);

        final int startElement =
            cpg.addInterfaceMethodref(TRANSLET_OUTPUT_INTERFACE,
                                      "startElement",
                                      START_ELEMENT_SIG);
        _startElement = new INVOKEINTERFACE(startElement, 2);

        final int endElement =
            cpg.addInterfaceMethodref(TRANSLET_OUTPUT_INTERFACE,
                                      "endElement",
                                      END_ELEMENT_SIG);
        _endElement = new INVOKEINTERFACE(endElement, 2);

        final int attribute =
            cpg.addInterfaceMethodref(TRANSLET_OUTPUT_INTERFACE,
                                      "addAttribute",
                                      "("
                                      + STRING_SIG
                                      + STRING_SIG
                                      + ")V");
        _attribute = new INVOKEINTERFACE(attribute, 3);

        final int uniqueAttribute =
            cpg.addInterfaceMethodref(TRANSLET_OUTPUT_INTERFACE,
                                      "addUniqueAttribute",
                                      "("
                                      + STRING_SIG
                                      + STRING_SIG
                                      + "I)V");
        _uniqueAttribute = new INVOKEINTERFACE(uniqueAttribute, 4);

        final int namespace =
            cpg.addInterfaceMethodref(TRANSLET_OUTPUT_INTERFACE,
                                      "namespaceAfterStartElement",
                                      "("
                                      + STRING_SIG
                                      + STRING_SIG
                                      + ")V");
        _namespace = new INVOKEINTERFACE(namespace, 3);

        int index = cpg.addInterfaceMethodref(TRANSLET_OUTPUT_INTERFACE,
                                              "startDocument",
                                              "()V");
        _startDocument = new INVOKEINTERFACE(index, 1);

        index = cpg.addInterfaceMethodref(TRANSLET_OUTPUT_INTERFACE,
                                          "endDocument",
                                          "()V");
        _endDocument = new INVOKEINTERFACE(index, 1);


        index = cpg.addInterfaceMethodref(NODE_ITERATOR,
                                          SET_START_NODE,
                                          SET_START_NODE_SIG);
        _setStartNode = new INVOKEINTERFACE(index, 2);

        index = cpg.addInterfaceMethodref(NODE_ITERATOR,
                                          "reset", "()"+NODE_ITERATOR_SIG);
        _reset = new INVOKEINTERFACE(index, 1);

        index = cpg.addInterfaceMethodref(NODE_ITERATOR, NEXT, NEXT_SIG);
        _nextNode = new INVOKEINTERFACE(index, 1);

        _slotAllocator = new SlotAllocator();
        _slotAllocator.initialize(getLocalVariableRegistry().getLocals());
        _allocatorInit = true;
    }

    /**
     * Allocates a local variable. If the slot allocator has already been
     * initialized, then call addLocalVariable2() so that the new variable
     * is known to the allocator. Failing to do this may cause the allocator
     * to return a slot that is already in use.
     */
    public LocalVariableGen addLocalVariable(String name, Type type,
                                             InstructionHandle start,
                                             InstructionHandle end)
    {
        LocalVariableGen lvg;

        if (_allocatorInit) {
            lvg = addLocalVariable2(name, type, start);
        } else {
            lvg = super.addLocalVariable(name, type, start, end);
            getLocalVariableRegistry().registerLocalVariable(lvg);
        }
        return lvg;
    }

    public LocalVariableGen addLocalVariable2(String name, Type type,
                                              InstructionHandle start)
    {
        LocalVariableGen lvg = super.addLocalVariable(name, type,
                                              _slotAllocator.allocateSlot(type),
                                              start, null);
        getLocalVariableRegistry().registerLocalVariable(lvg);
        return lvg;
    }
    private LocalVariableRegistry getLocalVariableRegistry() {
        if (_localVariableRegistry == null) {
            _localVariableRegistry = new LocalVariableRegistry();
        }

        return _localVariableRegistry;
    }

    /**
     * Keeps track of all local variables used in the method.
     * <p>The
     * {@link MethodGen#addLocalVariable(String,Type,InstructionHandle,InstructionHandle)}</code>
     * and
     * {@link MethodGen#addLocalVariable(String,Type,int,InstructionHandle,InstructionHandle)}</code>
     * methods of {@link MethodGen} will only keep track of
     * {@link LocalVariableGen} object until it'ss removed by a call to
     * {@link MethodGen#removeLocalVariable(LocalVariableGen)}.</p>
     * <p>In order to support efficient copying of local variables to outlined
     * methods by
     * {@link #outline(InstructionHandle,InstructionHandle,String,ClassGenerator)},
     * this class keeps track of all local variables defined by the method.</p>
     */
    protected class LocalVariableRegistry {
        /**
         * <p>A <code>java.lang.List</code> of all
         * {@link LocalVariableGen}s created for this method, indexed by the
         * slot number of the local variable.  The JVM stack frame of local
         * variables is divided into "slots".  A single slot can be used to
         * store more than one variable in a method, without regard to type, so
         * long as the byte code keeps the ranges of the two disjoint.</p>
         * <p>If only one registration of use of a particular slot occurs, the
         * corresponding entry of <code>_variables</code> contains the
         * <code>LocalVariableGen</code>; if more than one occurs, the
         * corresponding entry contains all such <code>LocalVariableGen</code>s
         * registered for the same slot; and if none occurs, the entry will be
         * <code>null</code>.
         */
        protected List<Object> _variables = new ArrayList<>();

        /**
         * Maps a name to a {@link LocalVariableGen}
         */
        protected Map<String, Object> _nameToLVGMap = new HashMap<>();

        /**
         * Registers a {@link org.apache.bcel.generic.LocalVariableGen}
         * for this method.
         * <p><b>Preconditions:</b>
         * <ul>
         * <li>The range of instructions for <code>lvg</code> does not
         * overlap with the range of instructions for any
         * <code>LocalVariableGen</code> with the same slot index previously
         * registered for this method.  <b><em>(Unchecked.)</em></b></li>
         * </ul></p>
         * @param lvg The variable to be registered
         */
        @SuppressWarnings("unchecked")
        protected void registerLocalVariable(LocalVariableGen lvg) {
            int slot = lvg.getIndex();

            int registrySize = _variables.size();

            // If the LocalVariableGen uses a slot index beyond any previously
            // encountered, expand the _variables, padding with intervening null
            // entries as required.
            if (slot >= registrySize) {
                for (int i = registrySize; i < slot; i++) {
                    _variables.add(null);
                }
                _variables.add(lvg);
            } else {
                // If the LocalVariableGen reuses a slot, make sure the entry
                // in _variables contains an ArrayList and add the newly
                // registered LocalVariableGen to the list.  If the entry in
                // _variables just contains null padding, store the
                // LocalVariableGen directly.
                Object localsInSlot = _variables.get(slot);
                if (localsInSlot != null) {
                    if (localsInSlot instanceof LocalVariableGen) {
                        List<LocalVariableGen> listOfLocalsInSlot = new ArrayList<>();
                        listOfLocalsInSlot.add((LocalVariableGen)localsInSlot);
                        listOfLocalsInSlot.add(lvg);
                        _variables.set(slot, listOfLocalsInSlot);
                    } else {
                        ((List<LocalVariableGen>) localsInSlot).add(lvg);
                    }
                } else {
                    _variables.set(slot, lvg);
                }
            }

            registerByName(lvg);
        }

        /**
         * <p>Find which {@link LocalVariableGen}, if any, is registered for a
         * particular JVM local stack frame slot at a particular position in the
         * byte code for the method.</p>
         * <p><b>Preconditions:</b>
         * <ul>
         * <li>The {@link InstructionList#setPositions()} has been called for
         * the {@link InstructionList} associated with this
         * {@link MethodGenerator}.</li>
         * </ul></p>
         * @param slot the JVM local stack frame slot number
         * @param offset the position in the byte code
         * @return the <code>LocalVariableGen</code> for the local variable
         * stored in the relevant slot at the relevant offset; <code>null</code>
         * if there is none.
         */
        protected LocalVariableGen lookupRegisteredLocalVariable(int slot,
                                                                 int offset) {
            Object localsInSlot = (_variables != null) ? _variables.get(slot)
                                                       : null;

            // If this slot index was never used, _variables.get will return
            // null; if it was used once, it will return the LocalVariableGen;
            // more than once it will return an ArrayList of all the
            // LocalVariableGens for variables stored in that slot.  For each
            // LocalVariableGen, check whether its range includes the
            // specified offset, and return the first such encountered.
            if (localsInSlot != null) {
                if (localsInSlot instanceof LocalVariableGen) {
                    LocalVariableGen lvg = (LocalVariableGen)localsInSlot;
                    if (offsetInLocalVariableGenRange(lvg, offset)) {
                        return lvg;
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    List<LocalVariableGen> listOfLocalsInSlot =
                            (List<LocalVariableGen>) localsInSlot;

                    for (LocalVariableGen lvg : listOfLocalsInSlot) {
                        if (offsetInLocalVariableGenRange(lvg, offset)) {
                            return lvg;
                        }
                    }
                }
            }

            // No local variable stored in the specified slot at the specified
            return null;
        }

        /**
         * <p>Set up a mapping of the name of the specified
         * {@link LocalVariableGen} object to the <code>LocalVariableGen</code>
         * itself.</p>
         * <p>This is a bit of a hack.  XSLTC is relying on the fact that the
         * name that is being looked up won't be duplicated, which isn't
         * guaranteed.  It replaces code which used to call
         * {@link MethodGen#getLocalVariables()} and looped through the
         * <code>LocalVariableGen</code> objects it contained to find the one
         * with the specified name.  However, <code>getLocalVariables()</code>
         * has the side effect of setting the start and end for any
         * <code>LocalVariableGen</code> which did not already have them
         * set, which causes problems for outlining..</p>
         * <p>See also {@link #lookUpByName(String)} and
         * {@link #removeByNameTracking(LocalVariableGen)}</P
         * @param lvg a <code>LocalVariableGen</code>
         */
        @SuppressWarnings("unchecked")
        protected void registerByName(LocalVariableGen lvg) {
            Object duplicateNameEntry = _nameToLVGMap.get(lvg.getName());

            if (duplicateNameEntry == null) {
                _nameToLVGMap.put(lvg.getName(), lvg);
            } else {
                List<LocalVariableGen> sameNameList;

                if (duplicateNameEntry instanceof ArrayList) {
                    sameNameList = (List<LocalVariableGen>)duplicateNameEntry;
                    sameNameList.add(lvg);
                } else {
                    sameNameList = new ArrayList<>();
                    sameNameList.add((LocalVariableGen)duplicateNameEntry);
                    sameNameList.add(lvg);
                }

                _nameToLVGMap.put(lvg.getName(), sameNameList);
            }
        }

        /**
         * Remove the mapping from the name of the specified
         * {@link LocalVariableGen} to itself.
         * See also {@link #registerByName(LocalVariableGen)} and
         * {@link #lookUpByName(String)}
         * @param lvg a <code>LocalVariableGen</code>
         */
        @SuppressWarnings("unchecked")
        protected void removeByNameTracking(LocalVariableGen lvg) {
            Object duplicateNameEntry = _nameToLVGMap.get(lvg.getName());

            if (duplicateNameEntry instanceof ArrayList) {
                List<LocalVariableGen> sameNameList =
                        (List<LocalVariableGen>)duplicateNameEntry;
                for (int i = 0; i < sameNameList.size(); i++) {
                    if (sameNameList.get(i) == lvg) {
                        sameNameList.remove(i);
                        break;
                    }
                }
            } else {
                _nameToLVGMap.remove(lvg.getName());
            }
        }

        /**
         * <p>Given the name of a variable, finds a {@link LocalVariableGen}
         * corresponding to it.</p>
         * <p>See also {@link #registerByName(LocalVariableGen)} and
         * {@link #removeByNameTracking(LocalVariableGen)}</p>
         * @param name
         * @return
         */
        @SuppressWarnings("unchecked")
        protected LocalVariableGen lookUpByName(String name) {
            LocalVariableGen lvg = null;
            Object duplicateNameEntry = _nameToLVGMap.get(name);

            if (duplicateNameEntry instanceof ArrayList) {
                List<LocalVariableGen> sameNameList =
                        (List<LocalVariableGen>)duplicateNameEntry;

                for (int i = 0; i < sameNameList.size(); i++) {
                    lvg = sameNameList.get(i);
                    if (lvg.getName() == null ? name == null : lvg.getName().equals(name)) {
                        break;
                    }
                }
            } else {
                lvg = (LocalVariableGen) duplicateNameEntry;
            }

            return lvg;
        }

        /**
         * Gets all {@link LocalVariableGen} objects.
         * This method replaces {@link MethodGen#getLocalVariables()} which has
         * a side-effect of setting the start and end range for any
         * {@code LocalVariableGen} if either was {@code null}.  That
         * side-effect causes problems for outlining of code in XSLTC.
         *
         * @return an array of {@code LocalVariableGen} containing all the
         * local variables
         */
        @SuppressWarnings("unchecked")
        private LocalVariableGen[] getLocals() {
            LocalVariableGen[] locals = null;
            List<LocalVariableGen> allVarsEverDeclared = new ArrayList<>();

            for (Map.Entry<String, Object> nameVarsPair : _nameToLVGMap.entrySet()) {
                Object vars = nameVarsPair.getValue();
                if (vars != null) {
                    if (vars instanceof ArrayList) {
                        List<LocalVariableGen> varsList =
                                (List<LocalVariableGen>) vars;
                        for (int i = 0; i < varsList.size(); i++) {
                            allVarsEverDeclared.add(varsList.get(i));
                        }
                    } else {
                        allVarsEverDeclared.add((LocalVariableGen)vars);
                    }
                }
            }

            locals = new LocalVariableGen[allVarsEverDeclared.size()];
            allVarsEverDeclared.toArray(locals);

            return locals;
        }
    }

    /**
     * Determines whether a particular variable is in use at a particular offset
     * in the byte code for this method.
     * <p><b>Preconditions:</b>
     * <ul>
     * <li>The {@link InstructionList#setPositions()} has been called for the
     * {@link InstructionList} associated with this {@link MethodGenerator}.
     * </li></ul></p>
     * @param lvg the {@link LocalVariableGen} for the variable
     * @param offset the position in the byte code
     * @return <code>true</code> if and only if the specified variable is in
     * use at the particular byte code offset.
     */
    boolean offsetInLocalVariableGenRange(LocalVariableGen lvg, int offset) {
        InstructionHandle lvgStart = lvg.getStart();
        InstructionHandle lvgEnd = lvg.getEnd();

        // If no start handle is recorded for the LocalVariableGen, it is
        // assumed to be in use from the beginning of the method.
        if (lvgStart == null) {
            lvgStart = getInstructionList().getStart();
        }

        // If no end handle is recorded for the LocalVariableGen, it is assumed
        // to be in use to the end of the method.
        if (lvgEnd == null) {
            lvgEnd = getInstructionList().getEnd();
        }

        // Does the range of the instruction include the specified offset?
        // Note that the InstructionHandle.getPosition method returns the
        // offset of the beginning of an instruction.  A LocalVariableGen's
        // range includes the end instruction itself, so that instruction's
        // length must be taken into consideration in computing whether the
        // varible is in range at a particular offset.
        return ((lvgStart.getPosition() <= offset)
                    && (lvgEnd.getPosition()
                            + lvgEnd.getInstruction().getLength() >= offset));
    }

    public void removeLocalVariable(LocalVariableGen lvg) {
        _slotAllocator.releaseSlot(lvg);
        getLocalVariableRegistry().removeByNameTracking(lvg);
        super.removeLocalVariable(lvg);
    }

    public Instruction loadDOM() {
        return _aloadDom;
    }

    public Instruction storeDOM() {
        return _astoreDom;
    }

    public Instruction storeHandler() {
        return _astoreHandler;
    }

    public Instruction loadHandler() {
        return _aloadHandler;
    }

    public Instruction storeIterator() {
        return _astoreIterator;
    }

    public Instruction loadIterator() {
        return _aloadIterator;
    }

    public final Instruction setStartNode() {
        return _setStartNode;
    }

    public final Instruction reset() {
        return _reset;
    }

    public final Instruction nextNode() {
        return _nextNode;
    }

    public final Instruction startElement() {
        return _startElement;
    }

    public final Instruction endElement() {
        return _endElement;
    }

    public final Instruction startDocument() {
        return _startDocument;
    }

    public final Instruction endDocument() {
        return _endDocument;
    }

    public final Instruction attribute() {
        return _attribute;
    }

    public final Instruction uniqueAttribute() {
        return _uniqueAttribute;
    }

    public final Instruction namespace() {
        return _namespace;
    }

    public Instruction loadCurrentNode() {
        if (_iloadCurrent == null) {
            int idx = getLocalIndex("current");
            if (idx > 0)
                _iloadCurrent = new ILOAD(idx);
            else
                _iloadCurrent = new ICONST(0);
        }
        return _iloadCurrent;
    }

    public Instruction storeCurrentNode() {
        return _istoreCurrent != null
            ? _istoreCurrent
            : (_istoreCurrent = new ISTORE(getLocalIndex("current")));
    }

    /** by default context node is the same as current node. MK437 */
    public Instruction loadContextNode() {
        return loadCurrentNode();
    }

    public Instruction storeContextNode() {
        return storeCurrentNode();
    }

    public int getLocalIndex(String name) {
        return getLocalVariable(name).getIndex();
    }

    public LocalVariableGen getLocalVariable(String name) {
        return getLocalVariableRegistry().lookUpByName(name);
    }

    public void setMaxLocals() {

        // Get the current number of local variable slots
        int maxLocals = super.getMaxLocals();
        int prevLocals = maxLocals;

        // Get numer of actual variables
        final LocalVariableGen[] localVars = super.getLocalVariables();
        if (localVars != null) {
            if (localVars.length > maxLocals)
                maxLocals = localVars.length;
        }

        // We want at least 5 local variable slots (for parameters)
        if (maxLocals < 5) maxLocals = 5;

        super.setMaxLocals(maxLocals);
    }

    /**
     * Add a pre-compiled pattern to this mode.
     */
    public void addInstructionList(Pattern pattern, InstructionList ilist) {
        _preCompiled.put(pattern, ilist);
    }

    /**
     * Get the instruction list for a pre-compiled pattern. Used by
     * test sequences to avoid compiling patterns more than once.
     */
    public InstructionList getInstructionList(Pattern pattern) {
        return _preCompiled.get(pattern);
    }

    /**
     * Used to keep track of an outlineable chunk of instructions in the
     * current method.  See {@link OutlineableChunkStart} and
     * {@link OutlineableChunkEnd} for more information.
     */
    private class Chunk implements Comparable<Object> {
        /**
         * {@link InstructionHandle} of the first instruction in the outlineable
         * chunk.
         */
        private InstructionHandle m_start;

        /**
         * {@link org.apache.bcel.generic.InstructionHandle} of the first
         * instruction in the outlineable chunk.
         */
        private InstructionHandle m_end;

        /**
         * Number of bytes in the instructions contained in this outlineable
         * chunk.
         */
        private int m_size;

        /**
         * <p>Constructor for an outlineable {@link MethodGenerator.Chunk}.</p>
         * <p><b>Preconditions:</b>
         * <ul>
         * <li>The {@link InstructionList#setPositions()} has been called for
         * the {@link InstructionList} associated with this
         * {@link MethodGenerator}.</li>
         * </ul></p>
         * @param start The {@link InstructionHandle} of the first
         *              instruction in the outlineable chunk.
         * @param end The {@link InstructionHandle} of the last
         *            instruction in the outlineable chunk.
         */
        Chunk(InstructionHandle start, InstructionHandle end) {
            m_start = start;
            m_end = end;
            m_size = end.getPosition() - start.getPosition();
        }

        /**
         * Determines whether this outlineable {@link MethodGenerator.Chunk} is
         * followed immediately by the argument
         * <code>MethodGenerator.Chunk</code>, with no other intervening
         * instructions, including {@link OutlineableChunkStart} or
         * {@link OutlineableChunkEnd} instructions.
         * @param neighbour an outlineable {@link MethodGenerator.Chunk}
         * @return <code>true</code> if and only if the argument chunk
         * immediately follows <code>this</code> chunk
         */
        boolean isAdjacentTo(Chunk neighbour) {
            return getChunkEnd().getNext() == neighbour.getChunkStart();
        }

        /**
         * Getter method for the start of this {@linke MethodGenerator.Chunk}
         * @return the {@link org.apache.bcel.generic.InstructionHandle} of the
         * start of this chunk
         */
        InstructionHandle getChunkStart() {
            return m_start;
        }

        /**
         * Getter method for the end of this {@link MethodGenerator.Chunk}
         * @return the {@link InstructionHandle} of the start of this chunk
         */
        InstructionHandle getChunkEnd() {
            return m_end;
        }

        /**
         * The size of this {@link MethodGenerator.Chunk}
         * @return the number of bytes in the byte code represented by this
         *         chunk.
         */
        int getChunkSize() {
            return m_size;
        }

        /**
         * Implements the <code>java.util.Comparable.compareTo(Object)</code>
         * method.
         * @return
         * <ul>
         * <li>A positive <code>int</code> if the length of <code>this</code>
         * chunk in bytes is greater than that of <code>comparand</code></li>
         * <li>A negative <code>int</code> if the length of <code>this</code>
         * chunk in bytes is less than that of <code>comparand</code></li>
         * <li>Zero, otherwise.</li>
         * </ul>
         */
        public int compareTo(Object comparand) {
            return getChunkSize() - ((Chunk)comparand).getChunkSize();
        }
    }

    /**
     * Find the outlineable chunks in this method that would be the best choices
     * to outline, based on size and position in the method.
     * @param classGen The {@link ClassGen} with which the generated methods
     *                 will be associated
     * @param totalMethodSize the size of the bytecode in the original method
     * @return a <code>java.util.List</code> containing the
     *  {@link MethodGenerator.Chunk}s that may be outlined from this method
     */
    private List<Chunk> getCandidateChunks(ClassGenerator classGen,
                                         int totalMethodSize) {
        Iterator<InstructionHandle> instructions = getInstructionList().iterator();
        List<Chunk> candidateChunks = new ArrayList<>();
        List<InstructionHandle> currLevelChunks = new ArrayList<>();
        Stack<List<InstructionHandle>> subChunkStack = new Stack<>();
        boolean openChunkAtCurrLevel = false;
        boolean firstInstruction = true;

        InstructionHandle currentHandle;

        if (m_openChunks != 0) {
            String msg =
                (new ErrorMsg(ErrorMsg.OUTLINE_ERR_UNBALANCED_MARKERS))
                    .toString();
            throw new InternalError(msg);
        }

        // Scan instructions in the method, keeping track of the nesting level
        // of outlineable chunks.
        //
        // currLevelChunks
        //     keeps track of the child chunks of a chunk.  For each chunk,
        //     there will be a pair of entries:  the InstructionHandles for the
        //     start and for the end of the chunk
        // subChunkStack
        //     a stack containing the partially accumulated currLevelChunks for
        //     each chunk that's still open at the current position in the
        //     InstructionList.
        // candidateChunks
        //     the list of chunks which have been accepted as candidates chunks
        //     for outlining
        do {
            // Get the next instruction.  The loop will perform one extra
            // iteration after it reaches the end of the InstructionList, with
            // currentHandle set to null.
            currentHandle = instructions.hasNext()
                                    ? instructions.next()
                                    : null;
            Instruction inst =
                    (currentHandle != null) ? currentHandle.getInstruction()
                                            : null;

            // At the first iteration, create a chunk representing all the
            // code in the method.  This is done just to simplify the logic -
            // this chunk can never be outlined because it will be too big.
            if (firstInstruction) {
                openChunkAtCurrLevel = true;
                currLevelChunks.add(currentHandle);
                firstInstruction = false;
            }

            // Found a new chunk
            if (inst instanceof OutlineableChunkStart) {
                // If last MarkerInstruction encountered was an
                // OutlineableChunkStart, this represents the first chunk
                // nested within that previous chunk - push the list of chunks
                // from the outer level onto the stack
                if (openChunkAtCurrLevel) {
                    subChunkStack.push(currLevelChunks);
                    currLevelChunks = new ArrayList<>();
                }

                openChunkAtCurrLevel = true;
                currLevelChunks.add(currentHandle);
            // Close off an open chunk
            } else if (currentHandle == null
                           || inst instanceof OutlineableChunkEnd) {
                List<InstructionHandle> nestedSubChunks = null;

                // If the last MarkerInstruction encountered was an
                // OutlineableChunkEnd, it means that the current instruction
                // marks the end of a chunk that contained child chunks.
                // Those children might need to be examined below in case they
                // are better candidates for outlining than the current chunk.
                if (!openChunkAtCurrLevel) {
                    nestedSubChunks = currLevelChunks;
                    currLevelChunks = subChunkStack.pop();
                }

                // Get the handle for the start of this chunk (the last entry
                // in currLevelChunks)
                InstructionHandle chunkStart =
                        currLevelChunks.get(currLevelChunks.size()-1);

                int chunkEndPosition =
                        (currentHandle != null) ? currentHandle.getPosition()
                                                : totalMethodSize;
                int chunkSize = chunkEndPosition - chunkStart.getPosition();

                // Two ranges of chunk size to consider:
                //
                // 1. [0,TARGET_METHOD_SIZE]
                //      Keep this chunk in consideration as a candidate,
                //      and ignore its subchunks, if any - there's nothing to be
                //      gained by outlining both the current chunk and its
                //      children!
                //
                // 2. (TARGET_METHOD_SIZE,+infinity)
                //      Ignore this chunk - it's too big.  Add its subchunks
                //      as candidates, after merging adjacent chunks to produce
                //      chunks that are as large as possible
                if (chunkSize <= TARGET_METHOD_SIZE) {
                    currLevelChunks.add(currentHandle);
                } else {
                    if (!openChunkAtCurrLevel) {
                        int childChunkCount = nestedSubChunks.size() / 2;
                        if (childChunkCount > 0) {
                            Chunk[] childChunks = new Chunk[childChunkCount];

                            // Gather all the child chunks of the current chunk
                            for (int i = 0; i < childChunkCount; i++) {
                                InstructionHandle start = nestedSubChunks.get(i*2);
                                InstructionHandle end = nestedSubChunks.get(i*2+1);

                                childChunks[i] = new Chunk(start, end);
                            }

                            // Merge adjacent siblings
                            List<Chunk> mergedChildChunks =
                                        mergeAdjacentChunks(childChunks);

                            // Add chunks that mean minimum size requirements
                            // to the list of candidate chunks for outlining
                            for (Chunk mergedChunk : mergedChildChunks) {
                                int mergedSize = mergedChunk.getChunkSize();

                                if (mergedSize >= MINIMUM_OUTLINEABLE_CHUNK_SIZE
                                        && mergedSize <= TARGET_METHOD_SIZE) {
                                    candidateChunks.add(mergedChunk);
                                }
                            }
                        }
                    }

                    // Drop the chunk which was too big
                    currLevelChunks.remove(currLevelChunks.size() - 1);
                }

                // currLevelChunks contains pairs of InstructionHandles.  If
                // its size is an odd number, the loop has encountered the
                // start of a chunk at this level, but not its end.
                openChunkAtCurrLevel = ((currLevelChunks.size() & 0x1) == 1);
            }

        } while (currentHandle != null);

        return candidateChunks;
    }

    /**
     * Merge adjacent sibling chunks to produce larger candidate chunks for
     * outlining
     * @param chunks array of sibling {@link MethodGenerator.Chunk}s that are
     *               under consideration for outlining.  Chunks must be in
     *               the order encountered in the {@link InstructionList}
     * @return a <code>java.util.List</code> of
     *         <code>MethodGenerator.Chunk</code>s maximally merged
     */
    private List<Chunk> mergeAdjacentChunks(Chunk[] chunks) {
        int[] adjacencyRunStart = new int[chunks.length];
        int[] adjacencyRunLength = new int[chunks.length];
        boolean[] chunkWasMerged = new boolean[chunks.length];

        int maximumRunOfChunks = 0;
        int startOfCurrentRun;
        int numAdjacentRuns = 0;

        List<Chunk> mergedChunks = new ArrayList<>();

        startOfCurrentRun = 0;

        // Loop through chunks, and record in adjacencyRunStart where each
        // run of adjacent chunks begins and how many are in that run.  For
        // example, given chunks A B C D E F, if A is adjacent to B, but not
        // to C, and C, D, E and F are all adjacent,
        //   adjacencyRunStart[0] == 0; adjacencyRunLength[0] == 2
        //   adjacencyRunStart[1] == 2; adjacencyRunLength[1] == 4
        for (int i = 1; i < chunks.length; i++) {
            if (!chunks[i-1].isAdjacentTo(chunks[i])) {
                int lengthOfRun = i - startOfCurrentRun;

                // Track the longest run of chunks found
                if (maximumRunOfChunks < lengthOfRun) {
                    maximumRunOfChunks = lengthOfRun;
                }

                if (lengthOfRun > 1 ) {
                    adjacencyRunLength[numAdjacentRuns] = lengthOfRun;
                    adjacencyRunStart[numAdjacentRuns] = startOfCurrentRun;
                    numAdjacentRuns++;
                }

                startOfCurrentRun = i;
            }
        }

        if (chunks.length - startOfCurrentRun > 1) {
            int lengthOfRun = chunks.length - startOfCurrentRun;

            // Track the longest run of chunks found
            if (maximumRunOfChunks < lengthOfRun) {
                maximumRunOfChunks = lengthOfRun;
            }

            adjacencyRunLength[numAdjacentRuns] =
                        chunks.length - startOfCurrentRun;
            adjacencyRunStart[numAdjacentRuns] = startOfCurrentRun;
            numAdjacentRuns++;
        }

        // Try merging adjacent chunks to come up with better sized chunks for
        // outlining.  This algorithm is not optimal, but it should be
        // reasonably fast.  Consider an example like this, where four chunks
        // of the sizes specified in brackets are adjacent.  The best way of
        // combining these chunks would be to merge the first pair and merge
        // the last three to form two chunks, but the algorithm will merge the
        // three in the middle instead, leaving three chunks in all.
        //    [25000] [25000] [20000] [1000] [20000]

        // Start by trying to merge the maximum number of adjacent chunks, and
        // work down from there.
        for (int numToMerge = maximumRunOfChunks; numToMerge>1; numToMerge--) {
            // Look at each run of adjacent chunks
            for (int run = 0; run < numAdjacentRuns; run++) {
                int runStart = adjacencyRunStart[run];
                int runEnd = runStart + adjacencyRunLength[run] - 1;

                boolean foundChunksToMerge = false;

                // Within the current run of adjacent chunks, look at all
                // "subruns" of length numToMerge, until we run out or find
                // a subrun that can be merged.
                for (int mergeStart = runStart;
                     mergeStart+numToMerge-1 <= runEnd && !foundChunksToMerge;
                     mergeStart++) {
                    int mergeEnd = mergeStart + numToMerge - 1;
                    int mergeSize = 0;

                    // Find out how big the subrun is
                    for (int j = mergeStart; j <= mergeEnd; j++) {
                        mergeSize = mergeSize + chunks[j].getChunkSize();
                    }

                    // If the current subrun is small enough to outline,
                    // merge it, and split the remaining chunks in the run
                    if (mergeSize <= TARGET_METHOD_SIZE) {
                        foundChunksToMerge = true;

                        for (int j = mergeStart; j <= mergeEnd; j++) {
                            chunkWasMerged[j] = true;
                        }

                        mergedChunks.add(
                                new Chunk(chunks[mergeStart].getChunkStart(),
                                          chunks[mergeEnd].getChunkEnd()));

                        // Adjust the length of the current run of adjacent
                        // chunks to end at the newly merged chunk...
                        adjacencyRunLength[run] =
                                adjacencyRunStart[run] - mergeStart;

                        int trailingRunLength = runEnd - mergeEnd;

                        // and any chunks that follow the newly merged chunk
                        // in the current run of adjacent chunks form another
                        // new run of adjacent chunks
                        if (trailingRunLength >= 2) {
                            adjacencyRunStart[numAdjacentRuns] = mergeEnd + 1;
                            adjacencyRunLength[numAdjacentRuns] =
                                                            trailingRunLength;
                            numAdjacentRuns++;
                        }
                    }
                }
            }
        }

        // Make a final pass for any chunk that wasn't merged with a sibling
        // and include it in the list of chunks after merging.
        for (int i = 0; i < chunks.length; i++) {
            if (!chunkWasMerged[i]) {
                mergedChunks.add(chunks[i]);
            }
        }

        return mergedChunks;
    }

    /**
     * Breaks up the IL for this {@link MethodGenerator} into separate
     * outlined methods so that no method exceeds the 64KB limit on the length
     * of the byte code associated with a method.
     * @param classGen The {@link ClassGen} with which the generated methods
     *                 will be associated
     * @param originalMethodSize The number of bytes of bytecode represented by
     *                 the {@link InstructionList} of this method
     * @return an array of the outlined <code>Method</code>s and the original
     *         method itself
     */
    public Method[] outlineChunks(ClassGenerator classGen,
                                  int originalMethodSize) {
        List<Method> methodsOutlined = new ArrayList<>();
        int currentMethodSize = originalMethodSize;

        int outlinedCount = 0;
        boolean moreMethodsOutlined;
        String originalMethodName = getName();

        // Special handling for initialization methods.  No other methods can
        // include the less than and greater than characters in their names,
        // so we munge the names here.
        if (originalMethodName.equals("<init>")) {
            originalMethodName = "$lt$init$gt$";
        } else if (originalMethodName.equals("<clinit>")) {
            originalMethodName = "$lt$clinit$gt$";
        }

        // Loop until the original method comes in under the JVM limit or
        // the loop was unable to outline any more methods
        do {
            // Get all the best candidates for outlining, and sort them in
            // ascending order of size
            List<Chunk> candidateChunks = getCandidateChunks(classGen,
                                                           currentMethodSize);
            Collections.sort(candidateChunks);

            moreMethodsOutlined = false;

            // Loop over the candidates for outlining, from the largest to the
            // smallest and outline them one at a time, until the loop has
            // outlined all or the original method comes in under the JVM
            // limit on the size of a method.
            for (int i = candidateChunks.size()-1;
                 i >= 0 && currentMethodSize > TARGET_METHOD_SIZE;
                 i--) {
                Chunk chunkToOutline = candidateChunks.get(i);

                methodsOutlined.add(outline(chunkToOutline.getChunkStart(),
                                            chunkToOutline.getChunkEnd(),
                                            originalMethodName + "$outline$"
                                                               + outlinedCount,
                                            classGen));
                outlinedCount++;
                moreMethodsOutlined = true;

                InstructionList il = getInstructionList();
                InstructionHandle lastInst = il.getEnd();
                il.setPositions();

                // Check the size of the method now
                currentMethodSize =
                        lastInst.getPosition()
                                + lastInst.getInstruction().getLength();
            }
        } while (moreMethodsOutlined && currentMethodSize > TARGET_METHOD_SIZE);

        // Outlining failed to reduce the size of the current method
        // sufficiently.  Throw an internal error.
        if (currentMethodSize > MAX_METHOD_SIZE) {
            String msg = (new ErrorMsg(ErrorMsg.OUTLINE_ERR_METHOD_TOO_BIG))
                                  .toString();
            throw new InternalError(msg);
        }

        Method[] methodsArr = new Method[methodsOutlined.size() + 1];
        methodsOutlined.toArray(methodsArr);

        methodsArr[methodsOutlined.size()] = getThisMethod();

        return methodsArr;
    }

    /**
     * Given an outlineable chunk of code in the current {@link MethodGenerator}
     * move ("outline") the chunk to a new method, and replace the chunk in the
     * old method with a reference to that new method.  No
     * {@link OutlineableChunkStart} or {@link OutlineableChunkEnd} instructions
     * are copied.
     * @param first The {@link InstructionHandle} of the first instruction in
     *              the chunk to outline
     * @param last The <code>InstructionHandle</code> of the last instruction in
     *             the chunk to outline
     * @param outlinedMethodName The name of the new method
     * @param classGen The {@link ClassGenerator} of which the original
     *              and new methods will be members
     * @return The new {@link Method} containing the outlined code.
     */
    private Method outline(InstructionHandle first, InstructionHandle last,
                           String outlinedMethodName, ClassGenerator classGen) {
        // We're not equipped to deal with exception handlers yet.  Bail out!
        if (getExceptionHandlers().length != 0) {
            String msg = (new ErrorMsg(ErrorMsg.OUTLINE_ERR_TRY_CATCH))
                                  .toString();
            throw new InternalError(msg);
        }

        int outlineChunkStartOffset = first.getPosition();
        int outlineChunkEndOffset = last.getPosition()
                                        + last.getInstruction().getLength();

        ConstantPoolGen cpg = getConstantPool();

        // Create new outlined method with signature:
        //
        //   private final outlinedMethodName(CopyLocals copyLocals);
        //
        // CopyLocals is an object that is used to copy-in/copy-out local
        // variables that are used by the outlined method.   Only locals whose
        // value is potentially set or referenced outside the range of the
        // chunk that is being outlined will be represented in CopyLocals.  The
        // type of the variable for copying local variables is actually
        // generated to be unique - it is not named CopyLocals.
        //
        // The outlined method never needs to be referenced outside of this
        // class, and will never be overridden, so we mark it private final.
        final InstructionList newIL = new InstructionList();

        final XSLTC  xsltc = classGen.getParser().getXSLTC();
        final String argTypeName = xsltc.getHelperClassName();
        final Type[] argTypes =
            new Type[] {(new ObjectType(argTypeName)).toJCType()};
        final String argName = "copyLocals";
        final String[] argNames = new String[] {argName};

        int methodAttributes = ACC_PRIVATE | ACC_FINAL;
        final boolean isStaticMethod = (getAccessFlags() & ACC_STATIC) != 0;

        if (isStaticMethod) {
            methodAttributes = methodAttributes | ACC_STATIC;
        }

        final MethodGenerator outlinedMethodGen =
            new MethodGenerator(methodAttributes,
                                com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                argTypes, argNames, outlinedMethodName,
                                getClassName(), newIL, cpg);

        // Create class for copying local variables to the outlined method.
        // The fields the class will need to contain will be determined as the
        // code in the outlineable chunk is examined.
        ClassGenerator copyAreaCG
            = new ClassGenerator(argTypeName, OBJECT_CLASS, argTypeName+".java",
                                 ACC_FINAL | ACC_PUBLIC | ACC_SUPER, null,
                                 classGen.getStylesheet()) {
                      public boolean isExternal() {
                          return true;
                      }
                  };
        ConstantPoolGen copyAreaCPG = copyAreaCG.getConstantPool();
        copyAreaCG.addEmptyConstructor(ACC_PUBLIC);

        // Number of fields in the copy class
        int copyAreaFieldCount = 0;

        // The handle for the instruction after the last one to be outlined.
        // Note that this should never end up being null.  An outlineable chunk
        // won't contain a RETURN instruction or other branch out of the chunk,
        // and the JVM specification prohibits code in a method from just
        // "falling off the end" so this should always point to a valid handle.
        InstructionHandle limit = last.getNext();

        // InstructionLists for copying values into and out of an instance of
        // CopyLocals:
        //      oldMethCoypInIL  - from locals in old method into an instance
        //                         of the CopyLocals class (oldMethCopyInIL)
        //      oldMethCopyOutIL - from CopyLocals back into locals in the old
        //                         method
        //      newMethCopyInIL  - from CopyLocals into locals in the new
        //                         method
        //      newMethCopyOutIL - from locals in new method into the instance
        //                         of the CopyLocals class
        InstructionList oldMethCopyInIL  = new InstructionList();
        InstructionList oldMethCopyOutIL = new InstructionList();
        InstructionList newMethCopyInIL  = new InstructionList();
        InstructionList newMethCopyOutIL = new InstructionList();

        // Allocate instance of class in which we'll copy in or copy out locals
        // and make two copies:  last copy is used to invoke constructor;
        // other two are used for references to fields in the CopyLocals object
        InstructionHandle outlinedMethodCallSetup =
            oldMethCopyInIL.append(new NEW(cpg.addClass(argTypeName)));
        oldMethCopyInIL.append(InstructionConst.DUP);
        oldMethCopyInIL.append(InstructionConst.DUP);
        oldMethCopyInIL.append(
            new INVOKESPECIAL(cpg.addMethodref(argTypeName, "<init>", "()V")));

        // Generate code to invoke the new outlined method, and place the code
        // on oldMethCopyOutIL
        InstructionHandle outlinedMethodRef;

        if (isStaticMethod) {
            outlinedMethodRef =
                oldMethCopyOutIL.append(
                    new INVOKESTATIC(cpg.addMethodref(
                                          classGen.getClassName(),
                                          outlinedMethodName,
                                          outlinedMethodGen.getSignature())));
        } else {
            oldMethCopyOutIL.append(InstructionConst.THIS);
            oldMethCopyOutIL.append(InstructionConst.SWAP);
            outlinedMethodRef =
                oldMethCopyOutIL.append(
                    new INVOKEVIRTUAL(cpg.addMethodref(
                                          classGen.getClassName(),
                                          outlinedMethodName,
                                          outlinedMethodGen.getSignature())));
        }

        // Used to keep track of the first in a sequence of
        // OutlineableChunkStart instructions
        boolean chunkStartTargetMappingsPending = false;
        InstructionHandle pendingTargetMappingHandle = null;

        // Used to keep track of the last instruction that was copied
        InstructionHandle lastCopyHandle = null;

        // Keeps track of the mapping from instruction handles in the old
        // method to instruction handles in the outlined method.  Only need
        // to track instructions that are targeted by something else in the
        // generated BCEL
        HashMap<InstructionHandle, InstructionHandle> targetMap = new HashMap<>();

        // Keeps track of the mapping from local variables in the old method
        // to local variables in the outlined method.
        HashMap<LocalVariableGen, LocalVariableGen> localVarMap = new HashMap<>();

        HashMap<LocalVariableGen, InstructionHandle> revisedLocalVarStart = new HashMap<>();
        HashMap<LocalVariableGen, InstructionHandle> revisedLocalVarEnd = new HashMap<>();

        // Pass 1: Make copies of all instructions, append them to the new list
        // and associate old instruction references with the new ones, i.e.,
        // a 1:1 mapping.  The special marker instructions are not copied.
        // Also, identify local variables whose values need to be copied into or
        // out of the new outlined method, and builds up targetMap and
        // localVarMap as described above.  The code identifies those local
        // variables first so that they can have fixed slots in the stack
        // frame for the outlined method assigned them ahead of all those
        // variables that don't need to exist for the entirety of the outlined
        // method invocation.
        for (InstructionHandle ih = first; ih != limit; ih = ih.getNext()) {
            Instruction inst = ih.getInstruction();

            // MarkerInstructions are not copied, so if something else targets
            // one, the targetMap will point to the nearest copied sibling
            // InstructionHandle:  for an OutlineableChunkEnd, the nearest
            // preceding sibling; for an OutlineableChunkStart, the nearest
            // following sibling.
            if (inst instanceof MarkerInstruction) {
                if (ih.hasTargeters()) {
                    if (inst instanceof OutlineableChunkEnd) {
                        targetMap.put(ih, lastCopyHandle);
                    } else {
                        if (!chunkStartTargetMappingsPending)  {
                            chunkStartTargetMappingsPending = true;
                            pendingTargetMappingHandle = ih;
                        }
                    }
                }
            } else {
                // Copy the instruction and append it to the outlined method's
                // InstructionList.
                Instruction c = inst.copy(); // Use clone for shallow copy

                if (c instanceof BranchInstruction) {
                    lastCopyHandle = newIL.append((BranchInstruction)c);
                } else {
                    lastCopyHandle = newIL.append(c);
                }

                if (c instanceof LocalVariableInstruction
                        || c instanceof RET) {
                    // For any instruction that touches a local variable,
                    // check whether the local variable's value needs to be
                    // copied into or out of the outlined method.  If so,
                    // generate the code to perform the necessary copying, and
                    // use localVarMap to map the variable in the original
                    // method to the variable in the new method.
                    IndexedInstruction lvi = (IndexedInstruction)c;
                    int oldLocalVarIndex = lvi.getIndex();
                    LocalVariableGen oldLVG =
                            getLocalVariableRegistry()
                                .lookupRegisteredLocalVariable(oldLocalVarIndex,
                                                              ih.getPosition());
                    LocalVariableGen newLVG = localVarMap.get(oldLVG);

                    // Has the code already mapped this local variable to a
                    // local in the new method?
                    if (localVarMap.get(oldLVG) == null) {
                        // Determine whether the local variable needs to be
                        // copied into or out of the outlined by checking
                        // whether the range of instructions in which the
                        // variable is accessible is outside the range of
                        // instructions in the outlineable chunk.
                        // Special case a chunk start offset of zero:  a local
                        // variable live at that position must be a method
                        // parameter, so the code doesn't need to check whether
                        // the variable is live before that point; being live
                        // at offset zero is sufficient to know that the value
                        // must be copied in to the outlined method.
                        boolean copyInLocalValue =
                            offsetInLocalVariableGenRange(oldLVG,
                                                (outlineChunkStartOffset != 0)
                                                    ? outlineChunkStartOffset-1
                                                    : 0);
                        boolean copyOutLocalValue =
                            offsetInLocalVariableGenRange(oldLVG,
                                                outlineChunkEndOffset+1);

                        // For any variable that needs to be copied into or out
                        // of the outlined method, create a field in the
                        // CopyLocals class, and generate the necessary code for
                        // copying the value.
                        if (copyInLocalValue || copyOutLocalValue) {
                            String varName = oldLVG.getName();
                            Type varType = oldLVG.getType();
                            newLVG = outlinedMethodGen.addLocalVariable(varName,
                                                                        varType,
                                                                        null,
                                                                        null);
                            int newLocalVarIndex = newLVG.getIndex();
                            String varSignature = varType.getSignature();

                            // Record the mapping from the old local to the new
                            localVarMap.put(oldLVG, newLVG);

                            copyAreaFieldCount++;
                            String copyAreaFieldName =
                                           "field" + copyAreaFieldCount;
                            copyAreaCG.addField(
                                new Field(ACC_PUBLIC,
                                        copyAreaCPG.addUtf8(copyAreaFieldName),
                                        copyAreaCPG.addUtf8(varSignature),
                                        null, copyAreaCPG.getConstantPool()));

                            int fieldRef = cpg.addFieldref(argTypeName,
                                                           copyAreaFieldName,
                                                           varSignature);

                            if (copyInLocalValue) {
                                // Generate code for the old method to store the
                                // value of the local into the correct field in
                                // CopyLocals prior to invocation of the
                                // outlined method.
                                oldMethCopyInIL.append(
                                        InstructionConst.DUP);
                                InstructionHandle copyInLoad =
                                    oldMethCopyInIL.append(
                                        loadLocal(oldLocalVarIndex, varType));
                                oldMethCopyInIL.append(new PUTFIELD(fieldRef));

                                // If the end of the live range of the old
                                // variable was in the middle of the outlined
                                // chunk.  Make the load of its value the new
                                // end of its range.
                                if (!copyOutLocalValue) {
                                    revisedLocalVarEnd.put(oldLVG, copyInLoad);
                                }

                                // Generate code for start of the outlined
                                // method to copy the value from a field in
                                // CopyLocals to the new local in the outlined
                                // method
                                newMethCopyInIL.append(
                                        InstructionConst.ALOAD_1);
                                newMethCopyInIL.append(new GETFIELD(fieldRef));
                                newMethCopyInIL.append(
                                        storeLocal(newLocalVarIndex, varType));
                            }

                            if (copyOutLocalValue) {
                                // Generate code for the end of the outlined
                                // method to copy the value from the new local
                                // variable into a field in CopyLocals
                                // method
                                newMethCopyOutIL.append(
                                        InstructionConst.ALOAD_1);
                                newMethCopyOutIL.append(
                                        loadLocal(newLocalVarIndex, varType));
                                newMethCopyOutIL.append(new PUTFIELD(fieldRef));

                                // Generate code to copy the value from a field
                                // in CopyLocals into a local in the original
                                // method following invocation of the outlined
                                // method.
                                oldMethCopyOutIL.append(
                                        InstructionConst.DUP);
                                oldMethCopyOutIL.append(new GETFIELD(fieldRef));
                                InstructionHandle copyOutStore =
                                    oldMethCopyOutIL.append(
                                        storeLocal(oldLocalVarIndex, varType));

                                // If the start of the live range of the old
                                // variable was in the middle of the outlined
                                // chunk.  Make this store into it the new start
                                // of its range.
                                if (!copyInLocalValue) {
                                    revisedLocalVarStart.put(oldLVG,
                                                             copyOutStore);
                                }
                            }
                        }
                    }
                }

                if (ih.hasTargeters()) {
                    targetMap.put(ih, lastCopyHandle);
                }

                // If this is the first instruction copied following a sequence
                // of OutlineableChunkStart instructions, indicate that the
                // sequence of old instruction all map to this newly created
                // instruction
                if (chunkStartTargetMappingsPending) {
                    do {
                         targetMap.put(pendingTargetMappingHandle,
                                       lastCopyHandle);
                         pendingTargetMappingHandle =
                                 pendingTargetMappingHandle.getNext();
                    } while(pendingTargetMappingHandle != ih);

                    chunkStartTargetMappingsPending = false;
                }
            }
        }

        // Pass 2: Walk old and new instruction lists, updating branch targets
        // and local variable references in the new list
        InstructionHandle ih = first;
        InstructionHandle ch = newIL.getStart();

        while (ch != null) {
            // i == old instruction; c == copied instruction
            Instruction i = ih.getInstruction();
            Instruction c = ch.getInstruction();

            if (i instanceof BranchInstruction) {
                BranchInstruction bc      = (BranchInstruction)c;
                BranchInstruction bi      = (BranchInstruction)i;
                InstructionHandle itarget = bi.getTarget(); // old target

                // New target must be in targetMap
                InstructionHandle newTarget = targetMap.get(itarget);

                bc.setTarget(newTarget);

                // Handle LOOKUPSWITCH or TABLESWITCH which may have many
                // target instructions
                if (bi instanceof Select) {
                    InstructionHandle[] itargets = ((Select)bi).getTargets();
                    InstructionHandle[] ctargets = ((Select)bc).getTargets();

                    // Update all targets
                    for (int j=0; j < itargets.length; j++) {
                        ctargets[j] = targetMap.get(itargets[j]);
                    }
                }
            }  else if (i instanceof LocalVariableInstruction
                            || i instanceof RET) {
                // For any instruction that touches a local variable,
                // map the location of the variable in the original
                // method to its location in the new method.
                IndexedInstruction lvi = (IndexedInstruction)c;
                int oldLocalVarIndex = lvi.getIndex();
                LocalVariableGen oldLVG =
                        getLocalVariableRegistry()
                                .lookupRegisteredLocalVariable(oldLocalVarIndex,
                                                              ih.getPosition());
                LocalVariableGen newLVG = localVarMap.get(oldLVG);
                int newLocalVarIndex;

                if (newLVG == null) {
                    // Create new variable based on old variable - use same
                    // name and type, but we will let the variable be active
                    // for the entire outlined method.
                    // LocalVariableGen oldLocal = oldLocals[oldLocalVarIndex];
                    String varName = oldLVG.getName();
                    Type varType = oldLVG.getType();
                    newLVG = outlinedMethodGen.addLocalVariable(varName,
                                                                varType,
                                                                null,
                                                                null);
                    newLocalVarIndex = newLVG.getIndex();
                    localVarMap.put(oldLVG, newLVG);

                    // The old variable's live range was wholly contained in
                    // the outlined chunk.  There should no longer be stores
                    // of values into it or loads of its value, so we can just
                    // mark its live range as the reference to the outlined
                    // method.
                    revisedLocalVarStart.put(oldLVG, outlinedMethodRef);
                    revisedLocalVarEnd.put(oldLVG, outlinedMethodRef);
                } else {
                    newLocalVarIndex = newLVG.getIndex();
                }
                lvi.setIndex(newLocalVarIndex);
            }

            // If the old instruction marks the end of the range of a local
            // variable, make sure that any slots on the stack reserved for
            // local variables are made available for reuse by calling
            // MethodGenerator.removeLocalVariable
            if (ih.hasTargeters()) {
                InstructionTargeter[] targeters = ih.getTargeters();

                for (int idx = 0; idx < targeters.length; idx++) {
                    InstructionTargeter targeter = targeters[idx];

                    if (targeter instanceof LocalVariableGen
                            && ((LocalVariableGen)targeter).getEnd()==ih) {
                        LocalVariableGen newLVG = localVarMap.get(targeter);
                        if (newLVG != null) {
                            outlinedMethodGen.removeLocalVariable(newLVG);
                        }
                    }
                }
            }

            // If the current instruction in the original list was a marker,
            // it wasn't copied, so don't advance through the list of copied
            // instructions yet.
            if (!(i instanceof MarkerInstruction)) {
                ch = ch.getNext();
            }
            ih = ih.getNext();

        }

        // POP the reference to the CopyLocals object from the stack
        oldMethCopyOutIL.append(InstructionConst.POP);

        for (Map.Entry<LocalVariableGen, InstructionHandle> lvgRangeStartPair :
                revisedLocalVarStart.entrySet()) {
            LocalVariableGen lvg = lvgRangeStartPair.getKey();
            InstructionHandle startInst = lvgRangeStartPair.getValue();

            lvg.setStart(startInst);
        }

        for (Map.Entry<LocalVariableGen, InstructionHandle> lvgRangeEndPair :
                revisedLocalVarEnd.entrySet()) {
            LocalVariableGen lvg = lvgRangeEndPair.getKey();
            InstructionHandle endInst = lvgRangeEndPair.getValue();

            lvg.setEnd(endInst);
        }

        xsltc.dumpClass(copyAreaCG.getJavaClass());

        // Assemble the instruction lists so that the old method invokes the
        // new outlined method
        InstructionList oldMethodIL = getInstructionList();

        oldMethodIL.insert(first, oldMethCopyInIL);
        oldMethodIL.insert(first, oldMethCopyOutIL);

        // Insert the copying code into the outlined method
        newIL.insert(newMethCopyInIL);
        newIL.append(newMethCopyOutIL);
        newIL.append(InstructionConst.RETURN);

        // Discard instructions in outlineable chunk from old method
        try {
            oldMethodIL.delete(first, last);
        } catch (TargetLostException e) {
            InstructionHandle[] targets = e.getTargets();
            // If there were still references to old instructions lingering,
            // clean those up.  The only instructions targetting the deleted
            // instructions should have been part of the chunk that was just
            // deleted, except that instructions might branch to the start of
            // the outlined chunk; similarly, all the live ranges of local
            // variables should have been adjusted, except for unreferenced
            // variables.
            for (int i = 0; i < targets.length; i++) {
                InstructionHandle lostTarget = targets[i];
                InstructionTargeter[] targeters = lostTarget.getTargeters();
                for (int j = 0; j < targeters.length; j++) {
                    if (targeters[j] instanceof LocalVariableGen) {
                        LocalVariableGen lvgTargeter =
                                             (LocalVariableGen) targeters[j];
                        // In the case of any lingering variable references,
                        // just make the live range point to the outlined
                        // function reference.  Such variables should be unused
                        // anyway.
                        if (lvgTargeter.getStart() == lostTarget) {
                            lvgTargeter.setStart(outlinedMethodRef);
                        }
                        if (lvgTargeter.getEnd() == lostTarget) {
                            lvgTargeter.setEnd(outlinedMethodRef);
                        }
                    } else {
                        targeters[j].updateTarget(lostTarget,
                                                  outlinedMethodCallSetup);
                    }
                }
            }
        }

        // Make a copy for the new method of all exceptions that might be thrown
        String[] exceptions = getExceptions();
        for (int i = 0; i < exceptions.length; i++) {
            outlinedMethodGen.addException(exceptions[i]);
        }

        return outlinedMethodGen.getThisMethod();
    }

    /**
     * Helper method to generate an instance of a subclass of
     * {@link LoadInstruction} based on the specified {@link Type} that will
     * load the specified local variable
     * @param index the JVM stack frame index of the variable that is to be
     * loaded
     * @param type the {@link Type} of the variable
     * @return the generated {@link LoadInstruction}
     */
    private static Instruction loadLocal(int index, Type type) {
        if (type == Type.BOOLEAN) {
           return new ILOAD(index);
        } else if (type == Type.INT) {
           return new ILOAD(index);
        } else if (type == Type.SHORT) {
           return new ILOAD(index);
        } else if (type == Type.LONG) {
           return new LLOAD(index);
        } else if (type == Type.BYTE) {
           return new ILOAD(index);
        } else if (type == Type.CHAR) {
           return new ILOAD(index);
        } else if (type == Type.FLOAT) {
           return new FLOAD(index);
        } else if (type == Type.DOUBLE) {
           return new DLOAD(index);
        } else {
           return new ALOAD(index);
        }
    }

    /**
     * Helper method to generate an instance of a subclass of
     * {@link StoreInstruction} based on the specified {@link Type} that will
     * store a value in the specified local variable
     * @param index the JVM stack frame index of the variable that is to be
     * stored
     * @param type the {@link Type} of the variable
     * @return the generated {@link StoredInstruction}
     */
    private static Instruction storeLocal(int index, Type type) {
        if (type == Type.BOOLEAN) {
           return new ISTORE(index);
        } else if (type == Type.INT) {
           return new ISTORE(index);
        } else if (type == Type.SHORT) {
           return new ISTORE(index);
        } else if (type == Type.LONG) {
           return new LSTORE(index);
        } else if (type == Type.BYTE) {
           return new ISTORE(index);
        } else if (type == Type.CHAR) {
           return new ISTORE(index);
        } else if (type == Type.FLOAT) {
           return new FSTORE(index);
        } else if (type == Type.DOUBLE) {
           return new DSTORE(index);
        } else {
           return new ASTORE(index);
        }
    }

    /**
     * Track the number of outlineable chunks seen.
     */
    private int m_totalChunks = 0;

    /**
     * Track the number of outlineable chunks started but not yet ended.  Used
     * to detect imbalances in byte code generation.
     */
    private int m_openChunks = 0;

    /**
     * Mark the end of the method's
     * {@link InstructionList} as the start of an outlineable chunk of code.
     * The outlineable chunk begins after the {@link InstructionHandle} that is
     * at the end of the method's {@link InstructionList}, or at the start of
     * the method if the <code>InstructionList</code> is empty.
     * See {@link OutlineableChunkStart} for more information.
     */
    public void markChunkStart() {
        // m_chunkTree.markChunkStart();
        getInstructionList()
                .append(OutlineableChunkStart.OUTLINEABLECHUNKSTART);
        m_totalChunks++;
        m_openChunks++;
    }

    /**
     * Mark the end of an outlineable chunk of code.  See
     * {@link OutlineableChunkStart} for more information.
     */
    public void markChunkEnd() {
        // m_chunkTree.markChunkEnd();
        getInstructionList()
                .append(OutlineableChunkEnd.OUTLINEABLECHUNKEND);
        m_openChunks--;
        if (m_openChunks < 0) {
            String msg = (new ErrorMsg(ErrorMsg.OUTLINE_ERR_UNBALANCED_MARKERS))
                                 .toString();
            throw new InternalError(msg);
        }
    }

    /**
     * <p>Get all {@link Method}s generated by this {@link MethodGenerator}.
     * The {@link MethodGen#getMethod()} only returns a single
     * <code>Method</code> object.  This method takes into account the Java
     * Virtual Machine Specification limit of 64KB on the size of a method, and
     * may return more than one <code>Method</code>.</p>
     * <p>If the code associated with the <code>MethodGenerator</code> would
     * exceed the 64KB limit, this method will attempt to split the code in
     * the {@link InstructionList} associated with this
     * <code>MethodGenerator</code> into several methods.</p>
     * @param classGen the {@link ClassGenerator} of which these methods are
     *                 members
     * @return an array of all the <code>Method</code>s generated
     */
    Method[] getGeneratedMethods(ClassGenerator classGen) {
        Method[] generatedMethods;
        InstructionList il = getInstructionList();
        InstructionHandle last = il.getEnd();

        il.setPositions();

        int instructionListSize =
                    last.getPosition() + last.getInstruction().getLength();

        // Need to look for any branch target offsets that exceed the range
        // [-32768,32767]
        if (instructionListSize > MAX_BRANCH_TARGET_OFFSET) {
            boolean ilChanged = widenConditionalBranchTargetOffsets();

            // If any branch instructions needed widening, recompute the size
            // of the byte code for the method
            if (ilChanged) {
                il.setPositions();
                last = il.getEnd();
                instructionListSize =
                        last.getPosition() + last.getInstruction().getLength();
            }
        }

        if (instructionListSize > MAX_METHOD_SIZE) {
            generatedMethods = outlineChunks(classGen, instructionListSize);
        } else {
            generatedMethods = new Method[] {getThisMethod()};
        }
        return generatedMethods;
    }

    protected Method getThisMethod() {
        stripAttributes(true);
        setMaxLocals();
        setMaxStack();
        removeNOPs();

        return getMethod();
    }
    /**
     * <p>Rewrites branches to avoid the JVM limits of relative branch
     * offsets.  There is no need to invoke this method if the bytecode for the
     * {@link MethodGenerator} does not exceed 32KB.</p>
     * <p>The Java Virtual Machine Specification permits the code portion of a
     * method to be up to 64KB in length.  However, some control transfer
     * instructions specify relative offsets as a signed 16-bit quantity,
     * limiting the range to a subset of the instructions that might be in a
     * method.</p>
     * <p>The <code>TABLESWITCH</code> and <code>LOOKUPSWITCH</code>
     * instructions always use 32-bit signed relative offsets, so they are
     * immune to this problem.</p>
     * <p>The <code>GOTO</code> and <code>JSR</code>
     * instructions come in two forms, one of which uses 16-bit relative
     * offsets, and the other of which uses 32-bit relative offsets.  The BCEL
     * library decides whether to use the wide form of <code>GOTO</code> or
     * <code>JSR</code>instructions based on the relative offset of the target
     * of the instruction without any intervention by the user of the
     * library.</p>
     * <p>This leaves the various conditional branch instructions,
     * <code>IFEQ</code>, <code>IFNULL</code>, <code>IF_ICMPEQ</code>,
     * <em>et al.</em>, all of which use 16-bit signed relative offsets, with no
     * 32-bit wide form available.</p>
     * <p>This method scans the {@link InstructionList} associated with this
     * {@link MethodGenerator} and finds all conditional branch instructions
     * that might exceed the 16-bit limitation for relative branch offsets.
     * The logic of each such instruction is inverted, and made to target the
     * instruction which follows it.  An unconditional branch to the original
     * target of the instruction is then inserted between the conditional
     * branch and the instruction which previously followed it.  The
     * unconditional branch is permitted to have a 16-bit or a 32-bit relative
     * offset, as described above.  For example,
     * <code>
     * 1234:   NOP
     *          ...
     * 55278:  IFEQ -54044
     * 55280:  NOP
     * </code>
     * is rewritten as
     * <code>
     * 1234:   NOP
     *          ...
     * 55278:  IFNE 7
     * 55280:  GOTO_W -54046
     * 55285:  NOP
     * </code></p>
     * <p><b>Preconditions:</b>
     * <ul><li>The {@link InstructionList#setPositions()} has been called for
     * the <code>InstructionList</code> associated with this
     * <code>MethodGenerator</code>.
     * </li></ul></p>
     * <p><b>Postconditions:</b>
     * <ul><li>Any further changes to the <code>InstructionList</code> for this
     * <code>MethodGenerator</code> will invalidate the changes made by this
     * method.</li></ul>
     * </p>
     * @return <code>true</code> if the <code>InstructionList</code> was
     * modified; <code>false</code> otherwise
     * @see The Java Virtual Machine Specification, Second Edition
     */
    boolean widenConditionalBranchTargetOffsets() {
        boolean ilChanged = false;
        int maxOffsetChange = 0;
        InstructionList il = getInstructionList();

        // Loop through all the instructions, finding those that would be
        // affected by inserting new instructions in the InstructionList, and
        // calculating the maximum amount by which the relative offset between
        // two instructions could possibly change.
        // In part this loop duplicates code in
        // org.apache.bcel.generic.InstructionList.setPosition(), which does
        // this to determine whether to use 16-bit or 32-bit offsets for GOTO
        // and JSR instructions.  Ideally, that method would do the same for
        // conditional branch instructions, but it doesn't, so we duplicate the
        // processing here.
        for (InstructionHandle ih = il.getStart();
             ih != null;
             ih = ih.getNext()) {
            Instruction inst = ih.getInstruction();

            switch (inst.getOpcode()) {
                // Instructions that may have 16-bit or 32-bit branch targets.
                // The size of the branch offset might increase by two bytes.
                case Const.GOTO:
                case Const.JSR:
                    maxOffsetChange = maxOffsetChange + 2;
                    break;
                // Instructions that contain padding for alignment purposes
                // Up to three bytes of padding might be needed.  For greater
                // accuracy, we should be able to discount any padding already
                // added to these instructions by InstructionList.setPosition(),
                // their APIs do not expose that information.
                case Const.TABLESWITCH:
                case Const.LOOKUPSWITCH:
                    maxOffsetChange = maxOffsetChange + 3;
                    break;
                // Instructions that might be rewritten by this method as a
                // conditional branch followed by an unconditional branch.
                // The unconditional branch would require five bytes.
                case Const.IF_ACMPEQ:
                case Const.IF_ACMPNE:
                case Const.IF_ICMPEQ:
                case Const.IF_ICMPGE:
                case Const.IF_ICMPGT:
                case Const.IF_ICMPLE:
                case Const.IF_ICMPLT:
                case Const.IF_ICMPNE:
                case Const.IFEQ:
                case Const.IFGE:
                case Const.IFGT:
                case Const.IFLE:
                case Const.IFLT:
                case Const.IFNE:
                case Const.IFNONNULL:
                case Const.IFNULL:
                    maxOffsetChange = maxOffsetChange + 5;
                    break;
            }
        }

        // Now that the maximum number of bytes by which the method might grow
        // has been determined, look for conditional branches to see which
        // might possibly exceed the 16-bit relative offset.
        for (InstructionHandle ih = il.getStart();
             ih != null;
             ih = ih.getNext()) {
            Instruction inst = ih.getInstruction();

            if (inst instanceof IfInstruction) {
                IfInstruction oldIfInst = (IfInstruction)inst;
                BranchHandle oldIfHandle = (BranchHandle)ih;
                InstructionHandle target = oldIfInst.getTarget();
                int relativeTargetOffset = target.getPosition()
                                               - oldIfHandle.getPosition();

                // Consider the worst case scenario in which the conditional
                // branch and its target are separated by all the instructions
                // in the method that might increase in size.  If that results
                // in a relative offset that cannot be represented as a 32-bit
                // signed quantity, rewrite the instruction as described above.
                if ((relativeTargetOffset - maxOffsetChange
                             < MIN_BRANCH_TARGET_OFFSET)
                        || (relativeTargetOffset + maxOffsetChange
                                    > MAX_BRANCH_TARGET_OFFSET)) {
                    // Invert the logic of the IF instruction, and append
                    // that to the InstructionList following the original IF
                    // instruction
                    InstructionHandle nextHandle = oldIfHandle.getNext();
                    IfInstruction invertedIfInst = oldIfInst.negate();
                    BranchHandle invertedIfHandle = il.append(oldIfHandle,
                                                              invertedIfInst);

                    // Append an unconditional branch to the target of the
                    // original IF instruction after the new IF instruction
                    BranchHandle gotoHandle = il.append(invertedIfHandle,
                                                        new GOTO(target));

                    // If the original IF was the last instruction in
                    // InstructionList, add a new no-op to act as the target
                    // of the new IF
                    if (nextHandle == null) {
                        nextHandle = il.append(gotoHandle, InstructionConst.NOP);
                    }

                    // Make the new IF instruction branch around the GOTO
                    invertedIfHandle.updateTarget(target, nextHandle);

                    // If anything still "points" to the old IF instruction,
                    // make adjustments to refer to either the new IF or GOTO
                    // instruction
                    if (oldIfHandle.hasTargeters()) {
                        InstructionTargeter[] targeters =
                                                  oldIfHandle.getTargeters();

                        for (int i = 0; i < targeters.length; i++) {
                            InstructionTargeter targeter = targeters[i];
                            // Ideally, one should simply be able to use
                            // InstructionTargeter.updateTarget to change
                            // references to the old IF instruction to the new
                            // IF instruction.  However, if a LocalVariableGen
                            // indicated the old IF marked the end of the range
                            // in which the IF variable is in use, the live
                            // range of the variable must extend to include the
                            // newly created GOTO instruction.  The need for
                            // this sort of specific knowledge of an
                            // implementor of the InstructionTargeter interface
                            // makes the code more fragile.  Future implementors
                            // of the interface might have similar requirements
                            // which wouldn't be accommodated seemlessly.
                            if (targeter instanceof LocalVariableGen) {
                                LocalVariableGen lvg =
                                        (LocalVariableGen) targeter;
                                if (lvg.getStart() == oldIfHandle) {
                                    lvg.setStart(invertedIfHandle);
                                } else if (lvg.getEnd() == oldIfHandle) {
                                    lvg.setEnd(gotoHandle);
                                }
                            } else {
                                targeter.updateTarget(oldIfHandle,
                                                      invertedIfHandle);
                            }
                        }
                    }

                    try {
                        il.delete(oldIfHandle);
                    } catch (TargetLostException tle) {
                        // This can never happen - we updated the list of
                        // instructions that target the deleted instruction
                        // prior to deleting it.
                        String msg =
                            new ErrorMsg(ErrorMsg.OUTLINE_ERR_DELETED_TARGET,
                                         tle.getMessage()).toString();
                        throw new InternalError(msg);
                    }

                    // Adjust the pointer in the InstructionList to point after
                    // the newly inserted IF instruction
                    ih = gotoHandle;

                    // Indicate that this method rewrote at least one IF
                    ilChanged = true;
                }
            }
        }

        // Did this method rewrite any IF instructions?
        return ilChanged;
    }
}
