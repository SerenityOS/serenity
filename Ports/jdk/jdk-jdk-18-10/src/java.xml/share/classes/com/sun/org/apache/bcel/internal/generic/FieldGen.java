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
import com.sun.org.apache.bcel.internal.classfile.AnnotationEntry;
import com.sun.org.apache.bcel.internal.classfile.Annotations;
import com.sun.org.apache.bcel.internal.classfile.Attribute;
import com.sun.org.apache.bcel.internal.classfile.Constant;
import com.sun.org.apache.bcel.internal.classfile.ConstantObject;
import com.sun.org.apache.bcel.internal.classfile.ConstantPool;
import com.sun.org.apache.bcel.internal.classfile.ConstantValue;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Utility;
import com.sun.org.apache.bcel.internal.util.BCELComparator;

/**
 * Template class for building up a field.  The only extraordinary thing
 * one can do is to add a constant value attribute to a field (which must of
 * course be compatible with to the declared type).
 *
 * @see Field
 * @LastModified: May 2021
 */
public class FieldGen extends FieldGenOrMethodGen {

    private Object value = null;
    private static BCELComparator bcelComparator = new BCELComparator() {

        @Override
        public boolean equals( final Object o1, final Object o2 ) {
            final FieldGen THIS = (FieldGen) o1;
            final FieldGen THAT = (FieldGen) o2;
            return Objects.equals(THIS.getName(), THAT.getName())
                    && Objects.equals(THIS.getSignature(), THAT.getSignature());
        }


        @Override
        public int hashCode( final Object o ) {
            final FieldGen THIS = (FieldGen) o;
            return THIS.getSignature().hashCode() ^ THIS.getName().hashCode();
        }
    };


    /**
     * Declare a field. If it is static (isStatic() == true) and has a
     * basic type like int or String it may have an initial value
     * associated with it as defined by setInitValue().
     *
     * @param access_flags access qualifiers
     * @param type  field type
     * @param name field name
     * @param cp constant pool
     */
    public FieldGen(final int access_flags, final Type type, final String name, final ConstantPoolGen cp) {
        super(access_flags);
        setType(type);
        setName(name);
        setConstantPool(cp);
    }


    /**
     * Instantiate from existing field.
     *
     * @param field Field object
     * @param cp constant pool (must contain the same entries as the field's constant pool)
     */
    public FieldGen(final Field field, final ConstantPoolGen cp) {
        this(field.getAccessFlags(), Type.getType(field.getSignature()), field.getName(), cp);
        final Attribute[] attrs = field.getAttributes();
        for (final Attribute attr : attrs) {
            if (attr instanceof ConstantValue) {
                setValue(((ConstantValue) attr).getConstantValueIndex());
            } else if (attr instanceof Annotations) {
                final Annotations runtimeAnnotations = (Annotations)attr;
                final AnnotationEntry[] annotationEntries = runtimeAnnotations.getAnnotationEntries();
                for (final AnnotationEntry element : annotationEntries) {
                    addAnnotationEntry(new AnnotationEntryGen(element,cp,false));
                }
            } else {
                addAttribute(attr);
            }
        }
    }


    private void setValue( final int index ) {
        final ConstantPool cp = super.getConstantPool().getConstantPool();
        final Constant c = cp.getConstant(index);
        value = ((ConstantObject) c).getConstantValue(cp);
    }


    /**
     * Set (optional) initial value of field, otherwise it will be set to null/0/false
     * by the JVM automatically.
     */
    public void setInitValue( final String str ) {
        checkType(  ObjectType.getInstance("java.lang.String"));
        if (str != null) {
            value = str;
        }
    }


    public void setInitValue( final long l ) {
        checkType(Type.LONG);
        if (l != 0L) {
            value = Long.valueOf(l);
        }
    }


    public void setInitValue( final int i ) {
        checkType(Type.INT);
        if (i != 0) {
            value = Integer.valueOf(i);
        }
    }


    public void setInitValue( final short s ) {
        checkType(Type.SHORT);
        if (s != 0) {
            value = Integer.valueOf(s);
        }
    }


    public void setInitValue( final char c ) {
        checkType(Type.CHAR);
        if (c != 0) {
            value = Integer.valueOf(c);
        }
    }


    public void setInitValue( final byte b ) {
        checkType(Type.BYTE);
        if (b != 0) {
            value = Integer.valueOf(b);
        }
    }


