/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared.executor;

import java.lang.reflect.InvocationTargetException;

import vm.runtime.defmeth.shared.Constants;
import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.MemoryClassLoader;
import vm.runtime.defmeth.shared.data.Tester;
import java.util.*;
import nsk.share.Pair;
import static vm.runtime.defmeth.shared.Constants.*;

/**
 * Run a single test with bytecode-generated asserts.
 */
public class GeneratedTest implements TestExecutor {

    /** Assertions to check */
    private Collection<? extends Tester> tests;

    /** Class loader to load all necessary classes for execution */
    private MemoryClassLoader cl;

    private DefMethTest testInstance;

    public GeneratedTest(MemoryClassLoader cl, DefMethTest testInstance,
                         Collection<? extends Tester> tests) {
        this.cl = cl;
        this.tests = tests;
        this.testInstance = testInstance;
    }

    public GeneratedTest(MemoryClassLoader cl, DefMethTest testObj,
                         Tester... tests) {
        this(cl, testObj, Arrays.asList(tests));
    }

    @Override
    public MemoryClassLoader getLoader() {
        return cl;
    }

    /**
     * Run individual assertion for the test by it's name.
     *
     * @param test
     * @throws Throwable
     */
    public void run(Tester test) throws Throwable {
        try {
            Class<?> clz = cl.loadClass(test.name());
            java.lang.reflect.Method m = clz.getMethod("test");
            m.invoke(null);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Check assertions from a test and return errors if any.
     *
     * @return
     */
    public List<Pair<Tester,Throwable>> run() {
        List<Pair<Tester,Throwable>> errors = new ArrayList<>();

        if (tests.isEmpty()) {
            throw new IllegalStateException("No tests to run");
        }

        for (Tester t : tests) {
            StringBuilder msg =
                    new StringBuilder(String.format("\t%-30s: ", t.name()));

            Throwable error = null;
            try {
                run(t);

                msg.append("PASSED");
            } catch (Throwable e) {
                error = e;
                errors.add(Pair.of(t,e));
                msg.append("FAILED");
            } finally {
                testInstance.getLog().info(msg.toString());
                if (error != null) {
                    testInstance.getLog().info("\t\t"+error.getMessage());
                    if (PRINT_STACK_TRACE) {
                        error.printStackTrace();
                    }
                }
            }
        }

        testInstance.addFailureCount(errors.isEmpty() ? 0 : 1);
        return errors;
    }
}
