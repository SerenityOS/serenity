/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS204/hs204t002.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * 1. Enable event ClassPrepare.
 * 2. Upon occurrence of ClassPrepare, set a breakpoint in class static
 *  initializer.
 * 3. Upon reaching the breakpoint, redefine the class and pop
 * a currently executed frame of the static initializer.
 *
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS204.hs204t002.hs204t002
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs204t002=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS204.hs204t002.hs204t002
 */

package nsk.jvmti.scenarios.hotswap.HS204.hs204t002;
import nsk.share.jvmti.RedefineAgent;

public class hs204t002 extends RedefineAgent {

    public hs204t002(String[] arg) {
        super(arg);
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs204t002 hsCase = new hs204t002(arg);
        System.exit(hsCase.runAgent());
    }

        public boolean  agentMethod(){
                MyThread mt = new MyThread();
                mt.start();
                try {
                mt.join();
                } catch(java.lang.InterruptedException ie) {
                        ie.printStackTrace();
                }
        boolean passed = false;
                if (mt.getStateValue() == 100) {
                        System.out.println(" ... Sorry its old class file..");
                }else if (redefineAttempted() && isRedefined() ) {
            passed = true;
                        System.out.println(" .... Thanks it got sucessfully completed..");
                }
                System.out.println(".satus==. "+mt.getStateValue());
        return passed;
        }
}
