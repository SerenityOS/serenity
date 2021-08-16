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
 * Receiver at invokes profiling and speculation
 *
 * @param <T> parameter to be returned
 */
public class ReceiverAtInvokes<T extends TypeHierarchy.I> extends Scenario<T, Integer> {
    public ReceiverAtInvokes(ProfilingType profilingType,
                             TypeHierarchy<? extends T, ? extends T> hierarchy) {
        super("ReceiverAtInvokes", profilingType, hierarchy);
    }

    @Override
    public boolean isApplicable() {
        return hierarchy.getM() != null && hierarchy.getN() != null;
    }

    /**
     * Receiver profiling
     *
     * @param obj is a profiled parameter for the test
     * @return parameter casted to the type R
     */
    @Override
    public Integer run(T obj) {
        switch (profilingType) {
            case RETURN:
                T t = collectReturnType(obj);
                return inlinee(t);
            case ARGUMENTS:
                field = obj;
                return inlinee(field);
            case PARAMETERS:
                return inlinee(obj);
        }
        throw new RuntimeException("Should not reach here");
    }

    private Integer inlinee(T obj) {
        return obj.m(); // should be inlined
    }

    @Override
    public void check(Integer result, T orig) {
        Asserts.assertEquals(result, orig.m(), "Results mismatch");
    }
}
