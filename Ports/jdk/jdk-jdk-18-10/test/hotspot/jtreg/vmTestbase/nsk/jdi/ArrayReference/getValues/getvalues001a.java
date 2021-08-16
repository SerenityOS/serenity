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


package nsk.jdi.ArrayReference.getValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class getvalues001a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        getvalues001aClassToCheck classToCheck = new getvalues001aClassToCheck();

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

class getvalues001aClassToCheck {
    static boolean z1[] = {true, false};
    static byte    b1[] = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};
    static char    c1[] = {Character.MIN_VALUE, '\u00ff', '\uff00',
                           Character.MAX_VALUE};
    static double  d1[] = {Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -1, -0,
                           0, 1, Double.MAX_VALUE, Double.POSITIVE_INFINITY,
                           Double.NaN};
    static float   f1[] = {Float.NEGATIVE_INFINITY, Float.MIN_VALUE, -1, -0,
                           0, 1, Float.MAX_VALUE, Float.POSITIVE_INFINITY,
                           Float.NaN};
    static int     i1[] = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
    static long    l1[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    static short   r1[] = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};

    static final     long lF1[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    static private   long lP1[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    static public    long lU1[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    static protected long lR1[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    static transient long lT1[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    static volatile  long lV1[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
}
