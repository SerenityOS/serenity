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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS103/hs103t002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 *  Periodically hotswap class(es) with a changed version in
 *  asynchronous manner from specified number of JVMTI agents. The VM
 *  works in default mode.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS103.hs103t002.hs103t002
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs103t002=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS103.hs103t002.hs103t002
 */

package nsk.jvmti.scenarios.hotswap.HS103.hs103t002;
/*

   Periodically hotswap class(es) with a changed version in
   asynchronous manner from specified number of JVMTI agents. The VM
   works in default mode.
*/

import java.util.concurrent.atomic.AtomicInteger;
import nsk.share.jvmti.RedefineAgent;

public class hs103t002 extends RedefineAgent {
    private static final int size = 10;

    private MyThread[] threads;

    private static volatile boolean isRedefineFinished = false;
    private static volatile boolean isRedefineFailed = false;

    public hs103t002(String[] arg) {
        super(arg);
        threads = new MyThread[size];
        for(int i = 0; i < size; i++) {
            threads[i] = new MyThread(" Name.."+i);
        }
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs103t002 hsCase = new hs103t002(arg);
        System.exit(hsCase.runAgent());
    }

    public static void setRedefinitionDone(){
        System.out.println("redefines finished");
        isRedefineFinished = true;
    }

    public static void setRedefinitionFailed(){
        System.out.println("redefines failed");
        isRedefineFailed = true;
    }

    public boolean  agentMethod() {
        try {
            boolean passed = false;
            startAgentThread();
            waitForRedefine();
            doMyTasks();
            if ((getResult() == 0) &&
                (redefineAttempted() && isRedefined() ) && !isRedefineFailed ) {
                System.out.println("all redefines ok");
                passed = true;
            }
            else {
                System.out.println("some of redefines failed");
            }

            return passed;
        } catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void doMyTasks() throws InterruptedException {
        System.out.println("running tasks");
        for(MyThread thread : threads)  {
            thread.start();
        }
        for(MyThread thread : threads)  {
            thread.join();
        }
        System.out.println("tasks done");
    }

    private void waitForRedefine() {
        while(!isRedefineFinished) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("interrupted while waiting for the redefines");
            }
        }
    }

    public int getResult() {
        while(MyThread.ai.get() != MyThread.size) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("interrupted while waiting for the threads");
            }
        }
        int result = 0;
        for(MyThread thread : threads) {
            int state = thread.getThreadState();
            if (state != MyThread.size * 10) {
                System.out.println("Error: thread.getThreadState() returned " + state + ", expected " +  MyThread.size * 10);
                result = 1;
                break;
            }
        }
        return result;
    }

    public native boolean startAgentThread();
}
