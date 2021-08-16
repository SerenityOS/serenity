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

package nsk.jvmti.scenarios.hotswap.HS101;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class hs101t007 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new hs101t007().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    int status = Consts.TEST_PASSED;
    Log log = null;
    long timeout = 0;

    final static int NUMBER_OF_ITERATIONS = 10000;

    // tested thread
    hs101t007Thread thread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        thread = new hs101t007Thread("Debuggee Thread", NUMBER_OF_ITERATIONS);
        status = checkStatus(status);

        thread.start();
        thread.startingBarrier.waitFor();

        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        log.display("Debugee finished: i=" + thread.i +
                    ", " + "Hi=" + thread.Hi + ", Lo=" + thread.Lo);

        if (thread.Hi != NUMBER_OF_ITERATIONS/2) {
            log.complain("Wrong number of Hi exceptions: " + thread.Hi +
                ", expected: " + NUMBER_OF_ITERATIONS/2);
            status = Consts.TEST_FAILED;
        }

        if (thread.Lo != NUMBER_OF_ITERATIONS/2) {
            log.complain("Wrong number of Lo exceptions: " + thread.Lo +
                ", expected: " + NUMBER_OF_ITERATIONS/2);
            status = Consts.TEST_FAILED;
        }

        return checkStatus(status);
    }
}

/* =================================================================== */

class hs101t007Thread extends Thread {
    public Wicket startingBarrier = new Wicket();
    private volatile boolean flag = true;
    public int i;
    public int n;
    public int Lo = 0;
    public int Hi = 0;

    public hs101t007Thread(String name, int n) {
        super(name);
        this.n = n;
    }

    public void run() {
        startingBarrier.unlock();

        for (i = 0; i < n; i++) {
            some_function(i);
        }
    }

    class Lo_Exception extends Exception {
        int num = 0;

        public Lo_Exception(int num) {
            this.num = num;
        }

        public String toString() {
            return "Lo_Exception, num = " + this.num;
        }
    }

    class Hi_Exception extends Exception {
        int num = 0;

        public Hi_Exception(int num) {
            this.num = num;
        }

        public String toString() {
            return "Hi_Exception, num = " + this.num;
        }
    }

    void some_function(int n) {
        try {
            hi_function(n);
        } catch (Exception e) {
            throw new Failure("shouldn't reach here: " + e);
        }
    }

    void hi_function(int n) throws Hi_Exception, Lo_Exception {
        try {
            lo_function(n);
        } catch (Hi_Exception e) {
            Hi++;
        }
    }

    void lo_function(int n) throws Hi_Exception, Lo_Exception {
        try {
            blowup(n);
        } catch (Lo_Exception e) {
            Lo++;
        }
    }

    void blowup(int n) throws Hi_Exception, Lo_Exception {
        if ( (n & 1) == 1) {
            throw new Lo_Exception(n);
        } else {
            throw new Hi_Exception(n);
        }
    }
}
