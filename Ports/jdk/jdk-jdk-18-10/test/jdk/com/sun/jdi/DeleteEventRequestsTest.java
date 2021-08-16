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
 * @bug 4331872
 * @summary erm.deleteEventRequests(erm.breakpointRequests()) throws exception
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g DeleteEventRequestsTest.java
 * @run driver DeleteEventRequestsTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class DeleteEventRequestsTarg {
    public static void main(String[] args){
        System.out.println("Howdy!");
        System.out.println("Goodbye from DeleteEventRequestsTarg!");
    }
}

    /********** test program **********/

public class DeleteEventRequestsTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    DeleteEventRequestsTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new DeleteEventRequestsTest(args).startTests();
    }

    /********** event handlers **********/

    public void stepCompleted(StepEvent event) {
        failure("Got StepEvent which was deleted");
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("DeleteEventRequestsTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();

        /*
         * Set event requests
         */
        StepRequest request = erm.createStepRequest(mainThread,
                                                    StepRequest.STEP_LINE,
                                                    StepRequest.STEP_OVER);
        request.enable();

        /*
         * This should not die with ConcurrentModificationException
         */
        erm.deleteEventRequests(erm.stepRequests());

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("DeleteEventRequestsTest: passed");
        } else {
            throw new Exception("DeleteEventRequestsTest: failed");
        }
    }
}
