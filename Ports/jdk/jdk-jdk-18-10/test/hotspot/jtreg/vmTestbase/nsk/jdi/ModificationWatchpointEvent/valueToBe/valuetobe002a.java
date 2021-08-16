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

package nsk.jdi.ModificationWatchpointEvent.valueToBe;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugged application in the test

class valuetobe002a {

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

    static int run( String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        // create instance of checked class
        valuetobe002aCheckedClass foo = new valuetobe002aCheckedClass();
        // initialize fields values
        foo.init();

        // notify debugger that debuggee started
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
class valuetobe002aCheckedClass {

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

    // initialize fields values
    void init() {
        initFields(false);
    }

    // change fields values
    void run() {
        initFields(true);
    }

    // calculate new fields values
    void initFields(boolean flag) {
        z0 = true;
        b0 = java.lang.Byte.MAX_VALUE;
        c0 = java.lang.Character.MAX_VALUE;
        d0 = java.lang.Double.MAX_VALUE;
        f0 = java.lang.Float.MAX_VALUE;
        i0 = java.lang.Integer.MAX_VALUE;
        l0 = java.lang.Long.MAX_VALUE;
        s0 = java.lang.Short.MAX_VALUE;

        z1  = flag ? z1  : new boolean[]   {z0};
        z2  = flag ? z2  : new boolean[][] {z1};
        b1  = flag ? b1  : new byte[]      {b0};
        b2  = flag ? b2  : new byte[][]    {b1};
        c1  = flag ? c1  : new char[]      {c0};
        c2  = flag ? c2  : new char[][]    {c1};
        d1  = flag ? d1  : new double[]    {d0};
        d2  = flag ? d2  : new double[][]  {d1};
        f1  = flag ? f1  : new float[]     {f0};
        f2  = flag ? f2  : new float[][]   {f1};
        i1  = flag ? i1  : new int[]       {i0};
        i2  = flag ? i2  : new int[][]     {i1};
        l1  = flag ? l1  : new long[]      {l0};
        l2  = flag ? l2  : new long[][]    {l1};
        s1  = flag ? s1  : new short[]     {s0};
        s2  = flag ? s2  : new short[][]   {s1};

        lS0 = l0;
        lP0 = l0;
        lU0 = l0;
        lR0 = l0;
        lT0 = l0;
        lV0 = l0;

        lS1 = flag ? lS1 : new long[]      {lS0};
        lS2 = flag ? lS2 : new long[][]    {lS1};
        lP1 = flag ? lP1 : new long[]      {lP0};
        lP2 = flag ? lP2 : new long[][]    {lP1};
        lU1 = flag ? lU1 : new long[]      {lU0};
        lU2 = flag ? lU2 : new long[][]    {lU1};
        lR1 = flag ? lR1 : new long[]      {lR0};
        lR2 = flag ? lR2 : new long[][]    {lR1};
        lT1 = flag ? lT1 : new long[]      {lT0};
        lT2 = flag ? lT2 : new long[][]    {lT1};
        lV1 = flag ? lV1 : new long[]      {lV0};
        lV2 = flag ? lV2 : new long[][]    {lV1};

        X0  = flag ? X0  : new Class();
        X1  = flag ? X1  : new Class[]     {X0};
        X2  = flag ? X2  : new Class[][]   {X1};
        Z0  = flag ? Z0  : new Boolean(true);
        Z1  = flag ? Z1  : new Boolean[]   {Z0};
        Z2  = flag ? Z2  : new Boolean[][] {Z1};
        B0  = flag ? B0  : new Byte(java.lang.Byte.MIN_VALUE);
        B1  = flag ? B1  : new Byte[]      {B0};
        B2  = flag ? B2  : new Byte[][]    {B1};
        C0  = flag ? C0  : new Character(java.lang.Character.MIN_VALUE);
        C1  = flag ? C1  : new Character[] {C0};
        C2  = flag ? C2  : new Character[][]{C1};
        D0  = flag ? D0  : Double.valueOf(java.lang.Double.MIN_VALUE);
        D1  = flag ? D1  : new Double[]    {D0};
        D2  = flag ? D2  : new Double[][]  {D1};
        F0  = flag ? F0  : Float.valueOf(java.lang.Float.MIN_VALUE);
        F1  = flag ? F1  : new Float[]     {F0};
        F2  = flag ? F2  : new Float[][]   {F1};
        I0  = flag ? I0  : Integer.valueOf(java.lang.Integer.MIN_VALUE);
        I1  = flag ? I1  : new Integer[]   {I0};
        I2  = flag ? I2  : new Integer[][] {I1};
        L0  = flag ? L0  : Long.valueOf(java.lang.Long.MIN_VALUE);
        L1  = flag ? L1  : new Long[]      {L0};
        L2  = flag ? L2  : new Long[][]    {L1};
        S0  = flag ? S0  : Short.valueOf(java.lang.Short.MIN_VALUE);
        S1  = flag ? S1  : new Short[]     {S0};
        S2  = flag ? S2  : new Short[][]   {S1};
        W0  = flag ? W0  : new String();
        W1  = flag ? W1  : new String[]    {W0};
        W2  = flag ? W2  : new String[][]  {W1};
        O0  = flag ? O0  : new Object();
        O1  = flag ? O1  : new Object[]    {O0};
        O2  = flag ? O2  : new Object[][]  {O1};

        LS0 = flag ? LS0 : Long.valueOf(java.lang.Long.MAX_VALUE);
        LS1 = flag ? LS1 : new Long[]      {LS0};
        LS2 = flag ? LS2 : new Long[][]    {LS1};
        LP0 = flag ? LP0 : Long.valueOf(java.lang.Long.MAX_VALUE);
        LP1 = flag ? LP1 : new Long[]      {LP0};
        LP2 = flag ? LP2 : new Long[][]    {LP1};
        LU0 = flag ? LU0 : Long.valueOf(java.lang.Long.MAX_VALUE);
        LU1 = flag ? LU1 : new Long[]      {LU0};
        LU2 = flag ? LU2 : new Long[][]    {LU1};
        LR0 = flag ? LR0 : Long.valueOf(java.lang.Long.MAX_VALUE);
        LR1 = flag ? LR1 : new Long[]      {LR0};
        LR2 = flag ? LR2 : new Long[][]    {LR1};
        LT0 = flag ? LT0 : Long.valueOf(java.lang.Long.MAX_VALUE);
        LT1 = flag ? LT1 : new Long[]      {LT0};
        LT2 = flag ? LT2 : new Long[][]    {LT1};
        LV0 = flag ? LV0 : Long.valueOf(java.lang.Long.MAX_VALUE);
        LV1 = flag ? LV1 : new Long[]      {LV0};
        LV2 = flag ? LV2 : new Long[][]    {LV1};

        E0  = flag ? E0  : new Class();
        E1  = flag ? E1  : new Inter[]     {E0};
        E2  = flag ? E2  : new Inter[][]   {E1};
        ES0 = flag ? ES0 : new Class();
        ES1 = flag ? ES1 : new Inter[]     {ES0};
        ES2 = flag ? ES2 : new Inter[][]   {ES1};
        EP0 = flag ? EP0 : new Class();
        EP1 = flag ? EP1 : new Inter[]     {EP0};
        EP2 = flag ? EP2 : new Inter[][]   {EP1};
        EU0 = flag ? EU0 : new Class();
        EU1 = flag ? EU1 : new Inter[]     {EU0};
        EU2 = flag ? EU2 : new Inter[][]   {EU1};
        ER0 = flag ? ER0 : new Class();
        ER1 = flag ? ER1 : new Inter[]     {ER0};
        ER2 = flag ? ER2 : new Inter[][]   {ER1};
        ET0 = flag ? ET0 : new Class();
        ET1 = flag ? ET1 : new Inter[]     {ET0};
        ET2 = flag ? ET2 : new Inter[][]   {ET1};
        EV0 = flag ? EV0 : new Class();
        EV1 = flag ? EV1 : new Inter[]     {EV0};
        EV2 = flag ? EV2 : new Inter[][]   {EV1};
    }

}
