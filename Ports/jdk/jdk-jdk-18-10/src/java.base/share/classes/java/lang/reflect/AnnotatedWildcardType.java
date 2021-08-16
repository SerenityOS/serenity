/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * {@code AnnotatedWildcardType} represents the potentially annotated use of a
 * wildcard type argument, whose upper or lower bounds may themselves represent
 * annotated uses of types.
 *
 * @jls 4.5.1 Type Arguments of Parameterized Types
 * @since 1.8
 */
public interface AnnotatedWildcardType extends AnnotatedType {

    /**
     * Returns the potentially annotated lower bounds of this wildcard type.
     * If no lower bound is explicitly declared, the lower bound is the
     * type of null. In this case, a zero length array is returned.
     *
     * @apiNote While to date a wildcard may have at most one lower
     * bound, callers of this method should be written to accommodate
     * multiple bounds.
     *
     * @return the potentially annotated lower bounds of this wildcard type or
     * an empty array if no lower bound is explicitly declared.
     * @see WildcardType#getLowerBounds()
     */
    AnnotatedType[] getAnnotatedLowerBounds();

    /**
     * Returns the potentially annotated upper bounds of this wildcard type.
     * If no upper bound is explicitly declared, the upper bound is
     * unannotated {@code Object}
     *
     * @apiNote While to date a wildcard may have at most one upper
     * bound, callers of this method should be written to accommodate
     * multiple bounds.
     *
     * @return the potentially annotated upper bounds of this wildcard type
     * @see WildcardType#getUpperBounds()
     */
    AnnotatedType[] getAnnotatedUpperBounds();

    /**
     * Returns the potentially annotated type that this type is a member of, if
     * this type represents a nested type. For example, if this type is
     * {@code @TA O<T>.I<S>}, return a representation of {@code @TA O<T>}.
     *
     * <p>Returns {@code null} for an {@code AnnotatedType} that is an instance
     *     of {@code AnnotatedWildcardType}.
     *
     * @return {@code null}
     *
     * @since 9
     */
    @Override
    AnnotatedType getAnnotatedOwnerType();
}
