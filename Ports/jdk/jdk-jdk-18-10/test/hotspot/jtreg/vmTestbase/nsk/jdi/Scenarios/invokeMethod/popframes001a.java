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

/**
 *  <code>popframes001a</code> is deugee's part of the popframes001.
 */

public class popframes001a {

    volatile public static boolean finishIt = false;

    public static void main(String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.out, argHandler);

        popframes001b.loadClass = true;

        while (!finishIt) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }

        log.display("completed succesfully.");
        System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
    }
}

class popframes001b {
    public final static int INITIAL_VALUE        = 0;

    public static boolean loadClass = false;
    public static int flag = INITIAL_VALUE;

    public final static String methodName = "runIt";
    public final static String methodNameCaller = "runItCaller";
    public final static String flagName = "flag";

    public static void runIt() {
        flag = INITIAL_VALUE;
    }

    // We need to call runIt() from a java function.
    // This is because jvmti function popFrame() requires that
    // both calling and called methods are java functions.
    public static void runItCaller() {
        runIt();
    }
}
