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

package nsk.jdi.BScenarios.multithrd;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 *  <code>tc04x001a</code> is deugee's part of the tc04x001.
 */
public class tc04x001a {

    public final static int threadCount = 3;
    static Log log;

    public final static int checkMethodBrkpLine = 74;
    Thready [] thrds = new Thready [threadCount];

    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(System.out, argHandler);

        tc04x001a obj = new tc04x001a();

        for (int i = 0; i < obj.thrds.length; i++ ) {
            try {
                obj.thrds[i].join(argHandler.getWaitTime() * 60000);//obj.thrds[i].join();
                if (!obj.thrds[i].isAlive()) {
                    log.display(obj.thrds[i].getName() + " finished");
                } else {
                    log.display(obj.thrds[i].getName() + " not finished");
                }
            } catch (InterruptedException e) {
                log.complain("DEBUGEE> " + obj.thrds[i].getName() + " didn't die.");
                System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
            }
        }
        log.display("completed succesfully.");
        System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
    }

    tc04x001a() {
        for (int i = 0; i < thrds.length; i++ ) {
            thrds[i] = new Thready("Thread-" + (i+1));
            thrds[i].start();
        }
    }

    public static void foo(String caller) {
        log.display(caller + "::foo is called"); // checkMethodBrkpLine
    }

    public static void bar(String caller) {
        log.display(caller + "::bar is called");
    }

    static class Thready extends Thread {
        Thready(String name) {
            super(name);
        }

        public void run() {
            tc04x001a.foo(getName());
            tc04x001a.bar(getName());
        }
    }
}
