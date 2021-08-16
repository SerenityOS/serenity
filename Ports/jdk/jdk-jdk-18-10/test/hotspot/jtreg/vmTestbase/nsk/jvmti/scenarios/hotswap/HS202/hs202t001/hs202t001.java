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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS202/hs202t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * 1. Set breakpoints in several methods when Object.wait(),
 * Object.notify(), Object.notifyAll() are in use in these methods.
 * 2. Upon reaching a breakpoint, enable SingleStep.
 * 3. Redefine an java.lang.Object class within SingleStep callback
 * when one of its methods is called by the tested method.
 * 4. Pop a currently executed frame.
 * This case is not possible as we need to keep break points on wait, notify and notifyall which are native methods.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS202.hs202t001.hs202t001
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs202t001=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS202.hs202t001.hs202t001
 */

package nsk.jvmti.scenarios.hotswap.HS202.hs202t001;
import nsk.share.jvmti.RedefineAgent;


public class hs202t001 extends RedefineAgent {

    public native boolean popThreadFrame(Thread thread);

    public native boolean resumeThread(Thread thread);

    public hs202t001(String[] arg) {
        super(arg);
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs202t001 hsCase = new hs202t001(arg);
        System.exit(hsCase.runAgent());
    }

    public boolean  agentMethod(){
        int state=0;
        MyObject myObject = new MyObject();
        MyThread mt = new MyThread(myObject);
                mt.start();
                try {
                        System.out.println("In side the for loop");
            for(int i=0; i < 100; i++) {
                add(myObject, 1);
            }
            myObject.stop(true);
            if( popThreadFrame(mt)) {;
                resumeThread(mt);
            } // Popoing will not be possible on ..
            mt.join();
            state = myObject.getAge();
        } catch(java.lang.InterruptedException ie) {
            ie.printStackTrace();
        }
        boolean passed = false;
        if (state != 0 ) {
            state =0;
            System.out.println(" Passed ..");
            passed = true;
        } else {
            System.out.println(" Failed..");
        }
        return passed;
    }

        private void add(MyObject obj, int i) throws InterruptedException {
                if ( !obj.isUpdated()) {
                        obj.addAge(i);
                }
                Thread.sleep(150);
        }

}
