/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.org.apache.bcel.internal.generic;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.classfile.AnnotationEntry;
import com.sun.org.apache.bcel.internal.classfile.Annotations;
import com.sun.org.apache.bcel.internal.classfile.Attribute;
import com.sun.org.apache.bcel.internal.classfile.Code;
import com.sun.org.apache.bcel.internal.classfile.CodeException;
import com.sun.org.apache.bcel.internal.classfile.ExceptionTable;
import com.sun.org.apache.bcel.internal.classfile.LineNumber;
import com.sun.org.apache.bcel.internal.classfile.LineNumberTable;
import com.sun.org.apache.bcel.internal.classfile.LocalVariable;
import com.sun.org.apache.bcel.internal.classfile.LocalVariableTable;
import com.sun.org.apache.bcel.internal.classfile.LocalVariableTypeTable;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.ParameterAnnotationEntry;
import com.sun.org.apache.bcel.internal.classfile.ParameterAnnotations;
import com.sun.org.apache.bcel.internal.classfile.RuntimeVisibleParameterAnnotations;
import com.sun.org.apache.bcel.internal.classfile.Utility;
import com.sun.org.apache.bcel.internal.util.BCELComparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

/**
 * Template class for building up a method. This is done by defining exception
 * handlers, adding thrown exceptions, local variables and attributes, whereas
 * the `LocalVariableTable' and `LineNumberTable' attributes will be set
 * automatically for the code. Use stripAttributes() if you don't like this.
 *
 * While generating code it may be necessary to insert NOP operations. You can
 * use the `removeNOPs' method to get rid off them.
 * The resulting method object can be obtained via the `getMethod()' method.
 *
 * @see     InstructionList
 * @see     Method
 * @LastModified: May 2021
 */
public class MethodGen extends FieldGenOrMethodGen {

    private String className;
    private Type[] argTypes;
    private String[] argNames;
    private int maxLocals;
    private int maxStack;
    private InstructionList il;
    private boolean stripAttributes;
    private LocalVariableTypeTable localVariableTypeTable = null;
    private final List<LocalVariableGen> variableList = new ArrayList<>();
    private final List<LineNumberGen> lineNumberList = new ArrayList<>();
    private final List<CodeExceptionGen> exceptionList = new ArrayList<>();
    private final List<String> throwsList = new ArrayList<>();
    private final List<Attribute> codeAttrsList = new ArrayList<>();

    private List<AnnotationEntryGen>[] paramAnnotations; // Array of lists containing AnnotationGen objects
    private boolean hasParameterAnnotations = false;
    private boolean haveUnpackedParameterAnnotations = false;

    private static BCELComparator bcelComparator = new BCELComparator() {

        @Override
        public boolean equals( final Object o1, final Object o2 ) {
            final MethodGen THIS = (MethodGen) o1;
            final MethodGen THAT = (MethodGen) o2;
            return Objects.equals(THIS.getName(), THAT.getName())
                    && Objects.equals(THIS.getSignature(), THAT.getSignature());
        }


        @Override
        public int hashCode( final Object o ) {
            final MethodGen THIS = (MethodGen) o;
            return THIS.getSignature().hashCode() ^ THIS.getName().hashCode();
        }
    };


    /**
     * Declare method. If the method is non-static the constructor
     * automatically declares a local variable `$this' in slot 0. The
     * actual code is contained in the `il' parameter, which may further
     * manipulated by the user. But he must take care not to remove any
     * instruction (handles) that are still referenced from this object.
     *
     * For example one may not add a local variable and later remove the
     * instructions it refers to without causing havoc. It is safe
     * however if you remove that local variable, too.
     *
     * @param access_flags access qualifiers
     * @param return_type  method type
     * @param argTypes argument types
     * @param argNames argument names (if this is null, default names will be provided
     * for them)
     * @param method_name name of method
     * @param className class name containing this method (may be null, if you don't care)
     * @param il instruction list associated with this method, may be null only for
     * abstract or native methods
     * @param cp constant pool
     */
    public MethodGen(final int access_flags, final Type return_type, final Type[] argTypes, String[] argNames,
            final String method_name, final String className, final InstructionList il, final ConstantPoolGen cp) {
        super(access_flags);
        setType(return_type);
        setArgumentTypes(argTypes);
        setArgumentNames(argNames);
        setName(method_name);
        setClassName(className);
        setInstructionList(il);
        setConstantPool(cp);
        final boolean abstract_ = isAbstract() || isNative();
        InstructionHandle start = null;
        final InstructionHandle end = null;
        if (!abstract_) {
            start = il.getStart();
            // end == null => live to end of method
            /* Add local variables, namely the implicit `this' and the arguments
             */
            if (!isStatic() && (className != null)) { // Instance method -> `this' is local var 0
                addLocalVariable("this",  ObjectType.getInstance(className), start, end);
            }
        }
        if (argTypes != null) {
            final int size = argTypes.length;
            for (final Type arg_type : argTypes) {
                if (Type.VOID == arg_type) {
                    throw new ClassGenException("'void' is an illegal argument type for a method");
                }
            }
            if (argNames != null) { // Names for variables provided?
                if (size != argNames.length) {
                    throw new ClassGenException("Mismatch in argument array lengths: " + size
                            + " vs. " + argNames.length);
                }
            } else { // Give them dummy names
                argNames = new String[size];
                for (int i = 0; i < size; i++) {
                    argNames[i] = "arg" + i;
                }
                setArgumentNames(argNames);
            }
            if (!abstract_) {
                for (int i = 0; i < size; i++) {
                    addLocalVariable(argNames[i], argTypes[i], start, end);
                }
            }
        }
    }


