/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.Field.isVolatile;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class isvol001a {
    boolean z0, z1[]={z0}, z2[][]={z1};
    byte    b0, b1[]={b0}, b2[][]={b1};
    char    c0, c1[]={c0}, c2[][]={c1};
    double  d0, d1[]={d0}, d2[][]={d1};
    float   f0, f1[]={f0}, f2[][]={f1};
    int     i0, i1[]={i0}, i2[][]={i1};
    long    l0, l1[]={l0}, l2[][]={l1};

    volatile boolean z0V, z1V[]={z0V}, z2V[][]={z1V};
    volatile byte    b0V, b1V[]={b0V}, b2V[][]={b1V};
    volatile char    c0V, c1V[]={c0V}, c2V[][]={c1V};
    volatile double  d0V, d1V[]={d0V}, d2V[][]={d1V};
    volatile float   f0V, f1V[]={f0V}, f2V[][]={f1V};
    volatile int     i0V, i1V[]={i0V}, i2V[][]={i1V};
    volatile long    l0V, l1V[]={l0V}, l2V[][]={l1V};

    static    long lS0, lS1[]={lS0}, lS2[][]={lS1};
    private   long lP0, lP1[]={lP0}, lP2[][]={lP1};
    public    long lU0, lU1[]={lU0}, lU2[][]={lU1};
    protected long lR0, lR1[]={lR0}, lR2[][]={lR1};
    transient long lT0, lT1[]={lT0}, lT2[][]={lT1};
    final     long lF0 = 999, lF1[]={lF0}, lF2[][]={lF1};

    volatile static    long lS0V, lS1V[]={lS0V}, lS2V[][]={lS1V};
    volatile private   long lP0V, lP1V[]={lP0V}, lP2V[][]={lP1V};
    volatile public    long lU0V, lU1V[]={lU0V}, lU2V[][]={lU1V};
    volatile protected long lR0V, lR1V[]={lR0V}, lR2V[][]={lR1V};
    volatile transient long lT0V, lT1V[]={lT0V}, lT2V[][]={lT1V};

    class Class {}
    Class     X0, X1[]={X0}, X2[][]={X1};
    Boolean   Z0, Z1[]={Z0}, Z2[][]={Z1};
    Byte      B0, B1[]={B0}, B2[][]={B1};
    Character C0, C1[]={C0}, C2[][]={C1};
    Double    D0, D1[]={D0}, D2[][]={D1};
    Float     F0, F1[]={F0}, F2[][]={F1};
    Integer   I0, I1[]={I0}, I2[][]={I1};
    Long      L0, L1[]={L0}, L2[][]={L1};
    String    S0, S1[]={S0}, S2[][]={S1};
    Object    O0, O1[]={O0}, O2[][]={O1};

    volatile Class     X0V, X1V[]={X0V}, X2V[][]={X1V};
    volatile Boolean   Z0V, Z1V[]={Z0V}, Z2V[][]={Z1V};
    volatile Byte      B0V, B1V[]={B0V}, B2V[][]={B1V};
    volatile Character C0V, C1V[]={C0V}, C2V[][]={C1V};
    volatile Double    D0V, D1V[]={D0V}, D2V[][]={D1V};
    volatile Float     F0V, F1V[]={F0V}, F2V[][]={F1V};
    volatile Integer   I0V, I1V[]={I0V}, I2V[][]={I1V};
    volatile Long      L0V, L1V[]={L0V}, L2V[][]={L1V};
    volatile String    S0V, S1V[]={S0V}, S2V[][]={S1V};
    volatile Object    O0V, O1V[]={O0V}, O2V[][]={O1V};

    static    Long LS0, LS1[]={LS0}, LS2[][]={LS1};
    private   Long LP0, LP1[]={LP0}, LP2[][]={LP1};
    public    Long LU0, LU1[]={LU0}, LU2[][]={LU1};
    protected Long LR0, LR1[]={LR0}, LR2[][]={LR1};
    transient Long LT0, LT1[]={LT0}, LT2[][]={LT1};
    final     Long LF0 = Long.valueOf(999), LF1[]={LF0}, LF2[][]={LF1};

    volatile static    Long LS0V, LS1V[]={LS0V}, LS2V[][]={LS1V};
    volatile private   Long LP0V, LP1V[]={LP0V}, LP2V[][]={LP1V};
    volatile public    Long LU0V, LU1V[]={LU0V}, LU2V[][]={LU1V};
    volatile protected Long LR0V, LR1V[]={LR0V}, LR2V[][]={LR1V};
    volatile transient Long LT0V, LT1V[]={LT0V}, LT2V[][]={LT1V};

    interface Inter {}
    Inter E0, E1[]={E0}, E2[][]={E1};

    volatile Inter E0V, E1V[]={E0V}, E2V[][]={E1V};

    static    Inter ES0, ES1[]={ES0}, ES2[][]={ES1};
    private   Inter EP0, EP1[]={EP0}, EP2[][]={EP1};
    public    Inter EU0, EU1[]={EU0}, EU2[][]={EU1};
    protected Inter ER0, ER1[]={ER0}, ER2[][]={ER1};
    transient Inter ET0, ET1[]={ET0}, ET2[][]={ET1};
    final     Inter EF0 = null, EF1[]={EF0}, EF2[][]={EF1};

    volatile static    Inter ES0V, ES1V[]={ES0V}, ES2V[][]={ES1V};
    volatile private   Inter EP0V, EP1V[]={EP0V}, EP2V[][]={EP1V};
    volatile public    Inter EU0V, EU1V[]={EU0V}, EU2V[][]={EU1V};
    volatile protected Inter ER0V, ER1V[]={ER0V}, ER2V[][]={ER1V};
    volatile transient Inter ET0V, ET1V[]={ET0V}, ET2V[][]={ET1V};

    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);

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
