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
 * @bug 4409241 4432820
 * @summary Test the bug fix for: MethodExitEvents disappear when Object-Methods are called from main
 * @author Tim Bell
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g MethodEntryExitEvents.java
 * @run driver MethodEntryExitEvents SUSPEND_EVENT_THREAD MethodEntryExitEventsDebugee
 * @run driver MethodEntryExitEvents SUSPEND_NONE MethodEntryExitEventsDebugee
 * @run driver MethodEntryExitEvents SUSPEND_ALL MethodEntryExitEventsDebugee
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;

class t2 {
    public static void sayHello1(int i, int j) {
        sayHello2(i, j);
    }
    public static void sayHello2(int i, int j) {
        sayHello3(i, j);
    }
    public static void sayHello3(int i, int j) {
        sayHello4(i, j);
    }
    public static void sayHello4(int i, int j) {
        sayHello5(i, j);
    }
    public static void sayHello5(int i, int j) {
        if (i < 2) {
            sayHello1(++i, j);
        } else {
            System.out.print  ("MethodEntryExitEventsDebugee: ");
            System.out.print  ("    -->> Hello.  j is: ");
            System.out.print  (j);
            System.out.println(" <<--");
        }
    }
}

class MethodEntryExitEventsDebugee {
    public static void loopComplete () {
        /*
         * The implementation here is deliberately inefficient
         * because the debugger is still watching this method.
         */
        StringBuffer sb = new StringBuffer();
        sb.append ("MethodEntryExitEventsDebugee: ");
        sb.append ("Executing loopComplete method for a graceful shutdown...");
        String s = sb.toString();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            System.out.print(c);
        }
        System.out.println();
    }
    public static void main(String[] args) {
        t2 test = new t2();
        for (int j = 0; j < 3; j++) {
            test.sayHello1(0, j);
        }
        loopComplete();
    }
}


public class MethodEntryExitEvents extends TestScaffold {
    int sessionSuspendPolicy = EventRequest.SUSPEND_ALL;
    StepRequest stepReq = null; //Only one step request allowed per thread
    boolean finishedCounting = false;

    /*
     * Enter main() , then t2.<init>, then sayHello[1,2,3,4,5] 15 times 3 loops,
     * then loopComplete()
     */
    final int expectedEntryCount = 1 + 1 + (15 * 3) + 1;
    int methodEntryCount = 0;

    /*
     * Exit t2.<init>, then sayHello[1,2,3,4,5] 15 times 3 loopa
     * (event monitoring is cancelled before we exit loopComplete() or main())
     */
    final int expectedExitCount = 1 + (15 * 3);
    int methodExitCount = 0;

    // Classes which we are interested in
    private List includes = Arrays.asList(new String[] {
        "MethodEntryExitEventsDebugee",
        "t2"
    });

    MethodEntryExitEvents (String args[]) {
        super(args);
    }

    private void usage(String[] args) throws Exception {
        StringBuffer sb = new StringBuffer("Usage: ");
        sb.append(System.getProperty("line.separator"));
        sb.append("  java ");
        sb.append(getClass().getName());
        sb.append(" [SUSPEND_NONE | SUSPEND_EVENT_THREAD | SUSPEND_ALL]");
        sb.append(" [MethodEntryExitEventsDebugee | -connect <connector options...>] ");
        throw new Exception (sb.toString());
    }

    public static void main(String[] args)      throws Exception {
        MethodEntryExitEvents meee = new MethodEntryExitEvents (args);
        meee.startTests();
    }

    public void exceptionThrown(ExceptionEvent event) {
        System.out.println("Exception: " + event.exception());
        System.out.println(" at catch location: " + event.catchLocation());

        // Step to the catch
        if (stepReq == null) {
            stepReq =
                eventRequestManager().createStepRequest(event.thread(),
                                                        StepRequest.STEP_MIN,
                                                        StepRequest.STEP_INTO);
            stepReq.addCountFilter(1);  // next step only
            stepReq.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        }
        stepReq.enable();
    }
    public void stepCompleted(StepEvent event) {
        System.out.println("stepCompleted: line#=" +
                           event.location().lineNumber() +
                           " event=" + event);
        // disable the step and then run to completion
        //eventRequestManager().deleteEventRequest(event.request());
        StepRequest str= (StepRequest)event.request();
        str.disable();
    }
    public void methodEntered(MethodEntryEvent event) {
        if (!includes.contains(event.method().declaringType().name())) {
            return;
        }

        if (! finishedCounting) {
            // We have to count the entry to loopComplete, but
            // not the exit
            methodEntryCount++;
            System.out.print  (" Method entry number: ");
            System.out.print  (methodEntryCount);
            System.out.print  ("  :  ");
            System.out.println(event);
            if ("loopComplete".equals(event.method().name())) {
                finishedCounting = true;
            }
        }
    }

