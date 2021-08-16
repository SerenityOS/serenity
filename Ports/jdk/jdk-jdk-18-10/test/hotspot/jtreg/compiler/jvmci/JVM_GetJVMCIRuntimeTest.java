/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8136421
 * @requires vm.jvmci
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules jdk.internal.vm.ci/jdk.vm.ci.runtime
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      -Dcompiler.jvmci.JVM_GetJVMCIRuntimeTest.positive=true
 *      -XX:+EnableJVMCI
 *      compiler.jvmci.JVM_GetJVMCIRuntimeTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      -Dcompiler.jvmci.JVM_GetJVMCIRuntimeTest.positive=false
 *      -XX:-EnableJVMCI -XX:-UseJVMCICompiler
 *      compiler.jvmci.JVM_GetJVMCIRuntimeTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      -Dcompiler.jvmci.JVM_GetJVMCIRuntimeTest.positive=true
 *      -Dcompiler.jvmci.JVM_GetJVMCIRuntimeTest.threaded=true
 *      -XX:+EnableJVMCI
 *      compiler.jvmci.JVM_GetJVMCIRuntimeTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      -Dcompiler.jvmci.JVM_GetJVMCIRuntimeTest.positive=false
 *      -Dcompiler.jvmci.JVM_GetJVMCIRuntimeTest.threaded=true
 *      -XX:-EnableJVMCI -XX:-UseJVMCICompiler
 *      compiler.jvmci.JVM_GetJVMCIRuntimeTest

 */

package compiler.jvmci;

import jdk.test.lib.Asserts;
import jdk.vm.ci.runtime.JVMCI;

public class JVM_GetJVMCIRuntimeTest implements Runnable {
    private static final boolean IS_POSITIVE = Boolean.getBoolean(
            "compiler.jvmci.JVM_GetJVMCIRuntimeTest.positive");
    private static final boolean IN_THREAD = Boolean.getBoolean(
            "compiler.jvmci.JVM_GetJVMCIRuntimeTest.threaded");

    public static void main(String[] args) throws Throwable {
        JVM_GetJVMCIRuntimeTest instance = new JVM_GetJVMCIRuntimeTest();
        if (IN_THREAD) {
            Thread t = new Thread(instance);
            t.start();
            t.join();
        } else {
            instance.run();
        }
    }

    public void run() {
        Object result;
        try {
            result = JVMCI.getRuntime();
        } catch (InternalError e) {
            if (IS_POSITIVE) {
                throw new AssertionError("unexpected exception", e);
            }
            return;
        }
        if (!IS_POSITIVE) {
            throw new AssertionError("didn't get expected exception");
        }
        Asserts.assertNotNull(result,
                "initializeRuntime returned null");
        Asserts.assertEQ(result, JVMCI.getRuntime(),
                "initializeRuntime returns different results");

    }
}
