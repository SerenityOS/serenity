/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ThreadGroupReference.toString;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged application of the test.
 */
public class tostring001a {

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

        display("debuger's <" + signal + "> signal received.");
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    static ThreadGroup mainGroup = new ThreadGroup("mainThreadGroup");
    static ThreadGroup thread2Group = new ThreadGroup("thread2Group");

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);
        try {
            tostring001aThread thread2 = new tostring001aThread(thread2Group, "testedThread");
            display("thread2 is created");

            synchronized (tostring001aThread.lockingObject) {
                synchronized (tostring001aThread.waitnotifyObj) {
                    thread2.start();
                    try {
                        tostring001aThread.waitnotifyObj.wait();
                    } catch (InterruptedException ie) {
                        thread2.interrupt();
                        throw new Failure("Unexpected InterruptedException while waiting for notifying.");
                    }

                    pipe.println(tostring001.SIGNAL_READY);
                    receiveSignal(tostring001.SIGNAL_QUIT);
                }
            }

            display("completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        } catch (Failure e) {
            log.complain(e.getMessage());
            System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
        }
    }

    //--------------------------------------------------------- test specific methods

}

//--------------------------------------------------------- test specific classes

class tostring001aThread extends Thread {

    public static Object waitnotifyObj = new Object();
    public static Object lockingObject = new Object();

    private static ThreadGroup thread2Group = null;

    public tostring001aThread(ThreadGroup groupName, String threadName) {
        super(groupName, threadName);
        thread2Group = groupName;
    }

    public void run() {
        synchronized (waitnotifyObj) {
            waitnotifyObj.notify();
        }
        synchronized (lockingObject) {}
    }
}
