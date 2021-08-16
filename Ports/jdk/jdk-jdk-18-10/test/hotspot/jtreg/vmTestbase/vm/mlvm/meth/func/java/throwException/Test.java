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
 * @summary converted from VM Testbase vm/mlvm/meth/func/java/throwException.
 * VM Testbase keywords: [feature_mlvm, quarantine]
 * VM Testbase comments: 8208255
 * VM Testbase readme:
 * DESCRIPTION
 *     The test creates a sequence of MHs (see vm/mlvm/mh/func/sequences test for details)
 *     and throws an exception from the latest test of this sequence and verifies that
 *     the exception is passed correctly.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.func.java.throwException.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.meth.func.java.throwException.Test
 */

package vm.mlvm.meth.func.java.throwException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHTransformationGen;
import vm.mlvm.meth.share.RandomArgumentGen;
import vm.mlvm.meth.share.RandomArgumentsGen;
import vm.mlvm.meth.share.transform.v2.MHMacroTF;
import vm.mlvm.share.MlvmTest;

public class Test extends MlvmTest {

    public static void main(String[] args) { MlvmTest.launch(args); }

    public static class Example {
        private Throwable t;

        public Example(Throwable t) {
            this.t = t;
        }

        public Object m0(int i, String s, Float f) {
            RuntimeException re = new RuntimeException("Good luck!");
            re.initCause(this.t);
            throw re;
        }
    }

    @Override
    public boolean run() throws Throwable {

        final RuntimeException requiredException = new RuntimeException("test");
        final Example e = new Example(requiredException);

        final MethodHandle mh = MethodHandles.lookup().findVirtual(
                Example.class, "m0",
                MethodType.methodType(Object.class, int.class, String.class, Float.class));

        Argument[] finalArgs = RandomArgumentsGen.createRandomArgs(true, mh.type());
        Argument retVal = RandomArgumentGen.next(mh.type().returnType());
        retVal.setPreserved(true);

        MHMacroTF seq = MHTransformationGen.createSequence(retVal, e, mh, finalArgs);

        try {
            MHTransformationGen.callSequence(seq, false);
            getLog().complain("Did not catch a required exception!");
            return false;
        } catch ( Throwable t ) {
            while ( t != null && t instanceof Exception ) {
                t = t.getCause();
                if ( t.equals(requiredException) ) {
                    getLog().display("Got a proper exception:");
                    t.printStackTrace(getLog().getOutStream());
                    return true;
                }
            }

            getLog().complain("Got wrong exception!");
            t.printStackTrace(getLog().getOutStream());
            return false;
        }
    }
}
