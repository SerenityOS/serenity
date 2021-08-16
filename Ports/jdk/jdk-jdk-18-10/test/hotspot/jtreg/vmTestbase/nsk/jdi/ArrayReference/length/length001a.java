/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.ArrayReference.length;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class length001a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        ClassToCheck classToCheck = new ClassToCheck();

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

class ClassToCheck {
    // User class and interface
    static class Class {}
    static interface Inter {}

    static boolean z1[]={};
    static boolean z2[][]={z1, z1, z1, z1, z1, z1, z1};
    static byte    b1[]={0};
    static byte    b2[][]={b1, b1, b1, b1, b1, b1};
    static char    c1[]={'\u00ff', '\u0f0f'};
    static char    c2[][]={c1, c1, c1, c1, c1};
    static double  d1[]={0, 1, -2};
    static double  d2[][]={d1, d1, d1, d1};
    static float   f1[]={0, 1, -2, 3};
    static float   f2[][]={f1, f1, f1};
    static int     i1[]={0, 1, -2, 3, -4};
    static int     i2[][]={i1, i1};
    static long    l1[]={0, 1, -2, 3, -4, 5};
    static long    l2[][]={l1};
    static short   r1[]={0, 1, -2, 3, -4, 5, -6};
    static short   r2[][]={};

    static final     long lF1[]={1, -2, 3};
    static private   long lP1[][]={{1}, {-2}, {3}};
    static public    long lU1[][][]={{{1}}, {{-2}}};
    static protected long lR1[][][][]={{{{1}}}, {{{-2}}}};
    static transient long lT1[][][][][]={{{{{1}}}}};
    static volatile  long lV1[][][][][][]={{{{{{1}}}}}};

    static Inter  E1[]={};
    static Inter  E2[][]={E1, E1};
    static Class  X1[]={new Class()};
    static Class  X2[][]={X1};
    static Object O1[]={new Object(), new Object()};
    static Object O2[][]={};

    static final     Long LF1[]={Long.valueOf(1), Long.valueOf(-2), Long.valueOf(3)};
    static private   Long LP1[][]={{Long.valueOf(1)}, {Long.valueOf(2)}, {Long.valueOf(3)}};
    static public    Long LU1[][][]={{{Long.valueOf(1)}}, {{Long.valueOf(-2)}}};
    static protected Long LR1[][][][]={{{{Long.valueOf(1)}}}, {{{Long.valueOf(-2)}}}};
    static transient Long LT1[][][][][]={{{{{Long.valueOf(1)}}}}};
    static volatile  Long LV1[][][][][][]={{{{{{Long.valueOf(1)}}}}}};

    static final     Inter EF1[]={};
    static private   Inter EP1[][]={{}};
    static public    Inter EU1[][][]={{{}}};
    static protected Inter ER1[][][][]={{{{}}}};
    static transient Inter ET1[][][][][]={{{{{}}}}};
    static volatile  Inter EV1[][][][][][]={{{{{{}}}}}};
}
