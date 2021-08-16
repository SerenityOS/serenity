/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4364671
 * @summary Creating a StepRequest on a nonexistant thread fails
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g AfterThreadDeathTest.java
 * @run driver AfterThreadDeathTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class AfterDeathTarg {
    public static void main(String[] args){
        System.out.println("Howdy!");
        System.out.println("Goodbye from AfterDeathTarg!");
    }
}

    /********** test program **********/

public class AfterThreadDeathTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    StepRequest stepRequest = null;
    EventRequestManager erm;
    boolean mainIsDead;

    AfterThreadDeathTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new AfterThreadDeathTest(args).startTests();
    }

    /********** event handlers **********/

    public void threadStarted(ThreadStartEvent event) {
        println("Got ThreadStartEvent: " + event);

        if (stepRequest != null) {
            erm.deleteEventRequest(stepRequest);
            stepRequest = null;
            println("Deleted stepRequest");
        }

        if (mainIsDead) {
            // Here is the odd thing about this test; whatever thread this event
            // is for, we do a step on the mainThread. If the mainThread is
            // already dead, we should get the exception.  Note that we don't
            // come here for the start of the main thread.
            stepRequest = erm.createStepRequest(mainThread,
                                                StepRequest.STEP_LINE,
                                                StepRequest.STEP_OVER);
            stepRequest.addCountFilter(1);
            stepRequest.setSuspendPolicy (EventRequest.SUSPEND_ALL);
            try {
                stepRequest.enable();
            } catch (IllegalThreadStateException ee) {
                println("Ok; got expected IllegalThreadStateException");
                return;
            } catch (Exception ee) {
                failure("FAILED: Did not get expected"
                      + " IllegalThreadStateException"
                      + " on a StepRequest.enable().  \n"
                      + "        Got this exception instead: " + ee);
                return;
            }
            failure("FAILED: Did not get expected IllegalThreadStateException"
                    + " on a StepRequest.enable()");
        }
    }

    public void threadDied(ThreadDeathEvent event) {
        println("Got ThreadDeathEvent: " + event);
        if (! mainIsDead) {
            if (mainThread.equals(event.thread())) {
                mainIsDead = true;
            }
        }
    }

    public void vmDied(VMDeathEvent event) {
        println("Got VMDeathEvent");
    }

    public void vmDisconnected(VMDisconnectEvent event) {
        println("Got VMDisconnectEvent");
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("AfterDeathTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        erm = vm().eventRequestManager();

        /*
         * Set event requests
         */
        ThreadStartRequest request = erm.createThreadStartRequest();
        request.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        request.enable();

        ThreadDeathRequest request1 = erm.createThreadDeathRequest();
        request1.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        request1.enable();

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("AfterThreadDeathTest: passed");
        } else {
            throw new Exception("AfterThreadDeathTest: failed");
        }
    }
}
