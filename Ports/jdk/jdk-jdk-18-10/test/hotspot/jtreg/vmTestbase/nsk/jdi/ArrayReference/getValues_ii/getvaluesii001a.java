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


package nsk.jdi.ArrayReference.getValues_ii;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class getvaluesii001a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        getvaluesii001aClassToCheck classToCheck = new getvaluesii001aClassToCheck();

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

class getvaluesii001aClassToCheck {

    // Each of 14 array fields has 14 elements
    static boolean z1[] = {true, false, false, false, true,
                           false, false, true, false, false,
                           false, true, true};
    static byte    b1[] = {0,
                           Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE,
                           -101, -100, -99, Byte.MIN_VALUE + 1, 99,
                           Byte.MAX_VALUE - 1, 101, -1};
    static char    c1[] = {0, 1,
                           Character.MIN_VALUE, '\u00ff', '\uff00', '\uff00',
                           '\uff00', '\u1234', '\u0f0f', Character.MAX_VALUE,
                           '\u0f0f', Character.MIN_VALUE, Character.MAX_VALUE,
                           '\u1234'};
    static double  d1[] = {0, 1, 2,
                           Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -1, -0,
                           0, 1, Double.MAX_VALUE, Double.NaN, Double.NaN,
                           Double.POSITIVE_INFINITY, Double.NaN};
    static float   f1[] = {0, 1, 2, 3,
                           Float.NEGATIVE_INFINITY, Float.MIN_VALUE, -1, -0,
                           0, 1, Float.MAX_VALUE, Float.POSITIVE_INFINITY,
                           Float.NaN, Float.POSITIVE_INFINITY};
    static int     i1[] = {0, 1, 2, 3, 4,
                           -255, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE,
                           254, 255, 256};
    static long    l1[] = {0, 1, 2, 3, 4, 5,
                           Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE, -1, -2, 0};
    static short   r1[] = {0, 1, 2, 3, 4, 5, 6,
                           Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE, -2, 0};

    static final     long lF1[] = {0, 1, 2, 3, 4, 5, 6, 7,
                                   -1, 0, 1, 2, 3, 4};
    static private   long lP1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8,
                                   Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    static public    long lU1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                                   Long.MAX_VALUE, 10, 0, -10};
    static protected long lR1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                                   -1, 0, 1};
    static transient long lT1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                                   Long.MAX_VALUE, Long.MIN_VALUE};
    static volatile  long lV1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                                   Long.MAX_VALUE};
}
