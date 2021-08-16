/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.TypeComponent.name;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class name002a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        name002aClassToCheck classToCheck = new name002aClassToCheck();

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

class name002aClassToCheck {
    // User class and interface
    class Class {}
    interface Inter {}

    void        Mv() {};
    boolean     Mz(boolean z)      { return z; };
    boolean[]   Mz1(boolean z[])   { return z; };
    boolean[][] Mz2(boolean z[][]) { return z; };
    byte        Mb(byte b)         { return b; };
    byte[]      Mb1(byte b[])      { return b; };
    byte[][]    Mb2(byte b[][])    { return b; };
    char        Mc(char c)         { return c; };
    char[]      Mc1(char c[])      { return c; };
    char[][]    Mc2(char c[][])    { return c; };
    double      Md(double d)       { return d; };
    double[]    Md1(double d[])    { return d; };
    double[][]  Md2(double d[][])  { return d; };
    float       Mf(float f)        { return f; };
    float[]     Mf1(float f[])     { return f; };
    float[][]   Mf2(float f[][])   { return f; };
    int         Mi(int i)          { return i; };
    int[]       Mi1(int i[])       { return i; };
    int[][]     Mi2(int i[][])     { return i; };
    long        Ml(long l)         { return l; };
    long[]      Ml1(long l[])      { return l; };
    long[][]    Ml2(long l[][])    { return l; };
    short       Mr(short r)        { return r; };
    short[]     Mr1(short r[])     { return r; };
    short[][]   Mr2(short r[][])   { return r; };

    final        void     MvF()            {};
    final        long     MlF(long l)      { return l; };
    final        long[]   MlF1(long l[])   { return l; };
    final        long[][] MlF2(long l[][]) { return l; };
    native       void     MvN();
    native       long     MlN(long l);
    native       long[]   MlN1(long l[]);
    native       long[][] MlN2(long l[][]);
    static       void     MvS()            {};
    static       long     MlS(long l)      { return l; };
    static       long[]   MlS1(long l[])   { return l; };
    static       long[][] MlS2(long l[][]) { return l; };
    strictfp     void     MvI()            {};
    strictfp     long     MlI(long l)      { return l; };
    strictfp     long[]   MlI1(long l[])   { return l; };
    strictfp     long[][] MlI2(long l[][]) { return l; };
    synchronized void     MvY()            {};
    synchronized long     MlY(long l)      { return l; };
    synchronized long[]   MlY1(long l[])   { return l; };
    synchronized long[][] MlY2(long l[][]) { return l; };
    public       void     MvU()            {};
    public       long     MlU(long l)      { return l; };
    public       long[]   MlU1(long l[])   { return l; };
    public       long[][] MlU2(long l[][]) { return l; };
    protected    void     MvR()            {};
    protected    long     MlR(long l)      { return l; };
    protected    long[]   MlR1(long l[])   { return l; };
    protected    long[][] MlR2(long l[][]) { return l; };
    private      void     MvP()            {};
    private      long     MlP(long l)      { return l; };
    private      long[]   MlP1(long l[])   { return l; };
    private      long[][] MlP2(long l[][]) { return l; };

    Class         MX(Class X)          { return X; };
    Class[]       MX1(Class X[])       { return X; };
    Class[][]     MX2(Class X[][])     { return X; };
    Object        MO(Object O)         { return O; };
    Object[]      MO1(Object[] O)      { return O; };
    Object[][]    MO2(Object[][] O)    { return O; };

    final        Long     MLF(Long L)      { return L; };
    final        Long[]   MLF1(Long[] L)   { return L; };
    final        Long[][] MLF2(Long[][] L) { return L; };
    native       Long     MLN(Long L);
    native       Long[]   MLN1(Long L[]);
    native       Long[][] MLN2(Long L[][]);
    static       Long     MLS(Long L)      { return L; };
    static       Long[]   MLS1(Long L[])   { return L; };
    static       Long[][] MLS2(Long L[][]) { return L; };
    strictfp     Long     MLI(Long L)      { return L; };
    strictfp     Long[]   MLI1(Long L[])   { return L; };
    strictfp     Long[][] MLI2(Long L[][]) { return L; };
    synchronized Long     MLY(Long L)      { return L; };
    synchronized Long[]   MLY1(Long[] L)   { return L; };
    synchronized Long[][] MLY2(Long[][] L) { return L; };
    public       Long     MLU(Long L)      { return L; };
    public       Long[]   MLU1(Long[] L)   { return L; };
    public       Long[][] MLU2(Long[][] L) { return L; };
    protected    Long     MLR(Long L)      { return L; };
    protected    Long[]   MLR1(Long[] L)   { return L; };
    protected    Long[][] MLR2(Long[][] L) { return L; };
    private      Long     MLP(Long L)      { return L; };
    private      Long[]   MLP1(Long[] L)   { return L; };
    private      Long[][] MLP2(Long[][] L) { return L; };

    Inter     ME(Inter E)      { return E; };
    Inter[]   ME1(Inter[] E)   { return E; };
    Inter[][] ME2(Inter[][] E) { return E; };

    final        Inter     MEF(Inter E)      { return E; };
    final        Inter[]   MEF1(Inter[] E)   { return E; };
    final        Inter[][] MEF2(Inter[][] E) { return E; };
    native       Inter     MEN(Inter E);
    native       Inter[]   MEN1(Inter[] E);
    native       Inter[][] MEN2(Inter[][] E);
    static       Inter     MES(Inter E)      { return E; };
    static       Inter[]   ME1S(Inter[] E)   { return E; };
    static       Inter[][] ME2S(Inter[][] E) { return E; };
    strictfp     Inter     MEI(Inter E)      { return E; };
    strictfp     Inter[]   MEI1(Inter[] E)   { return E; };
    strictfp     Inter[][] MEI2(Inter[][] E) { return E; };
    synchronized Inter     MEY(Inter E)      { return E; };
    synchronized Inter[]   MEY1(Inter[] E)   { return E; };
    synchronized Inter[][] MEY2(Inter[][] E) { return E; };
    public       Inter     MEU(Inter E)      { return E; };
    public       Inter[]   MEU1(Inter[] E)   { return E; };
    public       Inter[][] MEU2(Inter[][] E) { return E; };
    protected    Inter     MER(Inter E)      { return E; };
    protected    Inter[]   MER1(Inter[] E)   { return E; };
    protected    Inter[][] MER2(Inter[][] E) { return E; };
    private      Inter     MEP(Inter E)      { return E; };
    private      Inter[]   MEP1(Inter[] E)   { return E; };
    private      Inter[][] MEP2(Inter[][] E) { return E; };
}
