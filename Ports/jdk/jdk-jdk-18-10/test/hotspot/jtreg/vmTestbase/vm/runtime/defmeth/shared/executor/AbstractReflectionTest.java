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

import nsk.share.Pair;
import vm.runtime.defmeth.shared.Constants;
import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.MemoryClassLoader;
import vm.runtime.defmeth.shared.Util;
import vm.runtime.defmeth.shared.builder.TestBuilder;
import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.ParamValueExtractor;
import vm.runtime.defmeth.shared.data.Tester;
import vm.runtime.defmeth.shared.data.method.param.Param;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Commmon ancestor for reflection-based tests (Method.invoke & MethodHandle.invokeWithArguments).
 * Encapsulates all necessary state for test execution and contains some utility methods.
 */
public abstract class AbstractReflectionTest implements TestExecutor {
    protected MemoryClassLoader cl;
    protected Collection<? extends Tester> tests;
    protected DefMethTest testInstance;

    public AbstractReflectionTest(DefMethTest testInstance, MemoryClassLoader cl, Collection<? extends Tester> tests) {
        this.testInstance = testInstance;
        this.cl = cl;
        this.tests = tests;
    }

    @Override
    public MemoryClassLoader getLoader() {
        return cl;
    }

    protected Class[] paramType(String desc) throws ClassNotFoundException {
        Pair<String[],String> p = Util.parseDesc(desc);
        Class[] ptypes = new Class[p.first.length];
        for (int i = 0; i < ptypes.length; i++) {
            ptypes[i] = Util.decodeClass(p.first[i], getLoader());
        }
        return ptypes;
    }

    public Class resolve(Clazz clazz) {
        try {
            return cl.loadClass(clazz.name());
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    protected Object[] values(Param[] params) {
        Object[] result = new Object[params.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new ParamValueExtractor(this, params[i]).value();
        }
        return result;
    }

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
                    //testInstance.getLog().info("\t\t"+error.getMessage());
                    testInstance.getLog().info("\t\t"+error);
                    if (Constants.PRINT_STACK_TRACE) {
                        error.printStackTrace();
                    }
                }
            }
        }

        testInstance.addFailureCount(errors.isEmpty() ? 0 : 1);
        return errors;
    }
}