    /**
     * Instantiate from existing method.
     *
     * @param method method
     * @param className class name containing this method
     * @param cp constant pool
     */
    public MethodGen(final Method method, final String className, final ConstantPoolGen cp) {
        this(method.getAccessFlags(), Type.getReturnType(method.getSignature()),
            Type.getArgumentTypes(method.getSignature()), null /* may be overridden anyway */
            , method.getName(), className,
            ((method.getAccessFlags() & (Const.ACC_ABSTRACT | Const.ACC_NATIVE)) == 0)
                ? new InstructionList(getByteCodes(method))
                : null,
            cp);
        final Attribute[] attributes = method.getAttributes();
        for (final Attribute attribute : attributes) {
            Attribute a = attribute;
            if (a instanceof Code) {
                final Code c = (Code) a;
                setMaxStack(c.getMaxStack());
                setMaxLocals(c.getMaxLocals());
                final CodeException[] ces = c.getExceptionTable();
                if (ces != null) {
                    for (final CodeException ce : ces) {
                        final int type = ce.getCatchType();
                        ObjectType c_type = null;
                        if (type > 0) {
                            final String cen = method.getConstantPool().getConstantString(type,
                                    Const.CONSTANT_Class);
                            c_type =  ObjectType.getInstance(cen);
                        }
                        final int end_pc = ce.getEndPC();
                        final int length = getByteCodes(method).length;
                        InstructionHandle end;
                        if (length == end_pc) { // May happen, because end_pc is exclusive
                            end = il.getEnd();
                        } else {
                            end = il.findHandle(end_pc);
                            end = end.getPrev(); // Make it inclusive
                        }
                        addExceptionHandler(il.findHandle(ce.getStartPC()), end,
                                il.findHandle(ce.getHandlerPC()), c_type);
                    }
                }
                final Attribute[] c_attributes = c.getAttributes();
                for (final Attribute c_attribute : c_attributes) {
                    a = c_attribute;
                    if (a instanceof LineNumberTable) {
                        final LineNumber[] ln = ((LineNumberTable) a).getLineNumberTable();
                        for (final LineNumber l : ln) {
                            final InstructionHandle ih = il.findHandle(l.getStartPC());
                            if (ih != null) {
                                addLineNumber(ih, l.getLineNumber());
                            }
                        }
                    } else if (a instanceof LocalVariableTable) {
                        updateLocalVariableTable((LocalVariableTable) a);
                    } else if (a instanceof LocalVariableTypeTable) {
                        this.localVariableTypeTable = (LocalVariableTypeTable) a.copy(cp.getConstantPool());
                    } else {
                        addCodeAttribute(a);
                    }
                }
            } else if (a instanceof ExceptionTable) {
                final String[] names = ((ExceptionTable) a).getExceptionNames();
                for (final String name2 : names) {
                    addException(name2);
                }
            } else if (a instanceof Annotations) {
                final Annotations runtimeAnnotations = (Annotations) a;
                final AnnotationEntry[] aes = runtimeAnnotations.getAnnotationEntries();
                for (final AnnotationEntry element : aes) {
                    addAnnotationEntry(new AnnotationEntryGen(element, cp, false));
                }
            } else {
                addAttribute(a);
            }
        }
    }


    private static byte[] getByteCodes(final Method method) {
        final Code code = method.getCode();
        if (code == null) {
            throw new IllegalStateException(String.format("The method '%s' has no code.", method));
        }
        return code.getCode();
    }

    /**
     * Adds a local variable to this method.
     *
     * @param name variable name
     * @param type variable type
     * @param slot the index of the local variable, if type is long or double, the next available
     * index is slot+2
     * @param start from where the variable is valid
     * @param end until where the variable is valid
     * @param orig_index the index of the local variable prior to any modifications
     * @return new local variable object
     * @see LocalVariable
     */
    public LocalVariableGen addLocalVariable( final String name, final Type type, final int slot,
            final InstructionHandle start, final InstructionHandle end, final int orig_index ) {
        final byte t = type.getType();
        if (t != Const.T_ADDRESS) {
            final int add = type.getSize();
            if (slot + add > maxLocals) {
                maxLocals = slot + add;
            }
            final LocalVariableGen l = new LocalVariableGen(slot, name, type, start, end, orig_index);
            int i;
            if ((i = variableList.indexOf(l)) >= 0) {
                variableList.set(i, l);
            } else {
                variableList.add(l);
            }
            return l;
        }
        throw new IllegalArgumentException("Can not use " + type
                + " as type for local variable");
    }


