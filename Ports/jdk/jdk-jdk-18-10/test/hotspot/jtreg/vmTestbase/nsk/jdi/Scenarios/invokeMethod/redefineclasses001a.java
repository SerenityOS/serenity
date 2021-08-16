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

package nsk.jdi.Scenarios.invokeMethod;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import java.io.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 *  <code>redefineclasses001a</code> is deugee's part of the redefineclasses001.
 */
public class redefineclasses001a {
    public final static String brkpMethodName = "main";
    public final static int [] brkpLineNumber = {51, 59};

    public final static String testedObjName = "obj";
    private static Log log = null;

    public static void main(String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(System.out, argHandler);

        redefineclasses001b.loadClass = true;
        int i = 0;
        do {
            switch (++i) { // brkpLineNumber[0]
            case 0:
                break;
            case 1:
                new redefineclasses001b().start();

                waitStarting();

                notifyFinishing(); // brkpLineNumber[1]
                break;
            }
        } while (i < redefineclasses001.expectedEventCount);

        log.display("completed succesfully.");
        System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
    }

    private static void waitStarting() {
        log.display("waiting start of the tested thread...");
        synchronized(redefineclasses001b.waitStarting) {
            while(!redefineclasses001b.notified) {
                try {
                    redefineclasses001b.waitStarting.wait(10);
                } catch(InterruptedException e) {
                    log.display("Unexpected" + e);
                }
            }
        }
    }

    private static void notifyFinishing() {
        log.display("notifying the thread to finish...");
        synchronized(redefineclasses001b.waitFinishing) {
            redefineclasses001b.waitFinishing.notify();
        }
    }
}
