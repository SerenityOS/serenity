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

package nsk.jdi.ThreadReference.isSuspended;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ThreadReference.                                             <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ThreadReference.isSuspended()</code>       <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * After being started up,                                              <BR>
 * a debuggee creates a 'lockingObject' for synchronizing threads,      <BR>
 * enters a synchronized block in which it creates new thread, thread2, <BR>
 * informs a debugger of the thread creation, and is waiting for reply. <BR>
 * Since the thread2 uses the same locking object as main one           <BR>
 * it is locked up until the main thread leaves the synchronized block. <BR>
 * Upon the receiption a message from the debuggee, the debugger        <BR>
 * performs the following.                                              <BR>
 * (1) Using the method ThreadReference.isSuspended() to get thread2's  <BR>
 * state in the following three pairs of suspend-resume methods:        <BR>
 *         ThreadReference.suspend() - ThreadReference.resume()         <BR>
 *         ThreadReference.suspend() - VirtualMachine.resume()          <BR>
 *         VirtualMachine.suspend()  - VirtualMachine.resume()          <BR>
 * (2) sets up three breakpoints for the thread2 at which it            <BR>
 * checks up thread2's state in the following pairs:                    <BR>
 *         suspended_at_breakpoint - EventSet.resume()                  <BR>
 *         suspended_at_breakpoint - ThreadReference.resume()           <BR>
 *         suspended_at_breakpoint - VirtualMachine.resume()            <BR>
 * <BR>
 */