    /**
     * Adds a local variable to this method.
     *
     * @param name variable name
     * @param type variable type
     * @param slot the index of the local variable, if type is long or double, the next available
     * index is slot+2
     * @param start from where the variable is valid
     * @param end until where the variable is valid
     * @return new local variable object
     * @see LocalVariable
     */
    public LocalVariableGen addLocalVariable( final String name, final Type type, final int slot,
            final InstructionHandle start, final InstructionHandle end ) {
        return addLocalVariable(name, type, slot, start, end, slot);
    }

    /**
     * Adds a local variable to this method and assigns an index automatically.
     *
     * @param name variable name
     * @param type variable type
     * @param start from where the variable is valid, if this is null,
     * it is valid from the start
     * @param end until where the variable is valid, if this is null,
     * it is valid to the end
     * @return new local variable object
     * @see LocalVariable
     */
    public LocalVariableGen addLocalVariable( final String name, final Type type, final InstructionHandle start,
            final InstructionHandle end ) {
        return addLocalVariable(name, type, maxLocals, start, end);
    }


    /**
     * Remove a local variable, its slot will not be reused, if you do not use
     * addLocalVariable with an explicit index argument.
     */
    public void removeLocalVariable(final LocalVariableGen l) {
        variableList.remove(l);
    }


    /**
     * Remove all local variables.
     */
    public void removeLocalVariables() {
        variableList.clear();
    }


    /*
     * If the range of the variable has not been set yet, it will be set to be valid from
     * the start to the end of the instruction list.
     *
     * @return array of declared local variables sorted by index
     */
    public LocalVariableGen[] getLocalVariables() {
        final int size = variableList.size();
        final LocalVariableGen[] lg = new LocalVariableGen[size];
        variableList.toArray(lg);
        for (int i = 0; i < size; i++) {
            if ((lg[i].getStart() == null) && (il != null)) {
                lg[i].setStart(il.getStart());
            }
            if ((lg[i].getEnd() == null) && (il != null)) {
                lg[i].setEnd(il.getEnd());
            }
        }
        if (size > 1) {
            Arrays.sort(lg, (o1, o2) -> o1.getIndex() - o2.getIndex());
        }
        return lg;
    }


    /**
     * @return `LocalVariableTable' attribute of all the local variables of this method.
     */
    public LocalVariableTable getLocalVariableTable( final ConstantPoolGen cp ) {
        final LocalVariableGen[] lg = getLocalVariables();
        final int size = lg.length;
        final LocalVariable[] lv = new LocalVariable[size];
        for (int i = 0; i < size; i++) {
            lv[i] = lg[i].getLocalVariable(cp);
        }
        return new LocalVariableTable(cp.addUtf8("LocalVariableTable"), 2 + lv.length * 10, lv, cp
                .getConstantPool());
    }

    /**
     * @return `LocalVariableTypeTable' attribute of this method.
     */
    public LocalVariableTypeTable getLocalVariableTypeTable() {
        return localVariableTypeTable;
    }

    /**
     * Give an instruction a line number corresponding to the source code line.
     *
     * @param ih instruction to tag
     * @return new line number object
     * @see LineNumber
     */
    public LineNumberGen addLineNumber( final InstructionHandle ih, final int srcLine ) {
        final LineNumberGen l = new LineNumberGen(ih, srcLine);
        lineNumberList.add(l);
        return l;
    }


    /**
     * Remove a line number.
     */
    public void removeLineNumber( final LineNumberGen l ) {
        lineNumberList.remove(l);
    }


    /**
     * Remove all line numbers.
     */
    public void removeLineNumbers() {
        lineNumberList.clear();
    }


    /*
     * @return array of line numbers
     */
    public LineNumberGen[] getLineNumbers() {
        final LineNumberGen[] lg = new LineNumberGen[lineNumberList.size()];
        lineNumberList.toArray(lg);
        return lg;
    }


    /**
     * @return `LineNumberTable' attribute of all the local variables of this method.
     */
    public LineNumberTable getLineNumberTable( final ConstantPoolGen cp ) {
        final int size = lineNumberList.size();
        final LineNumber[] ln = new LineNumber[size];
        for (int i = 0; i < size; i++) {
            ln[i] = lineNumberList.get(i).getLineNumber();
        }
        return new LineNumberTable(cp.addUtf8("LineNumberTable"), 2 + ln.length * 4, ln, cp
                .getConstantPool());
    }


