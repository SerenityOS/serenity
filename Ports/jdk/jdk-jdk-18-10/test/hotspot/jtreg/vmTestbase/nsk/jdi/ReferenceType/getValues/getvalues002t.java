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

package nsk.jdi.ReferenceType.getValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is debuggee class.
 */
public class getvalues002t {
    public static void main(String args[]) {
        System.exit(run(args) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        // force debuggee VM to load dummy classes
        getvalues002tDummySuperCls dummySuperCls =
            new getvalues002tDummySuperCls();
        getvalues002tDummyCls dummyCls =
            new getvalues002tDummyCls();

        pipe.println(getvalues002.COMMAND_READY);
        String cmd = pipe.readln();
        if (cmd.equals(getvalues002.COMMAND_QUIT)) {
            System.err.println("Debuggee: exiting due to the command: "
                + cmd);
            return Consts.TEST_PASSED;
        }

        int stopMeHere = 0; // getvalues002.DEBUGGEE_STOPATLINE

        cmd = pipe.readln();
        if (!cmd.equals(getvalues002.COMMAND_QUIT)) {
            System.err.println("TEST BUG: unknown debugger command: "
                + cmd);
            return Consts.TEST_FAILED;
        }
        return Consts.TEST_PASSED;
    }
}

// dummy super class used for provoking IllegalArgumentException in debugger
class getvalues002tDummySuperCls {
    private static boolean boolPrFld = false;
    private static byte bytePrFld = 127;
    private static char charPrFld = 'b';
    private static double doublePrFld = 6.2D;
    private static float floatPrFld = 5.1F;
    private static int intPrFld = 2147483647;
    private static long longPrFld = 9223372036854775807L;
    private static short shortPrFld = -32768;
}

// dummy class used for provoking IllegalArgumentException in debugger
class getvalues002tDummyCls extends getvalues002tDummySuperCls {
    static boolean boolFld = false;
    static byte byteFld = 127;
    static char charFld = 'a';
    static double doubleFld = 6.2D;
    static float floatFld = 5.1F;
    static int intFld = 2147483647;
    static long longFld = 9223372036854775807L;
    static short shortFld = -32768;

    static boolean boolMiscFld = true;
    static byte byteMiscFld = Byte.MIN_VALUE;
    protected char charMiscFld = 'c';
    static double doubleMiscFld = Double.MAX_VALUE;
    static float floatMiscFld = Float.MAX_VALUE;
    static int intMiscFld = Integer.MIN_VALUE;
    long longMiscFld = Long.MIN_VALUE;
    static short shortMiscFld = Short.MAX_VALUE;
}
