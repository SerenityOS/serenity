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

package nsk.jdi.ObjectReference.getValue;

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
 * ObjectReference.                                             <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ObjectReference.getValue()</code>          <BR>
 * complies with its specification.                             <BR>
 * <BR>
 * The cases for testing include                                <BR>
 * static and instance fields of primitive types created in     <BR>
 * debuggee's second thread with the method ClassType.newInstance().<BR>
 * <BR>
 * The test works as follows.                                   <BR>
 * Upon launch, a debuggee creates second thread, thread2 and   <BR>
 * informs a debugger of its creation.                          <BR>
 * The debugger :                                               <BR>
 *   creates in the thread2                                     <BR>
 *      new object with static and instace fields,              <BR>
 *   gets mirrors of fields to test, and                        <BR>
 *   performs casts from Value to corresponding PrimitiveValue  <BR>
 *      for each of the tested types.                           <BR>
 */

public class getvalue002 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ObjectReference/getValue/getvalue002  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new getvalue002().runThis(argv, out);
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
        "nsk.jdi.ObjectReference.getValue.getvalue002a";

    private String testedClassName =
        "nsk.jdi.ObjectReference.getValue.getvalue002aTestClass";

    private String threadName = "testedThread";

    private String threadClassName =
        "nsk.jdi.ObjectReference.getValue.getvalue002aThread";

    //String mName = "nsk.jdi.ObjectReference.getValue";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine      vm            = null;
    static EventRequestManager eventRManager = null;
    static EventQueue          eventQueue    = null;
    static EventSet            eventSet      = null;

    ReferenceType     testedClass  = null;
    ReferenceType     threadClass  = null;

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

            int expresult = returnCode0;


            eventRManager = vm.eventRequestManager();
            eventQueue    = vm.eventQueue();

            String breakpointMethod1 = "runt1";

            String bpLine1 = "breakpointLineNumber1";

            List            allThreads   = null;
            ListIterator    listIterator = null;
            List            classes      = null;

            BreakpointRequest breakpRequest1 = null;

            ClassType classType    = null;

            label0: {

                log2("getting ThreadReference object");
                try {
                    allThreads  = vm.allThreads();
                    classes     = vm.classesByName(threadClassName);
                    threadClass = (ReferenceType) classes.get(0);
                } catch ( Exception e) {
                    log3("ERROR: Exception at very beginning !? : " + e);
                    expresult = returnCode1;
                    break label0;
                }


                log2("......getting Thread thread2 - 1-st argument");
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
                log2("      thread2 is at breakpoint");


                log2("......getting ClassType classType - an object to apply newInstance()");
                classes     = vm.classesByName(testedClassName);
                testedClass = (ReferenceType) classes.get(0);
                classType   = (ClassType) testedClass;

                List         invokeMethods = testedClass.methods();
                ListIterator            li = invokeMethods.listIterator();

                Method       invokeMethod = null;

                log2("......getting Method invokeMethod - a constructor to invoke; 2-nd argument");
                try {
                    for (;;) {
                        invokeMethod = (Method) li.next();
                        if (!invokeMethod.isConstructor())
                            continue;
                        if (invokeMethod.arguments().size() == 0)
                            break;
                    }
                } catch ( AbsentInformationException e1 ) {
                    log2("       AbsentInformationException ");
                    break label1;
                } catch ( NoSuchElementException e2 ) {
                    log3("ERROR: NoSuchElementException ");
                    expresult = returnCode2;
                    break label1;
                }


                // this is only to get an empty List with JDI methods but not with API List methods
                log2("......forming an empty List argumentList for 3-d argument");
                log2("       List argumentList = testedClass.methodsByName('m');");
                List argumentList = testedClass.methodsByName("m");
                log2("       argumentList.clear();");
                argumentList.clear();


                ObjectReference objRef = null;

                log2("......ObjectReference objRef = classType.newInstance(thread2, invokeMethod, argumentList, 0);");
                log2("      No Exception is expected");
                try {
                    objRef = classType.newInstance(thread2,
                                     invokeMethod, Collections.<Value>emptyList(), 0);
                    if (objRef == null)
                        log2("       objRe == null");


                    log2("        ReferenceType refType = objRef.referenceType();");
                    ReferenceType refType = objRef.referenceType();

                    log2("        ClassType cType = (ClassType) refType;");
                    ClassType cType = (ClassType) refType;

                    log2("......checking up on: cType.equals(classType); ");
                    if (!cType.equals(classType)) {
                        log3("ERROR: !cType.equals(classType)");
                        expresult = returnCode1;
                    }

                } catch ( Exception e ) {
                             log3("ERROR: Exception for classType.newInstance() :" + e);
                             expresult = returnCode1;
                }

                ObjectReference thisobj = objRef;

                String bl1 = "bl1", bl2 = "bl2";
                String bt1 = "bt1", bt2 = "bt2";
                String ch1 = "ch1", ch2 = "ch2";
                String db1 = "db1", db2 = "db2";
                String fl1 = "fl1", fl2 = "fl2";
                String in1 = "in1", in2 = "in2";
                String ln1 = "ln1", ln2 = "ln2";
                String sh1 = "sh1", sh2 = "sh2";

                Field fsbl1 = testedClass.fieldByName("bl1");
                Field fsbt1 = testedClass.fieldByName("bt1");
                Field fsch1 = testedClass.fieldByName("ch1");
                Field fsdb1 = testedClass.fieldByName("db1");
                Field fsfl1 = testedClass.fieldByName("fl1");
                Field fsin1 = testedClass.fieldByName("in1");
                Field fsln1 = testedClass.fieldByName("ln1");
                Field fssh1 = testedClass.fieldByName("sh1");

                Field fsbl2 = testedClass.fieldByName("bl2");
                Field fsbt2 = testedClass.fieldByName("bt2");
                Field fsch2 = testedClass.fieldByName("ch2");
                Field fsdb2 = testedClass.fieldByName("db2");
                Field fsfl2 = testedClass.fieldByName("fl2");
                Field fsin2 = testedClass.fieldByName("in2");
                Field fsln2 = testedClass.fieldByName("ln2");
                Field fssh2 = testedClass.fieldByName("sh2");


                log2("......loop of checks on each primitive type by performing statements like first one:");
                log2("        BooleanValue blv = (BooleanValue) thisobj.getValue(fsbl1);");
                for ( int i3 = 0; i3 < 8; i3++) {

                    try {

                        switch (i3) {

                        case 0:
                                log2("      checking up on boolean");
                                BooleanValue blv = (BooleanValue) thisobj.getValue(fsbl1);

                                blv = (BooleanValue) thisobj.getValue(fsbl2);
                                break;

                        case 1:
                                log2("      checking up on byte");
                                ByteValue btv = (ByteValue) thisobj.getValue(fsbt1);

                                btv = (ByteValue) thisobj.getValue(fsbt2);
                                break;

                        case 2:
                                log2("      checking up on char");
                                CharValue chv = (CharValue) thisobj.getValue(fsch1);

                                chv = (CharValue) thisobj.getValue(fsch2);
                                break;

                        case 3:
                                log2("      checking up on double");
                                DoubleValue dbv = (DoubleValue) thisobj.getValue(fsdb1);

                                dbv = (DoubleValue) thisobj.getValue(fsdb2);
                                break;

                        case 4:
                                log2("      checking up on float");
                                FloatValue flv = (FloatValue) thisobj.getValue(fsfl1);

                                flv = (FloatValue) thisobj.getValue(fsfl2);
                                break;

                        case 5:
                                log2("      checking up on int");
                                IntegerValue inv = (IntegerValue) thisobj.getValue(fsin1);

                                inv = (IntegerValue) thisobj.getValue(fsin2);
                                break;

                        case 6:
                                log2("      checking up on long");
                                LongValue lnv = (LongValue) thisobj.getValue(fsln1);

                                lnv = (LongValue) thisobj.getValue(fsln2);
                                break;

                        case 7:
                                log2("      checking up on short");
                                ShortValue shv = (ShortValue) thisobj.getValue(fssh1);

                                shv = (ShortValue) thisobj.getValue(fssh2);
                                break;


                      default : log3("ERROR: TEST ERROR:  case: default:");
                                expresult = returnCode1;
                                break;

                        }  // end of switch

                    } catch ( IllegalArgumentException  e ) {
                        log3("ERROR:  IllegalArgumentException");
                        expresult = returnCode1;
                    } catch ( ObjectCollectedException e ) {
                        log3("ERROR:  ObjectCollectedException");
                        expresult = returnCode1;
                    } catch ( VMMismatchException  e ) {
                        log3("ERROR:  VMMismatchException");
                        expresult = returnCode1;
                    } catch ( Exception e ) {
                        log3("ERROR:  unexpected exception:  " + e);
                        expresult = returnCode1;
                    } // end of try

                } // end of for


            }
            vm.resume();

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
//            Method  method  = (Method) testedclass.methodsByName(methodName).get(0);
            Method  method  = (Method) threadClass.methodsByName(methodName).get(0);

            alllineLocations = method.allLineLocations();

            int n =
//                ( (IntegerValue) testedclass.getValue(testedclass.fieldByName(bpLine) ) ).value();
                ( (IntegerValue) threadClass.getValue(threadClass.fieldByName(bpLine) ) ).value();
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
