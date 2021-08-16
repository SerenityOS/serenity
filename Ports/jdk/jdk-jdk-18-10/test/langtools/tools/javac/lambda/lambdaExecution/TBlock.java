/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Performs operations upon an input object which may modify that object and/or
 * external state (other objects).
 *
 * <p>All block implementations are expected to:
 * <ul>
 * <li>When used for aggregate operations upon many elements blocks
 * should not assume that the {@code apply} operation will be called upon
 * elements in any specific order.</li>
 * </ul>
 *
 * @param <T> The type of input objects to {@code apply}.
 */
public interface TBlock<T> {

    /**
     * Performs operations upon the provided object which may modify that object
     * and/or external state.
     *
     * @param t an input object
     */
    void apply(T t);

    /**
     * Returns a Block which performs in sequence the {@code apply} methods of
     * multiple Blocks. This Block's {@code apply} method is performed followed
     * by the {@code apply} method of the specified Block operation.
     *
     * @param other an additional Block which will be chained after this Block
     * @return a Block which performs in sequence the {@code apply} method of
     * this Block and the {@code apply} method of the specified Block operation
     */
    public default TBlock<T> chain(TBlock<? super T> other) {
        return (T t) -> { apply(t); other.apply(t); };
    }
}
