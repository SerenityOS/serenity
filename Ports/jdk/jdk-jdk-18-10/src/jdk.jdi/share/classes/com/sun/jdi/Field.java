/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi;

/**
 * A class or instance variable in the target VM.
 * See {@link TypeComponent}
 * for general information about Field and Method mirrors.
 *
 * @see ObjectReference
 * @see ReferenceType
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface Field extends TypeComponent, Comparable<Field> {

    /**
     * Returns a text representation of the type
     * of this field.
     * Where the type is the type specified in the declaration
     * of this field.
     * <P>
     * This type name is always available even if
     * the type has not yet been created or loaded.
     *
     * @return a String representing the
     * type of this field.
     */
    String typeName();

    /**
     * Returns the type of this field.
     * Where the type is the type specified in the declaration
     * of this field.
     * <P>
     * For example, if a target class defines:
     * <PRE>
     *    short s;
     *    Date d;
     *    byte[] ba;</PRE>
     * And the JDI client defines these {@code Field} objects:
     * <PRE>
     *    Field sField = targetClass.fieldByName("s");
     *    Field dField = targetClass.fieldByName("d");
     *    Field baField = targetClass.fieldByName("ba");</PRE>
     * to mirror the corresponding fields, then {@code sField.type()}
     * is a {@link ShortType}, {@code dField.type()} is the
     * {@link ReferenceType} for {@code java.util.Date} and
     * {@code ((ArrayType)(baField.type())).componentType()} is a
     * {@link ByteType}.
     * <P>
     * Note: if the type of this field is a reference type (class,
     * interface, or array) and it has not been created or loaded
     * by the declaring type's class loader - that is,
     * {@link TypeComponent#declaringType declaringType()}
     * {@code .classLoader()},
     * then ClassNotLoadedException will be thrown.
     * Also, a reference type may have been loaded but not yet prepared,
     * in which case the type will be returned
     * but attempts to perform some operations on the returned type
     * (e.g. {@link ReferenceType#fields() fields()}) will throw
     * a {@link ClassNotPreparedException}.
     * Use {@link ReferenceType#isPrepared()} to determine if
     * a reference type is prepared.
     *
     * @see Type
     * @return the {@link Type} of this field.
     * @throws ClassNotLoadedException if the type has not yet been loaded
     * or created through the appropriate class loader.
     */
    Type type() throws ClassNotLoadedException;

    /**
     * Determine if this is a transient field.
     *
     * @return {@code true} if this field is transient; {@code false} otherwise.
     */
    boolean isTransient();

    /**
     * Determine if this is a volatile field.
     *
     * @return {@code true} if this field is volatile; {@code false} otherwise.
     */
    boolean isVolatile();

    /**
     * Determine if this is a field that represents an enum constant.
     * @return {@code true} if this field represents an enum constant;
     * {@code false} otherwise.
     */
    boolean isEnumConstant();

    /**
     * Compares the specified Object with this field for equality.
     *
     * @return {@code true} if the Object is a Field and if both
     * mirror the same field (declared in the same class or interface, in
     * the same VM).
     */
    boolean equals(Object obj);

    /**
     * Returns the hash code value for this Field.
     *
     * @return the integer hash code.
     */
    int hashCode();
}
