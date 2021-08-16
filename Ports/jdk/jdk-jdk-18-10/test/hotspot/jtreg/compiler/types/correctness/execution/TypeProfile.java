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

package compiler.types.correctness.execution;

import compiler.types.correctness.hierarchies.TypeHierarchy;
import compiler.types.correctness.scenarios.Scenario;

/**
 * Profile type execution scenario. Executes tester method
 * in a loop without any manipulation with types or instances.
 */
public class TypeProfile<T extends TypeHierarchy.I, R> implements Execution<T, R> {
    /** Number of test method execution to make it profiled and compiled */
    private final static int PROFILE_THRESHOLD = 100000;

    /**
     * Makes scenario code be profiled and compiled
     * @param scenario Test scenario
     */
    @Override
    public void execute(Scenario<T, R> scenario) {
        R result = null;
        T prof = scenario.getProfiled();
        T confl = scenario.getConflict();

        for (int i = 0; i < PROFILE_THRESHOLD; i++) {
            result = methodNotToCompile(scenario, prof);
        }
        scenario.check(result, prof);

        result = methodNotToCompile(scenario, confl);
        scenario.check(result, confl);
    }

    protected R methodNotToCompile(Scenario<T, R> scenario, T t) {
        return scenario.run(t);
    }
}
