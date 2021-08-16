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


package nsk.jdi.TypeComponent.isStatic;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class isstatic001a {
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
    class Class {}
    interface Inter {}

    boolean z0, z1[]={z0}, z2[][]={z1};
    byte    b0, b1[]={b0}, b2[][]={b1};
    char    c0, c1[]={c0}, c2[][]={c1};
    double  d0, d1[]={d0}, d2[][]={d1};
    float   f0, f1[]={f0}, f2[][]={f1};
    int     i0, i1[]={i0}, i2[][]={i1};
    long    l0, l1[]={l0}, l2[][]={l1};
    short   r0, r1[]={r0}, r2[][]={r1};

    static boolean z0S, z1S[]={z0S}, z2S[][]={z1S};
    static byte    b0S, b1S[]={b0S}, b2S[][]={b1S};
    static char    c0S, c1S[]={c0S}, c2S[][]={c1S};
    static double  d0S, d1S[]={d0S}, d2S[][]={d1S};
    static float   f0S, f1S[]={f0S}, f2S[][]={f1S};
    static int     i0S, i1S[]={i0S}, i2S[][]={i1S};
    static long    l0S, l1S[]={l0S}, l2S[][]={l1S};
    static short   r0S, r1S[]={r0S}, r2S[][]={r1S};

    final     long lF0 = 1l, lF1[]={lF0}, lF2[][]={lF1};
    private   long lP0,      lP1[]={lP0}, lP2[][]={lP1};
    public    long lU0,      lU1[]={lU0}, lU2[][]={lU1};
    protected long lR0,      lR1[]={lR0}, lR2[][]={lR1};
    transient long lT0,      lT1[]={lT0}, lT2[][]={lT1};
    volatile  long lV0,      lV1[]={lV0}, lV2[][]={lV1};

    static final     long lF0S = 1l, lF1S[]={lF0S}, lF2S[][]={lF1S};
    static private   long lP0S,      lP1S[]={lP0S}, lP2S[][]={lP1S};
    static public    long lU0S,      lU1S[]={lU0S}, lU2S[][]={lU1S};
    static protected long lR0S,      lR1S[]={lR0S}, lR2S[][]={lR1S};
    static transient long lT0S,      lT1S[]={lT0S}, lT2S[][]={lT1S};
    static volatile  long lV0S,      lV1S[]={lV0S}, lV2S[][]={lV1S};

    Class     X0, X1[]={X0}, X2[][]={X1};
    Object    O0, O1[]={O0}, O2[][]={O1};

    static Class     X0S, X1S[]={X0S}, X2S[][]={X1S};
    static Object    O0S, O1S[]={O0S}, O2S[][]={O1S};

    final     Long LF0 = Long.valueOf(1), LF1[]={LF0}, LF2[][]={LF1};
    private   Long LP0,               LP1[]={LP0}, LP2[][]={LP1};
    public    Long LU0,               LU1[]={LU0}, LU2[][]={LU1};
    protected Long LR0,               LR1[]={LR0}, LR2[][]={LR1};
    transient Long LT0,               LT1[]={LT0}, LT2[][]={LT1};
    volatile  Long LV0,               LV1[]={LV0}, LV2[][]={LV1};

    static final     Long LF0S = Long.valueOf(1), LF1S[]={LF0S}, LF2S[][]={LF1S};
    static private   Long LP0S,               LP1S[]={LP0S}, LP2S[][]={LP1S};
    static public    Long LU0S,               LU1S[]={LU0S}, LU2S[][]={LU1S};
    static protected Long LR0S,               LR1S[]={LR0S}, LR2S[][]={LR1S};
    static transient Long LT0S,               LT1S[]={LT0S}, LT2S[][]={LT1S};
    static volatile  Long LV0S,               LV1S[]={LV0S}, LV2S[][]={LV1S};

    Inter E0, E1[]={E0}, E2[][]={E1};
    static Inter E0S, E1S[]={E0S}, E2S[][]={E1S};

    final     Inter EF0 = null, EF1[]={EF0}, EF2[][]={EF1};
    private   Inter EP0,        EP1[]={EP0}, EP2[][]={EP1};
    public    Inter EU0,        EU1[]={EU0}, EU2[][]={EU1};
    protected Inter ER0,        ER1[]={ER0}, ER2[][]={ER1};
    transient Inter ET0,        ET1[]={ET0}, ET2[][]={ET1};
    volatile  Inter EV0,        EV1[]={EV0}, EV2[][]={EV1};

    static final     Inter EF0S = null, EF1S[]={EF0S}, EF2S[][]={EF1S};
    static private   Inter EP0S,        EP1S[]={EP0S}, EP2S[][]={EP1S};
    static public    Inter EU0S,        EU1S[]={EU0S}, EU2S[][]={EU1S};
    static protected Inter ER0S,        ER1S[]={ER0S}, ER2S[][]={ER1S};
    static transient Inter ET0S,        ET1S[]={ET0S}, ET2S[][]={ET1S};
    static volatile  Inter EV0S,        EV1S[]={EV0S}, EV2S[][]={EV1S};
}
