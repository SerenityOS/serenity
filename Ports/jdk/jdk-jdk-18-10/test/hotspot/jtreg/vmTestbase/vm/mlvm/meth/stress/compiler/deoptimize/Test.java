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
 * @key stress
 *
 * @summary converted from VM Testbase vm/mlvm/meth/stress/compiler/deoptimize.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent, quarantine]
 * VM Testbase comments: 8079642 8208255
 * VM Testbase readme:
 * DESCRIPTION
 *     The test creates and calls MH sequences (see vm/mlvm/mh/func/sequences test) causing compilation
 *     (by calling sequence 10000 times) and deoptimization (by using uncommon traps).
 *     See vm.mlvm.meth.stress.java.sequences.Test for details on MH sequences.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.compiler.deoptimize.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @requires vm.debug != true
 *
 * @run main/othervm/timeout=300
 *      -XX:ReservedCodeCacheSize=100m
 *      vm.mlvm.meth.stress.compiler.deoptimize.Test
 *      -threadsPerCpu 4
 *      -threadsExtra 2
 */


/*
 * @test
 * @key stress
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.compiler.deoptimize.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @requires vm.debug == true
 *
 * @run main/othervm/timeout=300
 *      -XX:ReservedCodeCacheSize=100m
 *      vm.mlvm.meth.stress.compiler.deoptimize.Test
 *      -threadsPerCpu 2
 *      -threadsExtra 2
 */


package vm.mlvm.meth.stress.compiler.deoptimize;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHTransformationGen;
import vm.mlvm.meth.share.RandomArgumentsGen;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;
import vm.mlvm.share.MultiThreadedTest;

// TODO: check deopt using vm.mlvm.share.comp framework
public class Test extends MultiThreadedTest {

    static class A {
        Object m() {
            return Integer.valueOf(0);
        }
    }

    static class B extends A {
        @Override
        Object m() {
            return Integer.valueOf(1);
        }
    }

    static class TestData {
        final A _a;
        final int _expectedResult;

        TestData(A a) {
            _a = a;
            _expectedResult = (Integer) a.m();
        }

        Object m() {
            return _a.m();
        }
    }

    volatile TestData _data = new TestData(new A());

    volatile boolean _testDone = false;

    final MethodHandle _mh;
    final Argument[] _finalArgs;

    public Test() throws Throwable {
        _mh = MethodHandles.lookup().findVirtual(TestData.class, "m", MethodType.methodType(Object.class));
        _finalArgs = RandomArgumentsGen.createRandomArgs(true, _mh.type());
    }

    @Override
    protected void initializeTest() throws Throwable {
        super.initializeTest();
        if (calcThreadNum() < 2) {
            throw new IllegalArgumentException("Number of threads should be 2 or more");
        }
    }

    private void sleepAndDeoptimize() throws Throwable {
        try {
            Thread.sleep(3000);
            // Force deoptimization in uncommon trap logic
            Env.traceNormal("Deoptimizing");
            _data = new TestData((A) Test.class.getClassLoader().loadClass(Test.class.getName() + "$B").newInstance());
            Thread.sleep(3000);
        } finally {
            this._testDone = true;
        }
    }

    @Override
    protected boolean runThread(int threadNum) throws Throwable {
        if ( threadNum == 0 ) {
            sleepAndDeoptimize();
            return true;
        }

        while ( ! Test.this._testDone) {
            TestData dataSnapshot = _data;
            Integer expectedResult = (Integer) dataSnapshot._a.m();
            Argument retVal = new Argument(Object.class, expectedResult);
            retVal.setPreserved(true);

            MHTransformationGen.createAndCallSequence(retVal, dataSnapshot, _mh, _finalArgs, true);
        }

        return true;
    }

    public static void main(String[] args) { MlvmTest.launch(args); }
}
