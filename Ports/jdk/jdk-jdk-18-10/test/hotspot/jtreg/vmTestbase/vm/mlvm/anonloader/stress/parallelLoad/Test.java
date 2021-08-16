/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @modules java.base/jdk.internal.misc
 *
 * @summary converted from VM Testbase vm/mlvm/anonloader/stress/parallelLoad.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.anonloader.stress.parallelLoad.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm
 *      -Xverify:all
 *      vm.mlvm.anonloader.stress.parallelLoad.Test
 *      -threadsPerCpu 4
 *      -threadsExtra 20
 *      -parseTimeout 0
 *      -hiddenLoad true
 */

package vm.mlvm.anonloader.stress.parallelLoad;

import vm.mlvm.anonloader.share.StressClassLoadingTest;
import vm.mlvm.anonloader.share.AnonkTestee01;
import vm.mlvm.share.MlvmTestExecutor;
import vm.mlvm.share.MultiThreadedTest;
import vm.share.FileUtils;

/**
 * Verifies that loading classes in parallel from several threads using
 * {@link java.lang.invoke.MethodHandles.Lookup#defineHiddenClass}
 * does not produce exceptions and crashes.
 *
 */
public class Test extends MultiThreadedTest {
    private static final Class<?> HOST_CLASS = AnonkTestee01.class;
    private static final String NAME_PREFIX = "thread%03d";

    private final byte[] classBytes;

    private static class SubTest extends StressClassLoadingTest {
        private final byte[] classBytes;

        public SubTest(byte[] classBytes) {
            this.classBytes = classBytes;
        }

        @Override
        protected Class<?> getHostClass() {
            return HOST_CLASS;
        }

        @Override
        protected byte[] generateClassBytes() {
            return classBytes;
        }
    }

    public Test() throws Exception {
        classBytes = FileUtils.readClass(HOST_CLASS.getName());
    }

    /**
     * Constructs a sub-test class and runs it. The sub-test class loads
     * {@link vm.mlvm.anonloader.share.AnonkTestee01} class bytecodes
     * using {@link java.lang.invoke.MethodHandles.Lookup#defineHiddenClass}
     * @param numThread Number of the thread
     * @throws Exception if there any exceptions thrown in the sub-test
     */
    @Override
    protected boolean runThread(int numThread) throws Exception {
        SubTest subTest = new SubTest(classBytes);
        subTest.setFileNamePrefix(String.format(NAME_PREFIX, numThread));
        return subTest.run();
    }

    /**
     * Runs the test.
     * @param args Test arguments.
     */
    public static void main(String[] args) {
        MlvmTestExecutor.launch(args);
    }
}
