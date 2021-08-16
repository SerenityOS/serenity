/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.reflectiveObjects;


import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.tree.FieldTypeSignature;
import sun.reflect.generics.visitor.Reifier;
import java.util.Arrays;
import java.util.StringJoiner;


/**
 * Implementation of WildcardType interface for core reflection.
 */
public class WildcardTypeImpl extends LazyReflectiveObjectGenerator
    implements WildcardType {

    /*
     * We are required to evaluate the bounds lazily, so we store them as ASTs
     * until we are first asked for them.  This also neatly solves the problem
     * with F-bounds - you can't reify them before the formal is defined.
     */

    /** The upper bounds.  Lazily converted from FieldTypeSignature[] to Type[]. */
    private volatile Object[] upperBounds;

    /** The lower bounds.  Lazily converted from FieldTypeSignature[] to Type[]. */
    private volatile Object[] lowerBounds;

    // constructor is private to enforce access through static factory
    private WildcardTypeImpl(FieldTypeSignature[] ubs,
                             FieldTypeSignature[] lbs,
                             GenericsFactory f) {
        super(f);
        upperBounds = ubs;
        lowerBounds = lbs;
    }

    /**
     * Factory method.
     * @param ubs - an array of ASTs representing the upper bounds for the type
     * variable to be created
     * @param lbs - an array of ASTs representing the lower bounds for the type
     * variable to be created
     * @param f - a factory that can be used to manufacture reflective
     * objects that represent the bounds of this wildcard type
     * @return a wild card type with the requested bounds and factory
     */
    public static WildcardTypeImpl make(FieldTypeSignature[] ubs,
                                        FieldTypeSignature[] lbs,
                                        GenericsFactory f) {
        return new WildcardTypeImpl(ubs, lbs, f);
    }

    /**
     * Returns an array of {@code Type} objects representing the upper
     * bound(s) of this type variable.  Note that if no upper bound is
     * explicitly declared, the upper bound is {@code Object}.
     *
     * <p>For each upper bound B :
     * <ul>
     *  <li>if B is a parameterized type or a type variable, it is created,
     *  (see {@link #ParameterizedType} for the details of the creation
     *  process for parameterized types).
     *  <li>Otherwise, B is resolved.
     * </ul>
     *
     * @return an array of Types representing the upper bound(s) of this
     *     type variable
     * @throws {@code TypeNotPresentException} if any of the
     *     bounds refers to a non-existent type declaration
     * @throws {@code MalformedParameterizedTypeException} if any of the
     *     bounds refer to a parameterized type that cannot be instantiated
     *     for any reason
     */
    public Type[] getUpperBounds() {
        Object[] value = upperBounds;
        if (value instanceof FieldTypeSignature[]) {
            value = reifyBounds((FieldTypeSignature[])value);
            upperBounds = value;
        }
        return (Type[])value.clone();
    }

    /**
     * Returns an array of {@code Type} objects representing the
     * lower bound(s) of this type variable.  Note that if no lower bound is
     * explicitly declared, the lower bound is the type of {@code null}.
     * In this case, a zero length array is returned.
     *
     * <p>For each lower bound B :
     * <ul>
     *   <li>if B is a parameterized type or a type variable, it is created,
     *   (see {@link #ParameterizedType} for the details of the creation
     *   process for parameterized types).
     *   <li>Otherwise, B is resolved.
     * </ul>
     *
     * @return an array of Types representing the lower bound(s) of this
     *     type variable
     * @throws {@code TypeNotPresentException} if any of the
     *     bounds refers to a non-existent type declaration
     * @throws {@code MalformedParameterizedTypeException} if any of the
     *     bounds refer to a parameterized type that cannot be instantiated
     *     for any reason
     */
    public Type[] getLowerBounds() {
        Object[] value = lowerBounds;
        if (value instanceof FieldTypeSignature[]) {
            value = reifyBounds((FieldTypeSignature[])value);
            lowerBounds = value;
        }
        return (Type[])value.clone();
    }

    public String toString() {
        Type[] lowerBounds = getLowerBounds();
        Type[] bounds = lowerBounds;
        StringBuilder sb = new StringBuilder();

        if (lowerBounds.length > 0)
            sb.append("? super ");
        else {
            Type[] upperBounds = getUpperBounds();
            if (upperBounds.length > 0 && !upperBounds[0].equals(Object.class) ) {
                bounds = upperBounds;
                sb.append("? extends ");
            } else
                return "?";
        }

        assert bounds.length > 0;

        StringJoiner sj = new StringJoiner(" & ");
        for(Type bound: bounds) {
            sj.add(bound.getTypeName());
        }
        sb.append(sj.toString());

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WildcardType) {
            WildcardType that = (WildcardType) o;
            return
                Arrays.equals(this.getLowerBounds(),
                              that.getLowerBounds()) &&
                Arrays.equals(this.getUpperBounds(),
                              that.getUpperBounds());
        } else
            return false;
    }

    @Override
    public int hashCode() {
        Type [] lowerBounds = getLowerBounds();
        Type [] upperBounds = getUpperBounds();

        return Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
    }
}
