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


package nsk.jdi.TypeComponent.declaringType;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class decltype003a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        decltype003aOverridenClass overridenClass = new decltype003aOverridenClass();
        decltype003aImplClass implClass = new decltype003aImplClass();

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

// User class and interface
class decltype003aClass {}
interface decltype003aInter {}

class decltype003aOverridenClass extends decltype003aMainClass {
    // All fields are overriden from MainClass

    boolean z0, z1[]={z0}, z2[][]={z1};
    byte    b0, b1[]={b0}, b2[][]={b1};
    char    c0, c1[]={c0}, c2[][]={c1};
    double  d0, d1[]={d0}, d2[][]={d1};
    float   f0, f1[]={f0}, f2[][]={f1};
    int     i0, i1[]={i0}, i2[][]={i1};
    long    l0, l1[]={l0}, l2[][]={l1};
    short   r0, r1[]={r0}, r2[][]={r1};

    final     long lF0 = 1l, lF1[]={lF0}, lF2[][]={lF1};
    private   long lP0,      lP1[]={lP0}, lP2[][]={lP1};
    public    long lU0,      lU1[]={lU0}, lU2[][]={lU1};
    protected long lR0,      lR1[]={lR0}, lR2[][]={lR1};
    static    long l0S,      l1S[]={l0S}, l2S[][]={l1S};
    transient long lT0,      lT1[]={lT0}, lT2[][]={lT1};
    volatile  long lV0,      lV1[]={lV0}, lV2[][]={lV1};

    decltype003aClass     X0, X1[]={X0}, X2[][]={X1};
    Object    O0, O1[]={O0}, O2[][]={O1};

    final     Long LF0 = Long.valueOf(1), LF1[]={LF0}, LF2[][]={LF1};
    private   Long LP0,               LP1[]={LP0}, LP2[][]={LP1};
    public    Long LU0,               LU1[]={LU0}, LU2[][]={LU1};
    protected Long LR0,               LR1[]={LR0}, LR2[][]={LR1};
    static    Long L0S,               L1S[]={L0S}, L2S[][]={L1S};
    transient Long LT0,               LT1[]={LT0}, LT2[][]={LT1};
    volatile  Long LV0,               LV1[]={LV0}, LV2[][]={LV1};

    decltype003aInter     E0, E1[]={E0}, E2[][]={E1};

    final     decltype003aInter EF0 = null, EF1[]={EF0}, EF2[][]={EF1};
    private   decltype003aInter EP0,        EP1[]={EP0}, EP2[][]={EP1};
    public    decltype003aInter EU0,        EU1[]={EU0}, EU2[][]={EU1};
    protected decltype003aInter ER0,        ER1[]={ER0}, ER2[][]={ER1};
    static    decltype003aInter E0S,        E1S[]={E0S}, E2S[][]={E1S};
    transient decltype003aInter ET0,        ET1[]={ET0}, ET2[][]={ET1};
    volatile  decltype003aInter EV0,        EV1[]={EV0}, EV2[][]={EV1};
}

class decltype003aMainClass {
    boolean z0, z1[]={z0}, z2[][]={z1};
    byte    b0, b1[]={b0}, b2[][]={b1};
    char    c0, c1[]={c0}, c2[][]={c1};
    double  d0, d1[]={d0}, d2[][]={d1};
    float   f0, f1[]={f0}, f2[][]={f1};
    int     i0, i1[]={i0}, i2[][]={i1};
    long    l0, l1[]={l0}, l2[][]={l1};
    short   r0, r1[]={r0}, r2[][]={r1};

    final     long lF0 = 1l, lF1[]={lF0}, lF2[][]={lF1};
    private   long lP0,      lP1[]={lP0}, lP2[][]={lP1};
    public    long lU0,      lU1[]={lU0}, lU2[][]={lU1};
    protected long lR0,      lR1[]={lR0}, lR2[][]={lR1};
    static    long l0S,      l1S[]={l0S}, l2S[][]={l1S};
    transient long lT0,      lT1[]={lT0}, lT2[][]={lT1};
    volatile  long lV0,      lV1[]={lV0}, lV2[][]={lV1};

    decltype003aClass     X0, X1[]={X0}, X2[][]={X1};
    Object    O0, O1[]={O0}, O2[][]={O1};

    final     Long LF0 = Long.valueOf(1), LF1[]={LF0}, LF2[][]={LF1};
    private   Long LP0,               LP1[]={LP0}, LP2[][]={LP1};
    public    Long LU0,               LU1[]={LU0}, LU2[][]={LU1};
    protected Long LR0,               LR1[]={LR0}, LR2[][]={LR1};
    static    Long L0S,               L1S[]={L0S}, L2S[][]={L1S};
    transient Long LT0,               LT1[]={LT0}, LT2[][]={LT1};
    volatile  Long LV0,               LV1[]={LV0}, LV2[][]={LV1};