    /**
     * Add an exception handler, i.e., specify region where a handler is active and an
     * instruction where the actual handling is done.
     *
     * @param start_pc Start of region (inclusive)
     * @param end_pc End of region (inclusive)
     * @param handler_pc Where handling is done
     * @param catch_type class type of handled exception or null if any
     * exception is handled
     * @return new exception handler object
     */
    public CodeExceptionGen addExceptionHandler( final InstructionHandle start_pc,
            final InstructionHandle end_pc, final InstructionHandle handler_pc, final ObjectType catch_type ) {
        if ((start_pc == null) || (end_pc == null) || (handler_pc == null)) {
            throw new ClassGenException("Exception handler target is null instruction");
        }
        final CodeExceptionGen c = new CodeExceptionGen(start_pc, end_pc, handler_pc, catch_type);
        exceptionList.add(c);
        return c;
    }


    /**
     * Remove an exception handler.
     */
    public void removeExceptionHandler( final CodeExceptionGen c ) {
        exceptionList.remove(c);
    }


    /**
     * Remove all line numbers.
     */
    public void removeExceptionHandlers() {
        exceptionList.clear();
    }


    /*
     * @return array of declared exception handlers
     */
    public CodeExceptionGen[] getExceptionHandlers() {
        final CodeExceptionGen[] cg = new CodeExceptionGen[exceptionList.size()];
        exceptionList.toArray(cg);
        return cg;
    }


    /**
     * @return code exceptions for `Code' attribute
     */
    private CodeException[] getCodeExceptions() {
        final int size = exceptionList.size();
        final CodeException[] c_exc = new CodeException[size];
        for (int i = 0; i < size; i++) {
            final CodeExceptionGen c =  exceptionList.get(i);
            c_exc[i] = c.getCodeException(super.getConstantPool());
        }
        return c_exc;
    }


    /**
     * Add an exception possibly thrown by this method.
     *
     * @param className (fully qualified) name of exception
     */
    public void addException( final String className ) {
        throwsList.add(className);
    }


    /**
     * Remove an exception.
     */
    public void removeException( final String c ) {
        throwsList.remove(c);
    }


    /**
     * Remove all exceptions.
     */
    public void removeExceptions() {
        throwsList.clear();
    }


    /*
     * @return array of thrown exceptions
     */
    public String[] getExceptions() {
        return throwsList.toArray(new String[0]);
    }


    /**
     * @return `Exceptions' attribute of all the exceptions thrown by this method.
     */
    private ExceptionTable getExceptionTable( final ConstantPoolGen cp ) {
        final int size = throwsList.size();
        final int[] ex = new int[size];
        for (int i = 0; i < size; i++) {
            ex[i] = cp.addClass(throwsList.get(i));
        }
        return new ExceptionTable(cp.addUtf8("Exceptions"), 2 + 2 * size, ex, cp.getConstantPool());
    }


    /**
     * Add an attribute to the code. Currently, the JVM knows about the
     * LineNumberTable, LocalVariableTable and StackMap attributes,
     * where the former two will be generated automatically and the
     * latter is used for the MIDP only. Other attributes will be
     * ignored by the JVM but do no harm.
     *
     * @param a attribute to be added
     */
    public void addCodeAttribute( final Attribute a ) {
        codeAttrsList.add(a);
    }


    /**
     * Remove the LocalVariableTypeTable
     */
    public void removeLocalVariableTypeTable( ) {
        localVariableTypeTable = null;
    }

    /**
     * Remove a code attribute.
     */
    public void removeCodeAttribute( final Attribute a ) {
        codeAttrsList.remove(a);
    }


    /**
     * Remove all code attributes.
     */
    public void removeCodeAttributes() {
        localVariableTypeTable = null;
        codeAttrsList.clear();
    }


    /**
     * @return all attributes of this method.
     */
    public Attribute[] getCodeAttributes() {
        return codeAttrsList.toArray(new Attribute[0]);
    }

    /**
     * @since 6.0
     */
    public void addAnnotationsAsAttribute(final ConstantPoolGen cp) {
        final Attribute[] attrs = AnnotationEntryGen.getAnnotationAttributes(cp, super.getAnnotationEntries());
        for (final Attribute attr : attrs) {
            addAttribute(attr);
        }
    }

    /**
     * @since 6.0
     */
    public void addParameterAnnotationsAsAttribute(final ConstantPoolGen cp) {
        if (!hasParameterAnnotations) {
            return;
        }
        final Attribute[] attrs = AnnotationEntryGen.getParameterAnnotationAttributes(cp, paramAnnotations);
        if (attrs != null) {
            for (final Attribute attr : attrs) {
                addAttribute(attr);
            }
        }
    }

    private Attribute[] addRuntimeAnnotationsAsAttribute(final ConstantPoolGen cp) {
        final Attribute[] attrs = AnnotationEntryGen.getAnnotationAttributes(cp, super.getAnnotationEntries());
        for (final Attribute attr : attrs) {
            addAttribute(attr);
        }
        return attrs;
    }

