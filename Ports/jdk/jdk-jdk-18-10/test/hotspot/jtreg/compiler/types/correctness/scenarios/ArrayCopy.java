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

package compiler.types.correctness.scenarios;

import compiler.types.correctness.hierarchies.TypeHierarchy;

import java.util.Arrays;

/**
 * Tests System.arraycopy()
 */
public class ArrayCopy extends ArrayScenario {
    public ArrayCopy(ProfilingType profilingType,
                     TypeHierarchy<? extends TypeHierarchy.I, ? extends TypeHierarchy.I> hierarchy) {
        super("ArrayCopy", profilingType, hierarchy);
    }

    /**
     * @param obj is used to fill arrays
     * @return the same obj
     */
    @Override
    public TypeHierarchy.I run(TypeHierarchy.I obj) {
        switch (profilingType) {
            case RETURN:
                TypeHierarchy.I t = collectReturnType(obj);
                Arrays.fill(array, t);
                System.arraycopy(array, 0, matrix[0], 0, array.length);
                return array[0];
            case ARGUMENTS:
                field = obj;
                Arrays.fill(array, field);
                System.arraycopy(array, 0, matrix[0], 0, array.length);
                return array[0];
            case PARAMETERS:
                Arrays.fill(array, obj);
                System.arraycopy(array, 0, matrix[0], 0, array.length);
                return array[0];
        }
        throw new RuntimeException("Should not reach here");
    }
}
