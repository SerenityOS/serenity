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


package nsk.jdi.TypeComponent.isFinal;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class isfinal002a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        isfinal002aClassToCheck classToCheck = new isfinal002aClassToCheck();

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

class isfinal002aClassToCheck {
    // User class and interface
    class isfinal002aClass {}
    interface isfinal002aInter {}

    // Methods
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

    final void        MvF() {};
    final boolean     MzF(boolean z)      { return z; };
    final boolean[]   Mz1F(boolean z[])   { return z; };
    final boolean[][] Mz2F(boolean z[][]) { return z; };
    final byte        MbF(byte b)         { return b; };
    final byte[]      Mb1F(byte b[])      { return b; };
    final byte[][]    Mb2F(byte b[][])    { return b; };
    final char        McF(char c)         { return c; };
    final char[]      Mc1F(char c[])      { return c; };
    final char[][]    Mc2F(char c[][])    { return c; };
    final double      MdF(double d)       { return d; };
    final double[]    Md1F(double d[])    { return d; };
    final double[][]  Md2F(double d[][])  { return d; };
    final float       MfF(float f)        { return f; };
    final float[]     Mf1F(float f[])     { return f; };
    final float[][]   Mf2F(float f[][])   { return f; };
    final int         MiF(int i)          { return i; };
    final int[]       Mi1F(int i[])       { return i; };
    final int[][]     Mi2F(int i[][])     { return i; };
    final long        MlF(long l)         { return l; };
    final long[]      Ml1F(long l[])      { return l; };
    final long[][]    Ml2F(long l[][])    { return l; };
    final short       MrF(short r)        { return r; };
    final short[]     Mr1F(short r[])     { return r; };
    final short[][]   Mr2F(short r[][])   { return r; };

    static       void MvS()                {};
    static       long MlS(long l)          { return l; };
    static       long[] MlS1(long l[])     { return l; };
    static       long[][] MlS2(long l[][]) { return l; };
    native       void MvN();
    native       long MlN(long l);
    native       long[] MlN1(long l[]);
    native       long[][] MlN2(long l[][]);
    strictfp     void MvI() {};
    strictfp     long MlI(long l)          { return l; };
    strictfp     long[] MlI1(long l[])     { return l; };
    strictfp     long[][] MlI2(long l[][]) { return l; };
    synchronized void MvY() {};
    synchronized long MlY(long l)          { return l; };
    synchronized long[] MlY1(long l[])     { return l; };
    synchronized long[][] MlY2(long l[][]) { return l; };
    public       void MvU() {};
    public       long MlU(long l)          { return l; };
    public       long[] MlU1(long l[])     { return l; };
    public       long[][] MlU2(long l[][]) { return l; };
    protected    void MvR() {};
    protected    long MlR(long l)          { return l; };
    protected    long[] MlR1(long l[])     { return l; };
    protected    long[][] MlR2(long l[][]) { return l; };
    private      void MvP() {};
    private      long MlP(long l)          { return l; };
    private      long[] MlP1(long l[])     { return l; };
    private      long[][] MlP2(long l[][]) { return l; };

    final static       void MvSF()                {};
    final static       long MlSF(long l)          { return l; };
    final static       long[] MlS1F(long l[])     { return l; };
    final static       long[][] MlS2F(long l[][]) { return l; };
    final native       void MvNF();
    final native       long MlNF(long l);
    final native       long[] MlN1F(long l[]);
    final native       long[][] MlN2F(long l[][]);
    final strictfp     void MvIF() {};
    final strictfp     long MlIF(long l)          { return l; };
    final strictfp     long[] MlI1F(long l[])     { return l; };
    final strictfp     long[][] MlI2F(long l[][]) { return l; };
    final synchronized void MvYF() {};
    final synchronized long MlYF(long l)          { return l; };
    final synchronized long[] MlY1F(long l[])     { return l; };
    final synchronized long[][] MlY2F(long l[][]) { return l; };
    final public       void MvUF() {};
    final public       long MlUF(long l)          { return l; };
    final public       long[] MlU1F(long l[])     { return l; };
    final public       long[][] MlU2F(long l[][]) { return l; };
    final protected    void MvRF() {};
    final protected    long MlRF(long l)          { return l; };
    final protected    long[] MlR1F(long l[])     { return l; };
    final protected    long[][] MlR2F(long l[][]) { return l; };
    final private      void MvPF() {};
    final private      long MlPF(long l)          { return l; };
    final private      long[] MlP1F(long l[])     { return l; };
    final private      long[][] MlP2F(long l[][]) { return l; };

    isfinal002aClass         MX(isfinal002aClass X)          { return X; };
    isfinal002aClass[]       MX1(isfinal002aClass X[])       { return X; };
    isfinal002aClass[][]     MX2(isfinal002aClass X[][])     { return X; };
    Object        MO(Object O)         { return O; };
    Object[]      MO1(Object[] O)      { return O; };
    Object[][]    MO2(Object[][] O)    { return O; };

    final isfinal002aClass         MXF(isfinal002aClass X)          { return X; };
    final isfinal002aClass[]       MX1F(isfinal002aClass X[])       { return X; };
    final isfinal002aClass[][]     MX2F(isfinal002aClass X[][])     { return X; };
    final Object        MOF(Object O)         { return O; };
    final Object[]      MO1F(Object[] O)      { return O; };
    final Object[][]    MO2F(Object[][] O)    { return O; };

    static       Long     MLS(Long L)      { return L; };
    static       Long[]   MLS1(Long[] L)   { return L; };
    static       Long[][] MLS2(Long[][] L) { return L; };
    native       Long     MLN(Long L);
    native       Long[]   MLN1(Long L[]);
    native       Long[][] MLN2(Long L[][]);
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

