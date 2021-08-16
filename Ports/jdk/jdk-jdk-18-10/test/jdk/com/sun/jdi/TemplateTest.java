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

// TEMPLATE: global replace "Template" with your test name
// TEMPLATE: change bug number and fill out <SUMMARY> and <AUTHOR>
// TEMPLATE: delete TEMPLATE lines
/**
 * @test
 * @bug 0000000
 * @summary <SUMMARY>
 * @author <AUTHOR>
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g TemplateTest.java
 * @run driver TemplateTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class TemplateTarg {
    public static void main(String[] args){
        System.out.println("Howdy!");
        System.out.println("Goodbye from TemplateTarg!");
    }
}

    /********** test program **********/

public class TemplateTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    TemplateTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new TemplateTest(args).startTests();
    }

    /********** event handlers **********/

// TEMPLATE: delete the handlers you don't need
// TEMPLATE: defaults are in TargetAdapter

    public void eventSetReceived(EventSet set) {
        println("Got event set");
    }

    public void eventReceived(Event event) {
        println("Got event");
    }

    public void breakpointReached(BreakpointEvent event) {
        println("Got BreakpointEvent");
    }

    public void exceptionThrown(ExceptionEvent event) {
        println("Got ExceptionEvent");
    }

    public void stepCompleted(StepEvent event) {
        println("Got StepEvent");
    }

    public void classPrepared(ClassPrepareEvent event) {
        println("Got ClassPrepareEvent");
    }

    public void classUnloaded(ClassUnloadEvent event) {
        println("Got ClassUnloadEvent");
    }

    public void methodEntered(MethodEntryEvent event) {
        println("Got MethodEntryEvent");
    }

    public void methodExited(MethodExitEvent event) {
        println("Got MethodExitEvent");
    }

    public void fieldAccessed(AccessWatchpointEvent event) {
        println("Got AccessWatchpointEvent");
    }

    public void fieldModified(ModificationWatchpointEvent event) {
        println("Got ModificationWatchpointEvent");
    }

    public void threadStarted(ThreadStartEvent event) {
        println("Got ThreadStartEvent");
    }

    public void threadDied(ThreadDeathEvent event) {
        println("Got ThreadDeathEvent");
    }

    public void vmStarted(VMStartEvent event) {
        println("Got VMStartEvent");
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
        BreakpointEvent bpe = startToMain("TemplateTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();

// TEMPLATE: set things up

// TEMPLATE: for example
        /*
         * Set event requests
         */
        StepRequest request = erm.createStepRequest(mainThread,
                                                    StepRequest.STEP_LINE,
                                                    StepRequest.STEP_OVER);
        request.enable();

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("TemplateTest: passed");
        } else {
            throw new Exception("TemplateTest: failed");
        }
    }
}