    public void methodExited(MethodExitEvent event) {
        if (!includes.contains(event.method().declaringType().name())) {
            return;
        }

        if (! finishedCounting){
            methodExitCount++;
            System.out.print  (" Method exit  number: ");
            System.out.print  (methodExitCount);
            System.out.print  ("  :  ");
            System.out.println(event);
        }
    }

    protected void runTests() throws Exception {
        if (args.length < 1) {
            usage(args);
        }
        //Pick up the SUSPEND_xxx in first argument
        if ("SUSPEND_NONE".equals(args[0])) {
            sessionSuspendPolicy = EventRequest.SUSPEND_NONE;
        } else if ("SUSPEND_EVENT_THREAD".equals(args[0])) {
            sessionSuspendPolicy = EventRequest.SUSPEND_EVENT_THREAD;
        } else if ("SUSPEND_ALL".equals(args[0])) {
            sessionSuspendPolicy = EventRequest.SUSPEND_ALL;
        } else {
            usage(args);
        }
        System.out.print("Suspend policy is: ");
        System.out.println(args[0]);

        // Skip the test arg
        String[] args2 = new String[args.length - 1];
        System.arraycopy(args, 1, args2, 0, args.length - 1);

        if (args2.length < 1) {
            usage(args2);
        }
        List argList = new ArrayList(Arrays.asList(args2));
        System.out.println("run args: " + argList);
        connect((String[]) argList.toArray(args2));
        waitForVMStart();

        // Determine main thread
        ClassPrepareEvent e = resumeToPrepareOf("MethodEntryExitEventsDebugee");
        mainThread = e.thread();

        try {

            /*
             * Ask for Exception events
             */
            ExceptionRequest exceptionRequest =
                eventRequestManager().createExceptionRequest(null, // refType (null == all instances)
                                                             true, // notifyCaught
                                                             true);// notifyUncaught
            exceptionRequest.addThreadFilter(mainThread);
            exceptionRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            exceptionRequest.enable();

            /*
             * Ask for method entry events
             */
            MethodEntryRequest entryRequest =
               eventRequestManager().createMethodEntryRequest();
            entryRequest.addThreadFilter(mainThread);
            entryRequest.setSuspendPolicy(sessionSuspendPolicy);
            entryRequest.enable();

            /*
             * Ask for method exit events
             */
            MethodExitRequest exitRequest =
                eventRequestManager().createMethodExitRequest();
            exitRequest.addThreadFilter(mainThread);
            exitRequest.setSuspendPolicy(sessionSuspendPolicy);
            exitRequest.enable();

            /*
             * We are now set up to receive the notifications we want.
             * Here we go.  This adds 'this' as a listener so
             * that our handlers above will be called.
             */

            listenUntilVMDisconnect();
            System.out.println("All done...");

        } catch (Exception ex){
            ex.printStackTrace();
            testFailed = true;
        }

        if ((methodEntryCount != expectedEntryCount) ||
            (methodExitCount != expectedExitCount)) {
            testFailed = true;
        }
        if (!testFailed) {
            System.out.println();
            System.out.println("MethodEntryExitEvents: passed");
            System.out.print  ("    Method entry count: ");
            System.out.println(methodEntryCount);
            System.out.print  ("    Method exit  count: ");
            System.out.println(methodExitCount);
        } else {
            System.out.println();
            System.out.println("MethodEntryExitEvents: failed");
            System.out.print  ("    expected method entry count: ");
            System.out.println(expectedEntryCount);
            System.out.print  ("    observed method entry count: ");
            System.out.println(methodEntryCount);
            System.out.print  ("    expected method exit  count: ");
            System.out.println(expectedExitCount);
            System.out.print  ("    observed method exit  count: ");
            System.out.println(methodExitCount);
            throw new Exception("MethodEntryExitEvents: failed");
        }
    }
}
