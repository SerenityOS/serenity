/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase vm/mlvm/meth/stress/gc/createLotsOfMH.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test creates lots of MH to see if they are garbage collected and don't overflow different
 *     generations of heap (MH used to be allocated in PermGen, but since then they are moved
 *     to Eden and OldGen).
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.gc.createLotsOfMH.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.meth.stress.gc.createLotsOfMH.Test -stressIterationsFactor 100000
 */

package vm.mlvm.meth.stress.gc.createLotsOfMH;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import nsk.share.test.Stresser;
import vm.mlvm.share.MlvmTest;

// TODO: add other Lookup.findXXX methods

public class Test extends MlvmTest {

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }

    @Override
    public boolean run() throws Throwable {
        Stresser stresser = createStresser();
        try {
            stresser.start(1);
            Lookup lookup = MethodHandles.lookup();
            MethodHandle lastMH = lookup.findStatic(getClass(), "main",
                    MethodType.methodType(void.class, String[].class));

            getLog().display(
                    "Verifying that no OOME is thrown when creating MHs in a loop");
            getLog().display(
                    "Free memory on start (MB): "
                            + Runtime.getRuntime().freeMemory() / 1024 / 1024);

            while (stresser.continueExecution()) {
                stresser.iteration();
                switch (getRNG().nextInt(3)) {
                case 0:
                    lastMH = lookup.findConstructor(String.class,
                            MethodType.methodType(void.class, String.class));
                    break;
                case 1:
                    lastMH = lookup.findVirtual(getClass(), "run",
                            MethodType.methodType(boolean.class));
                    break;
                case 2:
                    lastMH = lookup.findStatic(ClassLoader.class,
                            "getSystemClassLoader",
                            MethodType.methodType(ClassLoader.class));
                    break;
                }
            }

            getLog().display(
                    "Free memory on end (MB): "
                            + Runtime.getRuntime().freeMemory() / 1024 / 1024);
            getLog().display("MHs created: " + stresser.getIteration());

            return true;
        } finally {
            stresser.finish();
        }
    }

}
