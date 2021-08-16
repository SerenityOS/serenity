/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.hotswap.HS201;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class hs201t001 extends DebugeeClass {


    static final String PACKAGE_NAME = "nsk.jvmti.scenarios.hotswap.HS201";
    static final String TESTED_EXCEPTION_NAME = PACKAGE_NAME + ".hs201t001a";
    static final String PATH_TO_NEW_BYTECODE = "pathToNewByteCode";

    static final String METHOD_NAME = "doInit";
    static final int MAX_TRIES_TO_SUSPEND_THREAD = 10;

    public static volatile int currentStep = 0;
    public static volatile boolean isInDoInitFunction = false;

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new hs201t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    public static Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    static native void setThread(Thread thread);
    static native boolean suspendThread(Thread thread);
    static native boolean resumeThread(Thread thread);
    static native boolean popFrame(Thread thread);

    // run debuggee
    public int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        log.display(">>> starting tested thread");
        Thread thread = new hs201t001Thread();

        // testing sync
        status = checkStatus(status);

        setThread(thread);
        thread.start();

        while (currentStep != 4) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }

        int suspendTry = 1;
        while ( true ) {
            suspendThread(thread);

            log.display("Checking that the thread is inside " + METHOD_NAME);

            if ( isInDoInitFunction ) {
                break;
            }

            if ( suspendTry > MAX_TRIES_TO_SUSPEND_THREAD ) {
                log.complain("Unable to suspend thread in method " + METHOD_NAME);
                status = Consts.TEST_FAILED;
                break;
            }

            log.complain("Thread suspended in a wrong moment. Retrying...");
            resumeThread(thread);
            suspendTry++;
        }

        if ( status != Consts.TEST_FAILED ) {
            log.display("Popping a frame in the test thread");
            popFrame(thread);
        } else {
            log.complain("Test is going to fail: just resuming the test thread");
            resumeThread(thread);
        }

        try {
            log.display("Waiting for the test thread to join");
            thread.join();
        } catch (InterruptedException e) {
        }

        // testing sync
        log.display("Testing sync: thread finished");
        status = checkStatus(status);

        return status;
    }

class hs201t001Thread extends Thread {

    hs201t001Thread() {
        setName("hs201t001Thread");
    }

    public void run() {
        // run method
        try {
            throwException();
        } catch (Exception e) {
        }

    }

    void throwException() throws Exception {

        ClassUnloader unloader = new ClassUnloader();
        Class cls = null;
        String path = argHandler.findOptionValue(PATH_TO_NEW_BYTECODE)
                            + "/newclass";

        try {
            log.display("[debuggee] loading exception...");
            unloader.loadClass(TESTED_EXCEPTION_NAME, path);
            cls = unloader.getLoadedClass();
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            log.display("[debuggee] throwing exception...");
            throw (Exception )cls.newInstance();
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
    }

}

}
