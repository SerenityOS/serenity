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

package nsk.jdi.VirtualMachine.dispose;

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
 * VirtualMachine.                                              <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.VirtualMachine.dispose()</code>            <BR>
 * complies with its specification.                             <BR>
 * The test checks up that after call to VirtualMachine.dispose(),<BR>
 * a method invocations executing in the target VM is continued,<BR>
 * and upon completion of the method invocation,                <BR>
 * the invoking thread continues from the location where        <BR>
 * it was originally stopped.                                   <BR>
 * <BR>
 * The test work as follows.                                    <BR>
 * Upon its launch the debuggee :                               <BR>
 *   creates new thread, thread2 which is waiting until         <BR>
 *   the main thread leaves a synchronized block, and           <BR>
 *   informs the debugger of the thread creation.               <BR>
 * The thread2 contains a method to be invoked from the debugger<BR>
 * The debugger :                                               <BR>
 *   sets up a breakpoint for debuggee's thread2,               <BR>
 *   instructs the debuggee to leave the synchronized block in  <BR>
 *      order to get its thread2 suspended at the breakpoint,   <BR>
 *   waits for a reply from the debuggee.                       <BR>
 * The debuggee :                                               <BR>
 *   leaves the synchronized block and                          <BR>
 *   enter another synchronized block to lock itself at a monitor<BR>
 *      which the method "runt2" to be invoked from             <BR>
 *      the debugger will unlock,                               <BR>
 *   informs the debugger and waits for new instruction.        <BR>
 * The debugger :                                               <BR>
 *   upon getting the thread2 at the breakpoint,                <BR>
 *      creates its own Thread object, named thread2 too,       <BR>
 *   gets its thread2 running;                                  <BR
 *      the thread2 invokes the method "runt2" within           <BR>
 *      debuggee's thread2 suspended at the breakpoint;         <BR>
 *      the method is at once locked at the monitor holding by  <BR>
 *      debuggee's main thread;                                 <BR>
 *   instructs the debuggee to check whether "runt2" is invoked.<BR>
 * The debuggee :                                               <BR>
 *   upon getting unlocked by "runt2",                          <BR>
 *      informs the debugger and waits for new instruction.     <BR>
 * The debugger :
 *   clears interruption status,                                <BR>
 *   invokes vm.dispose() that results in                       <BR>
 *      VMDisconnectedException in the thread2                  <BR>
 *      which has been suspended after invoking "runt2"         <BR>
 *      but after exception it is resumed and sends interruption<BR>
 *      to the main thread;                                     <BR>
 *   if the interruption is not received yet,                   <BR>
 *      sleeps for a predefined waitTime and within this time   <BR>
 *      catchs InterruptedException;                            <BR>
 * Then the debugger                                            <BR>
 * asks the debuggee to check up on its thread2 state           <BR>
 * which should be "not alive" if the thread2 was resumed.      <BR>
 */

