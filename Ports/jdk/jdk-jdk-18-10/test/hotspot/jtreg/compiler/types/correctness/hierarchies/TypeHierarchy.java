/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package compiler.types.correctness.hierarchies;

/**
 * Type hierarchy contains classes the type profiling and speculation are tested with
 */
public abstract class TypeHierarchy<M extends TypeHierarchy.I, N extends TypeHierarchy.I> {
    // Magic numbers
    public static final int ANSWER = 42;
    public static final int TEMP = 451;
    public static final int YEAR = 1984;

    private final M m;
    private final N n;
    private final Class<M> classM;
    private final Class<N> classN;

    protected TypeHierarchy(M m, N n, Class<M> classM, Class<N> classN) {
        this.m = m;
        this.n = n;
        this.classM = classM;
        this.classN = classN;
    }

    public final M getM() {
        return m;
    }

    public final N getN() {
        return n;
    }

    public final Class<M> getClassM() {
        return classM;
    }

    public final Class<N> getClassN() {
        return classN;
    }

    public interface I {
        int m();
    }

    public static class A implements I {
        @Override
        public int m() {
            return TypeHierarchy.ANSWER;
        }
    }
}
