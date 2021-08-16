/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.util;

import java.util.Objects;

/**
 * Triple - a three element tuple
 *
 * @param <F> first element type
 * @param <S> second element type
 * @param <T> third element type
 */
public class Triple<F, S, T> {
    private final Pair<F, Pair<S, T>> container;

    /**
     * Constructor
     *
     * @param first  first element of the triple
     * @param second second element of the triple
     * @param third  third element of the triple
     */
    public Triple(F first, S second, T third) {
        container = new Pair<>(first, new Pair<>(second, third));
    }

    /**
     * Gets first element of the triple
     */
    public F getFirst() {
        return container.first;
    }

    /**
     * Gets second element of the triple
     */
    public S getSecond() {
        return container.second.first;
    }

    /**
     * Gets third element of the triple
     */
    public T getThird() {
        return container.second.second;
    }

    @Override
    public int hashCode() {
        return container.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Triple<?, ?, ?>) {
            Triple<?, ?, ?> objTriple = (Triple<?, ?, ?>) obj;
            return Objects.equals(container.first, objTriple.container.first)
                    && Objects.equals(container.second,
                    objTriple.container.second);
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + getFirst() + " : " + getSecond() + " : " + getThird() + ")";
    }
}
