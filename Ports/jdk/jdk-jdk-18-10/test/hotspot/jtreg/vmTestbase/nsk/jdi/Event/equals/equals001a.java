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

package nsk.jdi.Event.equals;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged application of the test.
 */
public class equals001a {

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

    static Object lock = new Object();
    static Object wait = new Object();

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);
        try {
            Thread thread1 = new equals001aThread("thread1");

            synchronized (lock) {
                 synchronized(wait) {
                      thread1.start();
                      try {
                          wait.wait();
                          pipe.println(equals001.SIGNAL_READY);
                      } catch (InterruptedException e) {
                          throw new Failure("Unexpected InterruptedException while waiting for notification");
                      }
                 }
                 receiveSignal(equals001.SIGNAL_GO);
            }
            try {
                thread1.join();
            } catch (InterruptedException e) {
                throw new Failure("Unexpected InterruptedException while waiting for thread1 join");
            }

//            receiveSignal(equals001.SIGNAL_QUIT);
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

class equals001aThread extends Thread {

    String name;

    public equals001aThread (String name) {
        super(name);
        this.name = name;
    }

    public void run() {
        synchronized (equals001a.wait) {
            equals001a.wait.notifyAll();
        }
        synchronized (equals001a.lock) {
        }

        foo();

        equals001aThread1 thread2 = new equals001aThread1();
        thread2.start();

        try {
            throw new Exception();
        } catch (Exception e) {
        }

        try {
            thread2.join();
        } catch (InterruptedException e) {
            throw new Failure("Unexpected InterruptedException while waiting for thread2 join");
        }
    }

    void foo () {
        name = "afterFoo";
        equals001a.display("thread1's name == " + name);
    }
}

class equals001aThread1 extends Thread {
    public void run() {
    }
}
