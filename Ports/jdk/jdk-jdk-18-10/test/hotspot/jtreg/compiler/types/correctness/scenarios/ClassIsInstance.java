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

/**
 * Tests {@link Class#isInstance(Object)}
 */
public class ClassIsInstance<T extends TypeHierarchy.I> extends Scenario<T, Integer> {
    private final Class<?> baseClass;

    public ClassIsInstance(ProfilingType profilingType,
                           TypeHierarchy<? extends T, ? extends T> hierarchy) {
        super("ClassIsInstance", profilingType, hierarchy);
        this.baseClass = hierarchy.getClassM();
    }

    @Override
    public Integer run(T obj) {
        switch (profilingType) {
            case RETURN:
                T t = collectReturnType(obj);
                if (baseClass.isInstance(t)) {
                    return inlinee(t);
                }
                return TypeHierarchy.TEMP;
            case ARGUMENTS:
                field = obj;
                if (baseClass.isInstance(field)) {
                    return inlinee(field);
                }
                return TypeHierarchy.TEMP;
            case PARAMETERS:
                if (baseClass.isInstance(obj)) {
                    return inlinee(obj);
                }
                return TypeHierarchy.TEMP;
        }
        throw new RuntimeException("Should not reach here");
    }

    public int inlinee(T obj) {
        return obj.m();
    }

    @Override
    public void check(Integer result, T orig) {
        if (baseClass.isInstance(orig)) {
            Asserts.assertEquals(result, orig.m(), "Results are not equal for base class");
        } else {
            Asserts.assertEquals(result, TypeHierarchy.TEMP, "Result differs from expected");
        }
    }
}
