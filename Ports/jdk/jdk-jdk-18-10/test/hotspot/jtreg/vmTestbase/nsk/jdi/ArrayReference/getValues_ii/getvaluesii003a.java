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


public class getvaluesii003a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        getvaluesii003aClassToCheck classToCheck = new getvaluesii003aClassToCheck();

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

class getvaluesii003aClassToCheck {
    static boolean z1[] = {true, false, false, true, true};
    static byte    b1[] = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};
    static char    c1[] = {Character.MIN_VALUE, '\u00ff', '\uff00',
                           '\u1234', '\u4321', Character.MAX_VALUE};
    static double  d1[] = {Double.NEGATIVE_INFINITY};
    static float   f1[] = {Float.POSITIVE_INFINITY};
    static int     i1[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
    static long    l1[] = {Long.MIN_VALUE, Long.MAX_VALUE};
    static short   r1[] = {-2, -1, 0, 1, 2};

    static final     long lF1[] = {Long.MAX_VALUE};
    static private   long lP1[] = {0};
    static public    long lU1[] = {1, 2};
    static protected long lR1[] = {1, 2, 3};
    static transient long lT1[] = {2, 3, 4, 5};
    static volatile  long lV1[] = {2, 3, 4, 5, 6};
}
