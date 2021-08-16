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


package nsk.jdi.Field.isTransient;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class istrans001a {
    boolean z0, z1[]={z0}, z2[][]={z1};
    byte    b0, b1[]={b0}, b2[][]={b1};
    char    c0, c1[]={c0}, c2[][]={c1};
    double  d0, d1[]={d0}, d2[][]={d1};
    float   f0, f1[]={f0}, f2[][]={f1};
    int     i0, i1[]={i0}, i2[][]={i1};
    long    l0, l1[]={l0}, l2[][]={l1};

    transient boolean z0T, z1T[]={z0T}, z2T[][]={z1T};
    transient byte    b0T, b1T[]={b0T}, b2T[][]={b1T};
    transient char    c0T, c1T[]={c0T}, c2T[][]={c1T};
    transient double  d0T, d1T[]={d0T}, d2T[][]={d1T};
    transient float   f0T, f1T[]={f0T}, f2T[][]={f1T};
    transient int     i0T, i1T[]={i0T}, i2T[][]={i1T};
    transient long    l0T, l1T[]={l0T}, l2T[][]={l1T};

    static    long lS0, lS1[]={lS0}, lS2[][]={lS1};
    private   long lP0, lP1[]={lP0}, lP2[][]={lP1};
    public    long lU0, lU1[]={lU0}, lU2[][]={lU1};
    protected long lR0, lR1[]={lR0}, lR2[][]={lR1};
    volatile  long lV0, lV1[]={lV0}, lV2[][]={lV1};
    final     long lF0 = 999, lF1[]={lF0}, lF2[][]={lF1};

    transient static    long lS0T, lS1T[]={lS0T}, lS2T[][]={lS1T};
    transient private   long lP0T, lP1T[]={lP0T}, lP2T[][]={lP1T};
    transient public    long lU0T, lU1T[]={lU0T}, lU2T[][]={lU1T};
    transient protected long lR0T, lR1T[]={lR0T}, lR2T[][]={lR1T};
    transient volatile  long lV0T, lV1T[]={lV0T}, lV2T[][]={lV1T};
    transient final     long lF0T = 999, lF1T[]={lF0T}, lF2T[][]={lF1T};

    class Class {}
    Class X0 = new Class(), X1[]={X0}, X2[][]={X1};
    Boolean   Z0, Z1[]={Z0}, Z2[][]={Z1};
    Byte      B0, B1[]={B0}, B2[][]={B1};
    Character C0, C1[]={C0}, C2[][]={C1};
    Double    D0, D1[]={D0}, D2[][]={D1};
    Float     F0, F1[]={F0}, F2[][]={F1};
    Integer   I0, I1[]={I0}, I2[][]={I1};
    Long      L0, L1[]={L0}, L2[][]={L1};
    String    S0, S1[]={S0}, S2[][]={S1};
    Object    O0, O1[]={O0}, O2[][]={O1};

    transient Class     X0T, X1T[]={X0T}, X2T[][]={X1T};
    transient Boolean   Z0T, Z1T[]={Z0T}, Z2T[][]={Z1T};
    transient Byte      B0T, B1T[]={B0T}, B2T[][]={B1T};
    transient Character C0T, C1T[]={C0T}, C2T[][]={C1T};
    transient Double    D0T, D1T[]={D0T}, D2T[][]={D1T};
    transient Float     F0T, F1T[]={F0T}, F2T[][]={F1T};
    transient Integer   I0T, I1T[]={I0T}, I2T[][]={I1T};
    transient Long      L0T, L1T[]={L0T}, L2T[][]={L1T};
    transient String    S0T, S1T[]={S0T}, S2T[][]={S1T};
    transient Object    O0T, O1T[]={O0T}, O2T[][]={O1T};

    static    Long LS0, LS1[]={LS0}, LS2[][]={LS1};
    private   Long LP0, LP1[]={LP0}, LP2[][]={LP1};
    public    Long LU0, LU1[]={LU0}, LU2[][]={LU1};
    protected Long LR0, LR1[]={LR0}, LR2[][]={LR1};
    volatile  Long LV0, LV1[]={LV0}, LV2[][]={LV1};
    final     Long LF0 = Long.valueOf(999), LF1[]={LF0}, LF2[][]={LF1};

    transient static    Long LS0T, LS1T[]={LS0T}, LS2T[][]={LS1T};
    transient private   Long LP0T, LP1T[]={LP0T}, LP2T[][]={LP1T};
    transient public    Long LU0T, LU1T[]={LU0T}, LU2T[][]={LU1T};
    transient protected Long LR0T, LR1T[]={LR0T}, LR2T[][]={LR1T};
    transient volatile  Long LV0T, LV1T[]={LV0T}, LV2T[][]={LV1T};
    transient final     Long LF0T = Long.valueOf(999), LF1T[]={LF0T},
                             LF2T[][]={LF1T};

    interface Inter {}
    Inter E0, E1[]={E0}, E2[][]={E1};
    transient Inter E0T, E1T[]={E0T}, E2T[][]={E1T};

    static    Inter ES0, ES1[]={ES0}, ES2[][]={ES1};
    private   Inter EP0, EP1[]={EP0}, EP2[][]={EP1};
    public    Inter EU0, EU1[]={EU0}, EU2[][]={EU1};
    protected Inter ER0, ER1[]={ER0}, ER2[][]={ER1};
    volatile  Inter EV0, EV1[]={EV0}, EV2[][]={EV1};
    final     Inter EF0 = null, EF1[]={EF0}, EF2[][]={EF1};

    transient static    Inter ES0T, ES1T[]={ES0T}, ES2T[][]={ES1T};
    transient private   Inter EP0T, EP1T[]={EP0T}, EP2T[][]={EP1T};
    transient public    Inter EU0T, EU1T[]={EU0T}, EU2T[][]={EU1T};
    transient protected Inter ER0T, ER1T[]={ER0T}, ER2T[][]={ER1T};
    transient volatile  Inter EV0T, EV1T[]={EV0T}, EV2T[][]={EV1T};
    transient final     Inter EF0T = null, EF1T[]={EF0T}, EF2T[][]={EF1T};

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
