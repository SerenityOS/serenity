/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.org.apache.bcel.internal.Repository;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;

/**
 * Super class for object and array types.
 *
 */
public abstract class ReferenceType extends Type {

    protected ReferenceType(final byte t, final String s) {
        super(t, s);
    }


    /** Class is non-abstract but not instantiable from the outside
     */
    ReferenceType() {
        super(Const.T_OBJECT, "<null object>");
    }


    /**
     * Return true iff this type is castable to another type t as defined in
     * the JVM specification.  The case where this is Type.NULL is not
     * defined (see the CHECKCAST definition in the JVM specification).
     * However, because e.g. CHECKCAST doesn't throw a
     * ClassCastException when casting a null reference to any Object,
     * true is returned in this case.
     *
     * @throws ClassNotFoundException if any classes or interfaces required
     *  to determine assignment compatibility can't be found
     */
    public boolean isCastableTo( final Type t ) throws ClassNotFoundException {
        if (this.equals(Type.NULL)) {
            return t instanceof ReferenceType; // If this is ever changed in isAssignmentCompatible()
        }
        return isAssignmentCompatibleWith(t);
        /* Yes, it's true: It's the same definition.
         * See vmspec2 AASTORE / CHECKCAST definitions.
         */
    }


    /**
     * Return true iff this is assignment compatible with another type t
     * as defined in the JVM specification; see the AASTORE definition
     * there.
     * @throws ClassNotFoundException if any classes or interfaces required
     *  to determine assignment compatibility can't be found
     */
    public boolean isAssignmentCompatibleWith( final Type t ) throws ClassNotFoundException {
        if (!(t instanceof ReferenceType)) {
            return false;
        }
        final ReferenceType T = (ReferenceType) t;
        if (this.equals(Type.NULL)) {
            return true; // This is not explicitely stated, but clear. Isn't it?
        }
        /* If this is a class type then
         */
        if ((this instanceof ObjectType) && (((ObjectType) this).referencesClassExact())) {
            /* If T is a class type, then this must be the same class as T,
             or this must be a subclass of T;
             */
            if ((T instanceof ObjectType) && (((ObjectType) T).referencesClassExact())) {
                if (this.equals(T)) {
                    return true;
                }
                if (Repository.instanceOf(((ObjectType) this).getClassName(), ((ObjectType) T)
                        .getClassName())) {
                    return true;
                }
            }
            /* If T is an interface type, this must implement interface T.
             */
            if ((T instanceof ObjectType) && (((ObjectType) T).referencesInterfaceExact())) {
                if (Repository.implementationOf(((ObjectType) this).getClassName(),
                        ((ObjectType) T).getClassName())) {
                    return true;
                }
            }
        }
        /* If this is an interface type, then:
         */
        if ((this instanceof ObjectType) && (((ObjectType) this).referencesInterfaceExact())) {
            /* If T is a class type, then T must be Object (2.4.7).
             */
            if ((T instanceof ObjectType) && (((ObjectType) T).referencesClassExact())) {
                if (T.equals(Type.OBJECT)) {
                    return true;
                }
            }
            /* If T is an interface type, then T must be the same interface
             * as this or a superinterface of this (2.13.2).
             */
            if ((T instanceof ObjectType) && (((ObjectType) T).referencesInterfaceExact())) {
                if (this.equals(T)) {
                    return true;
                }
                if (Repository.implementationOf(((ObjectType) this).getClassName(),
                        ((ObjectType) T).getClassName())) {
                    return true;
                }
            }
        }
        /* If this is an array type, namely, the type SC[], that is, an
         * array of components of type SC, then:
         */
        if (this instanceof ArrayType) {
            /* If T is a class type, then T must be Object (2.4.7).
             */
            if ((T instanceof ObjectType) && (((ObjectType) T).referencesClassExact())) {
                if (T.equals(Type.OBJECT)) {
                    return true;
                }
            }
            /* If T is an array type TC[], that is, an array of components
             * of type TC, then one of the following must be true:
             */
            if (T instanceof ArrayType) {
                /* TC and SC are the same primitive type (2.4.1).
                 */
                final Type sc = ((ArrayType) this).getElementType();
                final Type tc = ((ArrayType) T).getElementType();
                if (sc instanceof BasicType && tc instanceof BasicType && sc.equals(tc)) {
                    return true;
                }
                /* TC and SC are reference types (2.4.6), and type SC is
                 * assignable to TC by these runtime rules.
                 */
                if (tc instanceof ReferenceType && sc instanceof ReferenceType
                        && ((ReferenceType) sc).isAssignmentCompatibleWith(tc)) {
                    return true;
                }
            }
            /* If T is an interface type, T must be one of the interfaces implemented by arrays (2.15). */
            // TODO: Check if this is still valid or find a way to dynamically find out which
            // interfaces arrays implement. However, as of the JVM specification edition 2, there
            // are at least two different pages where assignment compatibility is defined and
            // on one of them "interfaces implemented by arrays" is exchanged with "'Cloneable' or
            // 'java.io.Serializable'"
            if ((T instanceof ObjectType) && (((ObjectType) T).referencesInterfaceExact())) {
                for (final String element : Const.getInterfacesImplementedByArrays()) {
                    if (T.equals(ObjectType.getInstance(element))) {
                        return true;
                    }
                }
            }
        }
        return false; // default.
    }


