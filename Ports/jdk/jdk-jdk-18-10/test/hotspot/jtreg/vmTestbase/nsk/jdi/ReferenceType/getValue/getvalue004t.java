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

package nsk.jdi.ReferenceType.getValue;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is debuggee class.
 */
public class getvalue004t {
    public static void main(String args[]) {
        System.exit(run(args) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        // force debuggee VM to load dummy classes
        getvalue004tDummySuperCls dummySuperCls =
            new getvalue004tDummySuperCls();
        getvalue004tDummyCls dummyCls =
            new getvalue004tDummyCls();

        pipe.println(getvalue004.COMMAND_READY);
        String cmd = pipe.readln();
        if (cmd.equals(getvalue004.COMMAND_QUIT)) {
            System.err.println("Debuggee: exiting due to the command: "
                + cmd);
            return Consts.TEST_PASSED;
        }

        int stopMeHere = 0; // getvalue004.DEBUGGEE_STOPATLINE

        cmd = pipe.readln();
        if (!cmd.equals(getvalue004.COMMAND_QUIT)) {
            System.err.println("TEST BUG: unknown debugger command: "
                + cmd);
            return Consts.TEST_FAILED;
        }
        return Consts.TEST_PASSED;
    }
}

// dummy super class used for provoking IllegalArgumentException in debugger
class getvalue004tDummySuperCls {
    private static boolean boolPrFld = true;
    private static byte bytePrFld = Byte.MIN_VALUE;
    private static char charPrFld = 'z';
    private static double doublePrFld = Double.MAX_VALUE;
    private static float floatPrFld = Float.MIN_VALUE;
    private static int intPrFld = Integer.MIN_VALUE;
    private static long longPrFld = Long.MIN_VALUE;
    private static short shortPrFld = Short.MAX_VALUE;
}

// dummy class used for provoking IllegalArgumentException in debugger
class getvalue004tDummyCls extends getvalue004tDummySuperCls {
    static boolean boolFld = false;
    static byte byteFld = 127;
    static char charFld = 'a';
    static double doubleFld = 6.2D;
    static float floatFld = 5.1F;
    static int intFld = 2147483647;
    static long longFld = 9223372036854775807L;
    static short shortFld = -32768;
}
