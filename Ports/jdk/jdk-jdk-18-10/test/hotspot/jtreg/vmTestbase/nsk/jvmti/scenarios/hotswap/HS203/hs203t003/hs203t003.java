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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS203/hs203t003.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * Description ::
 *     Redefining a class while its field is being accessed, pop currently executing frame
 *     (return back to method call statement) and resume its execution.
 *     The Test creates a Thread (MyThread). During class is being prepared,
 *     a  field watch is added to jvmti. The thread is allowed to run (execute).
 *     During field change callbacks, class is being redefined with to (./newclass/MyThread.java)
 *     a different version of class and suspended's thread. The main thread would pop frame
 *     and resume its execution.
 *     The test is expected to pass, if redefine class is effective after resuming thread.
 *     (Hint : A thread `s frame can be poped only if it is suspended, so we need to suspend,
 *     later resume that thread).
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS203.hs203t003.hs203t003
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs203t003=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS203.hs203t003.hs203t003
 */

package nsk.jvmti.scenarios.hotswap.HS203.hs203t003;

import nsk.share.jvmti.RedefineAgent;
import java.util.concurrent.atomic.AtomicBoolean;

public class hs203t003 extends RedefineAgent {

    public native boolean popThreadFrame(Thread thread);
    public native boolean isSuspended(Thread thread);
    public native boolean resumeThread(Thread thread);


    public hs203t003(String[] arg) {
        super(arg);
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs203t003 hsCase = new hs203t003(arg);
        System.exit(hsCase.runAgent());
    }


    public boolean agentMethod() {
        boolean passed = false;
        MyThread mt = new MyThread();
        try {
            mt.start();
            // Check if we can can pop the thread.
            // We can not do redefine/pop frame on run method.
            while (!MyThread.resume.get());
            // Sleep for some few secs to get redefined.
            while (!isRedefined()) {
                if (!agentStatus()) {
                    System.out.println("Failed to redefine class");
                    return passed;
                }
                Thread.sleep(100);
            }
            // Wait for the thread to be suspended.
            while (!isSuspended(mt)) {
                if (!agentStatus()) {
                    System.out.println("Failed to suspend thread");
                    return passed;
                }
                Thread.sleep(100);
            }
            // Pop the frame.
            if (!popThreadFrame(mt)) {
                System.out.println("Failed to pop a frame = "
                                   + mt.threadState);
            }
            // Resume the thread.
            if(!resumeThread(mt)) {
                System.out.println("Failed to resume the thread = "
                                   + mt.threadState);
            }
            // Wait till the other thread completes its execution.
            mt.join();
            System.out.println("Thread state after popping/redefining = "
                               + mt.threadState);
        } catch(Exception ie) {
            ie.printStackTrace();
        }
        if ((mt.threadState < 1000) && agentStatus()) {
            passed = true;
        }
        return passed;
    }

}
