/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

/**
 * @test
 * @bug 6459476
 * @summary Debuggee is blocked,  looks like running
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g InterruptHangTest.java
 * @run driver InterruptHangTest
 */

/**
 * Debuggee has two threads.  Debugger keeps stepping in
 * the first thread.  The second thread keeps interrupting the first
 * thread.  If a long time goes by with the debugger not getting
 * a step event, the test fails.
 */
class InterruptHangTarg {
    public static String sync = "sync";
    public static void main(String[] args){
        int answer = 0;
        System.out.println("Howdy!");
        Interruptor interruptorThread = new Interruptor(Thread.currentThread());

        synchronized(sync) {
            interruptorThread.start();
            try {
                sync.wait();
            } catch (InterruptedException ee) {
                System.out.println("Debuggee interruptee: interrupted before starting loop");
            }
        }

        // Debugger will keep stepping thru this loop
        for (int ii = 0; ii < 200; ii++) {
            answer++;
            try {
                // Give other thread a chance to run
                Thread.sleep(100);
            } catch (InterruptedException ee) {
                System.out.println("Debuggee interruptee: interrupted at iteration: "
                                   + ii);
            }
        }
        // Kill the interrupter thread
        interruptorThread.interrupt();
        System.out.println("Goodbye from InterruptHangTarg!");
    }
}

class Interruptor extends Thread {
    Thread interruptee;
    Interruptor(Thread interruptee) {
        this.interruptee = interruptee;
    }

    public void run() {
        synchronized(InterruptHangTarg.sync) {
            InterruptHangTarg.sync.notify();
        }

        int ii = 0;
        while(true) {
            ii++;
            interruptee.interrupt();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ee) {
                System.out.println("Debuggee Interruptor: finished after " +
                                   ii + " iterrupts");
                break;
            }

        }
    }
}

    /********** test program **********/

public class InterruptHangTest extends TestScaffold {
    ThreadReference mainThread;
    Thread timerThread;
    String sync = "sync";
    static int nSteps = 0;

    InterruptHangTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new InterruptHangTest(args).startTests();
    }

    /********** event handlers **********/

    public void stepCompleted(StepEvent event) {
        synchronized(sync) {
            nSteps++;
        }
        println("Got StepEvent " + nSteps + " at line " +
                event.location().method() + ":" +
                event.location().lineNumber());
        if (nSteps == 1) {
            timerThread.start();
        }
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        BreakpointEvent bpe = startToMain("InterruptHangTarg");
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();

        /*
         * Set event requests
         */
        StepRequest request = erm.createStepRequest(mainThread,
                                                    StepRequest.STEP_LINE,
                                                    StepRequest.STEP_OVER);
        request.enable();

        // Will be started by the step event handler
        timerThread = new Thread("test timer") {
                public void run() {
                    int mySteps = 0;
                    float timeoutFactor = Float.parseFloat(System.getProperty("test.timeout.factor", "1.0"));
                    long sleepSeconds = (long)(20 * timeoutFactor);
                    println("Timer watching for steps every " + sleepSeconds + " seconds");
                    while (true) {
                        try {
                            Thread.sleep(sleepSeconds * 1000);
                            synchronized(sync) {
                                println("steps = " + nSteps);
                                if (mySteps == nSteps) {
                                    // no step for a long time
                                    failure("failure: Debuggee appears to be hung (no steps for " + sleepSeconds + "s)");
                                    vm().exit(-1);
                                    break;
                                }
                            }
                            mySteps = nSteps;
                        } catch (InterruptedException ee) {
                            break;
                        }
                    }
                }
            };

        /*
         * resume the target listening for events
         */

        listenUntilVMDisconnect();
        timerThread.interrupt();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("InterruptHangTest: passed");
        } else {
            throw new Exception("InterruptHangTest: failed");
        }
    }
}
