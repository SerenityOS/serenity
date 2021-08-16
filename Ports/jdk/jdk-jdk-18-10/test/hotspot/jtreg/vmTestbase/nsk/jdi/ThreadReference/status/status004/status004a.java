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

package nsk.jdi.ThreadReference.status.status004;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged application of the test.
 */
public class status004a {

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

    static String testedThreadNames[] = {"threadBlockedOnMethod", "threadBlockedOnStatmt"};
    static status004aThreadBlockedOnMethod threadBlockedOnMethod;
    static status004aThreadBlockedOnStatmt threadBlockedOnStatmt;

    static Wicket wickets[] = { new Wicket(), new Wicket() };
    static Lock lock = new Lock();

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        pipe.println(status004.SIGNAL_READY);

        threadBlockedOnMethod = new status004aThreadBlockedOnMethod(testedThreadNames[0]);
        threadBlockedOnStatmt = new status004aThreadBlockedOnStatmt(testedThreadNames[1]);

        synchronized (lock) {
            threadBlockedOnMethod.start();
            wickets[0].waitFor();

            threadBlockedOnStatmt.start();
            wickets[1].waitFor();

            pipe.println(status004.SIGNAL_GO);
            receiveSignal(status004.SIGNAL_QUIT);
        }

        waitForFinish(threadBlockedOnMethod);
        waitForFinish(threadBlockedOnStatmt);

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

class Lock {
    synchronized void foo () {}
}


class status004aThreadBlockedOnMethod extends Thread {
    status004aThreadBlockedOnMethod (String name) {
        super(name);
    }

    public void run () {
        status004a.wickets[0].unlock();
        synchronized (status004a.lock) {};
    }
}

class status004aThreadBlockedOnStatmt extends Thread {
    status004aThreadBlockedOnStatmt (String name) {
        super(name);
    }

    public void run () {
        status004a.wickets[1].unlock();
        status004a.lock.foo();
    }
}
