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

package nsk.jdi.StackFrame.getValue;

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
 * <code>com.sun.jdi.StackFrame.getValue()</code>               <BR>
 * complies with its spec in case when a tested program         <BR>
 * is prepared with full information (see README file),         <BR>
 * hence, AbsentInformationException is not expected to happen. <BR>
 * <BR>
 * The cases for testing are as follows.                                <BR>
 * After being started up,                                              <BR>
 * a debuggee creates a 'lockingObject' for synchronizing threads,      <BR>
 * enters a synchronized block in which it creates new thread, thread2, <BR>
 * informs a debugger of the thread2 creation, and is waiting for reply.<BR>
 * Since the thread2 uses the same locking object as main one           <BR>
 * it is locked up until the main thread leaves the synchronized block. <BR>
 * Upon the receiption a message from the debuggee, the debugger        <BR>
 * performs the following.                                              <BR>
 * 1) After getting a thread2 suspended but before to resume it,        <BR>
 *    StackFrame.getValue() is used for getting and                     <BR>
 *    checking up the values of visible variables,                      <BR>
 *    local in a tested method, two for each PrimitiveType;             <BR>
 * 2) After resuming the thread2, the method StackFrame.getValue() is   <BR>
 *    invoked second time and InvalidStackFrameException must be thrown.<BR>
 * <BR>
 */

