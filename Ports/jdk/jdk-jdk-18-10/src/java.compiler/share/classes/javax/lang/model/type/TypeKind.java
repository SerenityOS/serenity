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


/**
 * The kind of a type mirror.
 *
 * <p>Note that it is possible additional type kinds will be added to
 * accommodate new, currently unknown, language structures added to
 * future versions of the Java programming language.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see TypeMirror
 * @since 1.6
 */
public enum TypeKind {
    /**
     * The primitive type {@code boolean}.
     */
    BOOLEAN,

    /**
     * The primitive type {@code byte}.
     */
    BYTE,

    /**
     * The primitive type {@code short}.
     */
    SHORT,

    /**
     * The primitive type {@code int}.
     */
    INT,

    /**
     * The primitive type {@code long}.
     */
    LONG,

    /**
     * The primitive type {@code char}.
     */
    CHAR,

    /**
     * The primitive type {@code float}.
     */
    FLOAT,

    /**
     * The primitive type {@code double}.
     */
    DOUBLE,

    /**
     * The pseudo-type corresponding to the keyword {@code void}.
     * @see NoType
     */
    VOID,

    /**
     * A pseudo-type used where no actual type is appropriate.
     * @see NoType
     */
    NONE,

    /**
     * The null type.
     */
    NULL,

    /**
     * An array type.
     */
    ARRAY,

    /**
     * A class or interface type.
     */
    DECLARED,

    /**
     * A class or interface type that could not be resolved.
     */
    ERROR,

    /**
     * A type variable.
     */
    TYPEVAR,

    /**
     * A wildcard type argument.
     */
    WILDCARD,

    /**
     * A pseudo-type corresponding to a package element.
     * @see NoType
     */
    PACKAGE,

    /**
     * A method, constructor, or initializer.
     */
    EXECUTABLE,

    /**
     * An implementation-reserved type.
     * This is not the type you are looking for.
     */
    OTHER,

    /**
      * A union type.
      *
      * @since 1.7
      */
    UNION,

    /**
      * An intersection type.
      *
      * @since 1.8
      */
    INTERSECTION,

    /**
     * A pseudo-type corresponding to a module element.
     * @see NoType
     * @since 9
     */
    MODULE;

    /**
     * {@return {@code true} if this kind corresponds to a primitive
     * type and {@code false} otherwise}
     */
    public boolean isPrimitive() {
        switch(this) {
        case BOOLEAN:
        case BYTE:
        case SHORT:
        case INT:
        case LONG:
        case CHAR:
        case FLOAT:
        case DOUBLE:
            return true;

        default:
            return false;
        }
    }
}
