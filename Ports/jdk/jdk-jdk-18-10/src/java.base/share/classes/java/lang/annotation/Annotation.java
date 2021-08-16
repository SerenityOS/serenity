/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.annotation;

/**
 * The common interface extended by all annotation interfaces.  Note that an
 * interface that manually extends this one does <i>not</i> define
 * an annotation interface.  Also note that this interface does not itself
 * define an annotation interface.
 *
 * More information about annotation interfaces can be found in section
 * {@jls 9.6} of <cite>The Java Language Specification</cite>.
 *
 * The {@link java.lang.reflect.AnnotatedElement} interface discusses
 * compatibility concerns when evolving an annotation interface from being
 * non-repeatable to being repeatable.
 *
 * @author  Josh Bloch
 * @since   1.5
 */
public interface Annotation {
    /**
     * Returns true if the specified object represents an annotation
     * that is logically equivalent to this one.  In other words,
     * returns true if the specified object is an instance of the same
     * annotation interface as this instance, all of whose members are equal
     * to the corresponding member of this annotation, as defined below:
     * <ul>
     *    <li>Two corresponding primitive typed members whose values are
     *    {@code x} and {@code y} are considered equal if {@code x == y},
     *    unless their type is {@code float} or {@code double}.
     *
     *    <li>Two corresponding {@code float} members whose values
     *    are {@code x} and {@code y} are considered equal if
     *    {@code Float.valueOf(x).equals(Float.valueOf(y))}.
     *    (Unlike the {@code ==} operator, NaN is considered equal
     *    to itself, and {@code 0.0f} unequal to {@code -0.0f}.)
     *
     *    <li>Two corresponding {@code double} members whose values
     *    are {@code x} and {@code y} are considered equal if
     *    {@code Double.valueOf(x).equals(Double.valueOf(y))}.
     *    (Unlike the {@code ==} operator, NaN is considered equal
     *    to itself, and {@code 0.0} unequal to {@code -0.0}.)
     *
     *    <li>Two corresponding {@code String}, {@code Class}, enum, or
     *    annotation typed members whose values are {@code x} and {@code y}
     *    are considered equal if {@code x.equals(y)}.  (Note that this
     *    definition is recursive for annotation typed members.)
     *
     *    <li>Two corresponding array typed members {@code x} and {@code y}
     *    are considered equal if {@code Arrays.equals(x, y)}, for the
     *    appropriate overloading of {@link java.util.Arrays#equals Arrays.equals}.
     * </ul>
     *
     * @return true if the specified object represents an annotation
     *     that is logically equivalent to this one, otherwise false
     */
    boolean equals(Object obj);

    /**
     * Returns the hash code of this annotation.
     *
     * <p>The hash code of an annotation is the sum of the hash codes
     * of its members (including those with default values).
     *
     * The hash code of an annotation member is (127 times the hash code
     * of the member-name as computed by {@link String#hashCode()}) XOR
     * the hash code of the member-value.
     * The hash code of a member-value depends on its type as defined below:
     * <ul>
     * <li>The hash code of a primitive value <i>{@code v}</i> is equal to
     *     <code><i>WrapperType</i>.valueOf(<i>v</i>).hashCode()</code>, where
     *     <i>{@code WrapperType}</i> is the wrapper type corresponding
     *     to the primitive type of <i>{@code v}</i> ({@link Byte},
     *     {@link Character}, {@link Double}, {@link Float}, {@link Integer},
     *     {@link Long}, {@link Short}, or {@link Boolean}).
     *
     * <li>The hash code of a string, enum, class, or annotation member-value
     *     <i>{@code v}</i> is computed as by calling
     *     <code><i>v</i>.hashCode()</code>.  (In the case of annotation
     *     member values, this is a recursive definition.)
     *
     * <li>The hash code of an array member-value is computed by calling
     *     the appropriate overloading of
     *     {@link java.util.Arrays#hashCode(long[]) Arrays.hashCode}
     *     on the value.  (There is one overloading for each primitive
     *     type, and one for object reference types.)
     * </ul>
     *
     * @return the hash code of this annotation
     */
    int hashCode();

    /**
     * Returns a string representation of this annotation.  The details
     * of the representation are implementation-dependent, but the following
     * may be regarded as typical:
     * <pre>
     *   &#064;com.example.Name(first="Duke", middle="of", last="Java")
     * </pre>
     *
     * @return a string representation of this annotation
     */
    String toString();

    /**
     * Returns the annotation interface of this annotation.
     *
     * @apiNote Implementation-dependent classes are used to provide
     * the implementations of annotations. Therefore, calling {@link
     * Object#getClass getClass} on an annotation will return an
     * implementation-dependent class. In contrast, this method will
     * reliably return the annotation interface of the annotation.
     *
     * @return the annotation interface of this annotation
     * @see Enum#getDeclaringClass
     */
    Class<? extends Annotation> annotationType();
}
