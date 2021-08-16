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
 * @modules java.base/jdk.internal.org.objectweb.asm
 *
 * @summary converted from VM Testbase vm/mlvm/meth/stress/gc/createLotsOfMHConsts.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test loads lots of MH constants (by loading a class that has many of them many times
 *     using different classloaders) to see if they are garbage collected and don't overflow
 *     different generations of heap.
 *
 * @library /vmTestbase
 *          /test/lib
 *          /vmTestbase/vm/mlvm/patches
 *
 * @comment patch for java.base
 * @build java.base/*
 *
 * @comment build generator
 * @build vm.mlvm.cp.share.GenCPFullOfMH
 *
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.gc.createLotsOfMHConsts.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 * @ignore 8194951
 *
 * @run main/othervm/timeout=300
 *      vm.mlvm.meth.stress.gc.createLotsOfMHConsts.Test
 *      -stressIterationsFactor 100000
 *      -generator vm.mlvm.cp.share.GenCPFullOfMH
 */

package vm.mlvm.meth.stress.gc.createLotsOfMHConsts;

import nsk.share.test.Stresser;
import vm.mlvm.share.ClassfileGeneratorTest;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;

public class Test extends ClassfileGeneratorTest {

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }

    @Override
    public boolean run() throws Throwable {
        Stresser stresser = createStresser();
        try {
            stresser.start(1);
            while (stresser.continueExecution()) {
                stresser.iteration();
                super.run();
            }

            return true;

        } catch ( OutOfMemoryError e ) {
            Env.traceNormal(e, "Caught an OOME. This is OK.");
            return true;
        } finally {
            stresser.finish();
        }
    }
}
