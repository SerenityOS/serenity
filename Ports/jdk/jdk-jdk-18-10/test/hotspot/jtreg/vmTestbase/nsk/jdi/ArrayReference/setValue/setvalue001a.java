/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.ArrayReference.setValue;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class setvalue001a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        setvalue001aClassToCheck classToCheck = new setvalue001aClassToCheck();

        log.display("DEBUGEE> debugee started.");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            log.display("DEBUGEE> \"quit\" signal recieved.");
            log.display("DEBUGEE> completed succesfully.");
            System.exit(95);
        }
        log.complain("DEBUGEE FAILURE> unexpected signal "
                         + "(no \"quit\") - " + instruction);
        log.complain("DEBUGEE FAILURE> TEST FAILED");
        System.exit(97);
    }
}

class setvalue001aClassToCheck {

    // Each array has 18 components
    // 0..8 are to set and get values, 9..17 are to get sample Value in
    // debugger
    static boolean z1[] = {false, true, false, true, true, true, false,
                           false, true,
                           true, false, true, false, false, false, true,
                           true, false};
    static byte    b1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                           Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE,
                           Byte.MIN_VALUE + 1, -4, 4, Byte.MAX_VALUE - 1};
    static char    c1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                           Character.MIN_VALUE, '\u00ff', '\uff00',
                           Character.MAX_VALUE, Character.MIN_VALUE + 1,
                           '\u1234', '\u4321', '\u8888',
                           Character.MAX_VALUE - 1};
    static double  d1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                           Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -1, -0,
                           0, 1, Double.MAX_VALUE, Double.POSITIVE_INFINITY,
                           Double.NaN};
    static float   f1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                           Float.NEGATIVE_INFINITY, Float.MIN_VALUE, -1, -0,
                           0, 1, Float.MAX_VALUE, Float.POSITIVE_INFINITY,
                           Float.NaN};
    static int     i1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                           Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE,
                           Integer.MIN_VALUE + 1, -2, 2, Integer.MAX_VALUE - 1};
    static long    l1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                           Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE,
                           Long.MIN_VALUE + 1, -2, 2, Long.MAX_VALUE - 1};
    static short   r1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                           Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE,
                           Short.MIN_VALUE + 1, -3, 3, Short.MAX_VALUE - 1};

    static final     long lF1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                                   1, 2, 3, 4, 5, 6, 7, 8, 9};
    static private   long lP1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                                   100, 101, 102, 103, 104, 105, 106, 107, 108};
    static public    long lU1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                                   -1, -2, -3, -4, -5, -6, -7, -8, -9};
    static protected long lR1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                                   Long.MIN_VALUE,     Long.MIN_VALUE + 1,
                                   Long.MIN_VALUE + 2, Long.MIN_VALUE + 3,
                                   Long.MIN_VALUE + 4, Long.MIN_VALUE + 5,
                                   Long.MIN_VALUE + 6, Long.MIN_VALUE + 7,
                                   Long.MIN_VALUE + 8};
    static transient long lT1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                                   Long.MAX_VALUE,     Long.MAX_VALUE - 1,
                                   Long.MAX_VALUE - 2, Long.MAX_VALUE - 3,
                                   Long.MAX_VALUE - 4, Long.MAX_VALUE - 5,
                                   Long.MAX_VALUE - 6, Long.MAX_VALUE - 7,
                                   Long.MAX_VALUE - 8};
    static volatile  long lV1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                                   1, -1, 1, 0, -1, 1, 0, -1, 1};
}