public class issuspended001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ThreadReference/isSuspended/issuspended001  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //    sHeader2 = "--> issuspended003: ",
    //    sHeader3 = "##> issuspended003: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new issuspended001().runThis(argv, out);
    }

    //--------------------------------------------------   log procedures

    private static Log  logHandler;

    private static void log1(String message) {
        logHandler.display(sHeader1 + message);
    }
    private static void log2(String message) {
        logHandler.display(sHeader2 + message);
    }
    private static void log3(String message) {
        logHandler.complain(sHeader3 + message);
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.ThreadReference.isSuspended.issuspended001a";

    private String testedClassName =
        "nsk.jdi.ThreadReference.isSuspended.Threadissuspended001a";

    //String mName = "nsk.jdi.ThreadReference.isSuspended";

    //====================================================== test program
    //------------------------------------------------------ common section
    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine      vm            = null;
    static EventRequestManager eventRManager = null;
    static EventQueue          eventQueue    = null;
    static EventSet            eventSet      = null;

    ReferenceType     testedclass  = null;
    ThreadReference   thread2      = null;
    ThreadReference   mainThread   = null;

    static int  testExitCode = PASSED;

    static final int returnCode0 = 0;
    static final int returnCode1 = 1;
    static final int returnCode2 = 2;
    static final int returnCode3 = 3;
    static final int returnCode4 = 4;

    //------------------------------------------------------ methods

    private int runThis (String argv[], PrintStream out) {

        Debugee debuggee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);
        }

        waitTime = argsHandler.getWaitTime();


        IOPipe pipe     = new IOPipe(debuggee);

        debuggee.redirectStderr(out);
        log2("issuspended001a debuggee launched");
        debuggee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        vm = debuggee.VM();

    //------------------------------------------------------  testing section
        log1("      TESTING BEGINS");

        for (int i = 0; ; i++) {
        pipe.println("newcheck");
            line = pipe.readln();

            if (line.equals("checkend")) {
                log2("     : returned string is 'checkend'");
                break ;
            } else if (line.equals("waitnotifyerr")) {
                log2("     : returned string is 'waitnotifyerr'");
                break ;
            } else if (!line.equals("checkready")) {
                log3("ERROR: returned string is not 'checkready'");
                testExitCode = FAILED;
                break ;
            }

            log1("new checkready: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            int expresult = returnCode0;


            eventRManager = vm.eventRequestManager();
            eventQueue    = vm.eventQueue();

            String threadName = "testedThread";

            String breakpointMethod1 = "runt1";
            String breakpointMethod2 = "runt2";

            String bpLine1 = "breakpointLineNumber1";
            String bpLine2 = "breakpointLineNumber2";
            String bpLine3 = "breakpointLineNumber3";


            List            allThreads   = null;
            ListIterator    listIterator = null;
            List            classes      = null;

            BreakpointRequest breakpRequest1 = null;
            BreakpointRequest breakpRequest2 = null;
            BreakpointRequest breakpRequest3 = null;

            int suspCount = 0;
            int frameCount;


            label0: {

                log2("getting ThreadReference objects and setting up breakponts");
                try {
                    allThreads  = vm.allThreads();
                    classes     = vm.classesByName(testedClassName);
                    testedclass = (ReferenceType) classes.get(0);
                } catch ( Exception e) {
                    log3("ERROR: Exception at very beginning !? : " + e);
                    expresult = returnCode1;
                    break label0;
                }

                listIterator = allThreads.listIterator();
                for (;;) {
                    try {
                        thread2 = (ThreadReference) listIterator.next();
                        if (thread2.name().equals(threadName))
                            break ;
                    } catch ( NoSuchElementException e ) {
                        log3("ERROR: NoSuchElementException for listIterator.next()");
                        log3("ERROR: NO THREAD2 ?????????!!!!!!!");
                        expresult = returnCode1;
                        break label0;
                    }
                }

                log2("getting ThreadReference objects for main thread");
                listIterator = allThreads.listIterator();
                for (;;) {
                    try {
                        mainThread = (ThreadReference) listIterator.next();
                        if (mainThread.name().equals("main"))
                            break ;
                    } catch ( NoSuchElementException e ) {
                        log3("ERROR: NoSuchElementException for listIterator.next() for 'mainThread'");
                        log3("ERROR: NO MAIN THREAD ?????????!!!!!!!");
                        expresult = returnCode1;
                        break label0;
                    }
                }

                log2("setting up breakpoints");

                breakpRequest1 = settingBreakpoint(breakpointMethod1, bpLine1, "one");
                if (breakpRequest1 == null) {
                    expresult = returnCode1;
                    break label0;
                }
                breakpRequest3 = settingBreakpoint(breakpointMethod1, bpLine3, "three");
                if (breakpRequest3 == null) {
                    expresult = returnCode1;
                    break label0;
                }
                breakpRequest2 = settingBreakpoint(breakpointMethod2, bpLine2, "two");
                if (breakpRequest2 == null) {
                    expresult = returnCode1;
                    break label0;
                }
            }

            label1: {

                if (expresult != returnCode0)
                    break label1;

                log2("     checking up the thread2 before to begin testing it");

                suspCount = thread2.suspendCount();
                log2("         suspendCount == " + suspCount);
                if (!thread2.isSuspended()) {
                    log2("     :  !thread2.isSuspended()");
                } else {
                    log3("ERROR:  thread2.isSuspended()");
                    expresult = returnCode1;
                }

                if (expresult != returnCode0)
                    break label1;

                log2(".....testing case 1:");
                log2(".....the thread2 is suspended with thread2.suspend();");
                log2(".....and resumed with thread2.resume();");

                thread2.suspend();

                suspCount = thread2.suspendCount();
                log2("     checking up the thread2 after suspending it");
                if (thread2.isSuspended()) {
                    log2("     :   thread2.isSuspended()");
                } else {
                    log3("ERROR:  !thread2.isSuspended()");
                    expresult = returnCode1;
                }
                log2("         suspendCount == " + suspCount);

                thread2.resume();

                suspCount = thread2.suspendCount();
                log2("     checking up the thread2 after resuming it");
                if (!thread2.isSuspended()) {
                    log2("     :  !thread2.isSuspended()");
                } else {
                    log3("ERROR:   thread2.isSuspended()");
                    expresult = returnCode1;
                }
                log2("         suspendCount == " + suspCount);


                log2(".....testing case 2:");
                log2(".....the thread2 is suspended with thread2.suspend();");
                log2(".....and resumed with vm.resume();");

                thread2.suspend();

                suspCount = thread2.suspendCount();
                log2("     checking up the thread2 after suspending it");
                if (thread2.isSuspended()) {
                    log2("     :   thread2.isSuspended()");
                } else {
                    log3("ERROR:  !thread2.isSuspended()");
                    expresult = returnCode1;
                }
                log2("         suspendCount == " + suspCount);

                vm.resume();

                suspCount = thread2.suspendCount();
                log2("     checking up the thread2 after resuming the application in VM");
                if (!thread2.isSuspended()) {
                    log2("     :  !thread2.isSuspended()");
                } else {
                    log3("ERROR:   thread2.isSuspended()");
                    expresult = returnCode1;
                }
                log2("         suspendCount == " + suspCount);


                log2(".....testing case 3:");
                log2(".....the thread2 is suspended with vm.suspend();");
                log2(".....and resumed with vm.resume();");

                vm.suspend();

                suspCount = thread2.suspendCount();
                log2("     checking up the thread2 after suspending the application in VM");
                if (thread2.isSuspended()) {
                    log2("     :   thread2.isSuspended()");
                } else {
                    log3("ERROR:  !thread2.isSuspended()");
                    expresult = returnCode1;
                }
                log2("         suspendCount == " + suspCount);

                vm.resume();

                suspCount = thread2.suspendCount();
                log2("     checking up the thread2 after resuming the application in VM");
                if (!thread2.isSuspended()) {
                    log2("     :  !thread2.isSuspended()");
                } else {
                    log3("ERROR:   thread2.isSuspended()");
                    expresult = returnCode1;
                }
                log2("         suspendCount == " + suspCount);
            }

            if (expresult  == returnCode0) {
                log2("     enabling breakpRequest1");
                breakpRequest1.enable();
            }

            log2("     instructing main thread to leave the synchronized block");
            pipe.println("continue");
            line = pipe.readln();
            if (!line.equals("out_of_synchronized")) {
                log3("ERROR: returned string is not 'out_of_synchronized'");
                expresult = returnCode2;
            }

            label2: {

                if (expresult == returnCode0)
                    expresult = breakpoint();
                if (expresult != returnCode0)
                    break label2;

                log2("      the thread2 is at first breakpoint");
                try {
                    frameCount = thread2.frameCount();
                    log2("         frameCount   == " + frameCount);
                } catch ( Exception e ) {
                    log3("ERROR: Exception for : frameCount = thread2.frameCount();   : " + e);
                    expresult = returnCode1;
                }

                log2("      checking up that thread2 is suspended at first breakpoint");
                log2("         suspendCount == " + thread2.suspendCount());
                if ( thread2.isAtBreakpoint() ) {
                    log2("     :   thread2.isAtBreakpoint()");
                } else {
                    log3("ERROR:  !thread2.isAtBreakpoint()");
                    expresult = returnCode1;
                }
                if ( thread2.isSuspended() ) {
                    log2("     :   thread2.isSuspended()");
                } else {
                    log3("ERROR:  !thread2.isSuspended()");
                    expresult = returnCode1;
                }
            }

            label3: {

                if (expresult != returnCode0)
                    break label3;

                log2("     instructing main thread to enter the synchronized block");
                pipe.println("enter_synchronized");
                line = pipe.readln();
                if (!line.equals("in_synchronized")) {
                    log3("ERROR: returned string is not 'in_synchronized'");
                    expresult = returnCode4;
                    break label3;
                }

                eventSet.resume();

                log2("      the thread2 has left the breakpoint");

                log2(".....testing case 4: suspended_at_breakpoint - eventSet.resume();");
                log2(".....checking up that the thread2 is not at first breakpoint");
                if ( !thread2.isAtBreakpoint() ) {
                    log2("     :  !thread2.isAtBreakpoint()");
                } else {
                    log3("ERROR:   thread2.isAtBreakpoint()");
                    expresult = returnCode1;
                }
                log2("      checking up that the thread2 is not suspended");
                log2("         suspendCount == " + thread2.suspendCount());
                if ( !thread2.isSuspended() ) {
                    log2("     :  !thread2.isSuspended() ");
                } else {
                    log3("ERROR:   thread2.isSuspended()");
                    expresult = returnCode1;
                }

                if (expresult == returnCode0) {
                    log2("     enabling breakpRequest2");
                    breakpRequest2.enable();
                }

                log2("     instructing main thread to leave the synchronized block");

                pipe.println("leave_synchronized");
                line = pipe.readln();
                if (!line.equals("out_of_synchronized")) {
                    log3("ERROR: returned string is not 'out_of_synchronized'");
                    expresult = returnCode4;
                }
            }

            label4: {

                if (expresult == returnCode0)
                    expresult = breakpoint();
                if (expresult != returnCode0)
                    break label4;

                log2("      the thread2 is at second breakpoint");
                try {
                    frameCount = thread2.frameCount();
                    log2("         frameCount   == " + frameCount);
                } catch ( Exception e ) {
                    log3("ERROR: Exception for : frameCount = thread2.frameCount();   : " + e);
                    expresult = returnCode1;
                }

                log2("      checking up that the thread2 is suspended at second breakpoint");
                log2("         suspendCount == " + thread2.suspendCount());
                if ( thread2.isAtBreakpoint() ) {
                    log2("     :   thread2.isAtBreakpoint()");
                } else {
                    log3("ERROR:  !thread2.isAtBreakpoint()");
                    expresult = returnCode1;
                }
                if ( thread2.isSuspended() ) {
                    log2("     :   thread2.isSuspended()");
                } else {
                    log3("ERROR:  !thread2.isSuspended()");
                    expresult = returnCode1;
                }
            }

            label5: {

                if (expresult != returnCode0)
                    break label5;

                log2("     instructing main thread to enter the synchronized block");
                pipe.println("enter_synchronized");
                line = pipe.readln();
                if (!line.equals("in_synchronized")) {
                    log3("ERROR: returned string is not 'in_synchronized'");
                    expresult = returnCode4;
                    break label5;
                }

                thread2.resume();

                log2("      the thread2 has left the breakpoint");

                log2(".....testing case 5: suspended_at_breakpoint - thread2.resume();");
                log2(".....checking up that the thread is not at a breakpoint");
                if ( !thread2.isAtBreakpoint() ) {
                    log2("     :  !thread2.isAtBreakpoint() ");
                } else {
                    log3("ERROR:   thread2.isAtBreakpoint()");
                    expresult = returnCode1;
                }
                log2("      checking up that the thread is not suspended");
                log2("         suspendCount == " + thread2.suspendCount());
                if ( !thread2.isSuspended() ) {
                    log2("     :  !thread2.isSuspended() ");
                } else {
                    log3("ERROR:   thread2.isSuspended()");
                    expresult = returnCode1;
                }

                if (expresult == returnCode0) {
                    log2("     enabling breakpRequest3");
                    breakpRequest3.enable();
                }

                log2("     instructing main thread to leave the synchronized block");
                pipe.println("leave_synchronized");
                line = pipe.readln();
                if (!line.equals("out_of_synchronized")) {
                    log3("ERROR: returned string is not 'out_of_synchronized'");
                    expresult = returnCode4;
                }
            }

            label6: {

                if (expresult == returnCode0)
                    expresult = breakpoint();
                if (expresult != returnCode0)
                    break label6;

                log2("      the thread2 is at third breakpoint");
                try {
                    frameCount = thread2.frameCount();
                    log2("         frameCount   == " + frameCount);
                } catch ( Exception e ) {
                    log3("ERROR: Exception for : frameCount = thread2.frameCount();   : " + e);
                    expresult = returnCode1;
                }

                log2("      checking up that the thread2 is suspended at third breakpoint");
                log2("         suspendCount == " + thread2.suspendCount());
                if ( thread2.isAtBreakpoint() ) {
                    log2("     :   thread2.isAtBreakpoint()");
                } else {
                    log3("ERROR:  !thread2.isAtBreakpoint()");
                    expresult = returnCode1;
                }
                if ( thread2.isSuspended() ) {
                    log2("     :   thread2.isSuspended()");
                } else {
                    log3("ERROR:  !thread2.isSuspended()");
                    expresult = returnCode1;
                }

                vm.resume();

                log2("      the thread has left the breakpoint");

                log2(".....testing case 6: suspended_at_breakpoint - vm.resume();");
                log2(".....checking up that the thread is not at a breakpoint");
                if ( !thread2.isAtBreakpoint() ) {
                    log2("     :  !thread2.isAtBreakpoint() ");
                } else {
                    log3("ERROR:   thread2.isAtBreakpoint()");
                    expresult = returnCode1;
                }
                log2("      checking that the thread is not suspended");
                log2("         suspendCount == " + thread2.suspendCount());
                if ( !thread2.isSuspended() ) {
                    log2("     :  !thread2.isSuspended() ");
                } else {
                    log3("ERROR:   thread2.isSuspended()");
                    expresult = returnCode1;
                }
            }

            if (expresult != returnCode4) {
                if (line.equals("in_synchronized")) {
                    log2("instrucring the main thread to leave synchronized block");
                    pipe.println("leave_synchronized");
                    line = pipe.readln();
                }
                pipe.println("continue");
                line = pipe.readln();
                if (!line.equals("docontinue")) {
                   log3("ERROR: returned string is not 'docontinue'");
                   expresult = returnCode2;
                }
            }

            log2("resuming vm; for case :");
            log2("the thread2 had been suspended at a breakpoint when the test has been aborted");
            vm.resume();

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            log2("     the end of testing");
            if (expresult != returnCode0)
                testExitCode = FAILED;
        }
        log1("      TESTING ENDS");

    //--------------------------------------------------   test summary section
    //-------------------------------------------------    standard end section

        pipe.println("quit");
        log2("waiting for the debuggee to finish ...");
        debuggee.waitFor();

        int status = debuggee.getStatus();
        if (status != PASSED + PASS_BASE) {
            log3("debuggee returned UNEXPECTED exit status: " +
                    status + " != PASS_BASE");
            testExitCode = FAILED;
        } else {
            log2("debuggee returned expected exit status: " +
                    status + " == PASS_BASE");
        }

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;
    }


   /*
    * private BreakpointRequest settingBreakpoint(String, String, String)
    *
    * It sets up a breakpoint within a given method at given line number
    * for the thread2 only.
    * Third parameter is required for any case in future debugging, as if.
    *
    * Return codes:
    *  = BreakpointRequest object  in case of success
    *  = null   in case of an Exception thrown within the method
    */

    private BreakpointRequest settingBreakpoint ( String methodName,
                                                  String bpLine,
                                                  String property) {

        log2("setting up a breakpoint: method: '" + methodName + "' line: " + bpLine );

        List              alllineLocations = null;
        Location          lineLocation     = null;
        BreakpointRequest breakpRequest    = null;

        try {
            Method  method  = (Method) testedclass.methodsByName(methodName).get(0);

            alllineLocations = method.allLineLocations();

            int n =
                ( (IntegerValue) testedclass.getValue(testedclass.fieldByName(bpLine) ) ).value();
            if (n > alllineLocations.size()) {
                log3("ERROR:  TEST_ERROR_IN_settingBreakpoint(): number is out of bound of method's lines");
            } else {
                lineLocation = (Location) alllineLocations.get(n);
                try {
                    breakpRequest = eventRManager.createBreakpointRequest(lineLocation);
                    breakpRequest.putProperty("number", property);
                    breakpRequest.addThreadFilter(thread2);
                    breakpRequest.setSuspendPolicy( EventRequest.SUSPEND_EVENT_THREAD);
                } catch ( Exception e1 ) {
                    log3("ERROR: inner Exception within settingBreakpoint() : " + e1);
                    breakpRequest    = null;
                }
            }
        } catch ( Exception e2 ) {
            log3("ERROR: ATTENTION:  outer Exception within settingBreakpoint() : " + e2);
            breakpRequest    = null;
        }

        if (breakpRequest == null)
            log2("      A BREAKPOINT HAS NOT BEEN SET UP");
        else
            log2("      a breakpoint has been set up");

        return breakpRequest;
    }


    /*
     * private int breakpoint ()
     *
     * It removes events from EventQueue until gets first BreakpointEvent.
     * To get next EventSet value, it uses the method
     *    EventQueue.remove(int timeout)
     * The timeout argument passed to the method, is "waitTime*60000".
     * Note: the value of waitTime is set up with
     *       the method ArgumentHandler.getWaitTime() at the beginning of the test.
     *
     * Return codes:
     *  = returnCode0 - success;
     *  = returnCode2 - Exception when "eventSet = eventQueue.remove()" is executed
     *  = returnCode3 - default case when loop of processing an event, that is,
     *                  an unspecified event was taken from the EventQueue
     */

    private int breakpoint () {

        int returnCode = returnCode0;

        log2("       waiting for BreakpointEvent");

        labelBP:
            for (;;) {

                log2("       new:  eventSet = eventQueue.remove();");
                try {
                    eventSet = eventQueue.remove(waitTime*60000);
                    if (eventSet == null) {
                        log3("ERROR:  timeout for waiting for a BreakpintEvent");
                        returnCode = returnCode2;
                        break labelBP;
                    }
                } catch ( Exception e ) {
                    log3("ERROR: Exception for  eventSet = eventQueue.remove(); : " + e);
                    returnCode = 1;
                    break labelBP;
                }

                if (eventSet != null) {

                    log2("     :  eventSet != null;  size == " + eventSet.size());

                    EventIterator eIter = eventSet.eventIterator();
                    Event         ev    = null;

                    for (; eIter.hasNext(); ) {

                        if (returnCode != returnCode0)
                            break;

                        ev = eIter.nextEvent();

                    ll: for (int ifor =0;  ; ifor++) {

                        try {
                          switch (ifor) {

                          case 0:  AccessWatchpointEvent awe = (AccessWatchpointEvent) ev;
                                   log2("      AccessWatchpointEvent removed");
                                   break ll;
                          case 1:  BreakpointEvent be = (BreakpointEvent) ev;
                                   log2("      BreakpointEvent removed");
                                   break labelBP;
                          case 2:  ClassPrepareEvent cpe = (ClassPrepareEvent) ev;
                                   log2("      ClassPreparEvent removed");
                                   break ll;
                          case 3:  ClassUnloadEvent cue = (ClassUnloadEvent) ev;
                                   log2("      ClassUnloadEvent removed");
                                   break ll;
                          case 4:  ExceptionEvent ee = (ExceptionEvent) ev;
                                   log2("      ExceptionEvent removed");
                                   break ll;
                          case 5:  MethodEntryEvent mene = (MethodEntryEvent) ev;
                                   log2("      MethodEntryEvent removed");
                                   break ll;
                          case 6:  MethodExitEvent mexe = (MethodExitEvent) ev;
                                   log2("      MethodExiEvent removed");
                                   break ll;
                          case 7:  ModificationWatchpointEvent mwe = (ModificationWatchpointEvent) ev;
                                   log2("      ModificationWatchpointEvent removed");
                                   break ll;
                          case 8:  StepEvent se = (StepEvent) ev;
                                   log2("      StepEvent removed");
                                   break ll;
                          case 9:  ThreadDeathEvent tde = (ThreadDeathEvent) ev;
                                   log2("      ThreadDeathEvent removed");
                                   break ll;
                          case 10: ThreadStartEvent tse = (ThreadStartEvent) ev;
                                   log2("      ThreadStartEvent removed");
                                   break ll;
                          case 11: VMDeathEvent vmde = (VMDeathEvent) ev;
                                   log2("      VMDeathEvent removed");
                                   break ll;
                          case 12: VMStartEvent vmse = (VMStartEvent) ev;
                                   log2("      VMStartEvent removed");
                                   break ll;
                          case 13: WatchpointEvent we = (WatchpointEvent) ev;
                                   log2("      WatchpointEvent removed");
                                   break ll;

                          default: log3("ERROR:  default case for casting event");
                                   returnCode = returnCode3;
                                   break ll;
                          } // switch
                        } catch ( ClassCastException e ) {
                        }   // try
                    }       // ll: for (int ifor =0;  ; ifor++)
                }           // for (; ev.hasNext(); )
            }
        }
        if (returnCode == returnCode0)
            log2("     :  eventSet == null:  EventQueue is empty");

        return returnCode;
    }


}
