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

package nsk.jdi.WatchpointEvent.valueCurrent;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugged application in the test

class valuecur001a {

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    public static void main(String args[]) {
        System.exit(JCK_STATUS_BASE + run(args));
    }

    static int run(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        // create instance of checked class
        CheckedClass foo = new CheckedClass();
        foo.run();

        // notify debuggeer that debuggee started
        pipe.println(COMMAND_READY);

        // wait for command GO from debugger
        String command = pipe.readln();
        if (command.equals(COMMAND_QUIT)) {
             return PASSED;
        }
        if (!command.equals(COMMAND_GO)) {
             System.err.println("TEST BUG: unknown command: " + command);
             return FAILED;
        }

        // perform actions
        foo.run();

        // notify debugger that the command done
        pipe.println(COMMAND_DONE);

        // wait for command QUIT from debugger and exit
        command = pipe.readln();
        if (!command.equals(COMMAND_QUIT)) {
             System.err.println("TEST BUG: unknown command: " + command);
             return FAILED;
        }
        return PASSED;
    }
}

// class for checking
class CheckedClass {

    boolean z0, z1[], z2[][];
    byte    b0, b1[], b2[][];
    char    c0, c1[], c2[][];
    double  d0, d1[], d2[][];
    float   f0, f1[], f2[][];
    int     i0, i1[], i2[][];
    long    l0, l1[], l2[][];
    short   s0, s1[], s2[][];

    static    long lS0, lS1[], lS2[][];
    private   long lP0, lP1[], lP2[][];
    public    long lU0, lU1[], lU2[][];
    protected long lR0, lR1[], lR2[][];
    transient long lT0, lT1[], lT2[][];
    volatile  long lV0, lV1[], lV2[][];

    Boolean   Z0, Z1[], Z2[][];
    Byte      B0, B1[], B2[][];
    Character C0, C1[], C2[][];
    Double    D0, D1[], D2[][];
    Float     F0, F1[], F2[][];
    Integer   I0, I1[], I2[][];
    Long      L0, L1[], L2[][];
    String    W0, W1[], W2[][];
    Short     S0, S1[], S2[][];
    Object    O0, O1[], O2[][];

    static    Long LS0, LS1[], LS2[][];
    private   Long LP0, LP1[], LP2[][];
    public    Long LU0, LU1[], LU2[][];
    protected Long LR0, LR1[], LR2[][];
    transient Long LT0, LT1[], LT2[][];
    volatile  Long LV0, LV1[], LV2[][];

    interface Inter {}
    class Class implements Inter {}
    Class     X0, X1[], X2[][];
    Inter     E0, E1[], E2[][];
    static    Inter ES0, ES1[], ES2[][];
    private   Inter EP0, EP1[], EP2[][];
    public    Inter EU0, EU1[], EU2[][];
    protected Inter ER0, ER1[], ER2[][];
    transient Inter ET0, ET1[], ET2[][];
    volatile  Inter EV0, EV1[], EV2[][];

