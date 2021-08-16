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


package nsk.jdi.TypeComponent.isPackagePrivate;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class ispackageprivate002a {

    //--------------------------------------------- test mutable methods

    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = argHandler.createDebugeeLog();
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);

        ispackageprivate002a ispackageprivate002a_ = new ispackageprivate002a();

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

    //--------------------------------------------- test specific methods
           void        Mv() {};
           boolean     Mz(boolean z)       { return z; };
           boolean[]   Mz1(boolean z[])    { return z; };
           boolean[][] Mz2(boolean z[][])  { return z; };
           byte        Mb(byte b)          { return b; };
           byte[]      Mb1(byte b[])       { return b; };
           byte[][]    Mb2(byte b[][])     { return b; };
           char        Mc(char c)          { return c; };
           char[]      Mc1(char c[])       { return c; };
           char[][]    Mc2(char c[][])     { return c; };
           double      Md(double d)        { return d; };
           double[]    Md1(double d[])     { return d; };
           double[][]  Md2(double d[][])   { return d; };
           float       Mf(float f)         { return f; };
           float[]     Mf1(float f[])      { return f; };
           float[][]   Mf2(float f[][])    { return f; };
           int         Mi(int i)           { return i; };
           int[]       Mi1(int i[])        { return i; };
           int[][]     Mi2(int i[][])      { return i; };
           long        Ml(long l)          { return l; };
           long[]      Ml1(long l[])       { return l; };
           long[][]    Ml2(long l[][])     { return l; };
           short       Mr(short r)         { return r; };
           short[]     Mr1(short r[])      { return r; };
           short[][]   Mr2(short r[][])    { return r; };

    public void        MvM() {};
    public boolean     MzM(boolean z)      { return z; };
    public boolean[]   Mz1M(boolean z[])   { return z; };
    public boolean[][] Mz2M(boolean z[][]) { return z; };
    public byte        MbM(byte b)         { return b; };
    public byte[]      Mb1M(byte b[])      { return b; };
    public byte[][]    Mb2M(byte b[][])    { return b; };
    public char        McM(char c)         { return c; };
    public char[]      Mc1M(char c[])      { return c; };
    public char[][]    Mc2M(char c[][])    { return c; };
    public double      MdM(double d)       { return d; };
    public double[]    Md1M(double d[])    { return d; };
    public double[][]  Md2M(double d[][])  { return d; };
    public float       MfM(float f)        { return f; };
    public float[]     Mf1M(float f[])     { return f; };
    public float[][]   Mf2M(float f[][])   { return f; };
    public int         MiM(int i)          { return i; };
    public int[]       Mi1M(int i[])       { return i; };
    public int[][]     Mi2M(int i[][])     { return i; };
    public long        MlM(long l)         { return l; };
    public long[]      Ml1M(long l[])      { return l; };
    public long[][]    Ml2M(long l[][])    { return l; };
    public short       MrM(short r)        { return r; };
    public short[]     Mr1M(short r[])     { return r; };
    public short[][]   Mr2M(short r[][])   { return r; };

           static       void MvS()                 {};
           static       long MlS(long l)           { return l; };
           static       long[] MlS1(long l[])      { return l; };
           static       long[][] MlS2(long l[][])  { return l; };
           native       void MvN();
           native       long MlN(long l);
           native       long[] MlN1(long l[]);
           native       long[][] MlN2(long l[][]);
           strictfp     void MvI() {};
           strictfp     long MlI(long l)           { return l; };
           strictfp     long[] MlI1(long l[])      { return l; };
           strictfp     long[][] MlI2(long l[][])  { return l; };
           synchronized void MvY() {};
           synchronized long MlY(long l)           { return l; };
           synchronized long[] MlY1(long l[])      { return l; };
           synchronized long[][] MlY2(long l[][])  { return l; };
           public       void MvU() {};
           public       long MlU(long l)           { return l; };
           public       long[] MlU1(long l[])      { return l; };
           public       long[][] MlU2(long l[][])  { return l; };
           protected    void MvR() {};
           protected    long MlR(long l)           { return l; };
           protected    long[] MlR1(long l[])      { return l; };
           protected    long[][] MlR2(long l[][])  { return l; };
           private      void MvP() {};
           private      long MlP(long l)           { return l; };
           private      long[] MlP1(long l[])      { return l; };
           private      long[][] MlP2(long l[][])  { return l; };

    public static       void MvSM()                {};
    public static       long MlSM(long l)          { return l; };
    public static       long[] MlS1M(long l[])     { return l; };
    public static       long[][] MlS2M(long l[][]) { return l; };
    public native       void MvNM();
    public native       long MlNM(long l);
    public native       long[] MlN1M(long l[]);
    public native       long[][] MlN2M(long l[][]);
    public strictfp     void MvIM() {};
    public strictfp     long MlIM(long l)          { return l; };
    public strictfp     long[] MlI1M(long l[])     { return l; };
    public strictfp     long[][] MlI2M(long l[][]) { return l; };
    public synchronized void MvYM() {};
    public synchronized long MlYM(long l)          { return l; };
    public synchronized long[] MlY1M(long l[])     { return l; };
    public synchronized long[][] MlY2M(long l[][]) { return l; };
    public final        void MvPM() {};
    public final        long MlPM(long l)          { return l; };
    public final        long[] MlP1M(long l[])     { return l; };
    public final        long[][] MlP2M(long l[][]) { return l; };

           C002         MX(C002 X)          { return X; };
           C002[]       MX1(C002 X[])       { return X; };
           C002[][]     MX2(C002 X[][])     { return X; };
           Object       MO(Object O)        { return O; };
           Object[]     MO1(Object[] O)     { return O; };
           Object[][]   MO2(Object[][] O)   { return O; };

    public C002         MXM(C002 X)          { return X; };
    public C002[]       MX1M(C002 X[])       { return X; };
    public C002[][]     MX2M(C002 X[][])     { return X; };
    public Object       MOM(Object O)        { return O; };
    public Object[]     MO1M(Object[] O)     { return O; };
    public Object[][]   MO2M(Object[][] O)   { return O; };

           static       Long     MLS(Long L)       { return L; };
           static       Long[]   MLS1(Long[] L)    { return L; };
           static       Long[][] MLS2(Long[][] L)  { return L; };
           native       Long     MLN(Long L);
           native       Long[]   MLN1(Long L[]);
           native       Long[][] MLN2(Long L[][]);
           strictfp     Long     MLI(Long L)       { return L; };
           strictfp     Long[]   MLI1(Long L[])    { return L; };
           strictfp     Long[][] MLI2(Long L[][])  { return L; };
           synchronized Long     MLY(Long L)       { return L; };
           synchronized Long[]   MLY1(Long[] L)    { return L; };
           synchronized Long[][] MLY2(Long[][] L)  { return L; };
           public       Long     MLU(Long L)       { return L; };
           public       Long[]   MLU1(Long[] L)    { return L; };
           public       Long[][] MLU2(Long[][] L)  { return L; };
           protected    Long     MLR(Long L)       { return L; };
           protected    Long[]   MLR1(Long[] L)    { return L; };
           protected    Long[][] MLR2(Long[][] L)  { return L; };
           private      Long     MLP(Long L)       { return L; };
           private      Long[]   MLP1(Long[] L)    { return L; };
           private      Long[][] MLP2(Long[][] L)  { return L; };

    public static       Long     MLSM(Long L)      { return L; };
    public static       Long[]   MLS1M(Long[] L)   { return L; };
    public static       Long[][] MLS2M(Long[][] L) { return L; };
    public native       Long     MLNM(Long L);
    public native       Long[]   MLN1M(Long[] L);
    public native       Long[][] MLN2M(Long[][] L);
    public strictfp     Long     MLIM(Long L)      { return L; };
    public strictfp     Long[]   MLI1M(Long[] L)   { return L; };
    public strictfp     Long[][] MLI2M(Long[][] L) { return L; };
    public synchronized Long     MLYM(Long L)      { return L; };
    public synchronized Long[]   MLY1M(Long[] L)   { return L; };
    public synchronized Long[][] MLY2M(Long[][] L) { return L; };
    public final        Long     MLPM(Long L)      { return L; };
    public final        Long[]   MLP1M(Long[] L)   { return L; };
    public final        Long[][] MLP2M(Long[][] L) { return L; };

           I002     ME(I002 E)       { return E; };
           I002[]   ME1(I002[] E)    { return E; };
           I002[][] ME2(I002[][] E)  { return E; };
    public I002     MEM(I002 E)      { return E; };
    public I002[]   ME1M(I002[] E)   { return E; };
    public I002[][] ME2M(I002[][] E) { return E; };

           static       I002     MES(I002 E)      { return E; };
           static       I002[]   MES1(I002[] E)   { return E; };
           static       I002[][] MES2(I002[][] E) { return E; };
           native       I002     MEN(I002 E);
           native       I002[]   MEN1(I002[] E);
           native       I002[][] MEN2(I002[][] E);
           strictfp     I002     MEI(I002 E)      { return E; };
           strictfp     I002[]   MEI1(I002[] E)   { return E; };
           strictfp     I002[][] MEI2(I002[][] E) { return E; };
           synchronized I002     MEY(I002 E)      { return E; };
           synchronized I002[]   MEY1(I002[] E)   { return E; };
           synchronized I002[][] MEY2(I002[][] E) { return E; };
           public       I002     MEU(I002 E)      { return E; };
           public       I002[]   MEU1(I002[] E)   { return E; };
           public       I002[][] MEU2(I002[][] E) { return E; };
           protected    I002     MER(I002 E)      { return E; };
           protected    I002[]   MER1(I002[] E)   { return E; };
           protected    I002[][] MER2(I002[][] E) { return E; };
           private      I002     MEP(I002 E)      { return E; };
           private      I002[]   MEP1(I002[] E)   { return E; };
           private      I002[][] MEP2(I002[][] E) { return E; };

    public static       I002     MESM(I002 E)      { return E; };
    public static       I002[]   MES1M(I002[] E)   { return E; };
    public static       I002[][] MES2M(I002[][] E) { return E; };
    public native       I002     MENM(I002 E);
    public native       I002[]   MEN1M(I002[] E);
    public native       I002[][] MEN2M(I002[][] E);
    public strictfp     I002     MEIM(I002 E)      { return E; };
    public strictfp     I002[]   MEI1M(I002[] E)   { return E; };
    public strictfp     I002[][] MEI2M(I002[][] E) { return E; };
    public synchronized I002     MEYM(I002 E)      { return E; };
    public synchronized I002[]   MEY1M(I002[] E)   { return E; };
    public synchronized I002[][] MEY2M(I002[][] E) { return E; };
    public final        I002     MEPM(I002 E)      { return E; };
    public final        I002[]   MEP1M(I002[] E)   { return E; };
    public final        I002[][] MEP2M(I002[][] E) { return E; };

}

//--------------------------------------------- test specific methods
class C002 {}
interface I002 {}
