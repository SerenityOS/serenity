/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ThreadReference.status.status003;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged application of the test.
 */
public class status003a {

    //------------------------------------------------------- immutable common fields

    private static int exitStatus;
    private static ArgumentHandler argHandler;
    private static Log log;
    private static IOPipe pipe;

    //------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    public static void receiveSignal(String signal) {
        String line = pipe.readln();

        if ( !line.equals(signal) )
            throw new Failure("UNEXPECTED debugger's signal " + line);

        display("debugger's <" + signal + "> signal received.");
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    static String testedThreadNames[] = {"finishedThread", "interruptedThread"};
    static Thread finishedThread;
    static Thread interruptedThread;

    static Wicket wicket1 = new Wicket();
    static Wicket wicket2 = new Wicket();
    static Object lock = new Object();

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        pipe.println(status003.SIGNAL_READY);

        finishedThread = new status003aThreadFinished(testedThreadNames[0]);
        display(finishedThread.getName() + " is created");

        finishedThread.start();
        wicket1.waitFor();
        waitForFinish(finishedThread);

        interruptedThread = new status003aThreadInterrupted(testedThreadNames[1]);
        display(interruptedThread.getName() + " is created");

        synchronized (lock) {
            interruptedThread.start();
            wicket2.waitFor();

            interruptedThread.interrupt();
            if (!interruptedThread.isInterrupted()) {
                throw new Failure(interruptedThread.getName() + " has not been interrupted");
            }
        }
        waitForFinish(interruptedThread);

        pipe.println(status003.SIGNAL_GO);
        receiveSignal(status003.SIGNAL_QUIT);

        display("completed succesfully.");
        System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
    }

    private static void waitForFinish (Thread thread) {
        if (thread.isAlive()) {
            try {
                thread.join(argHandler.getWaitTime() * 60000);
                display(thread.getName() + " is finished");
            } catch (InterruptedException e) {
                throw new Failure("Unexpected InterruptedException while waiting for join of " + thread.getName());
            }
            if (thread.isAlive()) {
                throw new Failure(thread.getName() + " has not finished after " + argHandler.getWaitTime() + " mins");
            }
        }
    }
    //--------------------------------------------------------- test specific methods

}

//--------------------------------------------------------- test specific classes

class status003aThreadFinished extends Thread {
    status003aThreadFinished (String name) {
        super(name);
    }

    public void run () {
        status003a.wicket1.unlock();
        status003a.display(Thread.currentThread().getName() + " finished.");
    }
}

class status003aThreadInterrupted extends Thread {
    status003aThreadInterrupted (String name) {
        super(name);
    }

    public void run () {
        status003a.wicket2.unlock();
        synchronized (status003a.lock) {};
        status003a.display(Thread.currentThread().getName() + " finished.");
    }
}