public class dispose005 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    public static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/VirtualMachine/dispose/dispose005  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new dispose005().runThis(argv, out);
    }
    //--------------------------------------------------   log procedures

    private static Log  logHandler;

    private static void log1(String message) {
        logHandler.display(sHeader1 + message);
    }
    public static void log2(String message) {
        logHandler.display(sHeader2 + message);
    }
    private static void log3(String message) {
        logHandler.complain(sHeader3 + message);
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.VirtualMachine.dispose.dispose005a";

    private String testedClassName =
        "nsk.jdi.VirtualMachine.dispose.Threaddispose005a";

    //String mName = "nsk.jdi.VirtualMachine.dispose";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine      vm            = null;
    static EventRequestManager eventRManager = null;
    static EventQueue          eventQueue    = null;
    static EventSet            eventSet      = null;

    static ReferenceType     testedclass  = null;
    static ClassType         classType    = null;

    static ThreadReference   thread2      = null;
    static ThreadReference   mainThread   = null;

    static ObjectReference threadObjRef = null;


    public static int   testExitCode = PASSED;

    static final int returnCode0 = 0;
    public static final int returnCode1 = 1;
    static final int returnCode2 = 2;
    static final int returnCode3 = 3;
    static final int returnCode4 = 4;


    static Thread currentThread = Thread.currentThread();

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

        int expresult = returnCode0;

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
            //BreakpointRequest breakpRequest2 = null;
            //BreakpointRequest breakpRequest3 = null;

            StackFrame    stackFrame = null;



            label0: {

                log2("getting ThreadReference object");
                try {
                    allThreads  = vm.allThreads();
                    classes     = vm.classesByName(testedClassName);
                    testedclass = (ReferenceType) classes.get(0);
                    classType   = (ClassType) testedclass;
                } catch ( Exception e) {
                    log3("ERROR: Exception at very beginning !? : " + e);
                    expresult = returnCode1;
                    break label0;
                }

                mainThread = debuggee.threadByName("main");
                thread2 = debuggee.threadByName(threadName);
                threadObjRef = thread2;

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

                log2("      enabling breakpRequest1");
                breakpRequest1.enable();


                log2("......forcing the main thread to leave synchronized block");
                pipe.println("continue");
                line = pipe.readln();
                if (!line.equals("docontinue")) {
                    log3("ERROR: returned string is not 'docontinue'");
                    expresult = returnCode4;
                }

                if (expresult != returnCode0)
                    break label1;


                log2("      getting BreakpointEvent");
                expresult = breakpoint();
                if (expresult != returnCode0)
                    break label1;
                log2("      testedThread is at breakpoint");


                Threaddispose005 thread2 =  new Threaddispose005("Thread2");
                log2("......thread2 is created");

                synchronized (Threaddispose005.lockingObject) {
                    synchronized (Threaddispose005.waitnotifyObj) {
                        log2("       synchronized (waitnotifyObj) { enter");
                        log2("       before: thread2.start()");
                        thread2.start();

                        try {
                            log2("       before:   waitnotifyObj.wait();");
                            Threaddispose005.waitnotifyObj.wait();
                            log2("       after:    waitnotifyObj.wait();");

                        } catch ( Exception e2) {
                            log3("ERROR: Exception : " + e2 );
//?                            pipe.println("waitnotifyerr");
                        }
                    }
                }
                log2("mainThread is out of: synchronized (lockingObject)");



                log2("......line = pipe.readln(); 'method_invoked' is expected");
                line = pipe.readln();
                if (!line.equals("method_invoked")) {
                    log3("ERROR: unexpected reply: " + line);
                    expresult = returnCode4;
                    break label1;
                }

                if (Thread.interrupted())
                   log2("       ==>Thread.interrupted()");

                log2("      vm.dispose()");
                vm.dispose();


                if (!Thread.interrupted()) {
                    log2("      Thread.sleep(waitTime*60000);");
                    try {
                        Thread.sleep(waitTime*60000);
                    } catch ( InterruptedException e ) {
                        log2("      : InterruptedException");
                    }
                }

                log2("......sending to the debuggee: 'check_alive'");
                log2("       expected reply: 'not_alive'");
                pipe.println("check_alive");
                line = pipe.readln();
                if (line.equals("alive")) {
                    log3("ERROR: testedThread is alive");
                    expresult = returnCode1;
                } else if (line.equals("not_alive")) {
                    log2("     testedThread is not alive");
                } else {
                    log3("ERROR: unexpected reply: " + line);
                    expresult = returnCode4;
                }
            }

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

        if (expresult != returnCode0)
        {
            log3("expresult != 0 (= " + expresult + ")");

            testExitCode = FAILED;
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
                          case 14: VMDisconnectEvent wmde = (VMDisconnectEvent) ev;
                                   log2("      VMDisconnectEvent removed");
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

class Threaddispose005 extends Thread {

    public Threaddispose005 (String threadName) {
        super(threadName);
    }

    public static Object waitnotifyObj  = new Object();
    public static Object lockingObject  = new Object();


    public void run() {
        log("method 'run' enter");

        synchronized (waitnotifyObj) {
            log("entered into block:  synchronized (waitnotifyObj)");
            waitnotifyObj.notify();
        }
        log("exited from block:  synchronized (waitnotifyObj)");
        synchronized (lockingObject) {
            log("entered into block:  synchronized (lockingObject)");
        }
        log("exited from block:  synchronized (lockingObject)");


        List<Method> invokeMethods  = dispose005.testedclass.methodsByName("runt2");
        Method invokeMethod = invokeMethods.get(0);

        List<Value> argumentList = Collections.<Value>emptyList();

        try {
            log("......invoking a method in the debuggee; VMDisconnectedException is expected");
            dispose005.threadObjRef.invokeMethod(dispose005.thread2,
                          invokeMethod, argumentList, 0);
        } catch ( VMDisconnectedException e ) {
            log("      : VMDisconnectedException ");
        } catch ( Exception t ) {
            log("ERROR: Exception:" + t);
            dispose005.testExitCode = dispose005.FAILED;
        }
        log("dispose005.currentThread.interrupt();");
        dispose005.currentThread.interrupt();


        log("method 'run' exit");
        return;
    }

    void log(String str) {
        dispose005.log2("thread2: " + str);
    }

}
