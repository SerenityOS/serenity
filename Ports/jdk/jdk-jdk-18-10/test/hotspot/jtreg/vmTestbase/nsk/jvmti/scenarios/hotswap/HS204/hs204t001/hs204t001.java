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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS204/hs204t001.
 * VM Testbase keywords: [jpda, jvmti, onload_only_caps, noras, redefine, feature_hotswap, quarantine]
 * VM Testbase comments: 6813266
 * VM Testbase readme:
 * DESCRIPTION
 *     This test is for HS204 T001 scenario of "HotSwap class and pop frame".
 *     There are several java classes. First - hs204t001 - main class - is for redefinitions, running test thread and control
 *     results.
 *     All others - hs204t001R - are classes which are redefined and one of them is used to test popFrame feature.
 *     We enable events ClassFileLoadHook, ClassLoad, and ClassPrepare and popFrame feature.
 *     Then main class begins list of redefenitions in the following order:
 *     1. After receiving ClassLoad event, native agent redefines the class being loaded (hs204t001R).
 *     2. Waits for ClassFileLoadHook event from step 1 and redefines the class again
 *     3. Catches ClassPrepare event from step 2 and redefines the class again
 *     4. Receives ClassFileLoadHook event and makes another redefinition
 *     When all redefinitions have been done, we suspend the thread test class from Java code, invoke popFrame() and resume the test thread.
 *     This should result in doInThisThread() method executed for the second time.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS204.hs204t001.hs204t001
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass02 newclass03 newclass01 newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs204t001=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS204.hs204t001.hs204t001
 */

package nsk.jvmti.scenarios.hotswap.HS204.hs204t001;
import java.io.PrintStream;
import nsk.share.Log;
import nsk.share.Consts;
import nsk.share.jvmti.ArgumentHandler;
import nsk.share.jvmti.DebugeeClass;
import java.util.concurrent.atomic.AtomicBoolean;

public class hs204t001 extends DebugeeClass {
    static int status = Consts.TEST_PASSED;
    static Log log = null;
    static native void setThread(Thread thread);
    static native boolean suspendThread(Thread thread);
    static native boolean popFrame(Thread thread);

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);
        System.exit(Consts.JCK_STATUS_BASE + run(args, System.out));
    }

    // redirecting for some of the testing models.
    public static int run(String[] args , PrintStream out) {
        return new hs204t001().runIt(args, out);
    }

    private int runIt(String[] args, PrintStream out) {
        int iter;
        ArgumentHandler argHandler = new ArgumentHandler(args);
        int timeout = argHandler.findOptionIntValue("-waittime",10);
        out.println("... timeout.. "+timeout);
        out.println(".. Path "+argHandler.findOptionValue("pathToNewByteCode"));
        // Logger was initialized.
        log = new Log(out, argHandler);
        out.println(" Testing started..");
        hs204t001R  t1;
        t1= new hs204t001R() ;
        setThread(t1);
        t1.start();
        int afterSuspend=0;
        try {
            while(!hs204t001R.suspend.get());
            out.println(" State.."+t1.getIndex());
            suspendThread(t1);
            afterSuspend=t1.getIndex();
            //System.out.println("after suspending the read contents of index ="+t1.data);
            hs204t001R.run.set(false);
            popFrame(t1);
            // resume this thread.
            t1.join();
        } catch(InterruptedException ie) {
            ie.printStackTrace();
        }
        log.display(" index == "+t1.getIndex());
        out.println(" index ="+t1.getIndex());
        return (t1.getIndex() == afterSuspend  ? 0 : 1);
    }
}
