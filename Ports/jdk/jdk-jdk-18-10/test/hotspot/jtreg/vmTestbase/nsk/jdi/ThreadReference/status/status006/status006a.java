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

package nsk.jdi.ThreadReference.status.status006;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged application of the test.
 */
public class status006a {

    //------------------------------------------------------- immutable common fields

    private static int exitStatus;
    private static Log log;
    private static IOPipe pipe;
    static ArgumentHandler argHandler;

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

    static String testedThreadNames[] = {"threadSleepingLong", "threadSleepingLongInt"};
    static status006aThreadSleepingLong threadSleepingLong;
    static status006aThreadSleepingLongInt threadSleepingLongInt;
    static Wicket wickets[] = { new Wicket(), new Wicket() };

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        pipe.println(status006.SIGNAL_READY);

        threadSleepingLong = new status006aThreadSleepingLong(testedThreadNames[0]);
        threadSleepingLong.start();
        wickets[0].waitFor();

        pipe.println(status006.SIGNAL_GO);
        receiveSignal(status006.SIGNAL_NEXT);
        threadSleepingLong.interrupt();

        threadSleepingLongInt = new status006aThreadSleepingLongInt(testedThreadNames[1]);
        threadSleepingLongInt.start();
        wickets[1].waitFor();

        pipe.println(status006.SIGNAL_GO);
        receiveSignal(status006.SIGNAL_QUIT);
        threadSleepingLongInt.interrupt();

        waitForFinish(threadSleepingLong);
        waitForFinish(threadSleepingLongInt);
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

class status006aThreadSleepingLong extends Thread {
    status006aThreadSleepingLong (String name) {
        super(name);
    }

    public void run () {
        status006a.wickets[0].unlock();
        try {
            Thread.currentThread().sleep(status006a.argHandler.getWaitTime() * 60000);
        } catch (InterruptedException e) {
            // its's OK
        }
    }
}

class status006aThreadSleepingLongInt extends Thread {
    status006aThreadSleepingLongInt (String name) {
        super(name);
    }

    public void run () {
        status006a.wickets[1].unlock();
        try {
            Thread.currentThread().sleep(status006a.argHandler.getWaitTime() * 60000, 0);
        } catch (InterruptedException e) {
            // it's OK
        }
    }
}
