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


package nsk.jdi.ArrayReference.setValues_ilii;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class setvaluesilii005a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        setvaluesilii005aClassToCheck classToCheck = new setvaluesilii005aClassToCheck();

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

class setvaluesilii005aClassToCheck {

    // For each primitive type a sample array has the same length as
    // correspondent tested array
    static boolean z1[]  = {false};
    static boolean z1S[] = {true};

    static byte    b1[]  = {1, 2};
    static byte    b1S[] = {Byte.MIN_VALUE, Byte.MAX_VALUE};

    static char    c1[]  = {1, 2, 3};
    static char    c1S[] = {Character.MIN_VALUE, '\u00ff', Character.MAX_VALUE};

    static double  d1[]  = {1, 2, 3, 4};
    static double  d1S[] = {Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -1, -0};

    static float   f1[]  = {1, 2, 3, 4, 5};
    static float   f1S[] = {Float.NEGATIVE_INFINITY, Float.MIN_VALUE, -1, -0,
                            Float.NaN};

    static int     i1[]  = {1, 2, 3, 4, 5, 6};
    static int     i1S[] = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, -1};

    static long    l1[]  = {1, 2, 3, 4, 5, 6, 7};
    static long    l1S[]  = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE, 0, 1};

    static short   r1[]  = {1, 2, 3, 4, 5, 6, 7, 8};
    static short   r1S[] = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE,
                            Short.MIN_VALUE, -1, 0};

    static final     long lF1[]  = {-2, -1, 0, 1, Long.MAX_VALUE, 0, 1};
    static private   long lP1[]  = {-2, -1, 0, 1, Long.MAX_VALUE, 0, 1};
    static public    long lU1[]  = {-2, -1, 0, 1, Long.MAX_VALUE, 0, 1};
    static protected long lR1[]  = {-2, -1, 0, 1, Long.MAX_VALUE, 0, 1};
    static transient long lT1[]  = {-2, -1, 0, 1, Long.MAX_VALUE, 0, 1};
    static volatile  long lV1[]  = {-2, -1, 0, 1, Long.MAX_VALUE, 0, 1};
}
