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
 * @bug 4453310
 * @summary Test the deletion of event requests that are expired
 * by virtue of addCountFilter.
 *
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g ExpiredRequestDeletionTest.java
 * @run driver ExpiredRequestDeletionTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class ExpiredRequestDeletionTarg {
    int foo = 9;

    public static void main(String[] args){
        System.out.println("Why, hello there...");
        (new ExpiredRequestDeletionTarg()).bar();
    }

    void bar() {
        ++foo;
    }
}

    /********** test program **********/

public class ExpiredRequestDeletionTest extends TestScaffold {
    EventRequestManager erm;
    ReferenceType targetClass;
    ThreadReference mainThread;
    Throwable throwable = null;

    ExpiredRequestDeletionTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new ExpiredRequestDeletionTest(args).startTests();
    }

    /********** event handlers **********/

    public void breakpointReached(BreakpointEvent event) {
        try {
            EventRequest req = event.request();
            if (req != null) {
                println("Deleting BreakpointRequest");
                erm.deleteEventRequest(req);
            } else {
                println("Got BreakpointEvent with null request");
            }
        } catch (Throwable exc) {
            throwable = exc;
            failure("Deleting BreakpointRequest threw - " + exc);
        }
    }

    public void stepCompleted(StepEvent event) {
        try {
            EventRequest req = event.request();
            if (req != null) {
                println("Deleting StepRequest");
                erm.deleteEventRequest(req);
            } else {
                println("Got StepEvent with null request");
            }
        } catch (Throwable exc) {
            throwable = exc;
            failure("Deleting StepRequest threw - " + exc);
        }
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("ExpiredRequestDeletionTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        erm = vm().eventRequestManager();

        List meths = targetClass.methodsByName("bar");
        if (meths.size() != 1) {
            throw new Exception("test error: should be one bar()");
        }
        Method barMethod = (Method)meths.get(0);

        /*
         * Set event requests
         */
        StepRequest sr = erm.createStepRequest(mainThread,
                                                    StepRequest.STEP_LINE,
                                                    StepRequest.STEP_OVER);
        sr.addCountFilter(1);
        sr.enable();

        BreakpointRequest bpr =
            erm.createBreakpointRequest(barMethod.location());
        bpr.addCountFilter(1);
        bpr.enable();

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("ExpiredRequestDeletionTest: passed");
        } else {
            throw new Exception("ExpiredRequestDeletionTest: failed", throwable);
        }
    }
}
