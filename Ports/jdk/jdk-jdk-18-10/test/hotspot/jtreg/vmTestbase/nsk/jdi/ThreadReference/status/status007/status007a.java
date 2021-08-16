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

package nsk.jdi.ThreadReference.status.status007;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;
import java.util.*;

/**
 * The debugged application of the test.
 */
public class status007a {

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

    static String testedThreadNames[] = {"threadRunning", "threadRunningNanos"};
    static status007aThreadRunning threadRunning;
    static Wicket wicket = new Wicket();
    static volatile boolean shouldRun = true;

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        pipe.println(status007.SIGNAL_READY);

        threadRunning = new status007aThreadRunning(testedThreadNames[0]);
        threadRunning.start();
        wicket.waitFor();

        pipe.println(status007.SIGNAL_GO);
        receiveSignal(status007.SIGNAL_QUIT);

        shouldRun = false;
        waitForFinish(threadRunning);
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

class status007aThreadRunning extends Thread {
    status007aThreadRunning (String name) {
        super(name);
    }

    public void run () {
        status007a.wicket.unlock();
        // do something
        Vector<String> v = new Vector<String>();
        while (status007a.shouldRun) {
            v.add("bla-");
            if (v.size() > 1000) {
                v.removeAllElements();
            }
        }
    }
}
