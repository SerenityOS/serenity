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
 * @summary converted from VM Testbase vm/mlvm/meth/stress/gc/callSequencesDuringGC.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent, quarantine]
 * VM Testbase comments: 8208255
 * VM Testbase readme:
 * DESCRIPTION
 *     The test verifies that MH logic is not affected by garbage collector and garbage collector
 *     correctly walks through references in MHs.
 *     The test has 3 threads:
 *     - Thread 1 creates sequences of MH (see vm/mlvm/mh/func/sequences test) and calls them
 *     - Thread 2 tries to overflow heap with different objects and arrays
 *     - Thread 3 periodically calls GC
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.gc.callSequencesDuringGC.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.meth.stress.gc.callSequencesDuringGC.Test -stressIterationsFactor 1000
 */

package vm.mlvm.meth.stress.gc.callSequencesDuringGC;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.GarbageProducers;
import nsk.share.test.Stresser;
import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHTransformationGen;
import vm.mlvm.meth.share.RandomArgumentsGen;
import vm.mlvm.share.MlvmTest;

public class Test extends MlvmTest {

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }

    @Override
    public boolean run() throws Throwable {

        GCThread dustmanThread = new GCThread();
        dustmanThread.setDaemon(true);
        dustmanThread.start();

        LitterThread litterThread = new LitterThread();
        litterThread.setDaemon(true);
        litterThread.start();

        Stresser stresser = createStresser();
        try {
            stresser.start(1);

            while (stresser.continueExecution()) {
                stresser.iteration();

                String s = "Ziggy";

                final MethodHandle mhM0 = MethodHandles.lookup().findVirtual(
                        String.class, "toString",
                        MethodType.methodType(String.class));

                Argument[] finalArgs = RandomArgumentsGen.createRandomArgs(true, mhM0.type());
                Argument retVal = Argument.fromValue(s);
                retVal.setPreserved(true);
                MHTransformationGen.callSequence(MHTransformationGen.createSequence(retVal, s, mhM0, finalArgs), true);
            }

            return true;
        } finally {
            stresser.finish();
        }
    }

    private static class LitterThread extends Thread {
        @Override
        public void run() {
            try {
                @SuppressWarnings("rawtypes")
                List<GarbageProducer> gpList = new GarbageProducers()
                        .getAllProducers();

                for (;;) {
                    @SuppressWarnings("rawtypes")
                    GarbageProducer gp = gpList.get(getRNG().nextInt(
                            gpList.size()));
                    gp.create(Runtime.getRuntime().maxMemory() / 100);

                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private static class GCThread extends Thread {
        @Override
        public void run() {
            try {
                for (;;) {
                    System.gc();
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
            }
        }

    }
}
