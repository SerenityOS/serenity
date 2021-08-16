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

package nsk.jdi.ClassType.invokeMethod;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 *  <code>invokemethod003a</code> is deugee's part of the invokemethod003.
 */
public class invokemethod003a {

    public final static String anotherClassName = "invokemethod003b";
    public final static String class2Check = "invokemethod003Child";
    public final static String brkpMethodName = "main";
    public final static int brkpLineNumber = 50;

    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        invokemethod003Child child = new invokemethod003Child();
        invokemethod003b imb = new invokemethod003b();
        pipe.println(invokemethod003.SGNL_READY);

        String instr = pipe.readln(); // brkpLineNumber
        if (instr.equals(invokemethod003.SGNL_QUIT)) {
            log.display("completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        }

        log.complain("DEBUGEE> unexpected signal of debugger.");
        System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
    }

    public static long publicFromParent(int one, int two, int three) {
        return one + two + three;
    }

    protected static long protectFromParent(int one, int two, int three) {
        return one + two + three;
    }

    private static long privateFromParent(int one, int two, int three) {
        return one + two + three;
    }
}

class invokemethod003Child extends invokemethod003a {
    invokemethod003Child() {
    }

    public static long fromChild(int one, int two, int three) {
        return one + two + three;
    }
}

class invokemethod003b {
    public void run() {
        int i = 1;
    }
}
