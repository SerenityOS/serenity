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

public class hs101t003 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new hs101t003().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    int status = Consts.TEST_PASSED;
    Log log = null;
    long timeout = 0;

    // the first 64 precalculated "Fibonacci Numbers"
    static final long EXPECTED[] = {
        1L, 1L, 1L, 2L,
        3L, 5L, 8L, 13L,
        21L, 34L, 55L, 89L,
        144L, 233L, 377L, 610L,
        987L, 1597L, 2584L, 4181L,
        6765L, 10946L, 17711L, 28657L,
        46368L, 75025L, 121393L, 196418L,
        317811L, 514229L, 832040L, 1346269L,
        2178309L, 3524578L, 5702887L, 9227465L,
        14930352L, 24157817L, 39088169L, 63245986L,
        102334155L, 165580141L, 267914296L, 433494437L,
        701408733L, 1134903170L, 1836311903L, 2971215073L,
        4807526976L, 7778742049L, 12586269025L, 20365011074L,
        32951280099L, 53316291173L, 86267571272L, 139583862445L,
        225851433717L, 365435296162L, 591286729879L, 956722026041L,
        1548008755920L, 2504730781961L, 4052739537881L, 6557470319842L
    };

    // tested thread
    hs101t003Thread thread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        thread = new hs101t003Thread("Debuggee Thread", EXPECTED.length);
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

        if (thread.i > EXPECTED.length) {
            log.complain("thread.i > EXPECTED.length");
            status = Consts.TEST_FAILED;
        }

        for (int i = 0; i < thread.i; i++) {
            if (thread.numbers[i] != EXPECTED[i]) {
                log.complain("Wrong number[" + i + "]: " + thread.numbers[i] +
                    ", expected: " + EXPECTED[i]);
                status = Consts.TEST_FAILED;
            }
        }

        return status;
    }
}

/* =================================================================== */

class hs101t003Thread extends Thread {
    public Wicket startingBarrier = new Wicket();
    private volatile boolean flag = true;
    public int i;
    public long numbers[];

    public hs101t003Thread(String name, int n) {
        super(name);
        numbers = new long[n];
    }

    public void run() {
        startingBarrier.unlock();

        for (i = 0; flag && (i < numbers.length); i++) {
            numbers[i] = fibonacci(i);
        }
    }

    // calculate Fibonacci Numbers
    long fibonacci(int n) {
        if (n <= 2) {
            return 1;
        } else {
            return fibonacci(n-1) + fibonacci(n-2);
        }
    }

    public void letItFinish() {
        flag = false;
    }
}