    final static       Long     MLSF(Long L)      { return L; };
    final static       Long[]   MLS1F(Long[] L)   { return L; };
    final static       Long[][] MLS2F(Long[][] L) { return L; };
    final native       Long     MLNF(Long L);
    final native       Long[]   MLN1F(Long[] L);
    final native       Long[][] MLN2F(Long[][] L);
    final strictfp     Long     MLIF(Long L)      { return L; };
    final strictfp     Long[]   MLI1F(Long[] L)   { return L; };
    final strictfp     Long[][] MLI2F(Long[][] L) { return L; };
    final synchronized Long     MLYF(Long L)      { return L; };
    final synchronized Long[]   MLY1F(Long[] L)   { return L; };
    final synchronized Long[][] MLY2F(Long[][] L) { return L; };
    final public       Long     MLUF(Long L)      { return L; };
    final public       Long[]   MLU1F(Long[] L)   { return L; };
    final public       Long[][] MLU2F(Long[][] L) { return L; };
    final protected    Long     MLRF(Long L)      { return L; };
    final protected    Long[]   MLR1F(Long[] L)   { return L; };
    final protected    Long[][] MLR2F(Long[][] L) { return L; };
    final private      Long     MLPF(Long L)      { return L; };
    final private      Long[]   MLP1F(Long[] L)   { return L; };
    final private      Long[][] MLP2F(Long[][] L) { return L; };

    isfinal002aInter     ME(isfinal002aInter E)      { return E; };
    isfinal002aInter[]   ME1(isfinal002aInter[] E)   { return E; };
    isfinal002aInter[][] ME2(isfinal002aInter[][] E) { return E; };
    final isfinal002aInter     MEF(isfinal002aInter E)      { return E; };
    final isfinal002aInter[]   ME1F(isfinal002aInter[] E)   { return E; };
    final isfinal002aInter[][] ME2F(isfinal002aInter[][] E) { return E; };

    static       isfinal002aInter     MES(isfinal002aInter E)      { return E; };
    static       isfinal002aInter[]   MES1(isfinal002aInter[] E)   { return E; };
    static       isfinal002aInter[][] MES2(isfinal002aInter[][] E) { return E; };
    native       isfinal002aInter     MEN(isfinal002aInter E);
    native       isfinal002aInter[]   MEN1(isfinal002aInter[] E);
    native       isfinal002aInter[][] MEN2(isfinal002aInter[][] E);
    strictfp     isfinal002aInter     MEI(isfinal002aInter E)      { return E; };
    strictfp     isfinal002aInter[]   MEI1(isfinal002aInter[] E)   { return E; };
    strictfp     isfinal002aInter[][] MEI2(isfinal002aInter[][] E) { return E; };
    synchronized isfinal002aInter     MEY(isfinal002aInter E)      { return E; };
    synchronized isfinal002aInter[]   MEY1(isfinal002aInter[] E)   { return E; };
    synchronized isfinal002aInter[][] MEY2(isfinal002aInter[][] E) { return E; };
    public       isfinal002aInter     MEU(isfinal002aInter E)      { return E; };
    public       isfinal002aInter[]   MEU1(isfinal002aInter[] E)   { return E; };
    public       isfinal002aInter[][] MEU2(isfinal002aInter[][] E) { return E; };
    protected    isfinal002aInter     MER(isfinal002aInter E)      { return E; };
    protected    isfinal002aInter[]   MER1(isfinal002aInter[] E)   { return E; };
    protected    isfinal002aInter[][] MER2(isfinal002aInter[][] E) { return E; };
    private      isfinal002aInter     MEP(isfinal002aInter E)      { return E; };
    private      isfinal002aInter[]   MEP1(isfinal002aInter[] E)   { return E; };
    private      isfinal002aInter[][] MEP2(isfinal002aInter[][] E) { return E; };

    final static       isfinal002aInter     MESF(isfinal002aInter E)      { return E; };
    final static       isfinal002aInter[]   MES1F(isfinal002aInter[] E)   { return E; };
    final static       isfinal002aInter[][] MES2F(isfinal002aInter[][] E) { return E; };
    final native       isfinal002aInter     MENF(isfinal002aInter E);
    final native       isfinal002aInter[]   MEN1F(isfinal002aInter[] E);
    final native       isfinal002aInter[][] MEN2F(isfinal002aInter[][] E);
    final strictfp     isfinal002aInter     MEIF(isfinal002aInter E)      { return E; };
    final strictfp     isfinal002aInter[]   MEI1F(isfinal002aInter[] E)   { return E; };
    final strictfp     isfinal002aInter[][] MEI2F(isfinal002aInter[][] E) { return E; };
    final synchronized isfinal002aInter     MEYF(isfinal002aInter E)      { return E; };
    final synchronized isfinal002aInter[]   MEY1F(isfinal002aInter[] E)   { return E; };
    final synchronized isfinal002aInter[][] MEY2F(isfinal002aInter[][] E) { return E; };
    final public       isfinal002aInter     MEUF(isfinal002aInter E)      { return E; };
    final public       isfinal002aInter[]   MEU1F(isfinal002aInter[] E)   { return E; };
    final public       isfinal002aInter[][] MEU2F(isfinal002aInter[][] E) { return E; };
    final protected    isfinal002aInter     MERF(isfinal002aInter E)      { return E; };
    final protected    isfinal002aInter[]   MER1F(isfinal002aInter[] E)   { return E; };
    final protected    isfinal002aInter[][] MER2F(isfinal002aInter[][] E) { return E; };
    final private      isfinal002aInter     MEPF(isfinal002aInter E)      { return E; };
    final private      isfinal002aInter[]   MEP1F(isfinal002aInter[] E)   { return E; };
    final private      isfinal002aInter[][] MEP2F(isfinal002aInter[][] E) { return E; };
}