    private Attribute[] addRuntimeParameterAnnotationsAsAttribute(final ConstantPoolGen cp) {
        if (!hasParameterAnnotations) {
            return new Attribute[0];
        }
        final Attribute[] attrs = AnnotationEntryGen.getParameterAnnotationAttributes(cp, paramAnnotations);
        for (final Attribute attr : attrs) {
            addAttribute(attr);
        }
        return attrs;
    }

    /**
     * Would prefer to make this private, but need a way to test if client is
     * using BCEL version 6.5.0 or later that contains fix for BCEL-329.
     * @since 6.5.0
     */
    public void removeRuntimeAttributes(final Attribute[] attrs) {
        for (final Attribute attr : attrs) {
            removeAttribute(attr);
        }
    }


    /**
     * Get method object. Never forget to call setMaxStack() or setMaxStack(max), respectively,
     * before calling this method (the same applies for max locals).
     *
     * @return method object
     */
    public Method getMethod() {
        final String signature = getSignature();
        final ConstantPoolGen _cp = super.getConstantPool();
        final int name_index = _cp.addUtf8(super.getName());
        final int signature_index = _cp.addUtf8(signature);
        /* Also updates positions of instructions, i.e., their indices
         */
        byte[] byte_code = null;
        if (il != null) {
            byte_code = il.getByteCode();
        }
        LineNumberTable lnt = null;
        LocalVariableTable lvt = null;
        /* Create LocalVariableTable and LineNumberTable attributes (for debuggers, e.g.)
         */
        if ((variableList.size() > 0) && !stripAttributes) {
            updateLocalVariableTable(getLocalVariableTable(_cp));
            addCodeAttribute(lvt = getLocalVariableTable(_cp));
        }
        if (localVariableTypeTable != null) {
            // LocalVariable length in LocalVariableTypeTable is not updated automatically. It's a difference with LocalVariableTable.
            if (lvt != null) {
                adjustLocalVariableTypeTable(lvt);
            }
            addCodeAttribute(localVariableTypeTable);
        }
        if ((lineNumberList.size() > 0) && !stripAttributes) {
            addCodeAttribute(lnt = getLineNumberTable(_cp));
        }
        final Attribute[] code_attrs = getCodeAttributes();
        /* Each attribute causes 6 additional header bytes
         */
        int attrs_len = 0;
        for (final Attribute code_attr : code_attrs) {
            attrs_len += code_attr.getLength() + 6;
        }
        final CodeException[] c_exc = getCodeExceptions();
        final int exc_len = c_exc.length * 8; // Every entry takes 8 bytes
        Code code = null;
        if ((il != null) && !isAbstract() && !isNative()) {
            // Remove any stale code attribute
            final Attribute[] attributes = getAttributes();
            for (final Attribute a : attributes) {
                if (a instanceof Code) {
                    removeAttribute(a);
                }
            }
            code = new Code(_cp.addUtf8("Code"), 8 + byte_code.length + // prologue byte code
                    2 + exc_len + // exceptions
                    2 + attrs_len, // attributes
                    maxStack, maxLocals, byte_code, c_exc, code_attrs, _cp.getConstantPool());
            addAttribute(code);
        }
        final Attribute[] annotations = addRuntimeAnnotationsAsAttribute(_cp);
        final Attribute[] parameterAnnotations = addRuntimeParameterAnnotationsAsAttribute(_cp);
        ExceptionTable et = null;
        if (throwsList.size() > 0) {
            addAttribute(et = getExceptionTable(_cp));
            // Add `Exceptions' if there are "throws" clauses
        }
        final Method m = new Method(super.getAccessFlags(), name_index, signature_index, getAttributes(), _cp
                .getConstantPool());
        // Undo effects of adding attributes
        if (lvt != null) {
            removeCodeAttribute(lvt);
        }
        if (localVariableTypeTable != null) {
            removeCodeAttribute(localVariableTypeTable);
        }
        if (lnt != null) {
            removeCodeAttribute(lnt);
        }
        if (code != null) {
            removeAttribute(code);
        }
        if (et != null) {
            removeAttribute(et);
        }
        removeRuntimeAttributes(annotations);
        removeRuntimeAttributes(parameterAnnotations);
        return m;
    }

    private void updateLocalVariableTable(final LocalVariableTable a) {
        final LocalVariable[] lv = a.getLocalVariableTable();
        removeLocalVariables();
        for (final LocalVariable l : lv) {
            InstructionHandle start = il.findHandle(l.getStartPC());
            final InstructionHandle end = il.findHandle(l.getStartPC() + l.getLength());
            // Repair malformed handles
            if (null == start) {
                start = il.getStart();
            }
            // end == null => live to end of method
            // Since we are recreating the LocalVaraible, we must
            // propagate the orig_index to new copy.
            addLocalVariable(l.getName(), Type.getType(l.getSignature()), l
                    .getIndex(), start, end, l.getOrigIndex());
        }
    }

