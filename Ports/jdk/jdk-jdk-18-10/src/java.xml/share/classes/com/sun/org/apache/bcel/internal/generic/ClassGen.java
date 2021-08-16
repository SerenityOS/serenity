/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.classfile.AccessFlags;
import com.sun.org.apache.bcel.internal.classfile.AnnotationEntry;
import com.sun.org.apache.bcel.internal.classfile.Annotations;
import com.sun.org.apache.bcel.internal.classfile.Attribute;
import com.sun.org.apache.bcel.internal.classfile.ConstantPool;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.RuntimeInvisibleAnnotations;
import com.sun.org.apache.bcel.internal.classfile.RuntimeVisibleAnnotations;
import com.sun.org.apache.bcel.internal.classfile.SourceFile;
import com.sun.org.apache.bcel.internal.util.BCELComparator;

/**
 * Template class for building up a java class. May be initialized with an
 * existing java class (file).
 *
 * @see JavaClass
 * @LastModified: May 2021
 */
public class ClassGen extends AccessFlags implements Cloneable {

    /* Corresponds to the fields found in a JavaClass object.
     */
    private String className;
    private String superClassName;
    private final String fileName;
    private int classNameIndex = -1;
    private int superclass_name_index = -1;
    private int major = Const.MAJOR_1_1;
    private int minor = Const.MINOR_1_1;
    private ConstantPoolGen cp; // Template for building up constant pool
    // ArrayLists instead of arrays to gather fields, methods, etc.
    private final List<Field> fieldList = new ArrayList<>();
    private final List<Method> methodList = new ArrayList<>();
    private final List<Attribute> attributeList = new ArrayList<>();
    private final List<String> interfaceList = new ArrayList<>();
    private final List<AnnotationEntryGen> annotationList = new ArrayList<>();

    private static BCELComparator bcelComparator = new BCELComparator() {

        @Override
        public boolean equals( final Object o1, final Object o2 ) {
            final ClassGen THIS = (ClassGen) o1;
            final ClassGen THAT = (ClassGen) o2;
            return Objects.equals(THIS.getClassName(), THAT.getClassName());
        }


        @Override
        public int hashCode( final Object o ) {
            final ClassGen THIS = (ClassGen) o;
            return THIS.getClassName().hashCode();
        }
    };


    /** Convenience constructor to set up some important values initially.
     *
     * @param className fully qualified class name
     * @param superClassName fully qualified superclass name
     * @param fileName source file name
     * @param accessFlags access qualifiers
     * @param interfaces implemented interfaces
     * @param cp constant pool to use
     */
    public ClassGen(final String className, final String superClassName, final String fileName, final int accessFlags,
            final String[] interfaces, final ConstantPoolGen cp) {
        super(accessFlags);
        this.className = className;
        this.superClassName = superClassName;
        this.fileName = fileName;
        this.cp = cp;
        // Put everything needed by default into the constant pool and the vectors
        if (fileName != null) {
            addAttribute(new SourceFile(cp.addUtf8("SourceFile"), 2, cp.addUtf8(fileName), cp
                    .getConstantPool()));
        }
        classNameIndex = cp.addClass(className);
        superclass_name_index = cp.addClass(superClassName);
        if (interfaces != null) {
            for (final String interface1 : interfaces) {
                addInterface(interface1);
            }
        }
    }


    /** Convenience constructor to set up some important values initially.
     *
     * @param className fully qualified class name
     * @param superClassName fully qualified superclass name
     * @param fileName source file name
     * @param accessFlags access qualifiers
     * @param interfaces implemented interfaces
     */
    public ClassGen(final String className, final String superClassName, final String fileName, final int accessFlags,
            final String[] interfaces) {
        this(className, superClassName, fileName, accessFlags, interfaces,
                new ConstantPoolGen());
    }


    /**
     * Initialize with existing class.
     * @param clazz JavaClass object (e.g. read from file)
     */
    public ClassGen(final JavaClass clazz) {
        super(clazz.getAccessFlags());
        classNameIndex = clazz.getClassNameIndex();
        superclass_name_index = clazz.getSuperclassNameIndex();
        className = clazz.getClassName();
        superClassName = clazz.getSuperclassName();
        fileName = clazz.getSourceFileName();
        cp = new ConstantPoolGen(clazz.getConstantPool());
        major = clazz.getMajor();
        minor = clazz.getMinor();
        final Attribute[] attributes = clazz.getAttributes();
        // J5TODO: Could make unpacking lazy, done on first reference
        final AnnotationEntryGen[] annotations = unpackAnnotations(attributes);
        final Method[] methods = clazz.getMethods();
        final Field[] fields = clazz.getFields();
        final String[] interfaces = clazz.getInterfaceNames();
        for (final String interface1 : interfaces) {
            addInterface(interface1);
        }
        for (final Attribute attribute : attributes) {
            if (!(attribute instanceof Annotations)) {
                addAttribute(attribute);
            }
        }
        for (final AnnotationEntryGen annotation : annotations) {
            addAnnotationEntry(annotation);
        }
        for (final Method method : methods) {
            addMethod(method);
        }
        for (final Field field : fields) {
            addField(field);
        }
    }

