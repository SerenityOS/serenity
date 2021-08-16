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
 * @summary converted from VM Testbase vm/mlvm/meth/stress/java/sequences.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent, quarantine]
 * VM Testbase comments: 8208255
 * VM Testbase readme:
 * DESCRIPTION
 *     Creates a sequence of method handles that manipulates arguments. The following manipulations are used:
 *     - Numeric conversions
 *     - Boxing and unboxing
 *     - Adding arguments, binding arguments
 *     - Deleting arguments
 *     - Reordering arguments
 *     - Array scattering and gathering
 *     - A MH that calls pair of methods ("guardWithTest")
 *     - The test calculates the "correct result" of calling the sequence of method handles by using it's own logic and compares it with the result of calling MHs.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.stress.java.sequences.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.meth.stress.java.sequences.Test
 */

package vm.mlvm.meth.stress.java.sequences;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHTransformationGen;
import vm.mlvm.meth.share.RandomArgumentsGen;
import vm.mlvm.share.MlvmTest;

public class Test extends MlvmTest {

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }

    public static class Example {
        private Argument[] finalArgs;
        private boolean eqI;
        private boolean eqS;
        private boolean eqF;

        public String m0(int i, String s, Float f) {
            this.eqI = this.finalArgs[0].getValue().equals(i);
            this.eqS = this.finalArgs[1].getValue().equals(s);
            this.eqF = this.finalArgs[2].getValue().equals(f);

            return "i=" + i + " (" + this.eqI + "); " + "s=" + s + " (" + this.eqS
                    + "); " + "f=" + f + " (" + this.eqF + "); ";
        }

        public void setFinalArgs(Argument[] finalArgs) {
            this.finalArgs = finalArgs;
        }

        public boolean areParametersEqual() {
            return this.eqI && this.eqS && this.eqF;
        }
    }

    @Override
    public boolean run() throws Throwable {

        final Example e = new Example();

        final MethodHandle mhM0 = MethodHandles.lookup().findVirtual(
                Example.class,
                "m0",
                MethodType.methodType(String.class, int.class, String.class, Float.class));

        Argument[] finalArgs = RandomArgumentsGen.createRandomArgs(true, mhM0.type());
        e.setFinalArgs(finalArgs);

        Argument finalRetVal = Argument.fromValue(e.m0((int) (Integer) finalArgs[0].getValue(), (String) finalArgs[1].getValue(), (Float) finalArgs[2].getValue()));

        MHTransformationGen.callSequence(MHTransformationGen.createSequence(finalRetVal, e, mhM0, finalArgs), false);

        if (!e.areParametersEqual()) {
            getLog().complain("Unexpected argument values were received at the final method");
            return false;
        }

        return true;
    }
}