    decltype003aInter     E0, E1[]={E0}, E2[][]={E1};

    final     decltype003aInter EF0 = null, EF1[]={EF0}, EF2[][]={EF1};
    private   decltype003aInter EP0,        EP1[]={EP0}, EP2[][]={EP1};
    public    decltype003aInter EU0,        EU1[]={EU0}, EU2[][]={EU1};
    protected decltype003aInter ER0,        ER1[]={ER0}, ER2[][]={ER1};
    static    decltype003aInter E0S,        E1S[]={E0S}, E2S[][]={E1S};
    transient decltype003aInter ET0,        ET1[]={ET0}, ET2[][]={ET1};
    volatile  decltype003aInter EV0,        EV1[]={EV0}, EV2[][]={EV1};
}

class decltype003aImplClass implements decltype003aOverridenInter {}

interface decltype003aOverridenInter extends decltype003aMainInter {
    // All fields are overriden from decltype003aMainInter

    boolean z0 = true,     z1[]={z0}, z2[][]={z1};
    byte    b0 = 1,        b1[]={b0}, b2[][]={b1};
    char    c0 = '\u00ff', c1[]={c0}, c2[][]={c1};
    double  d0 = 0,        d1[]={d0}, d2[][]={d1};
    float   f0 = 0f,       f1[]={f0}, f2[][]={f1};
    int     i0 = 0,        i1[]={i0}, i2[][]={i1};
    long    l0 = 0l,       l1[]={l0}, l2[][]={l1};
    short   r0 = 0,        r1[]={r0}, r2[][]={r1};

    final     long lF0 = 1l, lF1[]={lF0}, lF2[][]={lF1};
    public    long lU0 = 1l, lU1[]={lU0}, lU2[][]={lU1};
    static    long l0S = 1l, l1S[]={l0S}, l2S[][]={l1S};

    decltype003aClass     X0 = new decltype003aClass(),  X1[]={X0}, X2[][]={X1};
    Object    O0 = new Object(), O1[]={O0}, O2[][]={O1};

    final     Long LF0 = Long.valueOf(1), LF1[]={LF0}, LF2[][]={LF1};
    public    Long LU0 = Long.valueOf(1), LU1[]={LU0}, LU2[][]={LU1};
    static    Long L0S = Long.valueOf(1), L1S[]={L0S}, L2S[][]={L1S};

    decltype003aInter     E0 = null, E1[]={E0}, E2[][]={E1};

    final     decltype003aInter EF0 = null, EF1[]={EF0}, EF2[][]={EF1};
    public    decltype003aInter EU0 = null, EU1[]={EU0}, EU2[][]={EU1};
    static    decltype003aInter E0S = null, E1S[]={E0S}, E2S[][]={E1S};
}

interface decltype003aMainInter {
    boolean z0 = true,     z1[]={z0}, z2[][]={z1};
    byte    b0 = 1,        b1[]={b0}, b2[][]={b1};
    char    c0 = '\u00ff', c1[]={c0}, c2[][]={c1};
    double  d0 = 0,        d1[]={d0}, d2[][]={d1};
    float   f0 = 0f,       f1[]={f0}, f2[][]={f1};
    int     i0 = 0,        i1[]={i0}, i2[][]={i1};
    long    l0 = 0l,       l1[]={l0}, l2[][]={l1};
    short   r0 = 0,        r1[]={r0}, r2[][]={r1};

    final     long lF0 = 1l, lF1[]={lF0}, lF2[][]={lF1};
    public    long lU0 = 1l, lU1[]={lU0}, lU2[][]={lU1};
    static    long l0S = 1l, l1S[]={l0S}, l2S[][]={l1S};

    decltype003aClass     X0 = new decltype003aClass(),  X1[]={X0}, X2[][]={X1};
    Object    O0 = new Object(), O1[]={O0}, O2[][]={O1};

    final     Long LF0 = Long.valueOf(1), LF1[]={LF0}, LF2[][]={LF1};
    public    Long LU0 = Long.valueOf(1), LU1[]={LU0}, LU2[][]={LU1};
    static    Long L0S = Long.valueOf(1), L1S[]={L0S}, L2S[][]={L1S};

    decltype003aInter     E0 = null, E1[]={E0}, E2[][]={E1};

    final     decltype003aInter EF0 = null, EF1[]={EF0}, EF2[][]={EF1};
    public    decltype003aInter EU0 = null, EU1[]={EU0}, EU2[][]={EU1};
    static    decltype003aInter E0S = null, E1S[]={E0S}, E2S[][]={E1S};
}
