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


package nsk.jdi.TypeComponent.declaringType;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class decltype004a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        decltype004aMainClass mainClass = new decltype004aMainClass();
        decltype004aImplClass implClass = new decltype004aImplClass();

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
class decltype004aClass {}
interface decltype004aInter {}

class decltype004aMainClass {
    void        Mv() {};
    boolean     Mz(boolean z)      { return !z; };
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

    final        void     MvF()           {};
    final        long     MlF(long l)     { return l; };
    final        long[]   MlF1(long l[])  { return l; };
    final        long[][] MlF2(long l[][]){ return l; };
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

    decltype004aClass         MX(decltype004aClass X)          { return X; };
    decltype004aClass[]       MX1(decltype004aClass X[])       { return X; };
    decltype004aClass[][]     MX2(decltype004aClass X[][])     { return X; };
    Object        MO(Object O)         { return O; };
    Object[]      MO1(Object[] O)      { return O; };
    Object[][]    MO2(Object[][] O)    { return O; };

    final        Long     MLF(Long L)      { return L; };
    final        Long[]   MLF1(Long L[])   { return L; };
    final        Long[][] MLF2(Long L[][]) { return L; };
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

    decltype004aInter     ME(decltype004aInter E)      { return E; };
    decltype004aInter[]   ME1(decltype004aInter[] E)   { return E; };
    decltype004aInter[][] ME2(decltype004aInter[][] E) { return E; };

    final        decltype004aInter     MEF(decltype004aInter E)      { return E; };
    final        decltype004aInter[]   MEF1(decltype004aInter[] E)   { return E; };
    final        decltype004aInter[][] MEF2(decltype004aInter[][] E) { return E; };
    native       decltype004aInter     MEN(decltype004aInter E);
    native       decltype004aInter[]   MEN1(decltype004aInter[] E);
    native       decltype004aInter[][] MEN2(decltype004aInter[][] E);
    static       decltype004aInter     MES(decltype004aInter E)      { return E; };
    static       decltype004aInter[]   ME1S(decltype004aInter[] E)   { return E; };
    static       decltype004aInter[][] ME2S(decltype004aInter[][] E) { return E; };
    strictfp     decltype004aInter     MEI(decltype004aInter E)      { return E; };
    strictfp     decltype004aInter[]   MEI1(decltype004aInter[] E)   { return E; };
    strictfp     decltype004aInter[][] MEI2(decltype004aInter[][] E) { return E; };
    synchronized decltype004aInter     MEY(decltype004aInter E)      { return E; };
    synchronized decltype004aInter[]   MEY1(decltype004aInter[] E)   { return E; };
    synchronized decltype004aInter[][] MEY2(decltype004aInter[][] E) { return E; };
    public       decltype004aInter     MEU(decltype004aInter E)      { return E; };
    public       decltype004aInter[]   MEU1(decltype004aInter[] E)   { return E; };
    public       decltype004aInter[][] MEU2(decltype004aInter[][] E) { return E; };
    protected    decltype004aInter     MER(decltype004aInter E)      { return E; };
    protected    decltype004aInter[]   MER1(decltype004aInter[] E)   { return E; };
    protected    decltype004aInter[][] MER2(decltype004aInter[][] E) { return E; };
    private      decltype004aInter     MEP(decltype004aInter E)      { return E; };
    private      decltype004aInter[]   MEP1(decltype004aInter[] E)   { return E; };
    private      decltype004aInter[][] MEP2(decltype004aInter[][] E) { return E; };
}

