/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase vm/mlvm/meth/stress/compiler/i2c_c2i.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent, quarantine]
 * VM Testbase comments: 8208255
 * VM Testbase readme:
 * DESCRIPTION
 *     The test attempts to check MethodHandle i2c and c2i adapters by using sequences.
 *     Then it forces compilation of some of intermediate method handles. The test enables
 *     diagnostic printing of compilation and analyse it's own standard output
 *     to see if method is really has been compiled. When some subsequence is compiled,
 *     the test calls the whole sequence and forces decompilation (by using uncommon trap logic)
 *     of some smaller subsequence. This way both i2c and c2i adapters are created.
 *     The test compares result of calling the sequence of MHs with the results computed
 *     by the test and fails if the result is different.
 *     The test is a random one, it makes random sequences of MH and calls random subsequences.
 *     To facilitate reproducing test failures, it prints it's random seed, which can be fed
 *     to test when reproducing a problem.
 *     See vm.mlvm.meth.stress.java.sequences.Test for details on MH sequences.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.compiler.i2c_c2i.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.meth.stress.compiler.i2c_c2i.Test
 */

package vm.mlvm.meth.stress.compiler.i2c_c2i;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.CyclicBarrier;

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHTransformationGen;
import vm.mlvm.meth.share.RandomArgumentsGen;
import vm.mlvm.meth.share.transform.v2.MHMacroTF;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;

// TODO: check that i2c/c2i adapters are really created
// TODO: check deopt using vm.mlvm.share.comp framework
// TODO: use multi-threaded test framework
public class Test extends MlvmTest {

    private static final int THREADS
            = Runtime.getRuntime().availableProcessors();

    Object finalTarget() {
        return Integer.valueOf(0);
    }

    static class A {
        MHMacroTF trList;

        A(MHMacroTF trList) {
            this.trList = trList;
        }

        Object m() throws Throwable {
            Env.traceNormal("Original m() in thread "
                    + Thread.currentThread().getName());
            return MHTransformationGen.callSequence(this.trList, false);
        }
    }

    static class B extends A {
        B() {
            super(null);
        }

        @Override
        Object m() throws Throwable {
            Env.traceNormal("Deoptimized m() in thread "
                    + Thread.currentThread().getName());
            return Integer.valueOf(1);
        }
    }

    volatile A intermediateTarget;

    Object callIntemediateTarget() throws Throwable {
        return this.intermediateTarget.m();
    }

    CyclicBarrier startBarrier = new CyclicBarrier(THREADS + 1);

    volatile boolean testDone = false;

    @Override
    public boolean run() throws Throwable {

        final MethodHandle mhB = MethodHandles.lookup().findVirtual(Test.class,
                "finalTarget", MethodType.methodType(Object.class));

        final Argument finalRetVal = Argument.fromValue(Integer.valueOf(0));
        finalRetVal.setPreserved(true);

        this.intermediateTarget = new A(
                MHTransformationGen.createSequence(finalRetVal, Test.this, mhB,
                        RandomArgumentsGen.createRandomArgs(true, mhB.type())));

        final MethodHandle mhM = MethodHandles.lookup().findVirtual(Test.class,
                "callIntemediateTarget", MethodType.methodType(Object.class));

        final Argument[] finalArgs = RandomArgumentsGen.createRandomArgs(true,
                mhM.type());

        Thread[] threads = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            (threads[t] = new Thread("Stresser " + t) {

                public void run() {
                    try {
                        MHMacroTF tList = MHTransformationGen.createSequence(
                                finalRetVal, Test.this, mhM, finalArgs);
                        Test.this.startBarrier.await();
                        while ( ! Test.this.testDone) {
                            int e = (Integer) Test.this.intermediateTarget.m();
                            int r = (Integer) MHTransformationGen.callSequence(
                                    tList, false);
                            if (r != e)
                                Env.traceNormal("Wrong result in thread "
                                        + getName() + ", but this is OK");
                        }
                        Env.traceVerbose("Thread " + getName()+ ": work done");
                    } catch (Throwable t) {
                        markTestFailed("Exception in thread " + getName(), t);
                    }
                }
            }).start();
        }

        this.startBarrier.await();
        Env.traceImportant("Threads started");

        Thread.sleep(3000);

        Env.traceImportant("Deoptimizing");
        // Force deoptimization in uncommon trap logic
        this.intermediateTarget = (A) Test.class.getClassLoader().loadClass(
                Test.class.getName() + "$B").newInstance();

        Thread.sleep(3000);

        this.testDone = true;
        for (int t = 0; t < THREADS; t++)  {
            threads[t].join();
        }
        return true;
    }

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }
}