    /**
     * Look for attributes representing annotations and unpack them.
     */
    private AnnotationEntryGen[] unpackAnnotations(final Attribute[] attrs)
    {
        final List<AnnotationEntryGen> annotationGenObjs = new ArrayList<>();
        for (final Attribute attr : attrs) {
            if (attr instanceof RuntimeVisibleAnnotations)
            {
                final RuntimeVisibleAnnotations rva = (RuntimeVisibleAnnotations) attr;
                final AnnotationEntry[] annos = rva.getAnnotationEntries();
                for (final AnnotationEntry a : annos) {
                    annotationGenObjs.add(new AnnotationEntryGen(a,
                            getConstantPool(), false));
                }
            }
            else
                if (attr instanceof RuntimeInvisibleAnnotations)
                {
                    final RuntimeInvisibleAnnotations ria = (RuntimeInvisibleAnnotations) attr;
                    final AnnotationEntry[] annos = ria.getAnnotationEntries();
                    for (final AnnotationEntry a : annos) {
                        annotationGenObjs.add(new AnnotationEntryGen(a,
                                getConstantPool(), false));
                    }
                }
        }
        return annotationGenObjs.toArray(new AnnotationEntryGen[annotationGenObjs.size()]);
    }


    /**
     * @return the (finally) built up Java class object.
     */
    public JavaClass getJavaClass() {
        final int[] interfaces = getInterfaces();
        final Field[] fields = getFields();
        final Method[] methods = getMethods();
        Attribute[] attributes = null;
        if (annotationList.isEmpty()) {
            attributes = getAttributes();
        } else {
            // TODO: Sometime later, trash any attributes called 'RuntimeVisibleAnnotations' or 'RuntimeInvisibleAnnotations'
            final Attribute[] annAttributes  = AnnotationEntryGen.getAnnotationAttributes(cp, getAnnotationEntries());
            attributes = new Attribute[attributeList.size()+annAttributes.length];
            attributeList.toArray(attributes);
            System.arraycopy(annAttributes,0,attributes,attributeList.size(),annAttributes.length);
        }
        // Must be last since the above calls may still add something to it
        final ConstantPool _cp = this.cp.getFinalConstantPool();
        return new JavaClass(classNameIndex, superclass_name_index, fileName, major, minor,
                super.getAccessFlags(), _cp, interfaces, fields, methods, attributes);
    }


    /**
     * Add an interface to this class, i.e., this class has to implement it.
     * @param name interface to implement (fully qualified class name)
     */
    public void addInterface( final String name ) {
        interfaceList.add(name);
    }


    /**
     * Remove an interface from this class.
     * @param name interface to remove (fully qualified name)
     */
    public void removeInterface( final String name ) {
        interfaceList.remove(name);
    }


    /**
     * @return major version number of class file
     */
    public int getMajor() {
        return major;
    }


    /** Set major version number of class file, default value is 45 (JDK 1.1)
     * @param major major version number
     */
    public void setMajor( final int major ) { // TODO could be package-protected - only called by test code
        this.major = major;
    }


    /** Set minor version number of class file, default value is 3 (JDK 1.1)
     * @param minor minor version number
     */
    public void setMinor( final int minor ) {  // TODO could be package-protected - only called by test code
        this.minor = minor;
    }

    /**
     * @return minor version number of class file
     */
    public int getMinor() {
        return minor;
    }


    /**
     * Add an attribute to this class.
     * @param a attribute to add
     */
    public void addAttribute( final Attribute a ) {
        attributeList.add(a);
    }

    public void addAnnotationEntry(final AnnotationEntryGen a) {
        annotationList.add(a);
    }


    /**
     * Add a method to this class.
     * @param m method to add
     */
    public void addMethod( final Method m ) {
        methodList.add(m);
    }


    /**
     * Convenience method.
     *
     * Add an empty constructor to this class that does nothing but calling super().
     * @param access_flags rights for constructor
     */
    public void addEmptyConstructor( final int access_flags ) {
        final InstructionList il = new InstructionList();
        il.append(InstructionConst.THIS); // Push `this'
        il.append(new INVOKESPECIAL(cp.addMethodref(superClassName, "<init>", "()V")));
        il.append(InstructionConst.RETURN);
        final MethodGen mg = new MethodGen(access_flags, Type.VOID, Type.NO_ARGS, null, "<init>",
                className, il, cp);
        mg.setMaxStack(1);
        addMethod(mg.getMethod());
    }


    /**
     * Add a field to this class.
     * @param f field to add
     */
    public void addField( final Field f ) {
        fieldList.add(f);
    }


    public boolean containsField( final Field f ) {
        return fieldList.contains(f);
    }


