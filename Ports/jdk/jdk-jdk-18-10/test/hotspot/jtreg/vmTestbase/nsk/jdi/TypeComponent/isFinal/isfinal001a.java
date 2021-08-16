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


package nsk.jdi.TypeComponent.isFinal;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class isfinal001a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        isfinal001aClassToCheck classToCheck = new isfinal001aClassToCheck();

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

class isfinal001aClassToCheck {
    // User class and interface
    class isfinal001aClass {}
    interface isfinal001aInter {}

    boolean z0, z1[]={z0}, z2[][]={z1};
    byte    b0, b1[]={b0}, b2[][]={b1};
    char    c0, c1[]={c0}, c2[][]={c1};
    double  d0, d1[]={d0}, d2[][]={d1};
    float   f0, f1[]={f0}, f2[][]={f1};
    int     i0, i1[]={i0}, i2[][]={i1};
    long    l0, l1[]={l0}, l2[][]={l1};
    short   r0, r1[]={r0}, r2[][]={r1};

    final boolean z0F = true,     z1F[]={z0F}, z2F[][]={z1F};
    final byte    b0F = 1,        b1F[]={b0F}, b2F[][]={b1F};
    final char    c0F = '\u00ff', c1F[]={c0F}, c2F[][]={c1F};
    final double  d0F = 1,        d1F[]={d0F}, d2F[][]={d1F};
    final float   f0F = 1f,       f1F[]={f0F}, f2F[][]={f1F};
    final int     i0F = 1,        i1F[]={i0F}, i2F[][]={i1F};
    final long    l0F = 1l,       l1F[]={l0F}, l2F[][]={l1F};
    final short   r0F = 1,        r1F[]={r0F}, r2F[][]={r1F};

    static    long lS0, lS1[]={lS0}, lS2[][]={lS1};
    private   long lP0, lP1[]={lP0}, lP2[][]={lP1};
    public    long lU0, lU1[]={lU0}, lU2[][]={lU1};
    protected long lR0, lR1[]={lR0}, lR2[][]={lR1};
    transient long lT0, lT1[]={lT0}, lT2[][]={lT1};
    volatile  long lV0, lV1[]={lV0}, lV2[][]={lV1};

    final static    long lS0F = 1l, lS1F[]={lS0F}, lS2F[][]={lS1F};
    final private   long lP0F = 1l, lP1F[]={lP0F}, lP2F[][]={lP1F};
    final public    long lU0F = 1l, lU1F[]={lU0F}, lU2F[][]={lU1F};
    final protected long lR0F = 1l, lR1F[]={lR0F}, lR2F[][]={lR1F};
    final transient long lT0F = 1l, lT1F[]={lT0F}, lT2F[][]={lT1F};

    isfinal001aClass     X0, X1[]={X0}, X2[][]={X1};
    Object    O0, O1[]={O0}, O2[][]={O1};

    final isfinal001aClass     X0F = new isfinal001aClass(), X1F[]={X0F}, X2F[][]={X1F};
    final Object    O0F = new Object(), O1F[]={O0F}, O2F[][]={O1F};

    static    Long LS0, LS1[]={LS0}, LS2[][]={LS1};
    private   Long LP0, LP1[]={LP0}, LP2[][]={LP1};
    public    Long LU0, LU1[]={LU0}, LU2[][]={LU1};
    protected Long LR0, LR1[]={LR0}, LR2[][]={LR1};
    transient Long LT0, LT1[]={LT0}, LT2[][]={LT1};
    volatile  Long LV0, LV1[]={LV0}, LV2[][]={LV1};

    final static    Long LS0F = Long.valueOf(1), LS1F[]={LS0F}, LS2F[][]={LS1F};
    final private   Long LP0F = Long.valueOf(1), LP1F[]={LP0F}, LP2F[][]={LP1F};
    final public    Long LU0F = Long.valueOf(1), LU1F[]={LU0F}, LU2F[][]={LU1F};
    final protected Long LR0F = Long.valueOf(1), LR1F[]={LR0F}, LR2F[][]={LR1F};
    final transient Long LT0F = Long.valueOf(1), LT1F[]={LT0F}, LT2F[][]={LT1F};

    isfinal001aInter E0, E1[]={E0}, E2[][]={E1};
    final isfinal001aInter E0F = null, E1F[]={E0F}, E2F[][]={E1F};

    static    isfinal001aInter ES0, ES1[]={ES0}, ES2[][]={ES1};
    private   isfinal001aInter EP0, EP1[]={EP0}, EP2[][]={EP1};
    public    isfinal001aInter EU0, EU1[]={EU0}, EU2[][]={EU1};
    protected isfinal001aInter ER0, ER1[]={ER0}, ER2[][]={ER1};
    transient isfinal001aInter ET0, ET1[]={ET0}, ET2[][]={ET1};
    volatile  isfinal001aInter EV0, EV1[]={EV0}, EV2[][]={EV1};

    final static    isfinal001aInter ES0F = null, ES1F[]={ES0F}, ES2F[][]={ES1F};
    final private   isfinal001aInter EP0F = null, EP1F[]={EP0F}, EP2F[][]={EP1F};
    final public    isfinal001aInter EU0F = null, EU1F[]={EU0F}, EU2F[][]={EU1F};
    final protected isfinal001aInter ER0F = null, ER1F[]={ER0F}, ER2F[][]={ER1F};
    final transient isfinal001aInter ET0F = null, ET1F[]={ET0F}, ET2F[][]={ET1F};
}