    void run() {

        z0 = true;
        b0 = 0;
        c0 = 0;
        d0 = 0;
        f0 = 0;
        i0 = 0;
        l0 = 0;
        s0 = 0;

        lS0 = 0;
        lP0 = 0;
        lU0 = 0;
        lR0 = 0;
        lT0 = 0;
        lV0 = 0;

        z1  = new boolean[]   {z0};
        z2  = new boolean[][] {z1};
        b1  = new byte[]      {b0};
        b2  = new byte[][]    {b1};
        c1  = new char[]      {c0};
        c2  = new char[][]    {c1};
        d1  = new double[]    {d0};
        d2  = new double[][]  {d1};
        f1  = new float[]     {f0};
        f2  = new float[][]   {f1};
        i1  = new int[]       {i0};
        i2  = new int[][]     {i1};
        l1  = new long[]      {l0};
        l2  = new long[][]    {l1};
        s1  = new short[]     {s0};
        s2  = new short[][]   {s1};

        lS1 = new long[]      {lS0};
        lS2 = new long[][]    {lS1};
        lP1 = new long[]      {lP0};
        lP2 = new long[][]    {lP1};
        lU1 = new long[]      {lU0};
        lU2 = new long[][]    {lU1};
        lR1 = new long[]      {lR0};
        lR2 = new long[][]    {lR1};
        lT1 = new long[]      {lT0};
        lT2 = new long[][]    {lT1};
        lV1 = new long[]      {lV0};
        lV2 = new long[][]    {lV1};

        X0  = new Class();
        X1  = new Class[]     {X0};
        X2  = new Class[][]   {X1};
        Z0  = Boolean.valueOf(true);
        Z1  = new Boolean[]   {Z0};
        Z2  = new Boolean[][] {Z1};
        B0  = Byte.valueOf(java.lang.Byte.MIN_VALUE);
        B1  = new Byte[]      {B0};
        B2  = new Byte[][]    {B1};
        C0  = Character.valueOf(java.lang.Character.MIN_VALUE);
        C1  = new Character[] {C0};
        C2  = new Character[][]{C1};
        D0  = Double.valueOf(java.lang.Double.MIN_VALUE);
        D1  = new Double[]    {D0};
        D2  = new Double[][]  {D1};
        F0  = Float.valueOf(java.lang.Float.MIN_VALUE);
        F1  = new Float[]     {F0};
        F2  = new Float[][]   {F1};
        I0  = Integer.valueOf(java.lang.Integer.MIN_VALUE);
        I1  = new Integer[]   {I0};
        I2  = new Integer[][] {I1};
        L0  = Long.valueOf(java.lang.Long.MIN_VALUE);
        L1  = new Long[]      {L0};
        L2  = new Long[][]    {L1};
        S0  = Short.valueOf(java.lang.Short.MIN_VALUE);
        S1  = new Short[]     {S0};
        S2  = new Short[][]   {S1};
        W0  = new String();
        W1  = new String[]    {W0};
        W2  = new String[][]  {W1};
        O0  = new Object();
        O1  = new Object[]    {O0};
        O2  = new Object[][]  {O1};

        LS0 = Long.valueOf(java.lang.Long.MAX_VALUE);
        LS1 = new Long[]      {LS0};
        LS2 = new Long[][]    {LS1};
        LP0 = Long.valueOf(java.lang.Long.MAX_VALUE);
        LP1 = new Long[]      {LP0};
        LP2 = new Long[][]    {LP1};
        LU0 = Long.valueOf(java.lang.Long.MAX_VALUE);
        LU1 = new Long[]      {LU0};
        LU2 = new Long[][]    {LU1};
        LR0 = Long.valueOf(java.lang.Long.MAX_VALUE);
        LR1 = new Long[]      {LR0};
        LR2 = new Long[][]    {LR1};
        LT0 = Long.valueOf(java.lang.Long.MAX_VALUE);
        LT1 = new Long[]      {LT0};
        LT2 = new Long[][]    {LT1};
        LV0 = Long.valueOf(java.lang.Long.MAX_VALUE);
        LV1 = new Long[]      {LV0};
        LV2 = new Long[][]    {LV1};

        E0  = new Class();
        E1  = new Inter[]     {E0};
        E2  = new Inter[][]   {E1};
        ES0 = new Class();
        ES1 = new Inter[]     {ES0};
        ES2 = new Inter[][]   {ES1};
        EP0 = new Class();
        EP1 = new Inter[]     {EP0};
        EP2 = new Inter[][]   {EP1};
        EU0 = new Class();
        EU1 = new Inter[]     {EU0};
        EU2 = new Inter[][]   {EU1};
        ER0 = new Class();
        ER1 = new Inter[]     {ER0};
        ER2 = new Inter[][]   {ER1};
        ET0 = new Class();
        ET1 = new Inter[]     {ET0};
        ET2 = new Inter[][]   {ET1};
        EV0 = new Class();
        EV1 = new Inter[]     {EV0};
        EV2 = new Inter[][]   {EV1};

       m(z2); m(b2); m(c2); m(d2); m(f2); m(i2); m(l2); m(s2);

       m(lS2); m(lP2); m(lU2); m(lR2); m(lT2); m(lV2);

       m(X2); m(Z2); m(B2); m(C2); m(D2); m(F2); m(I2); m(S2); m(W2); m(O2);

       m(LS2); m(LP2); m(LU2); m(LR2); m(LT2); m(LV2);

       m(X2); m(E2); m(ES2); m(EP2); m(EU2); m(ER2); m(ET2); m(EV2);

    }

    void m(Object obj) {
        return;
    }
}
