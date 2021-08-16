/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdi/StackFrame/setValue/setvalue004.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     StackFrame.
 *     The test checks up that a result of the method
 *     com.sun.jdi.StackFrame.setValue.()
 *     complies with its spec:
 *     public void setValue(LocalVariable variable, Value value)
 *               throws InvalidTypeException, ClassNotLoadedException
 *     Sets the Value of a LocalVariable in this frame.
 *     The variable must be valid for this frame's method and visible
 *     according to the rules described in visibleVariables().
 *         Object values must be assignment compatible with the variable type
 *     (This implies that the variable type must be loaded through
 *     the enclosing class's class loader).
 *     Primitive values must be either assignment compatible with the variable type
 *     or must be convertible to the variable type without loss of information.
 *     See JLS section 5.2 for more information on assignment compatibility.
 *     Parameters: variable - the field containing the requested value
 *                 value    - the new value to assign
 *     Throws: java.lang.IllegalArgumentException -
 *             if the field is not valid for this object's class.
 *             InvalidTypeException -
 *             if the value's type does not match the variable's type.
 *             ClassNotLoadedException -
 *             if the variable type has not yet been loaded through
 *             the appropriate class loader.
 *             InvalidStackFrameException -
 *             if this stack frame has become invalid.
 *             Once the frame's thread is resumed,
 *             the stack frame is no longer valid.
 *             VMMismatchException -
 *             if a Mirror argument and this mirror do not belong
 *             to the same VirtualMachine.
 *     The case for testing includes checking up on assigning null to
 *     both PrimitiveType and ReferenceType obects
 *     when a tested program is prepared with full information (see COMMENTS),
 *     hence, AbsentInformationException is not expected to happen.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.StackFrame.setValue.setvalue.004;
 *     the debuggee program - nsk.jdi.StackFrame.setValue.setvalue.004a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM,
 *     establishes a pipe with the debuggee program, and then
 *     send to the programm commands, to which the debuggee replies
 *     via the pipe. Upon getting reply,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and compares the data got to the data expected.
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *     This test is option depended,
 *     that is its .cfg file contains the option
 *         JAVAC_OPTS=-g
 *     because at the date of preparing the test
 *     javac prepared full information for the test only
 *     been invoked with the option.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StackFrame.setValue.setvalue004.setvalue004
 *        nsk.jdi.StackFrame.setValue.setvalue004.setvalue004a
 *
 * @comment make sure setvalue004a is compiled with full debug info
 * @clean nsk.jdi.StackFrame.setValue.setvalue004.setvalue004a
 * @compile -g:lines,source,vars setvalue004a.java
 *
 * @run main/othervm
 *      nsk.jdi.StackFrame.setValue.setvalue004.setvalue004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.StackFrame.setValue.setvalue004;

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
 * StackFrame.                                                  <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.StackFrame.setValue()</code>               <BR>
 * complies with its spec in case when a tested program         <BR>
 * is prepared with full information (see README file),         <BR>
 * hence, AbsentInformationException is not expected to happen. <BR>
 * The test checks up throwing  InvalidTypeException            <BR>
 * <BR>
 * After being started up,                                              <BR>
 * a debuggee creates a 'lockingObject' for synchronizing threads,      <BR>
 * enters a synchronized block in which it creates new thread, thread2, <BR>
 * informs a debugger of the thread creation, and is waiting for reply. <BR>
 * Since the thread2 uses the same locking object as main one           <BR>
 * it is locked up until the main thread leaves the synchronized block. <BR>
 * Upon the receiption a message from the debuggee, the debugger        <BR>
 * sets up a breakpoint, and after getting the thread2 suspended,       <BR>
 * checks up that for each in primitive types                           <BR>
 * (1)    setValue(PrimitiveType variable, null) does throw whereas     <BR>
 * (2)    setValue(Object variable, null); does not throw               <BR>
 *            InvalidTypeException                                      <BR>
 * <BR>
 */

public class setvalue004 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/StackFrame/setValue/setvalue004/setvalue004  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new setvalue004().runThis(argv, out);
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
        "nsk.jdi.StackFrame.setValue.setvalue004.setvalue004a";

    private String testedClassName =
        "nsk.jdi.StackFrame.setValue.setvalue004.Threadsetvalue004a";

    //String mName = "nsk.jdi.StackFrame.setValue";

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
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");  // *** tp
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);            // *** tp
        }

        waitTime = argsHandler.getWaitTime();


        IOPipe pipe     = new IOPipe(debuggee);

        debuggee.redirectStderr(out);
        log2("setvalue004a debuggee launched");
        debuggee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        VirtualMachine vm = debuggee.VM();

    //------------------------------------------------------  testing section
        log1("      TESTING BEGINS");

        for (int i = 0; ; i++) {
        pipe.println("newcheck");
            line = pipe.readln();

            if (line.equals("checkend")) {
                log2("     : returned string is 'checkend'");
                break ;
            } else if (!line.equals("checkready")) {
                log3("ERROR: returned string is not 'checkready'");
                testExitCode = FAILED;
                break ;
            }

            log1("new check: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            int expresult = returnCode0;


            eventRManager = vm.eventRequestManager();
            eventQueue    = vm.eventQueue();

            String threadName = "testedThread";

            String breakpointMethod1 = "runt1";
            String breakpointMethod2 = "runt2";

            String bpLine1 = "breakpointLineNumber1";
            String bpLine2 = "breakpointLineNumber2";
            //String bpLine3 = "breakpointLineNumber3";


            List            allThreads   = null;
            ListIterator    listIterator = null;
            List            classes      = null;

            BreakpointRequest breakpRequest1 = null;
            BreakpointRequest breakpRequest2 = null;
            //BreakpointRequest breakpRequest3 = null;

            int suspCount = 0;
            int frameCount;

            StackFrame    stackFrame = null;
            LocalVariable locvar1    = null;
            LocalVariable locvar2    = null;

            label0: {

                log2("getting ThreadReference object");
                try {
                    allThreads  = vm.allThreads();
                    classes     = vm.classesByName(testedClassName);
                    testedclass = (ReferenceType) classes.get(0);
                } catch ( Exception e) {
                    log3("ERROR: Exception at very beginning !? : " + e);
                    expresult = returnCode1;
                    break label0;
                }

                thread2 = debuggee.threadByName(threadName);

                log2("setting up breakpoints");

                breakpRequest1 = settingBreakpoint(breakpointMethod1, bpLine1, "one");
                if (breakpRequest1 == null) {
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

                log2("     enabling breakpRequest1");
                breakpRequest1.enable();

                log2("       forcing the main thread to leave synchronized block");
                pipe.println("continue");
                line = pipe.readln();
                if (!line.equals("docontinue")) {
                    log3("ERROR: returned string is not 'docontinue'");
                    expresult = returnCode4;
                    break label1;
                }

                expresult = breakpoint();
                if (expresult != returnCode0)
                    break label1;

                log2("      the thread2 is at the breakpoint");
                log2("      the check that the thread2 is suspended at the breakpoint");
                if ( thread2.isSuspended() ) {
                    log2("     :   thread2.isSuspended()");
                } else {
                    log3("ERROR:  !thread2.isSuspended()");
                    expresult = returnCode1;
                    break label1;
                }

                try {
                    stackFrame = thread2.frame(0);
                } catch ( Exception e ) {
                    log3("ERROR: Exception for stackFrame = thread2.frame(0)    :" + e);
                    expresult = returnCode1;
                    break label1;
                }


                String names[] = { "bl1", "bt1", "ch1", "db1",
                                   "fl1", "in1", "ln1", "sh1" };

                log2("      checking up exception for: stackFrame.setValue(PrimitiveType variable, null);");
                for ( int i2 = 0; i2 < 8; i2++ ) {

                    try {
                        locvar1 = stackFrame.visibleVariableByName(names[i2]);
                        log2("      checking up exception for: LocalVariable of " + locvar1.typeName() + "  type");
                        stackFrame.setValue(locvar1, null);
                        log3("ERROR: no Exception");
                        expresult = returnCode1;
                    } catch ( java.lang.IllegalArgumentException e1 ) {
                        log3("ERROR:  java.lang.IllegalArgumentException  ");
                        expresult = returnCode1;
                    } catch ( InvalidTypeException e2 ) {
                        log2("     :  InvalidTypeException  ");
                    } catch ( ClassNotLoadedException e3 ) {
                        log3("ERROR:  ClassNotLoadedException ");
                        expresult = returnCode1;
                    } catch ( InvalidStackFrameException e4 ) {
                        log3("ERROR:  InvalidStackFrameException ");
                        expresult = returnCode1;
                    } catch ( VMMismatchException e5 ) {
                        log3("ERROR:  VMMismatchException  ");
                        expresult = returnCode1;
                    } catch ( AbsentInformationException e7 ) {
                        log3("ERROR:  AbsentInformationException  ");
                        expresult = returnCode1;
                    } catch ( Exception e6 ) {
                        log3("ERROR:  UNSPECIFIED EXCEPTION:  " + e6);
                        expresult = returnCode1;
                    }
                }


                String name = "testObj";

                log2("      checking up exception for: stackFrame.setValue(Object variable, null);");
                try {
                    locvar1 = stackFrame.visibleVariableByName(name);
                    if (locvar1 != null) {
                        stackFrame.setValue(locvar1, null);
                        log2("     : no Exception");
                    } else {
                        expresult = returnCode1;
                        log3("ERROR: locvar1 == null after: locvar1 = stackFrame.visibleVariableByName(name);");
                    }
                } catch ( java.lang.IllegalArgumentException e1 ) {
                    log3("ERROR:  java.lang.IllegalArgumentException  ");
                    expresult = returnCode1;
                } catch ( InvalidTypeException e2 ) {
                    log3("ERROR:  InvalidTypeException  ");
                    expresult = returnCode1;
                } catch ( ClassNotLoadedException e3 ) {
                    log3("ERROR:  ClassNotLoadedException ");
                    expresult = returnCode1;
                } catch ( InvalidStackFrameException e4 ) {
                    log3("ERROR:  InvalidStackFrameException ");
                    expresult = returnCode1;
                } catch ( VMMismatchException e5 ) {
                    log3("ERROR:  VMMismatchException  ");
                    expresult = returnCode1;
                } catch ( AbsentInformationException e7 ) {
                    log3("ERROR:  AbsentInformationException  ");
                    expresult = returnCode1;
                } catch ( Exception e6 ) {
                    log3("ERROR:  UNSPECIFIED EXCEPTION:  " + e6);
                    expresult = returnCode1;
                }

            }

            log2("      resuming the thread2 for case it was suspended but not resumed yet");
            eventSet.resume();

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
                        returnCode = returnCode3;
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
