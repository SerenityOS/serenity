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

/**
 * The {@code kind} of an element.
 *
 * <p>Note that it is possible additional element kinds will be added
 * to accommodate new, currently unknown, language structures added to
 * future versions of the Java programming language.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see Element
 * @since 1.6
 */
public enum ElementKind {

    /** A package. */
    PACKAGE,

    // Declared types
    /** An enum class. */
    ENUM,
    /**
     * A class not described by a more specific kind (like {@code
     * ENUM} or {@code RECORD}).
     */
    CLASS,

    /** An annotation interface. (Formerly known as an annotation type.) */
    ANNOTATION_TYPE,
    /**
     * An interface not described by a more specific kind (like
     * {@code ANNOTATION_TYPE}).
     */
    INTERFACE,

    // Variables
    /** An enum constant. */
    ENUM_CONSTANT,
    /**
     * A field not described by a more specific kind (like
     * {@code ENUM_CONSTANT}).
     */
    FIELD,
    /** A parameter of a method or constructor. */
    PARAMETER,
    /** A local variable. */
    LOCAL_VARIABLE,
    /** A parameter of an exception handler. */
    EXCEPTION_PARAMETER,

    // Executables
    /** A method. */
    METHOD,
    /** A constructor. */
    CONSTRUCTOR,
    /** A static initializer. */
    STATIC_INIT,
    /** An instance initializer. */
    INSTANCE_INIT,

    /** A type parameter. */
    TYPE_PARAMETER,

    /**
     * An implementation-reserved element.  This is not the element
     * you are looking for.
     */
    OTHER,

    // Constants added since initial release

    /**
     * A resource variable.
     * @since 1.7
     */
     RESOURCE_VARIABLE,

    /**
     * A module.
     * @since 9
     */
     MODULE,

    /**
     * A record class.
     * @since 16
     */
    RECORD,

    /**
     * A record component of a {@code record}.
     * @since 16
     */
    RECORD_COMPONENT,

    /**
     * A binding variable in a pattern.
     * @since 16
     */
    BINDING_VARIABLE;

    /**
     * Returns {@code true} if this is a kind of class:
     * either {@code CLASS} or {@code ENUM} or {@code RECORD}.
     *
     * @return {@code true} if this is a kind of class
     */
    @SuppressWarnings("preview")
    public boolean isClass() {
        return this == CLASS || this == ENUM || this == RECORD;
    }

    /**
     * Returns {@code true} if this is a kind of interface:
     * either {@code INTERFACE} or {@code ANNOTATION_TYPE}.
     *
     * @return {@code true} if this is a kind of interface
     */
    public boolean isInterface() {
        return this == INTERFACE || this == ANNOTATION_TYPE;
    }

    /**
     * Returns {@code true} if this is a kind of field:
     * either {@code FIELD} or {@code ENUM_CONSTANT}.
     *
     * @return {@code true} if this is a kind of field
     */
    public boolean isField() {
        return this == FIELD || this == ENUM_CONSTANT;
    }
}
