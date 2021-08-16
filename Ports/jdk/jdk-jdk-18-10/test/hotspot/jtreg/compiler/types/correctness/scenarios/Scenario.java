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

/**
 * Test scenario
 *
 * @param <T> parameter type
 * @param <R> result type
 */
public abstract class Scenario<T extends TypeHierarchy.I, R> {

    private final String name;
    public final ProfilingType profilingType;
    public final TypeHierarchy <? extends T, ? extends T> hierarchy;
    protected volatile T field;

    /**
     * Constructor
     *
     * @param name          scenario name
     * @param profilingType tested profiling type
     * @param hierarchy     type hierarchy
     */
    protected Scenario(String name, ProfilingType profilingType,
                       TypeHierarchy<? extends T, ? extends T> hierarchy) {
        this.profilingType = profilingType;
        this.name = name + " # " + profilingType.name();
        this.hierarchy = hierarchy;
    }

    /**
     * Returns the object which should be used as a parameter
     * for the methods used for profile data
     *
     * @return profiled type object
     */
    public T getProfiled() {
        return hierarchy.getM();
    }

    /**
     * Returns the object which makes a conflict for a profiled data
     * when passed instead of {@linkplain Scenario#getProfiled}
     *
     * @return incompatible to profiled object
     */
    public T getConflict() {
        return hierarchy.getN();
    }

    /**
     * @return scenario name
     */
    public String getName() {
        return name;
    }

    /** Is this scenario applicable for a hierarchy it was constructed with */
    public boolean isApplicable() {
        return true;
    }

    /**
     * Runs test scenario
     *
     * @param t subject of the test
     * @return  result of the test invocation
     */
    public abstract R run(T t);

    /** Used for a return type profiling */
    protected final T collectReturnType(T t) {
        return t;
    }

    /**
     * Checks the result for R and T
     *
     * @param r result
     * @param t original
     * @throws java.lang.RuntimeException on result mismatch
     */
    public abstract void check(R r, T t);
}
