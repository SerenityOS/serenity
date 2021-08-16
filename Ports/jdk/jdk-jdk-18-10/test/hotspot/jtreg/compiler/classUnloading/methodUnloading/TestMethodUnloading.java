/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test MethodUnloadingTest
 * @bug 8029443
 * @summary Tests the unloading of methods to to class unloading
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 *        compiler.classUnloading.methodUnloading.WorkerClass
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-BackgroundCompilation -XX:-UseCompressedOops
 *                   -XX:CompileCommand=compileonly,compiler.classUnloading.methodUnloading.TestMethodUnloading::doWork
 *                   compiler.classUnloading.methodUnloading.TestMethodUnloading
 */

package compiler.classUnloading.methodUnloading;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import compiler.whitebox.CompilerWhiteBoxTest;

public class TestMethodUnloading {
    private static final String workerClassName = "compiler.classUnloading.methodUnloading.WorkerClass";
    private static int work = -1;

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    /**
     * Does some work by either using the workerClass or locally producing values.
     * @param workerClass Class performing some work (will be unloaded)
     * @param useWorker If true the workerClass is used
     */
    static private void doWork(Class<?> workerClass, boolean useWorker) throws InstantiationException, IllegalAccessException {
        if (useWorker) {
            // Create a new instance
            Object worker = workerClass.newInstance();
            // We would like to call a method of WorkerClass here but we cannot cast to WorkerClass
            // because the class was loaded by a different class loader. One solution would be to use
            // reflection but since we want C2 to implement the call as an optimized IC we call
            // Object::hashCode() here which actually calls WorkerClass::hashCode().
            // C2 will then implement this call as an optimized IC that points to a to-interpreter stub
            // referencing the Method* for WorkerClass::hashCode().
            work = worker.hashCode();
            if (work != 42) {
                new RuntimeException("Work not done");
            }
        } else {
            // Do some important work here
            work = 1;
        }
    }

    /**
     * Makes sure that method is compiled by forcing compilation if not yet compiled.
     * @param m Method to be checked
     */
    static private void makeSureIsCompiled(Method m) {
        // Make sure background compilation is disabled
        if (WHITE_BOX.getBooleanVMFlag("BackgroundCompilation")) {
            throw new RuntimeException("Background compilation enabled");
        }

        // Check if already compiled
        if (!WHITE_BOX.isMethodCompiled(m)) {
            // If not, try to compile it with C2
            if(!WHITE_BOX.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION)) {
                // C2 compiler not available, try to compile with C1
                WHITE_BOX.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_SIMPLE);
            }
            // Because background compilation is disabled, method should now be compiled
            if(!WHITE_BOX.isMethodCompiled(m)) {
                throw new RuntimeException(m + " not compiled");
            }
        }
    }

    /**
     * This test creates stale Method* metadata in a to-interpreter stub of an optimized IC.
     *
     * The following steps are performed:
     * (1) A workerClass is loaded by a custom class loader
     * (2) The method doWork that calls a method of the workerClass is compiled. The call
     *     is implemented as an optimized IC calling a to-interpreted stub. The to-interpreter
     *     stub contains a Method* to a workerClass method.
     * (3) Unloading of the workerClass is enforced. The to-interpreter stub now contains a dead Method*.
     * (4) Depending on the implementation of the IC, the compiled version of doWork should still be
     *     valid. We call it again without using the workerClass.
     */
    static public void main(String[] args) throws Exception {
        // (1) Create a custom class loader with no parent class loader
        URL url = TestMethodUnloading.class.getProtectionDomain().getCodeSource().getLocation();
        URLClassLoader loader = new URLClassLoader(new URL[] {url}, null);

        // Load worker class with custom class loader
        Class<?> workerClass = Class.forName(workerClassName, true, loader);

        // (2) Make sure all paths of doWork are profiled and compiled
        for (int i = 0; i < 100000; ++i) {
            doWork(workerClass, true);
            doWork(workerClass, false);
        }

        // Make sure doWork is compiled now
        Method doWork = TestMethodUnloading.class.getDeclaredMethod("doWork", Class.class, boolean.class);
        makeSureIsCompiled(doWork);

        // (3) Throw away class loader and reference to workerClass to allow unloading
        loader.close();
        loader = null;
        workerClass = null;

        // Force garbage collection to trigger unloading of workerClass
        // Dead reference to WorkerClass::hashCode triggers JDK-8029443
        WHITE_BOX.fullGC();

        // (4) Depending on the implementation of the IC, the compiled version of doWork
        // may still be valid here. Execute it without a workerClass.
        doWork(null, false);
        if (work != 1) {
            throw new RuntimeException("Work not done");
        }

        doWork(Object.class, false);
    }
}
