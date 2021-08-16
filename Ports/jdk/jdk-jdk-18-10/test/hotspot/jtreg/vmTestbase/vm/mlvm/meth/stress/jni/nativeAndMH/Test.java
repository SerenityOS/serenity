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
 * @summary converted from VM Testbase vm/mlvm/meth/stress/jni/nativeAndMH.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test obtains a MH to a native method and call it. The native method in turn, calls
 *     another method handle and so on.
 *     The test verifies that arguments are correctly passed between native methods and MHs.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.jni.nativeAndMH.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm/native
 *      vm.mlvm.meth.stress.jni.nativeAndMH.Test
 *      -stressIterationsFactor 1000
 *      -threadsPerCpu 20
 *      -threadsExtra 20
 */

package vm.mlvm.meth.stress.jni.nativeAndMH;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import nsk.share.test.Stresser;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;
import vm.mlvm.share.MultiThreadedTest;

public class Test extends MultiThreadedTest {

    private static final String RETURN_VALUE = "test";

    static {
        System.loadLibrary("nativeAndMH");
    }

    private static native Object native01(Object a1, String a2, Object a3, Object a4, Object a5, Object a6, MethodHandle mh);

    private static final MethodType MT_calledFromNative = MethodType.methodType(
            Object.class,
            Object.class, Object.class, int.class, long.class, double.class, float.class);

    private static Object calledFromNative(Object s1, Object s2, int i, long l, double d, float f) {
        return RETURN_VALUE;
    }

    @Override
    protected boolean runThread(int threadNum) throws Throwable {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                Test.class,
                "calledFromNative",
                MT_calledFromNative);

        Stresser stresser = createStresser();
        stresser.start(1);

        while ( stresser.continueExecution() ) {
            stresser.iteration();

            String retValMH = (String) (Object) mh.invokeExact((Object) "test1", (Object) "test2", 3, 4L, 5D, 6F);
            String retValNative = (String) native01("test1", "test2", 3, 4L, 5D, 6F, mh).toString();

            if ( ! retValMH.equals(RETURN_VALUE) || ! retValNative.equals(RETURN_VALUE) )
                markTestFailed("Gold value: " + RETURN_VALUE + "; MH returned: " + retValMH + "; Native returned: " + retValNative);
            else
                Env.traceVerbose("Gold value: " + RETURN_VALUE + "; MH returned: " + retValMH + "; Native returned: " + retValNative);
        }

        stresser.finish();
        stresser.printExecutionInfo(getLog().getOutStream());

        return true;
    }

    public static void main(String[] args) { MlvmTest.launch(args); }
}
