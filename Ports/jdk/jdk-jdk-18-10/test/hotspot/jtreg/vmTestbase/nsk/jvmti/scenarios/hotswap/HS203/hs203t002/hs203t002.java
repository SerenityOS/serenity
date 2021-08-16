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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS203/hs203t002.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * T002:
 * 1. Set a breakpoint in method b().
 * 2. Call method a() which calls b().
 *  Upon reaching the breakpoint, enable SingleStep.
 * 3. Redefine class within SingleStep callback. New class version
 * contains the same method b() and the changed method a(). Stepping
 * should be continued in the method b().
 * 4. Pop a currently executed frame. Stepping should be continued
 * on invoke instruction of the obsolete method a().
 * 5. Pop a frame once more. Stepping should be continued
 * on invoke instruction of main method and then in the changed
 * method b().
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS203.hs203t002.hs203t002
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs203t002=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS203.hs203t002.hs203t002
 */

package nsk.jvmti.scenarios.hotswap.HS203.hs203t002;
import java.util.concurrent.atomic.AtomicBoolean;
import nsk.share.jvmti.RedefineAgent;

public class hs203t002 extends RedefineAgent {

    public hs203t002(String[] arg) {
        super(arg);
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs203t002 hsCase = new hs203t002(arg);
        System.exit(hsCase.runAgent());
    }

    public boolean  agentMethod() {
        MyThread mt = new MyThread();
        try {
            mt.start();
            while(!MyThread.resume.get());
            MyThread.resume.set(false);
            Thread.sleep(10000);
            popThreadFrame(mt);
            resumeThread(mt);
            while(!MyThread.resume2.get());;
            Thread.sleep(10000);
                        suspendThread(mt);
            //mt.suspend();
            popThreadFrame(mt);
            resumeThread(mt);
            MyThread.resume.set(true);
            mt.join();
                        log.println(" ..."+mt.threadState);
                } catch(Exception ie) {
                        ie.printStackTrace();
                }
        boolean passed = false;
                if ( (mt.threadState < 1000)  && (redefineAttempted() && isRedefined()) ) {
                        passed = true;
                } else {
                        log.println(" FAILED ...");
                }
        return passed;

        }
        public static native boolean popThreadFrame(Thread thread);
        public static native boolean resumeThread(Thread thread);
        public static native boolean suspendThread(Thread thread);
}
