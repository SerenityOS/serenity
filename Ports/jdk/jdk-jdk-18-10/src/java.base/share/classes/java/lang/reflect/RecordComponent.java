/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.reflect;

import jdk.internal.access.SharedSecrets;
import sun.reflect.annotation.AnnotationParser;
import sun.reflect.annotation.TypeAnnotation;
import sun.reflect.annotation.TypeAnnotationParser;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.FieldRepository;
import sun.reflect.generics.scope.ClassScope;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

/**
 * A {@code RecordComponent} provides information about, and dynamic access to, a
 * component of a record class.
 *
 * @see Class#getRecordComponents()
 * @see java.lang.Record
 * @jls 8.10 Record Classes
 * @since 16
 */
public final class RecordComponent implements AnnotatedElement {
    // declaring class
    private Class<?> clazz;
    private String name;
    private Class<?> type;
    private Method accessor;
    private String signature;
    // generic info repository; lazily initialized
    private transient FieldRepository genericInfo;
    private byte[] annotations;
    private byte[] typeAnnotations;
    private RecordComponent root;

    // only the JVM can create record components
    private RecordComponent() {}

    /**
     * Returns the name of this record component.
     *
     * @return the name of this record component
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a {@code Class} that identifies the declared type for this
     * record component.
     *
     * @return a {@code Class} identifying the declared type of the component
     * represented by this record component
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Returns a {@code String} that describes the generic type signature for
     * this record component.
     *
     * @return a {@code String} that describes the generic type signature for
     * this record component
     *
     * @jvms 4.7.9.1 Signatures
     */
    public String getGenericSignature() {
        return signature;
    }

    /**
     * Returns a {@code Type} object that represents the declared type for
     * this record component.
     *
     * <p>If the declared type of the record component is a parameterized type,
     * the {@code Type} object returned reflects the actual type arguments used
     * in the source code.
     *
     * <p>If the type of the underlying record component is a type variable or a
     * parameterized type, it is created. Otherwise, it is resolved.
     *
     * @return a {@code Type} object that represents the declared type for
     *         this record component
     * @throws GenericSignatureFormatError if the generic record component
     *         signature does not conform to the format specified in
     *         <cite>The Java Virtual Machine Specification</cite>
     * @throws TypeNotPresentException if the generic type
     *         signature of the underlying record component refers to a non-existent
     *         type declaration
     * @throws MalformedParameterizedTypeException if the generic
     *         signature of the underlying record component refers to a parameterized
     *         type that cannot be instantiated for any reason
     */
    public Type getGenericType() {
        if (getGenericSignature() != null)
            return getGenericInfo().getGenericType();
        else
            return getType();
    }

    // Accessor for generic info repository
    private FieldRepository getGenericInfo() {
        // lazily initialize repository if necessary
        if (genericInfo == null) {
            // create and cache generic info repository
            genericInfo = FieldRepository.make(getGenericSignature(), getFactory());
        }
        return genericInfo; //return cached repository
    }

    // Accessor for factory
    private GenericsFactory getFactory() {
        Class<?> c = getDeclaringRecord();
        // create scope and factory
        return CoreReflectionFactory.make(c, ClassScope.make(c));
    }

    /**
     * Returns an {@code AnnotatedType} object that represents the use of a type to specify
     * the declared type of this record component.
     *
     * @return an object representing the declared type of this record component
     */
    public AnnotatedType getAnnotatedType() {
        return TypeAnnotationParser.buildAnnotatedType(typeAnnotations,
                SharedSecrets.getJavaLangAccess().
                        getConstantPool(getDeclaringRecord()),
                this,
                getDeclaringRecord(),
                getGenericType(),
                TypeAnnotation.TypeAnnotationTarget.FIELD);
    }

    /**
     * Returns a {@code Method} that represents the accessor for this record
     * component.
     *
     * @return a {@code Method} that represents the accessor for this record
     * component
     */
    public Method getAccessor() {
        return accessor;
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotation returned by this method is a
     * declaration annotation.
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return annotationClass.cast(declaredAnnotations().get(annotationClass));
    }

    private transient volatile Map<Class<? extends Annotation>, Annotation> declaredAnnotations;

    private Map<Class<? extends Annotation>, Annotation> declaredAnnotations() {
        Map<Class<? extends Annotation>, Annotation> declAnnos;
        if ((declAnnos = declaredAnnotations) == null) {
            synchronized (this) {
                if ((declAnnos = declaredAnnotations) == null) {
                    RecordComponent root = this.root;
                    if (root != null) {
                        declAnnos = root.declaredAnnotations();
                    } else {
                        declAnnos = AnnotationParser.parseAnnotations(
                                annotations,
                                SharedSecrets.getJavaLangAccess()
                                        .getConstantPool(getDeclaringRecord()),
                                getDeclaringRecord());
                    }
                    declaredAnnotations = declAnnos;
                }
            }
        }
        return declAnnos;
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     */
    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     */
    @Override
    public Annotation[] getDeclaredAnnotations() { return AnnotationParser.toArray(declaredAnnotations()); }

    /**
     * Returns a string describing this record component. The format is
     * the record component type, followed by a space, followed by the name
     * of the record component.
     * For example:
     * <pre>
     *    java.lang.String name
     *    int age
     * </pre>
     *
     * @return a string describing this record component
     */
    public String toString() {
        return (getType().getTypeName() + " " + getName());
    }

    /**
     * Returns the record class which declares this record component.
     *
     * @return The record class declaring this record component.
     */
    public Class<?> getDeclaringRecord() {
        return clazz;
    }
}
