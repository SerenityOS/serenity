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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS203/hs203t001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * HS203: Hotswap + pop frame within events
 * T001:
 * 1. Set a breakpoint.
 * 2. Upon reaching the breakpoint, enable SingleStep.
 * 3. Redefine a class within SingleStep callback. Stepping should
 *    be continued in obsolete method.
 * 4. Pop a currently executed frame. Stepping should be continued
 *    on invoke instruction.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS203.hs203t001.hs203t001
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs203t001=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS203.hs203t001.hs203t001
 */

package nsk.jvmti.scenarios.hotswap.HS203.hs203t001;
import java.util.concurrent.atomic.AtomicBoolean;
import nsk.share.jvmti.RedefineAgent;

public class hs203t001 extends RedefineAgent {
    public static native boolean popThreadFrame(Thread thread);

    public static native boolean resumeThread(Thread thread);

    public hs203t001(String[] arg) {
        super(arg);
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs203t001 hsCase = new hs203t001(arg);
        System.exit(hsCase.runAgent());
    }

    public boolean  agentMethod() {
        boolean passed = false;
        MyThread mt = new MyThread();
        try {
            mt.start();
            while(!MyThread.resume.get());
            Thread.sleep(10000);
            popThreadFrame(mt);
            resumeThread(mt);
            mt.join();
            log.println(" ..."+mt.threadState);
        } catch(Exception ie) {
            ie.printStackTrace();
        }
        if (mt.threadState < 1000 && (redefineAttempted() && isRedefined()) ) {
            passed = true;
        }
        return passed;
        }

}