    /** @return field object with given name, or null
     */
    public Field containsField( final String name ) {
        for (final Field f : fieldList) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }


    /** @return method object with given name and signature, or null
     */
    public Method containsMethod( final String name, final String signature ) {
        for (final Method m : methodList) {
            if (m.getName().equals(name) && m.getSignature().equals(signature)) {
                return m;
            }
        }
        return null;
    }


    /**
     * Remove an attribute from this class.
     * @param a attribute to remove
     */
    public void removeAttribute( final Attribute a ) {
        attributeList.remove(a);
    }


    /**
     * Remove a method from this class.
     * @param m method to remove
     */
    public void removeMethod( final Method m ) {
        methodList.remove(m);
    }


    /** Replace given method with new one. If the old one does not exist
     * add the new_ method to the class anyway.
     */
    public void replaceMethod( final Method old, final Method new_ ) {
        if (new_ == null) {
            throw new ClassGenException("Replacement method must not be null");
        }
        final int i = methodList.indexOf(old);
        if (i < 0) {
            methodList.add(new_);
        } else {
            methodList.set(i, new_);
        }
    }


    /** Replace given field with new one. If the old one does not exist
     * add the new_ field to the class anyway.
     */
    public void replaceField( final Field old, final Field new_ ) {
        if (new_ == null) {
            throw new ClassGenException("Replacement method must not be null");
        }
        final int i = fieldList.indexOf(old);
        if (i < 0) {
            fieldList.add(new_);
        } else {
            fieldList.set(i, new_);
        }
    }


    /**
     * Remove a field to this class.
     * @param f field to remove
     */
    public void removeField( final Field f ) {
        fieldList.remove(f);
    }


    public String getClassName() {
        return className;
    }


    public String getSuperclassName() {
        return superClassName;
    }


    public String getFileName() {
        return fileName;
    }


    public void setClassName( final String name ) {
        className = name.replace('/', '.');
        classNameIndex = cp.addClass(name);
    }


    public void setSuperclassName( final String name ) {
        superClassName = name.replace('/', '.');
        superclass_name_index = cp.addClass(name);
    }


    public Method[] getMethods() {
        return methodList.toArray(new Method[methodList.size()]);
    }


    public void setMethods( final Method[] methods ) {
        methodList.clear();
        for (final Method method : methods) {
            addMethod(method);
        }
    }


    public void setMethodAt( final Method method, final int pos ) {
        methodList.set(pos, method);
    }


    public Method getMethodAt( final int pos ) {
        return methodList.get(pos);
    }


    public String[] getInterfaceNames() {
        final int size = interfaceList.size();
        final String[] interfaces = new String[size];
        interfaceList.toArray(interfaces);
        return interfaces;
    }


    public int[] getInterfaces() {
        final int size = interfaceList.size();
        final int[] interfaces = new int[size];
        for (int i = 0; i < size; i++) {
            interfaces[i] = cp.addClass(interfaceList.get(i));
        }
        return interfaces;
    }


    public Field[] getFields() {
        return fieldList.toArray(new Field[fieldList.size()]);
    }


    public Attribute[] getAttributes() {
        return attributeList.toArray(new Attribute[attributeList.size()]);
    }

    //  J5TODO: Should we make calling unpackAnnotations() lazy and put it in here?
    public AnnotationEntryGen[] getAnnotationEntries() {
        return annotationList.toArray(new AnnotationEntryGen[annotationList.size()]);
    }


    public ConstantPoolGen getConstantPool() {
        return cp;
    }


    public void setConstantPool( final ConstantPoolGen constant_pool ) {
        cp = constant_pool;
    }


    public void setClassNameIndex( final int class_name_index ) {
        this.classNameIndex = class_name_index;
        className = cp.getConstantPool().getConstantString(class_name_index,
                Const.CONSTANT_Class).replace('/', '.');
    }


    public void setSuperclassNameIndex( final int superclass_name_index ) {
        this.superclass_name_index = superclass_name_index;
        superClassName = cp.getConstantPool().getConstantString(superclass_name_index,
                Const.CONSTANT_Class).replace('/', '.');
    }


    public int getSuperclassNameIndex() {
        return superclass_name_index;
    }


    public int getClassNameIndex() {
        return classNameIndex;
    }

    private List<ClassObserver> observers;


    /** Add observer for this object.
     */
    public void addObserver( final ClassObserver o ) {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        observers.add(o);
    }


    /** Remove observer for this object.
     */
    public void removeObserver( final ClassObserver o ) {
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
            for (final ClassObserver observer : observers) {
                observer.notify(this);
            }
        }
    }


    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new Error("Clone Not Supported"); // never happens
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
     * By default two ClassGen objects are said to be equal when
     * their class names are equal.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object obj ) {
        return bcelComparator.equals(this, obj);
    }


    /**
     * Return value as defined by given BCELComparator strategy.
     * By default return the hashcode of the class name.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return bcelComparator.hashCode(this);
    }
}
