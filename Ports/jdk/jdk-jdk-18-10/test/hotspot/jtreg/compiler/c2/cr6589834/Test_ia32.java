/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6589834
 * @summary Safepoint placed between stack pointer increment and decrement leads
 *          to interpreter's stack corruption after deoptimization.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *      -XX:+WhiteBoxAPI -XX:+IgnoreUnrecognizedVMOptions -XX:+VerifyStack
 *      -XX:CompileCommand=compileonly,compiler.c2.cr6589834.InlinedArrayCloneTestCase::*
 *      -XX:CompileCommand=dontinline,compiler.c2.cr6589834.InlinedArrayCloneTestCase::invokeArrayClone
 *      -XX:CompileCommand=inline,compiler.c2.cr6589834.InlinedArrayCloneTestCase::verifyArguments
 *      compiler.c2.cr6589834.Test_ia32
 */

package compiler.c2.cr6589834;

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;

public class Test_ia32 {
    private static final int NUM_THREADS
            = Math.min(100, 2 * Runtime.getRuntime().availableProcessors());
    private static final int CLONE_LENGTH = 1000;

    private static WhiteBox wb = WhiteBox.getWhiteBox();

    private final LoadedClass[] ARRAY = new LoadedClass[Test_ia32.CLONE_LENGTH];
    private volatile boolean doSpin = true;
    private volatile boolean testFailed = false;

    public boolean continueExecution() {
        return doSpin;
    }

    public void stopExecution() {
        doSpin = false;
    }

    public boolean isTestFailed() {
        return testFailed;
    }

    public void setTestFailed() {
        this.testFailed = true;
        stopExecution();
    }

    public LoadedClass[] getArray() {
        return ARRAY;
    }

    public void runTest() {
        Thread[] threads = new Thread[Test_ia32.NUM_THREADS];
        Method method;

        try {
            method = InlinedArrayCloneTestCase.class.getDeclaredMethod(
                    "invokeArrayClone", LoadedClass[].class);
        } catch (NoSuchMethodException e) {
            throw new Error("Tested method not found", e);
        }

        Asserts.assertTrue(wb.isMethodCompilable(method),
                "Method " + method.getName() + " should be compilable.");

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new InlinedArrayCloneTestCase(this));
            threads[i].start();
        }

        /*
         * Wait until InlinedArrayCloneTestCase::invokeArrayClone is compiled.
         */
        while (!wb.isMethodCompiled(method)) {
            Thread.yield();
        }

        /*
         * Load NotLoadedClass to cause deoptimization of
         * InlinedArrayCloneTestCase::invokeArrayClone due to invalidated
         * dependency.
         */
        try {
            Class.forName(Test_ia32.class.getPackage().getName() + ".NotLoadedClass");
        } catch (ClassNotFoundException e) {
            throw new Error("Unable to load class that invalidates "
                    + "CHA-dependency for method " + method.getName(), e);
        }

        stopExecution();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new Error("Fail to join thread " + thread, e);
            }
        }

        Asserts.assertFalse(isTestFailed(), "Test failed.");
    }

    public static void main(String[] args) {
        new Test_ia32().runTest();
    }
}

class LoadedClass {
}

@SuppressWarnings("unused")
class NotLoadedClass extends LoadedClass {
}
