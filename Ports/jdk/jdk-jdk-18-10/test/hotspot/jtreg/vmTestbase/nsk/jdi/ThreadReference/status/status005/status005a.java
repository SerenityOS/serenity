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

package nsk.jdi.ThreadReference.status.status005;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged application of the test.
 */
public class status005a {

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

    static String testedThreadNames[] = {"threadWaitingInfinitely", "threadWaitingLong"};
    static status005aThreadWaitingInfinitely threadWaitingInfinitely;
    static status005aThreadWaitingLong threadWaitingLong;

    static Wicket wickets[] = { new Wicket(), new Wicket() };
    static Lock lock = new Lock();

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        pipe.println(status005.SIGNAL_READY);

        threadWaitingInfinitely = new status005aThreadWaitingInfinitely(testedThreadNames[0]);
        threadWaitingLong = new status005aThreadWaitingLong(testedThreadNames[1]);

        threadWaitingInfinitely.start();
        wickets[0].waitFor();

        threadWaitingLong.start();
        wickets[1].waitFor();

        pipe.println(status005.SIGNAL_GO);
        receiveSignal(status005.SIGNAL_QUIT);

        synchronized (lock) {
            lock.notifyAll();
        }

        waitForFinish(threadWaitingInfinitely);
        waitForFinish(threadWaitingLong);

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


class status005aThreadWaitingInfinitely extends Thread {
    status005aThreadWaitingInfinitely (String name) {
        super(name);
    }

    public void run () {
        status005a.wickets[0].unlock();
        synchronized (status005a.lock) {
            try {
                status005a.lock.wait();
            } catch (InterruptedException e) {
                throw new Failure(Thread.currentThread().getName() + ": Unexpected InterruptedException while waiting");
            }
        }
    }
}

class status005aThreadWaitingLong extends Thread {
    status005aThreadWaitingLong (String name) {
        super(name);
    }

    public void run () {
        status005a.wickets[1].unlock();
        synchronized (status005a.lock) {
            try {
                status005a.lock.wait(status005a.argHandler.getWaitTime() * 60000 * 2);
            } catch (InterruptedException e) {
                throw new Failure(Thread.currentThread().getName() + ": Unexpected InterruptedException while waiting");
            }
        }
    }
}
