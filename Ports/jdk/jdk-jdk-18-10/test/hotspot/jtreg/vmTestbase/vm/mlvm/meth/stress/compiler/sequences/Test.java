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
 * @summary converted from VM Testbase vm/mlvm/meth/stress/compiler/sequences.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent, quarantine]
 * VM Testbase comments: 8208255
 * VM Testbase readme:
 * DESCRIPTION
 *     Create various long sequences of method handles, adding/removing/joining/splitting arguments.
 *     Don't verify correctness of results. Just try to stress compiler.
 *     See vm.mlvm.meth.stress.java.sequences.Test for details on MH sequences.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.compiler.sequences.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm
 *      vm.mlvm.meth.stress.compiler.sequences.Test
 *      -threadsPerCpu 1
 *      -threadsExtra 2
 *      -stressIterationsFactor 1000
 */

package vm.mlvm.meth.stress.compiler.sequences;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import nsk.share.test.Stresser;
import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHTransformationGen;
import vm.mlvm.meth.share.RandomArgumentsGen;
import vm.mlvm.share.MlvmTest;
import vm.mlvm.share.MultiThreadedTest;

//TODO: check deoptimization using vm.mlvm.share.comp framework
public class Test extends MultiThreadedTest {

    Object target(int i, String s, Float f) {
        return i + s + f;
    }

    final MethodHandle _mh;
    final Argument[] _finalArgs;
    final Argument _retVal;

    public Test() throws Throwable {
        super();

        _mh = MethodHandles.lookup().findVirtual(
                Test.class,
                "target",
                MethodType.methodType(Object.class, int.class, String.class, Float.class));

        _finalArgs = RandomArgumentsGen.createRandomArgs(true, _mh.type());
        _retVal = Argument.fromValue(target(
                       (Integer) _finalArgs[0].getValue(),
                       (String) _finalArgs[1].getValue(),
                       (Float) _finalArgs[2].getValue()));
        _retVal.setPreserved(true);
    }

    @Override
    public boolean runThread(int threadNum) throws Throwable {

        Stresser stresser = createStresser();
        stresser.start(1);
        try {
            while ( stresser.continueExecution() ) {
                stresser.iteration();
                MHTransformationGen.callSequence(MHTransformationGen.createSequence(_retVal, Test.this, _mh, _finalArgs), true);
            }
        } finally {
            stresser.finish();
        }

        return true;
    }

    public static void main(String[] args) { MlvmTest.launch(args); }
}
