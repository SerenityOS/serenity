/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Value._itself_;

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
 * Value.                                                       <BR>
 *                                                              <BR>
 * The test checks up that a Value object, mirroring neither    <BR>
 * a primitive type value nor a void, is casted to both         <BR>
 * ObjectReference and a corresponding object reference;        <BR>
 * the tested are ArrayReference, ClassObjectReference,         <BR>
 * StringReference, ThreadReference, and ThreadGroupReference.  <BR>
 * <BR>
 * The test contains two test cases with different ways to get  <BR>
 * a tested Value object:                                       <BR>
 *   Value = ReferenceType.getValue(Field);                     <BR>
 *     tested types are all of five mentioned above             <BR>
 *   Value = StackFrame.getValue(LocalVariable);                <BR>
 *     tested types are String, Array, and ClassObject only     <BR>
 */

public class value001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Value/_itself_/value001  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new value001().runThis(argv, out);
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
        "nsk.jdi.Value._itself_.value001a";

    private String testedClassName =
        "nsk.jdi.Value._itself_.Threadvalue001a";

    //String mName = "nsk.jdi.Value._itself_";

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
        log2(debuggeeName + " debuggee launched");
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
            } else if (!line.equals("checkready")) {
                log3("ERROR: returned string is not 'checkready'");
                testExitCode = FAILED;
                break ;
            }

            log1("new checkready: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            log2("===> test case : Value = ReferenceType.getValue(Field);");

            String stringObjName      = "str";
            String arrayObjName       = "bArray";
            String threadObjName      = "thread2";
            String threadgroupObjName = "threadGroupObj";
            String classObj1Name       = "classObj1";
            String classObj2Name       = "classObj2";

            ObjectReference stringObjRef      = null;
            ObjectReference arrayObjRef       = null;
            ObjectReference threadObjRef      = null;
            ObjectReference threadgroupObjRef = null;
            ObjectReference classObj1Ref      = null;
            ObjectReference classObj2Ref      = null;

            ReferenceType   debuggeeClass = null;

            Value val = null;


            List classes = null;

            classes     = vm.classesByName(debuggeeName);
            debuggeeClass = (ReferenceType) classes.get(0);


            log2("......check for a mirror of String object");
            val = debuggeeClass.getValue(debuggeeClass.fieldByName(stringObjName));
            try {
                log2("       cast from Value to String");
                StringReference sRef = (StringReference) val;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }
            try {
                log2("       cast from Value to ObjectReference");
                stringObjRef = (ObjectReference) val;
                log2("       cast from ObjectReference to String");
                StringReference sRef = (StringReference) stringObjRef;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }


            log2("......check for a mirror of Array object");
            val = debuggeeClass.getValue(debuggeeClass.fieldByName(arrayObjName));
            try {
                log2("       cast from Value to ArrayReference");
                ArrayReference aRef = (ArrayReference) val;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }
            try {
                log2("       cast from Value to ObjectReference");
                arrayObjRef = (ObjectReference) val;
                log2("       cast from ObjectReference to ArrayReference");
                ArrayReference aRef = (ArrayReference) arrayObjRef;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }


            log2("......check for a mirror of Thread object");
            val = debuggeeClass.getValue(debuggeeClass.fieldByName(threadObjName));
            try {
                log2("       cast from Value to ThreadReference");
                ThreadReference tRef = (ThreadReference) val;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }
            try {
                log2("       cast from Value to ObjectReference");
                threadObjRef = (ObjectReference) val;
                log2("       cast from ObjectReference to ThreadReference");
                ThreadReference tRef = (ThreadReference) threadObjRef;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }


            log2("......check for a mirror of ThreadGroup object");
            val = debuggeeClass.getValue(debuggeeClass.fieldByName(threadgroupObjName));
            try {
                log2("       cast from Value to ThreadGroupReference");
                ThreadGroupReference gRef = (ThreadGroupReference) val;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }
            try {
                log2("       cast from Value to ObjectReference");
                threadgroupObjRef = (ObjectReference) val;
                log2("       cast from ObjectReference to ThreadGroupReference");
                ThreadGroupReference gRef = (ThreadGroupReference) threadgroupObjRef;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }


            log2("......check for a mirror of Class object");
            val = debuggeeClass.getValue(debuggeeClass.fieldByName(classObj1Name));
            try {
                log2("       cast from Value to ClassObjectReference");
                ClassObjectReference c1Ref = (ClassObjectReference) val;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }
            try {
                log2("       cast from Value to ObjectReference");
                classObj1Ref = (ObjectReference) val;
                log2("       cast from ObjectReference to ClassObjectReference");
                ClassObjectReference c1Ref = (ClassObjectReference) classObj1Ref;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }


            log2("......check for a mirror of Class object");
            val = debuggeeClass.getValue(debuggeeClass.fieldByName(classObj2Name));
            try {
                log2("       cast from Value to ClassObjectReference");
                ClassObjectReference c2Ref = (ClassObjectReference) val;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }
            try {
                log2("       cast from Value to ObjectReference");
                classObj2Ref = (ObjectReference) val;
                log2("       cast from ObjectReference to ClassObjectReference");
                ClassObjectReference c2Ref = (ClassObjectReference) classObj2Ref;
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
            }


            log2("===> test case : Value = StackFrame.getValue(LocalVariable);");

            eventRManager = vm.eventRequestManager();
            eventQueue    = vm.eventQueue();

            String threadName = "Thread2";

            String            breakpointMethod1 = "run";
            String            bpLine1           = "breakpointLineNumber1";
            BreakpointRequest breakpRequest1    = null;

            StackFrame      stackFrame   = null;

            List            allThreads   = null;
            ListIterator    listIterator = null;


            int expresult = returnCode0;

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

                log2("setting up a breakpoint");

                breakpRequest1 = settingBreakpoint(breakpointMethod1, bpLine1, "one");
                if (breakpRequest1 == null) {
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

                log2("      getting BreakpointEvent");
                expresult = breakpoint();
                if (expresult != returnCode0)
                    break label1;


                log2("      getting thread2's StackFrame");
                try {
                    stackFrame = thread2.frame(0);
                } catch ( Exception e ) {
                    log3("ERROR: Exception for stackFrame = thread2.frame(0)    :" + e);
                    testExitCode = FAILED;
                    break label1;
                }

                String stringObj2Name      = "str2";
                String arrayObj2Name       = "bArray2";
                String classObj3Name       = "classObj3";

                log2("......check for a mirror of String object");
                try {
                    val = stackFrame.getValue(stackFrame.visibleVariableByName(stringObj2Name));

                    try {
                        log2("       cast from Value to String");
                        StringReference sRef = (StringReference) val;
                    } catch ( ClassCastException e ) {
                        log3("ERROR: ClassCastException");
                        testExitCode = FAILED;
                    }
                    try {
                        log2("       cast from Value to ObjectReference");
                        stringObjRef = (ObjectReference) val;
                        log2("       cast from ObjectReference to String");
                        StringReference sRef = (StringReference) stringObjRef;
                    } catch ( ClassCastException e ) {
                        log3("ERROR: ClassCastException");
                        testExitCode = FAILED;
                    }
                } catch ( AbsentInformationException e ) {
                    log3("ERROR:  AbsentInformationException for: stackFrame.getValue()");
                        expresult = returnCode1;
                }

                log2("......check for a mirror of Array object");
                try {
                    val = stackFrame.getValue(stackFrame.visibleVariableByName(arrayObj2Name));
                    try {
                        log2("       cast from Value to ArrayReference");
                        ArrayReference aRef = (ArrayReference) val;
                    } catch ( ClassCastException e ) {
                        log3("ERROR: ClassCastException");
                        testExitCode = FAILED;
                    }
                    try {
                        log2("       cast from Value to ObjectReference");
                        arrayObjRef = (ObjectReference) val;
                        log2("       cast from ObjectReference to ArrayReference");
                        ArrayReference aRef = (ArrayReference) arrayObjRef;
                    } catch ( ClassCastException e ) {
                        log3("ERROR: ClassCastException");
                        testExitCode = FAILED;
                    }
                } catch ( AbsentInformationException e ) {
                    log3("ERROR:  AbsentInformationException for: stackFrame.getValue()");
                    expresult = returnCode1;
                }

                log2("......check for a mirror of Class object");
                try {
                    val = stackFrame.getValue(stackFrame.visibleVariableByName(classObj3Name));
                    try {
                        log2("       cast from Value to ClassObjectReference");
                        ClassObjectReference c1Ref = (ClassObjectReference) val;
                    } catch ( ClassCastException e ) {
                        log3("ERROR: ClassCastException");
                        testExitCode = FAILED;
                    }
                    try {
                        log2("       cast from Value to ObjectReference");
                        classObj1Ref = (ObjectReference) val;
                        log2("       cast from ObjectReference to ClassObjectReference");
                        ClassObjectReference c1Ref = (ClassObjectReference) classObj1Ref;
                    } catch ( ClassCastException e ) {
                        log3("ERROR: ClassCastException");
                        testExitCode = FAILED;
                    }

                } catch ( AbsentInformationException e ) {
                    log3("ERROR:  AbsentInformationException for: stackFrame.getValue()");
                    expresult = returnCode1;
                }
            }

            thread2.resume();

            if (expresult != returnCode0)
                testExitCode = FAILED;

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
