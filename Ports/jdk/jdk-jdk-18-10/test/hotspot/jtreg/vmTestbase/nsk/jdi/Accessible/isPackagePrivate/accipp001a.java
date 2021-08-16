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

package nsk.jdi.Accessible.isPackagePrivate;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the JDI test.
 */
public class accipp001a {
    boolean z0, z1[]={z0}, z2[][]={z1}, z3[][][]={z2}, z4[][][][]={z3};
    byte    b0, b1[]={b0}, b2[][]={b1}, b3[][][]={b2}, b4[][][][]={b3};
    char    c0, c1[]={c0}, c2[][]={c1}, c3[][][]={c2}, c4[][][][]={c3};
    double  d0, d1[]={d0}, d2[][]={d1}, d3[][][]={d2}, d4[][][][]={d3};
    float   f0, f1[]={f0}, f2[][]={f1}, f3[][][]={f2}, f4[][][][]={f3};
    int     i0, i1[]={i0}, i2[][]={i1}, i3[][][]={i2}, i4[][][][]={i3};
    long    l0, l1[]={l0}, l2[][]={l1}, l3[][][]={l2}, l4[][][][]={l3};
    short   s0, s1[]={s0}, s2[][]={s1}, s3[][][]={s2}, s4[][][][]={s3};

    Boolean   Z0  = Boolean.valueOf(true),  Z1[] ={Z0},  Z2[][]={Z1},   Z3[][][]={Z2},   Z4[][][][]={Z3};
    Byte      B0  = Byte.valueOf("0"),      B1[] ={B0},  B2[][]={B1},   B3[][][]={B2},   B4[][][][]={B3};
    Character C0  = Character.valueOf('0'), C1[] ={C0},  C2[][]={C1},   C3[][][]={C2},   C4[][][][]={C3};
    Double    D0  = Double.valueOf(0),      D1[] ={D0},  D2[][]={D1},   D3[][][]={D2},   D4[][][][]={D3};
    Float     F0  = Float.valueOf(0),       F1[] ={F0},  F2[][]={F1},   F3[][][]={F2},   F4[][][][]={F3};
    Integer   I0  = Integer.valueOf(0),     I1[] ={I0},  I2[][]={I1},   I3[][][]={I2},   I4[][][][]={I3};
    Long      L0  = Long.valueOf(0),        L1[] ={L0},  L2[][]={L1},   L3[][][]={L2},   L4[][][][]={L3};
    Short     Sh0 = Short.valueOf("1"),     Sh1[]={Sh0}, Sh2[][]={Sh1}, Sh3[][][]={Sh2}, Sh4[][][][]={Sh3};
    String    S0  = new String(" "),        S1[] ={S0},  S2[][]={S1},   S3[][][]={S2},   S4[][][][]={S3};
    Object    O0  = new Object(),           O1[] ={O0},  O2[][]={O1},   O3[][][]={O2},   O4[][][][]={O3};

    private   static class  U {} // private ==> package private
    protected static class  V {}
    public    static class  W {}
              static class  P {} // package private

    U u0=new U(), u1[]={u0}, u2[][]={u1}, u3[][][]={u2}, u4[][][][]={u3};
    V v0=new V(), v1[]={v0}, v2[][]={v1}, v3[][][]={v2}, v4[][][][]={v3};
    W w0=new W(), w1[]={w0}, w2[][]={w1}, w3[][][]={w2}, w4[][][][]={w3};
    P p0=new P(), p1[]={p0}, p2[][]={p1}, p3[][][]={p2}, p4[][][][]={p3};

    accipp001a a0, a1[]={}, a2[][]={}, a3[][][]={}, a4[][][][]={};

    accipp001e e0=new accipp001e() , e1[]={e0}, e2[][]={e1}, e3[][][]={e2}, e4[][][][]={e3};

    public static void main (String args[]) {
        accipp001a a = new accipp001a();
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit"))
            System.exit(95);
        System.err.println("# Debugee: unknown instruction: " + instruction);
        System.exit(97);
    }
}

/** Sample package-private class. */
class accipp001e {}
