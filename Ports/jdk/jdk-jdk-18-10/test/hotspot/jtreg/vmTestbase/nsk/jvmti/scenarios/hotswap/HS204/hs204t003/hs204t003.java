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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS204/hs204t003.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * Description ::
 *     The test pop initializer frame, redefines class and pops thread's current frame (constructor).
 *     The test enables ClassPrepare event. A TempThread (dummy thread) is created and
 *     it starts creating MyThread object (It can even be normal class doesn;t needed to be a thread).
 *     On receiving ClassPrepare event for MyThread, It would enable field access event
 *     (JVMTI_EVENT_FIELD_ACCESS), adds access watch for static field 'intState' of MyThread.
 *     Inside the MyThread constructor, watch field is accessed(incremented). Which causes callback.
 *     Callback for 'FiledAccess', this redefines ./MyThread.java  class to ./newclass00/MyThread.java
 *     and suspends the currently executing thread ('TempThread').
 *     The main thread pops executing frame in suspended thread ('TempThread') and resumes its execution.
 *     'TempThread' would reenter to constructor again.
 *     Test is said to be pass, if MyThread is from ./newclass00/MyThread.
 * The notes from removed README
 * 1. Enable event ClassPrepare.
 * 2. Upon ClassPrepare occurrence, enable FieldAccessWatch for a field to be initialized by the initializer.
 * 3. Upon accessing the field by the initializer, redefine the class and pop a currently executed
 *  frame of the initializer within incoming FieldAccess callback.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS204.hs204t003.hs204t003
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs204t003=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS204.hs204t003.hs204t003
 */

package nsk.jvmti.scenarios.hotswap.HS204.hs204t003;

import nsk.share.jvmti.RedefineAgent;
public class hs204t003 extends RedefineAgent {
    public native boolean popFrame(Thread thread) ;

    public hs204t003(String[]arg) {
        super(arg);
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs204t003 hsCase = new hs204t003(arg);
        System.exit(hsCase.runAgent());
        }

        public boolean  agentMethod() {
        boolean passed = false;
        MyThread mthread =null;
                try {
            TempThread temp = new TempThread();
            temp.start();
            Thread.sleep(10000);
            popFrame(temp);
            temp.join();
            mthread = temp.mthread;
            mthread.start();
                        mthread.join();
                } catch(Exception exp) {
                        exp.printStackTrace();
                } finally {
            if ( mthread != null ) {
                System.out.println(" agentMethod() ::  field state = "+mthread.getIntState());
                if ( redefineAttempted() &&
                        isRedefined() &&
                        mthread.getIntState() == 10   &&
                        agentStatus() ) {
                    passed = true;
                }
            }
        }
        System.out.println(" agentMethod() =  "+passed+".");
        return passed;
        }
}
class TempThread extends Thread {
    public MyThread mthread;
    public TempThread() {
        super("TempThread.");
    }

    public void run() {
        System.out.println(" run for TempThread ");
        mthread = new MyThread();
    }

}