    /**
     * This commutative operation returns the first common superclass (narrowest ReferenceType
     * referencing a class, not an interface).
     * If one of the types is a superclass of the other, the former is returned.
     * If "this" is Type.NULL, then t is returned.
     * If t is Type.NULL, then "this" is returned.
     * If "this" equals t ['this.equals(t)'] "this" is returned.
     * If "this" or t is an ArrayType, then Type.OBJECT is returned;
     * unless their dimensions match. Then an ArrayType of the same
     * number of dimensions is returned, with its basic type being the
     * first common super class of the basic types of "this" and t.
     * If "this" or t is a ReferenceType referencing an interface, then Type.OBJECT is returned.
     * If not all of the two classes' superclasses cannot be found, "null" is returned.
     * See the JVM specification edition 2, "4.9.2 The Bytecode Verifier".
     *
     * @throws ClassNotFoundException on failure to find superclasses of this
     *  type, or the type passed as a parameter
     */
    public ReferenceType getFirstCommonSuperclass( final ReferenceType t ) throws ClassNotFoundException {
        if (this.equals(Type.NULL)) {
            return t;
        }
        if (t.equals(Type.NULL)) {
            return this;
        }
        if (this.equals(t)) {
            return this;
            /*
             * TODO: Above sounds a little arbitrary. On the other hand, there is
             * no object referenced by Type.NULL so we can also say all the objects
             * referenced by Type.NULL were derived from java.lang.Object.
             * However, the Java Language's "instanceof" operator proves us wrong:
             * "null" is not referring to an instance of java.lang.Object :)
             */
        }
        /* This code is from a bug report by Konstantin Shagin <konst@cs.technion.ac.il> */
        if ((this instanceof ArrayType) && (t instanceof ArrayType)) {
            final ArrayType arrType1 = (ArrayType) this;
            final ArrayType arrType2 = (ArrayType) t;
            if ((arrType1.getDimensions() == arrType2.getDimensions())
                    && arrType1.getBasicType() instanceof ObjectType
                    && arrType2.getBasicType() instanceof ObjectType) {
                return new ArrayType(((ObjectType) arrType1.getBasicType())
                        .getFirstCommonSuperclass((ObjectType) arrType2.getBasicType()), arrType1
                        .getDimensions());
            }
        }
        if ((this instanceof ArrayType) || (t instanceof ArrayType)) {
            return Type.OBJECT;
            // TODO: Is there a proof of OBJECT being the direct ancestor of every ArrayType?
        }
        if (((this instanceof ObjectType) && ((ObjectType) this).referencesInterfaceExact())
                || ((t instanceof ObjectType) && ((ObjectType) t).referencesInterfaceExact())) {
            return Type.OBJECT;
            // TODO: The above line is correct comparing to the vmspec2. But one could
            // make class file verification a bit stronger here by using the notion of
            // superinterfaces or even castability or assignment compatibility.
        }
        // this and t are ObjectTypes, see above.
        final ObjectType thiz = (ObjectType) this;
        final ObjectType other = (ObjectType) t;
        final JavaClass[] thiz_sups = Repository.getSuperClasses(thiz.getClassName());
        final JavaClass[] other_sups = Repository.getSuperClasses(other.getClassName());
        if ((thiz_sups == null) || (other_sups == null)) {
            return null;
        }
        // Waaahh...
        final JavaClass[] this_sups = new JavaClass[thiz_sups.length + 1];
        final JavaClass[] t_sups = new JavaClass[other_sups.length + 1];
        System.arraycopy(thiz_sups, 0, this_sups, 1, thiz_sups.length);
        System.arraycopy(other_sups, 0, t_sups, 1, other_sups.length);
        this_sups[0] = Repository.lookupClass(thiz.getClassName());
        t_sups[0] = Repository.lookupClass(other.getClassName());
        for (final JavaClass t_sup : t_sups) {
            for (final JavaClass this_sup : this_sups) {
                if (this_sup.equals(t_sup)) {
                    return ObjectType.getInstance(this_sup.getClassName());
                }
            }
        }
        // Huh? Did you ask for Type.OBJECT's superclass??
        return null;
    }

