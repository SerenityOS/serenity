/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.Event.hashCode;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;

/**
 * The debugger application of the test.
 */
public class hashcode001 {

    //------------------------------------------------------- immutable common fields

    final static String SIGNAL_READY = "ready";
    final static String SIGNAL_GO    = "go";
    final static String SIGNAL_QUIT  = "quit";

    private static int waitTime;
    private static int exitStatus;
    private static ArgumentHandler     argHandler;
    private static Log                 log;
    private static Debugee             debuggee;
    private static ReferenceType       debuggeeClass;

    //------------------------------------------------------- mutable common fields

    private final static String prefix = "nsk.jdi.Event.hashCode.";
    private final static String className = "hashcode001";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    private final static String threadClassName = prefix + "hashcode001aThread";
    private final static String threadName = "thread1";
    private final static String methodName = "foo";
    private final static String fieldName = "name";
    private final static int brkpLineNumber = 145;

    //------------------------------------------------------- immutable common methods

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        debuggee = Debugee.prepareDebugee(argHandler, log, debuggeeName);

        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            throw new Failure("Class '" + debuggeeName + "' not found.");
        }

        execTest();

        int status = debuggee.endDebugee();
        if ( status != Consts.JCK_STATUS_BASE ) {
            throw new Failure("UNEXPECTED Debugee's exit status : " + status);
        }
        display("expected Debugee's exit status : " + status);

        return exitStatus;
    }

    //------------------------------------------------------ mutable common method

    private static void execTest() {

        EventRequestManager eventRequestManager = debuggee.VM().eventRequestManager();
        EventRequest eventRequest;

        ReferenceType testedClass = debuggee.classByName(threadClassName);
        if ( testedClass == null ) {
            throw new Failure("Class '" + threadClassName + "' not found.");
        }

        for (int i = 0; i < 11; i++) {
            eventRequest = null;
            switch (i) {

                case 0:
                       ThreadReference thread = debuggee.threadByName(threadName);

                       display(".....setting up StepRequest");
                       eventRequest = eventRequestManager.createStepRequest
                                      (thread, StepRequest.STEP_MIN, StepRequest.STEP_INTO);
                       break;

                case 1:
                       display(".....setting up AccessWatchpointRequest");
                       eventRequest = eventRequestManager.createAccessWatchpointRequest
                                      (testedClass.fieldByName(fieldName));
                       break;

                case 2:
                       display(".....setting up ModificationWatchpointRequest");
                       eventRequest = eventRequestManager.createModificationWatchpointRequest
                                      (testedClass.fieldByName(fieldName));
                       break;

                case 3:
                       display(".....setting up ClassPrepareRequest");
                       eventRequest = eventRequestManager.createClassPrepareRequest();
                       break;

                case 4:
                       display(".....setting up ClassUnloadRequest");
                       eventRequest = eventRequestManager.createClassUnloadRequest();
                       break;

                case 5:
                       display(".....setting up MethodEntryRequest");
                       eventRequest = eventRequestManager.createMethodEntryRequest();
                       break;

                case 6:
                       display(".....setting up MethodExitRequest");
                       eventRequest = eventRequestManager.createMethodExitRequest();
                       break;

                case 7:
                       display(".....setting up ThreadDeathRequest");
                       eventRequest = eventRequestManager.createThreadDeathRequest();
                       break;

                case 8:
                       display(".....setting up ThreadStartRequest");
                       eventRequest = eventRequestManager.createThreadStartRequest();
                       break;

                case 9:
                       display(".....setting up ExceptionRequest");
                       eventRequest = eventRequestManager.createExceptionRequest( null, true, true );
                       break;

                case 10:
                       display(".....setting up BreakpointRequest");
                       eventRequest = debuggee.makeBreakpoint(testedClass, methodName, brkpLineNumber);
                       break;

                default:
                        throw new Failure("Wrong test case : " + i);
            }

            if (eventRequest == null) {
                throw new Failure("Cannot create request case : " + i);
            }
            eventRequest.addCountFilter(1);
            eventRequest.enable();
        }

        debuggee.sendSignal(SIGNAL_GO);

        display("Checking hashCode() method for Event objects");

        boolean exit = false;
        boolean connected = true;
        while (!exit) {
            EventSet eventSet = getEventSet();
            EventIterator eventIterator = eventSet.eventIterator();

            while (eventIterator.hasNext()) {
                Event event = eventIterator.nextEvent();
                String eventName = null;

                if (event instanceof VMDeathEvent) {
                    eventName = "VMDeathEvent";
                    connected = false;
                } else if (event instanceof VMDisconnectEvent) {
                    eventName = "VMDisconnectEvent";
                    connected = false;
                    exit = true;
                } else {
                    eventName = "" + event;
                }

                try {
                    int hCode = event.hashCode();
                    display("hashCode() returns " + hCode + " for Event object : " +  eventName);

                    if (hCode == 0) {
                        complain("hashCode() returns 0 for Event object " + eventName);
                        exitStatus = Consts.TEST_FAILED;
                    }

                    int hCode1 = event.hashCode();
                    if (hCode != hCode1) {
                        complain("hashCode() is not consistent for Event object " + eventName +
                            "\n\t first value :" + hCode + " ; second value : " + hCode1);
                        exitStatus = Consts.TEST_FAILED;
                    }

                } catch (VMDisconnectedException e) {
                    // skip error message if already disconnected
                    if (connected) {
                        throw new Failure("Caught unexpected VMDisconnectedException:\n\t" + e);
                    }
                }
            }
            if (connected) {
                eventSet.resume();
            }
        }
        display("Checking completed!");
    }

    //--------------------------------------------------------- test specific methods

    private static EventSet getEventSet() {
        EventSet eventSet;
        try {
            eventSet = debuggee.VM().eventQueue().remove(waitTime);
            if (eventSet == null) {
                throw new Failure("TIMEOUT while waiting for event");
            }
        } catch ( Exception e ) {
            throw new Failure("Unexpected exception while waiting for event " + e);
        }
        return eventSet;
    }
}
//--------------------------------------------------------- test specific classes