    private void adjustLocalVariableTypeTable(final LocalVariableTable lvt) {
        final LocalVariable[] lv = lvt.getLocalVariableTable();
        final LocalVariable[] lvg = localVariableTypeTable.getLocalVariableTypeTable();

        for (final LocalVariable element : lvg) {
            for (final LocalVariable l : lv) {
                if (element.getName().equals(l.getName()) && element.getIndex() == l.getOrigIndex()) {
                    element.setLength(l.getLength());
                    element.setStartPC(l.getStartPC());
                    element.setIndex(l.getIndex());
                    break;
                }
            }
        }
    }


    /**
     * Remove all NOPs from the instruction list (if possible) and update every
     * object referring to them, i.e., branch instructions, local variables and
     * exception handlers.
     */
    public void removeNOPs() {
        if (il != null) {
            InstructionHandle next;
            /* Check branch instructions.
             */
            for (InstructionHandle ih = il.getStart(); ih != null; ih = next) {
                next = ih.getNext();
                if ((next != null) && (ih.getInstruction() instanceof NOP)) {
                    try {
                        il.delete(ih);
                    } catch (final TargetLostException e) {
                        for (final InstructionHandle target : e.getTargets()) {
                            for (final InstructionTargeter targeter : target.getTargeters()) {
                                targeter.updateTarget(target, next);
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Set maximum number of local variables.
     */
    public void setMaxLocals( final int m ) {
        maxLocals = m;
    }


    public int getMaxLocals() {
        return maxLocals;
    }


    /**
     * Set maximum stack size for this method.
     */
    public void setMaxStack( final int m ) { // TODO could be package-protected?
        maxStack = m;
    }


    public int getMaxStack() {
        return maxStack;
    }


    /** @return class that contains this method
     */
    public String getClassName() {
        return className;
    }


    public void setClassName( final String class_name ) { // TODO could be package-protected?
        this.className = class_name;
    }


    public void setReturnType( final Type return_type ) {
        setType(return_type);
    }


    public Type getReturnType() {
        return getType();
    }


    public void setArgumentTypes( final Type[] arg_types ) {
        this.argTypes = arg_types;
    }


    public Type[] getArgumentTypes() {
        return argTypes.clone();
    }


    public void setArgumentType( final int i, final Type type ) {
        argTypes[i] = type;
    }


    public Type getArgumentType( final int i ) {
        return argTypes[i];
    }


    public void setArgumentNames( final String[] arg_names ) {
        this.argNames = arg_names;
    }


    public String[] getArgumentNames() {
        return argNames.clone();
    }


    public void setArgumentName( final int i, final String name ) {
        argNames[i] = name;
    }


    public String getArgumentName( final int i ) {
        return argNames[i];
    }


    public InstructionList getInstructionList() {
        return il;
    }


    public void setInstructionList( final InstructionList il ) { // TODO could be package-protected?
        this.il = il;
    }


    @Override
    public String getSignature() {
        return Type.getMethodSignature(super.getType(), argTypes);
    }


    /**
     * Computes max. stack size by performing control flow analysis.
     */
    public void setMaxStack() { // TODO could be package-protected? (some tests would need repackaging)
        if (il != null) {
            maxStack = getMaxStack(super.getConstantPool(), il, getExceptionHandlers());
        } else {
            maxStack = 0;
        }
    }


    /**
     * Compute maximum number of local variables.
     */
    public void setMaxLocals() { // TODO could be package-protected? (some tests would need repackaging)
        if (il != null) {
            int max = isStatic() ? 0 : 1;
            if (argTypes != null) {
                for (final Type arg_type : argTypes) {
                    max += arg_type.getSize();
                }
            }
            for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
                final Instruction ins = ih.getInstruction();
                if ((ins instanceof LocalVariableInstruction) || (ins instanceof RET)
                        || (ins instanceof IINC)) {
                    final int index = ((IndexedInstruction) ins).getIndex()
                            + ((TypedInstruction) ins).getType(super.getConstantPool()).getSize();
                    if (index > max) {
                        max = index;
                    }
                }
            }
            maxLocals = max;
        } else {
            maxLocals = 0;
        }
    }


    /** Do not/Do produce attributes code attributesLineNumberTable and
     * LocalVariableTable, like javac -O
     */
    public void stripAttributes( final boolean flag ) {
        stripAttributes = flag;
    }

    static final class BranchTarget {

        final InstructionHandle target;
        final int stackDepth;


        BranchTarget(final InstructionHandle target, final int stackDepth) {
            this.target = target;
            this.stackDepth = stackDepth;
        }
    }

    static final class BranchStack {

        private final Stack<BranchTarget> branchTargets = new Stack<>();
        private final Map<InstructionHandle, BranchTarget> visitedTargets = new HashMap<>();


        public void push( final InstructionHandle target, final int stackDepth ) {
            if (visited(target)) {
                return;
            }
            branchTargets.push(visit(target, stackDepth));
        }


        public BranchTarget pop() {
            if (!branchTargets.empty()) {
                final BranchTarget bt = branchTargets.pop();
                return bt;
            }
            return null;
        }


        private BranchTarget visit( final InstructionHandle target, final int stackDepth ) {
            final BranchTarget bt = new BranchTarget(target, stackDepth);
            visitedTargets.put(target, bt);
            return bt;
        }


        private boolean visited( final InstructionHandle target ) {
            return visitedTargets.get(target) != null;
        }
    }


    /**
     * Computes stack usage of an instruction list by performing control flow analysis.
     *
     * @return maximum stack depth used by method
     */
    public static int getMaxStack( final ConstantPoolGen cp, final InstructionList il, final CodeExceptionGen[] et ) {
        final BranchStack branchTargets = new BranchStack();
        /* Initially, populate the branch stack with the exception
         * handlers, because these aren't (necessarily) branched to
         * explicitly. in each case, the stack will have depth 1,
         * containing the exception object.
         */
        for (final CodeExceptionGen element : et) {
            final InstructionHandle handler_pc = element.getHandlerPC();
            if (handler_pc != null) {
                branchTargets.push(handler_pc, 1);
            }
        }
        int stackDepth = 0;
        int maxStackDepth = 0;
        InstructionHandle ih = il.getStart();
        while (ih != null) {
            final Instruction instruction = ih.getInstruction();
            final short opcode = instruction.getOpcode();
            final int delta = instruction.produceStack(cp) - instruction.consumeStack(cp);
            stackDepth += delta;
            if (stackDepth > maxStackDepth) {
                maxStackDepth = stackDepth;
            }
            // choose the next instruction based on whether current is a branch.
            if (instruction instanceof BranchInstruction) {
                final BranchInstruction branch = (BranchInstruction) instruction;
                if (instruction instanceof Select) {
                    // explore all of the select's targets. the default target is handled below.
                    final Select select = (Select) branch;
                    final InstructionHandle[] targets = select.getTargets();
                    for (final InstructionHandle target : targets) {
                        branchTargets.push(target, stackDepth);
                    }
                    // nothing to fall through to.
                    ih = null;
                } else if (!(branch instanceof IfInstruction)) {
                    // if an instruction that comes back to following PC,
                    // push next instruction, with stack depth reduced by 1.
                    if (opcode == Const.JSR || opcode == Const.JSR_W) {
                        branchTargets.push(ih.getNext(), stackDepth - 1);
                    }
                    ih = null;
                }
                // for all branches, the target of the branch is pushed on the branch stack.
                // conditional branches have a fall through case, selects don't, and
                // jsr/jsr_w return to the next instruction.
                branchTargets.push(branch.getTarget(), stackDepth);
            } else {
                // check for instructions that terminate the method.
                if (opcode == Const.ATHROW || opcode == Const.RET
                        || (opcode >= Const.IRETURN && opcode <= Const.RETURN)) {
                    ih = null;
                }
            }
            // normal case, go to the next instruction.
            if (ih != null) {
                ih = ih.getNext();
            }
            // if we have no more instructions, see if there are any deferred branches to explore.
            if (ih == null) {
                final BranchTarget bt = branchTargets.pop();
                if (bt != null) {
                    ih = bt.target;
                    stackDepth = bt.stackDepth;
                }
            }
        }
        return maxStackDepth;
    }

    private List<MethodObserver> observers;


    /** Add observer for this object.
     */
    public void addObserver( final MethodObserver o ) {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        observers.add(o);
    }


    /** Remove observer for this object.
     */
    public void removeObserver( final MethodObserver o ) {
        if (observers != null) {
            observers.remove(o);
        }
    }


    /** Call notify() method on all observers. This method is not called
     * automatically whenever the state has changed, but has to be
     * called by the user after he has finished editing the object.
     */
    public void update() {
        if (observers != null) {
            for (final MethodObserver observer : observers) {
                observer.notify(this);
            }
        }
    }


    /**
     * Return string representation close to declaration format,
     * `public static void main(String[]) throws IOException', e.g.
     *
     * @return String representation of the method.
     */
    @Override
    public final String toString() {
        final String access = Utility.accessToString(super.getAccessFlags());
        String signature = Type.getMethodSignature(super.getType(), argTypes);
        signature = Utility.methodSignatureToString(signature, super.getName(), access, true,
                getLocalVariableTable(super.getConstantPool()));
        final StringBuilder buf = new StringBuilder(signature);
        for (final Attribute a : getAttributes()) {
            if (!((a instanceof Code) || (a instanceof ExceptionTable))) {
                buf.append(" [").append(a).append("]");
            }
        }

        if (throwsList.size() > 0) {
            for (final String throwsDescriptor : throwsList) {
                buf.append("\n\t\tthrows ").append(throwsDescriptor);
            }
        }
        return buf.toString();
    }


    /** @return deep copy of this method
     */
    public MethodGen copy( final String className, final ConstantPoolGen cp ) {
        final Method m = ((MethodGen) clone()).getMethod();
        final MethodGen mg = new MethodGen(m, className, super.getConstantPool());
        if (super.getConstantPool() != cp) {
            mg.setConstantPool(cp);
            mg.getInstructionList().replaceConstantPool(super.getConstantPool(), cp);
        }
        return mg;
    }

    //J5TODO: Should paramAnnotations be an array of arrays? Rather than an array of lists, this
    // is more likely to suggest to the caller it is readonly (which a List does not).
    /**
     * Return a list of AnnotationGen objects representing parameter annotations
     * @since 6.0
     */
    public List<AnnotationEntryGen> getAnnotationsOnParameter(final int i) {
        ensureExistingParameterAnnotationsUnpacked();
        if (!hasParameterAnnotations || i > argTypes.length) {
            return null;
        }
        return paramAnnotations[i];
    }

    /**
     * Goes through the attributes on the method and identifies any that are
     * RuntimeParameterAnnotations, extracting their contents and storing them
     * as parameter annotations. There are two kinds of parameter annotation -
     * visible and invisible. Once they have been unpacked, these attributes are
     * deleted. (The annotations will be rebuilt as attributes when someone
     * builds a Method object out of this MethodGen object).
     */
    private void ensureExistingParameterAnnotationsUnpacked()
    {
        if (haveUnpackedParameterAnnotations) {
            return;
        }
        // Find attributes that contain parameter annotation data
        final Attribute[] attrs = getAttributes();
        ParameterAnnotations paramAnnVisAttr = null;
        ParameterAnnotations paramAnnInvisAttr = null;
        for (final Attribute attribute : attrs) {
            if (attribute instanceof ParameterAnnotations)
            {
                // Initialize paramAnnotations
                if (!hasParameterAnnotations)
                {
                    @SuppressWarnings({"rawtypes", "unchecked"}) // OK
                    final List<AnnotationEntryGen>[] parmList = new List[argTypes.length];
                    paramAnnotations = parmList;
                    for (int j = 0; j < argTypes.length; j++) {
                        paramAnnotations[j] = new ArrayList<>();
                    }
                }
                hasParameterAnnotations = true;
                final ParameterAnnotations rpa = (ParameterAnnotations) attribute;
                if (rpa instanceof RuntimeVisibleParameterAnnotations) {
                    paramAnnVisAttr = rpa;
                } else {
                    paramAnnInvisAttr = rpa;
                }
                final ParameterAnnotationEntry[] parameterAnnotationEntries = rpa.getParameterAnnotationEntries();
                for (int j = 0; j < parameterAnnotationEntries.length; j++)
                {
                    // This returns Annotation[] ...
                    final ParameterAnnotationEntry immutableArray = rpa.getParameterAnnotationEntries()[j];
                    // ... which needs transforming into an AnnotationGen[] ...
                    final List<AnnotationEntryGen> mutable = makeMutableVersion(immutableArray.getAnnotationEntries());
                    // ... then add these to any we already know about
                    paramAnnotations[j].addAll(mutable);
                }
            }
        }
        if (paramAnnVisAttr != null) {
            removeAttribute(paramAnnVisAttr);
        }
        if (paramAnnInvisAttr != null) {
            removeAttribute(paramAnnInvisAttr);
        }
        haveUnpackedParameterAnnotations = true;
    }

    private List<AnnotationEntryGen> makeMutableVersion(final AnnotationEntry[] mutableArray)
    {
        final List<AnnotationEntryGen> result = new ArrayList<>();
        for (final AnnotationEntry element : mutableArray) {
            result.add(new AnnotationEntryGen(element, getConstantPool(),
                    false));
        }
        return result;
    }

    public void addParameterAnnotation(final int parameterIndex,
            final AnnotationEntryGen annotation)
    {
        ensureExistingParameterAnnotationsUnpacked();
        if (!hasParameterAnnotations)
        {
            @SuppressWarnings({"rawtypes", "unchecked"})
            final List<AnnotationEntryGen>[] parmList = new List[argTypes.length];
            paramAnnotations = parmList;
            hasParameterAnnotations = true;
        }
        final List<AnnotationEntryGen> existingAnnotations = paramAnnotations[parameterIndex];
        if (existingAnnotations != null)
        {
            existingAnnotations.add(annotation);
        }
        else
        {
            final List<AnnotationEntryGen> l = new ArrayList<>();
            l.add(annotation);
            paramAnnotations[parameterIndex] = l;
        }
    }

    /**
     * @return Comparison strategy object
     */
    public static BCELComparator getComparator() {
        return bcelComparator;
    }


    /**
     * @param comparator Comparison strategy object
     */
    public static void setComparator( final BCELComparator comparator ) {
        bcelComparator = comparator;
    }


    /**
     * Return value as defined by given BCELComparator strategy.
     * By default two MethodGen objects are said to be equal when
     * their names and signatures are equal.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object obj ) {
        return bcelComparator.equals(this, obj);
    }


    /**
     * Return value as defined by given BCELComparator strategy.
     * By default return the hashcode of the method's name XOR signature.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return bcelComparator.hashCode(this);
    }
}