    /**
     * This commutative operation returns the first common superclass (narrowest ReferenceType
     * referencing a class, not an interface).
     * If one of the types is a superclass of the other, the former is returned.
     * If "this" is Type.NULL, then t is returned.
     * If t is Type.NULL, then "this" is returned.
     * If "this" equals t ['this.equals(t)'] "this" is returned.
     * If "this" or t is an ArrayType, then Type.OBJECT is returned.
     * If "this" or t is a ReferenceType referencing an interface, then Type.OBJECT is returned.
     * If not all of the two classes' superclasses cannot be found, "null" is returned.
     * See the JVM specification edition 2, "4.9.2 The Bytecode Verifier".
     *
     * @deprecated use getFirstCommonSuperclass(ReferenceType t) which has
     *             slightly changed semantics.
     * @throws ClassNotFoundException on failure to find superclasses of this
     *  type, or the type passed as a parameter
     */
    @Deprecated
    public ReferenceType firstCommonSuperclass( final ReferenceType t ) throws ClassNotFoundException {
        if (this.equals(Type.NULL)) {
            return t;
        }
        if (t.equals(Type.NULL)) {
            return this;
        }
        if (this.equals(t)) {
            return this;
            /*
             * TODO: Above sounds a little arbitrary. On the other hand, there is
             * no object referenced by Type.NULL so we can also say all the objects
             * referenced by Type.NULL were derived from java.lang.Object.
             * However, the Java Language's "instanceof" operator proves us wrong:
             * "null" is not referring to an instance of java.lang.Object :)
             */
        }
        if ((this instanceof ArrayType) || (t instanceof ArrayType)) {
            return Type.OBJECT;
            // TODO: Is there a proof of OBJECT being the direct ancestor of every ArrayType?
        }
        if (((this instanceof ObjectType) && ((ObjectType) this).referencesInterface())
                || ((t instanceof ObjectType) && ((ObjectType) t).referencesInterface())) {
            return Type.OBJECT;
            // TODO: The above line is correct comparing to the vmspec2. But one could
            // make class file verification a bit stronger here by using the notion of
            // superinterfaces or even castability or assignment compatibility.
        }
        // this and t are ObjectTypes, see above.
        final ObjectType thiz = (ObjectType) this;
        final ObjectType other = (ObjectType) t;
        final JavaClass[] thiz_sups = Repository.getSuperClasses(thiz.getClassName());
        final JavaClass[] other_sups = Repository.getSuperClasses(other.getClassName());
        if ((thiz_sups == null) || (other_sups == null)) {
            return null;
        }
        // Waaahh...
        final JavaClass[] this_sups = new JavaClass[thiz_sups.length + 1];
        final JavaClass[] t_sups = new JavaClass[other_sups.length + 1];
        System.arraycopy(thiz_sups, 0, this_sups, 1, thiz_sups.length);
        System.arraycopy(other_sups, 0, t_sups, 1, other_sups.length);
        this_sups[0] = Repository.lookupClass(thiz.getClassName());
        t_sups[0] = Repository.lookupClass(other.getClassName());
        for (final JavaClass t_sup : t_sups) {
            for (final JavaClass this_sup : this_sups) {
                if (this_sup.equals(t_sup)) {
                    return ObjectType.getInstance(this_sup.getClassName());
                }
            }
        }
        // Huh? Did you ask for Type.OBJECT's superclass??
        return null;
    }
}
