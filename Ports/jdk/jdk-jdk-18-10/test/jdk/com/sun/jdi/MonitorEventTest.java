/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4401399
 * @summary Simple basic test of jdi Monitor request and event.
 * @author Swamy Venkataramanappa
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g MonitorEventTest.java
 * @run driver MonitorEventTest
 */
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.MonitorContendedEnterEvent;
import com.sun.jdi.event.MonitorContendedEnteredEvent;
import com.sun.jdi.event.MonitorWaitEvent;
import com.sun.jdi.event.MonitorWaitedEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.MonitorContendedEnterRequest;
import com.sun.jdi.request.MonitorContendedEnteredRequest;
import com.sun.jdi.request.MonitorWaitRequest;
import com.sun.jdi.request.MonitorWaitedRequest;

/********** target program **********/

class MonitorEventTestTarg {
    public static Object endingMonitor;
    public static Object startingMonitor;
    public static final long timeout = 30 * 6000; // milliseconds

    public static volatile boolean aboutEnterLock;

    static void foo() {
        System.out.println("Howdy!");
    }

    public static void main(String[] args){
        endingMonitor = new Object();
        startingMonitor = new Object();

        myThread t1 = new myThread();
        foo();
        aboutEnterLock = false;

        synchronized(endingMonitor) {

            // run thread
            try {
                // start thread
                synchronized (startingMonitor) {
                    t1.start();
                    startingMonitor.wait(timeout);
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception " + e);
            }

            Thread.yield();
            while(!(aboutEnterLock && t1.getState() == Thread.State.BLOCKED)) {
                try {
                    Thread.sleep(1000);
                }catch(Exception x){
                    System.out.println(x);
                }
            }
        }
        try {
            t1.join(timeout);
        } catch (Exception x){
            System.out.println("Exception while thread.join :" + x);
        }
        System.out.println("Test exiting");
    }
}

class myThread extends Thread {
    public void run() {
        synchronized(MonitorEventTestTarg.startingMonitor) {
            MonitorEventTestTarg.startingMonitor.notify();
        }

        // contended enter wait until main thread release monitor
        MonitorEventTestTarg.aboutEnterLock = true;
        synchronized (MonitorEventTestTarg.endingMonitor) {
        }
    }
}


    /********** test program **********/

public class MonitorEventTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    MonitorContendedEnterRequest contendedEnterRequest;
    MonitorWaitedRequest monitorWaitedRequest;
    MonitorContendedEnteredRequest contendedEnteredRequest;
    MonitorWaitRequest monitorWaitRequest;

    static int actualWaitCount = 0;
    static int actualWaitedCount = 0;
    static int actualContendedEnterCount = 0;
    static int actualContendedEnteredCount= 0;

    MonitorEventTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new MonitorEventTest(args).startTests();
    }

    /********** event handlers **********/

    public void monitorContendedEnter(MonitorContendedEnterEvent event) {

        actualContendedEnterCount++;
    }

    public void monitorContendedEntered(MonitorContendedEnteredEvent event) {

        actualContendedEnteredCount++;

    }

    public void monitorWait(MonitorWaitEvent event) {

        actualWaitCount++;

    }
    public void monitorWaited(MonitorWaitedEvent event) {

        actualWaitedCount++;

    }



    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("MonitorEventTestTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();

        if (vm().canRequestMonitorEvents()) {
            contendedEnterRequest = eventRequestManager().createMonitorContendedEnterRequest();
            contendedEnterRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            contendedEnterRequest.enable();
            contendedEnteredRequest = eventRequestManager().createMonitorContendedEnteredRequest();
            contendedEnteredRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            contendedEnteredRequest.enable();
            monitorWaitRequest = eventRequestManager().createMonitorWaitRequest();
            monitorWaitRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            monitorWaitRequest.enable();
            monitorWaitedRequest = eventRequestManager().createMonitorWaitedRequest();
            monitorWaitedRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            monitorWaitedRequest.enable();
        } else {
            System.out.println("request monitor events not supported " );
        }


        resumeTo("MonitorEventTestTarg", "foo", "()V");

        /*
         * resume until end
         */
        listenUntilVMDisconnect();

        /*
         * At least one of each type event should have recevied by this test.
         */
        if (vm().canRequestMonitorEvents()) {
            if (actualContendedEnterCount == 0) {
                failure("Did not receive any  contended enter event.");
            }
            if (actualContendedEnteredCount == 0) {
                failure("Did not receive any contended entered event. ");
            }
            if (actualWaitCount == 0) {
                failure("Did not receive any contended monitor wait event");
            }
            if (actualWaitedCount == 0) {
                failure("Did not receive any contended monitor waited event");
            }
        }


        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("MonitorEventTest: passed");
        } else {
            throw new Exception("MonitorEventTest: failed");
        }
    }
}
