/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.vm.ci.meta;

import static jdk.vm.ci.meta.MetaUtil.internalNameToJava;

/**
 * Represents a resolved or unresolved type. Types include primitives, objects, {@code void}, and
 * arrays thereof.
 */
public interface JavaType {

    /**
     * Returns the name of this type in internal form. The following are examples of strings
     * returned by this method:
     *
     * <pre>
     *     "Ljava/lang/Object;"
     *     "I"
     *     "[[B"
     * </pre>
     */
    String getName();

    /**
     * Returns an unqualified name of this type.
     *
     * <pre>
     *     "Object"
     *     "Integer"
     * </pre>
     */
    default String getUnqualifiedName() {
        String name = getName();
        if (name.indexOf('/') != -1) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }
        if (name.endsWith(";")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    /**
     * Checks whether this type is an array class.
     *
     * @return {@code true} if this type is an array class
     */
    default boolean isArray() {
        return getComponentType() != null;
    }

    /**
     * For array types, gets the type of the components, or {@code null} if this is not an array
     * type. This method is analogous to {@link Class#getComponentType()}.
     */
    JavaType getComponentType();

    /**
     * Gets the elemental type for this given type. The elemental type is the corresponding zero
     * dimensional type of an array type. For example, the elemental type of {@code int[][][]} is
     * {@code int}. A non-array type is its own elemental type.
     */
    default JavaType getElementalType() {
        JavaType t = this;
        while (t.getComponentType() != null) {
            t = t.getComponentType();
        }
        return t;
    }

    /**
     * Gets the array class type representing an array with elements of this type.
     */
    JavaType getArrayClass();

    /**
     * Gets the {@link JavaKind} of this type.
     */
    JavaKind getJavaKind();

    /**
     * Resolves this type to a {@link ResolvedJavaType}.
     *
     * @param accessingClass the context of resolution (must not be null)
     * @return the resolved Java type
     * @throws LinkageError if the resolution failed
     * @throws NullPointerException if {@code accessingClass} is {@code null}
     */
    ResolvedJavaType resolve(ResolvedJavaType accessingClass);

    /**
     * Gets the Java programming language name for this type. The following are examples of strings
     * returned by this method:
     *
     * <pre>
     *      java.lang.Object
     *      int
     *      boolean[][]
     * </pre>
     *
     * @return the Java name corresponding to this type
     */
    default String toJavaName() {
        return internalNameToJava(getName(), true, false);
    }

    /**
     * Gets the Java programming language name for this type. The following are examples of strings
     * returned by this method:
     *
     * <pre>
     *     qualified == true:
     *         java.lang.Object
     *         int
     *         boolean[][]
     *     qualified == false:
     *         Object
     *         int
     *         boolean[][]
     * </pre>
     *
     * @param qualified specifies if the package prefix of this type should be included in the
     *            returned name
     * @return the Java name corresponding to this type
     */
    default String toJavaName(boolean qualified) {
        JavaKind kind = getJavaKind();
        if (kind == JavaKind.Object) {
            return internalNameToJava(getName(), qualified, false);
        }
        return getJavaKind().getJavaName();
    }

    /**
     * Returns this type's name in the same format as {@link Class#getName()}.
     */
    default String toClassName() {
        return internalNameToJava(getName(), true, true);
    }
}