    public void setInitValue( final boolean b ) {
        checkType(Type.BOOLEAN);
        if (b) {
            value = Integer.valueOf(1);
        }
    }


    public void setInitValue( final float f ) {
        checkType(Type.FLOAT);
        if (f != 0.0) {
            value = f;
        }
    }


    public void setInitValue( final double d ) {
        checkType(Type.DOUBLE);
        if (d != 0.0) {
            value = d;
        }
    }


    /** Remove any initial value.
     */
    public void cancelInitValue() {
        value = null;
    }


    private void checkType( final Type atype ) {
        final Type superType = super.getType();
        if (superType == null) {
            throw new ClassGenException("You haven't defined the type of the field yet");
        }
        if (!isFinal()) {
            throw new ClassGenException("Only final fields may have an initial value!");
        }
        if (!superType.equals(atype)) {
            throw new ClassGenException("Types are not compatible: " + superType + " vs. " + atype);
        }
    }


    /**
     * Get field object after having set up all necessary values.
     */
    public Field getField() {
        final String signature = getSignature();
        final int name_index = super.getConstantPool().addUtf8(super.getName());
        final int signature_index = super.getConstantPool().addUtf8(signature);
        if (value != null) {
            checkType(super.getType());
            final int index = addConstant();
            addAttribute(new ConstantValue(super.getConstantPool().addUtf8("ConstantValue"), 2, index,
                    super.getConstantPool().getConstantPool())); // sic
        }
        addAnnotationsAsAttribute(super.getConstantPool());
        return new Field(super.getAccessFlags(), name_index, signature_index, getAttributes(),
                super.getConstantPool().getConstantPool()); // sic
    }

    private void addAnnotationsAsAttribute(final ConstantPoolGen cp) {
          final Attribute[] attrs = AnnotationEntryGen.getAnnotationAttributes(cp, super.getAnnotationEntries());
        for (final Attribute attr : attrs) {
            addAttribute(attr);
        }
      }


    private int addConstant() {
        switch (super.getType().getType()) { // sic
            case Const.T_INT:
            case Const.T_CHAR:
            case Const.T_BYTE:
            case Const.T_BOOLEAN:
            case Const.T_SHORT:
                return super.getConstantPool().addInteger(((Integer) value));
            case Const.T_FLOAT:
                return super.getConstantPool().addFloat(((Float) value));
            case Const.T_DOUBLE:
                return super.getConstantPool().addDouble(((Double) value));
            case Const.T_LONG:
                return super.getConstantPool().addLong(((Long) value));
            case Const.T_REFERENCE:
                return super.getConstantPool().addString((String) value);
            default:
                throw new IllegalStateException("Unhandled : " + super.getType().getType()); // sic
        }
    }


    @Override
    public String getSignature() {
        return super.getType().getSignature();
    }

    private List<FieldObserver> observers;


    /** Add observer for this object.
     */
    public void addObserver( final FieldObserver o ) {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        observers.add(o);
    }


    /** Remove observer for this object.
     */
    public void removeObserver( final FieldObserver o ) {
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
            for (final FieldObserver observer : observers ) {
                observer.notify(this);
            }
        }
    }


    public String getInitValue() {
        if (value != null) {
            return value.toString();
        }
        return null;
    }


    /**
     * Return string representation close to declaration format,
     * `public static final short MAX = 100', e.g..
     *
     * @return String representation of field
     */
    @Override
    public final String toString() {
        String name;
        String signature;
        String access; // Short cuts to constant pool
        access = Utility.accessToString(super.getAccessFlags());
        access = access.isEmpty() ? "" : (access + " ");
        signature = super.getType().toString();
        name = getName();
        final StringBuilder buf = new StringBuilder(32); // CHECKSTYLE IGNORE MagicNumber
        buf.append(access).append(signature).append(" ").append(name);
        final String value = getInitValue();
        if (value != null) {
            buf.append(" = ").append(value);
        }
        return buf.toString();
    }


    /** @return deep copy of this field
     */
    public FieldGen copy( final ConstantPoolGen cp ) {
        final FieldGen fg = (FieldGen) clone();
        fg.setConstantPool(cp);
        return fg;
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
     * By default two FieldGen objects are said to be equal when
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
     * By default return the hashcode of the field's name XOR signature.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return bcelComparator.hashCode(this);
    }
}