class decltype004aImplClass implements decltype004aMainInter {
    public void        Mv() {};
    public boolean     Mz(boolean z)      { return !z; };
    public boolean[]   Mz1(boolean z[])   { return z; };
    public boolean[][] Mz2(boolean z[][]) { return z; };
    public byte        Mb(byte b)         { return b; };
    public byte[]      Mb1(byte b[])      { return b; };
    public byte[][]    Mb2(byte b[][])    { return b; };
    public char        Mc(char c)         { return c; };
    public char[]      Mc1(char c[])      { return c; };
    public char[][]    Mc2(char c[][])    { return c; };
    public double      Md(double d)       { return d; };
    public double[]    Md1(double d[])    { return d; };
    public double[][]  Md2(double d[][])  { return d; };
    public float       Mf(float f)        { return f; };
    public float[]     Mf1(float f[])     { return f; };
    public float[][]   Mf2(float f[][])   { return f; };
    public int         Mi(int i)          { return i; };
    public int[]       Mi1(int i[])       { return i; };
    public int[][]     Mi2(int i[][])     { return i; };
    public long        Ml(long l)         { return l; };
    public long[]      Ml1(long l[])      { return l; };
    public long[][]    Ml2(long l[][])    { return l; };
    public short       Mr(short r)        { return r; };
    public short[]     Mr1(short r[])     { return r; };
    public short[][]   Mr2(short r[][])   { return r; };

    public       void     MvU()            {};
    public       long     MlU(long l)      { return l; };
    public       long[]   MlU1(long l[])   { return l; };
    public       long[][] MlU2(long l[][]) { return l; };

    public decltype004aClass         MX(decltype004aClass X)          { return X; };
    public decltype004aClass[]       MX1(decltype004aClass X[])       { return X; };
    public decltype004aClass[][]     MX2(decltype004aClass X[][])     { return X; };
    public Object        MO(Object O)         { return O; };
    public Object[]      MO1(Object[] O)      { return O; };
    public Object[][]    MO2(Object[][] O)    { return O; };

    public       Long     MLU(Long L)      { return L; };
    public       Long[]   MLU1(Long[] L)   { return L; };
    public       Long[][] MLU2(Long[][] L) { return L; };

    public decltype004aInter     ME(decltype004aInter E)      { return E; };
    public decltype004aInter[]   ME1(decltype004aInter[] E)   { return E; };
    public decltype004aInter[][] ME2(decltype004aInter[][] E) { return E; };

    public       decltype004aInter     MEU(decltype004aInter E)      { return E; };
    public       decltype004aInter[]   MEU1(decltype004aInter[] E)   { return E; };
    public       decltype004aInter[][] MEU2(decltype004aInter[][] E) { return E; };
}

interface decltype004aMainInter {
    void        Mv();
    boolean     Mz(boolean z);
    boolean[]   Mz1(boolean z[]);
    boolean[][] Mz2(boolean z[][]);
    byte        Mb(byte b);
    byte[]      Mb1(byte b[]);
    byte[][]    Mb2(byte b[][]);
    char        Mc(char c);
    char[]      Mc1(char c[]);
    char[][]    Mc2(char c[][]);
    double      Md(double d);
    double[]    Md1(double d[]);
    double[][]  Md2(double d[][]);
    float       Mf(float f);
    float[]     Mf1(float f[]);
    float[][]   Mf2(float f[][]);
    int         Mi(int i);
    int[]       Mi1(int i[]);
    int[][]     Mi2(int i[][]);
    long        Ml(long l);
    long[]      Ml1(long l[]);
    long[][]    Ml2(long l[][]);
    short       Mr(short r);
    short[]     Mr1(short r[]);
    short[][]   Mr2(short r[][]);

    public       void     MvU();
    public       long     MlU(long l);
    public       long[]   MlU1(long l[]);
    public       long[][] MlU2(long l[][]);

    decltype004aClass         MX(decltype004aClass X);
    decltype004aClass[]       MX1(decltype004aClass X[]);
    decltype004aClass[][]     MX2(decltype004aClass X[][]);
    Object        MO(Object O);
    Object[]      MO1(Object[] O);
    Object[][]    MO2(Object[][] O);

    public       Long     MLU(Long L);
    public       Long[]   MLU1(Long[] L);
    public       Long[][] MLU2(Long[][] L);

    decltype004aInter     ME(decltype004aInter E);
    decltype004aInter[]   ME1(decltype004aInter[] E);
    decltype004aInter[][] ME2(decltype004aInter[][] E);

    public       decltype004aInter     MEU(decltype004aInter E);
    public       decltype004aInter[]   MEU1(decltype004aInter[] E);
    public       decltype004aInter[][] MEU2(decltype004aInter[][] E);
}
