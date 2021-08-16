/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.types.correctness.scenarios;

import compiler.types.correctness.hierarchies.TypeHierarchy;
import jdk.test.lib.Asserts;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 *  Base class for array scenarios
 */
public abstract class ArrayScenario extends Scenario<TypeHierarchy.I, TypeHierarchy.I> {
    protected final TypeHierarchy.I[] array;
    protected final TypeHierarchy.I[][] matrix;

    protected ArrayScenario(String name, ProfilingType profilingType,
                            TypeHierarchy<? extends TypeHierarchy.I, ? extends TypeHierarchy.I> hierarchy) {
        super(name, profilingType, hierarchy);
        final int x = 20;
        final int y = 10;

        TypeHierarchy.I prof = hierarchy.getM();
        TypeHierarchy.I confl = hierarchy.getN();

        this.array = (TypeHierarchy.I[]) Array.newInstance(hierarchy.getClassM(), y);
        Arrays.fill(array, prof);

        this.matrix = (TypeHierarchy.I[][]) Array.newInstance(hierarchy.getClassM(), x, y);
        for (int i = 0; i < x; i++) {
            this.matrix[i] = this.array;
        }

        Asserts.assertEquals(array.length, matrix[0].length, "Invariant");
    }

    @Override
    public boolean isApplicable() {
        return hierarchy.getClassM().isAssignableFrom(hierarchy.getClassN());
    }

    @Override
    public void check(TypeHierarchy.I res, TypeHierarchy.I orig) {
        Asserts.assertEquals(res, orig, "Check failed");
    }
}
