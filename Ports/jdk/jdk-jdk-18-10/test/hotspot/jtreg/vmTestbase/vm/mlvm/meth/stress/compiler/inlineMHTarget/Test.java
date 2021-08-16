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
 *
 * @summary converted from VM Testbase vm/mlvm/meth/stress/compiler/inlineMHTarget.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test creates MH to short methods that are likely to be inlined and
 *     verifies that they are inlined into MH code correctly.
 *     See vm.mlvm.meth.stress.java.sequences.Test for details on MH sequences.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.compiler.inlineMHTarget.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.meth.stress.compiler.inlineMHTarget.Test -stressIterationsFactor 100
 */

package vm.mlvm.meth.stress.compiler.inlineMHTarget;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import nsk.share.test.Stresser;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;

// TODO: check compilation using vm.mlvm.share.comp framework
// TODO: enhance to check NxN primitive types
public class Test extends MlvmTest {

    private static final int ITERATIONS = 10000; // Should be enough to trigger compilation

    private static final int THE_CONSTANT = 42;
    private int field = 96;

    static int i(int i) { return i; }
    static int k(int i) { return THE_CONSTANT;  }
    int getter() { return this.field; }

    int iplusk(int i) { return i(i) + k(i) + getter(); }

    static int mh_iplusk(MethodHandle a, MethodHandle b, MethodHandle c, int i) throws Throwable {
        return (int) a.invokeExact(i) + (int) b.invokeExact(i) + (int) c.invokeExact();
    }

    @Override
    public boolean run() throws Throwable {
        Lookup l = MethodHandles.lookup();
        MethodHandle ipluskMH = l.findVirtual(
                    Test.class, "iplusk", MethodType.methodType(int.class, int.class)
                ).bindTo(this);

        MethodHandle iMH = MethodHandles.identity(int.class);
        MethodHandle kMH = MethodHandles.dropArguments(MethodHandles.constant(int.class, THE_CONSTANT), 0, int.class);
        MethodHandle getterMH = l.findGetter(Test.class, "field", int.class).bindTo(this);
        MethodHandle mh_ipluskMH = MethodHandles.insertArguments(
                l.findStatic(Test.class, "mh_iplusk", MethodType.methodType(int.class, MethodHandle.class, MethodHandle.class, MethodHandle.class, int.class)),
                0, iMH, kMH, getterMH);

        int mh1Result = 0, mh2Result = 0, directResult = 0;
        Stresser s = createStresser();
        s.start(1);
        try {
            while ( s.continueExecution() ) {
                for (int i = 0; i < ITERATIONS; i++) {
                    s.iteration();
                    mh1Result += (int) ipluskMH.invokeExact(i);
                    mh2Result += (int) mh_ipluskMH.invokeExact(i);
                    directResult += iplusk(i);
                }
            }
        } finally {
            s.finish();
        }

        if (mh1Result != directResult || mh2Result != directResult) {
            Env.complain("Sum computed using MH 1=" + mh1Result
                       + "; Sum computed using MH 2=" + mh2Result
                       + "; using direct calls=" + directResult);
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }
}
