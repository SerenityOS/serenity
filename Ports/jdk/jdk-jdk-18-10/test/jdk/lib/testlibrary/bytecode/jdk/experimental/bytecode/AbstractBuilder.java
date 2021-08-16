/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

/**
 * Base builder.
 *
 * @param <S> the type of the symbol representation
 * @param <T> the type of type descriptors representation
 * @param <E> the type of pool entries
 * @param <D> the type of this builder
 */
public class AbstractBuilder<S, T, E, D extends AbstractBuilder<S, T, E, D>> {
    /**
     * The helper to build the constant pool.
     */
    protected final PoolHelper<S, T, E> poolHelper;

    /**
     * The helper to use to manipulate type descriptors.
     */
    protected final TypeHelper<S, T> typeHelper;

    /**
     * Create a builder.
     *
     * @param poolHelper the helper to build the constant pool
     * @param typeHelper the helper to use to manipulate type descriptors
     */
    AbstractBuilder(PoolHelper<S, T, E> poolHelper, TypeHelper<S, T> typeHelper) {
        this.poolHelper = poolHelper;
        this.typeHelper = typeHelper;
    }

    @SuppressWarnings("unchecked")
    D thisBuilder() {
        return (D) this;
    }
}
