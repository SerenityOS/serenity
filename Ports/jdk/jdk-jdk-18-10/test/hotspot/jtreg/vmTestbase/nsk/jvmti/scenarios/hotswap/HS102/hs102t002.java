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

package nsk.jvmti.scenarios.hotswap.HS102;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class hs102t002 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new hs102t002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    int status = Consts.TEST_PASSED;
    Log log = null;
    long timeout = 0;

    // string patterns
    static final String PATTERN[] = {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
        "BCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzA",
        "CDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzAB",
        "DEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABC",
        "EFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCD",
        "FGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDE",
        "GHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEF",
        "HIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFG",
        "IJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGH",
        "JKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHI",
        "KLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJ",
        "LMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJK",
        "MNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKL",
        "NOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLM",
        "OPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN",
        "PQRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO",
        "QRSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOP",
        "RSTUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ",
        "STUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQR",
        "TUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRS",
        "UVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRST",
        "VWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTU",
        "WXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV",
        "XYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVW",
        "YZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWX",
        "ZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXY",
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
        "bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZa",
        "cdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZab",
        "defghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabc",
        "efghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcd",
        "fghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcde",
        "ghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdef",
        "hijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefg",
        "ijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefgh",
        "jklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghi",
        "klmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghij",
        "lmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijk",
        "mnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijkl",
        "nopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklm",
        "opqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmn",
        "pqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmno",
        "qrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnop",
        "rstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopq",
        "stuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqr",
        "tuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrs",
        "uvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrst",
        "vwxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstu",
        "wxyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuv",
        "xyzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvw",
        "yzABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwx",
        "zABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxy"
    };

    // tested thread
    hs102t002Thread thread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        thread = new hs102t002Thread("Debuggee Thread", PATTERN[0]);
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

        log.display("Debugee finished: i=" + thread.i +
            "\n\tsb = \"" + thread.sb + "\"");

        if (thread.i > PATTERN.length) {
            log.complain("thread.i > PATTERN.length");
            status = Consts.TEST_FAILED;
        }

        if (!thread.sb.toString().equals(PATTERN[thread.i])) {
            log.complain("Wrong result string (i=" + thread.i + "): "
                + thread.sb + ", expected: " + PATTERN[thread.i]);
            status = Consts.TEST_FAILED;
        }

        return status;
    }
}

/* =================================================================== */

class hs102t002Thread extends Thread {
    public Wicket startingBarrier = new Wicket();
    private volatile boolean flag = true;
    public int i;
    public StringBuffer sb;

    public hs102t002Thread(String name, String s) {
        super(name);
        sb = new StringBuffer(s);
    }

    public void run() {
        startingBarrier.unlock();

        int sblen = sb.length();
        for (i = 0; flag; i = (i + 1) % sblen) {
            char c = sb.charAt(0);
            sb.deleteCharAt(0);
            sb.append(c);
        }
    }

    public void letItFinish() {
        flag = false;
    }
}
