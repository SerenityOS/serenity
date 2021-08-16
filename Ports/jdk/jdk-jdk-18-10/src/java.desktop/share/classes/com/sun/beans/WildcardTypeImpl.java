/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.beans;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * This class implements {@link WildcardType WildcardType} compatibly with the JDK's
 * {@link sun.reflect.generics.reflectiveObjects.WildcardTypeImpl WildcardTypeImpl}.
 * Unfortunately we can't use the JDK's
 * {@link sun.reflect.generics.reflectiveObjects.WildcardTypeImpl WildcardTypeImpl} here as we do for
 * {@link sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl ParameterizedTypeImpl} and
 * {@link sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl GenericArrayTypeImpl},
 * because {@link sun.reflect.generics.reflectiveObjects.WildcardTypeImpl WildcardTypeImpl}'s
 * constructor takes parameters representing intermediate structures obtained during class-file parsing.
 * We could reconstruct versions of those structures but it would be more trouble than it's worth.
 *
 * @since 1.7
 *
 * @author Eamonn McManus
 * @author Sergey Malenkov
 */
final class WildcardTypeImpl implements WildcardType {
    private final Type[] upperBounds;
    private final Type[] lowerBounds;

    /**
     * Creates a wildcard type with the requested bounds.
     * Note that the array arguments are not cloned
     * because instances of this class are never constructed
     * from outside the containing package.
     *
     * @param upperBounds  the array of types representing
     *                     the upper bound(s) of this type variable
     * @param lowerBounds  the array of types representing
     *                     the lower bound(s) of this type variable
     */
    WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
        this.upperBounds = upperBounds;
        this.lowerBounds = lowerBounds;
    }

    /**
     * Returns an array of {@link Type Type} objects
     * representing the upper bound(s) of this type variable.
     * Note that if no upper bound is explicitly declared,
     * the upper bound is {@link Object Object}.
     *
     * @return an array of types representing
     *         the upper bound(s) of this type variable
     */
    public Type[] getUpperBounds() {
        return this.upperBounds.clone();
    }

    /**
     * Returns an array of {@link Type Type} objects
     * representing the lower bound(s) of this type variable.
     * Note that if no lower bound is explicitly declared,
     * the lower bound is the type of {@code null}.
     * In this case, a zero length array is returned.
     *
     * @return an array of types representing
     *         the lower bound(s) of this type variable
     */
    public Type[] getLowerBounds() {
        return this.lowerBounds.clone();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * It is implemented compatibly with the JDK's
     * {@link sun.reflect.generics.reflectiveObjects.WildcardTypeImpl WildcardTypeImpl}.
     *
     * @param object  the reference object with which to compare
     * @return {@code true} if this object is the same as the object argument;
     *         {@code false} otherwise
     * @see sun.reflect.generics.reflectiveObjects.WildcardTypeImpl#equals
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof WildcardType) {
            WildcardType type = (WildcardType) object;
            return Arrays.equals(this.upperBounds, type.getUpperBounds())
                && Arrays.equals(this.lowerBounds, type.getLowerBounds());
        }
        return false;
    }

    /**
     * Returns a hash code value for the object.
     * It is implemented compatibly with the JDK's
     * {@link sun.reflect.generics.reflectiveObjects.WildcardTypeImpl WildcardTypeImpl}.
     *
     * @return a hash code value for this object
     * @see sun.reflect.generics.reflectiveObjects.WildcardTypeImpl#hashCode
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.upperBounds)
             ^ Arrays.hashCode(this.lowerBounds);
    }

    /**
     * Returns a string representation of the object.
     * It is implemented compatibly with the JDK's
     * {@link sun.reflect.generics.reflectiveObjects.WildcardTypeImpl WildcardTypeImpl}.
     *
     * @return a string representation of the object
     * @see sun.reflect.generics.reflectiveObjects.WildcardTypeImpl#toString
     */
    @Override
    public String toString() {
        StringBuilder sb;
        Type[] bounds;
        if (this.lowerBounds.length == 0) {
            if (this.upperBounds.length == 0 || Object.class == this.upperBounds[0]) {
                return "?";
            }
            bounds = this.upperBounds;
            sb = new StringBuilder("? extends ");
        }
        else {
            bounds = this.lowerBounds;
            sb = new StringBuilder("? super ");
        }
        for (int i = 0; i < bounds.length; i++) {
            if (i > 0) {
                sb.append(" & ");
            }
            sb.append((bounds[i] instanceof Class)
                    ? ((Class) bounds[i]).getName()
                    : bounds[i].toString());
        }
        return sb.toString();
    }
}
