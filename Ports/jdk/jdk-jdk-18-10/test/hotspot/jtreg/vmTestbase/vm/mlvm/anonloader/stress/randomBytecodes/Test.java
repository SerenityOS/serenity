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
 * @key randomness
 * @modules java.base/jdk.internal.misc
 *
 * @summary converted from VM Testbase vm/mlvm/anonloader/stress/randomBytecodes.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.anonloader.stress.randomBytecodes.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.anonloader.stress.randomBytecodes.Test -stressIterationsFactor 100000
 */

package vm.mlvm.anonloader.stress.randomBytecodes;

import java.util.Arrays;
import vm.mlvm.anonloader.share.StressClassLoadingTest;

/**
 * The test does the following in a cycle:
 * <ol>
 * <li>Creates a class bytecodes that has a valid 12-byte header
 *     and has totally random bytes after the header
 * <li>Tries to load such class using:
 *     <ul>
 *       <li>a custom class loader, or
 *       <li>{@link java.lang.invoke.MethodHandles.Lookup#defineHiddenClass}
 *           when {@code -hiddenLoad true} is set.
 *     </ul>
 * </ol>
 *
 * <p>In most cases the resulting class file is invalid and rejected by
 * the VM verifier. But this test is looking for pathological cases
 * such as infinite loops in the verifier or VM crashes.
 *
 */
public class Test extends StressClassLoadingTest {
    private static final Class<?> HOST_CLASS = Test.class;
    private static final int MAX_SIZE = 0xFFF7;
    private static final byte[] CLASS_HEADER = new byte[] {
        (byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE,
        0x00, 0x00, 0x00, 0x32
    };

    /**
     * Returns {@link vm.mlvm.anonloader.share.AnonkTestee01} class to the
     * parent.
     * @return {@link vm.mlvm.anonloader.share.AnonkTestee01} class.
     */
    @Override
    protected Class<?> getHostClass() {
        return HOST_CLASS;
    }

    /**
     * Generates a class with valid header (magic and version fields) and
     * random bytes after the header.
     * <p>Class size is random ([8..65527]).
     * Byte values are limited to [0..11] range in order to increase
     * possiblity that the random class passes the initial (dead-on-arrival)
     * stages of the verifier and is rejected
     * in more interesting ones, like method bytecode verification.
     * Class version is 52.
     *
     * @return Class with valid Java header (8 bytes) and totally random bytes
     * after the header
     */
    @Override
    protected byte[] generateClassBytes() {
        final byte[] classBytes = Arrays.copyOf(CLASS_HEADER,
                CLASS_HEADER.length + getRNG().nextInt(MAX_SIZE));
        for (int j = CLASS_HEADER.length; j < classBytes.length; j++) {
            classBytes[j] = (byte) getRNG().nextInt(12);
        }

        return classBytes;
    }

    /**
     * Runs the test.
     * @param args Test arguments.
     */
    public static void main(String[] args) {
        StressClassLoadingTest.launch(args);
    }
}
