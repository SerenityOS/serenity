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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Executes test scenario using {@link MethodHandle#invoke(Object...)}.
 * Delegates execution to the given {@link Execution} by creating
 * new test scenario, see {@link Scenario}
 */
public class MethodHandleDelegate<T  extends TypeHierarchy.I, R> implements Execution<T, R> {
    private final Execution<T, R> delegate;

    public MethodHandleDelegate(Execution<T, R> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Scenario<T, R> scenario) {
        delegate.execute(new MHScenario<T, R>(scenario));
    }

    @Override
    public String getName() {
        return "MethodHandleDelegate # " + delegate.getName();
    }

    private static class MHScenario<T extends TypeHierarchy.I, R> extends Scenario<T, R> {
        private final Scenario<T, R> scenario;
        private static final MethodHandle METHOD_HANDLE_RUN;

        static {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType methodType = MethodType.methodType(Object.class, TypeHierarchy.I.class);

            try {
                METHOD_HANDLE_RUN = lookup.findVirtual(Scenario.class, "run", methodType);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                System.err.println("Failed to get target method run() with " + e);
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        /**
         * Constructor
         *
         * @param scenario test scenario to be executed
         */
        private MHScenario(Scenario<T, R> scenario) {
            super("MethodHandle::" + scenario.getName(), scenario.profilingType, scenario.hierarchy);
            this.scenario = scenario;
        }

        /**
         * Runs {@link Scenario#run(T)} with {@link MethodHandle#invoke(Object...)}
         *
         * @param t subject of the test
         * @return  result of the underlying {@link Scenario#run(T)} invocation
         */
        @SuppressWarnings("unchecked")
        @Override
        public R run(T t) {
            try {
                return (R) METHOD_HANDLE_RUN.invoke(scenario, t);
            } catch (Throwable thr) {
                System.err.println(scenario.getName()
                        + " failed to invoke target method run() with " + thr);
                throw new RuntimeException("Invocation failed", thr);
            }
        }

        @Override
        public void check(R r, T t) {
            scenario.check(r, t);
        }
    }
}
