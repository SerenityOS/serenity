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

public class hs101t004 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new hs101t004().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    int status = Consts.TEST_PASSED;
    Log log = null;
    long timeout = 0;

    final static int MAX_LENGTH = 60;

    // tested thread
    hs101t004Thread thread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        thread = new hs101t004Thread("Debuggee Thread", MAX_LENGTH);
        status = checkStatus(status);

        thread.start();
        thread.startingBarrier.waitFor();
        status = checkStatus(status);
        thread.letItFinish();

        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        log.display("Debugee finished: i = " + thread.i);

        if (thread.i > MAX_LENGTH) {
            log.complain("thread.i > MAX_LENGTH");
            status = Consts.TEST_FAILED;
        }

        for (int i = 0; i < thread.i; i++) {
            long expected = (1L << (i + 3)) - 3;
            if (thread.numbers[i] != expected) {
                log.complain("Wrong number[" + i + "]: " + thread.numbers[i] +
                    ", expected: " + expected);
                status = Consts.TEST_FAILED;
            }
        }

        return status;
    }
}

/* =================================================================== */

class hs101t004Thread extends Thread {
    public Wicket startingBarrier = new Wicket();
    private volatile boolean flag = true;
    public int i;
    public long numbers[];

    public hs101t004Thread(String name, int n) {
        super(name);
        numbers = new long[n];
    }

    public void run() {
        startingBarrier.unlock();

        try {
            for (i = 0; flag && (i < numbers.length); i++) {
                numbers[i] = ackermann(3, i);
            }
        } catch (StackOverflowError e) {
            // ignore
        }
    }

    // Ackermann's function
    long ackermann(int m, long n) {
        if (m == 0) {
            return n + 1;
        } else if (n == 0) {
            return ackermann(m-1, 1);
        } else {
            return ackermann(m-1, ackermann(m, n - 1));
        }
    }

    public void letItFinish() {
        flag = false;
    }
}
