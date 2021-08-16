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

package javax.lang.model.type;

import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.*;
import javax.lang.model.util.Types;

/**
 * Represents a type in the Java programming language.
 * Types include primitive types, declared types (class and interface types),
 * array types, type variables, and the null type.
 * Also represented are wildcard type arguments, the signature and
 * return types of executables, and pseudo-types corresponding to
 * packages, modules, and the keyword {@code void}.
 *
 * <p> Types should be compared using the utility methods in {@link
 * Types}.  There is no guarantee that any particular type will always
 * be represented by the same object.
 *
 * <p> To implement operations based on the class of an {@code
 * TypeMirror} object, either use a {@linkplain TypeVisitor visitor}
 * or use the result of the {@link #getKind} method.  Using {@code
 * instanceof} is <em>not</em> necessarily a reliable idiom for
 * determining the effective class of an object in this modeling
 * hierarchy since an implementation may choose to have a single
 * object implement multiple {@code TypeMirror} subinterfaces.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see Element
 * @see Types
 * @jls 4.1 The Kinds of Types and Values
 * @jls 4.2 Primitive Types and Values
 * @jls 4.3 Reference Types and Values
 * @jls 4.4 Type Variables
 * @jls 4.5 Parameterized Types
 * @jls 4.8 Raw Types
 * @jls 4.9 Intersection Types
 * @jls 10.1 Array Types
 * @since 1.6
 */
public interface TypeMirror extends javax.lang.model.AnnotatedConstruct {

    /**
     * {@return the {@code kind} of this type}
     *
     * <ul>
     *
     * <li> The kind of a {@linkplain PrimitiveType primitive type} is
     * one of the kinds for which {@link TypeKind#isPrimitive} returns
     * {@code true}.
     *
     * <li> The kind of a {@linkplain NullType null type} is {@link
     * TypeKind#NULL NULL}.
     *
     * <li> The kind of an {@linkplain ArrayType array type} is {@link
     * TypeKind#ARRAY ARRAY}.
     *
     * <li> The kind of a {@linkplain DeclaredType declared type} is
     * {@link TypeKind#DECLARED DECLARED}.
     *
     * <li> The kind of an {@linkplain ErrorType error type} is {@link
     * TypeKind#ERROR ERROR}.
     *
     * <li> The kind of a {@linkplain TypeVariable type variable} is
     * {@link TypeKind#TYPEVAR TYPEVAR}.
     *
     * <li> The kind of a {@linkplain WildcardType wildcard type} is
     * {@link TypeKind#WILDCARD WILDCARD}.
     *
     * <li> The kind of an {@linkplain ExecutableType executable type}
     * is {@link TypeKind#EXECUTABLE EXECUTABLE}.
     *
     * <li> The kind of a {@linkplain NoType pseudo-type} is one
     * of {@link TypeKind#VOID VOID}, {@link TypeKind#PACKAGE
     * PACKAGE}, {@link TypeKind#MODULE MODULE}, or {@link
     * TypeKind#NONE NONE}.
     *
     * <li> The kind of a {@linkplain UnionType union type} is {@link
     * TypeKind#UNION UNION}.
     *
     * <li> The kind of an {@linkplain IntersectionType intersection
     * type} is {@link TypeKind#INTERSECTION INTERSECTION}.
     *
     * </ul>
     */
    TypeKind getKind();

    /**
     * Obeys the general contract of {@link Object#equals Object.equals}.
     * This method does not, however, indicate whether two types represent
     * the same type.
     * Semantic comparisons of type equality should instead use
     * {@link Types#isSameType(TypeMirror, TypeMirror)}.
     * The results of {@code t1.equals(t2)} and
     * {@code Types.isSameType(t1, t2)} may differ.
     *
     * @param obj  the object to be compared with this type
     * @return {@code true} if the specified object is equal to this one
     */
    boolean equals(Object obj);

    /**
     * Obeys the general contract of {@link Object#hashCode Object.hashCode}.
     *
     * @see #equals
     */
    int hashCode();

    /**
     * Returns an informative string representation of this type.  If
     * possible, the string should be of a form suitable for
     * representing this type in source code.  Any names embedded in
     * the result are qualified if possible.
     *
     * @return a string representation of this type
     */
    String toString();

    /**
     * {@inheritDoc}
     *
     * <p>Note that any annotations returned by this method are type
     * annotations.
     *
     * @since 8
     */
    @Override
    List<? extends AnnotationMirror> getAnnotationMirrors();

    /**
     * {@inheritDoc}
     *
     * <p>Note that any annotation returned by this method is a type
     * annotation.
     *
     * @since 8
     */
    @Override
    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    /**
     * {@inheritDoc}
     *
     * <p>Note that any annotations returned by this method are type
     * annotations.
     *
     * @since 8
     */
    @Override
    <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType);

    /**
     * Applies a visitor to this type.
     *
     * @param <R> the return type of the visitor's methods
     * @param <P> the type of the additional parameter to the visitor's methods
     * @param v   the visitor operating on this type
     * @param p   additional parameter to the visitor
     * @return a visitor-specified result
     */
    <R, P> R accept(TypeVisitor<R, P> v, P p);
}
