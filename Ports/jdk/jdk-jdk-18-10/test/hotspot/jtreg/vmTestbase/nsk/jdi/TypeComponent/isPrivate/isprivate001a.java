/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.TypeComponent.isPrivate;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class isprivate001a {

    boolean z0, z1[] = {z0}, z2[][] = {z1};
    byte    b0, b1[] = {b0}, b2[][] = {b1};
    char    c0, c1[] = {c0}, c2[][] = {c1};
    double  d0, d1[] = {d0}, d2[][] = {d1};
    float   f0, f1[] = {f0}, f2[][] = {f1};
    int     i0, i1[] = {i0}, i2[][] = {i1};
    long    l0, l1[] = {l0}, l2[][] = {l1};
    short   r0, r1[] = {r0}, r2[][] = {r1};

    private boolean zP0, zP1[] = {zP0}, zP2[][] = {zP1};
    private byte    bP0, bP1[] = {bP0}, bP2[][] = {bP1};
    private char    cP0, cP1[] = {cP0}, cP2[][] = {cP1};
    private double  dP0, dP1[] = {dP0}, dP2[][] = {dP1};
    private float   fP0, fP1[] = {fP0}, fP2[][] = {fP1};
    private int     iP0, iP1[] = {iP0}, iP2[][] = {iP1};
    private long    lP0, lP1[] = {lP0}, lP2[][] = {lP1};
    private short   rP0, rP1[] = {rP0}, rP2[][] = {rP1};

    Boolean   Z0, Z1[] = {Z0}, Z2[][] = {Z1};
    Byte      B0, B1[] = {B0}, B2[][] = {B1};
    Character C0, C1[] = {C0}, C2[][] = {C1};
    Double    D0, D1[] = {D0}, D2[][] = {D1};
    Float     F0, F1[] = {F0}, F2[][] = {F1};
    Integer   I0, I1[] = {I0}, I2[][] = {I1};
    Long      L0, L1[] = {L0}, L2[][] = {L1};
    Short     R0, R1[] = {R0}, R2[][] = {R1};

    private Boolean   ZP0, ZP1[] = {ZP0}, ZP2[][] = {ZP1};
    private Byte      BP0, BP1[] = {BP0}, BP2[][] = {BP1};
    private Character CP0, CP1[] = {CP0}, CP2[][] = {CP1};
    private Double    DP0, DP1[] = {DP0}, DP2[][] = {DP1};
    private Float     FP0, FP1[] = {FP0}, FP2[][] = {FP1};
    private Integer   IP0, IP1[] = {IP0}, IP2[][] = {IP1};
    private Long      LP0, LP1[] = {LP0}, LP2[][] = {LP1};
    private Short     RP0, RP1[] = {RP0}, RP2[][] = {RP1};

    String    s0, s1[] = {s0}, s2[][] = {s1};
    Object    o0, o1[] = {o0}, o2[][] = {o1};
    private String S0, S1[] = {S0}, S2[][] = {S1};
    private Object O0, O1[] = {O0}, O2[][] = {O1};

    protected P u0, u1[] = {u0}, u2[][] = {u1};
    private   P v0, v1[] = {v0}, v2[][] = {v1};
    public    P w0, w1[] = {w0}, w2[][] = {w1};
    P p0, p1[] = {p0}, p2[][] = {p1};  // package private

    protected  M h0, h1[] = {h0}, h2[][] = {h1};
    private    M j0, j1[] = {j0}, j2[][] = {j1};
    public     M k0, k1[] = {k0}, k2[][] = {k1};
    M m0, m1[] = {m0}, m2[][] = {m1}; // package private

    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = argHandler.createDebugeeLog();
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);

        isprivate001a isprivate001a_ = new isprivate001a();

        log.display(" debuggee started.");
        pipe.println("ready");

        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            log.display("debuggee > \"quit\" signal recieved.");
            log.display("debuggee > completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        }
        log.complain("debuggee > unexpected signal (not \"quit\") - " + instruction);
        log.complain("debuggee > TEST FAILED");
        System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
    }
}

class P {}
interface M {}