public class getvalue001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/StackFrame/getValue/getvalue001  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new getvalue001().runThis(argv, out);
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
        "nsk.jdi.StackFrame.getValue.getvalue001a";

    private String testedClassName =
        "nsk.jdi.StackFrame.getValue.Threadgetvalue001a";

    //String mName = "nsk.jdi.StackFrame.getValue";

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
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");  // *** tp
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);            // *** tp
        }

        waitTime = argsHandler.getWaitTime();


        IOPipe pipe     = new IOPipe(debuggee);

        debuggee.redirectStderr(out);
        log2("getvalue001a debuggee launched");
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
            //String breakpointMethod2 = "runt2";

            String bpLine1 = "breakpointLineNumber1";
            //String bpLine2 = "breakpointLineNumber2";
            //String bpLine3 = "breakpointLineNumber3";


            List            allThreads   = null;
            ListIterator    listIterator = null;
            List            classes      = null;

            BreakpointRequest breakpRequest1 = null;
           // BreakpointRequest breakpRequest2 = null;
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

                log2("      the thread2 is at the breakpoint");
                log2("      the check that the thread2 is suspended at the breakpoint");
                if ( thread2.isSuspended() ) {
                    log2("     :   thread2.isSuspended()");
                } else {
                    log3("ERROR:  !thread2.isSuspended()");
                    expresult = returnCode1;
                    break label1;
                }

                log2("      getting thread2's StackFrame");
                try {
                    stackFrame = thread2.frame(0);
                } catch ( Exception e ) {
                    log3("ERROR: Exception for stackFrame = thread2.frame(0)    :" + e);
                    expresult = returnCode1;
                    break label1;
                }

                //   StackFrame stackFrame is ready for testing

                String bl1 = "bl1", bl2 = "bl2";
                String bt1 = "bt1", bt2 = "bt2";
                String ch1 = "ch1", ch2 = "ch2";
                String db1 = "db1", db2 = "db2";
                String fl1 = "fl1", fl2 = "fl2";
                String in1 = "in1", in2 = "in2";
                String ln1 = "ln1", ln2 = "ln2";
                String sh1 = "sh1", sh2 = "sh2";

                String ini0 = "i0";


                for ( int i3 = 0; i3 < 8; i3++) {

                    try {

                        switch (i3) {

                        case 0: BooleanValue blv1 = null;
                                BooleanValue blv2 = null;

                                locvar1 = stackFrame.visibleVariableByName(bl1);
                                locvar2 = stackFrame.visibleVariableByName(bl2);
                                if (locvar1 == null || locvar2 == null) {
                                    log3("ERROR: 'locvar1 == null || locvar2 == null'  for Boolean");
                                    expresult = returnCode1;
                                    break;
                                }

                                blv1 = (BooleanValue) stackFrame.getValue(locvar1);
                                if (blv1.value() != true) {
                                    log3("ERROR: blv1 != true :  " + blv1.value() );
                                    expresult = returnCode1;
                                }
                                blv2 = (BooleanValue) stackFrame.getValue(locvar2);
                                if (blv2.value() != false) {
                                    log3("ERROR: blv2 != false :  " + blv2.value() );
                                }

                                break;


                        case 1: ByteValue btv1 = null;
                                ByteValue btv2 = null;

                                locvar1 = stackFrame.visibleVariableByName(bt1);
                                locvar2 = stackFrame.visibleVariableByName(bt2);
                                if (locvar1 == null || locvar2 == null) {
                                    log3("ERROR: 'locvar1 == null || locvar2 == null'  for Byte");
                                    expresult = returnCode1;
                                    break;
                                }

                                btv1 = (ByteValue) stackFrame.getValue(locvar1);
                                if (btv1.value() != 0) {
                                    log3("ERROR: btv1 != 0 :  " + btv1.value() );
                                    expresult = returnCode1;
                                }
                                btv2 = (ByteValue) stackFrame.getValue(locvar2);
                                if (btv2.value() != 1) {
                                    log3("ERROR: btv2 != 1 :  " + btv2.value() );
                                    expresult = returnCode1;
                                }

                                break;


                        case 2: CharValue chv1 = null;
                                CharValue chv2 = null;

                                locvar1 = stackFrame.visibleVariableByName(ch1);
                                locvar2 = stackFrame.visibleVariableByName(ch2);
                                if (locvar1 == null || locvar2 == null) {
                                    log3("ERROR: 'locvar1 == null || locvar2 == null'  for Char");
                                    expresult = returnCode1;
                                    break;
                                }

                                chv1 = (CharValue) stackFrame.getValue(locvar1);
                                if (chv1.value() != 0) {
                                    log3("ERROR: chv1 != 0 :  " + chv1.value() );
                                    expresult = returnCode1;
                                }
                                chv2 = (CharValue) stackFrame.getValue(locvar2);
                                if (chv2.value() != 1) {
                                    log3("ERROR: chv2 != 1 :  " + chv2.value() );
                                    expresult = returnCode1;
                                }

                                break;


                        case 3: DoubleValue dbv1 = null;
                                DoubleValue dbv2 = null;

                                locvar1 = stackFrame.visibleVariableByName(db1);
                                locvar2 = stackFrame.visibleVariableByName(db2);
                                if (locvar1 == null || locvar2 == null) {
                                    log3("ERROR: 'locvar1 == null || locvar2 == null'  for Double");
                                    expresult = returnCode1;
                                    break;
                                }

                                dbv1 = (DoubleValue) stackFrame.getValue(locvar1);
                                if (dbv1.value() != 0.0d) {
                                    log3("ERROR: dbv1 != 0.0d :  " + dbv1.value() );
                                    expresult = returnCode1;
                                }
                                dbv2 = (DoubleValue) stackFrame.getValue(locvar2);
                                if (dbv2.value() != 1111111111.0d) {
                                    log3("ERROR: dbv2 != 1111111111.0d :  " + dbv2.value() );
                                    expresult = returnCode1;
                                }

                                break;


                        case 4: FloatValue flv1 = null;
                                FloatValue flv2 = null;

                                locvar1 = stackFrame.visibleVariableByName(fl1);
                                locvar2 = stackFrame.visibleVariableByName(fl2);
                                if (locvar1 == null || locvar2 == null) {
                                    log3("ERROR: 'locvar1 == null || locvar2 == null'  for Float");
                                    expresult = returnCode1;
                                    break;
                                }

                                flv1 = (FloatValue) stackFrame.getValue(locvar1);
                                if (flv1.value() != 0.0f) {
                                    log3("ERROR: flv1 != 0.0f :  " + flv1.value() );
                                    expresult = returnCode1;
                                }
                                flv2 = (FloatValue) stackFrame.getValue(locvar2);
                                if (flv2.value() != 1111111111.0f) {
                                    log3("ERROR: flv2 != 1111111111.0f :  " + flv2.value() );
                                    expresult = returnCode1;
                                }

                                break;

                        case 5: IntegerValue inv1 = null;
                                IntegerValue inv2 = null;

                                locvar1 = stackFrame.visibleVariableByName(in1);
                                locvar2 = stackFrame.visibleVariableByName(in2);
                                if (locvar1 == null || locvar2 == null) {
                                log3("ERROR: 'locvar1 == null || locvar2 == null'  for Integer");
                                    expresult = returnCode1;
                                    break;
                                }

                                inv1 = (IntegerValue) stackFrame.getValue(locvar1);
                                if (inv1.value() != 0) {
                                    log3("ERROR: inv1 != 0 :  " + inv1.value() );
                                    expresult = 1;
                                }
                                inv2 = (IntegerValue) stackFrame.getValue(locvar2);
                                if (inv2.value() != 1) {
                                    log3("ERROR: inv2 != 1 :  " + inv2.value() );
                                    expresult = returnCode1;
                                }

                                break;


                        case 6: LongValue lnv1 = null;
                                LongValue lnv2 = null;

                                locvar1 = stackFrame.visibleVariableByName(ln1);
                                locvar2 = stackFrame.visibleVariableByName(ln2);
                                if (locvar1 == null || locvar2 == null) {
                                    log3("ERROR: 'locvar1 == null || locvar2 == null'  for Long");
                                    expresult = returnCode1;
                                    break;
                                }

                                lnv1 = (LongValue) stackFrame.getValue(locvar1);
                                lnv2 = (LongValue) stackFrame.getValue(locvar2);
                                log2("2       :  lnv1= 0x" + Long.toHexString(lnv1.value())
                                    + "  lnv2= 0x" + Long.toHexString(lnv2.value()) );
                                if (lnv1.value() != 0) {
                                    log3("ERROR: lnv1 != 0 :  " + Long.toHexString(lnv1.value()) );
                                    expresult = returnCode1;
                                }
                                if (lnv2.value() != 0x1234567890abcdefL) {
                                    log3("ERROR: lnv2 != 0x1234567890abcdefL :  " + Long.toHexString(lnv2.value()) );
                                    expresult = returnCode1;
                                }

                                break;


                        case 7: ShortValue shv1 = null;
                                ShortValue shv2 = null;
                                locvar1 = stackFrame.visibleVariableByName(sh1);
                                locvar2 = stackFrame.visibleVariableByName(sh2);
                                if (locvar1 == null || locvar2 == null) {
                                    log3("ERROR: 'locvar1 == null || locvar2 == null'  for Short");
                                    expresult = returnCode1;
                                    break;
                                }

                                shv1 = (ShortValue) stackFrame.getValue(locvar1);
                                if (shv1.value() != 0) {
                                    log3("ERROR: shv1 != 0 :  " + shv1.value() );
                                    expresult = returnCode1;
                                }
                                shv2 = (ShortValue) stackFrame.getValue(locvar2);
                                if (shv2.value() != 1) {
                                    log3("ERROR: shv2 != 1 :  " + shv2.value() );
                                    expresult = returnCode1;
                                }

                                break;


                      default : log3("ERROR: TEST ERROR:  case: default:");
                                expresult = returnCode1;
                                break;

                        }  // end of switch

                    } catch ( AbsentInformationException e ) {
                        log3("ERROR:  AbsentInformationException for: stackFrame.getValue()");
                        expresult = returnCode1;
                    } catch ( InvalidStackFrameException e ) {
                        log3("ERROR:  InvalidStackFrameException before the thread is resumed   ");
                        expresult = 1;
                    } catch ( Exception e ) {
                        log3("ERROR:  unexpected exception:  " + e);
                        expresult = returnCode1;
                    } // end of try

                } // end of for
            }

            label2: {

                log2("       resuming the thread2");
                eventSet.resume();

                if (expresult != returnCode0)
                     break label2;

                try {
                    Value value = stackFrame.getValue(locvar1);

                    log3("ERROR:  no InvalidStackFrameExceprtion after the thread is resumed");
                    expresult = returnCode1;
                } catch ( InvalidStackFrameException e ) {
                    log2("     :  InvalidStackFrameException after the thread is resumed");
                }
            }
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
