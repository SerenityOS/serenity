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
package com.sun.org.apache.bcel.internal.classfile;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.generic.Type;
import com.sun.org.apache.bcel.internal.util.BCELComparator;
import com.sun.org.apache.bcel.internal.util.ClassQueue;
import com.sun.org.apache.bcel.internal.util.SyntheticRepository;

/**
 * Represents a Java class, i.e., the data structures, constant pool,
 * fields, methods and commands contained in a Java .class file.
 * See <a href="https://docs.oracle.com/javase/specs/">JVM specification</a> for details.
 * The intent of this class is to represent a parsed or otherwise existing
 * class file.  Those interested in programatically generating classes
 * should see the <a href="../generic/ClassGen.html">ClassGen</a> class.

 * @see com.sun.org.apache.bcel.internal.generic.ClassGen
 * @LastModified: May 2021
 */
public class JavaClass extends AccessFlags implements Cloneable, Node, Comparable<JavaClass> {

    private String fileName;
    private String packageName;
    private String sourceFileName = "<Unknown>";
    private int classNameIndex;
    private int superclassNameIndex;
    private String className;
    private String superclassName;
    private int major;
    private int minor; // Compiler version
    private ConstantPool constantPool; // Constant pool
    private int[] interfaces; // implemented interfaces
    private String[] interfaceNames;
    private Field[] fields; // Fields, i.e., variables of class
    private Method[] methods; // methods defined in the class
    private Attribute[] attributes; // attributes defined in the class
    private AnnotationEntry[] annotations;   // annotations defined on the class
    private byte source = HEAP; // Generated in memory
    private boolean isAnonymous = false;
    private boolean isNested = false;
    private boolean computedNestedTypeStatus = false;
    public static final byte HEAP = 1;
    public static final byte FILE = 2;
    public static final byte ZIP = 3;
    private static final boolean debug = false;

    private static BCELComparator bcelComparator = new BCELComparator() {

        @Override
        public boolean equals( final Object o1, final Object o2 ) {
            final JavaClass THIS = (JavaClass) o1;
            final JavaClass THAT = (JavaClass) o2;
            return Objects.equals(THIS.getClassName(), THAT.getClassName());
        }


        @Override
        public int hashCode( final Object o ) {
            final JavaClass THIS = (JavaClass) o;
            return THIS.getClassName().hashCode();
        }
    };
    /**
     * In cases where we go ahead and create something,
     * use the default SyntheticRepository, because we
     * don't know any better.
     */
    private transient com.sun.org.apache.bcel.internal.util.Repository repository
            = SyntheticRepository.getInstance();


