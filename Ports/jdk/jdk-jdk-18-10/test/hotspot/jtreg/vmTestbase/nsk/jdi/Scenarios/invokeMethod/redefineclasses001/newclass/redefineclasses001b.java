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
 *  <code>redefineclasses001b</code> is deugee's part of the redefineclasses001.
 */

public class redefineclasses001b extends Thread{
    public final static int INITIAL_VALUE        = 0;
    public final static int BEFORE_REDEFINITION = 1;
    public final static int AFTER_REDEFINITION  = 2;

    public static boolean loadClass = false;
    public static int flag = INITIAL_VALUE;

    public final static String methodName = "runIt";
    public final static String flagName = "flag";

    public static Object waitStarting = new Object();
    public static Object waitFinishing = new Object();
    public volatile static boolean notified = false;

    public void run() {
        runIt(true);
    }

    public static void runIt(boolean doWait) {

        flag = AFTER_REDEFINITION;
//             ^^^^^^^^^^^^^^^^^^^ it will be redefined

//        System.out.println("runIt::notify...");
        notified = false;
        synchronized(waitStarting) {
            waitStarting.notify();
        }
        notified = true;
        System.out.println("runIt::notified...");

        if (!doWait) {
            return;
        }

        synchronized(waitFinishing) {
            try {
                waitFinishing.wait();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static String flag2String(int value) {
        switch (value) {
        case INITIAL_VALUE:
            return "INITIAL_VALUE(" + INITIAL_VALUE + ")";
        case BEFORE_REDEFINITION:
            return "BEFORE_REDEFINITION(" + BEFORE_REDEFINITION + ")";
        case AFTER_REDEFINITION:
            return "AFTER_REDEFINITION(" + AFTER_REDEFINITION + ")";
        default:
            return "UNKNOWN_VALUE";
        }
    }
}
