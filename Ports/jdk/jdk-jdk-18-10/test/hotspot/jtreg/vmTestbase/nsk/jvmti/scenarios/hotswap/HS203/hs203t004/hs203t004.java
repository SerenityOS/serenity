/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS203/hs203t004.
 * VM Testbase keywords: [jpda, jvmti, onload_only_caps, noras, redefine, feature_hotswap, monitoring]
 * VM Testbase readme:
 * Description ::
 *     The test would redefine a class during method compilation, pops currently executing frame.
 *     The Test starts a Thread (MyThread). On preparing of MyThread compiled_method_load event is enabled.
 *     While running the thread, it calls a method (doTask2() ) for number of times (10000).
 *     That would cause this method to be compiled, which causes a jvmti callback for compiled method load.
 *     (Hint : to force method compilation -XX:CompileThreshold=900 is used).
 *     The class which holds this method is redefined with ./newclass/MyThread.java, Once the redefine
 *     is completed the thread is suspended, pops current frame  and resumed.
 *     The test is said to pass if the redefine is effective in compile_method callback.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS203.hs203t004.hs203t004
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -XX:-Inline
 *      -XX:CompileThreshold=900
 *      -Xbatch
 *      -XX:-TieredCompilation
 *      -agentlib:hs203t004=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS203.hs203t004.hs203t004
 */

package nsk.jvmti.scenarios.hotswap.HS203.hs203t004;

import vm.share.VMRuntimeEnvUtils;
import nsk.share.Consts;
import nsk.share.jvmti.RedefineAgent;

public class hs203t004 extends RedefineAgent {

    public hs203t004(String[] arg) {
        super(arg);
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        if (!VMRuntimeEnvUtils.isJITEnabled()) {
            System.out.println("WARNING: test isn't valid if JIT compilation is disabled");
            System.out.println("Exiting with 'PASSED' status");
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_PASSED);
        }

        hs203t004 hsCase = new hs203t004(arg);
        System.exit(hsCase.runAgent());
    }

    public boolean agentMethod() {
        boolean passed = false;

        MyThread myThread = new MyThread();

        try {
            myThread.start();

            while (!isRedefined()) {
                Thread.yield();
            }

            suspendThread(myThread);

            popThreadFrame(myThread);

            resumeThread(myThread);

            MyThread.stop = false;
            myThread.join();

            System.out.println(" Thread state = " + myThread.threadState);
        } catch (Throwable t) {
            System.out.println("Unexpected exception: " + t);
            t.printStackTrace();
        } finally {
            if (myThread.threadState == 0 && agentStatus()) {
                passed = true;
            }
        }

        return passed;
    }

    public native void suspendThread(Thread thread);

    public native boolean popThreadFrame(Thread thread);

    public native boolean resumeThread(Thread thread);
}