    /**
     * Constructor gets all contents as arguments.
     *
     * @param classNameIndex Index into constant pool referencing a
     * ConstantClass that represents this class.
     * @param superclassNameIndex Index into constant pool referencing a
     * ConstantClass that represents this class's superclass.
     * @param fileName File name
     * @param major Major compiler version
     * @param minor Minor compiler version
     * @param access_flags Access rights defined by bit flags
     * @param constantPool Array of constants
     * @param interfaces Implemented interfaces
     * @param fields Class fields
     * @param methods Class methods
     * @param attributes Class attributes
     * @param source Read from file or generated in memory?
     */
    public JavaClass(final int classNameIndex, final int superclassNameIndex,
            final String fileName, final int major, final int minor, final int access_flags,
            final ConstantPool constantPool, int[] interfaces, Field[] fields,
            Method[] methods, Attribute[] attributes, final byte source) {
        super(access_flags);
        if (interfaces == null) {
            interfaces = new int[0];
        }
        if (attributes == null) {
            attributes = new Attribute[0];
        }
        if (fields == null) {
            fields = new Field[0];
        }
        if (methods == null) {
            methods = new Method[0];
        }
        this.classNameIndex = classNameIndex;
        this.superclassNameIndex = superclassNameIndex;
        this.fileName = fileName;
        this.major = major;
        this.minor = minor;
        this.constantPool = constantPool;
        this.interfaces = interfaces;
        this.fields = fields;
        this.methods = methods;
        this.attributes = attributes;
        this.source = source;
        // Get source file name if available
        for (final Attribute attribute : attributes) {
            if (attribute instanceof SourceFile) {
                sourceFileName = ((SourceFile) attribute).getSourceFileName();
                break;
            }
        }
        /* According to the specification the following entries must be of type
         * `ConstantClass' but we check that anyway via the
         * `ConstPool.getConstant' method.
         */
        className = constantPool.getConstantString(classNameIndex, Const.CONSTANT_Class);
        className = Utility.compactClassName(className, false);
        final int index = className.lastIndexOf('.');
        if (index < 0) {
            packageName = "";
        } else {
            packageName = className.substring(0, index);
        }
        if (superclassNameIndex > 0) {
            // May be zero -> class is java.lang.Object
            superclassName = constantPool.getConstantString(superclassNameIndex,
                    Const.CONSTANT_Class);
            superclassName = Utility.compactClassName(superclassName, false);
        } else {
            superclassName = "java.lang.Object";
        }
        interfaceNames = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            final String str = constantPool.getConstantString(interfaces[i], Const.CONSTANT_Class);
            interfaceNames[i] = Utility.compactClassName(str, false);
        }
    }


    /**
     * Constructor gets all contents as arguments.
     *
     * @param classNameIndex Class name
     * @param superclassNameIndex Superclass name
     * @param fileName File name
     * @param major Major compiler version
     * @param minor Minor compiler version
     * @param access_flags Access rights defined by bit flags
     * @param constantPool Array of constants
     * @param interfaces Implemented interfaces
     * @param fields Class fields
     * @param methods Class methods
     * @param attributes Class attributes
     */
    public JavaClass(final int classNameIndex, final int superclassNameIndex,
            final String fileName, final int major, final int minor, final int access_flags,
            final ConstantPool constantPool, final int[] interfaces, final Field[] fields,
            final Method[] methods, final Attribute[] attributes) {
        this(classNameIndex, superclassNameIndex, fileName, major, minor, access_flags,
                constantPool, interfaces, fields, methods, attributes, HEAP);
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitely
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitJavaClass(this);
    }


    /* Print debug information depending on `JavaClass.debug'
     */
    static void Debug( final String str ) {
        if (debug) {
            System.out.println(str);
        }
    }


    /**
     * Dump class to a file.
     *
     * @param file Output file
     * @throws IOException
     */
    public void dump(final File file) throws IOException {
        final String parent = file.getParent();
        if (parent != null) {
            final File dir = new File(parent);
            if (!dir.mkdirs()) { // either was not created or already existed
                if (!dir.isDirectory()) {
                    throw new IOException("Could not create the directory " + dir);
                }
            }
        }
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dump(dos);
        }
    }


    /**
     * Dump class to a file named fileName.
     *
     * @param _file_name Output file name
     * @throws IOException
     */
    public void dump( final String _file_name ) throws IOException {
        dump(new File(_file_name));
    }


    /**
     * @return class in binary format
     */
    public byte[] getBytes() {
        final ByteArrayOutputStream s = new ByteArrayOutputStream();
        final DataOutputStream ds = new DataOutputStream(s);
        try {
            dump(ds);
        } catch (final IOException e) {
            System.err.println("Error dumping class: " + e.getMessage());
        } finally {
            try {
                ds.close();
            } catch (final IOException e2) {
                System.err.println("Error dumping class: " + e2.getMessage());
            }
        }
        return s.toByteArray();
    }


    /**
     * Dump Java class to output stream in binary format.
     *
     * @param file Output stream
     * @throws IOException
     */
    public void dump( final OutputStream file ) throws IOException {
        dump(new DataOutputStream(file));
    }


    /**
     * Dump Java class to output stream in binary format.
     *
     * @param file Output stream
     * @throws IOException
     */
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeInt(Const.JVM_CLASSFILE_MAGIC);
        file.writeShort(minor);
        file.writeShort(major);
        constantPool.dump(file);
        file.writeShort(super.getAccessFlags());
        file.writeShort(classNameIndex);
        file.writeShort(superclassNameIndex);
        file.writeShort(interfaces.length);
        for (final int interface1 : interfaces) {
            file.writeShort(interface1);
        }
        file.writeShort(fields.length);
        for (final Field field : fields) {
            field.dump(file);
        }
        file.writeShort(methods.length);
        for (final Method method : methods) {
            method.dump(file);
        }
        if (attributes != null) {
            file.writeShort(attributes.length);
            for (final Attribute attribute : attributes) {
                attribute.dump(file);
            }
        } else {
            file.writeShort(0);
        }
        file.flush();
    }


    /**
     * @return Attributes of the class.
     */
    public Attribute[] getAttributes() {
        return attributes;
    }

    /**
     * @return Annotations on the class
     * @since 6.0
     */
    public AnnotationEntry[] getAnnotationEntries() {
        if (annotations == null) {
            annotations = AnnotationEntry.createAnnotationEntries(getAttributes());
        }

        return annotations;
    }

    /**
     * @return Class name.
     */
    public String getClassName() {
        return className;
    }


    /**
     * @return Package name.
     */
    public String getPackageName() {
        return packageName;
    }


    /**
     * @return Class name index.
     */
    public int getClassNameIndex() {
        return classNameIndex;
    }


    /**
     * @return Constant pool.
     */
    public ConstantPool getConstantPool() {
        return constantPool;
    }


    /**
     * @return Fields, i.e., variables of the class. Like the JVM spec
     * mandates for the classfile format, these fields are those specific to
     * this class, and not those of the superclass or superinterfaces.
     */
    public Field[] getFields() {
        return fields;
    }


    /**
     * @return File name of class, aka SourceFile attribute value
     */
    public String getFileName() {
        return fileName;
    }


    /**
     * @return Names of implemented interfaces.
     */
    public String[] getInterfaceNames() {
        return interfaceNames;
    }


    /**
     * @return Indices in constant pool of implemented interfaces.
     */
    public int[] getInterfaceIndices() {
        return interfaces;
    }


    /**
     * @return Major number of class file version.
     */
    public int getMajor() {
        return major;
    }


    /**
     * @return Methods of the class.
     */
    public Method[] getMethods() {
        return methods;
    }


    /**
     * @return A {@link Method} corresponding to
     * java.lang.reflect.Method if any
     */
    public Method getMethod( final java.lang.reflect.Method m ) {
        for (final Method method : methods) {
            if (m.getName().equals(method.getName()) && (m.getModifiers() == method.getModifiers())
                    && Type.getSignature(m).equals(method.getSignature())) {
                return method;
            }
        }
        return null;
    }


    /**
     * @return Minor number of class file version.
     */
    public int getMinor() {
        return minor;
    }


    /**
     * @return sbsolute path to file where this class was read from
     */
    public String getSourceFileName() {
        return sourceFileName;
    }


    /**
     * returns the super class name of this class. In the case that this class is
     * java.lang.Object, it will return itself (java.lang.Object). This is probably incorrect
     * but isn't fixed at this time to not break existing clients.
     *
     * @return Superclass name.
     */
    public String getSuperclassName() {
        return superclassName;
    }


    /**
     * @return Class name index.
     */
    public int getSuperclassNameIndex() {
        return superclassNameIndex;
    }

    /**
     * @param attributes .
     */
    public void setAttributes( final Attribute[] attributes ) {
        this.attributes = attributes;
    }


    /**
     * @param className .
     */
    public void setClassName( final String className ) {
        this.className = className;
    }


    /**
     * @param classNameIndex .
     */
    public void setClassNameIndex( final int classNameIndex ) {
        this.classNameIndex = classNameIndex;
    }


    /**
     * @param constantPool .
     */
    public void setConstantPool( final ConstantPool constantPool ) {
        this.constantPool = constantPool;
    }


    /**
     * @param fields .
     */
    public void setFields( final Field[] fields ) {
        this.fields = fields;
    }


    /**
     * Set File name of class, aka SourceFile attribute value
     */
    public void setFileName( final String fileName ) {
        this.fileName = fileName;
    }


    /**
     * @param interfaceNames .
     */
    public void setInterfaceNames( final String[] interfaceNames ) {
        this.interfaceNames = interfaceNames;
    }


    /**
     * @param interfaces .
     */
    public void setInterfaces( final int[] interfaces ) {
        this.interfaces = interfaces;
    }


    /**
     * @param major .
     */
    public void setMajor( final int major ) {
        this.major = major;
    }


    /**
     * @param methods .
     */
    public void setMethods( final Method[] methods ) {
        this.methods = methods;
    }


    /**
     * @param minor .
     */
    public void setMinor( final int minor ) {
        this.minor = minor;
    }


    /**
     * Set absolute path to file this class was read from.
     */
    public void setSourceFileName( final String sourceFileName ) {
        this.sourceFileName = sourceFileName;
    }


    /**
     * @param superclassName .
     */
    public void setSuperclassName( final String superclassName ) {
        this.superclassName = superclassName;
    }


    /**
     * @param superclassNameIndex .
     */
    public void setSuperclassNameIndex( final int superclassNameIndex ) {
        this.superclassNameIndex = superclassNameIndex;
    }


    /**
     * @return String representing class contents.
     */
    @Override
    public String toString() {
        String access = Utility.accessToString(super.getAccessFlags(), true);
        access = access.isEmpty() ? "" : (access + " ");
        final StringBuilder buf = new StringBuilder(128);
        buf.append(access).append(Utility.classOrInterface(super.getAccessFlags())).append(" ").append(
                className).append(" extends ").append(
                Utility.compactClassName(superclassName, false)).append('\n');
        final int size = interfaces.length;
        if (size > 0) {
            buf.append("implements\t\t");
            for (int i = 0; i < size; i++) {
                buf.append(interfaceNames[i]);
                if (i < size - 1) {
                    buf.append(", ");
                }
            }
            buf.append('\n');
        }
        buf.append("file name\t\t").append(fileName).append('\n');
        buf.append("compiled from\t\t").append(sourceFileName).append('\n');
        buf.append("compiler version\t").append(major).append(".").append(minor).append('\n');
        buf.append("access flags\t\t").append(super.getAccessFlags()).append('\n');
        buf.append("constant pool\t\t").append(constantPool.getLength()).append(" entries\n");
        buf.append("ACC_SUPER flag\t\t").append(isSuper()).append("\n");
        if (attributes.length > 0) {
            buf.append("\nAttribute(s):\n");
            for (final Attribute attribute : attributes) {
                buf.append(indent(attribute));
            }
        }
        final AnnotationEntry[] annotations = getAnnotationEntries();
        if (annotations!=null && annotations.length>0) {
            buf.append("\nAnnotation(s):\n");
            for (final AnnotationEntry annotation : annotations) {
                buf.append(indent(annotation));
            }
        }
        if (fields.length > 0) {
            buf.append("\n").append(fields.length).append(" fields:\n");
            for (final Field field : fields) {
                buf.append("\t").append(field).append('\n');
            }
        }
        if (methods.length > 0) {
            buf.append("\n").append(methods.length).append(" methods:\n");
            for (final Method method : methods) {
                buf.append("\t").append(method).append('\n');
            }
        }
        return buf.toString();
    }


    private static String indent( final Object obj ) {
        final StringTokenizer tok = new StringTokenizer(obj.toString(), "\n");
        final StringBuilder buf = new StringBuilder();
        while (tok.hasMoreTokens()) {
            buf.append("\t").append(tok.nextToken()).append("\n");
        }
        return buf.toString();
    }


    /**
     * @return deep copy of this class
     */
    public JavaClass copy() {
        JavaClass c = null;
        try {
            c = (JavaClass) clone();
            c.constantPool = constantPool.copy();
            c.interfaces = interfaces.clone();
            c.interfaceNames = interfaceNames.clone();
            c.fields = new Field[fields.length];
            for (int i = 0; i < fields.length; i++) {
                c.fields[i] = fields[i].copy(c.constantPool);
            }
            c.methods = new Method[methods.length];
            for (int i = 0; i < methods.length; i++) {
                c.methods[i] = methods[i].copy(c.constantPool);
            }
            c.attributes = new Attribute[attributes.length];
            for (int i = 0; i < attributes.length; i++) {
                c.attributes[i] = attributes[i].copy(c.constantPool);
            }
        } catch (final CloneNotSupportedException e) {
            // TODO should this throw?
        }
        return c;
    }


    public final boolean isSuper() {
        return (super.getAccessFlags() & Const.ACC_SUPER) != 0;
    }


    public final boolean isClass() {
        return (super.getAccessFlags() & Const.ACC_INTERFACE) == 0;
    }

    /**
     * @since 6.0
     */
    public final boolean isAnonymous() {
        computeNestedTypeStatus();
        return this.isAnonymous;
    }

    /**
     * @since 6.0
     */
    public final boolean isNested() {
        computeNestedTypeStatus();
        return this.isNested;
    }

    private void computeNestedTypeStatus() {
        if (computedNestedTypeStatus) {
            return;
        }
        for (final Attribute attribute : this.attributes) {
              if (attribute instanceof InnerClasses) {
                  final InnerClass[] innerClasses = ((InnerClasses) attribute).getInnerClasses();
                  for (final InnerClass innerClasse : innerClasses) {
                      boolean innerClassAttributeRefersToMe = false;
                      String inner_class_name = constantPool.getConstantString(innerClasse.getInnerClassIndex(),
                                 Const.CONSTANT_Class);
                      inner_class_name = Utility.compactClassName(inner_class_name, false);
                      if (inner_class_name.equals(getClassName())) {
                          innerClassAttributeRefersToMe = true;
                      }
                      if (innerClassAttributeRefersToMe) {
                          this.isNested = true;
                          if (innerClasse.getInnerNameIndex() == 0) {
                              this.isAnonymous = true;
                          }
                      }
                  }
              }
        }
        this.computedNestedTypeStatus = true;
    }


    /** @return returns either HEAP (generated), FILE, or ZIP
     */
    public final byte getSource() {
        return source;
    }


    /********************* New repository functionality *********************/
    /**
     * Gets the ClassRepository which holds its definition. By default
     * this is the same as SyntheticRepository.getInstance();
     */
    public com.sun.org.apache.bcel.internal.util.Repository getRepository() {
        return repository;
    }


    /**
     * Sets the ClassRepository which loaded the JavaClass.
     * Should be called immediately after parsing is done.
     */
    public void setRepository( final com.sun.org.apache.bcel.internal.util.Repository repository ) { // TODO make protected?
        this.repository = repository;
    }


    /** Equivalent to runtime "instanceof" operator.
     *
     * @return true if this JavaClass is derived from the super class
     * @throws ClassNotFoundException if superclasses or superinterfaces
     *   of this object can't be found
     */
    public final boolean instanceOf( final JavaClass super_class ) throws ClassNotFoundException {
        if (this.equals(super_class)) {
            return true;
        }
        final JavaClass[] super_classes = getSuperClasses();
        for (final JavaClass super_classe : super_classes) {
            if (super_classe.equals(super_class)) {
                return true;
            }
        }
        if (super_class.isInterface()) {
            return implementationOf(super_class);
        }
        return false;
    }


    /**
     * @return true, if this class is an implementation of interface inter
     * @throws ClassNotFoundException if superclasses or superinterfaces
     *   of this class can't be found
     */
    public boolean implementationOf( final JavaClass inter ) throws ClassNotFoundException {
        if (!inter.isInterface()) {
            throw new IllegalArgumentException(inter.getClassName() + " is no interface");
        }
        if (this.equals(inter)) {
            return true;
        }
        final JavaClass[] super_interfaces = getAllInterfaces();
        for (final JavaClass super_interface : super_interfaces) {
            if (super_interface.equals(inter)) {
                return true;
            }
        }
        return false;
    }


    /**
     * @return the superclass for this JavaClass object, or null if this
     * is java.lang.Object
     * @throws ClassNotFoundException if the superclass can't be found
     */
    public JavaClass getSuperClass() throws ClassNotFoundException {
        if ("java.lang.Object".equals(getClassName())) {
            return null;
        }
        return repository.loadClass(getSuperclassName());
    }


    /**
     * @return list of super classes of this class in ascending order, i.e.,
     * java.lang.Object is always the last element
     * @throws ClassNotFoundException if any of the superclasses can't be found
     */
    public JavaClass[] getSuperClasses() throws ClassNotFoundException {
        JavaClass clazz = this;
        final List<JavaClass> allSuperClasses = new ArrayList<>();
        for (clazz = clazz.getSuperClass(); clazz != null; clazz = clazz.getSuperClass()) {
            allSuperClasses.add(clazz);
        }
        return allSuperClasses.toArray(new JavaClass[allSuperClasses.size()]);
    }


    /**
     * Get interfaces directly implemented by this JavaClass.
     */
    public JavaClass[] getInterfaces() throws ClassNotFoundException {
        final String[] _interfaces = getInterfaceNames();
        final JavaClass[] classes = new JavaClass[_interfaces.length];
        for (int i = 0; i < _interfaces.length; i++) {
            classes[i] = repository.loadClass(_interfaces[i]);
        }
        return classes;
    }


    /**
     * Get all interfaces implemented by this JavaClass (transitively).
     */
    public JavaClass[] getAllInterfaces() throws ClassNotFoundException {
        final ClassQueue queue = new ClassQueue();
        final Set<JavaClass> allInterfaces = new TreeSet<>();
        queue.enqueue(this);
        while (!queue.empty()) {
            final JavaClass clazz = queue.dequeue();
            final JavaClass souper = clazz.getSuperClass();
            final JavaClass[] _interfaces = clazz.getInterfaces();
            if (clazz.isInterface()) {
                allInterfaces.add(clazz);
            } else {
                if (souper != null) {
                    queue.enqueue(souper);
                }
            }
            for (final JavaClass _interface : _interfaces) {
                queue.enqueue(_interface);
            }
        }
        return allInterfaces.toArray(new JavaClass[allInterfaces.size()]);
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
     * By default two JavaClass objects are said to be equal when
     * their class names are equal.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object obj ) {
        return bcelComparator.equals(this, obj);
    }


    /**
     * Return the natural ordering of two JavaClasses.
     * This ordering is based on the class name
     * @since 6.0
     */
    @Override
    public int compareTo( final JavaClass obj ) {
        return getClassName().compareTo(obj.getClassName());
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
