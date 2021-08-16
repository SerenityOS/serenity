/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.lang.model.element;


import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.IncompleteAnnotationException;
import java.util.List;
import java.util.Set;

import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * Represents a program element such as a module, package, class, or method.
 * Each element represents a compile-time language-level construct
 * (and not, for example, a runtime construct of the virtual machine).
 *
 * <p> Elements should be compared using the {@link #equals(Object)}
 * method.  There is no guarantee that any particular element will
 * always be represented by the same object.
 *
 * <p> To implement operations based on the class of an {@code
 * Element} object, either use a {@linkplain ElementVisitor visitor} or
 * use the result of the {@link #getKind} method.  Using {@code
 * instanceof} is <em>not</em> necessarily a reliable idiom for
 * determining the effective class of an object in this modeling
 * hierarchy since an implementation may choose to have a single object
 * implement multiple {@code Element} subinterfaces.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see Elements
 * @see TypeMirror
 * @since 1.6
 */
public interface Element extends javax.lang.model.AnnotatedConstruct {
    /**
     * {@return the type defined by this element}
     *
     * @see Types
     * @see ExecutableElement#asType
     * @see ModuleElement#asType
     * @see PackageElement#asType
     * @see TypeElement#asType
     * @see TypeParameterElement#asType
     * @see VariableElement#asType
     */
    TypeMirror asType();

    /**
     * {@return the {@code kind} of this element}
     *
     * <ul>
     *
     * <li> The kind of a {@linkplain PackageElement package} is
     * {@link ElementKind#PACKAGE PACKAGE}.
     *
     * <li> The kind of a {@linkplain ModuleElement module} is {@link
     * ElementKind#MODULE MODULE}.
     *
     * <li> The kind of a {@linkplain TypeElement type element} is one
     * of {@link ElementKind#ANNOTATION_TYPE ANNOTATION_TYPE}, {@link
     * ElementKind#CLASS CLASS}, {@link ElementKind#ENUM ENUM}, {@link
     * ElementKind#INTERFACE INTERFACE}, or {@link ElementKind#RECORD
     * RECORD}.
     *
     * <li> The kind of a {@linkplain VariableElement variable} is one
     * of {@link ElementKind#ENUM_CONSTANT ENUM_CONSTANT}, {@link
     * ElementKind#EXCEPTION_PARAMETER EXCEPTION_PARAMETER}, {@link
     * ElementKind#FIELD FIELD}, {@link ElementKind#LOCAL_VARIABLE
     * LOCAL_VARIABLE}, {@link ElementKind#PARAMETER PARAMETER},
     * {@link ElementKind#RESOURCE_VARIABLE RESOURCE_VARIABLE}, or
     * {@link ElementKind#BINDING_VARIABLE BINDING_VARIABLE}.
     *
     * <li> The kind of an {@linkplain ExecutableElement executable}
     * is one of {@link ElementKind#CONSTRUCTOR CONSTRUCTOR}, {@link
     * ElementKind#INSTANCE_INIT INSTANCE_INIT}, {@link
     * ElementKind#METHOD METHOD}, or {@link ElementKind#STATIC_INIT
     * STATIC_INIT}.
     *
     * <li> The kind of a {@linkplain TypeParameterElement type parameter} is
     * {@link ElementKind#TYPE_PARAMETER TYPE_PARAMETER}.
     *
     * <li> The kind of a {@linkplain RecordComponentElement record
     * component} is {@link ElementKind#RECORD_COMPONENT
     * RECORD_COMPONENT}.
     *
     * </ul>
     */
    ElementKind getKind();

    /**
     * Returns the modifiers of this element, excluding annotations.
     * Implicit modifiers, such as the {@code public} and {@code static}
     * modifiers of interface members, are included.
     *
     * @return the modifiers of this element, or an empty set if there are none
     */
    Set<Modifier> getModifiers();

    /**
     * {@return the simple (unqualified) name of this element} The
     * name of a generic class or interface does not include any
     * reference to its formal type parameters.
     *
     * For example, the simple name of the type element representing
     * {@code java.util.Set<E>} is {@code "Set"}.
     *
     * If this element represents an unnamed {@linkplain
     * PackageElement#getSimpleName package} or unnamed {@linkplain
     * ModuleElement#getSimpleName module}, an <a
     * href=Name.html#empty_name>empty name</a> is returned.
     *
     * If it represents a {@linkplain ExecutableElement#getSimpleName
     * constructor}, the name "{@code <init>}" is returned.  If it
     * represents a {@linkplain ExecutableElement#getSimpleName static
     * initializer}, the name "{@code <clinit>}" is returned.
     *
     * If it represents an {@linkplain TypeElement#getSimpleName
     * anonymous class} or {@linkplain ExecutableElement#getSimpleName
     * instance initializer}, an <a href=Name.html#empty_name>empty
     * name</a> is returned.
     *
     * @see PackageElement#getSimpleName
     * @see ExecutableElement#getSimpleName
     * @see TypeElement#getSimpleName
     * @see VariableElement#getSimpleName
     * @see ModuleElement#getSimpleName
     * @see RecordComponentElement#getSimpleName
     * @revised 9
     */
    Name getSimpleName();

    /**
     * Returns the innermost element
     * within which this element is, loosely speaking, enclosed.
     * <ul>
     * <li> If this element is one whose declaration is lexically enclosed
     * immediately within the declaration of another element, that other
     * element is returned.
     *
     * <li> If this is a {@linkplain TypeElement#getEnclosingElement
     * top-level class or interface}, its package is returned.
     *
     * <li> If this is a {@linkplain
     * PackageElement#getEnclosingElement package}, its module is
     * returned if such a module exists. Otherwise, {@code null} is returned.
     *
     * <li> If this is a {@linkplain
     * TypeParameterElement#getEnclosingElement type parameter},
     * {@linkplain TypeParameterElement#getGenericElement the
     * generic element} of the type parameter is returned.
     *
     * <li> If this is a {@linkplain
     * VariableElement#getEnclosingElement method or constructor
     * parameter}, {@linkplain ExecutableElement the executable
     * element} which declares the parameter is returned.
     *
     * <li> If this is a {@linkplain
     * RecordComponentElement#getEnclosingElement record component},
     * {@linkplain TypeElement the record class} which declares the
     * record component is returned.
     *
     * <li> If this is a {@linkplain ModuleElement#getEnclosingElement
     * module}, {@code null} is returned.
     *
     * </ul>
     *
     * @return the enclosing element, or {@code null} if there is none
     * @see Elements#getPackageOf
     * @revised 9
     */
    Element getEnclosingElement();

    /**
     * Returns the elements that are, loosely speaking, directly
     * enclosed by this element.
     *
     * A {@linkplain TypeElement#getEnclosedElements class or
     * interface} is considered to enclose the fields, methods,
     * constructors, record components, and member classes and interfaces that it directly declares.
     *
     * A {@linkplain PackageElement#getEnclosedElements package}
     * encloses the top-level classes and interfaces within it, but is
     * not considered to enclose subpackages.
     *
     * A {@linkplain ModuleElement#getEnclosedElements module}
     * encloses packages within it.
     *
     * Enclosed elements may include implicitly declared {@linkplain
     * Elements.Origin#MANDATED mandated} elements.
     *
     * Other kinds of elements are not currently considered to enclose
     * any elements; however, that may change as this API or the
     * programming language evolves.
     *
     * @apiNote Elements of certain kinds can be isolated using
     * methods in {@link ElementFilter}.
     *
     * @return the enclosed elements, or an empty list if none
     * @see TypeElement#getEnclosedElements
     * @see PackageElement#getEnclosedElements
     * @see ModuleElement#getEnclosedElements
     * @see Elements#getAllMembers
     * @jls 8.8.9 Default Constructor
     * @jls 8.9 Enum Classes
     * @revised 9
     */
    List<? extends Element> getEnclosedElements();

    /**
     * {@return {@code true} if the argument represents the same
     * element as {@code this}, or {@code false} otherwise}
     *
     * @apiNote The identity of an element involves implicit state
     * not directly accessible from the element's methods, including
     * state about the presence of unrelated types.  Element objects
     * created by different implementations of these interfaces should
     * <i>not</i> be expected to be equal even if &quot;the same&quot;
     * element is being modeled; this is analogous to the inequality
     * of {@code Class} objects for the same class file loaded through
     * different class loaders.
     *
     * @param obj  the object to be compared with this element
     */
    @Override
    boolean equals(Object obj);

    /**
     * Obeys the general contract of {@link Object#hashCode Object.hashCode}.
     *
     * @see #equals
     */
    @Override
    int hashCode();

    /**
     * {@inheritDoc}
     *
     * <p>To get inherited annotations as well, use {@link
     * Elements#getAllAnnotationMirrors(Element)
     * getAllAnnotationMirrors}.
     *
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     *
     * @since 1.6
     */
    @Override
    List<? extends AnnotationMirror> getAnnotationMirrors();

    /**
     * {@inheritDoc}
     *
     * <p>Note that any annotation returned by this method is a
     * declaration annotation.
     *
     * @since 1.6
     */
    @Override
    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    /**
     * {@inheritDoc}
     *
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     *
     * @since 8
     */
    @Override
    <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType);

    /**
     * Applies a visitor to this element.
     *
     * @param <R> the return type of the visitor's methods
     * @param <P> the type of the additional parameter to the visitor's methods
     * @param v   the visitor operating on this element
     * @param p   additional parameter to the visitor
     * @return a visitor-specified result
     */
    <R, P> R accept(ElementVisitor<R, P> v, P p);
}
