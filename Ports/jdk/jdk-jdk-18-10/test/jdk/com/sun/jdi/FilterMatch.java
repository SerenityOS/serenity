/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4331522
 * @summary addClassFilter("Foo") acts like "Foo*"
 * @author Robert Field/Jim Holmlund
 *
 * @run build TestScaffold VMConnection
 * @run compile -g HelloWorld.java
 * @run driver FilterMatch
 */

/* Look at patternMatch in JDK file:
 *    .../src/share/back/eventHandler.c
 */

/*
 *  This test tests patterns passed to addClassFilter that do match
 *  the classname of the event.  See also testcase FilterNoMatch.java.
 */

import java.util.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

public class FilterMatch extends TestScaffold {

    EventSet eventSet = null;
    boolean stepCompleted = false;

    public static void main(String args[]) throws Exception {
        new FilterMatch(args).startTests();
    }

    public FilterMatch(String args[]) {
        super(args);
    }

    protected void runTests() throws Exception {
        /*
         * Get to the top of HelloWorld main() to determine referenceType and mainThread
         */
        BreakpointEvent bpe = startToMain("HelloWorld");
        ReferenceType referenceType = (ClassType)bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager requestManager = vm().eventRequestManager();

        Location loc = findLocation(referenceType, 3);
        BreakpointRequest bpRequest = requestManager.createBreakpointRequest(loc);

        try {
            addListener(this);
        } catch (Exception ex){
            ex.printStackTrace();
            failure("failure: Could not add listener");
            throw new Exception("FilterMatch: failed");
        }

        bpRequest.enable();

        StepRequest stepRequest = requestManager.createStepRequest(mainThread,
                                  StepRequest.STEP_LINE,StepRequest.STEP_OVER);

        // These patterns all match HelloWorld.  Since they all match, our
        // listener should get called and the test will pass.  If any of them
        // are erroneously determined to _not_ match, then our listener will
        // not get called and the test will fail.
        stepRequest.addClassFilter("*");

        stepRequest.addClassFilter("H*");
        stepRequest.addClassFilter("He*");
        stepRequest.addClassFilter("HelloWorld*");

        stepRequest.addClassFilter("*d");
        stepRequest.addClassFilter("*ld");
        stepRequest.addClassFilter("*HelloWorld");

        stepRequest.addClassFilter("HelloWorld");

        // As a test, uncomment this line and the test should fail.
        //stepRequest.addClassFilter("x");

        stepRequest.enable();

        vm().resume();

        waitForVMDisconnect();

        if (!stepCompleted) {
            throw new Exception( "Failed: Event filtered out.");
        }
        System.out.println( "Passed: Event not filtered out.");
    }

    // ****************  event handlers **************

    public void eventSetReceived(EventSet set) {
        this.eventSet = set;
    }

    // This gets called if all filters match.
    public void stepCompleted(StepEvent event) {
        stepCompleted = true;
        System.out.println("StepEvent at" + event.location());
        // disable the step and then run to completion
        StepRequest str = (StepRequest)event.request();
        str.disable();
        eventSet.resume();
    }

    public void breakpointReached(BreakpointEvent event) {
        System.out.println("BreakpointEvent at" + event.location());
        BreakpointRequest bpr = (BreakpointRequest)event.request();
        // The bkpt was hit; disable it.
        bpr.disable();
    }
}
